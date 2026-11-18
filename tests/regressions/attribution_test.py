#!/usr/bin/env python3
"""The owner's rules on who a released commit is by (18 Nov 2026), for the commits being released (from the newest v* release tag before
HEAD to HEAD):
1. no AI identity: no author or committer named or mailed as Claude or Anthropic, and no bot but github-actions;
2. no AI attribution line: «Co-Authored-By: Claude», «Claude-Session:», «Generated with … Claude»;
3. our own commits are tshi-dev <272740088+tshi-dev@users.noreply.github.com>, author and committer. Contributors keep their own names
   (honest credit): a commit by another person is fine when it reaches the tree through a «Merge PR #N» commit by tshi-dev (it lies between
   that merge's two parents), and only then.
History is never rewritten (no force-push): what broke a rule before the rules and is still in the range is in ALLOW, with its reason.

Needs the history back to that tag: regressions.yml (the release gate) fetches it and sets ATTRIBUTION_STRICT=1, so a missing history
fails there; a shallow checkout elsewhere (checks.yml) says SKIP.
Run: python3 tests/regressions/attribution_test.py"""
import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WHO = "tshi-dev <272740088+tshi-dev@users.noreply.github.com>"
AI = re.compile(r"claude|anthropic", re.I)
BOT = re.compile(r"\[bot\]|\bbot\b", re.I)
GHA = "github-actions[bot] <41898099+github-actions[bot]@users.noreply.github.com>"
PR_MERGE = re.compile(r"^Merge (?:pull request|PR) #\d+")
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
}

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def git(*a):
    return subprocess.run(["git", *a], cwd=ROOT, capture_output=True, text=True)


def ai_identity(who):
    return bool(AI.search(who)) or (bool(BOT.search(who)) and who != GHA)


def problems(commits, contributed=frozenset()):
    """commits: [(sha, author, committer, message)]; contributed: the shas a tshi-dev «Merge PR #N» brought in
    -> [(sha, what)] for those that break a rule and are not allowed"""
    out = []
    for sha, author, committer, msg in commits:
        if any(sha.startswith(a) for a in ALLOW):
            continue
        for role, who in (("author", author), ("committer", committer)):
            if ai_identity(who):
                out.append((sha[:8], f"an AI or bot {role}"))
        if BAD.search(msg):
            out.append((sha[:8], "an AI attribution line: " + BAD.search(msg).group(0).strip()[:40]))
        if sha in contributed:
            continue   # a contributor's own commit, under their name, merged by the owner
        if author not in (WHO, GHA) and not ai_identity(author):   # github-actions commits its data updates itself
            out.append((sha[:8], "another person's commit that no «Merge PR #N» by tshi-dev brought in"))
        if committer not in (WHO, GHA) and not ai_identity(committer):
            out.append((sha[:8], "committer is not tshi-dev"))
    return out


def contributed_by_prs(tag):
    """the commits each «Merge PR #N» by tshi-dev in the range brought in: those between its first and second parent"""
    out = set()
    for ln in git("log", "--merges", "--format=%H%x1f%an <%ae>%x1f%s", f"{tag}..HEAD").stdout.splitlines():
        sha, who, subj = ln.split("\x1f")
        if who == WHO and PR_MERGE.match(subj):
            out |= set(git("rev-list", f"{sha}^1..{sha}^2").stdout.split())
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
    other = "A Contributor <1+someone@users.noreply.github.com>"
    probe = problems([ok_c, ("b" * 40, "Claude <noreply@anthropic.com>", WHO, "x"), ("c" * 40, WHO, "Claude <noreply@anthropic.com>", "x"),
                      ("d" * 40, WHO, WHO, "x\n\nCo-Authored-By: Claude Opus <noreply@anthropic.com>"),
                      ("e" * 40, WHO, WHO, "x\n\nClaude-Session: https://example.com/s"), ("f" * 40, WHO, WHO, "x\n\n🤖 Generated with [Claude Code](https://example.com)"),
                      ("1" * 40, other, other, "a fix"), ("2" * 40, "renovate[bot] <bot@example.com>", WHO, "x"),
                      ("3" * 40, other, other, "their fix"), ("4" * 40, WHO, GHA, "data"),
                      ("f3ec1914" + "0" * 32, "x", "x", "Co-Authored-By: Claude")], contributed={"3" * 40})
    check("the rules find an AI author or committer, each AI line, a bot, and a person's commit no PR merge brought in; they pass a clean commit, "
          "a contributor's merged commit under their name, github-actions and an allowed sha",
          [p[0][0] for p in probe] == ["b", "c", "d", "e", "f", "1", "1", "2"], probe)
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
    contrib = contributed_by_prs(tag)
    bad = problems(commits, contrib)
    check(f"the {len(commits)} commits since {tag}: no AI identity or line; ours by tshi-dev; {len(contrib & {c[0] for c in commits})} "
          "contributors' commits under their names, each through a «Merge PR #N» by tshi-dev", not bad, bad[:20])
    unused = [a for a in ALLOW if not any(c[0].startswith(a) for c in commits)]
    if unused:
        print("NOTE allowed commits no longer in the released range (their lines can go once every branch is past them): " + ", ".join(unused))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
