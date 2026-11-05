#!/usr/bin/env python3
"""«أرسل تقرير» of the settings (the home's ReportActivity): a few lines and a QR code to docs/share, as optimize/share.sh makes. Every line
passes share.py's own allowlist (ALLOWED_LINE, read from it), nothing personal is ever read (no city, name, address, network or serial), odd
values are cut to letters, digits, dots and dashes, the link is share.py's page with the report after the #, quoted the same way, and it fits
the QR encoder. Needs javac.   Run: python3 tests/settings_report_test.py"""
import importlib.util, re, shutil, subprocess, sys, tempfile, urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

spec = importlib.util.spec_from_file_location("share", ROOT / "optimize/share.py")
share = importlib.util.module_from_spec(spec)
spec.loader.exec_module(share)
src = (HOME / "Report.java").read_text(encoding="utf-8")
act = (HOME / "ReportActivity.java").read_text(encoding="utf-8")
check("its page is share.py's PAGE", f'PAGE = "{share.PAGE}"' in src)
code = re.sub(r"/\*.*?\*/|//[^\n]*", "", act + src, flags=re.S)   # the code, without its comments
check("nothing personal is read: no location, account, network, serial or the screensaver's settings",
      not re.search(r"Prefs|[Cc]ity|getAccounts|WifiManager|SERIAL|getSerial|ANDROID_ID|MacAddress|Settings\.Secure|ContentResolver|getContentResolver", code))
check("the screen shows the lines as they are, and the QR code is the large kind (version 40, level L)",
      'String.join("\\n", lines)' in act and "QrCode.encodeLarge(url)" in act and "Report.MAX_BYTES = 2953" not in src and "MAX_BYTES = 2953;" in src)
check("its words are in one place (Report), Arabic, for a translation later", all(k in src for k in ("TITLE = ", "HOW = ", "WHAT = ", "BACK = ")))
if not shutil.which("javac"):
    print("SKIP report: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "Report.java"), str(ROOT / "tests/SettingsReportRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.SettingsReportRun"], check=True, capture_output=True).stdout.decode("utf-8").split("\n")
lines = out[:4]
url = next(l[4:] for l in out if l.startswith("url "))
check("the lines: what it is, the two apps' versions, Android, WebView", lines == ["UrTV problem report, settings", "Versions: home 4.0.12, screensaver 4.0.12",
      "Device: Android 11, API 30", "WebView: 120.0.6099.230"], lines)
check("every line passes share.py's allowlist (restrict keeps them all)", share.restrict("\n".join(lines) + "\n") == "\n".join(lines) + "\n")
check("the link is share.py's page with the lines after the #, quoted as share.py quotes",
      url == share.PAGE + "#" + urllib.parse.quote("\n".join(lines) + "\n") and len(url.encode()) <= 2953, url)
odd = next(l[4:] for l in out if l.startswith("odd "))
check("odd values are cut to letters, digits, dots and dashes (no new line, no field of another kind), «?» for none",
      odd == "Versions: home 4.0betarm-rf, screensaver ? | Device: Android 11TVme, API 30 | WebView: ?", odd)
check("a report too long for the QR code drops its last lines until it fits", "cut true" in out)
sys.exit(1 if failed else 0)
