#!/usr/bin/env python3
"""The «تلفزيونك» tile for a TV that keeps its own home (setupwraith / launcherx): an activity-alias with LEANBACK_LAUNCHER and our banner that opens
MainActivity by component. It must never change HOME handling (no HOME category on it, MainActivity's HOME entry stays the only one), Back must
leave our home when it is not the default one, our own «كل التطبيقات» must not list us, the other launcher entries stay, and the family lock is not touched.
Needs javac for the Java cases. Run: python3 tests/home_tile_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


H = "android/launcher/src/main/java/sa/urtv/home/"
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / H / "HomeBack.java"), str(ROOT / "tests/HomeBackRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.HomeBackRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every HomeBack case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")

A = "{http://schemas.android.com/apk/res/android}"
root = ET.parse(ROOT / "android/launcher/src/main/AndroidManifest.xml").getroot()
app = root.find("application")


def filters(el):
    out = []
    for f in el.findall("intent-filter"):
        out.append((sorted(a.get(A + "name") for a in f.findall("action")), sorted(c.get(A + "name").rsplit(".", 1)[-1] for c in f.findall("category"))))
    return out


alias = [a for a in app.findall("activity-alias") if a.get(A + "name") == ".HomeTile"]
check("there is one activity-alias .HomeTile", len(alias) == 1)
al = alias[0]
check("it targets MainActivity, is exported, is called «تلفزيونك» and has our banner and icon",
      al.get(A + "targetActivity") == ".MainActivity" and al.get(A + "exported") == "true" and al.get(A + "label") == "تلفزيونك"
      and al.get(A + "banner") == "@drawable/banner" and al.get(A + "icon") == "@mipmap/ic_launcher")
check("its one filter is MAIN + LEANBACK_LAUNCHER and nothing else (no HOME, so it cannot become or compete as a home)",
      filters(al) == [(["android.intent.action.MAIN"], ["LEANBACK_LAUNCHER"])], filters(al))
main = [a for a in app.findall("activity") if a.get(A + "name") == ".MainActivity"][0]
check("MainActivity keeps its one HOME + DEFAULT filter and gained no launcher category", filters(main) == [(["android.intent.action.MAIN"], ["DEFAULT", "HOME"])], filters(main))
homes = [c for c in root.iter("category") if c.get(A + "name") == "android.intent.category.HOME" and c in [x for f in root.iter("intent-filter") for x in f]]
check("HOME is declared once in the manifest (the queries block is a query, not a filter)", len(homes) == 1, len(homes))
others = sorted(a.get(A + "name") for a in app.findall("activity") if any("LEANBACK_LAUNCHER" in cats or "LAUNCHER" in cats for _, cats in filters(a)))
check("the other launcher entries are as they were: the setup screen only", others == [".setup.SetupActivity"], others)
check("the banner art exists", (ROOT / "android/launcher/src/main/res/drawable-xhdpi/banner.png").is_file())

dock = text(H + "Dock.java")
check("our own «كل التطبيقات» skips our package, so the tile is not listed (nor the setup screen)", "if (p.equals(c.getPackageName()) || out.containsKey(p)" in dock)
ma = text(H + "MainActivity.java")
back = ma[ma.index("public void onBackPressed()"):ma.index("// ---------------------------------------------------------------- layout")]
check("Back: the grid's Back to the dock as before; at the top it leaves when this app is not the default home",
      "dockCards.get(0).requestFocus();" in back and "else if (HomeBack.leaves(isDefaultHome(), false)) finish();" in back
      and 'new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY' in back and "HomeBack.isDefaultHome(" in back)
check("nothing in the code hands the home role to us or opens the HOME intent when opened from the tile",
      not re.search(r"RoleManager|ROLE_HOME|ACTION_SET_DEFAULT", "".join(f.read_text(encoding="utf-8") for f in (ROOT / H).rglob("*.java"))))
check("the family lock and kids mode are not touched by the tile", "HomeTile" not in text(H + "Parental.java") + text(H + "ParentalPolicy.java") + text(H + "PhoneApps.java"))
tilawah = text("android/launcher/src/main/AndroidManifest.xml")
check("the tilawah entries are still there", ".tilawah." in tilawah or "tilawah" in tilawah)
f = text("docs/FEATURES.md")
check("FEATURES says how to open UrTV when the TV keeps its own home: the «تلفزيونك» tile, Back and Home return to the stock home", "مربع **«تلفزيونك»**" in f and "**«رجوع»** يرجعك للشاشة الأصلية" in f)
check("the CHANGELOG says so", "A tile «تلفزيونك»" in text("CHANGELOG.md"))
print("ALL PASS" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
