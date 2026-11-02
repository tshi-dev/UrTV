package sa.urtv.home.setup;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one setup run of this process. The TV's own button («جهّز كل شي» in «إعداد تلفزيوني») and, later, the phone start runs here and only read from here: ONE run at a
 * time (a second start while one runs is «busy»), a model of its steps (pending, running, done, skipped, failed, each with its reason) and a bounded
 * ring of its lines with sequence numbers, so a screen or a phone that comes back asks for «the lines since n». The run lives on its own thread (on the
 * TV a foreground service holds the process, see SetupService), so it outlives the screen that started it.
 * The full run («FULL») writes its step pointer to runner.txt BEFORE each step: a TV restart in the middle (the measurement restarts it on purpose)
 * resumes from there through the boot path (SetupBootReceiver). A step that is not safe to repeat (the home switch) is never run a second time after
 * a cut: it is reported as cut, and the person presses «جهّز كل شي» for it. Plain Java (no Android) so tests/SetupRunnerRun.java runs it on a JVM.
 * There is one way to set the TV up: the full run. After the TV's prompt is accepted and the precheck (read only) is done, the run reads what it would
 * change (the Preview: the start screen's real counts) and WAITS for the person's «ابدأ» (confirm) before anything is changed; no answer in ten minutes,
 * or «إلغاء», ends it with nothing changed. It pauses ONCE more, before the home switch, when the TV's setup wizard owns the saved Wi-Fi (install.sh's
 * step 0): the person forgets the network and joins it again, then presses «كمّل», which resumes the run at the home switch (await=wifi in runner.txt).
 * While a run goes (from the prompt to the end) our own screensaver does not start (SetupHold, asked by the screensaver), and when the prompt got no
 * answer in 20 s the screen says how to clear a prompt the TV left pending (PROMPT_LATE).
 */
final class SetupRunner {
    enum Kind { UNDO, FULL, ANSWERS }
    enum State { PENDING, RUNNING, DONE, SKIPPED, FAILED }
    enum Start { STARTED, BUSY, NOTHING }

    /** One step as a screen or the phone shows it. */
    static final class Step {
        final String id;
        State state = State.PENDING;
        String reason = "";
        long startedMs, endedMs;
        Step(String id) { this.id = id; }
        Step copy() { Step s = new Step(id); s.state = state; s.reason = reason; s.startedMs = startedMs; s.endedMs = endedMs; return s; }
    }

    /** One line of the run, with its number (1, 2, 3 … for the life of the process). */
    static final class Line {
        final long seq;
        final String text;
        Line(long seq, String text) { this.seq = seq; this.text = text; }
    }

    /** How a run ended: the sentence the screen shows, and whether it is shown as a problem (red). wifi: paused for the Wi-Fi (the screen asks, «كمّل»). */
    static final class End {
        final String text;
        final boolean bad, wifi;
        End(String text, boolean bad) { this(text, bad, false); }
        End(String text, boolean bad, boolean wifi) { this.text = text; this.bad = bad; this.wifi = wifi; }
    }

    /** What the full run would do on this TV, read (nothing changed) after the precheck: the start screen's plan and its real counts. */
    static final class Preview {
        int safe = -1;                // apps of the safe list installed and on now (-1: the TV did not say)
        boolean saverInstalled, tvbro, smarttube, tcl, restarts, homeOurs, wifiAsk;
    }

    /** What the person chose for a full run. */
    static final class Options {
        boolean extras = true;    // TV Bro and SmartTube (the owner's choice: on unless the person unticks them)
        Map<String, Boolean> answers = new LinkedHashMap<>();   // ANSWERS: the person's yes/no to the community list's questions
    }

    /** One connection to the TV with an engine on it (SetupSession on the TV, a fake in the tests). */
    interface Session extends Closeable {
        SetupEngine engine();
        SetupEngine.Shell shell();
        /** `exec:` with a stream: an APK's bytes to `cmd package install -S` (AdbClient.exec). */
        String exec(String command, java.io.InputStream data, long size) throws IOException;
        @Override void close();
    }

    /** What the runner needs from the platform. */
    interface Host {
        Session open(int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException;
        SetupEngine.Store store();
        /** Runs the job on a thread that survives the screen (a foreground service holds the process on the TV). */
        void spawn(Runnable job);
        /** After EVERY end of a run, when its connection is closed: the ADB key goes when nothing is pending (SetupSession.deleteKeyIfFinished). */
        void finished();
        void sleep(int ms) throws IOException;
        long now();
        /** A file of the app's assets (setup/community.txt), null when it is not there. */
        String asset(String name);
        /** A text from the internet (UAD-ng's ratings, a GitHub release list), null when it cannot be fetched. */
        String fetch(String url, int maxBytes);
        /** Downloads and reads APKs; knows this app's signer and the installed apps (PackageManager on the TV). */
        SetupInstall.Apks apks();
        /** This app's version as the screen says it («4.0 (build 151)»). */
        String appVersion();
        /** Runs r once, ms from now, on another thread (the prompt's 20 s notice); the tests run it by hand. */
        void later(int ms, Runnable r);
    }

    /** One step of a plan: what it runs, and whether it may run a second time after it was cut off (a restart, a killed process). */
    interface Body { SetupEngine.Result run(Context c) throws IOException; }

    static final class Def {
        final String id;
        final boolean repeatable;
        final Body body;
        Def(String id, boolean repeatable, Body body) { this.id = id; this.repeatable = repeatable; this.body = body; }
    }

    /** What a step gets: the engine, the shell, the store, the options, and the run's own lines. */
    static final class Context {
        final SetupEngine engine;
        Session session;
        final SetupEngine.Shell shell;
        final SetupEngine.Store store;
        final Options options;
        final SetupEngine.Out out;
        final SetupRunner runner;
        final Host host;
        Context(SetupEngine engine, SetupEngine.Shell shell, SetupEngine.Store store, Options options, SetupEngine.Out out, SetupRunner runner, Host host) {
            this.engine = engine; this.shell = shell; this.store = store; this.options = options; this.out = out; this.runner = runner; this.host = host;
        }
    }

    /** Thrown by a step that has just restarted the TV on purpose: the run stops here and resumes after the boot (runner.txt says where). */
    static final class Restarting extends IOException {
        Restarting() { super("the TV is restarting"); }
    }

    /** Thrown before the home switch when the person is asked about the Wi-Fi (nothing was changed by it): «كمّل» resumes the run at the switch. */
    static final class Paused extends IOException {
        Paused() { super("waiting for the person: the Wi-Fi network"); }
    }

    /** The person did not press «ابدأ» (cancelled, or ten minutes passed) after the precheck: the run ends, nothing changed. */
    static final class NotStarted extends IOException {
        NotStarted() { super("not started"); }
    }

    static final int PROMPT_LATE_MS = 20_000, CONFIRM_POLL_MS = 500, CONFIRM_TRIES = 1200;   // the 20 s notice; ten minutes for «ابدأ»
    static final String PROMPT_LINE = "التلفزيون يسأل «السماح بتصحيح USB؟»: اختر «السماح دائماً» بالريموت.";
    /** No answer to the prompt in 20 s: a prompt sent while the screensaver showed stays pending until the TV restarts (tester's diagnosis, 30 Oct). */
    static final String PROMPT_LATE = "ما ظهرت الرسالة؟ أعد تشغيل التلفزيون وافتح «إعداد تلفزيوني» واضغط «جهّز كل شي» وخلك عند التلفزيون";
    /** The Wi-Fi question (install.sh's line for the person), shown with «كمّل». */
    static final String WIFI_ASK_TEXT = "قبل تبديل الشاشة الرئيسية: شبكة الواي فاي محفوظة باسم «وسيط الإعداد» في هذا التلفزيون، وإيقافه يمسحها. "
        + "افتح إعدادات التلفزيون ← الشبكة، واختر «انسَ الشبكة»، ثم اتصل بها من جديد بنفس كلمة المرور (خلّها قدامك قبل). بعدها ارجع هنا واضغط «كمّل». "
        + "ما تبي؟ اضغط «كمّل» بدونها، ونكمل والوسيط شغّال.";
    static final String NOT_STARTED = "ما بدأنا الإعداد، وما تغيّر شيء في التلفزيون.";

    static final String STATE_FILE = "runner.txt";
    static final int RING = 400;
    static final int RESUME_TRIES = 40, RESUME_WAIT_MS = 15_000;   // ten minutes for the TV's adb to come back after a restart

    private static final SetupRunner INSTANCE = new SetupRunner();

    static SetupRunner get() { return INSTANCE; }

    private final ArrayDeque<Line> ring = new ArrayDeque<>();
    private long seq;
    private List<Step> steps = new ArrayList<>();
    private Kind kind;
    private boolean running;
    private End end;
    private Runnable listener;
    private Options options = new Options();
    private Options live;                 // the options of the run that is going (the extras box is read at «ابدأ»)
    private Preview preview;
    private boolean awaitingConfirm;
    private Boolean confirmed;            // null: no answer yet
    private long promptSince;             // when the TV was asked (0: not waiting for the prompt)
    private boolean promptLate;

    SetupRunner() { }

    // ---- the plans --------------------------------------------------------------------------------------------------------------

    /** FULL: the whole setup (the TV's «جهّز كل شي», later the phone's). UNDO: one step. ANSWERS: the community questions. */
    static List<Def> plan(Kind k) {
        List<Def> p = new ArrayList<>();
        if (k == Kind.UNDO) {
            p.add(new Def("undo", true, c -> c.engine.undo()));
            return p;
        }
        if (k == Kind.ANSWERS) {
            p.add(new Def("answers", true, c -> community(c).answer(c.options.answers)));
            return p;
        }
        return fullPlan();
    }

    /**
     * The whole setup, in optimize/run.sh's order, with TCL's auto-start before the home switch and after the screensaver is installed (it only covers
     * apps that are on the TV: on a TV with only the home app the screensaver arrives in install-saver). The precheck and the backup are gates: when
     * either did not finish, nothing after them runs (run.sh stops there too). Nothing is changed before the person's «ابدأ» (in the precheck step).
     */
    static List<Def> fullPlan() {
        List<Def> p = new ArrayList<>();
        p.add(new Def("precheck", true, c -> c.runner.precheckAndAsk(c)));
        p.add(new Def("backup", true, c -> new SetupPrep(c.shell, c.store, c.out).backup()));
        p.add(new Def("keep", true, c -> new SetupPrep(c.shell, c.store, c.out).keep()));
        p.add(new Def("before", true, c -> measure(c).measure("before", !switching(c.store))));
        p.add(new Def("grants", true, c -> c.engine.grants()));
        p.add(new Def("safe", true, c -> c.engine.debloat()));
        p.add(new Def("community", true, c -> community(c).apply()));
        p.add(new Def("tweaks", true, c -> new SetupTweaks(c.shell, c.store, c.out).apply()));
        p.add(new Def("install-saver", true, c -> install(c).screensaver()));
        p.add(new Def("screensaver", true, c -> c.engine.screensaver()));
        p.add(new Def("extras", true, c -> install(c).extras(c.options.extras, c.host)));
        p.add(new Def("autostart", true, c -> new SetupPrep(c.shell, c.store, c.out).autostart()));
        p.add(new Def("home", false, c -> {
            if (c.engine.askWifi()) throw new Paused();   // before anything of the switch: the person is asked once, the run resumes here
            return c.engine.home();
        }));
        p.add(new Def("after", true, c -> measure(c).measure("after", !switching(c.store))));
        p.add(new Def("report", true, c -> report(c)));
        return p;
    }

    /** One line per step on the progress screen. */
    static String label(String id) {
        switch (id) {
            case "precheck": return "فحص التلفزيون";
            case "backup": return "نسخة احتياطية من حالة التلفزيون";
            case "keep": return "الميزات المحمية";
            case "before": return "قياس الذاكرة قبل";
            case "grants": return "أذونات تلفزيونك";
            case "safe": return "القائمة الآمنة";
            case "community": return "قائمة المجتمع";
            case "tweaks": return "تسريع التلفزيون";
            case "install-saver": return "تثبيت شاشة التوقف";
            case "screensaver": return "تفعيل شاشة التوقف";
            case "extras": return "TV Bro و SmartTube";
            case "autostart": return "التشغيل التلقائي";
            case "home": return "الشاشة الرئيسية";
            case "after": return "قياس الذاكرة بعد";
            case "report": return "التقرير";
            case "undo": return "التراجع";
            case "answers": return "أجوبة قائمة المجتمع";
            default: return id;
        }
    }

    /** The start screen's plan, in the run's order, with this TV's counts (Western digits: the screen writes them in the person's digits). */
    static List<String> planLines(Preview p) {
        List<String> l = new ArrayList<>();
        l.add("نحفظ حالة التلفزيون الآن (نسخة احتياطية) قبل أي تغيير، ونرجّع أي ميزة محمية موقوفة (البث، الصوت، الريموت، المتجر).");
        l.add(p.restarts ? "نقيس الذاكرة قبل الإعداد وبعده: يُعاد تشغيل التلفزيون مرتين، ويكمل الإعداد وحده بعد كل مرة."
            : "نقيس الذاكرة قبل الإعداد وبعده (بدون إعادة تشغيل في هذا التلفزيون).");
        l.add("نعطي تلفزيونك أذوناته (الموقع، منبّهات الأذان، التحديثات).");
        if (p.safe < 0) l.add("نوقف تطبيقات القائمة الآمنة (إعلانات وتتبّع وعروض)، ونسجّلها لنرجعها بزر واحد.");
        else if (p.safe == 0) l.add("القائمة الآمنة: لا شيء منها شغّال في هذا التلفزيون.");
        else l.add("نوقف " + p.safe + " تطبيقاً من القائمة الآمنة (إعلانات وتتبّع وعروض)، ونسجّلها لنرجعها بزر واحد.");
        l.add("نطابق قائمة المجتمع لطراز تلفزيونك إن وُجدت، ونسألك عن المشكوك فيه. بدون إنترنت نتركها.");
        l.add("نسرّع التلفزيون: حركات أقصر، ومتجر Play لا يعمل في الخلفية.");
        l.add(p.saverInstalled ? "نحدّث شاشة التوقف إن وُجدت نسخة أحدث، ونفعّلها."
            : "نثبّت شاشة التوقف من إصداراتنا (بعد التأكد أنها موقّعة بمفتاحنا)، ونفعّلها.");
        if (p.tcl) l.add("نسمح لتطبيقاتنا بالعمل عند تشغيل التلفزيون (الأذان ومؤقت النوم بعد إعادة التشغيل).");
        if (p.homeOurs) l.add("تلفزيونك هو الشاشة الرئيسية أصلاً.");
        else l.add("نجعل تلفزيونك الشاشة الرئيسية. إن ما اشتغلت خلال 5 دقائق ترجع الأصلية وحدها."
            + (p.wifiAsk ? " وقبلها نطلب منك نسيان شبكة الواي فاي والاتصال بها من جديد (هذا التلفزيون يحفظها باسم وسيط الإعداد)." : ""));
        l.add("نكتب تقريراً بلا أي معلومة شخصية، وتقدر ترسله برمز QR إن أردت.");
        return l;
    }

    /** The extras box's words (TV Bro and SmartTube, on by default: the owner's choice). */
    static String extrasLine(Preview p) {
        if (p != null && p.tvbro && p.smarttube) return "TV Bro و SmartTube مثبّتان من قبل: نتركهما.";
        return "ثبّت أيضاً TV Bro (متصفح) و SmartTube (يوتيوب بلا إعلانات)";
    }

    /** The precheck, then what the run would change, then the person's «ابدأ» (or not): nothing is changed before it. */
    SetupEngine.Result precheckAndAsk(Context c) throws IOException {
        SetupEngine.Result r = c.engine.precheck();
        if (r.status != SetupEngine.Status.DONE) return r;
        Preview p = preview(c);
        synchronized (this) { preview = p; confirmed = null; awaitingConfirm = true; }
        changed();
        Boolean got = null;
        try {
            for (int i = 0; i <= CONFIRM_TRIES; i++) {
                synchronized (this) { got = confirmed; }
                if (got != null) break;
                if (i < CONFIRM_TRIES) c.host.sleep(CONFIRM_POLL_MS);
            }
        } finally {
            synchronized (this) { awaitingConfirm = false; }
            changed();
        }
        if (got == null || !got) throw new NotStarted();
        return r;
    }

    /** What the run would change on this TV (read only). */
    static Preview preview(Context c) throws IOException {
        Preview p = new Preview();
        List<String> safe = c.engine.safeToTurnOff();
        p.safe = safe == null ? -1 : safe.size();
        SetupTv tv = new SetupTv(c.shell);
        p.saverInstalled = tv.installed(SetupEngine.SAVER);
        p.tvbro = tv.installed(SetupInstall.TVBRO);
        p.smarttube = tv.installed(SetupInstall.SMARTTUBE);
        p.tcl = new SetupPrep(c.shell, c.store, s -> { }).isTcl();
        p.restarts = !switching(c.store) && new SetupMeasure(c.shell, c.store, s -> { }, c.host::sleep).adbSurvivesRestart();
        p.homeOurs = c.engine.resolveHome().startsWith(SetupEngine.SELF + "/");
        p.wifiAsk = !p.homeOurs && c.engine.wizardOwnsWifi();
        return p;
    }

    /** «ابدأ» on the start screen: the run goes on, with or without the extras. False when no run is waiting for it. */
    boolean confirm(boolean extras) {
        synchronized (this) {
            if (!awaitingConfirm || confirmed != null) return false;
            if (live != null) live.extras = extras;
            options.extras = extras;
            confirmed = true;
        }
        changed();
        return true;
    }

    /** «إلغاء» on the start screen: the run ends, nothing changed. */
    boolean cancel() {
        synchronized (this) {
            if (!awaitingConfirm || confirmed != null) return false;
            confirmed = false;
        }
        changed();
        return true;
    }

    synchronized boolean awaitingConfirm() { return awaitingConfirm; }
    synchronized Preview preview() { return preview; }
    /** The prompt got no answer in 20 s (the screen shows PROMPT_LATE while it still waits). */
    synchronized boolean promptLate() { return promptLate && promptSince > 0; }
    synchronized boolean promptWaiting() { return promptSince > 0; }

    private void promptCheck(Host host) {
        boolean say;
        synchronized (this) {
            say = promptSince > 0 && !promptLate && host.now() - promptSince >= PROMPT_LATE_MS;
            if (say) promptLate = true;
        }
        if (say) line(PROMPT_LATE);
    }

    static SetupMeasure measure(Context c) { return new SetupMeasure(c.shell, c.store, c.out, c.host::sleep); }

    static SetupInstall install(Context c) { return new SetupInstall(c.shell, c.session::exec, c.host.apks(), c.store, c.out, c.host::sleep); }

    /** A home switch still waiting for its safety switch: the TV is not restarted then (the boot path would have to undo it first). */
    static boolean switching(SetupEngine.Store store) {
        String j = store.get("home-switch.json");
        return j != null && j.contains("\"state\": \"switching\"");
    }

    static SetupEngine.Result report(Context c) throws IOException {
        String text = new SetupReport(c.shell, c.store).build(c.host.appVersion(), c.host.now());
        return new SetupEngine.Result(SetupEngine.Status.DONE, shareUrl(text) != null ? "كتبنا تقرير الإعداد (بلا أي معلومة شخصية)؛ تقدر ترسله للمطوّر من رمز QR إن أردت."
            : "كتبنا تقرير الإعداد (بلا أي معلومة شخصية).");
    }

    /** The link of the done screen's QR code (share.sh's docs/share page with the report), null without a report or when it is too long. */
    static String shareUrl(String report) { return report == null || report.isEmpty() ? null : SetupReport.shareUrl(report); }

    static SetupCommunity community(Context c) { return new SetupCommunity(c.shell, c.store, c.out, c.host.asset("setup/community.txt"), c.host::fetch); }

    /** What the community list left on and asks the person about: [package, the question in Arabic]. */
    static List<String[]> questions(SetupEngine.Store store) { return SetupCommunity.questions(store); }

    /** The person's answers to those questions, through the same one-run-at-a-time rule (BUSY while a run goes). */
    Start answer(Map<String, Boolean> answers, Host host) {
        Options o = new Options();
        o.extras = false;
        if (answers != null) o.answers.putAll(answers);
        return start(Kind.ANSWERS, o, host);
    }

    /** The steps that stop the run when they did not finish: nothing has been changed before them. */
    static boolean gate(String id) { return id.equals("precheck") || id.equals("backup"); }

    // ---- reading (any thread) ---------------------------------------------------------------------------------------------------

    synchronized boolean running() { return running; }
    synchronized Kind kind() { return kind; }
    synchronized End end() { return end; }
    synchronized Options options() { Options o = new Options(); o.extras = options.extras; o.answers.putAll(options.answers); return o; }

    synchronized List<Step> steps() {
        List<Step> out = new ArrayList<>();
        for (Step s : steps) out.add(s.copy());
        return out;
    }

    /** The number of the newest line (0 before any). */
    synchronized long lastSeq() { return seq; }

    /** The lines after line n that are still in the ring (the oldest ones go first when it is full). */
    synchronized List<Line> linesSince(long n) {
        List<Line> out = new ArrayList<>();
        for (Line l : ring) if (l.seq > n) out.add(l);
        return out;
    }

    /** Called (on the run's thread) after every change; the screen posts it to its own thread. */
    void setListener(Runnable r) { synchronized (this) { listener = r; } }

    // ---- writing (the run's thread) ---------------------------------------------------------------------------------------------

    void line(String text) {
        synchronized (this) {
            ring.addLast(new Line(++seq, text));
            while (ring.size() > RING) ring.removeFirst();
        }
        changed();
    }

    private void changed() {
        Runnable r;
        synchronized (this) { r = listener; }
        if (r != null) {
            try { r.run(); } catch (RuntimeException ignored) { }
        }
    }

    private synchronized Step step(String id) {
        for (Step s : steps) if (s.id.equals(id)) return s;
        return null;
    }

    private void set(String id, State st, String reason, Host host) {
        synchronized (this) {
            Step s = step(id);
            if (s == null) return;
            s.state = st;
            s.reason = reason == null ? "" : reason;
            if (st == State.RUNNING) s.startedMs = host.now(); else s.endedMs = host.now();
        }
        changed();
    }

    // ---- starting ---------------------------------------------------------------------------------------------------------------

    /**
     * Starts a run; BUSY when one is running (nothing else happens then). A full run that was cut off (or paused for the Wi-Fi) goes on from where it
     * stopped. The undo abandons a full run that was cut off or paused: after it, «جهّز كل شي» starts afresh.
     */
    Start start(Kind k, Options o, Host host) {
        if (k == Kind.FULL && resumable(host.store())) return resume(host) ? Start.STARTED : Start.BUSY;
        synchronized (this) {
            if (running) return Start.BUSY;
            running = true;
            kind = k;
            end = null;
            preview = null;
            promptSince = 0;
            promptLate = false;
            options = o == null ? new Options() : o;
            steps = new ArrayList<>();
            for (Def d : plan(k)) steps.add(new Step(d.id));
        }
        changed();
        final Options opts = options();
        synchronized (this) { live = opts; }
        if (k == Kind.UNDO) {
            try { host.store().put(STATE_FILE, ""); } catch (IOException ignored) { }
        }
        if (k == Kind.FULL) {
            try { host.store().put(SetupReport.STEPS, ""); } catch (IOException ignored) { }
        }
        host.spawn(() -> run(k, opts, host, null));
        return Start.STARTED;
    }

    /** At the TV's start (and when «جهّز كل شي» is pressed with a full run cut off): a full run goes on from runner.txt. False when one is running. */
    boolean resume(Host host) {
        Map<String, String> saved = readState(host.store());
        if (!"FULL".equals(saved.get("kind"))) return false;
        Options o = new Options();
        o.extras = !"0".equals(saved.get("extras"));
        synchronized (this) {
            if (running) return false;
            running = true;
            kind = Kind.FULL;
            end = null;
            preview = null;
            promptSince = 0;
            promptLate = false;
            options = o;
            live = o;
            steps = new ArrayList<>();
            for (Def d : plan(Kind.FULL)) {
                Step s = new Step(d.id);
                String was = saved.get("s." + d.id);
                if (was != null) {
                    int tab = was.indexOf('\t');
                    try { s.state = State.valueOf(tab < 0 ? was : was.substring(0, tab)); } catch (IllegalArgumentException e) { s.state = State.PENDING; }
                    s.reason = tab < 0 ? "" : was.substring(tab + 1);
                }
                steps.add(s);
            }
        }
        changed();
        final String at = saved.get("at"), phase = saved.get("phase");
        host.spawn(() -> run(Kind.FULL, o, host, new String[] {at, phase}));
        return true;
    }

    /** True when a full run was cut off within the last day (runner.txt has a step pointer): «جهّز كل شي» goes on from there. */
    static boolean resumable(SetupEngine.Store store) {
        Map<String, String> s = readState(store);
        if (!"FULL".equals(s.get("kind")) || s.get("at") == null || s.get("at").isEmpty()) return false;
        try { return Math.abs(System.currentTimeMillis() - Long.parseLong(s.get("time"))) < 24L * 3600_000L; } catch (NumberFormatException e) { return false; }
    }

    /** True when the run restarted the TV on purpose and waits for it: only then does the TV's start resume it by itself. */
    static boolean awaitingRestart(SetupEngine.Store store) {
        return resumable(store) && "restart".equals(readState(store).get("await"));
    }

    /** True when the run paused for the Wi-Fi (the person forgets the network and joins it again, then «كمّل»). */
    static boolean awaitingWifi(SetupEngine.Store store) {
        return resumable(store) && "wifi".equals(readState(store).get("await"));
    }

    /** The ADB key stays only while a run waits: for the TV's restart, or for the person's «كمّل» (no second prompt then). */
    static boolean awaiting(SetupEngine.Store store) { return awaitingRestart(store) || awaitingWifi(store); }

    static Map<String, String> readState(SetupEngine.Store store) {
        Map<String, String> out = new LinkedHashMap<>();
        String t = store.get(STATE_FILE);
        if (t == null) return out;
        for (String l : t.split("\n")) {
            int eq = l.indexOf('=');
            if (eq > 0) out.put(l.substring(0, eq), l.substring(eq + 1));
        }
        return out;
    }

    /** The pointer and every finished step, written before a step starts and after it ends (all or nothing: FileStore). */
    private void writeState(SetupEngine.Store store, Options o, String at, String phase) throws IOException {
        StringBuilder b = new StringBuilder("kind=FULL\nextras=" + (o.extras ? "1" : "0") + "\nat=" + at + "\nphase=" + phase + "\ntime=" + System.currentTimeMillis() + "\n");
        for (Step s : steps()) {
            if (s.state == State.DONE || s.state == State.SKIPPED || s.state == State.FAILED) b.append("s.").append(s.id).append('=').append(s.state).append('\t').append(oneLine(s.reason)).append('\n');
        }
        store.put(STATE_FILE, b.toString());
    }

    private static String oneLine(String s) { return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' '); }

    // ---- the run ------------------------------------------------------------------------------------------------------------------

    private void run(Kind k, Options o, Host host, String[] resumeAt) {
        End e;
        boolean complete = false;
        try {
            e = runSteps(k, o, host, resumeAt);
            complete = true;
        } catch (Restarting r) {
            e = null;   // the TV is restarting on purpose: the run goes on after the boot (runner.txt says where), nothing ends here
            try {
                String st = host.store().get(STATE_FILE);
                host.store().put(STATE_FILE, (st == null ? "" : st) + "await=restart\n");
            } catch (IOException ignored) { }
        } catch (Paused p) {
            e = new End(WIFI_ASK_TEXT, false, true);   // the person is asked; «كمّل» resumes the run at the home switch
            try {
                String st = host.store().get(STATE_FILE);
                host.store().put(STATE_FILE, (st == null ? "" : st) + "await=wifi\n");
            } catch (IOException ignored) { }
        } catch (NotStarted n) {
            e = new End(NOT_STARTED, false);
            complete = true;
        } catch (IOException ex) {
            e = new End(cutText(ex.getMessage(), host.store()), true);
        } catch (RuntimeException ex) {
            e = new End("حدث خطأ غير متوقع، ما غيّرنا شيئاً بعده. جرّب «تراجع» ثم أعد.", true);
        }
        if (k == Kind.FULL && e != null && !e.wifi) {
            try {
                // the run is over: nothing to resume. Cut off (the connection dropped): the pointer stays for «جهّز كل شي», but nothing resumes by itself at a boot
                if (complete) host.store().put(STATE_FILE, "");
                else {
                    String st = host.store().get(STATE_FILE);
                    if (st != null) host.store().put(STATE_FILE, st.replace("await=restart\n", ""));
                }
            } catch (IOException ignored) { }
        }
        try { host.finished(); } catch (RuntimeException ignored) { }
        synchronized (this) {
            running = false;
            end = e;
            promptSince = 0;
            awaitingConfirm = false;
        }
        changed();
    }

    private End runSteps(Kind k, Options o, Host host, String[] resumeAt) throws IOException {
        SetupEngine.Out out = this::line;
        Session s;
        if (resumeAt == null) {
            try {
                s = host.open(180_000, () -> {
                    line(PROMPT_LINE);
                    synchronized (this) { promptSince = host.now(); promptLate = false; }
                    changed();
                    host.later(PROMPT_LATE_MS, () -> promptCheck(host));
                }, out);
            } finally {
                synchronized (this) { promptSince = 0; }
            }
            changed();
        } else {
            s = openAgain(host, out);
        }
        try {
            if (resumeAt != null) waitForHome(host);
            Context c = new Context(s.engine(), s.shell(), host.store(), o, out, this, host);
            c.session = s;
            for (Def d : plan(k)) {
                Step cur = step(d.id);
                if (cur == null || cur.state != State.PENDING) continue;    // finished before a restart (runner.txt)
                if (resumeAt != null && d.id.equals(resumeAt[0]) && "started".equals(resumeAt[1]) && !d.repeatable) {
                    // cut off in the middle, and not a step to run twice by itself: said, and left to the person
                    String why = "انقطعت هذه الخطوة (أُعيد تشغيل التلفزيون أو أُغلق التطبيق) فما أعدناها وحدها. اضغط «جهّز كل شي» لإكمالها.";
                    line(why);
                    set(d.id, State.FAILED, why, host);
                    writeState(host.store(), o, d.id, "done");
                    continue;
                }
                if (stopped()) {
                    set(d.id, State.SKIPPED, "توقّفنا قبل أي تغيير.", host);
                    continue;
                }
                if (k == Kind.FULL) { writeState(host.store(), o, d.id, "started"); stamp(host, "start " + d.id); }
                set(d.id, State.RUNNING, "", host);
                SetupEngine.Result r;
                try {
                    r = d.body.run(c);
                } catch (Restarting e) {
                    if (k == Kind.FULL) stamp(host, "stopped " + d.id + " (the TV restarts for the measurement)");
                    throw e;
                } catch (Paused e) {
                    // nothing of the switch was done: the step is pending again, and its pointer says «paused» (not «started»: it is run again)
                    set(d.id, State.PENDING, "ننتظر الواي فاي: انسَ الشبكة واتصل بها من جديد، ثم «كمّل».", host);
                    writeState(host.store(), o, d.id, "paused");
                    stamp(host, "paused " + d.id + " (waiting for the person: the Wi-Fi network)");
                    throw e;
                } catch (NotStarted e) {
                    set(d.id, State.SKIPPED, NOT_STARTED, host);
                    for (Step st : steps()) if (st.state == State.PENDING) set(st.id, State.SKIPPED, NOT_STARTED, host);
                    if (k == Kind.FULL) stamp(host, "stopped " + d.id + " (not started: no «ابدأ»)");
                    throw e;
                } catch (IOException | RuntimeException e) {
                    if (k == Kind.FULL) stamp(host, "stopped " + d.id + " (" + (e instanceof IOException ? "the connection" : "an error") + ")");
                    throw e;
                }
                set(d.id, r.status == SetupEngine.Status.DONE ? State.DONE : r.status == SetupEngine.Status.SKIPPED ? State.SKIPPED : State.FAILED, r.message, host);
                if (k == Kind.FULL) { writeState(host.store(), o, d.id, "done"); stamp(host, "done " + d.id); }
            }
        } finally {
            s.close();
        }
        return endOf(k, steps());
    }

    /** run.sh's steps.log: when each step started, ended or stopped (the report turns it into how long each took). A new full run starts it afresh. */
    private static void stamp(Host host, String what) throws IOException {
        String old = host.store().get(SetupReport.STEPS);
        String line = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(new java.util.Date(host.now())) + " " + what + "\n";
        host.store().put(SetupReport.STEPS, (old == null ? "" : old) + line);
    }

    /** A gate (the precheck, the backup) ran and did not finish: the rest is skipped. */
    private synchronized boolean stopped() {
        for (Step s : steps) if (gate(s.id) && s.state != State.PENDING && s.state != State.RUNNING && s.state != State.DONE) return true;
        return false;
    }

    /** After a restart a home switch that was cut off is put back by the boot path first (SetupRecovery): the run waits for it, ten minutes at most. */
    private void waitForHome(Host host) throws IOException {
        for (int i = 0; i < RESUME_TRIES; i++) {
            String j = host.store().get("home-switch.json");
            if (j == null || !j.contains("\"state\": \"switching\"")) return;
            host.sleep(RESUME_WAIT_MS);
        }
    }

    /** After a restart the TV's adb takes a while: tried every 15 s for ten minutes (the key is known by then: no prompt). */
    private Session openAgain(Host host, SetupEngine.Out out) throws IOException {
        IOException last = null;
        for (int i = 0; i < RESUME_TRIES; i++) {
            try {
                return host.open(5000, null, out);
            } catch (IOException e) {
                last = e;
                host.sleep(RESUME_WAIT_MS);
            }
        }
        throw new IOException("the TV did not come back after the restart" + (last == null ? "" : ": " + last.getMessage()));
    }

    /** The sentence at the end, exactly the one the setup screen always showed. */
    static End endOf(Kind k, List<Step> steps) {
        if (k == Kind.ANSWERS) {
            boolean ok = !steps.isEmpty() && steps.get(0).state == State.DONE;
            return new End(ok ? steps.get(0).reason : "ما تمّت الأجوبة (التفاصيل فوق).", !ok);
        }
        if (k == Kind.UNDO) {
            boolean ok = !steps.isEmpty() && steps.get(0).state == State.DONE;
            return new End(ok ? "تم الرجوع. أطفئ «تصحيح USB» من خيارات المطوّر، واختر «إلغاء تراخيص تصحيح USB» لتنظيف التلفزيون." : "انتهى الرجوع مع ملاحظات (فوق).", false);
        }
        int failed = 0, skipped = 0;
        for (Step s : steps) {
            if (gate(s.id)) {
                if (s.state != State.DONE) return new End("توقّفنا قبل أي تغيير.", true);
                continue;
            }
            if (s.state == State.FAILED) failed++;
            else if (s.state == State.SKIPPED) skipped++;
        }
        if (failed == 0 && skipped == 0) {
            return new End("انتهى الإعداد. اضغط زر الرئيسية في الريموت. ثم أطفئ «تصحيح USB» من خيارات المطوّر واختر «إلغاء تراخيص تصحيح USB» إن لم تحتجهما (التراجع من هنا يحتاجهما).", false);
        }
        return new End("انتهى الإعداد مع " + failed + " خطوة لم تتم و" + skipped + " تُركت (التفاصيل فوق). «تراجع» يرجّع كل شيء.", failed > 0);
    }

    /**
     * A cut connection said from what is TRUE (install.sh's home_stop_line, in the app): with a home switch still waiting for its safety switch (the
     * journal says «switching») the stock home comes back by itself within 5 minutes; with none, the home screen was not touched.
     */
    static String cutText(String m, SetupEngine.Store store) {
        String f = friendly(m);
        if (!f.equals(CUT)) return f;
        if (switching(store)) return "انقطع الاتصال بالتلفزيون أثناء تبديل الشاشة الرئيسية. مفتاح الأمان يرجّع الشاشة الأصلية وحده خلال 5 دقائق؛ إن لم ترجع فاضغط «تراجع». ثم «جهّز كل شي» ليكمل من حيث توقف.";
        return "انقطع الاتصال بالتلفزيون، والشاشة الرئيسية كما هي (ما كنا في منتصف تبديلها). اضغط «جهّز كل شي» ليكمل من حيث توقف.";
    }

    static final String CUT = "انقطع الاتصال بالتلفزيون. إن كنت في منتصف تبديل الشاشة الرئيسية فسترجع الأصلية وحدها بعد 5 دقائق. جرّب مرة ثانية.";

    /** What a connection error means for the person. */
    static String friendly(String m) {
        String cut = CUT;
        if (m == null) return cut;
        if (m.startsWith("debugging is off")) return "تصحيح USB مطفأ في التلفزيون. فعّله (خيارات المطوّر ← تصحيح USB) ثم اضغط «جهّز كل شي».";
        if (m.contains("permission was not accepted")) return "لم تُقبل رسالة «السماح بتصحيح USB؟». إذا ما ظهرت الرسالة أصلاً: أعد تشغيل التلفزيون، ثم افتح «إعداد تلفزيوني» واضغط «جهّز كل شي» وخلك عند التلفزيون، تطلع خلال ثواني. واختر «السماح دائماً».";   // a prompt left unanswered (e.g. under the screensaver) blocks every later one until adbd restarts: TV test, 30 Oct
        if (m.contains("TLS")) return "هذا التلفزيون يطلب التصحيح اللاسلكي؛ استخدم «تصحيح USB» العادي.";
        if (m.startsWith("the TV did not come back")) return "ما رجع الاتصال بالتلفزيون بعد إعادة تشغيله. تأكد أن «تصحيح USB» ما زال مفعّلاً، ثم اضغط «جهّز كل شي» ليكمل الإعداد من حيث توقف.";
        return cut;
    }

    /** For the tests: a fresh runner state (the process-wide one is never reset on the TV). */
    synchronized void resetForTest() {
        ring.clear(); seq = 0; steps = new ArrayList<>(); kind = null; running = false; end = null; listener = null; options = new Options();
        live = null; preview = null; awaitingConfirm = false; confirmed = null; promptSince = 0; promptLate = false;
    }

    static List<String> ids(List<Def> plan) {
        List<String> out = new ArrayList<>();
        for (Def d : plan) out.add(d.id);
        return Collections.unmodifiableList(out);
    }
}
