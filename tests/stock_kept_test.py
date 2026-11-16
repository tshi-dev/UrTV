"""4.3.3: the computer's home switch keeps the stock home, the setup wizard and its helpers ON, like the app's (SetupEngine.KEEP_STOCK_ON).

    python3 tests/stock_kept_test.py

The runs on a stub TV are in tests/install_retry_test.py («keep: …»); here: the default, the app's read-only provider the script asks about
«تخلّص من واجهة المصنع», and the words the docs give for the new line.
"""
import re, sys
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
bad = 0
def check(name, ok):
    global bad
    print(("PASS " if ok else "FAIL ") + name); bad += not ok

ins = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
check("install.sh: the switch keeps the stock home on by default (URTV_KEEP_STOCK unset = 1)", "KEEP_STOCK=${URTV_KEEP_STOCK:-1}" in ins)
check("install.sh: the Wi-Fi question and the cast question are only for the old switch", '[ "$KEEP_STOCK" = 1 ]; then rm -f "$ASKF"' in ins
      and '[ "$KEEP_STOCK" != 1 ] && [ "$(tr -d' in ins)
keep_branch = ins[ins.index('elif [ "$KEEP_STOCK" = 1 ]; then\n        # ours holds Home'):]
keep_branch = keep_branch[:keep_branch.index("      else\n        jstep switch")]
check("install.sh: the kept switch's batch has no pm disable-user", "disable-user" not in keep_branch and "add-role-holder" in keep_branch)
uri = re.search(r"content://([a-z.]+)/factory", ins)
check("install.sh asks the app whether «تخلّص من واجهة المصنع» is on", uri is not None)

A = "{http://schemas.android.com/apk/res/android}"
man = ET.parse(ROOT / "android/launcher/src/main/AndroidManifest.xml").getroot()
prov = next((p for p in man.iter("provider") if p.get(A + "name") == ".setup.SetupStateProvider"), None)
check("the home's SetupStateProvider: the authority install.sh asks, read by the shell only (DUMP)", prov is not None and uri is not None
      and prov.get(A + "authorities") == uri.group(1) and prov.get(A + "permission") == "android.permission.DUMP")
src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupStateProvider.java").read_text(encoding="utf-8")
check("SetupStateProvider: one column «off» from SetupHook.factoryOff, nothing written", '"off"' in src and "SetupHook.factoryOff" in src
      and re.search(r"insert\([^)]*\) \{ return null; \}", src) and "store.put" not in src)
check("install.sh's off=1 / off=0 words match the provider's column", "off=1" in ins and "off=0" in ins)

doc = (ROOT / "docs/SETUP-PROMPT.md").read_text(encoding="utf-8")
check("SETUP-PROMPT: the new line is in «When something goes wrong»", "`Turned back on what an older home switch turned off (…)`" in doc
      and "Turned back on what an older home switch turned off (" in ins)
sys.exit(1 if bad else 0)
