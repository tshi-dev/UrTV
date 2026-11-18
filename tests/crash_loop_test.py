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
      "write(app, read(app).crashed(System.currentTimeMillis()));" in store and "o.getFD().sync();" in store
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
check("SafeActivity drops only our own claim as the default home", "clearPackagePreferredActivities(getPackageName())" in safe)
check("«افتح الشاشة الأصلية» opens another app's HOME (never ours, never Settings' stand-in)",
      "getPackageName().equals(r.activityInfo.packageName)) continue;" in safe and '"com.android.settings".equals(r.activityInfo.packageName)) continue;' in safe)
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
    ([f"s {S}", f"c {S+5000}", f"s {S+8000}", f"q {S+8001}"], ["home"], "one quick crash: the home again"),
    ([f"s {S}", f"c {S+5000}", f"s {S+8000}", f"c {S+12000}", f"s {S+15000}", f"q {S+15001}"], ["safe"], "two quick crashes: the safe page"),
    ([f"s {S}", f"c {S+61000}", f"s {S+70000}", f"c {S+140000}", f"q {S+141000}"], ["home"], "crashes after a minute up are not quick: the home"),
    ([f"s {S}", f"c {S+5000}", f"s {S+8000}", f"c {S+12000}", f"q {S+12000+600000}"], ["home"], "ten minutes later the count is gone"),
    ([f"s {S}", f"c {S+5000}", f"s {S+8000}", f"c {S+12000}", "x", f"q {S+13000}"], ["home"], "«حاول مرة ثانية» clears it"),
    ([f"c {S}", f"q {S+1}"], ["home"], "a crash with no start known is not counted"),
    (["p garbage|1,x,", f"q {S}"], ["home"], "a damaged file is a clean state"),
    ([f"s {S}", f"c {S+1000}", f"s {S+2000}", f"c {S+3000}", f"s {S+4000}", f"c {S+5000}", "w"], [f"{S+4000}|{S+3000},{S+5000}|0", "0", "1|"], "the file keeps the start, the last two quick crashes, and this start's screen"),
    # the hang watchdog: a home screen that opened and never drew its first frame within 20 s counts at the next start
    ([f"s {S}", f"o {S+1000}", f"d {S+2000}", f"s {S+100000}", f"o {S+101000}", f"d {S+102000}", f"s {S+200000}", f"q {S+200001}"], ["home"], "screens that drew in time: the home"),
    ([f"s {S}", f"o {S+1000}", f"s {S+40000}", f"o {S+41000}", f"s {S+80000}", f"q {S+80001}"], ["safe"], "two screens that never drew (hangs): the safe page"),
    ([f"s {S}", f"o {S+1000}", f"s {S+40000}", f"q {S+40001}"], ["home"], "one hang: the home again"),
    ([f"s {S}", f"o {S+1000}", f"d {S+25000}", f"s {S+40000}", f"o {S+41000}", f"d {S+70000}", f"s {S+90000}", f"q {S+90001}"], ["safe"], "a first frame after 20 s is a hang too"),
    ([f"s {S}", f"o {S+1000}", f"c {S+2000}", f"s {S+5000}", f"q {S+5001}"], ["home"], "a crash before the first frame is counted once, not again as a hang"),
    ([f"s {S}", f"s {S+40000}", f"s {S+80000}", f"q {S+80001}"], ["home"], "starts with no home screen (a receiver, a service) are never hangs"),
    ([f"s {S}", f"o {S+1000}", f"s {S+40000}", f"o {S+41000}", "x", f"s {S+80000}", f"q {S+80001}"], ["home"], "«حاول مرة ثانية» also forgives the screen that was open"),
]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "CrashLoop.java"), str(ROOT / "tests/CrashLoopRun.java")], check=True, capture_output=True)
    for steps, want, name in runs:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CrashLoopRun"] + steps, check=True, capture_output=True, text=True).stdout.split()
        check(name, got == want, f"{got}, want {want}")
# ---- 4.4 review F7, F8: no fsync on the UI thread; each home screen of a process is judged on its own first frame
check("F7: opened() and drew() write on the store's background thread (never the caller's), the crash handler and the hook still write at once under one lock",
      "BG.execute(" in store and "static void opened(Context c) {" in store and "write(c, read(c).opened" not in store and "write(c, read(c).drewFrame" not in store
      and "synchronized (LOCK) { write(app, read(app).crashed(" in store and "synchronized (LOCK) { write(app, read(app).started(" in store and "setDaemon(true)" in store)
check("F8: a second home screen in the same process is judged on its own first frame (opened() lets drew() mark again)",
      store.split("static void opened(Context c) {")[1].split("}")[0].lstrip().startswith("drewMarked = false;"))
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
