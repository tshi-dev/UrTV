#!/usr/bin/env python3
"""4.4 beta 3: makes android/launcher/src/main/java/sa/urtv/home/PairCommandShapes.java, the shapes of every command the setup sends (the only ones the add-on's paired
route runs: PairGuard.allowed). It runs the setup's fake-TV scenarios with URTV_CMDLOG, adds the commands made outside the fake TV (PairGuardRun), and writes the list.
Run it after a change to the setup's commands; tests/pair_commands_test.py fails while the committed list is not exactly this.
    python3 tests/pair_commands_gen.py [--print]"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
OUT = HOME / "PairCommandShapes.java"
SCENARIOS = ["setup_engine_test", "setup_runner_test", "factory_test", "rescue_home_test", "safe_stock_test"]


def java_str(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def shapes():
    with tempfile.TemporaryDirectory() as tmp:
        log = Path(tmp) / "cmds.txt"
        env = dict(os.environ, URTV_CMDLOG=str(log))
        for t in SCENARIOS:
            r = subprocess.run([sys.executable, str(ROOT / "tests" / (t + ".py"))], env=env, capture_output=True, text=True)
            if r.returncode != 0:
                sys.exit(f"{t} failed; fix it first:\n{r.stdout[-800:]}{r.stderr[-800:]}")
        # compile against a stub list (the real one may be stale or missing)
        stub = Path(tmp) / "src/sa/urtv/home"; stub.mkdir(parents=True)
        (stub / "PairCommandShapes.java").write_text("package sa.urtv.home;\npublic final class PairCommandShapes { public static final String[] SHAPES = {}; }\n", encoding="utf-8")
        srcs = [HOME / "PairGuard.java", stub / "PairCommandShapes.java", HOME / "setup/IdleSetRule.java", ROOT / "tests/PairGuardRun.java"]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp + "/out", *map(str, srcs)], capture_output=True, text=True)
        if r.returncode != 0: sys.exit(r.stderr[-1500:])
        r = subprocess.run(["java", "-cp", tmp + "/out", "sa.urtv.home.setup.PairGuardRun", "shapes", str(log)], capture_output=True, text=True, encoding="utf-8")
        return [l for l in r.stdout.splitlines() if l]


def render(lines):
    body = ",\n".join("        " + java_str(l) for l in lines)
    return ("package sa.urtv.home;\n\n"
            "/**\n * 4.4 beta 3: the shapes of the setup's own commands, the ONLY ones the add-on's paired route runs (PairGuard.allowed). Made by tests/pair_commands_gen.py from every\n"
            " * command the setup's fake-TV scenarios send; tests/pair_commands_test.py fails while this is not exactly that list. Do not edit by hand.\n */\n"
            "public final class PairCommandShapes {\n    private PairCommandShapes() { }\n\n    public static final String[] SHAPES = {\n" + body + "\n    };\n}\n")


if __name__ == "__main__":
    if not shutil.which("javac"): sys.exit("needs javac")
    text = render(shapes())
    if "--print" in sys.argv: print(text, end="")
    else: OUT.write_text(text, encoding="utf-8"); print(f"wrote {OUT.relative_to(ROOT)}")
