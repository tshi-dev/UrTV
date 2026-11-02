#!/usr/bin/env python3
"""«إعداد تلفزيوني» and the stuck debugging prompt: while a run waits for the TV's «السماح بتصحيح USB؟», our screensaver must not start (a
prompt sent under it stays stuck until a restart; TCL starts it even with FLAG_KEEP_SCREEN_ON on the setup screen). The rule (SaverHoldRule:
held from the wait's start, free by itself after 240 s) runs in tests/SaverHoldRun.java; here the wiring, read from the source: the hold
begins in the Approval's waiting() and ends once the session is open and in a finally, the app holds a screen wake lock with a timeout
(no DEVICE_POWER), the launcher's provider says «held», and the screensaver's dream ends at once while it does.
Run: python3 tests/saver_hold_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def text(p): return (ROOT / p).read_text(encoding="utf-8")
act, hold, rule = (SETUP / "SetupActivity.java").read_text(encoding="utf-8"), (SETUP / "SaverHold.java").read_text(encoding="utf-8"), (SETUP / "SaverHoldRule.java").read_text(encoding="utf-8")
prov = text("android/launcher/src/main/java/sa/urtv/home/tilawah/NowPlayingProvider.java")
bridge, dream = text("android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java"), text("android/app/src/main/java/sa/urtv/screensaver/ScreenDream.java")
lman, sman = text("android/launcher/src/main/AndroidManifest.xml"), text("android/app/src/main/AndroidManifest.xml")

check("SaverHoldRule has no Android in it", "import android" not in rule)
wait = act.split("@Override public void waiting() {")[1].split("}")[0]
check("the hold begins when the TV is asked (the Approval's waiting()), before the line for the person", "SaverHold.begin(SetupActivity.this);" in wait and wait.index("SaverHold.begin") < wait.index("line("))
body = act.split("SetupEngine.Out() { @Override public void line(String t) { SetupActivity.this.line(t); } })) {")[1]
check("it ends as soon as the session is open (the answer came), before the run's steps", body.index("SaverHold.end();") < body.index("SetupEngine e = s.engine;"))
check("…and in a finally, so a wait that gave up or failed frees the screensaver too", "} finally {\n                    SaverHold.end();" in act)
check("the app's own screen wake lock, with the rule's timeout, not reference counted, released at the end",
      "SCREEN_BRIGHT_WAKE_LOCK" in hold and "lock.acquire(SaverHoldRule.MAX_MS);" in hold and "setReferenceCounted(false)" in hold and "lock.release();" in hold)
check("no DEVICE_POWER and no userActivity: a normal app cannot poke the TV's activity", "DEVICE_POWER" not in lman and "userActivity(" not in hold.split("*/", 1)[1])
check("WAKE_LOCK is already declared (no new permission for this)", 'android.permission.WAKE_LOCK' in lman)
check("the provider answers .../setup_hold with one row, held 1 or 0, from SaverHold.held()",
      '"setup_hold".equals(uri.getLastPathSegment())' in prov and 'new MatrixCursor(new String[]{"held"})' in prov and "SaverHold.held() ? 1 : 0" in prov)
check("only the screensaver may ask (READ_NOW_PLAYING, signature)", 'android:readPermission="sa.urtv.home.READ_NOW_PLAYING"' in lman and "sa.urtv.home.READ_NOW_PLAYING" in sman)
check("the screensaver asks it, false when the launcher does not say (older one, missing, refused)",
      "content://sa.urtv.home.nowplaying/setup_hold" in bridge and 'cur.getColumnIndex("held") < 0' in bridge and "return false;" in bridge.split("static boolean setupWaiting")[1].split("\n    }\n")[0])
att = dream.split("public void onAttachedToWindow() {")[1].split("\n    }\n")[0]
check("the dream ends at once while it is held, before anything is shown or counted", "if (NowPlayingBridge.setupWaiting(this)) {" in att and "finish();" in att
      and att.index("setupWaiting") < att.index("Showing.enter()") < att.index("show(true)"))
check("the screen-on flag of a run stays as it was", "addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)" in act and "clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)" in act)
if not shutil.which("javac"):
    print("SKIP saver hold rule: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(SETUP / "SaverHoldRule.java"), str(ROOT / "tests/SaverHoldRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.setup.SaverHoldRun"], capture_output=True, text=True).stdout
print(out.strip())
check("every SaverHoldRule case ran", "ALL PASS" in out, out[-200:])
sys.exit(1 if failed else 0)
