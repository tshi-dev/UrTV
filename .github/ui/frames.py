#!/usr/bin/env python3
"""Reads measure.sh's videos (screenrecord, variable frame rate) and says, per case, what the person saw after the change:
when the screen first moved, when it settled, the frames in between, blank or flash frames, and a contact sheet of them.
   python3 frames.py OUT_DIR      -> OUT_DIR/report.md, OUT_DIR/report.json, OUT_DIR/<case>-sheet.jpg"""
import json, os, re, subprocess, sys
import numpy as np
from PIL import Image, ImageDraw

def _ffmpeg():
    """FFMPEG=... , else the one pip's imageio-ffmpeg brings (CI: no apt, a stuck mirror once held the run 10 minutes), else the system's"""
    if os.environ.get("FFMPEG"):
        return os.environ["FFMPEG"]
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except ImportError:
        return "ffmpeg"


FFMPEG = _ffmpeg()
W, H = 192, 108          # the frames are compared small: a moved tile shows, the encoder's noise does not
MOVE = 1.2               # mean |difference| (0..255) that counts as the picture changing
GAP = 1.5                # changes closer than this (s) are one transition


def frames(path):
    info = subprocess.run([FFMPEG, "-hide_banner", "-i", path, "-vf", "showinfo", "-fps_mode", "passthrough", "-f", "null", "-"], capture_output=True, text=True).stderr
    pts = re.findall(r"pts_time:\s*([0-9.]+)", info)   # each frame's time (ffmpeg's own showinfo: no ffprobe needed)
    raw = subprocess.run([FFMPEG, "-v", "error", "-i", path, "-vf", f"scale={W}:{H},format=gray", "-fps_mode", "passthrough", "-f", "rawvideo", "-"],
                         capture_output=True).stdout
    n = len(raw) // (W * H)
    a = np.frombuffer(raw[: n * W * H], np.uint8).reshape(n, H, W).astype(np.float32)
    t = np.array([float(x) for x in pts[:n]]) if len(pts) >= n else np.arange(n) / 30.0
    return t, a


def full_frame(path, ts):
    out = subprocess.run([FFMPEG, "-v", "error", "-ss", f"{ts:.3f}", "-i", path, "-frames:v", "1", "-vf", "scale=384:216", "-f", "image2pipe", "-vcodec", "png", "-"],
                         capture_output=True).stdout
    import io
    try:
        return Image.open(io.BytesIO(out)).convert("RGB")
    except Exception:
        return Image.new("RGB", (384, 216), (255, 0, 255))


def times(txt):
    d = {}
    for line in open(txt, encoding="utf-8", errors="replace"):
        k, _, v = line.partition(":")
        d[k.strip()] = v.strip()
    return d


def case(out, name):
    mp4 = os.path.join(out, name + ".mp4")
    meta = times(os.path.join(out, name + ".txt"))
    try:
        t_change = float(meta["t_change"].split()[0]) - float(meta["t_rec"].split()[0])
    except Exception:
        t_change = 2.0
    t, a = frames(mp4)
    r = {"case": name, "frames": int(len(t)), "change_at": round(t_change, 2), "focus_before": meta.get("focus-before", ""), "focus_after": meta.get("focus-after", "")}
    if len(t) < 2:
        r["error"] = "no video"; return r
    t = t - t[0] + (t[0] if t[0] < 1 else 0)
    diff = np.concatenate([[0], np.abs(a[1:] - a[:-1]).mean(axis=(1, 2))])
    after = np.where((t >= t_change - 0.05) & (diff > MOVE))[0]
    before = np.where(t < t_change)[0]
    base = before[-1] if len(before) else 0
    if not len(after):
        r.update(first=None, settled=None, note="nothing changed on screen in the recording"); return r
    first = after[0]
    last = first
    for i in after[1:]:
        if t[i] - t[last] > GAP: break
        last = i
    end = min(len(t) - 1, last)
    mb, me = a[base].mean(), a[end].mean()
    blank, flash, states = [], [], 0
    for i in range(first, end + 1):
        m, s = a[i].mean(), a[i].std()
        if s < 6 or m < 0.35 * min(mb, me): blank.append(round(float(t[i]), 3))
        if m > 1.6 * max(mb, me, 1): flash.append(round(float(t[i]), 3))
        if diff[i] > MOVE: states += 1
    r.update(first=round(float(t[first] - t_change), 3), settled=round(float(t[end] - t_change), 3), span=round(float(t[end] - t[first]), 3),
             changed_frames=states, blank=blank, flash=flash, total_change=round(float(np.abs(a[end] - a[base]).mean()), 2))
    # contact sheet: the frame before, every changed frame (up to 23), the frame after
    picks = [base] + [i for i in range(first, end + 1) if diff[i] > MOVE][:22] + [min(len(t) - 1, end + 1)]
    cols = 4; rows = (len(picks) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * 388, rows * 236), (20, 20, 20))
    dr = ImageDraw.Draw(sheet)
    for k, i in enumerate(picks):
        x, y = (k % cols) * 388 + 2, (k // cols) * 236 + 2
        sheet.paste(full_frame(mp4, float(t[i]) + 0.001), (x, y))
        label = f"{t[i] - t_change:+.3f}s  d={diff[i]:.1f}  mean={a[i].mean():.0f}"
        dr.text((x + 4, y + 218), label, fill=(255, 220, 120))
    sheet.save(os.path.join(out, name + "-sheet.jpg"), quality=82)
    return r


# 4.5 the owner's budgets (docs/ui/PROPOSAL.md, approved: CI checks), at the emulator's scale (software drawing: about twice a 2 GB TV's time)
BUDGETS = [
    ("no blank or dark frame in any switch of look (the cold start, 4x, is reported, not yet budgeted)", lambda r: r["case"].startswith("4") or not r.get("blank")),
    ("a change from outside shows within 3 s (it waited for the minute: 4 to 52 s)", lambda r: not r["case"][:2] in ("01", "02", "03", "04", "05", "06", "07", "08", "09") or (r.get("first") is not None and r["first"] <= 3.0)),
    ("after Back the new look is settled within 1.2 s (2 x the 600 ms of a TV)", lambda r: not r["case"].startswith("2") or (r.get("settled") is not None and r["settled"] <= 1.2)),
]


def budgets(res):
    """the owner's budgets over every case: [(rule, [failing cases])]"""
    return [(name, [r["case"] for r in res if not ok(r)]) for name, ok in BUDGETS]


def main(out):
    names = sorted(f[:-4] for f in os.listdir(out) if f.endswith(".mp4"))
    res = [case(out, n) for n in names]
    json.dump(res, open(os.path.join(out, "report.json"), "w"), indent=1, ensure_ascii=False)
    lines = ["| case | first change (s) | settled (s) | changed frames | blank | flash | focus before → after |", "|---|---|---|---|---|---|---|"]
    for r in res:
        lines.append(f"| {r['case']} | {r.get('first')} | {r.get('settled')} | {r.get('changed_frames')} | {len(r.get('blank') or [])} | {len(r.get('flash') or [])} | {r.get('focus_before','')} → {r.get('focus_after','')} |")
    bad = budgets(res)
    lines += ["", "| budget | cases over it |", "|---|---|"] + [f"| {n} | {', '.join(c) if c else 'none'} |" for n, c in bad]
    open(os.path.join(out, "report.md"), "w").write("\n".join(lines) + "\n")
    print("\n".join(lines))
    if "--budget" in sys.argv and any(c for _, c in bad):
        sys.exit("over the budget: " + "; ".join(f"{n}: {', '.join(c)}" for n, c in bad if c))


if __name__ == "__main__":
    main(sys.argv[1])  # --budget: exit 1 when a case is over the owner's budgets
