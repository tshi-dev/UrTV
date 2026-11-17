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
    ([f"s {S}", f"c {S+1000}", f"s {S+2000}", f"c {S+3000}", f"s {S+4000}", f"c {S+5000}", "w"], [f"{S+4000}|{S+3000},{S+5000}|"], "the file keeps the start and the last two quick crashes"),
]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "CrashLoop.java"), str(ROOT / "tests/CrashLoopRun.java")], check=True, capture_output=True)
    for steps, want, name in runs:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CrashLoopRun"] + steps, check=True, capture_output=True, text=True).stdout.split()
        check(name, got == want, f"{got}, want {want}")
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
