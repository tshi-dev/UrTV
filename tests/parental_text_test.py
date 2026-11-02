"""The family lock's card (ParentalText.java, docs/UX-REVIEW.md): the count of locked apps as Arabic counts it, and the one-line summary.
Run: python3 tests/parental_text_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

act = (HOME / "ParentalActivity.java").read_text(encoding="utf-8")
check("off: the card says it is off, with «فعّل الرقابة الأبوية» (focused) and «ما الذي يُقفل؟» under it",
      'card(S.t("الرقابة الأبوية متوقفة")' in act and 'button(S.t("فعّل الرقابة الأبوية"), v -> choosePin(false))' in act and 'button(S.t("ما الذي يُقفل؟"), v -> explain())' in act)
check("on: the card says it is on and what is locked, with «التطبيقات المقفلة» (focused) and «غيّر الرمز»",
      'card(S.t("الرقابة الأبوية مفعّلة"), Digits.of(ParentalText.summary(' in act and 'button(S.t("غيّر الرمز"), v -> choosePin(true))' in act
      and 'button(S.t("التطبيقات المقفلة"), v -> focusAfter(appsRow, 1))' in act)
check("the two buttons are one row of the D-pad, wired like every row", "rows.add(new View[] {second, first});" in act and act.count("wire();") >= 2)
if not shutil.which("javac"):
    print("SKIP parental text: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(HOME / "Tr.java"), str(HOME / "ParentalText.java"),
                    str(ROOT / "tests/ParentalTextRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.ParentalTextRun"], check=True, capture_output=True).stdout.decode("utf-8").split("\n")
want = ["لا تطبيق مقفل", "تطبيق واحد مقفل", "تطبيقان مقفلان", "3 تطبيقات مقفلة", "10 تطبيقات مقفلة", "11 تطبيقًا مقفلًا", "25 تطبيقًا مقفلًا",
        "3 تطبيقات مقفلة، والإعدادات مقفلة، ووضع الأطفال متوقف.", "لا تطبيق مقفل، والإعدادات مفتوحة، ووضع الأطفال مفعّل."]
for w, g in zip(want, out):
    check(f"«{w}»", g == w, g)
sys.exit(1 if failed else 0)
