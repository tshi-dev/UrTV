#!/usr/bin/env python3
"""4.3.3 «مسح الملفات المؤقتة» (الإعدادات ← حول): the two apps' caches only. CacheSweep runs for real on a JVM over made-up folders; the row, its
question and the home's side are read from the source. Never the settings, the person's own photos, the packs, the adhan's sounds or an APK on its way.

    python3 tests/cache_sweep_test.py      (needs javac)"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


a, h = (APP / "CacheSweep.java").read_text(encoding="utf-8"), (HOME / "CacheSweep.java").read_text(encoding="utf-8")
strip = lambda t: t.split("\n", 1)[1].replace("public ", "")
check("the same sweep in both apps (only the package and the home's public access differ)", strip(a) == strip(h))
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        cls = Path(tmp) / "cls"
        subprocess.run(["javac", "-d", str(cls), str(APP / "CacheSweep.java"), str(ROOT / "tests/CacheSweepRun.java")], check=True, capture_output=True)
        cache, photos = Path(tmp) / "cache", Path(tmp) / "photos"
        for f, n in (("cache/a.json", 100), ("cache/WebView/x/y.bin", 300), ("cache/adhan/adhan-1.mp3", 5000), ("cache/suggested.apk", 7000),
                     ("photos/1.jpg", 2000), ("own/mine.jpg", 900), ("packs/space/1.jpg", 800), ("shared_prefs/s.xml", 50)):
            p = Path(tmp) / f
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(b"x" * n)
        run = lambda m: int(subprocess.run(["java", "-cp", str(cls), "sa.urtv.screensaver.CacheSweepRun", m, str(cache), str(photos)], check=True, capture_output=True).stdout)
        check("the size: the caches only, without the adhan's sounds and an APK on its way", run("size") == 2400, run("size"))
        freed = run("clear")
        left = sorted(str(p.relative_to(tmp)) for p in Path(tmp).rglob("*") if p.is_file() and "cls" not in p.parts)
        check("the sweep frees what it said and leaves the adhan's sounds, the APK, the person's photos, the packs and the settings",
              freed == 2400 and left == ["cache/adhan/adhan-1.mp3", "cache/suggested.apk", "own/mine.jpg", "packs/space/1.jpg", "shared_prefs/s.xml"]
              and cache.is_dir() and photos.is_dir(), (freed, left))
else:
    print("SKIP the sweep on a JVM: no javac here")
st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
fn = st[st.index("private void clearCaches()"):].split("\n    }\n")[0]
check("the row is in «حول» and asks first with the size, then clears and says what was freed; the sizes off the UI thread",
      'row("مسح الملفات المؤقتة"' in st and '{"مسح الملفات المؤقتة", UPDATE}' in (APP / "SettingsHub.java").read_text(encoding="utf-8")
      and "new Thread(() -> {" in fn and fn.index('homeCache("cache_size")') < fn.index('.setPositiveButton("امسح"') < fn.index('homeCache("clear_cache")')
      and 'setNegativeButton("إلغاء", null)' in fn and '"مُسح " + megabytes(freed)' in fn)
check("this app's caches: its cache folder and the hosted photos; never Own (the person's photos) or PackStore (the packs)",
      'return new java.io.File[] {getCacheDir(), new java.io.File(getFilesDir(), "photos")};' in st
      and 'new File(c.getFilesDir(), "own")' in (APP / "Own.java").read_text(encoding="utf-8") and 'new File(c.getFilesDir(), "packs")' in (APP / "PackStore.java").read_text(encoding="utf-8"))
np = (HOME / "tilawah/NowPlayingProvider.java").read_text(encoding="utf-8")
check("the home's side: its cache folder only, size or sweep, for the same key only", '"cache_size".equals(method) || "clear_cache".equals(method)' in np
      and 'checkCallingPermission("sa.urtv.home.READ_NOW_PLAYING") == PackageManager.PERMISSION_GRANTED' in np.split("cache_size", 1)[1][:300]
      and "java.io.File cache = getContext().getCacheDir();" in np)
sys.exit(1 if failed else 0)
