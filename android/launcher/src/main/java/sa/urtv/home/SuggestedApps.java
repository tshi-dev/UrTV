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
 * - MIRROR: our own app built elsewhere (UrTV Player, com.urtv.player, another signing key) copied into a public release of this repository, tag
 *   «player-<version>», files «urtv-player.apk» and «urtv-player.apk.sha256». The newest such release is read; the file must have the published
 *   SHA-256, be that package, AND be signed by exactly the pinned certificate (its SHA-256). Until the certificate is pinned the entry is not
 *   offered at all and nothing is installed (mirrorReady).
 *
 * The IPTV player is a player only, offered as its Play Store page: it comes with no channels, films or playlists, and we give no links (the owner
 * adds their own provider).
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

        App(String id, String pkg, String name, String why, Kind kind, String repo, String file, int minSdk, boolean setupDefault) {
            this.id = id; this.pkg = pkg; this.name = name; this.why = why; this.kind = kind; this.repo = repo; this.file = file;
            this.minSdk = minSdk; this.setupDefault = setupDefault;
        }

        /** MIRROR: the signing certificate's SHA-256 (lower-case hex) the file must carry; "" = not pinned yet */
        public String cert = "";
        /** MIRROR: the tag prefix of its releases here */
        public String tagPrefix = "";

        /** the setup installs it over the TV's own connection (a file it can check); a Play Store app is only offered in the settings */
        public boolean inSetup() { return kind == Kind.GITHUB; }   // a Play Store app or a MIRROR one (UrTV Player): the settings only

        /** GITHUB: the releases list it is picked from; STORE: the Play Store page */
        public String address() {
            switch (kind) {
                case GITHUB: return "https://api.github.com/repos/" + repo + "/releases/latest";
                case MIRROR: return "https://api.github.com/repos/" + repo + "/releases?per_page=30";
                default: return "https://play.google.com/store/apps/details?id=" + pkg;
            }
        }
    }

    /** UrTV Player's signing certificate, SHA-256 of the certificate (lower-case hex, 64 digits). PLACEHOLDER: the owner has not made the key yet;
     *  while it is empty the Player is not offered and never installed. */
    public static final String PLAYER_CERT = "";
    public static final String PLAYER = "com.urtv.player", PLAYER_REPO = "tshi-dev/UrTV", PLAYER_FILE = "urtv-player.apk", PLAYER_TAG = "player-";

    public static final String SMARTTUBE = "org.smarttube.stable", TVBRO = "com.phlox.tvwebbrowser", VLC = "org.videolan.vlc", TIVIMATE = "ar.tvplayer.tv";

    /** In the order shown. The why lines are Arabic; i18n/en/home.tsv has their English. */
    public static final App[] ALL = {
        new App("tvbro", TVBRO, "TV Bro", "متصفح إنترنت يشتغل بالريموت", Kind.GITHUB,
            "truefedex/tv-bro", "tvbro-[0-9.]+-generic-geckoExcluded\\.apk", 21, true),
        new App("smarttube", SMARTTUBE, "SmartTube", "يوتيوب بلا إعلانات، مصمَّم للريموت", Kind.GITHUB,
            "yuliskov/SmartTube", "SmartTube_stable_[0-9.]+_{abi}\\.apk", 21, true),
        new App("vlc", VLC, "VLC", "يشغّل الفيديو والصوت من ذاكرة USB ومن الشبكة", Kind.STORE, null, null, 21, false),
        // the IPTV player: its Play Store page only (never a file from us), unticked, not in the setup. A player only: no channels or playlists,
        // and we give none (the owner adds their own provider). TiviMate (Armobsoft, ar.tvplayer.tv), made for the TV remote.
        new App("tivimate", TIVIMATE, "TiviMate", "مشغّل IPTV فارغ من متجر Play: تضيف قائمتك أنت، وما فيه ولا نعطي أي قنوات", Kind.STORE, null, null, 21, false),
    };

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
     * MIRROR: from the releases list (newest first, as GitHub gives it), the newest release whose tag is the app's prefix and a version, not a draft
     * or a pre-release: {the file's address, its .sha256's address}, both on this repository's own releases; null when there is none.
     */
    public static String[] pickMirror(App a, String releasesJson) {
        if (a == null || a.kind != Kind.MIRROR || releasesJson == null) return null;
        String[] parts = releasesJson.split("\"tag_name\"\\s*:\\s*\"", -1);
        for (int i = 1; i < parts.length; i++) {
            String seg = parts[i];
            int end = seg.indexOf('"');
            if (end < 0) continue;
            String tag = seg.substring(0, end);
            if (!tag.matches(java.util.regex.Pattern.quote(a.tagPrefix) + "[0-9][0-9A-Za-z.\\-]{0,30}")) continue;
            if (seg.matches("(?s).*\"draft\"\\s*:\\s*true.*") || seg.matches("(?s).*\"prerelease\"\\s*:\\s*true.*")) continue;
            String base = "https://github.com/" + a.repo + "/releases/download/" + tag + "/" + a.file;
            if (seg.contains("\"" + base + "\"") && seg.contains("\"" + base + ".sha256\"")) return new String[] {base, base + ".sha256"};
        }
        return null;
    }

    /** MIRROR: an address of this repository's own «<prefix><version>» release files, nothing else */
    public static boolean mirrorOk(App a, String url) {
        return a != null && a.kind == Kind.MIRROR && url != null
            && url.matches(java.util.regex.Pattern.quote("https://github.com/" + a.repo + "/releases/download/" + a.tagPrefix) + "[0-9][0-9A-Za-z.\\-]{0,30}/"
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
