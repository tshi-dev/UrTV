#!/usr/bin/env python3
"""Fetch the text of the "verse of the day" list -> verses/verses.json. Per verse:

  text     api.alquran.cloud, edition quran-simple (Tanzil): plain script, kept for checks and as the fallback
  uthmani  the KFGQPC / QPC Hafs text (`text_qpc_hafs` of the Quran.com API v4, Tarteel's QUL script), the text the font
           assets/fonts/UthmanicHafs-v30.woff2 is made for; verify_uthmani.py compares it with the source in CI.
           Never show it in another font.
  tafsir   «التفسير الميسر» (King Fahd Complex), api.alquran.cloud edition ar.muyassar; the screen shows it
           only with #tafsir=1, whole (its terms of publication allow no cut, no addition): src/build.py writes it to
           dist/tafsir.json, read by the page only when it is on

Runs in CI (.github/workflows/verses.yml): api.alquran.cloud is unreachable from the dev container. The screen never
holds Quran text typed by hand; it only shows what this script fetched, copied to assets/shared/verses.json.

  python3 fetch_verses.py --uthmani   add `uthmani` (QPC Hafs) to assets/shared/verses.json and to the Quranic
                                      entries of assets/shared/adhkar.json, in place
"""
import json
import sys
import time
import urllib.request
from pathlib import Path

import verify_uthmani as vu

# surah:ayah. Short verses of remembrance, patience, gratitude and hope that fit the card.
REFS = [
    "13:28", "94:6", "2:152", "3:139", "14:7", "2:153", "16:128", "2:45", "29:69", "55:13",
    "50:16", "3:8", "2:201", "20:25", "93:5", "33:41", "4:28", "18:46", "39:53", "40:60",
    "2:186", "65:3", "8:46", "25:74", "7:56", "103:3", "94:8", "17:80", "21:89", "28:24",
    "1:2", "1:5", "1:6", "2:110", "2:115", "2:156", "2:163", "2:269", "3:31", "3:92",
    "3:134", "3:173", "3:185", "3:190", "3:200", "4:86", "4:110", "6:162", "7:23", "7:199",
    "9:51", "9:129", "10:62", "11:6", "11:114", "11:115", "12:86", "12:87", "14:40", "14:41",
    "15:49", "15:98", "16:18", "16:90", "16:97", "17:24", "17:82", "18:10", "20:46", "20:114",
    "20:132", "21:83", "21:87", "23:118", "25:58", "25:63", "26:80", "26:83", "27:62", "30:21",
    "31:17", "33:3", "33:56", "33:70", "35:34", "39:10", "39:36", "40:44", "40:55", "41:34",
    "42:19", "42:43", "47:7", "49:10", "51:56", "52:48", "53:39", "54:17", "55:27", "55:60",
    "59:22", "64:11", "67:2", "71:10", "73:8", "76:9", "87:14", "87:17", "89:27", "89:28",
    "93:3", "93:11", "94:5", "96:1", "99:7", "108:1", "110:3", "112:1", "2:2", "2:21",
    "2:112", "2:127", "2:148", "2:155", "2:183", "2:238", "2:245", "2:263", "2:274", "2:277",
    "2:280", "3:9", "3:16", "3:26", "3:38", "3:53", "3:102", "3:104", "3:133", "3:135",
    "3:147", "3:160", "3:194", "4:31", "4:40", "4:147", "5:35", "6:17", "6:32", "6:103",
    "6:160", "7:31", "7:55", "7:180", "7:201", "7:204", "8:2", "8:29", "9:105", "9:119",
    "9:128", "10:9", "10:44", "10:57", "10:58", "11:90", "12:53", "12:92", "13:29", "13:39",
    "14:24", "14:34", "14:52", "15:9", "15:99", "16:53", "16:96", "16:114", "16:125", "16:127",
    "17:9", "17:19", "17:36", "17:37", "17:53", "17:70", "17:84", "18:30", "18:107", "19:96",
    "20:2", "20:14", "20:82", "21:35", "21:107", "22:77", "23:1", "23:2", "23:3", "23:115",
    "24:52", "25:72", "25:75", "26:78", "26:79", "26:82", "26:88", "26:217", "28:56", "28:60",
    "28:83", "29:2", "29:6", "29:57", "29:64", "30:60", "31:13", "31:18", "31:19", "31:22",
    "33:21", "35:5", "35:15", "36:58", "36:82", "37:180", "37:181", "37:182", "39:18", "39:54",
    "40:39", "40:65", "41:30", "41:33", "41:46", "42:11", "42:25", "42:30", "42:36", "42:40",
    "43:67", "45:15", "46:13", "47:19", "49:13", "50:18", "50:31", "51:17", "51:18", "51:50",
    "51:58", "55:26", "57:3", "59:18", "59:19", "59:24", "61:2", "61:3", "62:10", "63:9",
    "64:15", "64:16", "67:1", "67:13", "67:14", "67:15", "68:4", "73:9", "76:8", "78:9",
    "87:1", "87:16", "88:17", "91:9", "91:10", "93:4", "94:1", "94:7", "95:4", "96:3",
    "97:3", "99:8", "102:1", "102:8", "103:2", "106:3", "112:2", "112:3", "112:4", "113:1",
    "114:1",
    "2:128", "2:195", "2:208", "2:214", "2:218", "2:261", "2:262", "2:268", "2:271", "2:276",
    "2:281", "2:284", "2:286", "3:18", "3:29", "3:76", "3:85", "3:103", "3:110", "3:132",
    "3:142", "3:145", "3:159", "3:164", "3:189", "3:193", "4:1", "4:27", "4:32", "4:36",
    "4:58", "4:69", "4:114", "4:124", "4:135", "5:8", "5:9", "6:54", "6:59", "6:82",
    "6:102", "6:120", "7:96", "7:128", "7:151", "7:205", "9:71", "9:72", "10:25", "10:26",
    "10:107", "11:52", "11:56", "11:123", "12:101", "12:108", "13:11", "13:26", "14:27", "14:31",
    "16:78", "16:91", "16:98", "17:23", "17:25", "17:26", "17:29", "17:34", "17:35", "17:44",
    "17:79", "17:111", "18:28", "18:109", "18:110", "19:65", "19:76", "20:8", "20:124", "20:130",
    "20:131", "21:47", "23:96", "23:97", "24:22", "24:35", "25:71", "26:84", "26:85", "26:87",
    "27:59", "28:70", "28:77", "28:88", "29:45", "29:58", "29:60", "30:17", "30:22", "30:30",
    "31:14", "31:33", "31:34", "33:35", "33:43", "34:39", "35:2", "35:10", "35:29", "36:83",
    "38:29", "39:9", "39:42", "40:19", "40:51", "41:8", "42:28", "46:15", "49:6", "49:11",
    "49:12", "51:21", "51:22", "52:49", "54:49", "57:2", "57:16", "57:21", "57:22", "58:11",
    "59:10", "59:23", "62:9", "63:10", "64:13", "64:17", "65:7", "66:6", "66:8", "67:12",
    "73:10", "74:7", "76:25", "82:6", "85:14",
    "2:25", "2:136", "2:164", "2:185", "2:265", "2:285", "4:17", "4:79", "4:123", "6:1",
    "6:18", "6:95", "6:115", "6:153", "7:10", "7:54", "7:57", "7:200", "8:24", "9:18",
    "10:5", "10:6", "10:12", "10:65", "12:111", "13:2", "16:10", "16:12", "16:79", "17:12",
    "17:85", "18:45", "18:49", "20:6", "20:7", "20:112", "21:33", "22:32", "22:65", "23:51",
    "24:27", "24:30", "25:65", "25:67", "27:88", "28:73", "28:84", "29:8", "29:62", "30:19",
    "30:20", "30:23", "30:24", "31:16", "35:41", "36:40", "39:62", "42:12", "42:20", "51:47",
    "55:9", "57:9", "60:8", "67:19",
]
EDITION = "quran-simple"   # standard Arabic script with diacritics; renders in any Arabic font
TAFSIR_EDITION = "ar.muyassar"   # التفسير الميسر, King Fahd Glorious Quran Printing Complex
ROOT = Path(__file__).resolve().parents[2]


def get(url):
    for wait in (0, 2, 5, 10):
        time.sleep(wait)
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "urtv"})
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)
        except Exception as e:  # noqa: BLE001
            err = e
    raise err


class Uthmani:
    """The QPC Hafs text by verse key, from the source verify_uthmani.py checks against. A range 112:1-4 joins its
    ayahs with the ayah-end sign and number (U+06DD and digits), the form the font draws as the ornament."""

    def __init__(self):
        probes = []
        name, self.src = vu.fetch_source(probes)
        if not self.src:
            raise SystemExit("no QPC Hafs source reachable: " + json.dumps(probes, ensure_ascii=False)[:500])

    def ref(self, ref):
        return vu.expected(ref, self.src)


def add_uthmani():
    u = Uthmani()
    for name, key in (("verses", "ref"), ("adhkar", "quran")):
        path = ROOT / "assets/shared" / f"{name}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for v in data:
            if v.get(key):
                v["uthmani"] = u.ref(v[key])
        path.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
        print(path.relative_to(ROOT), sum("uthmani" in v for v in data), "with uthmani")


def main():
    u = Uthmani()
    out = []
    for ref in REFS:
        d = get(f"https://api.alquran.cloud/v1/ayah/{ref}/{EDITION}")["data"]
        tafsir = get(f"https://api.alquran.cloud/v1/ayah/{ref}/{TAFSIR_EDITION}")["data"]["text"]
        out.append({"ref": ref, "surah": d["surah"]["name"], "ayah": d["numberInSurah"], "text": d["text"],
                    "uthmani": u.ref(ref), "tafsir": " ".join(tafsir.split())})
        print(ref, d["surah"]["name"], len(d["text"].split()), "words")
    Path("verses").mkdir(exist_ok=True)
    Path("verses/verses.json").write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding="utf-8")


if __name__ == "__main__":
    add_uthmani() if "--uthmani" in sys.argv else main()
