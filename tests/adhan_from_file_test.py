#!/usr/bin/env python3
"""4.4 beta 4 (the owner, «Do all», item 3): add an adhan from a file on the TV. «المؤذن» (the voice list) ends with «أذاني من ملف», which opens «صوتياتي»
(its «أضف من فلاشة أو التنزيلات»: a USB stick, an SD card or the TV's Downloads folder); the file copied in becomes the person's own adhan (or the Fajr one)
at once, nothing else asked, and the voice list shows it when the person comes back. No new permission: the same read permission «صوتياتي» already asks for.
Run: python3 tests/adhan_from_file_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
rd = lambda p: Path(p).read_text(encoding="utf-8")

ad, au = rd(T / "AdhanActivity.java"), rd(T / "AudioActivity.java")
check("«المؤذن» ends with «أذاني من ملف», for the Fajr list too, and OK opens «صوتياتي» asking for the file to become that adhan",
      'S.t("أذاني من ملف")' in ad and "position == fileRow()" in ad and 'putExtra(AudioActivity.EXTRA_USE, fajr ? "own_fajr" : "own")' in ad
      and "voices.length + own + ownFajr + 1" in ad)
check("coming back, the list knows the new own recording (onResume)", "protected void onResume()" in ad and "adapter.notifyDataSetChanged();" in ad)
check("«صوتياتي» with a use: the copied file becomes that adhan at once and the screen closes; only own and own_fajr are taken",
      'u = getIntent().getStringExtra(EXTRA_USE)' in au and '"own".equals(u) || "own_fajr".equals(u) ? u : null' in au and "AudioUse.set(this, a.item, useAs)" in au and "finish();" in au)
check("«أضف من فلاشة أو التنزيلات»: the TV's Downloads folder is a place beside the sticks, named «التنزيلات»",
      "DIRECTORY_DOWNLOADS" in au and "if (dl != null) out.add(dl);" in au and 'S.t("التنزيلات")' in au and 'S.t("أضف من فلاشة أو التنزيلات")' in au)
man = rd(ROOT / "android/launcher/src/main/AndroidManifest.xml")
check("no new permission: the manifest declares no storage permission the audio screen did not already ask for (read permission only, asked at the click)",
      "MANAGE_EXTERNAL_STORAGE" not in man and "WRITE_EXTERNAL_STORAGE" not in man and 'READ_MEDIA_AUDIO"' in au)
sys.exit(1 if failed else 0)
