#!/usr/bin/env python3
"""The new party games (4.4, docs/GAMES-NEW.md): the rules run on a plain JVM (tests/NewGamesTest.java, fake clocks), and the pins that keep the TV side to what the docs say.
A game's session adds its engine to ENGINES, its checks to NewGamesTest and its pins at the end."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
G = APP / "games"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
if not shutil.which("javac"): print("SKIP: no javac"); sys.exit(1)
ENGINES = [G / "forehead/ForeheadGame.java", G / "author/AuthorGame.java", G / "closest/ClosestGame.java", G / "liar/LiarGame.java", APP / "GameBundle.java", APP / "MiniJson.java"]
with tempfile.TemporaryDirectory() as t:
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", t, *map(str, ENGINES), str(ROOT / "tests/NewGamesTest.java")], capture_output=True, text=True)
    check("the new games' rules compile on a plain JVM (no Android)", c.returncode == 0, c.stderr[-500:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", t, "sa.urtv.screensaver.NewGamesTest", str(ROOT / "assets/games/author.gb"), str(ROOT / "assets/games/closest.gb"), str(ROOT / "assets/games/liar.gb")], capture_output=True, text=True, timeout=120)
        for l in r.stdout.splitlines():
            if l.startswith(("PASS", "FAIL")): print(l); failed += l.startswith("FAIL")
        check("the JVM test ran to the end", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-400:])
def src(n): return (APP / n).read_text(encoding="utf-8")
reg, gl, room, page = src("GameRegistry.java"), src("QuizGamesActivity.java"), src("GamePhoneRoom.java"), (ROOT / "assets/quiz/game.html").read_text(encoding="utf-8")
manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
GAMES = {"forehead": "ForeheadActivity", "author": "AuthorActivity", "closest": "ClosestActivity", "liar": "LiarActivity"}
for gid, act in GAMES.items():
    a = src(act + ".java")
    check(f"{gid}: in the registry (needs phones), in the filter's list, in the manifest (not exported, landscape), and a GameActivity",
          re.search(r'new Game\("%s", "[^"]+", "[^"]+", "%s", true, true\)' % (gid, act), reg) is not None and f'"{gid}"' in reg.split("PHONE_GAMES = {")[1].split("}")[0]
          and re.search(r'android:name="\.%s"\s+android:exported="false"\s+android:screenOrientation="landscape"' % act, manifest) is not None and f"class {act} extends GameActivity" in a)
    check(f"{gid}: opens the phone room itself (the phones are the game), takes no socket of its own, writes no colour of its own",
          "openPhones()" in a and not re.search(r"ServerSocket|QuizServer|QuizRoom|java\.net\.", a) and not re.search(r"0x[0-9A-Fa-f]{8}|Color\.parseColor", a))
fh = src("ForeheadActivity.java")
check("forehead: the word is only ever on the phone (a tilt card), never drawn on the TV; the turns go round the phones of a team; the words are the charades bank and nothing repeats on this TV",
      "GamePhoneRoom.TILT" in fh and "game.word" in fh and fh.count("game.word") == 2 and "CharadesActivity.PUBLISHED" in fh and "GamePrefs.remember(this, id(), r)" in fh and "private int playerSeat()" in fh)
check("the phone page: a tilt card with tilt down = right, up = skip, the two buttons too, permission asked on a tap (iOS); the room answers a tilt card as an action card",
      "c.kind === 'tilt'" in page and "z <= -7" in page and "z >= 7" in page and "requestPermission" in page and "case ACT: case TILT:" in room)
au = src("AuthorActivity.java")
check("author: the prompts come from the bundled bank (GameBundles), nothing repeats on this TV; the answers are typed on the phones (a text card), the votes are a pick card that never offers your own name, and the writer's phone is told not to vote",
      'GameBundles.load(this, "author")' in au and "GamePrefs.remember(this, id(), r)" in au and "GamePhoneRoom.TEXT" in au and "GamePhoneRoom.PICK" in au and "if (j != i) acts[k++] = j;" in au and 'S.t("هذي إجابتك، لا تصوّت")' in au)
check("the bundled banks are bundles, not plain text: assets/games/author.gb has no readable Arabic and no JSON",
      all(("سمك".encode() not in (ROOT / "assets/games" / f).read_bytes()) and (ROOT / "assets/games" / f).read_bytes()[:3] == b"GB1" for f in ("author.gb",)))
cl = src("ClosestActivity.java")
check("closest: the items come from the bundled bank, nothing repeats on this TV; the guesses are typed on the phones in a number card (a number pad) and read by the rules' own number parser",
      'GameBundles.load(this, "closest")' in cl and "GamePrefs.remember(this, id(), r)" in cl and "GamePhoneRoom.TEXT" in cl and "unit.isEmpty() ? S.t(\"رقم\") : unit}, null, 1)" in cl and "game.submit(p, t[0])" in cl and "inputmode', 'numeric'" in page)
check("the bundled closest bank is a bundle (no readable Arabic, no JSON)", (ROOT / "assets/games/closest.gb").read_bytes()[:3] == b"GB1" and "مساحة".encode() not in (ROOT / "assets/games/closest.gb").read_bytes())
li = src("LiarActivity.java")
check("liar: the roles go only to the tellers' own phones (a secret card), the others hear the three names; the vote is a pick card; nothing on the TV names the liar before the reveal; nothing repeats on this TV",
      "GamePhoneRoom.SECRET, names[i]" in li and "game.liarIs(i)" in li and li.count("liarIs(") == 1 and "game.liar()" in li and li.index("case REVEAL: {") < li.index("game.liar()") and "GamePhoneRoom.PICK" in li and "GamePrefs.remember(this, id(), r)" in li and 'GameBundles.load(this, "liar")' in li)
check("the bundled liar topics are a bundle", (ROOT / "assets/games/liar.gb").read_bytes()[:3] == b"GB1" and "موقف".encode() not in (ROOT / "assets/games/liar.gb").read_bytes())
sys.exit(1 if failed else 0)
