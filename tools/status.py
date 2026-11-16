#!/usr/bin/env python3
"""status/status.json: for each dataset, its last update (index.json of the data branch) and its writer's last good run (GitHub's API, the
workflow's own token), and "late" when that run is older than the writer's schedule allows. Read by docs/index.html (GitHub Pages).
    GITHUB_TOKEN=... GITHUB_REPOSITORY=tshi-dev/urtv-data python3 tools/status.py <index.json> <out dir>"""
import datetime as dt
import json
import os
import sys
import urllib.request
from pathlib import Path

# dataset -> (writer workflow file, hours after which its last good run is late: the schedule plus GitHub's delays)
WRITERS = {
    "football/leagues": ("football.yml", 8),
    "football/archive": ("football.yml", 8),
    "football/catalogue": ("catalogue.yml", 8 * 24 + 6),
    "games": ("games.yml", 50),
    "games/quiz": ("games.yml", 50),
    "markets": ("markets.yml", 2),
    "stations": ("stations.yml", 8 * 24),
    "photos": ("seed.yml", None),
    "notices": ("content.yml", None),
    "devices": ("content.yml", None),
    "seasonal": ("content.yml", None),
    "prayer": ("content.yml", None),
}


def last_good(repo, wf, token):
    url = f"https://api.github.com/repos/{repo}/actions/workflows/{wf}/runs?status=success&per_page=1"
    req = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json", "User-Agent": "urtv-data-status",
                                               **({"Authorization": f"Bearer {token}"} if token else {})})
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            runs = json.load(r).get("workflow_runs") or []
    except Exception as e:  # noqa: BLE001 - the page says unknown
        print(f"{wf}: {e}")
        return None, None
    return (runs[0]["updated_at"], runs[0]["html_url"]) if runs else (None, None)


def main():
    idx = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    out = Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    repo, token = os.environ.get("GITHUB_REPOSITORY", "tshi-dev/urtv-data"), os.environ.get("GITHUB_TOKEN", "")
    now = dt.datetime.now(dt.timezone.utc)
    cache, sets = {}, {}
    for name, d in sorted(idx.get("datasets", {}).items()):
        if name == "status":
            continue
        wf, max_h = WRITERS.get(name, (None, None))
        if wf and wf not in cache:
            cache[wf] = last_good(repo, wf, token)
        ok_at, ok_url = cache.get(wf, (None, None))
        late = False
        if max_h is not None:
            late = ok_at is None or (now - dt.datetime.fromisoformat(ok_at.replace("Z", "+00:00"))).total_seconds() > max_h * 3600
        sets[name] = {"updated": d.get("updated"), "v": d.get("v"), "count": d.get("count"), "bytes": d.get("bytes"), "writer": wf,
                      "last_good_run": ok_at, "run": ok_url, "late_after_h": max_h, "late": late}
    status = {"v": 1, "updated": now.replace(microsecond=0).isoformat().replace("+00:00", "Z"), "late": sorted(k for k, s in sets.items() if s["late"]),
              "datasets": sets}
    (out / "status.json").write_text(json.dumps(status, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print("late: " + (", ".join(status["late"]) or "none"))


if __name__ == "__main__":
    main()
