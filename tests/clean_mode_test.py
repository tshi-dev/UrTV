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
check("«تخصيص»: one tick per thing (the Wi-Fi mark, the greeting, the settings button); the clock, the date and the apps bar always stay; Back from the sheet goes back to the description",
      "for (final String o : CleanKeep.OPTIONS)" in act and "customising = false; render(); return;" in act and "الساعة والتاريخ وشريط التطبيقات دائمًا" in act)
check("exported only behind a signature permission (same key), which the screensaver's settings hold: no other app opens it",
      re.search(r'android:name="\.CleanActivity"\s+android:exported="true"\s+android:permission="sa\.urtv\.home\.OPEN_CLEAN"', man) is not None
      and re.search(r'<permission\s+android:name="sa\.urtv\.home\.OPEN_CLEAN"\s+android:protectionLevel="signature"', man) is not None
      and '<uses-permission android:name="sa.urtv.home.OPEN_CLEAN" />' in sman)
check("a mask, not a rewrite: the home hides by visibility and by not making the card's page; off, it draws everything again from the person's own settings",
      "private void applyClean()" in main and "homeCard.hide(on);" in main and "void hide(boolean on)" in card and "if (want == null || hidden)" in card
      and "if (was && !on) {" in main and "updateClock();" in main.split("if (was && !on) {")[1][:200] and "wireFocus();" in main.split("if (was && !on) {")[1][:300])
check("the top row stays closed with one button (the settings, in the control center's place); the others, «يُتلى الآن», the extra rows and the notices are hidden and stay hidden when the home redraws",
      "pillBar.lock(on)" in main and "void lock(boolean on)" in bar and "if (locked) return;" in bar and "centerPill.setIcon(on ? R.drawable.ic_settings : R.drawable.ic_center);" in main
      and "if (cleanShown) openOurSettings(false);" in main and "applyClean();   // «UrTV النظيف» (a mask over what the settings above just applied)" in main
      and "if (tvRowsBox == null || cleanShown) return;" in main and "if (cleanShown) { nudge.setVisibility(View.GONE); return; }" in main and "if (cleanShown) { nowCard.setVisibility(View.GONE); return; }" in main)
check("the greeting only when kept, never its weather line; the Wi-Fi mark only when kept",
      "CleanMode.keep(this).contains(CleanMode.GREETING) ? View.VISIBLE : View.GONE" in main and "maskVis(netIcon, on && !keep.contains(CleanMode.WIFI) ? View.GONE : (on ? View.VISIBLE : null))" in main)
check("«off» puts every masked view back exactly as it was (step 47: the quick-buttons box was narrower after: x 96-240, was 104-264): each view's visibility is remembered the first time the mask touches it and restored, never set to «visible» by default; the bar is unlocked, measured again and laid out",
      "private final java.util.Map<View, Integer> cleanWas" in main and "if (!cleanWas.containsKey(v)) cleanWas.put(v, v.getVisibility());" in main and "for (java.util.Map.Entry<View, Integer> e : cleanWas.entrySet()) e.getKey().setVisibility(e.getValue());" in main
      and "pillBar.lock(false); pillBar.requestLayout();" in main and "phonePill.setVisibility(on ? View.GONE : View.VISIBLE)" not in main and "else apply();" in (HOME / "PillBar.java").read_text(encoding="utf-8"))
check("the settings' row (UrTV النظيف, in «الشاشة الرئيسية») opens the launcher's screen; nothing is set from there",
      'row("\\u2066UrTV\\u2069 النظيف"' in saver and 'setClassName("sa.urtv.home", "sa.urtv.home.CleanActivity")' in saver and "CleanMode" not in saver)

# ---- part B: a really empty UrTV (home line, screensaver page, hub), read through the provider; off restores exactly
tmpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
mask = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/CleanMask.java").read_text(encoding="utf-8")
prefsj = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")
hub = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/YourTvHubActivity.java").read_text(encoding="utf-8")
prov = (HOME / "tilawah/NowPlayingProvider.java").read_text(encoding="utf-8")
check("home: «الصلاة القادمة» is a tick (on by default), one line under the date, shown only while the mask is on and the tick is kept; off hides it (its own view, nothing of the person's is touched)",
      "DEFAULT_PRAYER = LINE" in src("CleanKeep.java") and "بطاقة الصلاة" in act
      and "if (!CleanKeep.LINE.equals(mode)) prayerLine.setVisibility(View.GONE);" in main and "nextPrayerLine" in main)
check("home: no way into the markets, matches or games pages while the mask is on (the pills are locked away, the stocks card and the games widget do nothing)",
      "if (!cleanShown) launch(new Intent(MainActivity.this, MarketsActivity.class));" in main and "if (cleanShown && FootballFocus.MT.equals(which)) return;" in main
      and "if (!cleanShown) guarded(ParentalPolicy.WIDGET_MATCHES" in main and "if (!cleanShown) launchGuarded(ParentalPolicy.WIDGET_MATCHES" in main)
check("the provider tells the saver: on, factory, prayer (read only); the saver only reads it (CleanMask) and keeps the last answer",
      '"on", "factory", "prayer"' in prov and "CleanMask" in prefsj and "clean_on" in mask and "getInt(\"clean_mask\"" in mask and "setOn" not in mask and "setKeep" not in mask)
check("saver page: the link carries #clean=1 (photo and clock) or #clean=2 (and the next-prayer line) only while the mask is on; the page hides the header, the card, the markets, the matches, the np line and the layers by CSS and reads the flag once",
      'return state == OFF ? "" : "&clean=" + state;' in mask and "dashboardUrl(c) + CleanMask.fragment(CleanMask.state(c))" in prefsj
      and "html.clean .top,html.clean .week,html.clean .p-mid,html.clean .p-match" in tmpl and "html.clean:not(.cl2) .p-prayer{display:none!important}" in tmpl
      and "classList.add('clean','cl'+_Q.get('clean'))" in tmpl)
check("the page is part of the build: dist/tv.html and tv-en.html carry the flag and stay under the budget",
      all("html.clean .top" in (ROOT / f"dist/{n}").read_text(encoding="utf-8") for n in ("tv.html", "tv-en.html")) and (ROOT / "dist/tv.html").stat().st_size < 740_000)
check("hub: with the mask on «تلفزيونك» opens straight on our settings and finishes (no games, markets or matches windows); off, the four windows as before",
      "if (CleanMask.state(this) != CleanMask.OFF) {" in hub and "startActivity(new Intent(this, SettingsActivity.class));" in hub.split("CleanMask.state(this)")[1][:300] and "finish();" in hub.split("CleanMask.state(this)")[1][:300]
      and "static final String[][] WINDOWS = {" in hub)
check("adhan, reminders, the parental lock and updates are not masked: the saver page keeps #rm and the adhan layer (only the listed blocks are hidden), the home keeps the adhan bar and the update path",
      "#rm" not in tmpl.split("html.clean .top")[1].split("</style>")[0] and "adhanBar" not in main.split("private void applyClean()")[1].split("private final java.util.Map<View, Integer> cleanWas")[0] and "UpdateNotice" not in main.split("private void applyClean()")[1].split("private final java.util.Map<View, Integer> cleanWas")[0])
check("back from «UrTV النظيف» the focus returns to ITS row, also in the build that the home's later answer causes (the row is kept as the wanted one for 4 s or until a key is pressed)",
      "returnWant = back; returnUntil = System.currentTimeMillis() + 4000;" in saver and "returnWant != null && !entering ? returnWant" in saver and "returnWant = null;   // the person moves" in saver)
if not shutil.which("javac"):
    print("SKIP clean keep rules: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-d", tmp, str(HOME / "CleanKeep.java"), str(HOME / "PrayerCardBox.java"), str(ROOT / "tests/CleanKeepRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CleanKeepRun"], check=True, capture_output=True, text=True).stdout.split("\n")
check("the rules: never set = the Wi-Fi mark and the settings button (the prayer line or card is its own choice); set to nothing = nothing; unknown names go, the sheet's order, no repeats",
      out[:4] == ["wifi,settings", "", "settings", "wifi,greeting"], out)
sys.exit(1 if failed else 0)
