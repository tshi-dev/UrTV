#!/usr/bin/env python3
"""The tester's step 47 findings (build 222, a real TCL) that are pinned in the source: the update screen opened once (singleTop and a debounce), a clean-mode screen
is not left behind the home, «UrTV النظيف» says when it is on, the two «صوري» tiles are named by where they show. (The hub's own findings are in your_tv_hub_test, the
clean mask's exact restore in clean_mode_test.)"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(p): return (ROOT / p).read_text(encoding="utf-8")

man = src("android/launcher/src/main/AndroidManifest.xml")
main = src("android/launcher/src/main/java/sa/urtv/home/MainActivity.java")
check("UpdateActivity is created once: singleTop, the result's intent singleTop too, and a second ask within 3 s from the home is ignored",
      'android:name=".UpdateActivity"\n            android:launchMode="singleTop"' in man and "new Intent(this, UpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)" in main
      and "SystemClock.elapsedRealtime() - updateResultAt < 3000) return;" in main)
check("the clean-mode screens (CleanActivity, FactoryActivity) are out of recents and finish when Home is pressed (nothing stale to come back to the front when Settings opens later)",
      'android:name=".CleanActivity"' in man and man.split('android:name=".CleanActivity"')[1].split("/>")[0].count('android:excludeFromRecents="true"') == 1
      and man.split('android:name=".setup.FactoryActivity"')[1].split("/>")[0].count('android:excludeFromRecents="true"') == 1
      and "protected void onUserLeaveHint()" in src("android/launcher/src/main/java/sa/urtv/home/CleanActivity.java") and "finish();" in src("android/launcher/src/main/java/sa/urtv/home/CleanActivity.java").split("protected void onUserLeaveHint()")[1][:120]
      and "protected void onUserLeaveHint()" in src("android/launcher/src/main/java/sa/urtv/home/setup/FactoryActivity.java"))
st = src("android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")
prov = src("android/launcher/src/main/java/sa/urtv/home/tilawah/NowPlayingProvider.java")
check("the settings' «UrTV النظيف» row says «مفعّل» (the on/off rule: the value starts with the state word) while the mask is on: the home answers .../clean_on, read once per visit, the last answer kept, nothing set from there",
      '"clean_on".equals(uri.getLastPathSegment())' in prov and 'getSharedPreferences("clean_mode"' in prov and "homeCleanSeen()" in st and '"مفعّل الأساسيات فقط، وبضغطة ترجع الشاشة الكاملة"' in st and "nowplaying/clean_on" in st)
ap = src("android/app/src/main/java/sa/urtv/screensaver/AppearanceActivity.java")
check("the screensaver's and the home's «صوري» tiles are named by where they show (…في شاشة التوقف, …في الرئيسية): never the same name one above the other",
      'w("صوري في شاشة التوقف")' in ap and 'w("صوري في الرئيسية")' in ap and 'w("صوري فقط في الرئيسية")' in ap and 'w("بدون صوري في الرئيسية")' in ap
      and 'String ownName = own == 0 ? w("صوري") ' not in ap and 'String mineName = Own.count(this) == 0 ? w("صوري") ' not in ap)
sys.exit(1 if failed else 0)
