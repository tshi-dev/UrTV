#!/usr/bin/env python3
"""tools/publish.py against a local bare repository: a first publish, index.json, nothing pushed when nothing changed, a dataset's updated
kept while its files are the same, two writers at once (the lease, then the newer tree), one commit on the branch, and refusals."""
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


def sh(*a, cwd, ok=True):
    r = subprocess.run(a, cwd=cwd, capture_output=True, text=True, env={**os.environ, "GIT_AUTHOR_NAME": "t", "GIT_AUTHOR_EMAIL": "t@t",
                                                                     "GIT_COMMITTER_NAME": "t", "GIT_COMMITTER_EMAIL": "t@t"})
    if ok and r.returncode:
        raise SystemExit(f"{a}: {r.stderr}")
    return r


with tempfile.TemporaryDirectory() as t:
    t = Path(t)
    sh("git", "init", "-q", "--bare", str(t / "o.git"), cwd=t)
    for w in ("w1", "w2"):
        (t / w).mkdir()
        sh("git", "init", "-q", cwd=t / w)
        sh("git", "remote", "add", "origin", str(t / "o.git"), cwd=t / w)
        shutil.copytree(ROOT / "tools", t / w / "tools")

    def put(w, folder, files, dest, msg="m"):
        d = t / w / folder
        shutil.rmtree(d, ignore_errors=True)
        d.mkdir(parents=True)
        for k, v in files.items():
            (d / k).write_text(json.dumps(v))
        return sh(sys.executable, "tools/publish.py", "--put", f"{folder}:{dest}", "-m", msg, cwd=t / w, ok=False)

    def show(path):
        return sh("git", "--git-dir", str(t / "o.git"), "show", f"data:{path}", cwd=t, ok=False).stdout

    r = put("w1", "m", {"markets.json": {"p": 1}}, "markets")
    check("first publish creates the branch", r.returncode == 0 and json.loads(show("markets/markets.json")) == {"p": 1}, r.stderr)
    idx = json.loads(show("index.json"))
    check("index.json: v, the dataset with its v, count and each file's sha256", idx["v"] == 1 and idx["datasets"]["markets"]["v"] == 1
          and idx["datasets"]["markets"]["count"] == 1 and len(idx["datasets"]["markets"]["files"]["markets.json"]["sha256"]) == 64, idx)
    first = idx["datasets"]["markets"]["updated"]
    r = put("w1", "m", {"markets.json": {"p": 1}}, "markets")
    check("the same files: nothing pushed", "nothing pushed" in r.stdout, r.stdout)
    r = put("w2", "s", {"report.json": {"ok": True}}, "stations")
    idx = json.loads(show("index.json"))
    check("another writer's folder is added, the first one's kept with its updated", r.returncode == 0 and show("markets/markets.json")
          and idx["datasets"]["markets"]["updated"] == first and "stations" in idx["datasets"], idx)
    r = put("w1", "g", {"odd-one.json": {"v": 1, "t": []}}, "games")
    check("a writer whose last view is old publishes on the current tree, the others kept", r.returncode == 0 and show("stations/report.json") and show("games/odd-one.json"), r.stdout + r.stderr)
    n = sh("git", "--git-dir", str(t / "o.git"), "rev-list", "--count", "data", cwd=t).stdout.strip()
    check("the branch is one commit", n == "1", n)
    r = put("w1", "x", {"a.json": {}}, "secrets")
    check("a folder that is not a dataset is refused", r.returncode != 0 and "not a dataset" in (r.stderr + r.stdout), r.stderr)
    (t / "w1" / "empty").mkdir()
    r = sh(sys.executable, "tools/publish.py", "--put", "empty:markets", "-m", "m", cwd=t / "w1", ok=False)
    check("an empty folder publishes nothing (the last good copy stays)", r.returncode != 0 and json.loads(show("markets/markets.json")) == {"p": 1}, r.stderr)

    # as in CI: a checkout of main (a HEAD commit), the generator's folder at the same path as its destination, with files that differ from
    # the branch's, and an index.json of the Status job in the worktree (Stations run 43828097721, Status run 43828105367)
    w = t / "w3"
    w.mkdir()
    sh("git", "init", "-q", "-b", "main", cwd=w)
    sh("git", "remote", "add", "origin", str(t / "o.git"), cwd=w)
    shutil.copytree(ROOT / "tools", w / "tools")
    (w / "README.md").write_text("x")
    sh("git", "add", "README.md", cwd=w)
    sh("git", "commit", "-qm", "main", cwd=w)
    (w / "stations").mkdir()
    (w / "stations" / "report.json").write_text(json.dumps({"ok": "new"}))
    (w / "index.json").write_text("{}")
    r = sh(sys.executable, "tools/publish.py", "--put", "stations:stations", "-m", "Stations", cwd=w, ok=False)
    check("CI's case: the folder at its own destination path, a stray index.json in the worktree", r.returncode == 0
          and json.loads(show("stations/report.json")) == {"ok": "new"} and json.loads(show("index.json")).get("v") == 1, r.stderr[-400:])
    st = sh("git", "status", "--porcelain", cwd=w).stdout
    check("…and the worktree and the checkout's own index are untouched", "stations/" in st and "index.json" in st and " D " not in st, st)

print(f"\n{len(fails)} failed" if fails else "\nall passed")
sys.exit(1 if fails else 0)
