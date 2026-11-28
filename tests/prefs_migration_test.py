#!/usr/bin/env python3
"""R-411: some preferences reset after the update to 4.4. Every SharedPreferences key 4.3.6 wrote (tests/prefs_43_keys.tsv, read from the tag
v4.3.517) must still be read by 4.4 under the same name, with the type 4.3 wrote (a getInt on a String is a ClassCastException: the default), and
4.4 must never remove one, unless it is on KEPT_OUT below with its reason. The one key renamed in 4.4 (the update notice's «newest»/«channel», now
per channel) is read again as before until 4.4's own first read (UpdateNotice.newest).
Run: python3 tests/prefs_migration_test.py   (--make DIR: write the list again from a 4.3.6 android/ directory)"""
import pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
LIST = ROOT / "tests/prefs_43_keys.tsv"
PUT = re.compile(r'\.put(String|Boolean|Int|Long|Float|StringSet)\(\s*([A-Za-z_][A-Za-z0-9_.]*|"[^"]+")\s*,')
CONST = re.compile(r'\b([A-Z][A-Z0-9_]*)\s*=\s*"([^"]+)"')
# (app, key): why 4.4 no longer reads it. Empty: every 4.3.6 key is read.
KEPT_OUT = {}
# the removes 4.3.6 already had for its own keys (a person's reset, a cleared list, a counter): 4.4 may keep them, never add one
REMOVES_43 = {("app", "homeMine"): 1, ("app", "tasks"): 1, ("launcher", "apps_order"): 1, ("launcher", "crash"): 1, ("launcher", "dock"): 1,
              ("launcher", "end"): 3, ("launcher", "off"): 1, ("launcher", "rows"): 1, ("launcher", "since"): 3, ("launcher", "sound_t"): 1,
              ("launcher", "until"): 1}

def written(root):
    out = {}
    for f in pathlib.Path(root).rglob("*.java"):
        app = str(f.relative_to(root)).split("/")[0]
        t = f.read_text(encoding="utf-8", errors="ignore")
        consts = dict(CONST.findall(t))
        for m in PUT.finditer(t):
            line = t[t.rfind("\n", 0, m.start()) + 1:t.find("\n", m.end())]
            if not re.search(r'edit\(\)|\be\.put|editor\.|ed\.put|Editor', line): continue   # an editor's put, never a Bundle's or an Intent's
            k = m.group(2)
            k = k[1:-1] if k.startswith('"') else consts.get(k.split(".")[-1])
            if k: out.setdefault((app, k), set()).add(m.group(1))
    return out

if len(sys.argv) == 3 and sys.argv[1] == "--make":
    rows = written(sys.argv[2])
    head = [l for l in LIST.read_text(encoding="utf-8").splitlines() if l.startswith("#")] if LIST.exists() else []
    LIST.write_text("\n".join(head + [f"{a}\t{k}\t{','.join(sorted(t))}" for (a, k), t in sorted(rows.items())]) + "\n", encoding="utf-8")
    sys.exit(0)

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def code(app):   # the app's 4.4 Java, comments out
    t = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in (ROOT / "android" / app / "src/main/java").rglob("*.java"))
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    return re.sub(r"(?m)//[^\n\"]*$", "", t)

rows = [l.split("\t") for l in LIST.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
check("the 4.3.6 list is there (both apps)", len(rows) > 150 and {r[0] for r in rows} == {"app", "launcher"}, len(rows))
src = {a: code(a) for a in ("app", "launcher")}
lost, typed, removed = [], [], []
for app, key, types in rows:
    if (app, key) in KEPT_OUT: continue
    t = src[app]
    if '"' + key + '"' not in t: lost.append(f"{app}:{key}"); continue
    gets = set(re.findall(r'\.get(String|Boolean|Int|Long|Float|StringSet)\(\s*"' + re.escape(key) + '"', t))
    if gets and not gets & set(types.split(",")): typed.append(f"{app}:{key} 4.3 {types} 4.4 {sorted(gets)}")
    n = len(re.findall(r'\.remove\(\s*"' + re.escape(key) + r'"\s*\)', t))
    if n > REMOVES_43.get((app, key), 0): removed.append(f"{app}:{key} ({n} removes, 4.3.6 had {REMOVES_43.get((app, key), 0)})")
check("every key 4.3.6 wrote is still named in 4.4 (nothing renamed away)", not lost, lost)
check("4.4 reads each with the type 4.3.6 wrote", not typed, typed)
check("4.4 adds no remove of a key 4.3.6 wrote (an update must not drop a setting)", not removed, removed)
un = (ROOT / "android/launcher/src/main/java/sa/urtv/home/UpdateNotice.java").read_text(encoding="utf-8")
nw = un.split("private static int newest(Context c) {")[1].split("\n    }\n")[0] if "private static int newest(Context c) {" in un else ""
check("the update notice's 4.3 read counts for its channel until 4.4's first read", 'sp.contains("newest_" + ch)' in nw
      and 'ch.equals(sp.getString("channel", "")) ? sp.getInt("newest", 0) : 0' in nw)
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
