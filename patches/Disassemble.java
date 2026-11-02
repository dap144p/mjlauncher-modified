import java.io.File;
import java.nio.file.Files;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.baksmali.Baksmali;
import com.android.tools.smali.baksmali.BaksmaliOptions;
public class Disassemble {public static void main(String[] args)throws Exception {byte[] bytes=Files.readAllBytes(new File(args[0]).toPath());DexBackedDexFile dex=new DexBackedDexFile(null,bytes,0);BaksmaliOptions options=new BaksmaliOptions();options.localsDirective=true;options.debugInfo=true;Baksmali.disassembleDexFile(dex,new File(args[1]),4,options);System.out.println("Disassembled "+bytes.length+" bytes, "+dex.classCount+" classes");}}