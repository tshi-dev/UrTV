"""4.3.4: «the home's three cards with the weather still show although I turned them off». On the football layout the home's cards are its three widgets
(weather, prayers, games); HomeCard chose the football home whenever anything was known, «بدون» included. Now «بدون» turns the widgets off as on the classic and
couch layouts. The rule runs on a JVM (tests/FootballOffRun.java), the wiring is read from the source.   Run: python3 tests/football_off_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

card, choice = text(HOME / "HomeCard.java"), text(HOME / "HomeCardChoice.java")
check("HomeCard asks the rule, and the football host still has to be there", "football = HomeCardChoice.football(layout, choice) && footballHost != null;" in card)
check("the old rule («anything known») is gone", 'footballHost != null && choice != null;' not in card)
check("the rule: football layout, a known pick, not empty, not «none»", 'return "football".equals(layout) && choice != null && choice[1] != null && !choice[1].isEmpty() && !"none".equals(choice[1]);' in choice)
if not shutil.which("javac"):
    print("SKIP rule: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "HomeCardChoice.java"), str(ROOT / "tests/FootballOffRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FootballOffRun"], capture_output=True, text=True).stdout
for l in out.splitlines():
    if l.startswith(("ok ", "BAD ")): check("football_off: " + l.split(" ", 1)[1], l.startswith("ok "))
check("every case ran", "ALL PASS" in out, out[-200:])
sys.exit(1 if failed else 0)
