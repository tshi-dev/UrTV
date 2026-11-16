#!/usr/bin/env python3
"""The match archive's health for the owner, with no AI session needed: the problems the archive itself reports (health.json: a followed
league with no new result in its season, a past season short of its expected count, ...) and whether the two published copies hold the
same games (tshi-dev/urtv-data's football/archive, read by 4.4, and this repository's data-archive, read by the apps up to 4.3.2); then
ONE GitHub issue labelled data-health, opened or updated while there is a problem and closed when it is healthy again (GitHub mails the
owner about it). Run by league-fixtures.yml's job «archive». Python standard library, git and gh (on GitHub's runners).

    archive_health.py --published <repo url> <branch> <folder> [--published ...] --health health.json    prints the problems, one a line
    archive_health.py --issue "<the problems>"                                                          the issue (GH_TOKEN, GH_REPO)
"""
import argparse
import json
import subprocess
import sys
import tempfile
from pathlib import Path

LABEL = "data-health"
TITLE = "Match archive: data health"


def count_events(url, branch, folder):
    """The games of a published archive (every store file's events), or None when it cannot be read."""
    with tempfile.TemporaryDirectory() as t:
        r = subprocess.run(["git", "init", "-q", t], capture_output=True)
        r = subprocess.run(["git", "-C", t, "fetch", "-q", "--depth=1", url, branch], capture_output=True)
        if r.returncode:
            return None
        ls = subprocess.run(["git", "-C", t, "ls-tree", "-r", "--name-only", "FETCH_HEAD", "--", (folder.rstrip("/") + "/store") if folder else "store"],
                            capture_output=True, text=True)
        n = 0
        for path in ls.stdout.split():
            if not path.endswith(".json"):
                continue
            body = subprocess.run(["git", "-C", t, "show", f"FETCH_HEAD:{path}"], capture_output=True, text=True).stdout
            try:
                n += len(json.loads(body).get("events") or {})
            except ValueError:
                return None
        return n


def problems(published, health):
    out = []
    if health and Path(health).is_file():
        out += json.loads(Path(health).read_text(encoding="utf-8")).get("problems") or []
    counts = {f"{u.rsplit('/', 1)[-1]}@{b}": count_events(u, b, f) for u, b, f in published}
    if len(counts) > 1:
        known = {k: v for k, v in counts.items() if v is not None}
        if len(known) < len(counts):
            out.append("not readable: " + ", ".join(k for k, v in counts.items() if v is None))
        elif len(set(known.values())) > 1:
            out.append("the two copies differ: " + ", ".join(f"{k} {v} games" for k, v in known.items()))
    return out


def gh(*a):
    return subprocess.run(["gh", *a], capture_output=True, text=True)


def issue(text):
    lines = [x for x in text.splitlines() if x.strip()]
    gh("label", "create", LABEL, "--color", "D93F0B", "--description", "the match archive needs a look (opened and closed by CI)")
    r = gh("issue", "list", "--label", LABEL, "--state", "open", "--json", "number,body", "--limit", "5")
    if r.returncode:
        print(r.stderr.strip()[:300])
        return 1
    open_ = json.loads(r.stdout or "[]")
    if not lines:
        for i in open_:
            gh("issue", "close", str(i["number"]), "--comment", "Healthy again: closed by the League fixtures workflow.")
            print(f"closed #{i['number']}")
        return 0
    body = ("The match archive has a problem (checked by the League fixtures workflow every time it runs; this issue closes itself when "
            "it is healthy again):\n\n" + "\n".join(f"- {x}" for x in lines) +
            "\n\nThe archive: tshi-dev/urtv-data, `football/archive` (health.json, history.json, history-report.json); the copy for the apps "
            "up to 4.3.2: `data-archive` here. A rerun of some leagues: urtv-data's «Football history» workflow, input `leagues`.")
    if open_:
        if open_[0]["body"].strip() != body.strip():
            gh("issue", "edit", str(open_[0]["number"]), "--body", body)
            print(f"updated #{open_[0]['number']}")
        return 0
    r = gh("issue", "create", "--title", TITLE, "--label", LABEL, "--body", body)
    print(r.stdout.strip() or r.stderr.strip()[:300])
    return 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--published", nargs=3, action="append", default=[], metavar=("URL", "BRANCH", "FOLDER"))
    p.add_argument("--health")
    p.add_argument("--issue")
    a = p.parse_args()
    if a.issue is not None:
        return issue(a.issue)
    print("\n".join(problems(a.published, a.health)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
