#!/usr/bin/env python3
"""Build the «تلاوة» catalog from mp3quran.net and alquran.cloud.

The generated JSON is committed so the app builds offline; run this again when a reciter or a
station changes. `--check` only verifies that every URL still answers with audio; streams do move,
so run it by hand now and then.

    python3 .github/tools/fetch_quran.py
    python3 .github/tools/fetch_quran.py --check
"""
import argparse
import json
import sys
import unicodedata
import urllib.error
import urllib.request
from pathlib import Path

API = "https://www.mp3quran.net/api/v3/"
SURAHS_API = "https://api.alquran.cloud/v1/surah"
OUT = Path(__file__).resolve().parent.parent.parent / "android/launcher/src/main/assets/quran"

# Arabic name exactly as mp3quran spells it, and the short id the app and optimize/music.sh use.
RECITERS = [
    ("عبدالرحمن السديس", "sudais"),
    ("سعود الشريم", "shuraim"),
    ("ماهر المعيقلي", "muaiqly"),
    ("مشاري العفاسي", "afasy"),
    ("سعد الغامدي", "ghamdi"),
    ("ياسر الدوسري", "dosari"),
    ("عبدالباسط عبدالصمد", "basit"),
    ("محمد صديق المنشاوي", "minshawi"),
    ("أحمد بن علي العجمي", "ajmi"),
    ("علي بن عبدالرحمن الحذيفي", "hudhaifi"),
    ("محمود خليل الحصري", "husary"),
    ("عبدالله المطرود", "matrood"),
    ("عبدالله بصفر", "basfar"),
    ("بندر بليله", "baleela"),
    ("صلاح بو خاطر", "bukhatir"),
    ("علي جابر", "alijaber"),
    ("فارس عباد", "farsabbad"),
    ("عمر الدريويز", "duraiwiz"),
    ("هاني الرفاعي", "hanirifai"),
    ("عبدالودود حنيف", "hinaify"),
    ("علي حجاج السويسي", "aligaga"),
    ("فؤاد الخامري", "khamiri"),
    ("عبدالبديع غيلان", "ghailan"),
    ("عبدالكريم الحازمي", "hazmi"),
    ("خالد القحطاني", "qahtani"),
    ("خالد الجليل", "jaleel"),
    ("أحمد ديبان", "deeban"),
    ("أنس العمادي", "emadi"),
    ("محمد جبريل", "jibreel"),
    ("محمد الفقيه", "faqih"),
    ("زكي داغستاني", "daghistani"),
    ("محمود عبدالحكم", "abdulhakim"),
    ("عبدالرحمن العوسي", "oosi"),
    ("عبدالله الكندري", "kandari"),
    ("بدر التركي", "turki"),
    ("محمد أيوب", "ayyub"),   # field reports (Nov 5); mp3quran's «حفص عن عاصم - مرتل», 114 surahs   # 4.1 reports; mp3quran's «حفص عن عاصم - مرتل», 114 surahs
    ("عبدالعزيز الزهراني", "zahrani"),
    ("صالح الصاهود", "sahood"),
    ("مفتاح السلطني", "saltani"),
    ("يوسف الشويعي", "shouei"),
    ("ياسر سلامة", "salama"),
    ("وديع اليمني", "yamani"),
]

# Continuous streams, all https. The Saudi Quran Radio is the Saudi Broadcasting Authority's own HLS on its CDN (KWIKmotion, the
# player of radioplus.sba.sa): https end to end, no redirect, no token. Its radiojar address (an https -> http redirect with a one-use
# token) never played on the owner's TV (step 182); radiojar.com keeps its cleartext permission in case it is used again.
STATIONS = [
    ("إذاعة القرآن الكريم - السعودية", "https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8"),
    # 4.3.1: Egypt's Quran Radio (Maspero's, the address its own player and surahquran.com use) right after the Saudi one; a saved station
    # number from before moves with it (tilawah Prefs: «station_order» 2). radiojar answers https with a 302 to http nNN.radiojar.com and a
    # one-use token: StreamUrl hands that address over unasked and ExoPlayer plays it (4.3; MediaPlayer gave nothing, step 32/182); cleartext is
    # allowed for radiojar.com only (network_security_config.xml)
    ("إذاعة القرآن الكريم - مصر", "https://stream.radiojar.com/8s5u5tpdtwzuv"),
    ("الشيخ أبو بكر الشاطري", "radio/shaik_abu_bakr_al_shatri"),
    ("عبدالرحمن السديس", "radio/abdulrahman_alsudaes"),
    ("سعود الشريم", "radio/saud_alshuraim"),
    ("ماهر المعيقلي", "radio/maher"),
    ("مشاري العفاسي", "radio/mishary_alafasi"),
    ("سعد الغامدي", "radio/saad_alghamdi"),
    ("ياسر الدوسري", "radio/yasser_aldosari"),
    ("عبدالباسط عبدالصمد", "radio/abdulbasit_abdulsamad"),
    ("محمد صديق المنشاوي", "radio/mohammed_siddiq_alminshawi"),
    ("أحمد العجمي", "radio/ahmad_alajmy"),
]


# English names of the stations the committed stations.json does not have yet
EN_STATIONS = {"إذاعة القرآن الكريم - مصر": "Quran Radio, Egypt"}


def get_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": "urtv-listening"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def audio_ok(url):
    """A GET of the first kilobyte: 200/206 and an audio content type."""
    request = urllib.request.Request(url, headers={"User-Agent": "urtv-listening", "Range": "bytes=0-1023"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            kind = response.headers.get("Content-Type", "")
            return response.status in (200, 206) and (kind.startswith("audio/") or "octet-stream" in kind), kind
    except (urllib.error.URLError, TimeoutError) as error:
        return False, str(error)


def plain_name(name):
    """«سُورَةُ ٱلۡكَهۡفِ» -> «الكهف»: drop the prefix and the vowel marks."""
    text = unicodedata.normalize("NFC", name).replace("ٱ", "ا").replace("ـ", "")   # ٱ -> ا, no tatweel
    text = "".join(ch for ch in text if unicodedata.category(ch) != "Mn")
    return text.removeprefix("سورة ").strip()


def build():
    reciters_api = get_json(API + "reciters?language=ar")["reciters"]
    radios_api = get_json(API + "radios?language=ar")["radios"]
    surahs_api = get_json(SURAHS_API)["data"]
    if len(surahs_api) != 114:
        raise SystemExit(f"alquran.cloud returned {len(surahs_api)} surahs, expected 114")

    reciters = []
    for name, short in RECITERS:
        found = next((r for r in reciters_api if r["name"] == name), None)
        if not found:
            raise SystemExit(f"reciter not on mp3quran: {name}")
        moshaf = next((m for m in found["moshaf"]
                       if "حفص" in m["name"] and "مرتل" in m["name"] and m["surah_total"] == 114), None)
        if not moshaf:
            raise SystemExit(f"no Hafs murattal with 114 surahs: {name}")
        reciters.append({"id": short, "name": name, "server": moshaf["server"], "surahs": 114})

    stations = []
    kept = {s["name"]: s for s in json.loads((OUT / "stations.json").read_text("utf-8"))["stations"]} if (OUT / "stations.json").is_file() else {}
    for name, url in STATIONS:
        if not url.startswith("https://"):
            found = next((r for r in radios_api if r["url"].rstrip("/").endswith("/" + url)), None)
            if not found:
                raise SystemExit(f"station not on mp3quran: {name} ({url})")
            url = found["url"]
        row = {"name": name, "en": kept.get(name, {}).get("en", EN_STATIONS.get(name, name)), "url": url, "live": True}
        alt = kept.get(name, {}).get("alt")
        if alt:
            row["alt"] = alt   # the second source (4.3), written by hand in stations.json, kept
        stations.append(row)

    surahs = [{"n": s["number"], "name": plain_name(s["name"])} for s in surahs_api]
    for s in surahs:
        if not s["name"]:
            raise SystemExit(f"empty surah name at {s['n']}")
    return {"reciters": reciters}, {"stations": stations}, {"surahs": surahs}


def check(catalog):
    bad = 0
    reciters, stations, _ = catalog
    for reciter in reciters["reciters"]:
        url = reciter["server"] + "001.mp3"
        ok, detail = audio_ok(url)
        print(("ok   " if ok else "FAIL ") + reciter["name"] + "  " + detail)
        bad += not ok
    for station in stations["stations"]:
        ok, detail = audio_ok(station["url"])
        print(("ok   " if ok else "FAIL ") + station["name"] + "  " + detail)
        bad += not ok
    return bad


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="only verify the committed catalog's URLs")
    args = parser.parse_args()

    if args.check:
        catalog = tuple(json.loads((OUT / f"{name}.json").read_text("utf-8"))
                        for name in ("reciters", "stations", "surahs"))
        bad = check(catalog)
        print(f"\n{bad} dead URL(s)" if bad else "\nall URLs answer with audio")
        return 1 if bad else 0

    catalog = build()
    OUT.mkdir(parents=True, exist_ok=True)
    for name, data in zip(("reciters", "stations", "surahs"), catalog):
        (OUT / f"{name}.json").write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", "utf-8")
        print(f"wrote {name}.json")
    bad = check(catalog)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
