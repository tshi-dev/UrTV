#!/usr/bin/env python3
"""4.3 (issue #55): ZoneTable.java for both apps, the UTC offsets of the zones whose rules changed in recent years, from the newest tz database
this machine has (Python's zoneinfo; its version goes in the file). A TV whose own tz data is older shows those cities' clocks, dates, prayer and
reminder times off by an hour (Morocco: +01 instead of +00 since 2026-09-20, tzdata 2026c+); ZoneFix takes the offset from this table first.

    python3 .github/tools/zone_table.py            writes both ZoneTable.java files
    python3 .github/tools/zone_table.py --check    exits 1 when the files are not what this tzdata gives (tests/zone_fix_test.py)

Each zone: its offset at FROM, then every change up to TO, to the minute (a day-by-day scan, then a search to the minute).
"""
import datetime as dt
import sys
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
OUT = [(ROOT / "android/launcher/src/main/java/sa/urtv/home/ZoneTable.java", "sa.urtv.home"),
       (ROOT / "android/app/src/main/java/sa/urtv/screensaver/ZoneTable.java", "sa.urtv.screensaver")]
FROM, TO = dt.datetime(2016, 1, 1, tzinfo=dt.timezone.utc), dt.datetime(2046, 1, 1, tzinfo=dt.timezone.utc)
# the zones with a rule change since 2016 that an older TV gets wrong, and the ones whose rules are written as negative DST
ZONES = [
    "Africa/Casablanca", "Africa/El_Aaiun",          # Morocco: +01 with Ramadan at +00 (2018), then +00 for good from 2026-09-20 (tzdata 2026c+)
    "Africa/Cairo",                                  # DST back from 2023
    "Africa/Juba",                                   # +03 to +02 in 2021
    "Africa/Khartoum",                               # +03 to +02 in 2017
    "Asia/Amman", "Asia/Damascus",                   # +03 all year from late 2022
    "Asia/Gaza", "Asia/Hebron",                      # DST dates set year by year (Ramadan)
    "Asia/Tehran",                                   # no DST from 2022
    "Asia/Almaty", "Asia/Qostanay", "Asia/Qyzylorda",  # Kazakhstan one zone, +05, from 2024-03-01 (Qyzylorda 2018)
    "Asia/Pyongyang",                                # +08:30 back to +09 in 2018
    "America/Mexico_City", "America/Monterrey", "America/Merida", "America/Chihuahua", "America/Ciudad_Juarez",
    "America/Ojinaga", "America/Mazatlan", "America/Bahia_Banderas",  # Mexico: no DST from 2022 (the border zones keep it)
    "America/Edmonton", "America/Winnipeg", "America/Yellowknife", "America/Inuvik", "America/Rainy_River",  # Alberta, Manitoba: permanent DST from 2026-11-01
    "America/Nuuk",                                  # -03 to -02 (2023), DST after
    "Pacific/Fiji",                                  # no DST from 2022
    "Europe/Dublin",                                 # negative DST in the tz source: the same offsets, listed so no reading of it can differ
]


def off(z, t):
    return int(t.astimezone(z).utcoffset().total_seconds() // 60)


def changes(zid):
    z = ZoneInfo(zid)
    out, prev, t = [], off(z, FROM), FROM
    base = prev
    step = dt.timedelta(hours=12)
    while t < TO:
        n = t + step
        o = off(z, n)
        if o != prev:
            lo, hi = t, n   # the first minute with the new offset
            while hi - lo > dt.timedelta(minutes=1):
                mid = lo + (hi - lo) / 2
                mid = mid.replace(second=0, microsecond=0)
                if mid <= lo:
                    mid = lo + dt.timedelta(minutes=1)
                if off(z, mid) == prev:
                    lo = mid
                else:
                    hi = mid
            out.append((int(hi.timestamp() // 60), o))
            prev = o
        t = n
    return base, out


def version():
    try:
        line = Path("/usr/share/zoneinfo/tzdata.zi").read_text(encoding="utf-8").splitlines()[0]
        return line.replace("# version", "").strip()
    except OSError:
        return "unknown"


def source(pkg):
    rows = []
    for zid in ZONES:
        base, ch = changes(zid)
        rows.append('        "' + zid + "|" + str(base) + "|" + ",".join(f"{m}:{o}" for m, o in ch) + '",')
    return f"""package {pkg};

/**
 * Made by .github/tools/zone_table.py from tzdata {version()} (do not edit by hand; run it again with a newer tz database). The UTC offsets, in
 * minutes, of the zones whose rules changed in recent years: "zone|offset at {FROM.date()}|minute since 1970 (UTC):new offset,...", up to
 * {TO.date()}. ZoneFix reads it first, so a TV with older tz data shows these cities' times right (issue #55: Morocco). Both apps carry it.
 */
final class ZoneTable {{
    private ZoneTable() {{ }}

    static final String VERSION = "{version()}";
    static final long FROM_MS = {int(FROM.timestamp()) * 1000}L, TO_MS = {int(TO.timestamp()) * 1000}L;

    static final String[] DATA = {{
{chr(10).join(rows)}
    }};
}}
"""


def main():
    check = "--check" in sys.argv
    bad = 0
    for path, pkg in OUT:
        text = source(pkg)
        if check:
            if not path.is_file() or path.read_text(encoding="utf-8") != text:
                print(f"FAIL {path.relative_to(ROOT)} is not what tzdata {version()} gives: run .github/tools/zone_table.py")
                bad = 1
        else:
            path.write_text(text, encoding="utf-8")
            print(f"wrote {path.relative_to(ROOT)} (tzdata {version()})")
    return bad


if __name__ == "__main__":
    sys.exit(main())
