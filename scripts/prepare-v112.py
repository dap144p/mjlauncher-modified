#!/usr/bin/env python3
"""Prepare *unsigned* v1.12 from known-good v1.11.2. No secrets, no asset changes."""
import argparse
from pathlib import Path
from zipfile import ZipFile

p = argparse.ArgumentParser()
p.add_argument('--base', type=Path, required=True, help='signed v1.11.2 APK')
p.add_argument('--out', type=Path, required=True, help='unsigned v1.12.0 APK')
a = p.parse_args()
old = 'justicia-h1-v1.11.2-compat'.encode('utf-16le')
new = 'justicia-h1-v1.12.0-stable'.encode('utf-16le')
assert len(old) == len(new)
a.out.parent.mkdir(parents=True, exist_ok=True)

with ZipFile(a.base) as source, ZipFile(a.out, 'w') as dest:
    assert 'AndroidManifest.xml' in source.namelist()
    for member in source.infolist():
        content = source.read(member.filename)
        if member.filename == 'AndroidManifest.xml':
            if content.count(old) != 1 or content.count((555).to_bytes(4,'little')) != 1:
                raise ValueError('Expected exact v1.11.2 AndroidManifest.xml')
            content = content.replace(old, new).replace(
                (555).to_bytes(4,'little'), (556).to_bytes(4,'little'))
        dest.writestr(member, content)

with ZipFile(a.base) as base, ZipFile(a.out) as result:
    changed = [n for n in base.namelist() if base.read(n) != result.read(n)]
    assert changed == ['AndroidManifest.xml'], changed
print('Prepared unsigned MJLauncher v1.12.0:', a.out)
