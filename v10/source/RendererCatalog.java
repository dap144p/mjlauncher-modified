package git.artdeell.mojo.game.renderer;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.game.renderer.impl.GLESRenderSpec;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.utils.GpuUtils;
import git.artdeell.mojo.utils.JREUtils;
import git.artdeell.mojoexec.MojoExec;
import java.io.*;
import java.util.*;

/** Complete catalog of the backends integrated into this MJ build. */
public final class RendererCatalog {
    private RendererCatalog() {}
    public static final String[] IDS = {
        "opengles2", "opengles2_angle", "opengles3_ltw", "opengles3_ltw_angle",
        "mobileglues", "mobileglues_angle", "vulkan_zink", "vulkan_legacyzink",
        "freedreno_kgsl", "mesa_desktop", "mesa_desktop_ext"
    };
    private static final String[] LABELS = {
        "Holy GL4ES", "Holy GL4ES + ANGLE", "LTW", "LTW + ANGLE",
        "MobileGlues 2.0.0", "MobileGlues 2.0.0 + ANGLE", "Zink (Mesa bawaan)",
        "Zink Legacy (Mesa 23.0.4)", "Freedreno (Adreno)", "Mesa Desktop (Chromebook)", "Mesa eksternal"
    };
    private static File lib(String name) { return new File(Tools.NATIVE_LIB_DIR, name); }
    private static boolean angleReady(Context context) {
        return Build.VERSION.SDK_INT >= 26 && GpuUtils.checkVulkanSupport(context.getPackageManager()) &&
            lib("libEGL_angle.so").isFile() && lib("libGLESv2_angle.so").isFile();
    }
    private static boolean glesReady() { return JREUtils.getDetectedVersion() >= 3; }
    public static String label(String id) {
        for (int i=0;i<IDS.length;i++) if (IDS[i].equals(id)) return LABELS[i];
        return id == null ? "(kosong)" : id;
    }
    public static RendererCache catalog(Context context) {
        ArrayList<String> ids = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();
        for (String id : IDS) {
            ids.add(id);
            RenderSpec spec = GameRenderer.getKnownRenderer(id);
            boolean ready = false;
            try { ready = spec != null && spec.compatibleDevice(context); }
            catch (RuntimeException failure) { Log.w("Renderer", "Cannot check " + id, failure); }
            names.add(label(id) + (ready ? "" : " — " + unavailable(id)));
        }
        return new RendererCache(ids, names.toArray(new String[0]));
    }
    private static String unavailable(String id) {
        if (id.endsWith("_angle")) return "perlu Vulkan dan ANGLE yang cocok";
        if (id.equals("freedreno_kgsl")) return "khusus GPU Adreno, Android 10+";
        if (id.equals("mesa_desktop")) return "khusus Chromebook, Android 10+";
        if (id.equals("mesa_desktop_ext")) return "perlu plugin Mesa eksternal";
        if (id.equals("vulkan_legacyzink")) return "perlu Vulkan, Android 10+, ABI ARM";
        if (id.equals("vulkan_zink")) return "perlu Vulkan dan Mesa, Android 10+";
        return "perlu OpenGL ES 3 dan library sesuai ABI";
    }
    /** Called before the original registry switch; null means an original Mesa backend. */
    public static RenderSpec find(String id) {
        if ("opengles2".equals(id) || "opengles2_4".equals(id) || "opengles2_5".equals(id)) return new PinnedGl4es(false);
        if ("opengles2_angle".equals(id)) return new PinnedGl4es(true);
        if ("opengles3_ltw".equals(id)) return new PinnedLtw(false);
        if ("opengles3_ltw_angle".equals(id)) return new PinnedLtw(true);
        if ("mobileglues".equals(id)) return new MobileGlues(false);
        if ("mobileglues_angle".equals(id)) return new MobileGlues(true);
        if ("vulkan_legacyzink".equals(id)) return new LegacyZink();
        return null;
    }
    public static RenderSpec require(String id) {
        RenderSpec spec = id == null ? null : GameRenderer.getKnownRenderer(id);
        if (spec == null) throw new IllegalArgumentException("Renderer pilihan tidak dikenal: " + label(id) + ". Pilihan instance tidak diubah.");
        return spec;
    }
    public static void validate(Context context, RenderSpec spec) {
        if (spec == null || !spec.compatibleDevice(context)) {
            String id = spec == null ? null : spec.tag();
            throw new IllegalStateException("Renderer " + label(id) + " belum bisa dipakai: " + unavailable(id == null ? "" : id) + ". Pilihan disimpan tetap; pilih renderer lain secara manual.");
        }
        // Existing LTW option checks must reflect the pinned backend, not a global preference.
        LauncherPreferences.PREF_USE_ANGLE = spec.tag().endsWith("_angle");
        Log.i("Renderer", "Renderer instance dikunci: " + spec.tag());
    }
    public static void rejectAutomaticChange(RenderSpec selected, RenderSpec proposed) {
        throw new IllegalStateException("Renderer " + label(selected.tag()) + " tidak cocok dengan versi/mod game ini. Pilihan instance tetap. Jika ingin mengganti, pilih " + label(proposed.tag()) + " secara manual di pengaturan instance.");
    }
    public static void guardReplacement(RenderSpec selected, RenderSpec proposed) {
        if (selected == null || proposed == null || !selected.tag().equals(proposed.tag()))
            throw new IllegalStateException("Renderer instance terkunci; tidak boleh diganti otomatis saat mulai.");
    }
    public static boolean setupOrThrow(RenderSpec selected) {
        if (!selected.setupRenderer()) throw new IllegalStateException("Gagal memuat renderer " + label(selected.tag()) + ". Tidak beralih ke renderer lain; pilihan instance tetap.");
        return true;
    }
    private static void configureGles(Context context, Map<String,String> env, boolean angle) {
        if (angle && !angleReady(context)) throw new IllegalStateException("ANGLE pilihan tidak tersedia pada GPU/ABI ini.");
        env.put("LIBGL_EGL", angle ? lib("libEGL_angle.so").getAbsolutePath() : "libEGL.so");
        env.put("LIBGL_GLES", angle ? lib("libGLESv2_angle.so").getAbsolutePath() : "libGLESv2.so");
        if (LauncherPreferences.PREF_DUMP_SHADERS) env.put("LIBGL_VGPU_DUMP", "1");
        env.put("force_glsl_extensions_warn", "true");
        env.put("allow_higher_compat_version", "true");
        env.put("allow_glsl_extension_directive_midshader", "true");
        env.put("LIBGL_NOERROR", "1");
    }
    public static final class PinnedLtw extends GLESRenderSpec.LTWRenderSpec {
        private final boolean angle;
        public PinnedLtw(boolean angle) { this.angle=angle; }
        public String tag() { return angle ? "opengles3_ltw_angle" : "opengles3_ltw"; }
        public String name() { return label(tag()); }
        public boolean compatibleDevice(Context context) { return super.compatibleDevice(context) && (!angle || angleReady(context)); }
        public void setupEnvironment(Context context, Map<String,String> env) { configureGles(context,env,angle); }
        public boolean setupRenderer() { return MojoExec.prepareEgl(library(),false,true,3); }
    }
    public static final class PinnedGl4es extends GLESRenderSpec.GL4ESRenderSpec {
        private final boolean angle;
        public PinnedGl4es(boolean angle) { this.angle=angle; }
        public String tag() { return angle ? "opengles2_angle" : "opengles2"; }
        public String name() { return label(tag()); }
        public boolean compatibleDevice(Context context) { return lib(library()).isFile() && (!angle || angleReady(context)); }
        public void setupEnvironment(Context context, Map<String,String> env) { configureGles(context,env,angle); }
        public boolean setupRenderer() { return MojoExec.prepareEgl(library(),false,true,2); }
    }
    public static final class MobileGlues implements RenderSpec {
        private final boolean angle;
        public MobileGlues(boolean angle) { this.angle=angle; }
        public String tag() { return angle ? "mobileglues_angle" : "mobileglues"; }
        public String name() { return label(tag()); }
        public int displayName() { return 0; } // catalog supplies an explicit label
        public String library() { return "libmobileglues.so"; }
        public String librarySearchPath() { return null; }
        public boolean compatibleDevice(Context context) { return glesReady() && lib(library()).isFile() && (!angle || angleReady(context)); }
        public void setupEnvironment(Context context, Map<String,String> env) {
            configureGles(context,env,angle);
            // Keep MG settings/cache/logs independent for every live session process.
            File folder = new File(context.getFilesDir(), "renderer-state/mobileglues/" + Process.myPid());
            if (!folder.isDirectory() && !folder.mkdirs()) throw new IllegalStateException("Cannot create MobileGlues settings directory");
            File config = new File(folder,"config.json");
            // Pin ANGLE explicitly: neither automatic detection nor plugin settings may replace it.
            String json = "{\"enableANGLE\":" + (angle ? 3 : 2) + ",\"enableNoError\":0,\"enableExtTimerQuery\":1,\"enableExtDirectStateAccess\":1}";
            try (FileOutputStream out=new FileOutputStream(config)) { out.write(json.getBytes("UTF-8")); }
            catch(IOException failure) { throw new IllegalStateException("Cannot save MobileGlues renderer settings",failure); }
            env.put("MG_DIR_PATH",folder.getAbsolutePath());
            env.put("MG_ANGLE_DIR",angle ? Tools.NATIVE_LIB_DIR : "");
        }
        public boolean setupRenderer() { return MojoExec.prepareEgl(library(),false,true,3); }
    }
    public static final class LegacyZink implements RenderSpec {
        public String tag() { return "vulkan_legacyzink"; }
        public String name() { return label(tag()); }
        public int displayName() { return 0; }
        public String library() { return "libEGL_legacy.so"; }
        public String librarySearchPath() { return null; }
        public boolean compatibleDevice(Context context) {
            return Build.VERSION.SDK_INT>=29 && GpuUtils.checkVulkanSupport(context.getPackageManager()) &&
                lib(library()).isFile() && lib("libgallium_old.so").isFile() && lib("libdrz.so").isFile();
        }
        public void setupEnvironment(Context context, Map<String,String> env) {
            env.put("MESA_GLSL_CACHE_DIR",Tools.DIR_CACHE.getAbsolutePath());
            env.put("MESA_GL_VERSION_OVERRIDE","4.3");env.put("MESA_GLSL_VERSION_OVERRIDE","460");
            env.put("MESA_LOADER_DRIVER_OVERRIDE","zink");
        }
        public boolean setupRenderer() { MojoExec.preloadVulkan(); return MojoExec.prepareEgl(library(),true,false,0); }
    }
}