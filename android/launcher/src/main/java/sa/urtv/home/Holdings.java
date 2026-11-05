package sa.urtv.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * «محفظتي» (docs/PROPOSALS-NOV2.md §7): the holdings, their buys, and what they are worth at the file's prices. Plain Java (no Android):
 * tests/HoldingsRun.java runs it. Where they are kept, and who may read them, is Portfolio's business; this only computes.
 *
 * <p>A holding is a code (4 digits for Tadawul, letters for a US ticker) and its buys (quantity and price), so the average can be edited later
 * from the buys. A US holding is in dollars; the totals turn it into riyals at the peg (MarketData.PEG), a fixed rate, never a fetched one.
 *
 * <p>Kept as text, one holding a line: {@code CODE|qty:price;qty:price}, Western digits and «.». Nothing else is in it: no name, no date.
 */
final class Holdings {
    private Holdings() { }

    static final int MAX = 40, MAX_BUYS = 20;
    static final double MAX_QTY = 100_000_000, MAX_PRICE = 10_000_000;

    static final class Buy {
        final double qty, price;
        Buy(double qty, double price) { this.qty = qty; this.price = price; }
    }

    static final class Holding {
        final String code;
        final List<Buy> buys;
        Holding(String code, List<Buy> buys) { this.code = code; this.buys = Collections.unmodifiableList(new ArrayList<>(buys)); }

        double qty() { double q = 0; for (Buy b : buys) q += b.qty; return q; }
        double cost() { double c = 0; for (Buy b : buys) c += b.qty * b.price; return c; }
        /** the weighted average of the buys */
        double avg() { double q = qty(); return q > 0 ? cost() / q : 0; }
        boolean usd() { return !MarketData.isSaudi(code); }
    }

    // ---------------------------------------------------------------- the stored text

    static List<Holding> parse(String text) {
        List<Holding> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            int bar = line.indexOf('|');
            if (bar < 0) continue;
            String code = line.substring(0, bar);
            if (!MarketData.validCode(code) || find(out, code) >= 0) continue;
            List<Buy> buys = new ArrayList<>();
            for (String b : line.substring(bar + 1).split(";")) {
                String[] qp = b.split(":");
                if (qp.length != 2) continue;
                Buy x = buy(qp[0], qp[1]);
                if (x != null && buys.size() < MAX_BUYS) buys.add(x);
            }
            if (!buys.isEmpty() && out.size() < MAX) out.add(new Holding(code, buys));
        }
        return out;
    }

    static String write(List<Holding> all) {
        StringBuilder b = new StringBuilder();
        for (Holding h : all) {
            if (b.length() > 0) b.append('\n');
            b.append(h.code).append('|');
            for (int i = 0; i < h.buys.size(); i++) {
                if (i > 0) b.append(';');
                b.append(plain(h.buys.get(i).qty)).append(':').append(plain(h.buys.get(i).price));
            }
        }
        return b.toString();
    }

    /** a number as kept: up to 4 decimals, no trailing zeros */
    static String plain(double v) {
        String s = String.format(Locale.ROOT, "%.4f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    // ---------------------------------------------------------------- changes

    static int find(List<Holding> all, String code) {
        for (int i = 0; i < all.size(); i++) if (all.get(i).code.equals(code)) return i;
        return -1;
    }

    /** The list with this holding set to these buys (added at the end if new); no buys, or a quantity of 0, removes it. Null: too many. */
    static List<Holding> put(List<Holding> all, String code, List<Buy> buys) {
        List<Holding> out = new ArrayList<>(all);
        int i = find(out, code);
        Holding h = new Holding(code, buys);
        if (buys.isEmpty() || h.qty() <= 0) { if (i >= 0) out.remove(i); return out; }
        if (buys.size() > MAX_BUYS) return null;
        if (i >= 0) out.set(i, h);
        else if (out.size() >= MAX) return null;
        else out.add(h);
        return out;
    }

    static List<Holding> remove(List<Holding> all, String code) {
        List<Holding> out = new ArrayList<>(all);
        int i = find(out, code);
        if (i >= 0) out.remove(i);
        return out;
    }

    /** One buy from what was typed (Arabic or Western digits, «٫» «.» or «,» for the point), or null when it is not a quantity and a price. */
    static Buy buy(String qty, String price) {
        double q = number(qty), p = number(price);
        if (Double.isNaN(q) || Double.isNaN(p) || q <= 0 || p <= 0 || q > MAX_QTY || p > MAX_PRICE) return null;
        return new Buy(q, p);
    }

    /** which of the two is not usable yet: 0 the quantity only, 1 the cost only, 2 both, -1 none (a quantity of 0 and no cost are missing; «احفظ» says which) */
    static int missing(String qty, String price) {
        double q = number(qty), p = number(price);
        boolean noQ = Double.isNaN(q) || q <= 0 || q > MAX_QTY, noP = Double.isNaN(p) || p <= 0 || p > MAX_PRICE;
        return noQ && noP ? 2 : noQ ? 0 : noP ? 1 : -1;
    }

    /** a typed number: digits of either kind and one decimal point («٫», «.» or «,»); NaN for anything else */
    static double number(String raw) {
        if (raw == null) return Double.NaN;
        StringBuilder b = new StringBuilder();
        for (char c : DigitsText.convert(raw.trim(), true).toCharArray()) {   // ٠-٩ and «٫» by the one helper
            if (c >= '۰' && c <= '۹') b.append((char) ('0' + (c - '۰')));
            else if (c == '٫' || c == ',' || c == '.') b.append('.');
            else if (c >= '0' && c <= '9') b.append(c);
            else return Double.NaN;
        }
        String s = b.toString();
        if (!s.matches("\\d{1,9}(\\.\\d{1,4})?")) return Double.NaN;
        return Double.parseDouble(s);
    }

    // ---------------------------------------------------------------- what they are worth

    interface Prices { MarketData.Quote of(String code); }

    /** One holding at today's price; amounts in its own currency (dollars for a US one). */
    static final class Row {
        final Holding h;
        final double price;      // NaN: no price in the file (value and gain are then at cost)
        final double value, gain, gainPct, day, dayPct;
        final double valueSar;   // for the totals and the allocation
        Row(Holding h, double price, double value, double gain, double gainPct, double day, double dayPct, double valueSar) {
            this.h = h; this.price = price; this.value = value; this.gain = gain; this.gainPct = gainPct; this.day = day; this.dayPct = dayPct;
            this.valueSar = valueSar;
        }
    }

    static final class Totals {
        final List<Row> rows;
        final double value, cost, gain, gainPct, day, dayPct;   // riyals
        Totals(List<Row> rows, double value, double cost, double gain, double gainPct, double day, double dayPct) {
            this.rows = rows; this.value = value; this.cost = cost; this.gain = gain; this.gainPct = gainPct; this.day = day; this.dayPct = dayPct;
        }
        /** each row's share of the value, in %, in the rows' order */
        double share(Row r) { return value > 0 ? r.valueSar / value * 100 : 0; }
    }

    static Totals totals(List<Holding> all, Prices prices) {
        List<Row> rows = new ArrayList<>();
        double value = 0, cost = 0, day = 0;
        for (Holding h : all) {
            MarketData.Quote q = prices == null ? null : prices.of(h.code);
            double qty = h.qty(), c = h.cost(), fx = h.usd() ? MarketData.PEG : 1;
            double price = q == null ? Double.NaN : q.p;
            double v = q == null ? c : q.p * qty;
            double d = q != null && q.hasPrev() ? (q.p - q.v) * qty : 0;
            double dPct = q != null && q.hasPrev() ? q.pct() : Double.NaN;
            double g = v - c;
            rows.add(new Row(h, price, v, g, c > 0 ? g / c * 100 : 0, d, dPct, v * fx));
            value += v * fx;
            cost += c * fx;
            day += d * fx;
        }
        double gain = value - cost, before = value - day;
        return new Totals(rows, value, cost, gain, cost > 0 ? gain / cost * 100 : 0, day, before > 0 ? day / before * 100 : 0);
    }
}
