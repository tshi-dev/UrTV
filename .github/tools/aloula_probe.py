#!/usr/bin/env python3
"""4.3: which addresses do the Saudi Broadcasting Authority's own live pages (Aloula) play for the Quran channel (Makkah) and the Sunnah channel
(Madinah)? Run in CI (stations-check.yml): the pages are unreachable from the dev container. It reads each page and the scripts it loads from the same
site, and lists every .m3u8 address and every stream host named in them, with whether it carries a token (an expiring query). Nothing is sent but
the GETs. Out: stations/aloula.md.

    python3 .github/tools/aloula_probe.py
"""
import re
import urllib.request
from pathlib import Path
from urllib.parse import urljoin, urlparse

PAGES = {"quran": "https://aloula.sba.sa/live/quran", "sunna": "https://aloula.sba.sa/live/sunna"}
UA = "Mozilla/5.0 (Linux; Android 11; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"
OUT = Path("stations")


def get(url, limit=3_000_000):
    try:
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.read(limit).decode("utf-8", "replace")
    except Exception as e:   # noqa: BLE001 - a probe reports, it does not stop
        return None, f"{type(e).__name__}: {e}"


def main():
    OUT.mkdir(exist_ok=True)
    lines = ["# Aloula live pages: the stream addresses they name", ""]
    for key, page in PAGES.items():
        status, html = get(page)
        lines.append(f"## {key}: {page} -> {status}")
        texts = [html or ""]
        if status:
            host = urlparse(page).netloc
            scripts = [urljoin(page, s) for s in re.findall(r'<script[^>]+src="([^"]+)"', html)]
            for s in [s for s in scripts if urlparse(s).netloc == host][:40]:
                st, js = get(s)
                if st:
                    texts.append(js)
        found = sorted({m for t in texts for m in re.findall(r'https?://[^"\'\s<>\\]+?\.m3u8[^"\'\s<>\\]*', t)})
        hosts = sorted({m for t in texts for m in re.findall(r'https?://([a-z0-9.-]*(?:kwikmotion|holol|faulio|sba)[a-z0-9.-]*)', t)})
        apis = sorted({m for t in texts for m in re.findall(r'https?://[a-z0-9.-]+/[^"\'\s<>\\]*(?:live|channel|stream)[^"\'\s<>\\]*', t)})[:30]
        lines.append(f"- m3u8 named: {len(found)}")
        lines += [f"  - {u}  (token: {'yes' if re.search(r'[?&](token|hdnts|expires|e|st|sig|hash)=', u) else 'no'})" for u in found]
        lines.append("- stream hosts named: " + ", ".join(hosts))
        lines.append("- live/channel/stream addresses named:")
        lines += [f"  - {u}" for u in apis]
        lines.append("")
    (OUT / "aloula.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
