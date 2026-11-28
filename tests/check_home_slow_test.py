"""optimize/check-home.sh on a slow TV: the app and the home may take more than 8 s to come to the front (R-365).

It waited a fixed 8 s once, then said «!! No app could be opened» (or tested nothing) on a TV that was still opening
the app. Now the front is read once a second for up to 30 s, and the app is started (and Home pressed) once more.
A fake tv.py here brings the app to the front 12 s after it is started, and the home 5 s after Home.

    python3 tests/check_home_slow_test.py          (no TV needed, about 30 s)
"""
import os, stat, subprocess, sys, tempfile
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (f": {detail}" if detail and not ok else ""))
    if not ok:
        bad.append(name)

def run(app_delay, home_delay, monkey_works_on=1, wait=None):
    d = tempfile.mkdtemp()
    fake = os.path.join(d, "py")
    with open(fake, "w") as f:   # «$PY -I optimize/tv.py <command>»: the TV's answers, with the time each thing came to the front
        f.write(f"""#!/usr/bin/env bash
cmd="$3"; st="{d}"
now=$(date +%s)
case "$cmd" in
  monkey*) n=$(( $(cat "$st/monkeys" 2> /dev/null || echo 0) + 1 )); echo $n > "$st/monkeys"
           [ "$n" -ge {monkey_works_on} ] && echo "$now" > "$st/app_at"; echo "Events injected: 1" ;;
  *KEYCODE_HOME*) echo "$now" > "$st/home_at" ;;
  dumpsys*) if [ -f "$st/home_at" ] && [ $((now - $(cat "$st/home_at"))) -ge {home_delay} ]; then echo "  topResumedActivity=ActivityRecord{{1 u0 sa.urtv.home/.Home t1}}"
            elif [ -f "$st/app_at" ] && [ $((now - $(cat "$st/app_at"))) -ge {app_delay} ]; then echo "  topResumedActivity=ActivityRecord{{2 u0 com.example.app/.Main t2}}"
            else echo "  topResumedActivity=ActivityRecord{{3 u0 com.stock.launcher/.Main t3}}"; fi ;;
esac
""")
    os.chmod(fake, os.stat(fake).st_mode | stat.S_IEXEC)
    r = subprocess.run(["bash", "optimize/check-home.sh", "com.example.app"], cwd=ROOT, capture_output=True, text=True,
                       env={**os.environ, "PY": fake, **({"CHECK_HOME_WAIT": str(wait)} if wait else {})}, timeout=240)
    return r.returncode, r.stdout + r.stderr

rc, out = run(app_delay=12, home_delay=5)
check("an app that comes to the front after 12 s, a home after 5 s: OK", rc == 0 and "OK: Home opens" in out, out.strip())
rc, out = run(app_delay=1, home_delay=1, monkey_works_on=2, wait=8)
check("an app that did not start the first time is started once more", rc == 0 and "started once more" in out, out.strip())
src = open(os.path.join(ROOT, "optimize", "check-home.sh"), encoding="utf-8").read()
check("the wait is 30 s by default", 'WAIT="${CHECK_HOME_WAIT:-30}"' in src)
sys.exit(1 if bad else 0)
