#!/usr/bin/env python3
"""The soak's verdict (tester/soak/soak.sh, .github/workflows/soak.yml): the home's memory (PSS) and thread count, sampled every 5 minutes while the radio
and the recitation play for hours, must not grow. A report of «the screen froze after a day of Quran» (R-454) is what this watches for.

    python3 -I tester/soak/judge.py samples.tsv [--warmup-min 30] [--pss-mb-per-hour 6] [--threads 10]

samples.tsv: one line per sample, tab-separated: minutes since the start, pid, PSS of sa.urtv.home in KB, its threads, system_server's PSS in KB,
ANRs so far, what played (radio|surah). A value the emulator did not give is «-». Exit 0 GREEN, 1 RED, with one line per check.

RED when: the home's process changed or vanished (a crash, a kill) or an ANR was logged; fewer than 12 samples (an hour) after the warm-up; the home's PSS grows
(a least-squares slope over the samples after the warm-up above the limit, and the last hour's median above the first hour's by more than half the
hour's limit); its threads grow (the last quarter's median above the first quarter's by more than the limit). system_server's slope is reported, not judged.
"""
import sys


def parse(text):
    rows = []
    for line in text.splitlines():
        f = line.rstrip("\n").split("\t")
        if not line.strip() or line.startswith("#") or len(f) < 6:
            continue
        num = lambda s: None if s in ("-", "") else float(s)
        rows.append({"min": float(f[0]), "pid": f[1], "pss": num(f[2]), "thr": num(f[3]), "sys": num(f[4]), "anr": int(f[5]) if f[5] not in ("-", "") else 0,
                     "what": f[6] if len(f) > 6 else ""})
    return rows


def slope_per_hour(pts):
    """least squares, y per hour; pts: (minutes, y)"""
    n = len(pts)
    if n < 2:
        return 0.0
    mx = sum(p[0] for p in pts) / n
    my = sum(p[1] for p in pts) / n
    den = sum((p[0] - mx) ** 2 for p in pts)
    return 0.0 if den == 0 else sum((p[0] - mx) * (p[1] - my) for p in pts) / den * 60.0


def median(xs):
    s = sorted(xs)
    return 0.0 if not s else (s[len(s) // 2] if len(s) % 2 else (s[len(s) // 2 - 1] + s[len(s) // 2]) / 2)


def judge(rows, warmup_min=30.0, pss_mb_per_hour=6.0, threads=10):
    out, red = [], False
    def say(ok, text):
        nonlocal red
        red |= not ok
        out.append(("PASS " if ok else "FAIL ") + text)
    pids = {r["pid"] for r in rows if r["pid"] not in ("-", "")}
    gone = [r["min"] for r in rows if r["pid"] in ("-", "")]
    say(len(pids) == 1 and not gone, f"the home's process stayed the same (pids seen: {sorted(pids)}; missing at minutes {gone[:5]})")
    anr = max((r["anr"] for r in rows), default=0)
    say(anr == 0, f"no ANR was logged ({anr})")
    after = [r for r in rows if r["min"] >= warmup_min]
    say(len(after) >= 12, f"enough samples after the {warmup_min:g} min warm-up ({len(after)}, at least 12: an hour)")
    pss = [(r["min"], r["pss"] / 1024.0) for r in after if r["pss"] is not None]
    if len(pss) >= 6:
        s = slope_per_hour(pss)
        first = median([y for m, y in pss if m < pss[0][0] + 60])
        last = median([y for m, y in pss if m > pss[-1][0] - 60])
        grew = s > pss_mb_per_hour and last - first > pss_mb_per_hour / 2
        say(not grew, f"the home's PSS does not grow: {s:+.1f} MB/h (limit {pss_mb_per_hour:g}), first hour {first:.0f} MB, last hour {last:.0f} MB")
    thr = [r["thr"] for r in after if r["thr"] is not None]
    if len(thr) >= 4:
        q = max(1, len(thr) // 4)
        a, b = median(thr[:q]), median(thr[-q:])
        say(b - a <= threads, f"the home's threads do not grow: {a:.0f} at the start, {b:.0f} at the end (limit +{threads})")
    sysp = [(r["min"], r["sys"] / 1024.0) for r in after if r["sys"] is not None]
    if len(sysp) >= 6:
        out.append(f"INFO system_server PSS {slope_per_hour(sysp):+.1f} MB/h (not judged: the emulator's own work moves it)")
    out.append("RED" if red else "GREEN")
    return red, out


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    opts = {"--warmup-min": 30.0, "--pss-mb-per-hour": 6.0, "--threads": 10}
    i = 2
    while i < len(argv):
        if argv[i] in opts and i + 1 < len(argv):
            opts[argv[i]] = type(opts[argv[i]])(argv[i + 1]); i += 2
        else:
            print(f"unknown option {argv[i]}"); return 2
    with open(argv[1], encoding="utf-8") as f:
        red, out = judge(parse(f.read()), opts["--warmup-min"], opts["--pss-mb-per-hour"], opts["--threads"])
    print("\n".join(out))
    return 1 if red else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
