#!/usr/bin/env python3
"""«دومينو» (docs/DOMINO.md): the rules and the computer players run on a plain JVM (tests/DominoTest.java, every game from a seed), and the pins
that keep the engine what the doc says: no Android, no clock, no chance but the Random given in. Needs javac; without it the JVM part is skipped (exit 1 so CI notices)."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PKG = ROOT / "android/app/src/main/java/sa/urtv/screensaver/games/domino"
ENGINE = ("Tile", "DominoGame", "DominoBot", "Snake", "DominoTable")
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

if not shutil.which("javac"):
    print("SKIP domino: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-source", "8", "-target", "8", "-d", tmp, *(str(PKG / f"{n}.java") for n in ENGINE), str(ROOT / "tests/DominoTest.java")], capture_output=True, text=True)
    check("the domino engine compiles alone on a plain JVM, as Java 8 (no Android in the rules or the computer players)", c.returncode == 0, c.stderr[-600:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.games.domino.DominoTest"], capture_output=True, text=True, timeout=600)
        ran = 0
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL", "INFO")):
                print(line); ran += line.startswith("PASS")
                if line.startswith("FAIL"): failed += 1
        check("the JVM test ran to the end", (r.returncode == 0 and ran >= 30) or "FAIL" in r.stdout, r.stderr[-600:])

src = {n: (PKG / f"{n}.java").read_text(encoding="utf-8") for n in ENGINE}
code = {n: re.sub(r"/\*.*?\*/|//[^\n]*", "", s, flags=re.S) for n, s in src.items()}
every = "\n".join(code.values())
imports = sorted(set(re.findall(r"^import ([\w.]+);", every, flags=re.M)))
check("the engine imports java.util only: no Android, no I/O, no network, no threads", all(i.startswith("java.util.") and "concurrent" not in i for i in imports), imports)
check("no chance but the Random given in, and no clock: no new Random, Math.random, SecureRandom, currentTimeMillis or nanoTime in the engine",
      not re.search(r"new Random\(|Math\.random|SecureRandom|currentTimeMillis|nanoTime|Collections\.shuffle", every))
check("the computer player is given a View and a Random, never the game: DominoBot has no DominoGame parameter or field, and reads no hand but its own",
      not re.search(r"DominoGame\s+\w+\s*[,;)=]", code["DominoBot"]) and "rawHand" not in code["DominoBot"] and "rawBoneyard" not in code["DominoBot"] and ".hand(" not in code["DominoBot"])
check("the hidden tiles have no public road out: the raw lists are package-private (for the test), hand() and chain() are read only, the view is copies",
      re.search(r"^\s+List<Tile> rawHand\(", src["DominoGame"], flags=re.M) and re.search(r"^\s+List<Tile> rawBoneyard\(", src["DominoGame"], flags=re.M)
      and "public List<Tile> hand(int p) { return Collections.unmodifiableList(" in src["DominoGame"] and "private final List<Tile> boneyard" in src["DominoGame"])
check("nothing about a game is kept: no prefs, no file", not re.search(r"SharedPreferences|File|Stream", every))
doc = (ROOT / "docs/DOMINO.md").read_text(encoding="utf-8") if (ROOT / "docs/DOMINO.md").exists() else ""
check("docs/DOMINO.md states every rule choice: the draw game, 7/9/7 tiles and the double blank, who opens, draw until a tile fits, the blocked round, the targets, each for himself, the three levels",
      all(k in doc for k in ("double-six", "double blank", "highest double", "boneyard", "blocked", "50, 100", "each for himself", "Level 1", "Level 2", "Level 3", "[variant]")))
check("no «·» or «•» in the engine or its doc", not re.search("[·•]", every + doc))
sys.exit(1 if failed else 0)
