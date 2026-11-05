#!/usr/bin/env python3
"""«حماية العائلة (DNS)» (FamilyDns.java, the launcher): nothing changes without the grant or a route, the originals are recorded first and
put back exactly (a value that was not set is deleted), a broken internet or a value that is not taken reverts at once, a second "on" keeps
the first record, an unreadable value is never recorded as "not set", Private DNS must validate (not cached DNS), a crashed check is recovered, API < 28 is unsupported,
"off" leaves a value someone else changed. Needs javac.
Run: python3 tests/family_dns_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if not shutil.which("javac"):
    print("SKIP family dns: no javac here")
    sys.exit(0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/FamilyDns.java"),
                    str(ROOT / "tests/FamilyDnsRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.FamilyDnsRun"],
                         check=True, capture_output=True, text=True, encoding="utf-8").stdout.splitlines()
check("one place for the host and the port", out[0] == "family.adguard-dns.com 853", out[0])
check("no grant: NEEDS_GRANT, nothing written or recorded", out[1] == "NEEDS_GRANT 0 opportunistic/null true", out[1])
check("no route to the service: NO_ROUTE, nothing changed, nothing recorded", out[2] == "NO_ROUTE 0 opportunistic/null true", out[2])
check("on then off: the exact originals are back, the record is gone", out[3] == "ENABLED hostname/family.adguard-dns.com RESTORED hostname/dns.example true", out[3])
check("originals that were not set are deleted on off, not written as defaults", out[4] == "hostname/family.adguard-dns.com false false true", out[4])
check("internet breaks: REVERTED at once, a null original is deleted", out[5] == "REVERTED off/null false true", out[5])
check("a second on keeps the first record (not our own values); off puts the real originals back",
      out[6] == "ENABLED opportunistic x.example" and out[7] == "opportunistic/x.example", out[6] + " / " + out[7])
check("an unreadable value is not recorded as null: nothing written, no record", out[8] == "UNREADABLE_VALUE 0 true", out[8])
check("a value that is not taken: NOT_TAKEN, originals back, record cleared", out[9] == "NOT_TAKEN opportunistic/keep.example true", out[9])
check("off with no record: NOTHING_TO_RESTORE and nothing touched", out[10] == "NOTHING_TO_RESTORE 0 hostname/other.example", out[10])
check("status: off, on (by us), unknown when unreadable; on by someone else is left alone",
      out[11] == "off (none) | on (us) | unknown (none) | on (other) | ALREADY_ON_BY_OTHER 0 true", out[11])
check("a record that cannot be saved: nothing written", out[12] == "NOT_RECORDED 0", out[12])
check("Arabic copy for the row", out[13] == "ما قدرنا نوصل لخدمة الحماية، ما تغيّر شي | الإنترنت وقف فرجّعنا الإعداد القديم | يحتاج تفعيل من الكمبيوتر", out[13])
check("API below 28: UNSUPPORTED, nothing written, status never ON, supported=no",
      out[14] == "UNSUPPORTED 0 true false off (none) true", out[14])
check("Private DNS never validated (cached DNS): REVERTED, originals back, record and flag cleared", out[15] == "REVERTED opportunistic/x.example true 0", out[15])
check("the check flag is saved before the first write and cleared after ENABLED", out[16] == "ENABLED 111 0 true", out[16])
check("recover: flag set -> originals back, flag and record cleared; no flag -> nothing; flag without record -> flag cleared",
      out[17] == "RESTORED opportunistic/null false 0 true | NOTHING_TO_RESTORE 0 | NOTHING_TO_RESTORE 0 0", out[17])
check("off after someone else changed the value: CHANGED_BY_SOMEONE_ELSE, no write, record dropped",
      out[18] == "CHANGED_BY_SOMEONE_ELSE 0 hostname/mine.example true", out[18])
check("Arabic copy for unsupported and changed-elsewhere",
      out[19] == "غير مدعومة في هذا التلفزيون (أندرويد قديم) | الإعداد تغيّر من مكان آخر، تركناه كما هو", out[19])
parts = out[20].split(" | ")
check("fresh name: fast NXDOMAIN or an address continues; timeout, slow NXDOMAIN, other failure -> REVERTED, originals back; not asked before Private DNS is active",
      parts[0] == parts[1] == "ENABLED hostname/family.adguard-dns.com false0"
      and all(x == "REVERTED opportunistic/x.example true0" for x in parts[2:5]) and parts[5] == "0", out[20])
check("UnknownHostException: only a GaiException EAI_NODATA/EAI_NONAME cause is «no such name»; any other cause is a failure",
      out[21] == "NOT_FOUND NOT_FOUND FAILED FAILED", out[21])
check("validated Private DNS servers: none validated is not working; a TV that cannot say is not refused", out[22] == "false true true", out[22])
check("fresh name retries: two fast failures then a slow (4.5 s) answer is ENABLED after waiting out the slots; never answering = 3 tries then REVERTED; an answer at once = 1 lookup",
      out[27] == "ENABLED 3 11800 | REVERTED 3 opportunistic/x.example true 0 | ENABLED 1 0", out[27])
check("budget: INTERNET_MS 40 s = 12 s Private DNS + 3 x 6 s fresh names + 10 s internet; the resolver's own «no such name» text counts, a temporary failure does not",
      out[28] == "40000 40000 10000 | true true false false", out[28])
android = (ROOT / "android/launcher/src/main/java/sa/urtv/home/FamilyDnsAndroid.java").read_text(encoding="utf-8")
check("last=RUNNING is written before the worker thread starts",
      0 <= android.find('putString(K_LAST, "RUNNING")') < android.find('worker.start()'))
check("the fresh name is a random label under connectivitycheck.gstatic.com resolved with InetAddress.getByName",
      '".connectivitycheck.gstatic.com"' in android and "InetAddress.getByName(name)" in android and "SecureRandom" in android)
src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/FamilyDns.java").read_text(encoding="utf-8")
check("FamilyDns has no Android imports", "import android" not in src)
import re
lm = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
act = re.search(r"<activity[^>]*FamilyDnsActivity.*?/>", lm, re.S).group(0)
check("manifest: FamilyDnsActivity needs the signature permission sa.urtv.home.FAMILY_DNS, which is declared and held by the screensaver",
      'android:permission="sa.urtv.home.FAMILY_DNS"' in act
      and re.search(r'<permission\s+android:name="sa.urtv.home.FAMILY_DNS"\s+android:protectionLevel="signature"', lm)
      and '<uses-permission android:name="sa.urtv.home.FAMILY_DNS"' in (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
dr = (ROOT / "android/launcher/src/main/java/sa/urtv/home/DockReceiver.java").read_text(encoding="utf-8")
check("DockReceiver answers: off at once, on 'started (on)', status ends with supported=", 'disableNow(c).name()' in dr and '"family-dns: started (on)"' in dr and '"; supported="' in dr)
check("manifest: the permission is declared and DockReceiver takes FAMILY_DNS",
      "android.permission.WRITE_SECURE_SETTINGS" in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
      and 'sa.urtv.home.FAMILY_DNS' in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
check("the check fails and the old values cannot go back: RESTORE_FAILED (not REVERTED), our values still on, record and flag kept",
      out[23] == "RESTORE_FAILED hostname/family.adguard-dns.com true true", out[23])
check("the partial write that cannot be undone: RESTORE_FAILED, record and flag kept", out[24] == "RESTORE_FAILED true true", out[24])
check("then recover() puts the old values back and clears the record and the flag", out[25] == "RESTORED opportunistic/x.example true 0", out[25])
check("when the old values did go back (read back) it is still REVERTED / NOT_TAKEN, with the record and the flag cleared",
      out[26] == "REVERTED true 0 | NOT_TAKEN opportunistic/x.example true 0", out[26])
sys.exit(1 if failed else 0)
