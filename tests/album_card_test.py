#!/usr/bin/env python3
"""«ألبوم صوري» (C17): the card of the owner's own photos: on the page, on the screensaver's settings and on the home.

    python3 tests/album_card_test.py
"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


page = text(ROOT / "src/template.html")
prefs, rcv, cache, shrink = (text(SAVER / n) for n in ("Prefs.java", "SaverReceiver.java", "PhotoCache.java", "PhotoShrink.java"))
card, prov, ownj = text(HOME / "HomeCard.java"), text(SAVER / "ConfigProvider.java"), text(SAVER / "Own.java")

check("page: album is a card kind with a title and a renderer", "'album']" in page and "album:'ألبوم صوري'" in page and "albumCard(card)" in page)
check("page: the speed is 4 s, 10 s or a minute, the date is on by default",
      "_Q.get('albummode') === 'minute' ? 60000 : _Q.get('albummode') === 'ten' ? 10000 : 4000" in page and "_Q.get('albumdate') !== '0'" in page)
check("page (4.3.1): fill (cover, the top third kept), tall (about twice the height, the card's zoom steps down where it does not fit), no title above the photo: each reads its own #albumfit=fill, #albumsize=tall, #albumtitle=0 and changes nothing else",
      "albumFill: _Q.get('albumfit') === 'fill', albumTall: _Q.get('albumsize') === 'tall', albumTitle: _Q.get('albumtitle') !== '0'" in page
      and "li.alb.fill img{object-fit:cover;object-position:50% 33%}" in page and "li.alb.tall .af{height:calc(46vh*var(--z,1))}" in page and ".week.notitle .hd{display:none}" in page
      and "card.el.classList.toggle('notitle', !CONFIG.albumTitle)" in page and "${CONFIG.albumFill ? ' fill' : ''}${CONFIG.albumTall ? ' tall' : ''}" in page
      and re.search(r"li\.alb img\{[^}]*object-fit:contain", page) is not None)   # the default is still fitted
check("page: fitted, never cropped, rounded", re.search(r"li\.alb img\{[^}]*object-fit:contain", page) is not None and "border-radius" in page.split(".af{", 1)[1][:200])
check("page: an empty «صوري» gives the one-line hint, in the page's English table",
      "ALBUM_HINT = 'أضف صورك من صفحة الجوال ← صوري'" in page and "أضف صورك من صفحة الجوال ← صوري\t" in text(ROOT / "i18n/en/page.tsv"))
check("page: a weak TV holds one photo (no second image)", "CONFIG.lowram || " in page.split("function albumNext", 1)[1][:600])
check("page: photos are asked at the card's size", "'.jpg?s=900'" in page)
check("saver: the card is in PANELS with its line, and the settings are sent only when not the default (so the page's address is as before)",
      '{"album", "ألبوم صوري"}' in prefs and '{"album", "صورك' in prefs and '"&albummode=" + m' in prefs and '"&albumdate=0"' in prefs and '"&albumtitle=0"' in prefs
      and '"&albumfit=fill"' in prefs and '"&albumsize=tall"' in prefs and '"seconds".equals(m) ? ""' in prefs)
check("saver: albummode (seconds, ten, minute), albumdate, albumtitle, albumfit and albumsize are validated, set and shown; the older yes/no minute setting is read as it was",
      all(k in rcv for k in ('"albummode"', '"albumdate"', '"albumtitle"', '"albumfit"', '"albumsize"', "setAlbumMode", "setAlbumTitle", "setAlbumFit", "setAlbumSize", "albummode: ", "albumtitle: ", "albumfit: ", "albumsize: "))
      and 'sp.getBoolean("albumMinute", false) ? "minute" : "seconds"' in prefs)
settings = text(SAVER / "SettingsActivity.java")
check("settings: a row for each (speed, date, title, shape, size) in the screensaver's settings, mapped to its category, each name and value in the English table",
      all(f'row("{n}"' in settings for n in ("سرعة ألبوم صوري", "تاريخ الصورة في ألبوم صوري", "عنوان ألبوم صوري", "شكل صورة ألبوم صوري", "حجم ألبوم صوري"))
      and all(f'{{"{n}", SAVER}}' in text(SAVER / "SettingsHub.java") for n in ("سرعة ألبوم صوري", "عنوان ألبوم صوري", "شكل صورة ألبوم صوري", "حجم ألبوم صوري"))
      and all(f"{n}\t" in text(ROOT / "i18n/en/saver.tsv") for n in ("سرعة ألبوم صوري", "عنوان ألبوم صوري", "شكل صورة ألبوم صوري", "حجم ألبوم صوري", "كل ١٠ ثوانٍ", "طويل بضعف الارتفاع")))
check("settings: no «·» or «•» in the new rows' words", all(c not in s_ for s_ in ("ظاهر تحت الصورة", "ظاهر فوق الصورة", "تملأ البطاقة وأعلاها يبقى", "طويل بضعف الارتفاع", "كل ١٠ ثوانٍ") for c in "·•"))
check("saver: PhotoCache answers photos 1..50, a size, and dates.json; the page cap is not stale",
      "[1-4][0-9]|50" in cache and "dates.json" in cache and "PhotoShrink.toSize" in cache and "([1-9]|10)" not in cache)
check("saver: the provider gives the dates and Own knows the day a photo was added", '"dates"' in prov and "static String date(" in ownj)
check("home: the card answers the album's photos and dates from the screensaver app, read-only, size-limited",
      "own.urtv.invalid" in card and "config/own" in card and "PhotoShrink.toSize" in card and "[1-4][0-9]|50" in card and "[0-9-]{10}" in card)
check("both PhotoShrink copies are the same code", shrink.replace("package sa.urtv.screensaver", "") == text(HOME / "PhotoShrink.java").replace("package sa.urtv.home", ""))
sys.exit(1 if failed else 0)
