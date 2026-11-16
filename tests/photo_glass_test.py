#!/usr/bin/env python3
"""4.4 «الساعة: زجاجية خلف الصورة» on the home's own photos (docs/mockups/LOOKS.md): the big glass clock over the photo, and the photo's scenery over
the clock when it is one of ours with a cut-out made offline (.github/tools/photo_masks.py, only the ones checked by eye in photo_masks_ok.txt),
published in urtv-data masks/. Never «الحرمين», never the person's own photos; with «الساعة: عادية» (the default) nothing of it exists.

    python3 tests/photo_glass_test.py      (PhotoGlassPlan runs on a JVM: needs javac)"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p): return p.read_text(encoding="utf-8")


plan, glass, main = (read(HOME / f) for f in ("PhotoGlassPlan.java", "PhotoGlass.java", "MainActivity.java"))
tool = read(ROOT / ".github/tools/photo_masks.py")

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "PhotoGlassPlan.java"), str(ROOT / "tests/PhotoGlassRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.PhotoGlassRun"], check=True, capture_output=True).stdout.decode("utf-8").splitlines()
    rows = [l.split("\t") for l in out]
    key = {r[1]: r[2] for r in rows if r[0] == "key"}
    check("a pack's photo, our hosted photo and our bundled photo (the same names) have their cut-out's set and name",
          key["pack:nature:7"] == "nature/7" and key["pack:ksa-cities:1"] == "ksa-cities/1" and key["photo:9hWJs7iblh8.jpg"] == key["bundled:wallpapers/9hWJs7iblh8.jpg"] == "photos/9hWJs7iblh8", key)
    check("«الحرمين» never (the owner: no clock behind the Kaaba or the Prophet's Mosque), the person's own photos never, nothing odd (a path, a bad name)",
          all(key[k] == "-" for k in ("pack:haramain:3", "own:3:12345", "pack:x:1", "pack:sea:abc", "photo:../x.jpg", "photo:a.png", "null")), key)
    box = {r[1]: [int(x) for x in r[2].split(",")] for r in rows if r[0] == "box"}
    check("no cut-out: the digits in the middle with no cards, on the start half with them (right in Arabic, left in English), inside the screen",
          box["none-nocards"][0] + box["none-nocards"][2] == 1920 and box["none-cards-rtl"][0] >= 960 and box["none-cards-ltr"][2] <= 960
          and all(0 <= b[0] < b[2] <= 1920 and 0 <= b[1] < b[3] <= 1080 for b in box.values()), box)
    m = box["mid"]
    check("a skyline at mid-height: it crosses the digits' bottom (by 30 % of their height), so the scenery stands in front of them",
          m[1] < 540 < m[3] and abs((m[3] - 540) - round((m[3] - m[1]) * 0.3)) <= 1, m)
    check("a skyline too high or too low (or no skyline of 48): the digits at their usual place, never squeezed",
          box["high"] == box["low"] == box["short"] == box["none-nocards"], box)
    check("«كنبة» (the cards in a strip under the clock): the digits never reach the strip; a scenery that low leaves them at their usual place",
          box["lowish"][3] > 1080 * 0.55 and box["lowish-row"] == box["mid-row"] == box["none-nocards"], box)
    check("a 720 screen: the same place, scaled", all(abs(a * 2 / 3 - b) <= 2 for a, b in zip(m, box["mid720"])), box["mid720"])
else:
    print("SKIP photo glass rules: no javac here")

# ---- the home: made only while chosen
tick = main[main.index("private void photoGlassTick(ConfigSnapshot cfg, boolean want)"):].split("\n    }\n")[0]
body = [l.strip() for l in tick.split("\n", 1)[1].splitlines() if l.strip() and not l.strip().startswith("//")]
check("«الساعة: عادية» (the default): photoGlassTick's first step returns when not chosen and nothing exists; not chosen, it is shut down and removed",
      body[0] == "if (!want) {" and body[1].startswith("if (photoGlass == null) return;") and "photoGlass.shutdown();" in tick and "sceneBackdrop.removeView(photoGlass);" in tick
      and "photoGlass = null;" in tick, body[:2])
check("chosen only with the glass clock and the photos (the scenes have their own clock)", "photoGlassTick(cfg, !on && glass);" in main)
check("made in one place, over the two photo layers and under the dimming; the header clock hidden while it shows",
      main.count("new PhotoGlass(") == 1 and "sceneBackdrop.addView(photoGlass, sceneBackdrop.indexOfChild(wallFront) + 1," in tick and "clock.setVisibility(View.INVISIBLE);" in tick)
check("every other use waits for it to exist (the photo shown, the screen closed)", main.count("if (photoGlass != null) photoGlass.") == 2)
users = sorted(p.name for p in HOME.rglob("*.java") if re.search(r"\bPhotoGlass\b", read(p)) and p.name != "PhotoGlass.java")
check("only the home's screen knows it", users == ["MainActivity.java"], users)
check("each photo shown passes its key (pack:, photo:, bundled:, own:) and whether the network may be used now",
      "showWallpaperIfCurrent(bm, gen, key);" in main and "photoGlass.show(bm, key, NetGate.allowed(resumed, NetGateAndroid.displayOn(this)));" in main)

# ---- the cut-out: read, checked, kept
check("from urtv-data's masks/ (its CDN copy when GitHub's raw host cannot be reached), nowhere else",
      'HOSTS = {"https://raw.githubusercontent.com/tshi-dev/urtv-data/data/masks/", "https://cdn.jsdelivr.net/gh/tshi-dev/urtv-data@data/masks/"}' in glass)
check("its sha256 from the set's index checked before it is decoded or kept; 200 KB at most; moved into place whole; 40 kept",
      'SceneDownload.hex(MessageDigest.getInstance("SHA-256").digest(data)).equals(sha)' in glass and glass.index("equals(sha)") < glass.index("decodeByteArray")
      and "MAX_MASK = 200 * 1024, KEEP = 40" in glass and "tmp.renameTo(f)" in glass)
check("no network while it may not be used (another app in front, standby): a kept cut-out only", "if (!net) return null;" in glass and "if (net && (" in glass)
check("a weak TV (LOW): the clock in front, no cut-out read", "if (key == null || !ScenePlan.mask(tier)) return;" in glass)
check("a newer photo wins: a cut-out that arrives for an older one is not shown", "if (g != gen || !alive) return;" in glass)

# ---- the CI tool
check("the tool never cuts «الحرمين», writes at most 180 KB a cut-out, and publishes only the ones checked by eye",
      'NEVER = {"haramain"}' in tool and "MAX_FG = 180 * 1024" in tool and 'if ok is not None and f"{name}/{stem}" not in ok: continue' in tool)
ok = [l.strip() for l in read(ROOT / ".github/tools/photo_masks_ok.txt").splitlines() if l.strip() and not l.startswith("#")]
check("the reviewed list: set/name lines, no «الحرمين», no duplicates",
      all(re.fullmatch(r"(photos|nature|sea|space|ksa-cities)/[A-Za-z0-9_-]{1,40}", l) for l in ok) and len(set(ok)) == len(ok), ok)
check("the workflow runs the tool and puts its output on a data branch for urtv-data", "photo_masks.py" in read(ROOT / ".github/workflows/masks.yml"))
sys.exit(1 if failed else 0)
