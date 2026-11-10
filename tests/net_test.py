#!/usr/bin/env python3
"""Net: the small GET the apps use for their own data files (IPv4 first, 4 s per address, a deadline, a size cap, no redirect), on a plain JVM against a scripted server;
and the pins that keep the user-visible fetches on it (the leagues list, the quiz bank, the next match) and the two copies of the file the same."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
LAUNCHER = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def text(p): return p.read_text(encoding="utf-8")

check("the launcher's Net.java is the app's, unchanged but for the package line", text(APP / "Net.java").split("\n", 1)[1] == text(LAUNCHER / "Net.java").split("\n", 1)[1])
st, qb, nm = text(APP / "SettingsActivity.java"), text(APP / "QuizBank.java"), text(APP / "NextMatch.java")
check("the leagues list, the quiz bank and the next match are fetched through Net (no HttpURLConnection of their own)",
      "Net.get(CatalogueLeagues.URL" in st and "Net.get(URL, null" in qb and "Net.get(url, null" in nm and "HttpURLConnection" not in qb + nm and "con = (HttpURLConnection) new URL(CatalogueLeagues.URL)" not in st)
check("Net tries IPv4 first, 4 s per address, with a deadline for the whole call; follows no redirect",
      "static final int CONNECT_MS = 4000;" in text(APP / "Net.java") and "if (a instanceof Inet4Address)" in text(APP / "Net.java") and "Math.min(CONNECT_MS, left)" in text(APP / "Net.java") and "followRedirects" not in text(APP / "Net.java").lower())
check("https: the platform's TLS over the connected socket, the certificate checked against the host name", "setEndpointIdentificationAlgorithm(\"HTTPS\")" in text(APP / "Net.java") and "getDefaultHostnameVerifier().verify(host, ssl.getSession())" in text(APP / "Net.java"))
if not shutil.which("javac"):
    print("SKIP net: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-d", tmp, str(APP / "Net.java"), str(ROOT / "tests/NetTest.java"), str(ROOT / "tests/NetOnline.java")], capture_output=True, text=True)
    check("Net compiles on a plain JVM", c.returncode == 0, c.stderr[-500:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.NetTest"], capture_output=True, text=True, timeout=120)
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line)
                if line.startswith("FAIL"): failed += 1
        check("the JVM test ran to the end", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-500:])
        # the real TLS path, where there is a way out (CI): exit 2 = no way out from here, skipped, not a pass
        o = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.NetOnline"], capture_output=True, text=True, timeout=60)
        for line in o.stdout.splitlines():
            if line.startswith(("PASS", "FAIL", "SKIP")): print(line)
        if o.returncode == 1: failed += 1
sys.exit(1 if failed else 0)
