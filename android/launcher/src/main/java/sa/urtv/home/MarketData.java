package sa.urtv.home;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * The markets page's data (docs/PROPOSALS-NOV2.md §6), from the one file CI keeps (.github/tools/market_prices.py, branch data-markets): the
 * same download the page's markets line and stocks card use, nothing else. Plain Java (no Android): tests/MarketDataRun.java runs it.
 *
 * <p>What it gives: each symbol's price, the close before and the day's 15-minute closes (the day's open and range are worked out from
 * those), and, when CI kept them from the same Yahoo answer, the volume and the 52-week range. Nothing here asks for anything else: the
 * sectors, the whole market's breadth and turnover, dividends and news have no free source, so the page has no card for them.
 */
final class MarketData {
    /** the page's MARKET_URL (src/template.html): the same file */
    static final String URL = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/markets.json";
    /** a year of daily closes per symbol (CI, once a day; «أسبوع / شهر / سنة»): a file of its own, so only a TV that opens this page downloads it */
    static final String DAILY_URL = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/daily.json";
    /** the riyal's peg to the dollar: fixed by the central bank, not fetched */
    static final double PEG = 3.75;
    /** grams in a troy ounce: gold and silver are quoted per ounce in dollars */
    static final double OUNCE_G = 31.1034768;
    static final String TASI = "^TASI.SR", GOLD = "GC=F", SILVER = "SI=F", BRENT = "BZ=F";

    /** the page's TADAWUL (src/template.html, tests/markets_page_test.py checks they agree): the 30 largest companies, in its order */
    static final String[][] TADAWUL = {
        {"2222", "أرامكو"}, {"1120", "الراجحي"}, {"1180", "الأهلي"}, {"2010", "سابك"}, {"7010", "stc"}, {"1211", "معادن"}, {"2082", "أكوا"},
        {"5110", "كهرباء السعودية"}, {"1150", "الإنماء"}, {"1060", "ساب"}, {"1010", "بنك الرياض"}, {"1050", "السعودي الفرنسي"}, {"1140", "البلاد"},
        {"1020", "الجزيرة"}, {"1080", "العربي"}, {"2280", "المراعي"}, {"2380", "بترورابغ"}, {"2310", "سبكيم"}, {"2020", "سافكو"}, {"2290", "ينساب"},
        {"2350", "كيان"}, {"4013", "الحبيب"}, {"4190", "جرير"}, {"7020", "موبايلي"}, {"7030", "زين"}, {"7203", "علم"}, {"4030", "البحري"},
        {"1810", "سيرا"}, {"8010", "التعاونية"}, {"4300", "دار الأركان"},
    };

    /** The fixed pegs against the riyal (riyals for one unit): they need no source. Names as the screens show them. */
    static final String[][] PEGS = {{"الدولار", "3.7500"}, {"الدرهم الإماراتي", "1.0211"}, {"الريال القطري", "1.0302"}, {"الدينار البحريني", "9.9734"}};

    /** One symbol's quote. */
    static final class Quote {
        final double p;          // the price
        final double v;          // the close before (NaN: not known)
        final double[] c;        // the day's closes, oldest first (at most 30)
        final long t;            // when CI last confirmed it (ms, 0: not said)
        final double vol, yh, yl;   // volume and the 52-week range, when CI kept them (NaN: not kept)
        final double[] d;        // a year of daily closes, oldest first (CI asks once a day; empty: none)
        final long df, dl;       // the first and the last day of d (UTC midnight ms, 0: not said)

        Quote(double p, double v, double[] c, long t, double vol, double yh, double yl) {
            this(p, v, c, t, vol, yh, yl, new double[0], 0, 0);
        }

        Quote(double p, double v, double[] c, long t, double vol, double yh, double yl, double[] d, long df, long dl) {
            this.p = p; this.v = v; this.c = c; this.t = t; this.vol = vol; this.yh = yh; this.yl = yl; this.d = d; this.df = df; this.dl = dl;
        }

        boolean hasPrev() { return !Double.isNaN(v) && v > 0; }
        double change() { return hasPrev() ? p - v : Double.NaN; }
        double pct() { return hasPrev() ? (p - v) / v * 100 : Double.NaN; }
        /** the first of the day's closes, the nearest to the open we have */
        double open() { return c.length > 0 ? c[0] : Double.NaN; }
        double low() { double m = p; for (double x : c) m = Math.min(m, x); return m; }
        double high() { double m = p; for (double x : c) m = Math.max(m, x); return m; }
        /** up for the day: by the close before, else by the day's own line; null when neither says */
        Boolean up() {
            if (hasPrev()) return p >= v;
            return c.length > 1 ? c[c.length - 1] >= c[0] : null;
        }
    }

    final long updated;
    final Map<String, Quote> quotes;

    // ---- live mode («مباشر»): the TV's own quotes for the symbols on screen, only while the page is in front; kept in memory, never stored

    /** one live answer: the price, the close before (NaN: none) and when Yahoo stamps it (ms) */
    static final class Live {
        final double p, v;
        final long at;
        Live(double p, double v, long at) { this.p = p; this.v = v; this.at = at; }
    }

    /** the seconds between two live asks: 30 at first, 60 after one failure, 120 after two or more (the CI file is what shows meanwhile) */
    static long liveDelay(int fails) { return fails <= 0 ? 30_000 : fails == 1 ? 60_000 : 120_000; }

    /** the same on a weak TV (Ram.low; a user report on 4.1, lag): never more often than every 60 s */
    static long liveDelay(int fails, boolean lowRam) { return lowRam ? Math.max(60_000, liveDelay(fails)) : liveDelay(fails); }

    /** «مباشر» is said only while the last good live answer is younger than this (two asks of the slowest pace would have passed otherwise) */
    static boolean liveFresh(long okAt, long now) { return okAt > 0 && now >= okAt && now - okAt <= 150_000; }

    /** how a live row is labelled: Tadawul's data is delayed at the source («متأخرة ~١٥ د»), any other shows the quote's time */
    static String liveKind(String code) { return code != null && code.endsWith(".SR") ? "delayed" : "time"; }

    /**
     * Yahoo's spark answer ({"spark":{"result":[{"symbol", "response":[{"meta":{regularMarketPrice, chartPreviousClose, regularMarketTime (s)},
     * "timestamp":[…], "indicators":{"quote":[{"close":[…]}]}}]}]}}) -> {symbol: Live}. The price is the meta's, else the last close; the time is the meta's,
     * else the last timestamp. A row with no usable price is skipped.
     */
    static Map<String, Live> parseLive(String json) {
        Map<String, Live> out = new LinkedHashMap<>();
        if (json == null) return out;
        try {
            Object root = new Reader(json).read();
            if (!(root instanceof Map)) return out;
            Map<?, ?> top = (Map<?, ?>) root;
            Object sp = top.get("spark");
            Object list = sp instanceof Map ? ((Map<?, ?>) sp).get("result") : null;
            if (list instanceof List) {   // shape 1: spark.result[].response[].meta
                for (Object o : (List<?>) list) {
                    if (!(o instanceof Map) || !(((Map<?, ?>) o).get("symbol") instanceof String)) continue;
                    Object resp = ((Map<?, ?>) o).get("response");
                    Object first = resp instanceof List && !((List<?>) resp).isEmpty() ? ((List<?>) resp).get(0) : null;
                    if (!(first instanceof Map)) continue;
                    Map<?, ?> r = (Map<?, ?>) first;
                    Map<?, ?> meta = r.get("meta") instanceof Map ? (Map<?, ?>) r.get("meta") : new LinkedHashMap<String, Object>();
                    Live l = liveOf(meta, lastCloses(r), r.get("timestamp"));
                    if (l != null) out.put((String) ((Map<?, ?>) o).get("symbol"), l);
                }
            } else {   // shape 2: the map keyed by symbol ({"2222.SR": {"timestamp": [...], "close": [...], "chartPreviousClose": n, ...}, ...})
                for (Map.Entry<?, ?> e : top.entrySet()) {
                    if (!(e.getKey() instanceof String) || !(e.getValue() instanceof Map)) continue;
                    Map<?, ?> r = (Map<?, ?>) e.getValue();
                    Live l = liveOf(r, numbers(r.get("close")), r.get("timestamp"));
                    if (l != null) out.put((String) e.getKey(), l);
                }
            }
        } catch (RuntimeException e) {
            out.clear();
        }
        return out;
    }

    /**
     * True when this live answer says what the page already shows: every symbol in it has the same price and the same close before (and the same quote time, except
     * where the source's delay is told in words, not by a time). A live tick that changes nothing (the exchange is closed, or quiet) draws nothing again.
     */
    static boolean liveSame(Map<String, Live> shown, Map<String, Live> now) {
        for (Map.Entry<String, Live> e : now.entrySet()) {
            Live o = shown.get(e.getKey()), l = e.getValue();
            if (o == null || Double.compare(o.p, l.p) != 0 || Double.compare(o.v, l.v) != 0) return false;
            if (!"delayed".equals(liveKind(e.getKey())) && o.at != l.at) return false;
        }
        return true;
    }

    /** one symbol's live price from its fields (a meta, or the symbol's own map), its closes and its timestamps; null without a usable price */
    private static Live liveOf(Map<?, ?> meta, double[] closes, Object timestamps) {
        double p = num(meta.get("regularMarketPrice"));
        if (Double.isNaN(p) && closes.length > 0) p = closes[closes.length - 1];
        if (Double.isNaN(p)) p = num(meta.get("fulldayPrice"));   // Tadawul in market hours: {"timestamp":[],"close":null,"fulldayPrice":9876.13} (step 28b: parsed=0, then one symbol of seven)
        if (Double.isNaN(p) || p <= 0) return null;
        double prev = num(meta.get("chartPreviousClose"));
        if (Double.isNaN(prev)) prev = num(meta.get("previousClose"));
        double t = num(meta.get("regularMarketTime"));
        if (Double.isNaN(t) && timestamps instanceof List && !((List<?>) timestamps).isEmpty()) {
            List<?> ts = (List<?>) timestamps;
            t = num(ts.get(ts.size() - 1));
        }
        if (Double.isNaN(t)) t = System.currentTimeMillis() / 1000.0;   // an answer with no time at all is as of now (the provider's delay is said by the page: «متأخرة»)
        return new Live(p, prev, (long) t * 1000L);
    }

    private static double[] lastCloses(Map<?, ?> r) {
        Object ind = r.get("indicators");
        Object q = ind instanceof Map ? ((Map<?, ?>) ind).get("quote") : null;
        Object first = q instanceof List && !((List<?>) q).isEmpty() ? ((List<?>) q).get(0) : null;
        return first instanceof Map ? numbers(((Map<?, ?>) first).get("close")) : new double[0];
    }

    /** these quotes with the live prices put over the file's (the day's line, volume and the year are the file's); what is live is in liveAt */
    MarketData withLive(Map<String, Live> live) {
        Map<String, Quote> out = new LinkedHashMap<>(quotes);
        MarketData d = new MarketData(updated, out);
        d.src = src;
        d.liveAt = new LinkedHashMap<>(liveAt);
        for (Map.Entry<String, Live> e : live.entrySet()) {
            Quote q = quotes.get(e.getKey());
            if (q == null) continue;
            Live l = e.getValue();
            out.put(e.getKey(), new Quote(l.p, l.v > 0 ? l.v : q.v, q.c, q.t, q.vol, q.yh, q.yl, q.d, q.df, q.dl));
            d.liveAt.put(e.getKey(), l.at);
        }
        return d;
    }

    /** symbol -> when its live price was stamped (ms); empty for a file's own */
    Map<String, Long> liveAt = new LinkedHashMap<>();

    /** who the prices are from, as the file says ("yahoo" when it says nothing): "yahoo", "stooq" or "yahoo+stooq" */
    String src = "yahoo";

    private MarketData(long updated, Map<String, Quote> quotes) { this.updated = updated; this.quotes = quotes; }

    /** the name a person reads under the prices («المصدر: Yahoo Finance»): a provider's own name, not translated */
    String sourceName() {
        return "stooq".equals(src) ? "Stooq" : "yahoo+stooq".equals(src) ? "Yahoo Finance + Stooq" : "Yahoo Finance";
    }

    /** The file's contents, or null when it is not the file (an error page, a cut download). */
    static MarketData parse(String json) {
        if (json == null) return null;
        try {
            Object root = new Reader(json).read();
            if (!(root instanceof Map)) return null;
            Map<?, ?> m = (Map<?, ?>) root;
            Object q = m.get("quotes");
            if (!(q instanceof Map)) return null;
            Map<String, Quote> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) q).entrySet()) {
                if (!(e.getKey() instanceof String) || !(e.getValue() instanceof Map)) continue;
                Map<?, ?> x = (Map<?, ?>) e.getValue();
                double p = num(x.get("p"));
                if (Double.isNaN(p) || p <= 0) continue;
                out.put((String) e.getKey(), new Quote(p, num(x.get("v")), numbers(x.get("c")), time(x.get("t")), num(x.get("vol")), num(x.get("yh")), num(x.get("yl"))));
            }
            MarketData d = new MarketData(time(m.get("updated")), out);
            if (m.get("src") instanceof String && ((String) m.get("src")).matches("yahoo|stooq|yahoo\\+stooq")) d.src = (String) m.get("src");
            return d;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** These quotes with the year's daily closes of CI's daily file (null or not that file: as they are). */
    MarketData withDaily(String dailyJson) {
        if (dailyJson == null) return this;
        Map<?, ?> days;
        try {
            Object root = new Reader(dailyJson).read();
            if (!(root instanceof Map) || !(((Map<?, ?>) root).get("daily") instanceof Map)) return this;
            days = (Map<?, ?>) ((Map<?, ?>) root).get("daily");
        } catch (RuntimeException e) {
            return this;
        }
        Map<String, Quote> out = new LinkedHashMap<>();
        for (Map.Entry<String, Quote> e : quotes.entrySet()) {
            Quote q = e.getValue();
            Object x = days.get(e.getKey());
            if (x instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) x;
                q = new Quote(q.p, q.v, q.c, q.t, q.vol, q.yh, q.yl, numbers(m.get("d")), day(m.get("df")), day(m.get("dl")));
            }
            out.put(e.getKey(), q);
        }
        MarketData d = new MarketData(updated, out);
        d.src = src;
        d.liveAt = liveAt;
        return d;
    }

    private static double num(Object o) { return o instanceof Double ? (Double) o : Double.NaN; }

    /** the numbers of a list (anything else left out) */
    private static double[] numbers(Object o) {
        List<Double> l = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<?>) o) { double v = num(x); if (!Double.isNaN(v) && v > 0) l.add(v); }
        double[] out = new double[l.size()];
        for (int i = 0; i < out.length; i++) out[i] = l.get(i);
        return out;
    }

    /** "2026-11-02" to that day's UTC midnight in ms; 0 for anything else */
    static long day(Object o) {
        return o instanceof String && ((String) o).matches("\\d{4}-\\d\\d-\\d\\d") ? time(o + "T00:00:00Z") : 0;
    }

    // ---------------------------------------------------------------- the ranges: «اليوم» «أسبوع» «شهر» «سنة»

    static final int TODAY = 0, WEEK = 1, MONTH = 2, YEAR = 3;
    /** trading days in a week and in a month (Tadawul and New York both trade five days a week) */
    static final int WEEK_DAYS = 5, MONTH_DAYS = 21;

    /** One range of a quote: its line, the value it is measured from (the dashed line), its first day (the year's only, else 0) and its last. */
    static final class Range {
        final double[] line;
        final double base;
        final long from, to;
        Range(double[] line, double base, long from, long to) { this.line = line; this.base = base; this.from = from; this.to = to; }
        double change(double p) { return Double.isNaN(base) ? Double.NaN : p - base; }
        double pct(double p) { return Double.isNaN(base) || base <= 0 ? Double.NaN : (p - base) / base * 100; }
    }

    /** Does the quote have the daily closes the week, month and year need? */
    static boolean hasRanges(Quote q) { return q != null && q.d.length > 1; }

    /**
     * The quote over a range. «اليوم»: the day's closes against the close before. The others: the last days of the year's line (and today's
     * price after them when it is newer than the line's last day), measured from the close the range starts at.
     */
    static Range range(Quote q, int which) {
        if (which == TODAY || !hasRanges(q)) return new Range(q.c, q.v, 0, 0);
        boolean newer = q.dl > 0 && q.t > 0 && q.t >= q.dl + 86_400_000L;   // today's price is past the line's last day: it ends the line
        int days = which == WEEK ? WEEK_DAYS : which == MONTH ? MONTH_DAYS : q.d.length;
        int from = Math.max(0, q.d.length - (newer ? days : days + 1));
        double[] line = new double[q.d.length - from + (newer ? 1 : 0)];
        System.arraycopy(q.d, from, line, 0, q.d.length - from);
        if (newer) line[line.length - 1] = q.p;
        // its first day is known for the year only (the file keeps no date per point): the week and the month say «قبل أسبوع», «قبل شهر»
        return new Range(line, line[0], which == YEAR ? q.df : 0, newer ? q.t : q.dl);
    }

    /** "2026-11-02T21:15:44+00:00" (as CI writes it) to ms; 0 for anything else */
    static long time(Object o) {
        if (!(o instanceof String)) return 0;
        String s = (String) o;
        if (!s.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d([+-]\\d\\d:\\d\\d|Z)")) return 0;
        Calendar k = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        k.clear();
        k.set(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)) - 1, Integer.parseInt(s.substring(8, 10)),
                Integer.parseInt(s.substring(11, 13)), Integer.parseInt(s.substring(14, 16)), Integer.parseInt(s.substring(17, 19)));
        long off = 0;
        if (!s.endsWith("Z")) {
            int sign = s.charAt(19) == '-' ? -1 : 1;
            off = sign * (Integer.parseInt(s.substring(20, 22)) * 3600_000L + Integer.parseInt(s.substring(23, 25)) * 60_000L);
        }
        return k.getTimeInMillis() - off;
    }

    // ---------------------------------------------------------------- symbols and names

    /** a Tadawul code is 4 digits; a US ticker letters (as the page's watchlist takes them) */
    static boolean isSaudi(String code) { return code != null && code.matches("\\d{4}"); }

    static boolean validCode(String code) { return code != null && code.matches("\\d{4}|[A-Z][A-Z.\\-]{0,5}"); }

    /** the file's key for a watchlist code: 2222 -> 2222.SR, AAPL -> AAPL */
    static String symbol(String code) { return isSaudi(code) ? code + ".SR" : code; }

    Quote of(String code) { return code == null ? null : quotes.get(symbol(code)); }

    /** the short Arabic name of one of the 30, else the code itself (a US ticker, another Tadawul code) */
    static String name(String code) {
        for (String[] t : TADAWUL) if (t[0].equals(code)) return t[1];
        return code;
    }

    /** a code the screens offer: one of the 30, or any 4-digit code or US ticker that the file has a price for */
    boolean known(String code) {
        if (!validCode(code)) return false;
        for (String[] t : TADAWUL) if (t[0].equals(code)) return true;
        return of(code) != null;
    }

    /** the codes the file has a price for, Tadawul's first, as the phone's search lists them */
    List<String> codes() {
        List<String> out = new ArrayList<>();
        for (String[] t : TADAWUL) out.add(t[0]);
        for (String k : quotes.keySet()) {
            String code = k.endsWith(".SR") ? k.substring(0, k.length() - 3) : k;
            if (validCode(code) && !out.contains(code)) out.add(code);
        }
        return out;
    }

    // ---------------------------------------------------------------- the 30

    /** «اتجاه السوق» over the 30 companies we fetch (and only those): {up, unchanged, down}; a company with no close before is left out */
    int[] breadth() {
        int up = 0, same = 0, down = 0;
        for (String[] t : TADAWUL) {
            Quote q = of(t[0]);
            if (q == null || !q.hasPrev()) continue;
            double d = Math.round(q.pct() * 100) / 100.0;
            if (d > 0) up++; else if (d < 0) down++; else same++;
        }
        return new int[] {up, same, down};
    }

    /** the n that rose most (up) or fell most, of the 30 */
    List<String> movers(boolean up, int n) {
        List<String> all = new ArrayList<>();
        for (String[] t : TADAWUL) {
            Quote q = of(t[0]);
            if (q != null && q.hasPrev() && (up ? q.pct() > 0 : q.pct() < 0)) all.add(t[0]);
        }
        Collections.sort(all, (a, b) -> Double.compare(up ? of(b).pct() : of(a).pct(), up ? of(a).pct() : of(b).pct()));
        return all.subList(0, Math.min(n, all.size()));
    }

    /** «الأكثر نشاطًا» of the 30 by volume; empty while the file has no volume (CI keeps it from the same answer) */
    List<String> active(int n) {
        List<String> all = new ArrayList<>();
        for (String[] t : TADAWUL) {
            Quote q = of(t[0]);
            if (q != null && !Double.isNaN(q.vol) && q.vol > 0) all.add(t[0]);
        }
        Collections.sort(all, (a, b) -> Double.compare(of(b).vol, of(a).vol));
        return all.subList(0, Math.min(n, all.size()));
    }

    // ---------------------------------------------------------------- Tadawul's hours (public facts)

    /** Riyadh is UTC+3 all year */
    static final long RIYADH = 3 * 3600_000L;
    /** continuous trading 10:00 to 15:00, Sunday to Thursday; the closing auction runs to 15:10 (Ramadan has its own hours, not known here) */
    static final int OPEN_MIN = 600, CLOSE_MIN = 900, AUCTION_END_MIN = 910;

    /** Riyadh's day of the week (Calendar.SUNDAY..SATURDAY) and minute of the day */
    private static int[] riyadh(long ms) {
        Calendar k = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        k.setTimeInMillis(ms + RIYADH);
        return new int[] {k.get(Calendar.DAY_OF_WEEK), k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE)};
    }

    private static boolean tradingDay(int dow) { return dow >= Calendar.SUNDAY && dow <= Calendar.THURSDAY; }

    /** trading now (the page's marketOpen(true)) */
    static boolean open(long ms) {
        int[] r = riyadh(ms);
        return tradingDay(r[0]) && r[1] >= OPEN_MIN && r[1] < CLOSE_MIN;
    }

    /** the next opening (ms), from a moment the market is closed */
    static long nextOpen(long ms) {
        long day = (ms + RIYADH) / 86_400_000L * 86_400_000L - RIYADH;   // Riyadh's midnight of today, in UTC ms
        for (int i = 0; i < 8; i++) {
            long at = day + i * 86_400_000L + OPEN_MIN * 60_000L;
            if (at > ms && tradingDay(riyadh(at)[0])) return at;
        }
        return ms;
    }

    /** today's close (ms), while it is open */
    static long closeAt(long ms) {
        long day = (ms + RIYADH) / 86_400_000L * 86_400_000L - RIYADH;
        return day + CLOSE_MIN * 60_000L;
    }

    /** {hours, minutes} from now to then, rounded up to the minute */
    static int[] until(long now, long then) {
        long m = Math.max(0, (then - now + 59_999) / 60_000);
        return new int[] {(int) (m / 60), (int) (m % 60)};
    }

    /** the weekday of a moment in Riyadh, Calendar.SUNDAY..SATURDAY */
    static int weekday(long ms) { return riyadh(ms)[0]; }

    // ---------------------------------------------------------------- numbers

    /** {@code v} with {@code dec} decimals and thousands grouped, in Western digits with «,» and «.» (Digits converts them for the screen) */
    static String num(double v, int dec) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "";
        String s = String.format(java.util.Locale.ROOT, "%." + dec + "f", Math.abs(v));
        int dot = s.indexOf('.');
        String whole = dot < 0 ? s : s.substring(0, dot), frac = dot < 0 ? "" : s.substring(dot);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) b.append(',');
            b.append(whole.charAt(i));
        }
        boolean zero = s.replaceAll("[0.,]", "").isEmpty();
        return (v < 0 && !zero ? "-" : "") + b + frac;
    }

    /** the same with its sign always written: +44.57, −1.20 (a true minus, as the mockups) */
    static String signed(double v, int dec) {
        String s = num(v, dec);
        if (s.isEmpty()) return s;
        return s.startsWith("-") ? "−" + s.substring(1) : "+" + s;
    }

    // ---- «صرف العملات»: rates against the dollar, the converter's maths (pure; the names and the screen are MarketsActivity's)

    /** the currencies the page can show that are not pegged: the ones CI asks Yahoo for (market_prices.py FX_CODES), and what the app asks itself when CI's file is missing */
    static final String[] FX_CODES = {"KWD", "EGP", "EUR", "GBP", "TRY", "INR", "PKR", "IQD", "MAD", "DZD", "TND", "LYD", "SDG", "YER", "LBP", "SYP", "MRU", "PHP", "IDR", "BDT", "LKR", "NPR",
        "MYR", "ETB", "KES", "JPY", "CNY", "CHF", "CAD", "AUD", "SGD", "HKD", "KRW", "RUB", "ZAR"};

    /** the fx file's text for rates read live: {"updated":"<UTC time>","rates":{"KWD":0.307,...}}, only positive numbers; "" when there are none */
    static String fxFile(Map<String, Double> rates, long nowMs) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Double> e : rates.entrySet()) {
            if (!e.getKey().matches("[A-Z]{3}") || !(e.getValue() > 0) || Double.isInfinite(e.getValue())) continue;
            b.append(b.length() > 0 ? "," : "").append('"').append(e.getKey()).append("\":").append(new java.math.BigDecimal(e.getValue()).round(new java.math.MathContext(6)).stripTrailingZeros().toPlainString());
        }
        if (b.length() == 0) return "";
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return "{\"updated\":\"" + f.format(new java.util.Date(nowMs)) + "\",\"rates\":{" + b + "}}";
    }

    /** CI's file (or the app's own copy) is today's while it is younger than this; older, missing or with no rate, the app asks Yahoo itself (step 182) */
    static final long FX_STALE_MS = 48 * 3600_000L;

    /** True when this fx text (CI's file, the copy here, or null) cannot stand for today's rates: no rate in it, no time, or older than FX_STALE_MS. */
    static boolean fxNeedsOwn(String json, long now) {
        if (parseFx(json).isEmpty()) return true;
        long at = fxUpdated(json);
        return at <= 0 || now - at > FX_STALE_MS;
    }

    static final String FX_URL = "https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/fx.json";

    /** Currencies whose rate to the dollar never moves (units per US dollar): they are never fetched, and the screen says «مربوط بالدولار». */
    static final String[][] FX_PEGS = {{"SAR", "3.75"}, {"AED", "3.6725"}, {"QAR", "3.64"}, {"BHD", "0.376"}, {"OMR", "0.3845"}, {"JOD", "0.709"}};

    static boolean pegged(String ccy) {
        for (String[] p : FX_PEGS) if (p[0].equals(ccy)) return true;
        return false;
    }

    /** Units of this currency for one US dollar; NaN when nothing is known (a currency the file has no rate for): the screen shows «—», never 0. */
    static double perUsd(String ccy, Map<String, Double> rates) {
        if ("USD".equals(ccy)) return 1;
        for (String[] p : FX_PEGS) if (p[0].equals(ccy)) return Double.parseDouble(p[1]);
        Double r = rates == null ? null : rates.get(ccy);
        return r == null || Double.isNaN(r) || r <= 0 ? Double.NaN : r;
    }

    /**
     * A gram of gold in this currency (4.1 reports: no gold price in KWD or BHD anywhere): the ounce in dollars (GC=F) over OUNCE_G, times the
     * currency's units per dollar (the same rates as the list); NaN when either is unknown, so the line is left out.
     */
    static double goldGram(double ounceUsd, String ccy, Map<String, Double> rates) {
        double per = perUsd(ccy, rates);
        if (Double.isNaN(ounceUsd) || ounceUsd <= 0 || Double.isNaN(per)) return Double.NaN;
        return ounceUsd / OUNCE_G * per;
    }

    /** An amount of `from` in `to`, through the dollar (both directions use the same two numbers); NaN when either rate is unknown. */
    static double convert(double amount, String from, String to, Map<String, Double> rates) {
        double a = perUsd(from, rates), b = perUsd(to, rates);
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(amount)) return Double.NaN;
        return amount / a * b;
    }

    /** The fx file CI writes ({"updated": "...", "rates": {"KWD": 0.307, ...}}, units per dollar): the rates that are positive numbers; empty for anything else. */
    static Map<String, Double> parseFx(String json) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (json == null) return out;
        try {
            Object root = new Reader(json).read();
            Object r = root instanceof Map ? ((Map<?, ?>) root).get("rates") : null;
            if (!(r instanceof Map)) return out;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) r).entrySet()) {
                double v = num(e.getValue());
                if (e.getKey() instanceof String && ((String) e.getKey()).matches("[A-Z]{3}") && !Double.isNaN(v) && v > 0) out.put((String) e.getKey(), v);
            }
        } catch (RuntimeException e) {
            out.clear();
        }
        return out;
    }

    /** When the fx file says it was made (ms), 0 for anything else. */
    static long fxUpdated(String json) {
        try {
            Object root = json == null ? null : new Reader(json).read();
            return root instanceof Map ? time(((Map<?, ?>) root).get("updated")) : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** A rate or a converted amount for the screen: whole above 1000, two decimals above 1, four above 0.01, six under; «—» for NaN. Western digits (the caller turns them). */
    static String fxNumber(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "—";
        if (v == 0) return "0";   // nothing converts to «0», not to «0.000000»
        String s = v >= 1000 ? String.format(java.util.Locale.ROOT, "%,.0f", v) : v >= 1 ? String.format(java.util.Locale.ROOT, "%,.2f", v)
            : v >= 0.01 ? String.format(java.util.Locale.ROOT, "%.4f", v) : String.format(java.util.Locale.ROOT, "%.6f", v);
        return s;
    }

    /** The amount as it is typed in the converter, with thousands separators in its whole part and the rest as typed («1234.50» -> «1,234.50», «1234.» -> «1,234.»); Western digits. */
    static String typedAmount(String raw) {
        if (raw == null || raw.isEmpty()) return "0";
        int dot = raw.indexOf('.');
        String whole = dot < 0 ? raw : raw.substring(0, dot), rest = dot < 0 ? "" : raw.substring(dot);
        StringBuilder g = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) g.append(',');
            g.append(whole.charAt(i));
        }
        return g + rest;
    }

    /** A signed number, its point and its percent sign kept in reading order inside right-to-left text («−٠٫٣٣٪», not «−(٪٠٫٣٣−)»): a left-to-right isolate around it */
    /**
     * The price line's change, «−٦٫٧٢  (−٠٫٠٧٪)»: each number in its own isolate («−٦٫٧٢» and «(−٠٫٠٧٪)»), the two spaces between them outside. One isolate
     * around the whole made the space and bracket between the two Arabic-digit runs join them, and the line drew as «−(٪٠٫٠٧−)  ٦٫٧٢» (step 27).
     */
    static String changeLine(double change, double pct, boolean latin) {
        return isolate(show(signed(change, 2), latin)) + "  " + isolate(show("(" + signed(pct, 2) + "%)", latin));
    }

    static String isolate(String s) { return s == null || s.isEmpty() ? s : "\u2066" + s + "\u2069"; }

    /** For the screen: Arabic digits («٫» for the point, «٬» between thousands) unless the setting is Latin; «%» becomes «٪» likewise. */
    static String show(String western, boolean latin) {
        if (latin || western == null) return western;
        return DigitsText.convert(western.replace('.', '٫').replace(',', '٬'), false);   // the digits and «٪» by the one helper
    }

    // ---------------------------------------------------------------- a small JSON reader for the file (numbers as doubles)

    /** Objects, arrays, strings, numbers (Double), true/false/null. Depth and size are bounded; anything else is a RuntimeException. */
    static final class Reader {
        private final String s;
        private int i;
        Reader(String s) { this.s = s; }

        Object read() {
            Object v = value(0);
            ws();
            if (i != s.length()) throw new IllegalArgumentException("trailing");
            return v;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private Object value(int depth) {
            if (depth > 12) throw new IllegalArgumentException("deep");   // the spark answer is ten deep
            ws();
            if (i >= s.length()) throw new IllegalArgumentException("end");
            char c = s.charAt(i);
            if (c == '{') {
                i++;
                Map<String, Object> m = new LinkedHashMap<>();
                ws();
                if (s.charAt(i) == '}') { i++; return m; }
                while (true) {
                    ws();
                    String k = string();
                    ws();
                    if (s.charAt(i++) != ':') throw new IllegalArgumentException("colon");
                    m.put(k, value(depth + 1));
                    ws();
                    char d = s.charAt(i++);
                    if (d == '}') return m;
                    if (d != ',') throw new IllegalArgumentException("object");
                }
            }
            if (c == '[') {
                i++;
                List<Object> a = new ArrayList<>();
                ws();
                if (s.charAt(i) == ']') { i++; return a; }
                while (true) {
                    a.add(value(depth + 1));
                    if (a.size() > 100_000) throw new IllegalArgumentException("long");
                    ws();
                    char d = s.charAt(i++);
                    if (d == ']') return a;
                    if (d != ',') throw new IllegalArgumentException("array");
                }
            }
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw new IllegalArgumentException("value");
            return Double.parseDouble(s.substring(start, i));
        }

        private String string() {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("string");
            i++;
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c != '\\') { b.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                    default: b.append(e);
                }
            }
        }
    }
}
