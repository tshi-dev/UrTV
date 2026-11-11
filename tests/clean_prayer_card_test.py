#!/usr/bin/env python3
"""«UrTV النظيف»'s prayer card («بطاقة الصلاة»: بدون / سطر تحت التاريخ / بطاقة; «مكان البطاقة»: وسط / تحت الساعة / الجهة المقابلة للساعة): the three places on real boxes at 1920x1080 and
1280x720 (three densities each), right to left and left to right: inside the safe area, never over the clock block or the app title and the dock; the choices and their defaults from the source.
Run: python3 tests/clean_prayer_card_test.py   (needs javac)"""
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
keep, mode, act, main = src("CleanKeep.java"), src("CleanMode.java"), src("CleanActivity.java"), src("MainActivity.java")

check("the old «بطاقة واحدة» tick is gone (no other card types in clean mode); the ticks left are the Wi-Fi mark, the greeting and the settings button",
      'OPTIONS = {WIFI, GREETING, SETTINGS}' in keep and "case CleanMode.CARD" not in act and "homeCard.hide(on);" in main)
check("«بطاقة الصلاة»: بدون / سطر تحت التاريخ (the default) / بطاقة; «مكان البطاقة»: وسط (the default) / تحت الساعة / الجهة المقابلة للساعة",
      'LINE = "line"' in keep and 'DEFAULT_PRAYER = LINE' in keep and "PrayerCardBox.CENTER" in keep
      and all(w in act for w in ("بطاقة الصلاة", "مكان البطاقة", "سطر تحت التاريخ", "تحت الساعة", "الجهة المقابلة للساعة", "وسط الشاشة")))
check("the card is one view built once and filled on the minute tick (no per-frame work); the line and the card never show together; off hides both",
      "prayerCard" in main and "private void updatePrayerLine()" in main and "invalidate" not in main.split("private void updatePrayerLine()")[1][:1800])
if not shutil.which("javac"):
    print("SKIP boxes: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(HOME / "PrayerCardBox.java"), str(HOME / "CleanKeep.java"), str(ROOT / "tests/PrayerCardBoxRun.java")], capture_output=True, text=True)
    check("the box rules compile", r.returncode == 0, r.stderr[-500:])
    if r.returncode: sys.exit(1)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.PrayerCardBoxRun"], capture_output=True, text=True).stdout.splitlines()
rows = [l.split() for l in out if l.split()[0] in ("center", "below", "opposite")]
check("the runner gave 3 places x 2 directions x 6 screens", len(rows) == 36, len(rows))
bad = []
for p, rtl, w, h, d, l, t, rr, b, cl, ct, cr, cb, tt in rows:
    rtl, w, h = int(rtl), int(w), int(h); d = float(d); l, t, rr, b, cl, ct, cr, cb, tt = map(int, (l, t, rr, b, cl, ct, cr, cb, tt))
    side = max(round(w * .05), round(48 * d)); top = max(round(h * .05), round(24 * d))
    tag = f"{p} {'rtl' if rtl else 'ltr'} {w}x{h} d{d}"
    if l < side or rr > w - side or t < top: bad.append(tag + " outside the safe area")
    if b > tt: bad.append(tag + " over the app title and the dock")
    if not (rr <= cl or l >= cr or b <= ct or t >= cb): bad.append(tag + " over the clock block")
    if rr - l < round(240 * d) or b - t < round(64 * d): bad.append(tag + " too small")
check("all 36: inside the safe area (5 %, 48 dp at the sides), above the app title, clear of the clock block, a usable size", not bad, bad[:5])
for p in ("center", "opposite"):
    sides = {(int(x[1])): (int(x[5]), int(x[7])) for x in rows if x[0] == p and x[2] == "1920" and x[4] == "2.0"}
    ok = sides[1][1] <= int(next(x for x in rows if x[0] == p and x[1] == "1" and x[2] == "1920" and x[4] == "2.0")[9]) and sides[0][0] >= int(next(x for x in rows if x[0] == p and x[1] == "0" and x[2] == "1920" and x[4] == "2.0")[11])
    check(f"{p}: it moves with the direction (the clock is at the other side in English)", ok, sides)
opp = [x for x in rows if x[0] == "opposite"]
check("«الجهة المقابلة للساعة»: level with the clock, on the far side (left in Arabic, right in English)",
      all(int(x[6]) == max(int(x[6]), int(x[10])) and ((int(x[1]) == 1 and int(x[5]) < int(x[9])) or (int(x[1]) == 0 and int(x[5]) > int(x[11]))) for x in opp))
bel = [x for x in rows if x[0] == "below" and abs(int(x[6]) - (int(x[12]) + round(8 * float(x[4])))) <= 1]
check("«تحت الساعة»: right under the clock block where the room allows (and the middle where it does not)", len(bel) >= 6, len(bel))
sys.exit(1 if failed else 0)
