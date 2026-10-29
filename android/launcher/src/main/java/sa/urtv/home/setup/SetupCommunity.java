package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * optimize/community.sh in the app: for a TV our own lists do not know, the community list for exactly this model (CommunityMatch), with every
 * filter the script uses: keep.txt, Android's core, the apps that answer HOME on this TV, launchers and setup wizards, and what was already off
 * before us (not ours: the undo would turn it on). What owners often want (community-ask.txt: an updater, voice, casting, a player) stays ON and
 * becomes a yes/no question (SetupRunner.questions / answer). What is turned off is journaled BEFORE the commands, in community-applied.txt and in the
 * ledger disabled-by-us.txt, and the undo turns it back on. UAD-ng's rating: a package rated Unsafe is never turned off; when the rating cannot be
 * fetched (no internet) the check is skipped and the screen says so. Plain Java.
 */
final class SetupCommunity {
    static final String APPLIED = "community-applied.txt", ASK = "community-ask.txt", NAME = "community-name.txt", LEDGER = "disabled-by-us.txt";
    static final String UAD = "https://raw.githubusercontent.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/main/resources/assets/uad_lists.json";
    static final int UAD_MAX = 8 * 1024 * 1024;
    static final Pattern HOME_LINE = Pattern.compile("^[ \\t]*([A-Za-z0-9_.]*)/.*$", Pattern.MULTILINE);

    /** Fetches a text from the internet (UAD-ng's list); null when it cannot. */
    interface Fetch { String text(String url, int maxBytes); }

    private final SetupTv tv;
    private final SetupEngine.Store store;
    private final SetupEngine.Out out;
    private final String asset;
    private final Fetch fetch;

    SetupCommunity(SetupEngine.Shell sh, SetupEngine.Store store, SetupEngine.Out out, String asset, Fetch fetch) {
        this.tv = new SetupTv(sh);
        this.store = store;
        this.out = out;
        this.asset = asset;
        this.fetch = fetch;
    }

    private SetupEngine.Result done(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.DONE, m); }
    private SetupEngine.Result skipped(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.SKIPPED, m); }
    private SetupEngine.Result failed(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.FAILED, m); }

    static List<String> lines(String text) { return SetupEngine.ledgerNames(text); }

    private void union(String file, List<String> add) throws IOException {
        List<String> all = lines(store.get(file));
        for (String p : add) if (!all.contains(p)) all.add(p);
        StringBuilder b = new StringBuilder();
        for (String p : all) b.append(p).append('\n');
        store.put(file, b.toString());
    }

    /** The packages that answer HOME on this TV (community.sh's sed over query-activities). */
    static Set<String> homes(String text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = HOME_LINE.matcher(text.replace("\r", ""));
        while (m.find()) if (!m.group(1).isEmpty()) out.add(m.group(1));
        return out;
    }

    SetupEngine.Result apply() throws IOException {
        Set<String> installed = tv.packages("pm list packages"), disabled = tv.packages("pm list packages -d");
        if (installed == null || disabled == null) return failed("لم يقل التلفزيون أي التطبيقات عنده وأيها موقوف، فما طبّقنا قائمة المجتمع. اضغط «ابدأ» مرة ثانية.");
        CommunityMatch.Lists lists = CommunityMatch.parse(asset);
        if (lists.metas.isEmpty()) return skipped("قوائم المجتمع ليست في هذه النسخة من التطبيق، فما طبّقنا شيئاً منها.");
        // our lists know a TV when several of their TCL packages are on it (the Google ones are on every Android TV)
        int mine = 0;
        for (String p : lists.knownTcl) if (installed.contains(p)) mine++;
        if (mine >= 3) return done("قوائمنا تعرف هذا التلفزيون، فلا حاجة لقائمة المجتمع.");
        Map<String, String> props = new LinkedHashMap<>();
        for (String k : new String[] {"ro.product.manufacturer", "ro.product.brand", "ro.product.model", "ro.product.device"}) props.put(k, tv.tail1("getprop " + k));
        CommunityMatch.Choice choice = CommunityMatch.choose(props, lists.metas);
        if (choice.meta == null) return done("ما وجدنا قائمة مجتمع لطراز تلفزيونك بالضبط، فما أوقفنا شيئاً منها (" + why(choice.why) + ").");
        Set<String> homes = homes(tv.text("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null"));
        if (homes.isEmpty()) return failed("لم يقل التلفزيون أي تطبيق شاشته الرئيسية، فما أوقفنا شيئاً من قائمة المجتمع. اضغط «ابدأ» مرة ثانية.");
        List<String> pkgs = new ArrayList<>();
        for (String p : CommunityMatch.packages(choice.meta, Arrays.asList(SetupLists.KEEP), homes, installed)) if (!CommunityMatch.core(p)) pkgs.add(p);
        // what owners often want: left on, and asked
        List<String> ask = new ArrayList<>();
        for (String p : pkgs) if (CommunityMatch.ask(p, lists.ask)) ask.add(p);
        pkgs.removeAll(ask);
        StringBuilder a = new StringBuilder();
        for (String p : ask) a.append(p).append('\n');
        store.put(ASK, a.toString());
        if (!ask.isEmpty()) out.line("تركنا " + ask.size() + " تطبيقاً من قائمة المجتمع شغّالاً حتى تقرر (تحديثات، صوت، ربط الجوال، مشغّل فيديو).");
        // already off before we came: not ours
        List<String> todo = new ArrayList<>();
        for (String p : pkgs) if (!disabled.contains(p)) todo.add(p);
        if (!todo.isEmpty()) todo = withoutUnsafe(todo);
        String base = choice.meta.name;
        store.put(NAME, base + "\n");
        if (todo.isEmpty()) return done("قائمة المجتمع لطراز تلفزيونك: لا شيء فيها نوقفه على هذا التلفزيون.");
        union(APPLIED, todo);   // written BEFORE the commands: a run cut half way is still undone
        union(LEDGER, todo);
        for (String p : todo) tv.run("pm disable-user --user 0 " + p);
        Set<String> now = tv.packages("pm list packages -d");
        int off = 0;
        if (now != null) for (String p : todo) if (now.contains(p)) off++;
        return done("أوقفنا " + off + " تطبيقاً من قائمة المجتمع لطراز تلفزيونك (CleanYourTV)" + (off < todo.size() ? "؛ " + (todo.size() - off) + " رفض التلفزيون إيقافها." : "."));
    }

    /** Never what UAD-ng rates Unsafe; when its list cannot be fetched the check is skipped, and said. */
    private List<String> withoutUnsafe(List<String> todo) {
        Map<String, String> rated = CommunityMatch.removals(fetch == null ? null : fetch.text(UAD, UAD_MAX));
        if (rated == null) {
            out.line("فحص «UAD-ng» للتطبيقات غير الآمنة لم يعمل (لا إنترنت)، فتجاوزناه: طبّقنا القائمة بفلاترها فقط.");
            return todo;
        }
        List<String> keep = new ArrayList<>(), unsafe = new ArrayList<>();
        for (String p : todo) ("Unsafe".equals(rated.get(p)) ? unsafe : keep).add(p);
        if (!unsafe.isEmpty()) out.line("تركنا شغّالاً ما يصنّفه «UAD-ng» غير آمن: " + String.join(" ", unsafe));
        return keep;
    }

    private static String why(String en) {
        if (en.contains("brand")) return "لا قائمة لهذه الشركة";
        if (en.contains("more than one") || en.contains("shared")) return "أكثر من قائمة تناسبه، فلا نخمّن";
        return "لا قائمة لهذا الطراز";
    }

    // ---- the questions (community-ask.txt) -----------------------------------------------------------------------------------------

    /** What the run left on and asks about: [package, the question in Arabic]. Only what is still asked (an answered one leaves the file). */
    static List<String[]> questions(SetupEngine.Store store) {
        List<String[]> q = new ArrayList<>();
        for (String p : lines(store.get(ASK))) q.add(new String[] {p, "نوقف «" + p + "»؟ " + kind(p)});
        return q;
    }

    static String kind(String p) {
        String l = p.toLowerCase(java.util.Locale.ROOT);
        if (l.matches(".*(update|upgrade|ota).*")) return "(خدمة تحديث: إن أوقفناها قد لا تصل تحديثات التلفزيون)";
        if (l.matches(".*(voice|assistant).*")) return "(الأوامر الصوتية أو المساعد)";
        if (l.contains("milink")) return "(ربط الجوال بالتلفزيون)";
        if (l.matches(".*(player|mediaexplorer).*")) return "(مشغّل فيديو أو ملفات)";
        return "(قد يحتاجه بعض الناس)";
    }

    /**
     * The person's answers: each «yes» is turned off (journaled first, as the run does), each answered package leaves community-ask.txt. Only a package
     * that is still asked, on the TV, on, and not protected is touched.
     */
    SetupEngine.Result answer(Map<String, Boolean> answers) throws IOException {
        List<String> asked = lines(store.get(ASK));
        if (answers == null || answers.isEmpty()) return done("لا أجوبة جديدة.");
        Set<String> installed = tv.packages("pm list packages"), disabled = tv.packages("pm list packages -d");
        if (installed == null || disabled == null) return failed("لم يقل التلفزيون أي التطبيقات عنده وأيها موقوف، فما غيّرنا شيئاً. جرّب مرة ثانية.");
        List<String> yes = new ArrayList<>();
        for (String p : asked) {
            if (!Boolean.TRUE.equals(answers.get(p))) continue;
            if (!installed.contains(p) || disabled.contains(p) || CommunityMatch.core(p) || Arrays.asList(SetupLists.KEEP).contains(p) || CommunityMatch.NOT_FOR_US.matcher(p).find()) continue;
            yes.add(p);
        }
        if (!yes.isEmpty()) {
            union(APPLIED, yes);
            union(LEDGER, yes);
            for (String p : yes) tv.run("pm disable-user --user 0 " + p);
        }
        List<String> left = new ArrayList<>();
        for (String p : asked) if (!answers.containsKey(p)) left.add(p);
        StringBuilder b = new StringBuilder();
        for (String p : left) b.append(p).append('\n');
        store.put(ASK, b.toString());
        return done(yes.isEmpty() ? "تركنا ما سألناك عنه شغّالاً." : "أوقفنا " + yes.size() + " مما وافقت عليه، و«تراجع» يرجعها.");
    }
}
