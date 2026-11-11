package sa.urtv.screensaver.games.oddone;

/** «واحد مو معنا»: which screen is up and what each key of the remote does on it (no Android: the activity draws screen() and feeds key(), select() and tick();
 *  tested on a plain JVM, tests/PartyGamesTest.java). Grids (the suspects) are focused by the view and answered with select(); every other screen needs OK only.
 *
 *  PASS → OK → COUNT (2 s, so the holder can lift the remote) → SECRET (OK or 5 s hides) → the next PASS … → TALK (OK = the next speaker) → the vote:
 *  by hands HANDS (select a player, or NOBODY for «ما اتفقنا»); secret VOTE_PASS → OK → VOTE_PICK (select) for every player, REVOTE_NOTE before a second go →
 *  ACCUSED (who the room chose) → OK → ODD (who it really was) → OK → caught: GUESS_ASK → OK opens the word → GUESS_JUDGE (LEFT «خطأ», RIGHT «صح», OK) →
 *  SCORE → OK → the next round's PASS, or END (UP/DOWN between «لعبة جديدة» and «الرئيسية», OK).
 *  Back: on SECRET or COUNT it only hides (the same player's PASS again); anywhere else it opens the pause card (متابعة، إنهاء الجولة، الرئيسية; Back = متابعة);
 *  on END it leaves. The app going to the background hides a secret and opens the pause card. */
public final class OddOneFlow {
    public enum Key { OK, BACK, LEFT, RIGHT, UP, DOWN }
    public enum Screen { PASS, COUNT, SECRET, TALK, HANDS, VOTE_PASS, VOTE_PICK, REVOTE_NOTE, ACCUSED, ODD, GUESS_ASK, GUESS_JUDGE, SCORE, END }
    public static final int CONTINUE = 0, END_ROUND = 1, HOME = 2, AGAIN = 0, LEAVE = 1;
    public static final long COUNT_MS = 2000;

    public final OddOneGame game; private final OddOneGame.Clock clock;
    private boolean counting, picking, oddShown, revoteSeen, beeped; private long countAt;
    /** PASS / TALK: 1 = on «رجّعها» (only while the game allows it); GUESS_JUDGE: 0 «صح», 1 «خطأ»; END: AGAIN / LEAVE */
    public int focus;
    public boolean pauseOpen; public int pauseFocus;
    /** set by the flow, read (and acted on) by the activity */
    public boolean wantsExit, wantsAgain;

    public OddOneFlow(OddOneGame game, OddOneGame.Clock clock) { this.game = game; this.clock = clock; }

    public Screen screen() {
        switch (game.phase) {
            case DEAL: return game.secretShown() ? Screen.SECRET : counting ? Screen.COUNT : Screen.PASS;
            case TALK: return Screen.TALK;
            case VOTE: return !game.secretVote ? Screen.HANDS : game.revote && !revoteSeen ? Screen.REVOTE_NOTE : picking ? Screen.VOTE_PICK : Screen.VOTE_PASS;
            case REVEAL: return oddShown ? Screen.ODD : Screen.ACCUSED;
            case GUESS: return game.wordShown ? Screen.GUESS_JUDGE : Screen.GUESS_ASK;
            case SCORE: return Screen.SCORE;
            default: return Screen.END;
        }
    }
    /** the seconds left on the count before a secret (COUNT) */
    public long countLeftMs() { return counting ? Math.max(0, COUNT_MS - (clock.now() - countAt)) : 0; }
    /** true once per speaker, when his 30 s are over: the soft beep */
    public boolean beepDue() {
        if (game.phase != OddOneGame.Phase.TALK || pauseOpen || beeped || game.talkLeftMs() > 0) return false;
        beeped = true; return true;
    }

    public void key(Key k) {
        if (pauseOpen) { pauseKey(k); return; }
        Screen s = screen();
        if (k == Key.BACK) {
            if (s == Screen.END) wantsExit = true;
            else if (s == Screen.SECRET || s == Screen.COUNT) { counting = false; game.cover(); }
            else openPause();
            return;
        }
        switch (s) {
            case PASS: case TALK:
                if (k == Key.DOWN && game.canDealBack()) focus = 1;
                else if (k == Key.UP) focus = 0;
                else if (k == Key.OK) {
                    if (focus == 1 && game.canDealBack()) game.dealBack();
                    else if (s == Screen.PASS) { counting = true; countAt = clock.now(); }
                    else { game.nextSpeaker(); beeped = false; }
                    focus = 0;
                }
                break;
            case SECRET: if (k == Key.OK) game.hide(); break;
            case VOTE_PASS: if (k == Key.OK) picking = true; break;
            case REVOTE_NOTE: if (k == Key.OK) revoteSeen = true; break;
            case ACCUSED: if (k == Key.OK) oddShown = true; break;
            case ODD: if (k == Key.OK) { game.afterReveal(); focus = 0; } break;
            case GUESS_ASK: if (k == Key.OK) { game.showWord(); focus = 0; } break;
            case GUESS_JUDGE:
                if (k == Key.LEFT) focus = 1; else if (k == Key.RIGHT) focus = 0;
                else if (k == Key.OK) { game.guess(focus == 0); focus = 0; }
                break;
            case SCORE: if (k == Key.OK) { game.nextRound(); newRound(); } break;
            case END:
                if (k == Key.DOWN) focus = LEAVE; else if (k == Key.UP) focus = AGAIN;
                else if (k == Key.OK) { if (focus == AGAIN) wantsAgain = true; else wantsExit = true; }
                break;
            default: break;
        }
    }
    /** a cell of the suspects' grid: HANDS (a player, or OddOneGame.NOBODY) or VOTE_PICK (one of game.candidates()) */
    public void select(int player) {
        if (pauseOpen) return;
        Screen s = screen();
        if (s == Screen.HANDS) game.accuse(player);
        else if (s == Screen.VOTE_PICK && game.vote(player)) picking = false;
    }
    private void newRound() { counting = picking = oddShown = revoteSeen = beeped = false; focus = 0; }

    private void openPause() { counting = false; game.pause(); pauseOpen = true; pauseFocus = CONTINUE; }
    private void pauseKey(Key k) {
        if (k == Key.DOWN) pauseFocus = Math.min(HOME, pauseFocus + 1);
        else if (k == Key.UP) pauseFocus = Math.max(CONTINUE, pauseFocus - 1);
        else if (k == Key.BACK || (k == Key.OK && pauseFocus == CONTINUE)) { pauseOpen = false; game.resume(); }
        else if (k == Key.OK && pauseFocus == END_ROUND) { pauseOpen = false; game.resume(); game.dropRound(); newRound(); }
        else if (k == Key.OK) wantsExit = true;
    }
    /** onPause of the activity (HOME, sleep, another app): a secret never stays up; the pause card waits on return */
    public void background() { if (game.phase != OddOneGame.Phase.END && !pauseOpen) { picking = false; openPause(); } }

    /** a few times a second. True when the screen must be drawn again. */
    public boolean tick() {
        if (pauseOpen) return false;
        if (counting && clock.now() - countAt >= COUNT_MS) { counting = false; game.show(); return true; }
        return game.tick();
    }
}
