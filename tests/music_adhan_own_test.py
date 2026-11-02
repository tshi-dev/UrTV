#!/usr/bin/env python3
"""optimize/music.sh adhan add / voice own / own-clear: what the computer prepares, what it uploads and sends to the TV, and how it
refuses bad arguments (no TV needed).

    python3 tests/music_adhan_own_test.py

PY is a stand-in for the venv's Python that writes every command to a file and answers like the TV does; FFMPEG and FFPROBE are
stand-ins too (with the real ffmpeg, if there is one, the last cases convert generated sound for real). Generated sound only.
"""
import os, shutil, subprocess, sys, tempfile

here = os.path.dirname(os.path.abspath(__file__))
script = os.path.join(here, "..", "optimize", "music.sh")
tmp = tempfile.mkdtemp()
sent_log = os.path.join(tmp, "sent.txt")
ff_log = os.path.join(tmp, "ffmpeg.txt")
kept = os.path.join(tmp, "pushed")
os.makedirs(kept)

fake_py = os.path.join(tmp, "fakepy")
with open(fake_py, "w") as f:
    f.write(r'''#!/usr/bin/env bash
# $1 = -I, $2 = the script, $3 = the command (or --push-b64)
if [ "$3" = --push-b64 ]; then
  echo "PUSH $5" >> "$SENT"; cp "$4" "$KEPT/$(ls "$KEPT" | wc -l).m4a"; echo "pushed $4 -> $5 (1 bytes, md5 ok)"; exit 0
fi
echo "$3" >> "$SENT"
case "$3" in
  *GET_PLAYER*) printf 'Broadcast completed: result=0, data="playing=0|source=none|adhan=on|voice=%s|fajr_voice=10|adhan_short=off|own_bytes=%s|adhan_next=0|adhan_last=0"\n' "${FAKE_VOICE:-3}" "${FAKE_OWN:-0}" ;;
  *ADHAN_OWN*"op add"*)
    if [ -n "${FAKE_ADD_FAIL:-}" ] && { [ "$FAKE_ADD_FAIL" = all ] || [[ "$3" != *"--es path"* ]]; }; then
      echo 'Broadcast completed: result=0, data="error: not an m4a (AAC) or Ogg (Opus) recording"'
    else
      printf 'Broadcast completed: result=0, data="saved\nown: 40 KB (in use)"\n'
    fi ;;
  *ADHAN_OWN*"op clear"*) printf 'Broadcast completed: result=0, data="saved\nown: none"\n' ;;
  *sa.urtv.home.FAJR_VOICE*"--ei voice 0"*)
    if [ -n "${FAKE_NO_OWN:-}" ]; then echo 'Broadcast completed: result=0, data="error: no recording of your own yet"'
    else printf 'Broadcast completed: result=0, data="saved\nown: 40 KB (in use)"\n'; fi ;;
  *sa.urtv.home.FAJR_VOICE*) printf 'Broadcast completed: result=0, data="saved\nown: none"\n' ;;
  *sa.urtv.home.VOICE*"--ei voice 0"*)
    if [ -n "${FAKE_NO_OWN:-}" ]; then echo 'Broadcast completed: result=0, data="error: no recording of your own yet"'
    else printf 'Broadcast completed: result=0, data="saved\nown: 40 KB (in use)"\n'; fi ;;
  *sa.urtv.home.VOICE*) printf 'Broadcast completed: result=0, data="saved\nown: none"\n' ;;
esac
''')
os.chmod(fake_py, 0o755)

fake_ffmpeg = os.path.join(tmp, "fakeffmpeg")
with open(fake_ffmpeg, "w") as f:
    f.write(r'''#!/usr/bin/env bash
echo "$*" >> "$FFLOG"
[ -z "${FAKE_FFMPEG_FAIL:-}" ] || { echo "input.mp3: Invalid data found when processing input" >&2; exit 1; }
out="${@: -1}"
printf '\0\0\0\040ftypM4A ' > "$out"
head -c "${FAKE_OUT_SIZE:-40000}" /dev/zero >> "$out"
''')
os.chmod(fake_ffmpeg, 0o755)
fake_ffprobe = os.path.join(tmp, "fakeffprobe")
with open(fake_ffprobe, "w") as f:
    f.write('#!/usr/bin/env bash\necho "${FAKE_DURATION:-200}.500000"\n')
os.chmod(fake_ffprobe, 0o755)

failed = 0
def run(*args, env_extra=None, ffmpeg=fake_ffmpeg, ffprobe=fake_ffprobe):
    for p in (sent_log, ff_log):
        open(p, "w").close()
    for f in os.listdir(kept):
        os.remove(os.path.join(kept, f))
    env = {**os.environ, "PY": fake_py, "SENT": sent_log, "KEPT": kept, "FFLOG": ff_log, "FFMPEG": ffmpeg, "FFPROBE": ffprobe}
    for k in ("FAKE_ADD_FAIL", "FAKE_FFMPEG_FAIL", "FAKE_OUT_SIZE", "FAKE_DURATION", "FAKE_NO_OWN", "FAKE_OWN", "FAKE_VOICE"):
        env.pop(k, None)
    env.update(env_extra or {})
    r = subprocess.run(["bash", script, *args], capture_output=True, text=True, env=env, timeout=300)
    sent = open(sent_log, encoding="utf-8").read().strip().split("\n") if os.path.getsize(sent_log) else []
    return r.returncode, r.stdout + r.stderr, sent, open(ff_log).read()

def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

OWN = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_OWN "
VOICE = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.VOICE "
song = os.path.join(tmp, "my adhan.mp3")
open(song, "wb").write(b"ID3 not really sound; the fake ffmpeg does not read it")

# ---- adhan add
rc, out, sent, ff = run("adhan", "add", song)
pushes = [s for s in sent if s.startswith("PUSH")]
check("add: ok (the file name has a space in it), says it is the adhan now and that it stays on the TV", rc == 0 and "unbound" not in out and "it is the adhan now" in out and "stays on this TV" in out, out)
check("add: mono AAC at 48 kbps, at most 300 s, no tags, the first audio stream only",
      all(x in ff for x in ("-ac 1", "-c:a aac", "-b:a 48k", "-t 300", "-map_metadata -1", "-map 0:a:0")) and "-af" not in ff, ff)
check("add: the original is never uploaded, one small file goes to the staging place",
      pushes == ["PUSH /data/local/tmp/urtv-own/in.m4a"] and len(os.listdir(kept)) == 1, pushes)
size = os.path.getsize(os.path.join(kept, "0.m4a"))
check("add: the shell streams it into the launcher's own door, then the app is told its size",
      any(s == "content write --uri content://sa.urtv.home.adhan/own-in < /data/local/tmp/urtv-own/in.m4a" for s in sent)
      and OWN + f"--es op add --ei size {size}" in sent, sent)
check("add: the staging file is removed, and the answer shows its size", any(s.startswith("rm -f /data/local/tmp/urtv-own/in.m4a") for s in sent)
      and "own: 40 KB (in use)" in out, out)

rc, out, sent, ff = run("adhan", "add", song, env_extra={"FAKE_DURATION": "420"})
check("add: longer than 5 minutes: cut at 5:00 with a fade, and says so",
      rc == 0 and "-t 300" in ff and "afade=t=out:st=296:d=4" in ff and "7:00 long" in out and "first 5:00" in out, (out, ff))
rc, out, sent, ff = run("adhan", "add", song, env_extra={"FAKE_DURATION": "300"})
check("add: exactly 5 minutes: nothing cut, no fade", rc == 0 and "-af" not in ff and "first 5:00" not in out, (out, ff))

rc, out, sent, ff = run("adhan", "add", song, env_extra={"FAKE_ADD_FAIL": "first"})
check("add: where `content write` cannot work, the app reads the staged file itself",
      rc == 0 and any("--es path /data/local/tmp/urtv-own/in.m4a" in s for s in sent), (rc, out, sent))
rc, out, sent, ff = run("adhan", "add", song, env_extra={"FAKE_ADD_FAIL": "all"})
check("add: when both ways fail, says so and exits 1", rc != 0 and "error: not an m4a" in out and "it is the adhan now" not in out, out)

rc, out, sent, ff = run("adhan", "add", song, ffmpeg="/nonexistent/ffmpeg")
check("add: no ffmpeg: says what is needed and how to get it, nothing sent to the TV",
      rc != 0 and "ffmpeg is needed" in out and "brew install ffmpeg" in out and "apt install ffmpeg" in out and not sent, (out, sent))
rc, out, sent, ff = run("adhan", "add", song, env_extra={"FAKE_FFMPEG_FAIL": "1"})
check("add: ffmpeg cannot read the file: says so, nothing uploaded", rc != 0 and "could not make an adhan" in out and "Invalid data" in out and not sent, (out, sent))
rc, out, sent, ff = run("adhan", "add", song, env_extra={"FAKE_OUT_SIZE": str(4 << 20)})
check("add: a result over 3 MB is refused before any upload", rc != 0 and "still" in out and not sent, (out, sent))
rc, out, sent, ff = run("adhan", "add", os.path.join(tmp, "missing.mp3"))
check("add: a missing file is refused, nothing sent", rc != 0 and "is not a file" in out and not sent and not ff, (out, sent))
rc, out, sent, ff = run("adhan", "add")
check("add without a file: usage", rc != 0 and "Usage" in out and not sent, out)
rc, out, sent, ff = run("adhan", "add", song, song)
check("add with two files: usage (one recording only)", rc != 0 and "Usage" in out and not sent, out)

# ---- voice own / numbers, own-clear, the state
rc, out, sent, ff = run("adhan", "voice", "own")
check("voice own", rc == 0 and sent == [VOICE + "--ei voice 0"] and "your own recording" in out, (out, sent))
rc, out, sent, ff = run("adhan", "voice", "own", env_extra={"FAKE_NO_OWN": "1"})
check("voice own with no recording: says to add one, exit 1", rc != 0 and "adhan add" in out, out)
rc, out, sent, ff = run("adhan", "voice", "7")
check("voice 7 still works", rc == 0 and sent == [VOICE + "--ei voice 7"] and out.strip() == "Adhan voice 7.", (out, sent))
for bad in ("0", "22", "x", "own; reboot", "-1", ""):
    rc, out, sent, ff = run("adhan", "voice", bad) if bad else run("adhan", "voice")
    check(f"voice {bad!r}: refused, nothing sent", rc != 0 and not sent, (out, sent))
# ---- «مؤذن الفجر», the Fajr voice, and «أذان مختصر»
FAJR = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.FAJR_VOICE "
SHORT = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_SHORT "
for fajr_recording in ("10", "13", "17", "20"):
    rc, out, sent, ff = run("adhan", "voice", fajr_recording)
    check(f"voice {fajr_recording} is a Fajr recording: refused, pointed to fajr-voice, nothing sent",
          rc == 2 and f"fajr-voice {fajr_recording}" in out and not sent, (rc, out, sent))
rc, out, sent, ff = run("adhan", "fajr-voice", "13")
check("fajr-voice 13", rc == 0 and sent == [FAJR + "--ei voice 13"] and out.strip() == "Fajr voice 13.", (out, sent))
rc, out, sent, ff = run("adhan", "fajr-voice", "5")
check("fajr-voice takes a normal recording too", rc == 0 and sent == [FAJR + "--ei voice 5"], (out, sent))
rc, out, sent, ff = run("adhan", "fajr-voice", "own")
check("fajr-voice own", rc == 0 and sent == [FAJR + "--ei voice 0"] and "your own recording" in out, (out, sent))
rc, out, sent, ff = run("adhan", "fajr-voice", "own", env_extra={"FAKE_NO_OWN": "1"})
check("fajr-voice own with no recording of the person's: refused", rc != 0 and "adhan add" in out, out)
for bad in ("0", "22", "x", "own; reboot", "-1"):
    rc, out, sent, ff = run("adhan", "fajr-voice", bad)
    check(f"fajr-voice {bad!r}: refused, nothing sent", rc != 0 and not sent, (out, sent))
rc, out, sent, ff = run("adhan", "fajr-voice")
check("fajr-voice without a number: usage", rc != 0 and not sent, out)
rc, out, sent, ff = run("adhan", "now", "fajr")
check("now fajr plays the Fajr voice", rc == 0 and sent == ["am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN --ez fajr true"], (out, sent))
rc, out, sent, ff = run("adhan", "short", "on")
check("short on", rc == 0 and sent == [SHORT + "--ez on true"] and "first minute" in out, (out, sent))
rc, out, sent, ff = run("adhan", "short", "off")
check("short off", rc == 0 and sent == [SHORT + "--ez on false"], (out, sent))
for bad in ("yes", "", "on off"):
    rc, out, sent, ff = run("adhan", "short", bad) if bad else run("adhan", "short")
    check(f"short {bad!r}: refused, nothing sent", rc != 0 and not sent, (out, sent))

STOPKEY = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_STOPKEY "
rc, out, sent, ff = run("adhan", "stopkey", "on")
check("stopkey on", rc == 0 and sent == [STOPKEY + "--ez on true"] and "Any key" in out, (out, sent))
rc, out, sent, ff = run("adhan", "stopkey", "off")
check("stopkey off", rc == 0 and sent == [STOPKEY + "--ez on false"], (out, sent))
for bad in ("yes", "", "on off"):
    rc, out, sent, ff = run("adhan", "stopkey", bad) if bad else run("adhan", "stopkey")
    check(f"stopkey {bad!r}: refused, nothing sent", rc != 0 and not sent, (out, sent))
SAVERONLY = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_SAVERONLY "
rc, out, sent, ff = run("adhan", "saver-only", "on")
check("saver-only on", rc == 0 and sent == [SAVERONLY + "--ez on true"] and "screensaver" in out, (out, sent))
rc, out, sent, ff = run("adhan", "saver-only", "off")
check("saver-only off", rc == 0 and sent == [SAVERONLY + "--ez on false"], (out, sent))
for bad in ("yes", "", "on off"):
    rc, out, sent, ff = run("adhan", "saver-only", bad) if bad else run("adhan", "saver-only")
    check(f"saver-only {bad!r}: refused, nothing sent", rc != 0 and not sent, (out, sent))
NOTICE = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_NOTICE "
rc, out, sent, ff = run("adhan", "notice", "on")
check("notice on", rc == 0 and sent == [NOTICE + "--ez on true"] and "5-second" in out, (out, sent))
rc, out, sent, ff = run("adhan", "notice", "off")
check("notice off", rc == 0 and sent == [NOTICE + "--ez on false"], (out, sent))
for bad in ("yes", "", "on off"):
    rc, out, sent, ff = run("adhan", "notice", bad) if bad else run("adhan", "notice")
    check(f"notice {bad!r}: refused, nothing sent", rc != 0 and not sent, (out, sent))
TAKBIR = "am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_TAKBIR "
rc, out, sent, ff = run("adhan", "takbir", "on")
check("takbir on", rc == 0 and sent == [TAKBIR + "--ez on true"] and "takbir" in out, (out, sent))
rc, out, sent, ff = run("adhan", "takbir", "off")
check("takbir off", rc == 0 and sent == [TAKBIR + "--ez on false"], (out, sent))
for bad in ("yes", "", "on off"):
    rc, out, sent, ff = run("adhan", "takbir", bad) if bad else run("adhan", "takbir")
    check(f"takbir {bad!r}: refused, nothing sent", rc != 0 and not sent, (out, sent))
rc, out, sent, ff = run("adhan", "stop")
check("adhan stop sends STOP_ADHAN only", rc == 0 and sent == ["am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.STOP_ADHAN"], (out, sent))

rc, out, sent, ff = run("adhan", "own-clear")
check("own-clear", rc == 0 and sent == [OWN + "--es op clear"] and "removed" in out, (out, sent))
rc, out, sent, ff = run("adhan", "own-clear", "x")
check("own-clear takes no argument", rc != 0 and not sent, out)
rc, out, sent, ff = run("adhan", env_extra={"FAKE_OWN": "1843200", "FAKE_VOICE": "own"})
check("adhan state says the voice is own and its size", rc == 0 and "voice=own" in out and "own=1800 KB" in out, out)
rc, out, sent, ff = run("adhan")
check("adhan state with none: says how to add one", rc == 0 and "own=none" in out and "adhan add FILE" in out and "voice=3" in out, out)
check("adhan state shows the Fajr voice and the short adhan", "fajr_voice=10" in out and "adhan_short=off" in out, out)

# ---- with the real ffmpeg, on generated sound
if shutil.which("ffmpeg") and shutil.which("ffprobe"):
    def probe(path, entry):
        return subprocess.run(["ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries", entry, "-of", "csv=p=0", path],
                              capture_output=True, text=True).stdout.strip()
    def tone(name, seconds, src="sine=frequency=440", ext="wav"):
        p = os.path.join(tmp, f"{name}.{ext}")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", f"{src}:duration={seconds}", "-ac", "2", p], check=True)
        return p
    short = tone("short", 12, "sine=frequency=440:sample_rate=44100")
    rc, out, sent, ff = run("adhan", "add", short, ffmpeg="ffmpeg", ffprobe="ffprobe")
    m4a = os.path.join(kept, "0.m4a")
    check("real ffmpeg: a stereo wav becomes a mono AAC m4a", rc == 0 and os.path.exists(m4a) and probe(m4a, "stream=codec_name,channels") == "aac,1", (out, os.listdir(kept)))
    rate = int(probe(m4a, "stream=bit_rate") or 0) if os.path.exists(m4a) else 0
    check("real ffmpeg: about 48 kbps", 30000 <= rate <= 70000, rate)
    check("real ffmpeg: it opens as an MP4 (the TV's check sniffs «ftyp»)", os.path.exists(m4a) and open(m4a, "rb").read(12)[4:8] == b"ftyp")
    long = tone("long", 330, "anoisesrc=color=pink:sample_rate=22050")
    rc, out, sent, ff = run("adhan", "add", long, ffmpeg="ffmpeg", ffprobe="ffprobe")
    m4a = os.path.join(kept, "0.m4a")
    dur = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", m4a], capture_output=True, text=True).stdout or 0) if os.path.exists(m4a) else 0
    check("real ffmpeg: 5:30 of sound is kept as 5:00, under 3 MB, and says so",
          rc == 0 and 299 <= dur <= 301 and os.path.getsize(m4a) < 3 << 20 and "first 5:00" in out, (rc, dur, out))
    check("real ffmpeg: no tags travel with it (no title, no artist, no encoder note we did not set)",
          "artist" not in subprocess.run(["ffprobe", "-v", "error", "-show_format", m4a], capture_output=True, text=True).stdout.lower())
else:
    print("SKIP real ffmpeg cases: ffmpeg and ffprobe are not installed here")

shutil.rmtree(tmp, ignore_errors=True)
sys.exit(1 if failed else 0)
