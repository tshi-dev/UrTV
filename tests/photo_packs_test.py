#!/usr/bin/env python3
"""Screensaver photo packs («صور شاشة التوقف», docs/PHOTO-PACKS.md): the rules on a JVM (PhotoPacks.java), and the wiring read from the source:
a pack's zip is checked against the index's sha256 BEFORE it is opened, only N.jpg and credits.json come out of it, the zip comes only from our
releases, the page asks the app for ID/N.jpg and the app answers only a pack on the TV, «صورنا» stays on by default, the settings row is there,
the index in the repository is valid and every pack in it can be downloaded by the client's own rules.
Run: python3 tests/photo_packs_test.py   (the JVM part needs javac; without it, skipped with a note)"""
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def src(n):
    return (APP / n).read_text(encoding="utf-8")


store, cache, prefs, st, act, rules = (src(n) for n in ("PackStore.java", "PhotoCache.java", "Prefs.java", "SettingsActivity.java",
                                                         "PhotoPacksActivity.java", "PhotoPacks.java"))
tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")

dl = store[store.index("static String download("):]
check("download: the sha256 is compared before the zip is opened (the digest, then «matches», then unpack)",
      0 < dl.index("PhotoPacks.sha256(") < dl.index("PhotoPacks.matches(digest") < dl.index("unpack(zip, work, p)"))
check("download: a wrong digest returns «sha» and nothing is unpacked or kept", 'return "sha";' in dl and dl.index('return "sha";') < dl.index("unpack(zip, work, p)"))
check("download: only a valid index entry is fetched (the address is ours), and the zip is bounded",
      "if (!PhotoPacks.valid(p)) return" in dl and "PhotoPacks.ZIP_MAX" in dl)
un = store[store.index("private static String unpack("):store.index("/** «حذف»")]
check("unpack: every entry's name goes through PhotoPacks.safeEntry, a photo must be a JPEG that decodes, all 1..count there",
      "PhotoPacks.safeEntry(name)" in un and "PhotoPacks.looksJpeg(data)" in un and "PhotoCache.validImage(data)" in un and "PhotoPacks.complete(has, p.count)" in un)
check("unpack: written into a work folder and moved into place whole (a half pack is never there)",
      "work.renameTo(dest)" in dl and "deleteTree(work)" in dl)
check("installed: a folder without its credits or with a gap is not a pack on the TV", 'new File(d, "credits.json").isFile()' in store and "PhotoPacks.complete(has, n)" in store)
check("the app answers the page's ID/N.jpg and ID/credits.json only for ids and names of the allowed form, and only what is on the TV",
      "https://packs.urtv.invalid/" in cache and "if (url.startsWith(PACKS)) return pack(c, url.substring(PACKS.length()));" in cache
      and "[1-9][0-9]{0,2}" in cache and "f.isFile() ? jpeg(" in cache)
check("«صورنا» is on by default, and off counts only while a pack that is on is on the TV",
      'sp.getBoolean("oursOn", true)' in prefs and "PhotoPacks.mayTurnOursOff(packsOn(), have)" in prefs)
check("the fragment carries #packs= and #ours=0 only when set", 'return PhotoPacks.fragment(packsOn(), have) + (ours ? "" : "&ours=0");' in prefs and "+ packsParam();" in prefs)
check("the settings row opens the picker, and it is a row of the hub", 'row("صور شاشة التوقف", packsLabel()' in st and "PhotoPacksActivity.class" in st
      and '{"صور شاشة التوقف", LOOK}' in src("SettingsHub.java"))
check("the picker: OK downloads a pack that is not on the TV, else turns it on or off, deletes it; the credits are one button",
      "download(p)" in act and "choose(p, active)" in act and "PackStore.delete(" in act and "showCredits()" in act and 'w("حقوق الصور")' in act)
check("the activity is in the manifest and not exported", 'android:name=".PhotoPacksActivity" android:exported="false"' in (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
check("the page: #packs= only where the app answers (UrtvNative), ours off by #ours=0, the packs join the pool and each photo carries its credit",
      "window.UrtvNative ? (_Q.get('packs')" in tpl and "_Q.get('ours') !== '0'" in tpl and "pool = ours.concat(mine)" in tpl and "loadPackCredits();" in tpl
      and "showOwn(pick, mine, local)" in tpl)
check("the built pages are the template's (src/build.py was run)", "packs.urtv.invalid" in (ROOT / "dist/tv.html").read_text(encoding="utf-8")
      and "packs.urtv.invalid" in (ROOT / "dist/tv-en.html").read_text(encoding="utf-8"))

# the index in the repository, read by the client's own rules
idx = json.loads((ROOT / "photos/packs.json").read_text(encoding="utf-8"))
check("photos/packs.json is version 1 with a list of packs", idx.get("version") == 1 and isinstance(idx.get("packs"), list))
for p in idx["packs"]:
    ok = (re.fullmatch(r"[a-z][a-z0-9-]{1,30}", p["id"]) and re.fullmatch(r"[0-9a-f]{64}", p["sha256"])
          and p["url"] == f"https://github.com/tshi-dev/UrTV/releases/download/photos-{p['id']}/{p['id']}.zip"
          and 1 <= p["count"] <= 200 and 1 <= p["bytes"] <= 80 << 20 and p["name"] and p["name_en"])
    check(f"pack {p['id']}: its index entry passes the client's rules", ok, p)
doc = (ROOT / "docs/PHOTO-PACKS.md").read_text(encoding="utf-8")
check("docs/PHOTO-PACKS.md names every field the index has", all(f"`{k}`" in doc for k in ("id", "name", "name_en", "source", "source_en", "cover", "url", "sha256", "count", "bytes")))
check("docs/SETTINGS.md has the row", "صور شاشة التوقف" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP photo packs: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as t:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", t, str(APP / "PhotoPacks.java"), str(ROOT / "tests/PhotoPacksRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", t, "sa.urtv.screensaver.PhotoPacksRun"], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")
sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
