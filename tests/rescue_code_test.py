#!/usr/bin/env python3
"""4.4: the rescue app's Downloader code (made by the owner for the apk-latest link of urtv-rescue.apk) is the same in the setup's «قبل ما نبدأ: لو
صار شيء» page (RescueInfo.DOWNLOADER_CODE, shown in step 2 once it is set) and in docs/RESCUE.md, Arabic and English.
Run: python3 tests/rescue_code_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

info = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/RescueInfo.java").read_text(encoding="utf-8")
code = re.search(r'DOWNLOADER_CODE = "([^"]*)";', info).group(1)
check("RescueInfo has the code, digits as hasCode() wants them", code == "8042301" and re.fullmatch(r"[0-9]{4,10}", code), code)
act = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupActivity.java").read_text(encoding="utf-8")
check("the setup's «لو صار شيء» page shows it in step 2", "RescueInfo.hasCode() ? S.tv(RescueInfo.STEP2_CODE, RescueInfo.DOWNLOADER_CODE)" in act)
doc = (ROOT / "docs/RESCUE.md").read_text(encoding="utf-8")
ar, en = doc.split("## English")
check("docs/RESCUE.md in Arabic: «في Downloader اكتب» the same code, and in the routes table", f"في Downloader اكتب **{code}**" in ar and f"Downloader (الكود {code})" in ar)
check("docs/RESCUE.md in English: the same code", f"in Downloader enter **{code}**" in en)
check("no «once it exists» left in the guide", "لما يتوفر" not in doc and "once it exists" not in doc)
others = set(re.findall(r"\b\d{7}\b", ar + en)) - {code}
check("the guide names no other 7-digit code", not others, others)
sys.exit(1 if failed else 0)
