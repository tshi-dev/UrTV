#!/usr/bin/env python3
"""cities-world.json + check-result.json (name fixes, added names, methods) + names-result.json (Arabic names for the rest)
-> cities-merged.json: every city with name_ar AND name_en, each country with its method. Plain code, no model."""
import json, csv, re
from geonamescache import GeonamesCache
D = json.load(open('cities-world.json'))
CHK = json.load(open('check-result.json')); NAM = json.load(open('names-result.json'))
miss = list(csv.DictReader(open('missing-arabic.csv')))
BAD = re.compile(r'[پچژگکیۀٹڈڑںےھڪڵەډېڀڏٽڌڙ]|[A-Za-z0-9()]')
key = lambda c, n: (c, n)
fix, added, methods, cnames = {}, {}, {}, {}
for n in range(1, 7):
    f, v = CHK[f'check:{n}'], CHK[f'verify:{n}']
    rf, ra = set(v['reject_fixes']), set(v['reject_added'])
    for x in f['name_fixes']:
        if f"{x['country']}|{x['name_en']}" not in rf: fix[key(x['country'], x['name_en'])] = x['new']
    for x in f['added']:
        if f"{x['country']}|{x['name_en']}" not in ra: added[key(x['country'], x['name_en'])] = x['name_ar']
    better = {m['code']: m['better'] for m in v['reject_methods']}
    for m in f['methods']: methods[m['code']] = better.get(m['code'], m['method'])
    for c in f['country_names']: cnames[c['code']] = c['country_ar']
for n in range(1, 9):
    a, v = NAM.get(f'name:{n}'), NAM.get(f'verify:{n}') or {'fixes': []}
    if not a: continue
    fx = {key(x['country'], x['name_en']): x['name_ar'] for x in v['fixes']}
    for x in a['names']: added.setdefault(key(x['country'], x['name_en']), fx.get(key(x['country'], x['name_en']), x['name_ar']))
gc = GeonamesCache(min_city_population=500)
by = {}
for c in gc.get_cities().values(): by.setdefault((c['countrycode'], c['name']), c)
caps = {c['iso']: c['capital'] for c in gc.get_countries().values()}
order = {r['country']: (r['order'], r['country_ar']) for r in D}
out, skipped = [], []
for r in D:
    r = dict(r); r['name_ar'] = fix.get(key(r['country'], r['name_en']), r['name_ar']); out.append(r)
seen = {(r['country'], r['name_en']) for r in out}
for m in miss:
    k = (m['country'], m['name'])
    if k in seen: continue
    ar = added.get(k); c = by.get(k)
    if not ar or not c or BAD.search(ar): skipped.append(m); continue
    if m['country'] not in order: order[m['country']] = (999, cnames.get(m['country'], m['country']))
    out.append({'country': m['country'], 'country_ar': cnames.get(m['country'], order[m['country']][1]), 'name_ar': ar.strip(), 'name_en': m['name'],
                'lat': round(c['latitude'], 4), 'lon': round(c['longitude'], 4), 'tz': c['timezone'], 'population': c['population'],
                'capital': m['name'] == caps.get(m['country']), 'geonameid': c['geonameid'], 'order': order[m['country']][0]})
    seen.add(k)
bad = [r for r in out if BAD.search(r['name_ar'])]
for r in out: r['method'] = methods.get(r['country'], 'MWL')
out.sort(key=lambda r: (r['order'], -r['population']))
# country names left as codes are filled from CLDR in the step after
json.dump(out, open('cities-merged.json', 'w'), ensure_ascii=False, indent=0)
print('cities', len(out), 'countries', len({r['country'] for r in out}), 'capitals', sum(r['capital'] for r in out))
print('fixes applied', len(fix), 'skipped (no name or no geonames row)', len(skipped), 'bad letters left', len(bad))
