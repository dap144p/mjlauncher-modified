#!/usr/bin/env python3
"""Summarize monitor manager bytecode call graph (no client jar or bytecode uploaded)."""
import re,subprocess,sys
jar,klass=sys.argv[1:3]
out=subprocess.check_output(["javap","-p","-c","-classpath",jar,klass],text=True)
methods=out.splitlines()
print("MONITOR-MANAGER",klass)
show=False
for line in methods:
  hdr=re.match(r"^\s{2}(?:public|private|protected)\s+(.+)\($",line)
  if re.match(r"^\s{2}(?:public|private|protected)\s+.+\([^)]*\);",line):
    show=True
    print(" METHOD",line.strip()[:150])
  if show and re.search(r"\b(?:invokevirtual|invokestatic|invokespecial|invokeinterface|getfield|putfield)\b",line) and "//" in line:
    ref=line.split("//",1)[1].strip()
    if any(k.lower() in ref.lower() for k in ["glfw",klass,"java/util", "java/util/function","cfj","cfr","ctu","cty","getPrimaryMonitor","getMonitors"]):
      print("  ",ref[:150])
