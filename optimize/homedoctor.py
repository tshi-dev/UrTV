#!/usr/bin/env python3
"""The Home doctor (4.4): why does UrTV not stay the TV's main screen? It records the TV's state, tries one angle at a time, tests each the
person's way (Home from YouTube), puts the TV back between angles, and stops at the first angle that passes. docs/HOMEDOCTOR.md.

    optimize/venv/bin/python -I optimize/homedoctor.py               every angle in order, until one passes
    optimize/venv/bin/python -I optimize/homedoctor.py --dry-run     the plan only (nothing sent)
    optimize/venv/bin/python -I optimize/homedoctor.py --angle C     one angle
    ... --reboot   the angle that passes is also checked after a restart (tv.py --reboot)
    ... --keep     an angle that passed stays (else the TV is put back as it was before the run)

Angles (each safe; NEVER a disable of launcherx or setupwraith, never a factory reset, never the family PIN):
  A  the HOME role to UrTV (cmd role add-role-holder, with flags)
  B  the home activity to UrTV (cmd package set-home-activity), no role change
  C  launcherx's claim cleared (its role and preferred activities), then A
  D  A + «إضافات تلفزيونك»'s Home watch (on and bound, «wanted» set), its UrTVWatch lines read
  E  the TV's own default-home screen, opened for the tester (manual: press Enter when done)
  F  A + «wanted» set, then a restart: the home re-applies the role at boot (4.4)

Everything is saved under optimize/run/homedoctor/<time>/ (local, git-ignored): snapshots before and after each angle, a screenshot,
report.md and report.json. No address, serial or name of the TV is written in them.
"""
import argparse
import json
import os
import re
import subprocess
import sys
import time

SELF = "sa.urtv.home"
HOME_ACTIVITY = SELF + "/.MainActivity"
LAUNCHERX = "com.google.android.apps.tv.launcherx"
YOUTUBE = "com.google.android.youtube.tv"
EXTRAS_SERVICE = "sa.urtv.extras/sa.urtv.home.ExtrasA11yService"
ROLE = "android.app.role.HOME"
ANGLES = "ABCDEF"
ABOUT = {
    "A": "the HOME role to UrTV (cmd role add-role-holder --flags 1)",
    "B": "the home activity to UrTV (cmd package set-home-activity), the role untouched",
    "C": "launcherx's claim cleared (remove-role-holder, its preferred activities), then A",
    "D": "A + «إضافات تلفزيونك»'s Home watch (on, bound, wanted)",
    "E": "the TV's own default-home screen, for the tester (manual)",
    "F": "A + wanted, then a restart: the home re-applies the role at boot",
}
# the snapshot: name -> command (read only)
SNAPSHOT = [
    ("role", f"cmd role get-role-holders --user 0 {ROLE}"),
    ("resolve", "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"),
    ("dumpsys-role", "dumpsys role"),
    ("preferred", "dumpsys package preferred-activities"),
    ("accessibility", "dumpsys accessibility"),
    ("alarm", "dumpsys alarm | grep -i -A2 SetupRevert"),
    ("activities", "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity|mFocusedApp'"),
    ("disabled", "pm list packages -d"),
    ("a11y-list", "settings get secure enabled_accessibility_services"),
    ("a11y-switch", "settings get secure accessibility_enabled"),
    ("wanted", f"am broadcast -n {SELF}/.DockReceiver -a {SELF}.HOME_WANTED --es mode status"),
]
LOG_KEEP = re.compile(r"RoleService|RoleController|RoleManager|ActivityTaskManager.*(HOME|android\.intent\.category\.HOME)|UrTVSetup|UrTVWatch|launcherx", re.I)
FORBIDDEN = re.compile(r"\bpm (disable|disable-user|uninstall|clear)\b|factory|master_clear|wipe|recovery|\bPIN_|\bpin (set|reset|off)\b", re.I)


# ---- parsing (pure; tests/homedoctor_test.py)

def role_holders(text):
    """`cmd role get-role-holders`: one package per line"""
    return [l.strip() for l in (text or "").splitlines() if re.fullmatch(r"[A-Za-z][\w.]*", l.strip() or "-")]


def resolved(text):
    """`resolve-activity --brief`: the last line, a component (package/class) or ''"""
    lines = [l.strip() for l in (text or "").splitlines() if "/" in l and " " not in l.strip()]
    return lines[-1] if lines else ""


def top_package(text):
    """`dumpsys activity activities` lines: the package of the resumed (or top resumed) activity, '' when none"""
    for key in ("topResumedActivity", "mResumedActivity", "mFocusedApp"):
        m = re.search(key + r"[=:].*?\s([A-Za-z][\w.]*)/", text or "")
        if m:
            return m.group(1)
    return ""


def bound(text, service=EXTRAS_SERVICE):
    """`dumpsys accessibility`: the add-on is among the bound services (by its component, or the label Android shows)"""
    line = next((l for l in (text or "").splitlines() if "Bound services:" in l), "")
    return service in line or "sa.urtv.extras" in line or "إضافات تلفزيونك" in line


def revert_armed(text):
    """`dumpsys alarm | grep SetupRevert`: the 4.4 safety switch is still armed"""
    return "SetupRevertReceiver" in (text or "")


def explain(log_text, limit=25):
    """the log lines that say who changed Home (the role service, the activity manager's HOME starts, our own tags, launcherx)"""
    keep = [l.strip() for l in (log_text or "").splitlines() if LOG_KEEP.search(l)]
    return keep[-limit:]


def verdict(presses, late_press, role_after, angle):
    """An angle passes when Home lands on UrTV at 6 s on every press, and a press two minutes later still does. B and D may pass with the role
    elsewhere (B sets only the home activity, D's watch brings UrTV back); the others need the role to be ours after two minutes too."""
    ok = bool(presses) and all(p.get("6s") == SELF for p in presses) and late_press == SELF
    if angle not in ("B", "D", "E"):
        ok = ok and SELF in role_after
    why = []
    if not presses or not all(p.get("6s") == SELF for p in presses):
        why.append("Home opened " + ", ".join(sorted({p.get("6s") or "nothing" for p in presses if p.get("6s") != SELF})) + " at 6 s")
    if late_press != SELF:
        why.append(f"two minutes later Home opened {late_press or 'nothing'}")
    if angle not in ("B", "D", "E") and SELF not in role_after:
        why.append("the role went back to " + (", ".join(role_after) or "nobody") + " within two minutes")
    return ok, "; ".join(why) or "Home stayed on UrTV"


def plan(only=None, reboot=False):
    angles = [only] if only else list(ANGLES)
    out = ["The Home doctor, the plan (nothing is sent with --dry-run):", "1. snapshot of the TV before (" + ", ".join(n for n, _ in SNAPSHOT) + ", the log, a screenshot)"]
    for a in angles:
        out.append(f"2. angle {a}: {ABOUT[a]}; then the test (YouTube, Home ×3 at 2 s and 6 s, two minutes, Home again); the snapshot again; the TV put back")
    out.append("3. stop at the first angle that passes" + (" and check it after a restart" if reboot else "") + "; report.md and report.json")
    out.append("never: a disable of launcherx or setupwraith, a factory reset, the family PIN")
    return out


# ---- the TV

class Tv:
    """`optimize/tv.py` for each command (its checks and retries); fake in the tests."""

    def __init__(self, py=None):
        here = os.path.dirname(os.path.abspath(__file__))
        self.tvpy = os.path.join(here, "tv.py")
        self.py = py or os.environ.get("PY") or (os.path.join(here, "venv", "bin", "python") if os.path.exists(os.path.join(here, "venv", "bin", "python")) else sys.executable)

    def _run(self, *args, timeout=180):
        r = subprocess.run([self.py, "-I", self.tvpy, *args], capture_output=True, text=True, timeout=timeout)
        return (r.stdout or "") + (r.stderr or "")

    def sh(self, cmd):
        if FORBIDDEN.search(cmd):
            raise SystemExit(f"!! homedoctor refused a command it must never send: {cmd}")
        return self._run(cmd)

    def shot(self, path):
        self._run("--shot", path, timeout=120)

    def reboot(self):
        return self._run("--reboot", timeout=900)

    def wait(self, s):
        time.sleep(s)

    def ask(self, prompt):
        return input(prompt)


class Doctor:
    def __init__(self, tv, out_dir, log=print):
        self.tv, self.dir, self.log = tv, out_dir, log
        self.before = {}
        self.since = ""

    def save(self, name, text):
        with open(os.path.join(self.dir, name), "w", encoding="utf-8") as f:
            f.write(text or "")

    def snapshot(self, tag):
        snap = {}
        for name, cmd in SNAPSHOT:
            snap[name] = self.tv.sh(cmd)
            self.save(f"{tag}-{name}.txt", snap[name])
        log = self.tv.sh("logcat -d -b all -v time" + (f" -T '{self.since}'" if self.since else " -t 4000"))
        snap["log"] = "\n".join(explain(log, limit=400))
        self.save(f"{tag}-log.txt", snap["log"])
        try:
            self.tv.shot(os.path.join(self.dir, f"{tag}.png"))
        except Exception:
            pass
        return snap

    def mark(self):
        """the log from now on: the TV's own clock (logcat -T takes 'MM-DD hh:mm:ss.mmm')"""
        self.since = (self.tv.sh("date '+%m-%d %H:%M:%S.000'").strip().splitlines() or [""])[-1]

    def top(self):
        return top_package(self.tv.sh("dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity|mFocusedApp'"))

    def test(self, angle):
        presses = []
        for i in range(3):
            self.tv.sh(f"monkey -p {YOUTUBE} -c android.intent.category.LEANBACK_LAUNCHER 1")
            self.tv.wait(5)
            self.tv.sh("input keyevent KEYCODE_HOME")
            self.tv.wait(2)
            at2 = self.top()
            self.tv.wait(4)
            presses.append({"2s": at2, "6s": self.top()})
            self.log(f"   Home {i + 1} from YouTube: {presses[-1]['2s'] or '?'} at 2 s, {presses[-1]['6s'] or '?'} at 6 s")
        self.log("   waiting two minutes (does the TV take Home back?)")
        self.tv.wait(120)
        role_after = role_holders(self.tv.sh(f"cmd role get-role-holders --user 0 {ROLE}"))
        self.tv.sh(f"monkey -p {YOUTUBE} -c android.intent.category.LEANBACK_LAUNCHER 1")
        self.tv.wait(5)
        self.tv.sh("input keyevent KEYCODE_HOME")
        self.tv.wait(6)
        late = self.top()
        ok, why = verdict(presses, late, role_after, angle)
        return {"presses": presses, "role_after_2min": role_after, "home_after_2min": late, "passed": ok, "why": why}

    def wanted(self, on):
        return self.tv.sh(f"am broadcast -n {SELF}/.DockReceiver -a {SELF}.HOME_WANTED --es mode {'on' if on else 'off'}")

    def role_to_us(self):
        out = self.tv.sh(f"cmd role add-role-holder --user 0 --flags 1 {ROLE} {SELF}")
        if "rror" in out or "nknown" in out:
            out += self.tv.sh(f"cmd role add-role-holder --user 0 {ROLE} {SELF}")
        return out

    def apply(self, a):
        """returns (applied, note)"""
        if a == "A":
            return True, self.role_to_us()
        if a == "B":
            return True, self.tv.sh(f"cmd package set-home-activity --user 0 {HOME_ACTIVITY}")
        if a == "C":
            out = ""
            for h in role_holders(self.tv.sh(f"cmd role get-role-holders --user 0 {ROLE}")):
                if h != SELF:
                    out += self.tv.sh(f"cmd role remove-role-holder --user 0 {ROLE} {h}")
            out += self.tv.sh(f"cmd package clear-package-preferred-activities {LAUNCHERX}")   # an unknown command on some TVs: said, nothing else
            return True, out + self.role_to_us()
        if a == "D":
            if "package:" not in self.tv.sh("pm path sa.urtv.extras"):
                return False, "«إضافات تلفزيونك» is not installed (optimize/install.sh addon)"
            out = self.role_to_us()
            if not bound(self.tv.sh("dumpsys accessibility")):
                lst = self.before.get("a11y-list", "").strip().splitlines()
                cur = lst[-1].strip() if lst else ""
                new = EXTRAS_SERVICE if cur in ("", "null") else (cur if EXTRAS_SERVICE in cur else cur + ":" + EXTRAS_SERVICE)
                out += self.tv.sh(f"settings put secure enabled_accessibility_services '{new}'") + self.tv.sh("settings put secure accessibility_enabled 1")
                self.tv.wait(4)
            out += self.wanted(True)
            if not bound(self.tv.sh("dumpsys accessibility")):
                return False, out + "\nthe add-on's service is not bound (Bound services)"
            return True, out
        if a == "E":
            out = self.tv.sh("am start -a android.settings.HOME_SETTINGS")
            if "Error" in out:
                out += self.tv.sh("am start -a android.settings.MANAGE_DEFAULT_APPS_SETTINGS")
            self.tv.shot(os.path.join(self.dir, "E-settings.png"))
            exists = "Error" not in out.splitlines()[-1] if out.strip() else False
            answer = self.tv.ask("   E: the TV shows its default-home screen if it has one. Choose UrTV there if it is offered, then press Enter here"
                                 " (type what it offered, e.g. 'Google TV, UrTV'): ")
            return True, out + f"\nscreen opened: {'yes' if exists else 'no'}; the tester saw: {answer.strip() or '(nothing typed)'}"
        if a == "F":
            out = self.role_to_us() + self.wanted(True)
            out += self.tv.reboot()
            return True, out
        raise ValueError(a)

    def restore(self):
        """the TV as before the run: the role holder, the home activity, the accessibility list and switch, «wanted» as it was (read before the run). Nothing is turned off."""
        holders = role_holders(self.before.get("role", ""))
        now = role_holders(self.tv.sh(f"cmd role get-role-holders --user 0 {ROLE}"))
        if SELF in now and SELF not in holders:
            self.tv.sh(f"cmd role remove-role-holder --user 0 {ROLE} {SELF}")
        for h in holders:
            if h not in now:
                self.tv.sh(f"cmd role add-role-holder --user 0 {ROLE} {h}")
        comp = resolved(self.before.get("resolve", ""))
        if comp:
            self.tv.sh(f"cmd package set-home-activity --user 0 {comp}")
        for key, setting in (("a11y-list", "enabled_accessibility_services"), ("a11y-switch", "accessibility_enabled")):
            lines = self.before.get(key, "").strip().splitlines()
            v = lines[-1].strip() if lines else ""
            if v and v != "null" and re.fullmatch(r"[\w./:$]+", v):
                self.tv.sh(f"settings put secure {setting} '{v}'")
            elif v == "null":
                self.tv.sh(f"settings delete secure {setting}")
        self.wanted("home-wanted: yes" in self.before.get("wanted", ""))
        self.tv.sh("input keyevent KEYCODE_HOME")

    def run(self, angles, reboot=False, keep=False):
        self.before = self.snapshot("before")
        results, winner, done = [], None, False
        try:
            for a in angles:
                self.log(f"angle {a}: {ABOUT[a]}")
                self.mark()
                try:
                    applied, note = self.apply(a)
                except SystemExit:
                    raise
                except Exception as e:   # the TV dropped: said, put back, the next angle
                    applied, note = False, f"failed: {e}"
                r = {"angle": a, "about": ABOUT[a], "applied": applied, "note": note.strip()[-800:]}
                if applied:
                    self.tv.wait(3)
                    r.update(self.test(a))
                else:
                    r.update({"passed": False, "why": "not applied: " + note.strip().splitlines()[-1] if note.strip() else "not applied"})
                after = self.snapshot(f"after-{a}")
                r["log"] = explain(after["log"])
                r["revert_alarm"] = revert_armed(after["alarm"])
                r["watch_bound"] = bound(after["accessibility"])
                self.log(f"   {'PASSED' if r['passed'] else 'failed'}: {r['why']}")
                if r["passed"] and reboot and a != "F":
                    self.tv.reboot()
                    r["after_reboot"] = self.test(a)
                    self.log(f"   after a restart: {'passed' if r['after_reboot']['passed'] else 'failed'}: {r['after_reboot']['why']}")
                results.append(r)
                if r["passed"]:
                    winner = a
                    break
                self.restore()
            done = True
            return results, winner
        finally:   # an error or Ctrl-C mid-run puts the TV back too; --keep keeps only a finished, passed run
            if not (done and winner is not None and keep):
                self.restore()


def report(results, winner, props, out_dir):
    data = {"tv": props, "winner": winner, "angles": results}
    with open(os.path.join(out_dir, "report.json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    md = ["# Home doctor", "", f"TV: {props.get('model', '?')} ({props.get('brand', '?')}), Android {props.get('release', '?')} (SDK {props.get('sdk', '?')})",
          "", f"**Winner: {('angle ' + winner + ': ' + ABOUT[winner]) if winner else 'none: no angle kept Home on UrTV'}**", ""]
    for r in results:
        md += [f"## Angle {r['angle']}: {r['about']}", f"- applied: {'yes' if r['applied'] else 'no'}",
               f"- result: {'PASSED' if r['passed'] else 'failed'}: {r['why']}"]
        if r.get("presses"):
            md.append("- Home from YouTube: " + "; ".join(f"{p['2s'] or '?'} at 2 s, {p['6s'] or '?'} at 6 s" for p in r["presses"]))
            md.append(f"- after two minutes: role {', '.join(r['role_after_2min']) or 'nobody'}; Home opened {r['home_after_2min'] or '?'}")
        md.append(f"- safety switch armed after: {'yes' if r.get('revert_alarm') else 'no'}; add-on bound: {'yes' if r.get('watch_bound') else 'no'}")
        if r.get("after_reboot"):
            md.append(f"- after a restart: {'passed' if r['after_reboot']['passed'] else 'failed'}: {r['after_reboot']['why']}")
        if r.get("log"):
            md += ["- why (the log):", "```"] + r["log"] + ["```"]
        md.append("")
    with open(os.path.join(out_dir, "report.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(md) + "\n")


def props_of(tv):
    keys = {"model": "ro.product.model", "brand": "ro.product.brand", "release": "ro.build.version.release", "sdk": "ro.build.version.sdk"}
    return {k: (tv.sh(f"getprop {p}").strip().splitlines() or [""])[-1] for k, p in keys.items()}


def main(argv=None):
    ap = argparse.ArgumentParser(description="The Home doctor: why UrTV does not stay the TV's main screen (docs/HOMEDOCTOR.md)")
    ap.add_argument("--dry-run", action="store_true", help="print the plan, send nothing")
    ap.add_argument("--angle", choices=list(ANGLES), help="one angle only")
    ap.add_argument("--reboot", action="store_true", help="check the angle that passes after a restart too")
    ap.add_argument("--keep", action="store_true", help="keep an angle that passed (else the TV is put back as it was)")
    a = ap.parse_args(argv)
    if a.dry_run:
        print("\n".join(plan(a.angle, a.reboot)))
        return 0
    here = os.path.dirname(os.path.abspath(__file__))
    out_dir = os.path.join(os.environ.get("URTV_RUN_DIR") or os.path.join(here, "run"), "homedoctor", time.strftime("%Y%m%d-%H%M%S"))
    os.makedirs(out_dir, exist_ok=True)
    tv = Tv()
    doc = Doctor(tv, out_dir)
    results, winner = doc.run([a.angle] if a.angle else list(ANGLES), reboot=a.reboot, keep=a.keep)
    report(results, winner, props_of(tv), out_dir)
    print(f"✓ report: {os.path.relpath(os.path.join(out_dir, 'report.md'))}" + (f" (winner: angle {winner})" if winner else " (no angle passed)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
