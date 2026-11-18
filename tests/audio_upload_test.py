#!/usr/bin/env python3
"""4.4 «صوتياتي» end to end, the rough edges found by an audit of the upload flow:
  - a sound deleted while it was «صوت الإقامة»: the iqama goes back to no sound (it kept pointing at a missing file, alarms with nothing to play);
  - the copy and Android's reader run outside the store's lock (a 50 MB file held it for seconds while an iqama or a dua alarm waited on list());
  - one phone upload at a time: a second start while one sends is refused «busy» (it used to start over the first and break both), the page
    keeps its picker off while it sends, and the last piece is moved aside before the copy so the next upload never waits on it;
  - an upload the phone never finished (the part, a copy not kept) is swept after ten minutes, the first time the folder is used;
  - the phone page says what went wrong (the TV's storage, an empty file, another upload, too fast, the duas' list full) instead of «check the
    Wi-Fi» for everything; an empty file is never sent; «duas_full» is no longer the files' «too_many»; the iqama's use is shown.
AudioRules' new rules run for real.
    python3 tests/audio_upload_test.py"""
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
st, use, act = read(T / "AudioStore.java"), read(T / "AudioUse.java"), read(T / "AudioActivity.java")
page = read(ROOT / "android/launcher/src/main/assets/phone/audio.html")
rm = st[st.index("static synchronized void remove("):st.index("static synchronized void rename(")]

check("a deleted sound that was «صوت الإقامة»: the iqama has no sound, its alarm set again", 'if (("a" + id).equals(prefs.iqamaSound())) {' in rm and 'prefs.setIqamaSound("");' in rm and "AdhanScheduler.schedule(c);" in rm)
check("the iqama is a use of the sound (the TV's line, the phone's list)", 'if (("a" + id).equals(new Prefs(c).iqamaSound())) return "iqama";' in use and '"iqama".equals(use) ? S.t("صوت الإقامة")' in act)
check("«أوقف استخدامه للأذان» only for an adhan use (it did nothing for a dua)", 'if ("own".equals(use) || "own_fajr".equals(use)) out.add(Row.of(S.t("أوقف استخدامه للأذان")' in act)
check("the phone's «بلا استخدام» on a dua or the iqama says it is changed on the TV", 'if (now.isEmpty() && !of(c, it.id).isEmpty()) return "on_tv";' in use)
check("the duas' list full is its own word", 'return DuaStore.put(c, d) ? null : "duas_full";' in use and '"too_many"' not in use)
check("reads never wait for a copy: list() is not synchronized, add() is not", "    static List<Item> list(Context c) {" in st and "static Added add(Context c, InputStream in, long size, String name) {" in st
      and "static synchronized Added add(Context c, InputStream in" not in st)
check("the number is taken at once, the list changed under the lock with the caps checked again", "private static synchronized int reserve(Context c)" in st
      and "private static synchronized Added keep(Context c, File tmp, Item it)" in st and "AudioRules.refuse(it.bytes, used(c), list(c).size(), big(c))" in st)
check("one upload at a time: «busy» while another sent a piece in the last 30 s", 'if (AudioRules.busy(upTotal, upLast, System.currentTimeMillis())) return "busy";' in st and "upLast = System.currentTimeMillis();" in st)
check("the last piece is moved aside and copied outside the lock", 'File whole = new File(dir(c), "phone-" + (++upDone) + ".done");' in st and "private static synchronized Object piece(" in st
      and "    static String uploadPiece(Context c, long offset, byte[] data) {" in st)
check("leftovers swept once per start of the app, after ten minutes", "if (!swept) { swept = true; sweep(d, System.currentTimeMillis()); }" in st and "STALE_MS = 10 * 60_000L" in st)
check("the page: one file at a time, an empty file not sent, a 429 is «too fast»", "pick.disabled = true;" in page and "function end() { pick.disabled = false;" in page
      and "if (!size) { say(W.empty); return; }" in page and "r.status === 429 ? { ok: false, error: 'slow_down' }" in page)
words = ["copy", "empty", "busy", "order", "slow_down", "duas_full", "on_tv", "iqama"]
check("the page says each new code, in Arabic and English", all(page.count(w + ": '") == 2 for w in words), [w for w in words if page.count(w + ": '") != 2])

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
public class UpRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  long t = 1_000_000;
  out("busy while a piece came in the last 30 s", AudioRules.busy(5_000_000, t - 29_000, t) && !AudioRules.busy(5_000_000, t - 31_000, t) && !AudioRules.busy(0, t, t));
  out("leftovers: the part, a moved last piece, a copy not kept", AudioRules.leftover("phone.part") && AudioRules.leftover("phone-3.done") && AudioRules.leftover("in-12.tmp"));
  out("never a kept sound", !AudioRules.leftover("a12.mp3") && !AudioRules.leftover("a3.ogg") && !AudioRules.leftover("in-x.tmp") && !AudioRules.leftover(null));
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "UpRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "AudioRules.java"), str(Path(tmp, "UpRun.java"))], capture_output=True, text=True)
        check("AudioRules compiles on its own", r.returncode == 0, r.stderr[-500:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.UpRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 3, len(out.splitlines()))
sys.exit(1 if failed else 0)
