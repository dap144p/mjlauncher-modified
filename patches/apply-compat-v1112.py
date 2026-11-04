#!/usr/bin/env python3
"""Make UNSIGNED v1.11.2 test APK from signed v1.11.1 without altering native libs.
Use the separate private signing tool before installing. Never commit keys/passwords.
"""
import argparse
from pathlib import Path
import zipfile
parser=argparse.ArgumentParser()
parser.add_argument('--base',type=Path,required=True,help='existing v1.11.1 signed APK')
parser.add_argument('--agent-jar',type=Path,required=True,help='built Java AFK agent with GLFW recovery')
parser.add_argument('--out',type=Path,required=True,help='unsigned output APK')
a=parser.parse_args()
old=b'justicia-h1-v1.11.1-hotfix'.decode().encode('utf-16le')
new=b'justicia-h1-v1.11.2-compat'.decode().encode('utf-16le')
assert len(old)==len(new)
a.out.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(a.base) as z, zipfile.ZipFile(a.out,'w') as out:
    for item in z.infolist():
        payload=z.read(item.filename)
        if item.filename=='AndroidManifest.xml':
            if payload.count(old)!=1 or payload.count((554).to_bytes(4,'little'))!=1:
                raise ValueError('Expected version 554 v1.11.1 AndroidManifest.xml')
            payload=payload.replace(old,new).replace((554).to_bytes(4,'little'),(555).to_bytes(4,'little'))
        elif item.filename=='assets/native-afk/afk-engine-v9.jar':
            payload=a.agent_jar.read_bytes()
        out.writestr(item,payload)
print('Built UNSIGNED v1.11.2:',a.out)