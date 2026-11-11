#!/usr/bin/env python3
"""«واحد مو معنا» and «بدون كلام / وصّف»: the rules run on a plain JVM (tests/PartyGamesTest.java, a fake clock, the bundled word files), and the pins on the
word files. Needs javac; without it the JVM part is skipped (exit 1 so CI notices)."""
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
import make_party_words as mpw, json
check("the bundled word files pass the tool's verify (assets/games: 30 topics or more, 600 charades words or more, nothing banned, no «·»)", mpw.verify(str(ROOT / "assets/games")) == [])
with tempfile.TemporaryDirectory() as t2:
    def bad(name, mut):
        for n in ("odd-one.json", "charades.json"):
            d = json.load(open(ROOT / "assets/games" / n, encoding="utf-8"))
            if n == name: mut(d)
            (Path(t2) / n).write_text(json.dumps(d, ensure_ascii=False), encoding="utf-8")
        return mpw.verify(t2)
    check("the verify refuses: a «·» in a word, a duplicate word in a topic, a level 4, a short deck, a duplicate charades id",
          bad("odd-one.json", lambda d: d["topics"][0]["w"][0].__setitem__(0, "أ · ب")) and bad("odd-one.json", lambda d: d["topics"][1]["w"].append(d["topics"][1]["w"][0]))
          and bad("charades.json", lambda d: d["words"][0].__setitem__(4, 4)) and bad("charades.json", lambda d: d["words"].__delitem__(slice(100, None)))
          and bad("charades.json", lambda d: d["words"][1].__setitem__(0, d["words"][0][0])))
sys.exit(1 if failed else 0)
