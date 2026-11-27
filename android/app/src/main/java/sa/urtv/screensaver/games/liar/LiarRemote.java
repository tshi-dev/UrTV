package sa.urtv.screensaver.games.liar;

/**
 * «واحد يكذب» with one remote (docs/GAMES-REMOTE.md), no Android: the three tellers get their role through the hand-over, and the vote is a show of hands: the room points on the count
 * and the host picks the teller the room chose, or «ما اتفقنا». The room is scored as ONE voice: every player who did not tell votes the same, so LiarGame's own scoring does the rest
 * (a found liar: FOUND to each voter; not found: the liar gets 1 for each voter fooled). With phones LiarGame is used as it was.
 */
public final class LiarRemote {
    private LiarRemote() {}
    /** the fewest players (the phone game takes 4: then one voter decides the round); one place to change it */
    public static final int MIN_PLAYERS = 5;
    /** «ما اتفقنا»: counts as «not found» (the voters were fooled) when true; when false nobody scores for that round */
    public static final boolean NOBODY_FOOLS = true;

    /** the room's verdict: `accused` is a teller (player number), or -1 for «ما اتفقنا». Call in the VOTE phase; then game.reveal(). */
    public static void applyRoom(LiarGame g, int accused) {
        int pick = accused;
        if (pick < 0 && NOBODY_FOOLS) { for (int t : g.tellers()) if (!g.liarIs(t)) { pick = t; break; } }
        if (pick < 0) return;
        for (int p = 0; p < g.players; p++) if (!g.isTeller(p)) g.vote(p, pick);
    }
}
