"""The setup's report in the app (SetupReport.java) and the measurement (SetupMeasure.java): the report is optimize/share.py's allowlist only (its own
restrict() and scrub() leave it as it is), it never carries a city, a team, an address or a serial, its link is the one share.py puts in its QR code
(docs/share/ with the report after the #), and a reading taken without the restart says «بدون إعادة تشغيل». measure.sh and the app read the same
fake TV into the same ram file and summary row. No TV needed.

    python3 tests/setup_report_test.py
"""
import importlib.util, os, re, shutil, subprocess, sys, tempfile, urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

spec = importlib.util.spec_from_file_location("share", ROOT / "optimize/share.py"); share = importlib.util.module_from_spec(spec); spec.loader.exec_module(share)
src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupReport.java").read_text(encoding="utf-8")
java_allowed = re.search(r'ALLOWED_LINE = Pattern.compile\(\s*(.*?)\);', src, re.S).group(1)
java_allowed = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', java_allowed)).encode().decode("unicode_escape")
check("the allowlist is share.py's ALLOWED_LINE, to the letter", java_allowed == share.ALLOWED_LINE.pattern, (java_allowed, share.ALLOWED_LINE.pattern))
check("the page is share.py's PAGE", f'PAGE = "{share.PAGE}"' in src)
docs = (ROOT / "docs/share/index.html").read_text(encoding="utf-8")
check("docs/share/ reads the report from the # (decodeURIComponent of location.hash)", "decodeURIComponent(location.hash.slice(1))" in docs)

if not shutil.which("javac"):
    print("SKIP the report on a JVM: no javac")
    sys.exit(1 if failed else 0)
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
CLS = Path(tempfile.mkdtemp()); OUT = Path(tempfile.mkdtemp())
pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")] + [SETUP.parent / "phone/UpdateRules.java"]
r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(CLS), *map(str, pure), *(str(ROOT / "tests" / f) for f in ("SetupEngineRun.java", "SetupRunnerRun.java", "ReportRun.java"))], capture_output=True, text=True)
check("it compiles", r.returncode == 0, r.stderr)
if r.returncode:
    sys.exit(1)
subprocess.run(["java", "-Dassets=" + str(ROOT / "android/launcher/src/main/assets"), "-Dout=" + str(OUT), "-cp", str(CLS), "sa.urtv.home.setup.SetupRunnerRun"], capture_output=True, text=True, timeout=300)
def jr(*a):
    return subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.ReportRun", *map(str, a)], capture_output=True, text=True, encoding="utf-8").stdout

rep, rep_now = (OUT / "report.md").read_text(encoding="utf-8"), (OUT / "report-now.md").read_text(encoding="utf-8")
print("---- the report of a full run (fake TV) ----\n" + rep + "----")
check("the report starts as share.py's does, with the TV, the steps, memory and the checks", rep.startswith("UrTV setup report, ") and "\nTV: " in rep and "\nSteps, minutes:seconds:\n" in rep
      and "\nMemory:\n- before: " in rep and "\n- after: " in rep and "\nChecks:\nProtected features on: " in rep and "\nVersions: screensaver 4.0 (build 151), home 4.0 (build 151)" in rep, rep)
for name, text in (("restart", rep), ("no restart", rep_now)):
    check(f"share.py's own restrict() and scrub() leave the report as it is ({name}): nothing outside its allowlist, nothing personal", share.scrub(share.restrict(text)) == text,
          [l for l in text.splitlines() if l not in share.scrub(share.restrict(text)).splitlines()])
check("the steps' times are share.py's format (and the run is in it)", re.search(r"^precheck \d+:\d\d, backup \d+:\d\d, .*report \d+:\d\d$", rep, re.M) is not None
      or re.search(r"^precheck \d+:\d\d, ", rep, re.M) is not None, rep)
check("a reading without the restart says «بدون إعادة تشغيل»; one after the restart does not", "- before (بدون إعادة تشغيل): " in rep_now and "- after (بدون إعادة تشغيل): " in rep_now
      and "بدون إعادة تشغيل" not in rep, rep_now)
PERSONAL = [r"(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])", r"(?i)serial", r"(?i)ssid|wi-?fi name", r"[\w.-]+@[\w-]+\.\w+", r"(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}",
            r"(?i)riyadh|jeddah|mecca|makkah|medina|dammam|الرياض|جدة|مكة|المدينة|الدمام", r"(?i)hilal|nassr|ittihad|ahli|الهلال|النصر|الاتحاد|الأهلي", r"/home/|/Users/"]
for name, text in (("restart", rep), ("no restart", rep_now)):
    hits = [p for p in PERSONAL if re.search(p, text)]
    check(f"the report holds no city, team, address, serial, Wi-Fi name, e-mail or path ({name})", not hits, hits)
check("the engine never reads the city or the team for it (no settings of the person's in SetupReport)", not re.search(r"(?i)city|team|club|wifi|serial|ssid", src.split("final class SetupReport")[1]), "")

# the link: share.py's PAGE + "#" + urllib.parse.quote(report)
tricky = OUT / "tricky.txt"; tricky.write_text("a b/c?d#e&f=g+h%i\n«عربي» ~_.- é\t!*'();:@$,[]\n", encoding="utf-8")
for f in (OUT / "report.md", tricky):
    t = f.read_text(encoding="utf-8")
    check(f"the quoting is Python's urllib.parse.quote ({f.name})", jr("quote", f) == urllib.parse.quote(t), (jr("quote", f)[:200], urllib.parse.quote(t)[:200]))
url = jr("url", OUT / "report.md")
check("the link is share.py's: PAGE + # + the quoted report, short enough for a QR code", url == share.PAGE + "#" + urllib.parse.quote(rep) and len(url.encode()) <= 2953, (url[:120], len(url)))
check("docs/share/ gets the very report back from that link", urllib.parse.unquote(urllib.parse.urlsplit(url).fragment) == rep)
try:
    import segno
    check("share.py's QR code (segno, error level L) holds this link", segno.make(url, error="l").version <= 40)
except ImportError:
    print("SKIP segno is not installed: the QR check")
big = OUT / "big.txt"; big.write_text("x" * 4000)
check("a report too long for a QR code gives no link (share.py says the same)", jr("url", big) == "null")

# share.py's steps() and memory() on the same files
log = OUT / "steps.log"
log.write_text("2026-10-29 10:00:00 start precheck\n2026-10-29 10:00:07 done precheck\n2026-10-29 10:00:07 start before\n2026-10-29 10:01:00 stopped before (the connection)\n"
               "2026-10-29 10:04:30 start before\n2026-10-29 10:08:45 done before\n2026-10-29 10:08:45 start home\n2026-10-29 10:09:59 done home\n")
saved = share.RUN
share.RUN = OUT
try:
    want_steps = share.steps()
    summ = OUT / "summary.md"
    summ.write_text("| state | free RAM (MB of 1983) | swap used (MB) | processes | CPU idle | disabled packages |\n|---|---|---|---|---|---|\n"
                    "| before | 812 | 12 | 300 | 85% | 120 |\n| after | not measured (memory read failed) | 0 | 290 | | 140 |\n")
    want_mem = share.memory(summ)
finally:
    share.RUN = saved
check("the steps' times: share.py's steps() on the same steps.log", jr("steps", log).split("\n") == want_steps, (jr("steps", log), want_steps))
(OUT / "kinds").write_text("")
check("memory: share.py's memory() on the same summary.md", jr("memory", OUT / "summary.md", OUT / "kinds").split("\n") == want_mem, (jr("memory", OUT / "summary.md", OUT / "kinds"), want_mem))

# ---- measure.sh and SetupMeasure on one fake TV (no restart: the TV is reached by «wireless», as tv.py --transport would say)
import setup_parity
from setup_parity import Pair
if setup_parity.compile_java(check):
    MEMINFO = "MemTotal:        2030000 kB\nMemFree:          200000 kB\nMemAvailable:     830000 kB\nSwapTotal:        500000 kB\nSwapFree:         488000 kB\n"
    p = Pair(files={"system.screen_off_timeout": "600000"}, MEMINFO=MEMINFO)
    for side in ("sh", "java"):
        (p.base / side / "tv" / "meminfo").write_text(MEMINFO)
    rc, out = p.shell("measure.sh", "before")
    res, jout = p.java("measure-before")
    ram_sh, ram_app = (p.tree / "optimize/run/ram-before.txt").read_text() if (p.tree / "optimize/run/ram-before.txt").exists() else None, p.file("java", "ram-before.txt")
    check("measure: the app reads the same lines into the same ram-before.txt as measure.sh", rc == 0 and ram_sh == ram_app and "== biggest" in (ram_app or ""), (rc, out, ram_sh, ram_app, jout))
    check("measure: the same summary.md row (free RAM of the total, swap, processes, CPU idle, disabled)", p.file("sh", "summary.md") == p.file("java", "summary.md") and "| before | 810 | 11 | 300 | 87% |" in (p.file("java", "summary.md") or ""),
          (p.file("sh", "summary.md"), p.file("java", "summary.md")))
    check("measure: the screensaver timeout stretched for the reading and put back exactly, by both", p.same() and p.tv("java")["system.screen_off_timeout"] == "600000\n"
          and "settings put system screen_off_timeout 1800000" in p.changes("java"), p.diff())
    check("measure: not restarted (adb would not come back on 5555), and said so", res == "DONE" and "بدون إعادة تشغيل" in jout and "reboot" not in " ".join(p.changes("java")), jout)
sys.exit(1 if failed else 0)
