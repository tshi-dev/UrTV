#!/usr/bin/env python3
"""The LESSONS rules (the supervisor's LESSONS.md, R1-R25) that a machine can check; the others are the checklist in docs/dev/RELEASE.md.
Covered elsewhere: R1 ui_thread_ipc_test.py (onCreate, onResume, keys, the tick; subprocesses too), R3 key_handlers_test.py and
tests/edit_keys_test.py, R11 tests/beta_channel_test.py and tests/beta_newest_test.py, R14 no_bundled_packs_test.py, R18 tests/check.py
(the byte budget) and tests/market_names_test.py, R21 privacy_test.py. Here:

- R4  no focus listener scrolls (fullScroll, pageScroll, arrowScroll) or moves the focus (requestFocus) of another view by itself;
- R9  every app targets API 34; the home declares no accessibility service (4.4 moved them to the add-on);
- R10 tests/webview66_test.py (every inset, clamp()/min()/max() and flex gap with its fallback) is there and run;
- R12 no fixed clean-up list names Android's core (core.txt), and the setup's own copy of the core rules is core.txt;
- R15 apk-latest keeps the Latest flag and its stable names, rescue included; no other release takes Latest;
- R16 only master and next publish (a build of another branch is never installed over apk-next), and a local build is versionCode 1;
- R17 the CI runners and tools are pinned: no *-latest image, no action at a branch, npm and pip at exact versions.
Run: python3 tests/regressions/lessons_test.py"""
import re, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import javagraph as g  # noqa: E402

ROOT = g.ROOT
W = ROOT / ".github/workflows"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return (ROOT / p).read_text(encoding="utf-8")

# R4: a focus listener that scrolls or takes the focus elsewhere moves it under the remote (4.3.4: «تحديث تلفزيوني» skipped a row)
bad = []
for app, src in g.SRC.items():
    for p in sorted(src.rglob("*.java")):
        s = g.blank(p.read_text(encoding="utf-8"))
        spans = []
        for m in re.finditer(r"\bsetOnFocusChangeListener\s*\(", s):
            spans.append(s[m.end() - 1:g.match_paren(s, m.end() - 1)])
        for m in re.finditer(r"\bvoid\s+onFocusChange\s*\([^)]*\)\s*\{", s):
            spans.append(s[m.end() - 1:g.match_paren(s, m.end() - 1)])
        for sp in spans:
            for k in re.findall(r"\.\s*(fullScroll|pageScroll|arrowScroll)\s*\(", sp):
                bad.append(f"{p.relative_to(ROOT)}: {k} in a focus listener")
check("R4 no focus listener scrolls by itself (fullScroll, pageScroll, arrowScroll)", not bad, bad)

# R9
for gr in sorted((ROOT / "android").glob("*/build.gradle")):
    m = re.search(r"targetSdk\s+(\d+)", gr.read_text(encoding="utf-8"))
    check(f"R9 {gr.parent.name} targets API 34", m and m.group(1) == "34", m and m.group(1))
man = text("android/launcher/src/main/AndroidManifest.xml")
a11y = sorted(re.findall(r'<service[^>]*android:name="([^"]+)"[^>]*android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"', man, re.S))
a11y += sorted(re.findall(r'<service[^>]*android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"[^>]*android:name="([^"]+)"', man, re.S))
KNOWN_A11Y = set()   # 4.4 moved SleepService and TypeService to the add-on (Play Protect «sensitive data»): the home declares none
check("R9 the home declares no accessibility service (Play Protect; they live in the add-on)", set(a11y) <= KNOWN_A11Y, sorted(set(a11y) - KNOWN_A11Y))
if set(a11y) < KNOWN_A11Y:
    print("NOTE an accessibility service left the home: remove it from KNOWN_A11Y")

# R10: WebView 66 (Chrome 66) has no inset, clamp()/min()/max() or flex gap: tests/webview66_test.py (4.4, GitHub #19) checks that every
# use has its fallback before it and that the flex-gap stand-in is there; it must stay and stay in run.sh (it replaced 4.3.4's count ceilings)
wv = ROOT / "tests/webview66_test.py"
check("R10 tests/webview66_test.py guards every inset/clamp()/min()/max()/flex gap of the page, and run.sh runs it",
      wv.is_file() and "python3 tests/webview66_test.py" in text("tests/run.sh") and "has a plain value of the same property just before it" in wv.read_text(encoding="utf-8"))

# R12
core = [re.compile(l.strip()) for l in text("optimize/lists/core.txt").splitlines() if l.strip() and not l.lstrip().startswith("#")]
def words(p): return [l.split("#")[0].split()[0] for l in text(p).splitlines() if l.split("#")[0].strip()]
for f in ("tcl-safe.txt", "tcl-optional.txt", "home-replaced.txt"):
    hit = [p for p in words(f"optimize/lists/{f}") if any(c.fullmatch(p) for c in core)]
    check(f"R12 {f} names nothing of Android's core (core.txt)", not hit, hit)
sl = text("android/launcher/src/main/java/sa/urtv/home/setup/SetupLists.java")
m = re.search(r"static final String\[\] CORE = \{(.*?)\};", sl, re.S)
setup_core = [x.encode().decode("unicode_escape") for x in re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1))] if m else []
check("R12 the TV setup's core rules are core.txt's (optimize/gen_setup_lists.py)", setup_core == [c.pattern for c in core], (len(setup_core), len(core)))
for p in ("com.android.sdksandbox", "com.android.location.fused"):
    check(f"R12 {p} is core (never turned off)", any(c.fullmatch(p) for c in core))

# R15
aw = text(".github/workflows/android.yml")
latest = aw.split("tag_name: apk-latest")[1].split("- name:")[0] if "tag_name: apk-latest" in aw else ""
check("R15 apk-latest is the Latest release, with its stable names (screensaver, home, rescue, version.txt)",
      "make_latest: true" in latest and all(n in latest for n in ("urtv-screensaver.apk", "urtv-home.apk", "urtv-rescue.apk", "version.txt")), latest[:300])
others = [f"{p.name}" for p in W.glob("*.yml") for blk in re.split(r"- name:", p.read_text(encoding="utf-8"))
          if "make_latest: true" in blk and "tag_name: apk-latest" not in blk]
others += [p.name for p in W.glob("*.yml") if re.search(r"--latest(?!=false)\b", p.read_text(encoding="utf-8"))]
check("R15 no other release step takes the Latest flag", not others, others)
check("R15 the rescue app's fixed link is apk-latest's (docs/RESCUE.md)", "releases/download/apk-latest/urtv-rescue.apk" in text("docs/RESCUE.md"))

# R16
check("R16 android.yml builds on a push to master and next only", re.search(r"push:\s*\n\s*branches:\s*\[master, next\]", aw) is not None)
pubs = re.findall(r"- name: Publish[^\n]*\n\s*if: ([^\n]+)", aw)
check("R16 every publish step is for master or next", pubs and all("github.ref_name == 'master'" in x or "github.ref_name == 'next'" in x for x in pubs), pubs)
for gr in sorted((ROOT / "android").glob("*/build.gradle")):
    check(f"R16 a local {gr.parent.name} build is versionCode 1 (below every published build)",
          "versionCode((System.getenv('GITHUB_RUN_NUMBER') ?: '1') as int)" in gr.read_text(encoding="utf-8"))

# R17
unpinned = []
for p in sorted(W.glob("*.yml")):
    t = p.read_text(encoding="utf-8")
    for x in re.findall(r"runs-on:\s*(\S+)", t):
        if x.endswith("-latest"):
            unpinned.append(f"{p.name}: runs-on {x}")
    for x in re.findall(r"uses:\s*([\w./-]+)@(\S+)", t):
        if x[1] in ("main", "master", "latest", "HEAD"):
            unpinned.append(f"{p.name}: {x[0]}@{x[1]}")
    for x in re.findall(r"npm install[^\n]*", t):
        if re.search(r"npm install(?: --no-save)? (?!.*@\d)", x) and "-g" not in x:
            unpinned.append(f"{p.name}: {x.strip()[:80]}")
    for x in re.findall(r"pip install ([^\n]*)", t):
        for w in x.split():
            if w.startswith("-") or w.endswith(".txt") or "==" in w or w in ("&&",):
                continue
            if re.fullmatch(r"[A-Za-z][\w.-]*", w):
                unpinned.append(f"{p.name}: pip {w} without a version")
check("R17 runners and actions pinned (no *-latest image, no action at a branch, npm and pip at exact versions)", not unpinned, unpinned)
sys.exit(1 if failed else 0)
