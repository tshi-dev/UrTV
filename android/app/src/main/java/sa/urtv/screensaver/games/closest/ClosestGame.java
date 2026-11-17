package sa.urtv.screensaver.games.closest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * «أقرب رقم»: the items and the rules (no Android, tests/NewGamesTest.java). A question whose answer is a whole number; everyone types a guess on their phone; the
 * closest wins 3 points (5 for the exact number), the next 2, the next 1; guesses at the same distance share the same points. The file:
 * {"v":1,"items":[{"i","q":["ar","en"],"v":86400,"u":["ثانية","seconds"],"d":1,"k":1,"s":"source"}]}; an unusable item is skipped, not the file. docs/GAMES-NEW.md.
 */
public final class ClosestGame {
    public static final int MIN_PLAYERS = 2, MAX_PLAYERS = 10, ITEMS_MIN = 40, EXACT = 5;
    public static final int[] QUESTIONS = {8, 10, 12}, SECONDS = {20, 30, 45};
    public static final long VALUE_MAX = 9_999_999_999_999L;

    public static final class Item {
        public final String id; public final String[] q, u; public final long value; public final boolean kids; public final int diff;
        Item(String id, String qa, String qe, long value, String ua, String ue, int diff, boolean kids) { this.id = id; q = new String[] {qa, qe}; u = new String[] {ua, ue}; this.value = value; this.diff = diff; this.kids = kids; }
        public String text(boolean en) { return en && !q[1].isEmpty() ? q[1] : q[0]; }
        public String unit(boolean en) { return en && !u[1].isEmpty() ? u[1] : u[0]; }
    }

    public static List<Item> parse(Object tree) {
        if (!(tree instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) tree;
        if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1 || !(m.get("items") instanceof List)) return null;
        List<Item> out = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (Object o : (List<?>) m.get("items")) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) o;
            Object id = p.get("i"), q = p.get("q"), v = p.get("v"), u = p.get("u"), d = p.get("d"), k = p.get("k");
            if (!(id instanceof String) || !ids.add((String) id) || !(q instanceof List) || ((List<?>) q).size() < 1 || !(((List<?>) q).get(0) instanceof String) || !(v instanceof Number)) continue;
            double dv = ((Number) v).doubleValue();
            if (dv != Math.rint(dv) || dv < 0 || dv > VALUE_MAX) continue;
            List<?> ql = (List<?>) q, ul = u instanceof List ? (List<?>) u : new ArrayList<>();
            String qa = ((String) ql.get(0)).trim(), qe = ql.size() > 1 && ql.get(1) instanceof String ? ((String) ql.get(1)).trim() : "";
            String ua = ul.size() > 0 && ul.get(0) instanceof String ? (String) ul.get(0) : "", ue = ul.size() > 1 && ul.get(1) instanceof String ? (String) ul.get(1) : "";
            if (qa.isEmpty() || qa.length() > 200 || qe.length() > 240 || (qa + qe + ua + ue).indexOf('·') >= 0) continue;
            out.add(new Item((String) id, qa, qe, (long) dv, ua, ue, d instanceof Number ? ((Number) d).intValue() : 2, k instanceof Number && ((Number) k).intValue() == 1));
        }
        return out.size() >= ITEMS_MIN ? out : null;
    }

    public static List<Item> pick(List<Item> all, int n, boolean kids, Set<String> recent, Random rnd) {
        List<Item> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (Item p : all) if (!kids || p.kids) (recent.contains(p.id) ? old : fresh).add(p);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd);
        fresh.addAll(old);
        return new ArrayList<>(fresh.subList(0, Math.min(n, fresh.size())));
    }

    /** what a phone typed as a number: Arabic-Indic or Latin digits, spaces and group marks ignored, at most 13 digits; null when it is not a whole number */
    public static Long number(String raw) {
        if (raw == null) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '٠' && c <= '٩') b.append((char) ('0' + c - '٠'));
            else if (c >= '۰' && c <= '۹') b.append((char) ('0' + c - '۰'));
            else if (c >= '0' && c <= '9') b.append(c);
            else if (c == ' ' || c == ',' || c == '٬' || c == '،') continue;
            else return null;
        }
        if (b.length() == 0 || b.length() > 13) return null;
        return Long.parseLong(b.toString());
    }

    // ---------------------------------------------------------------- the game

    public enum Phase { ASK, REVEAL, END }
    public final int players, total; public final List<Item> items; public final int[] score, gained;
    public Phase phase; public int index;
    private final Long[] guess; private List<Integer> ranking = new ArrayList<>();

    public ClosestGame(int players, List<Item> items) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) throw new IllegalArgumentException("players");
        this.players = players; this.items = items; total = items.size(); score = new int[players]; gained = new int[players]; guess = new Long[players];
        phase = total == 0 ? Phase.END : Phase.ASK;
    }
    public Item item() { return phase == Phase.END ? null : items.get(index); }

    /** a player's guess (once); false when it is not a number or the question is over */
    public boolean submit(int p, String text) {
        Long g = number(text);
        if (phase != Phase.ASK || p < 0 || p >= players || guess[p] != null || g == null) return false;
        guess[p] = g; return true;
    }
    public int answered() { int n = 0; for (Long g : guess) if (g != null) n++; return n; }
    public boolean allIn() { return answered() == players; }
    public Long guessOf(int p) { return guess[p]; }

    /** the answer is shown: the guesses ranked by distance; 3, 2, 1 points to the first three distances (5 for the exact one), ties share */
    public void reveal() {
        if (phase != Phase.ASK) return;
        long v = item().value;
        java.util.Arrays.fill(gained, 0);
        ranking = new ArrayList<>();
        for (int p = 0; p < players; p++) if (guess[p] != null) ranking.add(p);
        ranking.sort((a, b) -> Long.compare(Math.abs(guess[a] - v), Math.abs(guess[b] - v)));
        int place = 0; long last = -1;
        for (int p : ranking) {
            long d = Math.abs(guess[p] - v);
            if (d != last) { place++; last = d; }
            if (place > 3) break;
            gained[p] = place == 1 ? (d == 0 ? EXACT : 3) : place == 2 ? 2 : 1;
        }
        for (int p = 0; p < players; p++) score[p] += gained[p];
        phase = Phase.REVEAL;
    }
    /** the players with a guess, the closest first */
    public List<Integer> ranking() { return ranking; }
    public long distance(int p) { return guess[p] == null ? -1 : Math.abs(guess[p] - item().value); }

    public void next() {
        if (phase != Phase.REVEAL) return;
        index++; java.util.Arrays.fill(guess, null);
        phase = index >= total ? Phase.END : Phase.ASK;
    }
    public void finish() { phase = Phase.END; }
    public List<String> asked() { List<String> v = new ArrayList<>(); for (int i = 0; i < Math.min(total, index + 1); i++) v.add(items.get(i).id); return v; }
}
