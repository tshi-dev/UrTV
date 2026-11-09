package sa.urtv.home;

import java.util.Arrays;

/** For tests/suggested_apps_test.py: SuggestedApps' rules. One line per check: name TAB ok|FAIL. */
public final class SuggestedAppsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] x) {
        SuggestedApps.App tivi = SuggestedApps.byId("tivimate"), tube = SuggestedApps.byId("smarttube"), vlc = SuggestedApps.byId("vlc");
        check("no Lumen and no pinned third-party file: GitHub releases, Play Store pages, and our own mirrored Player only", SuggestedApps.byId("lumen") == null
            && SuggestedApps.Kind.values().length == 3 && SuggestedApps.Kind.valueOf("MIRROR") != null && java.util.Arrays.stream(SuggestedApps.ALL).noneMatch(e -> e.kind == SuggestedApps.Kind.MIRROR));
        check("the IPTV player is TiviMate's Play Store page only: no file, unticked, not in the setup", tivi.kind == SuggestedApps.Kind.STORE && tivi.file == null && tivi.repo == null
            && !tivi.inSetup() && !tivi.setupDefault && tivi.address().equals("https://play.google.com/store/apps/details?id=ar.tvplayer.tv"));
        check("a GitHub app only from its own repository's releases, an .apk, no way out", SuggestedApps.githubOk(tube, "https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk")
            && !SuggestedApps.githubOk(tube, "https://github.com/evil/SmartTube/releases/download/29.1/a.apk") && !SuggestedApps.githubOk(tube, "https://github.com/yuliskov/SmartTube/releases/download/../x.apk")
            && !SuggestedApps.githubOk(tube, "http://github.com/yuliskov/SmartTube/releases/download/29.1/a.apk") && !SuggestedApps.githubOk(tivi, "https://github.com/x/y/releases/download/1/a.apk"));
        check("a Play Store app has no file: its page only, and is not in the setup", vlc.kind == SuggestedApps.Kind.STORE && vlc.file == null && !vlc.inSetup()
            && vlc.address().equals("https://play.google.com/store/apps/details?id=org.videolan.vlc"));
        check("every suggestion runs on any TV we support (Android 5+)", SuggestedApps.forTv(21).size() == SuggestedApps.ALL.length);
        check("the setup's first ticks are TV Bro and SmartTube, as the one box was", SuggestedApps.defaults(34).equals(Arrays.asList("tvbro", "smarttube")));
        check("the setup's boxes: the two it can install, TV Bro then SmartTube (no store app)", SuggestedApps.forSetup(34).size() == 2 && SuggestedApps.forSetup(34).get(1) == tube);
        check("saved ticks read back: known setup ids, once, in order; a store app or a stranger dropped", SuggestedApps.parse("smarttube,vlc,x,tivimate,tvbro,smarttube").equals(Arrays.asList("tvbro", "smarttube"))
            && SuggestedApps.parse("").isEmpty() && SuggestedApps.parse(null).isEmpty());
        int iptv = 0;
        for (SuggestedApps.App a : SuggestedApps.ALL) if (a.why.contains("IPTV")) iptv++;
        check("at most two IPTV players", iptv >= 1 && iptv <= SuggestedApps.MAX_IPTV && SuggestedApps.MAX_IPTV == 2);
        check("pickAsset: the TV's processor first", "https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk".equals(SuggestedApps.pickAsset(tube.file, "arm64-v8a,armeabi-v7a",
            "{\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_armeabi-v7a.apk\"}, {\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk\"}")));
        check("hex", SuggestedApps.hex(new byte[] {0, 15, (byte) 255}).equals("000fff"));

        // 4.3 UrTV Player: a MIRROR entry, from this repository's own «player-<version>» releases
        SuggestedApps.App pl = SuggestedApps.PLAYER_APP;
        check("UrTV Player: not offered and never installed while its certificate is not pinned (the placeholder)", SuggestedApps.PLAYER_CERT.isEmpty()
            && !SuggestedApps.mirrorReady(pl) && !SuggestedApps.forTv(34).contains(pl) && !pl.inSetup()
            && !SuggestedApps.mirrorFileOk(pl, "ab".repeat(32), "ab".repeat(32), "com.urtv.player", new java.util.HashSet<>(java.util.Collections.singleton("cd".repeat(32)))));
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
