#!/usr/bin/env python3
"""4.3.3 (4) remote notices on the home (urtv-data notices/announcements.json, NoticeRule): at most one at a time, only when the top bar has
nothing else to say, never while our tilawah or adhan plays, never in kids mode or «UrTV النظيف», each id once (its first visit), within its
days and app versions, no links, 140 characters at most; «تم» or Back hides a dismissible one. Runs NoticeRule on a plain JVM."""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


m = (HOME / "MainActivity.java").read_text(encoding="utf-8")
upd = m[m.index("private void updateHomeNudge(boolean takeFocus)"):]
upd = upd[:upd.index("\n    }\n")]
check("the bar's own messages first: a notice only when the update, the city and the nudge say nothing, never in kids mode",
      "NoticeRule.Notice notice = show || kids ? null : dueNotice();" in upd and upd.index("boolean show = ") < upd.index("dueNotice()"))
check("«UrTV النظيف»: no notices (the bar is hidden before anything is asked)", upd.index("if (cleanShown)") < upd.index("dueNotice()"))
check("never over playback: not while our tilawah or adhan plays", "if (sa.urtv.home.tilawah.MusicService.carryOurPlaying()) return null;" in m)
check("once per id: marked seen as it first appears, kept only for this visit (onPause ends it)",
      'np.edit().putStringSet("notice_seen", s).apply();' in m and "noticeOnScreen = null;   // 4.3.3: a notice was seen on this visit" in m)
check("«تم» and Back hide a dismissible one; one that is not has no button and no focus to reach",
      "if (nudgeIsNotice) { dismissNotice(); return; }" in m and "if (nudgeIsNotice) { if (!noticeFixed) dismissNotice(); return; }" in m
      and "nudgeCard.setVisibility(noticeFixed ? View.GONE : View.VISIBLE);" in upd and "&& !noticeFixed;" in m)
check("a notice never takes the focus by itself, and has no «لاحقاً»", "&& notice == null) nudge.post(" in upd and "nudgeLater.setVisibility(notice != null ? View.GONE : View.VISIBLE);" in upd)
check("its text in the app's language, the kept copy only (fetched in the background every 3 hours)",
      "nudgeAsk.setText(notice.text(S.en()));" in upd and "ContentFile.refresh(getApplicationContext(), NoticeRule.PATH, NoticeRule.NAME, 3 * 3600_000L);" in m
      and "NoticeRule.pick(notices.value(), day, BuildConfig.MARKETING, seen, noticeOnScreen)" in m and "notices.load(app);" in m)   # 4.4 (QA-10): the kept copy, loaded off the UI thread

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "NoticeRule.java"), str(HOME / "MiniJson.java"), str(ROOT / "tests/NoticeRuleRun.java")],
                           capture_output=True, text=True)
        check("NoticeRule compiles on a plain JVM", r.returncode == 0, r.stderr[-800:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.NoticeRuleRun"], capture_output=True, text=True).stdout
            got = dict(l.split(" ", 1) for l in out.splitlines() if " " in l)
            check("a notice with a link or a web address is never shown; the first due one is", got.get("EID") == "eid", out)
            check("one at a time", got.get("ONE") == "eid ONLY", out)
            check("seen: the next one due instead (here the fixed one), never the same again", got.get("SEEN") == "fixed!", out)
            check("…but it stays on the visit that shows it", got.get("SAMEVISIT") == "eid", out)
            check("app versions: under min_app or over max_app, not shown", got.get("TOOOLD") == "old-app" and got.get("TOONEW") == "fixed!", out)
            check("before its days, or no file: nothing", got.get("BEFORE") == "none" and got.get("NODOC") == "none", out)
            check("over 140 characters: never", got.get("LONG") == "none", out)
            check("versions compare part by part", got.get("CMP") == "1 0 1 -1", out)
            check("Arabic, or English when the app is in English", got.get("TEXT") == "عيدكم مبارك|Hello", out)

print(f"\n{len(fails)} failed" if fails else "\nall passed")
sys.exit(1 if fails else 0)
