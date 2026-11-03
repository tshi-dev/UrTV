package sa.urtv.home;

/**
 * «التلفزيون المكتوم يبقى مكتومًا» (step 27): the TV's own audio service sets the mute off when the screen comes on (after its own sleep, after a restart; three
 * times in one day). The owner's rule: a muted TV stays muted. The home remembers «muted» when the screen goes off (and at shutdown) and, if it was, mutes
 * again after the wake, inside WINDOW_MS, never over the adhan, never after the owner moved the volume, and at most once per wake. No Android in here:
 * tests/MuteKeepRun.java runs it.
 */
final class MuteKeepRule {
    private MuteKeepRule() { }

    /** after the wake (the screen on, or the boot) the TV may still unmute: checked at these delays, the last inside WINDOW_MS */
    static final long[] CHECKS_MS = {1_500, 4_000, 9_000, 16_000, 28_000};
    static final long WINDOW_MS = 30_000;

    static final String MUTE = "mute", STILL_MUTED = "still_muted";

    /**
     * What to do at a check: MUTE, STILL_MUTED (nothing to do yet, check again), or a reason to stop (not_muted_before, already_done, window_over, adhan,
     * volume_changed, no_record).
     * @param rememberedMuted the mute when the screen went off; hasRecord false when nothing was remembered
     * @param wakeAt when the wake began (ms, the same clock as now)
     */
    static String decide(boolean hasRecord, boolean rememberedMuted, int rememberedVol, boolean doneThisWake, long wakeAt, long now, boolean mutedNow, int volNow, boolean adhan) {
        if (!hasRecord) return "no_record";
        if (!rememberedMuted) return "not_muted_before";
        if (doneThisWake) return "already_done";
        if (now < wakeAt || now - wakeAt > WINDOW_MS) return "window_over";
        if (adhan) return "adhan";
        if (rememberedVol >= 0 && volNow >= 0 && volNow != rememberedVol) return "volume_changed";
        return mutedNow ? STILL_MUTED : MUTE;
    }
}
