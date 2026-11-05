package sa.urtv.home;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * «محفظتي» for the phone's portfolio page (assets/phone/portfolio.html, served at /m), through PhoneActions. The one way out of the TV for
 * the holdings, and only to a paired phone (PhoneApi asks for the session first) while the TV is editing them (Portfolio.editing, on Portfolio.now(): the monotonic clock): outside
 * that window both answer "portfolio_closed" and nothing is read. The page keeps nothing after saving.
 */
public final class PortfolioPhone {
    private PortfolioPhone() { }

    /** The clock of the TV's window (Portfolio.now: monotonic), for PhoneActions. */
    public static long now() { return Portfolio.now(); }

    /**
     * The holdings with their buys, the names the search offers and the codes the price file knows, or null when the TV is not editing them.
     * {"holdings": [{"code", "name", "buys": ["qty:price", …]}], "names": [["2222", "أرامكو"], …], "codes": ["2222", …, "AAPL", …],
     * "prices": {"2222": "26.1", …}}. Numbers are strings: the phone's JSON is whole numbers only.
     */
    public static Map<String, Object> read(Context c, long now) {
        if (!Portfolio.editing(now)) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        List<Object> hs = new ArrayList<>();
        for (Holdings.Holding h : Portfolio.load(c)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", h.code);
            m.put("name", S.tv(MarketData.name(h.code)));
            List<Object> buys = new ArrayList<>();
            for (Holdings.Buy b : h.buys) buys.add(Holdings.plain(b.qty) + ":" + Holdings.plain(b.price));
            m.put("buys", buys);
            hs.add(m);
        }
        out.put("holdings", hs);
        List<Object> names = new ArrayList<>();
        for (String[] t : MarketData.TADAWUL) { List<Object> p = new ArrayList<>(); p.add(t[0]); p.add(S.tv(t[1])); names.add(p); }
        out.put("names", names);
        MarketData d = MarketFile.cached(c);
        Map<String, Object> prices = new LinkedHashMap<>();
        List<Object> codes = new ArrayList<>();
        if (d != null) for (String code : d.codes()) {
            MarketData.Quote q = d.of(code);
            codes.add(code);
            if (q != null) prices.put(code, Holdings.plain(q.p));
        }
        out.put("codes", codes);
        out.put("prices", prices);
        return out;
    }

    /**
     * One holding's buys from the phone (each "qty:price"; an empty list removes the holding): "saved", or a fixed failure, "portfolio_closed"
     * (the TV is not editing), "unknown_code" (not one of the 30 and not in the price file), "bad_request", "full" or "failed".
     */
    public static String save(Context c, long now, String code, List<String> buys) {
        if (!Portfolio.editing(now)) return "portfolio_closed";
        if (!MarketData.validCode(code)) return "bad_request";
        List<Holdings.Buy> list = new ArrayList<>();
        for (String b : buys) {
            String[] qp = b.split(":");
            Holdings.Buy x = qp.length == 2 ? Holdings.buy(qp[0], qp[1]) : null;
            if (x == null) return "bad_request";
            list.add(x);
        }
        List<Holdings.Holding> all = Portfolio.load(c);
        if (!list.isEmpty() && Holdings.find(all, code) < 0) {
            MarketData d = MarketFile.cached(c);
            boolean known = false;
            for (String[] t : MarketData.TADAWUL) known |= t[0].equals(code);
            if (!known && (d == null || !d.known(code))) return "unknown_code";
        }
        List<Holdings.Holding> next = Holdings.put(all, code, list);
        if (next == null) return "full";
        if (!Portfolio.save(c, next)) return "failed";
        // the window is not renewed here: only the TV renews it (Portfolio.openEditing), so a phone cannot keep itself in
        Portfolio.changed();
        return "saved";
    }
}
