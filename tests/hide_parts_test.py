#!/usr/bin/env python3
"""Three parts that can be hidden, each shown by default as always (owner rows 350/526, 501, 537): «المناسبة القادمة» at the bottom middle of
the screensaver (`saver.sh occasion`), «تعتيم الليل» (`saver.sh nightdim`) and the home screen's «المهام» button (`saver.sh taskspill`). Read
from the source: the prefs and their defaults, the fragment (nothing added while shown), the receiver (checked before anything is saved, `show`
says each), the settings rows and their categories, the page, the home's row of pills (out of the row, never just hidden, back after
«المصادر»), then saver.sh against a stand-in for tv.py. The page itself: tests/page.test.mjs.   Run: python3 tests/hide_parts_test.py"""
import os
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
text = lambda p: p.read_text(encoding="utf-8")

prefs, receiver, st, hub = (text(APP / f) for f in ("Prefs.java", "SaverReceiver.java", "SettingsActivity.java", "SettingsHub.java"))
tpl, main, snap = text(ROOT / "src/template.html"), text(HOME / "MainActivity.java"), text(HOME / "ConfigSnapshot.java")
render = st[st.index("    private void render() {"):].split("\n    }\n")[0]

for key, pref, extra, frag in (("occasion", "occasion", "occasion", "&occasion=0"), ("nightDim", "nightDim", "nightdim", "&nightdim=0"),
                               ("tasksPill", "tasksPill", "taskspill", "&taskspill=0")):
    check(f"{extra}: shown by default (nothing changes for anyone)", f'boolean {key}() {{ return sp.getBoolean("{pref}", true); }}' in prefs)
    check(f"{extra}: in the fragment only while hidden", f'({key}() ? "" : "{frag}")' in prefs)
    check(f"{extra}: the receiver takes on or off and refuses the rest before saving",
          f'{{"{extra}", ' in receiver and 'is one of on off' in receiver
          and receiver.index(f'{{"{extra}", ') < receiver.index(f'p.set{key[0].upper() + key[1:]}("on".equals('))
    check(f"{extra}: `saver.sh show` says it", f'"\\n{extra}: " + (p.{key}() ? "on" : "off")' in receiver)

check("rows «المناسبة القادمة» and «تعتيم الليل» in «العرض» (شاشة التوقف), before «المناسبات الوطنية»; OK flips each",
      render.index('header("العرض")') < render.index('row("المناسبة القادمة"') < render.index('row("تعتيم الليل"') < render.index('row("المناسبات الوطنية"')
      and "prefs.setOccasion(!prefs.occasion()); render();" in render and "prefs.setNightDim(!prefs.nightDim()); render();" in render
      and '{"المناسبة القادمة", SAVER}' in hub and '{"تعتيم الليل", SAVER}' in hub)
check("row «زر المهام» in «الشاشة الرئيسية», after «زر تلاوة وأذان», saying where the tasks are while it is hidden",
      render.index('row("زر تلاوة وأذان"') < render.index('row("زر المهام"') < render.index('row("جرس التحديثات"')
      and "prefs.setTasksPill(!prefs.tasksPill()); render();" in render and "بطاقة «المهام» ومن الجوال" in render and '{"زر المهام", HOME}' in hub)

check("page: #occasion=0 and #nightdim=0 only; anything else as always",
      "occasion: _Q.get('occasion') !== '0'," in tpl and "nightDim: _Q.get('nightdim') !== '0'," in tpl)
check("page: the occasion's place stays (visibility, not display), so the prayer and the slot do not move",
      "html.occ-off .p-mid{visibility:hidden}" in tpl and "if(!CONFIG.occasion) document.documentElement.classList.add('occ-off');" in tpl)
night = tpl[tpl.index("function applyNight(){"):].split("\n}\n")[0]
check("page: with night dimming off the dimmer stays clear, before any time is read", night.index("if(!CONFIG.nightDim)") < night.index("riyadhParts()"))

check("home: an older screensaver (or none) says nothing: the button is shown",
      'boolean tasksPillOn() { return fragment == null || !(fragment.contains("&taskspill=0") || fragment.startsWith("taskspill=0")); }' in snap)
check("home: the row is built from the setting too (no flash of the button on a TV that hid it)",
      "if (isInstalled(SAVER_PKG) && ConfigSnapshot.read(this).tasksPillOn()) row.add(tasks);" in main)
apply = main[main.index("private void applyTasksPill(boolean setting) {"):].split("\n    }\n")[0]
check("home: hiding takes it out of the row and out of the pills, the focus to «المصادر»; showing puts it back right after «المصادر»",
      "pills.remove(tasksPill);" in apply and "pillRow.removeView(tasksPill);" in apply and "inputsPill.requestFocus();" in apply
      and "pillRow.addView(tasksPill, pillRow.indexOfChild(inputsPill) + 1," in apply and "pills.add(pills.indexOf(inputsPill) + 1, tasksPill);" in apply
      and "wireFocus();" in apply and "isInstalled(SAVER_PKG)" in apply)
check("home: the minute tick applies it, beside the «تلاوة وأذان» button", "applyListenPill(cfg.listenPillOn);\n        applyTasksPill(cfg.tasksPillOn());" in main)
check("home: the phone's and the card's ways into the list do not look at it",
      'case "@tasks": launchGuarded(ParentalPolicy.PILL_TASKS, new Intent(this, TasksActivity.class), "المهام"); break;' in main
      and "tasksPill" not in text(HOME / "HomeCard.java"))

tmp = tempfile.mkdtemp()
log, fake = os.path.join(tmp, "sent.txt"), os.path.join(tmp, "fakepy")
with open(fake, "w") as f:
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)
def run(*args):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip()
for cmd in ("occasion", "nightdim", "taskspill"):
    for v in ("off", "on"):
        rc, out, sent = run(cmd, v)
        check(f"saver.sh {cmd} {v}: one SET with the value quoted", rc == 0 and sent.endswith(f"screensaver.SET --es {cmd} '{v}'"), (rc, sent))
    rc, out, sent = run(cmd, "it's")
    check(f"saver.sh {cmd} with an apostrophe: refused in the shell, nothing sent", rc != 0 and not sent, (rc, out, sent))
    rc, out, sent = run(cmd)
    check(f"saver.sh {cmd} with no value: usage, nothing sent", rc != 0 and not sent, (rc, out, sent))
check("SETTINGS.md and FEATURES.md say each", all(f"saver.sh {c} off" in text(ROOT / "docs" / d) for c in ("occasion", "nightdim", "taskspill")
                                                    for d in ("SETTINGS.md", "FEATURES.md")))
sys.exit(1 if failed else 0)
