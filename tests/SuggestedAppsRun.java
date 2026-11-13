package sa.urtv.home;

import java.util.Arrays;

/** For tests/suggested_apps_test.py: SuggestedApps' rules. One line per check: name TAB ok|FAIL. */
public final class SuggestedAppsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] x) {
        SuggestedApps.App tube = SuggestedApps.byId("smarttube"), vlc = SuggestedApps.byId("vlc");
        check("no Lumen and no pinned third-party file: GitHub releases, Play Store pages, and our own mirrored Player only", SuggestedApps.byId("lumen") == null
            && SuggestedApps.Kind.values().length == 3 && SuggestedApps.Kind.valueOf("MIRROR") != null && java.util.Arrays.stream(SuggestedApps.ALL).noneMatch(e -> e.kind == SuggestedApps.Kind.MIRROR));
        check("TiviMate is gone from the suggestions", SuggestedApps.byId("tivimate") == null);
        check("a GitHub app only from its own repository's releases, an .apk, no way out", SuggestedApps.githubOk(tube, "https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk")
            && !SuggestedApps.githubOk(tube, "https://github.com/evil/SmartTube/releases/download/29.1/a.apk") && !SuggestedApps.githubOk(tube, "https://github.com/yuliskov/SmartTube/releases/download/../x.apk")
            && !SuggestedApps.githubOk(tube, "http://github.com/yuliskov/SmartTube/releases/download/29.1/a.apk") && !SuggestedApps.githubOk(vlc, "https://github.com/x/y/releases/download/1/a.apk"));
        check("a Play Store app has no file: its page only, and is not in the setup", vlc.kind == SuggestedApps.Kind.STORE && vlc.file == null && !vlc.inSetup()
            && vlc.address().equals("https://play.google.com/store/apps/details?id=org.videolan.vlc"));
        SuggestedApps.App shelf = SuggestedApps.byId("openshelf");
        check("every suggestion but Open Shelf runs on any TV we support (Android 5+); Open Shelf needs Android 9+", SuggestedApps.forTv(21).size() == SuggestedApps.all().size() - 1 && SuggestedApps.forTv(27).indexOf(shelf) < 0
            && SuggestedApps.forTv(28).size() == SuggestedApps.all().size() && SuggestedApps.forTv(28).contains(shelf));
        check("Open Shelf: a GitHub entry after VLC, from its own repository's release (OpenShelf-x.y.z.apk, one universal file), that package, unticked and not a box of the setup",
            shelf.kind == SuggestedApps.Kind.GITHUB && shelf.repo.equals("wingedpanther/openshelf") && shelf.pkg.equals("io.github.wingedpanther.openshelf") && shelf.minSdk == 28 && !shelf.setupDefault
            && !shelf.inSetup() && SuggestedApps.ALL[3] == shelf && SuggestedApps.ALL[2] == vlc
            && "https://github.com/wingedpanther/openshelf/releases/download/v0.6.0/OpenShelf-0.6.0.apk".equals(SuggestedApps.pickAsset(shelf.file, "arm64-v8a,armeabi-v7a",
                "{\"browser_download_url\": \"https://github.com/wingedpanther/openshelf/releases/download/v0.6.0/OpenShelf-0.6.0.apk\"}, {\"browser_download_url\": \"https://github.com/wingedpanther/openshelf/archive/refs/tags/v0.6.0.zip\"}"))
            && SuggestedApps.githubOk(shelf, "https://github.com/wingedpanther/openshelf/releases/download/v0.6.0/OpenShelf-0.6.0.apk") && !SuggestedApps.githubOk(shelf, "https://github.com/evil/openshelf/releases/download/v0.6.0/OpenShelf-0.6.0.apk"));
        check("Open Shelf's line carries the disclaimer: a third-party app we have not vetted, its apps at the person's risk", shelf.why.contains("تطبيق من طرف ثالث، ما جرّبناه بأنفسنا") && shelf.why.contains("على مسؤوليتك"));
        check("the setup's first ticks are TV Bro and SmartTube, as the one box was", SuggestedApps.defaults(34).equals(Arrays.asList("tvbro", "smarttube")));
        check("the setup's boxes: the two it can install, TV Bro then SmartTube (no store app)", SuggestedApps.forSetup(34).size() == 2 && SuggestedApps.forSetup(34).get(1) == tube);
        check("saved ticks read back: known setup ids, once, in order; a store app or a stranger dropped", SuggestedApps.parse("smarttube,vlc,x,tivimate,tvbro,smarttube").equals(Arrays.asList("tvbro", "smarttube"))
            && SuggestedApps.parse("").isEmpty() && SuggestedApps.parse(null).isEmpty());
        int iptv = 0;
        for (SuggestedApps.App a : SuggestedApps.ALL) if (a.why.contains("IPTV")) iptv++;
        check("at most two IPTV players", iptv <= SuggestedApps.MAX_IPTV && SuggestedApps.MAX_IPTV == 2);
        check("pickAsset: the TV's processor first", "https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk".equals(SuggestedApps.pickAsset(tube.file, "arm64-v8a,armeabi-v7a",
            "{\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_armeabi-v7a.apk\"}, {\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk\"}")));
        check("hex", SuggestedApps.hex(new byte[] {0, 15, (byte) 255}).equals("000fff"));

        // 4.3 UrTV Player: a MIRROR entry, from this repository's own «player-<version>» releases
        SuggestedApps.App pl = SuggestedApps.PLAYER_APP;
        check("UrTV Player: its certificate pinned (the owner's key), offered in the settings, never in the setup", SuggestedApps.PLAYER_CERT.equals("13ade7d009a07f5ba5bab0b7734bd449df10dd47b53868dfa28e44cd8069dfea")
            && SuggestedApps.mirrorReady(pl) && SuggestedApps.forTv(34).contains(pl) && !pl.inSetup() && !SuggestedApps.forSetup(34).contains(pl));
        SuggestedApps.App unpinned = new SuggestedApps.App("player", "com.urtv.player", "UrTV Player", "x", SuggestedApps.Kind.MIRROR, "tshi-dev/UrTV", "urtv-player.apk", 21, false);
        unpinned.tagPrefix = "player-";
        long now = 1_800_000_000_000L, h = 3600_000L;
        check("the Player's row is shown only after a valid release was seen, for a day at most; never asked, none there, or an odd answer: hidden",
            SuggestedApps.mirrorShown("1|" + (now - h), now) && !SuggestedApps.mirrorShown("1|" + (now - 25 * h), now) && !SuggestedApps.mirrorShown("0|" + (now - h), now)
            && !SuggestedApps.mirrorShown("", now) && !SuggestedApps.mirrorShown(null, now) && !SuggestedApps.mirrorShown("1|x", now) && !SuggestedApps.mirrorShown("1|" + (now + h), now));
        check("GitHub is asked again only after 6 hours (or never asked, or a clock set back)", !SuggestedApps.mirrorAskAgain("0|" + (now - 5 * h), now)
            && SuggestedApps.mirrorAskAgain("0|" + (now - 6 * h), now) && SuggestedApps.mirrorAskAgain("", now) && SuggestedApps.mirrorAskAgain("1|" + (now + h), now));
        check("a MIRROR entry without a pinned certificate is never ready and never installs", !SuggestedApps.mirrorReady(unpinned)
            && !SuggestedApps.mirrorFileOk(unpinned, "ab".repeat(32), "ab".repeat(32), "com.urtv.player", new java.util.HashSet<>(java.util.Collections.singleton("cd".repeat(32)))));
        SuggestedApps.App t = new SuggestedApps.App("player", "com.urtv.player", "UrTV Player", "x", SuggestedApps.Kind.MIRROR, "tshi-dev/UrTV", "urtv-player.apk", 21, false);
        t.tagPrefix = "player-"; t.cert = "cd".repeat(32);
        String base = "https://github.com/tshi-dev/UrTV/releases/download/";
        String json = "[{\"tag_name\": \"player-1.3.0\", \"draft\": false, \"prerelease\": true, \"assets\": [{\"browser_download_url\": \"" + base + "player-1.3.0/urtv-player.apk\"}, {\"browser_download_url\": \"" + base + "player-1.3.0/urtv-player.apk.sha256\"}]},"
            + " {\"tag_name\": \"v4.3.210\", \"assets\": [{\"browser_download_url\": \"" + base + "v4.3.210/urtv-home.apk\"}]},"
            + " {\"tag_name\": \"player-1.2.0\", \"draft\": false, \"prerelease\": false, \"assets\": [{\"browser_download_url\": \"" + base + "player-1.2.0/urtv-player.apk\"}, {\"browser_download_url\": \"" + base + "player-1.2.0/urtv-player.apk.sha256\"}]},"
            + " {\"tag_name\": \"player-1.1.0\", \"assets\": [{\"browser_download_url\": \"" + base + "player-1.1.0/urtv-player.apk\"}]}]";
        String[] got = SuggestedApps.pickMirror(t, json);
        check("the newest «player-» release that is not a pre-release or draft and has both files; our other releases and one without its .sha256 skipped",
            got != null && got[0].equals(base + "player-1.2.0/urtv-player.apk") && got[1].equals(base + "player-1.2.0/urtv-player.apk.sha256")
            && SuggestedApps.pickMirror(t, "[]") == null && SuggestedApps.pickMirror(t, null) == null);
        check("only this repository's own player releases", SuggestedApps.mirrorOk(t, base + "player-1.2.0/urtv-player.apk") && SuggestedApps.mirrorOk(t, base + "player-1.2.0/urtv-player.apk.sha256")
            && !SuggestedApps.mirrorOk(t, "https://github.com/evil/UrTV/releases/download/player-1.2.0/urtv-player.apk") && !SuggestedApps.mirrorOk(t, base + "v4.3.210/urtv-player.apk")
            && !SuggestedApps.mirrorOk(t, base + "player-1.2.0/other.apk") && !SuggestedApps.mirrorOk(t, base + "player-../x/urtv-player.apk"));
        check("the .sha256 file: its 64 hex digits, sha256sum's form too; anything else nothing", ("ab".repeat(32)).equals(SuggestedApps.parseSha("AB".repeat(32) + "  urtv-player.apk\n"))
            && SuggestedApps.parseSha("no hash") == null && SuggestedApps.parseSha(null) == null);
        java.util.Set<String> one = new java.util.HashSet<>(java.util.Collections.singleton("cd".repeat(32)));
        java.util.Set<String> other = new java.util.HashSet<>(java.util.Collections.singleton("ef".repeat(32)));
        java.util.Set<String> two = new java.util.HashSet<>(java.util.Arrays.asList("cd".repeat(32), "ef".repeat(32)));
        check("installed only when all three hold: the published SHA-256, the package, exactly the pinned certificate",
            SuggestedApps.mirrorFileOk(t, "ab".repeat(32), "ab".repeat(32), "com.urtv.player", one)
            && !SuggestedApps.mirrorFileOk(t, "ab".repeat(32), "aa".repeat(32), "com.urtv.player", one)
            && !SuggestedApps.mirrorFileOk(t, "ab".repeat(32), null, "com.urtv.player", one)
            && !SuggestedApps.mirrorFileOk(t, "ab".repeat(32), "ab".repeat(32), "com.other", one)
            && !SuggestedApps.mirrorFileOk(t, "ab".repeat(32), "ab".repeat(32), "com.urtv.player", other)
            && !SuggestedApps.mirrorFileOk(t, "ab".repeat(32), "ab".repeat(32), "com.urtv.player", two)
            && !SuggestedApps.mirrorFileOk(t, "ab".repeat(32), "ab".repeat(32), "com.urtv.player", new java.util.HashSet<String>()));
    }
}
