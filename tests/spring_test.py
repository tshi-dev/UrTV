#!/usr/bin/env python3
"""Spring focus motion on the home (from talkies and Free My TV, MIT: THIRD_PARTY_NOTICES.md): the focused card grows on a spring and
the page glides on one; a new target keeps the speed it has, so a held arrow keeps its pace. Spring runs for real here
(tests/SpringRun.java, 60 frames a second); the home's wiring is read from the source. Needs javac.   Run: python3 tests/spring_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
spring = (HOME / "Spring.java").read_text(encoding="utf-8")
notices = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")

# ---- the wiring
check("Spring has no Android in it (it runs on a plain JVM here)", "import android" not in spring)
check("Spring says where it comes from: talkies (TechPP) and Free My TV, MIT",
      "github.com/rajupp/talkies" in spring and "TechPP" in spring and "free-my-tv" in spring and "MIT" in spring and "THIRD_PARTY_NOTICES.md" in spring)
check("THIRD_PARTY_NOTICES.md has both MIT notices in full",
      "Copyright (c) 2026 TechPP" in notices and "Copyright (c) 2026 Free My TV contributors" in notices
      and notices.count("THE SOFTWARE IS PROVIDED \"AS IS\"") >= 2 and "Spring.java" in notices)
check("the page never jumps by smoothScrollTo any more: every scroll of the home glides (glideTo)", "smoothScrollTo" not in main and main.count("glideTo(") >= 8)
check("no card's size is animated by animate() any more (the spring and an animator would fight over it)",
      not re.search(r"(card|v)\.animate\(\)\.scaleX", main), re.findall(r".*\.animate\(\)\.scaleX.*", main))
check("the focus (FavBar's 1.08 in the bar, 1.1 in the grid) and the lifted card (FavBar's 1.12 in the bar, 1.25 in the grid) take their size from scaleTo",
      "scaleTo(v, s);" in main and "scaleTo(card, fav ? FavBar.LIFTED_SCALE : 1.25f);" in main and "scaleTo(card, s);" in main)
drv = main[main.index("private void doMotion(long now)"):].split("\n    }\n")[0]
check("one frame callback steps every spring, a slow frame capped at 50 ms, and stops once all have settled",
      "Math.min(0.05f, (now - motionNanos) / 1e9f)" in drv and "if (moving) android.view.Choreographer.getInstance().postFrameCallback(motionFrame);" in drv
      and "private final android.view.Choreographer.FrameCallback motionFrame = this::doMotion;" in main and main.count("this::doMotion") == 1
      and "else motionPosted = false;" in drv and "if (isDestroyed())" in drv)
check("a card at rest at its own size is let go (no spring kept for it)", "else if (sp.target == 1f) it.remove();" in drv)
glide = main[main.index("private void glideTo(int y)"):].split("\n    }\n")[0]
check("a glide starts from where the page is when nothing was gliding it, and keeps its speed when it was",
      "if (!scrollGliding) scrollSpring.snap(scroll.getScrollY());" in glide and "scrollSpring.target = y;" in glide)
check("the page's spring has no bounce, the card's a little (as the focus had)",
      "new Spring(0.34f, 1f, 0, 0.25f);" in main and "new Spring(0.34f, 0.74f, v.getScaleX())" in main)
check("with the TV's animations off (animator scale 0) the size and the scroll go straight there",
      "Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f" in main.split("private void readAnimationScale()")[1].split("\n    }\n")[0]
      and "readAnimationScale();" in main.split("protected void onResume()")[1].split("\n    }\n")[0] and "if (!motionOn()) { scaleSprings.remove(v); v.setScaleX(scale); v.setScaleY(scale); return; }" in main
      and "if (!motionOn()) { scrollSpring.snap(y); scroll.scrollTo(0, y); return; }" in main)

# ---- the spring, for real
if not shutil.which("javac"):
    print("SKIP spring: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "Spring.java"), str(ROOT / "tests/SpringRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SpringRun"], check=True, capture_output=True).stdout.decode()
r = {}
for line in out.split("\n"):
    if line: k, *v = line.split(" "); r[k] = [float(x) for x in v]
v = lambda k: r[k][0]
check("the page: 600 px in well under a second, with no overshoot, and it ends on its target",
      v("page_settle") < 0.8 and v("page_peak") <= 600.25 and abs(v("page_end") - 600) < 0.01, (v("page_settle"), v("page_peak")))
check("a card: grows to 1.1 with a small bounce (under 1.13) and rests on 1.1 within a second",
      1.1 < v("card_peak") < 1.13 and v("card_settle") < 1.0 and abs(v("card_end") - 1.1) < 1e-6, (v("card_peak"), v("card_settle")))
check("a new target mid-flight keeps the speed it has (no restart from a standstill)",
      v("retarget_speed_before") > 100 and v("retarget_speed_after") == v("retarget_speed_before") and v("retarget_speed_next") > v("retarget_speed_before"),
      (v("retarget_speed_before"), v("retarget_speed_after"), v("retarget_speed_next")))
check("a held arrow (a new row every 120 ms, eight times): the page never slows to a stop between two presses, and rests on the last",
      v("held_slowest") > 300 and abs(v("held_end") - 2400) < 0.01 and v("held_rest") < 0.9, (v("held_slowest"), v("held_rest")))
check("a slow frame (50 ms) lands within 3 px (0.5% of a 600 px glide) of three short ones: no jump, no blow-up", v("slow_frame_diff") < 3, v("slow_frame_diff"))
check("at rest it does not move; snap puts it there standing still", v("rest_moving") == 0 and r["snap"] == [42.0, 0.0, 42.0], r["snap"])
sys.exit(1 if failed else 0)
