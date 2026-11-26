#!/usr/bin/env python3
"""4.4 beta 4 (the tester: with «بدون» as the home's card the clock was still the small one, top right): HomeCardChoice.count says 0 for «بدون»
(it said 1, so the glass clock, which stays off while a card shows, never came back), and 1 to 3 for the cards. Plain Java run on a JVM.
Run: python3 tests/home_card_count_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/launcher/src/main/java/sa/urtv/home/HomeCardChoice.java"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
check("the home reads the glass clock from the shown cards, which are none for «بدون»",
      "boolean glass = glassClock(cfg.homeClock, homeCard != null && homeCard.shownCards() > 0);" in main)
if not (shutil.which("javac") and shutil.which("java")):
    print("SKIP HomeCardChoice.count: no javac here (CI compiles it)")
    sys.exit(1 if failed else 0)
run = """package sa.urtv.home;
public class CountRun { static void o(String n, boolean ok) { System.out.println(n + "\\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  o("none: 0 cards", HomeCardChoice.count(new String[]{"", "none"}) == 0);
  o("empty: 0 cards", HomeCardChoice.count(new String[]{"", ""}) == 0);
  o("one card: 1", HomeCardChoice.count(new String[]{"", "prayers"}) == 1);
  o("two cards: 2", HomeCardChoice.count(new String[]{"", "prayers,stocks"}) == 2);
  o("more than three: 3", HomeCardChoice.count(new String[]{"", "a,b,c,d"}) == 3);
  o("nothing known: the prayers' one card, as before", HomeCardChoice.count(null) == 1);
} }"""
with tempfile.TemporaryDirectory() as tmp:
    Path(tmp, "CountRun.java").write_text(run, encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(SRC), str(Path(tmp, "CountRun.java"))], capture_output=True, text=True)
    check("HomeCardChoice compiles on its own", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CountRun"], capture_output=True, text=True).stdout
        for line in out.splitlines():
            n, res = line.rsplit("\t", 1)
            check("count: " + n, res == "ok")
        check("every case ran", len(out.splitlines()) == 6, len(out.splitlines()))
sys.exit(1 if failed else 0)
