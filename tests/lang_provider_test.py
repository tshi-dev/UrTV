#!/usr/bin/env python3
"""4.3: «اللغة» for UrTV Player (com.urtv.player, another key): content://sa.urtv.home.lang/lang, one row {lang: ar|en}, read only, behind the
normal-level permission sa.urtv.permission.READ_LANG declared by the home app; nothing else exposed. LangRule runs for real (tests/LangRuleRun.java).
    python3 tests/lang_provider_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
mf = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
prov = (H / "LangProvider.java").read_text(encoding="utf-8")
m = re.search(r'<provider\s+android:name="\.LangProvider"(.*?)/>', mf, re.S)
check("declared: authority sa.urtv.home.lang, exported, one permission for read and write", m and 'android:authorities="sa.urtv.home.lang"' in m.group(1)
      and 'android:exported="true"' in m.group(1) and 'android:permission="sa.urtv.permission.READ_LANG"' in m.group(1)
      and "readPermission" not in m.group(1) and "grantUriPermissions" not in m.group(1), m and m.group(1))
check("the permission is ours, normal level", re.search(r'<permission\s+android:name="sa.urtv.permission.READ_LANG"\s+android:protectionLevel="normal"\s*/>', mf))
check("one column «lang», one row, only for the one URI; no write of any kind",
      'new MatrixCursor(new String[] {COLUMN}, 1)' in prov and 'COLUMN = "lang"' in prov and "LangRule.isLangUri(" in prov
      and prov.count('throw new UnsupportedOperationException("read only")') == 3 and "addRow(new Object[] {LangRule.code(S.lang())})" in prov)
check("nothing else read or exposed (no prefs of its own, no file, no other provider call)", not re.search(r"getSharedPreferences|openFile|ContentResolver|getString\(", prov))
check("docs/SETTINGS.md says it", "content://sa.urtv.home.lang/lang" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP the rule's run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "LangRule.java"), str(ROOT / "tests/LangRuleRun.java")], capture_output=True, text=True)
        check("LangRule compiles on a plain JVM", r.returncode == 0, r.stderr[-300:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.LangRuleRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            check("LangRule: every case", len(lines) == 3 and all(v == "ok" for _, v in lines), out)
sys.exit(1 if failed else 0)
