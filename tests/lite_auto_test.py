#!/usr/bin/env python3
"""4.4 beta 3: «الوضع الخفيف» on «تلقائي» also turns on where this TV shows it is weak (LiteAuto): memory running low with the home in front, a first frame
slower than 8 s, a low-memory kill. Two signals within 7 days turn it on, 14 days with none turn it off; from the home's next start; said once; «تشغيل»,
«إيقاف» and `dock.sh ramtier` still win. Nothing on the UI thread. The rule on a fake clock (tests/LiteAutoRun.java) and source pins.
Run: python3 tests/lite_auto_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

ram, main, cls, store, hook, rep, ra = (text(HOME / f) for f in ("Ram.java", "MainActivity.java", "CrashLoopStore.java", "LiteAutoStore.java", "setup/SetupHook.java", "Report.java", "ReportActivity.java"))
tier = ram[ram.index("public static int tier(Context c) {"):]
check("Ram.tier: only on «تلقائي», after «تشغيل/إيقاف» (liteTier) and before the tester's forced tier (which wins)",
      tier.index('t = liteTier(t, sp.getString("lite", "auto"));') < tier.index('LiteAuto.on(sp.getString(LiteAuto.KEY, "")') < tier.index('int f = named(sp.getString("force", ""));')
      and 'if (t != LOW && "auto".equals(sp.getString("lite", "auto")) && LiteAuto.on(' in tier)
check("the signal of memory: onTrimMemory RUNNING_LOW or CRITICAL while the home is in front (not the background trims)",
      "if (resumed && level >= TRIM_MEMORY_RUNNING_LOW && level < TRIM_MEMORY_UI_HIDDEN) LiteAutoStore.note(this, LiteAuto.TRIM);" in main)
check("the signal of a slow start: the first frame later than LiteAuto.SLOW_MS after the home opened (the crash counter's own times)",
      "if (was != null && was.openedAt > 0 && now - was.openedAt > LiteAuto.SLOW_MS) LiteAutoStore.note(c, LiteAuto.SLOW);" in cls)
note = store[store.index("static void note("):store.index("static boolean on(")]
check("nothing on the UI thread: a signal is written on lite-auto's own thread; the «said once» runs on the home's start thread",
      "BG.execute(" in note and ".commit();" in note and "LiteAutoStore.tellOnce(app);" in hook and hook.index("LiteAutoStore.tellOnce(app);") > hook.index("private static void atHomeStartNow("))
check("never silent: said once when it comes on (with why), and the report's Home line carries «lite=auto-on-t…»",
      "p.getLong(TOLD, 0) == since" in store and "String.format(S.tv(SAID), S.tv(why))" in store and '", lite=" + safe(lite)' in rep and "LiteAutoStore.report(this)" in ra)
check("Ram.java is shared with the add-on: LiteAuto goes with it (the add-on keeps no signals: no change there)", "'Ram.java', 'LiteAuto.java'" in text(ROOT / "android/extras/build.gradle"))

if not shutil.which("javac"):
    print("SKIP the rule: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "LiteAuto.java"), str(ROOT / "tests/LiteAutoRun.java")], capture_output=True, text=True)
    check("LiteAuto compiles with no Android", r.returncode == 0, r.stderr[-800:])
    if r.returncode: sys.exit(1)
    got = {l.split(" ", 1)[0]: l for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.LiteAutoRun"], capture_output=True, text=True).stdout.splitlines()}
check("nothing: off", got.get("NONE") == "NONE false 0 []", got.get("NONE"))
check("one signal: still off", got.get("ONE") == "ONE false", got.get("ONE"))
check("two within 7 days: on, since the second, why = memory, the report's word", got.get("TWO") == "TWO true since=true kind=trim report=auto-on-t2-s0-o0", got.get("TWO"))
check("stays on 13 days after the last signal, off after 14", got.get("HELD") == "HELD true false", got.get("HELD"))
check("two signals 8 days apart: off", got.get("FAR") == "FAR false", got.get("FAR"))
check("why: a low-memory kill comes first", got.get("KIND") == "KIND oom", got.get("KIND"))
check("a signal from the future (the clock went back) counts nothing", got.get("FUTURE") == "FUTURE false", got.get("FUTURE"))
check("a damaged text: only well-formed signals count", got.get("JUNK") == "JUNK 2 true", got.get("JUNK"))
check("at most 20 kept, signals older than 14 days let go", got.get("KEEP") == "KEEP 20" and got.get("OLD") == "OLD 1", (got.get("KEEP"), got.get("OLD")))
rl = store.split("public static String rowLine(Context c) {")[1].split("\n    }\n")[0]
npp = (ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/NowPlayingProvider.java").read_text(encoding="utf-8")
sa = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
nb = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java").read_text(encoding="utf-8")
check("the UI session's wording: the notice and the row's line, one set of reasons, no arrow or dash, tanween on the alif",
      'S.n("شغّلنا «الوضع الخفيف» تلقائيًا لأن %s مرتين هذا الأسبوع. تقدر تطفيه من الإعدادات في «الوضع الخفيف».")' in store
      and 'S.n("تلقائي: شغّال الآن لأن %s مرتين هذا الأسبوع")' in store and 'S.n("التشغيل كان بطيئًا")' in store and "←" not in store and "—" not in store)
check("the row's line: only on «تلقائي» while this TV's signs keep it on, from prefs, the same reasons as the notice",
      '"auto".equals(p.getString("lite", "auto"))' in rl and "LiteAuto.on(s, now)" in rl and "String.format(S.tv(ROW), S.tv(why(LiteAuto.reasonKind(s, now))))" in rl)
check("the screensaver asks the home (lite_line, same key only) on a thread, once per return, and keeps «تلقائي، …» when there is no line",
      '"lite_line".equals(method)' in npp and "LiteAutoStore.rowLine(getContext())" in npp and 'checkCallingPermission("sa.urtv.home.READ_NOW_PLAYING")' in npp.split('"lite_line".equals(method)')[1][:200]
      and '"lite_line", null, null)' in nb and '}, "lite-line").start();' in sa and "liteLineAsked = false;" in sa
      and 'liteLine != null && !liteLine.isEmpty() ? liteLine : Prefs.liteLabel(prefs.lite(), Prefs.lowRam(this))' in sa)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
