#!/usr/bin/env python3
"""App-switch numbers (4.4 beta 4, reports of lag and hangs switching apps): the outputs tester/perf/switch.sh saved, read into one line per round and
the medians, and judged against limits when they are given.

    python3 -I tester/perf/switch.py DIR [--leave-ms N] [--back-ms N] [--jank-pct N] [--p99-ms N]

DIR holds, per round i: leave-i.txt (`am start -W` of Settings from the home: the home's onPause is in its WaitTime), back-i.txt (`am start -W` of
the home's HOME: TotalTime is the home's own resume and first frame) and gfx-i.txt (`dumpsys gfxinfo sa.urtv.home` for the 3 s after the return).
Without limits it only reports (the CI emulator draws in software); with them, a median over its limit is RED (exit 1).
"""
import re
import sys
from pathlib import Path


def am_start(text):
    """TotalTime and WaitTime of `am start -W` (None when it did not say)"""
    t = re.search(r"^\s*TotalTime:\s*(\d+)", text or "", re.M)
    w = re.search(r"^\s*WaitTime:\s*(\d+)", text or "", re.M)
    return (int(t.group(1)) if t else None), (int(w.group(1)) if w else None)


def gfx(text):
    """frames, janky %, 90th and 99th percentile in ms of `dumpsys gfxinfo` (None when it did not say)"""
    f = re.search(r"Total frames rendered:\s*(\d+)", text or "")
    j = re.search(r"Janky frames:\s*\d+\s*\(([\d.]+)%\)", text or "")
    p90 = re.search(r"90th percentile:\s*(\d+)ms", text or "")
    p99 = re.search(r"99th percentile:\s*(\d+)ms", text or "")
    v = lambda m, k=int: k(m.group(1)) if m else None
    return v(f), v(j, float), v(p90), v(p99)


def median(xs):
    s = sorted(x for x in xs if x is not None)
    return None if not s else (s[len(s) // 2] if len(s) % 2 else (s[len(s) // 2 - 1] + s[len(s) // 2]) / 2)


def read(d):
    d = Path(d)
    rows = []
    for i in sorted(int(m.group(1)) for p in d.glob("back-*.txt") for m in [re.fullmatch(r"back-(\d+)\.txt", p.name)] if m):
        rd = lambda n: (d / f"{n}-{i}.txt").read_text(encoding="utf-8", errors="replace") if (d / f"{n}-{i}.txt").exists() else ""
        _, leave_wait = am_start(rd("leave"))
        back_total, back_wait = am_start(rd("back"))
        frames, jank, p90, p99 = gfx(rd("gfx"))
        rows.append({"round": i, "leave_ms": leave_wait, "back_ms": back_total, "back_wait_ms": back_wait, "frames": frames, "jank_pct": jank, "p90_ms": p90, "p99_ms": p99})
    return rows


def summary(rows, limits):
    out, red = [], False
    keys = ("leave_ms", "back_ms", "jank_pct", "p99_ms")
    med = {k: median([r[k] for r in rows]) for k in keys}
    out.append("round\tleave_ms\tback_ms\tframes\tjank_pct\tp90_ms\tp99_ms")
    for r in rows:
        out.append("\t".join(str(r[k]) if r[k] is not None else "-" for k in ("round", "leave_ms", "back_ms", "frames", "jank_pct", "p90_ms", "p99_ms")))
    for k in keys:
        lim = limits.get(k)
        v = med[k]
        if v is None:
            out.append(f"{'FAIL' if lim is not None else 'INFO'} {k}: no number (the TV did not say)"); red |= lim is not None
        elif lim is None:
            out.append(f"INFO {k}: median {v:g}")
        else:
            ok = v <= lim
            red |= not ok
            out.append(f"{'PASS' if ok else 'FAIL'} {k}: median {v:g} (limit {lim:g})")
    if not rows:
        out.append("FAIL no round was measured"); red = True
    return red, out


def main(argv):
    if len(argv) < 2:
        print(__doc__); return 2
    names = {"--leave-ms": "leave_ms", "--back-ms": "back_ms", "--jank-pct": "jank_pct", "--p99-ms": "p99_ms"}
    limits, i = {}, 2
    while i < len(argv):
        if argv[i] in names and i + 1 < len(argv):
            limits[names[argv[i]]] = float(argv[i + 1]); i += 2
        else:
            print(f"unknown option {argv[i]}"); return 2
    red, out = summary(read(argv[1]), limits)
    print("\n".join(out))
    return 1 if red else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
