from pathlib import Path
import subprocess,zipfile
b=Path(__file__).resolve().parents[1];base=b.parent/'input/mj-launcher-justicia-h1-multisession-afk-9.modified.apk';out=b/'decoded';helper=b/'dex-tools';helper.mkdir(exist_ok=True)
subprocess.run(['java','-jar',str(b/'tools/ecj.jar'),'-17','-nowarn','-cp',str(b/'tools/apktool.jar'),'-d',str(helper),str(b/'source/Disassemble.java')],check=True)
subprocess.run(['java','-jar',str(b/'tools/apktool.jar'),'d','-s','-f',str(base),'-o',str(out)],check=True)
with zipfile.ZipFile(base) as z:
 for name,folder in [('classes.dex','smali'),('classes2.dex','smali_classes2')]:
  raw=b/('baseline-v9-'+name);raw.write_bytes(z.read(name))
  subprocess.run(['java','-cp',str(helper)+':'+str(b/'tools/apktool.jar'),'Disassemble',str(raw),str(out/folder)],check=True)
  raw.unlink();(out/name).unlink(missing_ok=True)