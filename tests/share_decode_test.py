"""optimize/share.py reads what the TV says as UTF-8 and replaces a byte it cannot read (R-363).

On Windows the console code page decoded the TV's getprop (cp1252) and one byte it could not read stopped share.py
with a UnicodeDecodeError, so the report had no TV line. A fake tv.py here prints a byte that is not UTF-8.

    python3 tests/share_decode_test.py          (no TV needed)
"""
import os, re, stat, sys, tempfile
here = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(here, "..", "optimize"))
import share

bad = []
def check(name, ok):
    print(("PASS " if ok else "FAIL ") + name)
    if not ok:
        bad.append(name)

fake = os.path.join(tempfile.mkdtemp(), "fake-python")
with open(fake, "w") as f:   # ignores its arguments: a getprop with one byte that is not UTF-8
    f.write("#!/bin/sh\nprintf '[ro.product.model]: [Box\\377]\\n[ro.build.version.sdk]: [28]\\n'\n")
os.chmod(fake, os.stat(fake).st_mode | stat.S_IEXEC)
real, sys.executable = sys.executable, fake
try:
    props = share.live_props()
finally:
    sys.executable = real
check("a byte that is not UTF-8 does not lose the TV's properties", props.get("ro.build.version.sdk") == "28"
      and props.get("ro.product.model", "").startswith("Box"))

src = open(os.path.join(here, "..", "optimize", "share.py"), encoding="utf-8").read()
runs = re.findall(r"subprocess\.run\((?:[^()]|\([^()]*\))*\)", src)
texts = [r for r in runs if "text=True" in r]
check("every text subprocess.run in share.py decodes UTF-8 with errors=replace",
      texts and all('encoding="utf-8"' in r and 'errors="replace"' in r for r in texts))
sys.exit(1 if bad else 0)
