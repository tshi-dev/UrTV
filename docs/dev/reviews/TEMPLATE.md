# Review of <version>

Range: <tag>..<sha>
Reviewer: <a fresh session or subagent: say which, never a person's name>
Date: <yyyy-mm-dd>
Read: <N> commits, <M> files (git diff --stat <tag>..<sha> | tail -1)
API surface diff: <PASS/FAIL, and its new lines worth a word>
Regressions group: <PASS/FAIL>

Summary: <blocker n, high n, medium n, low n; one sentence on the release's risk>

## Rules

The checklist rules of docs/dev/RELEASE.md («The LESSONS rules»): each `ok`, `n/a` or a finding below.
R2: · R5: · R6: · R7: · R8: · R13: · R19: · R20: · R22: · R23: · R24: · R25:

## Findings

### F1 [high] [open] <one-line title>

- Where: `path/File.java:123`
- What a person sees: <on which TV, after which step>
- Why: <the cause in the code>
- Smallest fix: <one or two lines>

<!-- status: [open] → [fixed] (commit <sha>) or [accepted] («accepted by the owner»: their reason). Severity: blocker = a TV can be
bricked, lose its setup or its updates; high = a feature most people use breaks, or the remote freezes; medium = wrong on some TVs or
settings; low = polish. With nothing to report, write the line «No findings.» instead of headings. -->
