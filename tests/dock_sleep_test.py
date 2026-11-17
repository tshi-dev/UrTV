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
    f.write('#!/usr/bin/env bash\nprintf "%s\\n" "$3" >> "$SENT"\ncase "$3" in "pm list packages sa.urtv.extras") [ "${NOEXTRAS-}" = 1 ] || echo package:sa.urtv.extras ;; "dumpsys accessibility") n=$(( $(cat "$SENT.d" 2>/dev/null || echo 0) + 1 )); echo $n > "$SENT.d"; if [ "$n" -ge 2 ] && [ -n "${DUMP_AFTER-}" ]; then printf "%s\\n" "$DUMP_AFTER"; else printf "%s\\n" "${DUMP-}"; fi ;; "settings get"*";"*) echo "${GETS-null}"; echo "${GETS2-null}" ;; "settings get"*) echo "${GETS-null}" ;; *) echo \'Broadcast completed: result=0, data="sleep: ok"\' ;; esac\n')
os.chmod(fake, 0o755)

failed = 0
def run(*args, gets="null", dump="", noextras="", dump_after=""):
    open(log, "w").close()
    if os.path.exists(log + ".d"): os.remove(log + ".d")
    r = subprocess.run(["bash", dock, *args], capture_output=True, text=True, env={**os.environ, "PY": fake, "SENT": log, "GETS": gets, "DUMP": dump, "NOEXTRAS": noextras, "DUMP_AFTER": dump_after, "URTV_RUN_DIR": os.path.join(tmp, "run")})
    sent = open(log, encoding="utf-8").read().strip().split("\n")
    if sent and sent[0] == "pm list packages sa.urtv.extras": sent = sent[1:]   # 4.3.3: first, is «إضافات تلفزيونك» installed
    return r.returncode, r.stdout + r.stderr, sent

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
# 4.3.3: one service, the add-on's («إضافات تلفزيونك»); sleep-setup and type-setup are the old names of extras-setup
SVC = TYPE = "sa.urtv.extras/sa.urtv.home.ExtrasA11yService"
OLD = "sa.urtv.home/sa.urtv.home.SleepService"
rc, out, sent = run("extras-setup", noextras="1")
check("extras-setup: the add-on not installed: stops with a !! line naming install.sh addon, nothing is read or put", rc == 1 and "optimize/install.sh addon" in out and not any("settings" in x for x in sent), (out, sent))
for cmd in ("extras-setup", "sleep-setup", "type-setup"):
    rc, out, sent = run(cmd)
    check(f"{cmd}: nothing enabled yet: turns on the add-on's service alone, and accessibility",
          rc == 0 and sent[:4] == ["settings get secure enabled_accessibility_services; settings get secure accessibility_enabled",
                               "settings put secure enabled_accessibility_services '" + SVC + "'", "settings put secure accessibility_enabled 1",
                               "settings get secure enabled_accessibility_services"] and "«إضافات تلفزيونك» is on" in out, (out, sent))
rc, out, sent = run("extras-setup", gets="")
check("extras-setup: an empty answer (the TV did not say) is NOT «no service on»: stops with a !! line, nothing is put",
      rc == 1 and "!! enabled_accessibility_services: the TV did not say" in out and not any("settings put" in x for x in sent), (out, sent))
rc, out, sent = run("extras-setup", gets="com.other/com.other.Svc")
check("extras-setup: another accessibility service is kept, ours added after it",
      rc == 0 and sent[1] == "settings put secure enabled_accessibility_services 'com.other/com.other.Svc:" + SVC + "'", sent)
rc, out, sent = run("extras-setup", gets="com.other/com.other.Svc:" + OLD)
check("extras-setup: the home's own service of 4.3.2 (gone from the app) leaves the list, the others stay, ours is added",
      rc == 0 and sent[1] == "settings put secure enabled_accessibility_services 'com.other/com.other.Svc:" + SVC + "'", sent)
rc, out, sent = run("extras-setup", gets="com.other/com.other.Svc:" + SVC)
check("extras-setup: already on: the list is left as it is", rc == 0 and "already" in out and not any("enabled_accessibility_services com" in x for x in sent[1:]), (out, sent))

# the TV test of 157 -> 159: after the app's update our service was still in the list, the switch 0 and NOTHING bound; type-setup only set the switch
KM = "io.github.sds100.keymapper/io.github.sds100.keymapper.system.accessibility.MyAccessibilityService"
LIST = KM + ":" + TYPE
BOUND = "  Bound services:{Service[label=x, component=ComponentInfo{" + TYPE + "}]}"
rc, out, sent = run("type-setup", gets=LIST, dump="  Bound services:{}", dump_after=BOUND)
check("type-setup: listed but not bound (the TV says so): the list without ours, then the SAME list exactly, then the switch",
      rc == 0 and sent[1:4] == ["dumpsys accessibility", "settings put secure enabled_accessibility_services '" + KM + "'",
                                "settings put secure enabled_accessibility_services '" + LIST + "'"] and "settings put secure accessibility_enabled 1" in sent
      and "not running" in out, (out, sent))
check("type-setup: Key Mapper's entry is never dropped from the list that stays", all(KM in x for x in sent if "put secure enabled_accessibility_services" in x), sent)
rc, out, sent = run("type-setup", gets=TYPE, dump="  Bound services:{}", dump_after=BOUND)
check("type-setup: ours alone and not bound: the list is cleared for a moment, then written again as it was",
      rc == 0 and sent[2:4] == ["settings delete secure enabled_accessibility_services", "settings put secure enabled_accessibility_services '" + TYPE + "'"], sent)
rc, out, sent = run("type-setup", gets=LIST, dump="  Bound services:{Service[label=x, component=ComponentInfo{" + TYPE + "}]}")
check("type-setup: listed and bound: the list is left as it is", rc == 0 and "already on" in out
      and not any("enabled_accessibility_services '" in x for x in sent), (out, sent))
rc, out, sent = run("type-setup", gets=LIST, dump="")
check("type-setup: the TV did not say what is bound: the list is left as it is (nothing guessed)", rc == 0
      and not any("enabled_accessibility_services '" in x for x in sent), (out, sent))

# 4.4 (the tester's TCL: TclAppBoot refused the first bind; a force-stopped add-on stays unbound): the bind is checked after the list is written
rc, out, sent = run("extras-setup", gets=LIST, dump="  Bound services:{}", dump_after=BOUND)
check("extras-setup: listed but not bound and bound after the list went off and on: Android's answer is read again, it says it runs, rc 0, TCL's AUTO_START and ASSOC_START allowed and written (4.4 T3)",
      rc == 0 and "Android runs it (Bound services)." in out and any("appops set sa.urtv.extras AUTO_START allow" in c and "ASSOC_START allow" in c and "appops write-settings" in c for c in sent) and sent.count("dumpsys accessibility") >= 2, (out, sent))
rc, out, sent = run("extras-setup", gets=LIST, dump="  Bound services:{}")
puts = [x for x in sent if x.startswith("settings put secure enabled_accessibility_services '" + KM + "'")]
check("extras-setup: Android keeps refusing: the list off and on twice more, then a !! line with the person's one fix (off and on in the TV's accessibility settings), rc 1",
      rc == 1 and "!! «إضافات تلفزيونك» (UrTV Extras) is in the list but Android does not run it" in out and "أطفئها وشغّلها من جديد" in out and len(puts) >= 3, (out, sent))
rc, out, sent = run("extras-setup", gets="null", dump="")
check("extras-setup: a TV that does not say what is bound: nothing guessed (no toggling), rc 0", rc == 0 and sent.count("dumpsys accessibility") == 1 and "Android runs it" not in out, (out, sent))
print("\n" + (f"{failed} failed" if failed else "dock sleep: all passed"))
raise SystemExit(1 if failed else 0)
