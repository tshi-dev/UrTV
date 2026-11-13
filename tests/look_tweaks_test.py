#!/usr/bin/env python3
"""4.3 «اللون والتحكم»: eight accents, the tray's strength, the wallpaper's extra dimming and the focus style, on top of the nine looks. Defaults leave everything as it was; the
colours are checked for real (tests/AccentRun.java, every accent over every look); the plumbing (Prefs, the receiver, the provider, the launcher, the page) is pinned.

    python3 tests/look_tweaks_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "GlassStyle.java"), str(ROOT / "tests/AccentRun.java")], capture_output=True, text=True)
        check("it compiles", r.returncode == 0, r.stderr[-300:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.AccentRun"], capture_output=True, text=True).stdout
            bad = [l for l in out.splitlines() if l.startswith("FAIL")]
            check(f"every accent on every look, dark and light: the accent words and the focus ink read at 4.5:1 ({out.count('PASS ')} cases)", not bad and "ALL PASS" in out, bad[:3])
gs, prefs, rcv, prov, snap, main, paint = (read(HOME / "GlassStyle.java"), read(APP / "Prefs.java"), read(APP / "SaverReceiver.java"), read(APP / "ConfigProvider.java"),
                                           read(HOME / "ConfigSnapshot.java"), read(HOME / "MainActivity.java"), read(HOME / "GlassPaint.java"))
check("eight accents and three focus styles; nothing set means the look as it is (tinted() returns the look itself)",
      len(re.findall(r'\{"[a-z]+", "[^"]+", "#[0-9A-F]{6}"\}', gs.split("ACCENTS = {", 1)[1].split("};", 1)[0])) == 8 and 'FOCUS_STYLES = {"solid", "frame", "glow"}' in gs and "if (a == 0) return l;" in gs)
check("Prefs: each setting has its default (none, 100, 0, solid, «توحيد المظهر» on) and the string is empty while nothing was touched; both apps' GlassStyle is told",
      'if (a.isEmpty() && b == 100 && d == 0 && "solid".equals(f) && sync && !clear && "off".equals(glowMode())) return "";' in prefs and "GlassStyle.setTweaks(tweaks());" in prefs)
check("an agent can set accent, baropacity, walldim and focusstyle, and `show` says them",
      all(w in rcv for w in ('getStringExtra("accent")', 'getStringExtra("baropacity")', 'getStringExtra("walldim")', 'getStringExtra("focusstyle")', '"\\naccent: "', '"\\nbaropacity: "', '"\\nwalldim: "', '"\\nfocusstyle: "')))
check("the provider gives look_tweaks and the launcher reads it (an older screensaver says nothing: the look as it is)",
      '"look_tweaks"' in prov and 'getColumnIndex("look_tweaks")' in snap and "GlassStyle.setTweaks(tweaks);" in snap)
check("the home: the tray thins by the percent, the wallpaper dims by its own view, and a touch repaints", "GlassStyle.barPercent()" in main and "wallDimView.setAlpha(GlassStyle.dimPercent() / 100f)" in main and "GlassStyle.focusStyle()" in main)
check("the focus keeps its fill (the words' contrast) and only its line changes", '"frame".equals(style)' in paint and '"glow".equals(style)' in paint and "GradientDrawable d = fill(c, fill, radiusDp, 0);" in paint)
page = read(ROOT / "src/template.html")
check("the page: «تعتيم الخلفية» is a CSS variable at 0 by default, so the photo's own dimming is exactly what it was",
      "opacity:calc(1 - (1 - var(--dim,.05)) * (1 - var(--walldim,0)))" in page and "wallDim: Math.max(0, Math.min(90, parseInt(_Q.get('walldim'), 10) || 0))" in page and '(wallDim() > 0 ? "&walldim=" + wallDim() : "")' in prefs)
sys.exit(1 if failed else 0)
