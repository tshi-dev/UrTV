package sa.urtv.screensaver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * «الأسرع», the rules of one live-quiz game in one object (no Android, no network: tests/quiz_test.py runs it on a plain JVM with a fake clock).
 * The TV owns everything: the phones only join, send «answer N for question Q» and read their own state. Nothing here is written to disk;
 * when the game is closed the object is dropped (names, colours, tokens and scores exist only in memory). docs/QUIZ.md, games/live-quiz/SPEC.md.
 */
final class QuizRoom {
    static final int MAX_PLAYERS = 12, NAME_MAX = 12, COLORS = 8;
    static final int QUESTION_MS = 20000, REVEAL_MS = 6000;
    static final int BASE = 500, BONUS = 500;
    static final long CONNECTED_MS = 6000, DROP_LOBBY_MS = 120000, CODE_LIFE_AFTER_END_MS = 10 * 60 * 1000;
    static final int JOIN_PER_MIN = 20, WRONG_CODE_LOCK = 5; static final long LOCK_MS = 60000;
    static final int ANSWER_PER_SEC = 5;

    enum Phase { LOBBY, QUESTION, REVEAL, END, PAUSED, CLOSED }

    /** one question as the room plays it: the options are already in their shown order, `correct` is an index into them */
    static final class Q {
        final String text; final String[] opts; final int correct;
        Q(String text, String[] opts, int correct) { this.text = text; this.opts = opts; this.correct = correct; }
    }

    static final class Player {
        final String token; String name; int color; int score, streak, rank, prevRank, answer = -1, lastPoints, lastDelta;
        long answerMs = -1, lastSeen, joinedAt; boolean left, waiting, lastKnown; Boolean lastOk; final long order;
        final ArrayDeque<Long> sent = new ArrayDeque<>();
        Player(String token, String name, int color, long order) { this.token = token; this.name = name; this.color = color; this.order = order; }
    }

    static final class Result {
        final boolean ok; final String error; final Player player;
        Result(boolean ok, String error, Player player) { this.ok = ok; this.error = error; this.player = player; }
    }

    private final LongSupplier clock; private final Random rnd;
    final String code; Phase phase = Phase.LOBBY; int version;
    private final List<Player> players = new ArrayList<>(); private long nextOrder;
    private List<Q> questions = new ArrayList<>(); int index = -1; private long qStart, qLeftWhenPaused; private Phase pausedFrom;
    private long revealAt, endAt, lockedUntil; private int wrongCodes; private final ArrayDeque<Long> joins = new ArrayDeque<>();
    int[] counts = new int[4];

    QuizRoom(String code, LongSupplier clock, Random rnd) { this.code = code; this.clock = clock; this.rnd = rnd; }

    static String newCode(Random r) { return String.format(java.util.Locale.ROOT, "%04d", r.nextInt(10000)); }

    private static final java.util.Map<String, Long> USED = new java.util.HashMap<>();
    /** a room code not used in the last hour (the same code is never reused within the hour; with 10,000 codes a draw almost always succeeds at once) */
    static synchronized String freshCode(Random r, long now) {
        USED.values().removeIf(t -> now - t > 3600000L);
        String c = newCode(r);
        for (int i = 0; i < 300 && USED.containsKey(c); i++) c = newCode(r);
        USED.put(c, now);
        return c;
    }

    // ---------------------------------------------------------------- join

    /** the code is checked here too: 5 wrong codes lock joining for 60 s, 20 join attempts a minute for the room, 12 players, a late join waits for the next question */
    synchronized Result join(String c, String name, int color, String token) {
        long now = clock.getAsLong();
        if (phase == Phase.CLOSED || (phase == Phase.END && now - endAt > CODE_LIFE_AFTER_END_MS)) return new Result(false, "closed", null);
        if (now < lockedUntil) return new Result(false, "locked", null);
        while (!joins.isEmpty() && now - joins.peekFirst() > 60000) joins.pollFirst();
        if (joins.size() >= JOIN_PER_MIN) return new Result(false, "rate", null);
        joins.addLast(now);
        if (!code.equals(c)) {
            if (++wrongCodes >= WRONG_CODE_LOCK) { lockedUntil = now + LOCK_MS; wrongCodes = 0; }
            return new Result(false, "code", null);
        }
        wrongCodes = 0;
        Player known = byToken(token);
        if (known != null) {            // the same phone coming back: its name, colour and score are as they were
            known.left = false; known.lastSeen = now; bump();
            return new Result(true, null, known);
        }
        if (phase == Phase.END) return new Result(false, "closed", null);
        String n = cleanName(name);
        if (n.isEmpty()) return new Result(false, "name", null);
        if (activeCount() >= MAX_PLAYERS) return new Result(false, "full", null);
        Player p = new Player(newToken(), uniqueName(n), color >= 0 && color < COLORS ? color : (int) (nextOrder % COLORS), nextOrder++);
        p.lastSeen = now; p.joinedAt = now; p.waiting = phase != Phase.LOBBY;
        players.add(p); rerank(); bump();
        return new Result(true, null, p);
    }

    /** letters, digits and single spaces only, at most 12 characters, nothing that reads as a link or an address */
    static String cleanName(String raw) {
        if (raw == null) return "";
        StringBuilder b = new StringBuilder();
        int cps = 0;
        for (int i = 0; i < raw.length() && cps < NAME_MAX; ) {
            int cp = raw.codePointAt(i); i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) { b.appendCodePoint(cp); cps++; }
            else if (cp == ' ' && b.length() > 0 && b.charAt(b.length() - 1) != ' ') { b.append(' '); cps++; }
        }
        String s = b.toString().trim();
        String low = s.toLowerCase(java.util.Locale.ROOT);
        if (low.contains("http") || low.contains("www")) return "";   // the dots and slashes are already gone: no link can be written
        return s;
    }

    private String uniqueName(String n) {
        boolean ar = false;
        for (int i = 0; i < n.length(); i++) if (n.charAt(i) >= 0x0600 && n.charAt(i) <= 0x06FF) ar = true;
        String cand = n; int k = 1;
        while (nameTaken(cand)) { k++; cand = n + " " + (ar ? arabicDigits(k) : String.valueOf(k)); }
        return cand;
    }
    private boolean nameTaken(String n) { for (Player p : players) if (p.name.equals(n)) return true; return false; }
    static String arabicDigits(int v) { StringBuilder b = new StringBuilder(); for (char c : String.valueOf(v).toCharArray()) b.append((char) ('٠' + (c - '0'))); return b.toString(); }
    private String newToken() { StringBuilder b = new StringBuilder(); for (int i = 0; i < 16; i++) b.append("0123456789abcdefghijklmnopqrstuvwxyz".charAt(rnd.nextInt(36))); return b.toString(); }

    synchronized Player byToken(String t) { if (t == null || t.isEmpty()) return null; for (Player p : players) if (p.token.equals(t)) return p; return null; }
    synchronized int activeCount() { int n = 0; for (Player p : players) if (!p.left) n++; return n; }
    synchronized List<Player> players() { return new ArrayList<>(players); }

    // ---------------------------------------------------------------- the game

    synchronized boolean canStart() { return phase == Phase.LOBBY && connectedCount() >= 2; }
    private int connectedCount() { int n = 0; long now = clock.getAsLong(); for (Player p : players) if (!p.left && now - p.lastSeen <= CONNECTED_MS) n++; return n; }

    /** OK on the TV with at least 2 players in: the first question opens */
    synchronized boolean start(List<Q> qs) {
        if (!canStart() || qs == null || qs.isEmpty()) return false;
        questions = new ArrayList<>(qs); index = -1;
        for (Player p : players) { p.waiting = false; p.score = 0; p.streak = 0; p.lastOk = null; }
        rerank(); next(); return true;
    }

    int total() { return questions.size(); }
    synchronized Q current() { return index >= 0 && index < questions.size() ? questions.get(index) : null; }

    /** the next question, or the podium after the last one */
    synchronized void next() {
        if (phase != Phase.REVEAL && phase != Phase.LOBBY) return;
        if (index + 1 >= questions.size()) { phase = Phase.END; endAt = clock.getAsLong(); bump(); return; }
        index++; phase = Phase.QUESTION; qStart = clock.getAsLong();
        for (Player p : players) { p.answer = -1; p.answerMs = -1; p.waiting = false; p.lastOk = null; p.lastPoints = 0; }
        counts = new int[4]; bump();
    }

    synchronized long leftMs() {
        if (phase == Phase.PAUSED) return qLeftWhenPaused;
        if (phase != Phase.QUESTION) return 0;
        return Math.max(0, QUESTION_MS - (clock.getAsLong() - qStart));
    }
    synchronized long revealLeftMs() { return phase == Phase.REVEAL ? Math.max(0, revealAt + REVEAL_MS - clock.getAsLong()) : 0; }

    /** a phone's answer: valid only for the open question, once, with an index of 0 to 3, five messages a second at most */
    synchronized String answer(String token, int q, int a) {
        Player p = byToken(token);
        if (p == null) return "token";
        long now = clock.getAsLong();
        while (!p.sent.isEmpty() && now - p.sent.peekFirst() > 1000) p.sent.pollFirst();
        if (p.sent.size() >= ANSWER_PER_SEC) return "rate";
        p.sent.addLast(now); p.lastSeen = now;
        if (phase != Phase.QUESTION || q != index || leftMs() <= 0) return "closed";
        if (a < 0 || a > 3) return "bad";
        if (p.waiting || p.left) return "closed";
        if (p.answer >= 0) return "done";
        p.answer = a; p.answerMs = now - qStart; counts[a]++; bump();
        if (allAnswered()) reveal();
        return null;
    }

    private boolean allAnswered() {
        int n = 0;
        long now = clock.getAsLong();
        for (Player p : players) if (!p.left && !p.waiting && now - p.lastSeen <= CONNECTED_MS) { n++; if (p.answer < 0) return false; }
        return n > 0;
    }

    /** called often by the TV (10 times a second is enough): closes a question at 20 s, ends the reveal at 6 s, drops phones silent for 2 minutes from the lobby */
    synchronized void tick() {
        long now = clock.getAsLong();
        if (phase == Phase.QUESTION && now - qStart >= QUESTION_MS) reveal();
        else if (phase == Phase.REVEAL && now - revealAt >= REVEAL_MS) next();
        if (phase == Phase.LOBBY) { for (int i = players.size() - 1; i >= 0; i--) if (now - players.get(i).lastSeen > DROP_LOBBY_MS) { players.remove(i); bump(); } }
    }

    /** the question is over: points are worked out here, on the TV */
    synchronized void reveal() {
        if (phase != Phase.QUESTION) return;
        Q q = questions.get(index);
        for (Player p : players) {
            if (p.waiting || p.left) { p.lastOk = null; continue; }
            if (p.answer < 0) { p.lastOk = null; p.streak = 0; p.lastPoints = 0; continue; }
            boolean ok = p.answer == q.correct;
            p.lastOk = ok;
            if (ok) {
                p.streak++;
                p.lastPoints = points(p.answerMs, p.streak);
                p.score += p.lastPoints;
            } else { p.streak = 0; p.lastPoints = 0; }
        }
        for (Player p : players) p.prevRank = p.rank;
        rerank();
        for (Player p : players) p.lastDelta = p.prevRank == 0 ? 0 : p.prevRank - p.rank;
        phase = Phase.REVEAL; revealAt = clock.getAsLong(); bump();
    }

    /** 500 + 500 x time left / 20 s, plus the streak: +50 from the 3rd right answer in a row, +100 the 4th, +150 from the 5th */
    static int points(long answerMs, int streak) {
        long left = Math.max(0, QUESTION_MS - Math.max(0, answerMs));
        int p = (int) (BASE + Math.round((double) BONUS * left / QUESTION_MS));
        return p + (streak >= 5 ? 150 : streak == 4 ? 100 : streak == 3 ? 50 : 0);
    }

    private void rerank() {
        List<Player> v = new ArrayList<>(players);
        v.sort(Comparator.<Player>comparingInt(p -> -p.score).thenComparingLong(p -> p.answerMs < 0 ? Long.MAX_VALUE : p.answerMs).thenComparingLong(p -> p.order));
        int r = 1; for (Player p : v) p.rank = r++;
    }
    synchronized List<Player> ranked() { List<Player> v = new ArrayList<>(players); v.sort(Comparator.comparingInt(p -> p.rank)); return v; }

    synchronized void skipReveal() { if (phase == Phase.REVEAL) next(); }
    synchronized void pause() { if (phase == Phase.QUESTION || phase == Phase.REVEAL) { qLeftWhenPaused = leftMs(); pausedFrom = phase; if (phase == Phase.REVEAL) qLeftWhenPaused = revealLeftMs(); phase = Phase.PAUSED; bump(); } }
    synchronized void resume() {
        if (phase != Phase.PAUSED) return;
        long now = clock.getAsLong();
        if (pausedFrom == Phase.QUESTION) qStart = now - (QUESTION_MS - qLeftWhenPaused); else revealAt = now - (REVEAL_MS - qLeftWhenPaused);
        phase = pausedFrom; bump();
    }
    /** «إنهاء اللعبة» from the pause card: the podium so far */
    synchronized void finish() { if (phase == Phase.QUESTION || phase == Phase.REVEAL || phase == Phase.PAUSED) { phase = Phase.END; endAt = clock.getAsLong(); bump(); } }
    /** «العب مرة أخرى»: the same players, scores at 0, back to the lobby */
    synchronized void playAgain() {
        if (phase != Phase.END) return;
        for (Player p : players) { p.score = 0; p.streak = 0; p.waiting = false; p.lastOk = null; p.answer = -1; p.rank = 0; p.prevRank = 0; }
        rerank(); index = -1; phase = Phase.LOBBY; bump();
    }
    synchronized void close() { phase = Phase.CLOSED; players.clear(); questions.clear(); bump(); }

    /** a player leaves: gone at once from the lobby, grey in the list with the points kept during a game */
    synchronized void leave(String token) {
        Player p = byToken(token); if (p == null) return;
        if (phase == Phase.LOBBY) players.remove(p); else p.left = true;
        bump();
        if (phase == Phase.QUESTION && allAnswered()) reveal();
    }
    /** the host removes a player from the lobby with OK on its tile */
    synchronized void kick(String token) { Player p = byToken(token); if (p != null) { players.remove(p); bump(); } }

    synchronized void touch(String token) { Player p = byToken(token); if (p != null) p.lastSeen = clock.getAsLong(); }
    synchronized boolean connected(Player p) { return !p.left && clock.getAsLong() - p.lastSeen <= CONNECTED_MS; }
    synchronized int answeredCount() { int n = 0; for (Player p : players) if (!p.left && !p.waiting && p.answer >= 0) n++; return n; }
    synchronized int playingCount() { int n = 0; for (Player p : players) if (!p.left && !p.waiting) n++; return n; }
    private void bump() { version++; }
    synchronized int version() { return version; }

    // ---------------------------------------------------------------- what a phone may read

    static String esc(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20) b.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    /** the state a phone reads (GET /api/state): never the right answer before the reveal */
    synchronized String stateFor(String token) {
        Player p = byToken(token);
        if (phase == Phase.CLOSED) return "{\"ok\":false,\"e\":\"closed\"}";
        if (p == null) return "{\"ok\":false,\"e\":\"token\"}";
        p.lastSeen = clock.getAsLong();
        String ph = phase.name().toLowerCase(java.util.Locale.ROOT);
        StringBuilder b = new StringBuilder("{\"ok\":true,\"v\":").append(version).append(",\"ph\":\"").append(ph).append("\",\"me\":{\"n\":").append(esc(p.name))
            .append(",\"k\":").append(p.color).append(",\"s\":").append(p.score).append(",\"r\":").append(p.rank).append(",\"of\":").append(activeCount())
            .append(",\"left\":").append(p.left).append("},\"pl\":").append(activeCount());
        Q q = current();
        if ((phase == Phase.QUESTION || phase == Phase.REVEAL || phase == Phase.PAUSED) && q != null) {
            boolean open = phase == Phase.QUESTION || phase == Phase.PAUSED;
            b.append(",\"q\":{\"i\":").append(index).append(",\"n\":").append(total()).append(",\"t\":").append(esc(q.text)).append(",\"o\":[");
            for (int i = 0; i < 4; i++) { if (i > 0) b.append(','); b.append(esc(q.opts[i])); }
            b.append("],\"ms\":").append(open ? leftMs() : 0).append(",\"dur\":").append(QUESTION_MS).append(",\"ans\":").append(answeredCount()).append(",\"of\":").append(playingCount())
                .append(",\"pick\":").append(p.answer).append(",\"wait\":").append(p.waiting).append('}');
            if (phase == Phase.REVEAL) {
                b.append(",\"rv\":{\"c\":").append(q.correct).append(",\"ok\":").append(p.lastOk == null ? "null" : p.lastOk.toString()).append(",\"p\":").append(p.lastPoints)
                    .append(",\"st\":").append(p.streak).append(",\"r\":").append(p.rank).append(",\"d\":").append(p.lastDelta).append(",\"cnt\":[")
                    .append(counts[0]).append(',').append(counts[1]).append(',').append(counts[2]).append(',').append(counts[3]).append("]}");
            }
        }
        if (phase == Phase.END) b.append(",\"end\":{\"r\":").append(p.rank).append(",\"of\":").append(activeCount()).append(",\"s\":").append(p.score).append('}');
        return b.append('}').toString();
    }
}
