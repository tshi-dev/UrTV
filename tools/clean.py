#!/usr/bin/env python3
"""Clean the synced setup reports (reports/*.csv, written by apps-script/sync.gs) into one record per real report.

    python3 tools/clean.py [reports/] [--out clean/]

- keeps only the lines UrTV's own allowlist lets into a report (ALLOWED_LINE: the same as optimize/share.py and the TV's SetupReport.java;
  copy it again when they change) and the two free-text parts after their headings, scrubbed once more (emails, phone numbers, network
  addresses, file paths, links);
- drops what is not a real report: rows that are not a UrTV report at all, tests (an emulator TV, or the owner's or the agent's words only
  "test"/«تجربة»), and duplicates (the same report sent twice: the same text once cleaned);
- normalises the TV (maker, model, device, Android, SDK) and UrTV's version (4.3.2, its build, or a git commit and its date).

Writes clean/reports.jsonl (one JSON object per report, oldest first) and clean/dropped.json (how many rows were dropped, and why).
Python standard library only.
"""
import argparse
import csv
import hashlib
import json
import re
import sys
from pathlib import Path

csv.field_size_limit(10 * 1024 * 1024)

# --- the allowlist (optimize/share.py ALLOWED_LINE, FREE_HEADINGS) ----------------------------------------------------------------
FREE_HEADINGS = ("Notes from the agent:", "From the owner:")
ALLOWED_LINE = re.compile(
    r"UrTV (problem|setup) report, |UrTV Player (problem report|feature request), |TV: |Computer: |Steps, minutes:seconds:$|Memory:$|Checks:$|Setup results: |ADB: "
    r"|Protected features on: |(Device|WebView|Home|Home switch|Screensaver|Versions|Extras):|Auto-start|Optional picks|!! |- |  (off|on): "
    r"|home hides|[\w-]+: \d+ off, |\(no steps\.log: an older run\.sh\)$|\(not measured\)$|\(no report\.md yet: the setup didn't finish\)$|\d+ min in all$|\[\d+:\d\d until it was run again\]$"
    r"|[A-Za-z0-9_.-]+ \d+:\d\d( STOPPED.*)?(, [A-Za-z0-9_.-]+ \d+:\d\d( STOPPED.*)?)*(, \[.*\])?$")
MAX_LINE = 400

KINDS = [("UrTV setup report, ", "setup"), ("UrTV problem report, ", "problem"),
         ("UrTV Player problem report, ", "player-problem"), ("UrTV Player feature request, ", "player-feature")]

# --- the second net over everything kept ---------------------------------------------------------------------------------------
SCRUB = [
    (re.compile(r"[\w.+-]+@[\w-]+(\.[\w-]+)+"), "<email>"),
    (re.compile(r"https?://\S+|www\.\S+"), "<link>"),
    (re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b"), "<ip>"),
    (re.compile(r"\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\b"), "<mac>"),
    (re.compile(r"(?:[A-Za-z]:\\|/(?:Users|home|root|data/data|sdcard|storage)/)[^\s,;)]*"), "<path>"),
    (re.compile(r"(?<![\w.])\+?\d[\d\s-]{7,}\d(?![\w.])"), "<phone>"),
]


def scrub(text):
    for rx, mark in SCRUB:
        text = rx.sub(mark, text)
    return text


# --- tests ------------------------------------------------------------------------------------------------------------------
TEST_WORDS = {"test", "testing", "tst", "تجربة", "تجربه", "تست", "اختبار"}
EMULATOR = re.compile(r"sdk_google|\bgeneric(_x86|_arm)?\b|emulator|ranchu|goldfish", re.I)

# --- normalising ------------------------------------------------------------------------------------------------------------
MAKERS = {"tcl": "TCL", "xiaomi": "Xiaomi", "mi": "Xiaomi", "sony": "Sony", "hisense": "Hisense", "samsung": "Samsung", "lg": "LG",
          "philips": "Philips", "skyworth": "Skyworth", "haier": "Haier", "changhong": "Changhong", "toshiba": "Toshiba", "sharp": "Sharp",
          "nvidia": "NVIDIA", "google": "Google", "amazon": "Amazon", "onn": "onn", "realme": "realme", "oneplus": "OnePlus"}
TV_LINE = re.compile(r"TV: (?P<name>.*?) \((?P<device>[^()]*)\), Android (?P<android>\S+) \(SDK (?P<sdk>[^)]*)\), firmware (?P<firmware>.*)$")
STEP = re.compile(r"\[(?P<gm>\d+):(?P<gs>\d\d) until it was run again\]"
                  r"|(?P<step>[A-Za-z0-9_.-]+) (?P<m>\d+):(?P<s>\d\d)(?P<stop> STOPPED(?P<cause>.*?))?(?=, (?:[A-Za-z0-9_.-]+ \d+:\d\d|\[)|$)")


def kind_of(first):
    for prefix, kind in KINDS:
        if first.startswith(prefix):
            return kind, first[len(prefix):]
    return None, None


def version_of(rest):
    """«2026-11-02, UrTV 4.3.2 (build 309)» → date, {version, build, commit, commit_date}"""
    date, _, ver = rest.partition(", UrTV ")
    out = {"version": "?", "build": None, "commit": None, "commit_date": None}
    ver = ver.strip()
    m = re.fullmatch(r"v?(\d+\.\d+(?:\.\d+)?)(?: \(build (\d+)\))?", ver)
    if m:
        out["version"], out["build"] = m.group(1), int(m.group(2)) if m.group(2) else None
    else:
        m = re.fullmatch(r"([0-9a-f]{7,40}) (\d{4}-\d{2}-\d{2})", ver)
        if m:
            out.update(version="git-" + m.group(1)[:7], commit=m.group(1)[:7], commit_date=m.group(2))
    return (date.strip() if re.fullmatch(r"\d{4}-\d{2}-\d{2}", date.strip()) else None), out


def tv_of(line, models):
    m = TV_LINE.match(line)
    if not m:
        return {"maker": "?", "model": "?", "device": "?", "android": "?", "sdk": None, "firmware": "?"}
    words = m.group("name").split()
    maker = MAKERS.get(words[0].casefold(), words[0]) if words else "?"
    model = " ".join(words[1:]) or "?"
    model = models.setdefault((maker, model.casefold()), model)   # the first spelling seen stands for every other case
    android = m.group("android")
    sdk = int(m.group("sdk")) if m.group("sdk").isdigit() else None
    return {"maker": maker, "model": model, "device": m.group("device").strip().lower() or "?",
            "android": android.split(".")[0] if android != "?" else "?", "sdk": sdk, "firmware": m.group("firmware").strip()}


def steps_of(line):
    steps, gaps = [], 0
    for m in STEP.finditer(line):
        if m.group("gm"):
            gaps += int(m.group("gm")) * 60 + int(m.group("gs"))
            continue
        cause = (m.group("cause") or "").strip()
        cause = re.sub(r"^(at |:)\s*", "", cause) if cause else None
        steps.append({"step": m.group("step"), "seconds": int(m.group("m")) * 60 + int(m.group("s")),
                      "stopped": bool(m.group("stop")), "cause": cause or (None if not m.group("stop") else "?")})
    return steps, gaps


def restrict(text):
    """(kept lines, free parts {heading: text}, how many lines were dropped)"""
    kept, free, dropped, heading = [], {}, 0, None
    for line in text.replace("\r\n", "\n").split("\n"):
        if line.strip() in FREE_HEADINGS:
            heading = line.strip()
            free[heading] = []
            continue
        if heading:
            free[heading].append(line)
        elif not line.strip():
            continue
        elif ALLOWED_LINE.match(line) and len(line) <= MAX_LINE:
            kept.append(line)
        else:
            dropped += 1
    return kept, {h: scrub("\n".join(v).strip()) for h, v in free.items()}, dropped


def parse(row, models):
    """one sheet row → (record, None) or (None, why it was dropped)"""
    text = (row.get("report") or "").strip()
    kept, free, dropped = restrict(text)
    if not kept:
        return None, "not a report"
    kind, rest = kind_of(kept[0])
    if not kind:
        return None, "not a report"
    date, ver = version_of(rest)
    rec = {"row": int(row["row"]) if str(row.get("row", "")).isdigit() else None, "timestamp": row.get("timestamp", ""), "kind": kind,
           "date": date, **ver, "tv": None, "computer": None, "agent": None, "steps": [], "gaps_seconds": 0, "total_min": None,
           "adb": None, "errors": [], "versions": None, "notes": free.get("Notes from the agent:", ""), "owner": free.get("From the owner:", ""),
           "lines_dropped": dropped}
    for line in kept[1:]:
        if line.startswith("TV: "):
            rec["tv"] = tv_of(line, models)
        elif line.startswith("Computer: "):
            comp, _, agent = line[len("Computer: "):].partition(", agent: ")
            rec["computer"], rec["agent"] = comp.strip(), (agent.strip() or None)
        elif re.fullmatch(r"\d+ min in all", line):
            rec["total_min"] = int(line.split()[0])
        elif line.startswith("ADB: "):
            m = re.match(r"ADB: (\d+) dropped connections \(retried\), (\d+) resumed uploads", line)
            rec["adb"] = {"drops": int(m.group(1)), "resumed": int(m.group(2))} if m else None
        elif re.match(r"(- )?!! ", line):
            rec["errors"].append(scrub(re.sub(r"^(- )?!! ", "", line)).strip())
        elif line.startswith("Versions: "):
            m = re.match(r"Versions: screensaver (.+?), home (.+)$", line)
            rec["versions"] = {"screensaver": m.group(1), "home": m.group(2)} if m else None
        elif STEP.match(line) and not rec["steps"]:
            rec["steps"], rec["gaps_seconds"] = steps_of(line)
    if rec["tv"] is None:
        rec["tv"] = tv_of("", models)
    words = {w.strip().strip(".!").casefold() for w in (rec["owner"], rec["notes"]) if w.strip()}
    if words and words <= TEST_WORDS:
        return None, "test"
    if EMULATOR.search(" ".join(str(rec["tv"].get(k, "")) for k in ("model", "device", "firmware"))):
        return None, "test"
    return rec, None


def fingerprint(rec):
    """the same report sent twice: everything but where and when it arrived"""
    key = {k: v for k, v in rec.items() if k not in ("row", "timestamp")}
    return hashlib.sha256(json.dumps(key, sort_keys=True, ensure_ascii=False).encode()).hexdigest()


def read_rows(src):
    files = sorted(Path(src).glob("*.csv")) if Path(src).is_dir() else [Path(src)]
    for f in files:
        with open(f, newline="", encoding="utf-8") as fh:
            yield from csv.DictReader(fh)


def clean(rows):
    """(records oldest first, {reason: count})"""
    models, seen, out = {}, set(), []
    dropped = {"not a report": 0, "test": 0, "duplicate": 0}
    rows = sorted(rows, key=lambda r: (r.get("timestamp") or "", int(r["row"]) if str(r.get("row", "")).isdigit() else 0))
    for row in rows:
        rec, why = parse(row, models)
        if why:
            dropped[why] += 1
            continue
        fp = fingerprint(rec)
        if fp in seen:
            dropped["duplicate"] += 1
            continue
        seen.add(fp)
        out.append(rec)
    return out, dropped


def main():
    a = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    a.add_argument("src", nargs="?", default="reports", help="the folder of synced CSV files (or one file)")
    a.add_argument("--out", default="clean", help="where reports.jsonl and dropped.json go")
    args = a.parse_args()
    recs, dropped = clean(read_rows(args.src))
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    with open(out / "reports.jsonl", "w", encoding="utf-8") as fh:
        for r in recs:
            fh.write(json.dumps(r, ensure_ascii=False) + "\n")
    (out / "dropped.json").write_text(json.dumps({"kept": len(recs), "dropped": dropped,
                                                  "lines_dropped": sum(r["lines_dropped"] for r in recs)}, ensure_ascii=False, indent=1) + "\n",
                                      encoding="utf-8")
    print(f"{len(recs)} reports kept; dropped: " + ", ".join(f"{k} {v}" for k, v in dropped.items()))
    return 0


if __name__ == "__main__":
    sys.exit(main())
