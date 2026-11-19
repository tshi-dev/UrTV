#!/usr/bin/env python3
"""4.4 beta 3, wireless debugging (the owner: every Google TV and Chromecast with only «تصحيح الأخطاء اللاسلكي»), ported from the 4.3.1 pairing (work/pair-431) with the owner's
rule that the libraries live in the ADD-ON only: the pure rules on a plain JVM (PairCode, PairState: tests/PairRun.java), then the pins: the libraries in the add-on's build only,
pinned by SHA-256; the home reaches them only through the add-on's signature-guarded provider; 5555 stays first; it fails closed with a word; the code is never logged; the
licence notices.   Run: python3 tests/pair_test.py   (needs javac)"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SETUP = HOME / "setup"
XTRA = ROOT / "android/extras/src/main/java/sa/urtv/home"
PAIR = XTRA / "pair"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(PAIR / "PairCode.java"), str(PAIR / "PairState.java"), str(ROOT / "tests/PairRun.java")], capture_output=True, text=True)
        check("PairCode and PairState compile on a plain JVM (no Android in them)", r.returncode == 0, r.stderr[-500:])
        if r.returncode == 0:
            out = {l.split(" ", 1)[0]: l.split(" ", 1)[1] for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.pair.PairRun"], capture_output=True, text=True).stdout.splitlines() if " " in l}
            check("the code: exactly six digits, Latin or Arabic-Indic, spaces and dashes ignored; anything else is refused", out.get("code1") == "123456 123456 123456 123456" and out.get("code2") == "null null null null null", (out.get("code1"), out.get("code2")))
            check("a typed port: 1024 to 65535 only", out.get("port") == "41233 -1 -1 -1 -1", out.get("port"))
            check("the pairing is armed first: off refuses a code; armed lasts 10 minutes, then it is off again", out.get("s1") == "off false" and out.get("s2") == "armed armed off", (out.get("s1"), out.get("s2")))
            check("one code at a time: the first begins (pairing), the second is refused", out.get("s3") == "true false pairing", out.get("s3"))
            check("a failure keeps its reason and refuses codes until retry; retry arms again, then pairing and paired", out.get("s4") == "failed wrong false" and out.get("s5") == "armed [] true" and out.get("s6") == "paired []" and out.get("s7") == "off", (out.get("s4"), out.get("s5"), out.get("s6"), out.get("s7")))
else:
    print("SKIP pair rules: no javac"); failed += 1

# the owner's rule: the libraries in the ADD-ON only
lgr, xgr, agr = read(ROOT / "android/launcher/build.gradle"), read(ROOT / "android/extras/build.gradle"), read(ROOT / "android/app/build.gradle")
libs = ("libadb-android", "conscrypt", "bouncycastle", "spake2")
check("the libraries are in the add-on's build only: exact versions there, none in the home's or the screensaver's",
      "'com.github.MuntashirAkon:libadb-android:3.1.1'" in xgr and "'org.conscrypt:conscrypt-android:2.5.3'" in xgr and "'org.bouncycastle:bcprov-jdk15to18:1.81'" in xgr and "'org.bouncycastle:bcpkix-jdk15to18:1.81'" in xgr
      and not any(l in (lgr + agr).lower() for l in libs))
check("no home or screensaver source imports them (the home reaches them only through the add-on)",
      not [str(f) for d in (HOME, ROOT / "android/app/src") for f in d.rglob("*.java") if re.search(r"io\.github\.muntashirakon|org\.conscrypt|org\.bouncycastle", read(f))])
check("every library file is pinned by SHA-256 before the add-on builds (verifyPins on preBuild; a new, missing or changed file stops the build)",
      "preBuild.dependsOn verifyPins" in xgr and "MessageDigest.getInstance('SHA-256')" in xgr and "throw new GradleException" in xgr and (ROOT / "android/extras/pins.txt").exists())
sett = read(ROOT / "android/settings.gradle")
check("JitPack is used for libadb-android's group only", "maven { url 'https://jitpack.io'; content { includeGroupByRegex 'com\\\\.github\\\\.MuntashirAkon.*' } }" in sett)
check("android_compile.sh compiles the add-on with exactly the pinned files (SHA-256 checked, a wrong one a FAIL) or says SKIP; never a silent pass",
      "android/extras/pins.txt" in read(ROOT / "tests/android_compile.sh") and "does not have its pinned SHA-256" in read(ROOT / "tests/android_compile.sh") and "SKIP android compile: the extras module" in read(ROOT / "tests/android_compile.sh"))

# the add-on side
prov, host = read(XTRA / "ExtrasCallProvider.java"), read(PAIR / "PairHost.java")
xman = read(ROOT / "android/extras/src/main/AndroidManifest.xml")
check("the add-on's provider is signature-guarded, and each pairing call goes through PairHost",
      'android:permission="sa.urtv.permission.EXTRAS"' in xman and 'android:protectionLevel="signature"' in xman and all(f"case ExtrasWords.{m}:" in prov for m in ("PAIR_ARM", "PAIR_CODE", "PAIR_STATUS", "PAIR_OPEN", "PAIR_RUN", "PAIR_EXEC", "PAIR_CLOSE", "PAIR_FORGET")))
check("an install's file is read only from the home's own one-time file provider", '"sa.urtv.home.pairfiles".equals(from.getAuthority())' in prov)
check("the pairing runs on the add-on's own thread, one code at a time, only while armed", '"extras-pair").start();' in host and "STATE.begin(System.currentTimeMillis())" in host)
check("a failed open, run or exec answers ERROR (never an empty result)", prov.count("out.putString(ExtrasWords.ERROR,") >= 3)
code_logs = re.findall(r"Log\.[a-z]\(TAG,[^;]*\);", host) + re.findall(r"log\.line\([^;]*\);|Log\.[a-z]\([^;]*\);", "".join(read(p) for p in PAIR.glob("*.java")))
_leaks = [l for l in code_logs if re.search(r"\b(digits|typed|command|getHostAddress|host)\b", re.sub(r'"[^"]*"', '""', l))]   # the variables, not the fixed words
check("the code, an address or a command's text is never a log line (fixed words and ports only)", not _leaks, _leaks)

# the home side
route, sess, files = read(SETUP / "PairRoute.java"), read(SETUP / "SetupSession.java"), read(SETUP / "PairFiles.java")
check("5555 stays first: openAny tries the TV's own adb, and only when it fails the paired route", sess.index("return open(c, approvalMs, approval, out);") < sess.index("return PairRoute.open(c, out);"))
check("fails closed: when neither answers, the 5555 reason is kept and the paired route's word added (no_addon, old_addon, old_android, not_paired, no_port, refused)",
      'throw new IOException(first.getMessage() + "; paired route: " + why);' in sess and 'throw new IOException(first.getMessage() + "; paired route: " + second.getMessage());' in sess
      and all(w in route for w in ('"no_addon"', '"old_addon"', '"old_android"', '"not_paired"', '"paired"')))
check("the whole run, its restores and the home's start hooks go through openAny (the paired route where 5555 does not answer)",
      "SetupSession.openAny(app, approvalMs" in read(SETUP / "SetupHost.java") and "SetupSession.openAny(" in read(SETUP / "SetupBootReceiver.java") and "SetupSession.openAny(" in read(SETUP / "SetupRecovery.java")
      and "SetupSession.open(" not in read(SETUP / "SetupHook.java") and "s.adb" not in read(SETUP / "SetupHost.java"))
check("the one-time file: random token, read only, deleted right after the install; signature-guarded",
      "UUID.randomUUID()" in files and 'if (!"r".equals(mode))' in files and "PairFiles.drop(c, token);" in route and 'android:name=".setup.PairFiles"' in read(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
check("the add-on's key goes with ours (dropKey)", "PairRoute.forget(c);" in sess)

notices = read(ROOT / "THIRD_PARTY_NOTICES.md")
check("THIRD_PARTY_NOTICES: libadb-android (Apache-2.0 side), its LGPL-3.0 SPAKE2 library as a separate replaceable library with where its source is, Conscrypt, BouncyCastle; the licence texts",
      all(w in notices for w in ("libadb-android", "spake2-android", "LGPL-3.0", "Conscrypt", "BouncyCastle", "github.com/MuntashirAkon/spake2-java", "android/extras/build.gradle")) and (ROOT / "docs/licenses/LGPL-3.0.txt").exists() and (ROOT / "docs/licenses/GPL-3.0.txt").exists())
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
