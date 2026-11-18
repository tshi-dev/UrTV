package sa.urtv.screensaver.games.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * «اربط الكلمات»: the board and the rules (no Android, tests/NewGamesTest.java). 25 words; a secret key says which belong to team 1 (9 for the team that starts,
 * else 8), team 2 (8 or 9), none (7) and which one ends the game (1). Each team's captain sees the key and gives a one-word clue and a number aloud; the team
 * touches words: its own word goes on (up to the number plus one), a neutral word ends the turn, the other team's word gives it the point and ends the turn, the
 * ending word loses the game for the team that touched it. The first team to uncover all its words wins. The file: {"v":1,"words":[["ar","en"],...]}.
 */
public final class LinkGame {
    public static final int CELLS = 25, WORDS_MIN = 120;
    public static final int NONE = 0, TEAM1 = 1, TEAM2 = 2, END = 3;

    public static List<String[]> parse(Object tree) {
        if (!(tree instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) tree;
        if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1 || !(m.get("words") instanceof List)) return null;
        List<String[]> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (Object o : (List<?>) m.get("words")) {
            if (!(o instanceof List) || ((List<?>) o).size() < 2 || !(((List<?>) o).get(0) instanceof String) || !(((List<?>) o).get(1) instanceof String)) continue;
            String ar = ((String) ((List<?>) o).get(0)).trim(), en = ((String) ((List<?>) o).get(1)).trim();
            if (ar.isEmpty() || en.isEmpty() || ar.length() > 30 || en.length() > 40 || (ar + en).indexOf('·') >= 0 || !seen.add(ar)) continue;
            out.add(new String[] {ar, en});
        }
        return out.size() >= WORDS_MIN ? out : null;
    }

    /** 25 words not among the recent ones when there are enough others */
    public static List<String[]> pick(List<String[]> all, Set<String> recent, Random rnd) {
        List<String[]> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (String[] w : all) (recent.contains(w[0]) ? old : fresh).add(w);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd);
        fresh.addAll(old);
        return new ArrayList<>(fresh.subList(0, CELLS));
    }

    public enum Phase { CLUE, GUESS, END }
    public final List<String[]> words; public final int[] key = new int[CELLS]; public final boolean[] shown = new boolean[CELLS];
    public final int starter; public int turn; public Phase phase = Phase.CLUE; public int guessesLeft, winner, clueNumber; public boolean lostByEnd;

    public LinkGame(List<String[]> words, Random rnd) {
        if (words.size() != CELLS) throw new IllegalArgumentException("words");
        this.words = words; starter = rnd.nextBoolean() ? TEAM1 : TEAM2; turn = starter;
        List<Integer> cells = new ArrayList<>(); for (int i = 0; i < CELLS; i++) cells.add(i);
        Collections.shuffle(cells, rnd);
        int k = 0;
        for (int i = 0; i < 9; i++) key[cells.get(k++)] = starter;
        for (int i = 0; i < 8; i++) key[cells.get(k++)] = starter == TEAM1 ? TEAM2 : TEAM1;
        key[cells.get(k++)] = END;
        // the other 7 stay NONE
    }
    public int remaining(int team) { int n = 0; for (int i = 0; i < CELLS; i++) if (key[i] == team && !shown[i]) n++; return n; }
    public int other(int team) { return team == TEAM1 ? TEAM2 : TEAM1; }

    /** the captain's clue number (1 to 9): the team may touch up to that many plus one */
    public boolean clue(int n) { if (phase != Phase.CLUE || n < 1 || n > 9) return false; clueNumber = n; guessesLeft = n + 1; phase = Phase.GUESS; return true; }

    /** the team touches a word; true when it counted */
    public boolean guess(int cell) {
        if (phase != Phase.GUESS || cell < 0 || cell >= CELLS || shown[cell]) return false;
        shown[cell] = true; int k = key[cell];
        if (k == END) { lostByEnd = true; winner = other(turn); phase = Phase.END; return true; }
        if (remaining(TEAM1) == 0) { winner = TEAM1; phase = Phase.END; return true; }
        if (remaining(TEAM2) == 0) { winner = TEAM2; phase = Phase.END; return true; }
        if (k != turn) { pass(); return true; }
        if (--guessesLeft <= 0) pass();
        return true;
    }
    /** the team stops (or its turn ends): the other team's clue */
    public void pass() { if (phase == Phase.END) return; turn = other(turn); phase = Phase.CLUE; guessesLeft = 0; clueNumber = 0; }
    public void finish() { if (phase != Phase.END) { winner = 0; phase = Phase.END; } }

    /** the key as the captains' phones get it: one character a cell, '1' team 1, '2' team 2, 'n' none, 'x' the ending word */
    public String keyString() { StringBuilder b = new StringBuilder(); for (int k : key) b.append(k == TEAM1 ? '1' : k == TEAM2 ? '2' : k == END ? 'x' : 'n'); return b.toString(); }
}
