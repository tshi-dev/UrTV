package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * optimize/measure.sh in the app, «the best» measurement (the owner's choice): the TV is restarted, left 3 minutes on its home screen (kept awake),
 * then read: memory (/proc/meminfo, dumpsys meminfo), processes, CPU, the disabled count, what is on screen, the biggest processes, the tweaks and our
 * two apps. The same commands, the same ram-<label>.txt and the same row in summary.md. The run cannot outlive the restart, so the step writes where
 * it is (measure-phase.txt: the label and the boot it restarts from) BEFORE it sends `reboot`, and the runner resumes after the boot; the step then
 * sees a new boot and reads. It never restarts the TV twice for one reading: the same boot after a resume means it reads at once, without a restart.
 * A TV whose adb would not be on port 5555 after a restart (opened only for this session, as `adb tcpip` does, or Wireless debugging) is not restarted:
 * it is read at once, and the report says «بدون إعادة تشغيل». Plain Java.
 */
final class SetupMeasure {
    static final String PHASE = "measure-phase.txt", SUMMARY = "summary.md", KINDS = "measure-kinds.txt", TIMEOUT = "screen-off-timeout.txt";
    static final String NO_RESTART = "بدون إعادة تشغيل";
    static final int SETTLE_MS = 180_000, PACKAGE_SETTLE_MS = 15_000;
    /** measure.sh's batch, line for line. */
    static final String[] READ = {
        "echo '== memory'; grep -E 'MemTotal|MemAvailable|SwapTotal|SwapFree' /proc/meminfo",
        "echo '== android'; dumpsys meminfo | grep -E 'Total RAM|Free RAM|Used RAM|Lost RAM'",
        "echo '== processes'; ps -A | wc -l",
        "echo '== cpu'; top -b -n 1 | head -14",
        "echo '== disabled packages'; pm list packages -d | wc -l",
        "echo '== on screen'; dumpsys activity activities 2> /dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity'; dumpsys dreams 2> /dev/null | grep -m1 -iE 'dreaming|mCurrentDream'",
        "echo '== biggest'; dumpsys meminfo | sed -n '/Total [PR]SS by process/,/^$/p' | head -16",
        "echo '== tweaks'; for k in window_animation_scale transition_animation_scale animator_duration_scale cached_apps_freezer; do echo \"$k=$(settings get global $k)\"; done; echo \"max_cached_processes=$(device_config get activity_manager max_cached_processes)\"",
        "echo '== our apps'; for p in sa.urtv.screensaver sa.urtv.home; do echo \"$p: $(dumpsys meminfo $p 2> /dev/null | grep -m1 -E '^ *TOTAL')\"; done",
    };

    private final SetupTv tv;
    private final SetupEngine.Store store;
    private final SetupEngine.Out out;
    private final SetupEngine.Sleeper sleeper;

    SetupMeasure(SetupEngine.Shell sh, SetupEngine.Store store, SetupEngine.Out out, SetupEngine.Sleeper sleeper) {
        this.tv = new SetupTv(sh);
        this.store = store;
        this.out = out;
        this.sleeper = sleeper;
    }

    private SetupEngine.Result done(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.DONE, m); }

    /** Which boot this is (a new number after every restart); «» when the TV does not say. */
    String bootId() throws IOException { return tv.tail1("cat /proc/sys/kernel/random/boot_id").trim(); }

    /** Will adb be on port 5555 again after a restart? Only when the TV keeps it there itself (a persistent setting, or its own build.prop). */
    boolean adbSurvivesRestart() throws IOException {
        String t = tv.text("getprop persist.adb.tcp.port; grep -h -s -E '^(persist|service)\\.adb\\.tcp\\.port=5555$' /system/build.prop /vendor/build.prop /product/build.prop /odm/build.prop 2> /dev/null");
        for (String l : t.split("\n")) if (l.trim().equals("5555") || l.trim().endsWith(".adb.tcp.port=5555")) return true;
        return false;
    }

    /**
     * One reading. restartAllowed false: never a restart (a home switch is still waiting for its safety switch). Throws SetupRunner.Restarting after
     * it sent `reboot`: the run ends there and resumes after the boot.
     */
    SetupEngine.Result measure(String label, boolean restartAllowed) throws IOException {
        if (label.startsWith("before") && hasRow(store.get(SUMMARY), "before")) return done("القياس قبل الإعداد موجود من قبل: أبقيناه.");
        String phase = store.get(PHASE);
        boolean restarted = false;
        if (phase != null && phase.startsWith(label + " ")) {
            String was = phase.substring(label.length() + 1).trim(), now = bootId();
            store.put(PHASE, "");
            if (!now.isEmpty() && !now.equals(was)) restarted = true;
            else out.line("التلفزيون لم يُعِد التشغيل كما طلبنا، فلن نطلبه مرة ثانية: نقيس الآن " + NO_RESTART + ".");
        } else if (!restartAllowed) {
            out.line("نقيس الآن " + NO_RESTART + " (تبديل الشاشة الرئيسية ما زال ينتظر تأكيده).");
        } else if (!adbSurvivesRestart()) {
            out.line("اتصال التصحيح في هذا التلفزيون لا يبقى بعد إعادة التشغيل، فلن نعيد تشغيله: نقيس الآن " + NO_RESTART + "، والقراءة قد تكون أعلى قليلاً.");
        } else {
            String boot = bootId();
            if (boot.isEmpty()) {
                out.line("لم يقل التلفزيون رقم تشغيله، فلن نعيد تشغيله: نقيس الآن " + NO_RESTART + ".");
            } else {
                // Android writes what was disabled about 10 s after a change: a restart before that can lose it (optimize/settle.sh)
                sleeper.sleep(PACKAGE_SETTLE_MS);
                tv.run("sync");
                store.put(PHASE, label + " " + boot + "\n");   // BEFORE the restart: the resumed run knows it was sent, and never sends it twice
                out.line("نعيد تشغيل التلفزيون للقياس الدقيق. الإعداد يكمل وحده بعد التشغيل (حوالي 5 دقائق)؛ لا تطفئه.");
                try { tv.run("reboot"); } catch (IOException gone) { /* the connection drops as the TV goes down */ }
                throw new SetupRunner.Restarting();
            }
        }
        return read(label, restarted);
    }

    /** measure.sh after the restart (or at once): the screensaver timeout stretched and put back exactly, Home, 3 minutes when it restarted, the batch. */
    SetupEngine.Result read(String label, boolean restarted) throws IOException {
        putTimeoutBack();
        String timeout = tv.tail1("settings get system screen_off_timeout").trim();
        boolean stretched = false;
        if (timeout.equals("null") || timeout.matches("[0-9].*")) {
            store.put(TIMEOUT, timeout + "\n");
            tv.run("settings put system screen_off_timeout 1800000");   // 30 min: stay on the home screen
            stretched = true;
        } else {
            out.line("لم يقل التلفزيون مهلة شاشة التوقف، فما مددناها؛ قد تفسد شاشة التوقف هذه القراءة.");
        }
        String text;
        try {
            tv.run("input keyevent KEYCODE_HOME");
            if (restarted) {
                out.line("نترك التلفزيون 3 دقائق على الشاشة الرئيسية ثم نقيس…");
                sleeper.sleep(SETTLE_MS);
            }
            StringBuilder b = new StringBuilder();
            for (String l : READ) b.append("$ ").append(l).append('\n').append(SetupPrep.rstrip(tv.text(l))).append('\n');
            text = b.toString();
        } finally {
            if (stretched) putTimeoutBack();
        }
        store.put("ram-" + label + ".txt", text);
        store.put(SUMMARY, summaryWith(store.get(SUMMARY), label, text));
        String kinds = store.get(KINDS) == null ? "" : store.get(KINDS);
        StringBuilder k = new StringBuilder();
        for (String l : kinds.split("\n")) if (!l.isEmpty() && !l.startsWith(label + " ")) k.append(l).append('\n');
        k.append(label).append(' ').append(restarted ? "restart" : "now").append('\n');
        store.put(KINDS, k.toString());
        String row = row(label, text);
        String[] c = row.split("\\|");
        return done("قِسنا الذاكرة " + (label.startsWith("before") ? "قبل" : "بعد") + " الإعداد" + (restarted ? " بعد إعادة التشغيل" : " (" + NO_RESTART + ")") + ": " + c[2].trim() + " ميغابايت فاضية.");
    }

    /** The TV's own screensaver timeout, exactly as it was (null: deleted); also after a run that died while it was stretched. */
    void putTimeoutBack() throws IOException {
        String t = store.get(TIMEOUT);
        if (t == null || t.trim().isEmpty()) return;
        t = t.trim();
        if (t.equals("null")) tv.run("settings delete system screen_off_timeout");
        else if (t.matches("[0-9]+")) tv.run("settings put system screen_off_timeout " + t);
        store.put(TIMEOUT, "");
    }

    // ---- the numbers, as measure.sh reads them --------------------------------------------------------------------------------------

    static boolean hasRow(String summary, String label) {
        if (summary == null) return false;
        for (String l : summary.split("\n")) if (l.startsWith("| " + label + " |")) return true;
        return false;
    }

    static long kb(String text, String key) {
        Matcher m = Pattern.compile("(?m)^" + key + ":[ \\t]*([0-9]+)").matcher(text);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    /** sed -n '/<what>/{n;n;p;}': the line two below the first line that holds it, digits only. */
    static String twoBelow(String text, String what) {
        String[] l = text.split("\n", -1);
        for (int i = 0; i < l.length; i++) if (l[i].contains(what)) return i + 2 < l.length ? l[i + 2].replaceAll("[^0-9]", "") : "";
        return "";
    }

    static String firstNumberBefore(String text, String suffix) {
        Matcher m = Pattern.compile("([0-9]+)" + Pattern.quote(suffix)).matcher(text);
        return m.find() ? m.group(1) : "";
    }

    /** The summary.md row measure.sh writes for this reading. */
    static String row(String label, String text) {
        long availK = kb(text, "MemAvailable"), totalK = kb(text, "MemTotal");
        long avail = availK / 1024, total = totalK / 1024;
        long swap = (kb(text, "SwapTotal") - kb(text, "SwapFree")) / 1024;
        String procs = twoBelow(text, "== processes");
        String cpus = firstNumberBefore(text, "%cpu"), idle = firstNumberBefore(text, "%idle");
        if (!cpus.isEmpty() && !idle.isEmpty() && Long.parseLong(cpus) > 0) idle = String.valueOf(Long.parseLong(idle) * 100 / Long.parseLong(cpus));
        String disabled = twoBelow(text, "== disabled packages");
        String a = String.valueOf(avail);
        if (avail == 0 || total == 0 || avail > total) a = "not measured (memory read failed)";
        return "| " + label + " | " + a + " | " + swap + " | " + procs + " | " + (idle.isEmpty() ? "" : idle + "%") + " | " + disabled + " |";
    }

    /** summary.md with this reading's row (a repeated reading replaces its row; the header carries the total of the first reading). */
    static String summaryWith(String summary, String label, String text) {
        long total = kb(text, "MemTotal") / 1024, avail = kb(text, "MemAvailable") / 1024;
        boolean bad = avail == 0 || total == 0 || avail > total;
        List<String> lines = new ArrayList<>();
        if (summary == null || summary.isEmpty()) {
            lines.add("| state | free RAM (MB" + (bad ? "" : " of " + total) + ") | swap used (MB) | processes | CPU idle | disabled packages |");
            lines.add("|---|---|---|---|---|---|");
        } else {
            for (String l : summary.split("\n")) if (!l.isEmpty() && !l.startsWith("| " + label + " |")) lines.add(l);
        }
        lines.add(row(label, text));
        return String.join("\n", lines) + "\n";
    }
}
