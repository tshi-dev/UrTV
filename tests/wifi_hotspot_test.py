#!/usr/bin/env python3
"""A user's tested route (Xiaomi): when the setup wizard owns the saved Wi-Fi and the TV shows no «انسَ الشبكة» or refuses, join a phone hotspot
first, forget the home network, then join it again from Settings. It is in the setup's Wi-Fi step (SetupRunner.WIFI_ASK_TEXT, its marker and English),
in docs/RESCUE.md (Arabic and English) and in the agent's row. Never deleting the network any other way stays said.
Run: python3 tests/wifi_hotspot_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return (ROOT / p).read_text(encoding="utf-8")
run = read("android/launcher/src/main/java/sa/urtv/home/setup/SetupRunner.java")
text = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', re.search(r"WIFI_ASK_TEXT = (.*?);\n", run, re.S).group(1)))
ROUTE = "اتصل أول بنقطة اتصال من جوالك، بعدها انسَ شبكة البيت وارجع اتصل بها من الإعدادات"
check("the Wi-Fi step names the hotspot route, after the plain forget and before «كمّل»", ROUTE in text and text.index("«انسَ الشبكة»، ثم") < text.index(ROUTE) < text.index("بعدها ارجع هنا واضغط «كمّل»"))
check("…as tested on a Xiaomi", "(مجرّبة على شاومي)" in text)
check("the step's marker and English follow the text", 'S.n("' + text + '")' in read("android/launcher/src/main/java/sa/urtv/home/setup/SetupActivity.java")
      and any(l.startswith(text + "\t") and "hotspot" in l for l in read("i18n/en/setup.tsv").splitlines()))
res = read("docs/RESCUE.md")
ar = res.split("## الواي فاي محفوظ عند «وسيط الإعداد» (طريقة مجرّبة)")[1].split("## English")[0]
check("RESCUE.md: the route in three steps, hotspot, forget, rejoin from Settings", ar.index("1. شغّل **نقطة اتصال**") < ar.index("2. وهو على نقطة الاتصال") < ar.index("3. ارجع اتصل بشبكة البيت من **الإعدادات**"))
check("RESCUE.md: never delete the network another way, never the wizard by hand", "لا تحذف الشبكة ولا ملفاتها" in ar and "ولا توقف «وسيط الإعداد» بنفسك" in ar)
check("RESCUE.md English: the same tested route", "a tested route" in res and "hotspot" in res.split("## English")[1] and "never turn the setup wizard off by hand" in res)
check("the agent's row gives the person the route in Arabic", "(a Xiaomi user's tested route): «" + ROUTE in read("docs/SETUP-PROMPT.md"))
sys.exit(1 if failed else 0)
