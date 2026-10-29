package sa.urtv.home.tilawah;

/** What to do with the TV's volume when an adhan ends, with no Android in it (tests/AdhanVoicesRun.java runs it). */
final class AdhanVolume {
    private AdhanVolume() { }

    /**
     * The level to put the TV's volume back to, or -1 to leave it alone. The volume is put back to what it was (saved) unless the person
     * changed it during the adhan (current is not the level the adhan set): then it stays where they put it.
     * saved -1: nothing was saved. level -1: the level the adhan set is not known (a restart of the player): put it back.
     */
    static int restore(int saved, int level, int current) {
        if (saved < 0) return -1;
        if (level >= 0 && current != level) return -1;
        return saved;
    }

    /**
     * The TV was muted when the adhan came (the owner's TCL test: muted before, unmuted after): the adhan is heard, and the TV is muted again
     * when its volume goes back. Not when the person changed the volume during the adhan (restoreTo -1): then they chose to hear it.
     */
    static boolean muteAgain(boolean wasMuted, int restoreTo) {
        return wasMuted && restoreTo >= 0;
    }
}
