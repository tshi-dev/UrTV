#!/usr/bin/env python3
"""4.3.5 (an LG-branded CVTE Android 9 set, black screen): the home's minSdk is 26 and lint does not stop the build, so nothing kept a call to an API
newer than the TV out of the code. This compiles the home against Android 9's own classes (Robolectric's android-all for API 28, its sha1
pinned, kept in the same cache as tests/android_compile.sh): every symbol Android 9 lacks must sit behind a Build.VERSION check in the 10 lines
above it (SDK_INT), so a TV on 8 or 9 never reaches it. Also: the setup report's «WebView:» line (package and version) on a JVM.
SKIP (exit 0) when javac is missing or the jar can't be fetched (offline). Run: python3 tests/api28_compile_test.py"""
import hashlib, os, re, shutil, subprocess, sys, tempfile, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/launcher/src/main/java"
VER = "9-robolectric-4913185-2"
SHA1 = "92c6ca3712d7a06f76f70655ec81162ff372fb19"
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
    print("SKIP api 28: no javac"); sys.exit(0)
jar = CACHE / f"android-all-{VER}.jar"
if not jar.is_file() or sha1(jar) != SHA1:
    try:
        CACHE.mkdir(parents=True, exist_ok=True)
        tmp = CACHE / (jar.name + ".part")
        urllib.request.urlretrieve(URL, tmp)
        if sha1(tmp) != SHA1:
            tmp.unlink(); print("FAIL api 28: the downloaded jar does not match the pinned sha1 (not used)"); sys.exit(1)
        tmp.replace(jar)
    except Exception as e:
        print(f"SKIP api 28: android-all {VER} is not cached and could not be fetched ({type(e).__name__})"); sys.exit(0)

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
check("against Android 9 the home has no error but a symbol Android 9 lacks", not other, other[:5])
unguarded = []
for f, l, m in errors:
    lines = Path(f).read_text(encoding="utf-8").split("\n")
    i = int(l) - 1
    if not any("SDK_INT" in lines[k] for k in range(max(0, i - 10), i + 1)):
        unguarded.append(f"{Path(f).relative_to(SRC)}:{l}")
check(f"every API newer than Android 9 ({len(errors)} uses) is behind a Build.VERSION check just above it", not unguarded, unguarded)

# the setup report's WebView line: its Java pattern, read from the source, on the answers TVs give
eng = (SRC / "sa/urtv/home/setup/SetupEngine.java").read_text(encoding="utf-8")
body = eng.split("static String webviewLine(String dump) {")[1].split("\n    }\n")[0]
pat = re.search(r'Pattern\.compile\("(.*?)"\)\.matcher\(dump\)', body).group(1).encode().decode("unicode_escape")
def line(dump):
    if not dump.strip(): return "?"
    m = re.search(pat, dump)
    if m: return m.group(1) + " " + m.group(2)
    return "none" if "Current WebView package: null" in dump or "no webview" in dump.lower() else "?"
cases = [("Current WebView package (name, version): (com.android.webview, 66.0.3359.158)\nMinimum WebView version code: 3283", "com.android.webview 66.0.3359.158"),
         ("Current WebView package (name, version): (com.google.android.webview, 120.0.6099.230)", "com.google.android.webview 120.0.6099.230"),
         ("", "?"), ("Current WebView package: null", "none"), ("something else", "?")]
got = [line(d) for d, _ in cases]
check("setup report: «WebView:» is the package and version, «none» for a TV without one, «?» when it does not say",
      got == [w for _, w in cases] and 'return dump.contains("Current WebView package: null")' in body, got)
rep = (SRC / "sa/urtv/home/setup/SetupReport.java").read_text(encoding="utf-8")
act = (SRC / "sa/urtv/home/ReportActivity.java").read_text(encoding="utf-8")
check("the setup report writes it", 'out.add("WebView: " + SetupEngine.webviewLine(tv.text("dumpsys webviewupdate")));' in rep)
check("the report screen (and the crash note through it) names the WebView package and build, «none» without one",
      'return p.packageName + "-" + (Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : (long) p.versionCode);' in act and 'if (p == null) return "none";' in act)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
