#!/usr/bin/env python3
"""4.3 report only: the Globecast addresses that public lists give for SBA's Quran (Makkah) and Sunnah (Madinah) channels (…/live/eds/saudi_quran and
saudi_sunnah, hls_roku). Run in CI (globecast-probe.yml): the host is unreachable from the dev container. For each it records the master playlist as
served (headers, the variants, any token or expiry in the addresses), then plays it with ffmpeg for PLAY_S seconds (10 minutes) in parallel, counting
how long it really ran and how ffmpeg ended. It also reads SBA's own sites (sba.sa, aloula.sba.sa) for the word «globecast», as ownership evidence.
Nothing is sent but the GETs. Out: globecast/report.md.

    python3 .github/tools/globecast_probe.py
"""
import os
import re
import subprocess
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

STREAMS = {
    "quran": "https://cdn-globecast.akamaized.net/live/eds/saudi_quran/hls_roku/index.m3u8",
    "sunnah": "https://cdn-globecast.akamaized.net/live/eds/saudi_sunnah/hls_roku/index.m3u8",
}
SBA_PAGES = ["https://sba.sa", "https://www.sba.sa/ar", "https://aloula.sba.sa", "https://aloula.sba.sa/live/quran"]
PLAY_S = int(os.environ.get("GLOBECAST_PLAY_S", "600"))
UA = "stagefright/1.2 (Linux;Android 11)"
OUT = Path("globecast")
TOKEN = re.compile(r"[?&](token|hdnts|hdntl|expires|exp|e|st|sig|hash|auth|acl)=", re.I)


def get(url, limit=400_000):
    try:
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, dict(r.headers), r.geturl(), r.read(limit).decode("utf-8", "replace")
    except Exception as e:   # noqa: BLE001 - a probe reports
        return None, {}, url, f"{type(e).__name__}: {e}"


def play(name, url):
    t0 = time.time()
    r = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-loglevel", "warning", "-user_agent", UA, "-rw_timeout", "15000000", "-i", url,
                        "-t", str(PLAY_S), "-f", "null", "-"], capture_output=True, text=True, timeout=PLAY_S + 180)
    ran = time.time() - t0
    warn = [l for l in r.stderr.splitlines() if l.strip()]
    return name, ran, r.returncode, warn[-8:]


def main():
    OUT.mkdir(exist_ok=True)
    lines = ["# Globecast SBA Quran / Sunnah streams (report only)", ""]
    for name, url in STREAMS.items():
        st, hdr, final, body = get(url)
        lines.append(f"## {name}: {url}")
        lines.append(f"- status {st}, final address {final}")
        lines.append("- headers: " + ", ".join(f"{k}={v}" for k, v in hdr.items() if k.lower() in ("server", "cache-control", "expires", "access-control-allow-origin", "akamai-grn", "x-cache", "content-type")))
        lines.append(f"- token or expiry in the address: {'yes' if TOKEN.search(final) else 'no'}")
        variants = [l for l in body.splitlines() if l and not l.startswith("#")]
        lines.append(f"- variants: {len(variants)}; token in any: {'yes' if any(TOKEN.search(v) for v in variants) else 'no'}")
        lines += [f"  - {v[:200]}" for v in variants[:6]]
        lines += ["  " + l for l in body.splitlines() if l.startswith("#EXT-X-STREAM-INF")][:6]
        lines.append("")
    lines.append("## SBA's own sites: do they name Globecast?")
    for page in SBA_PAGES:
        st, _, _, body = get(page, 3_000_000)
        hits = sorted(set(re.findall(r".{0,80}globecast.{0,80}", body, re.I)))[:5]
        lines.append(f"- {page} -> {st}: {len(hits)} mention(s)")
        lines += [f"  - {h.strip()}" for h in hits]
    lines.append("")
    lines.append(f"## {PLAY_S} s of playback each (ffmpeg, in parallel)")
    with ThreadPoolExecutor(2) as ex:
        for name, ran, rc, warn in ex.map(lambda kv: play(*kv), STREAMS.items()):
            ok = rc == 0 and ran >= PLAY_S * 0.98
            lines.append(f"- {name}: ran {ran:.0f} s, exit {rc}, {'OK' if ok else 'NOT OK'}")
            lines += [f"  - {w[:200]}" for w in warn]
    (OUT / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
