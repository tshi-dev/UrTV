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

# F4: the page copy is made by one thread at a time (one shared temp file)
hp = src(HOME / "HomePage.java")
check("F4: HomePage.copy is synchronized (the card's thread and the matches page's thread share the temp file)",
      "private static synchronized File copy(Context c, String name) {" in hp and hp.count('prefix + "tmp"') == 1)

# F5: the markets' toggles reach the screensaver in the order pressed (one thread), a refusal only undoes the latest value
mk = src(HOME / "MarketsActivity.java")
sv = body(mk, "private void saverSet(")
check("F5: saverSet runs on a single-thread executor and a refusal does not roll back over a later press",
      "Executors.newSingleThreadExecutor(" in mk and "saverQueue.execute(" in sv and "new Thread(" not in sv and "!value.equals(this.saved.get(key))" in sv)

# F6: the baseline's reason for CrashLoopStore.write says what the code does
cl = src(HOME / "CrashLoopStore.java")
bl = (ROOT / "tests/regressions/ui_thread_baseline.txt").read_text(encoding="utf-8").splitlines()
wl = [l for l in bl if "| CrashLoopStore.write |" in l]
check("F6: opened/drew/clear write only through the background writer; the baseline lines say so and name hook() in HomeApp.onCreate",
      "BG.execute(" in body(cl, "private static void later(") and "write(" not in body(cl, "static void opened(Context c) {") and "write(" not in body(cl, "static void drew(Context c) {")
      and "CrashLoopStore.hook(this)" in src(HOME / "HomeApp.java") and len(wl) == 2
      and all("crashloop-store" in l and "hook() in HomeApp.onCreate" in l and "must be on disk before the screen builds" not in l for l in wl))

# overlay F2: a notice the add-on gave up on (2 s) is not drawn later
xn = src(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasNotice.java")
sh = body(xn, "static boolean show(final Context c, final Bundle in) {")
check("overlay F2: the posted runnable takes a token (compareAndSet 0 to 1) and the timeout gives up with 0 to 2, so the home's toast and the overlay are never both shown",
      "state.compareAndSet(0, 1)" in sh and "state.compareAndSet(0, 2)" in sh and sh.index("compareAndSet(0, 1)") < sh.index("add(c.getApplicationContext()") and sh.index("compareAndSet(0, 2)") > sh.index("done.await("))

# overlay F4: the saver's «فوق تطبيق آخر» row is honest about the add-on's permission and never asks the package manager in a render
sa = src(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")
NEWLINE = "يعمل لو سمحت للإضافة بالظهور فوق التطبيقات"
tsv = (ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8")
check("overlay F4: the row says «يعمل لو سمحت…» when the add-on is installed, with an English entry",
      NEWLINE in sa and any(l.startswith(NEWLINE + "\t") and l.split("\t")[1].strip() for l in tsv.splitlines()))
check("overlay F4: the install check is asked on a thread (extras-look), cached in extrasHere, and never called from a render or a press",
      '"extras-look").start();' in sa and "extrasInstalled()" not in sa and sa.count("extrasInstalled(pm)") == 1 and "extrasLook();" in body(sa, "protected void onResume() {"))

print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
