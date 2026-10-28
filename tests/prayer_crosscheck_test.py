#!/usr/bin/env python3
"""The prayer times against an independent calculation and against printed timetables.

tests/prayer_crosscheck.tsv holds the six times of every city in the app's list (Saudi Arabia, the Gulf, Egypt, Jordan, Syria, Lebanon,
Palestine, Morocco, Algeria, Tunisia) on 12 dates of 2026-2027, on the days the clocks change, on the Ramadan edges, with each country's method,
from tests/adhan_port.mjs (a port of the formulas of adhan-js 4.4.6, Meeus' full series, MIT) -- see tests/prayer_crosscheck_gen.mjs. Here:
  - the port reproduces the values adhan's own unit tests expect (tests/adhan_port_test.mjs);
  - the file is exactly what the generator writes, and holds every city of Cities.java;
  - the launcher's PrayerMath is within one minute of it for every time (tests/PrayerCrosscheckRun.java), and of the timetables four authorities
    printed (tests/prayer_published.tsv). The page's computePrayers is checked the same way by tests/prayer_crosscheck_page.mjs (run.sh).
No network. Needs node (the generator and the port) and javac (the launcher); a missing one is skipped with a note.   Run: python3 tests/prayer_crosscheck_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


# Exactly the file, but for the time-zone data: a node with other data (Morocco left UTC+1 in tzdata 2026c) writes a zone's rows at
# another offset, its times shifted by just that much. Those rows are drift, told apart; any other difference is a real one.
def drift_only(new, old):
    a, b = new.splitlines(), old.splitlines()
    if len(a) != len(b):
        return None
    drift, zones = 0, set()
    for x, y in zip(a, b):
        if x == y:
            continue
        if x.startswith("# Written with the time-zone data") and y.startswith("# Written with the time-zone data"):
            continue
        fx, fy = x.split("\t"), y.split("\t")
        if len(fx) != 9 or len(fy) != 9 or fx[:7] != fy[:7]:
            return None
        shift = int(fx[7]) - int(fy[7])
        mx = [int(t[:2]) * 60 + int(t[3:]) for t in fx[8].split()]
        my = [int(t[:2]) * 60 + int(t[3:]) for t in fy[8].split()]
        if not shift or any(u - v != shift for u, v in zip(mx, my)):
            return None
        drift += 1
        zones.add(fx[0])
    return drift, zones


if shutil.which("node"):
    run = subprocess.run(["node", str(ROOT / "tests/adhan_port_test.mjs")], capture_output=True, text=True)
    print(run.stdout, end="")
    check("adhan port: self-test", run.returncode == 0, run.stderr)
    gen = subprocess.run(["node", str(ROOT / "tests/prayer_crosscheck_gen.mjs")], capture_output=True, text=True)
    stored = (ROOT / "tests/prayer_crosscheck.tsv").read_text(encoding="utf-8")
    same = gen.returncode == 0 and gen.stdout == stored
    tz = drift_only(gen.stdout, stored) if gen.returncode == 0 and not same else None
    if tz:
        print(f"NOTE prayer_crosscheck.tsv: {tz[0]} rows differ only by this node's time-zone data ({', '.join(sorted(tz[1]))}), not by a formula")
    check("prayer_crosscheck.tsv is what tests/prayer_crosscheck_gen.mjs writes, but for the time-zone data (a new city or a changed date needs it run again)",
          same or bool(tz), gen.stderr[:300] or "differs beyond the time-zone data")
else:
    print("SKIP port and generator: no node here")

rows = [l.split("\t") for l in (ROOT / "tests/prayer_crosscheck.tsv").read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
cities = re.findall(r'\{"([^"]+)", "(-?[\d.]+)", "(-?[\d.]+)"(?:, "([^"]+)")?\}',
                    (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Cities.java").read_text(encoding="utf-8"))
have = {}
for r in rows:
    if r[6] != "0":
        have.setdefault((r[1], r[0]), set()).add(r[4])
missing = [n for n, _, _, z in cities if len(have.get((n, z or "Asia/Riyadh"), ())) < 12]
check(f"every one of the {len(cities)} cities of Cities.java has at least 12 dates in the file", not missing and len(cities) > 300, missing[:5])
check("the dates include both Ramadans, the days the clocks change in Egypt, Lebanon, Palestine and Morocco, and 2026 and 2027",
      {"2026-02-20", "2027-02-09", "2026-04-24", "2026-03-29", "2026-03-28", "2026-02-15", "2026-10-29", "2026-09-20"} <= {r[4] for r in rows}
      and any(r[4].startswith("2027") for r in rows) and any(r[4].startswith("2026") for r in rows), "")

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/PrayerMath.java"),
                        str(ROOT / "tests/PrayerCrosscheckRun.java"), str(ROOT / "tests/PrayerPublishedRun.java")], check=True)
        for cls, tsv in (("PrayerCrosscheckRun", "prayer_crosscheck.tsv"), ("PrayerPublishedRun", "prayer_published.tsv")):
            run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah." + cls, str(ROOT / "tests" / tsv)], capture_output=True)
            out = run.stdout.decode("utf-8")
            print(out, end="")
            check(cls + " exit code", run.returncode == 0 and "FAIL" not in out, run.stderr.decode("utf-8", "replace")[-300:])
        # Time-zone drift is tested against whichever rule THIS machine has, so the test does not depend on its tzdata version: the file is
        # written once for each rule (Morocco's old one: UTC+1 from 20 September 2026; the new one, tzdata 2026c: UTC+0), and the one for the
        # rule this machine does NOT have must pass with its rows re-based and reported as drift, never as a formula error; the one for the
        # rule it has must pass with none re-based; and one minute more than the drift still fails either way.
        stored = (ROOT / "tests/prayer_crosscheck.tsv").read_text(encoding="utf-8").splitlines()   # written with tzdata 2025b: the old rule
        def as_rule(new_rule, extra=0):
            out = []
            for l in stored:
                f = l.split("\t")
                if not l.startswith("#") and f[0] == "Africa/Casablanca" and f[4] >= "2026-09-20" and f[7] == "60":
                    ms = [int(t[:2]) * 60 + int(t[3:]) - (60 if new_rule else 0) + extra for t in f[8].split()]
                    if new_rule:
                        f[7] = "0"
                    f[8] = " ".join(f"{m // 60:02d}:{m % 60:02d}" for m in ms)
                out.append("\t".join(f))
            return "\n".join(out) + "\n"
        def as_new_rule(extra=0):
            return as_rule(True, extra)
        moved = sum(1 for a, b in zip(as_new_rule().splitlines(), stored) if a != b)
        own = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.PrayerCrosscheckRun", str(ROOT / "tests/prayer_crosscheck.tsv")], capture_output=True)
        machine_new = f"{moved} rows re-based" in own.stdout.decode("utf-8")   # the old-rule file is drift here: this machine has the new rule
        print(f"NOTE time-zone drift test: this machine's Java has Morocco's {'new' if machine_new else 'old'} rule; the file for the {'old' if machine_new else 'new'} rule is the drifted one")
        drifted, wrong = Path(tmp) / "drifted.tsv", Path(tmp) / "wrong.tsv"
        drifted.write_text(as_rule(not machine_new), encoding="utf-8")
        wrong.write_text(as_rule(not machine_new, extra=2), encoding="utf-8")
        own_rule = Path(tmp) / "own.tsv"
        own_rule.write_text(as_rule(machine_new), encoding="utf-8")
        for path, want_ok in ((drifted, True), (wrong, False)):
            run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.PrayerCrosscheckRun", str(path)], capture_output=True)
            out = run.stdout.decode("utf-8")
            if want_ok:
                check(f"time-zone drift: the file as the other Morocco rule writes it ({moved} rows) passes, its rows re-based and reported as drift",
                      run.returncode == 0 and f"{moved} rows re-based" in out and "Africa/Casablanca" in out, out[-400:])
            else:
                check("time-zone drift is not a blind spot: the same rows two minutes further off still fail as a formula error", run.returncode != 0, out[-300:])
        run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.PrayerCrosscheckRun", str(own_rule)], capture_output=True)
        out = run.stdout.decode("utf-8")
        check("the file written with this machine's own Morocco rule passes with no row re-based", run.returncode == 0 and "every row at the offset the file was written with" in out, out[-300:])
        check("the generator check tells the new rule's file apart as drift only",
              drift_only(as_new_rule(), "\n".join(stored) + "\n") == (moved, {"Africa/Casablanca"})
              and drift_only(as_new_rule(extra=2), "\n".join(stored) + "\n") is None and moved > 50, str(moved))
else:
    print("SKIP launcher crosscheck: no javac here")
sys.exit(1 if failed else 0)
