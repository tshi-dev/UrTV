#!/usr/bin/env python3
"""«اللعب بالجوالات»: the generic phone room of the party games and the game-only server on a plain JVM (tests/PhoneRoomTest.java), and the pins that keep the
phone side to what docs/GAMES-PHONES.md says. Needs javac; without it the JVM part is skipped (exit 1 so CI notices)."""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

if not shutil.which("javac"):
    print("SKIP phone room: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    src = [str(APP / f"{n}.java") for n in ("PhoneRoom", "GamePhoneRoom", "QuizRoom", "QuizServer", "DigitsText")] + [str(ROOT / "tests/PhoneRoomTest.java")]
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", tmp, *src], capture_output=True, text=True)
    check("the phone room compiles on a plain JVM (no Android in the rules or the server)", c.returncode == 0, c.stderr[-600:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.PhoneRoomTest"], capture_output=True, text=True, timeout=120)
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line)
                if line.startswith("FAIL"): failed += 1
        check("the JVM test ran to the end", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-600:])
# ---- the pins: what the TV and the page must keep
def src(n): return (APP / n).read_text(encoding="utf-8")
odd, cha, ga, gl, reg = src("OddOneActivity.java"), src("CharadesActivity.java"), src("GameActivity.java"), src("QuizGamesActivity.java"), src("GameRegistry.java")
page = (ROOT / "assets/quiz/game.html").read_text(encoding="utf-8")
for name, a in (("odd-one", odd), ("charades", cha)):
    check(f"{name}: the switch «اللعب بالجوالات» is a chip that is off unless the phones' page opened the game, and is never kept from last time",
          '.add("phones", S.t("اللعب بالجوالات"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0)' in a and 'chips.get("phones").picked = getIntent().getBooleanExtra("phones", false) ? 1 : 0' in a
          and a.index("remembered(soundChip(chips))") < a.index('getBooleanExtra("phones"'))
    check(f"{name}: with the switch off the room is closed and the game is the remote's, as before", "closePhones();   // the switch is off" in a and "begin(" in a)
    check(f"{name}: no socket, no server and no java.net in the game's own screen (the room is the base's)", not __import__("re").search(r"ServerSocket|QuizServer|QuizRoom|java\.net\.", a))
check("odd-one: the word goes only to its own phone, the outsider gets «أنت برا السالفة», the vote cannot be for yourself",
      'phones.room.card(p, GamePhoneRoom.SECRET, names[p], w == null ? S.t("أنت برا السالفة") : w.text(en)' in odd and 'S.t("مين اللي مو معنا؟"), "", names, null, i)' in odd)
check("odd-one: a tie or no name is «ما اتفقنا»; the start needs 3 phones and takes 10 at most", "else if (c[i] == best && best > 0) who = OddOneGame.NOBODY" in odd and "OddOneGame.MIN_PLAYERS" in odd and 'S.t("اللعبة تتسع لعشرة لاعبين")' in odd)
check("charades: the word is on the actor's phone alone (the TV never draws it, never peeks it), the clock starts when the actor taps",
      "game.wordVisible() && !onPhones()" in cha and "if (onPhones()) return; game.peek();" in cha and "game.ready(); game.hide();" in cha and "phones.room.replied(actor)" in cha and "if (game.phase == CharadesGame.Phase.PLAY) game.hide();" in cha)
check("the base opens a room only on the person's switch, closes it with the screen, and says so when there is no local network",
      "protected final boolean openPhones()" in ga and "protected void onDestroy() { closePhones(); super.onDestroy(); }" in ga and 'S.t("لا توجد شبكة محلية. وصّل التلفزيون بالواي فاي")' in ga)
check("the games list: ONE «بالجوالات» tile (the quiz's place), a page with the three games, Back returns; the party games get the switch on only from that page",
      'PHONE_GAMES = {"quiz", "odd-one", "charades"}' in reg and 'boolean door = !phonePage && g.id.equals("quiz")' in gl and "phonePage = true" in gl and 'go.putExtra("phones", true)' in gl and "KEYCODE_BACK && phonePage" in gl)
check("the phone page: no outside address, no innerHTML or eval, text built with text nodes; it talks only to /api/",
      not __import__("re").search(r"https?://(?!www\.w3\.org)", page) and "innerHTML" not in page and "eval(" not in page and page.count("/api/") == 5 and "createTextNode" in page)
check("the game-only server holds no game rules: it routes a PhoneRoom (the quiz's room and this one) and nothing else", "private final PhoneRoom room;" in src("QuizServer.java") and "implements PhoneRoom" in src("QuizRoom.java") and "implements PhoneRoom" in src("GamePhoneRoom.java"))
check("docs/GAMES-PHONES.md says the switch is off by default and what is not in phase 1", "Off by default, every time" in (ROOT / "docs/GAMES-PHONES.md").read_text(encoding="utf-8") and "Not in phase 1" in (ROOT / "docs/GAMES-PHONES.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
