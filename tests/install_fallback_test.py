#!/usr/bin/env python3
"""4.4 (reports, causes 3 and 4): install.sh's fallbacks. «Unable to open file» twice on the file way (Nokia Streaming Box 8000, TCL BeyondTV4): the APK
is streamed once more (no file to open). INSTALL_FAILED_VERIFICATION_FAILURE (Play Protect; the launcher, TV Bro): the ADB check off for this install
only, then, still refused, an Arabic line for the person, a wait for their «تثبيت على أي حال», one more try; the same for the extras. A stub tv.py.
Run: python3 tests/install_fallback_test.py"""
import os, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

src = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
def fn(name): i = src.index(name + "() {"); return src[i:src.index("\n}\n", i) + 3]

def verify(answers, verifier="null"):
    t = Path(tempfile.mkdtemp())
    (t / "answers").write_text("\n".join(answers) + "\n")
    (t / "py").write_text(f'''#!/usr/bin/env bash
shift 2
echo "$*" >> "{t}/log"
case "$1" in "settings get global"*) printf '%s\\n' "$VANSWER" ;; esac
''')
    (t / "py").chmod(0o755)
    script = (f'PY="{t}/py"; VERIFIER=verifier_verify_adb_installs; verifier_set=0\n'
              f'pm_install() {{ n=$(( $(cat "{t}/n" 2>/dev/null || echo 0) + 1 )); echo $n > "{t}/n"; sed -n "${{n}}p" "{t}/answers"; }}\n'
              f'sleep() {{ echo "sleep $1" >> "{t}/log"; }}\n' + fn("verify_retry") + '\nout=""; verify_retry urtv-home.apk sa.urtv.home; rc=$?; echo "RC=$rc"; echo "OUT=$out"; echo "SET=$verifier_set"')
    r = subprocess.run(["bash", "-c", script], capture_output=True, text=True, env=dict(os.environ, VANSWER=verifier))
    return r.stdout, (t / "log").read_text() if (t / "log").exists() else ""

V = "Failure [INSTALL_FAILED_VERIFICATION_FAILURE: Install not allowed]"
o, log = verify(["Success"])
check("Play Protect: the ADB check off for the install, the retry succeeds, the check marked to be put back", "RC=0" in o and "OUT=Success" in o and "SET=1" in o
      and "settings put global verifier_verify_adb_installs 0" in log and "sleep" not in log, (o, log))
o, log = verify([V, "Success"])
check("still refused: the person is told in Arabic, the run waits 60 s, then one more try that succeeds", "RC=0" in o and "OUT=Success" in o and "sleep 60" in log
      and "اضغط «تثبيت على أي حال»" in o and "Play Protect still refused it" in o, (o, log))
o, log = verify([V, V])
check("refused every time: the last answer stays (the caller prints its !! with the hint)", "OUT=" + V in o, o)
o, log = verify(["Success"], verifier="")
check("the TV did not say its ADB check: nothing changed, return 2", "RC=2" in o and "put global" not in log and "SET=0" in o, (o, log))

pi = src[src.index("push_install() {"):]
check("«Unable to open file» twice: the APK is streamed once more (stream_again), its answer taken",
      pi.index('out=$(pm_install "$1" "$2")\n  fi\n  if grep -q "Unable to open file"') > 0 and 'local streamed; streamed=$(stream_again "$1")' in pi
      and "[ -z \"$streamed\" ] || out=$streamed" in pi and 'stream_again() {   # $1 file name in optimize/' in src and '--install-stream "optimize/$1" -r' in fn("stream_again"))
check("push_install: Play Protect goes through verify_retry (a stream refusal gets the file back first)", "verify_retry \"$1\" \"$2\" || exit 1" in pi)
fg = fn("from_github")
check("the extras (TV Bro): the same retry and hint, an extra that stays refused is a failed extra, not the end of the run",
      'verify_retry "$name" "$2" || { failed=1; return 0; }' in fg and fg.index("verify_retry") < fg.index("skipped after 2 timeouts"))
sys.exit(1 if failed else 0)
