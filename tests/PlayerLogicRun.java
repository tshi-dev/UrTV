package sa.urtv.home.tilawah;

/** For tests/player_logic_test.py: PlayIntent, AdhanVoices, AdhanVolume and PlayerFault. Prints one line per check: name TAB ok (or the isFajr list first). */
public final class PlayerLogicRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static boolean has(int[] a, int v) { for (int x : a) if (x == v) return true; return false; }

    public static void main(String[] args) {
        StringBuilder fajr = new StringBuilder();
        for (int v = 1; v <= AdhanVoices.COUNT; v++) if (AdhanVoices.isFajr(v)) fajr.append(v).append(',');
        System.out.println("FAJR\t" + fajr);

        // ---- F21: Pause before the media is ready is not undone by it
        PlayIntent w = new PlayIntent();
        w.ask();
        int g = w.newPlayer();
        w.pause();   // Pause while it loads
        check("pause while loading: it does not start when prepared", !w.startOnPrepared(g) && !w.wants());
        w.ask();   // an explicit Resume
        check("resume after that: it starts, once (the one prepared callback)", w.startOnPrepared(g));

        w = new PlayIntent();
        w.ask();
        g = w.newPlayer();
        w.stop();
        check("stop while loading: a late prepared does not start released media", !w.startOnPrepared(g) && !w.current(g));

        w = new PlayIntent();
        w.ask();
        int first = w.newPlayer();
        w.ask();
        int second = w.newPlayer();   // another surah picked while the first loads
        check("another pick while loading: the first one's callbacks are stale, the second's are not",
                !w.startOnPrepared(first) && w.startOnPrepared(second));

        w = new PlayIntent();
        w.ask();
        g = w.newPlayer();   // an error, a retry timer is set for this generation
        int timer = w.currentGeneration();
        w.pause();
        check("pause during the retry wait: the retry still re-prepares, but the prepared callback will not start it",
                w.retry(timer, false) && !w.startOnPrepared(g));
        w.stop();
        check("stop during the retry wait: the retry does nothing", !w.retry(timer, false));
        w = new PlayIntent();
        w.ask();
        g = w.newPlayer();
        w.ask();
        int next = w.newPlayer();
        check("a retry of the replaced media does nothing", !w.retry(g, false) && w.retry(next, false));
        check("a retry never restarts what is playing", !w.retry(next, true));

        w = new PlayIntent();
        w.ask();
        g = w.newPlayer();
        w.pause();
        w.pause();
        check("pause twice is still paused", !w.wants() && !w.startOnPrepared(g));
        w.ask();
        w.ask();
        check("resume twice starts once: one generation, one start", w.startOnPrepared(g) && w.current(g));
        w.pause();
        w.ask();
        check("toggle pause then resume while loading ends wanting to play", w.startOnPrepared(g));

        // ---- Fajr voices
        check("four Fajr recordings", AdhanVoices.fajrList().length == AdhanVoices.COUNT && AdhanVoices.normal().length == AdhanVoices.COUNT - 4);
        boolean noFajrInNormal = true;
        for (int v : AdhanVoices.normal()) if (AdhanVoices.isFajr(v)) noFajrInNormal = false;
        check("the list for the other prayers has no Fajr recording", noFajrInNormal);
        int[] list = AdhanVoices.fajrList();
        boolean firstFour = true, all = true;
        for (int i = 0; i < 4; i++) if (!AdhanVoices.isFajr(list[i])) firstFour = false;
        for (int v = 1; v <= AdhanVoices.COUNT; v++) if (!has(list, v)) all = false;
        check("the Fajr list: the four Fajr ones first, then every normal one, each once", firstFour && all && list.length == AdhanVoices.COUNT);
        check("the default Fajr voice is a Fajr recording («فجر الحرم المكي»), the default for the rest is not",
                AdhanVoices.isFajr(AdhanVoices.DEFAULT_FAJR) && !AdhanVoices.isFajr(AdhanVoices.DEFAULT) && AdhanVoices.DEFAULT_FAJR == 10);
        check("migration: a TV whose voice was a Fajr recording keeps it for Fajr and gets voice 1 for the rest",
                AdhanVoices.fajrVoice(-1, 13) == 13 && AdhanVoices.normalVoice(13) == 1 && AdhanVoices.fajrVoice(-1, 20) == 20 && AdhanVoices.normalVoice(20) == 1);
        check("migration: a TV with a normal voice keeps it and gets the default Fajr voice",
                AdhanVoices.fajrVoice(-1, 5) == 10 && AdhanVoices.normalVoice(5) == 5 && AdhanVoices.fajrVoice(-1, 1) == 10);
        check("a Fajr voice that was chosen wins over the old single voice", AdhanVoices.fajrVoice(17, 5) == 17 && AdhanVoices.fajrVoice(3, 13) == 3);
        check("nonsense saved: the defaults", AdhanVoices.normalVoice(0) == 1 && AdhanVoices.normalVoice(99) == 1 && AdhanVoices.fajrVoice(99, 99) == 10);

        // ---- the short adhan
        check("short adhan: full volume for 57 seconds", AdhanVoices.shortGain(0) == 1f && AdhanVoices.shortGain(57_000) == 1f);
        check("short adhan: a 3-second fade down to nothing at 60 seconds",
                Math.abs(AdhanVoices.shortGain(58_500) - 0.5f) < 0.001f && AdhanVoices.shortGain(60_000) == 0f && AdhanVoices.shortGain(70_000) == 0f);
        float last = 1f;
        boolean falling = true;
        for (long t = 57_000; t <= 60_000; t += 250) { float x = AdhanVoices.shortGain(t); if (x > last) falling = false; last = x; }
        check("short adhan: the fade only goes down", falling);

        // ---- the TV's volume after an adhan
        check("volume: back to what it was when nobody touched it", AdhanVolume.restore(12, 6, 6) == 12);
        check("volume: left alone when the person changed it during the adhan", AdhanVolume.restore(12, 6, 9) == -1);
        check("volume: nothing saved, nothing to restore", AdhanVolume.restore(-1, 6, 6) == -1);
        check("volume: the adhan's level unknown (the player restarted): put back", AdhanVolume.restore(12, -1, 6) == 12);
        check("volume: a TV muted before the adhan is muted again when its volume goes back", AdhanVolume.muteAgain(true, 12) && !AdhanVolume.muteAgain(false, 12));
        check("volume: not muted again when the person turned the volume during the adhan", !AdhanVolume.muteAgain(true, -1));

        // ---- what a failure says
        check("fault codes", PlayerFault.code(1, -1004).equals("io") && PlayerFault.code(1, -110).equals("timeout")
                && PlayerFault.code(1, -1007).equals("malformed") && PlayerFault.code(1, -1010).equals("unsupported")
                && PlayerFault.code(100, 0).equals("server_died") && PlayerFault.code(1, 0).equals("unknown"));
        check("fault sentences say what failed, for a surah and for a station",
                PlayerFault.message(false, 1, -1004).contains("تحميل السورة") && PlayerFault.message(true, 1, -1004).contains("البث")
                && PlayerFault.message(false, 1, -1007).contains("السورة") && PlayerFault.message(true, 1, 0).contains("البث"));

        // ---- a held seek key: one seek per 250 ms, a fresh press always
        SeekPace sp = new SeekPace();
        int seeks = 0;
        for (int k = 0; k < 20; k++) if (sp.due(k, 1_000 + k * 50L)) seeks++;   // held for a second, a repeat every 50 ms
        check("seek: a held key seeks once per 250 ms, not on every repeat", seeks == 4);
        check("seek: a fresh press right after always seeks", sp.due(0, 1_960) && sp.due(0, 1_970));
    }
}
