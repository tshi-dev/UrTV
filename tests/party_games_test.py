#!/usr/bin/env python3
"""«واحد مو معنا» and «بدون كلام / وصّف»: the rules run on a plain JVM (tests/PartyGamesTest.java, a fake clock, the bundled word files), and the pins on the
word files (the shared ones of docs/GAMES-DATA.md). Needs javac; without it the JVM part is skipped (exit 1 so CI notices)."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
ENGINES = [APP / "games/oddone/OddOneWords.java", APP / "games/oddone/OddOneGame.java", APP / "games/charades/CharadesWords.java", APP / "games/charades/CharadesGame.java"]
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

if not shutil.which("javac"):
    print("SKIP party games: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-d", tmp, *map(str, ENGINES), str(APP / "MiniJson.java"), str(ROOT / "tests/PartyGamesTest.java")], capture_output=True, text=True)
    check("the two games' rules compile on a plain JVM (no Android in them)", c.returncode == 0, c.stderr[-800:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.PartyGamesTest", str(ROOT / "assets/games")], capture_output=True, text=True, timeout=240)
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line)
                if line.startswith("FAIL"): failed += 1
        check("the JVM test ran to the end", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-800:])

src = "".join(p.read_text(encoding="utf-8") for p in ENGINES)
code = re.sub(r"/\*.*?\*/|//[^\n]*", "", src, flags=re.S)
check("the rules know no Android, no screen, no file and no network: only java.util", not re.search(r"\bandroid\.|java\.(io|net|nio)\.|getSharedPreferences", code) and set(re.findall(r"^import ([\w.]+)\.\w+;", src, flags=re.M)) == {"java.util"})
check("they run on Android 5 (API 21) as they are: no java.util.function, no streams, no List.sort", not re.search(r"java\.util\.(function|stream)|\.stream\(\)|\w\.sort\(\(|\.forEach\(|\.removeIf\(", code))
check("no Arabic text is decided in the rules (the screen says everything through S.t)", not re.search(r'"[^"\n]*[؀-ۿ][^"\n]*"', code))
sys.path.insert(0, str(ROOT / ".github/tools"))
import make_games_data as mgd
check("the two starters are the shared ones (docs/GAMES-DATA.md) and pass the publisher's verify", all(mgd.verify_file(n, str(ROOT / "assets/games" / (n + ".json")), minimum=False) == [] for n in ("odd-one", "charades")))
for n, cls in (("odd-one", "oddone/OddOneWords.java"), ("charades", "charades/CharadesWords.java")):
    lo, hi = mgd.LIMITS[n][1], mgd.LIMITS[n][2]; j = (APP / "games" / cls).read_text(encoding="utf-8")
    check(f"{n}: the app's own check keeps the published limits ({lo} to {hi}; the charades starter is smaller, 100 is its floor)", (f"TOPICS_MIN = {lo}, TOPICS_MAX = {hi}" in j) if n == "odd-one" else (f"WORDS_MAX = {hi}" in j and "WORDS_MIN = 100" in j))
sys.exit(1 if failed else 0)
