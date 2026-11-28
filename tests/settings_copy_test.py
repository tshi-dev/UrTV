#!/usr/bin/env python3
"""R-453 (a report: «a reinstall of the screensaver reset its preferences»). The home keeps a copy of the screensaver's settings in its own files and
a fresh install of the screensaver takes it back, once, on a thread. Both sides check the other is ours (a signature permission for the call, and the
provider's owner signed with our key); the copy never holds the update channel or the memory debug switch; an empty or broken answer never replaces
the kept copy; the home is not handed a fresh install's defaults before the restore ran. The encoding runs on a plain JVM (tests/SettingsCopyRun.java).

    python3 tests/settings_copy_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def read(p): return p.read_text(encoding="utf-8") if p.exists() else ""

cp, sc, prefs = read(APP / "ConfigProvider.java"), read(APP / "SettingsCopy.java"), read(APP / "Prefs.java")
home, prov, app = read(HOME / "SaverSettingsCopy.java"), read(HOME / "tilawah/NowPlayingProvider.java"), read(HOME / "HomeApp.java")
main = read(HOME / "MainActivity.java")
check("the screensaver answers «settings_copy» only to a caller holding READ_CONFIG (signature: the home)",
      '"settings_copy".equals(method)' in cp and 'checkCallingPermission("sa.urtv.screensaver.READ_CONFIG")' in cp.split('"settings_copy".equals(method)')[1][:400]
      and 'throw new SecurityException("the home only")' in cp)
check("the home answers «saver_settings_copy» only to a caller holding READ_NOW_PLAYING (signature: the screensaver)",
      '"saver_settings_copy".equals(method)' in prov and 'checkCallingPermission("sa.urtv.home.READ_NOW_PLAYING")' in prov.split('"saver_settings_copy".equals(method)')[1][:300])
check("each side asks only a provider owned by the other app and signed with our key",
      "pm.checkSignatures(c.getPackageName(), HOME_PKG) == PackageManager.SIGNATURE_MATCH" in sc and "HOME_PKG.equals(pi.packageName)" in sc
      and "pm.checkSignatures(c.getPackageName(), SAVER_PKG) == PackageManager.SIGNATURE_MATCH" in home and "SAVER_PKG.equals(pi.packageName)" in home)
check("the restore and the keeping run on threads of their own (never the UI thread)",
      'new Thread(() -> {' in sc and '"settings-restore"' in sc and 'new Thread(() -> {' in home and '"saver-settings-copy"' in home
      and sc.index("new Thread(") < sc.index('getContentResolver().call(') and home.index("new Thread(") < home.index('getContentResolver().call('))
check("a fresh install asks once (Prefs: nothing saved yet, or a restore still pending), and takes the copy whole or not at all",
      "if (sp.getAll().size() <= 1 || SettingsCopy.pending(c)) SettingsCopy.restoreSoon(c, sp.getAll().keySet());" in prefs
      and "SettingsCopyRule.shouldRestore(ownKeys, seen.getBoolean(TRIED, false))" in sc and "if (vals == null) return;" in sc and "if (!e.commit()) return;" in sc)
check("the export says nothing while a fresh install has not asked yet (the home never keeps its defaults over the person's settings)",
      "if (busy || c.getSharedPreferences(\"hub_seen\", Context.MODE_PRIVATE).getBoolean(PENDING, false)) return null;" in sc)
check("the home keeps the copy atomically (a temporary file, synced, renamed) and only a real one that differs",
      "if (!worthKeeping(in, read(f))) return;" in home and "o.getFD().sync();" in home and "tmp.renameTo(f)" in home)
check("the home refreshes it at its start and when it comes in front, at most every 30 minutes",
      "SaverSettingsCopy.refreshSoon(this);" in app and "SaverSettingsCopy.refreshSoon(this);" in main.split("protected void onStart() {")[1][:300]
      and "EVERY_MS = 30 * 60_000L" in home)
check("restored settings reach a dream or a page already up (Prefs.CHANGED)", "new android.content.Intent(Prefs.CHANGED).setPackage(app.getPackageName())" in sc)

if not shutil.which("javac"):
    print("SKIP the JVM part: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "SettingsCopyRule.java"), str(ROOT / "tests/SettingsCopyRun.java")], capture_output=True, text=True)
    check("SettingsCopyRule compiles on a plain JVM", r.returncode == 0, r.stderr)
    if r.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.SettingsCopyRun"], capture_output=True, text=True)
        print(r.stdout, end="")
        failed += r.returncode != 0 or r.stdout.count("PASS ") != 9
sys.exit(1 if failed else 0)
