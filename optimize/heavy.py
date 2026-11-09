#!/usr/bin/env python3
"""Is our screensaver heavy for this TV? Reads the before/after table optimize/measure.sh writes and compares the
two screensaver rows: «before-screensaver» (the TV's own, measured before setup) and «after-screensaver» (ours).

    heavy.py optimize/run/summary.md            prints one `!!` line when it is heavy, nothing otherwise (exit 0 either way)
    heavy.py --status optimize/run/summary.md   always one line: heavy (the `!!` line), ok with both readings, or what was not measured

Heavy when either:
  RAM  free RAM with ours is more than DROP_MB below the stock one's, or below FLOOR_MB (see the note on the floor).
  CPU  CPU idle with ours is more than IDLE_POINTS below the stock one's.
Anything not measured (a row missing, «not measured …», no number) is no evidence: no verdict from that part.

Why these numbers. The table's free RAM is MemAvailable in MB, read 60 s into the screensaver; CPU idle is one `top`
snapshot averaged over the cores. Field readings put ours at 120-250 MB of free RAM (docs/OPTIMIZE.md), so 200 MB below
the stock one is the top of that range: normal cost is left alone, only an unusually big one is flagged. 400 MB free is
about where these 1.7-2.3 GB sets start to drop cached apps and the home screen reloads. The floor only counts when ours
is also at least NOISE_MB below the stock one: two readings of the same state differ by tens of MB, and a TV that is
low with the stock screensaver too is not made slow by ours. 30 points of idle CPU is far outside the spread of one
snapshot (the report that started this: 92% idle with the stock one, 34% with ours = 58 points).
"""
import re
import sys

FLOOR_MB = 400
DROP_MB = 200
NOISE_MB = 50
IDLE_POINTS = 30
OFF = "optimize/saver.sh off"


def rows(text):
    """label -> (free RAM MB or None, CPU idle % or None) for every row of the table."""
    out = {}
    for line in text.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 5 or not line.lstrip().startswith("|"):
            continue
        # 4.3 S10: a reading with decimals (92.5%, 1830.4) is a reading too, not «not measured»
        ram = float(cells[1]) if re.fullmatch(r"\d+(\.\d+)?", cells[1]) else None
        idle = float(cells[4][:-1]) if re.fullmatch(r"\d+(\.\d+)?%", cells[4]) else None
        out[cells[0]] = (ram, idle)
    return out


def n(x):
    """1830.0 -> 1830, 92.5 -> 92.5"""
    return f"{x:g}"


def verdict(text):
    """The `!!` line, or None when the screensaver is not heavy or the two readings are not both there."""
    table = rows(text)
    stock, ours = table.get("before-screensaver"), table.get("after-screensaver")
    if not stock or not ours:
        return None
    numbers = []
    (sram, sidle), (oram, oidle) = stock, ours
    if sram is not None and oram is not None:
        drop = sram - oram
        if drop > DROP_MB or (oram < FLOOR_MB and drop >= NOISE_MB):
            numbers.append(f"free RAM {n(oram)} MB with ours, {n(sram)} MB with the TV's own")
    if sidle is not None and oidle is not None and sidle - oidle > IDLE_POINTS:
        numbers.append(f"CPU idle {n(oidle)}% with ours, {n(sidle)}% with the TV's own")
    if not numbers:
        return None
    return (f"!! the screensaver may slow this TV: {', '.join(numbers)}; "
            f"tell the person they can turn it off: {OFF}")


def status(text):
    """One line whatever the table says: the `!!` line, «ok» with both readings, or what was not measured."""
    line = verdict(text)
    if line:
        return line
    table = rows(text)
    stock, ours = table.get("before-screensaver"), table.get("after-screensaver")
    missing = [k for k, v in (("before-screensaver", stock), ("after-screensaver", ours)) if not v]
    if missing:
        return f"screensaver weight: not measured ({', '.join(missing)} missing)"
    parts = []
    if stock[0] is not None and ours[0] is not None:
        parts.append(f"free RAM {n(ours[0])} MB with ours, {n(stock[0])} MB with the TV's own")
    if stock[1] is not None and ours[1] is not None:
        parts.append(f"CPU idle {n(ours[1])}% with ours, {n(stock[1])}% with the TV's own")
    if not parts:
        return "screensaver weight: not measured (no number in the two rows)"
    return "screensaver weight: ok (" + ", ".join(parts) + ")"


if __name__ == "__main__":
    args = sys.argv[1:]
    want_status = bool(args) and args[0] == "--status"
    if want_status:
        args = args[1:]
    try:
        text = open(args[0], encoding="utf-8", errors="replace").read() if args else ""
    except OSError:
        text = None
    if want_status:
        print(status(text) if text is not None else "screensaver weight: not measured (no table)")
    else:
        line = verdict(text) if text else None
        if line:
            print(line)
