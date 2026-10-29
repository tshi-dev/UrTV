"""Installing from the app (InstallRules.java, SetupInstall.java, SetupApks.java): a package that is not installed may be installed only when its APK
is signed with this launcher's own key (the same signer: yes; another one, or none: no); TV Bro and SmartTube are picked from their releases as
optimize/install.sh picks them (the same pattern, the TV's processor types first); the APK goes to `cmd package install -S` as a stream; what the run
installs is journaled before, Play Protect's check is switched off for one install only and always put back. No TV needed.

    python3 tests/setup_install_test.py
"""
import json, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

inst = (SETUP / "SetupInstall.java").read_text(encoding="utf-8")
apks = (SETUP / "SetupApks.java").read_text(encoding="utf-8")
sh = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
consts = {"TVBRO": "com.phlox.tvwebbrowser", "SMARTTUBE": "org.smarttube.stable"}
rows = [(r, consts[c], pat.replace("\\\\", "\\")) for r, c, pat in re.findall(r'\{"([^"]+)", (TVBRO|SMARTTUBE), "([^"]+)"\}', inst)]
check("the extras are install.sh's: the same repositories, packages and asset patterns", len(rows) == 2 and all(f"from_github {r} {p} '{pat}'" in sh for r, p, pat in rows)
      and f'TVBRO = "{consts["TVBRO"]}"' in inst and f'SMARTTUBE = "{consts["SMARTTUBE"]}"' in inst, rows)
check("the screensaver comes from our release (apk-latest), the file install.sh and «تحديث تلفزيونك» use", 'UpdateRules.BASE + "urtv-screensaver.apk"' in inst and "apk-latest/" in (SETUP.parent / "phone/UpdateRules.java").read_text())
check("downloads only from our release or the extras' own GitHub releases, over https, at most UpdateRules.MAX_APK_BYTES",
      "UpdateRules.isReleaseUrl(url)" in apks and '"https://github.com/" + x[0] + "/releases/download/"' in apks and "MAX_APK_BYTES" in apks and 'getProtocol().equals("https")' in apks)
check("an update goes by UpdateRules.verify, a fresh install by InstallRules.fresh, an extra by its package name",
      "UpdateRules.verify(pkg, apk.pkg" in inst and "InstallRules.fresh(pkg, apk.pkg, apks.selfSigners(), apk.signers)" in inst and "if (!pkg.equals(apk.pkg))" in inst)
check("the APK is streamed to `cmd package install -S <size>` (no file on the TV)", '"cmd package install " + flags + " -S " + apk.size' in inst)
s = inst[inst.index("SetupEngine.Result screensaver()"):inst.index("static String firstLine")]
check("a fresh install is journaled (installed-by-us.txt) BEFORE it is installed", s.index("remember(pkg);") < s.index("installWithVerifier(apk, pkg)"))
v = inst[inst.index("private String installWithVerifier"):inst.index("void putVerifierBack")]
check("Play Protect's ADB check: only after it refused, journaled first, put back in a finally", "INSTALL_FAILED_VERIFICATION_FAILURE" in v and v.index("store.put(VERIFIER_BEFORE") < v.index('" 0");')
      and "finally {\n            putVerifierBack();" in v)
check("never uninstalls to make room: another signer installed is said and left", "UPDATE_INCOMPATIBLE|signatures do not match" in inst and inst.count("pm uninstall") == 1
      and inst.index("pm uninstall") > inst.index("List<String> undo()"))
check("the undo removes only installed-by-us.txt's packages, and never this app", 'ours.remove(SetupEngine.SELF)' in inst)

if not shutil.which("javac"):
    print("SKIP the rules on a JVM: no javac")
    sys.exit(1 if failed else 0)
CLS = Path(tempfile.mkdtemp())
pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")] + [SETUP.parent / "phone/UpdateRules.java"]
r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(CLS), *map(str, pure), str(ROOT / "tests/InstallRun.java")], capture_output=True, text=True)
check("it compiles", r.returncode == 0, r.stderr)
if r.returncode:
    sys.exit(1)
got = dict(l.split(" ", 1) for l in subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.InstallRun", "rules"], capture_output=True, text=True).stdout.splitlines())
check("fresh install: the same signer as this app is allowed (one certificate, or the same set)", got.get("same") == "null" and got.get("sameTwo") == "null", got)
check("fresh install: another signer is refused (another key, a subset, a superset)", got.get("other") == got.get("subset") == got.get("superset") == "other_key", got)
check("fresh install: unsigned is refused (no certificate, none read, or this app's own unknown)", got.get("unsigned") == got.get("unsignedNull") == got.get("selfUnknown") == got.get("notHex") == "unsigned", got)
check("fresh install: another package is refused whatever its signer", got.get("otherApp") == got.get("noApp") == "other_app", got)

# pick_asset: install.sh's own Python, and the app's, on the same release lists
py = re.search(r"pick_asset\(\) \{.*?\"\$PY\" -I -c '\n(.*?)\n' \"\$1\" \"\$2\" <<< \"\$3\"", sh, re.S).group(1)
tmp = Path(tempfile.mkdtemp())
def release(*names):
    return json.dumps({"assets": [{"name": n, "size": 1000 + i, "uploader": {"login": "x"}, "browser_download_url": f"https://github.com/o/r/releases/download/v1/{n}"} for i, n in enumerate(names)]})
cases = [
    ("SmartTube_stable_[0-9.]+_{abi}\\.apk", "arm64-v8a,armeabi-v7a,armeabi", release("SmartTube_stable_29.1_armeabi-v7a.apk", "SmartTube_stable_29.1_arm64-v8a.apk", "SmartTube_stable_29.1_x86.apk")),
    ("SmartTube_stable_[0-9.]+_{abi}\\.apk", "armeabi-v7a,armeabi", release("SmartTube_stable_29.1_arm64-v8a.apk", "SmartTube_stable_29.1_armeabi-v7a.apk")),
    ("SmartTube_stable_[0-9.]+_{abi}\\.apk", "x86_64,x86", release("SmartTube_stable_29.1_universal.apk", "SmartTube_stable_29.1_arm64-v8a.apk")),
    ("SmartTube_stable_[0-9.]+_{abi}\\.apk", "mips", release("SmartTube_stable_29.1_arm64-v8a.apk")),
    ("SmartTube_stable_[0-9.]+_{abi}\\.apk", "", release("SmartTube_stable_29.1_arm64-v8a.apk", "SmartTube_stable_29.0_arm64-v8a.apk")),
    ("tvbro-[0-9.]+-generic-geckoExcluded\\.apk", "arm64-v8a", release("tvbro-2.0.1-generic-geckoIncluded.apk", "tvbro-2.0.1-generic-geckoExcluded.apk", "tvbro-2.0.1-generic-geckoExcluded.apk.sig")),
    ("tvbro-[0-9.]+-generic-geckoExcluded\\.apk", "arm64-v8a", release("tvbro-2.0.1-x.apk")),
]
diff = []
for i, (pat, abis, body) in enumerate(cases):
    f = tmp / f"r{i}.json"; f.write_text(body)
    want = subprocess.run([sys.executable, "-I", "-c", py, pat, abis], input=body, capture_output=True, text=True).stdout.split(" ")[0].strip() or "null"
    got_ = subprocess.run(["java", "-cp", str(CLS), "sa.urtv.home.setup.InstallRun", "pick", pat, abis, str(f)], capture_output=True, text=True).stdout.strip()
    if want != got_:
        diff.append((pat, abis, want, got_))
check(f"pick_asset: the app picks the very build install.sh picks ({len(cases)} release lists: the TV's processors first, universal, none)", not diff, diff)
sys.exit(1 if failed else 0)
