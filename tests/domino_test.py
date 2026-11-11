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

# ---- the screen (DominoActivity on the games kit): read from the source
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
act = (APP / "DominoActivity.java").read_text(encoding="utf-8")
ac = re.sub(r"/\*.*?\*/|//[^\n]*", "", act, flags=re.S)
reg = (APP / "GameRegistry.java").read_text(encoding="utf-8")
manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("the game is on the kit and in the list: DominoActivity extends GameActivity, ready in the registry, in the manifest (not exported, landscape)",
      "public class DominoActivity extends GameActivity" in act and '"DominoActivity", false, true)' in reg
      and re.search(r'android:name="\.DominoActivity"\s+android:exported="false"\s+android:screenOrientation="landscape"', manifest) is not None)
key = ac[ac.index("protected void onKey("):ac.index("private void botMove()")]
table_keys = key[key.index("DominoTable.State was"):]
arrow = table_keys[table_keys.index("if (c == DominoTable.FOCUS) {"):table_keys.index("View from")]
check("an arrow key is light: at the table it never calls redraw(), and when only the focus moved it restyles the two tiles (or lays the two ends again) and returns",
      "redraw()" not in table_keys and "handTiles.get(focus).style(PLAIN); handTiles.get(tb.focus()).style(FOCUSED);" in arrow and arrow.rstrip().endswith("return;\n        }")
      and not re.search(r"\b(bar|hand|row|keys|changed|group)\(", arrow) and "addView" not in arrow)
check("a restyled tile redraws only when its style really changed", "void style(int s) { if (s != style) { style = s; invalidate(); } }" in ac)
check("the table is built once; a move replaces only the groups that changed (the bar on a state or score change, the hand when the change says HAND)",
      "if ((c & (DominoTable.STATE | DominoTable.SCORE)) != 0) bar(); else counts.invalidate();" in ac and "if ((c & DominoTable.HAND) != 0) hand();" in ac and "removeAllViews" not in ac)
check("only the played tile moves: one view slides for 250 ms while the chain leaves that place empty; a computer waits 350 ms first (600 ms in all); OK skips",
      "BOT_MS = 350, MOVE_MS = 250" in ac and "chain.hidden = index;" in ac and ".setDuration(MOVE_MS)" in ac and ac.count(".animate()") == 2 and "tb.ok()" in key)
check("low-RAM TVs and animations off: no slide at all (the kit's `animate`)", "if (animate && chain.snake != null" in ac)
check("tiles and pips are drawn (rectangles, lines, circles): no bitmap, no picture, no rounded shape, no colour of its own",
      "drawCircle" in ac and "drawRect" in ac and not re.search(r"Bitmap|Drawable|ImageView|setCornerRadi|drawRoundRect|0xFF[0-9A-Fa-f]{6}|Color\.", ac))
check("a hand never stays on the screen behind the pause card or when the app leaves the front: hide() in onPause and when the card opens; the hand-over shows no tile",
      "protected void onPause() { if (tb != null && screen == Screen.PLAY) tb.hide(); super.onPause(); }" in ac and "if (tb != null && screen == Screen.PLAY) tb.hide(); }" in ac.split("onPauseChanged")[1][:300]
      and "if (st == DominoTable.State.HANDOVER) {" in ac and "new TileView" not in ac.split("if (st == DominoTable.State.HANDOVER) {")[1].split("return;")[0])
check("Back at the table opens the pause card (or leaves the choice of the end); the screen draws no rule of its own: every move goes through DominoTable",
      "if (tb.back()) { chain.sync(); keys(); } else openPause();" in ac and not re.search(r"\.play\(\w+, |\.draw\(\w+\)|\.pass\(", ac.replace("play(QuizSound", "")))
check("the table on the screen is the table the snake was tested on: 1728 x 480 at 72", "TABLE_H = 480" in ac and "UNIT = 72" in ac and "Snake.lay(list, anchor, px(1728), px(TABLE_H), px(UNIT))" in ac
      and "H = 480" in (ROOT / "tests/DominoTest.java").read_text(encoding="utf-8"))
check("every number through the person's digits (num), every word through S.t; nothing kept about the players", not re.search(r'String\.valueOf|Integer\.toString|"\s*\+\s*g\.count|getSharedPreferences|FileOutputStream', ac) and "num(g.count(i))" in ac)
check("no «·» or «•» on the screen", not re.search("[·•]", ac))
sys.exit(1 if failed else 0)
