#!/usr/bin/env python3
"""«خلية الحروف» and «خمّن الكلمة»: the rules of both games run on a plain JVM (tests/HiveTest.java, tests/Word5Test.java: no Android class in either engine) against the
bundled data, and the data is what its tools make. Needs javac; without it the JVM part is skipped (exit 1 so CI notices)."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GAMES = ROOT / "android/app/src/main/java/sa/urtv/screensaver/games"
DATA = ROOT / "assets/games"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

RUNS = [("hive", "HiveTest", ["letter-hive/starter.tsv", "letter-hive/hints.tsv"], ("Board", "Bank", "Game")),
        ("word5", "Word5Test", ["word5/accepted.bin", "word5/answers.tsv"], ("Text", "Words", "Game", "Keys"))]
if not shutil.which("javac"):
    print("SKIP games: no javac"); sys.exit(1)
for pkg, test, data, parts in RUNS:
    engine = [GAMES / pkg / f"{pkg.capitalize()}{p}.java" for p in parts]
    with tempfile.TemporaryDirectory() as tmp:
        c = subprocess.run(["javac", "-nowarn", "-d", tmp, *map(str, engine), str(ROOT / f"tests/{test}.java")], capture_output=True, text=True)
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

hive = (DATA / "letter-hive/starter.tsv").read_text(encoding="utf-8").splitlines()
check("hive: the starter is tab-separated lines of 15 fields, sorted by id, LF only, under 100 KB",
      all(len(l.split("\t")) == 15 for l in hive) and [l.split("\t")[0] for l in hive] == sorted(l.split("\t")[0] for l in hive)
      and b"\r" not in (DATA / "letter-hive/starter.tsv").read_bytes() and (DATA / "letter-hive/starter.tsv").stat().st_size < 100_000)
shown = "\n".join(hive) + (DATA / "letter-hive/hints.tsv").read_text(encoding="utf-8")
check("no «·» or «•» in what the hive shows", "·" not in shown and "•" not in shown)
sys.path.insert(0, str(ROOT / ".github/tools"))
import make_word5
check("word5: the tool's spelling rule is the engine's (the same ranges and letters in both)",
      make_word5.ALPHABET in (GAMES / "word5/Word5Text.java").read_text(encoding="utf-8") and make_word5.normalize("مَدْرَسَة إبريق مستشفى كـتاب آٱ") == "مدرسه ابريق مستشفي كتاب اا")
sys.exit(1 if failed else 0)
