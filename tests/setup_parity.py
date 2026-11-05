"""Shared by the parity tests of the setup in the app (tests/setup_prep_test.py, tests/setup_community_test.py): the pure classes and
tests/SetupParityRun.java compiled once, and a Pair: one fake TV state (tests/fixtures/setup-faketv.sh), copied for a script of optimize/ and
for the app's port of it, so their commands, files and the TV afterwards can be compared. Not a test by itself.
"""
import os, re, shutil, subprocess, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
STUB = ROOT / "tests/fixtures/setup-faketv.sh"
ASSETS = ROOT / "android/launcher/src/main/assets"
CLS = Path(tempfile.mkdtemp())
SCRIPTS = ("tweaks.sh", "backup.sh", "keep.sh", "autostart.sh", "community.sh", "community.py", "disable.sh", "offlist.py", "settle.sh", "measure.sh", "start-saver.sh", "power.sh")


def compile_java(check):
    """False (and a SKIP line) without javac; else compiles and checks it."""
    if not shutil.which("javac"):
        print("SKIP the parity tests: no javac on this machine")
        return False
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")] + [SETUP.parent / "phone/UpdateRules.java"]   # (read only: its rule for updates)
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(CLS), *map(str, pure), str(ROOT / "tests/SetupParityRun.java"), str(ROOT / "tests/CommunityRun.java")],
                       capture_output=True, text=True)
    check("the pure classes and the parity harness compile", r.returncode == 0, r.stderr)
    return r.returncode == 0


PKGS = ["com.tcl.airplay2", "com.google.android.katniss", "com.android.vending", "com.google.android.gms", "com.google.android.apps.tv.launcherx",
        "sa.urtv.home", "sa.urtv.screensaver", "com.tcl.bi", "com.tcl.logkit"] + [f"com.example.app{i}" for i in range(20)]

# the shell's PY: tv.py goes to the fake TV, any other script of optimize/ to a real python (check-packages.py is the tree's own fake)
PYWRAP = """#!/usr/bin/env bash
if [ "$2" = optimize/tv.py ]; then exec "$STUB" "$@"; else shift; exec python3 -I "$@"; fi
"""


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
        for f in SCRIPTS:
            shutil.copy(ROOT / "optimize" / f, self.tree / "optimize")
        shutil.copytree(ROOT / "optimize/lists", self.tree / "optimize/lists")
        (self.base / "py").write_text(PYWRAP); (self.base / "py").chmod(0o755)
        (self.base / "home").mkdir()
        self.store = self.base / "store"; self.store.mkdir()
    def _env(self, side):
        return dict(os.environ, T=str(self.base / side), PY=str(self.base / "py"), STUB=str(STUB), URTV_RUN_DIR=str(self.tree / "optimize/run"),
                    URTV_HOME=str(self.base / "home"), PACKAGE_SETTLE_S="0", **self.env)
    def shell(self, script, *args):
        r = subprocess.run(["bash", str(self.tree / "optimize" / script), *args], env=self._env("sh"), capture_output=True, text=True, cwd=self.tree)
        return r.returncode, r.stdout + r.stderr
    def java(self, step):
        r = subprocess.run(["java", "-Dassets=" + str(ASSETS), "-cp", str(CLS), "sa.urtv.home.setup.SetupParityRun", step, str(STUB), str(self.store)],
                           env=self._env("java"), capture_output=True, text=True, encoding="utf-8")
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
