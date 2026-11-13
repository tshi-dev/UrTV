#!/usr/bin/env python3
"""4.3 (a user report on 4.2: «إذاعة القرآن تشتغل كذا ثواني بعدين يكتب صيغة لا تدعم بعدين يكتب تعذّر التشغيل»): every live station plays through
ExoPlayer (HLS and Icecast with ICY), a station that played and stopped is joined again quietly, one that does not start is tried 3 times, then
its second source (stations.json «alt»), and only then a fault is said, «تعذّر الاتصال بالبث» unless it never played and its format is not one
the TV plays. RadioPolicyRun.java runs StreamUrl and RadioPolicy against a scripted local server (a 302 to an allowed cleartext host, «ICY 200
OK», a stream that drops after 5 s, a 500), then the wiring is read from the source.
    python3 tests/radio_policy_test.py"""
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p):
    return Path(p).read_text(encoding="utf-8")


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        srcs = [ROOT / "tests/i18n_home_stub/S.java", ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java", T / "StreamUrl.java", T / "PlayerFault.java",
                T / "RadioPolicy.java", ROOT / "tests/RadioPolicyRun.java"]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(s) for s in srcs], capture_output=True, text=True)
        check("StreamUrl and RadioPolicy compile on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.RadioPolicyRun"], capture_output=True, text=True, timeout=300).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every scripted-server case ran", "ALL PASS" in out, out[-400:])
else:
    print("SKIP the Java cases: no javac")

pb, exo, ms, cat = read(T / "Playback.java"), read(ROOT / "android/launcher/src/exo/java/sa/urtv/home/tilawah/ExoPlayback.java"), read(T / "MusicService.java"), read(T / "Catalog.java")
check("every live station through ExoPlayer (a surah and the adhan stay on MediaPlayer)", "if (radio) {" in pb and "Playback.create(this, url, source == Prefs.SOURCE_RADIO)" in ms)
check("ExoPlayer plays HLS and progressive streams (ICY read by ExoPlayer), an .m3u8 behind a query string too", "new DefaultMediaSourceFactory(c).setDataSourceFactory(http)" in exo
      and "MimeTypes.APPLICATION_M3U8" in exo and "HlsMediaSource" not in exo)
check("the end of a live stream is a failure to join again, not an end", "state == Player.STATE_ENDED" in exo and "onError.onError(ExoPlayback.this, MediaPlayer.MEDIA_ERROR_UNKNOWN, PlayerFault.IO)" in exo
      and "else if (source == Prefs.SOURCE_RADIO) onPlaybackError(MediaPlayer.MEDIA_ERROR_UNKNOWN, PlayerFault.IO);" in ms)
check("ExoPlayer's errors reach the service as MediaPlayer's numbers", "RadioPolicy.extraForExo(e.errorCode)" in exo)
check("https stays https in the player (a cleartext step only through StreamUrl, for radiojar.com)", ".setAllowCrossProtocolRedirects(false)" in exo)
err = ms[ms.index("private void onPlaybackError"):ms.index("private void playSurahAgain")]
check("a station's failure goes to RadioPolicy first: joined again, retried, its second source, then said", err.index("RadioPolicy.after(radioState") < err.index("source == Prefs.SOURCE_ADHAN && retries >= 2")
      and "RadioPolicy.message(radioState, what, extra)" in err and "RadioFault.message(RadioFault.reason(" in err and "radioAddress = radioAlt;" in err and "RadioPolicy.started(radioState)" in ms)
check("no station says «صيغة البث غير مدعومة» but through RadioPolicy (never for a station that played)", "PlayerFault.message(true" not in ms and "PlayerFault.message(source == Prefs.SOURCE_RADIO" not in ms
      and 'S.t("تعذّر التشغيل")' not in ms)
check("a new pick starts with nothing held against it", "resetRadioState();" in ms.split("private void playRadio(int index)")[1].split("startRadio(radioAddress, 0);")[0])
st = json.loads(read(ROOT / "android/launcher/src/main/assets/quran/stations.json"))["stations"]
check("the Saudi Quran Radio has its second source (radiojar, https; its cleartext step is StreamUrl's, for radiojar.com only)", st[0]["alt"] == "https://stream.radiojar.com/0tpy1h0kxtzuv"
      and "kwikmotion" in st[0]["url"] and all("alt" not in s or s["alt"].startswith("https://") for s in st))
check("a station's «alt» is read only as https", 'if (alt.startsWith("https://")) st.alt = alt;' in cat)
check("StreamUrl takes a Shoutcast «ICY 200 OK» as a stream answering", "if (status == -1) return now;" in read(T / "StreamUrl.java"))
sys.exit(1 if failed else 0)
