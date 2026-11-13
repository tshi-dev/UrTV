#!/usr/bin/env python3
"""Three owners' options, each off by default (today's behaviour unchanged):
1. «الآية والحديث» (VerseKind.java): the verse card with both (default), verses only or hadith only; #versekind= only when chosen.
2. «إظهار الثواني»: the seconds on the screensaver's clock; #seconds=1 only while on.
3. «الخروج من تلفزيونك» (HomeExit.java in the screensaver, BackExit.java in the home): one Back (default), a long Back or three Backs leave
   our home where the TV keeps its own; #backexit= only when chosen.
Read from the source (the prefs, the fragment, the receiver, the rows, the home's key path, the page), then `optimize/saver.sh` against a
stand-in for tv.py, and the JVM parts (needs javac; without it, skipped with a note). The page itself: tests/page.test.mjs.
Run: python3 tests/user_options_test.py"""
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
receiver = (APP / "SaverReceiver.java").read_text(encoding="utf-8")
st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
hub = (APP / "SettingsHub.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
settings_md = (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8")
render = st[st.index("    private void render() {"):].split("\n    }\n")[0]

# the defaults: nothing changes for anyone
check("defaults: both, off, one press", 'VerseKind.read(sp.getString("verseKind", VerseKind.BOTH))' in prefs
      and 'sp.getBoolean("seconds", false)' in prefs and 'HomeExit.read(sp.getString("homeExit", HomeExit.PRESS))' in prefs)
check("the fragment gets each only when chosen", "+ VerseKind.fragment(verseKind())" in prefs and '+ (seconds() ? "&seconds=1" : "")' in prefs
      and "+ HomeExit.fragment(homeExit())" in prefs)
for key, err, setter in (("versekind", "error: versekind is one of both verse hadith", "if (verseKind != null) p.setVerseKind(verseKind);"),
                         ("seconds", "error: seconds is one of on off", 'if (seconds != null) p.setSeconds("on".equals(seconds));'),
                         ("backexit", "error: backexit is one of press hold three", "if (backExit != null) p.setHomeExit(backExit);")):
    check(f"receiver: {key} is checked before anything is saved", err in receiver and setter in receiver and receiver.index(err) < receiver.index(setter))
check("`saver.sh show` says all three", '+ "\\nversekind: " + p.verseKind() + "\\nseconds: " + (p.seconds() ? "on" : "off") + "\\nbackexit: " + p.homeExit()' in receiver)
check("rows: «الآية والحديث» under «تبديل الآية والحديث» (only while the verse card is chosen), «إظهار الثواني» after «نظام الساعة», "
      "«الخروج من تلفزيونك» after «الرجوع يفتح شاشة التوقف»",
      render.index('row("تبديل الآية والحديث"') < render.index('row("الآية والحديث"') < render.index('header("الشاشة الرئيسية (تلفزيونك)")')
      and render.count('if ("verse".equals(prefs.panel()) || "verse".equals(prefs.panel2()) || prefs.homePanels().contains("verse"))') == 2
      and render.index('row("نظام الساعة"') < render.index('row("إظهار الثواني"')
      and render.index('row("الرجوع يفتح شاشة التوقف"') < render.index('row("الخروج من تلفزيونك"'))
check("the pickers save like the agent's commands", "prefs.setVerseKind(VerseKind.fromChoice(i)); d.dismiss(); render();" in st
      and "prefs.setHomeExit(HomeExit.fromChoice(i)); d.dismiss(); render();" in st)
check("the rows' categories", '{"الآية والحديث", SAVER}' in hub and '{"إظهار الثواني", LOOK}' in hub and '{"الخروج من تلفزيونك", HOME}' in hub)
check("home: BackExit sees Back last, after the adhan, the shortcut keys and the edit modes, and only when it guards",
      main.index("if (adhanTakes(e)) return true;") < main.index("if (edit != null && editKey(e)) return true;")
      < main.index("if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && backExitTakes(e)) return true;")
      and "backGuarded = BackExit.guards(mode, isDefaultHome(), scrolled);" in main and "if (!backGuarded) return false;" in main)
check("home: a new visit starts the count and the hint again", "backExit.reset(); backGuarded = false;" in main)
check("home: one press as always still goes to onBackPressed (HomeBack.leaves)", "else if (HomeBack.leaves(isDefaultHome(), false)) finish();" in main)
check("page: #versekind and #seconds only as written", "verseKind: (v => v === 'verse' || v === 'hadith' ? v : 'both')(_Q.get('versekind'))," in tpl
      and "seconds: _Q.get('seconds') === '1' && _Q.get('lowram') !== '1'," in tpl)   # a weak TV (#lowram=1) never shows them
check("page: the seconds' digits in fixed boxes, counted in the small clock's column",
      ".clock .sec i{font-style:normal;display:inline-block;width:.62em;text-align:center}" in tpl and "(CONFIG.seconds ? .44 : 0)" in tpl)
check("docs: SETTINGS.md has the three commands", all(c in settings_md for c in ("saver.sh versekind verse", "saver.sh seconds on", "saver.sh backexit hold")))

# optimize/saver.sh against a stand-in for tv.py that writes the command it was given
tmp = tempfile.mkdtemp()
log, fake = os.path.join(tmp, "sent.txt"), os.path.join(tmp, "fakepy")
with open(fake, "w") as f:
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)
def run(*args):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip()
for cmd, values, bad in (("versekind", ("both", "verse", "hadith"), ("quran", "Verse", "")),
                         ("seconds", ("on", "off"), ("yes", "1", "")),
                         ("backexit", ("press", "hold", "three"), ("3", "long", ""))):
    for v in values:
        rc, out, sent = run(cmd, v)
        check(f"saver.sh {cmd} {v}: one SET with the value quoted", rc == 0 and sent.endswith(f"screensaver.SET --es {cmd} '{v}'"), (rc, sent))
    for v in bad:
        rc, out, sent = run(cmd, v)
        check(f"saver.sh {cmd} {v!r}: usage, nothing sent", rc != 0 and not sent and f"usage: optimize/saver.sh {cmd}" in out, (rc, out, sent))
    rc, out, sent = run(cmd)
    check(f"saver.sh {cmd} with no value: usage, nothing sent", rc != 0 and not sent, (rc, out, sent))

if not shutil.which("javac"):
    print("SKIP user options: no javac here")
    sys.exit(1 if failed else 0)
outs, rcs = [], []
with tempfile.TemporaryDirectory() as t:
    for srcs, cls in (([APP / "VerseKind.java", APP / "HomeExit.java", ROOT / "tests/VerseKindRun.java"], "sa.urtv.screensaver.VerseKindRun"),
                      ([HOME / "BackExit.java", ROOT / "tests/BackExitRun.java"], "sa.urtv.home.BackExitRun")):
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", t, *map(str, srcs)], check=True)
        r = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", t, cls], capture_output=True)
        outs.append(r.stdout.decode("utf-8")); rcs.append(r.returncode)
out = "".join(outs)
print(out, end="")
sys.exit(1 if failed or any(rcs) or "FAIL" in out else 0)
