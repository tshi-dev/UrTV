#!/usr/bin/env python3
"""4.3.2 (a user's adhan times and prayer card disappeared after 4.3.1): the home keeps the screensaver's last REAL answer (the city, the cards) on disk and uses it,
marked old, when it starts while that app cannot answer, instead of the page's defaults; an old copy never becomes the remembered place. Needs javac for the JVM part.
Run: python3 tests/fragment_keep_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


cs = (HOME / "ConfigSnapshot.java").read_text(encoding="utf-8")
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FragmentKeep.java"), str(ROOT / "tests/FragmentKeepRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FragmentKeepRun"], check=True, capture_output=True, text=True).stdout.split("\n")
    r = {l.split(" ")[0]: l.split(" ", 1)[1] for l in out if l}
    check("a real answer is saved when it is new, not when it is the same, empty or missing", r["save_new"] == "true" and r["save_same"] == "false" and r["save_none"] == "false" and r["save_empty"] == "false", r)
    check("no answer: the kept fragment; an answer: the answer; neither (or an empty kept one): nothing, the defaults as before",
          r["choose_fresh"] == "city=new" and r["choose_kept"] == "city=old" and r["choose_nothing"] == "null" and r["choose_emptykept"] == "null", r)
    check("the kept one counts as used only when there was no answer", r["used"] == "true false false", r["used"])
else:
    print("SKIP fragment keep (JVM half): no javac here")
check("ConfigSnapshot writes every real answer (keep) and, with no answer and nothing in memory, uses the kept one marked old (asStale: it never becomes the place or moves the alarm)",
      "keep(c, fragment);" in cs and "d.withFragment(kept).asStale()" in cs and "FragmentKeep.usedKept(null, kept)" in cs and "placeFragment() { return stale ? null : fragment; }" in cs)
check("it is a plain file of the home's own preferences (config_last), no personal data beyond the settings fragment the home already reads", '"config_last"' in cs)
sys.exit(1 if failed else 0)
