"""optimize/tweaks.sh records what the TV had before it changes anything, reads every value back, and restore puts exactly
that back (a value that was not set is deleted). A fake TV with settings that persist in files; no TV needed.

    python3 tests/tweaks_journal_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STUB = r'''#!/usr/bin/env bash
# fake: python -I optimize/tv.py <args>; the TV's settings are files in $T/tv (a missing file: not set)
shift 2
S="$T/tv"; mkdir -p "$S"
refused() { case " $REFUSE " in *" $1 "*) return 0 ;; esac; return 1; }
num() { case "$1" in active) echo 10 ;; working_set) echo 20 ;; frequent) echo 30 ;; rare) echo 40 ;; restricted) echo 45 ;; never) echo 50 ;; esac; }
apply_line() {
  set -- $1
  case "$1 $2" in
    "settings put"|"device_config put") refused "$4" || echo "$5" > "$S/$4" ;;
    "settings delete"|"device_config delete") refused "$4" || rm -f "$S/$4" ;;
    "am set-standby-bucket") if [ "$4" = restricted ] && [ "${SDK:-34}" -lt 30 ] 2> /dev/null; then echo "Bad bucket: restricted" >> "$T/errors"; else refused standby_bucket || num "$4" > "$S/standby_bucket"; fi ;;
    "cmd appops") refused "$5" || { [ "$6" = default ] && rm -f "$S/op_$5" || echo "$6" > "$S/op_$5"; } ;;
  esac
}
case "$1" in
  --batch) while read -r l; do echo "$l" >> "$T/batch"; apply_line "$l"; done ;;
  "settings get global "*) k=${1##* }; case " $FAILREAD " in *" $k "*) exit 1 ;; esac; case " $EMPTYREAD " in *" $k "*) exit 0 ;; esac
                           f="$S/$k"; [ -f "$f" ] && cat "$f" || echo null ;;
  "device_config get activity_manager "*) [ "${SDK:-34}" -ge 29 ] 2> /dev/null || { echo "/system/bin/sh: device_config: not found"; exit 127; }; f="$S/${1##* }"; [ -f "$f" ] && cat "$f" || echo null ;;
  "getprop ro.build.version.sdk") echo "${SDK-34}" ;;
  "am get-standby-bucket "*) [ "$UNREADABLE" = yes ] && exit 0; [ -f "$S/standby_bucket" ] && cat "$S/standby_bucket" || echo 10 ;;
  "appops get "*) [ "$UNREADABLE" = yes ] && exit 0; op=${1##* }; [ -f "$S/op_$op" ] && echo "$op: $(cat "$S/op_$op")" || echo "No operations." ;;
  "getprop ro.build.type") case "$TESTBUILD" in yes) echo userdebug ;; keys) echo user ;; *) echo user ;; esac ;;
  "df -k /data"*) echo "/dev/x 16777216 4194304 12582912 25% /data" ;;
esac
exit 0
'''
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

class Tv:
    def __init__(self, **settings):
        self.t = Path(tempfile.mkdtemp()); self.tree = self.t / "tree"; (self.tree / "optimize").mkdir(parents=True)
        shutil.copy(ROOT / "optimize" / "tweaks.sh", self.tree / "optimize")
        (self.t / "py").write_text(STUB); (self.t / "py").chmod(0o755)
        (self.t / "tv").mkdir()
        for k, v in settings.items():
            (self.t / "tv" / k).write_text(v + "\n")
    def run(self, mode, refuse="", unreadable="", failread="", emptyread="", testbuild="", sdk="34"):
        env = dict(os.environ, T=str(self.t), PY=str(self.t / "py"), REFUSE=refuse, UNREADABLE=unreadable, FAILREAD=failread, EMPTYREAD=emptyread, TESTBUILD=testbuild, SDK=sdk)
        r = subprocess.run(["bash", str(self.tree / "optimize" / "tweaks.sh"), mode], env=env, capture_output=True, text=True, cwd=self.tree)
        return r.returncode, r.stdout + r.stderr
    def get(self, k):
        f = self.t / "tv" / k
        return f.read_text().strip() if f.exists() else "null"
    def journal(self):
        f = self.tree / "optimize" / "run" / "tweaks-journal.txt"
        return f.read_text() if f.exists() else None
    def done(self):
        return (self.tree / "optimize" / "run" / "tweaks-journal.done").exists()
    def reboot(self):   # device_config does not survive a restart on some sets (seen on a TCL Android 14)
        (self.t / "tv" / "max_cached_processes").unlink(missing_ok=True)

# 1. nothing was set on the TV (freezer and process limit): restore deletes them, it does not write a default
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
rc, out = tv.run("apply")
check("apply: the original values are recorded first (an unset one as null)", rc == 0 and "cached_apps_freezer=null" in tv.journal()
      and "window_animation_scale=1.0" in tv.journal() and "max_cached_processes=null" in tv.journal(), f"{out} {tv.journal()}")
check("apply: the TV has the new values and nothing is flagged", tv.get("window_animation_scale") == "0.5" and tv.get("cached_apps_freezer") == "enabled"
      and "!!" not in out, out)
rc, out = tv.run("restore")
check("restore of an unset original: deleted, not written as device_default", rc == 0 and tv.get("cached_apps_freezer") == "null"
      and tv.get("max_cached_processes") == "null" and tv.get("window_animation_scale") == "1.0" and "!!" not in out, f"{out} {tv.get('cached_apps_freezer')}")
check("restore: the journal is used up (a later apply records afresh)", tv.journal() is None)

# 2. the TV had its own values: exactly those come back
tv = Tv(window_animation_scale="0.8", transition_animation_scale="0.8", animator_duration_scale="0", cached_apps_freezer="disabled", max_cached_processes="64")
tv.run("apply")
check("a limit the TV had is replaced by apply (Android's own) and recorded", tv.get("max_cached_processes") == "null" and "max_cached_processes=64" in tv.journal(), tv.journal())
tv.run("restore")
check("restore after apply: its own scales, freezer and limit, exactly",
      (tv.get("window_animation_scale"), tv.get("transition_animation_scale"), tv.get("animator_duration_scale"), tv.get("cached_apps_freezer"), tv.get("max_cached_processes"))
      == ("0.8", "0.8", "0", "disabled", "64"), str([tv.get(k) for k in ("window_animation_scale", "animator_duration_scale", "cached_apps_freezer", "max_cached_processes")]))

# 3. a second apply (run.sh again) keeps the first record
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
tv.run("apply"); first = tv.journal(); tv.run("apply")
check("a second apply keeps the first record (the TV is not 'as it came' any more)", tv.journal() == first and "window_animation_scale=1.0" in first, tv.journal())

# 4. a TV that refuses a value
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
rc, out = tv.run("apply", refuse="cached_apps_freezer")
check("apply: a value the TV does not take gets a !! line (and the setup goes on: exit 0)", rc == 0
      and "!! cached_apps_freezer: this TV did not take the value (asked for enabled, it says null)" in out and "!! window_animation_scale" not in out, out)
rc, out = tv.run("restore", refuse="window_animation_scale")
check("restore: a value the TV does not take is flagged and the journal is kept for another try", "!! window_animation_scale" in out and tv.journal() is not None, out)

# 5. after a restart of the TV
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0", max_cached_processes="64")
tv.run("apply"); tv.reboot()
rc, out = tv.run("restore")
check("restore after a restart (the device_config value is gone): the recorded value is put back", rc == 0 and tv.get("max_cached_processes") == "64" and "!!" not in out, f"{out} {tv.get('max_cached_processes')}")

# 6. no journal (a TV set up by an older version): Android's defaults, said
tv = Tv()
rc, out = tv.run("restore")
check("restore with no journal: Android's defaults, and it says so", rc == 0 and "No journal" in out and tv.get("window_animation_scale") == "1.0", out)

# 7. status: what it was and what it is
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
tv.run("apply"); rc, out = tv.run("status")
check("status: was and now, beside each other", "- tweak window_animation_scale: was 1.0, now 0.5" in out and "- tweak cached_apps_freezer: was null, now enabled" in out, out)

# 8. the Play Store's standby bucket and background appops: recorded, restored exactly, read back
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0", standby_bucket="30", op_RUN_IN_BACKGROUND="ignore")
rc, out = tv.run("apply")
check("apply: the Play Store's own bucket and appops are recorded (frequent, ignore, and none set -> default)",
      "standby_bucket=frequent" in tv.journal() and "appop_RUN_IN_BACKGROUND=ignore" in tv.journal() and "appop_RUN_ANY_IN_BACKGROUND=default" in tv.journal(), tv.journal())
check("apply: restricted and deny are in place and read back without a !!", tv.get("standby_bucket") == "45" and tv.get("op_RUN_IN_BACKGROUND") == "deny" and "!!" not in out, out)
rc, out = tv.run("restore")
check("restore: the bucket and the appops it had, not active/allow; an appop with no mode is back to default",
      tv.get("standby_bucket") == "30" and tv.get("op_RUN_IN_BACKGROUND") == "ignore" and tv.get("op_RUN_ANY_IN_BACKGROUND") == "null" and "!!" not in out,
      f"{out} {[tv.get(k) for k in ('standby_bucket', 'op_RUN_IN_BACKGROUND', 'op_RUN_ANY_IN_BACKGROUND')]}")
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
rc, out = tv.run("apply", refuse="standby_bucket RUN_IN_BACKGROUND")
check("apply: a TV that does not take the bucket or the appop gets a !! line each",
      "!! standby_bucket: this TV did not take the value (asked for restricted, it says active)" in out and "!! appop_RUN_IN_BACKGROUND: this TV did not take" in out, out)
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0", standby_bucket="30")
tv.run("apply")
rc, out = tv.run("restore", refuse="standby_bucket")
check("restore: a bucket the TV refuses to put back is flagged and the journal is kept for another try",
      "!! standby_bucket: this TV did not take the value (asked for frequent, it says restricted)" in out and tv.journal() is not None, out)
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
rc, out = tv.run("apply", unreadable="yes")
check("a firmware that does not say what the Play Store had: recorded as unknown, NOT changed (a !! line each), so there is nothing to put back",
      "standby_bucket=unknown" in tv.journal() and "appop_RUN_IN_BACKGROUND=unknown" in tv.journal()
      and "!! standby_bucket: could not read what this TV has for the Play Store" in out and "!! appop_RUN_IN_BACKGROUND: could not read" in out
      and tv.get("standby_bucket") == "null" and tv.get("op_RUN_IN_BACKGROUND") == "null", f"{out} {tv.journal()}")
rc, out = tv.run("restore")
check("restore leaves them alone and says so with a !! line (it never forces active/allow onto a value it did not change)",
      "!! standby_bucket: the TV's own value was not recorded" in out and "left as it is" in out
      and tv.get("standby_bucket") == "null" and tv.get("op_RUN_IN_BACKGROUND") == "null", out)
tv.run("apply"); rc, out = tv.run("status")
check("status lists the Play Store's values too", "- tweak standby_bucket: was" in out and "now restricted" in out, out)
# 9. a read that failed or came back empty is "unknown", never "null": restore must not delete the TV's real value
for how in ("failread", "emptyread"):
    tv = Tv(window_animation_scale="0.75", transition_animation_scale="1.0", animator_duration_scale="1.0")
    rc, out = tv.run("apply", **{how: "window_animation_scale"})
    check(f"{how}: recorded as unknown (not null), said with a !! line, and the setting is not changed",
          "window_animation_scale=unknown" in tv.journal() and "window_animation_scale=null" not in tv.journal()
          and "!! window_animation_scale: could not read" in out and tv.get("window_animation_scale") == "0.75" and tv.get("transition_animation_scale") == "0.5", f"{out} {tv.journal()}")
    rc, out = tv.run("restore")
    check(f"{how}: restore leaves that value alone (0.75 stays, nothing deleted) and says so", tv.get("window_animation_scale") == "0.75"
          and "!! window_animation_scale: what the TV had was not recorded" in out and tv.get("transition_animation_scale") == "1.0", out)
tv = Tv(window_animation_scale="0.75", transition_animation_scale="1.0", animator_duration_scale="1.0")
tv.run("apply", failread="window_animation_scale")
rc, out = tv.run("apply")   # the TV answers now: it was never touched, so 0.75 is its original
check("a later apply reads an unknown one again, records it, and then changes it", "window_animation_scale=0.75" in tv.journal() and tv.get("window_animation_scale") == "0.5", tv.journal())
tv.run("restore")
check("and restore then puts 0.75 back", tv.get("window_animation_scale") == "0.75")

# 10. a second restore does nothing (the first one's journal is kept as .done)
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0", standby_bucket="30")
tv.run("apply"); tv.run("restore")
(tv.t / "tv" / "standby_bucket").write_text("40\n")   # the person (or Android) moved it afterwards
rc, out = tv.run("restore")
check("a second restore leaves the restored values alone", tv.done() and tv.journal() is None and tv.get("standby_bucket") == "40" and "nothing to do" in out, out)
rc, out = tv.run("status")
check("status says they were already put back", "already put back" in out, out)

# 11. a Play Store exempted from standby (bucket 5) is never changed and never forced
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0", standby_bucket="5")
rc, out = tv.run("apply")
check("an exempted bucket is recorded as exempted and left as it is", "standby_bucket=exempted" in tv.journal() and tv.get("standby_bucket") == "5" and "exempted from standby" in out, out)
rc, out = tv.run("restore")
check("and restore does not force it to active", tv.get("standby_bucket") == "5" and "!!" not in out, out)
# 12. a test build (userdebug, test-keys) keeps its own standby bucket: an info line, not a !!
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
rc, out = tv.run("apply", refuse="standby_bucket", testbuild="yes")
check("a test build that keeps its standby bucket: an info line, no !!, and the setup is not marked as a problem", "info: standby_bucket stays" in out and "!! standby_bucket" not in out, out)
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
rc, out = tv.run("apply", refuse="standby_bucket")
tv = Tv(window_animation_scale="1.0", transition_animation_scale="1.0", animator_duration_scale="1.0")
rc, out = tv.run("apply", refuse="standby_bucket", testbuild="keys")
check("test-keys alone (shipped TVs often have it) is not a test build: the !! line stays", "!! standby_bucket: this TV did not take the value" in out, out)
check("a normal build still gets the !! line", "!! standby_bucket: this TV did not take the value" in out, out)

# ---- the Android a TV runs: device_config is Android 10 (SDK 29) and newer; standby buckets Android 9 (28) and newer, «restricted» Android 11 (30) and newer
def batch_of(tv): return (tv.t / "batch").read_text() if (tv.t / "batch").exists() else ""
for sdk, want_dc, want_bucket in (("28", False, "40"), ("29", True, "40"), ("30", True, "45"), ("34", True, "45")):
    tv = Tv(); rc, out = tv.run("apply", sdk=sdk); b = batch_of(tv); j = tv.journal() or ""
    check(f"SDK {sdk}: device_config {'is used' if want_dc else 'is never called'}; the standby bucket is {'restricted' if want_bucket == '45' else 'rare'}; no !! line, no refused bucket",
          rc == 0 and ("device_config" in b) == want_dc and tv.get("standby_bucket") == want_bucket and "!!" not in out and not (tv.t / "errors").exists(), f"{rc} {out} {b}")
    if not want_dc:
        check(f"SDK {sdk}: the journal says what was skipped and why", "skipped_max_cached_processes=" in j and "max_cached_processes=" not in j.replace("skipped_max_cached_processes=", ""), j)
    rc, out = tv.run("restore", sdk=sdk); b2 = batch_of(tv)
    check(f"SDK {sdk}: restore puts back the bucket and the others and never calls device_config below SDK 29", rc == 0 and "!!" not in out and (want_dc or "device_config" not in b2), f"{rc} {out}")
tv = Tv(); rc, out = tv.run("apply", sdk="27"); b = batch_of(tv)
check("SDK 27: no standby bucket at all (Android 8 has none), no device_config", rc == 0 and "set-standby-bucket" not in b and "device_config" not in b and "!!" not in out and "skipped_standby_bucket=" in (tv.journal() or ""), f"{rc} {out} {b}")
tv = Tv(); rc, out = tv.run("apply", sdk=""); b = batch_of(tv)
check("an SDK the TV did not say: device_config and the standby bucket are left alone (never «restricted»), the rest goes on", rc == 0 and "device_config" not in b and "set-standby-bucket" not in b and "restricted" not in b and "window_animation_scale" in b, f"{rc} {out} {b}")
tv = Tv(); rc, out = tv.run("apply", sdk="28"); rc, out = tv.run("status", sdk="28")
check("status on an old Android lists the keys it handles without a device_config question", rc == 0 and "max_cached_processes" not in out and "!!" not in out, out)
sys.exit(1 if failed else 0)
