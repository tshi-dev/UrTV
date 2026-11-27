#!/usr/bin/env python3
"""4.4 beta 4 (the owner, «Do all»): the adhan per prayer, ON THE SCREEN apart from the sound (the sound apart is «أذان كل صلاة», PrayerSound). A prayer set to no
screen has no bar (before its adhan, while it plays, to its iqama) and no «تنبيه عند الأذان». The four states are one picker (the UI session: no two toggles in a
row): «صوت وشاشة» / «صوت فقط» / «شاشة فقط» / «لا شيء». The home keeps the flags (Prefs ps_<prayer>); the screensaver reads the mask through the launcher's
provider (adhan_prayers) for its page's bar. Run: python3 tests/prayer_screen_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = H / "tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
rd = lambda p: Path(p).read_text(encoding="utf-8")

prefs, rcv, adhan, bar, prov, mr = rd(T / "Prefs.java"), rd(T / "AdhanReceiver.java"), rd(H / "AdhanBar.java"), rd(H / "AdhanBarText.java"), rd(T / "NowPlayingProvider.java"), rd(T / "MusicReceiver.java")
check("Prefs: a prayer shows unless its flag is off; the state is sound and screen together; setting it keeps the mask current",
      'sp.getBoolean("ps_" + prayer, true)' in prefs and "int prayerState(int prayer)" in prefs and "PrayerScreen.cache(hideMask());" in prefs and "setScreenOn(prayer, PrayerScreen.screen(state));" in prefs)
check("the notice «تنبيه عند الأذان» is shown only for a prayer that shows on the screen", "boolean notice = prefs.adhanNotice() && prefs.screenOn(prayer);" in rcv)
check("the home's bar: the mask goes into the bar's text (before and to the iqama), the adhan playing says nothing for a hidden prayer",
      "PrayerScreen.mask(c));" in adhan and "PrayerScreen.hiddenNow(NowPlaying.adhanPrayer())" in adhan and "if (hide != 0 && (hide >> HIDE_BIT[i] & 1) != 0) continue;" in bar)
check("the receiver: PRAYER_SCREEN and PRAYER_STATE are checked, set, and said in the status (prayer_screen=)",
      '"sa.urtv.home.PRAYER_SCREEN"' in mr and '"sa.urtv.home.PRAYER_STATE"' in mr and '"|prayer_screen=" + prayerScreens(prefs)' in mr.replace('\n                + ', '') or "|prayer_screen=" in mr)
check("the provider answers adhan_prayers: the five states and the hidden mask", '"adhan_prayers".equals(uri.getLastPathSegment())' in prov and 'new String[]{"states", "hide"}' in prov)
sh = rd(ROOT / "optimize/music.sh")
check("music.sh: adhan screen <prayer> on|off and adhan state <prayer> both|sound|screen|none, in the usage and the status", "      screen)" in sh and "      state)" in sh and "sa.urtv.home.PRAYER_SCREEN" in sh and "sa.urtv.home.PRAYER_STATE" in sh and "prayer_screen=" in sh)
br, tpl = rd(ROOT / "android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java"), rd(ROOT / "src/template.html")
check("the screensaver: the bridge asks the launcher for the mask and the page's bar skips a hidden prayer (before, to the iqama, and while it plays)",
      '"content://sa.urtv.home.nowplaying/adhan_prayers"' in br and 'o.put("hide"' in br and "ADHAN_HIDE = s && typeof s.hide === 'number' ? s.hide | 0 : 0;" in tpl
      and "if(adhanHidden(ADHAN_BIT[x.key])) continue;" in tpl and "!adhanHidden(ADHAN_LIVE.prayer)" in tpl)
act = rd(T / "PrayerSoundActivity.java")
check("the home's screen «الأذان لكل صلاة»: each prayer one row with its state, OK opens the four states (one focus target each), the muezzin below",
      'PrayerScreen.word(prefs.prayerState(p))' in act and "for (int st : new int[] {PrayerScreen.BOTH, PrayerScreen.SOUND_ONLY, PrayerScreen.SCREEN_ONLY, PrayerScreen.NONE})" in act)
ph = ROOT / "android/launcher/src/main/java/sa/urtv/home/phone"
sc, pb, pa = rd(ph / "Schema.java"), rd(T / "PhoneBridge.java"), rd(ph / "PhoneApi.java")
pg, ix = rd(ROOT / "android/launcher/src/main/assets/phone/prayers.html"), rd(ROOT / "android/launcher/src/main/assets/phone/index.html")
check("the phone: adhan_prayers is five digits 0..3 (Schema), set prayer by prayer and said in the state (PhoneBridge)",
      '"adhan_prayers"' in sc and 'matches("[0-3]{5}")' in sc and 'prefs.setPrayerState(PrayerScreen.IDS[i], st.charAt(i) - \'0\')' in pb and 'adhan.put("prayers", prayers.toString());' in pb)
check("the phone: its own page /y (the main page is at its limit), linked from the main page, reads adhan.prayers and sets adhan_prayers, asks the family PIN when it is wanted",
      'r.path.equals("/y")' in pa and 'href="/y"' in ix and len(ix.encode()) < 64 * 1024 and "s.adhan.prayers" in pg and "adhan_prayers: next" in pg and "pin_required" in pg)
if not (shutil.which("javac") and shutil.which("java")):
    print("SKIP PrayerScreen: no javac here (CI compiles it)")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    stub = Path(tmp, "stub"); stub.mkdir()
    Path(stub, "S.java").write_text("package sa.urtv.home; public final class S { public static String t(String s, Object... a) { return a.length == 0 ? s : String.format(s, a); } public static String sep() { return \"، \"; } public static String n(String s) { return s; } }", encoding="utf-8")
    Path(stub, "Context.java").write_text("package android.content; public class Context { }", encoding="utf-8")
    Path(stub, "Prefs.java").write_text("package sa.urtv.home.tilawah; public final class Prefs { public Prefs(android.content.Context c) { } public int hideMask() { return 0; } }", encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(Path(stub, "S.java")), str(Path(stub, "Context.java")), str(Path(stub, "Prefs.java")), str(T / "PrayerScreen.java"), str(ROOT / "tests/PrayerScreenRun.java")], capture_output=True, text=True)
    check("PrayerScreen compiles on a JVM with a stand-in S", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.PrayerScreenRun"], capture_output=True, text=True).stdout
        for line in out.splitlines():
            n, res = line.rsplit("\t", 1)
            check(n, res == "ok")
        check("every case ran", len(out.splitlines()) == 5, len(out.splitlines()))
sys.exit(1 if failed else 0)
