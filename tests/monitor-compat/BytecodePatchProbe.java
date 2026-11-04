package monitor.test;

import git.artdeell.mojo.afk.EngineTransformer;
import java.nio.file.Files;
import java.util.jar.JarFile;
import java.io.InputStream;

/** Validate the exact obfuscated classes without publishing Mojang client bytecode. */
public final class BytecodePatchProbe {
    private static void check(String jarName,String className) throws Exception {
        try(JarFile jar=new JarFile(jarName)){
            java.util.jar.JarEntry entry=jar.getJarEntry(className+".class");
            if(entry==null)throw new AssertionError("Missing class "+className);
            byte[] original;
            try(InputStream in=jar.getInputStream(entry)){
                original=readFully(in);
            }
            byte[] updated=new EngineTransformer().transform(null,className,null,null,original);
            if(updated==null || updated.length<original.length)
                throw new AssertionError("No expected JVM agent transformation for "+className);
            String utf=new String(updated,"ISO-8859-1");
            if(!utf.contains("recoverMonitor"))throw new AssertionError("No recoverMonitor call in "+className);
            System.out.println("PASS: Vanilla "+className+" patched bytecode, original="+original.length+" patched="+updated.length);
        }
    }
    private static byte[] readFully(InputStream input)throws Exception{
        java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
        byte[] buf=new byte[8192];int n;
        while((n=input.read(buf))!=-1)output.write(buf,0,n);
        return output.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        check(args[0],"cfr");
        check(args[1],"cty");
    }
}
