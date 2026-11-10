#!/usr/bin/env python3
"""«المصادر» on any TV, not only TCL: TvVendor.java (no Android) picks the TV's live TV app and source picker from an ordered list (the
configured one first, so a TCL is unchanged), and MainActivity asks it each time the menu opens, tries the standard passthrough link, then
every live TV app the TV has, then the picker. A CultraView (com.cultraview.livetv, com.cultraview.setting/.SourceActivity) is the case
that was reported. Needs javac for the Java cases; without them they are skipped with a note.
Run: python3 tests/tv_vendor_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

vendor = (HOME / "TvVendor.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
manifest = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
config = (ROOT / "android/launcher/src/main/res/values/config.xml").read_text(encoding="utf-8")

check("TvVendor is pure: no android import", "import android" not in vendor)
known = re.findall(r'^\s+"([a-z0-9_.]+)(?:/[.\w]+)?",', vendor, re.M)
check("the lists name TCL first and CultraView's two apps", known[0] == "com.tcl.tv" and "com.cultraview.livetv" in known and "com.cultraview.setting" in known
      and '"com.tcl.suspension",' in vendor and '"com.cultraview.setting/.SourceActivity"' in vendor, known)
missing = [k for k in known if f'<package android:name="{k}" />' not in manifest]
check("every known package is in the manifest's <queries> (Android 11 hides it otherwise)", not missing, missing)
check("the configured defaults are unchanged (TCL's)", '<string name="live_tv_package" translatable="false">com.tcl.tv</string>' in config
      and '<string name="input_picker_package" translatable="false">com.tcl.suspension</string>' in config)
check("the configured value is tried first", "TvVendor.first(getString(R.string.live_tv_package), TvVendor.LIVE_TV" in main
      and "TvVendor.first(getString(R.string.input_picker_package), TvVendor.PICKERS" in main)
inputs = main.split("private void showInputsNow(")[1].split("\n    }\n")[0]
check("the menu asks again each time it opens, before its rows", inputs.index("findTvApps();") < inputs.index("rows.add("))
op = main.split("private void openPassthroughInput(")[1].split("\n    }\n")[0]
check("an input: the standard passthrough link first, then each live TV app the TV has, then the picker, then Settings",
      op.index("TvContract.buildChannelUriForPassthroughInput") < op.index("launch(base)") < op.index("TvVendor.usable(") < op.index("inputPickerPkg != null ? inputPickerPkg : liveTvPkg")
      < op.index("Settings.ACTION_SETTINGS"))
check("the fallbacks stay behind the family lock", "guarded(ParentalPolicy.INPUT_PICKER, TvVendor.pkg(fallback)" in op and "launchGuarded(ParentalPolicy.PILL_TV_SETTINGS" in op)
check("a picker named by its activity is opened by that activity", "TvVendor.cls(pkg)" in main.split("private void openPackage(")[1].split("\n    }\n")[0])
can = main.split("private boolean canOpen(")[1].split("\n    }\n")[0]
check("canOpen: installed, and the named activity resolves (or the app can be launched or viewed)", "isInstalled(pkg)" in can and "resolveActivity(" in can and "getLaunchIntentForPackage" in can)
check("tests/run.sh runs this test", "tests/tv_vendor_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

# ---- GitHub #54 (TCL G10, Android 12: «Live TV» and «TV» in the Sources row opened the LAST HDMI input): the tuner is found by its type, never as «the last input used»,
# and where the live TV app only resumes the last input the TV's own source picker opens instead
check("the Sources row's tuner is not opened as a passthrough link: its own action (openLiveTv), by the input's type", "t.getType() == TvInputInfo.TYPE_TUNER) openLiveTv(); else openPassthroughInput(t);" in main)
check("«البث التلفزيوني» and the remote's TV button open the tuner the same way (openLiveTv: the live TV app, or the picker where the app resumes the last input, else Settings), behind the same locks",
      "rows.add(inputRow(S.t(\"البث التلفزيوني\"), () -> openLiveTv()));" in main and "case RemoteKeys.OPEN_LIVE_TV:" in main and "TvVendor.tvStep(TvVendor.pkg(liveTvPkg), inputPickerPkg != null, liveTvPkg != null)" in main
      and "guarded(ParentalPolicy.INPUT_PICKER, TvVendor.pkg(inputPickerPkg)" in main.split("private void openLiveTv()")[1].split("\n    }\n")[0])
check("TCL's live TV app is the one that resumes the last input", '"com.tcl.tv"' in vendor.split("RESUMES_LAST_INPUT")[1].split("};")[0])
tail_cases = True

if not shutil.which("javac"):
    print("SKIP tv vendor: no javac here")
    sys.exit(1 if failed else 0)
cases = [   # configured live, configured picker, what the TV can open -> live|picker|usable live TVs
    (["com.tcl.tv", "com.tcl.suspension", "com.tcl.tv", "com.tcl.suspension"], "com.tcl.tv|com.tcl.suspension|com.tcl.tv"),
    (["com.tcl.tv", "com.tcl.suspension", "com.cultraview.livetv", "com.cultraview.setting/.SourceActivity"],
     "com.cultraview.livetv|com.cultraview.setting/.SourceActivity|com.cultraview.livetv"),
    (["com.tcl.tv", "com.tcl.suspension", "com.cultraview.livetv", "com.cultraview.setting"], "com.cultraview.livetv|none|com.cultraview.livetv"),
    (["com.tcl.tv", "com.tcl.suspension", "com.mediatek.wwtv.tvcenter", "com.google.android.tv"],
     "com.mediatek.wwtv.tvcenter|none|com.mediatek.wwtv.tvcenter,com.google.android.tv"),
    (["com.tcl.tv", "com.tcl.suspension"], "none|none|"),
    (["-", "-", "org.droidtv.playtv"], "org.droidtv.playtv|none|org.droidtv.playtv"),
    (["com.example.tv", "com.example.src/.Pick", "com.example.tv", "com.example.src/.Pick", "com.tcl.tv"],
     "com.example.tv|com.example.src/.Pick|com.example.tv,com.tcl.tv"),
    (["com.tcl.tv", "com.tcl.suspension", "boom", "com.tcl.tv", "com.sony.dtv.tvx"], "com.sony.dtv.tvx|none|com.sony.dtv.tvx"),
]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "TvVendor.java"), str(ROOT / "tests/TvVendorRun.java")], check=True, capture_output=True)
    for args, want in cases:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.TvVendorRun", *args], check=True, capture_output=True, text=True).stdout.strip()
        check(f"TvVendor on {' '.join(args[2:]) or 'a TV with none'}", got == want, f"{got!r} != {want!r}")
    src = ('package sa.urtv.home; public final class TvVendorCls { public static void main(String[] a) { System.out.println('
           'TvVendor.pkg("com.cultraview.setting/.SourceActivity") + "|" + TvVendor.cls("com.cultraview.setting/.SourceActivity") + "|" + '
           'TvVendor.cls("a.b/c.d.E") + "|" + TvVendor.cls("com.tcl.suspension") + "|" + TvVendor.pkg(null) + "|" + TvVendor.cls("a.b/")); } }')
    (Path(tmp) / "TvVendorCls.java").write_text(src)
    subprocess.run(["javac", "-d", tmp, "-cp", tmp, str(Path(tmp) / "TvVendorCls.java")], check=True, capture_output=True)
    got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.TvVendorCls"], check=True, capture_output=True, text=True).stdout.strip()
    check("pkg/cls: a leading dot is the package's own, a full name stays, a bare package has no class",
          got == "com.cultraview.setting|com.cultraview.setting.SourceActivity|c.d.E|null||null", got)

    run = ('package sa.urtv.home; public final class TvTuner { public static void main(String[] a) { '
           '/* TCL G10 shape: three HDMI, the tuner last in the list; the last input used was an HDMI (index 1) */ '
           'int[] g10 = {TvVendor.TYPE_HDMI, TvVendor.TYPE_HDMI, TvVendor.TYPE_HDMI, TvVendor.TYPE_TUNER}; int lastUsed = 1; '
           'int[] tunerFirst = {TvVendor.TYPE_TUNER, TvVendor.TYPE_HDMI}; int[] none = {TvVendor.TYPE_HDMI, TvVendor.TYPE_HDMI}; '
           'System.out.println(TvVendor.tunerIndex(g10) + "|" + (TvVendor.tunerIndex(g10) != lastUsed) + "|" + TvVendor.tunerIndex(tunerFirst) + "|" + TvVendor.tunerIndex(none) + "|" + TvVendor.tunerIndex(new int[0]) + "|" + '
           'TvVendor.tvStep("com.tcl.tv", true, true) + "|" + TvVendor.tvStep("com.tcl.tv", false, true) + "|" + TvVendor.tvStep("com.cultraview.livetv", true, true) + "|" + '
           'TvVendor.tvStep("com.mediatek.wwtv.tvcenter", false, true) + "|" + TvVendor.tvStep("", true, false) + "|" + TvVendor.tvStep("", false, false)); } }')
    (Path(tmp) / "TvTuner.java").write_text(run)
    subprocess.run(["javac", "-d", tmp, "-cp", tmp, str(Path(tmp) / "TvTuner.java")], check=True, capture_output=True)
    got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.TvTuner"], check=True, capture_output=True, text=True).stdout.strip()
    check("TCL G10 shape (tuner and 3 HDMI, last used an HDMI): the tuner is found by its type (index 3), never the last input; none or an empty list gives -1",
          got.split("|")[:5] == ["3", "true", "0", "-1", "-1"], got)
    check("the plan: TCL's app with a picker on the TV: the picker; TCL's app with no picker: the app; the other vendors' apps unchanged; no app: the picker, else Settings",
          got.split("|")[5:] == ["picker", "live", "live", "live", "picker", "settings"], got)

print("\n" + (f"{failed} failed" if failed else "tv vendor: all passed"))
sys.exit(1 if failed else 0)
