#!/usr/bin/env python3
"""Candidates for new «تلاوة» entries, looked up in CI (the dev container cannot reach these hosts); a report, nothing is changed.

- radio-browser.info: the Quran stations of Egypt (EG) and Kuwait (KW), each with its stream URL, its resolved URL, codec and bitrate, and its
  homepage (to judge whether it is the broadcaster's own);
- mp3quran.net's API: a reciter by name (Hafs murattal, 114 surahs), its server.

Out: stations/candidates.json and stations/candidates.md.

    python3 .github/tools/find_streams.py
"""
import json
import os
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(os.environ.get("STATIONS_OUT", "stations"))
UA = {"User-Agent": "urtv-listening"}
RB = "https://de1.api.radio-browser.info/json/stations/search?"
RECITERS = ["بدر التركي", "محمد أيوب"]


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=40) as r:
        return json.load(r)


def main():
    out = {"radios": [], "reciters": []}
    for cc, terms in (("EG", ("quran", "قرآن", "القرآن")), ("KW", ("quran", "قرآن", "القرآن")),
                      ("SA", ("makkah", "mecca", "haram", "مكة", "الحرم", "quran"))):   # the Haram's live audio (4.1 reports)
        for term in terms:
            try:
                rows = get(RB + urllib.parse.urlencode({"countrycode": cc, "name": term, "hidebroken": "true", "limit": 40}))
            except Exception as e:   # noqa: BLE001 - a report, not a build
                out["radios"].append({"cc": cc, "term": term, "error": str(e)[:200]})
                continue
            for s in rows:
                row = {k: s.get(k) for k in ("name", "url", "url_resolved", "homepage", "codec", "bitrate", "lastcheckok", "votes")}
                row["cc"] = cc
                if row not in out["radios"]:
                    out["radios"].append(row)
    try:
        api = get("https://www.mp3quran.net/api/v3/reciters?language=ar")["reciters"]
        for name in RECITERS:
            for r in api:
                if name in r["name"]:
                    for m in r["moshaf"]:
                        out["reciters"].append({"name": r["name"], "moshaf": m["name"], "server": m["server"], "surah_total": m["surah_total"]})
    except Exception as e:   # noqa: BLE001
        out["reciters"].append({"error": str(e)[:200]})
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "candidates.json").write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding="utf-8")
    md = ["| cc | name | url | resolved | codec | kbps | ok | homepage |", "|---|---|---|---|---|---|---|---|"]
    for s in out["radios"]:
        md.append("| %s | %s | %s | %s | %s | %s | %s | %s |" % tuple(str(s.get(k, "")) for k in ("cc", "name", "url", "url_resolved", "codec", "bitrate", "lastcheckok", "homepage")))
    md += ["", "| reciter | moshaf | server | surahs |", "|---|---|---|---|"]
    for r in out["reciters"]:
        md.append("| %s | %s | %s | %s |" % tuple(str(r.get(k, "")) for k in ("name", "moshaf", "server", "surah_total")))
    (OUT / "candidates.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print("\n".join(md))


if __name__ == "__main__":
    main()
