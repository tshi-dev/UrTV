#!/usr/bin/env python3
"""The automatic tester round's brain (tester/auto/round.sh calls it): everything that reads the TV's answers, so it can be tested without a TV
(tests/tester_auto_test.py feeds it saved answers). Nothing here talks to the TV, presses a key or writes outside the folder it is given.

    lib.py start-time FILE              «TotalTime: 523» of `am start -W` -> 523
    lib.py pss FILE                     `dumpsys meminfo <pkg>` -> the TOTAL PSS in KB
    lib.py jank FILE                    `dumpsys gfxinfo <pkg> framestats` -> frames, janky %, 90th/99th ms
    lib.py crashes FILE PKG,PKG         `logcat -b crash -d` -> how many fatal exceptions of those packages
    lib.py anrs FILE PKG,PKG SINCE      `dumpsys dropbox --print` -> ANR and crash entries of those packages at or after SINCE («2026-11-27 16:48:21»)
    lib.py frozen FILE PIDS REGEX MIN   `logcat -v threadtime -d` -> frozen-screen lines (a Choreographer skip of MIN frames or more, or REGEX)
    lib.py dreaming FILE                `dumpsys dreams` + the resumed activity -> yes|no
    lib.py muted FILE                   `dumpsys audio` -> yes|no (is STREAM_MUSIC muted)
    lib.py volume FILE                  `cmd media_session volume --stream 3 --get` -> the music volume number
    lib.py city FILE                    `saver.sh show` -> the city's name
    lib.py style FILE                   `saver.sh show` -> buttons|couch|tvos (or empty)
    lib.py adhan-state FILE PRAYER      `music.sh adhan` status -> both|sound|screen|none for that prayer
    lib.py focused FILE                 `uiautomator dump` XML -> the text of the focused node (empty when none)
    lib.py has-text FILE TEXT           `uiautomator dump` XML -> yes|no (is TEXT on the screen)
    lib.py box XML TEXT IN OUT          draw a box round TEXT's place on the screenshot IN, write OUT (needs Pillow); prints boxed|missing|nopillow
    lib.py report OUTDIR [PREVDIR]      metrics.tsv + steps.tsv of this round (+ the previous build's) -> OUTDIR/report.md; prints green|red

Thresholds are in tester/auto/thresholds.tsv (one line each: key, limit, how it is judged); a metric over its limit turns the report red.
"""
import csv
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def _read(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        return f.read()


# ---- the TV's answers -> numbers

def start_time(text):
    """`am start -W` says «TotalTime: 523» (ms to the first frame); a cold start is the one that matters. None when it said nothing."""
    m = re.search(r"TotalTime:\s*(\d+)", text)
    return int(m.group(1)) if m else None


def pss(text):
    """The app's total PSS in KB from `dumpsys meminfo <pkg>`: «TOTAL PSS:   123456 ...» (Android 10+) or the older «TOTAL   123456 ...» line."""
    m = re.search(r"TOTAL PSS:\s*(\d+)", text) or re.search(r"^\s*TOTAL\s+(\d+)", text, re.M)
    return int(m.group(1)) if m else None


def jank(text):
    """From `dumpsys gfxinfo <pkg> framestats` (after a reset and a walk): the frames drawn, the janky percentage and the 90th / 99th percentile frame time.
    The summary lines are used when they are there («Total frames rendered», «Janky frames: 12 (3.40%)», «90th percentile: 11ms»); else the framestats
    rows are read (FrameCompleted - IntendedVsync over a 16.67 ms budget). None for a number the answer does not hold."""
    out = {"frames": None, "janky_pct": None, "p90_ms": None, "p99_ms": None}
    m = re.search(r"Total frames rendered:\s*(\d+)", text)
    if m:
        out["frames"] = int(m.group(1))
    m = re.search(r"Janky frames:\s*(\d+)\s*\(([\d.]+)%\)", text)
    if m:
        out["janky_pct"] = float(m.group(2))
    for key, p in (("p90_ms", 90), ("p99_ms", 99)):
        m = re.search(rf"{p}th percentile:\s*(\d+)ms", text)
        if m:
            out[key] = int(m.group(1))
    rows = _framestats(text)
    if rows:
        if out["frames"] is None:
            out["frames"] = len(rows)
        if out["janky_pct"] is None:
            out["janky_pct"] = round(100.0 * sum(1 for d in rows if d > 16.67) / len(rows), 2)
        rows.sort()
        for key, p in (("p90_ms", 90), ("p99_ms", 99)):
            if out[key] is None:
                out[key] = round(rows[min(len(rows) - 1, int(len(rows) * p / 100))])
    return out


def _framestats(text):
    """the frame times (ms) of the ---PROFILEDATA--- block: the column named FrameCompleted minus IntendedVsync, ns -> ms; the rows that are not frames are left out"""
    m = re.search(r"---PROFILEDATA---\s*\n(.*?)---PROFILEDATA---", text, re.S)
    if not m:
        return []
    lines = [ln.strip().rstrip(",") for ln in m.group(1).splitlines() if ln.strip()]
    if not lines or "FrameCompleted" not in lines[0]:
        return []
    head = lines[0].split(",")
    try:
        a, b, flags = head.index("IntendedVsync"), head.index("FrameCompleted"), head.index("Flags")
    except ValueError:
        return []
    times = []
    for ln in lines[1:]:
        c = ln.split(",")
        if len(c) <= max(a, b, flags) or c[flags] != "0":   # a flag other than 0: a frame that was not a normal one (skipped, window layout)
            continue
        try:
            d = (int(c[b]) - int(c[a])) / 1e6
        except ValueError:
            continue
        if d > 0:
            times.append(d)
    return times


def crashes(text, pkgs):
    """«FATAL EXCEPTION» blocks of `logcat -b crash` whose «Process: <pkg>» is one of ours; returns the process names, one per crash"""
    found = []
    for blk in re.split(r"FATAL EXCEPTION", text)[1:]:
        m = re.search(r"Process:\s*([\w.]+)", blk)
        if m and m.group(1) in pkgs:
            found.append(m.group(1))
    return found


def anrs(text, pkgs, since):
    """The entries of `dumpsys dropbox --print` that are an app crash or an ANR of ours at or after `since`: a list of «time kind package»"""
    found = []
    for m in re.finditer(r"^(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d)\s+(data_app_(?:crash|anr)|system_app_(?:crash|anr))\b(.*?)(?=^\d{4}-\d\d-\d\d \d\d:\d\d:\d\d\s|\Z)",
                         text, re.M | re.S):
        when, kind, body = m.group(1), m.group(2), m.group(3)
        if when < since:
            continue
        for p in pkgs:
            if re.search(rf"Process:\s*{re.escape(p)}\b|ANR in {re.escape(p)}\b|^Package:\s*{re.escape(p)}\b", body, re.M):
                found.append(f"{when} {kind} {p}")
                break
    return found


def frozen(text, pids, regex, min_frames):
    """Frozen-screen lines of a threadtime logcat: «Choreographer: Skipped N frames» of one of our processes with N >= min_frames, or any line REGEX matches"""
    out = []
    rx = re.compile(regex) if regex else None
    for ln in text.splitlines():
        m = re.match(r"^\S+\s+\S+\s+(\d+)\s+\d+\s+\w\s+(\S+?)\s*:\s*(.*)$", ln)
        if m:
            pid, tag, msg = m.group(1), m.group(2), m.group(3)
            k = re.search(r"Skipped (\d+) frames", msg)
            if tag == "Choreographer" and k and pid in pids and int(k.group(1)) >= min_frames:
                out.append(ln.strip())
                continue
        if rx and rx.search(ln):
            out.append(ln.strip())
    return out


def dreaming(text):
    """`dumpsys dreams` (mCurrentDream=… / mCurrentDreamToken=…, null while none runs) and the resumed activity (DreamActivity): is a dream running"""
    return bool(re.search(r"DreamActivity|mCurrentDream(?:Token)?=[^n\s]", text))


def muted(text):
    """`dumpsys audio`: is STREAM_MUSIC muted («Muted: true» in its block). False when the answer holds no such block."""
    m = re.search(r"- STREAM_MUSIC:(.*?)(?=\n- STREAM_|\Z)", text, re.S)
    return bool(m and re.search(r"Muted:\s*true", m.group(1)))


def music_volume(text):
    """«volume is 7 in range [0..15]» of `cmd media_session volume --stream 3 --get` -> 7"""
    m = re.search(r"volume is (\d+)", text)
    return int(m.group(1)) if m else None


def city(text):
    """the city's name in `saver.sh show` («city=الرياض», or a line «city: الرياض» / «المدينة: الرياض»), else empty"""
    m = re.search(r"(?:^|[|\s])(?:city|المدينة)[ \t]*[=:][ \t]*([^|\n]+)", text, re.M)
    return m.group(1).strip() if m else ""


def settings_style(text):
    """the settings style in `saver.sh show` («settingsstyle=couch»): buttons, couch or tvos; empty when it is not said (the saved settings put it back)"""
    m = re.search(r"settingsstyle[ \t]*[=:][ \t]*(buttons|couch|tvos)", text)
    return m.group(1) if m else ""


def adhan_state(text, prayer):
    """From `music.sh adhan`'s status («prayer_sound=fajr:silent,dhuhr:…» and «prayer_screen=fajr:on,dhuhr:off,…»): the prayer's state as the settings name them:
    both, sound (sound only), screen (screen only) or none; empty when the status does not say"""
    snd = re.search(rf"prayer_sound=[^|\n]*?\b{prayer}:([\w-]*)", text)
    scr = re.search(rf"prayer_screen=[^|\n]*?\b{prayer}:(on|off)", text)
    if not snd or not scr:
        return ""
    sound, screen = snd.group(1) != "silent", scr.group(1) == "on"
    return "both" if sound and screen else "sound" if sound else "screen" if screen else "none"


def _nodes(xml):
    for m in re.finditer(r"<node\b([^>]*)>", xml):
        a = dict(re.findall(r'([\w-]+)="([^"]*)"', m.group(1)))
        b = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", a.get("bounds", ""))
        a["_box"] = tuple(int(x) for x in b.groups()) if b else None
        yield a


def _label(a):
    return (a.get("text") or a.get("content-desc") or "").strip()


def focused(xml):
    """the text of the focused node of a uiautomator dump, else of the first one inside it that has a text (a row's name sits in a child)"""
    nodes = list(_nodes(xml))
    for i, a in enumerate(nodes):
        if a.get("focused") == "true":
            if _label(a):
                return _label(a)
            box = a["_box"]
            for c in nodes[i + 1:]:
                if box and c["_box"] and box[0] <= c["_box"][0] and c["_box"][2] <= box[2] and box[1] <= c["_box"][1] and c["_box"][3] <= box[3] and _label(c):
                    return _label(c)
            return ""
    return ""


def has_text(xml, needle):
    return any(needle in _label(a) for a in _nodes(xml))


def box(xml, needle, src, dst):
    """Draw a box round the first node that holds `needle` on the screenshot `src`, write `dst`. Says boxed, missing (the text is not on the screen), unreadable (not a picture) or nopillow."""
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        return "nopillow"
    for a in _nodes(xml):
        if needle in _label(a) and a["_box"]:
            try:
                im = Image.open(src).convert("RGB")
            except (OSError, ValueError):
                return "unreadable"
            d = ImageDraw.Draw(im)
            x0, y0, x1, y1 = a["_box"]
            for k in range(4):
                d.rectangle([x0 - 6 - k, y0 - 6 - k, x1 + 6 + k, y1 + 6 + k], outline=(255, 64, 64))
            im.save(dst)
            return "boxed"
    return "missing"


# ---- the thresholds, the metrics, the report

def thresholds(path=None):
    """key -> (limit, how): how is «max» (red when over) or «min» (red when under) or «zero» (red when not 0); lines starting with # are notes"""
    out = {}
    with open(path or os.path.join(HERE, "thresholds.tsv"), encoding="utf-8") as f:
        for ln in f:
            if not ln.strip() or ln.startswith("#"):
                continue
            c = ln.rstrip("\n").split("\t")
            out[c[0]] = (float(c[1]), c[2], c[3] if len(c) > 3 else "")
    return out


def _tsv(path):
    if not os.path.isfile(path):
        return []
    with open(path, encoding="utf-8", newline="") as f:
        return [r for r in csv.reader(f, delimiter="\t") if r and not r[0].startswith("#")]


def judge(key, value, th):
    """None when no limit or no number; else True when the number is fine"""
    if value is None or key not in th:
        return None
    limit, how = th[key][0], th[key][1]
    return value <= limit if how == "max" else value >= limit if how == "min" else value == 0


def report(outdir, prevdir=None):
    """Write OUTDIR/report.md from OUTDIR/metrics.tsv (key, value, unit) and OUTDIR/steps.tsv (step, PASS|FAIL|SKIP, note); with PREVDIR's metrics, a column of
    the previous build's numbers and the change. Returns «green» or «red»: red for a metric over its limit and for a FAIL step."""
    th = thresholds()
    cur = {r[0]: r for r in _tsv(os.path.join(outdir, "metrics.tsv"))}
    prev = {r[0]: r for r in _tsv(os.path.join(prevdir, "metrics.tsv"))} if prevdir else {}
    steps = _tsv(os.path.join(outdir, "steps.tsv"))
    build = (_read(os.path.join(outdir, "build.txt")).strip() if os.path.isfile(os.path.join(outdir, "build.txt")) else os.path.basename(os.path.abspath(outdir)))
    bad = []
    lines = [f"# Automatic round: {build}", ""]
    lines += ["## Health metrics", "", "| metric | now | limit | previous | change | verdict |", "|---|---|---|---|---|---|"]
    for key, r in cur.items():
        try:
            v = float(r[1])
        except (ValueError, IndexError):
            v = None
        unit = r[2] if len(r) > 2 else ""
        ok = judge(key, v, th)
        verdict = "no limit" if ok is None else "ok" if ok else "RED"
        if ok is False:
            bad.append(f"{key} = {r[1]}{unit} (limit {th[key][1]} {th[key][0]:g})")
        p = prev.get(key)
        pv = None
        if p:
            try:
                pv = float(p[1])
            except (ValueError, IndexError):
                pv = None
        change = "" if v is None or pv is None else f"{v - pv:+g}"
        limit = f"{th[key][1]} {th[key][0]:g}" if key in th else ""
        lines.append(f"| {key} | {r[1]}{unit} | {limit} | {'' if p is None else p[1] + unit} | {change} | {verdict} |")
    if not cur:
        lines.append("| (no metric was measured) | | | | | |")
    lines += ["", "## Checks", "", "| step | result | what |", "|---|---|---|"]
    for r in steps:
        res = r[1] if len(r) > 1 else ""
        if res == "FAIL":
            bad.append(f"step {r[0]} failed")
        lines.append(f"| {r[0]} | {res} | {r[2] if len(r) > 2 else ''} |")
    if not steps:
        lines.append("| (no step ran) | | |")
    lines += ["", "## Verdict", "", ("RED: " + "; ".join(bad)) if bad else "GREEN: every metric within its limit, no step failed.", ""]
    with open(os.path.join(outdir, "report.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    return "red" if bad else "green"


def main(a):
    if len(a) < 2:
        sys.stdout.write(__doc__)
        return 2
    cmd = a[1]
    if cmd == "start-time":
        v = start_time(_read(a[2]))
        print("" if v is None else v)
    elif cmd == "pss":
        v = pss(_read(a[2]))
        print("" if v is None else v)
    elif cmd == "jank":
        j = jank(_read(a[2]))
        print("\t".join("" if j[k] is None else str(j[k]) for k in ("frames", "janky_pct", "p90_ms", "p99_ms")))
    elif cmd == "crashes":
        print(len(crashes(_read(a[2]), a[3].split(","))))
    elif cmd == "anrs":
        for x in anrs(_read(a[2]), a[3].split(","), a[4]):
            print(x)
    elif cmd == "frozen":
        for x in frozen(_read(a[2]), a[3].split(","), a[4], int(a[5])):
            print(x)
    elif cmd == "dreaming":
        print("yes" if dreaming(_read(a[2])) else "no")
    elif cmd == "muted":
        print("yes" if muted(_read(a[2])) else "no")
    elif cmd == "volume":
        v = music_volume(_read(a[2]))
        print("" if v is None else v)
    elif cmd == "city":
        print(city(_read(a[2])))
    elif cmd == "style":
        print(settings_style(_read(a[2])))
    elif cmd == "adhan-state":
        print(adhan_state(_read(a[2]), a[3]))
    elif cmd == "focused":
        print(focused(_read(a[2])))
    elif cmd == "has-text":
        print("yes" if has_text(_read(a[2]), a[3]) else "no")
    elif cmd == "box":
        print(box(_read(a[2]), a[3], a[4], a[5]))
    elif cmd == "report":
        print(report(a[2], a[3] if len(a) > 3 else None))
    else:
        sys.stdout.write(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
