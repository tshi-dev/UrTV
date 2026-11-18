#!/usr/bin/env python3
"""4.4 «الأدعية والتذكيرات»: sounds the person picks, played at their times like the adhan. DuaRule and DuaPlan (run for real, tests/DuaRuleRun.java):
every day, days of the week, a Gregorian or a Hijri date, once or repeating; a clock time or before/after a prayer by minutes; never late, never in
standby, never over the adhan, never over another app's sound unless the person chose it. The source checks: one alarm for the earliest, set again
after a reboot and on every change; the dua plays through the adhan's path (the recitation paused and brought back, its own volume, the TV's
volume put back), the home's card with «إيقاف», the screensaver's bar with its name.   python3 tests/dua_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L = ROOT / "android/launcher/src/main"
T = L / "java/sa/urtv/home/tilawah"
H = L / "java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

rule, plan, store, sched, recv, svc, act = (read(T / f) for f in ("DuaRule.java", "DuaPlan.java", "DuaStore.java", "DuaScheduler.java", "DuaReceiver.java", "MusicService.java", "DuaActivity.java"))
man, home, card, bar, prov = read(L / "AndroidManifest.xml"), read(H / "MainActivity.java"), read(H / "DuaCard.java"), read(H / "AdhanBar.java"), read(T / "NowPlayingProvider.java")
saver, page = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/NowPlayingBridge.java"), read(ROOT / "src/template.html")

check("DuaRule and DuaPlan are plain Java", "import android" not in rule and "import android" not in plan)
check("«فوق تطبيق آخر» is off by default", "boolean overOthers;" in store and 'o.optBoolean("over", false)' in store)
check("a rule a newer version wrote that this one cannot read is left out, never guessed", "if (r == null) continue;" in store)
check("one alarm, the earliest of the ones that are on, after the last time each played",
      "if (!d.on) continue;" in sched and "d.rule.next(Math.max(after, d.lastDue), days)" in sched and "if (t > 0 && (best == 0 || t < best)) best = t;" in sched)
check("the alarm is the app's own (not exported) and set again after a reboot or a clock change, with the adhan's",
      '<receiver android:name=".tilawah.DuaReceiver" android:exported="false" />' in man and "DuaScheduler.schedule(context);   // 4.4" in read(T / "AdhanReceiver.java"))
check("the receiver: each dua plays once per moment, a one-time one turns itself off, then the next alarm is set",
      "if (!d.on || d.lastDue >= due) continue;" in recv and "if (!d.rule.repeat) d.on = false;" in recv and recv.rstrip().endswith("DuaScheduler.schedule(c);\n    }\n}") or recv.count("DuaScheduler.schedule(c);") == 2)
check("the receiver asks DuaPlan: on time, the screen on, no adhan, another app's sound only when chosen",
      "DuaPlan.decide(due, now, pm == null || pm.isInteractive(), MusicService.carryAdhanActive()," in recv
      and "am.isMusicActive() && !MusicService.carryOurPlaying(), chosen.overOthers" in recv)
pa = svc[svc.index("private void playDua("):svc.index("    /** Pauses the surah (or radio)")]
check("the dua plays through the adhan's path: the recitation paused and brought back, never over an adhan for a prayer",
      "if (sourceUrl == null || (source == Prefs.SOURCE_ADHAN && adhanLive && duaNow == null)) return;" in pa and "playAdhan(prefs.voice(), name, 0, -1);" in pa
      and "prepareAdhanPause()" in pa)
check("its own volume, and the TV's volume put back after (the adhan's way)", "volumePercent = duaUrl != null ? duaVolume : prefs.volumeFor(prayer);" in pa
      and "int want = AdhanLevel.index(volumePercent, max);" in svc)
check("an adhan takes the place of a dua; a dua never cuts an adhan", "if (duaNow == null && !adhanPreview) return;" in pa)
check("a dua plays whole (no short adhan cut) and is not the «last adhan»", "adhanCutMs = duaUrl != null ? 0 :" in pa and pa.index("if (duaUrl != null) {") < pa.index("prefs.setLastAdhan("))
check("every way an adhan ends clears the dua's name", svc.count("adhanName = null; adhanLive = false; adhanPrayer = -1; duaNow = null;") == 3)
check("the home: the card with the name and «إيقاف» (focused), OK on it stops it like the adhan's bar; no adhan bar for a dua",
      "duaCard.show(NowPlaying.duaName(), getCurrentFocus())" in home and "(card && getCurrentFocus() == duaCard.stop)" in home and "stop.requestFocus();" in card
      and 'S.t("إيقاف")' in card and "if (NowPlaying.duaName() != null) return null;" in bar)
check("the screensaver: the provider adds the name as a third column (older screensavers read two), the bar says it",
      'new String[]{"prayer", "any_key", "name"}' in prov and 'cur.getColumnIndex("name")' in saver and "if(ADHAN_LIVE.name) return ADHAN_LIVE.name" in page)
check("the screens: the list, one dua's page with every setting, the time in three steps, delete behind a question",
      all(w in act for w in ('S.t("إضافة دعاء أو تذكير")', 'S.t("الأيام")', 'S.t("الوقت")', 'S.t("يتكرر")', 'S.t("مستوى الصوت")', 'S.t("فوق تطبيق آخر يشغّل صوتًا")',
                             'S.t("استمع الآن")', 'S.t("في ساعة محددة")', 'S.t("قبل صلاة")', 'S.t("بعد صلاة")', 'S.t("تاريخ هجري")', 'S.t("تاريخ ميلادي")'))
      and "ask(S.t(\"تحذف «%s»؟\"" in act)
check("every change is kept and the alarm set again", "DuaStore.put(this, edit)" in act and "DuaScheduler.schedule(this);" in act)
check("a deleted sound turns its duas off (they stay)", "DuaStore.soundGone(c, id);" in read(T / "AudioStore.java") and "d.on = false;" in store[store.index("static synchronized void soundGone"):])

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "DuaRule.java"), str(T / "DuaPlan.java"), str(ROOT / "tests/DuaRuleRun.java")], capture_output=True, text=True)
        check("DuaRule and DuaPlan compile on their own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.DuaRuleRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 25, len(out.splitlines()))
sys.exit(1 if failed else 0)
