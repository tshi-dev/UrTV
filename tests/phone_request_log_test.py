#!/usr/bin/env python3
"""X, 19 Nov («لا يمكن الوصول» on a QR): the TV logs the path and the browser family of each phone-page request, never an address, a query, a token or the whole user agent."""
import shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PH = ROOT / "android/launcher/src/main/java/sa/urtv/home/phone"
if not shutil.which("javac"):
    print("SKIP request log: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(PH / "RequestLog.java"), str(ROOT / "tests/RequestLogRun.java")], check=True)
    got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.phone.RequestLogRun"], check=True, capture_output=True).stdout.decode().splitlines())
want = {"android chrome": "android-chrome", "iphone": "iphone-safari", "none": "none none", "desktop": "firefox", "path": "/api/state", "token": "/s/*/photo",
        "root": "/ -", "line": "request GET path=/y ua=android-chrome", "no ua text": "true"}
failed = 0
for k, v in want.items():
    ok = got.get(k) == v
    print(("PASS" if ok else "FAIL") + f" request log: {k}" + ("" if ok else f"  -> {got.get(k)!r}, wanted {v!r}"))
    failed += not ok
srv = (PH / "PhoneServer.java").read_text(encoding="utf-8"); host = (PH / "PhoneHost.java").read_text(encoding="utf-8")
ok = "RequestLog.line(req.method, req.path, req.header(\"user-agent\"))" in srv and "PhoneServer.LOG = line -> android.util.Log.i(\"UrTVPhone\", line)" in host and "android.util" not in srv
print(("PASS" if ok else "FAIL") + " request log: every request is logged through PhoneServer.LOG (set by PhoneHost); the server stays free of Android")
failed += not ok
sys.exit(1 if failed else 0)
