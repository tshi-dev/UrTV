package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * «إعداد تلفزيوني»: what `optimize/run.sh` does from a computer, done by the TV itself over its own local ADB (see {@link AdbClient}). The same
 * steps, the same lists (SetupLists, generated from optimize/lists), the same ledger of what was turned off (disabled-by-us.txt) and the same
 * journal of the home switch (home-switch.json, the same fields as optimize/home-journal.sh), so the undo follows optimize/restore.sh.
 * Fail closed: an empty or missing answer from the TV is «unknown», never «done» and never «nothing to do»; a list counts only when it ends with
 * its marker. A subset on purpose: the checks, the grants, the safe list, the screensaver and the home switch. Plain Java (the shell, the files
 * and the clock are passed in) so tests/SetupEngineRun.java runs it on a JVM against a fake TV.
 */
final class SetupEngine {
    static final String SELF = "sa.urtv.home", SAVER = "sa.urtv.screensaver", OURS_DREAM = SAVER + "/.ScreenDream", HOME_ACTIVITY = SELF + "/.MainActivity";
    static final String WRAITH = "com.google.android.tungsten.setupwraith";
    static final String LAUNCHERX = "com.google.android.apps.tv.launcherx", LAUNCHERX_HOME = LAUNCHERX + "/.home.HomeActivity";
    static final String TVLAUNCHER = "com.google.android.tvlauncher", TVLAUNCHER_HOME = TVLAUNCHER + "/.MainActivity";
    static final String NOTIFICATION_LISTENER = LAUNCHERX + "/.coreservices.notificationlistener.TvNotificationListenerService";
    static final String RESOLVE_HOME = "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN";
    static final String ROLE_HOLDERS = "cmd role get-role-holders --user 0 android.app.role.HOME 2> /dev/null";
    static final Pattern PKG = Pattern.compile("[A-Za-z0-9_.]+");
    static final Pattern COMPONENT = Pattern.compile("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+");
    static final int MIN_WEBVIEW = 80, SAFETY_SECONDS = 300;

    /** Runs one command in the TV's shell (an {@link AdbClient} on the TV, a fake in the tests). */
    interface Shell { AdbClient.Result run(String command) throws IOException; }
    /** Small text files: the ledger and the journals (files in the app's private folder on the TV). */
    interface Store { String get(String name); void put(String name, String text) throws IOException; }
    /** One line for the screen (never a name of a network, a serial or an address). */
    interface Out { void line(String text); }
    /**
     * The in-app safety switch: after the given seconds the stock home is put back unless it was disarmed. arm returns false when no alarm could
     * be set (then the home switch does not start); exact says whether it will fire on time (Android 12 and newer need a permission for that).
     */
    interface Deadman { boolean arm(int seconds); void disarm(); boolean exact(); }
    interface Sleeper { void sleep(int ms) throws IOException; }

    enum Status { DONE, SKIPPED, FAILED }

    static final class Result {
        final Status status;
        final String message;
        Result(Status status, String message) { this.status = status; this.message = message; }
        @Override public String toString() { return status + ": " + message; }
    }

    private final Shell sh;
    private final Store store;
    private final Out out;
    private final Deadman deadman;
    private final Sleeper sleeper;
    private final String stamp;
    private int sdk = 0;

    SetupEngine(Shell sh, Store store, Out out, Deadman deadman, Sleeper sleeper, String stamp) {
        this.sh = sh; this.store = store; this.out = out; this.deadman = deadman; this.sleeper = sleeper; this.stamp = stamp;
    }

    // ---- the plan the screen shows before «ابدأ» ------------------------------------------------------------------------------------

    /** What the run will do, in order, in words (the screen shows exactly this). */
    static List<String> plan() {
        List<String> p = new ArrayList<>();
        p.add("نفحص أن الجهاز تلفزيون أندرويد، وأن التلفزيون يجاوب على أسئلتنا.");
        p.add("نعطي تطبيق تلفزيونك الأذونات اللازمة (الموقع، المنبّهات الدقيقة للأذان، تثبيت التحديثات).");
        p.add("نوقف " + safeCount() + " تطبيقاً من قائمة التطبيقات الآمنة (إعلانات وتتبّع وعروض)، ونسجّل ما أوقفناه لنرجعه بزر واحد. ما نلمس تطبيقاً كان موقوفاً من قبل، ولا النظام، ولا ما تحتاجه (البث، الإدخال، المتجر).");
        p.add("نفعّل شاشة التوقف لو كانت مثبّتة ونسخة WebView تكفي، وإلا نتركها ونقول لك.");
        p.add("نجعل تلفزيونك الشاشة الرئيسية. تنتظر الشاشة 5 دقائق: إن ما اشتغلت ترجع الشاشة الأصلية وحدها.");
        return p;
    }

    static int safeCount() { return filterSafe(SetupLists.SAFE).size(); }

    /** One value for the TV's shell, in single quotes: a `$Inner` in a component name must not be expanded there. Never called with an apostrophe in it. */
    static String q(String v) {
        if (v.indexOf('\'') >= 0 || v.indexOf('\n') >= 0) throw new IllegalArgumentException("not a value the shell gets");
        return "'" + v + "'";
    }

    // ---- parsing (pure) -------------------------------------------------------------------------------------------------------------

    /** The packages of a `pm list packages` answer; null when it does not end with its marker (the TV did not say) or has no «android» in it. */
    static Set<String> packagesOf(String text) {
        if (text == null) return null;
        String[] lines = text.replace("\r", "").split("\n");
        int last = lines.length - 1;
        while (last >= 0 && lines[last].trim().isEmpty()) last--;
        if (last < 0 || !lines[last].trim().equals("END_OF_LIST")) return null;
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < last; i++) {
            String l = lines[i].trim();
            if (l.startsWith("package:")) out.add(l.substring("package:".length()).trim());
        }
        return out;
    }

    /** The safe list without anything in the never-turn-off lists (keep.txt, core.txt) and without anything that is not a package name. */
    static List<String> filterSafe(String[] safe) {
        List<String> out = new ArrayList<>();
        for (String p : safe) {
            if (!PKG.matcher(p).matches() || contains(SetupLists.KEEP, p)) continue;
            boolean core = false;
            for (String re : SetupLists.CORE) if (p.matches(re)) { core = true; break; }
            if (!core && !out.contains(p)) out.add(p);
        }
        return out;
    }

    private static boolean contains(String[] a, String x) {
        for (String s : a) if (s.equals(x)) return true;
        return false;
    }

    /** Lines of text as a trimmed, de-duplicated list of package names (anything else on a line is dropped). */
    static List<String> ledgerNames(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String l : text.replace("\r", "").split("\n")) {
            l = l.trim();
            if (PKG.matcher(l).matches() && !out.contains(l)) out.add(l);
        }
        return out;
    }

    /** The WebView major version from `dumpsys webviewupdate`, or -1 when the TV does not say. */
    static int webviewMajor(String dump) {
        if (dump == null) return -1;
        Matcher m = Pattern.compile("Current WebView package \\(name, version\\): \\(([\\w.]+), (\\d+)[\\w.\\-]*\\)").matcher(dump);
        return m.find() ? Integer.parseInt(m.group(2)) : -1;
    }

    // ---- the TV's answers -----------------------------------------------------------------------------------------------------------

    private String text(String command) throws IOException { return sh.run(command).output.replace("\r", ""); }

    /** `pm list packages …` with an end marker; null (the TV did not say) when the marker is missing or «android» is not in it. */
    private Set<String> packages(String command) throws IOException {
        Set<String> p = packagesOf(text(command + "; echo END_OF_LIST"));
        if (p == null) return null;
        return command.equals("pm list packages -d") || p.contains("android") ? p : null;   // the full list must hold the android package itself
    }

    private String lastLine(String text) {
        String last = "";
        for (String l : text.replace("\r", "").split("\n")) if (!l.trim().isEmpty()) last = l.trim();
        return last;
    }

    /** What Home resolves to now («pkg/.Activity»), or «» when the TV said nothing. */
    String resolveHome() throws IOException { return lastLine(text(RESOLVE_HOME)); }

    private String roleHolder() throws IOException {
        String l = text(ROLE_HOLDERS).trim().split("\n")[0].trim();
        return PKG.matcher(l).matches() ? l : "";
    }

    /** A value of `settings get`: null when the TV said nothing; the word «null» means «not set». */
    private String setting(String ns, String key) throws IOException {
        String t = lastLine(text("settings get " + ns + " " + key));
        return t.isEmpty() ? null : t;
    }

    private Result done(String m) { out.line(m); return new Result(Status.DONE, m); }
    private Result skipped(String m) { out.line(m); return new Result(Status.SKIPPED, m); }
    private Result failed(String m) { out.line(m); return new Result(Status.FAILED, m); }

    // ---- 1. the checks --------------------------------------------------------------------------------------------------------------

    Result precheck() throws IOException {
        String sdkText = lastLine(text("getprop ro.build.version.sdk")).replaceAll("[^0-9]", "");
        sdk = sdkText.isEmpty() ? 0 : Integer.parseInt(sdkText);
        String features = text("pm list features");
        if (!features.contains("feature:")) return failed("لم يقل التلفزيون ما هو (قائمة مزاياه جاءت فارغة)، فما غيّرنا شيئاً. انتظر دقيقة وجرّب مرة ثانية.");
        if (!features.contains("feature:android.software.leanback")) return failed("هذا الجهاز مو تلفزيون أندرويد (ما فيه leanback)، فما غيّرنا شيئاً.");
        Set<String> pk = packages("pm list packages");
        if (pk == null) return failed("لم يقل التلفزيون قائمة تطبيقاته، فما غيّرنا شيئاً. جرّب مرة ثانية.");
        return done("التلفزيون جاهز: أندرويد تلفزيون، ويجاوب.");
    }

    // ---- 2. the grants (optimize/install.sh) ----------------------------------------------------------------------------------------

    /** allow, ignore, deny, default or foreground from an `appops get` answer; «» when the TV said nothing usable. */
    static String modeOf(String text) {
        if (text == null) return "";
        Matcher m = Pattern.compile("[A-Z_]+: *(allow|ignore|deny|default|foreground)").matcher(text);
        if (m.find()) return m.group(1);
        return text.contains("No operations") ? "default" : "";
    }

    Result grants() throws IOException {
        List<String> bad = new ArrayList<>();
        if (sh.run("pm grant " + SELF + " android.permission.ACCESS_FINE_LOCATION").code != 0) bad.add("الموقع");
        if (sdk >= 31) {
            // exact alarms for the adhan; the mode it had is kept once, so the undo gives back that and nothing else
            String was = modeOf(text("appops get " + SELF + " SCHEDULE_EXACT_ALARM"));
            if (!was.equals("allow")) {
                String saved = store.get("exact-alarm-appop.txt");
                if ((saved == null || saved.trim().isEmpty()) && !was.isEmpty()) store.put("exact-alarm-appop.txt", was + "\n");
                sh.run("appops set " + SELF + " SCHEDULE_EXACT_ALARM allow");
                if (!modeOf(text("appops get " + SELF + " SCHEDULE_EXACT_ALARM")).equals("allow")) bad.add("المنبّهات الدقيقة");
            }
        }
        // «install unknown apps» for the TV's own update: the mode it had is kept once, and only Android's own default is replaced
        String saved = store.get("install-appop.txt");
        if (saved == null || saved.trim().isEmpty()) {
            String mode = modeOf(text("appops get " + SELF + " REQUEST_INSTALL_PACKAGES"));
            if (!mode.isEmpty()) { store.put("install-appop.txt", mode + "\n"); saved = mode; }
        }
        if (saved != null && saved.trim().equals("default")) {
            sh.run("appops set " + SELF + " REQUEST_INSTALL_PACKAGES allow");
            if (!modeOf(text("appops get " + SELF + " REQUEST_INSTALL_PACKAGES")).equals("allow")) bad.add("تثبيت التحديثات");
        }
        if (!bad.isEmpty()) return failed("لم يقبل التلفزيون بعض الأذونات (" + String.join("، ", bad) + "). أكمل الإعداد؛ تلك الميزات قد لا تعمل.");
        return done("أعطينا تلفزيونك الأذونات اللازمة.");
    }

    // ---- 3. the safe list (optimize/disable.sh) -------------------------------------------------------------------------------------

    Result debloat() throws IOException {
        List<String> pkgs = filterSafe(SetupLists.SAFE);
        Set<String> installed = packages("pm list packages"), wasOff = packages("pm list packages -d");
        if (installed == null || wasOff == null) {
            return failed("لم يقل التلفزيون أي التطبيقات عنده وأيها موقوف أصلاً، فما أوقفنا شيئاً (ما نقدر نرجعها بدقة). جرّب مرة ثانية.");
        }
        List<String> intended = new ArrayList<>();
        for (String p : pkgs) if (installed.contains(p) && !wasOff.contains(p)) intended.add(p);
        List<String> prev = ledgerNames(store.get("disabled-by-us.txt"));
        remember(prev, intended);    // written BEFORE the commands: a run that dies half way is still undone by the undo
        for (String p : pkgs) sh.run("pm disable-user --user 0 " + p);
        Set<String> nowOff = packages("pm list packages -d");
        if (nowOff == null) return failed("أوقفنا التطبيقات لكن التلفزيون لم يؤكد القائمة. الزر «تراجع» يرجعها كلها بأمان.");
        List<String> confirmed = new ArrayList<>();
        for (String p : intended) if (nowOff.contains(p)) confirmed.add(p);
        remember(prev, confirmed);   // the ledger holds what the TV says is off now
        return done("أوقفنا " + confirmed.size() + " تطبيقاً من القائمة الآمنة" + (intended.size() > confirmed.size() ? " (" + (intended.size() - confirmed.size()) + " رفض التلفزيون إيقافها)." : "."));
    }

    private void remember(List<String> prev, List<String> add) throws IOException {
        List<String> all = new ArrayList<>(prev);
        for (String p : add) if (!all.contains(p)) all.add(p);
        StringBuilder b = new StringBuilder();
        for (String p : all) b.append(p).append('\n');
        store.put("disabled-by-us.txt", b.toString());
    }

    // ---- 4. the screensaver (optimize/install.sh saver) -----------------------------------------------------------------------------

    Result screensaver() throws IOException {
        Set<String> pk = packages("pm list packages");
        if (pk == null) return failed("لم يقل التلفزيون قائمة تطبيقاته، فما غيّرنا شاشة التوقف.");
        if (!pk.contains(SAVER)) return skipped("شاشة التوقف غير مثبّتة بعد: ثبّتها من «تحديث تلفزيوني» ثم أعد الإعداد. ما غيّرنا شيئاً.");
        int web = webviewMajor(text("dumpsys webviewupdate"));
        if (web < 0) return skipped("لم يقل التلفزيون نسخة WebView، فتركنا شاشة التوقف كما هي (النسخة القديمة لا تشغّلها). جرّب مرة ثانية.");
        if (web < MIN_WEBVIEW) return skipped("نسخة WebView في التلفزيون قديمة (" + web + ") وشاشة التوقف تحتاج 80 أو أحدث. حدّث «Android System WebView» من متجر Play ثم أعد الإعداد.");
        // what the TV had, once, before anything is changed (null = it was never set)
        String savedBefore = store.get("screensaver-before.txt");
        if (savedBefore == null || savedBefore.trim().isEmpty()) {
            String comps = setting("secure", "screensaver_components"), enabled = setting("secure", "screensaver_enabled"), def = setting("secure", "screensaver_default_component");
            if (comps == null || enabled == null || def == null) return failed("لم يقل التلفزيون إعداد شاشة التوقف الحالي، فما غيّرناه. جرّب مرة ثانية.");
            store.put("screensaver-before.txt", comps + "\n" + enabled + "\n" + def + "\n");
        }
        sh.run("settings put secure screensaver_enabled 1");
        sh.run("settings put secure screensaver_components " + q(OURS_DREAM));
        sh.run("settings put secure screensaver_default_component " + q(OURS_DREAM));
        String got = setting("secure", "screensaver_components");
        if (got == null || !got.contains(SAVER + "/")) return failed("التلفزيون لم يقبل شاشة التوقف؛ اخترها من إعدادات التلفزيون (شاشة التوقف). «تراجع» يرجع ما كان.");
        return done("فعّلنا شاشة التوقف.");
    }

    // ---- 5. the home switch (optimize/install.sh switch_home) -----------------------------------------------------------------------

    /** The journal's fields, read the way optimize/home-journal.sh reads them. */
    String journal(String key) {
        String j = store.get("home-switch.json");
        if (j == null) return "";
        Matcher m = Pattern.compile("^  \"" + Pattern.quote(key) + "\": \"(.*)\",?$", Pattern.MULTILINE).matcher(j);
        return m.find() ? m.group(1) : "";
    }

    private void journalWrite(String state, String stock, String holder, String wizard, String stockBefore, String safety) throws IOException {
        store.put("home-switch.json", "{\n  \"nonce\": \"" + "app" + "\",\n  \"state\": \"" + state + "\",\n  \"stock\": \"" + stock + "\",\n  \"role_holder_before\": \""
            + holder + "\",\n  \"wizard_before\": \"" + wizard + "\",\n  \"stock_before\": \"" + stockBefore + "\",\n  \"safety_switch\": \"" + safety + "\",\n  \"written\": \"" + stamp + "\"\n}\n");
    }

    private void journalState(String state) throws IOException {
        String j = store.get("home-switch.json");
        if (j != null) store.put("home-switch.json", j.replaceAll("(?m)^  \"state\": \".*\",", "  \"state\": \"" + state + "\","));
    }

    private boolean wraithAnswersHome() throws IOException {
        return text("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null").contains("setupwraith");
    }

    /** owned: a saved network belongs to the setup wizard · other: none does · unknown: the TV did not say. */
    private String wifiOwner() throws IOException {
        Matcher m = Pattern.compile("cname=([^\\s]*)").matcher(text("dumpsys wifi"));
        boolean any = false, owned = false;
        while (m.find()) { any = true; if (m.group(1).contains("setupwraith")) owned = true; }
        return !any ? "unknown" : owned ? "owned" : "other";
    }

    private boolean ours(String top) { return top.contains(SELF + "/") && top.contains("MainActivity"); }

    Result home() throws IOException {
        Set<String> pk = packages("pm list packages");
        if (pk == null) return failed("لم يقل التلفزيون قائمة تطبيقاته، فما بدّلنا الشاشة الرئيسية.");
        String stock = pk.contains(LAUNCHERX) ? LAUNCHERX_HOME : pk.contains(TVLAUNCHER) ? TVLAUNCHER_HOME : null;
        if (stock == null) return skipped("ما عرفنا ما الشاشة الرئيسية الأصلية لهذا التلفزيون، فتركناها كما هي (ما غيّرنا شيئاً). تلفزيونك يشتغل كتطبيق.");
        String stockPkg = stock.substring(0, stock.indexOf('/'));
        String res = resolveHome();
        if (res.startsWith(SELF + "/") && !"switching".equals(journal("state"))) return done("تلفزيونك هو الشاشة الرئيسية أصلاً.");
        boolean wizardListed = false;
        if (wraithAnswersHome()) {
            String owner = wifiOwner();
            if (owner.equals("other")) wizardListed = true;
            else out.line("وسيط الإعداد الأول في التلفزيون يملك شبكة الواي فاي المحفوظة (أو لم نعرف)، فنتركه شغّالاً كي لا تضيع الشبكة.");
        }
        // who held Home before (the undo gives it back): the role, else the journal of an earlier try, else the app that answers Home now
        String holder = roleHolder();
        if (holder.equals(SELF) || holder.isEmpty()) holder = journalHolder();
        if (holder.isEmpty()) {
            if (res.isEmpty()) return failed("لم نعرف أي تطبيق هو الشاشة الرئيسية الآن، فما بدّلناها (لن نقدر نرجع الصحيح). جرّب مرة ثانية.");
            String pkg = res.contains("/") ? res.substring(0, res.indexOf('/')) : "";
            holder = pkg.equals(stockPkg) ? pkg : "";   // only the stock home counts as «before»; anything else: a restore goes to the stock home
        }
        Set<String> off = packages("pm list packages -d");
        String wiz = "absent", stk = "absent";
        if (off != null) { wiz = off.contains(WRAITH) ? "disabled" : "enabled"; stk = off.contains(stockPkg) ? "disabled" : "enabled"; }
        if (wiz.equals("disabled") && "enabled".equals(journal("wizard_before"))) wiz = "enabled";    // we turned it off in an earlier try
        if (stk.equals("disabled") && "enabled".equals(journal("stock_before"))) stk = "enabled";
        journalWrite("switching", stock, holder, wiz, stk, "armed");
        // the safety switch first, and the switch does not start without it: its deadline is on disk, so a restart or a force-stop does not lose it
        store.put("home-deadline.txt", (now() + SAFETY_SECONDS * 1000L) + "\n");
        if (!deadman.arm(SAFETY_SECONDS)) {
            clearDeadline();
            journalState("aborted");
            return failed("ما قدرنا نضبط مفتاح الأمان (منبّه الرجوع للشاشة الأصلية)، فما بدّلنا الشاشة الرئيسية. ما غيّرنا شيئاً.");
        }
        if (!deadman.exact()) out.line("مفتاح الأمان قد يتأخر بضع دقائق على هذا التلفزيون (لا إذن للمنبّهات الدقيقة)، فلا تطفئه أثناء الإعداد.");
        out.line("الشاشة الأصلية سترجع وحدها بعد 5 دقائق إن لم تنجح الشاشة الجديدة.");
        AdbClient.Result role = sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);
        AdbClient.Result setHome = sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
        boolean roleOk = role.code == 0 || (sdk > 0 && sdk < 29);    // Android 9 and older have no `cmd role`
        if (!roleOk || setHome.code != 0) return abort(stock, stockPkg, holder, "ما قبل التلفزيون تعيين شاشتنا شاشةً رئيسية، فما عطّلنا الأصلية.", false);
        if (wizardListed) sh.run("pm disable-user --user 0 " + WRAITH);
        AdbClient.Result disable = sh.run("pm disable-user --user 0 " + stockPkg);
        if (disable.output.toLowerCase().contains("illegal")) {   // Nokia Streaming Box 8000: no switch there
            return abort(stock, stockPkg, holder, "هذا الجهاز لا يسمح بإيقاف شاشته الرئيسية الأصلية («This command is illegal»)، فبقيت كما هي. بقية الإعداد لا تحتاجها.", true);
        }
        if (disable.code != 0) return abort(stock, stockPkg, holder, "ما قبل التلفزيون إيقاف الشاشة الرئيسية الأصلية.", false);
        sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
        sh.run("am force-stop com.android.tv.settings");
        sh.run("input keyevent KEYCODE_HOME");
        boolean ok = verifyHome();
        if (!ok) {
            out.line("الشاشة الرئيسية لم تفتح تلفزيونك: نسأل مرة ثانية.");
            sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);
            sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
            sh.run("input keyevent KEYCODE_HOME");
            ok = verifyHome();
        }
        if (!ok) return abort(stock, stockPkg, holder, "الشاشة الرئيسية لم تفتح تلفزيونك.", false);
        deadman.disarm();
        clearDeadline();
        journalState("done");
        for (String h : SetupLists.HOME_HELPERS) if (pk.contains(h)) sh.run("pm disable-user --user 0 " + h);   // only now: the stock home's helpers are dead weight
        return done("تلفزيونك الآن هو الشاشة الرئيسية. اضغط زر الرئيسية في الريموت للتأكد.");
    }

    /**
     * The switch cannot go on: the stock home is put back and CHECKED (Home resolves to it). Only then is the journal «restored» and the safety switch
     * disarmed; if Home cannot be confirmed, both stay, so the safety switch keeps trying by itself.
     */
    private Result abort(String stock, String stockPkg, String holder, String why, boolean skipOnly) throws IOException {
        if (putStockBack(stock, stockPkg, holder, false)) {
            deadman.disarm();
            clearDeadline();
            journalState("restored");
            return skipOnly ? skipped(why) : failed(why + " رجّعنا الشاشة الأصلية وتأكدنا منها. ما ضاع شيء.");
        }
        return failed(why + " حاولنا إرجاع الشاشة الأصلية ولم نتأكد منها بعد؛ مفتاح الأمان سيعيد المحاولة تلقائياً. إن لم تظهر فاضغط «تراجع».");
    }

    private void clearDeadline() throws IOException { store.put("home-deadline.txt", ""); }

    long now() { return System.currentTimeMillis(); }

    /** The wait in seconds before retry number n of the safety switch: 30, 60, 120, 240, 480, then 600 at most. */
    static int backoffSeconds(int n) {
        long w = 30;
        for (int i = 0; i < n && w < 600; i++) w *= 2;
        return (int) Math.min(w, 600);
    }

    /** The deadline of the safety switch in epoch milliseconds, 0 when none is set. */
    long deadline() {
        String d = store.get("home-deadline.txt");
        try { return d == null || d.trim().isEmpty() ? 0 : Long.parseLong(d.trim()); } catch (NumberFormatException e) { return 0; }
    }

    /** The package that held Home before, from the journal: a package name or the stock home (a journal value is never trusted as a shell word). */
    private String journalHolder() {
        String h = journal("role_holder_before");
        return PKG.matcher(h).matches() && !h.equals(SELF) ? h : "";
    }

    /** Home really resolves to ours AND ours is what is on screen after Home (a few tries: a slow TV). */
    private boolean verifyHome() throws IOException {
        for (int i = 0; i < 3; i++) {
            sleeper.sleep(6000);
            String res = resolveHome();
            String top = lastLine(text("dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -n 1"));
            if (res.startsWith(SELF + "/") && ours(top)) return true;
            sh.run("input keyevent KEYCODE_HOME");
        }
        return false;
    }

    /**
     * The stock home (or the app that held Home before) is given Home again, and then CHECKED: Home must resolve to it. withHelpers: the stock
     * home's helpers come back too (the undo). Returns whether Home is confirmed; nothing says «restored» before this is true.
     */
    private boolean putStockBack(String stock, String stockPkg, String holder, boolean withHelpers) throws IOException {
        String back = holder.isEmpty() || holder.equals(SELF) || !PKG.matcher(holder).matches() ? stockPkg : holder;
        if (withHelpers) for (String h : SetupLists.HOME_HELPERS) sh.run("pm enable " + h);
        sh.run("pm enable " + WRAITH);
        sh.run("pm enable " + stockPkg);
        if (!back.equals(stockPkg)) sh.run("pm enable " + back);
        sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + back);
        if (back.equals(stockPkg)) sh.run("cmd package set-home-activity --user 0 " + q(stock));
        if (stockPkg.equals(LAUNCHERX)) sh.run("cmd notification allow_listener " + NOTIFICATION_LISTENER);
        sh.run("input keyevent KEYCODE_HOME");
        if (resolveHome().startsWith(back + "/")) return true;   // a quick TV is checked at once, before any wait
        for (int i = 0; i < 3; i++) {
            sleeper.sleep(3000);
            if (resolveHome().startsWith(back + "/")) return true;
            sh.run("input keyevent KEYCODE_HOME");
        }
        return false;
    }

    /**
     * At the start of the app, at boot, and when the safety switch fires: a switch that was cut off (the connection dropped, the TV restarted) is
     * undone from the journal when Home is not ours. «switching» stays until the stock home is CHECKED; null when there was nothing to do.
     */
    Result recoverIfCutOff() throws IOException {
        if (!"switching".equals(journal("state"))) return null;
        String res = resolveHome();
        if (res.isEmpty()) return failed("بدأ تبديل الشاشة الرئيسية ثم انقطع، والتلفزيون لا يجاوب الآن. سنعيد المحاولة تلقائياً.");
        if (res.startsWith(SELF + "/")) {    // ours is Home, and this code is running: the switch took
            deadman.disarm();
            clearDeadline();
            journalState("done");
            return done("سجّلنا أن تلفزيونك صار الشاشة الرئيسية بعد تبديل انقطع (لم نفحص الشاشة بعد). اضغط زر الرئيسية للتأكد.");
        }
        String stock = journal("stock");
        if (stock.isEmpty() || !COMPONENT.matcher(stock).matches()) return failed("سجل التبديل ناقص، فاضغط «تراجع».");
        String pkg = stock.substring(0, stock.indexOf('/'));
        if (!putStockBack(stock, pkg, journalHolder(), false)) return failed("حاولنا إرجاع الشاشة الأصلية ولم نتأكد منها بعد؛ سنعيد المحاولة تلقائياً.");
        deadman.disarm();
        clearDeadline();
        journalState("restored");
        return done("كان تبديل الشاشة الرئيسية قد انقطع؛ رجّعنا الشاشة الأصلية وتأكدنا منها.");
    }

    // ---- the undo (optimize/restore.sh) ---------------------------------------------------------------------------------------------

    /**
     * In optimize/reset.sh's order: the home first (never leave the TV without one), then the apps in the ledger, the protected features (keep.sh),
     * the screensaver and the appops as they were, TCL's auto-start, and the speed tweaks last.
     */
    Result undo() throws IOException {
        List<String> problems = new ArrayList<>();
        String stock = journal("stock");
        if (stock.isEmpty() || !COMPONENT.matcher(stock).matches()) {
            Set<String> pk = packages("pm list packages");
            if (pk == null) return failed("لم يقل التلفزيون قائمة تطبيقاته، فلم نمسّ الشاشة الرئيسية. جرّب «تراجع» مرة ثانية.");
            stock = pk.contains(LAUNCHERX) ? LAUNCHERX_HOME : pk.contains(TVLAUNCHER) ? TVLAUNCHER_HOME : "";
        }
        if (!stock.isEmpty()) {
            String pkg = stock.substring(0, stock.indexOf('/'));
            if (putStockBack(stock, pkg, journalHolder(), true)) {
                deadman.disarm();
                clearDeadline();
                if (store.get("home-switch.json") != null) journalState("restored");
            } else {
                problems.add("لم نتأكد من رجوع الشاشة الرئيسية الأصلية؛ تركنا مفتاح الأمان يعيد المحاولة. إن لم تظهر فاضغط «تراجع» مرة ثانية.");
            }
        } else {
            problems.add("ما عرفنا الشاشة الرئيسية الأصلية، فاضغط زر الرئيسية؛ وإن لم يفتح شيء فاختر الشاشة من إعدادات التلفزيون (التطبيقات، التطبيقات الافتراضية).");
        }
        // the apps our run turned off
        List<String> ledger = ledgerNames(store.get("disabled-by-us.txt"));
        for (String p : ledger) sh.run("pm enable " + p);
        Set<String> off = packages("pm list packages -d");
        if (off == null) {
            problems.add("أرجعنا التطبيقات لكن التلفزيون لم يؤكد القائمة؛ اضغط «تراجع» مرة ثانية.");
        } else {
            boolean again = false;
            for (String p : ledger) if (off.contains(p)) { sh.run("pm enable --user 0 " + p); again = true; }
            Set<String> off2 = again ? packages("pm list packages -d") : off;
            List<String> keep = new ArrayList<>();
            if (off2 == null) keep = ledger; else for (String p : ledger) if (off2.contains(p)) keep.add(p);
            StringBuilder b = new StringBuilder();
            for (String p : keep) b.append(p).append('\n');
            store.put("disabled-by-us.txt", b.toString());
            if (!keep.isEmpty()) problems.add(keep.size() + " تطبيقاً لم يقبل التلفزيون تشغيله؛ جرّب «تراجع» مرة ثانية.");
        }
        // the protected features (optimize/reset.sh runs optimize/keep.sh here): only ever turned on; its own lines say what it found. Only after a
        // full setup (it saved backup.txt): the TV button's own setup never touched them, and its undo stays what it was
        String backup = store.get(SetupPrep.BACKUP);
        if (backup != null && !backup.isEmpty()) new SetupPrep(sh, store, out).keep();
        // the screensaver as the TV had it (null: it was never set)
        String before = store.get("screensaver-before.txt");
        if (before != null && !before.trim().isEmpty()) {
            String[] v = before.split("\n", -1);
            if (v.length >= 3) {
                String comps = v[0].trim(), enabled = v[1].trim();
                sh.run("settings delete secure screensaver_default_component");
                if (comps.equals("null") || comps.contains(SAVER + "/")) sh.run("settings delete secure screensaver_components");
                else if (comps.matches("[A-Za-z0-9_.$/,]+") && COMPONENT.matcher(comps.split(",")[0]).matches()) sh.run("settings put secure screensaver_components " + q(comps));
                if (enabled.equals("null")) sh.run("settings delete secure screensaver_enabled");
                else if (enabled.matches("[01]")) sh.run("settings put secure screensaver_enabled " + enabled);
                store.put("screensaver-before.txt", "");
            }
        }
        // the appops: only what we changed, back to the mode it had
        String exact = store.get("exact-alarm-appop.txt");
        if (exact != null && exact.trim().matches("ignore|deny|default|foreground")) {
            sh.run("appops set " + SELF + " SCHEDULE_EXACT_ALARM " + exact.trim());
            store.put("exact-alarm-appop.txt", "");
        }
        String mode = store.get("install-appop.txt");
        if (mode != null && mode.trim().matches("allow|ignore|deny|default|foreground")) sh.run("appops set " + SELF + " REQUEST_INSTALL_PACKAGES " + mode.trim());
        // TCL's auto-start, back to what each op had (autostart-before.txt)
        new SetupPrep(sh, store, out).autostartUndo();
        // the tweaks last, as optimize/reset.sh does: exactly what the TV had (tweaks-journal.txt); nothing when the app never changed one
        if (new SetupTweaks(sh, store, out).restore().status != Status.DONE) problems.add("لم يقبل التلفزيون إرجاع بعض إعدادات السرعة؛ اضغط «تراجع» مرة ثانية.");
        if (!problems.isEmpty()) return failed(String.join(" ", problems));
        return done("رجّعنا تلفزيونك كما كان. تطبيق تلفزيونك يبقى مثبتاً؛ تقدر تحذفه من إعدادات التلفزيون (التطبيقات).");
    }
}
