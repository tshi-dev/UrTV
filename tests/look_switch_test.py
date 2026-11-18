#!/usr/bin/env python3
"""4.5 switching looks (docs/ui/PROPOSAL.md, approved «with a protective way»): one write and one signal, the home applies a look whole behind a
still picture with one fade (a cut in «الوضع الخفيف»), a failed change goes back to the saved look with no blank frame, the cards' page never
shows as a black box, the focus stays on the same app, and «رجّع الشكل السابق» puts the look before back. Source rules, and LastGood on a JVM.
Run: python3 tests/look_switch_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def body(src, head):
    """the method that starts with head, to its closing brace at the same indent"""
    i = src.index(head)
    return src[i:src.index("\n    }\n", i)]


prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
provider = (APP / "ConfigProvider.java").read_text(encoding="utf-8")
st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
recv = (APP / "SaverReceiver.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
look = (HOME / "HomeLook.java").read_text(encoding="utf-8")
card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
snap = (HOME / "ConfigSnapshot.java").read_text(encoding="utf-8")
app_man = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
home_man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

# ---- one write, one signal
for head in ("    void setPreset(String key) {", "    void setCombo(String key) {", "    void setLookPart(String field, String value) {", "    void restorePreviousLook() {"):
    b = body(prefs, head)
    check(f"{head.strip()[:-2]}: written under LOOK_LOCK with the look before kept, then the home told once",
          "synchronized (LOOK_LOCK)" in b and ("rememberLook();" in b or "lookPut(prev);" in b) and b.rstrip().endswith("lookChanged();"), b[-200:])
q = provider[provider.index("public Cursor query("):]
check("ConfigProvider reads the look under the same lock: never half a look", "synchronized (Prefs.LOOK_LOCK) {" in q and q.index("synchronized (Prefs.LOOK_LOCK)") < q.index('"fragment", "home_panel"'))
lc = body(prefs, "    void lookChanged() {")
check("the signal goes to the home app only, under the signature permission sa.urtv.home.LOOK; an older home simply reads at its next minute",
      'setPackage("sa.urtv.home")' in lc and "LOOK_PERMISSION" in lc and 'LOOK_PERMISSION = "sa.urtv.home.LOOK"' in prefs)
check("sa.urtv.home.LOOK is a signature permission declared by both apps (as READ_GLASS), and the screensaver holds it",
      all(re.search(r'<permission\s+android:name="sa\.urtv\.home\.LOOK"\s+android:protectionLevel="signature"', m) for m in (app_man, home_man))
      and '<uses-permission android:name="sa.urtv.home.LOOK" />' in app_man)
check("the home's receiver asks for that permission (exported on 13+ only that way) and is let go in onDestroy",
      "registerReceiver(lookChanged, lf, LOOK_PERMISSION, null, Context.RECEIVER_EXPORTED)" in main and "registerReceiver(lookChanged, lf, LOOK_PERMISSION, null);" in main
      and "unregisterReceiver(lookChanged)" in main)
check("on the signal the home reads fresh (ConfigSnapshot.expire, not the 3-second copy), off the UI thread, then shows it",
      re.search(r"private void lookChangedNow\(\) \{.*?ConfigSnapshot\.expire\(\);.*clockReads\.execute\(.*ConfigSnapshot\.read\(app\).*runOnUiThread\(.*showLook\(got\)", main, re.S) is not None
      and "static void expire() { LAST.expire(); }" in snap)
check("every look row of the settings writes through setLookPart (the layout, the background, the clock, the apps' way) and leaving the settings tells the home",
      all(f'prefs.setLookPart("{f}"' in st for f in ("layout", "bg", "clock", "apps")) and re.search(r"protected void onPause\(\) \{\s*super\.onPause\(\);\s*prefs\.lookChanged\(\);", st))

# ---- the home: whole, behind a still picture, one fade
sl = body(main, "    private void showLook(final ConfigSnapshot cfg) {")
check("not on the screen (the settings in front): applied at once, so the return's first frame is the new look; on it: behind HomeLook's cover",
      "if (!shown || homeLook == null) { apply.run(); return; }" in sl and "homeLook.change(apply," in sl
      and re.search(r"protected void onStart\(\) \{\s*super\.onStart\(\);\s*shown = true;", main) and re.search(r"protected void onStop\(\) \{\s*shown = false;\s*if \(homeLook != null\) homeLook\.finishNow\(\);", main))
check("the focus stays on the same app (by its package, not its place in the row)", "final String pkg = focusedPkg();" in sl and "focusPkgAfter = pkg; applyPendingFocus();" in sl)
check("a change that throws: the look shown before is applied again, still behind the cover (no blank frame)",
      "() -> { if (before != null) updateClock(before); }" in sl and "catch (RuntimeException e)" in look and "undo.run()" in look)
ch = body(look, "    void change(final Runnable apply, final Runnable undo, final Ready ready) {")
check("the cover is on the screen before the change is made, and goes only once the new state has drawn (at most WAIT_MS), after one more frame",
      ch.index("root.addView(cover") < ch.index("run(apply, undo);\n        final long start") and "t >= MIN_MS && safe(ready)" in ch and "t < WAIT_MS" in ch and "AfterFrame.run(root, HomeLook.this::reveal)" in ch)
check("one fade of 250 ms on Motion's curve; «الوضع الخفيف» (Ram.low) a cut; the picture half size, recycled after",
      "FADE_MS = 250" in look and "setInterpolator(Motion.ease())" in look and "if (Ram.low(activity)) { done(c); return; }" in look
      and "getWidth() / 2" in look and "shot.recycle()" in look)
check("a change while one is under way waits for it (the last one wins), and a screen left with a cover up ends it at once",
      "if (busy()) { queued = () -> change(apply, undo, ready); return; }" in look and "void finishNow()" in look)
check("the cover waits for the apps' row (the one asked last is shown) and the cards: their url read and in place, the page drawn (HomeCard.settled)",
      "() -> appsSettled() && (homeCard == null || homeCard.settled())" in sl and "appsShownGen = gen;" in main and "private boolean appsSettled() { return appsShownGen == appsGen; }" in main
      and "boolean settled() { return appliedGen == refreshGen && !loading && (web == null || web.getAlpha() >= 1f); }" in card)
au = body(card, "    private void applyUrl(String want) {")
check("a new cards' page is hidden until its first frame is on the screen (onPageCommitVisible), at most WAIT_MS: never a black box",
      "w.setAlpha(0f);" in au and "HomeLook.WAIT_MS" in au and "public void onPageCommitVisible(WebView v, String u) { if (v.getAlpha() < 1f) v.setAlpha(1f); }" in card)

# ---- «رجّع الشكل السابق»
check("«رجّع الشكل السابق»: the first row of «المظهر», only when there is a look before that is not this one; it puts that look back",
      re.search(r'if \(prefs\.hasPreviousLook\(\)\) row\("رجّع الشكل السابق", prefs\.previousLookName\(\)', st)
      and st.index('row("رجّع الشكل السابق"') < st.index('row("أشكال جاهزة"') and "prefs.restorePreviousLook(); render();" in st)
rp = body(prefs, "    void restorePreviousLook() {")
check("…whole (its fields and which look, mix and own values it was), and the look it replaced becomes the look before (pressed again, it comes back)",
      "String now = lookWhole();" in rp and "lookPut(prev);" in rp and 'putString("lookPrev", now)' in rp
      and all(k in prefs for k in ('"preset", "presetCombo", "presetBefore", "presetApplied"',)))
check("…and from the computer: optimize/saver.sh lookback (SaverReceiver's lookback=restore)", 'if ("restore".equals(i.getStringExtra("lookback"))) p.restorePreviousLook();' in recv
      and "lookback)   set1 lookback restore" in (ROOT / "optimize/saver.sh").read_text(encoding="utf-8"))

# ---- «المظهر» over the live home (LookPanelActivity, in the screensaver app) and the home's side of it
panel = (APP / "LookPanelActivity.java").read_text(encoding="utf-8")
pv = body(panel, "    private void preview(String[] p) {")
check("the preview is never saved: it is a broadcast to the home with the candidate's parts, no Prefs write in it", "sendBroadcast(i, Prefs.LOOK_PERMISSION)" in pv
      and "prefs." not in pv and "lookOf" in panel and "String[] lookOf(String key)" in prefs and "sp.edit()" not in body(prefs, "    String[] lookOf(String key) {"))
check("a look or a part is shown only once the focus rests on it (REST_MS = 330, a passing focus shows nothing), and only OK keeps it",
      "REST_MS = 330" in panel and "ui.postDelayed(r, REST_MS)" in panel and "rest(() -> preview(prefs.lookOf(key)))" in panel
      and "prefs.setPreset(key)" in panel and "prefs.setLookPart(PARTS[part], value)" in panel)
dk = body(panel, "    public boolean dispatchKeyEvent(KeyEvent e) {")
check("Back always puts the saved look back (the preview ended first), closes the open choices, then the panel",
      dk.index("previewOff();") < dk.index("closeChoices()") and "finish();" in dk)
check("leaving the panel any way (Back, Home, another app) ends the preview and gives the home back its size (onStop)",
      re.search(r"protected void onStop\(\) \{ ui\.removeCallbacksAndMessages\(null\); previewOff\(\); panel\(false\);", panel) is not None)
check("the choices open in place with the focus on the current value; the panel is see-through, at the end side, guarded by sa.urtv.home.LOOK",
      "if (value.equals(saved)) focus = c;" in panel and "Gravity.LEFT : Gravity.RIGHT" in panel
      and re.search(r'android:name="\.LookPanelActivity"[^>]*android:permission="sa\.urtv\.home\.LOOK"[^>]*Theme\.Translucent', app_man, re.S))
check("the home: the preview laid over every read of the saved look and over the cards' choice, never written; a kept look (LOOK_CHANGED) ends it",
      "if (lookPreview != null && cfg != null) cfg = cfg.withLook(" in main and "homeCard.preview(p[0], p[5]);" in main
      and re.search(r"private void lookChangedNow\(\) \{\s*lookPreview = null;", main) and "void preview(String layout, String cards)" in card)
check("the home moves aside while the panel is open (one transform of its root, Motion's curve, a cut when light) and back when it closes, with the saved look",
      "root.animate().scaleX(s).scaleY(s).setDuration(Motion.MS).setInterpolator(Motion.ease())" in main and "if (Ram.low(this)) { root.setScaleX(s); root.setScaleY(s); return; }" in main
      and 'aside(i.getBooleanExtra("open", false)); if (!i.getBooleanExtra("open", false)) previewEnd();' in main)
check("its receiver asks for the same permission", "registerReceiver(lookPanel, pf2, LOOK_PERMISSION, null, Context.RECEIVER_EXPORTED)" in main and "unregisterReceiver(lookPanel)" in main)
check("the settings open it from the first row of «المظهر» («المظهر على الرئيسية»)", 'row("المظهر على الرئيسية"' in st and "LookPanelActivity.open(SettingsActivity.this)" in st)

# ---- LastGood.expire on a JVM: the next read asks again; the answer is kept for peek
if shutil.which("javac"):
    run = """package sa.urtv.home;
public final class LastGoodExpireRun { public static void main(String[] a) {
  LastGood<String> g = new LastGood<>(); int[] n = {0};
  g.read(0, 3000, () -> "a" + (++n[0]));
  String s1 = g.read(100, 3000, () -> "a" + (++n[0])).value;
  g.expire(); String p = g.peek();
  String s2 = g.read(200, 3000, () -> "a" + (++n[0])).value;
  String s3 = g.read(300, 3000, () -> "a" + (++n[0])).value;
  System.out.print(s1 + "," + p + "," + s2 + "," + s3); } }"""
    with tempfile.TemporaryDirectory() as tmp:
        (Path(tmp) / "LastGoodExpireRun.java").write_text(run)
        subprocess.run(["javac", "-d", tmp, str(HOME / "LastGood.java"), str(Path(tmp) / "LastGoodExpireRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.LastGoodExpireRun"], check=True, capture_output=True, text=True).stdout
    check("LastGood.expire: the read after it asks again (a1 shared, then a2), then shares again; peek keeps the last answer meanwhile", out == "a1,a1,a2,a2", out)
else:
    print("SKIP LastGood on a JVM: no javac here")
sys.exit(1 if failed else 0)
