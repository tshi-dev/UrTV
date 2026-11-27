#!/usr/bin/env python3
"""4.4 beta 4 (the owner, «Do all», item 3): add photos to «صوري» from a USB stick or Downloads. The photos are read in the home (PhotoAddActivity, which holds the
read permission the icon editor already asked for) and handed to the screensaver app through the provider call the phone's upload uses; so the screensaver app gets
NO storage permission of its own (the supervisor's rule: no permission added). Run: python3 tests/photo_from_file_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
rd = lambda p: Path(p).read_text(encoding="utf-8")

app_man = rd(ROOT / "android/app/src/main/AndroidManifest.xml")
perms = re.findall(r'<uses-permission[^>]*android:name="([^"]+)"', app_man)
check("the screensaver app's manifest has no READ_MEDIA_IMAGES (nor any other storage or media permission)",
      not any(p in ("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.MANAGE_EXTERNAL_STORAGE", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") for p in perms), perms)
home_man = rd(ROOT / "android/launcher/src/main/AndroidManifest.xml")
check("the home's manifest declares PhotoAddActivity (not exported) and the read-images permission it already had",
      'android:name=".PhotoAddActivity"' in home_man and "android.permission.READ_MEDIA_IMAGES" in home_man and "MANAGE_EXTERNAL_STORAGE" not in home_man)
L = ROOT / "android/launcher/src/main/java/sa/urtv/home"
pa, ma = rd(L / "PhotoAddActivity.java"), rd(L / "MainActivity.java")
check("PhotoAddActivity: the sticks, Download, Pictures and DCIM; a re-encoded JPEG at screen size handed to the screensaver app's phone_photo_add (the phone's own path)",
      "IconEditorActivity.usbRoots(this)" in pa and '"Download", "Downloads", "Pictures", "DCIM"' in pa and 'call(SAVER, "phone_photo_add"' in pa
      and "Bitmap.CompressFormat.JPEG" in pa and "PhotoShrink.screenMax(this)" in pa)
check("the home opens it for open=photos", 'case "photos":' in ma and "PhotoAddActivity.class" in ma)
st = rd(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")
check("the settings row «أضف من فلاشة أو التنزيلات» is next to «صوري» and opens it through the home", 'row("أضف من فلاشة أو التنزيلات"' in st and 'openHome("photos")' in st
      and st.index('row("صوري ("') < st.index('row("أضف من فلاشة أو التنزيلات"') < st.index('row("بدء شاشة التوقف بعد"'))
sys.exit(1 if failed else 0)
