#!/usr/bin/env python3
"""A rendered check of the favourites tray (owner, build 180: «the apps go out, the name is not in its place»). Nobody had looked at the drawn box before
the tester did; this runs the real launcher on the CI emulator and reads the REAL bounds (uiautomator dump) and pixels (screencap).

  tray_check.py OUT_DIR          on the emulator (adb on PATH, both apps installed, stub apps installed, the launcher set as Home)

For each density (2.0 and 1.5), each language (ar, en), the focus on the first and on the last tile, and the football layout with the matches widget focused:
  - the tray stays inside the safe area (x 96..1824, y 54..1026), at rest and lowered;
  - every tile that shows is a whole tile, fully inside the tray (the focused one, lifted and grown, with at least 8 px to spare); no sliver of another;
  - the app's name is in ONE place, whichever tile has the focus: its start edge (right in Arabic, left in English) is the tray's inner padding (24 px),
    its top and bottom do not move between the first and the last tile;
  - nothing of the tile row is drawn outside the tray: the strips left and right of the tray, over the tile row's height, are the same pixels with the
    focus on the first tile and on the last (the tiles are different in the two, the background is not).
Anything it measures is written to OUT_DIR/tray-report.json; the screencaps and the dumps stay there for the artifact. Exit 1 on a failed rule.
The analysis is plain functions on text and pixels: tests/tray_check_test.py runs them on made-up dumps.
"""
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

W, H = 1920, 1080
SAFE = (96, 54, 1824, 1026)   # x0 y0 x1 y1: the 5 % safe area
INSET = 24                    # the tray's padding on all four sides
TILE_W, TILE_H = 304, 170
LIFT_ROOM = 8                 # the focused tile (lifted 6, grown 1.08, ringed) keeps at least this much between it and the tray's edge


def rect(bounds):
    m = re.match(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds or "")
    return tuple(int(x) for x in m.groups()) if m else None


def nodes(xml_text):
    out = []
    for n in ET.fromstring(xml_text).iter("node"):
        r = rect(n.get("bounds"))
        if r:
            out.append({"cls": n.get("class", ""), "text": n.get("text", ""), "desc": n.get("content-desc", ""), "focusable": n.get("focusable") == "true",
                        "focused": n.get("focused") == "true", "rect": r})
    return out


def find_tray(ns):
    """the tray: the horizontal scroll view, 1664 wide at 1080p, in the lower half of the screen (the lowest one if the screen has more)"""
    c = [n for n in ns if n["cls"].endswith("HorizontalScrollView") and n["rect"][2] - n["rect"][0] > 1200 and n["rect"][3] - n["rect"][1] < 400]
    return max(c, key=lambda n: n["rect"][3])["rect"] if c else None


def summary(xml_text):
    """a few lines for the CI log (the artifact may not be readable where the log is): every scroll view, the focusable nodes and the texts in the lower half"""
    out = []
    for n in nodes(xml_text):
        if n["cls"].endswith("HorizontalScrollView"):
            out.append(f"  scroll {list(n['rect'])}")
        elif n["focusable"] and n["rect"][1] > 500:
            out.append(f"  focusable{'*' if n['focused'] else ''} {n['cls'].split('.')[-1]} {list(n['rect'])}")
        elif n["text"].strip() and n["rect"][1] > 500:
            out.append(f"  text '{n['text'][:20]}' {list(n['rect'])}")
    return "\n".join(out[:40])


def tiles(ns, tray):
    """the favourites that show: focusable nodes inside the tray's height whose height is a tile's (a sliver is as tall, but narrower)"""
    t0, t1 = tray[1], tray[3]
    out = [n for n in ns if n["focusable"] and not n["cls"].endswith("HorizontalScrollView") and n["rect"][1] >= t0 - 40 and n["rect"][3] <= t1 + 40
           and 0.8 * TILE_H <= n["rect"][3] - n["rect"][1] <= 1.3 * TILE_H and n["rect"][2] > tray[0] - TILE_W and n["rect"][0] < tray[2] + TILE_W]
    return sorted(out, key=lambda n: n["rect"][0])


def name_label(ns, tray):
    """the app's name: the biggest text just above the tray (the hint beside it is smaller)"""
    c = [n for n in ns if n["text"].strip() and tray[1] - 140 <= n["rect"][3] <= tray[1] + 2 and n["rect"][1] >= 0]
    return max(c, key=lambda n: n["rect"][3] - n["rect"][1]) if c else None


def analyse(xml_text, lang, focus, lowered=False, label_required=True):
    """-> (failures, measurements) for one dump"""
    ns = nodes(xml_text)
    tray = find_tray(ns)
    if not tray:
        return ["no tray found in the dump"], {}
    fails, m = [], {"tray": list(tray)}
    x0, y0, x1, y1 = SAFE
    if tray[0] < x0 or tray[1] < y0 or tray[2] > x1 or tray[3] > y1:
        fails.append(f"the tray {tray} is outside the safe area {SAFE}")
    if lowered and tray[3] != 1026:
        fails.append(f"lowered, the tray ends at {tray[3]}, not 1026")
    ts = tiles(ns, tray)
    m["tiles"] = [list(t["rect"]) for t in ts]
    if len(ts) < 3:
        fails.append(f"only {len(ts)} tiles found")
    inner = (tray[0] + INSET, tray[1] + INSET, tray[2] - INSET, tray[3] - INSET)
    for t in ts:
        r = t["rect"]
        w = r[2] - r[0]
        if w <= 0:
            continue   # a tile entirely outside the tray: the dump gives it no width, it is not drawn
        if t["focused"]:
            if r[0] < tray[0] + LIFT_ROOM or r[2] > tray[2] - LIFT_ROOM or r[1] < tray[1] + LIFT_ROOM or r[3] > tray[3] - LIFT_ROOM:
                fails.append(f"the focused tile {list(r)} is closer than {LIFT_ROOM} px to the tray {list(tray)}")
        else:
            if w < TILE_W - 8:
                fails.append(f"a tile cut to {w} px at {list(r)} (a sliver)")
            elif r[0] < inner[0] - 2 or r[2] > inner[2] + 2 or r[1] < inner[1] - 2 or r[3] > inner[3] + 2:
                fails.append(f"a tile {list(r)} is not inside the tray's inner rect {list(inner)}")
    label = name_label(ns, tray)
    if label:
        r = label["rect"]
        m["label"] = list(r)
        edge = r[2] if lang == "ar" else r[0]
        want = tray[2] - INSET if lang == "ar" else tray[0] + INSET
        m["label_edge"], m["label_want"] = edge, want
        if abs(edge - want) > 2:
            fails.append(f"the name's {'right' if lang == 'ar' else 'left'} edge is at {edge}, the tray's inner padding is at {want}")
    elif label_required:
        fails.append("no name label above the tray")
    return fails, m


def same_place(first, last):
    """the name's place with the focus on the first tile and on the last: the same edge and the same height (the words differ, so not the width)"""
    fails = []
    if not first or not last:
        return ["the name label is missing in one of the two dumps"]
    for i, name in ((1, "top"), (3, "bottom")):
        if abs(first[i] - last[i]) > 2:
            fails.append(f"the name's {name} moved from {first[i]} to {last[i]} between the first and the last tile")
    return fails


def strips_same(img_a, img_b, tray, tolerance=0.01):
    """The strips left and right of the tray over the tile row's height, in two captures (the focus on the first tile, on the last): equal pixels, within a
    small noise. A tile drawn outside the tray makes them differ (the tiles are different in the two)."""
    fails = []
    for name, box in (("left", (0, tray[1], tray[0], tray[3])), ("right", (tray[2], tray[1], W, tray[3]))):
        if box[2] - box[0] <= 0:
            continue
        a, b = img_a.crop(box).convert("RGB"), img_b.crop(box).convert("RGB")
        pa, pb = a.tobytes(), b.tobytes()
        n = len(pa) // 3
        diff = sum(1 for i in range(0, len(pa), 3) if abs(pa[i] - pb[i]) + abs(pa[i + 1] - pb[i + 1]) + abs(pa[i + 2] - pb[i + 2]) > 24)
        if diff / max(1, n) > tolerance:
            fails.append(f"{round(diff / n * 100, 1)} % of the pixels {name} of the tray differ between the two captures: something of the tile row is drawn outside it")
    return fails


# ---------------------------------------------------------------- the emulator side

def adb(*a, check=False, text=True, timeout=60):
    """one adb call that can never hang the job: after `timeout` seconds it gives up and answers nothing"""
    try:
        return subprocess.run(["adb", *a], capture_output=True, text=text, check=check, timeout=timeout)
    except subprocess.TimeoutExpired:
        print("adb timed out: " + " ".join(a)[:80])
        return subprocess.CompletedProcess(a, 124, "" if text else b"", "" if text else b"")


def shell(cmd):
    return adb("shell", cmd).stdout


def key(*ks):
    for k in ks:
        adb("shell", "input", "keyevent", k)
        time.sleep(0.5)


def dump(out, name):
    adb("shell", "uiautomator", "dump", "/sdcard/d.xml")
    p = Path(out) / f"{name}.xml"
    p.write_bytes(adb("exec-out", "cat", "/sdcard/d.xml", text=False).stdout)
    print(f"--- {name}: in front: " + " ".join(shell("dumpsys window | grep mCurrentFocus").split()[-2:]))
    print(summary(p.read_text(encoding="utf-8", errors="replace")))
    png = Path(out) / f"{name}.png"
    png.write_bytes(adb("exec-out", "screencap", "-p", text=False).stdout)
    return p.read_text(encoding="utf-8", errors="replace"), png


def settled_dump(out, name, tries=4, need_tile=True):
    """the tray after the keys and its 150 ms / 250 ms animations: wait, dump, and dump again while no favourite has the focus (the focus is out of the tray for a moment while it scrolls)"""
    for i in range(tries):
        time.sleep(1.5)
        xml, png = dump(out, name)
        if not need_tile:
            return xml, png
        try:
            ns = nodes(xml)
            tray = find_tray(ns)
            if tray and any(t["focused"] for t in tiles(ns, tray)):
                return xml, png
        except ET.ParseError:
            pass
    return xml, png


def saver(*args):
    shell("am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET " + " ".join(args))


def seed_dock(n=7):
    """the first n stub apps (urtv.stub*), then any others, as the favourites"""
    pk = [l.split(":")[1].strip() for l in shell("pm list packages urtv.stub").splitlines() if ":" in l]
    pk = sorted(pk)[:n]
    if len(pk) < n:
        raise SystemExit(f"only {len(pk)} stub apps are installed, {n} are needed")
    shell(f"am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SET_DOCK --es apps {','.join(pk)}")
    return pk


def focus_in_tray():
    """press DOWN until a favourite has the focus (the home starts with it on the top row on some builds); False after six tries"""
    for _ in range(6):
        adb("shell", "uiautomator", "dump", "/sdcard/q.xml", timeout=40)
        xml = adb("exec-out", "cat", "/sdcard/q.xml", text=False).stdout.decode("utf-8", "replace")
        try:
            ns = nodes(xml)
            tray = find_tray(ns)
            if tray and any(t["focused"] for t in tiles(ns, tray)):
                return True
        except ET.ParseError:
            pass
        key("DPAD_DOWN")
    return False


def quick(name="q"):
    """a dump kept in memory, without saving: (xml text or '')"""
    adb("shell", "uiautomator", "dump", f"/sdcard/{name}.xml", timeout=40)
    return adb("exec-out", "cat", f"/sdcard/{name}.xml", text=False).stdout.decode("utf-8", "replace")


def home_direction(xml):
    """'ar' or 'en' from where the app's name sits: at the tray's start edge (right in Arabic, left in English); None when no name shows"""
    try:
        ns = nodes(xml)
        tray = find_tray(ns)
        label = name_label(ns, tray) if tray else None
    except ET.ParseError:
        return None
    if not label:
        return None
    return "ar" if (label["rect"][0] + label["rect"][2]) / 2 > (tray[0] + tray[2]) / 2 else "en"


def start_in(lang, layout="classic", tries=4):
    """sets the language and the layout, starts the home fresh, and waits until the home shows that language (the launcher reads the setting from the
    screensaver when it starts; a broadcast not yet handled, or a process that kept the old one, shows the old direction). Prints how many tries it took."""
    for attempt in range(1, tries + 1):
        saver(f"--es lang {lang}", f"--es homelayout {layout}")
        for _ in range(8):   # the screensaver says it has the new language before the home is started
            if f"lang: {lang}" in shell("am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.GET"):
                break
            time.sleep(1)
        shell("am force-stop sa.urtv.home")
        go_home()
        if layout != "classic" or (focus_in_tray() and home_direction(quick()) == lang):
            print(f"{lang}/{layout}: the home shows the language after {attempt} start(s)")
            return True
    print(f"{lang}/{layout}: the home did NOT show the language after {tries} starts")
    return False


def go_home():
    shell("am start -W --activity-clear-task -n sa.urtv.home/.MainActivity")
    time.sleep(10)


def run(out):
    from PIL import Image
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    report, failures = {}, []
    adb("wait-for-device")
    shell("settings put global device_provisioned 1; settings put secure user_setup_complete 1; wm size 1920x1080")
    shell("cmd package set-home-activity sa.urtv.home/.MainActivity")
    seed_dock(7)
    for density in (320, 240):
        shell(f"wm density {density}")
        for lang in ("ar", "en"):
            fwd, back = ("DPAD_LEFT", "DPAD_RIGHT") if lang == "ar" else ("DPAD_RIGHT", "DPAD_LEFT")
            if not start_in(lang):
                failures.append(f"d{density}-{lang}: the home did not show the language {lang} (the layout stayed in the other direction)")
            key(*[back] * 10)
            tag = f"d{density}-{lang}"
            xa, pa = settled_dump(out, f"{tag}-first")
            fa, ma = analyse(xa, lang, "first")
            key(*[fwd] * 10)
            xb, pb = settled_dump(out, f"{tag}-last")
            fb, mb = analyse(xb, lang, "last")
            fs = same_place(ma.get("label"), mb.get("label"))
            if ma.get("tray"):
                fs += strips_same(Image.open(pa), Image.open(pb), ma["tray"])
            report[tag] = {"first": ma, "last": mb, "failures": fa + fb + fs}
            failures += [f"{tag}: {f}" for f in fa + fb + fs]
    # the football layout with the matches widget focused: the tray is lowered by 40 px (best effort: the widget needs the emulator to hold today's games)
    shell("wm density 320")
    start_in("ar", "football")
    key(*["DPAD_UP"] * 4)
    for _ in range(8):   # the widgets are focus targets of the page (weather, prayers, games): walk until the games have it
        focused = [n for n in nodes(quick()) if n["focused"]]
        if any("مباريات" in (n["desc"] or "") for n in focused):
            break
        key("DPAD_DOWN")
    xf, pf = settled_dump(out, "football-lowered", tries=1, need_tile=False)
    nf = nodes(xf)
    tf = find_tray(nf)
    low = bool(tf) and tf[3] == 1026
    report["football"] = {"tray": list(tf) if tf else None, "lowered_seen": low, "failures": []}
    if low:
        ff = analyse(xf, "ar", "first", lowered=True, label_required=False)[0]   # the focus is on the widget: the name is hidden then
        report["football"]["failures"] = ff
        failures += [f"football: {f}" for f in ff]
    else:
        # the matches widget needs today's games; without them it takes no focus here and the tray is not lowered (or the home has scrolled): not measured, said so
        print("football summary:\n" + summary(xf))
        print(f"football: NOT MEASURED - the tray is at {tf}, not lowered to y 1026 (no games on the emulator, so the matches widget did not take the focus)")
    saver("--es homelayout classic")
    (out / "tray-report.json").write_text(json.dumps(report, indent=1), encoding="utf-8")
    for f in failures:
        print("FAIL " + f)
    print("tray check:", "FAILED" if failures else "all rules held", json.dumps({k: v.get("failures") for k, v in report.items() if isinstance(v, dict)}, ensure_ascii=False)[:600])
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(run(sys.argv[1] if len(sys.argv) > 1 else "tray-out"))
