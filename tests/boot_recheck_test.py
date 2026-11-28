#!/usr/bin/env python3
"""R-502 (docs/research/LAUNCHERS-GITHUB.md ideas 5, 6): some TVs take HOME back or drop the key service about 20 s into the boot, after our boot
checks ran. Two late looks (25 s, 60 s after BOOT_COMPLETED) read HOME again (roleAtBoot: given back once, never two at once) and heal the key service.
Settings only: nothing is brought to the front, so an HDMI input the TV booted into or an app the person opened stays. Run: python3 tests/boot_recheck_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


f = HOME / "BootRecheck.java"
src = f.read_text(encoding="utf-8") if f.exists() else ""
check("BootRecheck exists", bool(src))
check("two late looks, 25 s and 60 s after the boot, by alarm to our own A11yHeal", "AT_MS = {25_000, 60_000}" in src
      and "setClass(c, A11yHeal.class)" in src and "setAndAllowWhileIdle" in src)
look = re.search(r"static void look\(.*?\n    \}", src, re.S)
lb = look.group(0) if look else ""
check("a look reads HOME again, heals the key service and re-judges the add-on's catch, off the main thread",
      "SetupHook.roleAtBoot(app)" in lb and 'A11yHeal.check(app, "boot")' in lb and 'A11yHeal.rearm(app, "boot-late")' in lb and "new Thread(" in lb
      and "r.goAsync()" in lb and "pending.finish()" in lb)
check("nothing is brought to the front (no activity, no Home press, no BootHome): HDMI and the person's app stay",
      not re.search(r"startActivity|GO_HOME|BootHome|KEYCODE_HOME|performGlobalAction", src))
heal = (HOME / "A11yHeal.java").read_text(encoding="utf-8")
check("A11yHeal arms the looks at a boot only, and answers them", 'if ("boot".equals(why)) BootRecheck.arm(app);' in heal
      and "BootRecheck.ACTION.equals(intent.getAction())) { BootRecheck.look(app, this); return; }" in heal)
hook = (HOME / "setup/SetupHook.java").read_text(encoding="utf-8")
rb = hook[hook.index("public static void roleAtBoot"):hook.index("public static void roleNoticeTold")]
check("the role is never given back twice at once (the boot's try may still run at a late look)",
      "if (roleGiving) return;" in rb and "roleGiving = true;" in rb and "roleGiving = false;" in rb
      and rb.index("if (roleGiving) return;") < rb.index("new Thread("))
print("ALL PASS" if not failed else f"{failed} FAILED")
raise SystemExit(1 if failed else 0)
