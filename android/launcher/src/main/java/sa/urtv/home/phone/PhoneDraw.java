package sa.urtv.home.phone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.3 #53 «اكتب وارسم», the drawing: a paired phone draws (assets/phone/draw.html) and its strokes go to the TV LIVE, as compact batches of points
 * (POST /api/draw about every 50 ms): {"s": [[id, colour, x, y, x, y, …], …]} with x and y whole numbers from 0 to COORD (the phone's canvas
 * scaled to 0..1000, the TV scales them to its screen), or {"clear": true} («امسح», both sides). A stroke's id joins the batches of one line.
 * The TV draws them over the screen in MessageActivity (the same overlay, switches and Back as the text), and the drawing fades FADE_AFTER_MS
 * after the last stroke. Caps: a batch at most MAX_BATCH_STROKES strokes and MAX_BATCH_POINTS points, a stroke MAX_STROKE_POINTS, a drawing
 * MAX_POINTS (more is refused with «draw_full»: «امسح» first); at most RATE batches a second. Plain Java (tests/phone_draw_test.py runs it).
 */
public final class PhoneDraw {
    public static final int COORD = 1000, COLOURS = 6, MAX_ID = 9999;
    /** one entry carries at most ENTRY_POINTS points (the JSON reader takes 64 items in a list); a longer run is split, the same id twice */
    public static final int ENTRY_POINTS = 30;
    public static final int MAX_BATCH_STROKES = 8, MAX_BATCH_POINTS = 240, MAX_STROKE_POINTS = 1500, MAX_POINTS = 6000, MAX_STROKES = 400;
    public static final long FADE_AFTER_MS = 8_000, FADE_MS = 1_000;
    /** batches a second (the page sends about 20) */
    public static final int RATE = 30;
    /** the six colours (ARGB), in the page's order: white, yellow, red, green, blue, pink */
    public static final int[] PALETTE = {0xFFFFFFFF, 0xFFFFD54F, 0xFFEF5350, 0xFF66BB6A, 0xFF42A5F5, 0xFFF06292};

    public enum Verdict { SHOW, OFF, BUSY_APP, SLOW_DOWN, FULL, EMPTY }

    /** One line: its colour and its points (x, y pairs, 0..COORD). */
    public static final class Stroke {
        public final int colour;
        int[] pts = new int[32];
        int n;
        Stroke(int colour) { this.colour = colour; }
        public int points() { return n / 2; }
        public int x(int i) { return pts[2 * i]; }
        public int y(int i) { return pts[2 * i + 1]; }
        void add(int x, int y) {
            if (n + 2 > pts.length) pts = java.util.Arrays.copyOf(pts, pts.length * 2);
            pts[n++] = x;
            pts[n++] = y;
        }
    }

    private final Map<Integer, Stroke> strokes = new LinkedHashMap<>();
    private int total;
    private long lastAt = Long.MIN_VALUE;
    private int version;
    private final long[] sent = new long[RATE];
    private int sentN;

    /** the TV's one drawing (the phone's batches go in, MessageActivity draws it) */
    public static final PhoneDraw TV = new PhoneDraw();

    public static PhoneDraw board() { return new PhoneDraw(); }

    /** Schema's check of a batch (the values as the JSON reader gives them: Long numbers, Lists): the shape and every cap, nothing drawn. */
    public static boolean valid(Object s) {
        if (!(s instanceof List)) return false;
        List<?> list = (List<?>) s;
        if (list.isEmpty() || list.size() > MAX_BATCH_STROKES) return false;
        int points = 0;
        for (Object o : list) {
            if (!(o instanceof List)) return false;
            List<?> st = (List<?>) o;
            if (st.size() < 4 || st.size() % 2 != 0 || st.size() > 2 + 2 * ENTRY_POINTS) return false;   // id, colour, then whole x, y pairs (at least one point)
            for (int i = 0; i < st.size(); i++) {
                Object v = st.get(i);
                if (!(v instanceof Long)) return false;
                long x = (Long) v;
                long max = i == 0 ? MAX_ID : i == 1 ? COLOURS - 1 : COORD;
                if (x < 0 || x > max) return false;
            }
            points += (st.size() - 2) / 2;
        }
        return points <= MAX_BATCH_POINTS;
    }

    /** May this batch be drawn now? on, overApps, ourScreen as for the text (PhoneMessage); now: elapsedRealtime. A SHOW is counted for the rate. */
    public synchronized Verdict decide(boolean on, boolean overApps, boolean ourScreen, long now) {
        if (!on) return Verdict.OFF;
        if (!overApps && !ourScreen) return Verdict.BUSY_APP;
        int recent = 0;
        for (int i = 0; i < sentN; i++) if (now - sent[i] < 1000) recent++;
        if (recent >= RATE) return Verdict.SLOW_DOWN;
        long[] keep = new long[RATE];
        int k = 0;
        for (int i = 0; i < sentN; i++) if (now - sent[i] < 1000 && k < RATE - 1) keep[k++] = sent[i];
        keep[k++] = now;
        System.arraycopy(keep, 0, sent, 0, k);
        sentN = k;
        return Verdict.SHOW;
    }

    /** A checked batch onto the drawing; FULL (nothing added) when it would pass MAX_POINTS, MAX_STROKES or a stroke's MAX_STROKE_POINTS. */
    public synchronized Verdict add(List<?> batch, long now) {
        if (batch == null || !valid(batch)) return Verdict.EMPTY;
        if (now - lastAt > FADE_AFTER_MS + FADE_MS && lastAt != Long.MIN_VALUE) clearLocked();   // the last drawing faded away: a new one
        int adding = 0;
        Map<Integer, Integer> grow = new LinkedHashMap<>();   // per id: its points after this batch (an id may come twice: a long run split)
        for (Object o : batch) {
            List<?> st = (List<?>) o;
            int id = ((Long) st.get(0)).intValue(), pts = (st.size() - 2) / 2;
            Stroke s = strokes.get(id);
            int after = (grow.containsKey(id) ? grow.get(id) : s == null ? 0 : s.points()) + pts;
            if (after > MAX_STROKE_POINTS) return Verdict.FULL;
            grow.put(id, after);
            adding += pts;
        }
        int fresh = 0;
        for (Integer id : grow.keySet()) if (!strokes.containsKey(id)) fresh++;
        if (total + adding > MAX_POINTS || strokes.size() + fresh > MAX_STROKES) return Verdict.FULL;
        for (Object o : batch) {
            List<?> st = (List<?>) o;
            int id = ((Long) st.get(0)).intValue(), colour = ((Long) st.get(1)).intValue();
            Stroke s = strokes.get(id);
            if (s == null) { s = new Stroke(colour); strokes.put(id, s); }
            for (int i = 2; i + 1 < st.size(); i += 2) s.add(((Long) st.get(i)).intValue(), ((Long) st.get(i + 1)).intValue());
        }
        total += adding;
        lastAt = now;
        version++;
        return Verdict.SHOW;
    }

    /** «امسح»: the drawing is gone on the TV too. */
    public synchronized void clear() { clearLocked(); version++; }

    private void clearLocked() { strokes.clear(); total = 0; lastAt = Long.MIN_VALUE; }

    /** The strokes as they are now (copies of the lists' order; the strokes themselves are only ever appended to). */
    public synchronized List<Stroke> strokes() { return new ArrayList<>(strokes.values()); }

    public synchronized int total() { return total; }

    public synchronized int version() { return version; }

    /** 1 while drawn, falling to 0 over FADE_MS from FADE_AFTER_MS after the last stroke; 0 with nothing drawn. */
    public synchronized float alpha(long now) {
        if (lastAt == Long.MIN_VALUE || strokes.isEmpty()) return 0f;
        long since = now - lastAt;
        if (since <= FADE_AFTER_MS) return 1f;
        if (since >= FADE_AFTER_MS + FADE_MS) return 0f;
        return 1f - (since - FADE_AFTER_MS) / (float) FADE_MS;
    }

    /** The word the phone page shows for a verdict. */
    public static String code(Verdict v) {
        switch (v) {
            case SHOW: return "draw_shown";
            case OFF: return "message_off";
            case BUSY_APP: return "message_busy_app";
            case FULL: return "draw_full";
            case EMPTY: return "bad_request";
            default: return "slow_down";
        }
    }
}
