#!/usr/bin/env python3
"""«الأسرع» (the live quiz): the rules, the room, the picking and the game-only server run on a plain JVM (tests/QuizTest.java, a fake clock, the server on the
loopback), and the pins that keep the TV side to what docs/QUIZ.md says. Needs javac; without it the JVM part is skipped (exit 1 so CI notices)."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

if not shutil.which("javac"):
    print("SKIP quiz: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-d", tmp, *(str(APP / f"Quiz{n}.java") for n in ("Room", "Pick", "Server", "BankCheck")), str(APP / "MiniJson.java"), str(APP / "DigitsText.java"), str(ROOT / "tests/QuizTest.java")], capture_output=True, text=True)
    check("the quiz compiles on a plain JVM (no Android in the rules, the picking or the server)", c.returncode == 0, c.stderr[-600:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.QuizTest", str(ROOT / "assets/quiz/starter.json")], capture_output=True, text=True, timeout=240)
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line)
                if line.startswith("FAIL"): failed += 1
        check("the JVM test ran to the end", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-600:])
# ---- the bank's tools: the workflow, and the Python check agrees with the app's
wf = "\n".join(l for l in (ROOT / ".github/workflows/quiz-bank.yml").read_text(encoding="utf-8").splitlines() if not l.lstrip().startswith("#")) if (ROOT / ".github/workflows/quiz-bank.yml").exists() else ""
check("quiz-bank.yml: runs games/check.py first, then builds and verifies, then publishes to data-games as one commit by tshi-dev, and only the push of games/** and by hand",
      wf.index("games/check.py") < wf.index("make_quiz_bank.py gd/") < wf.index("--verify") < wf.index("git push -f origin data-games") and "workflow_dispatch" in wf
      and 'user.name "tshi-dev"' in wf and "272740088+tshi-dev@users.noreply.github.com" in wf and "Co-Authored" not in wf and "Claude" not in wf and "--orphan data-games" in wf)
sys.path.insert(0, str(ROOT / ".github/tools"))
import make_quiz_bank as mqb, json as _json
check("the Python verify (CI) accepts the starter and the full compact bank form, as the app's check does", mqb.verify(str(ROOT / "assets/quiz/starter.json"), 40) == [])
with tempfile.TemporaryDirectory() as t2:
    good = _json.load(open(ROOT / "assets/quiz/starter.json", encoding="utf-8"))
    def bad(mut, name):
        d = _json.loads(_json.dumps(good)); mut(d); f = Path(t2) / name; f.write_text(_json.dumps(d, ensure_ascii=False), encoding="utf-8"); return mqb.verify(str(f), 40)
    (Path(t2) / "h.json").write_text("<html>404</html>", encoding="utf-8")
    check("the Python verify refuses an HTML page, a duplicate option, a duplicate id, difficulty 4, an extra key, too few items",
          mqb.verify(str(Path(t2) / "h.json"), 40) and bad(lambda d: d[0]["w"][0].__setitem__(0, d[0]["a"][0]), "a.json") and bad(lambda d: d[1].__setitem__("i", d[0]["i"]), "b.json")
          and bad(lambda d: d[0].__setitem__("d", 4), "c.json") and bad(lambda d: d[0].__setitem__("x", 1), "d.json") and bad(lambda d: d.__delitem__(slice(10, None)), "e.json"))
bank_src = (APP / "QuizBank.java").read_text(encoding="utf-8")
check("the app checks what it downloads before it keeps it: through Net (a size cap, no redirect), the whole-file check, a temporary file renamed, the last good copy kept; no org.json in the quiz",
      "QuizBankCheck.parse(b)" in bank_src and "Net.get(URL, null, 15000, 20000, QuizBankCheck.BYTES_MAX)" in bank_src and "r.status != 200" in bank_src and "tmp.renameTo(f)" in bank_src
      and "org.json" not in bank_src + (APP / "QuizBankCheck.java").read_text(encoding="utf-8"))
check("the room code and the tokens come from a SecureRandom", "new java.security.SecureRandom()" in (APP / "QuizActivity.java").read_text(encoding="utf-8"))

# ---- the pins: what the TV side must keep
act = (APP / "QuizActivity.java").read_text(encoding="utf-8")
srv = (APP / "QuizServer.java").read_text(encoding="utf-8")
room = (APP / "QuizRoom.java").read_text(encoding="utf-8")
bank = (APP / "QuizBank.java").read_text(encoding="utf-8")
manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
hub = (APP / "YourTvHubActivity.java").read_text(encoding="utf-8")
doc = (ROOT / "docs/QUIZ.md").read_text(encoding="utf-8") if (ROOT / "docs/QUIZ.md").exists() else ""
check("the screen is held awake from the lobby to the end (no screensaver mid-game), by the window flag and nothing else",
      "FLAG_KEEP_SCREEN_ON" in act and "closeRoom();" in act and "protected void onDestroy() { closeRoom(); super.onDestroy(); }" in act)
check("the game server answers the local network only, binds to the TV's own private address, and has no route outside the game",
      "lanOnly && !local(c.getInetAddress())" in srv and "server.start(lan)" in act and "isSiteLocalAddress" in act and "static final int BODY_MAX = 1024" in srv
      and not re.search(r'path\.(equals|startsWith)\("/(remote|pair|key|app|settings)', srv))
check("nothing about a game is written to disk: no names, scores or tokens in prefs or files (only the chips, the sound switch and the question ids)",
      "getSharedPreferences" not in room and "FileOutputStream" not in room and "FileOutputStream" not in srv and "putString(\"cat\"" in bank and "name" not in re.findall(r'put\w+\("(\w+)"', bank))
check("it is its own thing: not the remote-control pairing (no Pairing, PhoneBridge or ConfigProvider in the quiz), opened from the hub's «الألعاب» window",
      not re.search(r"Pairing|PhoneBridge|ConfigProvider|SaverReceiver", act + srv + room) and "QuizGamesActivity.class" in hub and "GAMES_READY = true" in hub)
check("both activities are in the manifest, not exported, landscape", 'android:name=".QuizActivity"' in manifest and 'android:name=".QuizGamesActivity"' in manifest and manifest.count('android:exported="false"') >= 3)
check("low-RAM TVs and reduced motion: no animation, no confetti (isLowRamDevice, the animator scale)",
      "isLowRamDevice()" in act and "ANIMATOR_DURATION_SCALE" in act and "animate = !lowRam && anim > 0f" in act and "if (animate && !lowRam) root.addView(new ConfettiView" in act)
check("the quiz's QR encoder is the launcher's own, unchanged but for the package line",
      (ROOT / "android/launcher/src/main/java/sa/urtv/home/phone/QrCode.java").read_text(encoding="utf-8").split("\n", 1)[1] == (APP / "QrCode.java").read_text(encoding="utf-8").split("\n", 1)[1])
check("no «·» as a separator in what it shows", "·" not in re.sub(r"/\*.*?\*/|//[^\n]*", "", act + (APP / "QuizGamesActivity.java").read_text(encoding="utf-8"), flags=re.S))
check("the sounds are made from tones in the code, no audio file (our own: no melody of any show)", "Math.sin(" in (APP / "QuizSound.java").read_text(encoding="utf-8") and not list((ROOT / "assets").glob("**/*.mp3")))
check("the starter bank ships in assets/quiz and has at least 40 items of each level and 40 for kids (so the first game works offline, mix with any chips)",
      (lambda d: all(sum(1 for x in d if x["d"] == lv) >= 40 for lv in (1, 2, 3)) and sum(x["k"] for x in d) >= 40 and all(len(x["w"]) == 3 for x in d))(__import__("json").load(open(ROOT / "assets/quiz/starter.json", encoding="utf-8"))))
# ---- step 48: what the real-TV test found
page = (ROOT / "assets/quiz/play.html").read_text(encoding="utf-8")
check("lobby: the address is the whole host:port/code, left-to-right isolated (U+2066 ... U+2069), big (46) and on one line; the hint says «ابدأ» once two are in",
      '"\\u2066" + url.replace("http://", "") + "\\u2069", 46' in act and "TEXT_DIRECTION_LTR" in act.split("the whole address")[1][:500] and "at(700, 64, 96, 862)" in act
      and 'can ? S.t("ابدأ") : S.t("ابدأ (لاعبان على الأقل)"), S.t("رجوع"), S.t("أغلق الغرفة")' in act)
check("names are cut with «…» by measuring (lobby tiles, podium), not left to the view", "private String clip(" in act and 'cut + "…"' in act and act.count("clip(p.name") == 2)
check("reveal: the four answers end at y 942 (546 + 2 x 190 + 16), above the hint line at 978; the colour squares are drawn at once at full size (no scale-in from 0); no «·»",
      "at(846, 190, 96 + (i % 2) * (846 + 36), 546 + (i / 2) * (190 + 16))" in act and "setScaleX(0)" not in act and "px(48), px(48)" in act and 'at(-1, 48, 96, 978)' in act)
check("reveal 4 s + the top five 4 s (REVEAL_MS 8000, REVEAL_VIEW_MS 4000)", "REVEAL_MS = 8000" in room and "REVEAL_VIEW_MS = 4000" in act)
check("podium: the winner is the tallest and in the middle (heights 512 / 400 / 300 for 1st / 2nd / 3rd), the blocks stand on y 960 inside the safe area, above the hint",
      "float[] hs = {512, 400, 300}; int[] place = {1, 0, 2};" in act and "cp.bottomMargin = px(120)" in act and 96 + 512 + 24 <= 960 - 352 + 96)
check("digits: the duplicate-name number and the bank's question and four answers follow the person's setting (Latin: 0-9) on the TV and the phones",
      "room.latinDigits = latin;" in act and "private QuizRoom.Q digits(" in act and "qs.add(digits(QuizPick.toQ(it, en, rnd)))" in act and "latinDigits" in room and "ar && !latinDigits" in room)
check("board: the gain is «+591» left-to-right (isolated); the last question's hint says «النتيجة النهائية»; no ▲/▼ for a player on 0",
      '"\\u2066" + signed(p.lastPoints) + "\\u2069"' in act and 'room.index + 1 >= room.total() ? S.t("النتيجة النهائية")' in act and 'p.score > 0 && p.lastDelta > 0 ? "▲"' in act)
check("phone state: the rank is among the players still in (rank 3 of 3, never 4 of 3), in the state, the reveal and the end", room.count("activeRank(p)") == 3 and "int activeRank(Player p)" in room)
check("setup: standing on the picked chip looks different from standing on another one (saffron words and frame, 12 px)", "focus && isSel ? SAFF : off" in act and "focus && isSel ? 12 : 6" in act)
check("HOME in the middle of a game pauses it (the phones say «اللعبة متوقفة»); the screen stays on from every screen, the podium included, and after every return",
      "room.pause();" in act.split("protected void onPause()")[1][:400] and act.count("FLAG_KEEP_SCREEN_ON") >= 3)
check("the phone: the join page offers ع / EN (reloads with ?l=), answers carry the nonce x", "function langSwitch()" in page and "langSwitch()]);" in page and "x: q.x || ''" in page)
check("server: a locked address gets a page or a 429 JSON, the body is read first, a busy or flooding connection is told 429, never cut silently",
      "LOCKED_PAGE" in srv and "reply(c, 429," in srv and "private static void drain(" in srv and "busy(s)" in srv)
# ---- step 52
check("drawing: the answer letters are drawn on the glyph's ink bounds (InkLetter: getTextBounds, baseline = box/2 - (top+bottom)/2), not centred by the font's line box; the rank box stands inside the row's 6 px line",
      "static final class InkLetter extends View" in act and "static float baseline(float boxH, int inkTop, int inkBottom) { return boxH / 2f - (inkTop + inkBottom) / 2f; }" in act
      and "new InkLetter(this, letter(i), 64 * scale" in act and "row.setPadding(px(6), px(6), px(6), px(6));" in act and "px(128 - 6), px(height - 12)" in act)
check("end: «لا فائز هذه المرة» when the best has 0, a tie for first is said; «جاوب الجميع» when the question ended early; places are the shared ones (p.rank)",
      'r.get(0).score <= 0 ? S.t("لا فائز هذه المرة")' in act and 'S.t("تعادل في المركز الأول")' in act and 'room.endedEarly ? S.t("جاوب الجميع")' in act and "playerRow(p, p.rank, true, 128)" in act and "num(p.rank)" in act)
check("phone: «gone» keeps the token and says «خرجت من الغرفة، ادخل من جديد»; the room code is 0-9 in the Arabic page; sums are isolated by the TV before they reach the phone",
      "e === 'gone'" in page and "e_gone: 'خرجت من الغرفة، ادخل من جديد'" in page and "function roomLabel()" in page and "QuizRoom.isolateMath(" in act)
check("docs/QUIZ.md exists and names the conflicts between the SPEC and the mockups", "conflicts" in doc.lower() and "SPEC" in doc)
sys.exit(1 if failed else 0)
