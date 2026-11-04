package git.artdeell.mojo.afk;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

/** Runs inside the game's OpenJDK, separate from Android ART. */
public final class Bridge {
    private static AfkPolicyV111 afkPolicy;
    private static final ThreadLocal<Long> lastFrame = new ThreadLocal<Long>();
    // Minecraft initializes gain on the client thread, then updates the same
    // OpenAL listener on its sound executor. Sessions use separate processes.
    private static final ConcurrentHashMap<Class<?>, Audio> audio = new ConcurrentHashMap<Class<?>, Audio>();
    private static final ConcurrentHashMap<Class<?>, Method> swaps = new ConcurrentHashMap<Class<?>, Method>();
    private static final ConcurrentHashMap<Class<?>, Method> gains = new ConcurrentHashMap<Class<?>, Method>();
    private static final int AL_GAIN = 0x100A;
    private static final AtomicInteger lastBackgroundLog = new AtomicInteger(-1);
    private static final ConcurrentHashMap<Class<?>, Sources> sources =
        new ConcurrentHashMap<Class<?>, Sources>();
    private static final ConcurrentHashMap<Class<?>, Method> sourceGains =
        new ConcurrentHashMap<Class<?>, Method>();
    private static final ConcurrentHashMap<Class<?>, Method> sourcePlays =
        new ConcurrentHashMap<Class<?>, Method>();
    private static final class Sources {
        final ConcurrentHashMap<Integer,Float> gain = new ConcurrentHashMap<Integer,Float>();
        boolean lastMute;
    }
    private static Sources getSources(Class<?> api) {
        Sources known = sources.get(api);
        if (known != null) return known;
        Sources created = new Sources();
        Sources previous = sources.putIfAbsent(api, created);
        return previous == null ? created : previous;
    }
    private Bridge() {}
    public static void initialize(String state) {
        afkPolicy = new AfkPolicyV111(new File(state));
    }
    public static boolean background() {
        boolean muted = afkPolicy != null && afkPolicy.shouldSuppress();
        int value = muted ? 1 : 0;
        int old = lastBackgroundLog.getAndSet(value);
        if (old != value) System.err.println("[MJ AFK] presentation=" + (muted ? "suppressed" : "normal") +
                ", background=" + muted);
        return muted;
    }
    /** OR into the game's existing noRender check, without changing the field. */
    public static boolean suppressRender(boolean original) { return original || background(); }
    private static Method method(ConcurrentHashMap<Class<?>, Method> cache, Class<?> api,
                                 String name, Class<?>... arguments) {
        Method m = cache.get(api);
        if (m != null) return m;
        try { m = api.getMethod(name, arguments); cache.put(api, m); return m; }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private static void call(Method method, Object... args) {
        try { method.invoke(null, args); }
        catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) throw (Error)cause;
            if (cause instanceof RuntimeException) throw (RuntimeException)cause;
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    // Window probes are pass-through. They NEVER substitute fake monitors or contexts.
    private static final java.util.concurrent.atomic.AtomicBoolean monitorReported =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicBoolean videoReported =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    public static void observeGlfwInit(boolean initialized) {
        System.err.println("[MJ GLFW] glfwInit=" + initialized);
    }
    public static void observeMonitor(long monitor) {
        if (monitorReported.compareAndSet(false, true))
            System.err.println("[MJ GLFW] primaryMonitor=" + monitor);
    }
    public static void observeVideoMode(Object videoMode) {
        if (videoReported.compareAndSet(false, true))
            System.err.println("[MJ GLFW] videoMode=" + (videoMode == null ? "NULL" : "available"));
    }
    public static void observeWindow(long window) {
        System.err.println("[MJ GLFW] createdWindow=" + window);
    }
    /**
     * In Minecraft 1.13/1.14, the monitor manager caches the handles from
     * glfwGetMonitors(), but Android's virtual primary monitor can be missing
     * from that initial list even though glfwGetPrimaryMonitor() is valid.
     * Recover using Minecraft's own private monitor-connected callback; never
     * synthesize a native handle or modify lwjgl/GLFW internals.
     */
    public static Object recoverMonitor(Object found, Object manager, long handle) {
        if (found != null || handle == 0L || manager == null) return found;
        final Class<?> type = manager.getClass();
        final String name = type.getName();
        if (!"cfr".equals(name) && !"cty".equals(name)) return found;
        final String mapName = "cfr".equals(name) ? "b" : "a";
        try {
            java.lang.reflect.Field field = type.getDeclaredField(mapName);
            field.setAccessible(true);
            Object raw = field.get(manager);
            if (!(raw instanceof java.util.Map)) return found;
            @SuppressWarnings("unchecked")
            java.util.Map<Object,Object> map = (java.util.Map<Object,Object>)raw;
            synchronized (manager) {
                Object entry = map.get(Long.valueOf(handle));
                if (entry != null) return entry;
                Method connect = type.getDeclaredMethod("a", long.class, int.class);
                connect.setAccessible(true);
                connect.invoke(manager, Long.valueOf(handle), Integer.valueOf(0x00040001));
                entry = map.get(Long.valueOf(handle));
                System.err.println("[MJ GLFW] " + name + " monitor recovery: handle=" + handle +
                    ", cached=" + (entry != null));
                return entry;
            }
        } catch (Throwable failure) {
            System.err.println("[MJ GLFW] " + name + " monitor recovery failed: " + failure);
            return found;
        }
    }
    /** Called on the sound thread. Existing playing sources get their gain refreshed
     * once per background transition, never by a polling Java thread with no AL context. */
    private static void updateSourceMute(Class<?> api) {
        Sources state = sources.get(api);
        if (state == null) return;
        boolean mute = background();
        synchronized (state) {
            if (state.lastMute == mute) return;
            state.lastMute = mute;
            for (Map.Entry<Integer,Float> item : state.gain.entrySet()) {
                setSourceGain(api, item.getKey(), mute ? 0f : item.getValue());
            }
            System.err.println("[MJ AFK] OpenAL tracked sources=" + state.gain.size() +
                               ", muted=" + mute);
        }
    }
    private static void setSourceGain(Class<?> api, int source, float volume) {
        try {
            call(method(sourceGains, api, "alSourcef", int.class, int.class, float.class),
                 source, AL_GAIN, volume);
        } catch (RuntimeException failure) {
            // Never prevent Minecraft from continuing when a source has been deleted.
            System.err.println("[MJ AFK] OpenAL source gain update failed: " + failure);
        }
    }
    /** Observe source volume changes without losing the volume requested by Minecraft. */
    public static void sourceGain(int source, int parameter, float value, Class<?> api) {
        if (parameter == AL_GAIN) {
            Sources state = getSources(api);
            if (state.gain.size() > 1024) state.gain.clear();
            state.gain.put(source, value);
        }
        updateSourceMute(api);
        call(method(sourceGains, api, "alSourcef", int.class, int.class, float.class),
             source, parameter, parameter == AL_GAIN && background() ? 0f : value);
    }
    /** New sounds spawned in background must start silent. */
    public static void sourcePlay(int source, Class<?> api) {
        Sources state = getSources(api);
        state.gain.putIfAbsent(source, 1.0f);
        updateSourceMute(api);
        if (background()) setSourceGain(api, source, 0f);
        call(method(sourcePlays, api, "alSourcePlay", int.class), source);
    }

    /** Keep event polling and ticks running; suppress only presentation. */
    public static void swapBuffers(long window, Class<?> api) {
        // Menus may not update the camera/listener. Minecraft also sets master
        // gain from this render thread, so refresh only contexts observed here.
        for (Audio a : audio.values()) if (a.gainThread == Thread.currentThread().getId()) audioPulse(a.api);
        if (!background()) {
            lastFrame.remove();
            call(method(swaps, api, "glfwSwapBuffers", long.class), window);
            return;
        }
        long now = System.nanoTime();
        Long previous = lastFrame.get();
        if (previous != null) {
            // 20 Hz ceiling is intentional: ticking/network code can share the render thread.
            // 0 presented frames does NOT mean that Minecraft can safely tick at 1 Hz.
            long wait = 50000000L - (now - previous.longValue());
            if (wait > 0) LockSupport.parkNanos(wait);
        }
        lastFrame.set(System.nanoTime());
    }
    private static final class Audio {
        final Class<?> api;
        float requested = 1f;
        boolean muted;
        volatile long gainThread;
        Audio(Class<?> api) { this.api = api; }
    }
    public static void listenerGain(int parameter, float value, Class<?> api) {
        Audio a = audio.get(api);
        if (a == null) { Audio fresh = new Audio(api); Audio old = audio.putIfAbsent(api, fresh); a = old == null ? fresh : old; }
        synchronized (a) {
            if (parameter == AL_GAIN) { a.requested = value; a.muted = background(); a.gainThread = Thread.currentThread().getId(); }
            call(method(gains, api, "alListenerf", int.class, float.class), parameter,
                 parameter == AL_GAIN && a.muted ? 0f : value);
        }
    }
    /** Called on the same thread/context as the game's listener update. */
    public static void audioPulse(Class<?> api) {
        updateSourceMute(api);
        Audio a = audio.get(api);
        // Do not guess a volume before observing the game's actual gain.
        if (a == null) return;
        synchronized (a) {
            boolean mute = background();
            if (mute != a.muted) {
                a.muted = mute;
                call(method(gains, api, "alListenerf", int.class, float.class), AL_GAIN,
                     mute ? 0f : a.requested);
            }
        }
    }
}