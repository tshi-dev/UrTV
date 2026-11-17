#!/usr/bin/env python3
"""Release checklist step 3 (docs/dev/RELEASE.md): the independent review of this version exists and has no blocker or high finding still
open. Reads docs/dev/reviews/<version>.md (the version from VERSION unless given); each finding is a heading
`### F<n> [<severity>] [<status>] <title>`, severity blocker|high|medium|low, status open|fixed|accepted (accepted needs the owner's
word in the finding: «accepted by the owner»). Prints the counts; exits 1 when the file is missing, malformed, reviews another
range than the one released, or a blocker/high is open (or accepted without the owner).
Run: python3 .github/tools/review_gate.py [<version>]"""
import re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HEAD = re.compile(r"^### (F\d+) \[(blocker|high|medium|low)\] \[(open|fixed|accepted)\] (.+)$", re.M)


def main():
    ver = sys.argv[1] if len(sys.argv) > 1 else (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    p = ROOT / "docs/dev/reviews" / f"{ver}.md"
    if not p.exists():
        print(f"FAIL no review for {ver}: {p.relative_to(ROOT)} (docs/dev/RELEASE.md, step 3)")
        return 1
    t = p.read_text(encoding="utf-8")
    bad = []
    m = re.search(r"^Range: (\S+)\.\.(\S+)$", t, re.M)
    if not m:
        bad.append("no «Range: <tag>..<sha>» line")
    else:
        try:
            subprocess.run(["git", "merge-base", "--is-ancestor", m.group(2), "HEAD"], cwd=ROOT, check=True, capture_output=True)
            left = subprocess.run(["git", "diff", "--name-only", m.group(2), "HEAD", "--", "android", "src", "dist", "optimize", "VERSION"],
                                  cwd=ROOT, check=True, capture_output=True, text=True).stdout.split()
            if left:
                bad.append(f"the review ends at {m.group(2)}, but these files changed after it: {' '.join(left[:10])}")
        except subprocess.CalledProcessError:
            bad.append(f"the reviewed sha {m.group(2)} is not in this history")
    fs = HEAD.findall(t)
    if not fs and "No findings." not in t:
        bad.append("no finding headings and no «No findings.» line")
    for fid, sev, st, title in fs:
        body = t.split(f"### {fid} ", 1)[1].split("\n### ", 1)[0]
        if sev in ("blocker", "high") and st == "open":
            bad.append(f"{fid} [{sev}] is open: {title}")
        if sev in ("blocker", "high") and st == "accepted" and "accepted by the owner" not in body:
            bad.append(f"{fid} [{sev}] is accepted without «accepted by the owner»: {title}")
    count = {s: sum(1 for f in fs if f[1] == s) for s in ("blocker", "high", "medium", "low")}
    print(f"review {ver}: {len(fs)} findings {count}")
    for b in bad:
        print("FAIL " + b)
    if not bad:
        print(f"PASS the review of {ver} has no blocker or high open")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
