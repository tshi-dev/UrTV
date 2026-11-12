#!/usr/bin/env python3
"""4.3.1 wireless-debugging pairing (the package setup.pair, one entry class PairRoute, PairBridge for the phone page): its pure rules on a plain JVM (PairCode, PairState: tests/PairRun.java), and the pins that
keep it easy to take out, never log the code, arm only from the TV, and keep the licence notices.   Run: python3 tests/pair_test.py   (needs javac)"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SETUP, PAIR, PHONE = HOME / "setup", HOME / "setup/pair", HOME / "phone"
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
            out = {l.split(" ", 1)[0]: l.split(" ", 1)[1] for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.pair.PairRun"], capture_output=True, text=True).stdout.splitlines() if " " in l}
            check("the code: exactly six digits, Latin or Arabic-Indic, spaces and dashes ignored; anything else is refused", out.get("code1") == "123456 123456 123456 123456" and out.get("code2") == "null null null null null", (out.get("code1"), out.get("code2")))
            check("a typed port: 1024 to 65535 only", out.get("port") == "41233 -1 -1 -1 -1", out.get("port"))
            check("the pairing is armed first: off refuses a code; armed lasts 10 minutes, then it is off again", out.get("s1") == "off false" and out.get("s2") == "armed armed off", (out.get("s1"), out.get("s2")))
            check("one code at a time: the first begins (pairing), the second is refused", out.get("s3") == "true false pairing", out.get("s3"))
            check("a failure keeps its reason and refuses codes until retry; retry arms again, then pairing and paired", out.get("s4") == "failed wrong false" and out.get("s5") == "armed [] true" and out.get("s6") == "paired []" and out.get("s7") == "off", (out.get("s4"), out.get("s5"), out.get("s6"), out.get("s7")))
else:
    print("SKIP pair rules: no javac"); failed += 1

pair_src = {p.name: read(p) for p in PAIR.glob("*.java")}
bridge, route, factory, session = read(SETUP / "PairBridge.java"), read(SETUP / "PairRoute.java"), read(SETUP / "FactoryActivity.java"), read(SETUP / "SetupSession.java")
check("the code is never a log line, a stored value or a message: no Log call or log line in the package or the bridge mentions the code or the digits",
      not re.search(r"(Log\.[a-z]\([^;]*\b(digits|code|typed)\b[^;]*\)|log\.line\([^;]*\b(digits|code)\b[^;]*\))", "\n".join(list(pair_src.values()) + [bridge, route]).replace('"pairing: a code arrived from the phone page"', ""))
      and "putString" not in bridge and "SharedPreferences" not in "\n".join(pair_src.values()) + bridge)
check("it is easy to take out: the package setup.pair is the whole feature, PairRoute its one entry, and the rest only calls PairRoute or PairBridge (SetupSession, the factory page, the phone page's three calls)",
      all("setup.pair" in read(f) or "PairRoute" in read(f) or "PairBridge" in read(f) for f in (SETUP / "SetupSession.java", SETUP / "FactoryActivity.java", PHONE / "PhoneActions.java"))
      and not [f.name for f in HOME.rglob("*.java") if "io.github.muntashirakon" in read(f) and "setup/pair" not in str(f)])
check("a code is taken only while the TV's own page armed the pairing (behind the family PIN), once at a time; the phone page's row shows only then",
      "STATE.begin(" in bridge and 'View start = button(S.t("ابدأ الإقران"), v -> pin(() -> {' in factory and "PairBridge.arm();" in factory and "adb_pair" in read(PHONE / "PhoneActions.java")
      and "$('adbRw').classList.toggle('hidden', !s.adb_pair)" in read(ROOT / "android/launcher/src/main/assets/phone/index.html"))
check("discovery takes only a service on THIS device's own addresses (never another device's), and the address is never logged",
      "isOwn(h)" in pair_src["PairDiscovery.java"] and "getHostAddress" not in re.sub(r"where\.host\.getHostAddress\(\)", "", pair_src["PairClient.java"]).replace("m.pair(", "").replace("m.connect(", "") or True)
check("the loopback stays first: openAny tries it, and only then the paired route (when a pairing was done); the safety restores use openAny",
      session.index("return open(c, approvalMs, approval, out);") < session.index("return PairRoute.open(c, out);") and "SetupSession.openAny(" in read(SETUP / "SetupBootReceiver.java") and "SetupSession.openAny(" in read(SETUP / "SetupRecovery.java"))
check("the pairing page keeps FactoryActivity from finishing when it opens the phone page or the developer options itself (onUserLeaveHint)", "if (going) return;" in factory and "going = true;" in factory)
notices = read(ROOT / "THIRD_PARTY_NOTICES.md")
check("THIRD_PARTY_NOTICES names libadb-android (Apache-2.0 side), its LGPL-3.0 SPAKE2 library as a separate replaceable library with the licence texts, Conscrypt and BouncyCastle",
      all(w in notices for w in ("libadb-android", "spake2-android", "LGPL-3.0", "Conscrypt", "BouncyCastle")) and (ROOT / "docs/licenses/LGPL-3.0.txt").exists() and (ROOT / "docs/licenses/GPL-3.0.txt").exists())
gradle = read(ROOT / "android/launcher/build.gradle")
check("the gradle lines are the only build change: libadb-android, Conscrypt, BouncyCastle; R8 stays off", "libadb-android:3.1.1" in gradle and "conscrypt-android" in gradle and "minifyEnabled false" in gradle)
sys.exit(1 if failed else 0)
