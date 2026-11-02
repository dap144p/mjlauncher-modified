#!/usr/bin/env python3
"""Rebuild a v1.11.1 preview APK from v1.11: render lifecycle and OpenAL hotfix.

Requires APKTool 2.11.1, JDK17, Disassemble.java, agent jar. No signing secret.
This intentionally changes GameActivity callbacks only; v1.13-1.14 GLFW fix is NOT included.
"""
from pathlib import Path
import argparse, subprocess, zipfile, shutil

def run(*args):
    subprocess.run([str(a) for a in args], check=True)

def patch_game_activity(path):
    source=path.read_text()
    for method, before, after in (
        ('.method protected onPause()V',
         '    invoke-super {p0}, Lgit/artdeell/mojo/BaseActivity;->onPause()V',
         '    invoke-static {}, Lgit/artdeell/mojo/multisession/SessionNativeAfk;->hidden()V\n\n'
         '    invoke-super {p0}, Lgit/artdeell/mojo/BaseActivity;->onPause()V'),
        ('.method public onResume()V',
         '    invoke-super {p0}, Lgit/artdeell/mojo/BaseActivity;->onResume()V',
         '    invoke-super {p0}, Lgit/artdeell/mojo/BaseActivity;->onResume()V\n\n'
         '    invoke-static {}, Lgit/artdeell/mojo/multisession/SessionNativeAfk;->shown()V')):
        start=source.index(method)
        end=source.index('.end method', start)
        body=source[start:end]
        if body.count(before)!=1 or 'SessionNativeAfk;->hidden()V' in body or 'SessionNativeAfk;->shown()V' in body:
            raise ValueError('Unexpected onPause/onResume smali layout: '+method)
        source=source[:start]+body.replace(before,after,1)+source[end:]
    path.write_text(source)

def patch_manifest(path):
    raw=path.read_bytes()
    previous='justicia-h1-v1.11-modified'.encode('utf-16le')
    version='justicia-h1-v1.11.1-hotfix'.encode('utf-16le')
    if len(previous)!=len(version) or raw.count(previous)!=1: raise ValueError('Unexpected manifest versionName')
    raw=raw.replace(previous,version)
    old=(553).to_bytes(4,'little')
    if raw.count(old)!=1: raise ValueError('Unexpected manifest versionCode')
    raw=raw.replace(old,(554).to_bytes(4,'little'))
    path.write_bytes(raw)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--base-apk',type=Path,required=True)
    parser.add_argument('--apktool',type=Path,required=True)
    parser.add_argument('--agent-jar',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--work',type=Path,default=Path('build-v1111'))
    opts=parser.parse_args()
    root=Path(__file__).resolve().parents[1]
    work=opts.work.resolve()
    if work.exists(): shutil.rmtree(work)
    work.mkdir(parents=True)
    decode=work/'decoded'
    run('java','-jar',opts.apktool.resolve(),'d','-f','-s','-r','-o',decode,opts.base_apk.resolve())
    disassembler=work/'disassembler'; disassembler.mkdir()
    run('javac','-cp',opts.apktool.resolve(),'-d',disassembler,root/'patches/Disassemble.java')
    with zipfile.ZipFile(opts.base_apk) as z:
        dex=work/'classes.dex';dex.write_bytes(z.read('classes.dex'))
    smali=decode/'smali';smali.mkdir(exist_ok=True)
    sep=';' if __import__('os').name=='nt' else ':'
    run('java','-Xmx1500m','-cp',str(disassembler)+sep+str(opts.apktool.resolve()),
        'Disassemble',dex,smali)
    (decode/'classes.dex').unlink()
    patch_game_activity(smali/'git/artdeell/mojo/game/GameActivity.smali')
    patch_manifest(decode/'AndroidManifest.xml')
    engine=decode/'assets/native-afk/afk-engine-v9.jar'
    if not engine.exists(): raise ValueError('v1.11 AFK engine asset not found')
    shutil.copy2(opts.agent_jar,engine)
    opts.output.parent.mkdir(parents=True,exist_ok=True)
    run('java','-Xmx1500m','-jar',opts.apktool.resolve(),'b',decode,'-o',opts.output.resolve())
    print('Built UNSIGNED v1.11.1 hotfix:',opts.output)

if __name__=='__main__':main()