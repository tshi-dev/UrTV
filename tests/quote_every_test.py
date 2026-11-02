#!/usr/bin/env python3
"""«تبديل الآية والحديث» (QuoteEvery.java): the verse-or-hadith card once a day (the default, as always) or every 3 minutes. Read from the
source: the pref, the fragment (nothing added for the day), the receiver (checked before anything is saved, `show` says it), the settings row
(only while the verse card is chosen somewhere) and the page's #versemin=3; then `optimize/saver.sh verse` against a stand-in for tv.py, and
the JVM part (needs javac; without it, skipped with a note). The page itself: tests/page.test.mjs.   Run: python3 tests/quote_every_test.py"""
import os
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

prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
receiver = (APP / "SaverReceiver.java").read_text(encoding="utf-8")
st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
hub = (APP / "SettingsHub.java").read_text(encoding="utf-8")
tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
render = st[st.index("    private void render() {"):].split("\n    }\n")[0]

check("the default is the day (nothing changes for anyone)", 'QuoteEvery.read(sp.getString("quoteEvery", QuoteEvery.DAY))' in prefs)
check("the fragment carries it the QuoteEvery way (nothing for the day)", "+ QuoteEvery.fragment(quoteEvery())" in prefs)
check("the receiver refuses anything but day or 3 before it saves anything",
      'return "error: verse is one of day 3";' in receiver and receiver.index('error: verse is one of day 3') < receiver.index("if (verse != null) p.setQuoteEvery(verse);"))
check("`saver.sh show` says it", '+ "\\nverse: " + p.quoteEvery()' in receiver)
check("the settings row is in «بجانب الساعة», after «شكل البطاقة», only while the verse card is chosen somewhere",
      render.index('header("بجانب الساعة")') < render.index('row("شكل البطاقة"') < render.index('row("تبديل الآية والحديث"') < render.index('header("الشاشة الرئيسية (تلفزيونك)")')
      and 'if ("verse".equals(prefs.panel()) || "verse".equals(prefs.panel2()) || prefs.homePanels().contains("verse"))' in render)
pick = st[st.index("private void pickQuoteEvery()"):].split("\n    }\n")[0]
check("its picker: the two choices, the one set checked, saved like `saver.sh verse`, closed and redrawn",
      "setSingleChoiceItems(QuoteEvery.CHOICES, QuoteEvery.choice(prefs.quoteEvery())," in pick
      and "prefs.setQuoteEvery(QuoteEvery.fromChoice(i)); d.dismiss(); render();" in pick)
check("the row belongs to «شاشة التوقف» in the settings' categories", '{"تبديل الآية والحديث", SAVER}' in hub)
check("the page reads #versemin=3 only, and turns the verse cards alone when the 3 minutes are up",
      "verseMin: _Q.get('versemin') === '3' ? 3 : 0," in tpl and "CARDS.forEach(c => { if(c.kind === 'verse') renderCard(c, true); });" in tpl)
check("the page's titles: «آية اليوم» / «حديث اليوم» as always, «آية كريمة» / «حديث شريف» every 3 minutes",
      "CONFIG.verseMin ? 'آية كريمة' : 'آية اليوم'" in tpl and "CONFIG.verseMin ? 'حديث شريف' : 'حديث اليوم'" in tpl)
check("docs: SETTINGS.md and FEATURES.md say it", "saver.sh verse 3" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8")
      and "saver.sh verse 3" in (ROOT / "docs/FEATURES.md").read_text(encoding="utf-8"))

# optimize/saver.sh verse, against a stand-in for tv.py that writes the command it was given
tmp = tempfile.mkdtemp()
log, fake = os.path.join(tmp, "sent.txt"), os.path.join(tmp, "fakepy")
with open(fake, "w") as f:
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)
def run(*args):
    open(log, "w").close()
    r = subprocess.run(["bash", str(ROOT / "optimize/saver.sh"), *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip()
for v in ("3", "day"):
    rc, out, sent = run("verse", v)
    check(f"saver.sh verse {v}: one SET with the value quoted", rc == 0 and sent.endswith(f"screensaver.SET --es verse '{v}'"), (rc, sent))
rc, out, sent = run("verse", "it's")
check("saver.sh verse with an apostrophe: refused in the shell, nothing sent", rc != 0 and not sent, (rc, out, sent))
rc, out, sent = run("verse")
check("saver.sh verse with no value: usage, nothing sent", rc != 0 and not sent, (rc, out, sent))

if not shutil.which("javac"):
    print("SKIP quote every: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as t:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", t, str(APP / "QuoteEvery.java"), str(ROOT / "tests/QuoteEveryRun.java")], check=True)
    run_ = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", t, "sa.urtv.screensaver.QuoteEveryRun"], capture_output=True)
out = run_.stdout.decode("utf-8")
print(out, end="")
sys.exit(1 if failed or run_.returncode != 0 or "FAIL" in out else 0)
