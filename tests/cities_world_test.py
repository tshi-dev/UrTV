#!/usr/bin/env python3
"""Reviewed worldwide data: reproducibility, script, capitals and source provenance."""
import importlib.util
import hashlib
import json
import re
import sys
import subprocess
import tempfile
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / '.github/tools/cities-world'
spec = importlib.util.spec_from_file_location('cities_merge', DATA / 'merge.py')
merge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(merge)
rows = json.loads((DATA / 'cities-merged.json').read_text(encoding='utf-8'))
failed = 0


def check(name, ok):
    global failed
    print(('PASS ' if ok else 'FAIL ') + name)
    failed += not ok


check('world data is exactly the reproducible merge', rows == merge.build())
check('all 3305 reviewed names and 228 countries are retained (3307 reviewed, less «إسرائيل» and Western Sahara, never listed)', len(rows) == 3305 and len({r['country'] for r in rows}) == 228)
check('city and country names contain Arabic script only', all(merge.arabic(r['name_ar']) and merge.arabic(r['country_ar']) for r in rows))
check('each country or territory has a capital/administrative centre', all(any(r['capital'] for r in rows if r['country'] == cc) for cc in {r['country'] for r in rows}))
check('world rows are ordered by country order, population and GeoNames ID', rows == sorted(rows, key=lambda r: (r['order'], -r['population'], r['geonameid'])))
check('late-added Muslim countries are in the correct order', next(r['order'] for r in rows if r['country'] == 'GN') < next(r['order'] for r in rows if r['country'] == 'FR'))
check('Iran uses MWL and no world country uses a Shia method', all(r['method'] == 'MWL' for r in rows if r['country'] == 'IR') and not any(r['method'].upper() in {'TEHRAN', 'JAFARI'} for r in rows))
by_key = {r['country'] + '|' + r['name_en']: r for r in rows}
check('wrong-city and mixed-script regressions are corrected', all(by_key[k]['name_ar'] == ar for k, ar in {'YE|Sayyān': 'سيان', 'IQ|Baḥarkah': 'بحركة', 'CN|Mudanjiang': 'مودانجيانغ', 'CN|Puyang': 'بويانغ', 'GA|Libreville': 'ليبرفيل'}.items()))
check('capital additions use their actual GeoNames records', by_key['LK|Sri Jayewardenepura Kotte']['geonameid'] == 1238992 and by_key['PW|Ngerulmud']['geonameid'] == 8063361)
check('historical/metro aliases do not create extra capitals', not any(by_key[k]['capital'] for k in ('NG|Suleja', 'IN|Delhi', 'HK|Hong Kong Island')))
review = (DATA / 'REVIEW.md').read_text(encoding='utf-8')
counts = re.findall(r'^\| ([A-Z]{2}) \| [^|]+ \| (\d+) \| (\d+) \| (\d+) \| (\d+) \|$', review, re.M)
check('REVIEW reports checked/fixed/added counts for every country', len([c for c in counts if c[0] not in ('IL', 'EH')]) == 228 and sum(int(c[1]) for c in counts if c[0] not in ('IL', 'EH')) == len(rows) and sum(int(c[2]) for c in counts) == 119 and sum(int(c[3]) for c in counts) == 2)
src = ROOT / 'android/app/src/main/java/sa/urtv/screensaver'
java = (src / 'Cities.java').read_text(encoding='utf-8')
original = re.findall(r'\{"[^"]+", "[-\d.]+", "[-\d.]+"(?:, "[^"]+")?\}', java)
check('all 374 original city rows are byte-for-byte unchanged', len(original) == 374 and hashlib.sha256('\n'.join(original).encode()).hexdigest() == 'c48a06f40efe53347b707158686f75679534067a45d0ac70799cfee2a46ccd89')
with tempfile.TemporaryDirectory() as temp:
    temp = Path(temp)
    subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(temp), *map(str, [src / 'Cities.java', src / 'CityMatch.java', src / 'TeamSearch.java', src / 'Location.java', src / 'PrayerMethod.java', ROOT / 'tests/WorldCitiesRun.java'])], check=True)
    result = subprocess.run(['java', '-Dfile.encoding=UTF-8', '-cp', str(temp), 'sa.urtv.screensaver.WorldCitiesRun'], capture_output=True, check=True).stdout.decode('utf-8').splitlines()
    decoded = [line.split('\t') for line in result if line.startswith('CITY\t')]
    world = [r for r in decoded if r[6]]
    actual = [(r[1], r[2], float(r[3]), float(r[4]), r[5], r[6], int(r[7])) for r in world]
    ids = {'MWL': 3, 'ISNA': 2, 'KARACHI': 1, 'KEMENAG': 20, 'JAKIM': 17, 'SINGAPORE': 11, 'DIYANET': 13, 'FRANCE_UOIF': 12, 'RUSSIA': 14, 'MOONSIGHTING': 15, 'EGYPT': 5, 'MOROCCO': 21}
    expected = [(r['country'], r['name_ar'], r['lat'], r['lon'], r['tz'], r['name_en'], ids[r['method']]) for r in rows]
    check('all 3305 packed app rows decode exactly to the reviewed data', actual == expected)
    check('all 6610 Arabic/English world names find their city within its country', 'MATCHED\t6610' in result)
    check('world picker labels show both complete names', all(r[8] == r[2] + ' · ' + r[6] for r in world))
    countries = [line.split('\t') for line in result if line.startswith('COUNTRY\t')]
    check('242 app countries resolve a numeric method and Arabic authority', len(countries) == 242 and all(r[4] == '&pm=' + r[3] and merge.arabic(r[5]) for r in countries))
    # Regenerate in a temporary tree: the test never edits the checked-out file.
    generated = temp / 'generated'
    tools_dir = generated / '.github/tools'
    data_dir = tools_dir / 'cities-world'
    data_dir.mkdir(parents=True)
    (generated / 'android/app/src/main/java/sa/urtv/screensaver').mkdir(parents=True)
    shutil.copyfile(ROOT / '.github/tools/gen_cities.py', tools_dir / 'gen_cities.py')
    for filename in ('Cities-support.java.txt', 'cities-merged.json'):
        shutil.copyfile(DATA / filename, data_dir / filename)
    import geonamescache
    geodata = Path(geonamescache.__file__).parent / 'data/cities500.json'
    subprocess.run([sys.executable, str(tools_dir / 'gen_cities.py'), str(geodata)], check=True, capture_output=True)
    check('Cities.java exactly matches its generator', (generated / 'android/app/src/main/java/sa/urtv/screensaver/Cities.java').read_text(encoding='utf-8') == java)
# the owner's rule: the app never lists «إسرائيل» (Palestine has its own checked list) or Western Sahara as a country (its cities are Morocco's)
_merged = json.loads((ROOT / ".github/tools/cities-world/cities-merged.json").read_text(encoding="utf-8"))
_bad = sorted({r["country"] for r in _merged if r["country"] in ("IL", "EH")})
_app = "".join((ROOT / f).read_text(encoding="utf-8") for f in ("android/app/src/main/java/sa/urtv/screensaver/Cities.java", "docs/CITIES.md", "dist/tv.html", "src/template.html"))
check("no «إسرائيل» and no Western Sahara as a country, in the data, the app's list, the page or CITIES.md",
      not _bad and "إسرائيل" not in _app and "الصحراء الغربية" not in _app)

sys.exit(1 if failed else 0)
