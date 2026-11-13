package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.3 «تطبيقات مقترحة»: the few apps we suggest, with one line each on why. The settings list them (SuggestedAppsActivity: install, or open the
 * Play Store page) and the in-app setup offers the ones it can install itself as boxes to tick (SetupInstall.extras).
 *
 * How each comes, never from anywhere else:
 * - GITHUB: the newest release of the app's own GitHub repository, the file that fits the TV's processor (the pattern), checked to be that package
 *   (SmartTube and TV Bro, as install.sh and the setup always did).
 * - STORE: no file at all: the app's Play Store page opens and the owner installs it there.
 * - MIRROR: our own app built elsewhere (UrTV Player, com.urtv.player, another signing key), from its own public repository's releases
 *   (tshi-dev/UrTV-Player, tags «<version>» or «v<version>»), files «urtv-player.apk», «urtv-player.apk.sha256» and «urtv-player.version»
 *   (PlayerUpdate). The newest full release with both files is read, else (before 1.0) the newest pre-release; the file must have the published
 *   SHA-256, be that package, AND be signed by exactly the pinned certificate (its SHA-256). Until the certificate is pinned the entry is not
 *   offered at all and nothing is installed (mirrorReady). While that repository cannot be read (private), nothing is offered.
 *
 * No IPTV player is suggested any more (the owner removed the one it had); MAX_IPTV still caps any that is added again.
 * Plain Java: tests/suggested_apps_test.py runs it on a JVM.
 */
public final class SuggestedApps {
    private SuggestedApps() { }

    public enum Kind { GITHUB, STORE, MIRROR }

    public static final class App {
        public final String id, pkg, name, why, repo, file;
        public final Kind kind;
        /** the oldest Android (API level) it runs on */
        public final int minSdk;
        /** ticked at first in the setup's list (the owner's choice: SmartTube and TV Bro, as before) */
        public final boolean setupDefault;
        /** GITHUB: also a box in the in-app setup's list (false for the ones only the settings offer) */
        public boolean setupBox = true;

        App(String id, String pkg, String name, String why, Kind kind, String repo, String file, int minSdk, boolean setupDefault) {
            this.id = id; this.pkg = pkg; this.name = name; this.why = why; this.kind = kind; this.repo = repo; this.file = file;
            this.minSdk = minSdk; this.setupDefault = setupDefault;
        }

        /** MIRROR: the signing certificate's SHA-256 (lower-case hex) the file must carry; "" = not pinned yet */
        public String cert = "";
        /** MIRROR: the tag prefix of its releases here */
        public String tagPrefix = "";

        /** the setup installs it over the TV's own connection (a file it can check); a Play Store app is only offered in the settings */
        public boolean inSetup() { return kind == Kind.GITHUB && setupBox; }   // a Play Store app or a MIRROR one (UrTV Player): the settings only

        /** GITHUB: the releases list it is picked from; STORE: the Play Store page */
        public String address() {
            switch (kind) {
                case GITHUB: return "https://api.github.com/repos/" + repo + "/releases/latest";
                case MIRROR: return "https://api.github.com/repos/" + repo + "/releases?per_page=30";
                default: return "https://play.google.com/store/apps/details?id=" + pkg;
            }
        }
    }

    /** UrTV Player's signing certificate: the SHA-256 of the certificate (lower-case hex, 64 digits; public). Empty would hide the Player. */
    public static final String PLAYER_CERT = "13ade7d009a07f5ba5bab0b7734bd449df10dd47b53868dfa28e44cd8069dfea";
    public static final String PLAYER = "com.urtv.player", PLAYER_REPO = "tshi-dev/UrTV-Player", PLAYER_FILE = "urtv-player.apk", PLAYER_TAG = "";

    public static final String SMARTTUBE = "org.smarttube.stable", TVBRO = "com.phlox.tvwebbrowser", VLC = "org.videolan.vlc", OPENSHELF = "io.github.wingedpanther.openshelf";

    /** In the order shown. The why lines are Arabic; i18n/en/home.tsv has their English. */
    public static final App[] ALL = {
        new App("tvbro", TVBRO, "TV Bro", "متصفح إنترنت يشتغل بالريموت", Kind.GITHUB,
            "truefedex/tv-bro", "tvbro-[0-9.]+-generic-geckoExcluded\\.apk", 21, true),
        new App("smarttube", SMARTTUBE, "SmartTube", "يوتيوب بلا إعلانات، مصمَّم للريموت", Kind.GITHUB,
            "yuliskov/SmartTube", "SmartTube_stable_[0-9.]+_{abi}\\.apk", 21, true),
        new App("vlc", VLC, "VLC", "يشغّل الفيديو والصوت من ذاكرة USB ومن الشبكة", Kind.STORE, null, null, 21, false),
        // 4.3.1 Open Shelf (GPL-3.0, Android 9+): a store of open-source TV apps that installs from each project's GitHub releases. We only offer its own release file (one universal
        // APK, OpenShelf-<version>.apk, checked to be that package); nothing of it is bundled. Unticked and not a box of the setup. A third-party app we have not vetted: the line says so.
        settingsOnly(new App("openshelf", OPENSHELF, "Open Shelf", "متجر لتطبيقات التلفزيون المفتوحة المصدر، يثبّتها من صفحاتها على GitHub. تطبيق من طرف ثالث، ما جرّبناه بأنفسنا. التطبيقات اللي يثبّتها على مسؤوليتك.", Kind.GITHUB,
            "wingedpanther/openshelf", "OpenShelf-[0-9.]+\\.apk", 28, false)),
    };

    private static App settingsOnly(App a) { a.setupBox = false; return a; }

    /** UrTV Player: a MIRROR entry, after the others; offered only once its certificate is pinned (mirrorReady) */
    public static final App PLAYER_APP;

    static {
        App p = new App("player", PLAYER, "UrTV Player", "مشغّل تلفزيونك للفيديو والقوائم، من إصدارات تلفزيونك نفسها", Kind.MIRROR, PLAYER_REPO, PLAYER_FILE, 21, false);
        p.cert = PLAYER_CERT;
        p.tagPrefix = PLAYER_TAG;
        PLAYER_APP = p;
    }

    /** at most this many IPTV players are ever suggested */
    public static final int MAX_IPTV = 2;

    /** the catalog with UrTV Player after the others */
    public static List<App> all() {
        List<App> out = new ArrayList<>(java.util.Arrays.asList(ALL));
        out.add(PLAYER_APP);
        return out;
    }

    public static App byId(String id) {
        for (App a : all()) if (a.id.equals(id)) return a;
        return null;
    }

    /** a MIRROR app may be offered and installed only with a real certificate pinned (64 hex digits) */
    public static boolean mirrorReady(App a) { return a != null && a.kind == Kind.MIRROR && a.cert != null && a.cert.matches("[0-9a-f]{64}"); }

    /** the ones this TV can run (its Android API level) */
    public static List<App> forTv(int sdk) {
        List<App> out = new ArrayList<>();
        for (App a : all()) if (sdk >= a.minSdk && (a.kind != Kind.MIRROR || mirrorReady(a))) out.add(a);
        return out;
    }

    /** the setup's boxes for this TV, in order */
    public static List<App> forSetup(int sdk) {
        List<App> out = new ArrayList<>();
        for (App a : forTv(sdk)) if (a.inSetup()) out.add(a);
        return out;
    }

    /** the ids ticked at first: the ones marked so (SmartTube and TV Bro, as the single box was before) */
    public static List<String> defaults(int sdk) {
        List<String> out = new ArrayList<>();
        for (App a : forSetup(sdk)) if (a.setupDefault) out.add(a.id);
        return out;
    }

    /** "tvbro,smarttube" read back: known setup ids only, each once, in the catalog's order */
    public static List<String> parse(String saved) {
        List<String> out = new ArrayList<>();
        if (saved == null) return out;
        List<String> given = java.util.Arrays.asList(saved.split(","));
        for (App a : all()) if (a.inSetup() && given.contains(a.id)) out.add(a.id);
        return out;
    }

    public static String join(List<String> ids) { return String.join(",", ids); }

    /** GITHUB: a file of that repository's own releases, an .apk, no way out of the path */
    public static boolean githubOk(App a, String url) {
        return a != null && a.kind == Kind.GITHUB && url != null && url.startsWith("https://github.com/" + a.repo + "/releases/download/")
            && url.endsWith(".apk") && !url.contains("..");
    }

    public static String pickAsset(String pattern, String abis, String releaseJson) {
        java.util.Map<String, String> assets = new java.util.LinkedHashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"").matcher(releaseJson == null ? "" : releaseJson);
        while (m.find()) assets.put(m.group(1).substring(m.group(1).lastIndexOf('/') + 1), m.group(1));
        List<String> order = new ArrayList<>();
        for (String a : abis.split(",")) if (!a.isEmpty()) order.add(a);
        order.add("universal");
        order.add("");
        for (String abi : order) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern.replace("{abi}", java.util.regex.Pattern.quote(abi)) + "$");
            List<String> hit = new ArrayList<>();
            for (String n : assets.keySet()) if (p.matcher(n).find()) hit.add(n);
            if (!hit.isEmpty()) {
                java.util.Collections.sort(hit);
                return assets.get(hit.get(0));
            }
        }
        return null;
    }

    /**
     * MIRROR: from the releases list (newest first, as GitHub gives it), the newest full release (not a draft, not a pre-release) whose tag is the
     * app's prefix and a version and that has both files; when there is none, the newest such pre-release (the Player before its 1.0):
     * {the file's address, its .sha256's address}, both on the app's own repository's releases; null when there is none.
     */
    public static String[] pickMirror(App a, String releasesJson) {
        if (a == null || a.kind != Kind.MIRROR || releasesJson == null) return null;
        String[] parts = releasesJson.split("\"tag_name\"\\s*:\\s*\"", -1);
        String[] pre = null;   // the newest pre-release with both files: counts only while there is no full release (the Player before 1.0)
        for (int i = 1; i < parts.length; i++) {
            String seg = parts[i];
            int end = seg.indexOf('"');
            if (end < 0) continue;
            String tag = seg.substring(0, end);
            if (!tag.matches(java.util.regex.Pattern.quote(a.tagPrefix) + TAG_VERSION)) continue;
            if (seg.matches("(?s).*\"draft\"\\s*:\\s*true.*")) continue;
            String base = "https://github.com/" + a.repo + "/releases/download/" + tag + "/" + a.file;
            if (!seg.contains("\"" + base + "\"") || !seg.contains("\"" + base + ".sha256\"")) continue;
            if (!seg.matches("(?s).*\"prerelease\"\\s*:\\s*true.*")) return new String[] {base, base + ".sha256"};
            if (pre == null) pre = new String[] {base, base + ".sha256"};
        }
        return pre;
    }

    /** a release tag after the prefix: a version, «v» first or not */
    static final String TAG_VERSION = "v?[0-9][0-9A-Za-z.\\-]{0,30}";

    /** how long the answer «is a Player release there?» is kept before the screen asks GitHub again */
    public static final long MIRROR_CHECK_MS = 6 * 3600_000L;

    /** the screen shows a MIRROR app only when the kept answer says a valid release was there (and it is not older than a day) */
    public static boolean mirrorShown(String answer, long now) {
        long at = answerAt(answer);
        return answer != null && answer.startsWith("1|") && at > 0 && now >= at && now - at < 24 * 3600_000L;
    }

    /** ask again: never asked, or the answer is older than MIRROR_CHECK_MS (a clock set back too) */
    public static boolean mirrorAskAgain(String answer, long now) {
        long at = answerAt(answer);
        return at <= 0 || now < at || now - at >= MIRROR_CHECK_MS;
    }

    private static long answerAt(String answer) {
        if (answer == null || !answer.matches("[01]\\|\\d{1,15}")) return -1;
        return Long.parseLong(answer.substring(2));
    }

    /** MIRROR: an address of this repository's own «<prefix><version>» release files, nothing else */
    public static boolean mirrorOk(App a, String url) {
        return a != null && a.kind == Kind.MIRROR && url != null
            && url.matches(java.util.regex.Pattern.quote("https://github.com/" + a.repo + "/releases/download/" + a.tagPrefix) + TAG_VERSION + "/"
                + java.util.regex.Pattern.quote(a.file) + "(\\.sha256)?") && !url.contains("..");
    }

    /** the .sha256 file's answer: its first 64 hex digits (sha256sum's form «<hash>  <name>» too), lower case; null when there are none */
    public static String parseSha(String body) {
        if (body == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\b([0-9a-f]{64})\\b").matcher(body.trim());
        return m.find() ? m.group(1).toLowerCase(java.util.Locale.ROOT) : null;
    }

    /** MIRROR: the file is the published one, that package, and signed by exactly the pinned certificate (one signer, the same SHA-256) */
    public static boolean mirrorFileOk(App a, String fileSha, String publishedSha, String pkg, java.util.Set<String> signers) {
        return mirrorReady(a) && fileSha != null && fileSha.equalsIgnoreCase(publishedSha == null ? "" : publishedSha) && a.pkg.equals(pkg)
            && signers != null && signers.size() == 1 && a.cert.equalsIgnoreCase(signers.iterator().next());
    }

    /** lower-case hex of a digest */
    public static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format(java.util.Locale.ROOT, "%02x", x & 0xff));
        return s.toString();
    }
}
