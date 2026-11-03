package sa.urtv.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/** For tests/markets_page_test.py: the markets page's data (MarketData) and «محفظتي»'s sums (Holdings) on a plain JVM. Prints PASS/FAIL lines. */
public final class MarketsRun {
    static int failed;

    static void check(String name, boolean ok, Object detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

    /** 2026-11-02 at hh:mm Riyadh (a Monday), in UTC ms */
    static long riyadh(int day, int h, int m) {
        java.util.Calendar k = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        k.clear();
        k.set(2026, java.util.Calendar.NOVEMBER, day, h - 3, m);
        return k.getTimeInMillis();
    }

    public static void main(String[] args) {
        data();
        hours();
        ranges();
        numbers();
        holdings();
        live();
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }

    // the file as CI writes it, cut down: TASI, five of the 30, gold, a US ticker; 2222 with volume and the 52-week range
    static final String FILE = "{\"updated\":\"2026-11-02T21:15:44+00:00\",\"quotes\":{"
        + "\"^TASI.SR\":{\"p\":9882.85,\"v\":9838.28,\"c\":[9870.04,9902.61,9850.84,9882.85],\"t\":\"2026-11-02T21:15:44+00:00\"},"
        + "\"2222.SR\":{\"p\":26.1,\"v\":25.92,\"c\":[25.96,25.94,26.08,26.1],\"t\":\"2026-11-02T21:15:44+00:00\",\"vol\":14200000,\"yh\":28.9,\"yl\":23.2},"
        + "\"1120.SR\":{\"p\":60.15,\"v\":59.7,\"c\":[59.8,60.15],\"t\":\"2026-11-02T21:15:44+00:00\",\"vol\":11800000},"
        + "\"2350.SR\":{\"p\":4.82,\"v\":4.66,\"c\":[],\"t\":\"2026-11-02T21:15:44+00:00\"},"
        + "\"8010.SR\":{\"p\":74.8,\"v\":77.5,\"c\":[77,74.8],\"t\":\"2026-11-02T21:15:44+00:00\"},"
        + "\"7010.SR\":{\"p\":41.5,\"v\":41.5,\"c\":[41.5],\"t\":\"2026-11-02T21:15:44+00:00\"},"
        + "\"9999.SR\":{\"p\":10,\"v\":null,\"c\":[9,10],\"t\":\"x\"},"
        + "\"GC=F\":{\"p\":3956.7,\"v\":3898.9,\"c\":[3915.3,3956.7],\"t\":\"2026-11-02T21:15:44+00:00\"},"
        + "\"AAPL\":{\"p\":337.06,\"v\":344.65,\"c\":[340,337.06],\"t\":\"2026-11-02T21:15:44+00:00\"},"
        + "\"BAD\":{\"p\":\"x\"}}}";

    static void data() {
        MarketData d = MarketData.parse(FILE);
        check("parse: the file's quotes, a bad one left out", d != null && d.quotes.size() == 9 && !d.quotes.containsKey("BAD"), d == null ? null : d.quotes.keySet());
        check("parse: «updated» to ms", d.updated == MarketData.time("2026-11-02T21:15:44+00:00") && d.updated == 1793654144000L, d.updated);
        check("parse: not the file -> null", MarketData.parse("<html>") == null && MarketData.parse("{\"x\":1}") == null && MarketData.parse(null) == null, "");
        MarketData.Quote a = d.of("2222");
        check("a quote: price, change, percent, the day's open, low and high from the closes", near(a.p, 26.1) && near(a.change(), 0.18)
            && Math.abs(a.pct() - 0.6944) < 1e-3 && near(a.open(), 25.96) && near(a.low(), 25.94) && near(a.high(), 26.1), a.pct());
        check("a quote: volume and 52-week range when CI kept them, NaN when not", near(a.vol, 14200000) && near(a.yh, 28.9) && near(a.yl, 23.2)
            && Double.isNaN(d.of("2350").vol) && Double.isNaN(d.of("2350").yh), "");
        check("a quote: no close before -> no change, up from its own line", Double.isNaN(d.of("9999").pct()) && Boolean.TRUE.equals(d.of("9999").up()), "");
        check("codes: 4 digits are Tadawul (.SR in the file), letters a US ticker", "2222.SR".equals(MarketData.symbol("2222")) && "AAPL".equals(MarketData.symbol("AAPL"))
            && MarketData.validCode("BRK-B") && !MarketData.validCode("aapl") && !MarketData.validCode("12345") && !MarketData.validCode(""), "");
        check("names: one of the 30 by its Arabic name, anything else by its code", "أرامكو".equals(MarketData.name("2222")) && "AAPL".equals(MarketData.name("AAPL"))
            && "9999".equals(MarketData.name("9999")) && MarketData.TADAWUL.length == 30, "");
        check("known: one of the 30 even with no price, or a code the file has; nothing else", d.known("4300") && d.known("AAPL") && d.known("9999") && !d.known("MSFT") && !d.known("12"), "");
        int[] b = d.breadth();
        check("breadth: over the 30 we fetch only, a company with no close before left out (9999 is not one of the 30)", Arrays.equals(b, new int[] {3, 1, 1}), Arrays.toString(b));
        check("movers: the most up first, the most down first", d.movers(true, 3).equals(Arrays.asList("2350", "1120", "2222")) && d.movers(false, 3).equals(Arrays.asList("8010")),
            d.movers(true, 3) + " " + d.movers(false, 3));
        check("most active: by volume, only those CI kept it for (none: empty, and the card is not shown)", d.active(5).equals(Arrays.asList("2222", "1120"))
            && MarketData.parse("{\"quotes\":{\"2222.SR\":{\"p\":1,\"v\":1,\"c\":[]}}}").active(5).isEmpty(), d.active(5));
        List<String> codes = d.codes();
        check("codes for the phone's search: the 30 first, then the file's others", codes.size() == 30 + 2 && codes.get(0).equals("2222") && codes.contains("AAPL") && codes.contains("9999")
            && !codes.contains("^TASI") && !codes.contains("GC=F"), codes);
        check("the pegs: fixed, four, dollar 3.75", MarketData.PEGS.length == 4 && "3.7500".equals(MarketData.PEGS[0][1]), "");
    }

    /** live mode («مباشر»): the pace and its backoff, what is fresh, the labels' kinds, Yahoo's answer read, and put over the file's prices */
    static void live() {
        check("live: every 30 s, 60 s after one failure, 120 s after two or more (and no slower)", MarketData.liveDelay(0) == 30_000 && MarketData.liveDelay(1) == 60_000
            && MarketData.liveDelay(2) == 120_000 && MarketData.liveDelay(9) == 120_000, MarketData.liveDelay(1));
        check("live: «مباشر» only while the last good answer is at most 150 s old; never for none or a clock gone back", MarketData.liveFresh(1000, 1000 + 150_000) && !MarketData.liveFresh(1000, 1000 + 150_001)
            && !MarketData.liveFresh(0, 5) && !MarketData.liveFresh(5000, 1000), "");
        check("live: Tadawul rows are 'delayed' (~15 min at the source), every other row shows its time", "delayed".equals(MarketData.liveKind("2222.SR")) && "delayed".equals(MarketData.liveKind("^TASI.SR"))
            && "time".equals(MarketData.liveKind("AAPL")) && "time".equals(MarketData.liveKind("^GSPC")) && "time".equals(MarketData.liveKind(null)), "");
        String json = "{\"quoteResponse\":{\"result\":[{\"symbol\":\"AAPL\",\"regularMarketPrice\":201.5,\"regularMarketPreviousClose\":200,\"regularMarketTime\":1793000000,\"currency\":\"USD\"},"
            + "{\"symbol\":\"2222.SR\",\"regularMarketPrice\":26.3,\"regularMarketTime\":1793000100},{\"symbol\":\"BAD\",\"regularMarketPrice\":\"x\"},{\"regularMarketPrice\":1}]}}";
        java.util.Map<String, MarketData.Live> m = MarketData.parseLive(json);
        check("live: Yahoo's quote answer is read (price, close before, time in ms); a row with no symbol or a bad price is skipped", m.size() == 2 && near(m.get("AAPL").p, 201.5) && near(m.get("AAPL").v, 200)
            && m.get("AAPL").at == 1793000000_000L && Double.isNaN(m.get("2222.SR").v), m.keySet());
        check("live: anything that is not that answer is an empty map (the caller backs off)", MarketData.parseLive("<html>").isEmpty() && MarketData.parseLive(null).isEmpty() && MarketData.parseLive("{}").isEmpty(), "");
        MarketData file = MarketData.parse(FILE);
        MarketData on = file.withLive(m);
        check("live: the live price goes over the file's (its close before too), the day's line stays the file's, the file itself is unchanged, and the live symbols are listed",
            near(on.of("2222.SR").p, 26.3) && near(on.of("2222.SR").v, file.of("2222.SR").v) && on.of("2222.SR").c.length == file.of("2222.SR").c.length && near(file.of("2222.SR").p, 26.1)
            && on.liveAt.containsKey("2222.SR") && !on.liveAt.containsKey("^TASI.SR") && file.liveAt.isEmpty(), on.liveAt);
        check("live: a symbol the file does not have is not invented", on.of("AAPL") == null || file.of("AAPL") != null, "");
    }

    static void ranges() {
        // 30 daily closes 100..129 from 2026-10-01 to 2026-11-01; today's price 131 confirmed on 2026-11-02 (after the line's last day)
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < 30; i++) d.append(i == 0 ? "" : ",").append(100 + i);
        String f = "{\"quotes\":{\"2222.SR\":{\"p\":131,\"v\":129,\"c\":[130,131],\"t\":\"2026-11-02T12:00:00+00:00\"},"
            + "\"1120.SR\":{\"p\":60,\"v\":59,\"c\":[59,60],\"t\":\"2026-11-01T12:00:00+00:00\"},\"7010.SR\":{\"p\":41.5,\"v\":41,\"c\":[41,41.5],\"t\":\"2026-11-02T12:00:00+00:00\"}}}";
        String daily = "{\"updated\":\"2026-11-02T03:00:00+00:00\",\"daily\":{\"2222.SR\":{\"d\":[" + d + "],\"df\":\"2026-10-01\",\"dl\":\"2026-11-01\"},"
            + "\"1120.SR\":{\"d\":[" + d + "],\"df\":\"2026-10-01\",\"dl\":\"2026-11-01\"},\"GONE.SR\":{\"d\":[1,2]}}}";
        check("ranges: the 15-minute file alone has no year (none of its quotes has ranges)", !MarketData.hasRanges(MarketData.parse(f).of("2222")), "");
        check("ranges: not the daily file -> the quotes as they are", MarketData.parse(f).withDaily("<html>").of("2222").d.length == 0
            && MarketData.parse(f).withDaily(null).of("2222").d.length == 0, "");
        MarketData m = MarketData.parse(f).withDaily(daily);
        check("ranges: the daily file adds the year to the quotes it names, and nothing for one that is not in the 15-minute file",
            m.quotes.size() == 3 && !m.quotes.containsKey("GONE.SR") && near(m.of("2222").p, 131), m.quotes.keySet());
        MarketData.Quote q = m.of("2222");
        check("ranges: the year's daily closes and their first and last day are read", q.d.length == 30 && q.df == MarketData.day("2026-10-01") && q.dl == MarketData.day("2026-11-01"), q.d.length);
        MarketData.Range today = MarketData.range(q, MarketData.TODAY);
        check("ranges: «اليوم» is the day's closes against the close before", today.line == q.c && near(today.base, 129) && near(today.pct(q.p), 2 / 129.0 * 100), today.base);
        MarketData.Range w = MarketData.range(q, MarketData.WEEK);
        check("ranges: «أسبوع» = the last five days and today's price after them, from the close five trading days back",
            w.line.length == 6 && near(w.line[0], 125) && near(w.line[5], 131) && near(w.base, 125) && near(w.change(q.p), 6) && w.from == 0, Arrays.toString(w.line));
        MarketData.Range mo = MarketData.range(q, MarketData.MONTH);
        check("ranges: «شهر» = 21 trading days", mo.line.length == 22 && near(mo.base, 109), Arrays.toString(mo.line));
        MarketData.Range y = MarketData.range(q, MarketData.YEAR);
        check("ranges: «سنة» = the whole line, from its first day (the only start that is known)", y.line.length == 31 && near(y.base, 100) && y.from == q.df, y.line.length);
        MarketData.Range same = MarketData.range(m.of("1120"), MarketData.WEEK);
        check("ranges: a price of the line's last day is not added again", same.line.length == 6 && near(same.line[5], 129) && near(same.base, 124), Arrays.toString(same.line));
        check("ranges: no daily closes -> no ranges, «اليوم» only", !MarketData.hasRanges(m.of("7010")) && MarketData.range(m.of("7010"), MarketData.YEAR).line == m.of("7010").c, "");
    }

    static void hours() {
        check("open: Monday 10:00 to 14:59 Riyadh", MarketData.open(riyadh(2, 10, 0)) && MarketData.open(riyadh(2, 14, 59)) && !MarketData.open(riyadh(2, 15, 0))
            && !MarketData.open(riyadh(2, 9, 59)), "");
        check("open: never on Friday or Saturday", !MarketData.open(riyadh(6, 11, 0)) && !MarketData.open(riyadh(7, 11, 0)) && MarketData.open(riyadh(1, 11, 0)), "");
        check("next open: Monday evening -> Tuesday 10:00 (the mockup's «يفتح بعد ١١ س و١٥ د، الثلاثاء»)", MarketData.nextOpen(riyadh(2, 22, 45)) == riyadh(3, 10, 0)
            && Arrays.equals(MarketData.until(riyadh(2, 22, 45), riyadh(3, 10, 0)), new int[] {11, 15}), "");
        check("next open: Thursday after the close -> Sunday", MarketData.nextOpen(riyadh(5, 16, 0)) == riyadh(8, 10, 0)
            && MarketData.weekday(MarketData.nextOpen(riyadh(5, 16, 0))) == java.util.Calendar.SUNDAY, "");
        check("next open: early on a trading day -> that day", MarketData.nextOpen(riyadh(4, 7, 30)) == riyadh(4, 10, 0), "");
        check("close: 15:00 the same day, and «until» rounds up to the minute", MarketData.closeAt(riyadh(2, 13, 0)) == riyadh(2, 15, 0)
            && Arrays.equals(MarketData.until(riyadh(2, 13, 0) + 1000, riyadh(2, 15, 0)), new int[] {2, 0}), "");
    }

    static void numbers() {
        check("num: grouped, rounded, a minus only when not zero", "9,882.85".equals(MarketData.num(9882.85, 2)) && "1,243.57".equals(MarketData.num(1243.567, 2))
            && "0.00".equals(MarketData.num(-0.001, 2)) && "-115.13".equals(MarketData.num(-115.13, 2)) && "1,000,000".equals(MarketData.num(999999.6, 0)), MarketData.num(-0.001, 2));
        check("signed: + or a true minus", "+44.57".equals(MarketData.signed(44.57, 2)) && "−1.20".equals(MarketData.signed(-1.2, 2)), MarketData.signed(-1.2, 2));
        check("show: Arabic digits with «٫» and «٬» (the page's ar-SA), Latin as is", "٩٬٨٨٢٫٨٥".equals(MarketData.show("9,882.85", false))
            && "+٠٫٤٥٪".equals(MarketData.show("+0.45%", false)) && "9,882.85".equals(MarketData.show("9,882.85", true)), MarketData.show("9,882.85", false));
    }

    static void holdings() {
        List<Holdings.Buy> two = Arrays.asList(Holdings.buy("80", "50.90"), Holdings.buy("٤٠", "٥٤٫٥٠"));
        Holdings.Holding r = new Holdings.Holding("1120", two);
        check("buys: Arabic or Western digits, «٫» «.» or «,» for the point; the weighted average (the phone mockup's ٥٢٫١٠)",
            near(r.qty(), 120) && Math.abs(r.avg() - 52.1) < 1e-9, r.avg());
        check("buy: nothing, zero, a negative, letters, too many decimals or too large -> null", Holdings.buy("", "1") == null && Holdings.buy("0", "1") == null
            && Holdings.buy("-1", "1") == null && Holdings.buy("a", "1") == null && Holdings.buy("1", "1.12345") == null && Holdings.buy("1000000000", "1") == null
            && Holdings.buy("1,5", "2") != null, "");
        List<Holdings.Holding> all = new ArrayList<>();
        all = Holdings.put(all, "2222", Arrays.asList(Holdings.buy("500", "27.40")));
        all = Holdings.put(all, "1120", two);
        all = Holdings.put(all, "AAPL", Arrays.asList(Holdings.buy("10", "245")));
        String kept = Holdings.write(all);
        check("kept as plain lines, nothing but the codes and the buys", "2222|500:27.4\n1120|80:50.9;40:54.5\nAAPL|10:245".equals(kept), kept);
        check("read back the same; a bad line or a second copy of a code is dropped",
            Holdings.write(Holdings.parse(kept + "\nzz|1:1\n2222|9:9\nBAD\n1010|x:1")).equals(kept), Holdings.write(Holdings.parse(kept + "\nzz|1:1")));
        check("put: replaces in place; no buys removes; remove", Holdings.write(Holdings.put(all, "2222", Arrays.asList(Holdings.buy("1", "2")))).startsWith("2222|1:2\n")
            && Holdings.find(Holdings.put(all, "2222", new ArrayList<Holdings.Buy>()), "2222") < 0 && Holdings.find(Holdings.remove(all, "AAPL"), "AAPL") < 0, "");
        List<Holdings.Holding> full = new ArrayList<>();
        for (int i = 0; i < Holdings.MAX; i++) full = Holdings.put(full, String.valueOf(1000 + i), Arrays.asList(Holdings.buy("1", "1")));
        check("put: at most MAX holdings (null), an existing one still changes", Holdings.put(full, "9999", Arrays.asList(Holdings.buy("1", "1"))) == null
            && Holdings.put(full, "1000", Arrays.asList(Holdings.buy("2", "1"))) != null, "");

        MarketData d = MarketData.parse(MarketsRun.FILE);
        Holdings.Totals t = Holdings.totals(all, d::of);
        // 2222: 500 x 26.10 = 13,050 (cost 13,700); 1120: 120 x 60.15 = 7,218 (cost 6,252); AAPL: 10 x 337.06 = 3,370.60 $ (cost 2,450 $) at 3.75
        double value = 13050 + 7218 + 3370.6 * 3.75, cost = 13700 + 6252 + 2450 * 3.75;
        check("totals: the value in riyals, US holdings at the 3.75 peg", near(t.value, value) && near(t.cost, cost), t.value + " " + value);
        check("totals: the gain and its percent", near(t.gain, value - cost) && Math.abs(t.gainPct - (value - cost) / cost * 100) < 1e-9, t.gain);
        double day = (26.1 - 25.92) * 500 + (60.15 - 59.7) * 120 + (337.06 - 344.65) * 10 * 3.75;
        check("totals: today's change in riyals and its percent of yesterday's value", near(t.day, day) && Math.abs(t.dayPct - day / (value - day) * 100) < 1e-9, t.day + " " + day);
        Holdings.Row aapl = t.rows.get(2);
        check("a US row stays in dollars; its share of the value counts it in riyals", near(aapl.value, 3370.6) && near(aapl.valueSar, 3370.6 * 3.75)
            && Math.abs(t.share(aapl) - 3370.6 * 3.75 / value * 100) < 1e-9, aapl.value);
        Holdings.Totals none = Holdings.totals(Arrays.asList(new Holdings.Holding("4300", Arrays.asList(Holdings.buy("10", "17")))), d::of);
        check("a holding with no price in the file: at its cost, no gain, no day", near(none.value, 170) && near(none.gain, 0) && Double.isNaN(none.rows.get(0).price)
            && Double.isNaN(none.rows.get(0).dayPct), none.value);
        check("plain: up to 4 decimals, no trailing zeros", "52.1".equals(Holdings.plain(52.1)) && "3".equals(Holdings.plain(3.0)) && "0.3333".equals(Holdings.plain(1 / 3.0)), "");
    }
}
