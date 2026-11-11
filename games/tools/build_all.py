"""Builds games/data/*.json from the verified builders, then prints counts. Needs network (cached). Usage:
SSL_CERT_FILE=<ca bundle> python3 -I games/tools/build_all.py"""
import sys, os, json, collections
sys.path.insert(0, os.path.dirname(__file__))
import build_geo, build_science, build_quran, build_maths, build_animals, build_langcur, hand, rows_islamic, rows_general, rows_hive

stats = {}
parts = [build_geo.build(), build_science.build(stats), build_quran.build(stats),
         hand.make(rows_islamic.ROWS + rows_general.ROWS + rows_hive.ROWS, None), build_maths.build(), build_animals.build(), build_langcur.build()]
files = collections.defaultdict(list)
for p in parts:
    for t, items in p.items(): files[t] += items
data = os.path.join(os.path.dirname(__file__), "..", "data")
for t, items in files.items():
    json.dump(items, open(os.path.join(data, t + ".json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0)
cnt = collections.Counter((i["type"], i["category"]) for t in files.values() for i in t)
stats["hand_dropped"] = [list(x) for x in hand.DROPPED]
print(json.dumps({"stats": stats, "counts": {f"{a}/{b}": n for (a, b), n in sorted(cnt.items())},
                  "total": sum(cnt.values())}, ensure_ascii=False, indent=1))
