#!/usr/bin/env python3
"""4.3.5 (R7, a CVTE Android 9 set left on the LG logo and then a black screen): a home that dies again and again right after it starts must never
leave the TV with nothing. CrashLoop counts crashes within 60 s of the process's start; two within ten minutes put the home in safe mode: its
next start opens SafeActivity (plain views, no WebView, no other app's data) with «افتح الشاشة الأصلية» and «حاول مرة ثانية», and drops our
claim as the default home. The rule runs on a JVM; the hooks are pinned.
Run: python3 tests/crash_loop_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

app = (HOME / "HomeApp.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
safe = (HOME / "SafeActivity.java").read_text(encoding="utf-8")
store = (HOME / "CrashLoopStore.java").read_text(encoding="utf-8")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
tsv = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")

oncreate = app.split("public void onCreate() {")[1]
check("the process's start is counted first in HomeApp (before anything that could crash)",
      oncreate.split("\n")[2].strip().startswith("CrashLoopStore.hook(this);"))
check("a crash is written at once (synced) before the old handler takes the process down",
      ".crashed(System.currentTimeMillis()); state = s; }" in store and "synchronized (FILE_LOCK) { write(app, s); }   // the process is dying" in store and "o.getFD().sync();" in store
      and store.index("crashed(") < store.index("prev.uncaughtException(t, e)"))
mc = main.split("protected void onCreate(Bundle b) {\n        super.onCreate(b);")[1]
check("MainActivity asks first: in safe mode it opens SafeActivity and finishes, building nothing",
      mc.lstrip().startswith("if (CrashLoopStore.safe(this)) {") and "openSafePage();" in mc.split("}")[0] and "private void openSafePage() {\n        launch(new Intent(this, SafeActivity.class));" in main and "finish();\n            return;" in mc.split("if (DebugFlags")[0])
check("…and its onDestroy then undoes nothing it did not do", "if (safeRedirect) { super.onDestroy(); return; }" in main)
check("the home screen marks its opening after the safe check, and its first frame (once, then the listener goes)",
      mc.index("CrashLoopStore.opened(this);") > mc.index("CrashLoopStore.safe(this)") and "addOnDrawListener(firstDraw)" in mc
      and "CrashLoopStore.drew(MainActivity.this);" in main and "removeOnDrawListener(self)" in main and "if (drewMarked) return;" in store)
check("SafeActivity: plain views only (no WebView, no provider, no photo)",
      "WebView" not in safe.split("public class SafeActivity")[1] and "getContentResolver" not in safe and "ConfigSnapshot" not in safe and "Bitmap" not in safe)
check("4.3.5 review F1: SafeActivity never touches our place as the default home (on Android 9 nothing could put it back)", "clearPackagePreferredActivities" not in safe)
check("4.3.5 review F2: «افتح الشاشة الأصلية» opens a real stock home (StockHome.real); none left: no button, the retry has the focus",
      "if (!StockHome.real(r.activityInfo.packageName, r.activityInfo.name)) continue;" in safe and "if (hasStock) col.addView(stock);" in safe
      and "(hasStock ? stock : retry).requestFocus();" in safe and "S.tv(hasStock ? BODY : BODY_RETRY)" in safe)   # F8: no line about a missing home
check("«حاول مرة ثانية» clears the count and starts the home again", "CrashLoopStore.clear(this);" in safe and "MainActivity.class" in safe)
check("SafeActivity is in the manifest, not exported", 'android:name=".SafeActivity"\n            android:exported="false"' in man)
words = ["تلفزيونك توقف أكثر من مرة عند التشغيل", "افتح الشاشة الأصلية للتلفزيون، أو جرّب مرة ثانية. إذا تكرر، أرسل تقرير من مساعدك",
         "افتح الشاشة الأصلية", "حاول مرة ثانية", "ما لقينا شاشة أصلية مفعّلة، افتح الإعدادات من زر الإعدادات في الريموت"]
check("its words are in the table and in English", all(f'S.n("{w}")' in safe for w in words) and all(f"\n{w}\t" in "\n" + tsv for w in words))

if not shutil.which("javac"):
    print("SKIP the rule on a JVM: no javac")
    sys.exit(1 if failed else 0)
S = 1_000_000_000_000
runs = [
    ([f"s {S}", f"q {S+1000}"], ["home"], "a start alone: the home"),
    ([f"s {S}", f"o {S+500}", f"c {S+5000}", f"s {S+8000}", f"q {S+8001}"], ["home"], "one quick crash: the home again"),
    ([f"s {S}", f"o {S+500}", f"c {S+5000}", f"s {S+8000}", f"o {S+8500}", f"c {S+12000}", f"s {S+15000}", f"q {S+15001}"], ["safe"], "two quick crashes of the home screen: the safe page"),
    ([f"s {S}", f"c {S+5000}", f"s {S+8000}", f"c {S+12000}", f"s {S+15000}", f"q {S+15001}"], ["home"], "review F3: a background service or receiver crash-looping (no home screen opened) never counts"),
    ([f"s {S}", f"o {S+500}", f"c {S+61000}", f"s {S+70000}", f"o {S+70500}", f"c {S+140000}", f"q {S+141000}"], ["home"], "crashes after a minute up are not quick: the home"),
    ([f"s {S}", f"o {S+500}", f"c {S+5000}", f"s {S+8000}", f"o {S+8500}", f"c {S+12000}", f"q {S+12000+600000}"], ["home"], "ten minutes later the count is gone"),
    ([f"s {S}", f"o {S+500}", f"c {S+5000}", f"s {S+8000}", f"o {S+8500}", f"c {S+12000}", "x", f"q {S+13000}"], ["home"], "«حاول مرة ثانية» clears it"),
    ([f"V {S} 470", f"o {S+500}", f"c {S+5000}", f"V {S+8000} 470", f"o {S+8500}", f"c {S+12000}", f"V {S+15000} 480", f"q {S+15001}"], ["home"], "review F5: a new version (an update that fixes the crash) starts with nothing counted"),
    ([f"V {S} 470", f"o {S+500}", f"c {S+5000}", f"V {S+8000} 470", f"o {S+8500}", f"c {S+12000}", f"V {S+15000} 470", f"q {S+15001}"], ["safe"], "…the same version keeps its count"),
    ([f"c {S}", f"q {S+1}"], ["home"], "a crash with no start known is not counted"),
    (["p garbage|1,x,", f"q {S}"], ["home"], "a damaged file is a clean state"),
    ([f"V {S} 470", f"o {S+500}", f"c {S+1000}", f"V {S+2000} 470", f"o {S+2500}", f"c {S+3000}", f"V {S+4000} 470", f"o {S+4500}", f"c {S+5000}", "w"], [f"{S+4000}|{S+3000},{S+5000}|{S+4500}", "0", f"1|470|{S+2000}", "1|"], "the file keeps the start, the last two quick crashes, this start's screen and the version"),
    (["p 5|6,7|0 0 0|470", "w"], ["5|6,7|0", "0", "0|470|0", "0|"], "the version line is read back"),
    # the hang watchdog: a home screen that opened and never drew its first frame within 20 s counts at the next start
    ([f"s {S}", f"o {S+1000}", f"d {S+2000}", f"s {S+100000}", f"o {S+101000}", f"d {S+102000}", f"s {S+200000}", f"q {S+200001}"], ["home"], "screens that drew in time: the home"),
    ([f"s {S}", f"o {S+1000}", f"s {S+40000}", f"o {S+41000}", f"s {S+80000}", f"q {S+80001}"], ["safe"], "two screens that never drew (hangs): the safe page"),
    ([f"s {S}", f"o {S+1000}", f"s {S+40000}", f"q {S+40001}"], ["home"], "one hang: the home again"),
    ([f"s {S}", f"o {S+1000}", f"d {S+25000}", f"s {S+40000}", f"o {S+41000}", f"d {S+70000}", f"s {S+90000}", f"q {S+90001}"], ["safe"], "a first frame after 20 s is a hang too"),
    ([f"s {S}", f"o {S+1000}", f"c {S+2000}", f"s {S+5000}", f"q {S+5001}"], ["home"], "a crash before the first frame is counted once, not again as a hang"),
    ([f"s {S}", f"s {S+40000}", f"s {S+80000}", f"q {S+80001}"], ["home"], "starts with no home screen (a receiver, a service) are never hangs"),
    ([f"s {S}", f"o {S+1000}", f"s {S+40000}", f"o {S+41000}", "x", f"s {S+80000}", f"q {S+80001}"], ["home"], "«حاول مرة ثانية» also forgives the screen that was open"),
    # 4.4 beta 3: low-memory kills (Android 11+ says how the last process ended; judged at the next start)
    ([f"s {S}", f"o {S+500}", f"d {S+1000}", f"s {S+30000}", f"k {S+20000} 1 {S+30000}", f"o {S+30500}", f"d {S+31000}", f"s {S+60000}", f"k {S+50000} 1 {S+60000}", f"q {S+60001}"], ["safe"],
     "two low-memory kills of the home in front, each within a minute of its start: the safe page (as two crashes)"),
    ([f"s {S}", f"o {S+500}", f"d {S+1000}", f"s {S+30000}", f"k {S+20000} 0 {S+30000}", f"o {S+30500}", f"d {S+31000}", f"s {S+60000}", f"k {S+50000} 0 {S+60000}", f"q {S+60001}"], ["home"],
     "kills in the background (normal on any TV): never counted"),
    ([f"s {S}", f"s {S+30000}", f"k {S+20000} 1 {S+30000}", f"s {S+60000}", f"k {S+50000} 1 {S+60000}", f"q {S+60001}"], ["home"],
     "a process that never opened the home screen (a receiver, a service): its kills are not counted"),
    ([f"s {S}", f"o {S+500}", f"d {S+1000}", f"s {S+300000}", f"k {S+200000} 1 {S+300000}", f"o {S+300500}", f"d {S+301000}", f"s {S+600000}", f"k {S+500000} 1 {S+600000}", f"q {S+600001}"], ["home"],
     "a kill long after the start (the home ran for minutes): not a start that fails"),
    # StockHome (review F2)
    (["h com.tcl.home com.tcl.home.Launcher", "h sa.urtv.home sa.urtv.home.MainActivity", "h com.google.android.tungsten.setupwraith x.Main", "h com.android.tv.settings com.android.tv.settings.FallbackHome", "h com.mediatek.home com.mediatek.home.FallbackHome", "h com.android.settings com.android.settings.FallbackHome"],
     ["real", "not", "not", "not", "not", "not"], "StockHome: a maker's home yes; ours, the setup wizard and the Settings stand-ins no"),
]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "CrashLoop.java"), str(HOME / "StockHome.java"), str(ROOT / "tests/CrashLoopRun.java")], check=True, capture_output=True)
    for steps, want, name in runs:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CrashLoopRun"] + steps, check=True, capture_output=True, text=True).stdout.split()
        check(name, got == want, f"{got}, want {want}")
# ---- 4.4 review F7, F8: no fsync on the UI thread; each home screen of a process is judged on its own first frame
hook = store.split("static void hook(final Context app) {")[1].split("Thread.setDefaultUncaughtExceptionHandler")[0]
check("F7 and 4.3.5 review F4: no file is written on the caller's thread (the hook, opened, drew, clear): the state changes in memory, BG writes it; only the dying process's handler writes at once",
      "BG.execute(" in store and "write(" not in hook and "step(app, s -> s.started(now, BuildConfig.VERSION_CODE));" in hook
      and "step(c, s -> s.opened(now));" in store and "step(c, s -> s.drewFrame(now));" in store and "step(c, CrashLoop::cleared);" in store and "setDaemon(true)" in store)
bg = store.split("BG.execute(() -> {")[1].split("});")[0] if "BG.execute(() -> {" in store else ""
check("4.3.5 review F7: the state lock is never held across the disk; the writer re-checks the state with the file held (a crash is never overwritten)",
      "synchronized (FILE_LOCK) {" in bg and bg.index("synchronized (FILE_LOCK)") < bg.index("synchronized (LOCK) { if (state != s) return; }") < bg.index("write(app, s);")
      and "synchronized (LOCK) { s = f.apply(" in store and "write(" not in store.split("private static void step(")[1].split("\n    }\n")[0])
check("4.3.5 review F4: MainActivity's safe() reads the state in memory, never the disk once the hook ran",
      "static boolean safe(Context c) { CrashLoop s = state; return (s != null ? s : read(c)).safe(System.currentTimeMillis()); }" in store)
check("F8: a second home screen in the same process is judged on its own first frame (opened() lets drew() mark again)",
      store.split("static void opened(Context c) {")[1].split("}")[0].lstrip().startswith("drewMarked = false;"))
ex = store[store.index("private static void exits(Context app) {"):store.index("/** MainActivity.onCreate")]
hk = store.split("static void hook(final Context app) {")[1].split("Thread.setDefaultUncaughtExceptionHandler")[0]
check("4.4 beta 3: how the last process ended is asked of Android (ApplicationExitInfo, Android 11+) on its own thread at the start, never the UI thread",
      "if (android.os.Build.VERSION.SDK_INT >= 30) {" in hk and "EXITS.execute(() -> exits(app));" in hk and '"crash-exits"' in store)
check("4.4 beta 3: only a low-memory kill of the home's own process; each once (the last judged is kept); in front it counts toward safe mode, in front or perceptible it feeds «الوضع الخفيف»",
      "REASON_LOW_MEMORY" in ex and '"sa.urtv.home".equals(e.getProcessName())' in ex and 'sp.getLong("oom_seen", 0)' in ex and 'sp.edit().putLong("oom_seen", latest).commit();' in ex
      and "step(app, s -> s.lowMemoryKill(at, front, now));" in ex and "LiteAutoStore.noteAt(app, LiteAuto.OOM, at);" in ex and "final boolean front = k[1] <= FOREGROUND;" in ex)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
