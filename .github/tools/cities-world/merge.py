#!/usr/bin/env python3
"""Reproducible draft merge with reviewed overrides. Run from any directory.
Requirements: requirements.txt. Coordinates are exclusively GeoNames records.
"""
import ast
import csv
import json
import re
import unicodedata
from pathlib import Path
from babel import Locale
from geonamescache import GeonamesCache
HERE = Path(__file__).resolve().parent

def read(name):
    return json.loads((HERE / name).read_text(encoding='utf-8'))

def normalized(s):
    return ''.join(c for c in unicodedata.normalize('NFKD', s).casefold() if c.isalnum() and not unicodedata.combining(c))

def arabic(s):
    return bool(s.strip()) and all(c.isspace() or c in "-'’،" or ('ARABIC' in unicodedata.name(c, '') and c not in 'پچژگکیۀٹڈڑںےھڪڵەډېڀڏٽڌڙ' and unicodedata.category(c) in ('Lo', 'Lm', 'Mn')) for c in s)

def build():
    draft, checks, names = read('cities-world.json'), read('check-result.json'), read('names-result.json')
    overrides = read('overrides.json')
    fixes, additions, methods = {}, {}, {}
    for n in range(1, 7):
        proposal, verified = checks[f'check:{n}'], checks[f'verify:{n}']
        for entry in proposal['name_fixes']:
            key = entry['country'] + '|' + entry['name_en']
            if key not in verified['reject_fixes']: fixes[key] = entry['new']
        for entry in proposal['added']:
            key = entry['country'] + '|' + entry['name_en']
            if key not in verified['reject_added']: additions[key] = entry['name_ar']
        better = {m['code']: m['better'] for m in verified['reject_methods']}
        methods.update({m['code']: better.get(m['code'], m['method']) for m in proposal['methods']})
    for n in range(1, 9):
        proposal = names.get(f'name:{n}')
        if not proposal: continue
        verified = names.get(f'verify:{n}') or {'fixes': []}
        corrected = {e['country'] + '|' + e['name_en']: e['name_ar'] for e in verified['fixes']}
        for entry in proposal['names']:
            key = entry['country'] + '|' + entry['name_en']
            additions.setdefault(key, corrected.get(key, entry['name_ar']))
    cache = GeonamesCache(min_city_population=500)
    countries, cities = cache.get_countries(), cache.get_cities()
    # Keep the source merge's first GeoNames match: a homonym must never silently
    # move to another settlement when the generation code is refactored.
    by_name = {}
    for c in cities.values():
        by_name.setdefault((c['countrycode'], c['name']), c)
    # Read ordering constants without executing build.py, which writes draft files.
    constants = {}
    for node in ast.parse((HERE / 'build.py').read_text(encoding='utf-8')).body:
        if isinstance(node, ast.Assign) and isinstance(node.targets[0], ast.Name) and node.targets[0].id in ('MUSLIMS', 'ARAB_NEW'):
            constants[node.targets[0].id] = ast.literal_eval(node.value)
    result = [dict(r) for r in draft]
    seen = {(r['country'], r['name_en']) for r in result}
    def append(city, capital=False):
        cc, en = city['countrycode'], city['name']
        key = cc + '|' + en
        if (cc, en) in seen: return
        ar = overrides.get(key, additions.get(key))
        if not ar or not arabic(ar): return
        result.append(dict(country=cc, name_ar=ar.strip(), name_en=en, lat=round(city['latitude'], 4), lon=round(city['longitude'], 4), tz=city['timezone'], population=city['population'], capital=capital, geonameid=city['geonameid']))
        seen.add((cc, en))
    with (HERE / 'missing-arabic.csv').open(encoding='utf-8', newline='') as source:
        for missing in csv.DictReader(source):
            city = by_name.get((missing['country'], missing['name']))
            if city: append(city)
    # Sri Lanka's legislative capital was absent from the draft selection.
    append(cities['1238992'], capital=True)
    # Palau's capital is below geonamescache's population cutoff: raw credited GeoNames record.
    for line in (HERE / 'sources/capitals-geonames.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'): continue
        f = line.split('\t')
        assert f[7] == 'PPLC'
        append(dict(geonameid=int(f[0]), name=f[1], latitude=float(f[4]), longitude=float(f[5]), countrycode=f[8], population=int(f[14]), timezone=f[17]), capital=True)
    def order_key(cc):
        muslims = constants['MUSLIMS']
        if cc in constants['ARAB_NEW']: return (0, -muslims.get(cc, 0), cc)
        if cc in muslims: return (1, -muslims[cc], cc)
        return (2, -countries[cc]['population'], cc)
    present = {r['country'] for r in result}
    order = {cc: i + 1 for i, cc in enumerate(sorted(present, key=order_key))}
    territories = Locale.parse('ar').territories
    # Only recover countries still lacking a capital. GeoNames aliases can be
    # historical (Suleja used to be called Abuja) or name a surrounding metro.
    recovered = set()
    for cc in present:
        candidates = [r for r in result if r['country'] == cc]
        if any(r['capital'] for r in candidates): continue
        cap = normalized(countries[cc].get('capital', ''))
        exact = [r for r in candidates if cap and normalized(r['name_en']) == cap]
        aliases = [r for r in candidates if cap and any(normalized(a) == cap for a in cities.get(str(r['geonameid']), {}).get('alternatenames', []))]
        if exact or aliases:
            recovered.add(max(exact or aliases, key=lambda r: r['population'])['geonameid'])
    used = set()
    for row in result:
        cc, en = row['country'], row['name_en']
        key = cc + '|' + en
        row['name_ar'] = overrides.get(key, fixes.get(key, row['name_ar'])).strip()
        if key in overrides: used.add(key)
        row['country_ar'] = re.sub(r'\s*\(.*?\)', '', territories[cc]).strip()
        row['method'] = 'MWL' if cc == 'IR' else methods.get(cc, 'MWL')
        row['order'] = order[cc]
        row['capital'] = row['capital'] or row['geonameid'] in recovered
        if not arabic(row['name_ar']) or not arabic(row['country_ar']): raise ValueError(f'non-Arabic name: {key}')
    unused = overrides.keys() - used
    if unused: raise ValueError(f'unused overrides: {sorted(unused)}')
    for cc in present:
        if not any(r['country'] == cc and r['capital'] for r in result): raise ValueError(f'missing capital: {cc}')
    result.sort(key=lambda r: (r['order'], -r['population'], r['geonameid']))
    return result

if __name__ == '__main__':
    rows = build()
    (HERE / 'cities-merged.json').write_text(json.dumps(rows, ensure_ascii=False, indent=0) + '\n', encoding='utf-8')
    print(f"cities {len(rows)}, countries {len({r['country'] for r in rows})}, capitals {sum(r['capital'] for r in rows)}")
