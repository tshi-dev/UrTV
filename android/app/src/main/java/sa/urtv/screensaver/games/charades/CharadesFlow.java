package sa.urtv.screensaver.games.charades;

/** «بدون كلام / وصّف»: which screen is up and what each key of the remote does on it (no Android: the activity draws screen() and feeds key() and tick();
 *  tested on a plain JVM, tests/PartyGamesTest.java).
 *
 *  PASS (the team's actor takes the remote, the guessers cover their eyes) → OK → WORD (the word, big) → OK hides it → PLAY (the clock; the screen is safe to look at).
 *  PLAY: OK «صح» (+1, the next WORD at once), LEFT «تخطّي», UP «شوف الكلمة» (WORD again for 3 s), DOWN «مخالفة» (only in «وصّف» with the setting on).
 *  WORD after the clock started: OK hides it, LEFT skips it. The clock at 0 → TURN_END (the bell once, the open word shown) → OK → the next PASS, or END
 *  (UP/DOWN between «لعبة جديدة» and «الرئيسية», OK). Back opens the pause card (متابعة، إنهاء الجولة، الرئيسية; Back = متابعة): the word hides first and
 *  the clock stops; on END it leaves. The app going to the background does the same. */
public final class CharadesFlow {
    public enum Key { OK, BACK, LEFT, RIGHT, UP, DOWN }
    public enum Screen { PASS, WORD, PLAY, TURN_END, END }
    public static final int CONTINUE = 0, END_ROUND = 1, HOME = 2, AGAIN = 0, LEAVE = 1;

    public final CharadesGame game;
    private boolean belled;
    /** END: AGAIN / LEAVE */
    public int focus;
    public boolean pauseOpen; public int pauseFocus;
    /** set by the flow, read (and acted on) by the activity */
    public boolean wantsExit, wantsAgain;

    public CharadesFlow(CharadesGame game) { this.game = game; }

    public Screen screen() {
        switch (game.phase) {
            case PASS: return Screen.PASS;
            case PLAY: return game.wordVisible() ? Screen.WORD : Screen.PLAY;
            case TURN_END: return Screen.TURN_END;
            default: return Screen.END;
        }
    }
    /** true once per turn that the clock ended: the bell */
    public boolean bellDue() {
        if (game.phase != CharadesGame.Phase.TURN_END || !game.timeUp || belled) return false;
        belled = true; return true;
    }

    public void key(Key k) {
        if (pauseOpen) { pauseKey(k); return; }
        Screen s = screen();
        if (k == Key.BACK) { if (s == Screen.END) wantsExit = true; else openPause(); return; }
        switch (s) {
            case PASS: if (k == Key.OK) game.ready(); break;
            case WORD:
                if (k == Key.OK) game.hide(); else if (k == Key.LEFT) game.skip();
                break;
            case PLAY:
                if (k == Key.OK) game.correct(); else if (k == Key.LEFT) game.skip(); else if (k == Key.UP) game.peek(); else if (k == Key.DOWN) game.foul();
                break;
            case TURN_END: if (k == Key.OK) { game.nextTurn(); belled = false; } break;
            case END:
                if (k == Key.DOWN) focus = LEAVE; else if (k == Key.UP) focus = AGAIN;
                else if (k == Key.OK) { if (focus == AGAIN) wantsAgain = true; else wantsExit = true; }
                break;
            default: break;
        }
    }

    private void openPause() { game.pause(); pauseOpen = true; pauseFocus = CONTINUE; }
    private void pauseKey(Key k) {
        if (k == Key.DOWN) pauseFocus = Math.min(HOME, pauseFocus + 1);
        else if (k == Key.UP) pauseFocus = Math.max(CONTINUE, pauseFocus - 1);
        else if (k == Key.BACK || (k == Key.OK && pauseFocus == CONTINUE)) { pauseOpen = false; game.resume(); }
        else if (k == Key.OK && pauseFocus == END_ROUND) { pauseOpen = false; game.resume(); game.dropTurn(); }
        else if (k == Key.OK) wantsExit = true;
    }
    /** onPause of the activity (HOME, sleep, another app) */
    public void background() { if (game.phase != CharadesGame.Phase.END && !pauseOpen) openPause(); }

    /** a few times a second. True when the screen must be drawn again. */
    public boolean tick() { return !pauseOpen && game.tick(); }
}
