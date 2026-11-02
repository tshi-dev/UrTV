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

sys.path.insert(0, str(Path(__file__).resolve().parent))
import setup_parity
from setup_parity import Pair, PKGS
if not setup_parity.compile_java(check):
    sys.exit(0 if not shutil.which("javac") else 1)

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
