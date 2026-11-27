#!/usr/bin/env python3
"""4.4 beta 4 (the UI session's tree): «المؤذن», «الأدعية والتذكيرات» and «صوتياتي» are rows of «الصلاة والأذان» in the screensaver's settings, each opening
its screen of the تلاوة app through the home (behind its lock). «المؤذن» shows the muezzin's name, «الأدعية والتذكيرات» says «جديد من الجوال: N» while duas added from
«صوتياتي» wait to be timed on the TV. Run: python3 tests/pray_rows_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
rd = lambda p: Path(p).read_text(encoding="utf-8")

st = rd(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")
hub = rd(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsHub.java")
home = rd(ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java")
prov = rd(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/NowPlayingProvider.java")
dua = rd(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/DuaStore.java")
audio = rd(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/AudioUse.java")
act = rd(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/DuaActivity.java")
check("three rows, each opening its screen through the home", all(f'openHome("{w}")' in st for w in ("voice", "duas", "sounds"))
      and 'row("المؤذن", homeValue("adhan_voice", "name"' in st and 'row("الأدعية والتذكيرات", duasValue()' in st and 'row("صوتياتي"' in st)
check("the home opens AdhanActivity, DuaActivity, AudioActivity for them, behind the adhan's lock", 'case "voice": case "duas": case "sounds":' in home and '"AdhanActivity"' in home
      and '"DuaActivity"' in home and '"AudioActivity"' in home and "launchGuarded(ParentalPolicy.PILL_LISTEN" in home[home.index('case "voice"'):home.index('case "prayers"')])
check("the hub: the three rows in «الصلاة والأذان» after «الأذان» (the muezzin first) and after «تنبيه قبل الأذان» (duas, then sounds), and the words people type lead to them",
      '{"المؤذن", PRAY}' in hub and '{"الأدعية والتذكيرات", PRAY}' in hub and '{"صوتياتي", PRAY}' in hub
      and '"الأذان", "المؤذن", "الأذان لكل صلاة", "تنبيه قبل الأذان", "الأدعية والتذكيرات", "صوتياتي"' in hub
      and '{"اذاني", "المؤذن"}' in hub and '{"دعاء", "الأدعية والتذكيرات"}' in hub)
check("the provider answers adhan_voice (the muezzin's name) and duas_new (how many wait)", '"adhan_voice".equals(uri.getLastPathSegment())' in prov and '"duas_new".equals(uri.getLastPathSegment())' in prov
      and "DuaStore.freshCount(getContext())" in prov)
check("a dua added from «صوتياتي» is fresh until the person changes it on the TV", "d.fresh = true;" in audio and "edit.fresh = false;" in act and 'put("fresh", d.fresh)' in dua and 'optBoolean("fresh", false)' in dua)
check("the value is asked off the UI thread, kept on disk, the row drawn again only when it changes, «جديد من الجوال: N» in the settings' digits",
      "private String homeValue(" in st and 'new Thread(' in st[st.index("private String homeValue("):st.index("private String duasValue()")] and '"جديد من الجوال: " + DigitsText.convert(' in st)
sys.exit(1 if failed else 0)
