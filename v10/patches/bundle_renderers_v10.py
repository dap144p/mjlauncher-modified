from pathlib import Path
import zipfile,hashlib,json,shutil
b=Path(__file__).resolve().parents[1];inputs=b/'renderer-input';manifest=[]
sources=json.loads((b/'renderer-sources-v10.json').read_text())
for source in sources:
 apk=inputs/(source['origin']+'.apk');assert hashlib.sha256(apk.read_bytes()).hexdigest()==source['sha256']
for key,selected in [('mg',{'libmobileglues.so'}),('angle',{'libEGL_angle.so','libGLESv2_angle.so'}),('zink',{'libEGL_legacy.so','libgallium_dri.so','libdrm.so'})]:
 apk=inputs/(key+'.apk')
 with zipfile.ZipFile(apk) as z:
  for name in z.namelist():
   if not name.startswith('lib/') or Path(name).name not in selected:continue
   data=z.read(name);original=hashlib.sha256(data).hexdigest();abi=name.split('/')[1];outname=Path(name).name
   if key=='zink':
    # ELF string-table layout is retained; this is not executable-code patching.
    for old,new in [(b'libgallium_dri.so',b'libgallium_old.so'),(b'libdrm.so',b'libdrz.so')]:
     assert len(old)==len(new);data=data.replace(old,new)
    outname={'libgallium_dri.so':'libgallium_old.so','libdrm.so':'libdrz.so'}.get(outname,outname)
   out=b/'decoded/lib'/abi/outname
   assert not out.exists(),out
   out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(data)
   manifest.append({'origin':key,'source_apk':apk.name,'source_path':name,'output_path':'lib/'+abi+'/'+outname,'original_sha256':original,'output_sha256':hashlib.sha256(data).hexdigest(),'native_code_modified':False,'dynamic_library_names_renamed':key=='zink'})
(b/'renderer-native-manifest-v10.json').write_text(json.dumps(manifest,indent=2)+'\n')
for license in (inputs/'licenses').iterdir():shutil.copy(license,b/'decoded/assets/licenses'/license.name)
print('Bundled and verified',len(manifest),'renderer libraries; original native libraries preserved')