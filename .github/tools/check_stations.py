#!/usr/bin/env python3
"""Does every live station still play? Run in CI (.github/workflows/stations-check.yml): the stream hosts are unreachable from the dev container.

For each station of android/launcher/src/main/assets/quran/stations.json, and each extra address given on the command line (a candidate for a
replacement), it records: the redirect chain of a GET (curl, as StreamUrl.resolve follows it), whether an https -> http step goes to a host the app
allows cleartext for (radiojar.com only), what ffprobe finds (an audio stream, its codec and rate), and whether ffmpeg can read PLAY_S seconds of audio
in at most PLAY_S + SLACK_S of wall time, staying up at least UP_S (a live stream that stalls or drops fails). Nothing but the addresses is sent anywhere.

Out: stations/report.json, stations/report.md (a table) and stations/status.json (4.4, read by the home app: StationStatus). A station is up when
any of its sources plays (its own address, «alt», «mirrors», and the extra mirrors of .github/tools/station_mirrors.json). status.json counts the
failed checks in a row from the last run's file (STATIONS_PREV); DEAD_AFTER in a row marks it dead and the app hides it. When no station at all
plays, the runner's network is the likely fault: the counts are kept as they were. The extra mirrors that played are published for the app.

    python3 .github/tools/check_stations.py [extra_url ...]
"""
import json
import os
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(os.environ.get("STATIONS_OUT", "stations"))
PLAY_S = int(os.environ.get("STATIONS_PLAY_S", "60"))   # seconds of audio read
UP_S = 30     # the stream must stay up at least this long (wall time)
SLACK_S = 40
UA_ANDROID = "stagefright/1.2 (Linux;Android 11)"   # what the TV's MediaPlayer sends
DEAD_AFTER = 3   # the same as StationStatus.DEAD_AFTER
MIRRORS = Path(os.environ.get("STATIONS_MIRRORS", str(ROOT / ".github/tools/station_mirrors.json")))


def cleartext_ok(host):
    h = (host or "").lower()
    return h == "radiojar.com" or h.endswith(".radiojar.com")


def chain(url):
    """The GET's redirects, hop by hop (headers only, the body is not read past the first bytes): [(status, url)]."""
    hops, now = [], url
    for _ in range(6):
        r = subprocess.run(["curl", "-sS", "-o", "/dev/null", "-D", "-", "--max-time", "10", "-A", UA_ANDROID, "-H", "Icy-MetaData: 0", "-r", "0-0", now],
                           capture_output=True, text=True)
        status, loc = None, None
        for line in r.stdout.splitlines():
            if line.startswith("HTTP/"):
                parts = line.split()
                status = int(parts[1]) if len(parts) > 1 and parts[1].isdigit() else None
            elif line.lower().startswith("location:"):
                loc = line.split(":", 1)[1].strip()
        hops.append({"status": status, "url": now, "err": r.stderr.strip()[-160:] if status is None else ""})
        if status and 300 <= status < 400 and loc:
            from urllib.parse import urljoin
            now = urljoin(now, loc)
            continue
        break
    return hops


def probe(url):
    r = subprocess.run(["ffprobe", "-v", "error", "-rw_timeout", "15000000", "-user_agent", UA_ANDROID, "-show_entries", "stream=codec_type,codec_name,sample_rate,bit_rate",
                        "-of", "json", url], capture_output=True, text=True, timeout=60)
    try:
        streams = json.loads(r.stdout or "{}").get("streams", [])
    except ValueError:
        streams = []
    audio = [s for s in streams if s.get("codec_type") == "audio"]
    return {"audio": bool(audio), "codec": audio[0].get("codec_name") if audio else None, "rate": audio[0].get("sample_rate") if audio else None,
            "err": r.stderr.strip()[-200:]}


def play(url):
    t0 = time.time()
    try:
        r = subprocess.run(["ffmpeg", "-v", "error", "-nostdin", "-rw_timeout", "15000000", "-user_agent", UA_ANDROID, "-i", url, "-vn", "-t", str(PLAY_S), "-f", "null", "-"],
                           capture_output=True, text=True, timeout=PLAY_S + SLACK_S)
        wall = time.time() - t0
        # -t counts media time: a live server sends a burst first (Icecast about 15 s, HLS its last segments), then real time, so 60 s of audio
        # read without an error takes at least UP_S of wall time on a live stream
        return {"ok": r.returncode == 0 and wall >= UP_S, "wall_s": round(wall, 1), "err": r.stderr.strip()[-200:]}
    except subprocess.TimeoutExpired:
        return {"ok": False, "wall_s": round(time.time() - t0, 1), "err": "stalled (no %d s of audio in %d s)" % (PLAY_S, PLAY_S + SLACK_S)}


def check(name, url):
    hops = chain(url)
    bad_http = [h["url"] for h in hops if urlparse(h["url"]).scheme == "http" and not cleartext_ok(urlparse(h["url"]).hostname)]
    p = probe(url)
    pl = play(url) if p["audio"] else {"ok": False, "wall_s": 0, "err": "no audio stream"}
    return {"name": name, "url": url, "hops": hops, "cleartext_blocked": bad_http, "probe": p, "play": pl,
            "ok": p["audio"] and pl["ok"] and not bad_http}


def sources(station, extra_mirrors):
    """the station's addresses: its own, alt, mirrors (stations.json), then the extra mirrors; https only past the first, no repeats"""
    out = [station["url"]]
    for u in [station.get("alt")] + list(station.get("mirrors") or []) + list(extra_mirrors or []):
        if isinstance(u, str) and u.startswith("https://") and u not in out:
            out.append(u)
    return out


def load_json(path):
    try:
        return json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def status(stations, played, prev, extra, now_ms):
    """stations/status.json: per Arabic name, failed checks in a row, dead, and the extra mirrors that played.
    played: name -> [(url, ok)] in source order. prev: last run's status (or None)."""
    before = (prev or {}).get("stations") if isinstance(prev, dict) and prev.get("v") == 1 else None
    before = before if isinstance(before, dict) else {}
    any_up = any(ok for res in played.values() for _, ok in res)
    out = {}
    for st in stations:
        name = st["name"]
        res = played.get(name, [])
        up = any(ok for _, ok in res)
        old = before.get(name) if isinstance(before.get(name), dict) else {}
        old_fails = old.get("fails") if isinstance(old.get("fails"), int) and old.get("fails") >= 0 else 0
        fails = 0 if up else (old_fails + 1 if any_up else old_fails)
        good = [u for u, ok in res if ok and u in (extra.get(name) or [])]
        out[name] = {"fails": fails, "dead": fails >= DEAD_AFTER, "mirrors": good}
    return {"v": 1, "checked_ms": now_ms, "checked": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(now_ms / 1000)), "dead_after": DEAD_AFTER,
            "stations": out}


def main(extra):
    stations = json.loads((ROOT / "android/launcher/src/main/assets/quran/stations.json").read_text(encoding="utf-8"))["stations"]
    mirrors = load_json(MIRRORS)
    mirrors = mirrors.get("mirrors") if isinstance(mirrors, dict) and isinstance(mirrors.get("mirrors"), dict) else {}
    rows, played = [], {}
    for i, s in enumerate(stations):
        played[s["name"]] = []
        up, extras = False, mirrors.get(s["name"]) or []
        for k, u in enumerate(sources(s, extras)):
            if k > 0 and up and u not in extras:
                continue   # one source that plays is enough for «up»; the extra mirrors are still checked, to be published
            r = check("%d %s%s" % (i, s.get("en") or s["name"], "" if k == 0 else " (source %d)" % (k + 1)), u)
            rows.append(r)
            played[s["name"]].append((u, r["ok"]))
            up = up or r["ok"]
    rows += [check("candidate", u) for u in extra]
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "report.json").write_text(json.dumps({"checked": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "rows": rows}, ensure_ascii=False, indent=1), encoding="utf-8")
    md = ["| station | ok | first status | final host | codec | played | note |", "|---|---|---|---|---|---|---|"]
    for r in rows:
        last = r["hops"][-1]
        note = (r["play"]["err"] if r["probe"]["audio"] else r["probe"]["err"] or "no audio stream") or last["err"] or (("cleartext not allowed: " + ", ".join(r["cleartext_blocked"])) if r["cleartext_blocked"] else "")
        md.append("| %s | %s | %s | %s | %s | %s s | %s |" % (r["name"], "yes" if r["ok"] else "NO", r["hops"][0]["status"], urlparse(last["url"]).hostname,
                                                         r["probe"]["codec"], r["play"]["wall_s"], note.replace("|", "/").replace("\n", " ")[:160]))
    (OUT / "report.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    prev = load_json(os.environ["STATIONS_PREV"]) if os.environ.get("STATIONS_PREV") else None
    st = status(stations, played, prev, mirrors, int(time.time() * 1000))
    (OUT / "status.json").write_text(json.dumps(st, ensure_ascii=False, indent=1), encoding="utf-8")
    print("dead: " + (", ".join(n for n, e in st["stations"].items() if e["dead"]) or "none"))
    print("\n".join(md))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
