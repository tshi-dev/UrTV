#!/usr/bin/env python3
"""Why does station 0 (Saudi Quran Radio, SBA's HLS on live.kwikmotion.com) stutter on the TV's MediaPlayer? Run in CI
(.github/workflows/quran-radio-probe.yml): the stream hosts are unreachable from the dev container.

1. The master playlist as the TV gets it (MediaPlayer's user agent): every variant and audio rendition, its BANDWIDTH/CODECS, its media playlist
   (target duration, segment lengths, discontinuities, the segment container), one segment's real size and ffprobe's streams (is there video?),
   and whether the variant addresses are the same when the master is read again at the end (a stable address can go in stations.json).
2. WATCH_S of live play for the master and for each variant at once: the media playlist polled as a player does, each new segment fetched and
   timed; missed sequence numbers, late playlists (no new segment for 1.5 target durations), slow fetches (longer than a segment lasts).
3. ffmpeg reading the same addresses (and the other candidates given on the command line) for WATCH_S of wall time: audio seconds read, and the
   warnings that are gaps (discontinuity, non-monotonous timestamps, failed segment, HTTP error, reconnect).

Out: probe/report.md and probe/report.json. Nothing but the addresses is sent anywhere.

    python3 .github/tools/hls_probe.py [extra_url ...]
"""
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.request
from pathlib import Path
from urllib.parse import urljoin

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(os.environ.get("PROBE_OUT", "probe"))
WATCH_S = int(os.environ.get("PROBE_WATCH_S", "300"))
UA = "stagefright/1.2 (Linux;Android 11)"   # what the TV's MediaPlayer sends


def station0():
    d = json.loads((ROOT / "android/launcher/src/main/assets/quran/stations.json").read_text(encoding="utf-8"))
    return d["stations"][0]["url"]


def get(url, timeout=20):
    t = time.time()
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        body = r.read()
        return body, r.geturl(), round((time.time() - t) * 1000), dict(r.headers)


def attrs(line):
    return {k: v.strip('"') for k, v in re.findall(r'([A-Z0-9-]+)=("[^"]*"|[^,]*)', line.split(":", 1)[1] if ":" in line else "")}


def parse_master(text, base):
    variants, renditions, lines = [], [], text.splitlines()
    for i, l in enumerate(lines):
        if l.startswith("#EXT-X-STREAM-INF"):
            a = attrs(l)
            uri = next((x.strip() for x in lines[i + 1:] if x.strip() and not x.startswith("#")), "")
            variants.append({"bandwidth": int(a.get("BANDWIDTH", "0") or 0), "average": a.get("AVERAGE-BANDWIDTH", ""), "codecs": a.get("CODECS", ""),
                             "resolution": a.get("RESOLUTION", ""), "audio_group": a.get("AUDIO", ""), "uri": urljoin(base, uri)})
        elif l.startswith("#EXT-X-MEDIA") and "TYPE=AUDIO" in l:
            a = attrs(l)
            if a.get("URI"):
                renditions.append({"group": a.get("GROUP-ID", ""), "name": a.get("NAME", ""), "uri": urljoin(base, a["URI"])})
    return variants, renditions


def parse_media(text, base):
    target, seq, segs, disc, dur = 0.0, 0, [], 0, None
    for l in text.splitlines():
        if l.startswith("#EXT-X-TARGETDURATION:"):
            target = float(l.split(":")[1])
        elif l.startswith("#EXT-X-MEDIA-SEQUENCE:"):
            seq = int(l.split(":")[1])
        elif l.startswith("#EXT-X-DISCONTINUITY") and not l.startswith("#EXT-X-DISCONTINUITY-SEQUENCE"):
            disc += 1
        elif l.startswith("#EXTINF:"):
            dur = float(l.split(":")[1].split(",")[0])
        elif l and not l.startswith("#"):
            segs.append({"seq": seq + len(segs), "dur": dur, "uri": urljoin(base, l.strip())})
    return {"target": target, "seq": seq, "segments": segs, "discontinuities": disc, "endlist": "#EXT-X-ENDLIST" in text}


def ffprobe_bytes(data):
    p = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "stream=codec_type,codec_name,sample_rate,channels,bit_rate,width,height", "-of", "json", "-"],
                       input=data, capture_output=True, timeout=60)
    try:
        return json.loads(p.stdout.decode() or "{}").get("streams", [])
    except ValueError:
        return []


def describe(uri):
    body, final, ms, h = get(uri)
    m = parse_media(body.decode("utf-8", "replace"), final)
    out = {"uri": uri, "final": final, "playlist_ms": ms, "target": m["target"], "segments": len(m["segments"]), "discontinuities": m["discontinuities"],
           "durations": sorted({s["dur"] for s in m["segments"] if s["dur"] is not None}), "endlist": m["endlist"], "playlist": body.decode("utf-8", "replace")[:1500]}
    if m["segments"]:
        s = m["segments"][-1]
        data, sfinal, sms, sh = get(s["uri"])
        out.update(segment_uri=s["uri"], segment_bytes=len(data), segment_ms=sms, segment_kbps=round(len(data) * 8 / 1000 / (s["dur"] or 1), 1),
                   container=(sh.get("Content-Type") or "") + " " + Path(s["uri"].split("?")[0]).suffix, streams=ffprobe_bytes(data))
    return out


def watch(name, uri, seconds, res):
    """A player's loop: poll the media playlist, fetch each new segment once, time it."""
    seen, fetches, missed, late, slow, errors, polls = set(), [], 0, 0, 0, [], 0
    last_new, last_seq, target = time.time(), None, 6.0
    end = time.time() + seconds
    while time.time() < end:
        try:
            body, final, ms, _ = get(uri)
            polls += 1
            m = parse_media(body.decode("utf-8", "replace"), final)
            target = m["target"] or target
            new = [s for s in m["segments"] if s["seq"] not in seen]
            if not seen:
                new = new[-3:]   # a player starts about three segments from the live edge
            for s in new:
                if last_seq is not None and s["seq"] > last_seq + 1:
                    missed += s["seq"] - last_seq - 1
                seen.add(s["seq"])
                last_seq = s["seq"]
                try:
                    data, _, sms, _ = get(s["uri"], timeout=30)
                    fetches.append({"seq": s["seq"], "dur": s["dur"], "bytes": len(data), "ms": sms})
                    if s["dur"] and sms > s["dur"] * 1000:
                        slow += 1
                except Exception as e:  # noqa: BLE001
                    errors.append(f"segment {s['seq']}: {e}")
            if new:
                last_new = time.time()
            elif time.time() - last_new > 1.5 * target:
                late += 1
                last_new = time.time()
        except Exception as e:  # noqa: BLE001
            errors.append(f"playlist: {e}")
        time.sleep(max(1.0, target / 2))
    ms = [f["ms"] for f in fetches]
    res[name] = {"uri": uri, "polls": polls, "segments": len(fetches), "missed": missed, "late_playlists": late, "slow_fetches": slow,
                 "fetch_ms_median": sorted(ms)[len(ms) // 2] if ms else None, "fetch_ms_max": max(ms) if ms else None,
                 "kbps_mean": round(sum(f["bytes"] for f in fetches) * 8 / 1000 / max(1e-9, sum(f["dur"] or 0 for f in fetches)), 1) if fetches else None,
                 "errors": errors[:10]}


GAP = re.compile(r"(?i)(discontinuity|non[- ]monoton|failed to open segment|failed to reload playlist|http error|reconnect|skipping \d+ segments|"
                 r"error while decoding|invalid data|connection reset|timed out|end of file)")


def ffread(name, uri, seconds, res):
    t = time.time()
    p = subprocess.run(["timeout", str(seconds + 60), "ffmpeg", "-hide_banner", "-nostats", "-user_agent", UA, "-i", uri, "-t", str(seconds), "-vn",
                        "-af", "astats=metadata=1:reset=0", "-f", "null", "-"], capture_output=True, text=True)
    wall = time.time() - t
    err = p.stderr
    times = re.findall(r"time=(\d+):(\d+):(\d+\.\d+)", err)
    got = 0.0
    if times:
        h, m, s = times[-1]
        got = int(h) * 3600 + int(m) * 60 + float(s)
    gaps = GAP.findall(err)
    res[name] = {"uri": uri, "rc": p.returncode, "wall_s": round(wall, 1), "audio_s": round(got, 1), "behind_s": round(wall - got, 1),
                 "gap_warnings": len(gaps), "kinds": sorted({g.lower() for g in gaps}), "opens": len(re.findall(r"Opening '", err)),
                 "input": [l.strip() for l in err.splitlines() if l.strip().startswith(("Stream #", "Input #", "Duration"))][:8],
                 "tail": err.splitlines()[-6:]}


def main(extra):
    OUT.mkdir(parents=True, exist_ok=True)
    url = station0()
    rep = {"station0": url, "watch_s": WATCH_S}
    body, final, ms, h = get(url)
    text = body.decode("utf-8", "replace")
    rep["master"] = {"final": final, "ms": ms, "text": text[:4000], "content_type": h.get("Content-Type")}
    variants, renditions = parse_master(text, final)
    is_media = "#EXTINF" in text
    rep["variants"], rep["renditions"] = variants, renditions
    rep["detail"] = {}
    for v in variants + renditions:
        try:
            rep["detail"][v["uri"]] = describe(v["uri"])
        except Exception as e:  # noqa: BLE001
            rep["detail"][v["uri"]] = {"error": str(e)}
    if is_media:
        rep["detail"][final] = describe(final)

    watches, reads, threads = {}, {}, []
    targets = [("master", url)] + [(f"variant {v['bandwidth']}", v["uri"]) for v in variants] + [(f"audio {r['name']}", r["uri"]) for r in renditions]
    for name, u in targets:
        if name != "master" or is_media:
            threads.append(threading.Thread(target=watch, args=(name, u, WATCH_S, watches)))
        threads.append(threading.Thread(target=ffread, args=(name, u, WATCH_S, reads)))
    for i, u in enumerate(extra):
        threads.append(threading.Thread(target=ffread, args=(f"candidate {i + 1}", u, WATCH_S, reads)))
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    rep["watch"], rep["ffmpeg"] = watches, reads
    try:   # are the variant addresses stable (no token that changes)?
        body2, final2, _, _ = get(url)
        v2, r2 = parse_master(body2.decode("utf-8", "replace"), final2)
        rep["stable"] = [v["uri"] for v in variants + renditions] == [v["uri"] for v in v2 + r2]
        rep["master_again"] = body2.decode("utf-8", "replace")[:4000]
    except Exception as e:  # noqa: BLE001
        rep["stable"] = f"error {e}"
    (OUT / "report.json").write_text(json.dumps(rep, ensure_ascii=False, indent=1), encoding="utf-8")

    md = [f"# Station 0 probe ({WATCH_S} s)", "", f"master: `{url}` -> `{final}` ({ms} ms, {rep['master']['content_type']})", "", "```", text[:2000], "```", "",
          f"variant addresses the same at the end: **{rep['stable']}**", "", "| variant | bandwidth | codecs | target s | seg s | segs | disc | seg kbps | streams |",
          "|---|---|---|---|---|---|---|---|---|"]
    for v in variants + renditions:
        d = rep["detail"].get(v["uri"], {})
        st = ", ".join(f"{s.get('codec_type')}:{s.get('codec_name')}" + (f" {s.get('width')}x{s.get('height')}" if s.get("width") else "") + (f" {s.get('sample_rate')}Hz" if s.get("sample_rate") else "") for s in d.get("streams", []))
        md.append(f"| `{v['uri']}` | {v.get('bandwidth', '')} | {v.get('codecs', v.get('name', ''))} | {d.get('target')} | {d.get('durations')} | {d.get('segments')} | {d.get('discontinuities')} | {d.get('segment_kbps')} | {st or d.get('error', '')} |")
    md += ["", "## Live watch (a player's loop)", "", "| stream | segments | missed | late playlists | slow fetches | fetch ms median/max | kbps | errors |", "|---|---|---|---|---|---|---|---|"]
    for n, w in watches.items():
        md.append(f"| {n} | {w['segments']} | {w['missed']} | {w['late_playlists']} | {w['slow_fetches']} | {w['fetch_ms_median']}/{w['fetch_ms_max']} | {w['kbps_mean']} | {'; '.join(w['errors'])[:200]} |")
    md += ["", "## ffmpeg", "", "| stream | wall s | audio s | behind s | gap warnings | kinds | opens | input |", "|---|---|---|---|---|---|---|---|"]
    for n, r in reads.items():
        md.append(f"| {n} `{r['uri']}` | {r['wall_s']} | {r['audio_s']} | {r['behind_s']} | {r['gap_warnings']} | {', '.join(r['kinds'])} | {r['opens']} | {' / '.join(r['input'])[:300]} |")
    (OUT / "report.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print("\n".join(md))


if __name__ == "__main__":
    main(sys.argv[1:])
