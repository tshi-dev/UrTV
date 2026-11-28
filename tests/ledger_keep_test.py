#!/usr/bin/env python3
"""Known bug (tester step 92): a reinstall of UrTV loses the setup record, so the rescue left 18 apps off. The home sends a copy of the ledger to the rescue app (signature permission);
the rescue keeps it and uses it when the home does not answer. The protocol: the rules (LedgerKeep on a JVM), and the pieces that must agree."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
rd = lambda p: (ROOT / p).read_text(encoding="utf-8")
S = "android/launcher/src/main/java/sa/urtv/home/setup/"; R = "android/rescue/src/main/java/sa/urtv/home/setup/"
failed = 0
def check(n, ok, d=""):
    global failed
    print(("PASS " if ok else "FAIL ") + n + ("" if ok else f"  -> {d}")); failed += not ok
keep, push, recv = rd(S + "LedgerKeep.java"), rd(S + "LedgerPush.java"), rd(R + "LedgerReceiver.java")
man, gradle, act = rd("android/rescue/src/main/AndroidManifest.xml"), rd("android/rescue/build.gradle"), rd(R + "RescueActivity.java")
const = lambda n: re.search(n + r' = "([^"]+)"', keep).group(1)
check("the receiver in the rescue manifest is the one the home sends to, for the action the home sends, and only a holder of the signature permission can broadcast to it",
      f'android:name="{const("RECEIVER")}"' in man and f'<action android:name="{const("ACTION")}" />' in man and 'android:permission="sa.urtv.permission.EXTRAS"' in man.split("LedgerReceiver")[1].split("</receiver>")[0]
      and const("RESCUE_PACKAGE") == "sa.urtv.rescue" and "applicationId 'sa.urtv.rescue'" in gradle and const("PERMISSION") == "sa.urtv.permission.EXTRAS")
check("the home sends to that component only (explicit, never an implicit broadcast), with the permission, only when the list changed and the rescue app is installed, on a thread of its own",
      "new Intent(LedgerKeep.ACTION).setComponent(new ComponentName(LedgerKeep.RESCUE_PACKAGE, LedgerKeep.RECEIVER))" in push and "LedgerKeep.PERMISSION, new BroadcastReceiver()" in push and "LedgerKeep.worthPushing(" in push
      and "getPackageInfo(LedgerKeep.RESCUE_PACKAGE" in push and 'new Thread(' in push)
check("F120: the broadcast may reach a stopped rescue app (FLAG_INCLUDE_STOPPED_PACKAGES), is ordered, and the list is remembered as sent only when the rescue answers RESULT_OK (not when it was only sent)",
      "addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)" in push and "app.sendOrderedBroadcast(i, LedgerKeep.PERMISSION" in push and "getResultCode() == Activity.RESULT_OK" in push
      and "app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, sent).apply();" in push.split("onReceive")[1] and "putString(KEY, now)" not in push
      and "setResultCode(android.app.Activity.RESULT_OK)" in recv and "isOrderedBroadcast()" in recv)
check("F120: «إنقاذ» uses the copy when the home answered nothing or an empty ledger (LedgerKeep.useCopy), not only when it did not answer",
      "boolean fromCopy = LedgerKeep.useCopy(fromHome, LedgerReceiver.read(this));" in act and "RescuePlan.ledger(fromCopy ? LedgerReceiver.read(this) : fromHome)" in act)
check("the home pushes at each start of its process (HomeApp)", "sa.urtv.home.setup.LedgerPush.maybe(this);" in rd("android/launcher/src/main/java/sa/urtv/home/HomeApp.java"))
check("the rescue app compiles the same LedgerKeep (shared from the setup's folder)", "'LedgerKeep.java'" in gradle.split("def sharedSetup")[1].split("\n")[0])
check("«إنقاذ» uses the kept copy only when the home does not answer, and says so",
      "if (fromCopy) said.append(" in act)
check("the receiver writes the copy atomically (tmp then rename) in its own files folder, ignores another action, and an empty list never wipes the kept one",
      "LedgerKeep.keep(read(c), i.getStringExtra(LedgerKeep.EXTRA))" in recv and ".renameTo(" in recv and "LedgerKeep.ACTION.equals(i.getAction())" in recv and "c.getFilesDir()" in recv)
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / S / "LedgerKeep.java"), str(ROOT / "tests/LedgerKeepRun.java")], check=True)
        got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.LedgerKeepRun"], check=True, capture_output=True).stdout.decode("utf-8").splitlines())
    want = {"use copy": "true true false false true", "clean": "com.a.b,x.y_z,", "names none": "0 0", "keep new": "new.one,new.two,", "keep empty": "old.pkg, old.pkg, old.pkg,", "keep nothing": "[]", "push": "true false false true true", "long": "0"}
    for k, v in want.items():
        check(f"ledger keep: {k}", got.get(k) == v, got.get(k))
else:
    print("SKIP ledger keep on a JVM: no javac"); failed += 1
sys.exit(1 if failed else 0)
