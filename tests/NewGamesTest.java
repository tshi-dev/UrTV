package sa.urtv.screensaver;

import java.util.*;
import sa.urtv.screensaver.games.forehead.ForeheadGame;

/** For tests/new_games_test.py: the rules of the new games (4.4) on a plain JVM, fake clocks. Prints PASS/FAIL lines. */
public final class NewGamesTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static long[] t = {1_000_000L};

    static void forehead() {
        int[] n = {0};
        ForeheadGame g = new ForeheadGame(2, 2, 60, () -> "كلمة" + (n[0]++), () -> t[0]);
        check("forehead: a turn starts READY; the count of three, then the first word; no word before it", g.phase == ForeheadGame.Phase.READY && g.word.isEmpty() && !g.right());
        g.ready(); check("forehead: ready starts the count, the word is not there yet", g.phase == ForeheadGame.Phase.COUNT && g.word.isEmpty());
        t[0] += 2000; g.tick(); check("forehead: two seconds into the count is still the count", g.phase == ForeheadGame.Phase.COUNT);
        t[0] += 1100; g.tick(); check("forehead: after three seconds the turn runs and the first word is dealt", g.phase == ForeheadGame.Phase.PLAY && g.word.equals("كلمة0"));
        check("forehead: right scores for the team whose turn it is and deals the next word; skip scores nothing", g.right() && g.score[0] == 1 && g.word.equals("كلمة1") && g.skip() && g.score[0] == 1 && g.got == 1 && g.skipped == 1 && g.word.equals("كلمة2"));
        g.pause(); t[0] += 90_000; g.resume(); g.tick();
        check("forehead: the pause stops the clock (90 s of pause do not end a 60 s turn), actions are refused while paused", g.phase == ForeheadGame.Phase.PLAY && g.leftMs() > 50_000);
        g.pause(); check("forehead: no answer while paused", !g.right()); g.resume();
        t[0] += 61_000; g.tick();
        check("forehead: the clock ends the turn, the word is gone", g.phase == ForeheadGame.Phase.TURN_END && g.word.isEmpty() && !g.right());
        g.nextTurn(); check("forehead: the next team's turn", g.phase == ForeheadGame.Phase.READY && g.team() == 1 && g.round() == 0 && g.got == 0);
        for (int i = 0; i < 3; i++) { g.ready(); t[0] += 3100; g.tick(); g.right(); t[0] += 61_000; g.tick(); g.nextTurn(); }
        check("forehead: every team plays its turns (2 x 2 = 4), then the end; the leaders are the teams with most points", g.phase == ForeheadGame.Phase.END && g.turn == 4 && g.leaders().size() >= 1 && g.score[0] + g.score[1] == 4);
        boolean threw = false; try { new ForeheadGame(1, 1, 60, () -> "", () -> t[0]); } catch (IllegalArgumentException e) { threw = true; }
        check("forehead: one team is refused (the game needs two to four)", threw);
    }

    public static void main(String[] a) throws Exception {
        forehead();
        System.exit(failed == 0 ? 0 : 1);
    }
}
