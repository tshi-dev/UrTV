"""optimize/install.sh curl_get: a 5xx answer is tried again with a longer wait, a 4xx is not (R-366).

One 503 from the release server stopped the setup: curl -f ends with 22 for every HTTP error, and 22 was never tried
again. curl_get is taken out of install.sh and run with a fake curl and a fake sleep.

    python3 tests/curl_5xx_test.py          (no TV, no network)
"""
import os, re, subprocess, sys, tempfile
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (f": {detail}" if detail and not ok else ""))
    if not ok:
        bad.append(name)

src = open(os.path.join(ROOT, "optimize", "install.sh"), encoding="utf-8").read()
fn = re.search(r"^curl_get\(\) \{.*?^\}\n", src, re.S | re.M).group(0)

def run(answers):   # answers: one HTTP code per try (200 = the file)
    d = tempfile.mkdtemp()
    script = f"""set -euo pipefail
n=0; answers=({' '.join(map(str, answers))})
curl() {{ case "$1" in -V|--help) return 0;; esac; local a=${{answers[$n]:-200}}; n=$((n + 1)); echo "$n" > "{d}/tries"
  if [ "$a" = 200 ]; then return 0; fi; echo "curl: (22) The requested URL returned error: $a" >&2; return 22; }}
sleep() {{ echo "$1" >> "{d}/sleeps"; }}
{fn}
rc=0; curl_get -fsSL -o x https://example.invalid/urtv-home.apk || rc=$?
echo "rc=$rc tries=$(cat {d}/tries) sleeps=$(paste -sd, {d}/sleeps 2> /dev/null)"
"""
    r = subprocess.run(["bash", "-c", script], capture_output=True, text=True, timeout=30)
    return r.stdout.strip().splitlines()[-1] if r.stdout.strip() else r.stderr

check("503 then the file: downloaded on the second try", run([503, 200]).startswith("rc=0 tries=2"), run([503, 200]))
out = run([502, 503, 504])
check("three 5xx: three tries, then the error", out.startswith("rc=22 tries=3"), out)
check("the waits grow (5 s, then 10 s)", out.endswith("sleeps=5,10"), out)
out = run([404, 200])
check("a 404 is not tried again", out.startswith("rc=22 tries=1"), out)
sys.exit(1 if bad else 0)
