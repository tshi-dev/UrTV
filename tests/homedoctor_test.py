#!/usr/bin/env python3
"""optimize/homedoctor.py (4.4, the Home doctor): its parsing of the TV's answers (sample dumpsys text), its verdict per angle, the plan, the
commands it must never send, and a whole run on a fake Google TV that takes the role back (angles A to C fail, D passes; everything put back).
    python3 tests/homedoctor_test.py"""
import importlib.util, json, os, re, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("homedoctor", ROOT / "optimize/homedoctor.py")
hd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hd)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


LX = "com.google.android.apps.tv.launcherx"
# ---- parsing
check("role holders: one package per line, nothing else", hd.role_holders(f"{LX}\n\n") == [LX] and hd.role_holders("") == [] and hd.role_holders("Error: unknown") == [])
check("resolve-activity --brief: the component on the last line",
      hd.resolved(f"priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\n{LX}/.home.HomeActivity\n") == f"{LX}/.home.HomeActivity" and hd.resolved("No activity found") == "")
acts = f"  topResumedActivity=ActivityRecord{{1a2b u0 {LX}/.home.HomeActivity t12}}\n    mResumedActivity: ActivityRecord{{1a2b u0 {LX}/.home.HomeActivity t12}}\n"
check("the app in front: topResumedActivity, else mResumedActivity", hd.top_package(acts) == LX
      and hd.top_package("    mResumedActivity: ActivityRecord{9f u0 sa.urtv.home/.MainActivity t3}") == "sa.urtv.home" and hd.top_package("") == "")
check("the add-on bound: by its component or the label Android shows; another service's line is not it",
      hd.bound("Bound services:{Service[label=إضافات تلفزيونك, feedbackType...]}") and hd.bound(f"  Bound services:{{{hd.EXTRAS_SERVICE}}}")
      and not hd.bound("  Bound services:{Service[label=Key Mapper]}") and not hd.bound("Enabled services:{sa.urtv.extras/x}"))
check("the safety switch still armed (dumpsys alarm)", hd.revert_armed("  RTC_WAKEUP #0: Alarm{... sa.urtv.home/.setup.SetupRevertReceiver}") and not hd.revert_armed("nothing"))
log = "\n".join([
    "11-16 19:55:12.001 I/RoleService( 912): Granted role android.app.role.HOME to sa.urtv.home",
    "11-16 19:56:58.420 I/RoleControllerServiceImpl( 2210): Removed role holder sa.urtv.home, added com.google.android.apps.tv.launcherx",
    "11-16 19:57:00.000 I/ActivityTaskManager( 912): START u0 {act=android.intent.action.MAIN cat=[android.intent.category.HOME] cmp=x}",
    "11-16 19:57:01.000 I/WifiService( 900): something else",
    "11-16 19:57:02.000 I/UrTVWatch( 3100): decision bring ours",
])
ex = hd.explain(log)
check("the log lines that say who changed Home are kept (the role service, HOME starts, our tags), the rest dropped", len(ex) == 4 and not any("WifiService" in l for l in ex), ex)
# ---- the verdict
ours, them = {"2s": "sa.urtv.home", "6s": "sa.urtv.home"}, {"2s": LX, "6s": LX}
check("passes: Home on UrTV at 6 s every time, still two minutes later, the role ours", hd.verdict([ours] * 3, "sa.urtv.home", ["sa.urtv.home"], "A") == (True, "Home stayed on UrTV"))
ok, why = hd.verdict([ours] * 3, "sa.urtv.home", [LX], "A")
check("fails: the role went back within two minutes (A, C, F need the role)", not ok and "the role went back to " + LX in why, why)
check("D passes with the role elsewhere (the watch brings UrTV back); B and E too", hd.verdict([ours] * 3, "sa.urtv.home", [LX], "D")[0] and hd.verdict([ours] * 3, "sa.urtv.home", [LX], "B")[0])
ok, why = hd.verdict([ours, them, ours], LX, ["sa.urtv.home"], "A")
check("fails: a press landed on launcherx, and two minutes later too; the why names it", not ok and LX + " at 6 s" in why and "two minutes later Home opened " + LX in why, why)
check("no presses (the TV did not answer): fails", not hd.verdict([], "", [], "A")[0])
# ---- the plan and the guard
p = "\n".join(hd.plan())
check("--dry-run: the plan names the six angles in order, the test, the stop and what is never done", all(f"angle {a}:" in p for a in "ABCDEF") and p.index("angle A") < p.index("angle F")
      and "never: a disable of launcherx or setupwraith" in p and "\n".join(hd.plan("C", True)).count("2. angle ") == 1)
for bad in ("pm disable-user --user 0 " + LX, "pm disable com.google.android.tungsten.setupwraith", "pm uninstall sa.urtv.home", "am broadcast -a android.intent.action.FACTORY_RESET",
            "pm clear " + LX, "am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.PIN_RESET"):
    try:
        hd.Tv(py=sys.executable).sh(bad)
        check(f"refuses: {bad}", False)
    except SystemExit as e:
        check(f"refuses: {bad}", "never send" in str(e))
src = (ROOT / "optimize/homedoctor.py").read_text(encoding="utf-8")
sent = re.findall(r'sh\(f?"([^"]+)"', src)
check("nothing it sends is forbidden (disable, uninstall, factory, PIN)", sent and not [c for c in sent if hd.FORBIDDEN.search(c)], [c for c in sent if hd.FORBIDDEN.search(c)])
check("it restarts the TV only through tv.py --reboot (never a plain reboot)", '"--reboot"' in src and not re.search(r'sh\(f?"reboot', src))


# ---- a whole run on a fake Google TV that takes the role back after a while; the watch (D) brings UrTV
class FakeTv:
    def __init__(self, extras=True):
        self.role, self.home_act, self.front, self.clock = LX, f"{LX}/.home.HomeActivity", "", 0
        self.a11y, self.switch, self.wanted, self.extras, self.sent, self.reboots, self.asked = "null", "0", False, extras, [], 0, 0
        self.granted_at = None

    def _settle(self):
        if self.role == "sa.urtv.home" and self.granted_at is not None and self.clock - self.granted_at > 60:
            self.role, self.granted_at = LX, None   # Google TV takes it back after a minute

    def sh(self, cmd):
        assert not hd.FORBIDDEN.search(cmd), cmd
        self.sent.append(cmd)
        self._settle()
        if cmd.startswith("cmd role get-role-holders"):
            return self.role + "\n"
        if cmd.startswith("cmd role add-role-holder"):
            pkg = cmd.split()[-1]
            self.role = pkg
            self.granted_at = self.clock if pkg == "sa.urtv.home" else None
            return ""
        if cmd.startswith("cmd role remove-role-holder"):
            if cmd.split()[-1] == self.role:
                self.role = ""
            return ""
        if cmd.startswith("cmd package resolve-activity"):
            return "priority=0\n" + (hd.HOME_ACTIVITY if self.role == "sa.urtv.home" else self.home_act) + "\n"
        if cmd.startswith("dumpsys accessibility"):
            return "Bound services:{" + (hd.EXTRAS_SERVICE if self.extras and hd.EXTRAS_SERVICE in self.a11y else "") + "}"
        if cmd.startswith("settings get secure enabled_accessibility_services"):
            return self.a11y + "\n"
        if cmd.startswith("settings get secure accessibility_enabled"):
            return self.switch + "\n"
        if cmd.startswith("settings put secure enabled_accessibility_services"):
            self.a11y = cmd.split("'")[1]
            return ""
        if cmd.startswith("settings delete secure enabled_accessibility_services"):
            self.a11y = "null"
            return ""
        if cmd.startswith("settings put secure accessibility_enabled"):
            self.switch = cmd.split()[-1].strip("'")   # the shell takes the quotes off
            return ""
        if cmd.startswith("settings delete secure accessibility_enabled"):
            self.switch = "null"
            return ""
        if "HOME_WANTED" in cmd:
            self.wanted = cmd.endswith("on")
            return "home-wanted: " + ("yes" if self.wanted else "no")
        if cmd.startswith("pm path sa.urtv.extras"):
            return "package:/data/app/x/base.apk" if self.extras else ""
        if cmd.startswith("monkey"):
            self.front = "com.google.android.youtube.tv"
            return ""
        if cmd == "input keyevent KEYCODE_HOME":
            watch = self.extras and hd.EXTRAS_SERVICE in self.a11y and (self.wanted or self.role == "sa.urtv.home")
            self.front = "sa.urtv.home" if self.role == "sa.urtv.home" or watch else LX
            return ""
        if cmd.startswith("dumpsys activity activities"):
            return f"  topResumedActivity=ActivityRecord{{1 u0 {self.front}/.X t1}}" if self.front else ""
        if cmd.startswith("date"):
            return "11-16 20:00:00.000\n"
        if cmd.startswith("getprop"):
            return {"ro.product.model": "Smart TV", "ro.product.brand": "TCL", "ro.build.version.release": "12", "ro.build.version.sdk": "31"}.get(cmd.split()[-1], "") + "\n"
        return ""

    def shot(self, path):
        Path(path).write_bytes(b"png")

    def reboot(self):
        self.reboots += 1
        return ""

    def wait(self, s):
        self.clock += s

    def ask(self, prompt):
        self.asked += 1
        return "Google TV"


with tempfile.TemporaryDirectory() as tmp:
    tv = FakeTv()
    doc = hd.Doctor(tv, tmp, log=lambda s: None)
    results, winner = doc.run(list("ABCDEF"))
    got = [(r["angle"], r["passed"]) for r in results]
    check("the run: A, B and C fail on this TV (it takes the role back in a minute), D (the watch) passes, and it stops there (E and F not tried)",
          got == [("A", False), ("B", False), ("C", False), ("D", True)] and winner == "D", got)
    a = results[0]
    check("angle A's why: the role went back to launcherx within two minutes", "the role went back to " + LX in a["why"], a["why"])
    check("between the angles and at the end the TV is put back: the role launcherx's, the home activity, the accessibility list as before, «wanted» off",
          tv.role == LX and tv.a11y == "null" and tv.switch == "0" and not tv.wanted and any(c.startswith(f"cmd package set-home-activity --user 0 {LX}/.home.HomeActivity") for c in tv.sent))
    check("nothing disabled, nothing asked of the tester (E not reached), no restart", not any("disable" in c for c in tv.sent) and tv.asked == 0 and tv.reboots == 0)
    check("snapshots before and after each angle, with their screenshots", all((Path(tmp) / f).exists() for f in ("before-role.txt", "before-alarm.txt", "before.png", "after-A-role.txt", "after-D-accessibility.txt", "after-D.png")))
    hd.report(results, winner, hd.props_of(tv), tmp)
    data = json.loads((Path(tmp) / "report.json").read_text(encoding="utf-8"))
    md = (Path(tmp) / "report.md").read_text(encoding="utf-8")
    check("report.json and report.md: the winner, per angle applied, result and why, the TV's model and Android (no address or serial)",
          data["winner"] == "D" and [r["angle"] for r in data["angles"]] == list("ABCD") and "**Winner: angle D" in md and "## Angle A" in md and "applied: yes" in md
          and "Android 12 (SDK 31)" in md and not re.search(r"\b\d{1,3}(\.\d{1,3}){3}\b", md))
    # --keep: the angle that passed stays
    tv2 = FakeTv()
    hd.Doctor(tv2, tmp, log=lambda s: None).run(["D"], keep=True)
    check("--keep: the angle that passed stays (the watch on, «wanted» on)", tv2.wanted and hd.EXTRAS_SERVICE in tv2.a11y)
    # no add-on: D is «not applied», said why
    tv3 = FakeTv(extras=False)
    r3, w3 = hd.Doctor(tv3, tmp, log=lambda s: None).run(["D"])
    check("D without the add-on: not applied, and the why says to install it", not r3[0]["applied"] and "not installed" in r3[0]["why"] and w3 is None, r3[0]["why"])
    # E: the tester is asked once, F restarts the TV once (through tv.py --reboot)
    tv4 = FakeTv(extras=False)
    hd.Doctor(tv4, tmp, log=lambda s: None).run(["E", "F"])
    check("E asks the tester once (Enter); F restarts the TV once", tv4.asked == 1 and tv4.reboots == 1)
sys.exit(1 if failed else 0)
