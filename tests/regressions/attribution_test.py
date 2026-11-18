#!/usr/bin/env python3
"""The owner's rule (18 Nov 2026): every commit released carries tshi-dev as its author AND its committer
(tshi-dev <272740088+tshi-dev@users.noreply.github.com>), and no message has a «Co-Authored-By: Claude», «Claude-Session:» or «Generated with
… Claude» line. Checked for the commits being released: from the newest v* release tag before HEAD to HEAD. History is never rewritten
(no force-push): what came before the rule and is still in that range is listed in ALLOW, each with its reason.

Needs the history back to that tag: regressions.yml (the release gate) fetches it and sets ATTRIBUTION_STRICT=1, so a missing history
fails there; a shallow checkout elsewhere (checks.yml) says SKIP.
Run: python3 tests/regressions/attribution_test.py"""
import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WHO = "tshi-dev <272740088+tshi-dev@users.noreply.github.com>"
BAD = re.compile(r"^\s*(Co-Authored-By:\s*Claude\b|Claude-Session:|.*Generated with \[?Claude)", re.I | re.M)

# sha (at least 8 hex) -> why it may stay; only commits from before the rule (18 Nov 2026), never a new one without the owner
ALLOW = {
    "f3ec1914": "17 Nov, before the rule: a Co-Authored-By line (PR #45 ported); history is not rewritten",
    "e1500db8": "17 Nov, before the rule: a Co-Authored-By line (PR #18 ported); history is not rewritten",
    "a48f4ed0": "17 Nov, before the rule: a Co-Authored-By line (PR #23 ported); history is not rewritten",
    "b8d1de22": "17 Nov, before the rule: a Co-Authored-By line (PR #17 ported); history is not rewritten",
    "22fe0146": "16 Nov, before the rule: a merge made under a default Claude identity; history is not rewritten",
    "20f5e8da": "16 Nov, before the rule: a merge made under a default Claude identity; history is not rewritten",
    "85f6ca2a": "16 Nov, before the rule: a merge made under a default Claude identity; history is not rewritten",
    # contributors' own commits, kept under their names, that 4.4 brings in through the owner's «Merge PR #N» commits
    "47b5f01d": "a contributor's commit of PR #59 (remote buttons), merged by the owner in 26e4f7c0",
    "5013cb8d": "a contributor's commit of PR #59 (remote buttons), merged by the owner in 26e4f7c0",
    "c5d49a7f": "a contributor's commit of PR #59 (remote buttons), merged by the owner in 26e4f7c0",
    "e44c4e64": "a contributor's commit of PR #10 (self-update), merged by the owner in 467a2e61",
    "3fe28e41": "a contributor's commit of PR #10 (self-update), merged by the owner in 467a2e61",
    "c17d80d1": "a contributor's commit of PR #30 (club crests), merged by the owner in 3ce8b5d7",
}

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def git(*a):
    return subprocess.run(["git", *a], cwd=ROOT, capture_output=True, text=True)


def problems(commits):
    """commits: [(sha, author, committer, message)] -> [(sha, what)] for those that break the rule and are not allowed"""
    out = []
    for sha, author, committer, msg in commits:
        if any(sha.startswith(a) for a in ALLOW):
            continue
        if author != WHO:
            out.append((sha[:8], "author is not tshi-dev"))
        if committer != WHO:
            out.append((sha[:8], "committer is not tshi-dev"))
        if BAD.search(msg):
            out.append((sha[:8], "an AI attribution line: " + BAD.search(msg).group(0).strip()[:40]))
    return out


def released():
    head = git("rev-parse", "HEAD").stdout.strip()
    for t in git("tag", "--merged", "HEAD", "--sort=-v:refname", "--list", "v*").stdout.split():
        if git("rev-list", "-n1", t).stdout.strip() != head:
            return t
    return None


def main():
    # the rule itself, on made-up commits
    ok_c = ("a" * 40, WHO, WHO, "A fix\n\nThe body.")
    probe = problems([ok_c, ("b" * 40, "Claude <noreply@anthropic.com>", WHO, "x"), ("c" * 40, WHO, "GitHub <noreply@github.com>", "x"),
                      ("d" * 40, WHO, WHO, "x\n\nCo-Authored-By: Claude Opus <noreply@anthropic.com>"),
                      ("e" * 40, WHO, WHO, "x\n\nClaude-Session: https://example.com/s"), ("f" * 40, WHO, WHO, "x\n\n🤖 Generated with [Claude Code](https://example.com)"),
                      ("f3ec1914" + "0" * 32, "x", "x", "Co-Authored-By: Claude")])
    check("the rule finds another author, another committer and each AI line, passes a clean commit and an allowed one",
          [p[0][0] for p in probe] == ["b", "c", "d", "e", "f"], probe)
    check("every allowed sha is 8+ hex with its reason", all(re.fullmatch(r"[0-9a-f]{8,40}", a) and r for a, r in ALLOW.items()))

    tag = released()
    cut = False
    if tag and git("rev-parse", "--is-shallow-repository").stdout.strip() == "true":   # a shallow clone: whole only if no cut lies in the range
        shallow = set((ROOT / git("rev-parse", "--git-dir").stdout.strip() / "shallow").read_text().split())
        cut = bool(shallow & set(git("rev-list", f"{tag}..HEAD").stdout.split()))
    if not tag or cut:
        msg = "no release tag in this (shallow?) history: the commits since the last release cannot be listed"
        if os.environ.get("ATTRIBUTION_STRICT") == "1":
            check(msg, False, "fetch the full history (fetch-depth: 0)")
        else:
            print("SKIP " + msg + " (the release gate, regressions.yml, checks it with the full history)")
        return 1 if failed else 0
    log = git("log", "--format=%H%x1f%an <%ae>%x1f%cn <%ce>%x1f%B%x1e", f"{tag}..HEAD").stdout
    commits = [tuple(x.strip("\n").split("\x1f")) for x in log.split("\x1e") if x.strip()]
    bad = problems(commits)
    check(f"the {len(commits)} commits since {tag}: author and committer tshi-dev, no AI attribution line", not bad, bad[:20])
    unused = [a for a in ALLOW if not any(c[0].startswith(a) for c in commits)]
    if unused:
        print("NOTE allowed commits no longer in the released range (their lines can go once every branch is past them): " + ", ".join(unused))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
