#!/usr/bin/env python3
"""The PIN recovery code (PinRecovery, PinGate, PinKeys; AGENTS.md «PIN recovery»), with a test-only code and a record this test makes:
the code typed where the PIN is asked opens «رمز جديد» and counts as nothing, the locks, allowed apps and kids mode stay, a 4-digit PIN
works as before, a wrong longer entry is a wrong try, a build with no record has no recovery code at all, and the code is never in what
is built (only its salted record). Also: nothing on screen or in the people's docs speaks of it. Needs javac; without it, the static part only.
  Run: python3 tests/pin_recovery_test.py"""
import importlib.util, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

spec = importlib.util.spec_from_file_location("make_pin_recovery", ROOT / ".github/tools/make_pin_recovery.py")
tool = importlib.util.module_from_spec(spec); spec.loader.exec_module(tool)
CODE = "4827193"   # test-only; the real code is in no file
record = tool.make(CODE, rounds=10000)
check("the tool makes «1:rounds:salt:hash», salted", re.fullmatch(r"1:10000:[0-9a-f]{32}:[0-9a-f]{64}", record) and tool.make(CODE, rounds=10000) != record, record)
check("the record does not hold the code", CODE not in record)
check("the tool reads its own record", tool.matches(CODE, record) and not tool.matches("4827194", record))
try:
    tool.make("1234"); check("the tool refuses 4 digits (that is a PIN)", False)
except ValueError:
    check("the tool refuses 4 digits (that is a PIN)", True)

src = {f.name: f.read_text(encoding="utf-8") for f in HOME.glob("Pin*.java")}
gradle = (ROOT / "android/launcher/build.gradle").read_text(encoding="utf-8")
check("the build reads only the record (URTV_PIN_RECOVERY or the untracked android/pin-recovery.local) and refuses a malformed one",
      "System.getenv('URTV_PIN_RECOVERY')" in gradle and "pin-recovery.local" in gradle and "GradleException" in gradle)
check("android/pin-recovery.local is untracked", "android/pin-recovery.local" in (ROOT / ".gitignore").read_text(encoding="utf-8"))
check("CI gives the build the record from a secret", "secrets.PIN_RECOVERY_RECORD" in (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8"))
check("the recovery check is PBKDF2 with a constant-time compare", "MessageDigest.isEqual" in src["PinCrypto.java"] and "PBKDF2WithHmacSHA256" in src["PinCrypto.java"])
check("no screen words in the recovery code's files", not any("S.t(" in src[n] for n in ("PinRecovery.java", "PinGate.java", "PinKeys.java")))
prompt = src["PinPrompt.java"]
check("the keypad still shows four dots, no longer placeholder", "i < 4; i++" in prompt and not re.search(r"[○●](\s[○●]){4,}", prompt))
people = ["README.md", "CHANGELOG.md", "docs/SETTINGS.md", "docs/FEATURES.md", "docs/PHONE-SETUP.md"] + [str(p.relative_to(ROOT)) for p in (ROOT / "i18n").rglob("*.tsv")] + \
         [str(p.relative_to(ROOT)) for p in (ROOT / "android/launcher/src/main/assets").rglob("*") if p.is_file() and p.suffix in (".html", ".js", ".tsv")]
said = [p for p in people if (ROOT / p).exists() and re.search(r"(?i)recovery|PIN_RECOVERY|استرداد|استعادة الرمز", (ROOT / p).read_text(encoding="utf-8", errors="replace"))]
check("the people's docs, the translations and the phone page do not speak of it", not said, said)

if not shutil.which("javac"):
    print("SKIP pin recovery: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    t = Path(tmp)
    (t / "stub/sa/urtv/home").mkdir(parents=True)
    # what Gradle generates with the record: the only place the build holds it
    (t / "stub/sa/urtv/home/BuildConfig.java").write_text('package sa.urtv.home;\npublic final class BuildConfig { public static final String PIN_RECOVERY = "%s"; }\n' % record)
    (t / "cls").mkdir()
    files = [str(HOME / n) for n in ("PinStore.java", "PinCrypto.java", "PinRecovery.java", "PinGate.java", "PinKeys.java")]
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(t / "cls"), *files, str(t / "stub/sa/urtv/home/BuildConfig.java")], check=True)
    built = b"".join(p.read_bytes() for p in (t / "cls").rglob("*.class"))
    check("the record is in what is built", record.encode() in built)
    check("the code is not in what is built (neither as text nor UTF-16)", CODE.encode() not in built and CODE.encode("utf-16-be") not in built)
    subprocess.run(["javac", "-encoding", "UTF-8", "-cp", str(t / "cls"), "-d", str(t / "cls"), str(ROOT / "tests/PinRecoveryRun.java")], check=True)
    got = dict(l.split("=", 1) for l in subprocess.run(["java", "-cp", str(t / "cls"), "sa.urtv.home.PinRecoveryRun", record, CODE],
                                                       check=True, capture_output=True, text=True).stdout.splitlines())
want = {
    "on": "true", "on_empty": "false", "on_garbage": "false",
    "matches": "true", "matches_other": "false", "matches_no_record": "false",   # the app reads the tool's record; nothing without one
    "recover": "RECOVER",                                    # the code opens «رمز جديد»
    "recover_fails": "0", "recover_wait": "0",               # and never counts, however often
    "pin4_right": "OK", "pin4_wrong": "WRONG", "pin4_wrong_fails": "1",   # a 4-digit PIN as before
    "long_wrong": "WRONG", "long_wrong_fails": "1", "long_wrong_locks": "true",   # a wrong longer entry is a wrong try, and locks
    "recover_while_locked": "RECOVER",
    "after_new_pin": "OK", "after_old_pin": "WRONG",
    "kept_locked": "com.example.a,@wifi", "kept_allowed": "com.example.kids", "kept_kids": "true",   # only the PIN is replaced
    "plain_kept": "false",
    "none_long": "WRONG", "none_long_fails": "1",           # no record: no recovery code at all
    "look_wrong": "false", "look_right": "true", "look_fails": "1",   # a look is a try, counted when made (R01)
    "probed_wrong": "WRONG", "probed_wrong_fails": "1", "probed_right": "OK", "probed_right_fails": "0",
    "submit_twice": "LOCKED 1",
    "back_loop": "....." + "L" * 15 + " 5 5",   # Back/reopen: 20 abandoned wrong looks lock after 5
    "look_while_locked": "false 5", "late_look": "false 1", "cancel_extended": "2",
    "extended_submitted": "WRONG 3", "recover_refund": "RECOVER 3",
    "recover_refund_lock": "true RECOVER 4 false", "submit_dismiss": "1",
    "keys4": "00010 1234",                                   # no recovery: on the fourth digit, a fifth not taken
    "keys8_pin": "00021 1234 4",
    "keys8_wrong4": "01 1111", "keys8_slow_look": "01 1111",
    "keys8_long": "0002" + "3" * (len(CODE) - 4) + "001 true 4",   # the screen still shows four dots
    "keys8_eight": "000233310 12345678", "keys8_erase": "31 1111",
}
for k, v in want.items():
    check(f"pin recovery: {k} = {v}", got.get(k) == v, got.get(k))
print("\n" + (f"{failed} failed" if failed else "pin recovery: all passed"))
sys.exit(1 if failed else 0)
