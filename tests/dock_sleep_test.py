"""optimize/dock.sh sleep / sleep-setup: what they send to the TV and how they refuse bad arguments (no TV needed).

    python3 tests/dock_sleep_test.py

PY is pointed at a stand-in for optimize/tv.py that writes the shell command it was given to a file and answers like `am broadcast`
(or like `settings get`, with what GETS says).
"""
import os, subprocess, tempfile

here = os.path.dirname(os.path.abspath(__file__))
dock = os.path.join(here, "..", "optimize", "dock.sh")
tmp = tempfile.mkdtemp()
log = os.path.join(tmp, "sent.txt")
fake = os.path.join(tmp, "fakepy")
with open(fake, "w") as f:
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\ncase "$3" in "dumpsys accessibility") printf "%s\\n" "${DUMP-}" ;; "settings get"*";"*) echo "${GETS-null}"; echo "${GETS2-null}" ;; "settings get"*) echo "${GETS-null}" ;; *) echo \'Broadcast completed: result=0, data="sleep: ok"\' ;; esac\n')
os.chmod(fake, 0o755)

failed = 0
def run(*args, gets="null", dump=""):
    open(log, "w").close()
    r = subprocess.run(["bash", dock, *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log, "GETS": gets, "DUMP": dump, "URTV_RUN_DIR": os.path.join(tmp, "run")})
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip().split("\n")

def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

A = "am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SLEEP_TV"
rc, out, sent = run("sleep")
check("sleep: no argument asks how it stands", rc == 0 and sent == [A] and out.strip() == "sleep: ok", (rc, out, sent))
for m in ("15", "30", "60", "90", "1", "600"):
    rc, out, sent = run("sleep", m)
    check(f"sleep {m}: starts it", rc == 0 and sent == [A + " --ei minutes " + m], sent)
rc, out, sent = run("sleep", "cancel")
check("sleep cancel: stops it", rc == 0 and sent == [A + " --ei minutes 0"], sent)
for bad, why in [("abc", "words"), ("-5", "a negative number"), ("1.5", "a fraction"), ("601", "over 600 minutes"), ("30; reboot", "a shell command"), ("", "nothing")]:
    rc, out, sent = run("sleep", bad)
    check(f"sleep: refuses {why}, sends nothing", rc != 0 and sent == [""], (rc, out, sent))
SVC = "sa.urtv.home/sa.urtv.home.SleepService"
rc, out, sent = run("sleep-setup")
check("sleep-setup: nothing enabled yet: turns on the sleep service alone, and accessibility",
      rc == 0 and sent == ["settings get secure enabled_accessibility_services; settings get secure accessibility_enabled",
                           "settings put secure enabled_accessibility_services '" + SVC + "'", "settings put secure accessibility_enabled 1",
                           "settings get secure enabled_accessibility_services"], sent)
rc, out, sent = run("sleep-setup", gets="")
check("sleep-setup: an empty answer (the TV did not say) is NOT «no service on»: stops with a !! line, nothing is put",
      rc == 1 and "!! enabled_accessibility_services: the TV did not say" in out and not any("settings put" in x for x in sent), (out, sent))
rc, out, sent = run("sleep-setup", gets="com.other/com.other.Svc")
check("sleep-setup: another accessibility service is kept, ours added after it",
      rc == 0 and sent[1] == "settings put secure enabled_accessibility_services 'com.other/com.other.Svc:" + SVC + "'", sent)
rc, out, sent = run("sleep-setup", gets="com.other/com.other.Svc:" + SVC)
check("sleep-setup: already on: the list is left as it is", rc == 0 and "already" in out and not any("enabled_accessibility_services com" in x for x in sent[1:]), (out, sent))
TYPE = "sa.urtv.home/sa.urtv.home.TypeService"
rc, out, sent = run("type-setup")
check("type-setup: the typing service is turned on on its own (not the sleep one), and accessibility",
      rc == 0 and sent[1] == "settings put secure enabled_accessibility_services '" + TYPE + "'" and SVC not in " ".join(sent) and "typing service is on" in out, (out, sent))
rc, out, sent = run("type-setup", gets="com.other/com.other.Svc:" + SVC)
check("type-setup: the sleep service and others stay in the list, the typing service is added after them",
      rc == 0 and sent[1] == "settings put secure enabled_accessibility_services 'com.other/com.other.Svc:" + SVC + ":" + TYPE + "'", sent)
rc, out, sent = run("type-setup", gets="")
check("type-setup: an empty answer stops with a !! line, nothing is put", rc == 1 and "!! enabled_accessibility_services: the TV did not say" in out and not any("settings put" in x for x in sent), (out, sent))
rc, out, sent = run("type-setup", gets="com.other/com.other.Svc:" + TYPE)
check("type-setup: already on: the list is left as it is", rc == 0 and "already" in out and not any("enabled_accessibility_services com" in x for x in sent[1:]), (out, sent))

# the TV test of 157 -> 159: after the app's update our service was still in the list, the switch 0 and NOTHING bound; type-setup only set the switch
KM = "io.github.sds100.keymapper/io.github.sds100.keymapper.system.accessibility.MyAccessibilityService"
LIST = KM + ":" + TYPE
rc, out, sent = run("type-setup", gets=LIST, dump="  Bound services:{}")
check("type-setup: listed but not bound (the TV says so): the list without ours, then the SAME list exactly, then the switch",
      rc == 0 and sent[1:4] == ["dumpsys accessibility", "settings put secure enabled_accessibility_services '" + KM + "'",
                                "settings put secure enabled_accessibility_services '" + LIST + "'"] and "settings put secure accessibility_enabled 1" in sent
      and "not running" in out, (out, sent))
check("type-setup: Key Mapper's entry is never dropped from the list that stays", all(KM in x for x in sent if "put secure enabled_accessibility_services" in x), sent)
rc, out, sent = run("type-setup", gets=TYPE, dump="  Bound services:{}")
check("type-setup: ours alone and not bound: the list is cleared for a moment, then written again as it was",
      rc == 0 and sent[2:4] == ["settings delete secure enabled_accessibility_services", "settings put secure enabled_accessibility_services '" + TYPE + "'"], sent)
rc, out, sent = run("type-setup", gets=LIST, dump="  Bound services:{Service[label=x, component=ComponentInfo{" + TYPE + "}]}")
check("type-setup: listed and bound: the list is left as it is", rc == 0 and "already on" in out
      and not any("enabled_accessibility_services '" in x for x in sent), (out, sent))
rc, out, sent = run("type-setup", gets=LIST, dump="")
check("type-setup: the TV did not say what is bound: the list is left as it is (nothing guessed)", rc == 0
      and not any("enabled_accessibility_services '" in x for x in sent), (out, sent))

print("\n" + (f"{failed} failed" if failed else "dock sleep: all passed"))
raise SystemExit(1 if failed else 0)
