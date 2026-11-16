#!/usr/bin/env python3
"""4.4 radio reliability, part 4: the stations' stalls, errors, reconnects and rebuffers of the last seven days are kept across restarts (RadioWeek,
in the tilawah prefs), said in the player status (radio_week=) and in optimize/share.py's report («Radio, last 7 days: …»). Numbers only.
    python3 tests/radio_week_test.py"""
import importlib.util, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
svc, rec, prefs = read(T / "MusicService.java"), read(T / "MusicReceiver.java"), read(T / "Prefs.java")
check("RadioWeek is plain Java", "import android" not in read(T / "RadioWeek.java"))
check("kept in the tilawah prefs", 'sp.getString("radio_week", "")' in prefs and 'putString("radio_week", r)' in prefs)
check("a quiet restart, a station's error, a reconnect and a station's rebuffer are each counted",
      "stallRestarts++;\n            week(RadioWeek.STALL);" in svc and "errors++;\n        if (source == Prefs.SOURCE_RADIO) week(RadioWeek.ERROR);" in svc
      and "reconnects++;\n        week(RadioWeek.RECONNECT);" in svc and "rebuffers++;\n                if (source == Prefs.SOURCE_RADIO) week(RadioWeek.REBUFFER);" in svc)
check("the day is the TV's place's (ZoneFix)", "sa.urtv.home.ZoneFix.offset(Place.get(c).tz, now)" in svc and "TimeZone" not in read(T / "RadioWeek.java"))
check("the player status says them", '"|radio_week=" + RadioWeek.field(prefs.radioWeek(), MusicService.weekDay(context))' in rec)

spec = importlib.util.spec_from_file_location("share_mod", ROOT / "optimize/share.py")
sm = importlib.util.module_from_spec(spec); spec.loader.exec_module(sm)
line = sm.radio_week_line('data="state=1|reconnects=0|radio_week=3.1.2.40|foreground=1"')
check("the report's line", line == "Radio, last 7 days: stalls=3 errors=1 reconnects=2 rebuffers=40", line)
check("no line for nothing, zeros or a malformed field", sm.radio_week_line("") is None and sm.radio_week_line(None) is None
      and sm.radio_week_line("radio_week=0.0.0.0|") is None and sm.radio_week_line("radio_week=1.2.3|") is None and sm.radio_week_line("radio_week=1.2.3.4.5") is None)
share = read(ROOT / "optimize/share.py")
check("the report adds both radio lines when the TV answers", 'parts += ["", *radio]' in share and "radio_week_line(out.stdout)" in share)

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
public class WkRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
static String t(long[] a) { return a[0] + "." + a[1] + "." + a[2] + "." + a[3]; }
public static void main(String[] x) {
  String r = "";
  r = RadioWeek.add(r, 100, RadioWeek.STALL); r = RadioWeek.add(r, 100, RadioWeek.STALL); r = RadioWeek.add(r, 101, RadioWeek.ERROR);
  r = RadioWeek.add(r, 103, RadioWeek.RECONNECT); r = RadioWeek.add(r, 103, RadioWeek.REBUFFER);
  out("counted by day and kind", r.equals("100,2,0,0,0;101,0,1,0,0;103,0,0,1,1"));
  out("the week's totals", t(RadioWeek.totals(r, 103)).equals("2.1.1.1") && RadioWeek.field(r, 103).equals("2.1.1.1"));
  out("a day more than a week back is not counted", t(RadioWeek.totals(r, 107)).equals("0.1.1.1") && t(RadioWeek.totals(r, 110)).equals("0.0.0.0"));
  out("and dropped on the next count", RadioWeek.add(r, 108, RadioWeek.STALL).equals("103,0,0,1,1;108,1,0,0,0"));
  out("a day ahead of today (the clock went back) is not counted", t(RadioWeek.totals(r, 101)).equals("2.1.0.0"));
  out("never more than seven entries", RadioWeek.add(RadioWeek.add(RadioWeek.add(RadioWeek.add(RadioWeek.add(RadioWeek.add(RadioWeek.add(RadioWeek.add("", 1, 0), 2, 0), 3, 0), 4, 0), 5, 0), 6, 0), 7, 0), 8, 0).split(";").length == 7);
  out("a broken record is dropped, not thrown", RadioWeek.add("x;1,2;-1,0,0,0,0;100,a,0,0,0", 100, RadioWeek.ERROR).equals("100,0,1,0,0") && RadioWeek.field(null, 5).equals("0.0.0.0"));
  out("an unknown kind changes nothing", RadioWeek.add(r, 103, 9).equals(r));
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "WkRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "RadioWeek.java"), str(Path(tmp, "WkRun.java"))], capture_output=True, text=True)
        check("RadioWeek compiles on its own", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.WkRun"], capture_output=True, text=True).stdout
            for ln in out.splitlines():
                name, res = ln.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 8, len(out.splitlines()))
sys.exit(1 if failed else 0)
