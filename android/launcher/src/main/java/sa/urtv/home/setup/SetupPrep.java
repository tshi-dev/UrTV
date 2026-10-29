package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Three steps of optimize/run.sh, ported: the backup (optimize/backup.sh and run.sh's check of it), the protected features turned back on
 * (optimize/keep.sh) and TCL's auto-start app-op for our two apps (optimize/autostart.sh), with its undo. Plain Java; the parity test runs the
 * scripts and these against the same fake TV (tests/setup_prep_test.py).
 */
final class SetupPrep {
    static final String BACKUP = "backup.txt", AUTOSTART = "autostart-before.txt";
    /** optimize/backup.sh: one command per line, and tv.py --batch's way of writing the answers («$ <line>», then the answer). */
    static final String[] BACKUP_LINES = {
        "echo '== model'; getprop ro.product.model; getprop ro.build.version.release",
        "echo '== disabled packages'; pm list packages -d",
        "echo '== all packages'; pm list packages",
        "echo '== home'; cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN",
        "echo '== screensaver'; settings get secure screensaver_components; settings get secure screensaver_enabled; settings get system screen_off_timeout",
        "echo '== animation'; settings get global window_animation_scale; settings get global transition_animation_scale; settings get global animator_duration_scale",
        "echo '== cached apps (setting, then the value in use since boot)'; device_config get activity_manager max_cached_processes; dumpsys activity settings 2> /dev/null | grep -m1 max_cached_processes; settings get global cached_apps_freezer",
    };
    /** keep.sh: in keep.txt, but the home switch's own (install.sh launcher deals with them). */
    static final String[] HOME_OWN = {"com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "com.google.android.tungsten.setupwraith", "com.tcl.keycustomfunctionservice"};
    static final String[] OURS = {SetupEngine.SELF, SetupEngine.SAVER};

    private final SetupTv tv;
    private final SetupEngine.Store store;
    private final SetupEngine.Out out;

    SetupPrep(SetupEngine.Shell sh, SetupEngine.Store store, SetupEngine.Out out) {
        this.tv = new SetupTv(sh);
        this.store = store;
        this.out = out;
    }

    private SetupEngine.Result done(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.DONE, m); }
    private SetupEngine.Result failed(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.FAILED, m); }

    // ---- the backup (optimize/backup.sh, run.sh's «backup» step) --------------------------------------------------------------------

    /** The text backup.sh writes for these answers. */
    static String backupText(List<String> answers) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < BACKUP_LINES.length; i++) b.append("$ ").append(BACKUP_LINES[i]).append('\n').append(rstrip(answers.get(i))).append('\n');
        return b.toString();
    }

    static String rstrip(String s) {
        int e = s.length();
        while (e > 0 && Character.isWhitespace(s.charAt(e - 1))) e--;
        return s.substring(0, e);
    }

    /** run.sh's check: a backup saved while the TV answered nothing would be kept for ever and make a reset remove the person's own apps. */
    static boolean usable(String backup) {
        int n = 0;
        boolean android = false;
        for (String l : backup.split("\n")) {
            if (l.startsWith("package:")) n++;
            if (l.equals("package:android")) android = true;
        }
        return n >= 20 && android;
    }

    /** The TV as it came, once: the first run's backup stays (a later run would record the TV it changed). Refused, and nothing changed, when it is empty. */
    SetupEngine.Result backup() throws IOException {
        String have = store.get(BACKUP);
        if (have != null && !have.isEmpty()) return done("النسخة الاحتياطية من أول إعداد باقية كما هي (التراجع يرجع ما كان عليه التلفزيون وقتها).");
        List<String> answers = new ArrayList<>();
        for (String l : BACKUP_LINES) answers.add(tv.text(l));
        String text = backupText(answers);
        if (!usable(text)) return failed("قائمة تطبيقات التلفزيون رجعت فاضية (أو شبه فاضية) ونحن نحفظ حالته، فما غيّرنا شيئاً. اضغط «ابدأ» مرة ثانية.");
        store.put(BACKUP, text);
        return done("حفظنا حالة التلفزيون الحالية (التطبيقات، الشاشة الرئيسية، شاشة التوقف، السرعة) قبل أي تغيير.");
    }

    /** The packages of a section of the backup («== disabled packages»), the way run.sh's awk reads it. */
    static Set<String> section(String backup, String name) {
        Set<String> out = new LinkedHashSet<>();
        if (backup == null) return out;
        boolean in = false;
        for (String l : backup.replace("\r", "").split("\n")) {
            if (l.startsWith("== ")) { in = l.startsWith("== " + name); continue; }
            if (in && l.startsWith("package:")) out.add(l.substring("package:".length()));
        }
        return out;
    }

    // ---- the protected features (optimize/keep.sh) ----------------------------------------------------------------------------------

    private Set<String> disabled() throws IOException { return tv.packages("pm list packages -d"); }

    SetupEngine.Result keep() throws IOException {
        Set<String> listed = disabled();
        if (listed == null) return failed("لم يقل التلفزيون أي التطبيقات موقوفة، فما فحصنا الميزات المحمية. اضغط «ابدأ» مرة ثانية.");
        List<String> off = new ArrayList<>();
        for (String p : listed) if (has(SetupLists.KEEP, p) && !has(HOME_OWN, p)) off.add(p);
        if (off.isEmpty()) return done("الميزات المحمية (البث، الصوت، الريموت، المتجر…) كلها شغّالة.");
        for (String p : off) tv.run("pm enable " + p);
        List<String> still = stillOff(off);
        if (still == null) return failed("لم يقل التلفزيون أي التطبيقات موقوفة بعد تشغيلها، فما تأكدنا من الميزات المحمية. اضغط «ابدأ» مرة ثانية.");
        if (!still.isEmpty()) {   // a second try, the way Android 9 sets want it
            for (String p : still) tv.run("pm enable --user 0 " + p);
            still = stillOff(off);
            if (still == null) return failed("لم يقل التلفزيون أي التطبيقات موقوفة بعد تشغيلها، فما تأكدنا من الميزات المحمية. اضغط «ابدأ» مرة ثانية.");
        }
        if (!still.isEmpty()) {
            // what the TV itself keeps off (off before UrTV, per the backup) is not ours; with no backup nothing shows that
            String backup = store.get(BACKUP);
            if (backup == null || backup.isEmpty()) return failed("بعض الميزات المحمية بقيت موقوفة ولا نسخة احتياطية تبيّن إن كانت موقوفة من قبل: " + String.join(" ", still));
            Set<String> before = section(backup, "disabled packages");
            List<String> ours = new ArrayList<>(), kept = new ArrayList<>();
            for (String p : still) (before.contains(p) ? kept : ours).add(p);
            if (!kept.isEmpty()) out.line("تركها التلفزيون نفسه موقوفة (كانت موقوفة قبل UrTV ولا يقبل تشغيلها): " + String.join(" ", kept));
            if (!ours.isEmpty()) return failed("بعض الميزات المحمية بقيت موقوفة، والتلفزيون لا يقبل تشغيلها: " + String.join(" ", ours));
        }
        return done("شغّلنا من جديد ميزات محمية كانت موقوفة: " + String.join(" ", off));
    }

    private List<String> stillOff(List<String> of) throws IOException {
        Set<String> now = disabled();
        if (now == null) return null;
        List<String> out = new ArrayList<>();
        for (String p : of) if (now.contains(p)) out.add(p);
        return out;
    }

    private static boolean has(String[] a, String x) {
        for (String s : a) if (s.equals(x)) return true;
        return false;
    }

    // ---- TCL's auto-start (optimize/autostart.sh) -----------------------------------------------------------------------------------

    boolean isTcl() throws IOException {
        for (String l : tv.text("getprop ro.product.manufacturer; getprop ro.product.brand; pm list packages com.tcl.").toLowerCase(java.util.Locale.ROOT).split("\n")) {
            if (l.equals("tcl") || l.startsWith("package:com.tcl.")) return true;
        }
        return false;
    }

    /**
     * One op as the TV says it, for the undo: allow, ignore, deny, default… («No operations.»: nothing set, so «default»); «none» when this build has
     * no such op, «unknown» when it said nothing.
     */
    String opMode(String pkg, String op) throws IOException {
        String o = tv.text("appops get " + pkg + " " + op + " 2>&1");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^" + op + ": ([a-z_]*)").matcher(o);
        if (m.find() && !m.group(1).isEmpty()) return m.group(1);
        String low = o.toLowerCase(java.util.Locale.ROOT);
        if (low.contains("unknown operation")) return "none";
        return low.contains("no operations") ? "default" : "unknown";
    }

    /** autostart.sh's state(): the first op that has a mode; none, or unknown when the TV said nothing about either. */
    String state(String pkg) throws IOException {
        boolean silent = false;
        for (String op : new String[] {"AUTO_START", "APP_AUTO_START"}) {
            String o = tv.text("appops get " + pkg + " " + op + " 2>&1");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^" + op + ": ([a-z_]*)").matcher(o);
            String got = m.find() ? m.group(1) : "";
            if (!got.isEmpty()) return got;
            if (!o.toLowerCase(java.util.Locale.ROOT).matches("(?s).*(unknown operation|no operations).*")) silent = true;
        }
        return silent ? "unknown" : "none";
    }

    /** Allowed for sa.urtv.home and sa.urtv.screensaver on a TCL, before the home switch; what they had is journaled first (once) for the undo. */
    SetupEngine.Result autostart() throws IOException {
        if (!isTcl()) return done("ليس تلفزيون TCL: لا حاجة لإذن التشغيل التلقائي.");
        List<String> present = new ArrayList<>();
        for (String p : OURS) if (tv.installed(p)) present.add(p);
        String saved = store.get(AUTOSTART);
        if (saved == null || saved.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (String p : present) for (String op : new String[] {"AUTO_START", "APP_AUTO_START"}) b.append(p).append(' ').append(op).append(' ').append(opMode(p, op)).append('\n');
            if (b.length() > 0) store.put(AUTOSTART, b.toString());
        }
        for (String p : present) {
            tv.run("appops set " + p + " AUTO_START allow 2> /dev/null; appops set " + p + " APP_AUTO_START allow 2> /dev/null; cmd appops set " + p
                + " AUTO_START allow 2> /dev/null; cmd appops set " + p + " APP_AUTO_START allow 2> /dev/null; appops write-settings 2> /dev/null; true");
        }
        String line = "", bad = "", silent = "";
        for (String p : present) {
            String st = state(p);
            line += (line.isEmpty() ? "" : ", ") + p + " " + st;
            if (st.equals("allow") || st.equals("none")) continue;
            if (st.equals("unknown")) silent += " " + p; else bad += " " + p + "(" + st + ")";
        }
        if (line.isEmpty()) return done("تطبيقاتنا غير مثبّتة بعد، فلا إذن تشغيل تلقائي نعطيه.");
        if (!silent.isEmpty() && bad.isEmpty()) return failed("لم يقل التلفزيون إن كان يسمح لتطبيقاتنا بالعمل عند التشغيل؛ بعد إعادة التشغيل قد لا يعمل الأذان. اضغط «ابدأ» مرة ثانية.");
        if (!bad.isEmpty()) return failed("لم يسمح التلفزيون لتطبيقاتنا بالعمل عند التشغيل؛ بعد إعادة التشغيل قد لا يعمل الأذان ولا مؤقت النوم.");
        return done("سمح التلفزيون لتطبيقاتنا بالعمل عند التشغيل (الأذان ومؤقت النوم بعد إعادة التشغيل).");
    }

    /** The undo: each op back to the mode it had (an op the build does not know, or one the TV did not say, is left). */
    void autostartUndo() throws IOException {
        String saved = store.get(AUTOSTART);
        if (saved == null || saved.isEmpty()) return;
        boolean any = false;
        for (String l : saved.split("\n")) {
            String[] w = l.trim().split(" ");
            if (w.length != 3 || !SetupEngine.PKG.matcher(w[0]).matches() || !w[1].matches("AUTO_START|APP_AUTO_START") || !w[2].matches("allow|ignore|deny|default|foreground")) continue;
            tv.run("appops set " + w[0] + " " + w[1] + " " + w[2] + " 2> /dev/null");
            any = true;
        }
        if (any) tv.run("appops write-settings 2> /dev/null; true");
        store.put(AUTOSTART, "");
    }
}
