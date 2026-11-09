package sa.urtv.home;

import java.util.Arrays;

/** For tests/suggested_apps_test.py: SuggestedApps' rules. One line per check: name TAB ok|FAIL. */
public final class SuggestedAppsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] x) {
        SuggestedApps.App tivi = SuggestedApps.byId("tivimate"), tube = SuggestedApps.byId("smarttube"), vlc = SuggestedApps.byId("vlc");
        check("no pinned file and no Lumen: only GitHub releases and Play Store pages", SuggestedApps.byId("lumen") == null && SuggestedApps.Kind.values().length == 2);
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
    }
}
