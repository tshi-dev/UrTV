#!/usr/bin/env python3
"""The home's cards on the newest page: the launcher carried the page of its own build for good, while the screensaver downloads the
newest one, so a page fix (the markets' requests among them) reached the screensaver and never the home, whose stocks card stayed at its
last good prices (Xiaomi MiTV, Android 11). Checked from the source (the wiring needs Android):
 1. the screensaver shares only its downloaded page, only tv.html / tv-en.html, read-only, only when it is newer than its APK's own and
    its automatic update is on, behind the provider's read permission (the launcher has it, signature);
 2. the launcher opens it only when its build stamp is newer than its own page's, from a copy named by that stamp, checked after the copy;
    on any failure its own page exactly as before; it stays a file:// page (the data APIs need file:// universal access);
 3. PageStamp is the same file in both apps but its package.
Run: python3 tests/home_page_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

prov = (APP / "ConfigProvider.java").read_text(encoding="utf-8")
page = (HOME / "HomePage.java").read_text(encoding="utf-8")
card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
man = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

of = prov.split("public ParcelFileDescriptor openFile(")[1].split("\n    }\n")[0]
check("1. the provider's page is read-only and only through sharedPage", '"page".equals(seg.get(0)) && "r".equals(mode)' in of
      and "sharedPage(c, seg.get(1))" in of and "MODE_READ_ONLY" in of.split('"page"')[1].split("}")[0] + of.split('"page"')[1].split("}")[1])
sp = prov.split("static File sharedPage(")[1].split("\n    }\n")[0]
check("1. only tv.html / tv-en.html, any other name is nothing", '"tv-en.html".equals(name) ? Tr.EN : "tv.html".equals(name) ? Tr.AR : null' in sp and "lang == null" in sp)
check("1. only with the automatic update on, and only a downloaded page newer than the APK's own",
      "new Prefs(c).autoUpdate()" in sp and "PageStamp.newer(Updater.fileStamp(f), Updater.assetStamp(c, lang))" in sp and "Updater.pageFile(c, lang)" in sp)
check("1. the provider is behind its signature read permission", 'android:readPermission="sa.urtv.screensaver.READ_CONFIG"' in man)

check("2. the home card opens HomePage.fresh of the url it chose", "return withGlass(HomePage.fresh(activity, HomeCardChoice.liveUrl(choice)));" in card)
cp = page.split("private static File copy(")[1]
check("2. newer than the launcher's own page by build stamp, else its own (and old copies go)",
      'PageStamp.read(c.getAssets().open("web/" + name))' in cp and "if (!PageStamp.newer(theirs, own)) { clean(dir, prefix, null); return null; }" in cp)
check("2. the copy is named by its stamp, written to a temp file, checked, then renamed",
      'new File(dir, prefix + theirs + ".html")' in cp and "theirs.equals(fileStamp(tmp))" in cp and "tmp.renameTo(want)" in cp)
check("2. the two languages' copies never clean each other (tv_ and tv-en_)", 'name.replace(".html", "") + "_"' in page)
fr = page.split("static String fresh(")[1].split("\n    }\n")[0]
check("2. any failure is the launcher's own page", "catch (RuntimeException e) {\n            return url;" in fr and "if (copy == null) return url;" in fr)
check("2. still a file:// page with the same fragment", "Uri.fromFile(copy) + \"#\" + url.substring(base.length())" in fr)
check("2. the WebView keeps file:// universal access (Yahoo has no CORS headers)", "s.setAllowUniversalAccessFromFileURLs(true);" in card)
strip = lambda t: re.sub(r"^package [\w.]+;", "", re.sub(r"/\*\*.*?\*/", "", t, count=1, flags=re.S), flags=re.M)
check("3. PageStamp is the same in both apps but its package",
      strip((APP / "PageStamp.java").read_text(encoding="utf-8")) == strip((HOME / "PageStamp.java").read_text(encoding="utf-8")))
check("tests/run.sh runs this test", "tests/home_page_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

print("\n" + (f"{failed} failed" if failed else "home page: all passed"))
raise SystemExit(1 if failed else 0)
