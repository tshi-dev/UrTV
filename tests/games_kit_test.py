#!/usr/bin/env python3
"""The family games' kit (docs/GAMES-KIT.md) and the games built on it: the pure-Java parts run on a plain JVM (tests/GamesKitTest.java: the registry, the
score table, the setup chips, the clock, the data checks, and each game's rules), then the pins that keep the Android side to what the doc says.
A game's session adds its engine to ENGINES, its checks to GamesKitTest (or its own test) and its pins at the end. Needs javac (exit 1 without, so CI notices)."""
import json, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
sys.path.insert(0, str(ROOT / "i18n"))
import tr
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(name): return (APP / name).read_text(encoding="utf-8")
def code(s): return re.sub(r"/\*.*?\*/|//[^\n]*", "", s, flags=re.S)

KIT = ["GameRegistry", "GameScore", "GameChips", "GameClock", "GameDataCheck"]   # no Android in these
ENGINES = ["KhanatData", "KhanatEngine", "MissingWord"]                           # each game's rules and data: no Android either
if not shutil.which("javac"):
    print("SKIP games kit: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-d", tmp, *(str(APP / f"{n}.java") for n in KIT + ENGINES), str(APP / "MiniJson.java"), str(ROOT / "tests/GamesKitTest.java")], capture_output=True, text=True)
    check("the kit's rules and every game's engine compile on a plain JVM (no Android in them)", c.returncode == 0, c.stderr[-800:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.GamesKitTest", str(ROOT / "assets/games/khanat.json"), str(ROOT / "assets/games/missing-word.json")], capture_output=True, text=True, timeout=120)
        ran = 0
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line); ran += 1
                if line.startswith("FAIL"): failed += 1
        check("the JVM test ran to the end", ran > 40 and (r.returncode == 0 or "FAIL" in r.stdout), r.stderr[-600:])
for n in KIT + ENGINES:
    check(f"{n}: no Android, no S, no Prefs (the phone mode will drive the same class)", not re.search(r"\bimport android\.|\bS\.t\(|\bS\.n\(|\bPrefs\b|Context\b", code(src(n + ".java"))))

# ---- the registry and the games list
reg = src("GameRegistry.java"); hub = src("QuizGamesActivity.java"); base = src("GameActivity.java"); manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
games = re.findall(r'new Game\("([\w-]+)", "([^"]+)", "([^"]+)", "(\w+)", (true|false), (true|false)\)', reg)
table = tr.load()
check("the registry lists the eight games, one line each", [g[0] for g in games] == ["quiz", "khanat", "missing-word", "odd-one", "charades", "letter-hive", "word5", "domino"], [g[0] for g in games])
words = [ar for _, ar in tr.java_literals(hub.split("WORDS = {")[1].split("};")[0])]
check("every name and line of the registry is marked for the English table in the games list (WORDS), in order, and has its English", words == [w for g in games for w in (g[1], g[2])] and all(w in table for w in words), [w for w in words if w not in table])
for gid, name, line, act, phones, ready in games:
    if ready == "true":
        check(f"{gid}: ready, so its activity {act} exists, is in the manifest, not exported, landscape", (APP / f"{act}.java").is_file() and re.search(r'android:name="\.%s"\s+android:exported="false"\s+android:screenOrientation="landscape"' % act, manifest) is not None)
        if phones == "false":
            check(f"{gid}: played with one remote: it extends GameActivity and opens no server and no socket", f"class {act} extends GameActivity" in src(f"{act}.java") and not re.search(r"ServerSocket|QuizServer|QuizRoom|java\.net\.", src(f"{act}.java")))
check("the list reads the registry: tiles four to a line, «قريبًا» for a game that is not ready, «بالجوالات» only for a game that needs phones, the default way said in the header",
      "GameRegistry.ALL" in hub and "COLS = 4" in hub and '!live ? S.t("قريبًا") : g.phones ? S.t("بالجوالات") : ""' in hub and 'S.t("بدون جوالات: ريموت واحد يدور بينكم")' in hub and "GameRegistry.firstFocus(GamePrefs.last(this))" in hub)
check("the list opens a ready game by its registry line, and a game whose screen is missing says «قريبًا» instead of stopping the app", 'setClassName(this, getClass().getPackage().getName() + "." + g.activity)' in hub and "catch (RuntimeException notThere)" in hub)
shown = code(hub + base + "".join(src(f"{g[3]}.java") for g in games if g[5] == "true" and g[4] == "false"))
check("no «·» or «•» separator in what the games show, no rounded (pill) shape", not re.search("[·•]", shown) and "setCornerRadius" not in shown and "setCornerRadii" not in shown)

# ---- the base activity: what every game gets
look = src("GameLook.java"); data = src("GameData.java"); prefs = src("GamePrefs.java")
check("the screen stays on while a game is open (the window flag, again after every return)", base.count("FLAG_KEEP_SCREEN_ON") >= 2)
check("Back is the game's to answer with the pause card (متابعة، إنهاء الجولة، الرئيسية); the card stops the clocks; HOME in a round leaves the card up",
      'S.t("متابعة"), S.t("إنهاء الجولة"), S.t("الرئيسية")' in base and "onPauseChanged(true, System.currentTimeMillis());" in base and "if (inPlay() && !pauseCard && hand == 0) openPause();" in base)
check("the hand-over: pass screen, a 2 s count, the secret alone; Back hides it first; it hides by itself after 20 s and when the app leaves the front",
      "HAND_HIDE_MS = 20000, HAND_COUNT_MS = 2000" in base and 'S.t("سلّموا الريموت لـ %s", handName)' in base and 'S.t("الباقين: غطّوا عيونكم")' in base and "if (back) { hand = 1; redraw(); }" in base
      and "if (now - handAt >= HAND_HIDE_MS) { hand = 1; redraw(); }" in base and "if (hand > 1) { hand = 1; handCount = null; }" in base.split("protected void onPause()")[1][:300])
check("low-RAM TVs and reduced motion: no animation; a mid TV keeps the small moves and drops the confetti (Prefs.ramTier, the animator scale)",
      "tier = Prefs.ramTier(this);" in base and "ANIMATOR_DURATION_SCALE" in base and "animate = tier != Prefs.RAM_LOW && anim > 0f;" in base and "confetti = animate && tier == Prefs.RAM_NORMAL;" in base)
check("language, direction and digits follow UrTV's settings (S, Dir, DigitsText); the arrows are turned for Arabic once, in the base", "latin = DigitsText.isLatin(prefs.digits()); en = S.en(); rtl = Dir.rtl();" in base and "if (rtl) dx = -dx;" in base and "root.setLayoutDirection(Dir.layout());" in base)
check("the setup opens with the start button focused (a round starts with one OK), and a picked chip is remembered", "row = rows.size(); return this;" in src("GameChips.java") and "GamePrefs.saveChips(this, id(), c.save());" in base)
check("«إنهاء الجولة» while a hand-over is up drops it (cancelHandOver, also for a game to call); ten sides fit: ten colours, ten names, rank rows of 64 from the ninth",
      "pauseCard = false; cancelHandOver(); onPauseChanged(false" in base and "protected final void cancelHandOver() { hand = 0; handDone = null;" in base and "static final int MAX = 10;" in src("GameScore.java")
      and len(re.findall(r"0xFF[0-9A-F]{6}", look.split("SIDES = {")[1].split("}")[0])) == 10 and "tiny ? 72 : small ? 90 : 142" in base and 236 + 9 * 72 + 64 <= 960 and 236 + 7 * 90 + 80 <= 960)
check("the sounds are the existing tones (QuizSound), behind the one sound switch", "QuizSound.play(sound, which)" in base and "AudioTrack" not in base)
check("one look, two sets (PAPER, NIGHT), decided in one place; a game writes no colour of its own",
      "static final GameLook PAPER" in look and "NIGHT = new GameLook(true" in look and "static GameLook of(Context c)" in look
      and all(not re.search(r"0xFF[0-9A-Fa-f]{6}", code(src(f"{g[3]}.java"))) for g in games if g[5] == "true" and g[4] == "false"))
check("data: the kept copy must pass the published check, the starter its own; a download goes through Net with a size cap, is checked whole, written beside and renamed, the last good copy stays",
      "published.parse(Bounded.readAll(in, GameDataCheck.BYTES_MAX))" in data and "starter.parse(Bounded.readAll(in, GameDataCheck.BYTES_MAX))" in data and "Net.get(BASE + name + \".json\", null, 15000, 20000, Math.min(maxBytes, GameDataCheck.BYTES_MAX))" in data
      and "r.status != 200" in data and "if (published.parse(b) == null) return;" in data and "tmp.renameTo(f)" in data and "data-games/games/" in data and "org.json" not in data)
check("nothing about the people playing is stored: only chips, the last game, the sound switch and item ids", set(re.findall(r'put\w+\("([\w.]+)"', prefs)) == {"chips.", "last", "sound", "recent."} and "FileOutputStream" not in base)
lim = json.loads(json.dumps({"khanat": 200_000, "missing-word": 300_000}))
check("the app's limits are docs/GAMES-DATA.md's: khanat 200 KB, 200 to 4,000 samples of 24; missing-word 300 KB, 100 to 5,000 items, 90 characters",
      "BYTES_MAX = 200_000, SAMPLES_MAX = 4000, SAMPLE_LEN = 24, PUBLISHED_MIN = 200" in src("KhanatData.java") and "BYTES_MAX = 300_000, ITEMS_MAX = 5000, PUBLISHED_MIN = 100" in src("MissingWord.java") and "TEXT_LEN = 90" in src("MissingWord.java"))
doc = (ROOT / "docs/GAMES-KIT.md").read_text(encoding="utf-8") if (ROOT / "docs/GAMES-KIT.md").exists() else ""
check("docs/GAMES-KIT.md says how to add a game and names every kit class", all(n in doc for n in KIT + ["GameActivity", "GameLook", "GameData", "GamePrefs", "How to add a game"]))
run = (ROOT / "tests/run.sh").read_text(encoding="utf-8")
check("tests/run.sh runs the quiz's and the games' tests", all(f"tests/{n}" in run for n in ("quiz_test.py", "quiz_phone.test.mjs", "quiz_e2e.test.mjs", "games_kit_test.py", "games_data_test.py")))
sys.exit(1 if failed else 0)
