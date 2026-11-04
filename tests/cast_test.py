#!/usr/bin/env python3
"""«البث من الجوال» on a Chromecast with Google TV (optimize/cast.sh, optimize/dock.sh cast): the cast receiver needs the setup wizard on (its
device credentials are in the wizard), so `on` only turns the wizard ON and restarts the receiver, then runs the usual home switch; `off` records
the choice and runs the home switch, which alone may turn the wizard off (with its Wi-Fi guard). Not a Chromecast: nothing changes. A fake TV.

    python3 tests/cast_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


FAKE_TV = r'''#!/bin/sh
# fake python: optimize/tv.py <command> | --batch
log() { echo "$1" >> "$T/log"; }
shift; shift   # -I optimize/tv.py
if [ "$1" = --batch ]; then
  in=$(cat)
  case "$in" in *"pm enable com.google.android.tungsten.setupwraith"*) log wizard-on; rm -f "$T/wiz_off" ;; esac
  case "$in" in *"am force-stop com.google.android.apps.mediashell"*) log receiver-restart ;; esac
  case "$in" in *"disable-user"*) log DISABLED-SOMETHING ;; esac
  [ "$DROP" = yes ] && exit 1
  exit 0
fi
case "$1" in
  "dumpsys package com.google.android.tungsten.setupwraith"*) [ "$CAST" = yes ] && echo "  x/com.google.android.libraries.tv.setupwraith.libs.castinfo.CastInfoProvider" ;;
  "pm list packages -d; echo END_OF_LIST") [ -f "$T/wiz_off" ] && echo "package:com.google.android.tungsten.setupwraith"; echo END_OF_LIST ;;
  *resolve-activity*) echo "sa.urtv.home/.MainActivity" ;;
  *disable-user*) log DISABLED-SOMETHING ;;
esac
exit 0
'''


def run(mode, cast="yes", wiz="off", drop="", stuck=""):
    t = Path(tempfile.mkdtemp())
    tree = t / "tree"
    (tree / "optimize").mkdir(parents=True)
    shutil.copy(ROOT / "optimize" / "cast.sh", tree / "optimize")
    shutil.copy(ROOT / "optimize" / "dock.sh", tree / "optimize")
    (tree / "optimize" / "install.sh").write_text('#!/bin/sh\necho "install $*" >> "$T/log"\necho "choice $(cat optimize/run/cast.txt 2>/dev/null)" >> "$T/log"\n')
    (tree / "optimize" / "install.sh").chmod(0o755)
    py = t / "py"
    py.write_text(FAKE_TV if not stuck else FAKE_TV.replace('log wizard-on; rm -f "$T/wiz_off"', "log wizard-on"))
    py.chmod(0o755)
    if wiz == "off":
        (t / "wiz_off").write_text("")
    env = dict(os.environ, PY=str(py), T=str(t), CAST=cast, DROP=drop)
    r = subprocess.run(["bash", str(tree / "optimize" / "dock.sh"), "cast", mode], env=env, capture_output=True, text=True, cwd=tree)
    log = (t / "log").read_text().split("\n") if (t / "log").exists() else []
    return r.returncode, r.stdout + r.stderr, log


c, o, l = run("on")
check("on: the wizard ON, the cast receiver restarted, then the usual home switch, with the choice recorded as on; nothing disabled",
      c == 0 and "wizard-on" in l and "receiver-restart" in l and "install launcher" in l and "choice on" in l and "DISABLED-SOMETHING" not in l
      and l.index("wizard-on") < l.index("install launcher") and "untrusted device" in o, (c, o, l))
c, o, l = run("off", wiz="on")
check("off: the choice recorded as off before the home switch (which alone may turn the wizard off); cast.sh itself turns nothing off",
      c == 0 and "choice off" in l and "install launcher" in l and "wizard-on" not in l and "DISABLED-SOMETHING" not in l and "تلفزيونك" in o, (c, o, l))
c, o, l = run("status", wiz="on")
check("status: read only (no install, nothing changed): the wizard, Home and the choice", c == 0 and "Wizard: on" in o and "Choice: on" in o and not [x for x in l if x], (o, l))
c, o, l = run("on", cast="")
check("not a Chromecast: it says so and changes nothing (exit 1 for on/off, 0 for status)", c == 1 and "not a Chromecast" in o and not [x for x in l if x], (c, o, l))
c, o, l = run("status", cast="")
check("not a Chromecast, status: exit 0", c == 0 and "not a Chromecast" in o, (c, o))
c, o, l = run("on", drop="yes")
check("on, the TV does not answer: a !! line and no home switch", c == 1 and o.count("!!") == 1 and "install launcher" not in l, (c, o, l))
c, o, l = run("on", stuck="yes")
check("on, the wizard did not come back on: a !! line and no home switch (never a switch that would turn it off again)", c == 1 and "!!" in o and "install launcher" not in l, (c, o, l))
c, o, l = run("maybe")
check("anything else: usage, nothing changed", c == 1 and "usage" in o and not [x for x in l if x], (c, o))
src = (ROOT / "optimize" / "cast.sh").read_text()
check("cast.sh never turns the wizard off itself, and tells a Chromecast by the provider, not by the model name",
      "disable-user" not in src and "castinfo.CastInfoProvider" in src and "sabrina" not in src and "ro.product" not in src)
ins = (ROOT / "optimize" / "install.sh").read_text()
check("install.sh: the cast check is the provider, a choice of «off» skips it, and wraith_off leaves the wizard on while casting is kept",
      'dumpsys package $WRAITH 2> /dev/null | grep -m 1 castinfo.CastInfoProvider' in ins and '!= off ] && wraith_casts' in ins
      and 'if [ "$CAST_KEPT" = yes ]; then echo "The setup wizard holds this TV' in ins)
eng = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupEngine.java").read_text()
check("the in-app setup keeps the wizard on a Chromecast too (the same provider, the same cast.txt choice), and asks nothing about the Wi-Fi there",
      "boolean castKeepsWizard()" in eng and "if (castKeepsWizard()) {" in eng and '"cast.txt"' in eng and eng.count("castKeepsWizard()") >= 4)
docs = (ROOT / "docs/SETTINGS.md").read_text() + (ROOT / "docs/SETUP-PROMPT.md").read_text()
check("documented: the SETTINGS row and the «When something goes wrong» row", "`optimize/dock.sh cast on|off|status`" in docs and "جهاز غير موثوق فيه" in docs)
sys.exit(1 if failed else 0)
