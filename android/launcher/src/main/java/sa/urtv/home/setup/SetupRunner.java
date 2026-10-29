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
 * The one setup run of this process. The TV's own button («إعداد تلفزيوني») and, later, the phone start runs here and only read from here: ONE run at a
 * time (a second start while one runs is «busy»), a model of its steps (pending, running, done, skipped, failed, each with its reason) and a bounded
 * ring of its lines with sequence numbers, so a screen or a phone that comes back asks for «the lines since n». The run lives on its own thread (on the
 * TV a foreground service holds the process, see SetupService), so it outlives the screen that started it.
 * The full run («FULL») writes its step pointer to runner.txt BEFORE each step: a TV restart in the middle (the measurement restarts it on purpose)
 * resumes from there through the boot path (SetupBootReceiver). A step that is not safe to repeat (the home switch) is never run a second time after
 * a cut: it is reported as cut, and the person presses «ابدأ» for it. Plain Java (no Android) so tests/SetupRunnerRun.java runs it on a JVM.
 */
final class SetupRunner {
    enum Kind { SETUP, UNDO, FULL, ANSWERS }
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

    /** How a run ended: the sentence the screen shows, and whether it is shown as a problem (red). */
    static final class End {
        final String text;
        final boolean bad;
        End(String text, boolean bad) { this.text = text; this.bad = bad; }
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
        /** A text from the internet (UAD-ng's ratings), null when it cannot be fetched. */
        String fetch(String url, int maxBytes);
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

    SetupRunner() { }

    // ---- the plans --------------------------------------------------------------------------------------------------------------

    /** SETUP: the TV's own button, exactly the five steps it always ran, in that order. FULL: the whole setup (the phone's). UNDO: one step. */
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
        if (k == Kind.FULL) return fullPlan();
        p.add(new Def("precheck", true, c -> c.engine.precheck()));
        p.add(new Def("grants", true, c -> c.engine.grants()));
        p.add(new Def("safe", true, c -> c.engine.debloat()));
        p.add(new Def("screensaver", true, c -> c.engine.screensaver()));
        p.add(new Def("home", false, c -> c.engine.home()));
        return p;
    }

    /**
     * The whole setup, in optimize/run.sh's order, with TCL's auto-start before the home switch. The precheck and the backup are gates: when either
     * did not finish, nothing after them runs (run.sh stops there too).
     */
    static List<Def> fullPlan() {
        List<Def> p = new ArrayList<>();
        p.add(new Def("precheck", true, c -> c.engine.precheck()));
        p.add(new Def("backup", true, c -> new SetupPrep(c.shell, c.store, c.out).backup()));
        p.add(new Def("keep", true, c -> new SetupPrep(c.shell, c.store, c.out).keep()));
        p.add(new Def("grants", true, c -> c.engine.grants()));
        p.add(new Def("safe", true, c -> c.engine.debloat()));
        p.add(new Def("community", true, c -> community(c).apply()));
        p.add(new Def("tweaks", true, c -> new SetupTweaks(c.shell, c.store, c.out).apply()));
        p.add(new Def("screensaver", true, c -> c.engine.screensaver()));
        p.add(new Def("autostart", true, c -> new SetupPrep(c.shell, c.store, c.out).autostart()));
        p.add(new Def("home", false, c -> c.engine.home()));
        return p;
    }

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

    /** Starts a run; BUSY when one is running (nothing else happens then). A full run that was cut off goes on from where it stopped. */
    Start start(Kind k, Options o, Host host) {
        if (k == Kind.FULL && resumable(host.store())) return resume(host) ? Start.STARTED : Start.BUSY;
        synchronized (this) {
            if (running) return Start.BUSY;
            running = true;
            kind = k;
            end = null;
            options = o == null ? new Options() : o;
            steps = new ArrayList<>();
            for (Def d : plan(k)) steps.add(new Step(d.id));
        }
        changed();
        final Options opts = options();
        host.spawn(() -> run(k, opts, host, null));
        return Start.STARTED;
    }

    /** At the TV's start (and when «ابدأ» is pressed with a full run cut off): a full run goes on from runner.txt. False when one is running. */
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
            options = o;
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

    /** True when a full run was cut off within the last day (runner.txt has a step pointer): «ابدأ» goes on from there. */
    static boolean resumable(SetupEngine.Store store) {
        Map<String, String> s = readState(store);
        if (!"FULL".equals(s.get("kind")) || s.get("at") == null || s.get("at").isEmpty()) return false;
        try { return Math.abs(System.currentTimeMillis() - Long.parseLong(s.get("time"))) < 24L * 3600_000L; } catch (NumberFormatException e) { return false; }
    }

    /** True when the run restarted the TV on purpose and waits for it: only then does the TV's start resume it by itself, and the ADB key stays. */
    static boolean awaitingRestart(SetupEngine.Store store) {
        return resumable(store) && "restart".equals(readState(store).get("await"));
    }

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
        } catch (IOException ex) {
            e = new End(friendly(ex.getMessage()), true);
        } catch (RuntimeException ex) {
            e = new End("حدث خطأ غير متوقع، ما غيّرنا شيئاً بعده. جرّب «تراجع» ثم أعد.", true);
        }
        if (k == Kind.FULL && e != null) {
            try {
                // the run is over: nothing to resume. Cut off (the connection dropped): the pointer stays for «ابدأ», but nothing resumes by itself at a boot
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
        }
        changed();
    }

    private End runSteps(Kind k, Options o, Host host, String[] resumeAt) throws IOException {
        SetupEngine.Out out = this::line;
        Session s = resumeAt == null
            ? host.open(180_000, () -> line("التلفزيون يسأل «السماح بتصحيح USB؟»: اختر «السماح دائماً» بالريموت."), out)
            : openAgain(host, out);
        try {
            if (resumeAt != null) waitForHome(host);
            Context c = new Context(s.engine(), s.shell(), host.store(), o, out, this, host);
            for (Def d : plan(k)) {
                Step cur = step(d.id);
                if (cur == null || cur.state != State.PENDING) continue;    // finished before a restart (runner.txt)
                if (resumeAt != null && d.id.equals(resumeAt[0]) && "started".equals(resumeAt[1]) && !d.repeatable) {
                    // cut off in the middle, and not a step to run twice by itself: said, and left to the person
                    String why = "انقطعت هذه الخطوة (أُعيد تشغيل التلفزيون أو أُغلق التطبيق) فما أعدناها وحدها. اضغط «ابدأ» لإكمالها.";
                    line(why);
                    set(d.id, State.FAILED, why, host);
                    writeState(host.store(), o, d.id, "done");
                    continue;
                }
                if (stopped()) {
                    set(d.id, State.SKIPPED, "توقّفنا قبل أي تغيير.", host);
                    continue;
                }
                if (k == Kind.FULL) writeState(host.store(), o, d.id, "started");
                set(d.id, State.RUNNING, "", host);
                SetupEngine.Result r = d.body.run(c);
                set(d.id, r.status == SetupEngine.Status.DONE ? State.DONE : r.status == SetupEngine.Status.SKIPPED ? State.SKIPPED : State.FAILED, r.message, host);
                if (k == Kind.FULL) writeState(host.store(), o, d.id, "done");
            }
        } finally {
            s.close();
        }
        return endOf(k, steps());
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

    /** What a connection error means for the person (the same sentences the setup screen always had). */
    static String friendly(String m) {
        String cut = "انقطع الاتصال بالتلفزيون. إن كنت في منتصف تبديل الشاشة الرئيسية فسترجع الأصلية وحدها بعد 5 دقائق. جرّب مرة ثانية.";
        if (m == null) return cut;
        if (m.startsWith("debugging is off")) return "تصحيح USB مطفأ في التلفزيون. فعّله (خيارات المطوّر ← تصحيح USB) ثم اضغط «ابدأ».";
        if (m.contains("permission was not accepted")) return "لم تُقبل رسالة «السماح بتصحيح USB؟». اضغط «ابدأ» وافتح الرسالة بالريموت واختر «السماح دائماً».";
        if (m.contains("TLS")) return "هذا التلفزيون يطلب التصحيح اللاسلكي؛ استخدم «تصحيح USB» العادي.";
        if (m.startsWith("the TV did not come back")) return "ما رجع الاتصال بالتلفزيون بعد إعادة تشغيله. تأكد أن «تصحيح USB» ما زال مفعّلاً، ثم اضغط «ابدأ» ليكمل الإعداد من حيث توقف.";
        return cut;
    }

    /** For the tests: a fresh runner state (the process-wide one is never reset on the TV). */
    synchronized void resetForTest() {
        ring.clear(); seq = 0; steps = new ArrayList<>(); kind = null; running = false; end = null; listener = null; options = new Options();
    }

    static List<String> ids(List<Def> plan) {
        List<String> out = new ArrayList<>();
        for (Def d : plan) out.add(d.id);
        return Collections.unmodifiableList(out);
    }
}
