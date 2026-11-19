#!/usr/bin/env python3
"""4.4 (R7, a user: «I deleted the app and the screen went black»): a TV whose stock home is off keeps a home when UrTV is removed. The rescue app
(«إنقاذ تلفزيونك») carries a HOME activity-alias that is OFF in its manifest; the setup arms it (read back) before the stock home goes and disarms it when the
stock home is back; the rescue's screen steps aside by itself when a stock home answers again. «عطّل واجهة المصنع» is offered only where the home switch
failed (HomeFails) and only with the rescue app installed. A TV switched the old way from the PC (no journal on the TV) gets Google's stock home back at
the home's start. Source pins, and the engine on SetupEngineRun's fake TV (tests/RescueHomeRun.java). Run: python3 tests/rescue_home_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
RESCUE = ROOT / "android/rescue/src/main"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

man = text(RESCUE / "AndroidManifest.xml")
alias = re.search(r"<activity-alias(.*?)</activity-alias>", man, re.S)
a = alias.group(1) if alias else ""
check("the rescue's manifest: one activity-alias sa.urtv.home.setup.RescueHome, OFF (enabled=false), MAIN + HOME + DEFAULT, onto FallbackHomeActivity",
      man.count("<activity-alias") == 1 and 'android:name="sa.urtv.home.setup.RescueHome"' in a and 'android:enabled="false"' in a
      and 'android:targetActivity="sa.urtv.home.setup.FallbackHomeActivity"' in a and "android.intent.category.HOME" in a and "android.intent.category.DEFAULT" in a, a)
check("no other component of the rescue answers HOME (the rescue screen stays in the apps list only)", man.count("android.intent.category.HOME") == 1)
gradle = text(ROOT / "android/rescue/build.gradle")
rh = text(SETUP / "RescueHome.java")
check("RescueHome's alias is the manifest's, in the rescue's package (applicationId)", "applicationId 'sa.urtv.rescue'" in gradle
      and 'static final String PKG = "sa.urtv.rescue";' in rh and 'static final String ALIAS_CLASS = "sa.urtv.home.setup.RescueHome";' in rh)
check("the rescue app is built with the same RescueHome (build.gradle's sharedSetup)", re.search(r"def sharedSetup = \[[^\]]*'RescueHome\.java'", gradle) is not None)

eng = text(SETUP / "SetupEngine.java")
fo = eng[eng.index("Result factoryOff()"):eng.index("Result restoreFactory()")]
check("«عطّل واجهة المصنع»: the alias is armed and read back before anything is remembered, before the switch and before the stock home goes; not armed: disarmed and refused",
      fo.index("sh.run(RescueHome.ARM);") < fo.index("RescueHome.armed(text(RescueHome.HOMES))") < fo.index("remember(") < fo.index("Result h = home();") < fo.index('"pm disable-user --user 0 " + stockPkg')
      and "{ sh.run(RescueHome.DISARM); return failed(RescueHome.NOT_ARMED); }" in fo)
of = eng[eng.index("FactoryOffer factoryOffer()"):eng.index("boolean factoryIsOff()")]
check("the offer: only where the switch failed (HomeFails) and only with the rescue app installed, both before anything is asked of the person",
      "HomeFails.count(store.get(HomeFails.FILE)) < 1) return new FactoryOffer(RescueHome.NOT_NEEDED" in of and "!pk.contains(RescueHome.PKG)) return new FactoryOffer(RescueHome.NEED" in of
      and of.index("RescueHome.NOT_NEEDED") < of.index("wizardOwnsWifi()"))
psb = eng[eng.index("private boolean putStockBack("):]
psb = psb[:psb.index("\n    }\n")]
rf = eng[eng.index("Result restoreFactory()"):]
ms = eng[eng.index("Result migrateStockHome()"):eng.index("Result migrateStockNoJournal()")]
check("every place that turns the stock home back on disarms the alias right after (the put-back, the one-press restore, the migrations)",
      psb.index('sh.run("pm enable " + stockPkg);') < psb.index("sh.run(RescueHome.DISARM);") and "sh.run(RescueHome.DISARM); }" in rf
      and "sh.run(RescueHome.DISARM);" in ms and "sh.run(RescueHome.DISARM);" in eng[eng.index("Result migrateStockNoJournal()"):eng.index("// ---- 4.4: the Home key")])
rs = text(ROOT / "optimize/restore.sh")
check("the PC's restore (and reset, through it) disarms the alias right after the stock home is back",
      rs.index('echo "pm enable ${STOCK_LAUNCHER%%/*}"') < rs.index('echo "pm disable sa.urtv.rescue/sa.urtv.home.setup.RescueHome 2> /dev/null"'))
check("RescueHome.DISARM is the PC's line to the letter", 'static final String DISARM = "pm disable " + ALIAS + " 2> /dev/null";' in rh)

fb = text(RESCUE / "java/sa/urtv/home/setup/FallbackHomeActivity.java")
check("the fallback screen: «رجّع واجهة المصنع» (the rescue's adb), «افتح الإعدادات» with the way by hand, the TV's apps; Back goes nowhere",
      "new Intent(this, RescueActivity.class)" in fb and "Settings.ACTION_SETTINGS" in fb and "← تفعيل" in fb and "CATEGORY_LEANBACK_LAUNCHER" in fb
      and "public void onBackPressed() { }" in fb)
res = fb[fb.index("protected void onResume()"):]
check("it steps aside by itself when a stock home answers Home again (its own alias off, then Home)",
      "RescueHome.stockHomeOn(homePackages())" in res and "COMPONENT_ENABLED_STATE_DISABLED" in res and res.index("setComponentEnabledSetting") < res.index("CATEGORY_HOME"))
check("the rescue's own restore turns the alias off once Home is the stock one's again", "if (!p.home.isEmpty())" in text(RESCUE / "java/sa/urtv/home/setup/RescueActivity.java")
      and "RescueHome.ALIAS_CLASS" in text(RESCUE / "java/sa/urtv/home/setup/RescueActivity.java"))
check("the fallback screen runs on Android 5 (the rescue's minSdk 21): no List.sort", ".sort(" not in fb.replace("Collections.sort(", ""))

hook = text(SETUP / "SetupHook.java")
nj = hook[hook.index("private static void noJournal("):]
check("at the home's start, a TV switched from the PC: asked of the app's own package manager first; adb only where this app's key exists (no «السماح بتصحيح USB؟» at a start), else told once",
      "queryIntentActivities(" in nj and nj.index("if (!AdbKeys.exists(app))") < nj.index("SetupSession.openAny(") and "StockMigration.TOLD_MARK" in nj and "migrateStockNoJournal()" in nj
      and "noJournal(app, store);" in hook)

if not shutil.which("javac"):
    print("SKIP the rescue home scenarios: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in text(p)] + [HOME / "phone/UpdateRules.java", HOME / "SuggestedApps.java", HOME / "ExtrasWords.java"]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java"), str(ROOT / "tests/RescueHomeRun.java")], capture_output=True, text=True)
    check("the engine, the fake TV and the runner compile", r.returncode == 0, r.stderr[-1500:])
    if r.returncode:
        sys.exit(1)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.setup.RescueHomeRun"], capture_output=True, text=True)
    got = {l.split(" ", 1)[0]: l for l in out.stdout.splitlines()}
STOCK = "com.google.android.apps.tv.launcherx"
check("armed(): the alias's line, whatever the spacing; nothing or another home: not armed", got.get("ARMED") == "ARMED true false false", got.get("ARMED"))
check("stockHomeOn(): ours, the rescue, Settings and the setup wizard are not a stock home", got.get("STOCKON") == "STOCKON false true false false", got.get("STOCKON"))
check("packages(): the components' packages of the answer", got.get("PKGS") == f"PKGS [{STOCK}, sa.urtv.rescue]", got.get("PKGS"))
check("googleStockOff(): launcherx first, else tvlauncher; another maker's home is never guessed", got.get("GOOGLE") == f"GOOGLE {STOCK} com.google.android.tvlauncher null null", got.get("GOOGLE"))
check("R1 the step arms the alias before the stock home goes; Home stays ours", got.get("R1") == "R1 DONE armed=true armBeforeStockOff=true stockOff=true homeOurs=true", (got.get("R1"), out.stderr[-500:]))
check("R2 the one-press restore: the stock home back, the alias off", got.get("R2") == "R2 DONE armed=false stockOn=true", got.get("R2"))
check("R3 no rescue app: offered with the reason, the step refuses, nothing changed", got.get("R3") == "R3 offer=true FAILED changes=0", got.get("R3"))
check("R4 the TV does not turn the alias on: nothing turned off, the alias put back off", got.get("R4") == "R4 FAILED said=true disables=0 armed=false disarmSent=true", got.get("R4"))
check("R5 the switch never failed here: not offered (Home opens ours without it), nothing changed", got.get("R5") == "R5 notNeeded=true changes=0", got.get("R5"))
check("R6 a TV switched from the PC: Google's stock home back on, ours still Home, marked, once", got.get("R6") == "R6 DONE stockOn=true role=sa.urtv.home mark=true again=true", got.get("R6"))
check("R7 a stock home already answers Home: nothing done, marked «none»", got.get("R7") == "R7 null enables=0 mark=true", got.get("R7"))
check("R8 the person chose «عطّل واجهة المصنع»: never undone at a start", got.get("R8") == "R8 null enables=0 stockOff=true", got.get("R8"))
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
