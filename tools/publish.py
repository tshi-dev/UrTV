#!/usr/bin/env python3
"""Put one or more folders into the data branch and rebuild index.json, safely beside the other writers (docs/LAYOUT.md).

    python3 tools/publish.py --put leagues:football/leagues [--put out:football/archive] -m "League fixtures"

The data branch holds ONE parentless commit (docs/LAYOUT.md says why): every run makes a new one from the branch's current tree with
its own folders replaced, and pushes it with --force-with-lease on the commit it started from. When another writer pushed in between
(markets every 5 minutes, live scores every 15), the lease fails, the branch is fetched again and the same folders are put again on the
newer tree: no writer ever drops another one's files, and no workflow has to wait for another one's lock. Nothing is pushed when the
tree is the same as the branch's (index.json's times aside).

Python standard library and git only. Run from a checkout of urtv-data whose remote "origin" may push (GITHUB_TOKEN in Actions).
"""
import argparse
import datetime as dt
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

BRANCH = os.environ.get("DATA_BRANCH", "data")
INDEX = "index.json"
# Every dataset of the branch: its folder, its form's version ("v"; a breaking change gets a new folder or file name, never a new form
# under the old name), whether the CDN may serve it (docs/SERVING.md) and what it is. The longest matching folder wins.
DATASETS = {
    "football/leagues/": {"v": 1, "cdn": False, "about": "each league's games of the coming week and the last day's results; the big tournaments' 36 h"},
    "football/archive/": {"v": 1, "cdn": False, "about": "the match archive: results, live window, teams, head-to-head, seasons' store"},
    "football/catalogue/": {"v": 1, "cdn": True, "about": "every competition the leagues picker offers"},
    "games/quiz/": {"v": 1, "cdn": True, "about": "«الأسرع»'s verified quiz bank, compact"},
    "games/": {"v": 1, "cdn": True, "about": "the family games' word lists"},
    "markets/": {"v": 1, "cdn": False, "about": "market prices (every 5 minutes in open hours), daily closes, currencies"},
    "stations/": {"v": 1, "cdn": True, "about": "the live Quran stations' check"},
    "photos/": {"v": 1, "cdn": True, "about": "photo packs' metadata (the images stay as releases of tshi-dev/UrTV)"},
    "notices/": {"v": 1, "cdn": False, "about": "short remote notices for the home screen (content/announcements.json)"},
    "devices/": {"v": 1, "cdn": True, "about": "per-model defaults and fixes (content/devices.json)"},
    "seasonal/": {"v": 1, "cdn": True, "about": "seasonal photo packs with their dates (content/seasonal.json)"},
    "prayer/": {"v": 1, "cdn": True, "about": "per-country and per-city prayer time corrections by date (content/prayer-adjustments.json)"},
    "status/": {"v": 1, "cdn": False, "about": "each dataset's last update and its writer's last good run (tools/status.py)"},
}


def git(*a, inp=None, env=None, check=True):
    r = subprocess.run(["git", *a], input=inp, capture_output=True, env={**os.environ, **(env or {})})
    if check and r.returncode:
        raise RuntimeError(f"git {' '.join(a)}: {r.stderr.decode(errors='replace').strip()}")
    return r


def dataset(path):
    best = None
    for k in DATASETS:
        if path.startswith(k) and (best is None or len(k) > len(best)):
            best = k
    return best


def files_under(src):
    src = Path(src)
    return sorted(p for p in src.rglob("*") if p.is_file())


def build(base, puts, idx_env):
    """The new tree: `base`'s (or an empty one) with each put's folder replaced; returns (tree, {path: sha256}) without index.json."""
    if base:
        git("read-tree", base, env=idx_env)
    else:
        git("read-tree", "--empty", env=idx_env)
    for src, dest in puts:
        git("rm", "-r", "-q", "--cached", "--ignore-unmatch", dest.rstrip("/"), env=idx_env)
        for p in files_under(src):
            blob = git("hash-object", "-w", str(p)).stdout.decode().strip()
            git("update-index", "--add", "--cacheinfo", f"100644,{blob},{dest.rstrip('/')}/{p.relative_to(src).as_posix()}", env=idx_env)
    git("rm", "-q", "--cached", "--ignore-unmatch", INDEX, env=idx_env)
    listing = git("ls-files", "-s", env=idx_env).stdout.decode().splitlines()
    blobs = {}
    for line in listing:
        meta, path = line.split("\t", 1)
        blobs[path] = meta.split()[1]
    # sha256 of every file, one cat-file process for all of them
    order = list(blobs)
    out = git("cat-file", "--batch", inp="".join(blobs[p] + "\n" for p in order).encode()).stdout
    sums, pos = {}, 0
    for p in order:
        head_end = out.index(b"\n", pos)
        size = int(out[pos:head_end].split()[2])
        body = out[head_end + 1: head_end + 1 + size]
        sums[p] = (hashlib.sha256(body).hexdigest(), size)
        pos = head_end + 1 + size + 1
    return sums


def index_json(sums, old, now):
    """index.json: every dataset with its v, its updated time (kept while its files are unchanged) and the sha256 of its files."""
    sets = {}
    for path, (sha, size) in sums.items():
        k = dataset(path)
        if k is None:
            continue
        sets.setdefault(k, {})[path[len(k):]] = {"sha256": sha, "bytes": size}
    out = {"v": 1, "updated": now, "branch": BRANCH, "datasets": {}}
    for k in sorted(sets):
        files = dict(sorted(sets[k].items()))
        whole = hashlib.sha256(json.dumps(files, sort_keys=True).encode()).hexdigest()
        prev = (old or {}).get("datasets", {}).get(k.rstrip("/")) or {}
        d = DATASETS[k]
        out["datasets"][k.rstrip("/")] = {
            "v": d["v"], "about": d["about"], "cdn": d["cdn"],
            "updated": prev.get("updated") if prev.get("sha256") == whole and prev.get("updated") else now,
            "sha256": whole, "count": len(files), "bytes": sum(f["bytes"] for f in files.values()), "files": files,
        }
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--put", action="append", required=True, help="SRC:DEST, a local folder and the folder it replaces on the branch")
    ap.add_argument("-m", "--message", required=True)
    ap.add_argument("--remote", default="origin")
    ap.add_argument("--tries", type=int, default=8)
    a = ap.parse_args()
    puts = []
    for p in a.put:
        src, dest = p.split(":", 1)
        if not dataset(dest.rstrip("/") + "/"):
            sys.exit(f"{dest}: not a dataset of DATASETS (tools/publish.py)")
        if not Path(src).is_dir() or not files_under(src):
            sys.exit(f"{src}: no files: nothing is published (the branch keeps its last good copy)")
        puts.append((src, dest))
    for attempt in range(1, a.tries + 1):
        r = git("fetch", "--depth=1", a.remote, f"+refs/heads/{BRANCH}:refs/remotes/{a.remote}/{BRANCH}", check=False)
        base = git("rev-parse", "--verify", "-q", f"refs/remotes/{a.remote}/{BRANCH}", check=False).stdout.decode().strip() if r.returncode == 0 else ""
        with tempfile.TemporaryDirectory() as tmp:
            idx_env = {"GIT_INDEX_FILE": str(Path(tmp) / "index")}
            sums = build(base, puts, idx_env)
            old = None
            if base:
                o = git("show", f"{base}:{INDEX}", check=False)
                if o.returncode == 0:
                    try:
                        old = json.loads(o.stdout)
                    except ValueError:
                        old = None
            now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
            idx = index_json(sums, old, now)
            if old and {k: v["sha256"] for k, v in old.get("datasets", {}).items()} == {k: v["sha256"] for k, v in idx["datasets"].items()}:
                print(f"{BRANCH}: no file changed, nothing pushed")
                return 0
            blob = git("hash-object", "-w", "--stdin", inp=(json.dumps(idx, ensure_ascii=False, indent=1) + "\n").encode()).stdout.decode().strip()
            git("update-index", "--add", "--cacheinfo", f"100644,{blob},{INDEX}", env=idx_env)
            tree = git("write-tree", env=idx_env).stdout.decode().strip()
        commit = git("commit-tree", tree, "-m", a.message).stdout.decode().strip()
        push = git("push", f"--force-with-lease=refs/heads/{BRANCH}:{base}", a.remote, f"{commit}:refs/heads/{BRANCH}", check=False)
        if push.returncode == 0:
            print(f"{BRANCH}: {commit[:10]} pushed ({', '.join(d for _, d in puts)}; try {attempt})")
            return 0
        print(f"{BRANCH}: another writer pushed first, again on its tree (try {attempt}): {push.stderr.decode(errors='replace').strip()[-200:]}")
        time.sleep(min(30, 2 * attempt))
    sys.exit(f"{BRANCH}: not pushed after {a.tries} tries")


if __name__ == "__main__":
    sys.exit(main())
