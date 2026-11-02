"""optimize/saver.sh: an apostrophe in any value stops the command and sends nothing (it used to send an empty value), and a
good value is sent quoted (no TV needed).

    python3 tests/saver_quote_test.py

PY is a stand-in for optimize/tv.py that writes the shell command it was given to a file.
"""
import os, subprocess, sys, tempfile

here = os.path.dirname(os.path.abspath(__file__))
saver = os.path.join(here, "..", "optimize", "saver.sh")
tmp = tempfile.mkdtemp()
log = os.path.join(tmp, "sent.txt")
fake = os.path.join(tmp, "fakepy")
with open(fake, "w") as f:
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\necho \'Broadcast completed: result=0, data="saved"\'\n')
os.chmod(fake, 0o755)

failed = 0
def run(*args):
    open(log, "w").close()
    r = subprocess.run(["bash", saver, *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip()

def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + detail))
    failed += not ok

A = "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET "
bad = "it's"
for words in (["city", bad], ["city", "x", bad, "46.7"], ["city", "x", "24.7", bad], ["city", "x", "24.7", "46.7", bad], ["teams", bad], ["card", bad],
              ["home-card", bad], ["greeting", bad], ["listenpill", bad], ["updatebell", bad], ["adhanbar", bad], ["genres", bad], ["leagues", bad], ["events", bad], ["langs", bad],
              ["minrating", bad], ["photos", bad], ["cardstyle", bad], ["markets", bad], ["mkstyle", bad], ["stocks", bad], ["slot", bad],
              ["countdown", "2026-12-01", bad], ["countdown", bad, "name"], ["national", bad], ["digits", bad], ["idle", bad]):
    rc, out, sent = run(*words)
    check(f"{' '.join(words[:2])}…: an apostrophe stops the command, nothing is sent", rc != 0 and not sent and "apostrophes" in out, f"rc={rc} sent={sent!r} {out}")
rc, out, sent = run("city", "الرس", "25.86", "43.49", "Asia/Riyadh")
check("city with a zone: every value quoted", rc == 0 and sent == A + "--es city 'الرس' --es lat '25.86' --es lon '43.49' --es tz 'Asia/Riyadh'", sent)
rc, out, sent = run("city", "الرس", "25.86", "43.49", "Asia/Riyadh", "SA")
check("custom city country is quoted with its coordinates and zone", rc == 0 and sent.endswith("--es tz 'Asia/Riyadh' --es country 'SA'"), sent)
rc, out, sent = run("city", "الرس", "25.86", "43.49", "Asia/Riyadh", "it's")
check("custom country with an apostrophe sends nothing", rc != 0 and not sent, sent)
rc, out, sent = run("city", "GB|London")
check("country-qualified city name is sent as one safe argument", rc == 0 and sent == A + "--es city 'GB|London'", sent)
rc, out, sent = run("card", "prayers")
check("card: quoted", rc == 0 and sent == A + "--es panel 'prayers'", sent)
rc, out, sent = run("teams", "1:A", "2:B")
check("teams: joined and quoted", rc == 0 and sent == A + "--es teams '1:A,2:B'", sent)
rc, out, sent = run("teams", "none")
check("teams none: an empty list is sent (the defaults go)", rc == 0 and sent == A + "--es teams ''", sent)
rc, out, sent = run("teams", "")
check("teams '': the same", rc == 0 and sent == A + "--es teams ''", sent)
rc, out, sent = run("teams", "none", "1:A")
check("teams none with a team: refused, nothing sent", rc != 0 and not sent, f"{rc} {sent!r} {out}")
rc, out, sent = run("teams")
check("teams with nothing: usage, nothing sent", rc != 0 and not sent and "none" in out, f"{rc} {sent!r} {out}")
rc, out, sent = run("prayeradj", "on")
check("prayeradj on: sent, and the home app is told to set its alarm again", rc == 0 and sent.splitlines()[0] == A + "--es prayeradj on"
      and "sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN_RESCHEDULE" in sent, sent)
rc, out, sent = run("prayeradj", "fajr", "+2", "maghrib", "-1")
check("prayeradj fajr +2 maghrib -1: each prayer's minutes quoted", rc == 0 and sent.splitlines()[0] == A + "--es adj_fajr '+2' --es adj_maghrib '-1'", sent)
rc, out, sent = run("prayeradj", "الفجر", "0")
check("prayeradj with an Arabic name", rc == 0 and sent.splitlines()[0] == A + "--es adj_fajr '0'", sent)
rc, out, sent = run("prayeradj", "sunrise", "2")
check("prayeradj sunrise: refused (it is not a prayer), nothing sent", rc != 0 and not sent and "not a prayer" in out, f"{rc} {sent!r} {out}")
rc, out, sent = run("prayeradj", "fajr")
check("prayeradj fajr with no minutes: usage, nothing sent", rc != 0 and not sent, f"{rc} {sent!r} {out}")
rc, out, sent = run("prayeradj")
check("prayeradj with nothing: usage", rc != 0 and not sent and "usage" in out, out)
rc, out, sent = run("prayeradj", "fajr", "it's")
check("prayeradj with an apostrophe: nothing sent", rc != 0 and not sent and "apostrophes" in out, out)
rc, out, sent = run("prayeradj", "on", "off")
check("prayeradj on off: usage", rc != 0 and not sent, out)
rc, out, sent = run("countdown", "2026-12-01", "a trip")
check("countdown: both quoted", rc == 0 and sent == A + "--es countdown '2026-12-01' --es cdname 'a trip'", sent)
rc, out, sent = run("leagues", "")
check("leagues '' (none): an empty value, quoted", rc == 0 and sent == A + "--es leagues ''", sent)
rc, out, sent = run("markets", "off")
check("markets off: hides the line, the list is not touched", rc == 0 and sent == A + "--es mkbar 'off'", sent)
rc, out, sent = run("markets", "on")
check("markets on: shows the saved list again", rc == 0 and sent == A + "--es mkbar 'on'", sent)
rc, out, sent = run("markets", "tasi,gold")
check("markets LIST: sets the list and turns the line on", rc == 0 and sent == A + "--es markets 'tasi,gold'\n" + A + "--es mkbar 'on'", sent)
rc, out, sent = run("clocksize", "medium")
check("clocksize: quoted", rc == 0 and sent == A + "--es clocksize 'medium'", sent)
sys.exit(1 if failed else 0)
