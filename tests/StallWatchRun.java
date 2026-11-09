package sa.urtv.home.tilawah;

/** For tests/stall_watch_test.py: StallWatch's verdicts on made-up clocks. One line per check: name TAB ok|FAIL. */
public final class StallWatchRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        StallWatch w = new StallWatch();
        long t = 1_000_000;
        boolean ok = true;
        for (int i = 0; i < 60; i++) ok &= w.check(t + i * 1000, true, true, true, 0, i * 1000) == StallWatch.Verdict.OK;
        check("a station whose position moves is left alone for a minute", ok);

        w = new StallWatch();
        check("a rebuffer of 11 s is left alone", w.check(t + 11_000, true, true, true, t, 5000) == StallWatch.Verdict.OK);
        check("a rebuffer of 12 s is restarted quietly", w.check(t + 12_000, true, true, true, t, 5000) == StallWatch.Verdict.RESTART);

        w = new StallWatch();
        StallWatch.Verdict v = StallWatch.Verdict.OK;
        for (int i = 0; i <= 15 && v == StallWatch.Verdict.OK; i++) v = w.check(t + i * 1000, true, true, true, 0, 7000);
        check("a position that stands still for 15 s while it plays is restarted", v == StallWatch.Verdict.RESTART);

        w = new StallWatch();
        ok = true;
        for (int i = 0; i < 60; i++) ok &= w.check(t + i * 1000, true, true, false, 0, 7000) == StallWatch.Verdict.OK;
        check("paused (not playing): a still position is not a stall", ok);
        ok = true;
        for (int i = 0; i < 60; i++) ok &= w.check(t + i * 1000, false, true, true, t, 7000) == StallWatch.Verdict.OK;
        check("not a live station the person wants (a surah, a stop): never acted on", ok);
        ok = true;
        for (int i = 0; i < 60; i++) ok &= w.check(t + i * 1000, true, false, false, 0, 0) == StallWatch.Verdict.OK;
        check("still loading (not prepared): left to the 20 s start check", ok);

        w = new StallWatch();
        int restarts = 0;
        StallWatch.Verdict last = null;
        for (int k = 0; k < 4; k++) { last = w.check(t + k * 20_000 + 12_000, true, true, true, t + k * 20_000, 0); if (last == StallWatch.Verdict.RESTART) { restarts++; w.newPlayer(); } }
        check("three quiet restarts in two minutes, the fourth stall is a failure", restarts == 3 && last == StallWatch.Verdict.GIVE_UP && w.restarts() == 3);
        check("after two quiet minutes a stall is restarted again", w.check(t + 300_000, true, true, true, t + 288_000, 0) == StallWatch.Verdict.RESTART);
        w.reset();
        check("the person's own pick clears the count", w.restarts() == 0);

        check("HLS: SBA's .m3u8 (and with a query) goes to ExoPlayer", StallWatch.isHls("https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8")
            && StallWatch.isHls("https://x.example/live/index.M3U8?token=1"));
        check("not HLS: mp3quran's radio, radiojar, a surah, a path with m3u8 elsewhere, null", !StallWatch.isHls("https://backup.qurango.net/radio/abdulrahman_alsudaes")
            && !StallWatch.isHls("https://stream.radiojar.com/0tpy1h0kxtzuv") && !StallWatch.isHls("https://server8.mp3quran.net/afs/001.mp3")
            && !StallWatch.isHls("https://x.example/m3u8/stream.mp3?f=a.m3u8") && !StallWatch.isHls(null));
    }
}
