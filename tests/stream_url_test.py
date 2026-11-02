#!/usr/bin/env python3
"""The live stations' addresses (tester step 19, the owner's TCL): radiojar's https -> http redirect followed by us and the final address handed to
the player; backup.qurango.net's random 500 tried again with a growing wait, «تعذّر» only after the last try. StreamUrl.java against a local server
(tests/StreamUrlRun.java), then the wiring in MusicService and network_security_config.xml.

    python3 tests/stream_url_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "StreamUrl.java"), str(ROOT / "tests/StreamUrlRun.java")], capture_output=True, text=True)
    check("it compiles", r.returncode == 0, r.stderr)
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.StreamUrlRun"], capture_output=True, text=True, timeout=120).stdout
        for line in out.splitlines():
            if line.startswith(("PASS ", "FAIL ")):
                print(line)
                failed += line.startswith("FAIL ")
        check("every StreamUrl case ran", "ALL PASS" in out, out[-300:])
else:
    print("SKIP the Java cases: no javac")

svc = (T / "MusicService.java").read_text(encoding="utf-8")
play = svc[svc.index("private void playRadio(int index)"):svc.index("// ---- the adhan")]
check("a station is resolved first (StreamUrl.resolve on a thread), then the player gets the final address", "startRadio(radioAddress, 0);" in play
      and "StreamUrl.resolve(url, 10_000)" in play and "if (go != null) { startUrl(go); return; }" in play and "new Thread(" in play)
check("a 5xx is tried again after StreamUrl.backoffMs, and «تعذّر» only after the last try", "StreamUrl.again(tries + 1)" in play and "StreamUrl.backoffMs(tries + 1)" in play
      and play.index("StreamUrl.again(tries + 1)") < play.index("PlayerFault.message(true, 0, PlayerFault.IO)"))
check("a stop or another pick meanwhile wins (the generation and the address are checked)", "if (!wish.current(gen) || source != Prefs.SOURCE_RADIO || !url.equals(currentUrl)) return;" in play)
err = svc[svc.index("private void onPlaybackError"):svc.index("private void playSurahAgain")]
check("a player error on a station resolves the address again, with the growing wait", "if (radio) startRadio(station, 0); else startUrl(failed);" in err and "radio ? StreamUrl.backoffMs(retries) : 3000" in err)
nsc = (ROOT / "android/launcher/src/main/res/xml/network_security_config.xml").read_text(encoding="utf-8")
check("cleartext only for radiojar.com (and its subdomains), everything else https", '<base-config cleartextTrafficPermitted="false" />' in nsc
      and re.findall(r"<domain[^>]*>([^<]+)</domain>", nsc) == ["radiojar.com"] and 'includeSubdomains="true"' in nsc)
sys.exit(1 if failed else 0)
