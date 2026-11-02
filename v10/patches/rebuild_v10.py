from pathlib import Path
import subprocess,shutil
b=Path(__file__).resolve().parents[1]
def run(*args):subprocess.run(list(map(str,args)),check=True)
run('python3',b/'patches/decode_v9_base.py')
for script in ['bundle_renderers_v10.py','patch_v10.py','compile_renderer_catalog.py']:run('python3',b/'patches'/script)
for raw in (b/'decoded').glob('classes*.dex'):raw.unlink()
run('java','-jar',b/'tools/apktool.jar','b','-f',b/'decoded','-o',b/'unsigned-v10.apk')
# sign_verify_v10 retains a simple sibling copy of v9 for byte comparisons.
shutil.copy(b.parent/'input/mj-launcher-justicia-h1-multisession-afk-9.modified.apk',b/'mj-launcher-justicia-h1-multisession-afk-9.modified.apk')
run('python3',b/'patches/sign_verify_v10.py')