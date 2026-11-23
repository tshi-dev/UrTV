#!/usr/bin/env python3
"""4.4 beta 3: «رجوع مرتين يفتح شاشة التوقف» (the owner promised it to a user; on by default): two Backs within a second on our home start the
screensaver; a single Back does what it always did; never in kids mode, never inside a dialog (a dialog takes Back itself), never without the
screensaver app. And the way back to the home (users of 4.3.6: «البطاقات تصير صورة سوداء وترجع»): the cards' last frame stays in their place
until the page has drawn again, never a black or empty box. HomeBack.twice runs on a JVM; the wiring is read from the source.
    python3 tests/back_twice_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L = ROOT / "android/launcher/src/main/java/sa/urtv/home"
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")
main, snap, prefs, st, recv, hub, card = (read(L / "MainActivity.java"), read(L / "ConfigSnapshot.java"), read(A / "Prefs.java"), read(A / "SettingsActivity.java"),
                                          read(A / "SaverReceiver.java"), read(A / "SettingsHub.java"), read(L / "HomeCard.java"))
ROW = "رجوع مرتين يفتح شاشة التوقف"

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(L / "HomeBack.java"), str(ROOT / "tests/BackTwiceRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.BackTwiceRun"], check=True, capture_output=True).stdout.decode().strip().split("\n")
    check("HomeBack.twice: a second Back within a second only; never a single Back, kids mode, the setting off, another home, the scrolled grid or no screensaver",
          len(out) == 10 and all(l.startswith("ok ") for l in out), [l for l in out if not l.startswith("ok ")])
else:
    print("SKIP the rule: no javac here")
back = main[main.index("    public void onBackPressed() {"):main.index("    private long lastBack;")]
check("the home: only a Back that did nothing else counts (after the nudge, the grid, leaving and the single-Back screensaver); the second opens the screensaver behind its own lock",
      "HomeBack.twice(backTwice, now, lastBack, true, false, Parental.state(this).kids, isInstalled(SAVER_PKG))" in back
      and 'guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", this::startSaver);' in back and "} else lastBack = now;" in back
      and back.index("HomeBack.startsSaver(") < back.index("HomeBack.twice("))
check("the home reads it with the settings (on unless the screensaver says off; an older one says nothing: on)",
      "backTwice = cfg.backTwice();" in main and 'return fragment == null || !(fragment.contains("&backtwice=0")' in snap)
check("the screensaver: on by default, sent only when off, a row of «الأزرار السريعة», saver.sh backtwice on|off and `show`",
      'sp.getBoolean("backTwice", true)' in prefs and '(backTwice() ? "" : "&backtwice=0")' in prefs and f'row("{ROW}", prefs.backTwice() ? "مفعّل" : "متوقف"' in st
      and 'if (backTwice != null) p.setBackTwice("on".equals(backTwice));' in recv and '"\\nbacktwice: "' in recv
      and "backtwice) set1 backtwice" in read(ROOT / "optimize/saver.sh") and hub.count(ROW) == 5)
check("its words have their English and the docs say it", f"{ROW}\t" in read(ROOT / "i18n/en/saver.tsv") and ROW in read(ROOT / "docs/SETTINGS.md"))
pause = card[card.index("    void pause() {"):card.index("    void destroy() {")]
res = card[card.index("    private void resumeWeb() {"):card.index("    private android.widget.ImageView still;")]
check("the cards on the way back: the last frame is kept as an app comes in front, in the page's box, and dropped once the page has drawn again (or at once, or with the page)",
      "keepStill();" in pause and pause.index("keepStill();") < pause.index("web.onPause();") and "web.draw(new android.graphics.Canvas(b));" in card
      and "parent.addView(still, parent.indexOfChild(web)" in card and res.count("dropStill();") == 3 and "        dropStill();\n        parent.removeView(web);" in card
      and "catch (RuntimeException | OutOfMemoryError e)" in card[card.index("private void keepStill()"):])
sys.exit(1 if failed else 0)
