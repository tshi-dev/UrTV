#!/usr/bin/env python3
"""«خلية الحروف» and «خمّن الكلمة»: the rules of both games run on a plain JVM (tests/HiveTest.java, tests/Word5Test.java: no Android class in either engine) against the
bundled data, and the data is what its tools make. Needs javac; without it the JVM part is skipped (exit 1 so CI notices)."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
GAMES = ROOT / "android/app/src/main/java/sa/urtv/screensaver/games"
DATA = ROOT / "assets/games"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

RUNS = [("hive", "HiveTest", ["../quiz/starter.json", "letter-hive.json", "khanat.json"], ("Board", "Bank", "Game")),
        ("word5", "Word5Test", ["word5-answers.json", "word5-guesses.json"], ("Text", "Words", "Game", "Keys"))]
if not shutil.which("javac"):
    print("SKIP games: no javac"); sys.exit(1)
for pkg, test, data, parts in RUNS:
    engine = [GAMES / pkg / f"{pkg.capitalize()}{p}.java" for p in parts]
    loader = [APP / f for f in ("GamesBoardData.java", "MiniJson.java", "QuizBankCheck.java", "QuizPick.java", "QuizRoom.java", "DigitsText.java")] + sorted(p for d in ("hive", "word5") for p in (GAMES / d).glob("*.java") if d != pkg)
    with tempfile.TemporaryDirectory() as tmp:
        c = subprocess.run(["javac", "-nowarn", "-d", tmp, *map(str, engine), *map(str, loader), str(ROOT / f"tests/{test}.java")], capture_output=True, text=True)
        check(f"{pkg}: the engine compiles on a plain JVM", c.returncode == 0, c.stderr[-800:])
        if c.returncode == 0:
            r = subprocess.run(["java", "-cp", tmp, f"sa.urtv.screensaver.games.{pkg}.{test}", *(str(DATA / d) for d in data)], capture_output=True, text=True, timeout=240)
            for line in r.stdout.splitlines():
                if line.startswith(("PASS", "FAIL")):
                    print(line)
                    if line.startswith("FAIL"): failed += 1
            check(f"{pkg}: the JVM test ran to the end", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-800:])
    src = "".join(p.read_text(encoding="utf-8") for p in engine)
    check(f"{pkg}: the engine imports nothing of Android, reads no file, keeps no preference and shows no text of its own (the screen does, through S.t)",
          not re.search(r"import (android|androidx)\.|getSharedPreferences|java\.io\.File|S\.t\(", src))

check("the loader (GamesBoardData) is plain Java too and reads the documented files through the quiz's own checks (MiniJson, QuizBankCheck), no org.json",
      (lambda t: not re.search(r"import (android|androidx)\.|org\.json", t) and "QuizBankCheck.parse(quiz)" in t and "MiniJson.parse(" in t)((APP / "GamesBoardData.java").read_text(encoding="utf-8")))
sys.exit(1 if failed else 0)
