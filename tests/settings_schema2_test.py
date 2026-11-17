#!/usr/bin/env python3
"""4.5: optimize/settings_move.py keeps writing schema 1 and also reads the TV's own «نسخة الإعدادات» file, schema 2 (docs/BACKUP.md): its "saver"
part as a schema-1 file, the "home" part left to the TV with one note. Anything newer than 2, or a schema-2 file with no "saver", is refused.
Run: python3 tests/settings_schema2_test.py"""
import importlib.util, json, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("settings_move", ROOT / "optimize/settings_move.py")
sm = importlib.util.module_from_spec(spec); spec.loader.exec_module(sm)
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def load(d):
    f = Path(tempfile.mkdtemp()) / "s.json"; f.write_text(json.dumps(d, ensure_ascii=False), encoding="utf-8")
    try: return sm.load(str(f)), None
    except sm.Refused as e: return None, str(e)

check("export still writes schema 1", sm.CURRENT_SCHEMA == 1)
one, err = load({"schema": 1, "digits": "latin", "clock24": True})
check("a schema-1 file reads as before", err is None and one[0]["digits"] == "latin", err)
two, err = load({"schema": 2, "app": "urtv", "saver": {"digits": "latin", "clock24": True}, "home": {"dock": ["com.netflix.ninja"]}})
check("a schema-2 file: its saver part reads as schema 1, the same values", err is None and two[0]["digits"] == "latin" and two[0].get("clock24") is True and two[0]["schema"] == 1, err)
check("…and one note says the home part is restored only on the TV", any("restored only on the TV" in n for n in two[1]), two and two[1])
nohome, err = load({"schema": 2, "app": "urtv", "saver": {"digits": "hindi"}})
check("a schema-2 file with no home part: no note", err is None and not any("restored only on the TV" in n for n in nohome[1]))
_, err = load({"schema": 2, "app": "urtv", "home": {}})
check("a schema-2 file with no saver part is refused", err is not None and '"saver"' in err, err)
_, err = load({"schema": 3, "saver": {}})
check("schema 3 (newer) is refused", err is not None and "schema 3" in err, err)
_, err = load({"schema": 2, "saver": {"digits": "roman"}})
check("the saver part is validated as schema 1 is (a bad value refuses the file)", err is not None, err)
sys.exit(1 if failed else 0)
