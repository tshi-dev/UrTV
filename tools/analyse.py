#!/usr/bin/env python3
"""What the cleaned reports say (clean/reports.jsonl from tools/clean.py), as Markdown for people and JSON for the nightly AI job.

    python3 tools/analyse.py [clean/reports.jsonl] [--since YYYY-MM-DD] [--min N] [--md analysis.md] [--json analysis.json]

1. Stops: every step that STOPPED, by step × TV model × Android × UrTV version.
2. Causes: the stop causes and the scripts' «!!» lines, normalised (numbers, packages, quotes), each new (first seen on or after --since;
   default: the newest report's day) or repeated.
3. Regressions by version: a step's stop rate in a version against the version before it (releases by number; git builds by commit
   date), flagged when it rises by 25 points or more and the version has at least --min reports with that step (default 3).
4. Timings: each finished step's median and 90th percentile, and the whole setup's median minutes, by version.
Python standard library only.
"""
import argparse
import json
import re
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

RISE = 0.25


def load(path):
    with open(path, encoding="utf-8") as fh:
        return [json.loads(l) for l in fh if l.strip()]


def model_of(r):
    tv = r.get("tv") or {}
    return f"{tv.get('maker', '?')} {tv.get('model', '?')}".strip()


def version_key(v, recs):
    """releases by number, then git builds by their commit date"""
    m = re.fullmatch(r"(\d+)\.(\d+)(?:\.(\d+))?", v)
    if m:
        return (0, tuple(int(x or 0) for x in m.groups()), "")
    dates = sorted(r.get("commit_date") or "" for r in recs if r.get("version") == v)
    return (1, (), dates[0] if dates else "") if v.startswith("git-") else (2, (), v)


def cause_key(text):
    t = text.strip().lower()
    t = re.sub(r"[«»\"'`]", "", t)
    t = re.sub(r"\b[a-z][a-z0-9_]*(\.[a-z0-9_]+){2,}\b", "<pkg>", t)   # com.vendor.thing
    t = re.sub(r"\d+", "#", t)
    return re.sub(r"\s+", " ", t).strip(" .")


def day_of(r):
    return r.get("date") or (r.get("timestamp") or "")[:10]


def percentile(v, p):
    v = sorted(v)
    if not v:
        return None
    k = (len(v) - 1) * p
    lo, hi = int(k), min(int(k) + 1, len(v) - 1)
    return round(v[lo] + (v[hi] - v[lo]) * (k - lo), 1)


def analyse(recs, since=None, min_n=3):
    setups = [r for r in recs if r.get("steps")]
    versions = sorted({r.get("version", "?") for r in recs}, key=lambda v: version_key(v, recs))
    since = since or max((day_of(r) for r in recs), default=None)

    stops = Counter()
    for r in setups:
        for s in r["steps"]:
            if s["stopped"]:
                stops[(s["step"], model_of(r), (r.get("tv") or {}).get("android", "?"), r.get("version", "?"))] += 1

    causes = {}
    for r in recs:
        found = [s["cause"] for s in r.get("steps", []) if s["stopped"] and s.get("cause")] + list(r.get("errors", []))
        for c in dict.fromkeys(cause_key(x) for x in found if x):
            e = causes.setdefault(c, {"cause": c, "reports": 0, "first": day_of(r), "last": day_of(r), "models": set(), "versions": set()})
            e["reports"] += 1
            e["first"], e["last"] = min(e["first"], day_of(r)), max(e["last"], day_of(r))
            e["models"].add(model_of(r))
            e["versions"].add(r.get("version", "?"))
    for e in causes.values():
        e["status"] = "new" if since and e["first"] >= since else "repeated"
        e["models"], e["versions"] = sorted(e["models"]), sorted(e["versions"], key=lambda v: version_key(v, recs))

    tried, stopped = defaultdict(Counter), defaultdict(Counter)   # step -> version -> reports
    for r in setups:
        for name in {s["step"] for s in r["steps"]}:
            tried[name][r.get("version", "?")] += 1
        for name in {s["step"] for s in r["steps"] if s["stopped"]}:
            stopped[name][r.get("version", "?")] += 1
    rates, regressions = {}, []
    for name in sorted(tried):
        row = {v: {"tried": tried[name][v], "stopped": stopped[name][v], "rate": round(stopped[name][v] / tried[name][v], 3)}
               for v in versions if tried[name][v]}
        rates[name] = row
        for family in (0, 1):
            seq = [v for v in versions if v in row and version_key(v, recs)[0] == family]
            for prev, cur in zip(seq, seq[1:]):
                if row[cur]["tried"] >= min_n and row[cur]["rate"] - row[prev]["rate"] >= RISE:
                    regressions.append({"step": name, "from": prev, "to": cur, "rate_from": row[prev]["rate"], "rate_to": row[cur]["rate"],
                                        "tried": row[cur]["tried"]})

    timing = defaultdict(list)
    for r in setups:
        for s in r["steps"]:
            if not s["stopped"]:
                timing[s["step"]].append(s["seconds"])
    timings = {k: {"n": len(v), "median_s": statistics.median(v), "p90_s": percentile(v, 0.9)} for k, v in sorted(timing.items())}
    totals = defaultdict(list)
    for r in setups:
        if r.get("total_min") is not None:
            totals[r.get("version", "?")].append(r["total_min"])
    total_by_version = {v: {"n": len(totals[v]), "median_min": statistics.median(totals[v])} for v in versions if totals[v]}

    return {
        "reports": len(recs), "since": since,
        "by_kind": dict(Counter(r.get("kind", "?") for r in recs)),
        "by_version": {v: sum(1 for r in recs if r.get("version") == v) for v in versions},
        "stops": [{"step": k[0], "model": k[1], "android": k[2], "version": k[3], "count": n}
                  for k, n in sorted(stops.items(), key=lambda kv: (-kv[1], kv[0]))],
        "causes": sorted(causes.values(), key=lambda e: (e["status"] != "new", -e["reports"], e["cause"])),
        "stop_rates": rates, "regressions": regressions,
        "timings": timings, "total_by_version": total_by_version,
    }


def markdown(a):
    out = [f"# UrTV setup reports: {a['reports']} reports", "",
           "By kind: " + ", ".join(f"{k} {v}" for k, v in sorted(a["by_kind"].items())),
           "By version: " + ", ".join(f"{k} {v}" for k, v in a["by_version"].items()), ""]
    out += ["## Stops (step × model × Android × version)", "", "| step | model | Android | version | stops |", "|---|---|---|---|---|"]
    out += [f"| {s['step']} | {s['model']} | {s['android']} | {s['version']} | {s['count']} |" for s in a["stops"]] or ["| — | | | | |"]
    out += ["", f"## Causes (new = first seen on or after {a['since']})", "", "| | cause | reports | first | last | models | versions |", "|---|---|---|---|---|---|---|"]
    out += [f"| {'**new**' if c['status'] == 'new' else 'repeated'} | {c['cause']} | {c['reports']} | {c['first']} | {c['last']} | "
            f"{', '.join(c['models'])} | {', '.join(c['versions'])} |" for c in a["causes"]] or ["| — | | | | | | |"]
    out += ["", "## Regressions by version", ""]
    out += [f"- **{g['step']}**: stops in {g['rate_to']:.0%} of {g['tried']} reports in {g['to']}, {g['rate_from']:.0%} in {g['from']}"
            for g in a["regressions"]] or ["None found."]
    out += ["", "## Timings (finished steps)", "", "| step | n | median | p90 |", "|---|---|---|---|"]
    out += [f"| {k} | {t['n']} | {t['median_s']} s | {t['p90_s']} s |" for k, t in a["timings"].items()]
    out += ["", "Whole setup, median minutes by version: " + (", ".join(f"{v} {t['median_min']} ({t['n']})" for v, t in a["total_by_version"].items()) or "—"), ""]
    return "\n".join(out)


def main():
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("src", nargs="?", default="clean/reports.jsonl")
    p.add_argument("--since", help="causes first seen on or after this day are new (default: the newest report's day)")
    p.add_argument("--min", type=int, default=3, dest="min_n", help="reports a version needs before a regression is flagged")
    p.add_argument("--md", help="write the Markdown here (default: print it)")
    p.add_argument("--json", help="write the analysis as JSON here")
    args = p.parse_args()
    a = analyse(load(args.src), args.since, args.min_n)
    md = markdown(a)
    if args.md:
        Path(args.md).write_text(md, encoding="utf-8")
    else:
        print(md)
    if args.json:
        Path(args.json).write_text(json.dumps(a, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
