#!/usr/bin/env python3
"""The one-remote modes (docs/GAMES-REMOTE.md): the rules on a plain JVM (tests/RemoteModesTest.java: «نادوا الجواب», «أقرب رقم» on paper, «واحد يكذب» by hands, «على الظهر»),
then the Android side by its source: the setting «اللعب بالريموت بس» is OFF by default and only a chip of the seven phone games, nothing changes without it, no colour literal and no
pill in the new screen, the encrypted banks are only read through GameBundles. Needs javac (exit 1 without, so CI notices)."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(n): return (APP / n).read_text(encoding="utf-8")
def code(s): return re.sub(r"/\*.*?\*/|//[^\n]*", "", s, flags=re.S)

ENGINES = ["games/remote/CallAloudRound", "games/closest/ClosestGame", "games/closest/PaperPlaces", "games/author/AuthorGame", "games/liar/LiarGame", "games/liar/LiarRemote",
           "games/charades/CharadesGame", "games/charades/CharadesWords"]
if not shutil.which("javac"):
    print("SKIP remote modes: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-d", tmp, *(str(APP / f"{n}.java") for n in ENGINES), str(APP / "MiniJson.java"), str(ROOT / "tests/RemoteModesTest.java")], capture_output=True, text=True)
    check("the modes' rules compile on a plain JVM (no Android in them)", c.returncode == 0, c.stderr[-800:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.RemoteModesTest", str(ROOT / "assets/games")], capture_output=True, text=True, timeout=240)
        ran = 0
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line); ran += 1
                if line.startswith("FAIL"): failed += 1
        check("the JVM test ran to the end", ran > 25 and (r.returncode == 0 or "FAIL" in r.stdout), r.stderr[-600:])
for n in ENGINES:
    t = code(src(n + ".java"))
    check(f"{n}: no Android, no S, no Prefs", not re.search(r"\bimport android\.|\bS\.t\(|\bS\.n\(|\bPrefs\b|Context\b", t))

base, ca, cl, li, fo, ch = src("GameActivity.java"), src("CallAloudActivity.java"), src("ClosestActivity.java"), src("LiarActivity.java"), src("ForeheadActivity.java"), src("CharadesActivity.java")
manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
phone_games = {"EmojiActivity.java": "emoji", "LogicActivity.java": "logic", "QuranVerseActivity.java": "quran-verse", "MathActivity.java": "maths"}
check("the setting «اللعب بالريموت بس» is one row (remoteChip) OFF by default, in the setup of the seven phone games and nowhere else",
      'c.add("remote", S.t("اللعب بالريموت بس"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0)' in base and "remoteChip(chips);" in cl and "remoteChip(chips);" in li and "remoteChip(chips);" in fo
      and all("remoteChip(chips);" in src(f) for f in phone_games)
      and sum(1 for f in APP.glob("*Activity.java") if "remoteChip(chips)" in f.read_text(encoding="utf-8")) == 7)
check("OFF changes nothing: each activity leaves the phone road as it was unless the row is on (a redirect, or a branch on `remoteOn`)",
      all(f'if (remoteOn(chips)) {{ startActivity(remoteIntent(CallAloudActivity.class, chips).putExtra("src", "{g}")); finish(); return; }}' in src(f) for f, g in phone_games.items())
      and "if (remoteOn(chips)) {   // «اللعب بالريموت بس»: «على جبيني» is a mode of «بدون كلام»" in fo and "paper = remoteOn(chips);" in cl and "if (remoteOn(chips)) { startRemote(); return; }" in li)
check("«نادوا الجواب»: one screen for the four games (emoji, logic, verses, sums), in the manifest, its ids apart from the phone games' (so the remembered chips never mix), and its words only from the games' own sources",
      'android:name=".CallAloudActivity"' in manifest and 'return src + "-remote";' in ca and "EmojiGame.parse(GameBundles.load(this, \"emoji\"))" in ca and "LogicItems.bank(GameBundles.load(this, \"logic\"))" in ca
      and 'getAssets().open("shared/verses.json")' in ca and "MathGame.generate(" in ca and "GamePrefs.remember(this, src, r)" in ca and "GamePrefs.recent(this, src)" in ca)
check("«نادوا الجواب»: who answered first by the D-pad on a grid of the sides and «ما أحد» (the focus is scale and shadow: focusCard), the score cells in their own place (scoreBar)",
      "focusCard(" in ca and "scoreBar(score, names, -1, 800)" in ca and "round.pick(pick == round.sides ? -1 : pick)" in ca and 'S.t("ما أحد")' in ca)
check("«أقرب رقم» on paper: the timer, the number, three places by the D-pad (a side holds one), «بالضبط» for the first place, the score cells", "paperKey(back, ok, dx, dy)" in cl and "pg.pick(pickSel == pg.sides ? -1 : pickSel)" in cl and "pg.setExact(!pg.exact)" in cl and "scoreBar(score, names, -1, 820)" in cl)
check("«واحد يكذب» by hands: the remote goes to each teller (handOver), the room votes by pointing (the host picks), scored as one; minimum and the points are constants (LiarRemote)",
      "handOver(names[p], game.liarIs(p)" in li and "LiarRemote.applyRoom(game, voteSel < 3 ? t[voteSel] : -1)" in li and "LiarRemote.MIN_PLAYERS + chips.picked(\"players\")" in li)
check("«على جبيني» is a mode of «بدون كلام»: the forehead's setup hands its teams, time and kids to the charades screen with `behind`, which uses the «وصّف» words, shows the word on the TV to all but the guesser and starts the clock at once",
      'putExtra("behind", true)' in fo and 'getBooleanExtra("behind", false)' in ch and "game.behind = behind && !onPhones();" in ch and "boolean describe = behind || chips.picked(\"mode\") == 1;" in ch and "if (game.behind) { screen = Screen.BACK;" in ch)
new = code(ca) + code(base[base.index("protected final TextView focusCard"):base.index("protected final void board")])
check("the new screens follow our rules: no colour of their own, no «·», no pill or rounded shape, focus by scale and shadow", not re.search(r"0xFF[0-9A-Fa-f]{6}", code(ca)) and "·" not in new and "•" not in new and "setCornerRadius" not in new and "setScaleX(1.06f)" in base and "setElevation(px(24))" in base)
check("the encrypted banks stay closed: only GameBundles opens a .gb, in memory", ".gb" not in code(ca) and "FileOutputStream" not in code(ca) and "openFileOutput" not in code(ca))
check("the score cells are everywhere: the old sidesBar draws through the same bar (88 high, so no old layout moves), the new screens use it at 112 with the «نقطة» label",
      "protected final void sidesBar(GameScore s, String[] names, int turn, float y) { scoreBarH(s, names, turn, y, 88); }" in base and "scoreBarH(s, names, turn, y, 112)" in base and "h >= 112" in base and "t.setBackground(shape(on ? L.ink" not in base)
check("«واحد يكذب»: the rounds' screens draw the players' bar at the bottom in both ways (the rank rows stood over the question block with 5 or more players), and «+٢ للكذّاب» keeps its plus with its number (an isolate)",
      "board(" not in code(li) and code(li).count("scoreBar(score, names, -1, 820);") == 2 and '"\\u2066+" + num(game.gained[game.liar()]) + "\\u2069"' in li)
print("failed" if failed else "ok"); sys.exit(1 if failed else 0)
