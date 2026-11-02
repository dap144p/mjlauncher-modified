#!/usr/bin/env python3
"""Reproducible v1.11 preview overlay on signed v10 APK (outputs UNSIGNED APK).
Requires Apktool 2.11.1, JDK 17, source Disassemble.java (v10 backup),
and an AFK engine JAR built from this branch's Java-agent sources.
The private signing key is NOT used by this script.
"""
from pathlib import Path
import argparse, subprocess, zipfile, shutil

def run(*cmd):
    subprocess.run([str(x) for x in cmd], check=True)

def replace_once(path, old, new):
    text=path.read_text()
    if text.count(old)!=1:raise RuntimeError(f'{path.name}: expected 1 anchor, found {text.count(old)}')
    path.write_text(text.replace(old,new,1))

def patch_manifest(path):
    raw=bytearray(path.read_bytes());original='justicia-h1-multisession-afk-10-modified';updated='justicia-h1-v1.11-modified'
    data=original.encode('utf-16le');where=raw.find(data)
    if where<2 or raw.count(data)!=1:raise RuntimeError('v10 versionName not present exactly once')
    if int.from_bytes(raw[where-2:where],'little')!=len(original):raise RuntimeError('unexpected AXML length encoding')
    raw[where-2:where]=len(updated).to_bytes(2,'little')
    raw[where:where+len(updated)*2]=updated.encode('utf-16le')
    raw[where+len(updated)*2:where+len(original)*2]=b'\x00'*(len(original)-len(updated))*2
    value=(552).to_bytes(4,'little')
    if raw.count(value)!=1:raise RuntimeError('versionCode 552 not unique in AXML')
    raw=raw.replace(value,(553).to_bytes(4,'little'))
    path.write_bytes(raw)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--base-v10',required=True,type=Path)
    ap.add_argument('--apktool',required=True,type=Path)
    ap.add_argument('--disassemble-java',required=True,type=Path)
    ap.add_argument('--agent-jar',required=True,type=Path)
    ap.add_argument('--output',required=True,type=Path)
    ap.add_argument('--work',default=Path('build-v111'),type=Path)
    args=ap.parse_args();work=args.work.resolve();root=Path(__file__).resolve().parents[1]
    if work.exists():shutil.rmtree(work)
    work.mkdir(parents=True)
    decoded=work/'decoded'
    # Apktool zip-based DEX extractor has an out-of-bounds issue with v10's dex.
    # Decode resources/assets in raw mode and disassemble classes2 via bytes.
    run('java','-jar',args.apktool,'d','-f','-s','-r','-o',decoded,args.base_v10)
    disassemble=work/'disassembler';disassemble.mkdir()
    run('javac','-cp',args.apktool,'-d',disassemble,args.disassemble_java)
    with zipfile.ZipFile(args.base_v10) as archive:
        raw_dex=work/'classes2.dex';raw_dex.write_bytes(archive.read('classes2.dex'))
    smali=decoded/'smali_classes2';smali.mkdir(exist_ok=True)
    run('java','-cp',str(disassemble)+':'+str(args.apktool),'Disassemble',raw_dex,smali)
    (decoded/'classes2.dex').unlink()
    dest=smali/'git/artdeell/mojo/multisession'
    for source in (root/'smali/git/artdeell/mojo/multisession').glob('SessionV111*.smali'):
        shutil.copyfile(source,dest/source.name)
    router=dest/'SessionRouterActivity.smali'
    replace_once(router,
        'invoke-direct {p0, p1}, Lgit/artdeell/mojo/multisession/SessionRouterActivity;->editRam(I)V',
        'invoke-static {p0, p1}, Lgit/artdeell/mojo/multisession/SessionV111;->show(Lgit/artdeell/mojo/multisession/SessionRouterActivity;I)V')
    anchor='.method synthetic lambda$0$git-artdeell-mojo-multisession-SessionRouterActivity(Landroid/view/View;)V'
    replace_once(router,anchor,'''.method public configureSlotRam(I)V
    .locals 0
    invoke-direct {p0, p1}, Lgit/artdeell/mojo/multisession/SessionRouterActivity;->editRam(I)V
    return-void
.end method

'''+anchor)
    controller=dest/'SessionNativeAfk.smali'
    anchor='    .line 41\n    new-instance v1, Ljava/io/File;'
    replace_once(controller,anchor,
        '    invoke-static {p0}, Lgit/artdeell/mojo/multisession/SessionV111;->markCurrent(Landroid/content/Context;)V\n\n'+anchor)
    patch_manifest(decoded/'AndroidManifest.xml')
    destjar=decoded/'assets/native-afk/afk-engine-v9.jar'
    if not destjar.is_file():raise RuntimeError('v10 AFK engine asset missing')
    shutil.copyfile(args.agent_jar,destjar)
    run('java','-jar',args.apktool,'b',decoded,'-o',args.output)
    print('Created UNSIGNED v1.11 preview:',args.output)

if __name__=='__main__':main()