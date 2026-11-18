#!/usr/bin/env python3
"""4.4 touch «سورة الكهف يوم الجمعة»: a ready reminder in «الأدعية والتذكيرات» that needs no file: surah 18 by a reciter of «تلاوة» (the one last
listened to, else the first), every Friday an hour after Fajr; offered while there is none; its reciter can be changed, or a file used instead; it
plays like any dua (the surah's address is the reciter's own, as «تلاوة» plays it).   python3 tests/kahf_template_test.py"""
import subprocess, sys, tempfile, shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
act, store = (T / "DuaActivity.java").read_text(encoding="utf-8"), (T / "DuaStore.java").read_text(encoding="utf-8")
kahf = act[act.index("private void addKahf()"):act.index("private boolean hasKahf()")]
check("surah 18 by the reciter last listened to (else the first)", 'd.sound = "q:" + reciterId(new Prefs(this).reciter()) + ":18";' in kahf
      and "cat.reciterIndex(wanted) >= 0 ? wanted : cat.reciters.get(0).id" in act)
check("every Friday, an hour after Fajr", "d.rule.day = DuaRule.WEEKDAYS;" in kahf and "d.rule.weekdays = 1 << 5;" in kahf and "d.rule.prayer = PrayerTimes.FAJR;" in kahf
      and "d.rule.offset = 60;" in kahf and "d.rule.at = DuaRule.AFTER;" in kahf)
check("offered only while there is none, and its page opens", "if (!hasKahf()) out.add(" in act and "openDua(edit.id);" in kahf)
check("its sound is the reciter's own address for surah 018, as «تلاوة» plays it", 'cat.reciters.get(r).server + String.format(java.util.Locale.US, "%03d", n) + ".mp3"' in store)
check("its reciter can be changed, or a file used instead", "chooseReciter" in act and 'S.t("ملف من «صوتياتي»")' in act)
if shutil.which("javac"):
    run = """package sa.urtv.home.tilawah;
public class KahfRun { public static void main(String[] a) {
  DuaRule r = new DuaRule(); r.day = DuaRule.WEEKDAYS; r.weekdays = 1 << 5; r.at = DuaRule.AFTER; r.prayer = 0; r.offset = 60;
  long D = 86_400_000L, M = 60_000L;
  DuaRule.Days days = k -> new DuaRule.Day(k % 7, 2026, 1, 1, 1448, 1, 1, k * D, new long[]{k * D + 300 * M, 0, 0, 0, 0, 0});
  System.out.println(r.next(0, days) == 5 * D + 360 * M && r.next(5 * D + 360 * M, days) == 12 * D + 360 * M ? "ok" : "bad");
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "KahfRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "DuaRule.java"), str(Path(tmp, "KahfRun.java"))], capture_output=True, text=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.KahfRun"], capture_output=True, text=True).stdout.strip() if r.returncode == 0 else r.stderr
        check("its rule: the first Friday (day 5) at Fajr + 60 minutes, then a week later", out == "ok", out)
sys.exit(1 if failed else 0)
