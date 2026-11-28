#!/usr/bin/env python3
"""4.4 the icons of the family (docs/mockups/icons, the owner's OK): the screensaver, the rescue and the extras take one adaptive icon each
(background, foreground, monochrome, made by .github/tools/icons/make.js) and a flattened PNG for older TVs at three densities. The gradients
sit in drawable-v26 only (they need API 24; the minSdk-21 apps read the PNGs below Android 8). Run: python3 tests/icons_family_test.py"""
import re
from pathlib import Path
from xml.dom import minidom
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

APPS = [("app", "ic_launcher"), ("rescue", "ic_rescue"), ("extras", "ic_extras")]
for module, name in APPS:
    res = ROOT / f"android/{module}/src/main/res"
    manifest = (ROOT / f"android/{module}/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    check(f"{module}: the manifest's icon is @mipmap/{name}", f'android:icon="@mipmap/{name}"' in manifest)
    ad = res / f"mipmap-anydpi-v26/{name}.xml"
    check(f"{module}: an adaptive icon with a background, a foreground and a monochrome layer", ad.exists() and all(
        f'android:drawable="@drawable/{name}_{k}"' in ad.read_text() for k in ("bg", "fg", "mono")))
    for k in ("bg", "fg", "mono"):
        f = res / f"drawable-v26/{name}_{k}.xml"
        ok = f.exists()
        if ok:
            minidom.parse(str(f))
            t = f.read_text()
            ok = 'android:viewportWidth="108"' in t and ("<gradient" not in t or k != "mono") and "make.js" in t
        check(f"{module}: {name}_{k} is a 108-unit vector in drawable-v26 (the monochrome has no gradient)", ok)
    grads = [p for p in res.glob("drawable/*.xml") if "<gradient" in p.read_text() and p.name.startswith(name)]
    check(f"{module}: no gradient icon layer outside drawable-v26 (the minSdk-21 apps would read it)", not grads, grads)
    for d, px in (("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        p = res / f"mipmap-{d}/{name}.png"
        im = Image.open(p) if p.exists() else None
        check(f"{module}: the flattened {d} icon is {px}×{px} with rounded corners", im is not None and im.size == (px, px)
              and im.mode == "RGBA" and im.getpixel((0, 0))[3] == 0 and im.getpixel((px // 2, px // 2))[3] == 255)
gen = (ROOT / ".github/tools/icons/make.js").read_text(encoding="utf-8")
check("the three apps of the family are the generator's, and the home is not (it waits for the owner's pick of its banner)",
      all(f"module: '{m}', name: '{n}'" in gen for m, n in APPS) and "module: 'launcher'" not in gen)
ban = (ROOT / ".github/tools/icons/banners.js").read_text(encoding="utf-8")
check("the banners: drawn only in Thmanyah (it stops without the private fonts, never a stand-in), the rescue first, then the other four",
      "ThmanyahDisplay-Bold.woff2" in ban and "process.exit(2)" in ban and ban.index("app: 'rescue'") < ban.index("app: 'saver'") < ban.index("app: 'extras'")
      and "app: 'doctor'" in ban and "app: 'player'" in ban and "app: 'home'" not in ban)
check("the banners are 320×180 and 1.5× and 2× of it, at drawable-xhdpi, -xxhdpi and -xxxhdpi", "[['xhdpi', 320], ['xxhdpi', 480], ['xxxhdpi', 640]]" in ban)
raise SystemExit(1 if failed else 0)
