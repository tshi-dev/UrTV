#!/usr/bin/env python3
"""The home screen's card box (CardBox.java): one card as it always was, two or three never over the greeting and clock block,
the app title or the dock, on a 1920 x 1080 screen at the three densities TVs report (960, 1280 and 1920 dp wide), and the
sizes tests/page.test.mjs lays its cards out in. Needs javac; without it, skipped.   Run: python3 tests/card_box_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP card box: no javac here")
    sys.exit(0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "android/launcher/src/main/java/sa/urtv/home/CardBox.java"),
                    str(ROOT / "tests/CardBoxRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CardBoxRun"], check=True, capture_output=True, text=True).stdout
rows = [l.split() for l in out.splitlines() if not l.startswith('strip ')]
strips = [l.split() for l in out.splitlines() if l.startswith('strip ')]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


for kind, d, n, w, h, *rest in rows:
    d, n, w, h = float(d), int(n), int(w), int(h)
    dp = lambda v: round(v * d)
    wdp, hdp = round(w / d), round(h / d)
    tag = f"card box {n} card{'s' if n > 1 else ''}, {round(1920 / d)} dp wide, {kind}"
    if n == 1:
        check(f"{tag}: 461 x 250 dp, as it always was", (wdp, hdp) == (461, 250), (wdp, hdp))
        continue
    if kind == "early":   # before the screen is laid out: the sizes the page tests use (58.44% of the width, 46.3% of the height)
        want = (round(1920 / d * .5844), round(1080 / d * .463))
        check(f"{tag}: {want[0]} x {want[1]} dp", abs(wdp - want[0]) <= 1 and abs(hdp - want[1]) <= 1, (wdp, hdp, want))
        continue
    clock_left, title_top, dock_top = map(int, rest)
    check(f"{tag}: clear of the greeting and clock block", dp(48) + w <= clock_left - dp(16) + 1, (dp(48) + w, clock_left))
    wide = round((200 if kind == "short" else 420) * d)
    reaches = 1920 - dp(56) - wide < dp(48) + w + dp(16)   # the widest title would run into the box: it ends above the title
    limit = (title_top - dp(6)) if reaches else (dock_top - dp(8))
    check(f"{tag}: ends {'above the app title' if reaches else 'above the dock'}", dp(84) + h <= limit + 1 or h == dp(150),
          (dp(84) + h, limit))
    check(f"{tag}: room for a third card (150 dp at least)", h >= dp(150), hdp)
for _, title_top, plain_h, clock_h, plain_over, clock_over, top in strips:
    check(f"4.4 the strip (title at {title_top}): with the big glass clock it never covers its digits (the owner: the stocks card over «2:08»), "
          f"it starts under them ({top} >= 542 + 12 dp) and is shorter ({clock_h} of {plain_h} px); without it, as it was",
          clock_over == "false" and int(top) >= 542 + 18 and int(clock_h) <= int(plain_h), (title_top, plain_h, clock_h, plain_over, clock_over, top))
src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
check("the home passes the big clock's bottom to the strip, and fits the cards again when the big clock comes or goes",
      "bigClockOn ? ScenePlan.fixedBox(dm.widthPixels, dm.heightPixels, false, Dir.rtl())[3] : -1" in src and "handler.post(homeCard::fit)" in src)
sys.exit(1 if failed else 0)
