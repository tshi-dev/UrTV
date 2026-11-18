#!/usr/bin/env python3
"""Regression gate: guards for the bugs the on-TV tester found and that were fixed (each verified fixed on the TV in the build named). Source
rules over the code that holds each fix, tight enough that undoing the fix fails them, plus two small runs: PhotoPacks.isolate on a JVM and the
phone page's send handler under node (no browser).
  step 28 (build 183): the first-start mute record, the markets labels, the currency picker, the quick buttons' look, «المباريات القادمة»
  step 38: the photo packs' index on the branch a build reads from (the fix was data: photos/packs.json)
  step 38 (build 209): «حقوق الصور»: file names left to right, «إغلاق» legible
  step 41 (build 212): the phone page says «اكتب الرسالة أولًا.» for an empty message
  step 47 (build 228): the quick-buttons box measured again when clean mode goes off
Not guarded here: step 86 (build 386: the setup page's language, «2 apps chosen» for none). Its fix (S.refresh before the configuration,
SetupActivity.chosenCount) is on next (4.4) and not on this branch, whose SetupActivity still counts picks.size(): nothing here to guard.
Run: python3 tests/regressions/tester_guards_test.py"""
import json, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
PHONE = ROOT / "android/launcher/src/main/assets/phone"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")
def body(src, head):
    """the block that starts at the line holding `head`, up to the first line closed at that line's indent"""
    i = src.find(head)
    if i < 0: return ""
    start = src.rfind("\n", 0, i) + 1
    indent = re.match(r"[ \t]*", src[start:]).group(0)
    end = re.compile("\n" + re.escape(indent) + r"\}").search(src, i)
    return src[i:end.start()] if end else ""

# step 28 (build 183): after a fresh install a restart came back unmuted: the first start wrote no mute record, so the wake found none
mk = text(HOME / "MuteKeep.java")
start = body(mk, "static synchronized void start(final Context app) {")
rec = start.find('if (!app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("has", false)) remember(app, "first_start");')
check("step 28: a first start with no mute record writes one (remember «first_start» when «has» is not set), in MuteKeep.start",
      rec >= 0, start[-400:])
check("step 28: …before the boot's wake reads it", rec >= 0 and 0 <= rec < start.find('woke(app, "boot")'))
check("step 28: …and remember() sets «has» (the record the next wake reads)", '.putBoolean("has", true)' in body(mk, "static void remember(Context c, String why) {"))

# step 28 (build 183): «صرف العملات» and «أخفِ المبالغ» cut in half by a fixed 40 dp height
mkt = text(HOME / "MarketsActivity.java")
check("step 28: «صرف العملات» takes its text's height (LayoutParams(-2, -2), not a fixed dp(40))",
      "controls.addView(fxBtn, new LinearLayout.LayoutParams(-2, -2));" in mkt and not re.search(r"addView\(fxBtn,[^;]*dp\(", mkt))
check("step 28: the row beside it («أخفِ المبالغ»'s holder) and «أخفِ المبالغ» itself wrap their text too",
      "LinearLayout.LayoutParams cp2 = new LinearLayout.LayoutParams(-2, -2);" in mkt
      and "ctl.addView(hideBtn, new LinearLayout.LayoutParams(-2, -2));" in mkt and not re.search(r"addView\(hideBtn,[^;]*(dp\(|-1\))", mkt))

# step 28 (build 183): «اختر العملات» jumped back to the top after each pick: the list is drawn again, so the pick and the scroll are carried over
pick = body(mkt, "private void chooseCurrencies() {")
check("step 28: a pick remembers its currency and the list's scroll before the picker is drawn again",
      re.search(r"pickedCode = code;\s*pickedScroll = sv\.getScrollY\(\);\s*d\.dismiss\(\);\s*chooseCurrencies\(\);", pick), pick[:300])
check("step 28: …and the new picker focuses that row and scrolls back to where it was (not the first row)",
      "final String keepCode = pickedCode;" in pick and "if (code.equals(keepCode)) kept = holder[0];" in pick
      and "sv.post(() -> sv.scrollTo(0, keepScroll));" in pick and re.search(r"if \(kept != null\) \{.*\} else if \(first != null\) first\.requestFocus\(\);", pick, re.S))

# step 28 (build 183): the two new quick buttons («الأسواق», «المباريات») stayed grey in «رملي» while the others took the look
ma = text(HOME / "MainActivity.java")
looks = re.search(r"for \(Pill p : new Pill\[\] \{([^}]*)\}\) if \(p != null\) p\.look\(glassNow\);", ma)
check("step 28: «الأسواق» and «المباريات» are kept as fields and take the look with the other pills",
      "marketsPill = marketsBtn;" in ma and "matchesPill = matchesBtn;" in ma and looks and {"marketsPill", "matchesPill"} <= set(re.findall(r"\w+", looks.group(1))),
      looks and looks.group(1))
check("step 28: …and every pill of the row takes it too (a button added later is not left grey)",
      "for (View v : pills) if (v instanceof Pill) ((Pill) v).look(glassNow);" in ma)

# step 28 (build 183): the widget said «مباريات اليوم» over tomorrow's games
for name in ("src/template.html", "dist/tv.html"):
    pg = text(ROOT / name)
    fb = body(pg, "function fbRender(){")
    check(f"step 28: {name}: the games widget is titled «المباريات القادمة» when none of its games is today, «مباريات اليوم» otherwise",
          "upcoming:'المباريات القادمة'" in pg and "today:'مباريات اليوم'" in pg
          and "const upcoming = list.length && !list.some(ev => fbDayOf(fbKick(ev)) === cityToday(now));" in fb
          and "for(const id of ['fb-mt', 'fb-row'])" in fb and "t.nodeValue = upcoming ? FB_W.upcoming : FB_W.today;" in fb, fb[:300])
check("step 28: «المباريات القادمة» has its English", re.search(r"^المباريات القادمة\t\S", text(ROOT / "i18n/en/saver.tsv"), re.M))

# step 38: the photo packs list was empty on next-channel builds: the index a build reads is photos/packs.json on its own branch, and it existed
# on master only. The fix was data (the file on the branch); the guard: the build reads its branch's file, and this branch's file lists the packs
ps = text(SAVER / "PackStore.java")
check("step 38: the packs' index is photos/packs.json of the build's branch (PackStore.INDEX_URL over PhotoCache.REPO, BuildConfig.BRANCH)",
      'static final String INDEX_URL = PhotoCache.REPO + "photos/packs.json";' in ps
      and 'static final String REPO = "https://raw.githubusercontent.com/tshi-dev/UrTV/" + BuildConfig.BRANCH + "/";' in text(SAVER / "PhotoCache.java"))
try:
    ids = [p.get("id") for p in json.loads(text(ROOT / "photos/packs.json")).get("packs", [])]
except (OSError, ValueError, AttributeError) as e:
    ids = [repr(e)]
check("step 38: this branch's photos/packs.json lists the five packs (an empty index left the picker with «لا توجد مجموعات للتنزيل الآن»)",
      {"space", "nature", "sea", "ksa-cities", "haramain"} <= set(ids), ids)

# step 38 (build 209): «حقوق الصور»: file names garbled in the Arabic lines (no left-to-right isolate), «إغلاق» pale on the dialog's pale fill
ppa = text(SAVER / "PhotoPacksActivity.java")
cr = body(ppa, "private void showCredits() {")
check("step 38: «حقوق الصور» puts each file name in a left-to-right isolate (PhotoPacks.isolate(r[0]))",
      'append(PhotoPacks.isolate(r[0]))' in cr and not re.search(r'append\(r\[0\]\)', cr), cr[:300])
check("step 38: …and «إغلاق» is drawn in the page's text colour once the dialog is shown",
      re.search(r"d\.show\(\);\s*android\.widget\.Button close = d\.getButton\(android\.content\.DialogInterface\.BUTTON_POSITIVE\);\s*"
                r"if \(close != null\) close\.setTextColor\(look\.text\);", body(ppa, "private void creditsDialog(String text) {")), cr[-400:])   # 4.4 QA-13: the dialog is built after the thread that reads the files
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as t:
        run = Path(t) / "src/sa/urtv/screensaver/TesterIsolateRun.java"
        run.parent.mkdir(parents=True)
        run.write_text('package sa.urtv.screensaver;\npublic class TesterIsolateRun { public static void main(String[] a) {\n'
                       '  String s = PhotoPacks.isolate("IMG_2041.jpg");\n'
                       '  System.out.println(s.equals("\\u2066IMG_2041.jpg\\u2069") ? "OK" : "BAD " + s.codePoints().boxed().collect(java.util.stream.Collectors.toList()));\n} }\n',
                       encoding="utf-8")
        c = subprocess.run(["javac", "-encoding", "UTF-8", "-d", t, str(SAVER / "PhotoPacks.java"), str(run)], capture_output=True, text=True)
        out = subprocess.run(["java", "-cp", t, "sa.urtv.screensaver.TesterIsolateRun"], capture_output=True, text=True).stdout if c.returncode == 0 else c.stderr
    check("step 38: PhotoPacks.isolate(«IMG_2041.jpg») is the name between U+2066 and U+2069 (run on a JVM)", out.strip() == "OK", out[-300:])
else:
    print("SKIP step 38: isolate on a JVM: no javac here")

# step 41 (build 212): the phone page's «اعرضها على التلفزيون» with an empty message did nothing and said nothing
for page, said in (("index.html", "اكتب الرسالة أولًا."), ("index.en.html", "Type your message first.")):
    src = text(PHONE / page)
    h = body(src, "$('msgGo').onclick = function () {")
    check(f"step 41: {page}: an empty message says «{said}», keeps the focus in the box and sends nothing",
          re.search(r"if \(!t\) \{ toast\('" + re.escape(said) + r"'\); \$\('msgT'\)\.focus\(\); return; \}", h)
          and h.find("if (!t)") < h.find("api('/api/message'"), h[:400])
    if shutil.which("node") and h:
        js = ("let toasted = null, sent = 0, focused = 0, box = {value: ' \\n  ', focus() { focused++; }};\n"
              "const $ = id => id === 'msgT' ? box : {}; const toast = m => { toasted = m; }; const say = () => true;\n"
              "const api = () => { sent++; return Promise.resolve({ok: true}); };\n"
              "const send = function () {" + h[h.index("{") + 1:] + "\n};\n"
              "send(); const empty = {toasted, sent, focused};\n"
              "toasted = null; box.value = 'hi'; send();\n"
              "console.log(JSON.stringify({empty, full: {toasted, sent}}));\n")
        with tempfile.TemporaryDirectory() as t:
            (Path(t) / "send.js").write_text(js, encoding="utf-8")
            r = subprocess.run(["node", str(Path(t) / "send.js")], capture_output=True, text=True)
        try:
            got = json.loads(r.stdout)
        except ValueError:
            got = {"error": r.stdout + r.stderr}
        check(f"step 41: {page}: the send handler run under node: blank sends nothing and says so, a real message is sent",
              got == {"empty": {"toasted": said, "sent": 0, "focused": 1}, "full": {"toasted": None, "sent": 1}}, got)
    elif not shutil.which("node"):
        print(f"SKIP step 41: {page} under node: no node here")

# step 47 (build 228): after clean mode was turned off the quick-buttons box came back narrower (x 96–240, was 104–264): not measured again
clean = body(ma, "private void applyClean() {")
restore = body(ma, "private void restoreClean() {")
check("step 47: turning clean mode off restores the views (restoreClean) once the mask was on",
      re.search(r"if \(was && !on\) \{[^}]*restoreClean\(\);", clean), clean[-400:])
check("step 47: …and restoreClean unlocks the quick-button bar and asks it and its row to be measured again",
      "if (pillBar != null) { pillBar.lock(false); pillBar.requestLayout(); }" in restore and "if (pillRow != null) pillRow.requestLayout();" in restore, restore)
lock = body(text(HOME / "PillBar.java"), "void lock(boolean on) {")
check("step 47: …and PillBar.lock(false) shapes every button again (apply()) at the openness of now",
      re.search(r"\n\s*else apply\(\);", lock), lock)
sys.exit(1 if failed else 0)
