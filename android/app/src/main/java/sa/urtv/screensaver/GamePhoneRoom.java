package sa.urtv.screensaver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * «اللعب بالجوالات» (docs/GAMES-PHONES.md): the generic room of the party games. The TV owns the game and decides what each phone shows; a phone only joins,
 * reads ITS OWN card (a secret word, a vote, an actor's buttons) and sends back one number for the card it is looking at. Nothing is kept on disk and
 * nothing of one phone's card is sent to another. No Android, no network: tests/phone_room_test.py runs it on a plain JVM with a fake clock.
 * The seats are the players of the game, in the order they joined; the TV maps seat i to player i.
 */
final class GamePhoneRoom implements PhoneRoom {
    static final int MAX_PLAYERS = 12, ANSWER_PER_SEC = 5, INBOX_MAX = 32, JOIN_PER_MIN = 20, WRONG_CODE_LOCK = 5;
    static final long CONNECTED_MS = 6000, LOCK_MS = 60000, LOBBY_LIFE_MS = 10 * 60 * 1000;
    /** what a card is: a secret to read, a vote for one name, a few action buttons, a plain line to wait on, or a form of one or more short answers to type («text»: `opts` are the field labels) */
    static final String SECRET = "secret", VOTE = "vote", ACT = "act", WAIT = "wait", TEXT = "text";
    /** the action buttons of an «act» card */
    static final int ACT_OK = 0, ACT_SKIP = 1, ACT_FOUL = 2;

    enum Phase { LOBBY, PLAY, CLOSED }

    private static final class Seat {
        final QuizRoom.Player who; long lastSeen; boolean left;
        int cardId; String kind = WAIT, title = "", text = "", note = ""; String[] opts = new String[0]; int[] acts = new int[0]; int self = -1;
        final ArrayDeque<Integer> inbox = new ArrayDeque<>(); final ArrayDeque<String> posted = new ArrayDeque<>(); final ArrayDeque<Long> sent = new ArrayDeque<>();
        Seat(QuizRoom.Player who) { this.who = who; }
    }

    private final LongSupplier clock; private final Random rnd; final String code;
    private Phase phase = Phase.LOBBY; private int version, serial; private long opened, lockedUntil; private int wrongCodes;
    private final List<Seat> seats = new ArrayList<>(); private final ArrayDeque<Long> joins = new ArrayDeque<>();
    private final java.util.LinkedHashSet<String> gone = new java.util.LinkedHashSet<>();

    GamePhoneRoom(String code, LongSupplier clock, Random rnd) { this.code = code; this.clock = clock; this.rnd = rnd; this.opened = clock.getAsLong(); }

    @Override public String code() { return code; }

    // ---------------------------------------------------------------- the phones: join, state, answer, leave

    @Override public synchronized QuizRoom.Result join(String c, String name, int color, String token) {
        long now = clock.getAsLong();
        if (phase == Phase.CLOSED || (phase == Phase.LOBBY && now - opened > LOBBY_LIFE_MS)) return new QuizRoom.Result(false, "closed", null);
        if (now < lockedUntil) return new QuizRoom.Result(false, "locked", null);
        while (!joins.isEmpty() && now - joins.peekFirst() > 60000) joins.pollFirst();
        if (joins.size() >= JOIN_PER_MIN) return new QuizRoom.Result(false, "rate", null);
        joins.addLast(now);
        if (!code.equals(c)) {
            if (++wrongCodes >= WRONG_CODE_LOCK) { lockedUntil = now + LOCK_MS; wrongCodes = 0; }
            return new QuizRoom.Result(false, "code", null);
        }
        wrongCodes = 0;
        Seat known = byToken(token);
        if (known != null) { known.left = false; known.lastSeen = now; version++; return new QuizRoom.Result(true, null, known.who); }   // the same phone coming back keeps its seat
        if (phase != Phase.LOBBY) return new QuizRoom.Result(false, "closed", null);   // a game in play takes no one new
        String n = QuizRoom.cleanName(name);
        if (n.isEmpty()) return new QuizRoom.Result(false, "name", null);
        if (active() >= MAX_PLAYERS) return new QuizRoom.Result(false, "full", null);
        QuizRoom.Player p = new QuizRoom.Player(newToken(), unique(n), color >= 0 && color < QuizRoom.COLORS ? color : seats.size() % QuizRoom.COLORS, seats.size());
        Seat s = new Seat(p); s.lastSeen = now; seats.add(s); version++;
        return new QuizRoom.Result(true, null, p);
    }

    @Override public synchronized String stateFor(String token, boolean en) {
        if (phase == Phase.CLOSED) return "{\"ok\":false,\"e\":\"closed\"}";
        Seat s = byToken(token);
        if (s == null) return gone.contains(token) ? "{\"ok\":false,\"e\":\"gone\"}" : "{\"ok\":false,\"e\":\"token\"}";
        s.lastSeen = clock.getAsLong();
        StringBuilder b = new StringBuilder("{\"ok\":true,\"v\":").append(version).append(",\"ph\":\"").append(phase == Phase.LOBBY ? "lobby" : "play")
            .append("\",\"me\":{\"n\":").append(QuizRoom.esc(s.who.name)).append(",\"k\":").append(s.who.color).append("},\"pl\":").append(active());
        b.append(",\"card\":{\"id\":").append(s.cardId).append(",\"kind\":").append(QuizRoom.esc(s.kind)).append(",\"title\":").append(QuizRoom.esc(s.title))
            .append(",\"text\":").append(QuizRoom.esc(s.text)).append(",\"note\":").append(QuizRoom.esc(s.note)).append(",\"self\":").append(s.self).append(",\"opts\":[");
        for (int i = 0; i < s.opts.length; i++) b.append(i > 0 ? "," : "").append(QuizRoom.esc(s.opts[i]));
        b.append("],\"acts\":[");
        for (int i = 0; i < s.acts.length; i++) b.append(i > 0 ? "," : "").append(s.acts[i]);
        return b.append("]}}").toString();
    }

    /** the phone answers the card it looks at (`q`); a card the TV has already replaced is refused (a stale tap), as is anything the card does not offer */
    @Override public synchronized String answer(String token, int q, int a, String x) {
        Seat s = byToken(token);
        if (s == null) return "token";
        if (phase != Phase.PLAY) return "phase";
        long now = clock.getAsLong();
        while (!s.sent.isEmpty() && now - s.sent.peekFirst() > 1000) s.sent.pollFirst();
        if (s.sent.size() >= ANSWER_PER_SEC) return "rate";
        s.sent.addLast(now);
        if (q != s.cardId) return "stale";
        boolean ok;
        switch (s.kind) {
            case VOTE: ok = a >= 0 && a <= s.opts.length && a != s.self && s.inbox.isEmpty(); break;   // one vote per card; a == opts.length is «ما أدري / ما اتفقنا»
            case ACT: ok = false; for (int t : s.acts) if (t == a) ok = true; break;
            case SECRET: ok = a == 0 && s.inbox.isEmpty(); break;
            case TEXT: {   // the answers, one line per field, each cleaned; one post per card
                String f = form(x, s.opts.length);
                ok = a == 0 && f != null && s.posted.isEmpty();
                if (ok) { s.posted.addLast(f); s.inbox.addLast(0); version++; return null; }
                break;
            }
            default: ok = false;
        }
        if (!ok) return "card";
        if (s.inbox.size() >= INBOX_MAX) s.inbox.pollFirst();
        s.inbox.addLast(a); version++;
        return null;
    }

    @Override public synchronized void leave(String token) { Seat s = byToken(token); if (s != null) { s.left = true; version++; } }

    // ---------------------------------------------------------------- the TV: who is here, what each phone shows, what came back

    synchronized int count() { return seats.size(); }
    synchronized int active() { int n = 0; for (Seat s : seats) if (!s.left) n++; return n; }
    synchronized String name(int seat) { return seats.get(seat).who.name; }
    synchronized int color(int seat) { return seats.get(seat).who.color; }
    synchronized List<String> names() { List<String> r = new ArrayList<>(); for (Seat s : seats) r.add(s.who.name); return r; }
    synchronized boolean connected(int seat) { Seat s = seats.get(seat); return !s.left && clock.getAsLong() - s.lastSeen < CONNECTED_MS; }
    synchronized int version() { return version; }
    synchronized Phase phase() { return phase; }

    /** the lobby ends: nobody new joins (a phone that left is dropped now, so the seats are the players) */
    synchronized void start() {
        if (phase != Phase.LOBBY) return;
        for (int i = seats.size() - 1; i >= 0; i--) if (seats.get(i).left) { gone.add(seats.get(i).who.token); seats.remove(i); }
        phase = Phase.PLAY; version++;
    }

    /** `seat` shows a new card: the old one is gone, anything it had not yet sent is dropped. `self` is the option the phone may not choose (its own name in a vote) or -1. Returns the card's id. */
    synchronized int card(int seat, String kind, String title, String text, String note, String[] opts, int[] acts, int self) {
        Seat s = seats.get(seat);
        s.cardId = ++serial; s.kind = kind; s.title = clip(title); s.text = clip(text); s.note = clip(note);
        s.opts = opts == null ? new String[0] : opts.clone(); s.acts = acts == null ? new int[0] : acts.clone(); s.self = self;
        s.inbox.clear(); s.posted.clear(); version++;
        return s.cardId;
    }
    synchronized void hold(int seat, String text) { card(seat, WAIT, "", text, "", null, null, -1); }
    /** every seat waits on the same line */
    synchronized void waitAll(String text) { for (int i = 0; i < seats.size(); i++) hold(i, text); }

    /** the first answer the phone sent for its current card, or -1 */
    synchronized int reply(int seat) { Integer a = seats.get(seat).inbox.peekFirst(); return a == null ? -1 : a; }
    synchronized boolean replied(int seat) { return !seats.get(seat).inbox.isEmpty(); }
    synchronized int repliedCount() { int n = 0; for (Seat s : seats) if (!s.left && !s.inbox.isEmpty()) n++; return n; }
    /** every answer sent for the current card since the last call, oldest first (an actor's buttons) */
    synchronized List<Integer> take(int seat) { Seat s = seats.get(seat); List<Integer> r = new ArrayList<>(s.inbox); s.inbox.clear(); return r; }

    /** what the phone typed for its current «text» card: one string per field (empty where it left a field empty), or null while nothing has been sent */
    synchronized String[] typed(int seat) {
        Seat s = seats.get(seat);
        if (s.posted.isEmpty()) return null;
        String[] v = s.posted.peekFirst().split("\n", -1), r = new String[Math.max(1, s.opts.length)];
        for (int i = 0; i < r.length; i++) r[i] = i < v.length ? v[i] : "";
        return r;
    }

    synchronized void close() { phase = Phase.CLOSED; version++; seats.clear(); }

    // ---------------------------------------------------------------- inside

    /** the fields of a post: `n` lines (a missing one is empty), each letters, digits and single spaces up to 30 characters; null when every one is empty or the post is too long */
    static String form(String x, int n) {
        if (x == null || x.length() > 600 || n < 1) return null;
        String[] v = x.split("\n", -1);
        StringBuilder b = new StringBuilder(); boolean any = false;
        for (int i = 0; i < n; i++) {
            String f = i < v.length ? field(v[i]) : "";
            if (!f.isEmpty()) any = true;
            b.append(i > 0 ? "\n" : "").append(f);
        }
        return any ? b.toString() : null;
    }
    private static String field(String raw) {
        StringBuilder b = new StringBuilder(); int n = 0;
        for (int i = 0; i < raw.length() && n < 30; ) {
            int cp = raw.codePointAt(i); i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) { b.appendCodePoint(cp); n++; }
            else if (cp == ' ' && b.length() > 0 && b.charAt(b.length() - 1) != ' ') { b.append(' '); n++; }
        }
        return b.toString().trim();
    }

    private Seat byToken(String t) { if (t == null || t.isEmpty()) return null; for (Seat s : seats) if (s.who.token.equals(t)) return s; return null; }
    private static String clip(String s) { return s == null ? "" : s.length() > 200 ? s.substring(0, 200) : s; }
    private String unique(String n) {
        String cand = n; int k = 1;
        while (taken(cand)) { k++; cand = n + " " + QuizRoom.arabicDigits(k); }
        return cand;
    }
    private boolean taken(String n) { for (Seat s : seats) if (s.who.name.equals(n)) return true; return false; }
    private String newToken() { StringBuilder b = new StringBuilder(); for (int i = 0; i < 16; i++) b.append("0123456789abcdefghijklmnopqrstuvwxyz".charAt(rnd.nextInt(36))); return b.toString(); }
}
