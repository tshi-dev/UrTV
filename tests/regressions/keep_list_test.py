#!/usr/bin/env python3
"""Regression gate R-KEEP (4.3.4: a Sony remote's Netflix, YouTube, Prime, mic and colour buttons did nothing after a setup; community lists
had turned off the packages that answer them). No list we turn packages off with may name a package of optimize/lists/keep.txt:
- the fixed lists (tcl-safe.txt, tcl-optional.txt, home-replaced.txt) and the TV setup's own copies (SetupLists.SAFE, HOME_HELPERS);
- SetupLists.KEEP is keep.txt (optimize/gen_setup_lists.py made it, nobody edited one side);
- the community lists may name them (they are other people's lists), so every place that applies one drops keep.txt first:
  optimize/disable.sh refuses them, optimize/community.sh and optimize/community.py drop them, the TV setup (SetupCommunity) drops them;
- the 4.3.4 remote-button packages are in keep.txt.
Run: python3 tests/regressions/keep_list_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
L = ROOT / "optimize/lists"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def pkgs(path):
    out = []
    for ln in path.read_text(encoding="utf-8").splitlines():
        w = ln.split("#")[0].strip().split()
        if w:
            out.append(w[0])
    return out

def java_array(src, name):
    m = re.search(r"static final String\[\] " + name + r" = \{(.*?)\};", src, re.S)
    return re.findall(r'"([^"]+)"', m.group(1)) if m else None

keep = set(pkgs(L / "keep.txt"))
check("keep.txt is read (more than 50 packages)", len(keep) > 50, len(keep))
for f in ("tcl-safe.txt", "tcl-optional.txt", "home-replaced.txt"):
    clash = sorted(set(pkgs(L / f)) & keep)
    check(f"{f} turns off nothing keep.txt keeps", not clash, clash)
sl = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupLists.java").read_text(encoding="utf-8")
for arr in ("SAFE", "HOME_HELPERS"):
    a = java_array(sl, arr)
    check(f"SetupLists.{arr} exists and turns off nothing keep.txt keeps", a is not None and not (set(a) & keep), a and sorted(set(a) & keep))
k = java_array(sl, "KEEP")
check("SetupLists.KEEP is keep.txt (optimize/gen_setup_lists.py)", k is not None and set(k) == keep,
      k and {"only in keep.txt": sorted(keep - set(k)), "only in SetupLists": sorted(set(k) - keep)})
dis = (ROOT / "optimize/disable.sh").read_text(encoding="utf-8")
check("optimize/disable.sh refuses keep.txt's packages", "keep.txt" in dis and re.search(r"refus", dis, re.I))
cs = (ROOT / "optimize/community.sh").read_text(encoding="utf-8")
check("optimize/community.sh drops keep.txt's packages", 'keep.txt' in cs)
cp = (ROOT / "optimize/community.py").read_text(encoding="utf-8")
check("optimize/community.py drops the protected ones (--keep)", "--keep" in cp)
sc = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupCommunity.java").read_text(encoding="utf-8")
check("the TV setup drops SetupLists.KEEP from a community list (both places)", sc.count("SetupLists.KEEP") >= 2, sc.count("SetupLists.KEEP"))
for p in ("com.sony.dtv.netflixmanager", "com.netflix.ninja"):
    check(f"4.3.4: {p} (a remote button) is kept", p in keep)
# R-280 (4.3.6): a TV's own Settings, Live TV / channels and tuning apps that community lists named are kept, each with its reason
SETTINGS_TV = ("com.zeasn.whaleos.settings", "com.apps.channels", "com.beko.tv.channel", "com.grundig.tv.channel", "com.zhixin.atvchannel", "com.cvte.tv.player",
               "com.mediatek.tv.oneworld.tvcenter", "com.sony.dtv.broadcastsettings", "com.sony.dtv.tuningconfirmation_dvbt", "com.sony.dtv.serviceinformation",
               "com.sony.dtv.b2b.prosettings", "com.sony.dtv.deviceadminsettings", "com.sony.dtv.b2b.deviceadminsettings", "com.softwinner.settingssetup",
               "com.softwinner.settingsassist", "com.amazon.whasettings")
kt = (L / "keep.txt").read_text(encoding="utf-8")
for p in SETTINGS_TV:
    check(f"R-280 {p} (Settings / Live TV / tuning) is kept, with its reason", p in keep and re.search(r"^" + re.escape(p) + r"\s+#\s*\S", kt, re.M))
sys.exit(1 if failed else 0)
