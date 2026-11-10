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
check("the app checks what it downloads before it keeps it: size cap, no redirects, the whole-file check, a temporary file renamed, the last good copy kept; no org.json in the quiz",
      "QuizBankCheck.parse(b)" in bank_src and "setInstanceFollowRedirects(false)" in bank_src and "getContentLengthLong() > QuizBankCheck.BYTES_MAX" in bank_src and "tmp.renameTo(f)" in bank_src
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
check("docs/QUIZ.md exists and names the conflicts between the SPEC and the mockups", "conflicts" in doc.lower() and "SPEC" in doc)
sys.exit(1 if failed else 0)
