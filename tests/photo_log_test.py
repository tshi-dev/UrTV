#!/usr/bin/env python3
"""4.4 beta 4: photos stayed black on some Google TVs (after the Object.fromEntries polyfill, R-363), and nothing said why: every photo load that
failed was silent, and the rotation's own error was swallowed. Now the page tells the app (UrtvNative.photoError, one UrTVSaver line: what failed
and the file's name only, at most 20 in 10 minutes) and, 8 s after the start, shows a bundled photo that loads when there is none or the one there
does not load (photoGuard). The wiring here; the page's behaviour in a browser in tests/photo_log.test.mjs.
Run: python3 tests/photo_log_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

br = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java").read_text(encoding="utf-8")
pe = br[br.index("public void photoError("):]
pe = pe[:pe.index("\n    }\n")]
check("the bridge has photoError for the page", "@JavascriptInterface\n    public void photoError(String what, String name)" in br)
check("photoError: at most 20 lines in 10 minutes", "now - photoLoggedAt > 600_000L" in pe and "if (++photoLogged > 20) return;" in pe)
check("photoError: only letters for what, a plain file name (no slash, no address), both cut short", 'replaceAll("[^a-z]", "")' in pe and 'replaceAll("[^A-Za-z0-9._-]", "")' in pe and "substring(0, 60)" in pe)
s = (ROOT / "src/template.html").read_text(encoding="utf-8")
check("the page names the file only (the last part of the address)", "split(/[?#]/)[0].split('/').pop().slice(0, 60)" in s)
check("each failing load says so: the owner's photos, the hosted one, the rotation's own error", "photoLog('own', order[i].url)" in s and "photoLog('hosted', url)" in s
      and "} catch(e) { photoLog('error', (e && e.name) || ''); }" in s)
check("the guard runs once at the start, after the first photo is asked for", "  applyBackground(true);\n  photoGuard();" in s and "const PHOTO_GUARD_MS = 8000;" in s)
check("the guard is not in the launcher's card (#widget=1)", "if(CONFIG.widget) return;   // the launcher's card" in s)
for page in ("dist/tv.html", "dist/tv-en.html"):
    d = (ROOT / page).read_text(encoding="utf-8")
    check(f"{page} is built with it", "photoGuard()" in d and "UrtvNative.photoError" in d)
check("tests/run.sh runs the browser half", "node tests/photo_log.test.mjs" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
