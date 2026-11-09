#!/usr/bin/env python3
"""4.3 «المظهر والخلفيات»: the screensaver's and the home's look and photos on one screen, each a tile with a live picture; and the extras that set them for an agent.

    python3 tests/appearance_test.py
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


act = text(SAVER / "AppearanceActivity.java")
check("the page has the screensaver's card style, its photos (packs, «صورنا», «صوري»), the home's look and lighting and the home's photos, each a tile with its own picture (Mock)",
      all(w in act for w in ("Prefs.CARD_STYLES", "photosRow()", "GlassStyle.LOOKS", "Prefs.APPEARANCES", "homePhotosRow()", "private final class Mock extends View", 'startActivity(new Intent(this, PhotoPacksActivity.class))')))
check("the home's photos are «مثل شاشة التوقف» or its own packs and «صورنا» (setHomePhotosSame, setHomePacksOn, setHomeOursOn)", all(w in act for w in ("setHomePhotosSame(true)", "setHomePhotosSame(false)", "setHomePacksOn(", "setHomeOursOn(")))
check("«صورنا» cannot be turned off with no pack on (the same guard as the packs screen), «صوري» toggles mixed or alone", "mayTurnOursOff" in act and "setOwnMode(only ? Own.MIX : Own.ONLY)" in act)
check("every tile is focusable with the glass card look and the on mark; the look of all our screens (GlassSource.page)", "t.setFocusable(true);" in act and "GlassPaint.card(t, look, 14);" in act and "GlassSource.page(this, root)" in act)
man = text(ROOT / "android/app/src/main/AndroidManifest.xml")
check("the activity is declared, not exported; the settings' «المظهر والخلفيات» row opens it", '".AppearanceActivity" android:exported="false"' in man and "AppearanceActivity.class" in text(SAVER / "SettingsActivity.java"))
rcv = text(SAVER / "SaverReceiver.java")
check("an agent can set the home's photos (homephotos same|own, homepacks, homeours) and the hidden quick buttons (pillshidden), and `show` says them",
      all(w in rcv for w in ('getStringExtra("homephotos")', 'getStringExtra("homepacks")', 'getStringExtra("homeours")', 'getStringExtra("pillshidden")', '"\\nhomephotos: "', '"\\npillshidden: "')))
packs = text(SAVER / "PhotoPacksActivity.java")
check("step 41: the page stays 5 % inside the top and bottom edges (the last tile and «رجوع» too), the home's pack tiles carry the pack's name, not its id",
      "heightPixels * 0.05f" in act and "PackStore.names(this)" in act and "names.containsKey(id)" in act)
check("step 41: the «تلقائي» mock shows both lights (split) and a glass card has its hairline, so it is not missing on a dark ground",
      '"auto".equals(a) ? GlassStyle.of(prefs.look(), true)' in act and "c.clipRect(w / 2, 0, w, h)" in act and "paint.setColor(l.line);" in act)
check("step 41: a pack that failed to download says «تعذّر التنزيل، حاول مرة ثانية» on its tile and stays a button that tries again",
      "failed.add(p.id)" in packs and '"failed".equals(state)' in packs and "تعذّر التنزيل، حاول مرة ثانية" in text(SAVER / "Words.java"))
sys.exit(1 if failed else 0)
