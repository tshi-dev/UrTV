#!/usr/bin/env python3
"""Review F101: the «المظهر على الرئيسية» panel must not share the saver's task (NEW_TASK with the default affinity brought it over the settings, not the home)."""
import re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
m = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
blk = re.search(r'<activity\s[^>]*android:name="\.LookPanelActivity"[^>]*>', m, re.S)
ok = bool(blk) and 'android:taskAffinity="sa.urtv.screensaver.look"' in blk.group(0)
print(("PASS" if ok else "FAIL") + " look panel: its own task affinity")
lm = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
app = re.search(r"<application\b[^>]*>", lm, re.S).group(0)
ok2 = 'android:requestLegacyExternalStorage="true"' in app
print(("PASS" if ok2 else "FAIL") + " Android 10: the home asks for the legacy storage view, so «أضف من فلاشة أو التنزيلات» and «أذاني من ملف» list the files of Downloads (review F97; the API 29 emulator step of auto-round.yml showed «لا صور هنا» without it)")
sys.exit(0 if ok and ok2 else 1)
