#!/usr/bin/env python3
"""Regression gate: guards for fixed bugs that had none (docs/dev/REGRESSIONS.md names each row here). Source rules over the code that holds
each fix, and one scripted run of optimize/keep.sh in a copy of the tree (the real optimize/run/ is never touched).
Run: python3 tests/regressions/legacy_guards_test.py"""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

# GitHub #1 (4.0): the store, TV Assistant and the maker's settings were missing from «كل التطبيقات»: only LEANBACK_LAUNCHER was listed
dock = text(HOME / "Dock.java")
check("#1 the app scan asks both LEANBACK_LAUNCHER and LAUNCHER, TV activity first",
      "new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}" in dock)
check("#1 …and one app found by both is listed once", "out.containsKey(p)" in dock)
man = text(ROOT / "android/launcher/src/main/AndroidManifest.xml")
q = man.split("<queries>")[1].split("</queries>")[0] if "<queries>" in man else ""
check("#1 the manifest may see LAUNCHER apps (package visibility, target 30+)", "android.intent.category.LAUNCHER" in q or "QUERY_ALL_PACKAGES" in man, q[:200])

# 4.3.2: deleting UrTV after «تخلّص من واجهة المصنع» left a TV with no home (black screen)
st = text(SAVER / "SettingsActivity.java")
check("4.3.2 the settings warn «قبل ما تحذف التطبيق…» and offer «رجّع واجهة المصنع»",
      'row("قبل ما تحذف التطبيق…"' in st and "رجّع واجهة المصنع" in st and 'setTitle("قبل ما تحذف التطب' in st)
check("4.3.2 …and README says it too", "واجهة المصنع" in text(ROOT / "README.md"))

# 4.3.2: the matches page's source line touched its title
tpl = text(ROOT / "src/template.html")
hdr = re.search(r"#mp header\{([^}]*)\}", tpl)
check("4.3.2 the matches page header keeps a gap and wraps (the source line never touches the title)",
      hdr and "gap:" in hdr.group(1) and "flex-wrap:wrap" in hdr.group(1), hdr and hdr.group(1))

# GitHub #35 (4.0): the Quran and the adhan stuttered or played over other sound: no audio focus
ms = text(HOME / "tilawah/MusicService.java")
fc = ms.split("public void onAudioFocusChange(int change) {")[1].split("\n    }\n")[0] if "public void onAudioFocusChange(int change) {" in ms else ""
check("#35 playback asks for audio focus and gives it back", "requestAudioFocus(" in ms and "abandonAudioFocusRequest(" in ms)
# 4.3: the cases in onAudioFocusChange itself; 4.4: decided by FocusRule (tests/focus_rule_test.py runs it), the service acting on its answer
old_form = ("AUDIOFOCUS_LOSS" in fc and "AUDIOFOCUS_GAIN" in fc and "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK" in fc)
new_form = ("FocusRule.on(change" in fc and "case PAUSE_UNTIL_GAIN" in fc and "case DUCK" in fc and "case GAIN" in fc
            and (ROOT / "tests/focus_rule_test.py").is_file())
check("#35 a loss pauses (and remembers to resume), a gain resumes, a duck lowers the volume",
      (old_form or new_form) and "player.pause()" in fc and "resumeOnFocus = true" in fc and "player.start()" in fc and "setVolume(" in fc)

# GitHub #4 (4.0): the setup stopped at a package the TV itself ships off (com.tcl.smartalexa) with «!! still off»
with tempfile.TemporaryDirectory() as tmp:
    t = Path(tmp)
    (t / "optimize/lists").mkdir(parents=True)
    (t / "optimize/run").mkdir()
    shutil.copy(ROOT / "optimize/keep.sh", t / "optimize/keep.sh")
    for f in (ROOT / "optimize/lists").glob("*.txt"):
        shutil.copy(f, t / "optimize/lists" / f.name)
    for f in ("offlist.py",):
        if (ROOT / "optimize" / f).exists():
            shutil.copy(ROOT / "optimize" / f, t / "optimize" / f)
    pkg = "com.tcl.smartalexa"
    (t / "optimize/run/backup.txt").write_text(f"== disabled packages\npackage:{pkg}\n== other\n", encoding="utf-8")
    fake = t / "fakepy"
    fake.write_text('#!/usr/bin/env bash\nif [ "${3:-}" = "--batch" ] || [ "${2:-}" = "--batch" ]; then cat > /dev/null; exit 0; fi\n'
                    f'case "$3" in "pm list packages -d") echo "package:{pkg}" ;; *) ;; esac\n', encoding="utf-8")
    fake.chmod(0o755)
    r = subprocess.run(["bash", str(t / "optimize/keep.sh")], capture_output=True, text=True, cwd=tmp, env={**os.environ, "PY": str(fake)})
    out = r.stdout + r.stderr
    check("#4 a kept package the TV had off before UrTV (backup.txt) and won't turn on: «the TV's own, not a failure» (R-380 wording), no !!, exit 0",
          pkg in "".join(text(ROOT / "optimize/lists/keep.txt").split()) and r.returncode == 0 and "the TV's own, not a failure" in out and "!!" not in out,
          f"rc={r.returncode} {out}")
sys.exit(1 if failed else 0)
