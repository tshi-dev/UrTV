#!/usr/bin/env python3
"""Community lists (CleanYourTV, MIT): the importer (.github/tools/community_lists.py), the matching rule (optimize/community.py),
the keep.txt / launcher filters, the credit header on every committed list, and the step (optimize/community.sh) on a fake TV.

    python3 tests/community_test.py
"""
import importlib.util, json, os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m
imp = load(ROOT / ".github/tools/community_lists.py", "community_lists")
com = load(ROOT / "optimize/community.py", "community")

# ---- the importer, on a small made-up copy of their repo
src = Path(tempfile.mkdtemp())
(src / "examples/tcl-43s5400a").mkdir(parents=True); (src / "examples/sony-bravia-4k-ur2").mkdir(parents=True); (src / "examples/empty-one").mkdir(parents=True)
(src / "LICENSE").write_text("MIT License\n\nCopyright (c) 2026 Gathin\n")
(src / "examples/tcl-43s5400a/debloat.txt").write_text(
    '# TCL 43S5400A (43" FHD, S5400A series), Indonesia unit: device C06, product C06_2K_GB, model "Smart TV".\n'
    '# Some prose of theirs that must not be copied: "keep the updater".\n#\n'
    "com.tcl.usercenter            # account, prose of theirs\n# com.tcl.tv   a package they keep\ncom.tcl.messagebox\ncom.tcl.usercenter\n\n"
    "com.google.android.apps.tv.launcherx\n")
(src / "examples/sony-bravia-4k-ur2/debloat.txt").write_text(
    "# Sony BRAVIA 4K UR2 (device BRAVIA_UR2_4K, MediaTek)\n# Android 10\ncom.sony.dtv.promos\n")
(src / "examples/empty-one/debloat.txt").write_text("# nothing here\n")
subprocess.run(["git", "init", "-q"], cwd=src); subprocess.run(["git", "-c", "user.email=a@b", "-c", "user.name=t", "add", "-A"], cwd=src)
subprocess.run(["git", "-c", "user.email=a@b", "-c", "user.name=t", "commit", "-qm", "x"], cwd=src)
sha = subprocess.run(["git", "rev-parse", "HEAD"], cwd=src, capture_output=True, text=True).stdout.strip()
out = Path(tempfile.mkdtemp()) / "community"
(out).mkdir(); (out / "stale-model.txt").write_text("# old\ncom.old\n")
r = subprocess.run([sys.executable, str(ROOT / ".github/tools/community_lists.py"), "--src", str(src), "--out", str(out), "--date", "2026-10-23"], capture_output=True, text=True)
tcl = (out / "tcl-43s5400a.txt").read_text()
check("importer: runs and says how many it wrote", r.returncode == 0 and "2 lists written" in r.stdout, r.stdout + r.stderr)
check("importer: the header credits the project, the licence, the source path, the commit and the date",
      "CleanYourTV" in tcl and "MIT licence" in tcl and "Copyright (c) 2026 Gathin" in tcl and "examples/tcl-43s5400a/debloat.txt" in tcl
      and f"commit {sha}" in tcl and "taken 2026-10-23" in tcl, tcl)
check("importer: packages only (no comment of theirs, no duplicate, nothing commented out)",
      [l for l in tcl.splitlines() if l and not l.startswith("#")] == ["com.tcl.usercenter", "com.tcl.messagebox", "com.google.android.apps.tv.launcherx"]
      and "prose" not in tcl and "keep the updater" not in tcl and "com.tcl.tv" not in tcl, tcl)
check("importer: brand, model and device read from the title", "# match-brand: TCL" in tcl and "43S5400A" in tcl.split("match-model:")[1].split("\n")[0]
      and "# match-device: C06" in tcl, tcl)
check("importer: a Sony title gives the model words and the device", "BRAVIA4KUR2" in (out / "sony-bravia-4k-ur2.txt").read_text()
      and "# match-device: BRAVIAUR24K" in (out / "sony-bravia-4k-ur2.txt").read_text())
check("importer: a list with no packages is not written, a stale one is removed, the licence is copied",
      not (out / "empty-one.txt").exists() and not (out / "stale-model.txt").exists() and "Gathin" in (out / "LICENSE-CleanYourTV.txt").read_text())

# ---- matching
def meta(brands, models=(), devices=(), name="x"):
    return {"brands": {com.norm(b) for b in brands}, "models": {com.norm(m) for m in models}, "devices": {com.norm(d) for d in devices}, "path": name}
L1 = meta(["TCL"], ["43S5400A"], ["C06"], "a")
L2 = meta(["TCL"], ["40S5400A"], ["C06"], "b")
L3 = meta(["Sony"], ["BRAVIA 4K UR2"], ["BRAVIA_UR2_4K"], "c")
L4 = meta(["TCL"], ["55P735"], ["G05"], "d")
def pick(props, *metas): m, why = com.choose(props, list(metas)); return (m["path"] if m else None), why
check("match: brand and model", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "43S5400A"}, L1, L2, L3)[0] == "a")
check("match: the model written differently (case, dashes, spaces)", pick({"ro.product.manufacturer": "Sony", "ro.product.model": "bravia 4k-ur2"}, L1, L3)[0] == "c")
check("match: the device code when it is unique", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "Smart TV", "ro.product.device": "G05"}, L1, L2, L4)[0] == "d")
check("match: a shared device code (C06 is two sizes) is a guess: none", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "Smart TV", "ro.product.device": "C06"}, L1, L2)[0] is None)
check("match: another model of the same brand: none", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "50P735", "ro.product.device": "G07"}, L1, L2, L4)[0] is None)
check("match: the right model of another brand: none", pick({"ro.product.manufacturer": "Haier", "ro.product.brand": "Haier", "ro.product.model": "43S5400A"}, L1)[0] is None)
check("match: the brand field counts as well as the maker", pick({"ro.product.manufacturer": "Hon Hai", "ro.product.brand": "TCL", "ro.product.model": "43S5400A"}, L1)[0] == "a")
check("match: a generic model («Smart TV») never counts", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "Smart TV"}, meta(["TCL"], ["Smart TV"], []))[0] is None)
check("match: a short model never counts", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "U75"}, meta(["TCL"], ["U75"], []))[0] is None)
check("match: two lists with the same model: none", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "43S5400A"}, L1, meta(["TCL"], ["43S5400A"], [], "dup"))[0] is None)
check("match: a model hit wins over a device hit on another list", pick({"ro.product.manufacturer": "TCL", "ro.product.model": "40S5400A", "ro.product.device": "C06"}, L1, L2)[0] == "b")
check("match: no props, no match", pick({}, L1)[0] is None)

# ---- the filters
tmp = Path(tempfile.mkdtemp())
lst = tmp / "l.txt"
lst.write_text("# h\n" + "\n".join(["com.tcl.messagebox", "com.tcl.tv", "com.google.android.apps.tv.launcherx", "com.google.android.tungsten.setupwraith",
                                   "com.acme.launcher", "com.acme.tvhome", "com.x.provision", "com.vendor.homeapp", "com.tcl.bi", "com.not.here"]) + "\n")
got = com.packages(str(lst), keep=["com.tcl.tv"], homes=["com.vendor.homeapp"], installed={"com.tcl.messagebox", "com.tcl.tv", "com.tcl.bi", "com.acme.launcher",
                                                                                    "com.google.android.tungsten.setupwraith", "com.vendor.homeapp"})
check("filter: keep.txt, launchers, the setup wizard, HOME handlers and what is not on the TV are all left out", got == ["com.tcl.messagebox", "com.tcl.bi"], str(got))

# ---- every committed list: credit header, packages only, matching fields
files = sorted((ROOT / "optimize/lists/community").glob("*.txt"))
files = [f for f in files if not f.name.startswith("LICENSE")]
check("snapshot: lists are committed", len(files) > 100, str(len(files)))
bad = []
for f in files:
    t = f.read_text(encoding="utf-8")
    head = [l for l in t.splitlines() if l.startswith("#")]
    body = [l for l in t.splitlines() if l and not l.startswith("#")]
    ok = (("CleanYourTV" in t) and "MIT licence" in t and re.search(r"Source: examples/[\w.-]+/debloat\.txt at commit [0-9a-f]{40}, taken \d{4}-\d\d-\d\d\.", t)
          and "# match-brand:" in t and "# match-model:" in t and "# match-device:" in t and body and all(re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.]*", l) for l in body)
          and all(len(l) < 160 for l in head))
    if not ok: bad.append(f.name)
check("snapshot: every list has the credit header (project, MIT, source path, commit, date) and only package names", not bad, str(bad[:5]))
check("snapshot: the licence text is beside them", "MIT License" in (ROOT / "optimize/lists/community/LICENSE-CleanYourTV.txt").read_text())
real = [com.load(str(f))[0] for f in files]
for props, want, label in (
        ({"ro.product.manufacturer": "TCL", "ro.product.brand": "TCL", "ro.product.model": "Smart TV", "ro.product.device": "C06"}, None, "C06 (several sizes)"),
        ({"ro.product.manufacturer": "Sony", "ro.product.brand": "Sony", "ro.product.model": "BRAVIA 4K UR2", "ro.product.device": "BRAVIA_UR2_4K"}, "sony-bravia-4k-ur2", "Sony UR2")):
    m, why = com.choose(props, real)
    check(f"snapshot: {label} -> {want or 'no list'}", (Path(m["path"]).stem if m else None) == want, why)

# ---- the step, on a fake TV (a copy of the scripts and lists)
tree = Path(tempfile.mkdtemp())
(tree / "optimize/lists/community").mkdir(parents=True); (tree / "optimize/run").mkdir()
for f in ("community.sh", "community.py", "offlist.py", "disable.sh"):
    shutil.copy(ROOT / "optimize" / f, tree / "optimize" / f)
for f in ("keep.txt", "core.txt", "tcl-safe.txt", "tcl-optional.txt", "names-ar.txt", "community-ask.txt"):
    shutil.copy(ROOT / "optimize/lists" / f, tree / "optimize/lists" / f)
(tree / "optimize/check-packages.py").write_text("import sys\nprint('Unsafe       com.acme.risky  x')\nprint('Recommended  com.acme.ads  x')\n")
(tree / "optimize/lists/community/acme-tv-100.txt").write_text(
    "# UrTV community list: acme-tv-100\n# match-brand: ACME\n# match-model: TV100X\n# match-device: acmedev\n"
    "com.acme.ads\ncom.acme.risky\ncom.android.vending\ncom.acme.launcher\ncom.acme.promo\ncom.acme.kept\ncom.acme.gone\ncom.acme.alreadyoff\n"
    "com.android.providers.contacts\ncom.google.android.permissioncontroller\ncom.vendor.support.webview\n"
    "com.acme.updater\ncom.acme.voiceassist\ncom.acme.milink.service\ncom.acme.videoplayer\ncom.google.android.apps.youtube.music\n")
fake = tree / "fakepy"
fake.write_text(r'''#!/usr/bin/env python3
import os, re, sys
a = sys.argv[3:]
S = open(os.environ["TVSTATE"]).read().split()
LOG = open(os.environ["LOG"], "a")
def save(): open(os.environ["TVSTATE"], "w").write("\n".join(S) + "\n")
def run(cmd):
    global S
    LOG.write(cmd + "\n")
    if cmd.endswith("; echo END_OF_LIST"):   # the end-marked questions of disable.sh
        base = cmd[: -len("; echo END_OF_LIST")]
        return ("package:android\n" if base == "pm list packages" else "") + run(base) + "END_OF_LIST\n"
    if cmd == "pm list packages": return "".join("package:%s\n" % p.lstrip("-") for p in S)
    if cmd == "pm list packages -d": return "".join("package:%s\n" % p[1:] for p in S if p.startswith("-"))
    if cmd.startswith("cmd package query-activities"): return "" if os.environ.get("NOHOMES") else "2 activities found:\n  Activity #0:\n    priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\n    com.acme.launcher/.Main\n  Activity #1:\n    priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\n    com.google.android.apps.tv.launcherx/.Home\n"
    m = re.fullmatch(r"pm disable-user --user 0 (\S+)", cmd)
    if m: S = [("-" + p if p == m.group(1) else p) for p in S]; save(); return "ok\n"
    return ""
if a[:1] == ["--batch"]:
    for l in sys.stdin:
        if l.strip(): sys.stdout.write("$ %s\n%s" % (l.strip(), run(l.strip())))
else: sys.stdout.write(run(a[0]))
''')
fake.chmod(0o755)
(tree / "py").write_text('#!/bin/bash\nif [ "$2" = optimize/tv.py ]; then exec ' + str(fake) + ' "$@"; else exec python3 "$@"; fi\n'); (tree / "py").chmod(0o755)
def step(props, installed, home=None, **extra_env):
    (tree / "optimize/run/tv.txt").write_text("".join(f"{k} = {v}\n" for k, v in props.items()))
    (tree / "state").write_text("\n".join(installed) + "\n"); (tree / "log").write_text("")
    env = {**os.environ, "PY": str(tree / "py"), "TVSTATE": str(tree / "state"), "LOG": str(tree / "log"), "URTV_HOME": str(home or tree / "home"), **extra_env}
    for f in ("community-applied.txt", "community-name.txt"):
        (tree / "optimize/run" / f).unlink(missing_ok=True)
    r = subprocess.run(["bash", str(tree / "optimize/community.sh")], cwd=tree, env=env, capture_output=True, text=True)
    ap = (tree / "optimize/run/community-applied.txt")
    return r.returncode, r.stdout + r.stderr, (ap.read_text().split() if ap.exists() else []), (tree / "log").read_text(), (tree / "state").read_text().split()
ACME = {"ro.product.manufacturer": "Acme", "ro.product.brand": "Acme", "ro.product.model": "TV100X", "ro.product.device": "acmedev"}
PKGS = ["com.android.providers.contacts", "com.google.android.permissioncontroller", "com.vendor.support.webview", "com.acme.ads", "com.acme.risky", "com.android.vending", "com.acme.launcher", "com.acme.promo", "com.acme.kept", "-com.acme.alreadyoff", "com.other.app", "com.acme.updater", "com.acme.voiceassist", "com.acme.milink.service", "com.acme.videoplayer", "com.google.android.apps.youtube.music"]
(tree / "home").mkdir(); (tree / "home/kept.txt").write_text("com.acme.kept\n")
rc, o, applied, log, state = step(ACME, PKGS, tree / "home")
state0 = state
check("step: the model's list is applied minus keep.txt, the launcher, the Unsafe one, the kept one, the missing and the already-off",
      rc == 0 and sorted(applied) == ["com.acme.ads", "com.acme.promo"] and "-com.acme.ads" in state and "-com.acme.promo" in state
      and "com.android.vending" in state and "com.acme.risky" in state and "com.acme.launcher" in state and "com.acme.kept" in state, f"{o} {applied} {state}")
rc2, o2, applied2, log2, state2 = step(ACME, PKGS, tree / "home", NOHOMES="1")
check("step: the TV did not say which app is its home (empty answer): nothing is turned off, a !! line says so", rc2 == 1 and applied2 == [] and "pm disable-user" not in log2 and "!! community list not applied: the TV did not say which app is its home screen" in o2, o2)
real_check = (tree / "optimize/check-packages.py").read_text()
(tree / "optimize/check-packages.py").write_text("import sys\nsys.exit(1)\n")
rc2, o2, applied2, log2, state2 = step(ACME, PKGS, tree / "home")
(tree / "optimize/check-packages.py").write_text(real_check)
check("step: the Unsafe check could not run (offline): the third-party list is NOT applied at all, a !! line says so, and the step does not stop the setup (exit 0)", rc2 == 0 and applied2 == [] and "pm disable-user" not in log2 and "!! community list not applied: the Unsafe check (UAD-ng) needs the internet" in o2, o2)
rc, o, applied, log, state = step(ACME, PKGS, tree / "home")   # (the real run again, for the checks below)
asked = (tree / "optimize/run/community-ask.txt")
check("step: an update service, voice, MiLink and a video player are left ON and offered to the person; YouTube Music is never turned off",
      sorted(asked.read_text().split()) == ["com.acme.milink.service", "com.acme.updater", "com.acme.videoplayer", "com.acme.voiceassist"] if asked.exists() else False
      and all(p in state for p in ("com.acme.updater", "com.acme.voiceassist", "com.acme.milink.service", "com.acme.videoplayer", "com.google.android.apps.youtube.music"))
      and "ask in one Arabic line" in o, f"{o} {state}")
check("step: recorded and said (name, CleanYourTV)", (tree / "optimize/run/community-name.txt").read_text().strip() == "acme-tv-100" and "CleanYourTV" in o and "rated Unsafe: com.acme.risky" in o, o)
rc, o, applied, log, state = step({**ACME, "ro.product.model": "TV200", "ro.product.device": "otherdev"}, PKGS)
check("step: another model of the brand: nothing applied, said", rc == 0 and applied == [] and "unknown model: no optional list" in o and "pm disable-user" not in log, o)
rc, o, applied, log, state = step({**ACME, "ro.product.manufacturer": "Other", "ro.product.brand": "Other"}, PKGS)
check("step: another brand with the same model: nothing applied", applied == [] and "pm disable-user" not in log, o)
rc, o, applied, log, state = step({"ro.product.manufacturer": "TCL", "ro.product.model": "55P735"}, ["com.tcl.bi", "com.tcl.usercenter", "com.tcl.logkit", "com.tcl.messagebox", "com.acme.ads"])
check("step: a TV our own lists know (3+ TCL packages): no community list at all", applied == [] and "our own lists know this TV" in o and "pm disable-user" not in log, o)
# Android's own core is never disabled: community.sh leaves it out, disable.sh refuses a list that names it
check("core: the model's list named contacts, permissioncontroller and a WebView: all left on",
      "com.android.providers.contacts" in state0 and "com.google.android.permissioncontroller" in state0 and "com.vendor.support.webview" in state0
      and "-com.android.providers.contacts" not in state0, state0)
bad = tree / "bad.txt"; bad.write_text("com.acme.ads\ncom.google.android.ext.services\n")
r = subprocess.run(["bash", str(tree / "optimize/disable.sh"), str(bad)], capture_output=True, text=True, env=dict(os.environ, PY=str(fake)))
check("core: disable.sh refuses a list with a core package, and changes nothing", r.returncode == 1 and "Android's own core" in r.stdout and "ext.services" in r.stdout, r.stdout + r.stderr)
committed = [l.split()[0] for f in (ROOT / "optimize/lists/community").glob("*.txt") for l in f.read_text().splitlines() if l.strip() and not l.startswith("#")]
core = [re.compile("^(" + l.strip() + ")$") for l in (ROOT / "optimize/lists/core.txt").read_text().splitlines() if l.strip() and not l.startswith("#")]
check("core: none of our own lists names a core package", not [p for f in ("tcl-safe.txt", "tcl-optional.txt") for p in
      [l.split()[0] for l in (ROOT / "optimize/lists" / f).read_text().splitlines() if l.strip() and not l.startswith("#")] if any(c.match(p) for c in core)])
# the ask list catches a firmware update service by its usual names (a community list turned off com.xiaomi.mitv.updateservice on one TV)
ask_file = ROOT / "optimize/lists/community-ask.txt"
pats = [l for l in ask_file.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#") and not l[0].isspace()]
import re as _re
def asked(pkg): return any(_re.search(p, pkg) for p in pats)
check("community-ask.txt catches com.xiaomi.mitv.updateservice and the other usual update-service names",
      all(asked(x) for x in ("com.xiaomi.mitv.updateservice", "com.vendor.tv.update", "com.vendor.updater", "com.vendor.ota", "com.vendor.systemupdate", "com.vendor.fota.service")), "")
check("... and not the ordinary packages around them", not any(asked(x) for x in ("com.vendor.tv.updatecheck.ads", "com.vendor.ads", "com.android.providers.contacts", "com.vendor.tv.launcher")), [x for x in ("com.vendor.tv.updatecheck.ads", "com.vendor.ads", "com.vendor.tv.launcher") if asked(x)])

# core.txt never lets these go (a Haier boot loop; Clean Your TV's CORE_RE), as whole names
core_res = [l for l in (ROOT / "optimize/lists/core.txt").read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
def is_core(pkg): return any(_re.fullmatch(r, pkg) for r in core_res)
check("core.txt protects com.cltv.mal, com.mediatek.tis, com.mediatek.tv.service, com.mediatek.tv.agent, android.ext.services and android.ext.shared",
      all(is_core(x) for x in ("com.cltv.mal", "com.mediatek.tis", "com.mediatek.tv.service", "com.mediatek.tv.agent", "android.ext.services", "android.ext.shared")), [x for x in ("com.cltv.mal", "com.mediatek.tis", "com.mediatek.tv.service", "com.mediatek.tv.agent", "android.ext.services", "android.ext.shared") if not is_core(x)])
check("... only those names, not the packages that merely start the same way", not any(is_core(x) for x in ("com.mediatek.tv.ads", "com.mediatek.tisx", "android.ext.servicesx", "com.cltv.mal.ads", "com.mediatek.logger")), "")
check("... and the comment credits Clean Your TV", "Clean Your TV" in (ROOT / "optimize/lists/core.txt").read_text(encoding="utf-8"), "")

print("\nall community checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
