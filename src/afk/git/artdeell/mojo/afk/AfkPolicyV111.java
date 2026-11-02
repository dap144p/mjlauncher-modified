package git.artdeell.mojo.afk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** v1.11 per-slot AFK policy. A=0 .. T=19, retained from v10. */
public final class AfkPolicyV111 {
    private final File state;
    private final File slotEnabled;
    private final File globalEnabled;
    private long nextReadNs;
    private boolean hidden;

    public AfkPolicyV111(File processState) {
        state = processState;
        File root = processState.getParentFile();
        globalEnabled = new File(root, "enabled");
        slotEnabled = findSlotFlag(root, processState.getName());
    }

    static File findSlotFlag(File root, String stateName) {
        if (!stateName.matches("process-[0-9]+\\.state")) return null;
        String prefix = stateName.substring(0, stateName.length() - ".state".length());
        String id = readSmall(new File(root, prefix + ".slot"));
        if (id == null || !id.matches("[0-9]{1,2}")) return null;
        int slot = Integer.parseInt(id);
        return slot >= 0 && slot < 20 ? new File(root, "slot-" + slot + ".enabled") : null;
    }
    private static String readSmall(File file) {
        if (!file.isFile() || file.length() > 8L) return null;
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[8];
            int size = input.read(bytes);
            return size > 0 ? new String(bytes, 0, size, "UTF-8").trim() : null;
        } catch (IOException failure) { return null; }
    }
    private static boolean flag(File file, boolean defaultValue) {
        if (file == null) return defaultValue;
        String text = readSmall(file);
        return text == null ? defaultValue : !"0".equals(text);
    }
    public synchronized boolean shouldSuppress() {
        long now = System.nanoTime();
        if (now >= nextReadNs) {
            hidden = flag(globalEnabled, true) && flag(slotEnabled, true) && flag(state, false);
            nextReadNs = now + 100_000_000L;
        }
        return hidden;
    }
    public File getSlotEnabledFile() { return slotEnabled; }
}
