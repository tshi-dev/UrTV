#!/usr/bin/env python3
"""«UrTV النظيف» part A (the clean home): it is a MASK over the home (nothing deleted, none of the person's choices written over), turned on only by «فعّل» on a screen that
describes it first, the focus starting on «إلغاء», the family PIN when one is set; off puts everything back; «تخصيص» ticks what stays. The rules on a plain JVM (CleanKeep), the rest pinned
from the source.   Run: python3 tests/clean_mode_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def src(n): return (HOME / n).read_text(encoding="utf-8")
act, mode, main, card, bar = src("CleanActivity.java"), src("CleanMode.java"), src("MainActivity.java"), src("HomeCard.java"), src("PillBar.java")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
saver = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
sman = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

check("only CleanActivity writes the switch (setOn / setKeep); the home only reads it",
      [f for f in HOME.glob("**/*.java") if re.search(r"CleanMode\.set(On|Keep)\(", f.read_text(encoding='utf-8'))] == [HOME / "CleanActivity.java"]
      and "CleanMode.setOn" not in main)
check("it describes first: what stays, what is hidden, that nothing is deleted and that one press brings it back; «فعّل» is the only thing that turns it on",
      all(w in act for w in ("يبقى: الساعة والتاريخ وشريط التطبيقات", "يختفي: الأزرار الإضافية", "ما يُحذف شيء", "بضغطة وحدة")) and act.count("CleanMode.setOn(this, true)") == 1 and 'S.t("فعّل")' in act)
check("the focus starts on «إلغاء» (the first thing pressed by mistake does nothing)", 'cancel = button(S.t("إلغاء"), v -> finish());' in act and "first.post(first::requestFocus);" in act)
check("the family PIN is asked whenever one is set, for turning it on and for turning it off; a wrong or cancelled PIN changes nothing",
      "if (!Parental.state(this).pinSet) { go.run(); return; }" in act and "Parental.confirm(this, Parental.adhanFirst(this)," in act and act.count("pin(() ->") == 2 and "() -> { });" in act)
check("«تخصيص»: one tick per thing (one card, the Wi-Fi mark, the greeting, the settings button); the clock, the date and the apps bar always stay; Back from the sheet goes back to the description",
      "for (final String o : CleanKeep.OPTIONS)" in act and "customising = false; render(); return;" in act and "الساعة والتاريخ وشريط التطبيقات دائمًا" in act)
check("exported only behind a signature permission (same key), which the screensaver's settings hold: no other app opens it",
      re.search(r'android:name="\.CleanActivity"\s+android:exported="true"\s+android:permission="sa\.urtv\.home\.OPEN_CLEAN"', man) is not None
      and re.search(r'<permission\s+android:name="sa\.urtv\.home\.OPEN_CLEAN"\s+android:protectionLevel="signature"', man) is not None
      and '<uses-permission android:name="sa.urtv.home.OPEN_CLEAN" />' in sman)
check("a mask, not a rewrite: the home hides by visibility and by not making the card's page; off, it draws everything again from the person's own settings",
      "private void applyClean()" in main and "homeCard.hide(on && !keep.contains(CleanMode.CARD));" in main and "void hide(boolean on)" in card and "if (want == null || hidden)" in card
      and "if (was && !on) {" in main and "updateClock();" in main.split("if (was && !on) {")[1][:200] and "wireFocus();" in main.split("if (was && !on) {")[1][:300])
check("the top row stays closed with one button (the settings, in the control center's place); the others, «يُتلى الآن», the extra rows and the notices are hidden and stay hidden when the home redraws",
      "pillBar.lock(on)" in main and "void lock(boolean on)" in bar and "if (locked) return;" in bar and "centerPill.setIcon(on ? R.drawable.ic_settings : R.drawable.ic_center);" in main
      and "if (cleanShown) openOurSettings(false);" in main and "applyClean();   // «UrTV النظيف» (a mask over what the settings above just applied)" in main
      and "if (tvRowsBox == null || cleanShown) return;" in main and "if (cleanShown) { nudge.setVisibility(View.GONE); return; }" in main and "if (cleanShown) { nowCard.setVisibility(View.GONE); return; }" in main)
check("the greeting only when kept, never its weather line; the Wi-Fi mark only when kept",
      "CleanMode.keep(this).contains(CleanMode.GREETING) ? View.VISIBLE : View.GONE" in main and "netIcon.setVisibility(on && !keep.contains(CleanMode.WIFI) ? View.GONE : View.VISIBLE)" in main)
check("the settings' row (UrTV النظيف, in «الشاشة الرئيسية») opens the launcher's screen; nothing is set from there",
      'row("\\u2066UrTV\\u2069 النظيف"' in saver and 'setClassName("sa.urtv.home", "sa.urtv.home.CleanActivity")' in saver and "CleanMode" not in saver)
if not shutil.which("javac"):
    print("SKIP clean keep rules: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-d", tmp, str(HOME / "CleanKeep.java"), str(ROOT / "tests/CleanKeepRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CleanKeepRun"], check=True, capture_output=True, text=True).stdout.split("\n")
check("the rules: never set = one card, the Wi-Fi mark and the settings button; set to nothing = nothing; unknown names go, the sheet's order, no repeats",
      out[:4] == ["card,wifi,settings", "", "card,settings", "wifi,greeting"], out)
sys.exit(1 if failed else 0)
