package git.artdeell.mojo.multisession;

import android.content.Context;
import android.os.Process;
import android.app.ActivityManager;
import android.content.ComponentName;
import java.io.*;

/** Session labels use the captured username and the configured heap display. */
public final class SessionSlotInfo {
    private static final int MAGIC=0x4d4a5336;
    public interface ProcessState { boolean matches(int pid,int index); }
    public static final class Record {
        public final int pid,heap;
        public final String username;
        public Record(int pid,String username,int heap) {this.pid=pid;this.username=username;this.heap=heap;}
    }
    private static void check(int index) {if(index<0||index>=20)throw new IllegalArgumentException("Invalid slot");}
    private static File path(Context context,int index) {
        check(index);return new File(new File(context.getFilesDir(),"session-info"),index+".bin");
    }
    public static void clear(Context context,int index) {try{path(context,index).delete();}catch(Exception ignored){}}
    public static void publishAttached(Context context) {
        try {
            int index=SessionRuntime.index();
            String username=SessionRuntime.loadAccount(null).username;
            publish(context,index,new Record(Process.myPid(),username,SessionRuntime.activeHeapMb()));
        } catch(Exception ignored) { }
    }
    public static void publish(Context context,int index,Record value) throws IOException {
        File destination=path(context,index),parent=destination.getParentFile();
        if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot save session status");
        File temporary=new File(parent,index+".part");
        try {
            try(FileOutputStream file=new FileOutputStream(temporary);DataOutputStream output=new DataOutputStream(file)) {
                output.writeInt(MAGIC);output.writeInt(value.pid);output.writeUTF(clean(value.username));output.writeInt(value.heap);
                output.flush();try{file.getFD().sync();}catch(IOException ignored){}
            }
            if(!temporary.renameTo(destination))throw new IOException("Cannot publish session status");
        } finally {temporary.delete();}
    }
    public static Record read(Context context,int index) {
        try(DataInputStream input=new DataInputStream(new FileInputStream(path(context,index)))) {
            if(input.readInt()!=MAGIC)return null;
            int pid=input.readInt();String username=input.readUTF();int heap=input.readInt();
            if(pid<=0||input.read()!=-1)return null;
            return new Record(pid,clean(username),heap);
        } catch(Exception ignored) {return null;}
    }
    public static String label(Context context,int index,boolean running,int configuredHeap) {
        return label(context,index,running,configuredHeap,null);
    }
    public static String label(Context context,int index,boolean running,int configuredHeap,ProcessState processes) {
        if(!running)return format(index,false,null,configuredHeap);
        Record active=read(context,index);
        String username=active!=null&&(processes==null||processes.matches(active.pid,index))?clean(active.username):"";
        if(username.isEmpty())username=taskUsername(context,index);
        return format(index,true,username,configuredHeap);
    }
    public static String format(int index,boolean running,String username,int heap) {
        check(index);
        String state=running?clean(username):"Mulai Sesi Baru";
        if(state.isEmpty())state="Memuat Username";
        return "Sesi "+(index+1)+" - "+state+" - "+(heap>0?heap+" MB":"-- MB");
    }
    private static String clean(String username) {
        if(username==null)return "";
        String value=username.replace('\n',' ').replace('\r',' ').replace('\t',' ').trim();
        return value.length()>64?value.substring(0,64):value;
    }
    private static String taskUsername(Context context,int index) {
        try {
            ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            if(manager==null)return "";
            String expectedClass=SessionRuntime.activityClass(index).getName();
            String prefix="MJ Sesi "+(index+1)+" · ";
            for(ActivityManager.AppTask task:manager.getAppTasks()) {
                try {
                    ActivityManager.RecentTaskInfo info=task.getTaskInfo();
                    ComponentName component=info.baseIntent==null?null:info.baseIntent.getComponent();
                    if(component==null||!expectedClass.equals(component.getClassName())||info.taskDescription==null)continue;
                    String title=info.taskDescription.getLabel();
                    if(title!=null&&title.startsWith(prefix))return clean(title.substring(prefix.length()));
                } catch(RuntimeException ignored) { }
            }
        } catch(RuntimeException ignored) { }
        return "";
    }
}