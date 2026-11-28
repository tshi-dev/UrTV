#!/usr/bin/env python3
"""«طريقة اللعب» (docs/GAMES-KIT.md): the small «!» on a game's start screen and the panel behind it. The rules of the texts and of the «!» focus run on a plain JVM (tests/HowToTest.java);
the pins keep the Android side and the three places a text lives (the registry's HOWTO table, saver.tsv, Words.java) and docs/mockups/howto/TEXTS.md the same."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
sys.path.insert(0, str(ROOT / "i18n")); import tr
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(n): return (APP / n).read_text(encoding="utf-8")
def code(s): return re.sub(r"/\*.*?\*/|//[^\n]*", "", s, flags=re.S)
if not shutil.which("javac"): print("SKIP: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as t:
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", t, *(str(APP / f"{n}.java") for n in ("GameHowTo", "GameRegistry", "GameChips", "MiniJson")), str(ROOT / "tests/HowToTest.java")], capture_output=True, text=True)
    check("the how-to texts and the «!» focus compile on a plain JVM (no Android)", c.returncode == 0, c.stderr[-600:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", t, "sa.urtv.screensaver.HowToTest"], capture_output=True, text=True, timeout=120)
        for l in r.stdout.splitlines():
            if l.startswith(("PASS", "FAIL")): print(l); failed += l.startswith("FAIL")
        check("the JVM test ran to the end", r.returncode == 0 and "DONE" in r.stdout, r.stdout[-300:] + r.stderr[-300:])
for n in ("GameHowTo",): check(f"{n}: no Android, no S, no Prefs", not re.search(r"\bimport android\.|\bS\.t\(|\bS\.n\(|\bPrefs\b|Context\b", code(src(n + ".java"))))
# ---- the three places a text lives, and the review file
reg, words, table = src("GameRegistry.java"), src("Words.java"), tr.load()
rows = re.findall(r"\n        \{((?:\"[^\"]*\"(?:, )?)+)\},", reg.split("HOWTO = {")[1].split("    };")[0])
texts = [[x for x in re.findall(r'"([^"]*)"', r)] for r in rows]
flat = [x for r in texts for x in r[1:] if x]
marked = {ar for _, ar in tr.java_literals(words, "Words.java")}
heads = ["طريقة اللعب", "هدف اللعبة", "كيف تلعب", "بالجوالات", "النقاط", "النهاية", "تصفّح", "إغلاق"]
check("every text of the table and every heading has its English in i18n/en/saver.tsv (no Arabic letter in it) and is marked once in Words.java",
      all(x in table and x in marked and not re.search("[؀-ۿ]", table[x]) for x in flat + heads), [x for x in flat + heads if x not in table or x not in marked or re.search("[؀-ۿ]", table.get(x, ""))][:3])
review = (ROOT / "docs/mockups/howto/TEXTS.md").read_text(encoding="utf-8")
secs = re.split(r"\n## ", review)[1:]
def ar_of(line): return line.rsplit(" / ", 1)[0].strip()
same = len(secs) == len(texts) == 19
for sec, row in zip(secs, texts):
    got = []
    for l in sec.split("\n")[1:]:
        m = re.match(r"\*\*([^*]+)\*\*\s*(.*)", l)
        if m and m.group(2): got.append((m.group(1), ar_of(m.group(2))))
        elif m: pass
        elif l.strip(): got.append(("step", ar_of(re.sub(r"^\d+\.\s*", "", l))))
    byname = {}
    for k, v in got: byname.setdefault(k, []).append(v)
    want = [byname["الهدف"][0], (byname.get("بالجوالات") or [""])[0], byname["النقاط"][0], byname["النهاية"][0]] + byname["step"]
    if row[1:] != want: same = False; print("   differs:", row[0])
check("the registry's table says exactly what docs/mockups/howto/TEXTS.md (the reviewed texts) says, game by game", same)
# ---- the Android side
base, view, quiz, chips = src("GameActivity.java"), src("GameHowToView.java"), src("QuizActivity.java"), src("GameChips.java")
check("GameActivity: the «!» is a small 48 x 48 mark at the end corner (a saffron ring with the focus), the small line makes room (80), up from the first row reaches it, OK opens the panel",
      "atEnd(48, 48, 96, 70)" in base and "atEnd(64, 64, 88, 62)" in base and "titleInset = (c.hasHelp ? 80 : 0) + 168" in base and "if (c.help) { openHowTo(); return false; }" in base and "if (dy < 0 && step(row, -1) == row && hasHelp) { help = true; return true; }" in chips
      and "focus = !c.help && c.row == r" in base and "c.onButton() && c.btn == 0 && !c.help" in base)
check("GameActivity: Menu opens the panel (and closes it), a round is paused first (the clocks stop), a secret on the screen or the names keep it closed; Back closes it, ▲ ▼ scroll, no other key reaches the game; no tick runs while it is open",
      "k == KeyEvent.KEYCODE_MENU" in base and "if (inPlay() && !pauseCard) openPause(); openHowTo();" in base and "namesMode == 0 && hand == 0" in base and "if (back) closeHowTo(); else if (dy != 0 && howtoView != null) howtoView.scroll(dy); return true;" in base
      and "if (pauseCard || howtoOpen) return;" in base and "if (howtoOpen) { showHowTo(); return; }" in base)
check("GameActivity: a game can give its own content (howtoContent(), for a game whose rules are in its data file: GameHowTo.fromJson), the registry's row is the default",
      "protected GameHowTo.Content howtoContent() { return GameHowTo.of(GameRegistry.howto(id())); }" in base)
check("«الأسرع»: the «!» in the setup (up from the first row, OK opens, Menu opens), Back or Menu closes it, ▲ ▼ scroll, the row of the room's screens never gets it (they refresh by themselves)",
      'GameHowTo.of(GameRegistry.howto("quiz"))' in quiz and "if (selRow < 0) { if (ok) openHowTo();" in quiz and "if (dy < 0 && selRow == 0) { selRow = -1;" in quiz and "if (k == KeyEvent.KEYCODE_MENU) { if (screen == Screen.SETUP) openHowTo(); return true; }" in quiz
      and "howtoView.scroll(-1)" in quiz and "howtoView.scroll(1)" in quiz and "atEnd(48, 48, 96, 70)" in quiz)
check("the panel: the title band, the goal and the steps at the start edge, the phones, the points and the end in boxes at the other, a ScrollView with ▲ ▼ cues; body text 36 design px, 42 on a 720p screen (28 real), and up to 1.3 more with the system font; the games' colours only; no «·»",
      "new ScrollView(c)" in view and "width <= 1400 ? 42f / 36f : 1f" in view and "Math.min(1.3f, fs)" in view and "canScrollVertically(1)" in view and "canScrollVertically(-1)" in view and 'S.tv(ar)' in view and "DigitsText.convert(v, latin)" in view
      and not re.search(r"0x[0-9A-Fa-f]{8}|Color\.parseColor", code(view)) and not re.search("[·•]", code(view)) and "setCornerRadius" not in view)
check("the docs say it", "طريقة اللعب" in (ROOT / "docs/GAMES-KIT.md").read_text(encoding="utf-8") and "howtoContent" in (ROOT / "docs/GAMES-KIT.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
