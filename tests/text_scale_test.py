#!/usr/bin/env python3
"""4.4 beta 4: the settings' text was too large on some TVs. Their words are sp, sized for the usual 960 x 540 dp TV screen; a TV that reports a higher
density for its size (a 720p set at density 2) made them half again as large against the screen. Now SettingsActivity and AppearanceActivity fit
the font scale to the screen (TextScale: below 960 x 540 dp, the screen's share, never under 0.75; the person's own font size kept on top).
TextScale (no Android) at 720p and 1080p and their densities: a settings row's 19 sp words take no more of the screen's height than on the usual
TV, and the usual TVs change nothing. Then the wiring.
Run: python3 tests/text_scale_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

for name in ("SettingsActivity.java", "AppearanceActivity.java"):
    s = (APP / name).read_text(encoding="utf-8")
    check(f"{name}: both languages' contexts go through TextScale.fit", "super.attachBaseContext(TextScale.fit(en));" in s and "super.attachBaseContext(TextScale.fit(base.createConfigurationContext(c)));" in s)
ts = (APP / "TextScale.java").read_text(encoding="utf-8")
check("the person's own font size stays on top (it is multiplied, not replaced)", "cfg.fontScale = now.fontScale * f;" in ts)

if not shutil.which("javac"):
    print("SKIP text scale: no javac here")
    sys.exit(1 if failed else 0)
# (screen px w, h, density): the dp screen is px / density
SCREENS = [
    ("1080p at density 2 (the usual)", 1920, 1080, 2.0),
    ("720p at 1.33 (the usual 720p)", 1280, 720, 4 / 3),
    ("720p at density 2 (the report's kind)", 1280, 720, 2.0),
    ("720p at density 1.5", 1280, 720, 1.5),
    ("1080p at density 3", 1920, 1080, 3.0),
    ("1080p at density 1.5 (more room)", 1920, 1080, 1.5),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(APP / "TextScale.java"), str(ROOT / "tests/TextScaleRun.java")], capture_output=True, text=True)
    if r.returncode != 0:   # TextScale.fit uses Android's Context: the rule alone (factor) is checked with a stand-in Context
        stub = Path(tmp) / "stub"
        for pkg, cls, body in (("android/content", "Context", "public abstract class Context { public abstract android.content.res.Resources getResources(); public abstract Context createConfigurationContext(android.content.res.Configuration c); }"),
                               ("android/content/res", "Resources", "public class Resources { public Configuration getConfiguration() { return null; } }"),
                               ("android/content/res", "Configuration", "public class Configuration { public int screenWidthDp, screenHeightDp; public float fontScale; public Configuration() {} public Configuration(Configuration o) {} }")):
            d = stub / pkg; d.mkdir(parents=True, exist_ok=True)
            (d / f"{cls}.java").write_text(f"package {pkg.replace('/', '.')};\n{body}\n")
        r = subprocess.run(["javac", "-d", tmp, *[str(x) for x in stub.rglob("*.java")], str(APP / "TextScale.java"), str(ROOT / "tests/TextScaleRun.java")], capture_output=True, text=True)
    check("TextScale compiles", r.returncode == 0, r.stderr[-600:])
    if r.returncode == 0:
        args = [f"{round(w / d)},{round(h / d)}" for _, w, h, d in SCREENS]
        out = [float(x) for x in subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.TextScaleRun", *args], capture_output=True, text=True).stdout.split()]
        usual = 19 * 2.0 / 1080   # a settings row's 19 sp words on the usual 1080p TV: their share of the screen's height
        for (name, w, h, d), f in zip(SCREENS, out):
            share = 19 * d * f / h
            check(f"{name}: factor {f:.3f}, a row's words {share * 100:.2f} % of the height (the usual {usual * 100:.2f} %)", share <= usual * 1.12 + 1e-9 or f == 0.75, (f, share))
        check("the usual 1080p and 720p TVs: nothing changes (1.000)", out[0] == 1.0 and out[1] == 1.0, out[:2])
        check("720p at density 2: the words a quarter smaller (0.750, the least; was half again as large as on the usual TV, now an eighth)", out[2] == 0.75, out[2])
        check("never larger than before (a TV with more room keeps 1.000)", out[5] == 1.0 and all(f <= 1.0 for f in out), out)
sys.exit(1 if failed else 0)
