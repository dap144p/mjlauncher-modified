"""Sign modified v10-based v1.11 preview using the original v6/v10 private signing key.
Passwords stay in private files and are never printed. APK v1 + v2, suitable for Android 10.
"""
from pathlib import Path
import argparse
import zipfile,os,struct,subprocess,hashlib
from cryptography.hazmat.primitives.serialization import pkcs12,Encoding,PublicFormat
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.serialization import pkcs7
parser=argparse.ArgumentParser(description="Sign locally with the existing modified v10 key. Never upload the key to GitHub.")
parser.add_argument("--base-v10",type=Path,required=True)
parser.add_argument("--unsigned",type=Path,required=True)
parser.add_argument("--keystore",type=Path,required=True)
parser.add_argument("--password-file",type=Path,required=True)
parser.add_argument("--agent-jar",type=Path,required=True)
parser.add_argument("--output",type=Path,required=True)
args=parser.parse_args()
BASE=args.base_v10
UNSIGNED=args.unsigned
KEY=args.keystore
PASS=args.password_file.read_text().strip()
OUT=args.output
OUT.parent.mkdir(parents=True,exist_ok=True)
CLEAN=OUT.parent/'clean-v111.apk'
V1=OUT.parent/'v1-v111.apk'
def lp(data):return struct.pack('<I',len(data))+data
with zipfile.ZipFile(UNSIGNED) as original,zipfile.ZipFile(CLEAN,'w') as unsigned:
    for item in original.infolist():
        name=item.filename
        if name.startswith('META-INF/') and (name.endswith(('.RSA','.DSA','.EC','.SF')) or name=='META-INF/MANIFEST.MF'):continue
        unsigned.writestr(item,original.read(name))
env=os.environ.copy();env['MJ_KS_PASS']=PASS
subprocess.run(['jarsigner','-keystore',str(KEY),'-storetype','PKCS12','-storepass:env','MJ_KS_PASS','-keypass:env','MJ_KS_PASS','-sigalg','SHA256withRSA','-digestalg','SHA-256','-signedjar',str(V1),str(CLEAN),'mj-modified'],check=True,capture_output=True,text=True,env=env)

def digest_segments(segments):
    chunks=[]
    for segment in segments:
        for start in range(0,len(segment),1048576):
            part=segment[start:start+1048576]
            chunks.append(hashlib.sha256(b'\xa5'+struct.pack('<I',len(part))+part).digest())
    return hashlib.sha256(b'\x5a'+struct.pack('<I',len(chunks))+b''.join(chunks)).digest()
data=V1.read_bytes();eocd=data.rfind(b'PK\x05\x06')
assert eocd>=0 and eocd+22+struct.unpack_from('<H',data,eocd+20)[0]==len(data)
central=struct.unpack_from('<I',data,eocd+16)[0]
assert data[central:central+4]==b'PK\x01\x02'
digest=digest_segments((data[:central],data[central:eocd],data[eocd:]))
key,cert,_=pkcs12.load_key_and_certificates(KEY.read_bytes(),PASS.encode())
certbytes=cert.public_bytes(Encoding.DER)
public=key.public_key().public_bytes(Encoding.DER,PublicFormat.SubjectPublicKeyInfo)
alg=0x0103
signed=lp(lp(struct.pack('<I',alg)+lp(digest)))+lp(lp(certbytes))+lp(b'')
sig=key.sign(signed,padding.PKCS1v15(),hashes.SHA256())
signer=lp(signed)+lp(lp(struct.pack('<I',alg)+lp(sig)))+lp(public)
blockvalue=lp(lp(signer))
pair=struct.pack('<Q',len(blockvalue)+4)+struct.pack('<I',0x7109871a)+blockvalue
size=len(pair)+24
block=struct.pack('<Q',size)+pair+struct.pack('<Q',size)+b'APK Sig Block 42'
new_eocd=bytearray(data[eocd:]);struct.pack_into('<I',new_eocd,16,central+len(block))
OUT.write_bytes(data[:central]+block+data[central:eocd]+new_eocd)
key.public_key().verify(sig,signed,padding.PKCS1v15(),hashes.SHA256())
with zipfile.ZipFile(BASE) as base,zipfile.ZipFile(OUT) as result:
    assert result.testzip() is None
    oldcert=pkcs7.load_der_pkcs7_certificates(base.read('META-INF/MJ-MODIF.RSA'))[0]
    newcert=pkcs7.load_der_pkcs7_certificates(result.read('META-INF/MJ-MODIF.RSA'))[0]
    assert oldcert.fingerprint(hashes.SHA256())==newcert.fingerprint(hashes.SHA256())
    for item in base.namelist():
        if item.startswith(('lib/','assets/')) and not item.endswith('/') and item!='assets/native-afk/afk-engine-v9.jar':
            assert base.read(item)==result.read(item),item
    assert base.read('classes.dex')==result.read('classes.dex')
    assert base.read('classes2.dex')!=result.read('classes2.dex')
    assert result.read('assets/native-afk/afk-engine-v9.jar')==args.agent_jar.read_bytes()
print('PASS: V1/V2 signed, signer matches v10, all native assets unchanged, classes.dex unchanged.')
print('APK:',OUT,'bytes',OUT.stat().st_size,'sha256',hashlib.sha256(OUT.read_bytes()).hexdigest())