package sa.urtv.home.tilawah;

/** 4.4: does a dua whose alarm came play now? No Android in it (tests/DuaRuleRun.java runs it). */
final class DuaPlan {
    /** 4.4: an alarm and a dua's moment within this are the same (the prayer times may move by a minute or two after the alarm was set) */
    static final long SAME_MS = 3 * 60_000L;

    static boolean same(long t, long due) { return t > 0 && due > 0 && Math.abs(t - due) <= SAME_MS; }

    /**
     * 4.4 review F11: which dua does this alarm belong to? moments[i] is dua i's own next moment found from SAME_MS before the alarm (0: none or off),
     * lastDue[i] the moment it last played. The window only finds the moment; the dua that plays is the one whose moment is nearest the alarm
     * (the first in the list on a tie), and only that one: two duas 2 minutes apart each play once. -1: none.
     */
    static int pick(long[] moments, long[] lastDue, long due) {
        int best = -1;
        for (int i = 0; i < moments.length; i++) {
            long t = moments[i];
            if (lastDue[i] >= t || !same(t, due)) continue;
            if (best < 0 || Math.abs(t - due) < Math.abs(moments[best] - due)) best = i;
        }
        return best;
    }

    private DuaPlan() { }

    /** an alarm later than this is a missed one: never made up */
    static final long LATE_MS = 3 * 60_000L;

    enum Go { PLAY, SKIP_LATE, SKIP_STANDBY, SKIP_ADHAN, SKIP_OTHER_APP, SKIP_NO_SOUND, SKIP_QUIET }

    /** 4.4 «ساعات الهدوء»: nothing plays inside them; otherwise as decide() */
    static Go decide(long due, long now, boolean screenOn, boolean adhanPlaying, boolean otherAppSounds, boolean overOthers, boolean hasSound, boolean quiet) {
        Go g = decide(due, now, screenOn, adhanPlaying, otherAppSounds, overOthers, hasSound);
        return g == Go.PLAY && quiet ? Go.SKIP_QUIET : g;
    }

    static Go decide(long due, long now, boolean screenOn, boolean adhanPlaying, boolean otherAppSounds, boolean overOthers, boolean hasSound) {
        if (now - due > LATE_MS) return Go.SKIP_LATE;
        if (!screenOn) return Go.SKIP_STANDBY;
        if (!hasSound) return Go.SKIP_NO_SOUND;
        if (adhanPlaying) return Go.SKIP_ADHAN;
        if (otherAppSounds && !overOthers) return Go.SKIP_OTHER_APP;
        return Go.PLAY;
    }
}
