"""SetupRunner (android/launcher/.../setup/SetupRunner.java): the one setup run of the process, used by the TV's own button «جهّز كل شي» and later by
the phone. One run at a time, a step model with reasons, a bounded ring of numbered lines («the lines since n»), ONE way to set up (the full run: the
old five-step «ابدأ» is gone), the start screen's plan with the TV's real counts and nothing changed before «ابدأ», the prompt's 20 s notice, the pause
for the Wi-Fi when the setup wizard owns it and the safe order after it, a TV with only the home app set up end to end, the undo after a full run,
and a full run that resumes from its step pointer after a restart without running a step that is not safe to repeat. On a plain JVM against the fake
TV of tests/SetupEngineRun.java, and once end to end over the real ADB client and the fake adbd of tests/AdbClientRun.java; no Android, no TV.

    python3 tests/setup_runner_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

runner = (SETUP / "SetupRunner.java").read_text(encoding="utf-8")
act = (SETUP / "SetupActivity.java").read_text(encoding="utf-8")
boot = (SETUP / "SetupBootReceiver.java").read_text(encoding="utf-8")
session = (SETUP / "SetupSession.java").read_text(encoding="utf-8")
service = (SETUP / "SetupService.java").read_text(encoding="utf-8")
manifest = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("the runner is plain Java (no Android): the tests run it on a JVM", "import android." not in runner)
check("the setup screen only starts and reads through the runner: no session, no engine step, no thread of its own for a run",
      "SetupRunner.get().start(" in act and "SetupSession.open" not in act and not re.search(r"\.(precheck|grants|debloat|screensaver|home|undo)\(\)", act)
      and '"setup-run"' not in act)
btn = act[act.index("private TextView button("):]
btn = btn[:btn.index("\n    }\n")]
check("the screen keeps the PIN gate and the buttons' guard against overlays (every button comes from button(), which sets it)",
      "SetupGate.guard" in act and "setFilterTouchesWhenObscured(true)" in btn and act.count("setFocusable(true)") == 1)
check("one way to set up: the screen starts only the full run or the undo (the old five-step run is gone from the runner too)",
      "SetupRunner.Kind.FULL" in act and "Kind.SETUP" not in act and "SETUP" not in runner[runner.index("enum Kind"):runner.index("enum State")] and "SetupEngine.plan()" not in act)
check("the screen: «جهّز كل شي», the plan with «ابدأ» and «إلغاء», a box per suggested app (4.3; SmartTube and TV Bro ticked at first), «كمّل» for the Wi-Fi, the QR of the report, «تراجع»",
      all(w in act for w in ('button(S.t("جهّز كل شي"))', 'S.t("ابدأ")', 'S.t("إلغاء")', 'S.t("كمّل")', "private boolean extras = true;", "QrCode.encodeLarge(url)",
                             '"تراجع (يرجّع التلفزيون كما كان)"', "SetupRunner.get().confirm(new java.util.ArrayList<>(picks))", "SuggestedApps.forSetup(android.os.Build.VERSION.SDK_INT)", "SetupRunner.get().cancel()", "SetupRunner.PROMPT_LATE", "SetupRunner.WIFI_ASK_TEXT")))
pv = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupProgressView.java").read_text(encoding="utf-8")
check("the progress screen (design/43): five phases on a rail (a tick done, the accent now, dim next, a cross failed) and the step in hand big, with its line, share and time; the USB prompt is one card that replaces the main area",
      all(w in act for w in ("private static final String[][] PHASES", "SetupProgressView.Model modelOf(", "progress.show(modelOf(r, false))", "m.prompt = r.promptWaiting();", "m.promptLate = r.promptLate();"))
      and all(w in pv for w in ("private final class Mark extends View", "private final class DialogArt extends View", "promptCard.setVisibility(showPrompt ? VISIBLE : GONE);", "running.setVisibility(!showPrompt && !m.finished ? VISIBLE : GONE);")))
check("the counts follow the person's digits setting (the screens' DigitsText)", "DigitsText.convert(s, Digits.latin())" in act)
check("the 20 s notice is the owner's wording", 'PROMPT_LATE = "ما ظهرت الرسالة؟ أعد تشغيل التلفزيون وافتح «إعداد تلفزيوني» واضغط «جهّز كل شي» وخلك عند التلفزيون"' in runner)
check("a run lives in a foreground service (it outlives the screen), not exported", 'android:name=".setup.SetupService" android:exported="false"' in manifest
      and "startForeground(" in service and "startForegroundService" in service)
check("the TV's start resumes a full run when it restarted the TV on purpose, or (4.3.3) when a restart nobody asked for cut it (twice at most); 4.4: the ADB key is kept between the steps",
      "SetupRunner.resumeAtBoot(new FileStore(app))" in boot and "awaitingRestart(store)" in runner[runner.index("static boolean resumeAtBoot"):] and "SetupRunner.get().resume(" in boot and "static void keepKey(" in session
      and "awaitingWifi" not in boot)
hold = (SETUP / "SetupHold.java").read_text(encoding="utf-8")
npp = (SETUP.parent / "tilawah/NowPlayingProvider.java").read_text(encoding="utf-8")
dream = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/ScreenDream.java").read_text(encoding="utf-8")
bridge = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java").read_text(encoding="utf-8")
check("our screensaver steps aside while a run goes: the launcher says so (same-key provider, memory only), the dream wakes up (Android 10+) or ends",
      "return SetupRunner.get().running();" in hold and '"setup".equals(uri.getLastPathSegment())' in npp and "SetupHold.on()" in npp
      and "content://sa.urtv.home.nowplaying/setup" in bridge and "if (!NowPlayingBridge.setupRunning(this)) return;" in dream and "wakeUp(); else finish();" in dream)
check("…and the service holds the screen on while a run goes (a TCL ignored the window's flag), let go at the end",
      "SCREEN_BRIGHT_WAKE_LOCK" in service and "hold(true);" in service and "hold(false);" in service)
check("the step pointer is written BEFORE a step runs", runner.index('writeState(host.store(), o, d.id, "started")') < runner.index("r = d.body.run(c);"))
check("the home switch is the one step never run again by itself after a cut", '"home", false' in runner and runner.count(", false, c ->") == 1)

if not shutil.which("javac"):
    print("SKIP the runner on a JVM: no javac on this machine")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")] + [SETUP.parent / "phone/UpdateRules.java", SETUP.parent / "SuggestedApps.java", SETUP.parent / "ExtrasWords.java"]   # (read only: its rule for updates)
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java"), str(ROOT / "tests/SetupRunnerRun.java"), str(ROOT / "tests/AdbClientRun.java")], capture_output=True, text=True)
    check("the pure classes and the runner's harness compile", r.returncode == 0, r.stderr)
    if r.returncode:
        sys.exit(1)
    OUT = Path(tempfile.mkdtemp())
    run = subprocess.run(["java", "-Dassets=" + str(ROOT / "android/launcher/src/main/assets"), "-Dout=" + str(OUT), "-cp", tmp, "sa.urtv.home.setup.SetupRunnerRun"], capture_output=True, text=True, timeout=300, encoding="utf-8")
    got = {l.split()[0]: l for l in run.stdout.splitlines() if l.strip()}
    if run.returncode:
        print(run.stderr[-2000:])

PLAN = (got.get("PLAN") or "PLAN ").split(" ", 1)[1].split(",")
FULL_DONE = ",".join(f"{i}:DONE" for i in PLAN)
check("the full plan, in the owner's order: precheck, backup, keep, measure, the safe list, community, tweaks, the screensaver, the extras, auto-start "
      "(after the screensaver is installed: it only covers apps on the TV), the home switch, measure, report",
      PLAN == ["precheck", "backup", "keep", "before", "grants", "safe", "community", "tweaks", "install-saver", "screensaver", "extras", "autostart", "home", "after", "report"], PLAN)
check("R1: «جهّز كل شي» on a healthy TV: every step in order, all done; the run is over; the key is let go once; the screen is told of changes",
      got.get("R1") == f"R1 STARTED {FULL_DONE} running=false finished=1 bad=false listened=true", got.get("R1"))
check("a fresh full run and an undo first let go of a key a run before kept (when nothing is pending); the community answers do not (tester step 18)",
      got.get("RK") == "RK 1 2 2", got.get("RK"))
check("4.4 F20: the runner tells the host to delete the ADB key once the undo is done, and not after a normal done full run or the answers",
      got.get("RKD") == "RKD 0 1 1", got.get("RKD"))
check("step 27: a full run asks the home to open the setup screen again (once), and the screen being shown takes the ask back", got.get("R1reopen") == "R1reopen true false false", got.get("R1reopen"))
hm = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
check("step 27: our home, when it comes in front, opens the setup screen once if it was left behind (a window in front may start an activity)", "SetupReopen.take()" in hm.split("protected void onResume()")[1][:600] and "FLAG_ACTIVITY_REORDER_TO_FRONT" in hm.split("SetupReopen.take()")[1][:400])
check("the home switch step raises the ask after it ran", "c.runner.wantScreen();" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupRunner.java").read_text(encoding="utf-8"))
check("the setup screen is the first to cancel the ask (onResume and show)", "SetupRunner.get().screenShown();" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupActivity.java").read_text(encoding="utf-8"))
check("the setup screen\'s end is its own sentence"[:0] or "R1: the end is the setup screen's own sentence", (got.get("R1end") or "").startswith("R1end انتهى الإعداد. اضغط زر الرئيسية في الريموت."), got.get("R1end"))
check("R1: numbered lines, in order, «since n» gives the ones after n; the prompt line comes first; a finished run leaves no resume pointer",
      got.get("R1lines") == "R1lines true true true 0 prompt=true" and got.get("R1nostate") == "R1nostate ", (got.get("R1lines"), got.get("R1nostate")))
check("R2: a second start (undo, or the full run) while one runs is BUSY and is not queued; once it ends a new one starts",
      got.get("R2") == "R2 STARTED BUSY BUSY queued=1 kind=FULL after=STARTED", got.get("R2"))
check("R3: the ring keeps the newest 400 lines with their numbers", got.get("R3") == "R3 400 51 l50 450 3", got.get("R3"))
check("R4: a TV that answers nothing: the precheck fails, the rest is skipped, not one change, «توقّفنا قبل أي تغيير.» in red",
      got.get("R4") == "R4 " + ",".join(f"{i}:" + ("FAILED" if i == "precheck" else "SKIPPED") for i in PLAN) + " changes=0 bad=true توقّفنا قبل أي تغيير.", got.get("R4"))
check("R5: debugging off / the prompt not answered: the screen's sentences, in red, the host told, not running; no answer offers «جهّز كل شي» again, debugging off does not",
      got.get("R5") == "R5 true 1 true true running=false retry=false/true", got.get("R5"))
check("R5c (4.4, step 86 B): «إلغاء» on «السماح بتصحيح USB؟»: «ضغطت «إلغاء». اضغط «جهّز كل شي» مرة ثانية…», nothing changed, «جهّز كل شي» offered again, not running",
      got.get("R5c") == "R5c true true retry=true changes=0 running=false", got.get("R5c"))
check("R5e (4.4, step 88): no dialog came (nothing covered the setup screen for 30 s): the wait ends with «ما ظهرت رسالة السماح؟ أعد تشغيل التلفزيون…» and «جهّز كل شي» again; once covered (the dialog, or the person left) it waits on",
      got.get("R5e") == "R5e false true true covered=false retry=true", got.get("R5e"))
check("R5g (4.4): the end sentence names the steps that did not finish, with their reason (no «التفاصيل فوق»)",
      got.get("R5g") == "R5g : الشاشة الرئيسية (ما قدرنا)؛ TV Bro و SmartTube.", got.get("R5g"))
check("R5d (4.4): the prompt counts as «إلغاء» only once the screen saw it close AND no answer came for 6 s; nothing while it was never seen closing or no prompt waits",
      got.get("R5d") == "R5d false false false true false", got.get("R5d"))
check("R6: the undo is one step through the same runner, with its old sentence", got.get("R6") == "R6 undo:DONE com.google.android.apps.tv.launcherx true", got.get("R6"))
check("R7: a full run cut in the middle of the home switch: after the restart the switch is NOT run again (said as failed), the pointer is cleared at the end",
      got.get("R7") == "R7 true STARTED " + FULL_DONE.replace("home:DONE", "home:FAILED").replace("extras:DONE", "extras:SKIPPED") + " roleAdds=0 disables=0 extras=false cleared=true resumableAfter=false opens=1", got.get("R7"))
check("R8: a full run cut in the middle of the safe list runs that step again (its ledger was written first): the same ledger, done",
      got.get("R8") == f"R8 {FULL_DONE} sameLedger=true", got.get("R8"))
check("R9: a resumed run waits while the home switch is still «switching» (the boot path puts the stock home back first)", (got.get("R9") or "").startswith("R9 waited=true"), got.get("R9"))
check("R10: the TV does not come back after the restart: ten minutes of tries, then said in red; the pointer stays for «ابدأ», nothing resumes by itself",
      got.get("R10") == "R10 opens=40 true true kept=true auto=false", got.get("R10"))
check("R11: a pointer older than a day is not resumed", got.get("R11") == "R11 false false", got.get("R11"))
check("R13: a full run then the undo, both through the runner, on a TCL: all done, tweaks and auto-start were on, the TV is as it was, the journals retired",
      got.get("R13") == "R13 true during=0.5/45/allow same=true undo:DONE journal=true done=true auto=true", got.get("R13"))
check("R14: the community questions, and the answers through the runner: «yes» turned off, «no» left on, both leave the questions",
      got.get("R14") == "R14 2 true STARTED answers:DONE off=true/false left=[] false", got.get("R14"))
check("R15: the full run with the accurate measurement: it restarts the TV before, stops, and goes on after the boot by itself; again after; never twice for one reading",
      got.get("R15") == f"R15 first=null await=true reboots=1 phase=before second=null reboots=2 third={FULL_DONE} reboots=2 kinds=[before restart|after restart] rows=true", got.get("R15"))
check("R15: the screensaver was downloaded from apk-latest and streamed to `cmd package install -S`, then made the screensaver; TV Bro and SmartTube (the TV's processor's build) too",
      got.get("R15i") == "R15i [https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-screensaver.apk, https://github.com/truefedex/tv-bro/releases/download/v2.0.1/tvbro-2.0.1-generic-geckoExcluded.apk, "
      "https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk] [cmd package install -r -S 19, cmd package install -r -S 22, cmd package install -r -S 20] "
      "installedByUs=[sa.urtv.screensaver, com.phlox.tvwebbrowser, org.smarttube.stable] saver=sa.urtv.screensaver/.ScreenDream timeout=1800000->back", got.get("R15i"))
check("R15: the screensaver timeout was stretched for the reading and put back exactly; the ADB key would stay only while it waits for the restart",
      (got.get("R15t") or "") == "R15t timeoutFile=[] value=null awaitAfter=false", got.get("R15t"))
FULL_SKIPPED_MEASURE = FULL_DONE.replace("before:DONE", "before:SKIPPED").replace("after:DONE", "after:SKIPPED")
check("R32 (4.2.1): the run is marked as waiting for the restart BEFORE `reboot` is sent (this app dies as the TV goes down), so the TV's start resumes it "
      "and the reading is taken after the boot; the restart is said before the 15 s wait, not after it",
      got.get("R32") == "R32 resumesAtBoot=true saidBeforeWait=true resumed=true precheck:DONE,backup:DONE,keep:DONE,before:DONE kinds=[before restart]", got.get("R32"))
check("R31 (4.2.1): a measurement command that hangs is cut by the TV's own timeout (toybox, 20 s) and the setup goes on to the end",
      got.get("R31limit") == f"R31limit {FULL_DONE} bad=false said=0 bounded=true unbounded=0 sysMeminfo=4 skipped=false timeout=null row=true", got.get("R31limit"))
check("R31: without a timeout on the TV the read gives up, both readings say «تعذّر القياس، كمّلنا.» and the setup still goes on to the end; the screensaver timeout is put back",
      got.get("R31nolimit") == f"R31nolimit {FULL_SKIPPED_MEASURE} bad=false said=2 bounded=false unbounded=2 sysMeminfo=2 skipped=false timeout=null row=false", got.get("R31nolimit"))
check("R31: a low-memory TV (under about 1 GB) is never asked for the whole system's `dumpsys meminfo`; the reading is still taken",
      got.get("R31lowram") == f"R31lowram {FULL_DONE} bad=false said=0 bounded=true unbounded=0 sysMeminfo=0 skipped=true timeout=null row=true", got.get("R31lowram"))
check("R16: a TV whose adb would not come back on 5555 after a restart: no restart at all, both readings «بدون إعادة تشغيل», the run in one go",
      got.get("R16") == f"R16 {FULL_DONE} reboots=0 kinds=[before now|after now]", got.get("R16"))
check("R17q (4.4, a Skyworth IRIS on Android 14 stopped at 26 %): by default the run never restarts the TV (quick readings before and after), even where adb survives a restart; its end is kept",
      got.get("R17q") == "R17q reboots=0 kinds=[before now,after now] await=false ended=true kept=ok", got.get("R17q"))
check("R17e (4.4, R2): the kept end comes back in a new process (the setup screen shows it), bad or not",
      got.get("R17e") == "R17e same=true bad=true", got.get("R17e"))
SR = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupRunner.java").read_text(encoding="utf-8")
SH = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupHook.java").read_text(encoding="utf-8")
SA = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupActivity.java").read_text(encoding="utf-8")
check("4.4: the full measure only when asked (Options.fullMeasure, kept across the restart in runner.txt and in the confirmed options); the preview says no restart then",
      "boolean fullMeasure;" in SR and "measure(label, c.options.fullMeasure && !switching(c.store), !c.options.fullMeasure)" in SR
      and 'o.fullMeasure = "1".equals(saved.get("measure"));' in SR and '(o.fullMeasure ? "\\nmeasure=1" : "")' in SR and "o.fullMeasure = options.fullMeasure;" in SR
      and "p.restarts = c.options != null && c.options.fullMeasure" in SR)
check("4.4 (R2): the end is written for every full run that ends, let go when a new run starts, shown by the setup screen of a new process, and said once at UrTV's next start when it was bad",
      'host.store().put(END_FILE, (e.bad ? "bad" : "ok")' in SR and 'try { host.store().put(END_FILE, ""); }' in SR and "SetupRunner.get().restoreEnd(new FileStore(this));" in SA
      and "endNotSeen(app);" in SH and 'store.put(SetupRunner.END_FILE, "bad told\\n"' in SH)
check("4.4: the full measure is a tester's switch only (dock.sh debug fullmeasure, DebugFlags), no button; the PC's is run.sh --measure",
      'o.fullMeasure = sa.urtv.home.DebugFlags.on(this, "fullmeasure");' in SA and '"fullmeasure"' in (ROOT / "android/launcher/src/main/java/sa/urtv/home/DebugFlags.java").read_text(encoding="utf-8")
      and "|fullmeasure|" in (ROOT / "optimize/dock.sh").read_text(encoding="utf-8") and "dock.sh debug fullmeasure on" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))
check("R17: the TV ignored the restart (the same boot after the resume): it is NOT asked again; the reading is taken without one",
      got.get("R17") == "R17 reboots=1 kinds=[before now] beforeDone=true", got.get("R17"))
check("R18: a fresh install is refused unless the APK is signed with this app's own key; nothing is installed or recorded then",
      got.get("R18") == "R18 install-saver:FAILED saverInstalled=false recorded=false", got.get("R18"))
check("R19: the undo after the full run removes exactly what the setup installed (the screensaver, TV Bro, SmartTube), never this app",
      got.get("R19") == "R19 undo:DONE saver=false tvbro=false smarttube=false home=true record=[]", got.get("R19"))
check("R20: «no extras» (the person unticked them): skipped, nothing downloaded for them", got.get("R20") == "R20 extras:SKIPPED downloads=1", got.get("R20"))
check("R20b (4.3): only TV Bro ticked (and a Play Store app's id): TV Bro alone is fetched and installed, the store app never",
      got.get("R20b") == "R20b extras:DONE github=[https://github.com/truefedex/tv-bro/releases/download/v2.0.1/tvbro-2.0.1-generic-geckoExcluded.apk] tvbro=true smarttube=false vlc=false", got.get("R20b"))
check("4.3: the ticks are saved with the run and read back at a resume (a run saved before 4.3 has none: the first ticks)",
      '(o.picks == null ? "" : "\\npicks=" + sa.urtv.home.SuggestedApps.join(o.picks))' in runner
      and 'if (saved.containsKey("picks")) o.picks = sa.urtv.home.SuggestedApps.parse(saved.get("picks"));' in runner)
check("R20c (4.3): nothing ticked: the extras are skipped and nothing is fetched from GitHub for them", got.get("R20c") == "R20c extras:SKIPPED github=0", got.get("R20c"))
check("R21: the start screen: after the precheck the run reads this TV (the safe list's real count, the screensaver missing) with not one change, waits for «ابدأ», then runs it all",
      got.get("R21") == "R21 plan=true safe=true saver=false homeOurs=false wifiAsk=false changesBefore=0 asked=true line=true saverLine=true true", got.get("R21"))
check("R22: «إلغاء», or no answer for ten minutes (1200 waits of 0.5 s): the run ends, not one change, nothing to resume, the key let go, not in red",
      got.get("R22") == "R22 true changes=0/0 end=true/true bad=false waited=1200 resumable=false finished=11", got.get("R22"))
check("R23: the prompt gets no answer: after 20 s the owner's notice («ما ظهرت الرسالة؟ …»), then the «not accepted» sentence with the restart advice; nothing changed",
      got.get("R23") == "R23 late=true said=true end=true bad=true lateAfter=false changes=0 holds=false", got.get("R23"))
check("R23b: our screensaver is held off exactly while a run goes (from the prompt on), and free again after it", got.get("R23b") == "R23b false true false", got.get("R23b"))
check("R24: the setup wizard owns the saved Wi-Fi: the run pauses before the home switch (every step before it done, nothing of the switch done), asks once, keeps the key; no boot resume",
      got.get("R24") == "R24 wifi=true bad=false ask=true home=home:PENDING beforeHomeDone=true roleAdds=0 stockOff=false wizardOff=false asked=true await=true keyKept=true autoBoot=false", got.get("R24"))
check("R25: the network moved (forgotten and joined again): «كمّل» resumes at the switch in the safe order: wizard off, Wi-Fi read, our home in front, then the stock home off and the role; "
      "the journal names each step before it acts; not asked again; the key let go",
      got.get("R25") == "R25 STARTED true order=true steps=[armed, wizard_off, wifi_check, home_start, stock_off, verify] journal=done home=sa.urtv.home/.MainActivity askCleared=true wifi=true/2 asks=0 keyLetGo=true", got.get("R25"))
check("R26: safe order, the Wi-Fi went with the wizard: everything put back (wizard on, the stock home never off, Home checked), «restored», said, never this order again",
      got.get("R26") == "R26 home=home:FAILED stockEverOff=false wizardOn=true roleBack=com.google.android.apps.tv.launcherx journal=restored steps=[armed, wizard_off, wifi_check] never=true disarmed=true said=1", got.get("R26"))
check("R27: safe order, our home not in front when started: put back, the stock home never off, «تُرك» (not an error)",
      got.get("R27") == "R27 home=home:SKIPPED stockEverOff=false wizardOn=true journal=restored steps=[armed, wizard_off, wifi_check, home_start]", got.get("R27"))
check("R28: the network was not moved («كمّل» anyway): not asked again, the switch as before with the wizard left on", got.get("R28") == "R28 went-on wizardEverOff=false steps=[armed, switch, verify] said=1 true", got.get("R28"))
check("R29: a TCL with only the home app: every step in the plan's order, the screensaver from apk-latest installed and set, auto-start (and ASSOC_START) for both apps, our Home; "
      "then the undo: the TV as it was, what the run installed removed, this app kept",
      got.get("R29") == "R29 true order=true during=true/sa.urtv.screensaver/.ScreenDream/allow/allow/sa.urtv.home/.MainActivity downloads=true undo:DONE same=true home=com.google.android.apps.tv.launcherx/.home.HomeActivity self=true", got.get("R29"))
check("R30: end to end over the real ADB client and the fake adbd: «السماح دائماً» once, the full run (three APKs streamed), Home ours; the undo on a new connection with no second prompt",
      got.get("R30") == "R30 true true/sa.urtv.screensaver/.ScreenDream/sa.urtv.home/.MainActivity execs=3 prompts=1/1 undo:DONE same=true shellCmds=true", got.get("R30"))
skipped_rest = ",".join(f"{i}:" + ("DONE" if i == "precheck" else "FAILED" if i == "backup" else "SKIPPED") for i in PLAN)
check("R12: the backup is the full run's gate: a nearly empty package list saves nothing and nothing after it runs (not one change), «توقّفنا قبل أي تغيير.»",
      got.get("R12") == f"R12 {skipped_rest} backup=null changes=0 true توقّفنا قبل أي تغيير.", got.get("R12"))
# ---- 4.3.3: a drop of the TV's adb in a step that may run twice; a restart nobody asked for
check("R33: the adb drops in the precheck: one new connection, the step again (said on screen and in steps.log), the run ends well",
      got.get("R33") == "R33 opens=2 done=true said=true stamped=true bad=false", got.get("R33"))
check("R33b: it keeps dropping: two new connections at most, then it stops as before; the pointer stays for «جهّز كل شي» and no boot resumes it",
      got.get("R33b") == "R33b opens=3 bad=true pointer=true boot=false", got.get("R33b"))
check("R33c: a drop in the home switch is never run again by itself", got.get("R33c") == "R33c true reconnects=false", got.get("R33c"))
check("R34: a run a restart cut in the middle is resumed by the boot twice at most; one that ended (no live) or waits for the Wi-Fi is not; the key stays only for a cut one",
      got.get("R34") == "R34 true true false keyKept=false/false ended=false wifi=false", got.get("R34"))
check("R34b: the resumed run goes on from its pointer to the end", got.get("R34b") == "R34b true true tweaks:DONE home=home:DONE cleared=true", got.get("R34b"))
# ---- 4.4: a Google TV whose launcherx keeps the Home key: the add-on's watch set up over the TV's adb, the key checked the person's way, the report honest
check("R35: the Home key opens the stock home with the role ours: «إضافات تلفزيونك» is installed and turned on over the TV's adb, the key is checked again and opens UrTV; nothing is turned off; the report's «Home:» is what the key really opened",
      got.get("R35") == "R35 home:DONE extras=true listed=true switch=1 role=sa.urtv.home stockOff=false wizardOff=false fails=0 reportHome=sa.urtv.home/.MainActivity safety=1/1", got.get("R35"))
check("R35b: the TV refuses the first bind (TclAppBoot): the list goes off and on again, then it binds", got.get("R35b") == "R35b home:DONE puts=2", got.get("R35b"))
check("R35w (4.4, step 88): once the add-on is bound it is told «wanted» BEFORE the Home key is checked (after a 15/15 setup it said «ours not chosen»), once; the setup page is brought back after each check",
      (got.get("R35w") or "").startswith("R35w told=[true] back=") and int(got.get("R35w").split("back=")[1]) >= 2, got.get("R35w"))
check("R35c: it never binds: «ما قدرنا نخلي زر الرئيسية يفتح تلفزيونك على هذا التلفزيون»; 4.4 (step 86 B): our home KEEPS the role (nothing put back: that undid the owner's switch), the stock home never off, the switch confirmed (the safety switch off), counted once, the rest done, the report honest (Home opens the stock home)",
      (got.get("R35c") or "").startswith("R35c home:FAILED said=true role=sa.urtv.home stockOff=false fails=1 after=true reportHome=com.google.android.apps.tv.launcherx/")
      and (got.get("R35c") or "").endswith(" safety=1/1"), got.get("R35c"))
check("R35e (4.4, step 88 T8): «تراجع» takes the add-on out of the list even when it was turned on elsewhere (nothing recorded), keeps the TV's other service and the switch, and clears «wanted»",
      got.get("R35e") == "R35e listed=com.example.keys/.Svc switch=1 wanted=", got.get("R35e"))
check("R35d: «تراجع» turns the add-on's service off again (the TV had none: the list and the switch as they were)", got.get("R35d") == "R35d undo:DONE listed=none switch=none", got.get("R35d"))
sys.exit(1 if failed else 0)
