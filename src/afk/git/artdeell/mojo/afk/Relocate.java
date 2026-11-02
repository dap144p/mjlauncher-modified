import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/** Keeps the engine's ASM private, avoiding conflicts with Fabric/Forge ASM. */
public class Relocate {
    public static void main(String[] args) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Premain-Class", "git.artdeell.mojo.afk.Agent");
        Set<String> written = new HashSet<String>();
        Remapper mapper = new Remapper() {
            public String map(String name) {
                return name.startsWith("org/objectweb/asm/") ?
                    name.replace("org/objectweb/asm/", "git/artdeell/mojo/afk/internal/asm/") : name;
            }
        };
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(args[0]), manifest)) {
            for (int i = 1; i < args.length; i++) {
                try (JarFile jar = new JarFile(args[i])) {
                    for (JarEntry entry : Collections.list(jar.entries())) {
                        if (!entry.getName().endsWith(".class") || entry.getName().equals("module-info.class")) continue;
                        byte[] input;
                        try (InputStream in = jar.getInputStream(entry); ByteArrayOutputStream data = new ByteArrayOutputStream()) {
                            byte[] buffer = new byte[8192]; int n;
                            while ((n = in.read(buffer)) != -1) data.write(buffer,0,n);
                            input = data.toByteArray();
                        }
                        ClassReader reader = new ClassReader(input);
                        String name = mapper.map(reader.getClassName()) + ".class";
                        if (!written.add(name)) continue;
                        ClassWriter writer = new ClassWriter(0);
                        reader.accept(new ClassRemapper(writer, mapper), 0);
                        out.putNextEntry(new JarEntry(name));out.write(writer.toByteArray());out.closeEntry();
                    }
                }
            }
        }
        System.out.println("Relocated AFK engine: " + written.size() + " classes");
    }
}