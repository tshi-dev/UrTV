"""The report's «Home switch: skipped» line is rewritten on each launcher run (R-373).

A run that skipped the home switch wrote «Home switch: skipped, …» into report.md; a later `install.sh launcher` that made the
switch removed home-skipped.txt but left the line in the report, so share.sh and the agent still read «skipped». The block
that rewrites it is taken out of install.sh and run on made-up reports. No TV needed.

    python3 tests/home_skip_line_test.py
"""
import re, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

src = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
m = re.search(r'\n  rep="\$\{URTV_RUN_DIR:-optimize/run\}/report\.md"\n  if \[ -f "\$rep" \]; then\n.*?\n  fi\n', src, re.S)
check("install.sh rewrites the report's home-switch line", m is not None)
launcher = src[src.index('if [ "$what" = launcher ] || [ "$what" = all ]; then'):src.index('echo "Undo everything: optimize/reset.sh"')]
check("it runs in the launcher step, after the switch's result is known (both «Launcher is home» paths and every skip)",
      m is not None and m.group(0) in launcher and launcher.index(m.group(0)) > launcher.rindex('echo "Launcher is home.'))

REPORT = "# TV setup report\n\nHome: sa.stock/.Home\n{skip}Auto-start (TCL): ok\nScreensaver: x\n"
def rewrite(report, skipped):
    d = Path(tempfile.mkdtemp())
    (d / "report.md").write_text(report)
    if skipped:
        (d / "home-skipped.txt").write_text(skipped + "\n")
    subprocess.run(["bash", "-c", f'URTV_RUN_DIR="{d}"\n' + (m.group(0) if m else "false")], check=False)
    return (d / "report.md").read_text()

OLD = "Home switch: skipped, this TV keeps its own home screen (open UrTV from its «تلفزيونك» tile in the apps row)\n"
out = rewrite(REPORT.format(skip=OLD), None)
check("a later run made the switch: the «skipped» line is gone, the rest kept", "Home switch: skipped" not in out and out == REPORT.format(skip=""), out)
out = rewrite(REPORT.format(skip=OLD), "Home switch: skipped, this TV refuses to turn its own home screen off")
check("skipped again, another way: the new reason replaces the old one, once", out.count("Home switch: skipped") == 1 and "refuses" in out, out)
out = rewrite(REPORT.format(skip=""), "Home switch: skipped, could not tell which app is this TV's own home screen")
check("skipped now, not before: the line goes under «Home:»", out.splitlines()[3].startswith("Home switch: skipped, could not tell"), out)
sys.exit(1 if bad else 0)
