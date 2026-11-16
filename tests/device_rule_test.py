#!/usr/bin/env python3
"""4.3.3 (5) per-model defaults (urtv-data devices/devices.json, DeviceRule, DeviceDefaults): an entry matches by brand, model, device (any
case) and SDK range, never every TV; the first entry to set a key wins; applied at the home's start only where nothing is set, never throwing.
Runs DeviceRule on a plain JVM."""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


dd = (HOME / "DeviceDefaults.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
cc = (HOME / "ControlCenter.java").read_text(encoding="utf-8")
check("applied once at the home's start, before the screen is built", "DeviceDefaults.apply(getApplicationContext());" in main
      and main.index("DeviceDefaults.apply(") < main.index("setContentView(buildUi());"))
check("only where nothing is set: the sound row's mark is written only when absent, in ControlCenter's own file, and ControlCenter reads it",
      'if (!sp.contains("sound_bad")) sp.edit().putBoolean("sound_bad", true).apply();' in dd and 'getSharedPreferences("control", Context.MODE_PRIVATE)' in dd
      and 'act.getSharedPreferences("control", Context.MODE_PRIVATE)' in cc and 'prefs().getBoolean("sound_bad", false)' in cc)
check("a failure never stops the home; the kept copy only, the network in the background once a day",
      "catch (RuntimeException ignored)" in dd and "ContentFile.refresh(c, DeviceRule.PATH, DeviceRule.NAME, 24 * 3600_000L)" in dd
      and "ContentFile.read(c, DeviceRule.NAME)" in dd)
check("the launcher's MiniJson and ContentFile are the screensaver's (the package aside)",
      all((HOME / n).read_text(encoding="utf-8").split("\nfinal class ", 1)[1].replace("final Context app = c.getApplicationContext();\n        ", "")
          == (ROOT / "android/app/src/main/java/sa/urtv/screensaver" / n).read_text(encoding="utf-8").split("\nfinal class ", 1)[1]
          .replace("final Context app = c.getApplicationContext();\n        ", "") for n in ("MiniJson.java", "ContentFile.java")))

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "DeviceRule.java"), str(HOME / "MiniJson.java"), str(ROOT / "tests/DeviceRuleRun.java")],
                           capture_output=True, text=True)
        check("DeviceRule compiles on a plain JVM", r.returncode == 0, r.stderr[-800:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.DeviceRuleRun"], capture_output=True, text=True).stdout
            got = dict(l.split(" ", 1) for l in out.splitlines() if " " in l)
            check("the G07: its entry first, so its value wins over the brand's; the brand's other key added",
                  got.get("G07") == "{sound_opens_general_settings=true, other=1.0}", out)
            check("brand and device in any case, spaces trimmed", got.get("G07LOWER") == got.get("G07"), out)
            check("outside its SDK range: only the brand's entry", got.get("G07SDK") == "{sound_opens_general_settings=false, other=1.0}", out)
            check("another TCL: the brand's entry", got.get("OTHERTCL") == "{sound_opens_general_settings=false, other=1.0}", out)
            check("an entry that names no brand, model or device (every TV) or an empty one: never applied",
                  got.get("SONY") == "{}" and "everyone" not in out and "blank" not in out, out)
            check("no Build values or no file: nothing", got.get("NULLS") == "{}" and got.get("NODOC") == "{}", out)

print(f"\n{len(fails)} failed" if fails else "\nall passed")
sys.exit(1 if fails else 0)
