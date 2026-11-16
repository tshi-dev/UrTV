"""The other settings the tools change besides the tweaks: measure.sh stretches the TV's screen-off timeout while it reads, and
dock.sh sleep-setup turns on an accessibility service. Each puts back what the TV had (null: deleted, not a guess), also after a
run that died, and says so with a !! line when the TV does not take the value. Fake TVs; no TV needed.

    python3 tests/settings_audit_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STUB = r'''#!/usr/bin/env bash
shift 2
S="$T/tv"; mkdir -p "$S"
key() { echo "$1" | tr ' /' '__'; }
case "$1" in
  "settings get "*) [ -n "$EMPTYGET" ] && case "$1" in *"$EMPTYGET"*) exit 0 ;; esac
       IFS=';' read -ra parts <<< "$1"   # «settings get a b; settings get c d»
       for part in "${parts[@]}"; do part=${part# }; f="$S/$(key "${part#settings get }")"; [ -f "$f" ] && cat "$f" || echo null; done ;;
  "settings put "*) set -- $1; ns=$3; k=$4; shift 4; echo "put $ns $k $*" >> "$T/log"
       case " $REFUSE " in *" $k "*) ;; *) echo "$*" > "$S/$(key "$ns $k")" ;; esac ;;
  "settings delete "*) set -- $1; echo "delete $3 $4" >> "$T/log"; rm -f "$S/$(key "$3 $4")" ;;
  --batch) while read -r l; do echo "batch $l" >> "$T/log"; done ;;
  --reboot) echo reboot >> "$T/log" ;;
  "pm list packages sa.urtv.extras") echo package:sa.urtv.extras ;;   # 4.3.3: «إضافات تلفزيونك» is installed
esac
exit 0
'''
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def tree(files, **settings):
    t = Path(tempfile.mkdtemp()); tr = t / "tree"; (tr / "optimize" / "run").mkdir(parents=True); (t / "tv").mkdir()
    for f in files: shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    (t / "py").write_text(STUB); (t / "py").chmod(0o755)
    for k, v in settings.items(): (t / "tv" / k).write_text(v + "\n")
    return t, tr

def sh(t, tr, script, *args, refuse="", **extra):
    env = dict(os.environ, T=str(t), PY=str(t / "py"), REFUSE=refuse, SETTLE_S="0", EMPTYGET=extra.pop("emptyget", ""), **extra)
    r = subprocess.run(["bash", str(tr / "optimize" / script), *args], env=env, capture_output=True, text=True, cwd=tr)
    log = (t / "log").read_text().splitlines() if (t / "log").exists() else []
    return r.returncode, r.stdout + r.stderr, log

# measure.sh: the screen-off timeout
t, tr = tree(["measure.sh"], system_screen_off_timeout="600000")
rc, out, log = sh(t, tr, "measure.sh", "after", "--now")
check("measure: the TV's own timeout is put back exactly", rc == 0 and log.index("put system screen_off_timeout 1800000") < log.index("put system screen_off_timeout 600000"), str(log))
check("measure: the reading shows the tweaks it was taken with", "tweaks at this reading:" in out, out)
t, tr = tree(["measure.sh"])   # screen_off_timeout never set
rc, out, log = sh(t, tr, "measure.sh", "after", "--now")
check("measure: a timeout that was not set is deleted afterwards, not written as 300000", "delete system screen_off_timeout" in log and not any("300000" in l for l in log), str(log))
t, tr = tree(["measure.sh"], system_screen_off_timeout="1800000")   # the TV restarted in the middle of an earlier reading: the file is left
(tr / "optimize/run/.screen-off-timeout").write_text("420000\n")
rc, out, log = sh(t, tr, "measure.sh", "after", "--now")
check("measure: after a run that died, the next one puts the TV's recorded timeout back first", log[0] == "put system screen_off_timeout 420000", str(log))
check("measure: and leaves no file behind", not (tr / "optimize/run/.screen-off-timeout").exists())

t, tr = tree(["measure.sh"], system_screen_off_timeout="600000")
rc, out, log = sh(t, tr, "measure.sh", "after", "--now", emptyget="screen_off_timeout")
check("measure: an empty answer for the timeout is NOT «not set»: nothing is stretched or deleted, a !! line says so",
      "!! screen_off_timeout: the TV did not say its value" in out and not any("screen_off_timeout" in l for l in log), f"{out} {log}")

# dock.sh sleep-setup: the accessibility service
svc = "sa.urtv.extras/sa.urtv.home.ExtrasA11yService"   # 4.3.3: the add-on «إضافات تلفزيونك»
t, tr = tree(["dock.sh"], secure_accessibility_enabled="0", secure_enabled_accessibility_services="other.app/.Svc")
rc, out, log = sh(t, tr, "dock.sh", "sleep-setup")
check("sleep-setup: what the TV had is recorded once, and the service is added to the list, not replacing it",
      (tr / "optimize/run/accessibility-before.txt").read_text().strip() == "accessibility_enabled=0"
      and f"put secure enabled_accessibility_services 'other.app/.Svc:{svc}'" in log and "!!" not in out, f"{out} {log}")
t, tr = tree(["dock.sh"])
rc, out, log = sh(t, tr, "dock.sh", "sleep-setup", refuse="enabled_accessibility_services")
check("sleep-setup: a TV that does not take the service gets a !! line", "!! enabled_accessibility_services" in out, out)
check("sleep-setup: an unset switch is recorded as null", (tr / "optimize/run/accessibility-before.txt").read_text().strip() == "accessibility_enabled=null")

# reset.sh: our entry leaves the list, the rest stays; with none left the switch goes back to what it was
src = (ROOT / "optimize/reset.sh").read_text(encoding="utf-8")
step = src[src.index("# the sleep service (an accessibility"):src.index('echo "3/4 Removing our apps')]
def reset_step(listing, before):
    t, tr = tree([], secure_enabled_accessibility_services=listing)
    (tr / "optimize/run/accessibility-before.txt").write_text(f"accessibility_enabled={before}\n")
    out = t / "sent"
    script = f'tv() {{ if [ "$1" = --batch ]; then cat >> "{out}"; else "$PY" -I optimize/tv.py "$@"; fi; }}\ncd "{tr}"\n' + step
    subprocess.run(["bash", "-c", script], env=dict(os.environ, T=str(t), PY=str(t / "py")), capture_output=True, text=True)
    return out.read_text().splitlines() if out.exists() else []
sent = reset_step(f"other.app/.Svc:{svc}", "0")
check("reset: only our entry leaves the list (the list in single quotes)", sent == ["settings put secure enabled_accessibility_services 'other.app/.Svc'"], str(sent))
sent = reset_step(f"other.app/.Svc$Inner:{svc}", "0")
check("reset: a service with a $ in its name is sent in single quotes, not expanded", sent == ["settings put secure enabled_accessibility_services 'other.app/.Svc$Inner'"], str(sent))
sent = reset_step(f"other.app/.Svc;reboot:{svc}", "0")
check("reset: a list with a character the script does not send is not sent (ours stays, and it says so)", not any("put secure enabled" in x for x in sent), str(sent))
sent = reset_step("", "0")
check("reset: an empty answer (the TV did not say) is NOT «no service on»: the switch is left as it is, with a !! line", sent == [], str(sent))
sent = reset_step("null", "0")
check("reset: the list is already empty (Android dropped our entry): the switch is still put back from the record", sent == ["settings put secure accessibility_enabled 0"], str(sent))
sent = reset_step("null", "null")
check("reset: empty list and a switch that was never set: deleted", sent == ["settings delete secure accessibility_enabled"], str(sent))
check("reset: the sleep service is taken out BEFORE our app is uninstalled (Android would drop it by itself)", src.index("# the sleep service (an accessibility") < src.index('tv "pm uninstall $p"'))
sent = reset_step(svc, "null")
check("reset: nothing else left: the list is deleted and the switch is as it was (null: deleted)",
      sent == ["settings delete secure enabled_accessibility_services", "settings delete secure accessibility_enabled"], str(sent))
sent = reset_step(svc, "0")
check("reset: and a switch that was 0 goes back to 0", sent == ["settings delete secure enabled_accessibility_services", "settings put secure accessibility_enabled 0"], str(sent))
sys.exit(1 if failed else 0)
