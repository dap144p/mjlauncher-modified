package git.artdeell.mojo.multisession;
import android.app.Activity;
import android.content.Context;
import android.os.Process;
import android.util.Log;
import git.artdeell.mojo.prefs.LauncherPreferences;
import java.io.*;
import java.util.List;

/** Android-side control. Each game process has its own state file. */
public final class SessionNativeAfk {
    private static File root, state, engine;
    private SessionNativeAfk() {}
    private static void write(File file, boolean value) throws IOException {
        File temp = new File(file.getPath() + "." + Process.myPid() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) { out.write(value ? '1' : '0'); }
        if (!temp.renameTo(file)) { temp.delete(); throw new IOException("AFK state rename"); }
    }
    public static void saveSettings(Context context) {
        // A game process may cache SharedPreferences; only the launcher owns
        // the global setting, otherwise returning to an old session reverts it.
        if (sessionProcess()) return;
        try {
            File folder = new File(context.getFilesDir(), "native-afk");
            folder.mkdirs();
            boolean enabled = LauncherPreferences.DEFAULT_PREF == null ||
                LauncherPreferences.DEFAULT_PREF.getBoolean("nativeAfkEnabled", true);
            write(new File(folder, "enabled"), enabled);
        } catch (Exception failure) { Log.w("MJNativeAFK", "Cannot save AFK setting", failure); }
    }
    private static boolean sessionProcess() {
        try (FileInputStream in = new FileInputStream("/proc/self/cmdline")) {
            byte[] bytes = new byte[256]; int n = in.read(bytes); int end = 0;
            while (end < n && bytes[end] != 0) end++;
            return new String(bytes, 0, end, "UTF-8").matches(".*:session[A-T]");
        } catch (Exception failure) { return true; }
    }
    public static void install(Activity activity) {
        try {
            root = new File(activity.getFilesDir(), "native-afk"); root.mkdirs();
            state = new File(root, "process-" + Process.myPid() + ".state");
            write(state, false);
            saveSettings(activity);
            engine = new File(root, "afk-engine-v9.jar");
            // Atomic, per-process temporary name also permits concurrent session startup.
            File temp = new File(root, "engine-" + Process.myPid() + ".tmp");
            try (InputStream in = activity.getAssets().open("native-afk/afk-engine-v9.jar");
                 FileOutputStream out = new FileOutputStream(temp)) {
                byte[] block = new byte[32768]; int count;
                while ((count = in.read(block)) != -1) out.write(block, 0, count);
            }
            if (!temp.renameTo(engine)) { temp.delete(); throw new IOException("AFK engine rename"); }
        } catch (Exception failure) {
            engine = null;
            Log.w("MJNativeAFK", "Built-in AFK unavailable", failure);
        }
    }
    public static void addJavaArgs(List<String> arguments) {
        if (engine == null || state == null || !engine.isFile()) return;
        arguments.add("-javaagent:" + engine.getAbsolutePath() + "=" + state.getAbsolutePath());
    }
    public static void hidden() { visible(false); }
    public static void shown() { visible(true); }
    private static void visible(boolean foreground) {
        if (state == null) return;
        try { write(state, !foreground); }
        catch (Exception failure) { Log.w("MJNativeAFK", "Cannot update AFK state", failure); }
    }
    public static String status() {
        return "AFK bawaan launcher • tanpa Fabric/mod • render OpenGL dan audio otomatis saat background";
    }
}