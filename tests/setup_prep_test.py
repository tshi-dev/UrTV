"""The app's ports of optimize/backup.sh (with run.sh's check of it), keep.sh, autostart.sh and tweaks.sh (SetupPrep.java, SetupTweaks.java), each
run next to the script itself against ONE fake TV (tests/fixtures/setup-faketv.sh): the same commands sent, the same journal written, the same TV
afterwards; and the undo puts back what the script's restore puts back. The engine's undo follows optimize/reset.sh's order. No TV needed.

    python3 tests/setup_prep_test.py
"""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
STUB = ROOT / "tests/fixtures/setup-faketv.sh"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

if not shutil.which("javac"):
    print("SKIP the parity tests: no javac on this machine")
    sys.exit(0)
CLS = Path(tempfile.mkdtemp())
pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")]
r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(CLS), *map(str, pure), str(ROOT / "tests/SetupParityRun.java")], capture_output=True, text=True)
check("the pure classes and the parity harness compile", r.returncode == 0, r.stderr)
if r.returncode:
    sys.exit(1)

PKGS = ["com.tcl.airplay2", "com.google.android.katniss", "com.android.vending", "com.google.android.gms", "com.google.android.apps.tv.launcherx",
        "sa.urtv.home", "sa.urtv.screensaver", "com.tcl.bi", "com.tcl.logkit"] + [f"com.example.app{i}" for i in range(20)]

class Pair:
    """One TV state, twice: one copy for the script, one for the app. env: the fake TV's knobs."""
    def __init__(self, packages=PKGS, off=(), files=None, **env):
        self.base = Path(tempfile.mkdtemp())
        self.env = {k: str(v) for k, v in env.items()}
        for side in ("sh", "java"):
            tvd = self.base / side / "tv"; tvd.mkdir(parents=True)
            (tvd / "packages").write_text("".join(p + "\n" for p in packages))
            (tvd / "off").write_text("".join(p + "\n" for p in off))
            for k, v in (files or {}).items():
                (tvd / k).write_text(v + "\n")
        self.tree = self.base / "tree"; (self.tree / "optimize/run").mkdir(parents=True)
        for f in ("tweaks.sh", "backup.sh", "keep.sh", "autostart.sh"):
            shutil.copy(ROOT / "optimize" / f, self.tree / "optimize")
        shutil.copytree(ROOT / "optimize/lists", self.tree / "optimize/lists")
        self.store = self.base / "store"; self.store.mkdir()
    def _env(self, side):
        return dict(os.environ, T=str(self.base / side), PY=str(STUB), URTV_RUN_DIR=str(self.tree / "optimize/run"), **self.env)
    def shell(self, script, *args):
        r = subprocess.run(["bash", str(self.tree / "optimize" / script), *args], env=self._env("sh"), capture_output=True, text=True, cwd=self.tree)
        return r.returncode, r.stdout + r.stderr
    def java(self, step):
        r = subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.SetupParityRun", step, str(STUB), str(self.store)], env=self._env("java"),
                           capture_output=True, text=True, encoding="utf-8")
        res = re.findall(r"^RESULT (\w+)$", r.stdout, re.M)
        return (res[0] if res else "CRASH " + r.stderr[-500:]), r.stdout
    def changes(self, side):
        f = self.base / side / "changes"
        return f.read_text().splitlines() if f.exists() else []
    def tv(self, side):
        d = self.base / side / "tv"
        return {f.name: f.read_text() for f in sorted(d.iterdir())}
    def file(self, where, name):
        f = (self.tree / "optimize/run" if where == "sh" else self.store) / name
        return f.read_text() if f.exists() and f.read_text() != "" else None
    def same(self):
        return self.changes("sh") == self.changes("java") and self.tv("sh") == self.tv("java")
    def diff(self):
        return {"sh": self.changes("sh"), "java": self.changes("java"), "tvsh": self.tv("sh"), "tvjava": self.tv("java")}

# ---- tweaks.sh ----------------------------------------------------------------------------------------------------------------------
def tweaks_case(name, files=None, **env):
    p = Pair(files=files, **env)
    rc, out = p.shell("tweaks.sh", "apply")
    res, jout = p.java("tweaks-apply")
    j_sh, j_app = p.file("sh", "tweaks-journal.txt"), p.file("java", "tweaks-journal.txt")
    refused = "did not take the value" in out
    check(f"tweaks {name}: apply sends the script's commands, writes its journal to the letter, leaves the TV the same",
          p.same() and j_sh == j_app and j_sh is not None, (p.diff(), j_sh, j_app, out))
    check(f"tweaks {name}: a value the TV refused makes the app's step a failure, as the script prints «!!»", (res == "FAILED") == refused and res in ("DONE", "FAILED"), (res, out))
    before = p.tv("sh")
    rc, out = p.shell("tweaks.sh", "restore")
    res, jout = p.java("tweaks-restore")
    check(f"tweaks {name}: restore puts back the same values, and keeps or retires the journal the same way",
          p.same() and p.file("sh", "tweaks-journal.txt") == p.file("java", "tweaks-journal.txt") and p.file("sh", "tweaks-journal.done") == p.file("java", "tweaks-journal.done"),
          (p.diff(), out, jout))
    return p

anim = {"global.window_animation_scale": "1.0", "global.transition_animation_scale": "1.0", "global.animator_duration_scale": "1.0"}
p = tweaks_case("(a TV with nothing set but its animations)", files=anim)
check("tweaks: after apply and restore the TV is as it was (null deleted, not written)", "global.cached_apps_freezer" not in p.tv("java") and p.tv("java")["global.window_animation_scale"] == "1.0\n", p.tv("java"))
tweaks_case("(a TV with its own values)", files={**anim, "global.window_animation_scale": "0.8", "global.cached_apps_freezer": "disabled", "dc.max_cached_processes": "64", "bucket": "20",
                                                  "op.com.android.vending.RUN_IN_BACKGROUND": "ignore"})
tweaks_case("(Android 9: no device_config, the «rare» bucket)", files=anim, SDK=28)
tweaks_case("(Android 8: no standby buckets either)", files=anim, SDK=27)
tweaks_case("(storage low: caches trimmed)", files=anim, DF_AVAIL=500000)
tweaks_case("(the Play Store's bucket and appops cannot be read: left alone)", files=anim, UNREADABLE="yes")
tweaks_case("(the TV refuses an animation value)", files=anim, REFUSE="window_animation_scale")
tweaks_case("(a test build keeps its own bucket)", files=anim, REFUSE="standby_bucket", BUILD_TYPE="userdebug")
p = tweaks_case("(an animation the TV does not say: never recorded as null, not changed)", files=anim, EMPTYREAD="transition_animation_scale")
# a second apply keeps the first record
p = Pair(files=anim)
p.shell("tweaks.sh", "apply"); p.java("tweaks-apply")
first = p.file("java", "tweaks-journal.txt")
p.shell("tweaks.sh", "apply"); p.java("tweaks-apply")
check("tweaks: a second apply keeps the journal of the first (the TV is not «as it came» then), as the script does",
      p.file("java", "tweaks-journal.txt") == first == p.file("sh", "tweaks-journal.txt") and p.same(), p.diff())
src = (SETUP / "SetupTweaks.java").read_text(encoding="utf-8")
check("tweaks: «unknown» is never written as null (an empty read is unknown; only the TV's word «null» means not set)",
      'return v.isEmpty() ? "unknown" : v;' in src and not re.search(r'"null"\s*:\s*"unknown"', src))
check("tweaks: the app's undo writes nothing when it never changed a tweak (no journal)", "if (!present(store.get(JOURNAL))) return new SetupEngine.Result(SetupEngine.Status.DONE, \"\");" in src)

# ---- backup.sh and run.sh's check -----------------------------------------------------------------------------------------------------
def runsh_accepts(path):
    return subprocess.run(["bash", "-c", '[ "$(grep -c \'^package:\' "$1" || true)" -lt 20 ] || ! grep -qx \'package:android\' "$1"', "x", str(path)]).returncode != 0
p = Pair(files={**anim, "secure.screensaver_components": "com.google.android.apps.tv.dreamx/.service.Dream"}, off=["com.tcl.bi"])
rc, out = p.shell("backup.sh")
res, jout = p.java("backup")
shf = sorted((p.tree / "optimize").glob("backup-*.txt"))
check("backup: the app saves exactly the file backup.sh saves (the same lines, the same answers)", rc == 0 and shf and p.file("java", "backup.txt") == shf[0].read_text() and res == "DONE",
      (rc, out, res, p.file("java", "backup.txt"), shf[0].read_text() if shf else None))
check("backup: and run.sh's check accepts it", runsh_accepts(shf[0]))
res2, _ = p.java("backup")
check("backup: the first run's backup stays (a later run does not overwrite it)", res2 == "DONE" and p.file("java", "backup.txt") == shf[0].read_text())
for name, kw in (("19 packages", {"packages": [f"com.x.a{i}" for i in range(18)]}), ("no package:android", {"NO_ANDROID": "yes"})):
    p = Pair(**kw)
    p.shell("backup.sh"); res, _ = p.java("backup")
    shf = sorted((p.tree / "optimize").glob("backup-*.txt"))
    check(f"backup: refused like run.sh refuses it ({name}): nothing saved, nothing changed", res == "FAILED" and p.file("java", "backup.txt") is None and not runsh_accepts(shf[0])
          and p.changes("java") == [], (res, p.changes("java")))

# ---- keep.sh ----------------------------------------------------------------------------------------------------------------------------
BACKUP_OFF = lambda pk: "$ echo '== disabled packages'; pm list packages -d\n== disabled packages\n" + "".join(f"package:{x}\n" for x in pk) + "$ echo '== all packages'; pm list packages\n== all packages\npackage:android\n"
for name, off, stuck, before, want_ok in (
        ("two protected features off, the stock home off too (not keep's)", ["com.tcl.airplay2", "com.google.android.katniss", "com.google.android.apps.tv.launcherx", "com.tcl.bi"], "", [], True),
        ("one that the TV keeps off, and had off before UrTV", ["com.tcl.airplay2"], "com.tcl.airplay2", ["com.tcl.airplay2"], True),
        ("one that the TV will not turn on, and was on before UrTV", ["com.tcl.airplay2"], "com.tcl.airplay2", [], False),
        ("all on", ["com.tcl.bi"], "", [], True)):
    p = Pair(off=off, STUCK=stuck)
    (p.tree / "optimize/run/backup.txt").write_text(BACKUP_OFF(before)); (p.store / "backup.txt").write_text(BACKUP_OFF(before))
    rc, out = p.shell("keep.sh")
    res, jout = p.java("keep")
    check(f"keep: {name}: the same commands, the same TV, and a failure exactly where keep.sh stops", p.same() and (rc == 0) == want_ok and (res == "DONE") == want_ok, (p.diff(), rc, out, res, jout))
p = Pair(off=["com.tcl.airplay2"], SILENT_OFF="yes")
rc, out = p.shell("keep.sh"); res, _ = p.java("keep")
check("keep: a TV that does not end its list of disabled apps: the app stops (fail closed, stricter than keep.sh's «all on») and changes nothing",
      res == "FAILED" and p.changes("java") == [], (res, p.changes("java"), out))

# ---- autostart.sh -----------------------------------------------------------------------------------------------------------------------
for name, env, pk in (("not a TCL", {"MAKER": "Sony"}, [x for x in PKGS if not x.startswith("com.tcl.")]),
                      ("a TCL that knows APP_AUTO_START (G10)", {"AUTOSTART_OPS": "APP_AUTO_START"}, PKGS),
                      ("a TCL that knows AUTO_START (G08)", {"AUTOSTART_OPS": "AUTO_START"}, PKGS),
                      ("a TCL that knows neither", {}, PKGS),
                      ("a TCL with only the home app", {"AUTOSTART_OPS": "APP_AUTO_START"}, [x for x in PKGS if x != "sa.urtv.screensaver"])):
    p = Pair(packages=pk, **env)
    before = p.tv("java")
    rc, out = p.shell("autostart.sh")
    res, jout = p.java("autostart")
    check(f"autostart: {name}: the same commands and the same TV as autostart.sh, and DONE", p.same() and res == "DONE" and "!!" not in out, (p.diff(), out, jout))
    if "AUTOSTART_OPS" in env:
        journal = p.file("java", "autostart-before.txt") or ""
        check(f"autostart: {name}: what each op had is journaled before the change", re.search(r"^sa\.urtv\.home " + env["AUTOSTART_OPS"] + r" default$", journal, re.M) is not None, journal)
        p.java("autostart-undo")
        check(f"autostart: {name}: the undo gives each op back what it had (the TV as before), and writes the settings", p.tv("java") == before and p.changes("java")[-1] == "appops write-settings",
              (p.tv("java"), before, p.changes("java")))
p = Pair(AUTOSTART_OPS="APP_AUTO_START", REFUSE="APP_AUTO_START", files={"op.sa.urtv.home.APP_AUTO_START": "ignore"})
rc, out = p.shell("autostart.sh"); res, _ = p.java("autostart")
check("autostart: a TCL that refuses: autostart.sh's «!!» line, the app's step failed", "!! Auto-start" in out and res == "FAILED" and p.same(), (out, res))

# ---- the engine's undo, in reset.sh's order ---------------------------------------------------------------------------------------------
eng = (SETUP / "SetupEngine.java").read_text(encoding="utf-8")
u = eng[eng.index("Result undo()"):]
order = [u.index(x) for x in ("putStockBack(stock, pkg, journalHolder(), true)", 'ledgerNames(store.get("disabled-by-us.txt"))', "new SetupPrep(sh, store, out).keep()",
                               'store.get("screensaver-before.txt")', '"exact-alarm-appop.txt"', "autostartUndo()", "new SetupTweaks(sh, store, out).restore()")]
check("undo: reset.sh's order: the stock home first, the ledger, the protected features, the screensaver, the appops, TCL's auto-start, the tweaks last", order == sorted(order), order)
sys.exit(1 if failed else 0)
