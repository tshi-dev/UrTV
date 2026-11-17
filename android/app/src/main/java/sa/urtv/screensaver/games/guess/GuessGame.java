package sa.urtv.screensaver.games.guess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * «كم تقدر؟»: the topics and the rules (no Android, tests/NewGamesTest.java). A topic is shown («فواكه»); everyone bids on their phone how many things of it they can name;
 * the highest bid names them (typed on its phone). Every name is checked against the topic's list: if the bidder names at least their bid, they score the bid, else
 * everybody else scores 1. A tie is settled by chance. The file: {"v":1,"topics":[{"i","t":["ar","en"],"k":1,"a":[["accepted spellings of one answer"...]...]}]}; an unusable
 * topic is skipped, not the file. Points only. docs/GAMES-NEW.md.
 */
public final class GuessGame {
    public static final int MIN_PLAYERS = 2, MAX_PLAYERS = 10, TOPICS_MIN = 15, LIST_MIN = 12, BID_CAP = 10, FAIL_POINTS = 1;
    public static final int[] ROUNDS = {6, 8, 10}, BID_SECONDS = {20, 30}, ANSWER_SECONDS = {45, 60, 90};
    public enum Phase { BID, ANSWER, REVEAL, END }

    public static final class Topic {
        public final String id; public final String[] t; public final boolean kids; public final List<String> names = new ArrayList<>(); final Map<String, Integer> index = new HashMap<>();
        Topic(String id, String ta, String te, boolean kids) { this.id = id; t = new String[] {ta, te}; this.kids = kids; }
        public String text(boolean en) { return en && !t[1].isEmpty() ? t[1] : t[0]; }
        public int size() { return names.size(); }
        /** the most a player may bid on this topic: at most ten, and at most half the list */
        public int cap() { return Math.min(BID_CAP, names.size() / 2); }
    }

    /** the spelling every answer is compared in: no marks, no tatweel, the alef, yaa and taa marbuta forms joined, no «ال», one space */
    public static String norm(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 0x064B && c <= 0x065F || c == 0x0640 || c == 0x0670 || c == 0x200F || c == 0x200E) continue;
            if (c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ') c = 'ا';
            else if (c == 'ى' || c == 'ئ') c = 'ي';
            else if (c == 'ة') c = 'ه';
            else if (c == 'ؤ') c = 'و';
            else if (c == '٫' || c == '،' || c == ',' || c == '.') c = ' ';
            b.append(Character.toLowerCase(c));
        }
        String[] w = b.toString().trim().split("\\s+");
        StringBuilder o = new StringBuilder();
        for (String x : w) { if (x.startsWith("ال") && x.length() > 3) x = x.substring(2); if (o.length() > 0) o.append(' '); o.append(x); }
        return o.toString();
    }

    public static List<Topic> parse(Object tree) {
        if (!(tree instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) tree;
        if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1 || !(m.get("topics") instanceof List)) return null;
        List<Topic> out = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (Object o : (List<?>) m.get("topics")) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) o; Object id = p.get("i"), t = p.get("t"), a = p.get("a"), k = p.get("k");
            if (!(id instanceof String) || !ids.add((String) id) || !(t instanceof List) || ((List<?>) t).isEmpty() || !(((List<?>) t).get(0) instanceof String) || !(a instanceof List)) continue;
            List<?> tl = (List<?>) t; String ta = ((String) tl.get(0)).trim(), te = tl.size() > 1 && tl.get(1) instanceof String ? ((String) tl.get(1)).trim() : "";
            if (ta.isEmpty() || ta.length() > 60 || te.length() > 80 || (ta + te).indexOf('·') >= 0) continue;
            Topic tp = new Topic((String) id, ta, te, k instanceof Number && ((Number) k).intValue() == 1);
            for (Object ans : (List<?>) a) {
                if (!(ans instanceof List) || ((List<?>) ans).isEmpty()) continue;
                int at = tp.names.size(); boolean first = true; List<String> keys = new ArrayList<>();
                for (Object s : (List<?>) ans) {
                    if (!(s instanceof String)) continue;
                    String n = norm((String) s);
                    if (n.isEmpty() || n.length() > 40 || tp.index.containsKey(n) || keys.contains(n)) continue;
                    if (first) { tp.names.add(((String) s).trim()); first = false; }
                    keys.add(n);
                }
                if (!first) for (String n : keys) tp.index.put(n, at);
            }
            if (tp.size() >= LIST_MIN) out.add(tp);
        }
        return out.size() >= TOPICS_MIN ? out : null;
    }

    public static List<Topic> pick(List<Topic> all, int n, boolean kids, Set<String> recent, Random rnd) {
        List<Topic> fresh = new ArrayList<>(), old = new ArrayList<>();
        for (Topic p : all) if (!kids || p.kids) (recent.contains(p.id) ? old : fresh).add(p);
        Collections.shuffle(fresh, rnd); Collections.shuffle(old, rnd); fresh.addAll(old);
        return new ArrayList<>(fresh.subList(0, Math.min(n, fresh.size())));
    }

    // ---------------------------------------------------------------- the game

    public final int players, total; public final int[] score, gained, bid; public Phase phase = Phase.BID; public int index, winner = -1, counted, topBid;
    private final List<Topic> topics; private final Random rnd; private final Set<Integer> found = new HashSet<>(); private final List<Boolean> lastValid = new ArrayList<>();
    private final List<String> asked = new ArrayList<>(); private boolean ok;

    public GuessGame(int players, List<Topic> topics, Random rnd) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS || topics.isEmpty()) throw new IllegalArgumentException("players");
        this.players = players; this.topics = topics; this.rnd = rnd; total = topics.size();
        score = new int[players]; gained = new int[players]; bid = new int[players]; Arrays.fill(bid, -1); asked.add(topics.get(0).id);
    }
    public Topic topic() { return topics.get(Math.min(index, total - 1)); }
    public List<String> asked() { return asked; }
    public int cap() { return topic().cap(); }

    /** a bid is a whole number from 1 to the topic's cap, once per player */
    public boolean bid(int p, int n) { if (phase != Phase.BID || p < 0 || p >= players || bid[p] >= 0 || n < 1 || n > cap()) return false; bid[p] = n; return true; }
    public int bidsIn() { int n = 0; for (int b : bid) if (b >= 0) n++; return n; }
    public boolean allBid() { return bidsIn() == players; }
    public int bidOf(int p) { return bid[p]; }

    /** closes the bidding: the highest bid names the things (a tie by chance); with no bid at all the round is passed */
    public void endBids() {
        if (phase != Phase.BID) return;
        topBid = 0; for (int b : bid) topBid = Math.max(topBid, b);
        winner = -1; counted = 0; found.clear(); lastValid.clear(); Arrays.fill(gained, 0);
        if (topBid <= 0) { phase = Phase.REVEAL; return; }
        List<Integer> tied = new ArrayList<>(); for (int i = 0; i < players; i++) if (bid[i] == topBid) tied.add(i);
        winner = tied.get(rnd.nextInt(tied.size())); phase = Phase.ANSWER;
    }

    /** what the bidder typed: each field checked on its own, a name counts once, at most the bid's worth of fields are read; closes the answer and scores it */
    public int answer(List<String> typed) {
        if (phase != Phase.ANSWER) return counted;
        Topic t = topic(); found.clear(); lastValid.clear();
        for (int i = 0; typed != null && i < typed.size() && i < topBid; i++) {
            Integer at = t.index.get(norm(typed.get(i)));
            boolean good = at != null && found.add(at); lastValid.add(good);
        }
        counted = found.size();
        if (counted >= topBid) gained[winner] = topBid; else for (int i = 0; i < players; i++) if (i != winner) gained[i] = FAIL_POINTS;
        for (int i = 0; i < players; i++) score[i] += gained[i];
        phase = Phase.REVEAL; return counted;
    }
    public boolean valid(int field) { return field >= 0 && field < lastValid.size() && lastValid.get(field); }
    public boolean succeeded() { return winner >= 0 && counted >= topBid; }

    /** a few answers of the list the bidder did not name, for the reveal */
    public List<String> examples(int n) {
        List<String> r = new ArrayList<>(); Topic t = topic();
        for (int i = 0; i < t.size() && r.size() < n; i++) if (!found.contains(i)) r.add(t.names.get(i));
        return r;
    }

    public void next() {
        if (phase != Phase.REVEAL) return;
        index++; Arrays.fill(bid, -1); Arrays.fill(gained, 0); winner = -1; counted = 0;
        if (index >= total) phase = Phase.END; else { phase = Phase.BID; asked.add(topics.get(index).id); }
    }
    public void finish() { phase = Phase.END; }
}
