#!/usr/bin/env python3
"""4.4 (QA-6..QA-10, rule R1: the 4.3.3 TCL freeze's shape): no call into another app's process, and no reading of a file, on a path the UI
thread takes for a key, the minute tick, onCreate or a return, for the five paths QA found in 4.4. One section per finding; the whole graph is
QA's tests/regressions/ui_thread_ipc_test.py (its QA-6..10 baseline lines go when this is merged).
Run: python3 tests/ui_thread_44_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def src(p): return re.sub(r"//[^\n]*", "", p.read_text(encoding="utf-8"))
def body(s, head):
    return s.split(head)[1].split("\n    }\n")[0] if head in s else ""

# QA-6: Extras.enabled
ext = src(HOME / "Extras.java")
known = body(ext, "public static Boolean enabledKnown() {")
check("QA-6: Extras.enabledKnown never calls the add-on (the kept answer, a refresh on a thread)",
      "call(" not in known and "askNow(" not in known and "refresh();" in known and "return known;" in known)
check("QA-6: enabled() is the kept answer («not known yet» is off for a screen's line, never stored as an answer)",
      "public static boolean enabled() { return Boolean.TRUE.equals(enabledKnown()); }" in ext)
ref = body(ext, "static void refresh() {")
check("QA-6: refresh asks on a thread, once at a time, only when the answer is older than 2 s",
      "FRESH_MS" in ref and "asking.compareAndSet(false, true)" in ref and '"extras-status").start();' in ref)
ask = body(ext, "static Boolean askNow() {")
check("QA-6: a failed call keeps the last real answer; not installed is a real «off»",
      "if (b != null) known = b.getBoolean(ExtrasWords.ON, false);" in ask and "else if (!installed(c)) known = false;" in ask)
heal = src(HOME / "A11yHeal.java")
check("QA-6: «stuck» and «bound» need a real answer (unknown is neither)",
      "Boolean.FALSE.equals(Extras.enabledKnown())" in heal and "Boolean.TRUE.equals(Extras.enabledKnown())" in heal and "Extras.enabled()" not in heal)
phone = src(HOME / "PhoneApps.java")
check("QA-6: the phone page's server threads ask now (enabledNow), the only callers that may wait",
      "return Extras.enabledNow();" in phone and [f.name for f in HOME.rglob("*.java") if "Extras.enabledNow()" in src(f)] == ["PhoneApps.java"])
sleep = src(HOME / "SleepTimer.java")
off = body(sleep, "static String turnOff(Context c, final Runnable end) {")
check("QA-6: «أطفئ الآن» and the timer's end ask the add-on on a thread; the screensaver comes up from the main thread without it",
      '"sleep-off").start();' in off and off.index("new Thread(") < off.index("Extras.sleepNow()") and "Looper.getMainLooper()).post(" in off)
carry = src(HOME / "UpdateCarry.java")
check("QA-6: the carry's Home press goes to the add-on on a thread", 'Extras.goHome()' in carry.split('"carry-front"')[0].split("new Thread(")[-1])
main = src(HOME / "MainActivity.java")
check("QA-6: the home refreshes the add-on's state at its return and on the tick's thread",
      "Extras.refresh();" in body(main, "protected void onResume() {") and "Extras.askNow();" in main.split("clockReads.execute(() -> {")[1].split("runOnUiThread")[0])

# QA-7: the screensaver settings' home_key
sa = src(APP / "SettingsActivity.java")
check("QA-7: the settings' render reads the kept answer; the home is asked on a thread, once per return, and told on a thread",
      "NowPlayingBridge.homeKey(this" not in sa and "final Boolean homeKey = homeKeySeen();" in sa and '"home-key").start();' in sa
      and '"home-key-set").start();' in sa and "homeKeyAsked = false;" in body(sa, "protected void onResume() {"))

# QA-8: the control centre's adhan preview
prev = src(HOME / "tilawah/AdhanPreview.java")
play = body(prev, "public static void play(Context c, int prayer) {")
check("QA-8: the preview's voice (it reads the place) is picked on a thread; the sound starts from the main thread",
      play.index("new Thread(") < play.index("playFor(") and '"adhan-preview").start();' in play and "Looper.getMainLooper()).post(() -> MusicService.adhan(" in play)

# QA-9: «المباريات» hands over the kept follows
ma = src(HOME / "MatchesActivity.java")
check("QA-9: «المباريات» hands the kept follows over on a thread", 'new Thread(() -> HomeFollows.sync(syncApp), "follows-sync").start();' in ma
      and "HomeFollows.sync(this)" not in ma)

# QA-10: the notice bar's file
due = body(main, "private NoticeRule.Notice dueNotice() {")
check("QA-10: the notice bar reads the kept copy, never the file", "notices.value()" in due and "ContentFile.read(" not in due)
check("QA-10: the copy is loaded on the tick's thread and at each return (off the UI thread)",
      "notices.load(app);" in main.split("clockReads.execute(() -> {")[1].split("runOnUiThread")[0]
      and "clockReads.execute(() -> notices.load(app));" in body(main, "protected void onResume() {"))
cf = src(HOME / "ContentFile.java")
load = body(cf, "synchronized void load(Context c) {").split("\n        }")[0] if "synchronized void load(Context c) {" in cf else ""
check("QA-10: the file is parsed again only when its time or size changed", "if (t == at && n == size) return;" in cf and "read(c, name)" in cf.split("synchronized void load(Context c) {")[1])
# 4.4 review: two reads of the screensaver's settings left on the UI thread by their own code
gr = src(HOME / "Greeting.java")
check("review: the greeting's weather answer reads the settings on its worker thread; the posted code only draws",
      "final ConfigSnapshot again = ConfigSnapshot.read(context);" in gr and "main.post(() -> { fetching = false; update(" in gr and "ConfigSnapshot.read(context)); });" not in gr)
rcv = main.split("private final BroadcastReceiver nowPlayingChanged = new BroadcastReceiver() {")[1].split("private final BroadcastReceiver sleepChanged")[0]
check("review: the adhan receiver takes the known answer, and reads only off the UI thread when there is none",
      "ConfigSnapshot.known()" in rcv and "clockReads.execute(" in rcv and "updateAdhanBar(ConfigSnapshot.read(" not in rcv)

# QA-11: the home's and the screens' reads of the screensaver's settings
cs = src(HOME / "ConfigSnapshot.java")
uib = body(cs, "static ConfigSnapshot ui(Context c) {")
check("QA-11: ConfigSnapshot.ui is the known answer, else the kept one, and asks on a thread (never read/query)",
      "LAST.peek()" in uib and "kept(c)" in uib and "refreshSoon(c)" in uib and "read(" not in uib and "query(" not in uib)
rs = body(cs, "private static void refreshSoon(Context c) {")
check("QA-11: the refresh is one thread at a time, not more than once in SHARE_MS, and hands the digits and language the answer",
      'new Thread(' in rs and '"config-ui").start();' in rs and "asking.compareAndSet(false, true)" in rs and "Digits.remember(app, read(app))" in rs)
check("QA-11: a real answer keeps its look, appearance and buttons on disk, so a cold first draw shows the last real ones",
      "keepValues(c, made)" in cs and '.putString("look", s.look)' in cs and 'p.getString("look", null)' in body(cs, "private static ConfigSnapshot kept(Context c) {"))
gs = src(HOME / "GlassSource.java")
check("QA-11: the glass look, light and page ground read the kept answer on the UI thread",
      "ConfigSnapshot.ui(c).appearance()" in gs and "ConfigSnapshot.ui(c).look()" in gs and "ConfigSnapshot.ui(a).pagesDark" in gs and "ConfigSnapshot.read(a)" not in gs)
check("QA-11: only the phone page's server threads ask the screensaver now (lookNameNow)", "lookNameNow(c)" in body(gs, "public static java.util.Map<String, Object> phoneState(Context c) {"))
check("QA-11: Digits.init takes the kept answer", "ConfigSnapshot.ui(c)" in body(src(HOME / "Digits.java"), "public static void init(Context c) {"))
check("QA-11: the markets, matches, tasks and reminder screens, and the pill row, take the kept answer",
      "ConfigSnapshot.ui(this)" in body(src(HOME / "MarketsActivity.java"), "String param(") if "String param(" in src(HOME / "MarketsActivity.java") else "ConfigSnapshot.ui(this).fragment" in src(HOME / "MarketsActivity.java"))
for f in ("MatchesActivity", "TaskReminderActivity", "TasksActivity"):
    check("QA-11: " + f + " takes the kept answer on the UI thread", "ConfigSnapshot.ui(this)" in src(HOME / (f + ".java")))
check("QA-11: the pill row takes the kept answer", main.count("ConfigSnapshot.ui(this)") >= 3)
uc = body(main, "private void updateClock() {")
check("QA-11: the first clock draw of a cold process uses the kept answer, then reads on its thread", "ConfigSnapshot.ui(this)" in uc and "ConfigSnapshot.read(this)" not in uc)

# QA-12: the tasks screens and the reminders' alarm
ta = src(HOME / "TasksActivity.java")
check("QA-12: «المهام» asks the provider only on its io thread (fetch is called from io.execute lambdas and from fetch's own helpers only)",
      "private static Tasks fetch(" in ta and "getContentResolver().query(TASKS" in body(ta, "private static Tasks fetch(")
      and "getContentResolver().query(" not in ta.replace(body(ta, "private static Tasks fetch("), ""))
check("QA-12: «المهام» draws the list as last answered (render has no query) and refreshes on its thread at each return",
      "fetch(" not in body(ta, "private void render(int focus) {") and "refresh(0);" in body(ta, "protected void onResume() {")
      and "io.execute(() -> {" in body(ta, "private void refresh(final int focus) {") and "post(() -> { apply(t); render(focus); });" in body(ta, "private void refresh(final int focus) {"))
ch = body(ta, "private void change(final int kind")
check("QA-12: add, finish and delete write, find the task's id, re-plan the reminders and re-read the list inside one io job; the UI thread only says and draws",
      "io.execute(() -> {" in ch and "ReminderScheduler.schedule(app);" in ch and "fetch(app)" in ch and "post(() -> {" in ch
      and "ReminderScheduler.schedule" not in ta.replace(ch, "") and "getContentResolver()" not in ta.replace(ch, "").replace(body(ta, "private static Tasks fetch("), ""))
check("QA-12: a refused add puts its words back in the field; a full list is still said at once", "field.setText(t)" in ta and "MAX_TASKS) { toast(" in ta)
check("QA-12: the voice answer is read against the list on screen (no query in onActivityResult)", "getContentResolver" not in body(ta, "protected void onActivityResult(") and "load()" not in ta)
trs = src(HOME / "TaskReminderActivity.java")
onc = body(trs, "protected void onCreate(Bundle b) {")
check("QA-12: the reminder page reads its task on a thread and shows when it is here (it still leaves when the task is gone)",
      "io.execute(() -> {" in onc and "fetch(app, id)" in onc and "finish();" in onc and "View.INVISIBLE" in onc and "getContentResolver" not in onc)
check("QA-12: a change on the reminder page is saved, and the alarm re-planned, on the io thread, in order",
      "io.execute(() -> {" in body(trs, "private void save() {") and "ReminderScheduler.schedule(app);" in body(trs, "private void save() {") and "ReminderScheduler.schedule(this)" not in trs
      and "if (edit == null) return;" in body(trs, "private void step(int row, int dir) {"))

# QA-12 / QA-13 (the launcher's other one-off calls and small disk reads)
tmain = src(HOME / "tilawah/MainActivity.java")
prow = tmain.split("row.kind == KIND_PRAYER")[1].split("row.kind == KIND_ADHAN_TOGGLE")[0]
check("QA-12: a prayer row's press picks the voice (it reads the place) on a thread and starts it from the main thread",
      'new Thread(() -> {' in prow and prow.index("new Thread(") < prow.index("prefs.playFor(app, prayer)") and "runOnUiThread(() -> {" in prow and '"adhan-row").start();' in prow)
sch = src(HOME / "tilawah/AdhanScheduler.java")
check("QA-12: the screen's alarm (the place is a question to the screensaver) is set on a thread: scheduleSoon; the screen and its switches call that",
      "static void scheduleSoon(Context c) {" in sch and '"adhan-schedule").start();' in sch and "AdhanScheduler.schedule(this)" not in tmain.replace("AdhanScheduler.schedule(this, ", ""))
mk = src(HOME / "MarketsActivity.java")
check("QA-12: a market or stock toggle calls the screensaver on a thread; the value is kept at once and taken back when it refuses",
      'new Thread(() -> {' in body(mk, "private void saverSet(") and '"saver-set").start();' in body(mk, "private void saverSet(") and "getContentResolver().call(SAVER" in body(mk, "private void saverSet(")
      and "this.saved.remove(key)" in mk and "if (saverSet(" not in mk)
sl = src(HOME / "setup/SetupLang.java")
check("QA-12: the first-run language choice tells the screensaver on a thread (the screen is built again in it at once)",
      'new Thread(() -> save(app, v), "setup-lang").start();' in sl and "save(a, v);" not in sl)
mt = src(HOME / "MatchesActivity.java")
check("QA-12: «المباريات» finds the freshest page on a thread and loads it when it is here",
      '"matches-page").start();' in mt and "HomePage.fresh(syncApp, wanted)" in mt and "web.loadUrl(HomePage.fresh(" not in mt)
sh = src(HOME / "setup/SetupHook.java")
check("QA-13: the setup ledger's files are read on a thread at the home's start and at its return (never in onCreate / onResume)",
      '"setup-home-check").start();' in sh and '"setup-home-up").start();' in sh and "homeUpRunning.compareAndSet(false, true)" in sh
      and "new FileStore(" not in body(sh, "public static void atHomeStart(final Context c) {") and "new FileStore(" not in body(sh, "public static void homeUp(final Context c) {"))
sa = src(HOME / "setup/SetupActivity.java")
check("QA-13: the setup page reads the saved deadline inside its recovery thread", "SetupRecovery.rearmFromDisk(SetupActivity.this);" in sa and "SetupRecovery.rearmFromDisk(this);" not in sa)
gs = src(HOME / "GlassSource.java")
pg = body(gs, "private static void paint(")
check("QA-13: a screen's page paints the backdrop the process has (or the look's colour) and decodes the file on a thread, painting again when it brings another picture",
      "backdropKnown()" in pg and "backdrop(app)" in pg and '"glass-backdrop").start();' in pg and "a.runOnUiThread(" in pg and "backdrop(a)" not in gs.replace("backdropKnown()", ""))
check("QA-13: the home's first frame takes the small photo copy from memory only; the file is decoded on clockReads",
      "int how = WallFirstFrame.source(has, smallPhoto != null, true);" in main and "BitmapFactory.decodeFile" in body(main, "private void firstFrameWallpaper() {")
      and "clockReads.execute(() -> {" in body(main, "private void firstFrameWallpaper() {") and "wallDiskLoading" in main)
snp = body(main, "private void showSnapshot() {")
check("QA-13: the last-picture overlay is decoded on clockReads and put over the home only when it comes within SNAP_LATE_MS (else as with no picture)",
      "clockReads.execute(() -> {" in snp and "HomeSnapshot.load(" in snp and "SNAP_LATE_MS" in snp and snp.index("clockReads.execute") < snp.index("HomeSnapshot.load("))
check("QA-13: the model's defaults file is read on clockReads", "clockReads.execute(() -> DeviceDefaults.apply(app))" in main)
rp = src(HOME / "ReportActivity.java")
check("QA-13: the report's logcat classes are read on a thread and the lines drawn again with them",
      '"report-errors").start();' in rp and "recentErrors()" in rp.split('"report-errors"')[0].split("new Thread(")[-1] and "= recentErrors();" not in rp)

# QA-2 and the screensaver's QA-12 / QA-13
cm = src(APP / "CleanMask.java")
check("QA-12: CleanMask.state is the kept copy (no query); a thread asks the home, keeps a changed answer and tells the screens that are up",
      "getContentResolver" not in body(cm, "static int state(Context c) {") and 'new Thread(' in body(cm, "static int state(Context c) {") and "Prefs.CHANGED" in cm and "private static int ask(Context c) {" in cm
      and body(cm, "static int state(Context c) {").index("putInt(\"clean_mask\"") < body(cm, "static int state(Context c) {").index("sendBroadcast("))
hub = src(APP / "YourTvHubActivity.java")
check("QA-12: the hub goes straight on to the settings when the mask turns out to be on after it opened", "CleanMask.state(this) != CleanMask.OFF" in body(hub, "private void staleCheck() {"))
npb = src(APP / "NowPlayingBridge.java")
check("QA-2: a key reads the adhan state kept (the page's ask every 3 s, or a thread's), never a query: adhanKnown",
      "getContentResolver" not in body(npb, "static JSONObject adhanKnown(Context c) {") and "refreshAdhan(c);" in body(npb, "static JSONObject adhanKnown(Context c) {")
      and "ADHAN_FRESH_MS" in body(npb, "static JSONObject adhanKnown(Context c) {") and "adhanState(app)" in body(npb, "static void refreshAdhan(Context c) {") and "new Thread(" in body(npb, "static void refreshAdhan(Context c) {"))
check("QA-2: the page's own ask feeds the same copy, and the stop call is made on a thread with the copy forgotten at once",
      "rememberAdhan(o);" in body(npb, "public String adhan() {") and "adhanSeen = null;" in body(npb, "static void stopAdhanSoon(Context c) {") and '"adhan-stop"' in body(npb, "static void stopAdhanSoon(Context c) {"))
for f in ("ScreenDream", "MainActivity"):
    t = src(APP / (f + ".java"))
    kd = body(t, "public boolean dispatchKeyEvent(KeyEvent e) {")
    check("QA-2: " + f + "'s key uses adhanKnown and stopAdhanSoon (no adhanState, no stopAdhan on the key)",
          "NowPlayingBridge.adhanKnown(this)" in kd and "NowPlayingBridge.stopAdhanSoon(this)" in kd and "adhanState(" not in kd and "NowPlayingBridge.stopAdhan(this)" not in kd)
    check("QA-2: " + f + " asks the adhan on a thread every 3 s while it shows, when the page does not (the adhan bar is off)",
          "adhanWatch" in t and "refreshAdhan(" in t and "adhanBar()" in body(t, "private final Runnable adhanWatch = new Runnable() {"))
ks = src(APP / "KickoffTry.java")
check("QA-12: the «جرّب» whistle is called on a thread", '"kickoff-whistle").start();' in ks and "if (sound) NowPlayingBridge.kickoffWhistle(a);" not in ks)
gsa = src(APP / "GlassSource.java")
pgs = body(gsa, "static GlassStyle.Look page(final Activity a, final View root) {")
check("QA-12: the settings screens paint what is known of the home's backdrop and ask the home on a thread, painting again when it brings another",
      "refresh(a)" not in pgs and "refresh(app);" in pgs and '"glass-refresh").start();' in pgs and "a.runOnUiThread(" in pgs and "refresh(c);" not in body(gsa, "static android.graphics.drawable.Drawable pageIn(Context c, GlassStyle.Look l) {"))
rf = body(gsa, "private static void refresh(Context c) {")
check("QA-12: refresh holds the class lock for its assignments only (the UI thread's draw never waits on the home)", "synchronized (GlassSource.class)" in rf and "private static synchronized void refresh" not in gsa)
ss = src(APP / "SaverStart.java")
check("QA-13: the dream's last picture is decoded on a thread; its content is set (with the picture in the first frame) when it is here",
      '"saver-picture").start();' in body(ss, "static void begin(final Host host, final String url, final Runnable afterContent) {") and "SaverSnapshot.load(" not in body(ss, "private static void show(")
      and "host.alive()" in body(ss, "static void begin(final Host host, final String url, final Runnable afterContent) {"))
st = src(APP / "SettingsActivity.java")
check("QA-13: a settings row's thumbnail, a mix's picture and the stage's photo are decoded on the pictures' thread",
      st.count("pics.execute(() -> {") >= 3 and "THUMBS" in st and "th.setImageBitmap(BitmapFactory.decodeStream(in))" not in st and "\n        Bitmap photo = photo(3);" not in st
      and "v.setImageBitmap(android.graphics.BitmapFactory.decodeStream(in));" not in st)
pk = src(APP / "PhotoPacksActivity.java")
check("QA-13: the pack cover of «صورنا» and the credits' files are read on a thread",
      '"pack-cover").start();' in pk and '"pack-credits").start();' in pk and "PackStore.creditRows(this" not in pk)
ap = src(APP / "AppearanceActivity.java")
check("QA-13: the packs' names are read on a thread and the page drawn again when they differ", "PackStore.names(this)" not in ap and '"pack-names").start();' in ap and "namesAsked = false;" in body(ap, "protected void onResume() {"))
pf = src(APP / "Prefs.java")
check("QA-13: the settings' correction note takes the kept published-corrections copy; a worker or provider thread waits for the first read as before, the UI thread never does",
      "prayerFixKnown(" in st and "Looper.myLooper() != android.os.Looper.getMainLooper()" in body(pf, "int[] prayerFix() {") and '"prayer-fix-first"' in body(pf, "int[] prayerFix() {")
      and "ContentFile.read(" not in body(pf, "int[] prayerFix() {") and "ContentFile.read(" not in body(pf, "int[] prayerFixKnown(final Runnable changed) {"))

print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
