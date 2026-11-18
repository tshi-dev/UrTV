package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The rules of every game the engine plays (docs/GAME-ENGINE.md; no Android, tests/GameEngineTest.java): rounds of the building blocks of GameDef, answers from phones, timing, scoring.
 * Randomness comes in as a `Random` and time as a number of milliseconds, so a test is exact. Solo (each phone a side) or in teams (phones take the teams in turn). A round is ASK (answers
 * come in), then REVEAL (scored), then the next round or END.
 *
 * Scoring («score» of the definition): «first» the first right answer scores `points`, the next one less, down to 1; «all» every right answer scores `points`; «speed» a right answer scores
 * more the earlier it came (1 to `points`); «closest» (a «closest» block) the nearest number scores `points`, the next one less, an exact number 1 more; «bank» one team bank: a round
 * where more than half the players are right adds `points` to it, any other loses one of three lives, and the game ends with the last life. A «vote» block scores 1 for everyone who voted
 * for the player with the most votes (a tie shares); a «draw» block scores 1 for the drawer for each right guess; a «buzz» block scores `points` for the first buzzer the host calls right.
 * A block that has no distance (everything but «closest») under «closest» scoring is scored «first».
 */
final class GameEngine {
    static final int LIVES = 3;
    enum Phase { ASK, REVEAL, END }

    final GameDef.Def def; final int players, teams, sides; final int[] score, gained; final List<GameDef.Item> items = new ArrayList<>();
    Phase phase = Phase.ASK; int index, lives = LIVES, buzzer = -1;
    private final Random rnd; private final Object[] answer; private final long[] at; private final boolean[] has, right, locked; private final int[] place, dist, votes;
    private int[] shown = new int[0]; private boolean drawerOut; private final List<Integer> used = new ArrayList<>();

    /** `pool` is the game's items (inline or from its pack); `recent` the item numbers of the last games on this TV (they wait while enough others are there); `teams` 0 is solo */
    GameEngine(GameDef.Def def, List<GameDef.Item> pool, int players, int teams, Random rnd, Set<Integer> recent) {
        if (def == null || pool == null || pool.isEmpty() || players < def.minPlayers || players > def.maxPlayers || teams < 0 || teams == 1 || (teams > 0 && (!def.teams || teams > players))) throw new IllegalArgumentException("args");
        this.def = def; this.players = players; this.teams = teams; this.rnd = rnd;
        sides = def.score.equals("bank") ? 1 : teams > 0 ? teams : players;
        score = new int[sides]; gained = new int[sides];
        answer = new Object[players]; at = new long[players]; has = new boolean[players]; right = new boolean[players]; locked = new boolean[players]; place = new int[players]; dist = new int[players]; votes = new int[players];
        List<Integer> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) (recent != null && recent.contains(i) ? old : fresh).add(i);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd); fresh.addAll(old);
        for (int i = 0; i < Math.min(def.rounds, fresh.size()); i++) { items.add(pool.get(fresh.get(i))); used.add(fresh.get(i)); }
        begin();
    }

    int total() { return items.size(); }
    GameDef.Item item() { return phase == Phase.END ? null : items.get(index); }
    /** the numbers (in the pool) of the items played so far, the current one included: kept on the TV so they wait next time */
    List<Integer> usedItems() { return new ArrayList<>(used.subList(0, Math.min(used.size(), index + 1))); }
    int sideOf(int p) { return sides == 1 ? 0 : teams == 0 ? p : p % teams; }
    int drawer() { return index % players; }
    /** the displayed order of an «order» or «pairs» item: shown[k] is the original number of the k-th thing on the screen */
    int[] shown() { return shown.clone(); }

    private void begin() {
        Arrays.fill(has, false); Arrays.fill(right, false); Arrays.fill(locked, false); Arrays.fill(place, 0); Arrays.fill(dist, 0); Arrays.fill(votes, 0); Arrays.fill(gained, 0); Arrays.fill(answer, null);
        buzzer = -1; drawerOut = false;
        GameDef.Item it = items.get(index);
        int n = it.t.equals("order") ? it.o.size() : it.t.equals("pairs") ? it.pairs.size() : 0;
        shown = new int[n];
        if (n > 0) {
            List<Integer> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(i);
            int tries = 0;
            do Collections.shuffle(l, rnd); while (isIdentity(l) && ++tries < 20);
            if (isIdentity(l)) Collections.rotate(l, 1);
            for (int i = 0; i < n; i++) shown[i] = l.get(i);
        }
    }
    private static boolean isIdentity(List<Integer> l) { for (int i = 0; i < l.size(); i++) if (l.get(i) != i) return false; return true; }

    // ---------------------------------------------------------------- answers

    /** how a typed number reads: Arabic-Indic and Persian digits, «٫» or «,» as the point, a leading minus; NaN when it is not a number */
    static double parseNumber(String s) {
        if (s == null) return Double.NaN;
        StringBuilder b = new StringBuilder();
        for (char c : s.trim().toCharArray()) {
            if (c >= '٠' && c <= '٩') c = (char) ('0' + c - '٠'); else if (c >= '۰' && c <= '۹') c = (char) ('0' + c - '۰');
            else if (c == '٫' || c == ',') c = '.'; else if (c == '٬' || c == ' ' || c == '‏') continue; else if (c == '−') c = '-';
            if (!((c >= '0' && c <= '9') || c == '.' || c == '-')) return Double.NaN;
            b.append(c);
        }
        String t = b.toString();
        if (t.isEmpty() || t.length() > 16 || !t.matches("-?\\d+(\\.\\d+)?")) return Double.NaN;
        return Double.parseDouble(t);
    }

    /** the forgiving spelling of a typed word: marks and tatweel off, أ إ آ ٱ = ا, ى ئ = ي, ؤ = و, ة = ه, a leading «ال» off (two letters or more left), digits as Latin, Latin lower case, one space */
    static String norm(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ((c >= 'ً' && c <= 'ْ') || c == 'ٰ' || c == 'ـ') continue;
            switch (c) { case 'أ': case 'إ': case 'آ': case 'ٱ': c = 'ا'; break; case 'ى': case 'ئ': c = 'ي'; break; case 'ؤ': c = 'و'; break; case 'ة': c = 'ه'; break; default: }
            if (c >= '٠' && c <= '٩') c = (char) ('0' + c - '٠'); else if (c >= '۰' && c <= '۹') c = (char) ('0' + c - '۰');
            if (Character.isLetterOrDigit(c)) b.append(Character.toLowerCase(c)); else if (c == ' ' && b.length() > 0 && b.charAt(b.length() - 1) != ' ') b.append(' ');
        }
        String r = b.toString().trim();
        return r.length() >= 4 && r.startsWith("ال") ? r.substring(2) : r;
    }

    /** a phone's answer, in the form the block takes: choose / odd an Integer option, tf a Boolean, order and pairs an int[] (the displayed numbers in the player's order), word / blank / draw a String,
     *  closest a Double, vote an Integer (a player, never yourself). The first answer counts; anything else, or after the round, is refused. `ms` is the time since the round began. */
    boolean answer(int p, Object a, long ms) {
        if (phase != Phase.ASK || p < 0 || p >= players || has[p] || a == null) return false;
        GameDef.Item it = items.get(index);
        switch (it.t) {
            case "choose": case "odd": if (!(a instanceof Integer) || (Integer) a < 0 || (Integer) a >= it.o.size()) return false; break;
            case "tf": if (!(a instanceof Boolean)) return false; break;
            case "order": case "pairs": {
                if (!(a instanceof int[]) || ((int[]) a).length != shown.length) return false;
                boolean[] seen = new boolean[shown.length];
                for (int v : (int[]) a) { if (v < 0 || v >= shown.length || seen[v]) return false; seen[v] = true; }
                break;
            }
            case "word": case "blank": case "draw": if (!(a instanceof String) || ((String) a).length() > 60 || norm((String) a).isEmpty()) return false; if (it.t.equals("draw") && p == drawer()) return false; break;
            case "closest": if (!(a instanceof Double) || ((Double) a).isNaN() || ((Double) a).isInfinite()) return false; break;
            case "vote": if (!(a instanceof Integer) || (Integer) a < 0 || (Integer) a >= players || (Integer) a == p) return false; break;
            default: return false;   // buzz has its own calls
        }
        answer[p] = a; at[p] = Math.max(0, ms); has[p] = true; return true;
    }
    boolean hasAnswered(int p) { return has[p]; }
    int answeredCount() { int n = 0; for (boolean h : has) if (h) n++; return n; }
    boolean allAnswered() {
        GameDef.Item it = items.get(index);
        if (it.t.equals("buzz")) return false;
        int need = it.t.equals("draw") ? players - 1 : players;
        return answeredCount() >= need;
    }

    // ---------------------------------------------------------------- a «buzz» round: the first phone to press speaks, the host calls it right or wrong

    /** a phone presses first (not one that already got it wrong); true when it is now the speaker */
    boolean buzz(int p, long ms) {
        if (phase != Phase.ASK || !items.get(index).t.equals("buzz") || p < 0 || p >= players || buzzer >= 0 || locked[p]) return false;
        buzzer = p; at[p] = ms; return true;
    }
    /** the host's call on the speaker: right scores `points` for their side and ends the round; wrong locks them out and opens the buzzers again (everyone out: the round ends with no point) */
    void judgeBuzz(boolean ok) {
        if (phase != Phase.ASK || buzzer < 0) return;
        int p = buzzer; buzzer = -1;
        if (ok) { right[p] = true; place[p] = 1; add(sideOf(p), def.points); reveal(); return; }
        locked[p] = true;
        boolean any = false; for (boolean l : locked) if (!l) any = true;
        if (!any) reveal();
    }
    boolean lockedOut(int p) { return locked[p]; }

    // ---------------------------------------------------------------- the end of a round

    private void add(int side, int pts) { score[side] += pts; gained[side] += pts; }

    /** is a phone's answer right (after reveal and for the screens that colour it) */
    private boolean judge(GameDef.Item it, int p) {
        Object a = answer[p];
        if (!has[p]) return false;
        switch (it.t) {
            case "choose": case "odd": return (Integer) a == it.a;
            case "tf": return (Boolean) a == it.tf;
            case "order": { int[] s = (int[]) a; for (int k = 0; k < s.length; k++) if (shown[s[k]] != k) return false; return true; }
            case "pairs": { int[] s = (int[]) a; for (int k = 0; k < s.length; k++) if (shown[s[k]] != k) return false; return true; }
            case "word": case "blank": case "draw": { String n = norm((String) a); for (String x : it.answers) if (norm(x).equals(n)) return true; return false; }
            case "closest": return ((Double) a) == it.num;
            default: return false;
        }
    }

    /** closes the round and scores it (a «buzz» round closes itself in judgeBuzz) */
    void reveal() {
        if (phase != Phase.ASK) return;
        GameDef.Item it = items.get(index);
        Arrays.fill(gained, 0);
        if (it.t.equals("vote")) {
            for (int p = 0; p < players; p++) if (has[p]) votes[(Integer) answer[p]]++;
            int max = 0; for (int v : votes) max = Math.max(max, v);
            for (int p = 0; p < players; p++) if (has[p] && max > 0 && votes[(Integer) answer[p]] == max) { right[p] = true; add(sideOf(p), 1); }
        } else if (!it.t.equals("buzz")) {
            for (int p = 0; p < players; p++) right[p] = judge(it, p);
            if (def.score.equals("closest") && it.t.equals("closest")) scoreClosest(it);
            else if (def.score.equals("bank")) scoreBank();
            else scoreByTime();
            if (it.t.equals("draw")) { int n = 0; for (boolean r : right) if (r) n++; if (n > 0) add(sideOf(drawer()), n); }
        }
        phase = Phase.REVEAL;
    }

    private void scoreByTime() {
        List<Integer> r = new ArrayList<>();
        for (int p = 0; p < players; p++) if (right[p]) r.add(p);
        Collections.sort(r, (x, y) -> at[x] != at[y] ? Long.compare(at[x], at[y]) : Integer.compare(x, y));
        long limit = def.seconds * 1000L;
        for (int i = 0; i < r.size(); i++) {
            int p = r.get(i); place[p] = i + 1;
            int pts = def.score.equals("all") ? def.points : def.score.equals("speed") ? 1 + (int) Math.round((def.points - 1) * Math.max(0.0, 1.0 - at[p] / (double) limit)) : Math.max(1, def.points - i);
            add(sideOf(p), pts);
        }
    }

    private void scoreClosest(GameDef.Item it) {
        for (int p = 0; p < players; p++) if (has[p]) dist[p] = (int) Math.min(Integer.MAX_VALUE, Math.round(Math.abs(((Double) answer[p]) - it.num)));
        for (int p = 0; p < players; p++) {
            if (!has[p]) continue;
            int rank = 1; for (int q = 0; q < players; q++) if (has[q] && dist[q] < dist[p]) rank++;
            place[p] = rank;
            int pts = Math.max(1, def.points - (rank - 1));
            if (rank > def.points) pts = 0;
            if (right[p]) pts += 1;
            if (pts > 0) add(sideOf(p), pts);
        }
    }

    private void scoreBank() {
        int n = 0; for (boolean r : right) if (r) n++;
        for (int p = 0; p < players; p++) if (right[p]) place[p] = 1;
        if (n * 2 > players) add(0, def.points); else lives--;
    }

    boolean rightOf(int p) { return right[p]; }
    int placeOf(int p) { return place[p]; }
    /** how far a «closest» answer was (whole units), -1 when the player gave none */
    int distOf(int p) { return has[p] && items.get(index).t.equals("closest") ? dist[p] : -1; }
    Object answerOf(int p) { return answer[p]; }
    long timeOf(int p) { return at[p]; }
    int votesFor(int p) { return votes[p]; }
    /** the players with the most votes in a «vote» round */
    List<Integer> topVoted() { int max = 0; for (int v : votes) max = Math.max(max, v); List<Integer> r = new ArrayList<>(); if (max > 0) for (int p = 0; p < players; p++) if (votes[p] == max) r.add(p); return r; }

    /** the next round, or the end (the last round played, or no lives left in «bank») */
    void next() {
        if (phase != Phase.REVEAL) return;
        boolean out = def.score.equals("bank") && lives <= 0;
        index++;
        if (index >= items.size() || out) { index = Math.min(index - 1, items.size() - 1); phase = Phase.END; return; }   // the last round played stays the current one
        phase = Phase.ASK; begin();
    }
    void finish() { phase = Phase.END; }

    /** the sides with the most points (a tie is shared) */
    List<Integer> leaders() { int m = -1; for (int s : score) m = Math.max(m, s); List<Integer> r = new ArrayList<>(); for (int s = 0; s < sides; s++) if (score[s] == m) r.add(s); return r; }
}
