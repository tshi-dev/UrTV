#!/usr/bin/env python3
"""«الوضع الآمن» and the crash-loop guard (docs/IF-BROKEN.md, docs/PHONE-SETUP.md «If something breaks later»): a second, small Home of the home app in its
own process, at a LOWER HOME priority than MainActivity; the crash-loop counter (tests/CrashLoopRun.java on a plain JVM); the safe screen's buttons wired
to the setup's own undo (setup/RestoreHome, the same as «تراجع»); the phone endpoint; the report link checked against optimize/share.py's allowlist; the
docs and their links. Needs javac for the JVM part (skipped with a note without).   Run: python3 tests/safe_mode_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


# ---- the manifest
A = "{http://schemas.android.com/apk/res/android}"
root = ET.parse(ROOT / "android/launcher/src/main/AndroidManifest.xml").getroot()
app = root.find("application")
acts = {a.get(A + "name"): a for a in app.findall("activity")}
safe, main = acts.get(".safe.SafeActivity"), acts.get(".MainActivity")
check("there is a «الوضع الآمن» activity", safe is not None)


def home_filters(el):
    return [f for f in el.findall("intent-filter") if any(c.get(A + "name") == "android.intent.category.HOME" for c in f.findall("category"))]


def prio(f):
    return int(f.get(A + "priority") or 0)


if safe is not None:
    sf, mf = home_filters(safe), home_filters(main)
    check("it runs in its own process (a crash of the home screen cannot take it down), not the main one or another app's",
          safe.get(A + "process") == ":safe" and main.get(A + "process") is None, safe.get(A + "process"))
    check("it answers HOME (MAIN + HOME + DEFAULT) with one filter", len(sf) == 1 and sorted(c.get(A + "name").rsplit(".", 1)[-1] for c in sf[0].findall("category")) == ["DEFAULT", "HOME"]
          and [a.get(A + "name") for a in sf[0].findall("action")] == ["android.intent.action.MAIN"], sf)
    check("its HOME priority is LOWER than MainActivity's (never picked over it, never a chooser between the two) and above Settings' FallbackHome (-1000)",
          len(mf) == 1 and -1000 < prio(sf[0]) < prio(mf[0]), (prio(sf[0]) if sf else None, prio(mf[0]) if mf else None))
    check("it is not in the apps row (no launcher category), not in recents, has its own task", not any(c.get(A + "name").endswith("LAUNCHER") for f in safe.findall("intent-filter") for c in f.findall("category"))
          and safe.get(A + "excludeFromRecents") == "true" and safe.get(A + "taskAffinity") == "sa.urtv.home.safe" and safe.get(A + "launchMode") == "singleTask")

# ---- the safe screen: light, and its buttons
sa = text(H / "safe/SafeActivity.java")
labels = ["افتح الشاشة الرئيسية من جديد", "كل التطبيقات", "إعدادات التلفزيون", "رجّع واجهة التلفزيون الأصلية", "أرسل تقرير المشكلة"]
check("the five buttons, in Arabic, in this order", all(f'button("{l}"' in sa for l in labels) and [sa.index(f'button("{l}"') for l in labels] == sorted(sa.index(f'button("{l}"') for l in labels))
code = "\n".join(l for l in (sa + text(H / "safe/CrashGuard.java") + text(H / "safe/CrashLoop.java") + text(H / "safe/SafeReport.java")).splitlines() if not l.lstrip().startswith(("*", "/*", "//")))
check("no WebView, no network, no photo, no font file, no keep-awake in the safe screen or what it uses of its own (the code, not its comments)",
      not re.search(r"WebView|android\.webkit|HttpURLConnection|URLConnection|java\.net\.URL\b|Socket|GlassSource|createFromAsset|FLAG_KEEP_SCREEN_ON|Thmanyah", code),
      re.findall(r"WebView|android\.webkit|HttpURLConnection|URLConnection|java\.net\.URL\b|Socket|GlassSource|createFromAsset|FLAG_KEEP_SCREEN_ON|Thmanyah", code))
check("«رجّع واجهة التلفزيون الأصلية» runs the setup's own undo (RestoreHome.run), behind the family PIN, off the screen's thread",
      "RestoreHome.run(SafeActivity.this" in sa and 'SetupGate.guard(this, "الوضع الآمن"' in sa and "new Thread(" in sa[sa.index("void restoreAfterPin"):])
check("with USB debugging off it says how to turn it on with the remote, before anything else",
      sa.index("RestoreHome.debuggingOn(this)") < sa.index("SetupGate.guard(this") and "RestoreHome.HOW_TO_DEBUG" in sa)
rh = text(H / "setup/RestoreHome.java")
check("RestoreHome is the one undo: one session on the TV's own ADB, SetupEngine.undo(), then the key goes unless a switch is pending",
      "SetupSession.open(app, 180_000" in rh and "s.engine.undo()" in rh and "SetupSession.deleteKeyIfFinished(app)" in rh and "Settings.Global.ADB_ENABLED" in rh)
check("…with the remote steps for USB debugging", all(w in rh for w in ("«Build»", "سبع مرات", "«خيارات المطوّر»", "«تصحيح USB»", "«السماح دائماً»")))
su = text(H / "setup/SetupActivity.java")
check("«تراجع» in «إعداد تلفزيوني» goes through the same RestoreHome.run (one path for the setup screen, the safe mode and the phone)",
      "RestoreHome.run(SetupActivity.this" in su and "e.undo()" not in su)
pa = text(H / "phone/PhoneActions.java")
check("the phone's «رجّع واجهة التلفزيون الأصلية» starts the same RestoreHome.run, and says when debugging is off",
      "RestoreHome.run(app, null)" in pa and 'Result.fail("debug_off")' in pa and 'Result.ok("started")' in pa)
api = text(H / "phone/PhoneApi.java")
check("…behind the family PIN, like every change from the phone", '|| endpoint.equals("restorehome")' in api and 'case "/api/restore_home": endpoint = "restorehome"' in api)
check("«كل التطبيقات» lists TV and phone launcher entries, not our own package", "CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER" in sa and "p.equals(getPackageName())" in sa)
check("«إعدادات التلفزيون» opens Android's settings", "Settings.ACTION_SETTINGS" in sa)
check("«أرسل تقرير المشكلة» draws the docs/share link with the TV's own QR encoder", "QrCode.encode(url)" in sa and "SafeReport.url(" in sa)
check("Home that reaches it with nothing wrong is passed on to the home screen; a loop keeps it", "if (byHome && !asked && !loop.tripped())" in sa and "openHome(false)" in sa)
check("«افتح الشاشة الرئيسية من جديد» clears the count and opens MainActivity once without the redirect",
      "if (fresh) CrashGuard.clear(this);" in sa and "CrashGuard.FROM_SAFE" in sa and '".MainActivity"' in sa)

# ---- the guard in MainActivity
ma = text(H / "MainActivity.java")
oc = ma[ma.index("protected void onCreate(Bundle b) {"):]
first = [l.strip() for l in oc.splitlines()[1:5] if l.strip() and not l.strip().startswith("//")]
check("MainActivity: the redirect is the first thing after super.onCreate, then the recorders, before anything that could crash",
      first[:3] == ["super.onCreate(b);", "if (sa.urtv.home.safe.CrashGuard.redirect(this, getIntent())) { redirected = true; return; }", "sa.urtv.home.safe.CrashGuard.arm(this);   // from here a crash or a hang of this screen is counted"], first)
check("…the stable minute is started at the end of onCreate, the front state follows onResume/onPause, onNewIntent redirects too",
      "CrashGuard.started(this, handler)" in ma and "CrashGuard.resumed(true)" in ma and "CrashGuard.resumed(false)" in ma and "CrashGuard.redirect(this, intent)" in ma)
check("…a redirected MainActivity unregisters nothing it never registered", "if (redirected) { super.onDestroy(); return; }" in ma)
cg = text(H / "safe/CrashGuard.java")
check("the guard never throws into the home screen, and keeps Android's own crash handling after writing", cg.count("catch (RuntimeException") >= 4 and "before.uncaughtException(t, e)" in cg)
check("the file is written whole (a temporary file, then a rename) and read in both processes from the same place",
      'DIR = "safe", FILE = "crash-loop.txt"' in cg and "renameTo(f)" in cg and "getFilesDir()" in cg and "CrashGuard.read(this)" in sa)

# ---- the JVM part: the counter and the report
share = text(ROOT / "optimize/share.py")
m = re.search(r'ALLOWED_LINE = re\.compile\(\n((?:\s+r".*"\)?\n)+)', share)
allowed = re.compile("".join(re.findall(r'r"(.*?)"', m.group(1)))) if m else None
check("share.py's allowlist was found", allowed is not None)
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "safe/CrashLoop.java"), str(H / "safe/SafeReport.java"), str(ROOT / "tests/CrashLoopRun.java")], capture_output=True, text=True)
        check("CrashLoop and SafeReport compile with no Android", r.returncode == 0, r.stderr)
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.safe.CrashLoopRun"], capture_output=True, text=True, encoding="utf-8").stdout if r.returncode == 0 else ""
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every CrashLoop case ran", "ALL PASS" in out, out[-300:])
    lines = [l[5:] for l in out.splitlines() if l.startswith("LINE ")]
    url = next((l[4:] for l in out.splitlines() if l.startswith("URL ")), "")
    check("every line of the safe mode's report is one of share.py's fields (its own ALLOWED_LINE)", lines and allowed and all(allowed.match(l) for l in lines), [l for l in lines if not (allowed and allowed.match(l))])
    page = re.search(r'^PAGE = "(.*)"', share, re.M).group(1)
    check("the link is share.py's page with the report (its first lines, as many as the QR code holds) after the #, quoted the way share.py quotes it",
          any(url == page + "#" + urllib.parse.quote("\n".join(lines[:n]) + "\n") for n in range(2, len(lines) + 1)), url)
else:
    print("SKIP the JVM part: no javac")

# ---- the docs
ib = ROOT / "docs/IF-BROKEN.md"
check("docs/IF-BROKEN.md exists", ib.is_file())
if ib.is_file():
    d = text(ib)
    for what, words in [("the black screen: wait 5 minutes (the safety switch), then «الوضع الآمن»", ["5 دقائق", "مفتاح الأمان", "«الوضع الآمن»"]),
                        ("the remote does nothing: unplug for 30 seconds", ["الريموت ما يسوي شي", "30 ثانية"]),
                        ("from the safe mode: restore the original home", ["«رجّع واجهة التلفزيون الأصلية»", "«السماح دائماً»"]),
                        ("from the phone page", ["من الجوال", "/api/restore_home"]),
                        ("from a computer", ["optimize/restore-home.sh"]),
                        ("the last resort and what it erases", ["إعادة ضبط المصنع", "تمسح كل شيء"]),
                        ("where to report: the QR code or a GitHub issue", ["«أرسل تقرير المشكلة»", "github.com/tshi-dev/UrTV/issues"])]:
        check("IF-BROKEN.md: " + what, all(w in d for w in words), [w for w in words if w not in d])
    check("IF-BROKEN.md is in Arabic and short", len(re.findall(r"[؀-ۿ]", d)) > 600 and len(d.splitlines()) < 90, len(d.splitlines()))
    ok = [ (p, "IF-BROKEN.md") for p in ("README.md", "docs/PHONE-SETUP.md", "docs/FEATURES.md") if "IF-BROKEN.md" in text(ROOT / p)]
    check("it is linked from README, PHONE-SETUP.md and FEATURES.md", len(ok) == 3, ok)
    check("README links it in both languages", text(ROOT / "README.md").count("docs/IF-BROKEN.md") >= 2)
    ps = text(ROOT / "docs/PHONE-SETUP.md")
    check("PHONE-SETUP.md has the anchor IF-BROKEN.md points to, with what Android does and the TV test", "### If something breaks later: the safe mode" in ps
          and "#if-something-breaks-later-the-safe-mode" in d and "| SM1 |" in ps and "am crash sa.urtv.home" in ps)
check("the done screen of «إعداد تلفزيوني» shows «لو صار شي», the same steps very short", '"لو صار شي:' in su and su.count("IF_BROKEN") >= 2
      and all(w in su[su.index("IF_BROKEN ="):su.index("IF_BROKEN =") + 1200] for w in ("5 دقائق", "«الوضع الآمن»", "30 ثانية", "optimize/restore-home.sh", "إعادة ضبط المصنع", "github.com/tshi-dev/UrTV/issues")))

sys.exit(1 if failed else 0)
