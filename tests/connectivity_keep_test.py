#!/usr/bin/env python3
"""4.4 beta 3 (a Xiaomi box: since the setup the Wi-Fi dropped and did not reconnect to saved networks): nothing in the connectivity path is ever
turned off, on any TV, from any list. core.txt (the never list, which disable.sh, community.sh and the TV's own setup all refuse) covers Wi-Fi,
connectivity, captive portal login, network recommendation and scoring, carrier config and the network stack; SetupLists.CORE says the same.
Run: python3 tests/connectivity_keep_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

core = [l.strip() for l in (ROOT / "optimize/lists/core.txt").read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
java = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupLists.java").read_text(encoding="utf-8")
jcore = [s.encode().decode("unicode_escape") for s in re.findall(r'"((?:[^"\\]|\\.)*)"', java.split("static final String[] CORE = {", 1)[1].split("};", 1)[0])]
check("SetupLists.CORE is core.txt, line for line (the TV's own setup refuses the same)", jcore == core, [x for x in core if x not in jcore] + [x for x in jcore if x not in core])
def kept(p): return any(re.fullmatch(r, p) for r in core)
PATH = ["com.android.wifi.resources", "com.google.android.wifi.resources", "com.android.networkstack", "com.google.android.networkstack",
        "com.android.networkstack.tethering", "com.android.tethering", "com.android.captiveportallogin", "com.google.android.captiveportallogin",
        "com.android.networkrecommendation", "com.sony.dtv.networkrecommendation", "com.google.android.apps.tv.connectivity",
        "com.android.connectivity.resources", "com.android.carrierconfig", "com.mediatek.network", "com.xiaomi.wifiservice", "com.mitv.wifi"]
missing = [p for p in PATH if not kept(p)]
check("the connectivity path is never turned off (Wi-Fi, network stack, tethering, captive portal, recommendation, carrier config, vendor)", not missing, missing)
check("... while the ordinary packages around it may still go", not any(kept(p) for p in ("com.miui.tv.analytics", "com.xm.webcontent", "com.google.android.feedback",
      "com.mitv.tvhome.michannel", "com.apps.networkspeed")), "")
lists = [f for f in (ROOT / "optimize/lists/community").glob("xiaomi-*.txt")] + [f for f in (ROOT / "optimize/lists/community").glob("redmi-*.txt")]
named = sorted({l.split()[0] for f in lists for l in f.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#") and kept(l.split()[0])})
print("NOTE Xiaomi community entries now refused by core.txt:", ", ".join(named) or "none")
check("tests/run.sh runs this test", "tests/connectivity_keep_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
