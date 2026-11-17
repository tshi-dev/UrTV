#!/usr/bin/env python3
"""4.4 radio reliability, part 1: a bigger buffer for live audio and a pre-buffer before it plays (ExoPlayback 30-120 s, starts at 5 s, 10 s after a
rebuffer); the moment the TV's network comes back a station that went quiet joins again (and a surah that failed goes on from its place), once per
flap (NetReconnect, run for real); a quiet stall restart and a reconnect say «يعيد الاتصال…» instead of silence; the Wi-Fi stays awake while it joins
again; music.sh status says how many reconnects.   python3 tests/net_reconnect_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
svc, rule = read(T / "MusicService.java"), read(T / "NetReconnect.java")
exo = read(ROOT / "android/launcher/src/exo/java/sa/urtv/home/tilawah/ExoPlayback.java")
check("NetReconnect is plain Java", "import android" not in rule)
check("the bigger buffer and the pre-buffer", "MIN_BUFFER_MS = 30_000, MAX_BUFFER_MS = 120_000, START_MS = 5_000, RESTART_MS = 10_000" in exo
      and "setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, START_MS, RESTART_MS)" in exo)
check("the service watches the default network from onCreate and lets go in onDestroy", "watchNetwork();" in svc and "registerDefaultNetworkCallback(netBack)" in svc
      and "unregisterNetworkCallback(netBack)" in svc)
check("only a network that came back and works (VALIDATED, after a loss) wakes it, on the service's thread",
      "if (!lost || !c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return;" in svc and "handler.post(MusicService.this::onNetworkBack);" in svc)
check("a station joins again from its own address, a surah from its place", "startRadio(radioAddress, 0);" in svc[svc.index("private void onNetworkBack()"):]
      and "playSurah(reciter, surah, Math.max(position, pendingSeek));" in svc)
check("«يعيد الاتصال…» while it joins again (a quiet stall restart, the network back), never silence",
      'RECONNECTING = sa.urtv.home.S.n("يعيد الاتصال…")' in svc and ": reconnecting ? RECONNECTING : null;" in svc
      and svc.count("reconnecting = true;") == 2)
check("the Wi-Fi lock is taken while it joins again too", "acquireWifi();   // 4.4" in svc)
check("music.sh status: reconnects=", "|reconnects=" in read(T / "MusicReceiver.java"))
if shutil.which("javac"):
    run = """package sa.urtv.home.tilawah;
public class NetRun { static void out(String n, boolean ok) { System.out.println(n + "\\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  long M = Long.MAX_VALUE;
  out("a station in trouble joins again", NetReconnect.onNetworkBack(Prefs.SOURCE_RADIO, true, false, true, M) == NetReconnect.Act.RADIO);
  out("a station that went silent without an error joins again too", NetReconnect.onNetworkBack(Prefs.SOURCE_RADIO, true, false, false, M) == NetReconnect.Act.RADIO);
  out("a station playing fine is left alone", NetReconnect.onNetworkBack(Prefs.SOURCE_RADIO, true, true, false, M) == NetReconnect.Act.NOTHING);
  out("a surah that failed goes on from its place", NetReconnect.onNetworkBack(Prefs.SOURCE_SURAH, true, false, true, M) == NetReconnect.Act.SURAH);
  out("a paused surah is not started", NetReconnect.onNetworkBack(Prefs.SOURCE_SURAH, true, false, false, M) == NetReconnect.Act.NOTHING);
  out("paused or stopped by the person: nothing", NetReconnect.onNetworkBack(Prefs.SOURCE_RADIO, false, false, true, M) == NetReconnect.Act.NOTHING);
  out("the adhan or nothing chosen: nothing", NetReconnect.onNetworkBack(Prefs.SOURCE_ADHAN, true, false, true, M) == NetReconnect.Act.NOTHING
      && NetReconnect.onNetworkBack(Prefs.SOURCE_NONE, true, false, true, M) == NetReconnect.Act.NOTHING);
  out("one reconnect per flap (5 s)", NetReconnect.onNetworkBack(Prefs.SOURCE_RADIO, true, false, true, 4_999) == NetReconnect.Act.NOTHING
      && NetReconnect.onNetworkBack(Prefs.SOURCE_RADIO, true, false, true, 5_000) == NetReconnect.Act.RADIO);
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "NetRun.java").write_text(run)
        Path(tmp, "Prefs.java").write_text("package sa.urtv.home.tilawah;\nfinal class Prefs { static final int SOURCE_NONE = 0, SOURCE_SURAH = 1, SOURCE_RADIO = 2, SOURCE_ADHAN = 3; }\n")
        check("the stub's numbers are Prefs' own", "static final int SOURCE_NONE = 0, SOURCE_SURAH = 1, SOURCE_RADIO = 2, SOURCE_ADHAN = 3;" in read(T / "Prefs.java"))
        r = subprocess.run(["javac", "-d", tmp, str(T / "NetReconnect.java"), str(Path(tmp, "Prefs.java")), str(Path(tmp, "NetRun.java"))], capture_output=True, text=True)
        check("NetReconnect compiles on its own", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.NetRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 8, len(out.splitlines()))
sys.exit(1 if failed else 0)
