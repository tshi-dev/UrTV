#!/usr/bin/env python3
"""docs/STYLE-AR.md: the Arabic wording check (.github/tools/ar_style.py) warns on what the guide forbids, stays quiet on good
wording, never fails a run, and the 50 approved Saudi rewrites are in the app.

    python3 tests/ar_style_test.py
"""
import importlib.util
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("ar_style", ROOT / ".github/tools/ar_style.py")
ar = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ar)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += 0 if ok else 1


def warns(s):
    return ar.check_one(s)


check("the middle dot is flagged", any("«·»" in w for w in warns("مهلًا · طلبات")))
check("an emoji is flagged, the app's own marks are not", warns("جاهزين 😀") and not warns("✓ UrTV هو الشاشة الرئيسية"))
check("Western digits are flagged, placeholders and Arabic-Indic digits are not",
      warns("الرمز 6 أرقام") and not warns("الرمز ٦ أرقام") and not warns("أنت {0}") and not warns("%1$s يجاوب"))
check("formal patterns are notes", any(w.startswith("note") for w in warns("لا يوجد اتصال")))
check("good Saudi wording is quiet", not warns("شوي شوي، طلبات كثيرة.") and not warns("ما فيه أذان الحين."))
check("TV space: old length + 10%, at least 2", ar.limit(10) == 12 and ar.limit(50) == 55 and ar.limit(57) == 63)
r = subprocess.run([sys.executable, str(ROOT / ".github/tools/ar_style.py"), "--text", "مهلًا · 😀"], capture_output=True, text=True)
check("warnings never fail a run (exit 0)", r.returncode == 0 and "WARN" in r.stdout, r.stdout + r.stderr)

java = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
home = (ROOT / "android/launcher/src/main/java/sa/urtv/home/ParentalActivity.java").read_text(encoding="utf-8")
phone = (ROOT / "android/launcher/src/main/assets/phone/index.html").read_text(encoding="utf-8")
check("approved: settings sentences in Saudi speech",
      '"ما قدرنا نحمّل القائمة، جرّب تدوّر على الفريق"' in java and '"ما تشتغل: التلفزيون موقّف شاشة التوقف"' in java)
check("approved: «يتقفل», never «ينقفل»", "واختر وش يتقفل. بدون رمز ما يتقفل شيء" in home
      and not any("ينقفل" in p.read_text(encoding="utf-8", errors="ignore") for p in (ROOT / "android").rglob("*.java")))
check("approved: the phone page's messages", "'شوي شوي، طلبات كثيرة.'" in phone and "'ما فيه أذان الحين.'" in phone)
check("the guide is there and names the check", "ar_style.py" in (ROOT / "docs/STYLE-AR.md").read_text(encoding="utf-8"))

print("all passed" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
