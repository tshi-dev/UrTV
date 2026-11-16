#!/usr/bin/env python3
"""4.4 «صوتياتي»: the person's own sound files, from a USB stick or the phone page, kept in the app. AudioRules (run for real, tests/AudioRulesRun.java):
a sound by its first bytes (mp3, m4a, ogg, wav), 20 MB a file and 100 MB in all (50 and 500 with «سعة أكبر», behind a red warning), the name shown,
the phone's pieces in order. The source checks: the stick's file is copied (never played from it), the phone sends 512 KB pieces behind the session,
a file becomes «أذاني الخاص» or the Fajr one as a copy, the read permission is asked only when the stick is opened.   python3 tests/audio_library_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L = ROOT / "android/launcher/src/main"
T = L / "java/sa/urtv/home/tilawah"
P = L / "java/sa/urtv/home/phone"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

rules, store, act, use, bridge = (read(T / f) for f in ("AudioRules.java", "AudioStore.java", "AudioActivity.java", "AudioUse.java", "AudioBridge.java"))
srv, api, schema, acts = (read(P / f) for f in ("PhoneServer.java", "PhoneApi.java", "Schema.java", "PhoneActions.java"))
man, page, index = read(L / "AndroidManifest.xml"), read(L / "assets/phone/audio.html"), read(L / "assets/phone/index.html")

check("AudioRules is plain Java", "import android" not in rules and "sa.urtv.home.S" not in rules)
check("the store keeps the files in the app's own folder, under its own names (a<N>.<ext>), never the stick's path",
      'new File(c.getFilesDir(), "audio")' in store and '"a" + it.id + "." + it.ext' in store)
add = store[store.index("static synchronized Added add(Context c, InputStream in"):]
add = add[:add.index("\n    }\n")]
check("an add is refused before a byte is written, stops past the cap while copying, and is judged by its bytes and by Android's reader",
      add.index("mayAdd(c, size)") < add.index("new FileOutputStream(tmp)") and "if (wrote > cap)" in add and "AudioRules.kind(head, headN)" in add and "length(tmp)" in add)
check("the stick: only listed sound files, the big ones shown but not copied, the copy off the screen's thread",
      "AudioRules.listed(f.getName())" in act and 'S.t("أكبر من %s: لا يُنسخ"' in act and '"audio-copy"' in act and "AudioStore.add(this, f, f.getName())" in act)
check("nothing on the stick is played: the preview plays the copy in the app",
      "AudioStore.file(this, it).getPath()" in act and act.count("setDataSource(") == 1)
check("the stick's files are read only after the person allows it (READ_MEDIA_AUDIO, or READ_EXTERNAL_STORAGE up to Android 12)",
      'android:name="android.permission.READ_MEDIA_AUDIO"' in man and 'android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"' in man
      and "requestPermissions(" in act and "if (!mayRead()) { askRead(); return; }" in act)
check("«سعة أكبر» is behind a red warning about the TV's storage, and turning it off never deletes anything",
      "ask(S.t(\"سعة أكبر\"), S.t(\"تنبيه: الملفات الكبيرة تأخذ من مساحة التلفزيون نفسها" in act and "\"), true," in act[act.index("private void toggleBig()"):]
      and "AudioRules.mayShrink(used(c), largest)" in store and "static final int WARN = 0xFFFF6B60;" in read(T / "RowScreen.java"))
check("the phone: a piece is at most 512 KB, the same number on both sides", "MAX_AUDIO_BODY = 512 * 1024" in srv and "CHUNK = 512 * 1024" in rules)
au = api[api.index("if (r.path.equals(PhoneServer.AUDIO_PATH))"):]
au = au[:au.index("}\n            if (type == null")]
check("the phone: the origin first, then bytes only, then the session (401 without), then the piece",
      api.index("Guard.originOk") < api.index("if (r.path.equals(PhoneServer.AUDIO_PATH))") and "application/octet-stream" in au
      and au.index("session(r)") < au.index("audioPiece(") and 'return json(401, err("no_session"));' in au)
check("the phone: the name comes percent-encoded and is cleaned before it is shown (AudioRules.name)",
      "URLDecoder.decode" in api and "encodeURIComponent(file.name)" in page and "AudioRules.name(name)" in store)
check("the phone: a new upload is checked for room before its first piece is kept, then piece by piece in order",
      "String why = mayAdd(c, total);" in store and "AudioRules.nextChunk(part.length(), offset, data.length, upTotal)" in store)
check("the phone: /api/audio and the piece need the session", '"/api/audio", PhoneServer.AUDIO_PATH' in api)
check("the phone: use and delete are allow-listed (op, id, as)", 'case "audio": {' in schema and 'only(body, "op", "id", "as")' in schema and 'AUDIO_USES = {"own", "own_fajr", "none"}' in schema)
check("the phone page: builds no HTML from data, one script with the nonce, the session as the other pages",
      not re.search(r"innerHTML|outerHTML|insertAdjacentHTML|document\.write|\beval\(", page) and page.count("<script") == 1
      and '<script nonce="__NONCE__">' in page and "'X-UrTV-Session'" in page and "credentials: 'same-origin'" in page)
check("the phone page sends pieces of the file (file.slice), never the whole file at once", "file.slice(at, Math.min(size, at + step))" in page and "'/api/audio_piece'" in page)
check("the main phone page links «صوتياتي» (/a), still under 64 KB", '<a class="rw" href="/a"' in index and len(index.encode()) < 64 * 1024)
check("a file becomes the own adhan (or the Fajr one) as a copy, at most 5 minutes, as music.sh adhan add does",
      "OwnAdhan.keepFromLibrary(c, AudioStore.file(c, it), fajr)" in use and "it.ms > OwnAdhanText.MAX_MS" in use
      and "SafeReplace.replace(src, fajr ? fajrFile(c) : file(c), false" in read(T / "OwnAdhan.java"))
check("«تلاوة»'s adhan tab opens «صوتياتي»", "startActivity(new Intent(this, AudioActivity.class));" in read(T / "MainActivity.java")
      and 'android:name=".tilawah.AudioActivity"' in man and 'android:exported="false"' in man[man.index('.tilawah.AudioActivity'):man.index('.tilawah.AudioActivity') + 200])

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "AudioRules.java"), str(ROOT / "tests/AudioRulesRun.java")], capture_output=True, text=True)
        check("AudioRules compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.AudioRulesRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 24, len(out.splitlines()))
sys.exit(1 if failed else 0)
