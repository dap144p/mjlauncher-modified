package git.artdeell.mojo.multisession;

import android.content.Context;
import android.os.Process;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Launcher-side state. No Minecraft mod or Fabric dependency. */
public final class SessionAfkConfigV111 {
    private SessionAfkConfigV111() {}
    private static File folder(Context context) {
        File root=new File(context.getFilesDir(),"native-afk");
        if(!root.isDirectory() && !root.mkdirs()) throw new IllegalStateException("Cannot create AFK directory");
        return root;
    }
    private static void atomic(File target,String content) throws IOException {
        File temporary=new File(target.getPath()+"."+Process.myPid()+".tmp");
        try(FileOutputStream out=new FileOutputStream(temporary)){
            out.write(content.getBytes("UTF-8"));
            out.getFD().sync();
        }
        if(!temporary.renameTo(target)){
            temporary.delete();
            throw new IOException("Could not atomically replace "+target.getName());
        }
    }
    private static void check(int slot) {
        if(slot<0 || slot>=20) throw new IllegalArgumentException("slot must be 0..19");
    }
    public static void recordCurrentProcessSlot(Context context,int slot) throws IOException {
        check(slot);
        atomic(new File(folder(context),"process-"+Process.myPid()+".slot"),Integer.toString(slot));
    }
    public static void setSessionEnabled(Context context,int slot,boolean enabled) throws IOException {
        check(slot);
        atomic(new File(folder(context),"slot-"+slot+".enabled"),enabled?"1":"0");
    }
    public static boolean isSessionEnabled(Context context,int slot) throws IOException {
        check(slot);
        File file=new File(folder(context),"slot-"+slot+".enabled");
        if(!file.exists()) return true; // migration from v10
        try(java.io.FileInputStream in=new java.io.FileInputStream(file)) {
            return in.read() != '0';
        }
    }
}
