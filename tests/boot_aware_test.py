#!/usr/bin/env python3
"""R-500 (docs/research/LAUNCHERS-GITHUB.md idea 3): TCL Android 11 skips a key service that is not directBootAware at a locked boot, writes
accessibility_enabled=0 and never binds it after the unlock. The Extras service is directBootAware and reads none of its settings before the
unlock: keys and windows go on untouched until ACTION_USER_UNLOCKED, then the usual start runs. Run: python3 tests/boot_aware_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


man = (ROOT / "android/extras/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
svc = re.search(r'<service\s+android:name="sa\.urtv\.home\.ExtrasA11yService".*?</service>', man, re.S)
check("the Extras key service is directBootAware", svc is not None and 'android:directBootAware="true"' in svc.group(0), svc and svc.group(0))
src = (ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java").read_text(encoding="utf-8")
conn = re.search(r"protected void onServiceConnected\(\) \{(.*?)\n    \}", src, re.S)
body = conn.group(1) if conn else ""
check("a locked start reads no settings: the first line waits for the unlock", body.strip().startswith("if (!unlocked(this)) { startAfterUnlock(); return; }"), body[:200])
wait = src.split("private void startAfterUnlock() {")[1].split("\n    }\n")[0] if "private void startAfterUnlock() {" in src else ""
check("after ACTION_USER_UNLOCKED the usual start runs (once), and nothing is read meanwhile", "ACTION_USER_UNLOCKED" in wait and "onServiceConnected();" in wait
      and "if (unlock != null) return;" in wait and "getSharedPreferences" not in wait, wait)
key = re.search(r"protected boolean onKeyEvent\(KeyEvent e\) \{(.*?)\n    \}", src, re.S)
kb = key.group(1) if key else ""
check("before the start every key goes on untouched (before any settings read)", "if (running != this) return false;" in kb
      and kb.index("running != this") < kb.index("keyFilter(this)"), kb)
ev = re.search(r"public void onAccessibilityEvent\(AccessibilityEvent e\) \{(.*?)\n        String pkg", src, re.S)
check("before the start no window is judged", ev is not None and "if (running != this) return;" in ev.group(1))
check("the unlock receiver is let go on unbind", re.search(r"onUnbind[^}]*unlock != null", src, re.S) is not None)
print("ALL PASS" if not failed else f"{failed} FAILED")
raise SystemExit(1 if failed else 0)
