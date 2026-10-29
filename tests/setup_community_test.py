"""The community lists (CleanYourTV, MIT) in the app: CommunityMatch.java against optimize/community.py on EVERY list header (the same list, or
none for an ambiguous match), the generated asset (small, and what the lists say), and SetupCommunity next to optimize/community.sh on one fake
TV (tests/fixtures/setup-faketv.sh): the same filters (keep, core, the HOME answerers, launchers, already off), the ask list left on, the same
packages turned off and recorded in community-applied.txt; the answers and the undo. No TV needed.

    python3 tests/setup_community_test.py
"""
import importlib.util, json, os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

ASSET = ROOT / "android/launcher/src/main/assets/setup/community.txt"
gen = subprocess.run([sys.executable, str(ROOT / "optimize/gen_setup_lists.py"), "--check"], capture_output=True, text=True)
check("the asset is what optimize/lists/community says (python3 optimize/gen_setup_lists.py rewrites it)", gen.returncode == 0, gen.stdout + gen.stderr)
size = ASSET.stat().st_size
raw = sum(f.stat().st_size for f in (ROOT / "optimize/lists/community").glob("*.txt"))
check(f"the asset is kept small: {size // 1024} KB for {raw // 1024} KB of lists (each package once, numbers in base 36)", size < 100_000 and size * 3 < raw, (size, raw))
check("the asset keeps CleanYourTV's credit", "CleanYourTV, MIT" in ASSET.read_text(encoding="utf-8").splitlines()[0])

import setup_parity
from setup_parity import Pair, CLS
if not setup_parity.compile_java(check):
    sys.exit(0 if not shutil.which("javac") else 1)

spec = importlib.util.spec_from_file_location("community", ROOT / "optimize/community.py"); com = importlib.util.module_from_spec(spec); spec.loader.exec_module(com)
metas = com.all_metas()

# ---- the match, on every header: each list's own brand with each of its models and devices, other brands, generic and short values
def raw_values(path, key):
    for l in Path(path).read_text(encoding="utf-8").splitlines():
        m = re.match(rf"#\s*{key}:\s*(.*)$", l.strip())
        if m:
            return m.group(1).split()
    return []
cases = []
for m in metas:
    brands, models, devices = raw_values(m["path"], "match-brand"), raw_values(m["path"], "match-model"), raw_values(m["path"], "match-device")
    for b in brands[:2]:
        for mo in models or [""]:
            cases.append((b, b, mo, ""))
            for d in devices[:2]:
                cases.append((b, "", mo, d))
        for d in devices or [""]:
            cases.append((b.lower(), "", "Smart TV", d))
            cases.append(("Other", b, "x", d))
    for mo in models[:1]:
        cases.append(("NoSuchBrand", "", mo, devices[0] if devices else ""))
cases += [("TCL", "TCL", "Smart TV", "TV"), ("", "", "", ""), ("TCL", "", "ABC", "AB"), ("Hisense", "Hisense", "Android TV", "")]
tmp = Path(tempfile.mkdtemp())
(tmp / "cases").write_text("".join("|".join(c) + "\n" for c in cases))
jout = subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.CommunityRun", "choose", str(ASSET), str(tmp / "cases")], capture_output=True, text=True, encoding="utf-8").stdout.splitlines()
pout = []
for c in cases:
    meta, why = com.choose({"ro.product.manufacturer": c[0], "ro.product.brand": c[1], "ro.product.model": c[2], "ro.product.device": c[3]}, metas)
    pout.append(f"{Path(meta['path']).stem if meta else '-'} {why}")
diff = [(cases[i], pout[i], jout[i] if i < len(jout) else None) for i in range(len(cases)) if i >= len(jout) or pout[i] != jout[i]]
check(f"the match: the app and optimize/community.py give the same answer (and the same reason) on all {len(cases)} cases from the {len(metas)} list headers",
      not diff and len(jout) == len(cases), diff[:5])
amb = sum("none is used" in l for l in pout)
picked = sum(not l.startswith("-") for l in pout)
check(f"the cases hold ambiguous ones ({amb}: none is used, by both) and matched ones ({picked})", amb > 10 and picked > 100, (amb, picked))

# ---- the packages filter (keep, homes, launchers, once each), on every list
keep = [l.split()[0] for l in (ROOT / "optimize/lists/keep.txt").read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
homes = ["com.tcl.tvhome", "com.mitv.tvhome.atv"]
(tmp / "keep").write_text("\n".join(keep)); (tmp / "homes").write_text("\n".join(homes))
jp = subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.CommunityRun", "packages", str(ASSET), str(tmp / "keep"), str(tmp / "homes")], capture_output=True, text=True, encoding="utf-8").stdout.splitlines()
pp = [f"{Path(m['path']).stem}: {' '.join(com.packages(m['path'], keep, homes))}" for m in metas]
ask_n = len([l for l in (ROOT / "optimize/lists/community-ask.txt").read_text().splitlines() if l and l[0] not in "# \t"])
check("the packages of every list after the filters are community.py's, in its order", jp[:-1] == pp, [(a, b) for a, b in zip(jp, pp) if a != b][:3])
check("the asset carries community-ask.txt's expressions and the TCL packages our lists know", jp[-1].startswith(f"ASK {ask_n} KNOWN ") and int(jp[-1].split()[-1]) > 10, jp[-1])

# ---- UAD-ng's ratings: a small exact JSON walker
tricky = {"a.b": {"list": "Oem", "description": "has \"removal\": \"Unsafe\" and { braces } [x]", "dependencies": ["x"], "neededBy": [], "labels": [], "removal": "Recommended"},
          "c.d": {"removal": "Unsafe", "description": "é \\u00e9 \n"}, "e.f": {"list": "Misc", "dependencies": [{"z": 1}], "removal": "Expert", "n": 3, "b": True, "nul": None}}
(tmp / "uad.json").write_text(json.dumps(tricky, indent=2))
jr = subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.CommunityRun", "removals", str(tmp / "uad.json")], capture_output=True, text=True).stdout.splitlines()
check("UAD-ng: the «removal» of each package, never one inside a description (escaped quotes, braces, nested values)", jr == ["a.b Recommended", "c.d Unsafe", "e.f Expert"], jr)
(tmp / "bad.json").write_text("<html>rate limited</html>")
check("UAD-ng: a page that is not its JSON is «no rating» (the check is then skipped, and said)",
      subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.CommunityRun", "removals", str(tmp / "bad.json")], capture_output=True, text=True).stdout.strip() == "NULL")

# ---- the step, next to community.sh, on one fake TV
target = None
for m in metas:
    models = raw_values(m["path"], "match-model")
    if not models or com.choose({"ro.product.manufacturer": raw_values(m["path"], "match-brand")[0], "ro.product.model": models[0]}, metas)[0] is not m:
        continue
    pk = com.load(m["path"])[1]
    ask = [p for p in pk if any(re.search(a, p) for a in [l for l in (ROOT / "optimize/lists/community-ask.txt").read_text().splitlines() if l and l[0] not in "# \t"])]
    plain = [p for p in pk if p not in ask and not com.NOT_FOR_US.search(p) and p not in keep and not any(re.fullmatch(c, p) for c in
             [l.split()[0] for l in (ROOT / "optimize/lists/core.txt").read_text().splitlines() if l.strip() and not l.startswith("#")])]
    if ask and len(plain) >= 4:
        target = (m, raw_values(m["path"], "match-brand")[0], models[0], pk, ask, plain)
        break
check("a list with questions and plain packages to test with", target is not None)
m, brand, model, pk, ask, plain = target
name = Path(m["path"]).stem
FAKE_UAD = """#!/usr/bin/env python3
import os, sys
if os.environ.get("UAD_OFFLINE") == "1":
    sys.exit("urlopen error")
unsafe = os.environ.get("UNSAFE", "").split()
for l in open(sys.argv[1]):
    p = l.split()[0] if l.strip() else ""
    if p: print(("Unsafe" if p in unsafe else "Recommended").ljust(12), p)
"""
def case(installed, off=(), unsafe=(), offline=False, homes=None, **env):
    p = Pair(packages=installed, off=off, MAKER=brand, MODEL=model, DEVICE="zz", UNSAFE=" ".join(unsafe), UAD_OFFLINE="1" if offline else "0",
             HOMES=homes if homes is not None else "com.google.android.apps.tv.launcherx/.home.HomeActivity", **env)
    (p.tree / "optimize/check-packages.py").write_text(FAKE_UAD)
    (p.tree / "optimize/run/tv.txt").write_text(f"ro.product.manufacturer = {brand}\nro.product.brand = {brand}\nro.product.model = {model}\nro.product.device = zz\n")
    if not offline:
        (p.base / "uad.json").write_text(json.dumps({x: {"removal": "Unsafe" if x in unsafe else "Recommended"} for x in pk}))
        p.env["UAD_FILE"] = str(p.base / "uad.json")
    rc, out = p.shell("community.sh")
    res, jout = p.java("community")
    return p, rc, out, res, jout

others = [f"com.example.app{i}" for i in range(20)]
inst = others + ["com.google.android.apps.tv.launcherx", "com.android.vending"] + pk + ["com.tcl.tv"]
p, rc, out, res, jout = case(inst, off=[plain[0]], unsafe=[plain[1]])
applied_sh, applied_app = p.file("sh", "community-applied.txt"), p.file("java", "community-applied.txt")
check(f"step ({name}): the same packages turned off as community.sh, the same TV afterwards", rc == 0 and res == "DONE" and p.same(), (p.diff(), out, jout))
check("step: community-applied.txt holds what was turned off (the same set as the script's), recorded before the commands",
      applied_sh and set(applied_sh.split()) == set(applied_app.split()) and plain[0] not in applied_app.split() and plain[1] not in applied_app.split() and plain[2] in applied_app.split(),
      (applied_sh, applied_app))
check("step: the questions are community.sh's community-ask.txt, and stay on", set((p.file("sh", "community-ask.txt") or "").split()) == set((p.file("java", "community-ask.txt") or "").split()) == set(ask)
      and all(a not in p.tv("java")["off"].split() for a in ask), (p.file("sh", "community-ask.txt"), p.file("java", "community-ask.txt"), ask))
check("step: already off before us is not ours; UAD-ng's Unsafe is left on; the list's name is kept", (p.file("java", "community-name.txt") or "").strip() == name and plain[1] not in p.tv("java")["off"].split())
check("step: the ledger (disabled-by-us.txt) gets them too, as disable.sh writes it", set((p.file("java", "disabled-by-us.txt") or "").split()) == set((p.file("sh", "disabled-by-us.txt") or "").split()),
      (p.file("java", "disabled-by-us.txt"), p.file("sh", "disabled-by-us.txt")))
src = (setup_parity.SETUP / "SetupCommunity.java").read_text(encoding="utf-8")
a = src[src.index("SetupEngine.Result apply()"):src.index("private List<String> withoutUnsafe")]
check("step: journaled before it acts (community-applied.txt and the ledger, then the commands)", a.index("union(APPLIED, todo)") < a.index("union(LEDGER, todo)") < a.index('tv.run("pm disable-user --user 0 " + p)'))

# a HOME answerer in the list is never turned off; no HOME answer at all: nothing is turned off
home_pkg = plain[3]
p, rc, out, res, jout = case(inst, homes=f"com.google.android.apps.tv.launcherx/.home.HomeActivity {home_pkg}/.Main")
check("step: an app that answers HOME on this TV is never turned off (both)", p.same() and home_pkg not in p.tv("java")["off"].split() and res == "DONE", (p.diff(), out))
p, rc, out, res, jout = case(inst, NO_HOMES="yes")
check("step: the TV does not say which app is its home: nothing turned off (community.sh exits 1, the app's step failed)", rc == 1 and res == "FAILED" and p.changes("java") == [] and p.changes("sh") == [], (rc, res, p.diff()))
# offline: neither applies anything (the third-party list is not applied without its one safety rating); the app says so, and the run goes on
p, rc, out, res, jout = case(inst, offline=True)
check("step offline: community.sh and the app both apply NOTHING; the app's step is «تُرك» with «ما قدرنا نتأكد من القائمة بدون إنترنت», and records it for the report",
      "!! community list not applied" in out and p.changes("sh") == [] == p.changes("java") and res == "SKIPPED" and "تُرك: ما قدرنا نتأكد من القائمة بدون إنترنت" in jout
      and (p.file("java", "community-offline.txt") or "").strip() == "تُرك: ما قدرنا نتأكد من القائمة بدون إنترنت" and p.file("java", "community-applied.txt") is None, (out, jout))
check("community.sh's comment says what it does offline (nothing applied)", "offline (the check cannot run) NOTHING from the community list is applied" in (ROOT / "optimize/community.sh").read_text())
rsrc = (setup_parity.SETUP / "SetupReport.java").read_text(encoding="utf-8")
check("the report says it too, as a «!! » line of share.py's allowlist", '"!! community list: " + SetupCommunity.OFFLINE_SAID' in rsrc)
# our own lists know this TV (3 TCL packages of ours): no community list
p, rc, out, res, jout = case(inst + ["com.tcl.bi", "com.tcl.logkit", "com.tcl.usercenter"])
check("step: a TV our own lists know (3 of their TCL packages) gets no community list (both)", p.changes("java") == [] == p.changes("sh") and res == "DONE", (p.diff(), out))
# another model of the brand: nothing
p = Pair(packages=inst, MAKER=brand, MODEL="ZZZZZ999", DEVICE="zz")
(p.tree / "optimize/run/tv.txt").write_text(f"ro.product.manufacturer = {brand}\nro.product.model = ZZZZZ999\nro.product.device = zz\n")
rc, out = p.shell("community.sh"); res, jout = p.java("community")
check("step: a model no list is for: nothing turned off (both)", p.changes("java") == [] == p.changes("sh") and res == "DONE" and rc == 0, (out, jout))

# ---- the answers, and the undo
p, rc, out, res, jout = case(inst)
q = ask[0]
p.env["ANSWERS"] = f"{q}=yes" + "".join(f",{x}=no" for x in ask[1:2])
res, jout = p.java("answers")
check("answers: a «yes» is turned off and recorded (community-applied.txt and the ledger) first; answered questions leave the list; the rest stay asked",
      res == "DONE" and q in p.tv("java")["off"].split() and q in (p.file("java", "community-applied.txt") or "").split() and q in (p.file("java", "disabled-by-us.txt") or "").split()
      and set((p.file("java", "community-ask.txt") or "").split()) == set(ask[2:]), (jout, p.file("java", "community-ask.txt")))
eng = (setup_parity.SETUP / "SetupEngine.java").read_text(encoding="utf-8")
check("undo: the engine turns community-applied.txt's packages back on with the ledger, and empties both once confirmed",
      'ledgerNames(store.get("community-applied.txt"))' in eng[eng.index("Result undo()"):] and 'store.put("community-applied.txt", cb.toString())' in eng)
runner = (setup_parity.SETUP / "SetupRunner.java").read_text(encoding="utf-8")
check("the runner: the community step after the safe list, the questions and the answers through it (one run at a time)",
      runner.index('"safe", true') < runner.index('"community", true') < runner.index('"tweaks", true') and "Start answer(Map<String, Boolean> answers, Host host)" in runner
      and "static List<String[]> questions(" in runner)
sys.exit(1 if failed else 0)
