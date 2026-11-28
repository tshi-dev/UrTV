#!/usr/bin/env python3
"""Review F94: after a wrong pairing code the TV says to type the new one, so the phone's next code must be accepted (the add-on is «failed», begin takes only «armed»)."""
import sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
s = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/PairBridge.java").read_text(encoding="utf-8")
sub = s[s.index("public static String submit("):]
a = sub.index('"failed".equals(PairRoute.state(c))')
ok = a >= 0 and "retry(c)" in sub[a:a + 120] and a < sub.index("return PairRoute.submit(c, typed)")
print(("PASS" if ok else "FAIL") + " pair retry: submit arms the add-on again when its state is failed, before the code is handed on")
sys.exit(0 if ok else 1)
