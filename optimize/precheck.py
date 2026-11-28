#!/usr/bin/env python3
"""Is this device an Android TV, and is its WebView new enough? Reads the TV's answers (the output of
optimize/precheck.sh: `tv.py --batch` blocks) and prints what to tell the person. Standard library only.

    precheck.py check < answers     the full check: prints lines, exits 0 (go on), 3 (not an Android TV: stop), 5 (no answer: stop),
                                    or 6 (Android older than 8: stop; nothing was changed)
    precheck.py report < answers    the two lines for report.md: «Device: …» and «WebView: …»
    precheck.py held < answers      exit 4 when the WebView is known and older than 80 (the TV's own screensaver stays
                                    selected until it is updated), else 0; prints nothing

The screensaver page's CSS (clamp, gap, inset) needs Chrome/WebView 80 or newer. One report had «system WebView is 66» and
the page never ran. The page is not rewritten for old WebViews: the person updates «Android System WebView» instead.
"""
import re
import sys

MIN_WEBVIEW = 80
MIN_SDK = 26   # the home app and the add-on need Android 8 (minSdk 26); 4.4: refused here, before anything is changed, not at the install

NOT_TV_AR = ("هذا الجهاز مو تلفزيون أندرويد (Android TV أو Google TV)، فتلفزيونك ما يشتغل عليه. تلفزيونات سامسونج (Tizen) "
             "وإل جي (webOS) ما تقدر تشغّل هالتطبيقات، والجوال أو التابلت مو تلفزيون. إذا عندك تلفزيون أو جهاز أندرويد تي في "
             "ثاني (ميبوكس، شاومي ستيك، كروم كاست مع Google TV…) وصّله على نفس الشبكة وجرّب عليه.")
WEBVIEW_AR = ("نسخة «Android System WebView» في تلفزيونك قديمة ({v})، وشاشة التوقف تحتاج ٨٠ أو أحدث وإلا ما تشتغل. "
              "افتح متجر Play في التلفزيون، دوّر على «Android System WebView» واضغط تحديث. وإذا تبي أفتح لك صفحته بنفسي أقدر.")
OLD_AR = ("نسخة أندرويد في هذا التلفزيون قديمة ({v})، وتلفزيونك يحتاج أندرويد ٨ أو أحدث. ما غيّرنا شي في التلفزيون. "
          "إذا فيه تحديث للنظام من الإعدادات ← حول ← تحديث النظام، حدّثه وجرّب مرة ثانية، أو جرّب على جهاز أندرويد تي في أحدث.")
FIRE_AR = ("هذا جهاز Fire TV من أمازون: ممكن يشتغل جزئياً، وغالباً ما يسمح بتغيير الشاشة الرئيسية (بنكمل ونقول لك وش صار).")


def blocks(text):
    """`tv.py --batch` output -> {command: output}."""
    out, cur = {}, None
    for line in text.replace("\r", "").splitlines():
        if line.startswith("$ "):
            cur = line[2:]
            out[cur] = []
        elif cur is not None:
            out[cur].append(line)
    return {k: "\n".join(v).strip() for k, v in out.items()}


def find(answers, start):
    for cmd, out in answers.items():
        if cmd.startswith(start):
            return out
    return ""


def major(version):
    m = re.match(r"\s*(\d+)", version or "")
    return int(m.group(1)) if m else None


def webview(answers):
    """(package, major version) of the WebView in use, or (None, None) when the TV does not say."""
    upd = find(answers, "dumpsys webviewupdate")
    m = re.search(r"Current WebView package \(name, version\): \(([\w.]+), ([\w.\-]+)\)", upd)
    if m and major(m.group(2)):
        return m.group(1), major(m.group(2))
    rows = {}   # "package versionName=…" lines of the packages that have 'webview' in their name
    for line in find(answers, "for p in").splitlines():
        m = re.match(r"([\w.]+)\s+versionName=(\S+)", line.strip())
        if m and major(m.group(2)):
            rows[m.group(1)] = major(m.group(2))
    for pkg in ("com.google.android.webview", "com.android.webview"):
        if pkg in rows:
            return pkg, rows[pkg]
    return next(iter(rows.items())) if rows else (None, None)


def kind(answers):
    """«tv», «fire» or «no»: leanback is what makes an Android TV; Amazon adds its own feature."""
    feats = find(answers, "pm list features")
    maker = find(answers, "getprop ro.product.manufacturer").lower()
    leanback = bool(re.search(r"^feature:android\.software\.leanback(_only)?\b", feats, re.M))
    if "amazon.hardware.fire_tv" in feats or (maker == "amazon" and leanback):
        return "fire"
    return "tv" if leanback else "no"


def lines(answers):
    k = kind(answers)
    pkg, ver = webview(answers)
    wv = "unknown (the TV does not say)" if ver is None else str(ver)
    out = [("Device: " + {"tv": "Android TV / Google TV", "fire": "Fire TV (Amazon): may work partly, the home switch may be refused",
                         "no": "!! not an Android TV (no android.software.leanback)"}[k]),
           f"WebView: {wv}" + (f" ({pkg})" if pkg else "")]
    if ver is not None and ver < MIN_WEBVIEW:
        out[1] = (f"!! WebView: system WebView is {ver} ({pkg}): the screensaver page needs {MIN_WEBVIEW} or newer (its CSS needs clamp, gap and inset) and "
                  "would not run. Update «Android System WebView» from the Play Store")
    return out


def check(answers):
    k = kind(answers)
    _, ver = webview(answers)
    if k == "no" and not re.search(r"^feature:", find(answers, "pm list features"), re.M):
        # no feature line at all: the TV did not answer (still booting?), which is not «this is a phone»
        print("!! The TV did not say what it is (its feature list came back empty), so nothing was changed. Wait a minute, check it is awake and on Wi-Fi, then run optimize/precheck.sh again.")
        return 5
    if k == "no":
        print("!! This device is not an Android TV (it has no android.software.leanback): the setup stops here, nothing was changed.")
        print("   Samsung (Tizen) and LG (webOS) TVs can't run these apps, and a phone or tablet is not a TV. Tell the person, in Arabic:")
        print("   «" + NOT_TV_AR + "»")
        return 3
    sdk = find(answers, "getprop ro.build.version.sdk").strip()
    if sdk.isdigit() and int(sdk) < MIN_SDK:
        release = find(answers, "getprop ro.build.version.release").strip() or "?"
        print(f"!! This TV runs Android {release} (SDK {sdk}): UrTV's home needs Android 8 (SDK {MIN_SDK}) or newer, so the setup stops here, "
              "nothing was changed. Tell the person, in Arabic:")
        print("   «" + OLD_AR.format(v=release) + "»")
        return 6
    print("Device: " + ("Fire TV (Amazon)" if k == "fire" else "Android TV / Google TV"))
    if k == "fire":
        print("   Fire TV may work partly, and its home switch may be refused (then SKIP_LAUNCHER=1 optimize/run.sh). Tell the person, in Arabic:")
        print("   «" + FIRE_AR + "»")
    if ver is None:
        print("WebView: the TV does not say which version it has; going on.")
    elif ver < MIN_WEBVIEW:
        print(lines(answers)[1])
        print("   Tell the person, in Arabic: «" + WEBVIEW_AR.format(v=ver) + "»")
        print("   `optimize/precheck.sh webview-store` opens its Play Store page on the TV; after the update run `optimize/precheck.sh` again.")
        print("   The setup goes on meanwhile, but the TV keeps its own screensaver selected (ours is installed, not switched on) "
              "until the WebView is new enough; the report keeps this line until then.")
    else:
        print(f"WebView: {ver} (needs {MIN_WEBVIEW} or newer): ok")
    return 0


def main(argv):
    text = sys.stdin.read()
    answers = blocks(text)
    if argv[:1] == ["report"]:
        print("\n".join(lines(answers)))
        return 0
    if argv[:1] == ["held"]:
        _, ver = webview(answers)
        if ver is None:   # the TV did not say which WebView it has: unknown is not «new enough»
            return 5
        return 4 if ver < MIN_WEBVIEW else 0
    return check(answers)


if __name__ == "__main__":
    for _s in (sys.stdout, sys.stderr):
        if hasattr(_s, "reconfigure"):
            _s.reconfigure(encoding="utf-8", errors="replace", newline="\n")
    sys.exit(main(sys.argv[1:]))
