package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The speed and memory tweaks of optimize/tweaks.sh, ported line by line, with the same journal (tweaks-journal.txt, and tweaks-journal.done once it
 * was put back): animations at half length, the Play Store kept from running in the background, closed apps frozen while cached, app caches trimmed
 * only when storage is low. What the TV had is recorded BEFORE anything is changed; only the word «null» the TV prints means «not set», an empty or
 * failed read is «unknown» and is never written as null (a restore would delete the TV's real value), and a setting whose original is unknown is not
 * changed. Every value is read back. device_config needs Android 10 (SDK 29), standby buckets Android 9 (28), the «restricted» bucket Android 11 (30).
 * CACHED_LIMIT is not offered here (tweaks.sh keeps it opt-in). The screen's lines are in Arabic; the commands and the journal are the script's, to
 * the letter (the parity test: tests/setup_tweaks_test.py).
 */
final class SetupTweaks {
    static final String JOURNAL = "tweaks-journal.txt", JOURNAL_DONE = "tweaks-journal.done", PS = "com.android.vending";
    static final String HEADER = "# what the TV had before optimize/tweaks.sh apply (null: it was not set; unknown: it could not be read, so it was not changed); optimize/tweaks.sh restore puts exactly this back";

    private final SetupTv tv;
    private final SetupEngine.Store store;
    private final SetupEngine.Out out;
    private int sdk;
    private String sdkSay;
    private final List<String> keys = new ArrayList<>(Arrays.asList("window_animation_scale", "transition_animation_scale", "animator_duration_scale", "cached_apps_freezer", "max_cached_processes"));
    private final List<String> psKeys = new ArrayList<>(Arrays.asList("standby_bucket", "appop_RUN_IN_BACKGROUND", "appop_RUN_ANY_IN_BACKGROUND"));
    private final List<String> skipped = new ArrayList<>();
    private String bucket;

    SetupTweaks(SetupEngine.Shell sh, SetupEngine.Store store, SetupEngine.Out out) {
        this.tv = new SetupTv(sh);
        this.store = store;
        this.out = out;
    }

    private void init() throws IOException {
        String s = tv.text("getprop ro.build.version.sdk").replaceAll("[^0-9]", "");
        sdkSay = s.isEmpty() ? "unknown" : s;
        try { sdk = s.isEmpty() ? 0 : Integer.parseInt(s); } catch (NumberFormatException e) { sdk = 0; }
        if (sdk < 29) { keys.remove("max_cached_processes"); skipped.add("max_cached_processes: device_config needs Android 10 (this TV is SDK " + sdkSay + ")"); }
        bucket = sdk >= 30 ? "restricted" : "rare";
        if (sdk < 28) { psKeys.remove("standby_bucket"); skipped.add("standby_bucket: standby buckets need Android 9 (this TV is SDK " + sdkSay + ")"); }
    }

    private boolean hasKey(String k) { return keys.contains(k) || psKeys.contains(k); }

    static String getcmd(String k) { return k.equals("max_cached_processes") ? "device_config get activity_manager " + k : "settings get global " + k; }

    static String putcmd(String k, String v) {
        if (k.equals("max_cached_processes")) return v.equals("null") ? "device_config delete activity_manager " + k : "device_config put activity_manager " + k + " " + v;
        return v.equals("null") ? "settings delete global " + k : "settings put global " + k + " " + v;
    }

    /** What the TV says for a setting; «unknown» when it said nothing (never «null»: that word is only the TV's own). */
    String tvget(String k) throws IOException {
        String v = tv.tail1(getcmd(k));
        return v.isEmpty() ? "unknown" : v;
    }

    static String bucketName(String n) {
        switch (n) {
            case "5": return "exempted";
            case "10": return "active";
            case "20": return "working_set";
            case "30": return "frequent";
            case "40": return "rare";
            case "45": return "restricted";
            case "50": return "never";
            default: return "unknown";
        }
    }

    String psget(String k) throws IOException {
        if (k.equals("standby_bucket")) return bucketName(tv.tail1("am get-standby-bucket " + PS).replaceAll("[^0-9]", ""));
        String op = k.substring("appop_".length());
        String o = SetupTv.head1of(tv.text("appops get " + PS + " " + op));
        if (o.startsWith("No operations")) return "default";
        if (o.startsWith(op + ": ")) {
            o = o.substring(o.indexOf(": ") + 2);
            int cut = o.length();
            for (char c : new char[] {';', ',', ' '}) { int i = o.indexOf(c); if (i >= 0 && i < cut) cut = i; }
            o = o.substring(0, cut);
            return o.matches("[a-z_]+") ? o : "unknown";
        }
        return "unknown";
    }

    static String psput(String k, String v) {
        return k.equals("standby_bucket") ? "am set-standby-bucket " + PS + " " + v : "cmd appops set " + PS + " " + k.substring("appop_".length()) + " " + v;
    }

    private boolean testBuild() throws IOException {
        for (String l : tv.text("getprop ro.build.type").split("\n")) if (l.equals("userdebug") || l.equals("eng")) return true;
        return false;
    }

    // ---- the journal ----------------------------------------------------------------------------------------------------------------

    private static boolean present(String text) { return text != null && !text.isEmpty(); }

    String jget(String k) {
        String j = store.get(JOURNAL);
        if (j == null) return "";
        for (String l : j.split("\n", -1)) if (l.startsWith(k + "=")) return l.substring(k.length() + 1);
        return "";
    }

    private void jset(String k, String v) throws IOException {
        String j = store.get(JOURNAL);
        if (j == null) j = "";
        StringBuilder b = new StringBuilder();
        boolean found = false;
        for (String l : j.split("\n", -1)) {
            if (l.isEmpty()) continue;
            if (l.startsWith(k + "=")) { b.append(k).append('=').append(v).append('\n'); found = true; }
            else b.append(l).append('\n');
        }
        if (!found) b.append(k).append('=').append(v).append('\n');
        store.put(JOURNAL, b.toString());
    }

    /** What the journal has; an «unknown» one is read again (the TV's value was never touched, so it is still the original). */
    private String original(String k, boolean ps) throws IOException {
        String o = jget(k);
        if (o.isEmpty() || o.equals("unknown")) {
            o = ps ? psget(k) : tvget(k);
            if (!o.equals("unknown")) jset(k, o);
        }
        return o.isEmpty() ? "unknown" : o;
    }

    // ---- apply ----------------------------------------------------------------------------------------------------------------------

    SetupEngine.Result apply() throws IOException {
        init();
        List<String> cmds = new ArrayList<>(), want = new ArrayList<>(), psWant = new ArrayList<>();
        if (!present(store.get(JOURNAL))) {   // the first apply records what the TV had; a later one keeps that record
            StringBuilder b = new StringBuilder(HEADER).append('\n');
            for (String k : keys) b.append(k).append('=').append(tvget(k)).append('\n');
            for (String k : psKeys) b.append(k).append('=').append(psget(k)).append('\n');
            for (String sk : skipped) b.append("skipped_").append(sk, 0, sk.indexOf(':')).append('=').append(sk.substring(sk.indexOf(": ") + 2)).append('\n');
            store.put(JOURNAL_DONE, "");
            store.put(JOURNAL, b.toString());
        }
        if (hasKey("standby_bucket")) {
            String o = original("standby_bucket", true);
            if (o.equals("exempted")) out.line("متجر Play مستثنى من وضع الاستعداد في هذا التلفزيون، فتركناه كما هو.");
            else if (o.equals("unknown")) out.line("لم يقل التلفزيون وضع الاستعداد لمتجر Play، فتركناه كما هو (لا شيء نرجعه).");
            else { cmds.add(psput("standby_bucket", bucket)); psWant.add("standby_bucket=" + bucket); }
        } else {
            out.line("وضع الاستعداد لمتجر Play: تركناه (يحتاج أندرويد 9 أو أحدث، وهذا التلفزيون SDK " + sdkSay + ").");
        }
        for (String op : new String[] {"RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND"}) {
            if (original("appop_" + op, true).equals("unknown")) out.line("لم يقل التلفزيون إذن متجر Play بالعمل في الخلفية (" + op + ")، فتركناه كما هو (لا شيء نرجعه).");
            else { cmds.add(psput("appop_" + op, "deny")); psWant.add("appop_" + op + "=deny"); }
        }
        for (String k : new String[] {"window_animation_scale", "transition_animation_scale", "animator_duration_scale"}) change(k, "0.5", cmds, want);
        if (!hasKey("max_cached_processes")) out.line("عدد التطبيقات المغلقة في الذاكرة: تركناه (يحتاج أندرويد 10، وهذا التلفزيون SDK " + sdkSay + ").");
        else change("max_cached_processes", "null", cmds, want);   // Android's own limit (undoes an older run; the journal has the original)
        change("cached_apps_freezer", "enabled", cmds, want);
        // trimming throws away every app's cache: only worth it when storage is short (under 1 GB, or under 10%)
        String[] w = tv.tail1("df -k /data 2> /dev/null | tail -n 1").trim().split("\\s+");
        String total = w.length > 1 ? w[1] : "", avail = w.length > 3 ? w[3] : "";
        if (total.matches("[0-9]+") && avail.matches("[0-9]+") && Long.parseLong(total) > 0) {
            long t = Long.parseLong(total), a = Long.parseLong(avail);
            if (a < 1048576 || a * 10 < t) {
                cmds.add("pm trim-caches 999G");
                out.line("مساحة التخزين قليلة (" + (a / 1024) + " ميغابايت فاضية من " + (t / 1024) + "): نظّفنا الملفات المؤقتة للتطبيقات.");
            } else {
                out.line("مساحة التخزين تكفي (" + (a / 1024) + " ميغابايت فاضية من " + (t / 1024) + "): تركنا الملفات المؤقتة للتطبيقات.");
            }
        } else {
            out.line("لم يقل التلفزيون مساحة التخزين، فتركنا الملفات المؤقتة للتطبيقات.");
        }
        for (String c : cmds) tv.run(c);
        boolean bad = readBack(want, psWant);
        if (bad) return new SetupEngine.Result(SetupEngine.Status.FAILED, "سرّعنا التلفزيون، لكنه لم يقبل بعض القيم (التفاصيل فوق). «تراجع» يرجع ما تغيّر.");
        return new SetupEngine.Result(SetupEngine.Status.DONE, "سرّعنا التلفزيون: حركات أقصر، ومتجر Play لا يعمل في الخلفية، والتطبيقات المغلقة مجمّدة. «تراجع» يرجعها كما كانت.");
    }

    private void change(String k, String v, List<String> cmds, List<String> want) throws IOException {
        if (original(k, false).equals("unknown")) { out.line("لم يقل التلفزيون قيمة " + k + "، فتركناها كما هي (لا شيء نرجعه)."); return; }
        cmds.add(putcmd(k, v));
        want.add(k + "=" + v);
    }

    /** Every value read back: a setting the TV did not take is said (and makes the step a failure); the standby bucket of a test build is the system's. */
    private boolean readBack(List<String> want, List<String> psWant) throws IOException {
        boolean bad = false;
        for (String kv : want) {
            String k = kv.substring(0, kv.indexOf('=')), v = kv.substring(kv.indexOf('=') + 1), got = tvget(k);
            if (!got.equals(v)) { out.line("التلفزيون لم يقبل " + k + " (طلبنا " + v + "، ويقول " + got + ")."); bad = true; }
        }
        for (String kv : psWant) {
            String k = kv.substring(0, kv.indexOf('=')), v = kv.substring(kv.indexOf('=') + 1), got = psget(k);
            if (!got.equals(v)) {
                if (k.equals("standby_bucket") && testBuild()) out.line("وضع الاستعداد لمتجر Play يبقى " + got + " في نسخة الاختبار هذه من النظام: النظام يحتفظ به.");
                else { out.line("التلفزيون لم يقبل " + k + " (طلبنا " + v + "، ويقول " + got + ")."); bad = true; }
            }
        }
        return bad;
    }

    // ---- restore (the undo) -----------------------------------------------------------------------------------------------------------

    /**
     * The values the journal recorded, exactly (null: deleted). With no journal the app never changed a tweak, so nothing is written (tweaks.sh
     * would write Android's defaults there; the app has nothing of its own to undo). A refused value keeps the journal for the next undo.
     */
    SetupEngine.Result restore() throws IOException {
        if (!present(store.get(JOURNAL))) return new SetupEngine.Result(SetupEngine.Status.DONE, "");
        init();
        List<String> cmds = new ArrayList<>(), want = new ArrayList<>(), psWant = new ArrayList<>();
        for (String k : keys) {
            String o = jget(k);
            if (o.equals("unknown") || o.isEmpty()) out.line("لم نسجّل قيمة " + k + " الأصلية (لم يقلها التلفزيون)، فتركناها كما هي.");
            else if (o.matches("null|[A-Za-z0-9_.-]+")) { cmds.add(putcmd(k, o)); want.add(k + "=" + o); }
            else out.line("القيمة المسجّلة لـ " + k + " ليست مما نرجعه، فتركناها كما هي.");
        }
        for (String k : psKeys) {
            String o = jget(k);
            if (o.equals("exempted")) continue;
            if (o.matches("[a-z_]+") && !o.equals("unknown")) { cmds.add(psput(k, o)); psWant.add(k + "=" + o); }
            else out.line("لم نسجّل قيمة " + k + " الأصلية (لم يقلها التلفزيون)، فتركناها كما هي.");
        }
        out.line("نرجع إعدادات السرعة كما كانت في التلفزيون.");
        for (String c : cmds) tv.run(c);
        boolean bad = readBack(want, psWant);
        if (!bad) {
            store.put(JOURNAL_DONE, store.get(JOURNAL));
            store.put(JOURNAL, "");
            return new SetupEngine.Result(SetupEngine.Status.DONE, "");
        }
        return new SetupEngine.Result(SetupEngine.Status.FAILED, "لم يقبل التلفزيون إرجاع بعض إعدادات السرعة؛ اضغط «تراجع» مرة ثانية.");
    }
}
