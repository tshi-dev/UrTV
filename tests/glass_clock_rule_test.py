#!/usr/bin/env python3
"""4.4 beta 4, the owner's word (typed): in the «كنبة» layout the big glass clock stays in the middle ALWAYS, with the card strip (stocks, matches) under it:
no small clock at the top right, and the clock never moves between card states. The other layouts are as they were (a card beside the clock makes the
regular clock take its place, the owner's sweep 2). GlassClockRule is plain Java, run on a JVM; the home asks it with the cards shown and the row.
Run: python3 tests/glass_clock_rule_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (H / "MainActivity.java").read_text(encoding="utf-8")
check("the home asks the rule with the cards shown and whether they are in the couch strip",
      "boolean glass = glassClock(cfg.homeClock, homeCard != null && homeCard.shownCards() > 0, homeCard != null && homeCard.inRow());" in main
      and "static boolean glassClock(String homeClock, boolean cardsShown, boolean inRow) { return GlassClockRule.glass(homeClock, cardsShown, inRow); }" in main)
check("the photo's glass clock follows the same one (the cards beside it only: the strip is under the clock)",
      "boolean cards = homeCard != null && homeCard.shownCards() > 0 && !homeCard.inRow();" in main and "boolean row = homeCard != null && homeCard.inRow();" in main)
if not (shutil.which("javac") and shutil.which("java")):
    print("SKIP GlassClockRule: no javac here (CI compiles it)")
    sys.exit(1 if failed else 0)
run = """package sa.urtv.home;
public class GlassRun { static void o(String n, boolean ok) { System.out.println(n + "\\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  o("couch, a card in the strip: the glass clock stays", GlassClockRule.glass("glass", true, true));
  o("couch, no card: the glass clock (the same place)", GlassClockRule.glass("glass", false, true));
  o("another layout, no card: the glass clock", GlassClockRule.glass("glass", false, false));
  o("another layout, a card beside the clock: the regular clock (option 2 stays)", !GlassClockRule.glass("glass", true, false));
  // the strip under the always-centred clock (the UI session's spacing, 1080 spec scaled with FavBar.px): its top is 32 under the digits, its bottom 28 over the
  // apps row with the focus growth (810), on the tray's own sides; the same at 1080p and at 720p (density 213)
  int[][] screens = {{1920, 1080, 2000}, {1280, 720, 1331}};   // w, h, density x 1000
  boolean fits = true, exact = true;
  for (int[] sc : screens) for (int titleTop : new int[] {-1, Math.round(sc[1] * 0.9f)}) {
    float d = sc[2] / 1000f;
    int clockBottom = ScenePlan.fixedBox(sc[0], sc[1], false, true)[3];
    int[] r = CardBox.row(sc[0], sc[1], d, titleTop, clockBottom);
    int top = sc[1] - r[2] - r[1], bottom = sc[1] - r[2];
    fits &= top >= clockBottom + FavBar.px(32, sc[1]) && bottom <= FavBar.px(810 - 28, sc[1]) && r[1] > 0;
    exact &= r[0] == sc[0] - 2 * FavBar.px(FavBar.SIDE, sc[1]) && bottom == FavBar.px(782, sc[1]) && r[2] == sc[1] - FavBar.px(782, sc[1]);
  }
  o("the strip starts 32 under the digits and ends 28 over the apps row (growth 8 included), at 1080p and at 720p", fits);
  int[] r1080 = CardBox.row(1920, 1080, 2f, -1, 542), r720 = CardBox.row(1280, 720, 1.331f, -1, ScenePlan.fixedBox(1280, 720, false, true)[3]);
  o("on the tray's own sides (SIDE 122), 208 high and 298 from the bottom at 1080p, 139 high and 199 at 720p", exact && r1080[1] == 208 && r1080[2] == 298 && r720[1] == 139 && r720[2] == 199 && r1080[0] == 1920 - 244);
  o("«الساعة: عادية»: never the glass clock, with or without cards", !GlassClockRule.glass("plain", false, true) && !GlassClockRule.glass("plain", true, true) && !GlassClockRule.glass(null, false, false));
} }"""
with tempfile.TemporaryDirectory() as tmp:
    Path(tmp, "GlassRun.java").write_text(run, encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "GlassClockRule.java"), str(H / "CardBox.java"), str(H / "ScenePlan.java"), str(H / "FavBar.java"), str(Path(tmp, "GlassRun.java"))], capture_output=True, text=True)
    check("GlassClockRule compiles on its own", r.returncode == 0, r.stderr[-300:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.GlassRun"], capture_output=True, text=True).stdout
        for line in out.splitlines():
            n, res = line.rsplit("\t", 1)
            check(n, res == "ok")
        check("every case ran", len(out.splitlines()) == 7, len(out.splitlines()))
sys.exit(1 if failed else 0)
