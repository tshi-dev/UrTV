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
    /** the person's choice on a Chromecast (optimize/cast.sh): «off» = our home first, the wizard may go; anything else = casting kept */
    static final String CAST_CHOICE = "cast.txt";
    static final String LAUNCHERX = "com.google.android.apps.tv.launcherx", LAUNCHERX_HOME = LAUNCHERX + "/.home.HomeActivity";
    static final String TVLAUNCHER = "com.google.android.tvlauncher", TVLAUNCHER_HOME = TVLAUNCHER + "/.MainActivity";
    static final String NOTIFICATION_LISTENER = LAUNCHERX + "/.coreservices.notificationlistener.TvNotificationListenerService";
    static final String RESOLVE_HOME = "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN";
    static final String ROLE_HOLDERS = "cmd role get-role-holders --user 0 android.app.role.HOME 2> /dev/null";
    static final Pattern PKG = Pattern.compile("[A-Za-z0-9_.]+");
    static final Pattern COMPONENT = Pattern.compile("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+");
    static final int MIN_WEBVIEW = 80, SAFETY_SECONDS = 300;
    /** install.sh's wifi-ask.txt: the person was asked once to forget the Wi-Fi and join it again («wifi-changed» added when the safe order saw it change). */
    static final String WIFI_ASK = "wifi-ask.txt";

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
            if (!core && !CommunityMatch.input(p) && !out.contains(p)) out.add(p);
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
        if (!intended.isEmpty()) {   // disable.sh's input guard: what the TV says drives an HDMI input or is a keyboard stays on
            InputGuard.Answer a = InputGuard.parse(text(InputGuard.command(intended)));
            if (a == null) return failed("لم يقل التلفزيون أي التطبيقات تشغّل مداخل HDMI أو لوحة المفاتيح، فما أوقفنا شيئاً. جرّب مرة ثانية.");
            if (a.hung) return failed("التلفزيون أوقف تقارير النظام (hung system process)، فما عرفنا أي التطبيقات تشغّل مداخل HDMI، وما أوقفنا شيئاً. أعد تشغيل التلفزيون ثم جرّب مرة ثانية.");
            if (!a.left().isEmpty()) {
                out.line("تركنا شغّالاً ما يشغّل مداخل التلفزيون (HDMI، الموالف) أو لوحة المفاتيح: " + String.join(" ", a.left()));
                pkgs = InputGuard.without(pkgs, a.left());
                intended = InputGuard.without(intended, a.left());
            }
        }
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

    /** What debloat would turn off on this TV now (installed, not off already): the start screen's count. Null when the TV did not say. */
    List<String> safeToTurnOff() throws IOException {
        Set<String> installed = packages("pm list packages"), wasOff = packages("pm list packages -d");
        if (installed == null || wasOff == null) return null;
        List<String> out = new ArrayList<>();
        for (String p : filterSafe(SetupLists.SAFE)) if (installed.contains(p) && !wasOff.contains(p)) out.add(p);
        return out;
    }

    private void remember(List<String> prev, List<String> add) throws IOException {
        store.put("disabled-by-us.txt", ledgerText(prev, add, store.get(SetupPrep.BACKUP)));
    }

    /**
     * 4.4 (reports, cause 10): the ledger never names a package that was already off when the TV came to us (the backup's «disabled packages», taken
     * once, before anything was changed): the undo must not turn on what the maker had off, and the report must not call it ours. The earlier
     * ledger's names and the new ones, in order, once each, without those.
     */
    static String ledgerText(List<String> prev, List<String> add, String backup) {
        Set<String> before = SetupPrep.section(backup, "disabled packages");
        List<String> all = new ArrayList<>();
        for (List<String> l : java.util.Arrays.asList(prev, add)) for (String p : l) if (!all.contains(p) && !before.contains(p)) all.add(p);
        StringBuilder b = new StringBuilder();
        for (String p : all) b.append(p).append('\n');
        return b.toString();
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
            + holder + "\",\n  \"wizard_before\": \"" + wizard + "\",\n  \"stock_before\": \"" + stockBefore + "\",\n  \"safety_switch\": \"" + safety + "\",\n  \"written\": \"" + stamp
            + "\",\n  \"step\": \"armed\"\n}\n");
    }

    /**
     * home-journal.sh's jstep: the step of the switch that is about to act, written BEFORE it acts (armed, wizard_off, wifi_check, home_start, stock_off,
     * switch, verify), so after a cut the journal says how far the switch got. A journal without the field (an older one) is left as it is.
     */
    void journalStep(String step) throws IOException {
        String j = store.get("home-switch.json");
        if (j != null && j.contains("  \"step\": \"")) store.put("home-switch.json", j.replaceAll("(?m)^  \"step\": \"[^\"]*\"", "  \"step\": \"" + step + "\""));
    }

    private void journalState(String state) throws IOException {
        String j = store.get("home-switch.json");
        if (j != null) store.put("home-switch.json", j.replaceAll("(?m)^  \"state\": \".*\",", "  \"state\": \"" + state + "\","));
    }

    /**
     * 4.3 A3, install.sh's «another maker's own home»: on a TV without Google's homes (a CVTE board's com.cvte.tv.launcher, a MediaTek set's own)
     * the stock home is the one app that answers Home besides ours, the setup wizard, the settings apps, a bare FallbackHome and Android's core,
     * and only when it is installed. Two or none: null (the switch is not made, as before). Never a guessed name.
     */
    String otherHome(Set<String> installed) throws IOException {
        return otherHome(text("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null"), installed);
    }

    static String otherHome(String query, Set<String> installed) {
        Set<String> cand = new java.util.TreeSet<>();
        for (String l : (query == null ? "" : query).split("\n")) {
            String s = l.trim();
            if (!COMPONENT.matcher(s).matches()) continue;
            String pkg = s.substring(0, s.indexOf('/'));
            if (pkg.startsWith("sa.urtv.") || pkg.equals(WRAITH) || pkg.equals("com.android.settings") || pkg.equals("com.android.tv.settings")) continue;
            if (s.matches(".*/([A-Za-z0-9_.]*\\.)?FallbackHome")) continue;
            boolean core = false;
            for (String c : SetupLists.CORE) if (pkg.matches(c)) { core = true; break; }
            if (!core) cand.add(s);
        }
        if (cand.size() != 1) return null;
        String only = cand.iterator().next();
        return installed != null && installed.contains(only.substring(0, only.indexOf('/'))) ? only : null;
    }

    private boolean wraithAnswersHome() throws IOException {
        return text("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null").contains("setupwraith");
    }

    /**
     * install.sh's wraith_casts: on a Chromecast with Google TV (and the Google TV Streamer) the cast receiver reads its device credentials from a
     * provider inside the setup wizard (…castinfo.CastInfoProvider); with the wizard off, casting from a phone and the Google Home remote say «untrusted
     * device» after the next restart (a user report on 4.1). Told by that provider, never by the model name. The wizard is then never turned off here,
     * unless the person chose our home first («cast.txt» = off, optimize/dock.sh cast off).
     */
    boolean castKeepsWizard() throws IOException {
        String c = store.get(CAST_CHOICE);
        if (c != null && c.trim().equals("off")) return false;
        return castDevice(text("getprop ro.product.manufacturer; getprop ro.product.device; dumpsys package " + WRAITH + " 2> /dev/null | grep -m 1 castinfo.CastInfoProvider"));
    }

    /**
     * install.sh's cast_device: the provider AND a Google-made device (ro.product.manufacturer «Google», or the codename sabrina, boreal or
     * kirkwood). Other makers' Google TV builds share the wizard and its provider (a TCL G07 has it, tester 4.2): there the wizard goes as before.
     */
    static boolean castDevice(String answer) {
        if (answer == null || !answer.contains("castinfo.CastInfoProvider")) return false;
        String[] l = answer.replace("\r", "").split("\n", -1);
        String maker = l.length > 0 ? l[0].trim().toLowerCase(java.util.Locale.ROOT) : "", device = l.length > 1 ? l[1].trim().toLowerCase(java.util.Locale.ROOT) : "";
        return maker.equals("google") || device.equals("sabrina") || device.equals("boreal") || device.equals("kirkwood");
    }

    /** owned: a saved network belongs to the setup wizard · other: none does · unknown: the TV did not say. */
    private String wifiOwner() throws IOException {
        int[] o = wifiOwners();
        return o[1] == 0 ? "unknown" : o[0] > 0 ? "owned" : "other";
    }

    /**
     * 4.3 A3: how many of the saved networks the setup wizard owns, and how many saved networks say an owner at all: {owned, all}. A TV can hold
     * several networks saved during its first-run wizard (a neighbour's, the shop's): each of them keeps the wizard, so the person is told to
     * forget ALL of them, not only the one in use. No network name is read or kept.
     */
    int[] wifiOwners() throws IOException { return owners(text("dumpsys wifi")); }

    static int[] owners(String dump) {
        Matcher m = Pattern.compile("cname=([^\\s]*)").matcher(dump == null ? "" : dump);
        int owned = 0, all = 0;
        while (m.find()) { all++; if (m.group(1).contains("setupwraith")) owned++; }
        return new int[] {owned, all};
    }

    /** The line said with the Wi-Fi ask (4.3 A3): the count, and «forget them all» (Western digits: the screen writes them in the person's). */
    static String ownersLine(int owned, int all) {
        String n = owned > 1 ? owned + " من " + all + " شبكات واي فاي محفوظة" : all > 1 ? "شبكة من " + all + " شبكات واي فاي محفوظة" : "شبكة الواي فاي المحفوظة";
        return n + " باسم «وسيط الإعداد». انسَ كل الشبكات المحفوظة في إعدادات التلفزيون ← الشبكة (كلها، لا الشبكة الحالية فقط)، ثم اتصل بشبكتك من جديد بكلمة المرور.";
    }

    /**
     * install.sh's wifi_state: the TV's Wi-Fi, read only, as «<connected> <saved>» (connected yes|no|unknown; saved: how many networks are saved, or
     * unknown). No network name is kept or said. Null when the TV did not answer (no end marker).
     */
    String wifiState() throws IOException {
        String o = text("cmd wifi status 2> /dev/null; echo W_END; cmd wifi list-networks 2> /dev/null; echo L_END");
        List<String> lines = new ArrayList<>();
        for (String l : o.split("\n")) lines.add(l);
        int w = lines.indexOf("W_END"), l = lines.indexOf("L_END");
        if (w < 0 || l < w) return null;
        String st = String.join("\n", lines.subList(0, w)).toLowerCase(java.util.Locale.ROOT);
        List<String> ls = lines.subList(w + 1, l);
        String c = st.contains("is connected to") ? "yes" : st.matches("(?s).*(not connected|disconnected|wifi is disabled).*") ? "no" : "unknown";
        String joined = String.join("\n", ls).toLowerCase(java.util.Locale.ROOT), n;
        if (joined.contains("network id")) {
            int k = 0;
            for (String x : ls) if (x.matches("^[ \t]*[0-9]+[ \t].*")) k++;
            n = String.valueOf(k);
        } else n = joined.contains("no networks") ? "0" : "unknown";
        return c + " " + n;
    }

    /** install.sh's wifi_same: the Wi-Fi as it was (still connected if it was, no saved network fewer, at least one). */
    static boolean wifiSame(String before, String now) {
        if (before == null || now == null) return false;
        String[] b = before.split(" "), n = now.split(" ");
        if (b.length != 2 || n.length != 2) return false;
        if (b[0].equals("yes") && !n[0].equals("yes")) return false;
        if (!b[1].matches("[0-9]+") || !n[1].matches("[0-9]+")) return false;
        int n0 = Integer.parseInt(b[1]), n1 = Integer.parseInt(n[1]);
        return n1 >= n0 && n1 >= 1;
    }

    /**
     * 4.3.2: the home switch keeps the stock home, the setup wizard and the stock home's helpers ON: it makes ours the HOME role and the home activity and nothing
     * else. A TV whose stock home or wizard was turned off could not start (a blank screen and a restart loop on some Google TVs: couch-tv's 0.10 findings, and the
     * setup that stopped at 20 to 26 %), and the wizard owning the saved Wi-Fi made the switch stop altogether. «تخلّص من واجهة المصنع» (the factory step, factoryOff)
     * is the one place that still turns the stock home off, only when the person asks for it. false = the old switch (kept for the tests of its rules).
     */
    static boolean KEEP_STOCK_ON = true;   // not final: the tests of the old switch's rules (the safe order, the wizard asked about the Wi-Fi) set it false

    /**
     * install.sh's step 0, before anything is changed: the setup wizard answers Home and owns the saved Wi-Fi (TCL G08/G10/G14, Google TV sets), so the
     * person is asked ONCE to forget the network and join it again in the TV's Settings (wifi-ask.txt says it was asked; never twice). True: ask now
     * (the run pauses; nothing was changed). We never forget, delete or change a network ourselves.
     */
    boolean askWifi() throws IOException {
        if (KEEP_STOCK_ON) return false;   // nothing is turned off by the switch any more, so the Wi-Fi is never at risk: no question
        String asked = store.get(WIFI_ASK);
        if (asked != null && !asked.trim().isEmpty()) return false;
        Set<String> pk = packages("pm list packages");
        if (pk == null || !(pk.contains(LAUNCHERX) || pk.contains(TVLAUNCHER))) return false;
        String res = resolveHome();
        if (res.isEmpty() || res.startsWith(SELF + "/")) return false;
        if (!wraithAnswersHome() || castKeepsWizard()) return false;   // a Chromecast keeps its wizard anyway
        int[] own = wifiOwners();
        if (own[0] == 0) return false;
        store.put(WIFI_ASK, "asked " + stamp + " owned=" + own[0] + "/" + own[1] + "\n");
        out.line(ownersLine(own[0], own[1]));
        return true;
    }

    /** For the start screen: will the home switch ask the person about the Wi-Fi first? (the same reads as askWifi, nothing written) */
    boolean wizardOwnsWifi() throws IOException {
        if (KEEP_STOCK_ON) return false;   // 4.3.3 (A17): the wizard is never turned off (the switch, the factory step), so its Wi-Fi is never at risk: no word on the start screen, no refusal of «تخلّص من واجهة المصنع»
        String asked = store.get(WIFI_ASK);
        if (asked != null && !asked.trim().isEmpty()) return false;
        String res = resolveHome();
        return !res.isEmpty() && !res.startsWith(SELF + "/") && wraithAnswersHome() && wifiOwner().equals("owned") && !castKeepsWizard();
    }

    private boolean ours(String top) { return top.contains(SELF + "/") && top.contains("MainActivity"); }

    /** the switch really started on this try (the journal said «switching»): HomeFails counts only these */
    private boolean homeAttempted;

    /**
     * 4.4: the home switch, counted (HomeFails): after two tries that did not make UrTV the home, the in-app switch is not offered any more on this TV; the step says
     * the one line and the setup screen shows the computer's way. A try that worked starts the count again.
     */
    Result home() throws IOException {
        // 4.4 (step 88, T1): never skipped because of earlier failures any more. On a Google TV the home step is the add-on and «wanted», which an earlier
        // build never tried: a fresh run skipped it («1 تُركت: الشاشة الرئيسية…») and the add-on was never turned on. The count still shows the
        // computer's way on the start screen; an update and «تراجع» clear it (SetupHook.clearOldFails, undo)
        homeAttempted = false;
        watchFor = null;
        Result r = homeSwitch();
        if (watchFor != null) return r;   // the Home watch is being set up (SetupRunner): finishWatch counts it
        if (homeAttempted || r.status == Status.DONE) {   // nothing tried (the TV did not say, no stock home known): nothing written
            try { store.put(HomeFails.FILE, HomeFails.after(store.get(HomeFails.FILE), homeAttempted, r.status == Status.DONE)); } catch (IOException ignored) { }
        }
        return r;
    }

    private Result homeSwitch() throws IOException {
        Set<String> pk = packages("pm list packages");
        if (pk == null) return failed("لم يقل التلفزيون قائمة تطبيقاته، فما بدّلنا الشاشة الرئيسية.");
        String stock = pk.contains(LAUNCHERX) ? LAUNCHERX_HOME : pk.contains(TVLAUNCHER) ? TVLAUNCHER_HOME : otherHome(pk);
        if (stock == null) return skipped("ما عرفنا ما الشاشة الرئيسية الأصلية لهذا التلفزيون، فتركناها كما هي (ما غيّرنا شيئاً). تلفزيونك يشتغل كتطبيق.");
        String stockPkg = stock.substring(0, stock.indexOf('/'));
        String res = resolveHome();
        if (res.startsWith(SELF + "/") && !"switching".equals(journal("state"))) return done("تلفزيونك هو الشاشة الرئيسية أصلاً.");
        boolean wizardListed = false, safeOrder = false;
        String wifiBefore = null, asked = store.get(WIFI_ASK);
        boolean wasAsked = asked != null && !asked.trim().isEmpty();
        if (KEEP_STOCK_ON) {
            // the wizard, the stock home and its helpers stay on (KEEP_STOCK_ON): nothing to decide about the Wi-Fi
        } else if (castKeepsWizard()) {
            out.line("وسيط الإعداد في هذا الجهاز يحمل شهادة البث (Chromecast)، فنتركه شغّالاً كي يشتغل البث من الجوال وريموت Google Home، ونحاول نخلي تلفزيونك الشاشة الرئيسية.");
        } else if (wraithAnswersHome()) {
            String owner = wifiOwner();
            if (owner.equals("other")) {
                wizardListed = true;
                // the network moved off the wizard after the person was asked: the safe order, if the Wi-Fi can be read
                if (wasAsked && !asked.contains("wifi-changed")) {
                    wifiBefore = wifiState();
                    if (wifiSame(wifiBefore, wifiBefore) && !wifiBefore.startsWith("unknown")) {
                        safeOrder = true;
                        out.line("شبكة الواي فاي ما عادت تتبع وسيط الإعداد: نوقف الوسيط أولاً، ثم نتأكد من الواي فاي، ثم نشغّل تلفزيونك، وبعدها فقط نوقف الشاشة الأصلية.");
                    } else {
                        out.line("شبكة الواي فاي ما عادت تتبع وسيط الإعداد، لكن التلفزيون لم يقل حالة الواي فاي الآن: نبدّل بالطريقة المعتادة.");
                    }
                }
            } else if (owner.equals("owned") && wasAsked) {
                int[] own = wifiOwners();
                out.line("شبكة الواي فاي ما زالت تتبع وسيط الإعداد (" + own[0] + " من " + own[1] + "؛ ما نُسيت كلها ورجع الاتصال من الإعدادات): نتركه شغّالاً كي لا تضيع الشبكة، ونكمل التبديل كما كان.");
            } else {
                out.line("وسيط الإعداد الأول في التلفزيون يملك شبكة الواي فاي المحفوظة (أو لم نعرف)، فنتركه شغّالاً كي لا تضيع الشبكة.");
            }
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
        homeAttempted = true;
        // the safety switch first, and the switch does not start without it: its deadline is on disk, so a restart or a force-stop does not lose it
        store.put("home-deadline.txt", (now() + SAFETY_SECONDS * 1000L) + "\n");
        if (!deadman.arm(SAFETY_SECONDS)) {
            clearDeadline();
            journalState("aborted");
            return failed("ما قدرنا نضبط مفتاح الأمان (منبّه الرجوع للشاشة الأصلية)، فما بدّلنا الشاشة الرئيسية. ما غيّرنا شيئاً.");
        }
        if (!deadman.exact()) out.line("مفتاح الأمان قد يتأخر بضع دقائق على هذا التلفزيون (لا إذن للمنبّهات الدقيقة)، فلا تطفئه أثناء الإعداد.");
        out.line("الشاشة الأصلية سترجع وحدها بعد 5 دقائق إن لم تنجح الشاشة الجديدة.");
        if (safeOrder) {
            Result r = safeSwitch(stock, stockPkg, holder, wifiBefore);
            if (r != null) return r;
            return verifyAndFinish(stock, stockPkg, holder, pk, true);
        }
        journalStep("switch");
        AdbClient.Result role = sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);
        AdbClient.Result setHome = sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
        boolean roleOk = role.code == 0 || (sdk > 0 && sdk < 29);    // Android 9 and older have no `cmd role`
        if (!roleOk || setHome.code != 0) return abort(stock, stockPkg, holder, "ما قبل التلفزيون تعيين شاشتنا شاشةً رئيسية، فما عطّلنا الأصلية.", false);
        if (KEEP_STOCK_ON) {   // ours holds Home; the stock home and the wizard are left as they are
            sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
            sh.run("am force-stop com.android.tv.settings");
            sh.run("input keyevent KEYCODE_HOME");
            // 4.4: the check is the person's: another app in front, the Home key, then who is in front. With the stock home on, Google TV's launcherx keeps the Home key
            // whatever the role says (16 TV-side runs: it «passed» on the role, and Home opened launcherx on every Google TV): then the setup sets up the add-on's
            // Home watch (SetupRunner: «إضافات تلفزيونك», installed and turned on over this adb) and checks again (finishWatch). The role stays ours meanwhile.
            journalStep("verify");
            if (homeKeyOpensOurs()) return finishHome(pk, "تلفزيونك الآن هو الشاشة الرئيسية. اضغط زر الرئيسية في الريموت للتأكد.");
            watchFor = new String[] {stock, stockPkg, holder};
            out.line("زر الرئيسية ما زال يفتح الشاشة الأصلية في هذا التلفزيون: نجرّب «إضافات تلفزيونك» التي ترجّعك لتلفزيونك.");
            return new Result(Status.FAILED, "watch needed");
        }
        if (wizardListed) sh.run("pm disable-user --user 0 " + WRAITH);
        AdbClient.Result disable = sh.run("pm disable-user --user 0 " + stockPkg);
        if (disable.output.toLowerCase().contains("illegal")) {   // Nokia Streaming Box 8000: no switch there
            return abort(stock, stockPkg, holder, "هذا الجهاز لا يسمح بإيقاف شاشته الرئيسية الأصلية («This command is illegal»)، فبقيت كما هي. بقية الإعداد لا تحتاجها.", true);
        }
        if (disable.code != 0) return abort(stock, stockPkg, holder, "ما قبل التلفزيون إيقاف الشاشة الرئيسية الأصلية.", false);
        sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
        sh.run("am force-stop com.android.tv.settings");
        sh.run("input keyevent KEYCODE_HOME");
        return verifyAndFinish(stock, stockPkg, holder, pk, false);
    }

    /**
     * install.sh's safe order (CleanYourTV, github.com/Gathin23/cleanyourtv, MIT; it held on a TCL G08 through three restarts), with the safety switch
     * already armed: a. the wizard off first, while the stock home still answers Home; b. the Wi-Fi read again (still connected, no saved network
     * fewer); c. our home started and confirmed in front; d. only then the stock home off, and the HOME role and the home activity ours. A changed
     * Wi-Fi or our home not in front puts the wizard and the stock home back (checked) and never turns the stock home off. Null: go on to the check.
     */
    private Result safeSwitch(String stock, String stockPkg, String holder, String wifiBefore) throws IOException {
        journalStep("wizard_off");
        sh.run("pm disable-user --user 0 " + WRAITH);
        journalStep("wifi_check");
        boolean wok = false;
        for (int i = 0; i < 4 && !wok; i++) {
            sleeper.sleep(5000);
            wok = wifiSame(wifiBefore, wifiState());
        }
        if (!wok) {
            String a = store.get(WIFI_ASK);
            store.put(WIFI_ASK, (a == null ? "" : a) + "wifi-changed " + stamp + "\n");   // the next run keeps the wizard on, as before, never this order again
            return abort(stock, stockPkg, holder, "الواي فاي ما بقي كما كان بعد إيقاف وسيط الإعداد، فأوقفنا التبديل وشغّلنا الوسيط من جديد. إن انقطع الواي فاي فاتصل به من إعدادات التلفزيون ← الشبكة.", false);
        }
        journalStep("home_start");
        String started = text("am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n " + q(HOME_ACTIVITY));
        boolean front = false;
        if (started.contains("Status: ok") && !Pattern.compile("(?mi)^Error").matcher(started).find()) {
            for (int i = 0; i < 3 && !front; i++) {
                sleeper.sleep(3000);
                front = text("dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity|mFocusedApp'").contains(SELF + "/");
            }
        }
        if (!front) {
            return abort(stock, stockPkg, holder, "تلفزيونك ما ظهر في الواجهة لما شغّلناه، فما أوقفنا الشاشة الأصلية، ورجّعنا وسيط الإعداد. تلفزيونك يشتغل من أيقونة «تلفزيونك» في صف التطبيقات.", true);
        }
        journalStep("stock_off");
        AdbClient.Result disable = sh.run("pm disable-user --user 0 " + stockPkg);
        if (disable.output.toLowerCase().contains("illegal")) {
            return abort(stock, stockPkg, holder, "هذا الجهاز لا يسمح بإيقاف شاشته الرئيسية الأصلية («This command is illegal»)، فبقيت كما هي. بقية الإعداد لا تحتاجها.", true);
        }
        if (disable.code != 0) return abort(stock, stockPkg, holder, "ما قبل التلفزيون إيقاف الشاشة الرئيسية الأصلية.", false);
        AdbClient.Result role = sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);
        AdbClient.Result setHome = sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
        boolean roleOk = role.code == 0 || (sdk > 0 && sdk < 29);
        if (!roleOk || setHome.code != 0) return abort(stock, stockPkg, holder, "ما قبل التلفزيون تعيين شاشتنا شاشةً رئيسية، فرجّعنا الأصلية.", false);
        sh.run("am force-stop com.android.tv.settings");
        sh.run("input keyevent KEYCODE_HOME");
        return null;
    }

    /**
     * 4.3.2 (StockMigration): a TV switched the old way gets back, once, what that switch turned off (the stock home, the wizard, the stock home's helpers), by what the journal
     * says; ours stays the HOME role. null: nothing to do (or the TV did not answer: tried again at the next start); a result: what was done, in one line.
     */
    Result migrateStockHome() throws IOException {
        if (!KEEP_STOCK_ON) return null;
        String j = store.get("home-switch.json");
        if (j == null || !StockMigration.due(journal("state"), journal("stock_before"), journal("wizard_before"), store.get(FACTORY), store.get(StockMigration.MARK))) return null;
        String stock = journal("stock");
        if (stock.isEmpty() || !COMPONENT.matcher(stock).matches()) return null;
        String stockPkg = stock.substring(0, stock.indexOf('/'));
        Set<String> off = packages("pm list packages -d"), pk = packages("pm list packages");
        if (off == null || pk == null) return null;   // the TV did not say: the next start asks again
        List<String> turn = StockMigration.toEnable(journal("stock_before"), stockPkg, journal("wizard_before"), WRAITH, off, java.util.Arrays.asList(SetupLists.HOME_HELPERS), pk);
        if (turn.isEmpty()) { store.put(StockMigration.MARK, "none " + stamp + "\n"); return null; }   // nothing of ours was off: nothing to say
        for (String p : turn) sh.run("pm enable " + p);
        Set<String> after = packages("pm list packages -d");
        if (after == null) return null;
        for (String p : turn) if (after.contains(p)) return failed("ما قبل التلفزيون تشغيل بعض ما أوقفه الإعداد القديم؛ نجرّب مرة ثانية عند التشغيل القادم.");
        sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);   // ours stays Home
        store.put(StockMigration.MARK, "done " + stamp + "\n");
        return done(StockMigration.NOTICE);
    }

    // ---- 4.4: the Home key, checked the person's way; the add-on's Home watch where the stock home keeps the key ------------------

    static final String EXTRAS_A11Y = "extras-a11y.txt";
    /** 4.4 (step 88 T3): the add-on may start and be bound at boot on TCL (AUTO_START, ASSOC_START), written to disk now (write-settings); quiet elsewhere */
    static final String EXTRAS_APPOPS = "appops set " + sa.urtv.home.ExtrasWords.PACKAGE + " AUTO_START allow 2> /dev/null; appops set " + sa.urtv.home.ExtrasWords.PACKAGE
        + " ASSOC_START allow 2> /dev/null; appops write-settings 2> /dev/null";
    /** stock, stockPkg, holder while the Home watch is being set up (the switch is armed, the role is ours, the stock home still wins the key) */
    private String[] watchFor;

    /** the switch waits for the add-on's Home watch (SetupRunner sets it up, then finishWatch) */
    boolean watchPending() { return watchFor != null; }

    /**
     * the real check: the TV's settings in front, then the Home key (up to three presses, 3 s apart): true only when our home is in front. On a Google TV
     * the Home key always goes to its own home whatever the role says (the Home doctor, step 87): what brings ours is the add-on's watch, so this is
     * also the check of the watch. 4.4 (step 88): the setup page is brought back at once after it (the TV's settings had stayed over it).
     */
    boolean homeKeyOpensOurs() throws IOException {
        sh.run("am start -a android.settings.SETTINGS");
        sleeper.sleep(3000);
        boolean ok = false;
        for (int i = 0; i < 3 && !ok; i++) {
            sh.run("input keyevent KEYCODE_HOME");
            sleeper.sleep(3000);
            ok = ours(lastLine(text("dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -n 1")));
        }
        backToSetup();
        return ok;
    }

    /** 4.4 (step 88): the setup page in front again after a check put another screen over it (the TV's settings, the Home key) */
    void backToSetup() throws IOException { sh.run("am start --activity-reorder-to-front -n " + SETUP_ACTIVITY); }

    static final String SETUP_ACTIVITY = SELF + "/.setup.SetupActivity";

    /** the switch is confirmed: the safety switch off, the journal done, the count of failed tries starts again */
    private Result finishHome(Set<String> pk, String said) throws IOException {
        confirmOurs();
        return done(said);
    }

    /**
     * 4.4 (step 86 B): our home holds the HOME role and works; the safety switch is for a TV left with no home, so it is disarmed and the journal
     * is «done». HOME_WANTED says the person chose our home: the add-on's watch brings it back, and the home re-applies the role once if the TV
     * takes it back by itself (SetupHook.reapplyIfLost).
     */
    private void confirmOurs() throws IOException {
        deadman.disarm();
        clearDeadline();
        journalState("done");
        store.put(WIFI_ASK, "");
        store.put(HOME_WANTED, "1");
        store.put(HOME_REAPPLIED, "");
    }

    /**
     * 4.4 (step 86 B): the person's home is ours (HOME_WANTED) but the TV gave the HOME role back to its own home by itself (a Google TV did, two minutes
     * after the owner's switch). The role and the home activity are given to ours again, ONCE (SetupHook.reapplyIfLost keeps the time; a second loss
     * is left to the person: it may be their own choice in the TV's settings). Nothing is turned off. True when Home resolves to ours after it.
     */
    boolean reapplyHomeRole() throws IOException {
        sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);
        sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
        store.put(HOME_REAPPLIED, String.valueOf(now()));
        return resolveHome().startsWith(SELF + "/");
    }

    /** 4.4: «1» while the person's chosen home is ours (set when a switch is confirmed; cleared when the stock home is put back or by «تراجع») */
    static final String HOME_WANTED = "home-wanted.txt";

    /** 4.4: the HOME role was re-applied after the TV took it back by itself (the time, once; cleared by a new switch) */
    static final String HOME_REAPPLIED = "home-reapplied.txt";

    /**
     * 4.4 (step 86 B): our home was drawn (MainActivity resumed) while it is the TV's default home and the journal still says «switching»: the switch
     * works, so the safety switch must not undo it. Disarmed and «done» here, with no adb (files and the alarm are this app's). False: nothing to do.
     */
    static boolean confirmDrawn(Store store, Deadman deadman) throws IOException {
        String j = store.get("home-switch.json");
        if (j == null || !j.contains("  \"state\": \"switching\",")) return false;
        store.put("home-switch.json", j.replaceAll("(?m)^  \"state\": \".*\",", "  \"state\": \"done\","));
        store.put("home-deadline.txt", "");
        store.put(HOME_WANTED, "1");
        store.put(HOME_REAPPLIED, "");
        deadman.disarm();
        return true;
    }

    /**
     * After the add-on's watch was set up (watchOk: installed, turned on and bound): the Home key is checked again the person's way. Ours in front: done. Else the stock
     * home is given Home back and checked (abort), the person is told plainly, and the try counts (HomeFails: after two the in-app switch is not offered any more).
     */
    Result finishWatch(boolean watchOk) throws IOException {
        String[] w = watchFor;
        watchFor = null;
        if (w == null) return failed("ما بدأ تبديل الشاشة الرئيسية.");
        // 4.4 (step 86 B): Home still opening the stock home is not a broken TV. Our home holds the role and opens from its tile, so nothing is put back
        // (that undid the owner's switch two minutes later); the switch is confirmed and the person told plainly. Only a TV left with no home is put back.
        Result r;
        if (watchOk && boundWithin(BOUND_WAIT_MS) && homeKeyOpensOurs()) r = finishHome(null, WATCH_OK);
        else {
            confirmOurs();
            r = failed(WATCH_FAILED);
        }
        try { store.put(HomeFails.FILE, HomeFails.after(store.get(HomeFails.FILE), true, r.status == Status.DONE)); } catch (IOException ignored) { }
        return r;
    }

    /**
     * 4.4 (build 413, the tester's T1): the result said the Home step failed while it worked: it was judged before Android had bound the add-on. Android's
     * own answer (Bound services) is read once a second, up to BOUND_WAIT_MS, before the Home key is tried; false when it never said ours.
     */
    static final int BOUND_WAIT_MS = 10_000;

    boolean boundWithin(int ms) throws IOException {
        String svc = sa.urtv.home.ExtrasWords.SERVICE;
        for (int waited = 0; ; waited += 1000) {
            if (text("dumpsys accessibility | grep -m1 'Bound services:'").contains(svc)) return true;
            if (waited >= ms) return false;
            sleeper.sleep(1000);
        }
    }

    /** 4.4 (step 87): on a Google TV the Home key is the TV's own whatever the role says; the add-on's watch is what brings ours, so it must stay on */
    static final String WATCH_OK = "زر الرئيسية يفتح تلفزيونك الآن عن طريق «إضافات تلفزيونك»: هذا التلفزيون يرسل زر الرئيسية لشاشته الأصلية دائماً، و«إضافات تلفزيونك» ترجّعك لتلفزيونك. خلّها مفعّلة. اضغط زر الرئيسية في الريموت للتأكد.";

    /** 4.4: Home still opens the stock home after the add-on was set up; our home stays the TV's home for everything else */
    static final String WATCH_FAILED = "ما قدرنا نخلي زر الرئيسية يفتح تلفزيونك على هذا التلفزيون، وكل شيء غيره يشتغل: افتح تلفزيونك من أيقونته في صف التطبيقات. تلفزيونك بقي الشاشة الرئيسية، و«تراجع» يرجّع الأصلية.";

    /**
     * The add-on's service on, over this adb: what the TV had is kept once (EXTRAS_A11Y, «تراجع» puts it back), our entry added (the home's own entries of 4.3.2 and before
     * leave the list), the switch on; then Android's own answer (Bound services) is read, and while ours is not in it the list goes off and on again (a TCL's TclAppBoot
     * refused the first bind), twice at most. DONE only when it is bound.
     */
    Result enableWatch() throws IOException {
        String svc = sa.urtv.home.ExtrasWords.SERVICE;
        String list = lastLine(text("settings get secure enabled_accessibility_services")).trim(), sw = lastLine(text("settings get secure accessibility_enabled")).trim();
        if (list.isEmpty() || sw.isEmpty()) return failed("التلفزيون لم يقل أي خدمات «تسهيل الاستخدام» مشغّلة، فما غيّرنا شيئاً.");
        String had = store.get(EXTRAS_A11Y);
        if (had == null || had.trim().isEmpty()) store.put(EXTRAS_A11Y, "list=" + list + "\nswitch=" + sw + "\n");
        String now = WatchList.with(list, svc);
        if (now == null) return failed("قائمة «تسهيل الاستخدام» في هذا التلفزيون فيها ما لا نرسله، فما غيّرناها.");
        sh.run("settings put secure enabled_accessibility_services '" + now + "'");
        sh.run("settings put secure accessibility_enabled 1");
        // TCL's boot manager (TclAppBoot): after a restart it refused to bind the add-on («callee doesn't have OP_AUTO_START», step 88 T3). Both ops allowed,
        // then written to disk at once: AppOps flushes only about 30 minutes later, so a restart before that lost them (3 of 3 boots with this)
        sh.run(EXTRAS_APPOPS);
        for (int i = 0; i < 3; i++) {
            sleeper.sleep(2500);
            String bound = text("dumpsys accessibility | grep -m1 'Bound services:'");
            if (bound.contains(svc)) return done("«إضافات تلفزيونك» شغّالة.");
            if (i == 2) break;
            String rest = WatchList.without(now, svc);
            if (rest.isEmpty()) sh.run("settings delete secure enabled_accessibility_services");
            else sh.run("settings put secure enabled_accessibility_services '" + rest + "'");
            sh.run("settings put secure enabled_accessibility_services '" + now + "'");
            out.line("التلفزيون ما شغّل «إضافات تلفزيونك» أول مرة: أطفأناها وشغّلناها من جديد.");
        }
        return failed("التلفزيون ما شغّل «إضافات تلفزيونك». افتح إعدادات التلفزيون ← تسهيل الاستخدام ← إضافات تلفزيونك، وأطفئها وشغّلها من جديد.");
    }

    /**
     * «تراجع»: our entry leaves the list (4.4, step 88 T8: ALWAYS, also when this app did not record turning it on: it stayed after an undo), the TV's
     * other services kept; the switch goes back to what the TV had only when it was recorded (EXTRAS_A11Y) and nothing else is on.
     */
    private void undoWatch() throws IOException {
        String had = store.get(EXTRAS_A11Y);
        if (had == null) had = "";
        String list = lastLine(text("settings get secure enabled_accessibility_services")).trim();
        if (list.isEmpty()) return;   // the TV did not say: left for the next «تراجع»
        String rest = WatchList.without(list, sa.urtv.home.ExtrasWords.SERVICE);
        if (!rest.equals(WatchList.without(list, ""))) {
            if (rest.isEmpty()) sh.run("settings delete secure enabled_accessibility_services");
            else sh.run("settings put secure enabled_accessibility_services '" + rest + "'");
        }
        String sw = WatchList.field(had, "switch");
        if (rest.isEmpty() && sw.matches("[0-9]+")) sh.run("settings put secure accessibility_enabled " + sw);
        else if (rest.isEmpty() && "null".equals(sw)) sh.run("settings delete secure accessibility_enabled");
        store.put(EXTRAS_A11Y, "");
    }

    /** Step 4 of the switch: Home really opens ours (two rounds), then the safety switch is disarmed; else everything is put back. */
    private Result verifyAndFinish(String stock, String stockPkg, String holder, Set<String> pk, boolean safeOrder) throws IOException {
        journalStep("verify");
        boolean ok = verifyHome();
        if (!ok) {
            out.line("الشاشة الرئيسية لم تفتح تلفزيونك: نسأل مرة ثانية.");
            sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);
            sh.run("cmd package set-home-activity --user 0 " + q(HOME_ACTIVITY));
            sh.run("input keyevent KEYCODE_HOME");
            ok = verifyHome();
        }
        if (!ok) return abort(stock, stockPkg, holder, "الشاشة الرئيسية لم تفتح تلفزيونك.", false);
        confirmOurs();   // done: a later run starts afresh (install.sh removes wifi-ask.txt here)
        if (!KEEP_STOCK_ON) for (String h : SetupLists.HOME_HELPERS) if (pk.contains(h)) sh.run("pm disable-user --user 0 " + h);   // only now: the stock home's helpers are dead weight
        if (safeOrder) out.line("وسيط الإعداد موقوف، والواي فاي بقي متصلاً وشبكته محفوظة (تأكدنا بعد إيقاف الوسيط وقبل إيقاف الشاشة الأصلية).");
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
            store.put(HOME_WANTED, "");
            return skipOnly ? skipped(why) : failed(why + " رجّعنا الشاشة الأصلية وتأكدنا منها. ما ضاع شيء.");
        }
        return failed(why + " حاولنا إرجاع الشاشة الأصلية ولم نتأكد منها بعد؛ مفتاح الأمان سيعيد المحاولة تلقائياً. إن لم تظهر فاضغط «تراجع».");
    }

    private void clearDeadline() throws IOException { store.put("home-deadline.txt", ""); }

    /** a file emptied only when it holds something (a TV that answers nothing gets no file written) */
    private void clearFile(String name) throws IOException { String v = store.get(name); if (v != null && !v.trim().isEmpty()) store.put(name, ""); }

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
            confirmOurs();
            return done("سجّلنا أن تلفزيونك صار الشاشة الرئيسية بعد تبديل انقطع (لم نفحص الشاشة بعد). اضغط زر الرئيسية للتأكد.");
        }
        String stock = journal("stock");
        if (stock.isEmpty() || !COMPONENT.matcher(stock).matches()) return failed("سجل التبديل ناقص، فاضغط «تراجع».");
        String pkg = stock.substring(0, stock.indexOf('/'));
        if (!putStockBack(stock, pkg, journalHolder(), false)) return failed("حاولنا إرجاع الشاشة الأصلية ولم نتأكد منها بعد؛ سنعيد المحاولة تلقائياً.");
        deadman.disarm();
        clearDeadline();
        journalState("restored");
        store.put(HOME_WANTED, "");
        return done("كان تبديل الشاشة الرئيسية قد انقطع؛ رجّعنا الشاشة الأصلية وتأكدنا منها.");
    }

    // ---- the undo (optimize/restore.sh) ---------------------------------------------------------------------------------------------

    /**
     * In optimize/reset.sh's order: the home first (never leave the TV without one), then the apps in the ledger, the protected features (keep.sh),
     * the screensaver and the appops as they were, TCL's auto-start, the apps the setup installed (and nothing else), and the speed tweaks last.
     */
    Result undo() throws IOException {
        List<String> problems = new ArrayList<>();
        clearFile(HOME_WANTED);   // 4.4: the person wants the TV as it was: nothing brings our home back or re-applies its role
        clearFile(HOME_REAPPLIED);
        clearFile(HomeFails.FILE);   // 4.4 (step 88): a new start after «تراجع» begins with no memory of failed home switches
        String stock = journal("stock");
        if (stock.isEmpty() || !COMPONENT.matcher(stock).matches()) {
            Set<String> pk = packages("pm list packages");
            if (pk == null) return failed("لم يقل التلفزيون قائمة تطبيقاته، فلم نمسّ الشاشة الرئيسية. جرّب «تراجع» مرة ثانية.");
            String other = pk.contains(LAUNCHERX) || pk.contains(TVLAUNCHER) ? null : otherHome(pk);
            stock = pk.contains(LAUNCHERX) ? LAUNCHERX_HOME : pk.contains(TVLAUNCHER) ? TVLAUNCHER_HOME : other != null ? other : "";
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
        undoWatch();   // 4.4: the add-on's service, turned on by the setup, is turned off again (the list as the TV had it)
        // the apps our run turned off: the ledger, and the community list's own record (optimize/reset.sh hands community-applied.txt to restore.sh)
        List<String> ledger = ledgerNames(store.get("disabled-by-us.txt"));
        for (String p : ledgerNames(store.get("community-applied.txt"))) if (!ledger.contains(p)) ledger.add(p);
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
            if (store.get("community-applied.txt") != null) {
                StringBuilder cb = new StringBuilder();
                for (String p : ledgerNames(store.get("community-applied.txt"))) if (keep.contains(p)) cb.append(p).append('\n');
                store.put("community-applied.txt", cb.toString());
            }
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
        // the screensaver timeout a measurement stretched (a run cut while it was), and what this setup installed (installed-by-us.txt): only that is removed
        new SetupMeasure(sh, store, out, sleeper).putTimeoutBack();
        List<String> stay = new SetupInstall(sh, null, null, store, out, sleeper).undo();
        if (!stay.isEmpty()) problems.add("لم يقبل التلفزيون إزالة ما ثبّته الإعداد (" + String.join(" ", stay) + ")؛ اضغط «تراجع» مرة ثانية.");
        // the tweaks last, as optimize/reset.sh does: exactly what the TV had (tweaks-journal.txt); nothing when the app never changed one
        if (new SetupTweaks(sh, store, out).restore().status != Status.DONE) problems.add("لم يقبل التلفزيون إرجاع بعض إعدادات السرعة؛ اضغط «تراجع» مرة ثانية.");
        if (!problems.isEmpty()) return failed(String.join(" ", problems));
        return done("رجّعنا تلفزيونك كما كان. تطبيق تلفزيونك يبقى مثبتاً؛ تقدر تحذفه من إعدادات التلفزيون (التطبيقات).");
    }
    // ---- «تخلّص من واجهة المصنع» (UrTV النظيف, the second step): turning the stock interface off from the TV, and giving it back --------------------------

    /** what the step wrote: «on <stamp>» while the stock interface is off by this step, «off <stamp>» after the one-press restore */
    static final String FACTORY = "factory-off.txt";

    /** What the step would do on this TV (nothing changed): the reason it refuses, or the plan. */
    static final class FactoryOffer {
        final String refusal;          // null: it may go on
        final String stock;            // the stock home's component ("" when unknown)
        final List<String> packages;   // what would be turned off, in the order it is done: the stock home first, then the safe list on this TV
        FactoryOffer(String refusal, String stock, List<String> packages) { this.refusal = refusal; this.stock = stock; this.packages = packages; }
    }

    /**
     * Offered only when it is safe, else the reason in plain words: the setup wizard owns the saved Wi-Fi (turning it off would lose the network: the person forgets and
     * rejoins it in the TV's settings first, as for the home switch); the safety switch cannot be set (an alarm that puts the stock home back); the stock home is not known;
     * the TV did not say what is installed. Never touches the protected features (SetupLists.KEEP): the list is the safe list and the stock home, nothing else.
     */
    FactoryOffer factoryOffer() throws IOException {
        Set<String> pk = packages("pm list packages"), off = packages("pm list packages -d");
        if (pk == null || off == null) return new FactoryOffer("التلفزيون لم يقل أي تطبيقاته مثبّتة وأيها موقوف، فما نقدر نقول وش بنوقف. جرّب مرة ثانية.", "", new ArrayList<String>());
        String stock = pk.contains(LAUNCHERX) ? LAUNCHERX_HOME : pk.contains(TVLAUNCHER) ? TVLAUNCHER_HOME : otherHome(pk);
        if (stock == null || stock.isEmpty()) return new FactoryOffer("ما عرفنا ما الشاشة الرئيسية الأصلية لهذا التلفزيون، فما نعطّل شيئاً.", "", new ArrayList<String>());
        if (wizardOwnsWifi())
            return new FactoryOffer("وسيط الإعداد في هذا التلفزيون يملك شبكة الواي فاي المحفوظة، وتعطيله يضيّع الشبكة. انسَ الشبكة وأعد الاتصال بها من إعدادات التلفزيون، ثم جرّب.", stock, new ArrayList<String>());
        if (!deadman.arm(30)) return new FactoryOffer("ما نقدر نضبط مفتاح الأمان (منبّه يرجّع الشاشة الأصلية) على هذا التلفزيون، فما نعطّلها.", stock, new ArrayList<String>());
        deadman.disarm();
        List<String> plan = new ArrayList<>();
        String stockPkg = stock.substring(0, stock.indexOf('/'));
        if (!off.contains(stockPkg)) plan.add(stockPkg);
        for (String p : filterSafe(SetupLists.SAFE)) if (pk.contains(p) && !off.contains(p) && !plan.contains(p)) plan.add(p);
        return new FactoryOffer(null, stock, plan);
    }

    /** true while the step's mark says the stock interface is off by it */
    boolean factoryIsOff() { String f = store.get(FACTORY); return f != null && f.startsWith("on"); }

    /**
     * Turns the stock interface off, in this order: the offer is checked again; the home is switched to ours (home(): its own safety switch, wizard rules and journal); the stock
     * home, if it is still on, is written into the ledger BEFORE it is turned off; then the safe list (debloat(): its input guard, its ledger). «disabled-by-us.txt» is the
     * ledger of everything this step and the setup turned off, and the one list the restore turns back on. Disable only: nothing is ever uninstalled.
     */
    Result factoryOff() throws IOException {
        FactoryOffer o = factoryOffer();
        if (o.refusal != null) return failed(o.refusal);
        String stockPkg = o.stock.substring(0, o.stock.indexOf('/'));
        boolean stockWasOn = o.packages.contains(stockPkg);   // the offer lists it only when it was on: what this step turns off is in the ledger, whoever runs the command
        if (stockWasOn) remember(ledgerNames(store.get("disabled-by-us.txt")), java.util.Collections.singletonList(stockPkg));   // written BEFORE any command
        Result h = home();
        if (h.status == Status.FAILED) return h;
        Set<String> off = packages("pm list packages -d");
        if (off != null && !off.contains(stockPkg)) {
            sh.run("pm disable-user --user 0 " + stockPkg);
            off = packages("pm list packages -d");
            if (off == null || !off.contains(stockPkg)) return failed("ما قبل التلفزيون إيقاف الشاشة الأصلية. ما تغيّر شيء مهم؛ «رجّع واجهة المصنع» يرجّع كل ما أوقفناه.");
        }
        Result d = debloat();
        if (d.status == Status.FAILED) return d;
        store.put(FACTORY, "on " + stamp + "\n");
        return done("عطّلنا واجهة المصنع (تعطيل لا حذف). «رجّع واجهة المصنع» يرجّعها بضغطة وحدة.");
    }

    /**
     * «رجّع واجهة المصنع»: EXACTLY the ledger comes back, and the stock home: the stock home is given Home again and checked, then every package of «disabled-by-us.txt» is
     * enabled and the list is read back from the TV (one that did not come back stays in the ledger for the next try). Nothing else is touched: no app of ours is
     * removed, the protected features and the tweaks are left as they are (this is not the setup's full undo).
     */
    Result restoreFactory() throws IOException {
        List<String> problems = new ArrayList<>();
        String stock = journal("stock");
        if (stock.isEmpty() || !COMPONENT.matcher(stock).matches()) {
            Set<String> pk = packages("pm list packages");
            if (pk == null) return failed("لم يقل التلفزيون قائمة تطبيقاته، فلم نمسّ شيئاً. جرّب «رجّع واجهة المصنع» مرة ثانية.");
            stock = pk.contains(LAUNCHERX) ? LAUNCHERX_HOME : pk.contains(TVLAUNCHER) ? TVLAUNCHER_HOME : otherHome(pk);
            if (stock == null) stock = "";
        }
        if (!stock.isEmpty()) {
            String pkg = stock.substring(0, stock.indexOf('/'));
            if (putStockBack(stock, pkg, journalHolder(), true)) { deadman.disarm(); clearDeadline(); if (store.get("home-switch.json") != null) journalState("restored"); }
            else problems.add("لم نتأكد من رجوع الشاشة الأصلية؛ تركنا مفتاح الأمان يعيد المحاولة.");
        } else {
            problems.add("ما عرفنا الشاشة الأصلية؛ اضغط زر الرئيسية، وإن لم يفتح شيء فاختر الشاشة من إعدادات التلفزيون.");
        }
        List<String> ledger = ledgerNames(store.get("disabled-by-us.txt"));
        for (String p : ledger) sh.run("pm enable " + p);
        Set<String> off = packages("pm list packages -d");
        List<String> keep = new ArrayList<>();
        if (off == null) { keep.addAll(ledger); problems.add("أرجعنا التطبيقات لكن التلفزيون لم يؤكد القائمة؛ اضغط «رجّع واجهة المصنع» مرة ثانية."); }
        else for (String p : ledger) if (off.contains(p)) keep.add(p);
        StringBuilder b = new StringBuilder();
        for (String p : keep) b.append(p).append('\n');
        store.put("disabled-by-us.txt", b.toString());
        if (!keep.isEmpty() && off != null) problems.add(keep.size() + " تطبيقاً لم يقبل التلفزيون تشغيله؛ جرّب مرة ثانية.");
        if (problems.isEmpty()) store.put(FACTORY, "off " + stamp + "\n");
        if (!problems.isEmpty()) return failed(String.join(" ", problems));
        return done("رجّعنا واجهة المصنع وكل ما أوقفناه.");
    }
}
