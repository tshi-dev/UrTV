#!/usr/bin/env python3
"""4.3 «صوري»: up to 50 photos (10 on a weak TV; one is decoded at a time, so only flash grows, and 200 MB stay free), and from a paired
phone: the /u page re-encodes each one at 1920 px (no camera or location data), POST /api/photo takes a raw JPEG of at most 1 MB and only
behind the session, and the screensaver's provider refuses a file that still carries camera data (JpegMeta, run for real here).
    python3 tests/photos_phone_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
L = ROOT / "android/launcher/src/main"
P = L / "java/sa/urtv/home/phone"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")

own, prov = read(A / "Own.java"), read(A / "ConfigProvider.java")
check("Own: 50 photos, 10 on a weak TV, 200 MB left free after each", "static final int MAX = 50, MAX_MID = 25, MAX_LOW = 10;" in own and "FREE_KEEP = 200L << 20" in own
      and "getUsableSpace() < FREE_KEEP" in own)
check("photos.sh says 50", "up to 50 in all; 10 on a weak TV" in read(ROOT / "optimize/photos.sh"))
call = prov[prov.index('case "phone_photo_add"'):]
call = call[:call.index('case "phone_state"')]
check("the provider: only through phone_ (PHONE_SETTINGS, signature), a JPEG of at most 3 MB, no camera data, room, then Own.add (its own checks)",
      'throw new SecurityException("phone control only")' in prov
      and prov.index('throw new SecurityException("phone control only")') < prov.index('case "phone_photo_add"')
      and "jpeg.length > Own.MAX_BYTES" in call and "Own.cameraData(jpeg)" in call and "Own.count(c) >= Own.max(c)" in call and "Own.add(c, Own.incoming(c))" in call)
srv, api = read(P / "PhoneServer.java"), read(P / "PhoneApi.java")
check("the server: a body over 4 KB only for POST /api/photo as image/jpeg, at most 1 MB, kept as bytes",
      'boolean photo = target.equals(PHOTO_PATH) && "image/jpeg".equals(' in srv and "MAX_PHOTO_BODY = 1_000_000" in srv
      and "if (n > (photo ? MAX_PHOTO_BODY : MAX_BODY)) fail(413);" in srv and "return new Request(p[0], target, h, \"\", s.getInetAddress().getHostAddress(), buf);" in srv)
ph = api[api.index("if (r.path.equals(PhoneServer.PHOTO_PATH))"):]
ph = ph[:ph.index("}\n            if (type == null")]
check("the API: the origin checks first, then a session (401 without), then the photo; nothing else takes raw bytes",
      api.index("Guard.originOk") < api.index("if (r.path.equals(PhoneServer.PHOTO_PATH))") and "if (secret == null) return json(401, err(\"no_session\"));" in ph
      and "control.actions().photoAdd(r.raw)" in ph and api.count("r.raw") == 2)
page = read(L / "assets/phone/photos.html")
check("/u: re-drawn at 1920 px at most and sent as a new JPEG (no camera data), under 900 KB, with the session; English words when the TV says lang=en",
      "createImageBitmap(file)" in page and "MAX = 1920, LIMIT = 900000" in page and "c.toBlob(res, 'image/jpeg', q[i])" in page
      and "'X-UrTV-Session'" in page and "fetch('/api/photo'" in page and "var EN = document.documentElement.lang === 'en';" in page
      and '<script nonce="__NONCE__">' in page and 'href="/u"' in read(L / "assets/phone/index.html"))
sr = read(A / "SaverReceiver.java")
check("4.3 S11: a photo add whose size came as a string (--es) is refused, never taken as «no size given»",
      'if (given != null && !(given instanceof Integer)) {' in sr and sr.index("instanceof Integer") < sr.index('int size = i.getIntExtra("size", -1);'))
if not shutil.which("javac"):
    print("SKIP JpegMeta's run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(A / "JpegMeta.java"), str(ROOT / "tests/PhotosPhoneRun.java")], capture_output=True, text=True)
        check("JpegMeta compiles on its own (plain Java)", r.returncode == 0, r.stderr[-500:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.PhotosPhoneRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                n, res = line.rsplit("\t", 1)
                check(n, res == "ok")
            check("every case ran", len(out.splitlines()) == 8, out)
sys.exit(1 if failed else 0)
