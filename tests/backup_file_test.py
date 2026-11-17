#!/usr/bin/env python3
"""4.5 «نسخة الإعدادات» (docs/BACKUP.md): the home's part of the schema-2 file. BackupFile's allow-list, checks and preview run in
tests/BackupFileRun.java; here: no Android in it, and its copy of the app-name rule is AppNames.valid's.
Run: python3 tests/backup_file_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
bf, an = (H / "BackupFile.java").read_text(encoding="utf-8"), (H / "AppNames.java").read_text(encoding="utf-8")
check("no Android in BackupFile", "import android" not in bf)
check("its app-name length is AppNames.MAX_LEN", re.search(r"MAX_LEN = (\d+);", an).group(1) == re.search(r"APP_NAME_MAX = (\d+);", bf).group(1))
check("its Arabic ranges are AppNames.hasArabic's", "(ch >= 0x0600 && ch <= 0x06FF) || (ch >= 0x0750 && ch <= 0x077F) || (ch >= 0xFB50 && ch <= 0xFEFC)" in an
      and "(ch >= 0x0600 && ch <= 0x06FF) || (ch >= 0x0750 && ch <= 0x077F) || (ch >= 0xFB50 && ch <= 0xFEFC)" in bf)
check("the PIN, Wi-Fi, keys, tasks and photos are not keys of the home part", not any(k in bf.split("HOME_KEYS = {")[1].split("};")[0] for k in ("pin", "wifi", "adb", "task", "photo")))
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(H / f) for f in ("BackupFile.java", "KeyApps.java", "InputHide.java", "InputNames.java")]
                       + [str(ROOT / "tests/BackupFileRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.BackupFileRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every BackupFile case ran", "ALL PASS" in out, out[-300:])
sys.exit(1 if failed else 0)
