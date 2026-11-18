#!/usr/bin/env python3
"""4.4 review round 2 (launcher): source pins for the findings fixed after the QA-home and overlay reviews. One section per finding.
Run: python3 tests/home_review2_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def src(p): return re.sub(r"//[^\n]*", "", Path(p).read_text(encoding="utf-8"))
def body(s, head):
    return s.split(head)[1].split("\n    }\n")[0] if head in s else ""

# F1: the kept copy restores what the first draw reads
cs = src(HOME / "ConfigSnapshot.java")
kept, kv = body(cs, "private static ConfigSnapshot kept(Context c) {"), body(cs, "private static void keepValues(Context c, ConfigSnapshot s) {")
for field, key in [("favBar", "fav_bar"), ("pillsHidden", "pills_hidden"), ("homeLayout", "home_layout"), ("scenePacks", "scene_packs"), ("sceneStay", "scene_stay"),
                   ("sceneByTime", "scene_time"), ("sceneByWeather", "scene_weather"), ("homeBg", "home_bg"), ("homeClock", "home_clock"),
                   ("appsStyle", "apps_style"), ("greetingName", "greeting_name"), ("cityChosen", "city_chosen")]:
    check(f"F1: {field} is read back in kept(), written in keepValues and part of its signature",
          f's.{field} =' in kept and f'"{key}"' in kept and f'"{key}"' in kv and f"s.{field}" in kv.split(".edit()")[0], field)

# F2: the tasks list's late render keeps the focus in the text field while the person types
ta = src(HOME / "TasksActivity.java")
rd = body(ta, "private void render(int focus) {")
check("F2: render skips its focus request while the text field has the focus (not on the first render)",
      "field.hasFocus() && !fieldFirst" in rd and "!typing &&" in rd and rd.index("boolean typing") < rd.index("fieldFirst = false;") < rd.index("f.post(f::requestFocus)"))

# F3: S.remember has the same 10 s guard as S.refresh and ignores an old copy
ss = src(HOME / "S.java")
rm = body(ss, "static void remember(Context c, ConfigSnapshot snap) {")
check("F3: remember ignores a stale snapshot and anything within 10 s of the person's own S.set",
      "snap.stale" in rm and "setAt != 0 && android.os.SystemClock.elapsedRealtime() - setAt < 10_000" in rm and rm.index("snap.stale") < rm.index("snap.lang()"))

print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
