package sa.urtv.home;

import java.util.Arrays;

/** For tests/suggested_apps_test.py: SuggestedApps' rules. One line per check: name TAB ok|FAIL. */
public final class SuggestedAppsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] x) {
        SuggestedApps.App lumen = SuggestedApps.byId("lumen"), tube = SuggestedApps.byId("smarttube"), vlc = SuggestedApps.byId("vlc");
        String pinned = "https://github.com/lumen-tv/lumen-tv/releases/download/v0.9.4/lumen.apk";
        check("the IPTV player is one pinned file of its own GitHub release", lumen.kind == SuggestedApps.Kind.PINNED && pinned.equals(lumen.address()) && lumen.sha256.matches("[0-9a-f]{64}"));
        check("its file is taken only with the pinned SHA-256", SuggestedApps.pinnedOk(lumen, pinned, lumen.sha256) && !SuggestedApps.pinnedOk(lumen, pinned, "00".repeat(32))
            && !SuggestedApps.pinnedOk(lumen, pinned, null) && !SuggestedApps.pinnedOk(lumen, pinned + "?x", lumen.sha256)
            && !SuggestedApps.pinnedOk(lumen, "https://github.com/lumen-tv/lumen-tv/releases/download/v0.9.5/lumen.apk", lumen.sha256));
        check("a GitHub app only from its own repository's releases, an .apk, no way out", SuggestedApps.githubOk(tube, "https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk")
            && !SuggestedApps.githubOk(tube, "https://github.com/evil/SmartTube/releases/download/29.1/a.apk") && !SuggestedApps.githubOk(tube, "https://github.com/yuliskov/SmartTube/releases/download/../x.apk")
            && !SuggestedApps.githubOk(tube, "http://github.com/yuliskov/SmartTube/releases/download/29.1/a.apk") && !SuggestedApps.githubOk(lumen, pinned));
        check("a Play Store app has no file: its page only, and is not in the setup", vlc.kind == SuggestedApps.Kind.STORE && vlc.file == null && !vlc.inSetup()
            && vlc.address().equals("https://play.google.com/store/apps/details?id=org.videolan.vlc"));
        check("Android 9 (API 28) does not get the IPTV player (Android 10+), API 29 does", !SuggestedApps.forTv(28).contains(lumen) && SuggestedApps.forTv(29).contains(lumen));
        check("the setup's first ticks are TV Bro and SmartTube, as the one box was", SuggestedApps.defaults(34).equals(Arrays.asList("tvbro", "smarttube")));
        check("the setup's boxes: the three it can install, in order", SuggestedApps.forSetup(34).size() == 3 && SuggestedApps.forSetup(34).get(2) == lumen);
        check("saved ticks read back: known setup ids, once, in order; a store app or a stranger dropped", SuggestedApps.parse("lumen,vlc,x,tvbro,lumen").equals(Arrays.asList("tvbro", "lumen"))
            && SuggestedApps.parse("").isEmpty() && SuggestedApps.parse(null).isEmpty());
        int iptv = 0;
        for (SuggestedApps.App a : SuggestedApps.ALL) if (a.why.contains("IPTV")) iptv++;
        check("at most two IPTV players", iptv >= 1 && iptv <= SuggestedApps.MAX_IPTV && SuggestedApps.MAX_IPTV == 2);
        check("pickAsset: the TV's processor first", "https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk".equals(SuggestedApps.pickAsset(tube.file, "arm64-v8a,armeabi-v7a",
            "{\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_armeabi-v7a.apk\"}, {\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk\"}")));
        check("hex", SuggestedApps.hex(new byte[] {0, 15, (byte) 255}).equals("000fff"));
    }
}
