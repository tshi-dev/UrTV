package sa.urtv.home.tilawah;

/** 4.4: does a dua whose alarm came play now? No Android in it (tests/DuaRuleRun.java runs it). */
final class DuaPlan {
    private DuaPlan() { }

    /** an alarm later than this is a missed one: never made up */
    static final long LATE_MS = 3 * 60_000L;

    enum Go { PLAY, SKIP_LATE, SKIP_STANDBY, SKIP_ADHAN, SKIP_OTHER_APP, SKIP_NO_SOUND }

    static Go decide(long due, long now, boolean screenOn, boolean adhanPlaying, boolean otherAppSounds, boolean overOthers, boolean hasSound) {
        if (now - due > LATE_MS) return Go.SKIP_LATE;
        if (!screenOn) return Go.SKIP_STANDBY;
        if (!hasSound) return Go.SKIP_NO_SOUND;
        if (adhanPlaying) return Go.SKIP_ADHAN;
        if (otherAppSounds && !overOthers) return Go.SKIP_OTHER_APP;
        return Go.PLAY;
    }
}
