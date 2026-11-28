"""Setup steps under `set -o pipefail` do not end with exit 141 (SIGPIPE) from a reader that quits early (R-364).

`cmd | grep -q x` and `cmd | head -n 1` end their reader at the first match; the writer then gets SIGPIPE, the pipeline
returns 141, and with pipefail that reads as «not found» in an if, or stops the step under set -e. The PC-side
scripts read the whole input instead (`grep x > /dev/null`, `sed -n 1p`); run.sh runs a step that ended with 141
again, and counts a second 141 as done only for the measure steps, which change nothing on the TV.

    python3 tests/sigpipe_test.py          (no TV needed)
"""
import glob, os, re, subprocess, sys
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (f": {detail}" if detail and not ok else ""))
    if not ok:
        bad.append(name)

def rc(cmd):
    return subprocess.run(["bash", "-c", "set -o pipefail; " + cmd], capture_output=True).returncode
check("the early reader really gives 141 under pipefail (what this guards)", rc("seq 1 300000 | head -n 1 > /dev/null") == 141)
check("sed -n 1p reads it all: 0", rc("seq 1 300000 | sed -n 1p > /dev/null") == 0)
check("grep without -q reads it all: 0", rc("seq 1 300000 | grep -x 1 > /dev/null") == 0)

def outside_quotes(line):   # the text of a line outside '…' and "…" (a TV command in quotes runs in the TV's sh, without pipefail)
    out, q = [], None
    for c in line:
        if q:
            if c == q:
                q = None
        elif c in "'\"":
            q = c
        else:
            out.append(c)
    return "".join(out)

EARLY = re.compile(r"\|\s*(grep\s+-[A-Za-z]*q|head\b)")
hits = []
for f in sorted(glob.glob(os.path.join(ROOT, "optimize", "*.sh"))):
    for n, line in enumerate(open(f, encoding="utf-8"), 1):
        if line.lstrip().startswith("#") or "pipefail" not in open(f, encoding="utf-8").read():
            continue
        if EARLY.search(outside_quotes(line.split(" # ")[0])):
            hits.append(f"{os.path.basename(f)}:{n}")
check("no early-quitting reader in a pipeline of a pipefail script in optimize/", not hits, ", ".join(hits))

run = open(os.path.join(ROOT, "optimize", "run.sh"), encoding="utf-8").read()
check("run.sh runs a step that ended with 141 again", re.search(r'if \[ "\$rc" = 141 \]; then.*?run_step "\$s"', run, re.S) is not None)
ok_steps = re.search(r'^SIGPIPE_OK="([^"]*)"', run, re.M)
check("a second 141 is done only for the steps that change nothing (the measures)",
      ok_steps is not None and set(ok_steps.group(1).split()) == {"before", "before-saver", "after", "after-saver"})
sys.exit(1 if bad else 0)
