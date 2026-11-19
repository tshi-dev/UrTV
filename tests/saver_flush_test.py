#!/usr/bin/env python3
"""R-305: «saved» from saver.sh (and the phone/home path) is answered only after the settings file is written to the TV's storage. apply() queues the
write and cannot say it failed, and `saver.sh show` reads the memory, so a write that never reached the disk looked saved (and verified) until a power
cycle brought the older values back (tester, 19 Nov: panel, home panel, preset, apps style).   python3 tests/saver_flush_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


recv, prefs, prov = ((APP / f).read_text(encoding="utf-8") for f in ("SaverReceiver.java", "Prefs.java", "ConfigProvider.java"))
check("Prefs.flush() is an empty commit(): it writes what apply() queued, now, and says false when the file could not be written",
      re.search(r"boolean flush\(\) \{ return sp\.edit\(\)\.commit\(\); \}", prefs) is not None)
body = recv[recv.index("static String set(Context c, Prefs p, Intent i)"):] if "static String set(Context c, Prefs p, Intent i)" in recv else ""
tail = body[body.index("List<String> events") if "List<String> events" in body else 0:]
saved = tail.find('return "saved\\n" + describe(c, p);')
flush = tail.find("if (!p.flush())")
check("SET writes to the disk before it answers «saved» (the flush sits between the last setter and the answer)", 0 <= flush < saved, (flush, saved))
check("a write that fails is an error line, never «saved»", 'if (!p.flush()) return "error: the settings could not be written' in recv)
check("the flush comes before the home is told (the home must not read a value the disk does not have)", flush < tail.find("p.lookChanged();"), flush)
check("the phone/home path (phone_set) is that same code, so it is flushed too", "SaverReceiver.set(c, p, i)" in prov and 'out.putBoolean("ok", r.startsWith("saved"))' in prov)
check("no code path writes the screensaver's settings back from a kept copy at boot: BootReceiver only re-arms the job",
      "UpdateJob.schedule(c);" in (APP / "BootReceiver.java").read_text(encoding="utf-8") and "Prefs" not in (APP / "BootReceiver.java").read_text(encoding="utf-8"))
for m in ("android/app/src/main/AndroidManifest.xml", "android/launcher/src/main/AndroidManifest.xml"):
    check("no Android backup restores an older copy (" + m.split("/")[1] + ")", 'android:allowBackup="false"' in (ROOT / m).read_text(encoding="utf-8"))
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
