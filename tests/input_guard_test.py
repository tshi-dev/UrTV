"""The TV's inputs and keyboards are never turned off (Clean Your TV's input-driver guard, github.com/Gathin23/cleanyourtv, MIT, issue #321: a port
whose input driver was disabled notices the cable and stays black). Two layers: optimize/lists/inputs.txt by name (disable.sh refuses a list that
names one, community.sh and the setup on the TV leave them out), and the TV's own answer (dumpsys package: TV_INPUT_HARDWARE asked for, a
TvInputService or an input method served), which disable.sh and InputGuard.java ask with the very same command. And every community list, after
our filters, holds nothing we protect. One fake TV (tests/fixtures/setup-faketv.sh); no TV needed.

    python3 tests/input_guard_test.py
"""
import importlib.util, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
from setup_parity import Pair
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def regexes(name):
    return [l.split()[0] for l in (ROOT / "optimize/lists" / name).read_text(encoding="utf-8").splitlines() if l.strip() and not l.lstrip().startswith("#")]
def words(name):
    return regexes(name)

INPUTS, CORE, KEEP = regexes("inputs.txt"), regexes("core.txt"), set(words("keep.txt"))
def is_input(p): return any(re.fullmatch(r, p) for r in INPUTS)

# ---- the names
for p in ("com.tcl.tvinput", "com.amazon.franktvinput", "com.mediatek.dtv.tvinput.atsctuner", "com.sony.dtv.tvinput.atsc3tuner", "com.sony.dtv.servicemode.tuner",
          "com.google.android.tv", "com.amazon.tv.livetv", "com.example.hdmicec", "com.example.inputsource", "com.example.tv.source", "com.example.cec",
          "com.google.android.inputmethod.latin", "com.google.android.inputmethod.japanese", "com.sohu.inputmethod.sogou.tv", "org.liskovsoft.androidtv.rukeyboard"):
    check(f"inputs.txt: {p} is an input or a keyboard", is_input(p))
for p in ("com.tcl.bi", "com.tcl.partnercustomizer.resource", "com.amazon.tv.ooberesource", "com.google.android.tvrecommendations", "com.google.android.tvlauncher",
          "com.netflix.ninja", "com.tcl.waterfall.resource", "com.google.android.youtube.tv"):
    check(f"inputs.txt: {p} is not (a name that only looks like one)", not is_input(p))
for name in ("tcl-safe.txt", "tcl-optional.txt", "home-replaced.txt"):
    hit = [p for p in words(name) if is_input(p)]
    check(f"our own {name} names no input or keyboard (disable.sh would refuse it)", not hit, hit)

# ---- the same command from the script and the app
sh = (ROOT / "optimize/disable.sh").read_text(encoding="utf-8")
java = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/InputGuard.java").read_text(encoding="utf-8")
g_sh = re.search(r"^GUARD='(.*)'$", sh, re.M)
g_java = re.search(r'static final String TEMPLATE = "((?:[^"\\]|\\.)*)";', java)
unescaped = re.sub(r'\\(.)', r'\1', g_java.group(1)) if g_java else None
check("the app's InputGuard.TEMPLATE is disable.sh's GUARD, to the letter", g_sh and unescaped == g_sh.group(1), (g_sh and g_sh.group(1), unescaped))
check("the guard reads only the package's own «requested permissions» block (not a shared user's), and TvInputService and input methods",
      g_sh and all(x in g_sh.group(1) for x in ("/requested permissions:/,/(install|runtime) permissions:|User [0-9]+:/p", "TV_INPUT_HARDWARE",
                                                  "android\\.media\\.tv\\.TvInputService", "android\\.view\\.InputMethod", "hung system process", "echo END_OF_LIST")))
for f, needle in (("SetupEngine.java", "InputGuard.parse(text(InputGuard.command(intended)))"), ("SetupCommunity.java", "InputGuard.parse(tv.text(InputGuard.command(pkgs)))")):
    check(f"{f} asks the TV with InputGuard before it turns anything off", needle in (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup" / f).read_text(encoding="utf-8"))
check("the setup on the TV leaves inputs.txt's packages out (SetupLists.INPUTS, CommunityMatch.input)",
      "static final String[] INPUTS" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupLists.java").read_text(encoding="utf-8")
      and "!CommunityMatch.input(p)" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupEngine.java").read_text(encoding="utf-8"))

# ---- disable.sh on one fake TV
def run(env, listed=("com.tcl.bi", "com.tcl.logkit", "com.not.here")):
    p = Pair(**env)
    (p.tree / "l.txt").write_text("".join(x + "\n" for x in listed))
    rc, out = p.shell("disable.sh", str(p.tree / "l.txt"))
    led = (p.tree / "optimize/run/disabled-by-us.txt")
    return p, rc, out, led.read_text().split() if led.exists() else None

p, rc, out, led = run({})
check("disable.sh: a TV with no input driver in the list: everything is turned off", rc == 0 and led == ["com.tcl.bi", "com.tcl.logkit"], (rc, out, led))
p, rc, out, led = run({"INPUT_PKGS": "com.tcl.bi"})
check("disable.sh: a package that asks for TV_INPUT_HARDWARE stays on, is said, and is not in the ledger",
      rc == 0 and "com.tcl.bi" not in p.tv("sh")["off"].split() and led == ["com.tcl.logkit"] and "drive its HDMI/AV/tuner inputs" in out
      and not any(c.endswith(" com.tcl.bi") and "disable-user" in c for c in p.changes("sh")), (out, led, p.changes("sh")))
p, rc, out, led = run({"IME_PKGS": "com.tcl.logkit"})
check("disable.sh: a package that serves an input method (a keyboard) stays on", rc == 0 and "com.tcl.logkit" not in p.tv("sh")["off"].split() and led == ["com.tcl.bi"], (out, led))
p, rc, out, led = run({"NOCHK_PKGS": "com.tcl.bi"})
check("disable.sh: a dump that cannot be read stays on (fail closed: an input driver is never guessed)", rc == 0 and "com.tcl.bi" not in p.tv("sh")["off"].split() and led == ["com.tcl.logkit"], (out, led))
p, rc, out, led = run({"INPUT_PKGS": "com.tcl.bi com.tcl.logkit"}, listed=("com.tcl.bi", "com.tcl.logkit"))
check("disable.sh: every package of the list is an input driver: nothing is sent", rc == 0 and not any("disable-user" in c for c in p.changes("sh")), p.changes("sh"))
for knob, said in (("DUMP_HUNG", "hung system process"), ("SILENT_DUMP", "did not say which of these packages drive")):
    p, rc, out, led = run({knob: "yes"})
    check(f"disable.sh ({knob}): a !! line, a non-zero exit and NOT ONE disable command, nothing remembered",
          rc == 1 and "!! disable.sh" in out and said in out and p.changes("sh") == [] and not led, (rc, out, p.changes("sh"), led))
check("the hung case says how to restart the TV (tv.py --reboot, never a plain reboot)", "optimize/venv/bin/python -I optimize/tv.py --reboot" in sh)
p, rc, out, led = run({}, listed=("com.tcl.bi", "com.amazon.franktvinput"))
check("disable.sh: a list that names an input (inputs.txt) is refused whole, nothing changed", rc == 1 and "com.amazon.franktvinput" in out and p.changes("sh") == [], (rc, out))
prompt = (ROOT / "docs/SETUP-PROMPT.md").read_text(encoding="utf-8")
check("both new !! lines of disable.sh are in SETUP-PROMPT's table «When something goes wrong»",
      "`!! disable.sh: the TV did not say which of these packages drive its HDMI inputs" in prompt and "`!! disable.sh: the TV answers «Service dumps disabled due to hung system process»" in prompt)

# ---- every community list, through our filters (community.sh: keep.txt, core.txt, inputs.txt, launchers and setup wizards)
spec = importlib.util.spec_from_file_location("community", ROOT / "optimize/community.py"); com = importlib.util.module_from_spec(spec); spec.loader.exec_module(com)
PROTECTED = re.compile(r"(launcher|setupwraith|setupwizard|inputmethod|keyboard|tvinput|hdmi|tuner)", re.I)
lists = [f for f in sorted((ROOT / "optimize/lists/community").glob("*.txt")) if not f.name.startswith(("LICENSE", "README"))]
check(f"the community lists are the re-import from Clean Your TV ({len(lists)} lists, 400 or more)", len(lists) >= 400, len(lists))
bad = {}
for f in lists:
    for p in com.packages(str(f), KEEP):
        if any(re.fullmatch(r, p) for r in CORE) or is_input(p):
            continue
        if PROTECTED.search(p) or p in ("com.google.android.gms", "com.google.android.gsf", "com.android.vending"):
            bad.setdefault(p, []).append(f.stem)
check("no community list, after our filters, turns off a launcher, a setup wizard, a keyboard, an input, Play services or the Play Store", not bad, bad)
heads = [f.read_text(encoding="utf-8") for f in lists]
check("every community list credits Clean Your TV (MIT) and names its commit", all("CleanYourTV" in h and "MIT licence" in h and re.search(r"at commit [0-9a-f]{40}", h) for h in heads))
for p in ("com.tcl.xian.StartandroidService", "com.xiaomi.android.tvsetup.partnercustomizer"):
    check(f"{p} is protected (keep.txt): no list, ours or a community one, turns it off", p in KEEP and all(p not in com.packages(str(f), KEEP) for f in lists)
          and p not in words("tcl-safe.txt"))
check("com.tcl.partnercustomizer stays on the safe list, with why (our home answers the remote's app keys itself)",
      "com.tcl.partnercustomizer" in words("tcl-safe.txt") and "com.tcl.partnercustomizer" not in KEEP
      and "our home answers the remote's app keys itself" in (ROOT / "optimize/lists/tcl-safe.txt").read_text(encoding="utf-8"))
for p in ("com.amazon.tv.ooberesource", "com.oneplus.oobe.tv", "org.droidtv.hidwizard"):
    check(f"a first-run or pairing wizard ({p}) is never taken from a community list", bool(com.NOT_FOR_US.search(p)))
sys.exit(1 if failed else 0)
