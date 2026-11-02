import git.artdeell.mojo.afk.AfkPolicyV111;
import java.io.File;
import java.nio.file.Files;

public class AfkPolicyV111Test {
    static void put(File file,String text) throws Exception {
        Files.write(file.toPath(), text.getBytes("UTF-8"));
    }
    static void eq(boolean want,boolean got,String label) {
        if (want != got) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        File dir=Files.createTempDirectory("afk-v111").toFile();
        File a=new File(dir,"process-111.state"), b=new File(dir,"process-222.state");
        put(a,"1"); put(b,"1"); put(new File(dir,"enabled"),"1");
        put(new File(dir,"process-111.slot"),"0");
        put(new File(dir,"process-222.slot"),"1");
        put(new File(dir,"slot-0.enabled"),"0");
        put(new File(dir,"slot-1.enabled"),"1");
        AfkPolicyV111 first=new AfkPolicyV111(a),second=new AfkPolicyV111(b);
        eq(false,first.shouldSuppress(),"slot1 OFF");
        eq(true,second.shouldSuppress(),"slot2 ON");
        put(new File(dir,"enabled"),"0"); Thread.sleep(110);
        eq(false,first.shouldSuppress(),"global OFF slot1");
        eq(false,second.shouldSuppress(),"global OFF slot2");
        put(new File(dir,"enabled"),"1");
        put(new File(dir,"slot-0.enabled"),"1");
        put(b,"0"); Thread.sleep(110);
        eq(true,first.shouldSuppress(),"slot1 live ON");
        eq(false,second.shouldSuppress(),"slot2 foreground");
        File c=new File(dir,"process-333.state"); put(c,"1");
        eq(true,new AfkPolicyV111(c).shouldSuppress(),"missing marker maintains v10");
        System.out.println("PASS: v1.11 AFK policy isolation, global master, live update, foreground and fallback");
    }
}
