#!/usr/bin/env python3
"""Print instruction *identifiers* around an obfuscated Minecraft crash line.
Downloads are handled by the CI workflow from Mojang's official CDN. This tool
does not copy game binaries, decompiled source, or bytecode into Git.
"""
import re, subprocess, sys
from pathlib import Path

def investigate(jar: str, classname: str, target_line: int) -> None:
    result=subprocess.run(["javap","-classpath",jar,"-c","-l","-p",classname],capture_output=True,text=True,check=True)
    lines=result.stdout.splitlines()
    found=False
    for i,line in enumerate(lines):
        if not re.match(r"^\s{2,}(?:public|protected|private)\s+"+re.escape(classname)+r"\(",line):
            continue
        found=True
        end=next((j for j in range(i+1,len(lines)) if re.match(r"^\s{2}(?:public|private|protected|static)\b",lines[j])),len(lines))
        section=lines[i:end]
        try: c=next(k for k,x in enumerate(section) if x.strip()=="Code:")
        except StopIteration: continue
        try: l=next(k for k,x in enumerate(section[c+1:],c+1) if x.strip()=="LineNumberTable:")
        except StopIteration: continue
        positions=[]
        for x in section[l+1:]:
            m=re.search(r"\bline\s+(\d+):\s+(\d+)",x)
            if m: positions.append((int(m.group(1)),int(m.group(2))))
            elif positions: break
        if not positions: continue
        starts=[(p,line_no) for line_no,p in positions]
        matches=[p for line_no,p in positions if line_no==target_line]
        if not matches:
            print("Line",target_line,"not in constructor line table; available range:",
                  min(n for n,_ in positions),max(n for n,_ in positions))
            continue
        start=min(matches)
        endpos=min((p for p,_ in starts if p>start),default=10**9)
        print("Class",classname,"constructor line",target_line,
              "BCI segment",start,"..",endpos)
        for x in section[c+1:l]:
            m=re.match(r"^\s*(\d+):\s*(\S+)(.*)$",x)
            if not m: continue
            addr,op,tail=int(m.group(1)),m.group(2),m.group(3)
            if (start-12<=addr<endpos+6):
                if "//" in tail:
                    comment=tail.split("//",1)[1].strip()
                    # Only method or field *identifiers*, not original bytecode.
                    print(" ",addr,op,comment[:180])
                elif op in {"aaload","aconst_null","areturn","dup","invokevirtual","invokestatic","invokespecial","getfield","putfield","ifnull","ifnonnull","athrow"}:
                    print(" ",addr,op)
        return
    if not found: print("No constructor method found for",classname)

if __name__=="__main__":
    investigate(sys.argv[1],sys.argv[2],int(sys.argv[3]))
