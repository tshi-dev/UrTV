#!/usr/bin/env python3
"""4.3.1 «الإذاعة»: why a station stopped, in one plain line (no internet / the station did not answer / format not supported / stopped after N tries),
and the one record kept for the report (station, category, HTTP code, SDK, decoder; no address, no token). RadioFault runs on a plain JVM; the wiring is
read from the source.
    python3 tests/radio_fault_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = H / "tilawah"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: Path(p).read_text(encoding="utf-8")
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        srcs = [ROOT / "tests/i18n_home_stub/S.java", H / "Tr.java", T / "RadioFault.java", ROOT / "tests/RadioFaultRun.java"]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(s) for s in srcs], capture_output=True, text=True)
        check("RadioFault compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.RadioFaultRun"], capture_output=True, text=True, timeout=60).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
else:
    check("java is installed", False, "javac/java missing")

ms, pb, ex, rc = read(T / "MusicService.java"), read(T / "Playback.java"), read(ROOT / "android/launcher/src/exo/java/sa/urtv/home/tilawah/ExoPlayback.java"), read(T / "MusicReceiver.java")
tr = read(ROOT / "i18n/en/home.tsv")
check("the station's fault is said with RadioFault (reason from the player's code, the HTTP status, the network and the tries), not the old one-size line",
      "RadioFault.reason(" in ms and "RadioFault.message(" in ms)
check("the network check is the TV's own (ConnectivityManager, a permission the app already holds)", "ConnectivityManager" in ms and "NET_CAPABILITY_VALIDATED" in ms)
check("ExoPlayback hands over the HTTP status and the decoder (public fields of Media3's exceptions), nothing else",
      "InvalidResponseCodeException" in ex and "httpCode = " in ex and "decoder = " in ex and "int httpCode" in pb and "String decoder" in pb)
check("the record reaches the status line (radio_fault=) which the report reads", "RadioFault.record(" in ms and "radio_fault=" in rc)
check("every new line has its English", all(s in tr for s in ("لا يوجد اتصال بالإنترنت، تحقق من الشبكة\t", "المحطة لا تردّ الآن، جرّب بعد قليل\t", "صيغة هذا البث غير مدعومة على هذا التلفزيون\t", "توقف البث بعد %s محاولات\t")))
share = read(ROOT / "optimize/share.py")
check("the report carries 'Radio, last error' when the TV answers, from the record only", "def radio_line(" in share and "radio_fault=" in share and "Radio, last error" in share)
import importlib.util
spec = importlib.util.spec_from_file_location("share_mod", ROOT / "optimize/share.py")
sm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sm)
line = sm.radio_line('data="state=1|radio_fault=station=radio-0;category=io;http=404;sdk=34;decoder=OMX.aac|buffering=0"')
check("radio_line reads the record and nothing around it", line == "Radio, last error: station=radio-0 category=io http=404 sdk=34 decoder=OMX.aac", line)
check("radio_line says nothing when no station failed", sm.radio_line('data="radio_fault=|buffering=0"') is None and sm.radio_line("") is None and sm.radio_line(None) is None)
sys.exit(1 if failed else 0)
