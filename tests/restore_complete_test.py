"""optimize/restore.sh puts back every package ANY of our disable runs turned off (a second run, a list that changed or is gone), not
only the lists it is given; a package that was never installed is not in it. Fake TV; no TV needed.

    python3 tests/restore_complete_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STUB = r'''#!/usr/bin/env bash
shift 2
case "$1" in
  --batch) cat >> "$T/batch" ;;
  "pm list packages; echo END_OF_LIST"|"pm list packages")
    echo package:android; echo package:com.a.one; echo package:com.a.two; echo package:com.c.four; echo package:com.b.three
    echo package:com.google.android.tvlauncher; [[ "$1" == *END* ]] && echo END_OF_LIST ;;
  "pm list packages -d; echo END_OF_LIST")   # 1st read of a run: nothing was off; 2nd read: what the batch turned off
    n=$(cat "$T/n" 2>/dev/null || echo 0); echo $(( (n+1) % 2 )) > "$T/n"
    [ "$n" = 0 ] || printf 'package:%s\\n' com.a.one com.a.two com.c.four
    echo END_OF_LIST ;;
  *"dumpsys package"*) echo END_OF_LIST ;;   # disable.sh's input guard: no input driver here
esac
exit 0
'''
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

t = Path(tempfile.mkdtemp()); tree = t / "tree"; lists = tree / "optimize" / "lists"; lists.mkdir(parents=True)
for f in ("disable.sh", "restore.sh", "home-journal.sh", "settle.sh"):
    shutil.copy(ROOT / "optimize" / f, tree / "optimize")
(lists / "keep.txt").write_text("com.keep.me\n"); (lists / "core.txt").write_text("com.core.pkg\n"); (lists / "home-replaced.txt").write_text("com.tcl.helper\n")
(tree / "optimize" / "offlist.py").write_text("")
(t / "py").write_text(STUB); (t / "py").chmod(0o755)
(lists / "first.txt").write_text("# a list\ncom.a.one\ncom.a.two  # note\n")
(lists / "second.txt").write_text("com.b.three\ncom.keep.me\n")
env = dict(os.environ, T=str(t), PY=str(t / "py"), URTV_RUN_DIR=str(tree / "optimize/run"), PACKAGE_SETTLE_S="0")
def sh(script, *args):
    return subprocess.run(["bash", str(tree / "optimize" / script), *args], env=env, capture_output=True, text=True, cwd=tree)

r1 = sh("disable.sh", "optimize/lists/first.txt")
r2 = sh("disable.sh", "optimize/lists/second.txt")   # refuses (keep.txt): nothing disabled, nothing remembered from it
ledger = (tree / "optimize/run/disabled-by-us.txt").read_text().split()
check("a disable run remembers what it turned off", r1.returncode == 0 and ledger == ["com.a.one", "com.a.two"], f"{r1.stderr} {ledger}")
check("a refused run (a package in keep.txt) remembers nothing", r2.returncode == 1 and "com.b.three" not in ledger and "com.keep.me" not in ledger, r2.stdout)
(lists / "third.txt").write_text("com.c.four\ncom.a.one\n")
sh("disable.sh", "optimize/lists/third.txt")   # a second run, another list; one package twice
ledger = (tree / "optimize/run/disabled-by-us.txt").read_text().split()
check("a second run adds to it, once each", ledger == ["com.a.one", "com.a.two", "com.c.four"], str(ledger))
check("the disable time is marked", (tree / "optimize/run/.package-change").exists())

# the lists change or are gone: restore with no list still re-enables everything that was turned off
(lists / "first.txt").unlink(); (lists / "third.txt").unlink()
(t / "batch").unlink(missing_ok=True)
r = sh("restore.sh")
b = (t / "batch").read_text().splitlines()
check("restore with no list: every package any run turned off is turned on (we never uninstall, so nothing is installed again)",
      all(f"pm enable {p}" in b for p in ("com.a.one", "com.a.two", "com.c.four")) and not any("install-existing" in x for x in b), str(b))
check("and the stock home's helpers and the home are still put back", "pm enable com.tcl.helper" in b and "input keyevent KEYCODE_HOME" in b, str(b))
check("restore is not harmed by a missing ledger (nothing disabled by us)", subprocess.run(["bash", str(tree / "optimize/restore.sh")], env=dict(env, URTV_RUN_DIR=str(t / "norun")), capture_output=True, text=True, cwd=tree).returncode == 0)

# ---- restore.sh on its own: the home recovery never depends on the package part (RC02), and only our own packages come back (RC04)
RSTUB = r'''#!/usr/bin/env bash
shift 2
case "$1" in
  --batch) cat >> "$T/batch" ;;
  "pm list packages") echo package:android; echo package:com.google.android.tvlauncher ;;
esac
exit 0
'''
def rtree(ledger=None, lists=None, backup=None):
    """ledger: text, a ('dir',) for an unreadable one, or None for none; lists: {name: text or ('dir',)}; backup: text or None."""
    d = Path(tempfile.mkdtemp()); tr = d / "tree"; (tr / "optimize" / "lists").mkdir(parents=True); run = tr / "optimize" / "run"; run.mkdir()
    for f in ("restore.sh", "home-journal.sh", "settle.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    (tr / "optimize/lists/home-replaced.txt").write_text("# the stock home's helpers\ncom.tcl.helper\n")
    (d / "py").write_text(RSTUB); (d / "py").chmod(0o755)
    if ledger == ("dir",): (run / "disabled-by-us.txt").mkdir()
    elif ledger is not None: (run / "disabled-by-us.txt").write_text(ledger)
    args = []
    for name, body in (lists or {}).items():
        f = tr / "optimize/lists" / name
        if body == ("dir",): f.mkdir()
        else: f.write_text(body)
        args.append(f"optimize/lists/{name}")
    if backup is not None: (run / "backup.txt").write_text(backup)
    e = dict(os.environ, T=str(d), PY=str(d / "py"), URTV_RUN_DIR=str(run), PACKAGE_SETTLE_S="0")
    r = subprocess.run(["bash", str(tr / "optimize/restore.sh"), *args], env=e, capture_output=True, text=True, cwd=tr)
    b = (d / "batch").read_text().splitlines() if (d / "batch").exists() else []
    return r, b
HOME_LINES = ("pm enable com.google.android.tvlauncher", "cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity", "touch /data/local/tmp/home_ok", "input keyevent KEYCODE_HOME")
has_home = lambda b: all(x in b for x in HOME_LINES) and any("add-role-holder" in x for x in b)

r, b = rtree(ledger="")
check("RC02: an EMPTY ledger: restore does not die, the stock home (enable, role, set-home) and the helpers are sent, exit 0", r.returncode == 0 and has_home(b) and "pm enable com.tcl.helper" in b, f"{r.returncode} {r.stderr} {b}")
r, b = rtree(ledger="# nothing\n\n  \n")
check("RC02: a ledger of comments and blank lines only: the same", r.returncode == 0 and has_home(b), f"{r.returncode} {r.stderr} {b}")
r, b = rtree(ledger=None, lists={"only-comments.txt": "# a list with no package\n\n"})
check("RC02: a list file of comments only (no ledger): the home recovery still goes", r.returncode == 0 and has_home(b), f"{r.returncode} {r.stderr} {b}")
r, b = rtree(ledger="com.a.one\nnot a package!\n\ncom.a.two\ncom.a.one\n")
check("RC04: a ledger: its valid packages once each, and not a line that is not a package name", all(f"pm enable {p}" in b for p in ("com.a.one", "com.a.two")) and b.count("pm enable com.a.one") == 1
      and not any("not a package" in x for x in b) and has_home(b), str(b))
r, b = rtree(ledger="com.a.one\n", lists={"l.txt": "com.a.one\ncom.owner.had.it.off\ncom.maker.off\n"})
check("RC04: with a ledger, a package of the lists that is NOT in it (the owner or the maker had it off before UrTV) is not turned on", "pm enable com.owner.had.it.off" not in b
      and "pm enable com.maker.off" not in b and "pm enable com.a.one" in b and has_home(b), str(b))
r, b = rtree(ledger="")
check("RC04: with a ledger the stock home's helpers still come back even when the ledger is empty", "pm enable com.tcl.helper" in b, str(b))
r, b = rtree(ledger=("dir",))
check("an unreadable ledger is fatal at the end (!! line, exit 1) but the home recovery was already sent", r.returncode == 1 and "!! restore:" in r.stdout and has_home(b), f"{r.returncode} {r.stdout} {r.stderr} {b}")
r, b = rtree(ledger=None, lists={"bad.txt": ("dir",)})
check("an unreadable list is fatal at the end (!! line, exit 1) but the home recovery was already sent", r.returncode == 1 and "!! restore:" in r.stdout and has_home(b), f"{r.returncode} {r.stdout} {r.stderr} {b}")
r, b = rtree(ledger=None, lists={"l.txt": "com.x.one\ncom.pre.off\n"})
check("legacy, no ledger and no backup: every package of the lists, and a plain note says so", "pm enable com.x.one" in b and "pm enable com.pre.off" in b and "no record of what our setup turned off" in r.stdout, f"{r.stdout} {b}")
backup = "== all packages\n" + "".join(f"package:com.p{i}\n" for i in range(25)) + "== disabled packages\npackage:com.pre.off\n== screensaver\nnull\n"
r, b = rtree(ledger=None, lists={"l.txt": "com.x.one\ncom.pre.off\n"}, backup=backup)
check("legacy, no ledger but a backup: a package the TV had off before UrTV (the backup's «disabled packages») stays off", "pm enable com.x.one" in b and "pm enable com.pre.off" not in b and has_home(b), str(b))
backup25 = "== all packages\n" + "".join(f"package:com.p{i}\n" for i in range(25)) + "== disabled packages\npackage:com.pre.off\n== screensaver\nnull\n"
r, b = rtree(ledger="com.led.new\n", lists={"l.txt": "com.old.a\ncom.pre.off\n"}, backup=backup25)
check("older install: a ledger that only has the later run + a real backup: the ledger PLUS the lists minus what was off before UrTV",
      "pm enable com.led.new" in b and "pm enable com.old.a" in b and "pm enable com.pre.off" not in b and has_home(b), str(b))
r, b = rtree(ledger="com.led.new\n", lists={"l.txt": "com.old.a\n"})
check("a ledger and no backup: the ledger alone", "pm enable com.led.new" in b and "pm enable com.old.a" not in b, str(b))
r, b = rtree(ledger="com.led.new\n", backup=backup25, lists={"l.txt": "com.old.a\n"})
i_home, i_pkg, i_key = b.index("cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity"), b.index("pm enable com.led.new"), b.index("input keyevent KEYCODE_HOME")
check("order: the helpers, the setup app, home_ok, the stock enable, role and set-home come BEFORE the package enables, and KEYCODE_HOME is last",
      b.index("pm enable com.tcl.helper") < b.index("touch /data/local/tmp/home_ok") < b.index("pm enable com.google.android.tvlauncher") < i_home < i_pkg < i_key and b[-1] == "input keyevent KEYCODE_HOME", str(b))
r, b = rtree(ledger="")
check("an empty backup 'disabled' section does not drop everything (no grep -f with an empty pattern file)", has_home(b), str(b))
sys.exit(1 if failed else 0)
