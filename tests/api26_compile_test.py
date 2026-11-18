#!/usr/bin/env python3
"""4.3.6 (a Percee OTT box on Android 8.0, the home's minSdk): tests/api28_compile_test.py keeps APIs newer than Android 9 behind a version check,
which leaves Android 8.0 and 8.1's own gaps (API 27 and 28) unchecked. This compiles the home against Android 8.0's own classes (Robolectric's
android-all 8.0.0_r4, its sha1 pinned, in the same cache): every symbol Android 8.0 lacks must sit behind a Build.VERSION check in the 10 lines
above it, and a check with a number must ask for 27 or newer (a «>= 26» guard would let Android 8.0 through).
SKIP (exit 0) when javac is missing or the jar can't be fetched (offline). Run: python3 tests/api26_compile_test.py"""
import hashlib, os, re, shutil, subprocess, sys, tempfile, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/launcher/src/main/java"
VER = "8.0.0_r4-robolectric-r1"
SHA1 = "30f97bebb50be3a1af9e12cb999360e1cdd2aaa5"
URL = f"https://maven-central.storage-download.googleapis.com/maven2/org/robolectric/android-all/{VER}/android-all-{VER}.jar"
CACHE = Path(os.environ.get("URTV_CACHE") or Path(os.environ.get("XDG_CACHE_HOME") or Path.home() / ".cache") / "urtv")
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def sha1(p):
    h = hashlib.sha1()
    with open(p, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""): h.update(b)
    return h.hexdigest()

if not shutil.which("javac"):
    print("SKIP api 26: no javac"); sys.exit(0)
jar = CACHE / f"android-all-{VER}.jar"
if not jar.is_file() or sha1(jar) != SHA1:
    try:
        CACHE.mkdir(parents=True, exist_ok=True)
        tmp = CACHE / (jar.name + ".part")
        urllib.request.urlretrieve(URL, tmp)
        if sha1(tmp) != SHA1:
            tmp.unlink(); print("FAIL api 26: the downloaded jar does not match the pinned sha1 (not used)"); sys.exit(1)
        tmp.replace(jar)
    except Exception as e:
        print(f"SKIP api 26: android-all {VER} is not cached and could not be fetched ({type(e).__name__})"); sys.exit(0)

with tempfile.TemporaryDirectory() as w:
    w = Path(w)
    # the stub R and BuildConfig, as tests/android_compile.sh makes them
    names = {}
    for f in SRC.rglob("*.java"):
        for t, n in re.findall(r"\bR\.(\w+)\.(\w+)", f.read_text(encoding="utf-8")):
            names.setdefault(t, set()).add(n)
    stub = w / "stub/sa/urtv/home"
    stub.mkdir(parents=True)
    r = ["package sa.urtv.home;\npublic final class R {"]
    for t, ns in sorted(names.items()):
        r.append(f"  public static final class {t} {{")
        r += [f"    public static final int {n} = {0x7f000000 + i};" for i, n in enumerate(sorted(ns))]
        r.append("  }")
    (stub / "R.java").write_text("\n".join(r + ["}"]))
    (stub / "BuildConfig.java").write_text('package sa.urtv.home;\npublic final class BuildConfig { public static final String VERSION_NAME="x"; public static final String MARKETING="4.0"; public static final int VERSION_CODE=1; public static final boolean DEBUG=false; public static final String BRANCH="master"; public static final String PIN_RECOVERY=""; public static final String APPLICATION_ID="sa.urtv.home"; }')
    out = w / "out"; out.mkdir()
    files = [str(p) for p in SRC.rglob("*.java")] + [str(stub / "R.java"), str(stub / "BuildConfig.java")]
    res = subprocess.run(["javac", "-encoding", "UTF-8", "-proc:none", "-Xmaxerrs", "5000", "-cp", str(jar), "-d", str(out)] + files, capture_output=True, text=True)
errors = re.findall(r"^(\S+\.java):(\d+): error: (.*)$", res.stderr, re.M)
other = [f"{Path(f).name}:{l}: {m}" for f, l, m in errors if m != "cannot find symbol"]
check("against Android 8.0 the home has no error but a symbol Android 8.0 lacks", not other, other[:5])
unguarded = []
for f, l, m in errors:
    lines = Path(f).read_text(encoding="utf-8").split("\n")
    i = int(l) - 1
    near = [lines[k] for k in range(max(0, i - 10), i + 1) if "SDK_INT" in lines[k]]
    if not near:
        unguarded.append(f"{Path(f).relative_to(SRC)}:{l}")
        continue
    m = re.findall(r"SDK_INT\s*(>=|<|>)\s*(\d+)", near[-1])
    if m and any((op == ">" and int(n) < 26) or (op != ">" and int(n) < 27) for op, n in m):
        unguarded.append(f"{Path(f).relative_to(SRC)}:{l} (guard: {near[-1].strip()[:80]})")
check(f"every API newer than Android 8.0 ({len(errors)} uses) is behind a Build.VERSION check for 27 or newer just above it", not unguarded, unguarded)

print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
