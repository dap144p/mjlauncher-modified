from pathlib import Path
import subprocess,shutil
b=Path(__file__).resolve().parents[1];w=b/'native-controller-build';stubs=w/'stubs';classes=w/'classes';dex=w/'dex'
files={
'android/content/Context.java':'package android.content;import java.io.*;import android.content.res.AssetManager;public class Context {public File getFilesDir(){return null;}public AssetManager getAssets(){return null;}}',
'android/content/SharedPreferences.java':'package android.content;public interface SharedPreferences {boolean getBoolean(String key,boolean defaultValue);}',
'android/content/res/AssetManager.java':'package android.content.res;import java.io.*;public class AssetManager {public InputStream open(String path)throws IOException {return null;}}',
'android/app/Activity.java':'package android.app;public class Activity extends android.content.Context {}',
'android/os/Process.java':'package android.os;public class Process {public static int myPid(){return 123;}}',
'android/util/Log.java':'package android.util;public class Log {public static int w(String tag,String message,Throwable error){return 0;}}',
'git/artdeell/mojo/prefs/LauncherPreferences.java':'package git.artdeell.mojo.prefs;public class LauncherPreferences {public static android.content.SharedPreferences DEFAULT_PREF;}',
}
for name,content in files.items():
 p=stubs/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
classes.mkdir(parents=True,exist_ok=True);dex.mkdir(parents=True,exist_ok=True)
subprocess.run(['java','-jar',str(b/'tools/ecj.jar'),'-8','-nowarn','-d',str(classes),*[str(p)for p in stubs.rglob('*.java')],str(b/'source/SessionNativeAfk.java')],check=True)
inputs=list((classes/'git/artdeell/mojo/multisession').glob('SessionNativeAfk*.class'))
subprocess.run(['java','-cp',str(b/'tools/sdk/android-16/lib/d8.jar'),'com.android.tools.r8.D8','--min-api','21','--lib','/usr/lib/jvm/java-17-openjdk-amd64','--classpath',str(classes),'--output',str(dex),*[str(p)for p in inputs]],check=True)
smali=b/'helper-native-smali'
subprocess.run(['java','-cp',str(b/'dex-tools')+':'+str(b/'tools/apktool.jar'),'Disassemble',str(dex/'classes.dex'),str(smali)],check=True)
target=b/'decoded/smali_classes2/git/artdeell/mojo/multisession'
for p in smali.glob('git/artdeell/mojo/multisession/SessionNativeAfk*.smali'):shutil.copy(p,target/p.name)
print('Compiled Android AFK controller; substitutes excluded from APK')