package git.artdeell.mojo.afk;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

/** Runs inside the game's OpenJDK, separate from Android ART. */
public final class Bridge {
    private static File stateFile, enabledFile;
    private static volatile boolean hidden;
    private static volatile long nextRead;
    private static final ThreadLocal<Long> lastFrame = new ThreadLocal<Long>();
    // Minecraft initializes gain on the client thread, then updates the same
    // OpenAL listener on its sound executor. Sessions use separate processes.
    private static final ConcurrentHashMap<Class<?>, Audio> audio = new ConcurrentHashMap<Class<?>, Audio>();
    private static final ConcurrentHashMap<Class<?>, Method> swaps = new ConcurrentHashMap<Class<?>, Method>();
    private static final ConcurrentHashMap<Class<?>, Method> gains = new ConcurrentHashMap<Class<?>, Method>();
    private static final int AL_GAIN = 0x100A;
    private Bridge() {}
    public static void initialize(String state) {
        stateFile = new File(state);
        enabledFile = new File(stateFile.getParentFile(), "enabled");
        nextRead = 0;
    }
    private static boolean bit(File file, boolean fallback) {
        if (file == null || !file.isFile()) return fallback;
        try (FileInputStream in = new FileInputStream(file)) { return in.read() == '1'; }
        catch (IOException failure) { return fallback; }
    }
    public static boolean background() {
        long now = System.nanoTime();
        if (now >= nextRead) {
            synchronized (Bridge.class) {
                if (now >= nextRead) {
                    hidden = bit(enabledFile, true) && bit(stateFile, false);
                    nextRead = now + 100000000L;
                }
            }
        }
        return hidden;
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