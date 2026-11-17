#!/usr/bin/env python3
"""4.5 «نسخة الإعدادات» (docs/BACKUP.md): the link between our two apps. The screensaver's ConfigProvider answers backup_export / backup_check /
backup_apply only for PHONE_SETTINGS (signature: our home), builds the part with SaverFile (the computer's schema-1 keys), checks with
SaverReceiver.set's own checks in a dry run that saves nothing, and on a refused apply puts the part from before back. Read from the source.
Run: python3 tests/backup_bridge_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
prov, recv = (A / "ConfigProvider.java").read_text(encoding="utf-8"), (A / "SaverReceiver.java").read_text(encoding="utf-8")
man = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
bk = prov.split("private Bundle backup(String method, String arg) {")[1].split("\n    }\n")[0]
check("only our home: PHONE_SETTINGS, a signature permission, checked before anything", bk.index("checkCallingPermission(PHONE_PERMISSION)") < bk.index("new Prefs(c)")
      and 'android:name="sa.urtv.screensaver.PHONE_SETTINGS"\n        android:protectionLevel="signature"' in man)
check("backup_ is routed to it before the phone_ calls", 'if (method != null && method.startsWith("backup_")) return backup(method, arg);' in prov)
check("export: the part from the app's own GET answer (SaverFile.fromGet(describe))", "SaverFile.fromGet(SaverReceiver.describe(c, p))" in bk and 'out.putString("json", SaverFile.json(now))' in bk)
check("check and apply: a strict reader, then SaverReceiver.set's own checks in a dry run first", "SaverFile.parseJson(" in bk and "SaverReceiver.set(c, p, intentOf(ex), true)" in bk
      and bk.index("intentOf(ex), true)") < bk.index("r = SaverReceiver.set(c, p, intentOf(ex));"))
check("apply: the part from before is kept and put back whole if the TV refuses", 'out.putString("before", SaverFile.json(now))' in bk and "if (!ok && now != null) SaverReceiver.set(c, p, intentOf(SaverFile.extras(now)));" in bk)
st = recv.split("static String set(Context c, Prefs p, Intent i, boolean dryRun) {")[1]
dry = st.index('if (dryRun) return "checked";')
first_save = min(i for i in (st.find("Settings.System.putInt("), st.find(".edit()"), st.find("p.set")) if i >= 0)
check("the dry run returns before the first thing is saved", dry < first_save, (dry, first_save))
check("set(c, p, i) is the same as before (no dry run)", "static String set(Context c, Prefs p, Intent i) { return set(c, p, i, false); }" in recv)
sys.exit(1 if failed else 0)
