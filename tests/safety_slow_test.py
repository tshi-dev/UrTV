#!/usr/bin/env python3
"""Tester b654: «the safety switch put the stock home back (ADB was too slow)» stopped `install.sh launcher` again and again on a slow TV. The line now leaves a mark and the next try gives the switch 900 s (300 by default)."""
import sys
from pathlib import Path
s = (Path(__file__).resolve().parents[1] / "optimize/install.sh").read_text(encoding="utf-8")
failed = 0
def check(n, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + n); failed += not ok
check("the switch is armed for SAFETY_S seconds (300 by default, 900 after a «too slow» stop)",
      "SAFETY_S=300" in s and '[ -f "${URTV_RUN_DIR:-optimize/run}/safety-slow" ] && SAFETY_S=900' in s and "deadman.sh $STOCK $SAFETY_S $NONCE" in s and "deadman.sh $STOCK 300 $NONCE" not in s)
a = s.index("The safety switch put the stock home back (ADB was too slow)")
check("the «too slow» line leaves the mark the next try reads", ': > "${URTV_RUN_DIR:-optimize/run}/safety-slow"' in s[a - 200:a])
sys.exit(1 if failed else 0)
