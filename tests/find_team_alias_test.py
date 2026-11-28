#!/usr/bin/env python3
"""optimize/find-team.py: Arabic nicknames, common spellings and club abbreviations find the club (R-377).

«الزعيم», «العالمي», «مان يونايتد», «PSG», «برشا» found nothing (or another club first): only the clubs' own names were
searched. The alias table puts the named club first. Offline (TheSportsDB's search fails the test).
Run: python3 tests/find_team_alias_test.py"""
import importlib.util, io, json, sys
from contextlib import redirect_stdout
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("find_team", ROOT / "optimize/find-team.py")
ft = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ft)
searched = []
ft.search = lambda name: searched.append(name) or []
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

def first(*args):
    sys.argv = ["find-team.py", *args]
    out = io.StringIO()
    try:
        with redirect_stdout(out):
            ft.main()
    except SystemExit as e:
        out.write(str(e))
    return out.getvalue().split(":", 1)[0]

for q, want in [("الزعيم", "136013"), ("زعيم آسيا", "136013"), ("العالمي", "136022"), ("العميد", "136018"), ("الراقي", "137721"),
                ("الليث", "136020"), ("السكري", "136012"), ("الأخضر", "136137"), ("الملكي", "133738"), ("ريال", "133738"),
                ("برشا", "133739"), ("مان يونايتد", "133612"), ("Man Utd", "133612"), ("مان سيتي", "133613"), ("PSG", "133714"),
                ("بي إس جي", "133714"), ("اليوفي", "133676"), ("الإنتر", "133681"), ("البلوز", "133610"), ("آرسنال", "133604"),
                ("Barca", "133739"), ("Spurs", "133616"), ("أتلتيكو", "133729"), ("BVB", "133650")]:
    got = first(q)
    check(f"{q} -> {want} first", got == want, got)
for q, want in [("الهلال", "136013"), ("النصر", "136022"), ("Real Madrid", "133738"), ("ميلان", "133667")]:
    check(f"a club's own name is unchanged: {q} -> {want}", first(q) == want, first(q))

clubs = {c["id"]: c for c in json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))}
missing = [cid for cid in ft.ALIASES if cid not in clubs]
check("every alias names a club in assets/shared/teams.json", not missing, missing)
clash = []
for key, cid in ft.ALIAS_OF.items():   # an alias must never be another club's own name (it would hide that club)
    for c in clubs.values():
        if c["id"] != cid and any(n and ft.alias_key(n) == key for n in [c.get("en"), c.get("ar"), *(c.get("alt") or [])]):
            clash.append((key, cid, c["id"]))
check("no alias is the own name of another club", not clash, clash)
check("no online search", not searched, searched)
sys.exit(1 if bad else 0)
