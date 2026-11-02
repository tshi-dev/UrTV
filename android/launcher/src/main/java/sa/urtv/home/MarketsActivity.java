package sa.urtv.home;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * «الأسواق» (docs/PROPOSALS-NOV2.md §6 and §7, the mockups in notes/proposals/markets-*.png): a screen of its own with the tabs «السوق»,
 * «قائمتي» and «محفظتي» («الأخبار» waits behind {@link #NEWS} until a source agrees in writing), and the stock card that OK on any stock opens.
 * It opens with OK on the home's stocks card and from the settings («المزيد ‹ المباريات والأسواق ‹ صفحة الأسواق»).
 *
 * <p>Only data we already have: CI's markets file (MarketFile), the fixed pegs and Tadawul's public hours. A card whose source we do not have
 * (the sectors, the whole market's breadth and turnover, dividends, the companies' announcements, news) is not on the screen at all; the
 * volume and the 52-week range show only when CI keeps them from the same Yahoo answer.
 *
 * <p>«محفظتي» is kept by Portfolio (this app's private prefs, never anywhere else); with «اقفل محفظتي» and a parental PIN, the tab asks for it.
 * {@code am start -n sa.urtv.home/.MarketsActivity} (--es tab market|watch|portfolio).
 */
public class MarketsActivity extends Activity {
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    /** «الأخبار»: no licensed source yet (§6: Argaam, Saudi Exchange, Mubasher, CMA and SPA give no permission); off until one agrees */
    static final boolean NEWS = false;
    static final String EXTRA_TAB = "tab";
    static final String[] TABS = {"market", "watch", "portfolio"};
    /** the screensaver's default watchlist (its Prefs.DEFAULT_STOCKS), for a fragment that says none */
    static final String DEFAULT_STOCKS = "2222,1120,7010,2010";
    /** the markets bar's keys a symbol can be shown as (the page's MARKETS, the screensaver's Prefs.MARKETS) */
    static final String[][] BAR_KEYS = {{"^TASI.SR", "tasi"}, {"2222", "aramco"}, {"GC=F", "gold"}, {"SI=F", "silver"}, {"BZ=F", "oil"}};
    private static final Uri SAVER = Uri.parse("content://sa.urtv.screensaver.config");
    /** the allocation bar's colours, in order (the mockups'), then again */
    private static final int[] ALLOC = {0xFF1F9D5C, 0xFF8FD3B0, 0xFFD4A82A, 0xFF9C7A52, 0xFF5B8FCF, 0xFFB9C4CF, 0xFFC47E9B, 0xFF6FB3B8};

    private GlassStyle.Look look;
    private int TEXT, DIM, ACCENT, UP, DOWN;
    private final Handler main = new Handler(Looper.getMainLooper());
    private MarketData data;
    private int tab;
    private boolean unlocked;   // the PIN was given for «محفظتي» since this screen opened
    private TextView state, updated;
    private final List<TextView> tabViews = new ArrayList<>();
    private LinearLayout tabRow, actions, content;
    private ScrollView scroll;
    private final Runnable minute = new Runnable() {
        @Override public void run() {
            header();
            if (tab == 2) Portfolio.openEditing(System.currentTimeMillis());   // the phone may edit while the tab is on the screen
            main.postDelayed(this, 60_000);
        }
    };
    private final Runnable changed = () -> main.post(() -> { if (tab == 2 && !isFinishing()) show(2, false); });

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        FrameLayout root = new FrameLayout(this);
        look = GlassSource.page(this, root);
        TEXT = look.text; DIM = look.sub; ACCENT = look.light ? 0xFF166A45 : OnOff.GREEN_ON_DARK;
        UP = ACCENT; DOWN = look.warn;
        root.setLayoutDirection(Dir.layout());

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(60), dp(28), dp(60), dp(0));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        // the title at the start; the market's state and the last update at the end
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.TOP);
        top.addView(text(S.t("الأسواق"), 30, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        state = text("", 16, "Regular", TEXT);
        updated = text("", 13, "Regular", DIM);
        status.addView(state, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-2, -2);
        up.topMargin = dp(4);
        status.addView(updated, up);
        top.addView(status, new LinearLayout.LayoutParams(-2, -2));
        page.addView(top, new LinearLayout.LayoutParams(-1, -2));

        // the tabs, and at the row's end the portfolio's buttons
        tabRow = new LinearLayout(this);
        tabRow.setGravity(Gravity.BOTTOM);
        String[] names = NEWS ? new String[] {S.t("السوق"), S.t("قائمتي"), S.t("محفظتي"), S.t("الأخبار")} : new String[] {S.t("السوق"), S.t("قائمتي"), S.t("محفظتي")};
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            TextView t = text(names[i], 19, "Medium", DIM);
            t.setId(View.generateViewId());
            t.setFocusable(true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(16), dp(8), dp(16), dp(8));
            t.setOnFocusChangeListener((v, has) -> { if (has && k != tab) want(k); paintTabs(); });
            t.setOnClickListener(v -> want(k));
            tabViews.add(t);
            tabRow.addView(t, new LinearLayout.LayoutParams(-2, -2));
        }
        tabRow.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-2, -2);
        ap.bottomMargin = dp(8);
        tabRow.addView(actions, ap);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(8);
        page.addView(tabRow, tp);
        View rule = new View(this);
        rule.setBackgroundColor(look.line);
        page.addView(rule, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1))));

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(16), 0, dp(28));
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        data = MarketFile.cached(this);
        int start = Arrays.asList(TABS).indexOf(getIntent().getStringExtra(EXTRA_TAB));
        tab = -1;
        want(Math.max(0, start));
        tabViews.get(Math.max(0, tab)).requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Portfolio.listener = changed;
        main.removeCallbacks(minute);
        minute.run();
        new Thread(() -> {
            MarketData d = MarketFile.load(getApplicationContext(), false);
            main.post(() -> {
                if (isFinishing() || d == null) return;
                data = d;
                header();
                show(tab, false);
            });
        }, "markets").start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        main.removeCallbacks(minute);
        if (Portfolio.listener == changed) Portfolio.listener = null;
    }

    // ---------------------------------------------------------------- the header

    /** «تداول مغلق · يفتح بعد ١١ س و١٥ د، الثلاثاء ١٠:٠٠ ص» and «آخر تحديث ١٠:٤٥ م · أسعار متأخرة» */
    private void header() {
        long now = System.currentTimeMillis();
        SpannableStringBuilder s = new SpannableStringBuilder();
        boolean open = MarketData.open(now);
        String word = open ? S.t("تداول مفتوح") : S.t("تداول مغلق");
        s.append(word);
        s.setSpan(new ForegroundColorSpan(open ? UP : TEXT), 0, word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.append("  ");
        if (open) {
            s.append(S.t("يغلق بعد %s", duration(MarketData.until(now, MarketData.closeAt(now)))));
        } else {
            long at = MarketData.nextOpen(now);
            s.append(S.t("يفتح بعد %s، %s %s", duration(MarketData.until(now, at)), S.tv(DAYS[Math.min(DAYS.length - 1, MarketData.weekday(at) - 1)]), clock(10, 0)));
        }
        state.setText(s);
        if (data == null || data.updated == 0) { updated.setText(S.t("لا أسعار بعد: يلزم اتصال بالإنترنت")); return; }
        Calendar k = Calendar.getInstance(TimeZone.getTimeZone(zone()));
        k.setTimeInMillis(data.updated);
        updated.setText(S.t("آخر تحديث %s · أسعار متأخرة", clock(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE))));
    }

    /** Tadawul's trading days, Sunday to Thursday (MarketData.nextOpen is always one of them) */
    private static final String[] DAYS = {S.n("الأحد"), S.n("الاثنين"), S.n("الثلاثاء"), S.n("الأربعاء"), S.n("الخميس")};

    private String duration(int[] hm) {
        return hm[0] > 0 ? S.t("%s س و%s د", digits(hm[0]), digits(hm[1])) : S.t("%s د", digits(hm[1]));
    }

    /** a time of day as the clock setting says: 24 hours, or 12 with ص/م */
    private String clock(int h, int m) {
        String mm = (m < 10 ? "0" : "") + m;
        if (Digits.clock24()) return Digits.of(h + ":" + mm);
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return Digits.of(h12 + ":" + mm) + " " + (h < 12 ? S.t("ص") : S.t("م"));
    }

    /** the city's zone from the screensaver's settings, as the clock uses */
    private String zone() {
        String tz = param("tz");
        return ClockZone.valid(tz);
    }

    // ---------------------------------------------------------------- tabs

    /** The tab asked for: «محفظتي» behind the PIN when «اقفل محفظتي» is on and a PIN exists. */
    private void want(int k) {
        if (k == tab) return;
        if (k == 2 && !unlocked && Portfolio.locked(this) && Parental.state(this).pinSet) {
            int back = Math.max(0, tab);
            Parental.confirm(this, Parental.adhanFirst(this), S.n("محفظتي"), () -> { unlocked = true; show(2, true); tabViews.get(2).requestFocus(); },
                    () -> { tabViews.get(back).requestFocus(); paintTabs(); });
            return;
        }
        show(k, true);
    }

    private void show(int k, boolean top) {
        if (k < 0) return;
        tab = k;
        paintTabs();
        content.removeAllViews();
        actions.removeAllViews();
        if (k == 0) market();
        else if (k == 1) watch();
        else portfolio();
        if (top) scroll.scrollTo(0, 0);
        // down from the tabs to the first thing that takes the focus
        View first = firstFocusable(content);
        for (TextView t : tabViews) t.setNextFocusDownId(first != null ? first.getId() : View.NO_ID);
    }

    private void paintTabs() {
        for (int i = 0; i < tabViews.size(); i++) {
            TextView t = tabViews.get(i);
            boolean on = i == tab;
            t.setTextColor(t.hasFocus() ? look.onFocus : on ? ACCENT : DIM);
            // the focus fill while focused; the chosen tab's line along its bottom
            android.graphics.drawable.LayerDrawable bg = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[] {
                    t.hasFocus() ? GlassPaint.focused(this, look, 12) : new ColorDrawable(0), new ColorDrawable(on ? ACCENT : 0)});
            bg.setLayerGravity(1, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL);
            bg.setLayerHeight(1, dp(3));
            t.setBackground(bg);
        }
    }

    private static View firstFocusable(View v) {
        if (v.isFocusable() && v.getVisibility() == View.VISIBLE) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) { View f = firstFocusable(g.getChildAt(i)); if (f != null) return f; }
        }
        return null;
    }

    // ---------------------------------------------------------------- «السوق»

    private void market() {
        if (data == null) { content.addView(note(S.t("لا أسعار بعد: يلزم اتصال بالإنترنت")), wrap()); return; }
        LinearLayout row1 = new LinearLayout(this);
        // TASI: value, change, and its day against yesterday's close
        MarketData.Quote tasi = data.quotes.get(MarketData.TASI);
        LinearLayout t = card();
        if (tasi != null) t.addView(chartBlock(S.t("تاسي"), tasi, 34, 96), wrap());
        else t.addView(text(S.t("تاسي"), 18, "Medium", TEXT), wrap());
        row1.addView(t, weight(1.75f, 0));

        // «اتجاه السوق»: the 30 companies we fetch (and it says so), and the session
        LinearLayout br = card();
        LinearLayout bh = new LinearLayout(this);
        bh.setGravity(Gravity.BOTTOM);
        bh.addView(text(S.t("اتجاه السوق"), 18, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        bh.addView(text(S.t("أكبر %s شركة", digits(MarketData.TADAWUL.length)), 13, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        br.addView(bh, wrap());
        int[] b = data.breadth();
        br.addView(new MarketChart(this).bar(new float[] {b[0], b[1], b[2]}, new int[] {UP, DIM, DOWN}, 0), sized(-1, 12, 16));
        LinearLayout counts = new LinearLayout(this);
        counts.addView(text(S.t("%s مرتفعة", digits(b[0])), 15, "Medium", UP), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView same = text(S.t("%s دون تغيير", digits(b[1])), 15, "Regular", DIM);
        same.setGravity(Gravity.CENTER);
        counts.addView(same, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView down = text(S.t("%s منخفضة", digits(b[2])), 15, "Medium", DOWN);
        down.setGravity(Dir.end());
        counts.addView(down, new LinearLayout.LayoutParams(0, -2, 1f));
        br.addView(counts, gap(12));
        View rule = new View(this);
        rule.setBackgroundColor(look.line);
        br.addView(rule, sized(-1, 1, 16));
        LinearLayout sh = new LinearLayout(this);
        sh.addView(text(S.t("الجلسة"), 14, "Regular", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        sh.addView(text(S.t("الأحد إلى الخميس"), 14, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        br.addView(sh, gap(16));
        float done = session(System.currentTimeMillis());
        br.addView(new MarketChart(this).bar(new float[] {done, 1 - done}, new int[] {done > 0 && done < 1 ? ACCENT : DIM, 0}, look.line), sized(-1, 8, 12));
        LinearLayout se = new LinearLayout(this);
        se.addView(text(clock(10, 0), 13, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 1f));
        se.addView(text(S.t("%s · مزاد الإغلاق إلى %s", clock(15, 0), clock(15, 10)), 13, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        br.addView(se, gap(8));
        row1.addView(br, weight(1f, 16));
        content.addView(row1, wrap());

        // the 3 that rose most and the 3 that fell most; «الأكثر نشاطًا» only when CI keeps the volume
        LinearLayout row2 = new LinearLayout(this);
        LinearLayout mv = new LinearLayout(this);
        mv.setOrientation(LinearLayout.VERTICAL);
        mv.addView(text(S.t("الأكثر ارتفاعًا وانخفاضًا"), 18, "Medium", TEXT), gap(0));
        LinearLayout list = card();
        list.setPadding(dp(16), dp(8), dp(16), dp(8));
        List<String> ups = data.movers(true, 3), downs = data.movers(false, 3);
        for (String code : ups) list.addView(stockRow(code, false), wrap());
        if (!ups.isEmpty() && !downs.isEmpty()) {
            View r = new View(this);
            r.setBackgroundColor(look.line);
            list.addView(r, sized(-1, 1, 8));
        }
        for (String code : downs) list.addView(stockRow(code, false), gap(ups.isEmpty() ? 0 : 6));
        if (ups.isEmpty() && downs.isEmpty()) list.addView(note(S.t("لا تغيير بعد اليوم")), wrap());
        mv.addView(list, gap(12));
        List<String> active = data.active(5);
        row2.addView(mv, weight(1f, 0));
        if (!active.isEmpty()) {
            LinearLayout ac = new LinearLayout(this);
            ac.setOrientation(LinearLayout.VERTICAL);
            LinearLayout ah = new LinearLayout(this);
            ah.setGravity(Gravity.BOTTOM);
            ah.addView(text(S.t("الأكثر نشاطًا"), 18, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
            ah.addView(text(S.t("بالكمية، من أكبر %s شركة", digits(MarketData.TADAWUL.length)), 13, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
            ac.addView(ah, wrap());
            LinearLayout al = card();
            al.setPadding(dp(16), dp(8), dp(16), dp(8));
            for (String code : active) al.addView(volumeRow(code), wrap());
            ac.addView(al, gap(12));
            row2.addView(ac, weight(1f, 16));
        }
        content.addView(row2, gap(24));
    }

    /** how far today's session is, 0 to 1 (0 before it, 1 after it or on a day off) */
    private static float session(long now) {
        long t = (now + MarketData.RIYADH) % 86_400_000L / 60_000L;
        int dow = MarketData.weekday(now);
        if (dow > Calendar.THURSDAY) return 0;
        if (t < MarketData.OPEN_MIN) return 0;
        if (t >= MarketData.CLOSE_MIN) return 1;
        return (t - MarketData.OPEN_MIN) / (float) (MarketData.CLOSE_MIN - MarketData.OPEN_MIN);
    }

    /**
     * Under a chart (it runs left to right: the start at the left). «اليوم»: «١٠:٠٠ ص — الخط المتقطع: إغلاق أمس — ٣:٠٠ م»; a longer range: its
     * start («قبل أسبوع», «قبل شهر», the year's first day), «الخط المتقطع: أول الفترة», and its last day.
     */
    private View axis(int which, MarketData.Range r) {
        LinearLayout a = new LinearLayout(this);
        a.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        String start = which == MarketData.TODAY ? clock(10, 0) : which == MarketData.WEEK ? S.t("قبل أسبوع") : which == MarketData.MONTH ? S.t("قبل شهر") : date(r.from);
        String end = which == MarketData.TODAY ? clock(15, 0) : date(r.to);
        a.addView(text(start, 13, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        TextView mid = text(which == MarketData.TODAY ? S.t("الخط المتقطع: إغلاق أمس") : S.t("الخط المتقطع: أول الفترة"), 13, "Regular", DIM);
        mid.setGravity(Gravity.CENTER);
        a.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
        a.addView(text(end, 13, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        return a;
    }

    private static final String[] MONTHS = {S.n("يناير"), S.n("فبراير"), S.n("مارس"), S.n("أبريل"), S.n("مايو"), S.n("يونيو"), S.n("يوليو"),
        S.n("أغسطس"), S.n("سبتمبر"), S.n("أكتوبر"), S.n("نوفمبر"), S.n("ديسمبر")};

    /** «٣ نوفمبر» (a day of the line, kept as UTC midnight); «» when not known */
    private String date(long ms) {
        if (ms <= 0) return "";
        Calendar k = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        k.setTimeInMillis(ms);
        return Digits.of(String.valueOf(k.get(Calendar.DAY_OF_MONTH))) + " " + S.tv(MONTHS[k.get(Calendar.MONTH)]);
    }

    /** the range each chart shows, by its quote, for as long as the screen is open */
    private final java.util.Map<MarketData.Quote, Integer> ranges = new java.util.HashMap<>();

    /**
     * A quote's chart with its range tabs (§6: «اليوم / أسبوع / شهر / سنة»; the three longer ones only when the file has the year's daily
     * closes): the title (null: none), the tabs, the price and its change over the range, the line and its axis. A tab redraws the block.
     */
    private View chartBlock(String title, MarketData.Quote q, float priceSp, int chartDp) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        fillChart(block, title, q, priceSp, chartDp, -1);
        return block;
    }

    private void fillChart(LinearLayout block, String title, MarketData.Quote q, float priceSp, int chartDp, int focusTab) {
        block.removeAllViews();
        int which = MarketData.hasRanges(q) ? ranges.getOrDefault(q, MarketData.TODAY) : MarketData.TODAY;
        MarketData.Range r = MarketData.range(q, which);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        if (title != null) head.addView(text(title, 18, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        else head.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        View focus = null;
        if (MarketData.hasRanges(q)) {
            String[] names = {S.t("اليوم"), S.t("أسبوع"), S.t("شهر"), S.t("سنة")};
            for (int i = 0; i < names.length; i++) {
                final int k = i;
                TextView t = text(names[i], 15, "Medium", i == which ? ACCENT : DIM);
                t.setId(View.generateViewId());
                t.setFocusable(true);
                t.setClickable(true);
                t.setGravity(Gravity.CENTER);
                t.setPadding(dp(16), dp(8), dp(16), dp(8));
                android.graphics.drawable.LayerDrawable bg = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[] {
                        new android.graphics.drawable.StateListDrawable(), new ColorDrawable(i == which ? ACCENT : 0)});
                ((android.graphics.drawable.StateListDrawable) bg.getDrawable(0)).addState(new int[] {android.R.attr.state_focused}, GlassPaint.focused(this, look, 10));
                bg.setLayerGravity(1, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL);
                bg.setLayerHeight(1, dp(2));
                t.setBackground(bg);
                final int rest = i == which ? ACCENT : DIM;
                t.setOnFocusChangeListener((v, has) -> t.setTextColor(has ? look.onFocus : rest));
                t.setOnClickListener(v -> { ranges.put(q, k); fillChart(block, title, q, priceSp, chartDp, k); });
                head.addView(t, new LinearLayout.LayoutParams(-2, -2));
                if (i == focusTab) focus = t;
            }
        }
        block.addView(head, wrap());
        block.addView(priceLine(q, r, priceSp), gap(8));
        int c = Double.isNaN(r.pct(q.p)) ? color(q) : r.pct(q.p) >= 0 ? UP : DOWN;
        block.addView(new MarketChart(this).line(r.line, r.base, c, DIM, true, 2.5f), sized(-1, chartDp, 8));
        block.addView(axis(which, r), gap(8));
        if (focus != null) focus.requestFocus();
    }

    // ---------------------------------------------------------------- «قائمتي»

    private void watch() {
        LinearLayout row = new LinearLayout(this);
        LinearLayout list = card();
        list.setPadding(dp(16), dp(16), dp(16), dp(12));
        LinearLayout hd = new LinearLayout(this);
        hd.setPadding(dp(16), 0, dp(16), dp(8));
        hd.addView(text(S.t("السهم"), 13, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 1.3f));
        hd.addView(text(S.t("اليوم"), 13, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 1.2f));
        hd.addView(text(S.t("السعر"), 13, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 0.8f));
        hd.addView(text(S.t("التغيّر"), 13, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 0.7f));
        list.addView(hd, wrap());
        List<String> codes = watchlist();
        for (String code : codes) list.addView(watchRow(code), gap(4));
        if (codes.isEmpty()) list.addView(note(S.t("قائمتك فارغة. افتح سهمًا من «السوق» واختر «أضف إلى قائمتي».")), wrap());
        row.addView(list, weight(1.75f, 0));

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        LinearLayout gold = card();
        gold.addView(text(S.t("الذهب والنفط"), 18, "Medium", TEXT), wrap());
        if (data != null) {
            commodity(gold, S.t("الذهب، للغرام"), MarketData.GOLD, MarketData.PEG / MarketData.OUNCE_G, S.t("ر.س"));
            commodity(gold, S.t("الذهب، للأونصة"), MarketData.GOLD, 1, "$");
            commodity(gold, S.t("الفضة، للغرام"), MarketData.SILVER, MarketData.PEG / MarketData.OUNCE_G, S.t("ر.س"));
            commodity(gold, S.t("برنت"), MarketData.BRENT, 1, "$");
        }
        side.addView(gold, wrap());
        LinearLayout fx = card();
        LinearLayout fh = new LinearLayout(this);
        fh.setGravity(Gravity.BOTTOM);
        fh.addView(text(S.t("العملات"), 18, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        fh.addView(text(S.t("بالريال"), 13, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        fx.addView(fh, wrap());
        for (int i = 0; i < MarketData.PEGS.length; i++) {
            String[] peg = MarketData.PEGS[i];
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout nm = new LinearLayout(this);
            nm.setOrientation(LinearLayout.VERTICAL);
            nm.addView(text(S.tv(PEG_NAMES[i]), 16, "Regular", TEXT), wrap());
            nm.addView(text(S.t("ربط ثابت"), 12, "Regular", DIM), wrap());
            r.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(ltr(text(MarketData.show(peg[1], Digits.latin()), 17, "Medium", TEXT)), new LinearLayout.LayoutParams(-2, -2));
            fx.addView(r, gap(12));
        }
        side.addView(fx, gap(16));
        row.addView(side, weight(1f, 16));
        content.addView(row, wrap());
    }

    /** MarketData.PEGS' names, marked for the table (tests/markets_page_test.py checks they are the same, in the same order) */
    private static final String[] PEG_NAMES = {S.n("الدولار"), S.n("الدرهم الإماراتي"), S.n("الريال القطري"), S.n("الدينار البحريني")};

    /** one line of «الذهب والنفط»: the name, its day's line, the price (times {@code mul}, in {@code unit}) and the change */
    private void commodity(LinearLayout into, String name, String sym, double mul, String unit) {
        MarketData.Quote q = data.quotes.get(sym);
        if (q == null) return;
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.addView(text(name, 16, "Regular", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams lp = sized(64, 20, 0);
        lp.setMarginEnd(dp(12));
        r.addView(new MarketChart(this).line(q.c, Double.NaN, color(q), DIM, false, 1.5f), lp);
        TextView px = text(money(q.p * mul, 2, unit), 16, "Medium", TEXT);
        px.setGravity(Dir.end());
        r.addView(px, new LinearLayout.LayoutParams(dp(112), -2));
        r.addView(pct(q.pct(), 14), new LinearLayout.LayoutParams(dp(72), -2));
        into.addView(r, gap(12));
    }

    /** the watchlist the screensaver keeps (its «قائمة الأسهم», the stocks card's), in its order */
    private List<String> watchlist() {
        String raw = param("stocks");
        List<String> out = new ArrayList<>();
        for (String s : (raw == null || raw.isEmpty() ? DEFAULT_STOCKS : raw).split(",")) {
            s = s.trim().toUpperCase(java.util.Locale.ROOT);
            if (MarketData.validCode(s) && !out.contains(s)) out.add(s);
        }
        return out;
    }

    private View watchRow(String code) {
        MarketData.Quote q = data == null ? null : data.of(code);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(8), dp(16), dp(8));
        r.addView(nameCol(code, 19), new LinearLayout.LayoutParams(0, -2, 1.3f));
        FrameLayout sp = new FrameLayout(this);
        if (q != null) sp.addView(new MarketChart(this).line(q.c, q.v, color(q), DIM, false, 2f), new FrameLayout.LayoutParams(dp(112), dp(32), Gravity.CENTER_VERTICAL | Dir.start()));
        r.addView(sp, new LinearLayout.LayoutParams(0, dp(36), 1.2f));
        r.addView(ltr(text(q == null ? "—" : price(q.p), 18, "Medium", TEXT)), new LinearLayout.LayoutParams(0, -2, 0.8f));
        r.addView(q == null ? text("", 15, "Regular", DIM) : pct(q.pct(), 16), new LinearLayout.LayoutParams(0, -2, 0.7f));
        focusRow(r, 12);
        r.setOnClickListener(v -> stockCard(code, null));
        return r;
    }

    // ---------------------------------------------------------------- «محفظتي»

    private void portfolio() {
        Portfolio.openEditing(System.currentTimeMillis());
        boolean hide = Portfolio.hidden(this);
        actions.addView(button(hide ? S.t("أظهر المبالغ") : S.t("أخفِ المبالغ"), false, v -> {
            Portfolio.setHidden(this, !Portfolio.hidden(this));
            show(2, false);
            actions.getChildAt(0).requestFocus();
        }), buttonParams(0));
        actions.addView(button(S.t("أضف سهمًا"), false, v -> pick()), buttonParams(8));
        actions.addView(button(S.t("خيارات محفظتي"), false, v -> options()), buttonParams(8));
        // the tabs reach the buttons with the arrow that goes on in reading order
        TextView last = tabViews.get(tabViews.size() - 1);
        View firstButton = actions.getChildAt(0);
        if (Dir.rtl()) last.setNextFocusLeftId(firstButton.getId()); else last.setNextFocusRightId(firstButton.getId());

        List<Holdings.Holding> all = Portfolio.load(this);
        if (all.isEmpty()) {
            LinearLayout e = card();
            e.addView(text(S.t("لا أسهم في محفظتك بعد"), 20, "Medium", TEXT), wrap());
            e.addView(text(S.t("اضغط «أضف سهمًا»، أو أضفها من الجوال: افتح «التحكم من الجوال» وامسح الرمز، ثم «محفظتي»."), 15, "Regular", DIM), gap(8));
            content.addView(e, wrap());
            content.addView(privacyNote(), gap(16));
            return;
        }
        Holdings.Totals t = Holdings.totals(all, data == null ? null : data::of);

        LinearLayout sum = card();
        LinearLayout figs = new LinearLayout(this);
        figs.addView(figure(S.t("القيمة الإجمالية"), hide ? null : money(t.value, 2, S.t("ر.س")), Double.NaN, 32), new LinearLayout.LayoutParams(0, -2, 1f));
        figs.addView(figure(S.t("الربح الكلي"), hide ? null : signedMoney(t.gain), t.gainPct, 26), new LinearLayout.LayoutParams(0, -2, 1f));
        figs.addView(figure(S.t("اليوم"), hide ? null : signedMoney(t.day), t.dayPct, 26), new LinearLayout.LayoutParams(0, -2, 1f));
        sum.addView(figs, wrap());
        float[] w = new float[t.rows.size()];
        int[] cols = new int[t.rows.size()];
        LinearLayout legend = new LinearLayout(this);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < t.rows.size(); i++) {
            Holdings.Row r = t.rows.get(i);
            w[i] = (float) Math.max(0, r.valueSar);
            cols[i] = ALLOC[i % ALLOC.length];
            if (i < 8) legend.addView(legendItem(cols[i], S.tv(MarketData.name(r.h.code)), t.share(r)), new LinearLayout.LayoutParams(-2, -2));
        }
        sum.addView(new MarketChart(this).bar(w, cols, 0), sized(-1, 12, 24));
        sum.addView(legend, gap(12));
        content.addView(sum, wrap());

        LinearLayout table = card();
        table.setPadding(dp(16), dp(16), dp(16), dp(12));
        LinearLayout hd = new LinearLayout(this);
        hd.setPadding(dp(16), 0, dp(16), dp(8));
        String[] heads = {S.t("السهم"), S.t("الكمية"), S.t("متوسط التكلفة"), S.t("السعر"), S.t("القيمة"), S.t("الربح"), S.t("اليوم")};
        for (int i = 0; i < heads.length; i++) hd.addView(text(heads[i], 13, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, COLS[i]));
        table.addView(hd, wrap());
        for (Holdings.Row r : t.rows) table.addView(holdingRow(r, hide), gap(4));
        LinearLayout foot = new LinearLayout(this);
        foot.setPadding(dp(16), dp(12), dp(16), 0);
        foot.addView(privacyNote(), new LinearLayout.LayoutParams(0, -2, 1f));
        boolean anyUsd = false;
        for (Holdings.Holding h : all) anyUsd |= h.usd();
        if (anyUsd) foot.addView(text(S.t("الأسهم الأمريكية بالدولار، والمجموع بالريال على سعر الربط %s", MarketData.show("3.75", Digits.latin())), 13, "Regular", DIM),
                new LinearLayout.LayoutParams(-2, -2));
        table.addView(foot, wrap());
        content.addView(table, gap(16));
    }

    private static final float[] COLS = {1.5f, 0.6f, 0.9f, 0.8f, 1.1f, 1f, 0.8f};

    private View holdingRow(Holdings.Row r, boolean hide) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        String code = r.h.code;
        LinearLayout nm = new LinearLayout(this);
        nm.setOrientation(LinearLayout.VERTICAL);
        nm.addView(text(S.tv(MarketData.name(code)), 18, "Medium", TEXT), wrap());
        nm.addView(ltrStart(text(r.h.usd() ? S.t("%s · بالدولار", code) : code, 13, "Regular", DIM)), wrap());
        row.addView(nm, new LinearLayout.LayoutParams(0, -2, COLS[0]));
        String dots = "••••";
        row.addView(ltrStart(text(hide ? "••" : MarketData.show(trim(r.h.qty()), Digits.latin()), 16, "Regular", TEXT)), new LinearLayout.LayoutParams(0, -2, COLS[1]));
        row.addView(ltrStart(text(hide ? dots : price(r.h.avg()), 16, "Regular", TEXT)), new LinearLayout.LayoutParams(0, -2, COLS[2]));
        row.addView(ltrStart(text(Double.isNaN(r.price) ? "—" : price(r.price), 16, "Medium", TEXT)), new LinearLayout.LayoutParams(0, -2, COLS[3]));
        row.addView(ltrStart(text(hide ? dots : (r.h.usd() ? "$ " : "") + price(r.value), 16, "Regular", TEXT)), new LinearLayout.LayoutParams(0, -2, COLS[4]));
        LinearLayout g = new LinearLayout(this);
        g.setOrientation(LinearLayout.VERTICAL);
        int gc = r.gain >= 0 ? UP : DOWN;
        g.addView(ltrStart(text(hide ? dots : MarketData.show(MarketData.signed(r.gain, 2), Digits.latin()), 14, "Medium", gc)), wrap());
        g.addView(ltrStart(text(MarketData.show(MarketData.signed(r.gainPct, 2) + "%", Digits.latin()), 13, "Regular", gc)), wrap());
        row.addView(g, new LinearLayout.LayoutParams(0, -2, COLS[5]));
        boolean dayKnown = !Double.isNaN(r.dayPct);
        int dc = !dayKnown ? DIM : r.day >= 0 ? UP : DOWN;
        String day = !dayKnown ? "—" : hide ? MarketData.signed(r.dayPct, 2) + "%" : MarketData.signed(r.day, 2);
        row.addView(ltrStart(text(MarketData.show(day, Digits.latin()), 15, "Medium", dc)), new LinearLayout.LayoutParams(0, -2, COLS[6]));
        focusRow(row, 12);
        row.setOnClickListener(v -> stockCard(code, null));
        return row;
    }

    private View privacyNote() {
        return text(S.t("تبقى في هذا التلفزيون فقط، ولا تُرسل في أي تقرير ولا تنتقل مع الإعدادات"), 13, "Regular", DIM);
    }

    /** a total: its name, the amount (null: hidden «••••»), and the percent beside it (NaN: none) */
    private View figure(String name, String amount, double pct, float sp) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.addView(text(name, 15, "Regular", DIM), wrap());
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.BOTTOM);
        int c = Double.isNaN(pct) ? TEXT : pct >= 0 ? UP : DOWN;
        line.addView(ltr(text(amount == null ? "••••" : amount, sp, "Medium", c)), new LinearLayout.LayoutParams(-2, -2));
        if (!Double.isNaN(pct)) {
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
            pp.setMarginStart(dp(12));
            line.addView(ltr(text(MarketData.show(MarketData.signed(pct, 2) + "%", Digits.latin()), 15, "Regular", c)), pp);
        }
        f.addView(line, gap(8));
        return f;
    }

    private View legendItem(int color, String name, double share) {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.CENTER_VERTICAL);
        View sw = new View(this);
        sw.setBackground(GlassPaint.fill(this, color, 3, 0));
        l.addView(sw, new LinearLayout.LayoutParams(dp(12), dp(12)));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
        np.setMarginStart(dp(8));
        np.setMarginEnd(dp(24));
        l.addView(text(name + " " + MarketData.show(Math.round(share) + "%", Digits.latin()), 14, "Regular", TEXT), np);
        return l;
    }

    // ---------------------------------------------------------------- the stock card

    /** OK on a stock: its price, its day, the open and the day's range; the watchlist and the bar; in the portfolio, «عدّل» and «احذف». */
    private void stockCard(String code, String title) {
        MarketData.Quote q = data == null ? null : code.startsWith("^") ? data.quotes.get(code) : data.of(code);
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 760);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.BOTTOM);
        head.addView(text(title != null ? title : S.tv(MarketData.name(code)), 26, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        boolean sa = code.startsWith("^") || MarketData.isSaudi(code);
        String sub = code.startsWith("^") ? S.t("مؤشر السوق الرئيسية · تداول") : sa ? S.t("%s · تداول · ر.س", code) : S.t("%s · بالدولار", code);
        head.addView(ltrStart(text(sub, 14, "Regular", DIM)), new LinearLayout.LayoutParams(-2, -2));
        box.addView(head, wrap());
        if (q != null) {
            box.addView(chartBlock(null, q, 40, 130), gap(8));
            LinearLayout stats = new LinearLayout(this);
            stats.addView(stat(S.t("الافتتاح"), price(q.open())), weight(1f, 0));
            stats.addView(stat(S.t("نطاق اليوم"), price(q.low()) + " – " + price(q.high())), weight(1f, 12));
            if (!Double.isNaN(q.yl) && !Double.isNaN(q.yh)) stats.addView(stat(S.t("نطاق ٥٢ أسبوعًا"), price(q.yl) + " – " + price(q.yh)), weight(1f, 12));
            if (!Double.isNaN(q.vol) && q.vol > 0) stats.addView(stat(S.t("الكمية"), S.t("%s مليون", MarketData.show(MarketData.num(q.vol / 1e6, 1), Digits.latin()))), weight(1f, 12));
            box.addView(stats, gap(16));
        } else {
            box.addView(note(S.t("لا سعر لهذا الرمز في ملف الأسعار بعد")), gap(12));
        }
        LinearLayout buttons = new LinearLayout(this);
        List<View> made = new ArrayList<>();
        List<Holdings.Holding> mine = Portfolio.load(this);
        int at = Holdings.find(mine, code);
        boolean inPortfolio = tab == 2 && at >= 0;
        if (inPortfolio) {
            Holdings.Holding h = mine.get(at);
            made.add(button(S.t("عدّل"), true, v -> { d.dismiss(); numberPad(code, h.qty(), h.avg()); }));
            made.add(button(S.t("احذف"), false, v -> confirm(S.t("أحذف «%s» من محفظتك؟", S.tv(MarketData.name(code))), () -> {
                d.dismiss();
                Portfolio.save(this, Holdings.remove(Portfolio.load(this), code));
                show(2, false);
            })));
        } else if (!code.startsWith("^")) {
            boolean listed = watchlist().contains(code);
            made.add(button(listed ? S.t("أزل من قائمتي") : S.t("أضف إلى قائمتي"), true, v -> { d.dismiss(); toggleWatch(code); }));
        }
        String key = barKey(code);
        if (key != null) {
            boolean onBar = bar().contains(key);
            made.add(button(onBar ? S.t("أخفه من شريط الأسواق") : S.t("أظهره في شريط الأسواق"), false, v -> { d.dismiss(); toggleBar(key); }));
        }
        for (int i = 0; i < made.size(); i++) buttons.addView(made.get(i), weight(1f, i == 0 ? 0 : 16));
        if (!made.isEmpty()) box.addView(buttons, gap(20));
        d.show();
        if (!made.isEmpty()) made.get(0).requestFocus();
    }

    private View stat(String name, String value) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(dp(16), dp(12), dp(16), dp(12));
        s.setBackground(GlassPaint.panel(this, look, 12));
        s.addView(text(name, 14, "Regular", DIM), wrap());
        s.addView(ltrStart(text(value, 18, "Medium", TEXT)), gap(8));
        return s;
    }

    private String barKey(String code) {
        for (String[] k : BAR_KEYS) if (k[0].equals(code)) return k[1];
        return null;
    }

    // ---------------------------------------------------------------- changing the screensaver's watchlist and bar

    private List<String> bar() {
        String raw = param("markets");
        return new ArrayList<>(Arrays.asList((raw == null || raw.isEmpty() ? "tasi,spx,btc" : raw).split(",")));
    }

    private void toggleWatch(String code) {
        List<String> w = watchlist();
        if (w.contains(code)) {
            if (w.size() == 1) { toast(S.t("تبقى في قائمتك سهم واحد على الأقل")); return; }
            w.remove(code);
        } else {
            if (w.size() >= 8) { toast(S.t("قائمتك ممتلئة: %s أسهم", digits(8))); return; }
            w.add(code);
        }
        if (saverSet("stocks", String.join(",", w))) { toast(S.t("تم الحفظ")); show(tab, false); }
    }

    private void toggleBar(String key) {
        List<String> b = bar();
        if (b.contains(key)) {
            if (b.size() == 1) { toast(S.t("يبقى في الشريط سوق واحد على الأقل")); return; }
            b.remove(key);
        } else {
            if (b.size() >= 4) { toast(S.t("الشريط ممتلئ: %s أسواق. أخفِ واحدًا أولًا", digits(4))); return; }
            b.add(key);
        }
        if (saverSet("markets", String.join(",", b))) toast(S.t("تم الحفظ"));
    }

    /** one setting of the screensaver's, through the same checks as its own settings (ConfigProvider phone_set, SaverReceiver.set) */
    private boolean saverSet(String key, String value) {
        Bundle e = new Bundle();
        e.putString(key, value);
        try {
            Bundle r = getContentResolver().call(SAVER, "phone_set", null, e);
            if (r != null && r.getBoolean("ok")) { saved.put(key, value); return true; }
        } catch (RuntimeException ignored) { }
        toast(S.t("ما حُفظ: حدّث تطبيق «شاشة تلفزيونك»"));
        return false;
    }

    /** what this screen saved a moment ago: the settings are read through a short shared cache (ConfigSnapshot.SHARE_MS) */
    private final java.util.Map<String, String> saved = new java.util.HashMap<>();

    /** one value of the screensaver's settings fragment ("a=b&c=d"), decoded; null when it is not there */
    private String param(String name) {
        if (saved.containsKey(name)) return saved.get(name);
        String f = ConfigSnapshot.read(this).fragment;
        if (f == null) return null;
        for (String kv : f.split("&")) {
            int eq = kv.indexOf('=');
            if (eq > 0 && kv.substring(0, eq).equals(name)) return Uri.decode(kv.substring(eq + 1));
        }
        return null;
    }

    // ---------------------------------------------------------------- adding on the TV: the stock, then the number pad

    private void pick() {
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 760);
        box.addView(text(S.t("أضف سهمًا"), 24, "Medium", TEXT), wrap());
        box.addView(text(S.t("أسهم أكبر %s شركة في تداول. لسهم أمريكي أو رمز آخر: «أسهل من الجوال».", digits(MarketData.TADAWUL.length)), 14, "Regular", DIM), gap(8));
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        sv.addView(grid);
        List<String> codes = new ArrayList<>();
        for (String[] t : MarketData.TADAWUL) codes.add(t[0]);
        for (String w : watchlist()) if (!codes.contains(w)) codes.add(w);
        View first = null;
        LinearLayout line = null;
        for (int i = 0; i < codes.size(); i++) {
            if (i % 4 == 0) { line = new LinearLayout(this); grid.addView(line, gap(i == 0 ? 0 : 8)); }
            final String code = codes.get(i);
            View b = button(S.tv(MarketData.name(code)), false, v -> {
                d.dismiss();
                List<Holdings.Holding> mine = Portfolio.load(this);
                int at = Holdings.find(mine, code);
                numberPad(code, at >= 0 ? mine.get(at).qty() : Double.NaN, at >= 0 ? mine.get(at).avg() : Double.NaN);
            });
            if (first == null) first = b;
            line.addView(b, weight(1f, i % 4 == 0 ? 0 : 8));
        }
        while (line != null && line.getChildCount() < 4) line.addView(new View(this), weight(1f, 8));
        box.addView(sv, sized(-1, 300, 16));
        View phone = button(S.t("أسهل من الجوال"), false, v -> { d.dismiss(); fromPhone(); });
        box.addView(phone, gap(16));
        d.show();
        if (first != null) first.requestFocus();
    }

    /** the large number pad for the quantity and the average cost (§7, markets-portfolio-add-tv.png) */
    private void numberPad(String code, double qty, double avg) {
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 480);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.BOTTOM);
        head.addView(text(S.tv(MarketData.name(code)), 26, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(ltrStart(text(MarketData.isSaudi(code) ? S.t("%s · ر.س", code) : S.t("%s · بالدولار", code), 14, "Regular", DIM)), new LinearLayout.LayoutParams(-2, -2));
        box.addView(head, wrap());
        final StringBuilder[] values = {new StringBuilder(Double.isNaN(qty) ? "" : trim(qty)), new StringBuilder(Double.isNaN(avg) ? "" : trim(avg))};
        final int[] field = {0};
        final TextView[] shown = new TextView[2];
        final View[] boxes = new View[2];
        LinearLayout fields = new LinearLayout(this);
        String[] labels = {S.t("الكمية"), S.t("متوسط التكلفة")};
        Runnable paint = () -> {
            for (int i = 0; i < 2; i++) {
                shown[i].setText(MarketData.show(values[i].toString(), Digits.latin()) + (i == field[0] ? "|" : ""));
                boxes[i].setBackground(i == field[0] ? GlassPaint.focused(this, look, 14) : GlassPaint.panel(this, look, 14));
                ((TextView) ((LinearLayout) boxes[i]).getChildAt(0)).setTextColor(i == field[0] ? look.onFocusSub : DIM);
                shown[i].setTextColor(i == field[0] ? look.onFocus : TEXT);
            }
        };
        for (int i = 0; i < 2; i++) {
            final int k = i;
            LinearLayout f = new LinearLayout(this);
            f.setOrientation(LinearLayout.VERTICAL);
            f.setPadding(dp(16), dp(12), dp(16), dp(12));
            f.addView(text(labels[i], 14, "Regular", DIM), wrap());
            shown[i] = ltrStart(text("", 26, "Medium", TEXT));
            f.addView(shown[i], gap(8));
            f.setClickable(true);
            f.setOnClickListener(v -> { field[0] = k; paint.run(); });
            boxes[i] = f;
            fields.addView(f, weight(1f, i == 0 ? 0 : 16));
        }
        box.addView(fields, gap(16));
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫"};
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        View five = null;
        for (int r = 0; r < 4; r++) {
            LinearLayout line = new LinearLayout(this);
            line.setLayoutDirection(Dir.layout());
            for (int c = 0; c < 3; c++) {
                final String k = keys[r * 3 + c];
                View b = button(k.equals("⌫") ? "⌫" : MarketData.show(k, Digits.latin()), false, v -> {
                    StringBuilder s = values[field[0]];
                    if (k.equals("⌫")) { if (s.length() > 0) s.setLength(s.length() - 1); }
                    else if (k.equals(".")) { if (s.indexOf(".") < 0 && s.length() < 9) s.append(s.length() == 0 ? "0." : "."); }
                    else if (s.length() < 10) s.append(k);
                    paint.run();
                });
                ((TextView) b).setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                if (k.equals("5")) five = b;
                line.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1f));
                if (c > 0) ((LinearLayout.LayoutParams) b.getLayoutParams()).setMarginStart(dp(8));
            }
            pad.addView(line, gap(r == 0 ? 0 : 8));
        }
        box.addView(pad, gap(16));
        LinearLayout bottom = new LinearLayout(this);
        View save = button(S.t("احفظ"), true, v -> {
            Holdings.Buy b = Holdings.buy(values[0].toString(), values[1].toString());
            if (b == null) { toast(S.t("اكتب الكمية ومتوسط التكلفة")); return; }
            List<Holdings.Holding> next = Holdings.put(Portfolio.load(this), code, java.util.Collections.singletonList(b));
            if (next == null) { toast(S.t("المحفظة ممتلئة: %s سهمًا", digits(Holdings.MAX))); return; }
            if (!Portfolio.save(this, next)) { toast(S.t("ما حُفظ")); return; }
            d.dismiss();
            show(2, false);
        });
        save.setBackground(GlassPaint.focused(this, look, ACCENT, 14));
        bottom.addView(save, weight(1f, 0));
        bottom.addView(button(S.t("أسهل من الجوال"), false, v -> { d.dismiss(); fromPhone(); }), weight(1f, 16));
        box.addView(bottom, gap(16));
        // the remote's digits type too; ◀▶ on the fields' row moves between them; OK on the pad presses a key
        d.setOnKeyListener((di, keyCode, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                StringBuilder s = values[field[0]];
                if (s.length() < 10) s.append((char) ('0' + keyCode - KeyEvent.KEYCODE_0));
                paint.run();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) { StringBuilder s = values[field[0]]; if (s.length() > 0) s.setLength(s.length() - 1); paint.run(); return true; }
            return false;
        });
        // a field is chosen by moving up to it: two focusable tabs over the pad
        for (int i = 0; i < 2; i++) {
            final int k = i;
            boxes[i].setFocusable(true);
            boxes[i].setOnFocusChangeListener((v, has) -> { if (has) { field[0] = k; paint.run(); } });
        }
        paint.run();
        d.show();
        (five != null ? five : save).requestFocus();
    }

    /** «أسهل من الجوال»: the phone page's QR (pairing), opened on «محفظتي»; the phone may edit for a while */
    private void fromPhone() {
        Portfolio.openEditing(System.currentTimeMillis());
        try {
            startActivity(new Intent(this, PhoneActivity.class).putExtra(PhoneActivity.EXTRA_PORTFOLIO, true));
        } catch (RuntimeException e) {
            toast(S.t("ما قدرنا نفتح «التحكم من الجوال»"));
        }
    }

    // ---------------------------------------------------------------- the tab's own options: the lock and the wipe

    private void options() {
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 600);
        box.addView(text(S.t("خيارات محفظتي"), 24, "Medium", TEXT), wrap());
        boolean pinSet = Parental.state(this).pinSet;
        boolean on = Portfolio.locked(this) && pinSet;
        View lock = settingRow(S.t("اقفل محفظتي"), on ? S.t("مفعّل · تسأل عن الرقم السري قبل فتحها") : pinSet ? S.t("متوقف") : S.t("متوقف · ضع رقمًا سريًا من الرقابة الأبوية أولًا"), on, v -> {
            if (!Parental.state(this).pinSet) { toast(S.t("ضع رقمًا سريًا من الرقابة الأبوية أولًا")); return; }
            if (Portfolio.locked(this)) {   // turning it off asks for the PIN too
                Parental.confirm(this, Parental.adhanFirst(this), S.n("محفظتي"), () -> { Portfolio.setLocked(this, false); d.dismiss(); options(); }, () -> { });
            } else {
                Portfolio.setLocked(this, true);
                unlocked = true;   // given already to be here
                d.dismiss();
                options();
            }
        });
        box.addView(lock, gap(16));
        View wipe = settingRow(S.t("احذف المحفظة"), S.t("كل الأسهم وعمليات الشراء، من هذا التلفزيون"), false, v -> confirm(S.t("أحذف المحفظة كلها من هذا التلفزيون؟ لا يمكن التراجع."), () -> {
            Portfolio.wipe(this);
            d.dismiss();
            show(2, false);
            toast(S.t("حُذفت المحفظة"));
        }));
        box.addView(wipe, gap(8));
        d.show();
        lock.requestFocus();
    }

    /**
     * A setting's row as the settings show it (OnOff, the owner's toggle style I): no switch; the value starts with its state word, and while it is
     * on three green marks stand at the row's start and that word is green, in the green that reads on what is under it.
     */
    private View settingRow(String name, String value, boolean on, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(16), dp(12));
        r.setId(View.generateViewId());
        r.setFocusable(true);
        r.setClickable(true);
        android.widget.ImageView marks = new android.widget.ImageView(this);
        if (on) {
            marks.setImageResource(R.drawable.ic_on);
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(7), dp(22));
            mp.setMarginEnd(dp(12));
            r.addView(marks, mp);
        }
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(name, 18, "Medium", TEXT), vt = text("", 14, "Regular", DIM);
        words.addView(title, wrap());
        words.addView(vt, gap(4));
        r.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        View.OnFocusChangeListener paint = (v, has) -> {
            r.setBackground(has ? GlassPaint.focused(this, look, 14) : GlassPaint.panel(this, look, 14));
            int green = OnOff.green(has ? look.focus : look.light ? 0xFFFFFFFF : 0xFF000000);
            title.setTextColor(has ? look.onFocus : TEXT);
            SpannableStringBuilder sv = new SpannableStringBuilder(value);
            if (on) sv.setSpan(new ForegroundColorSpan(green), 0, OnOff.wordEnd(value), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            vt.setText(sv);
            vt.setTextColor(has ? look.onFocusSub : DIM);
            if (on) marks.setColorFilter(green);
        };
        r.setOnFocusChangeListener(paint);
        paint.onFocusChange(r, false);
        r.setOnClickListener(click);
        return r;
    }

    private void confirm(String question, Runnable yes) {
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 520);
        box.addView(text(question, 19, "Medium", TEXT), wrap());
        LinearLayout b = new LinearLayout(this);
        View no = button(S.t("لا"), false, v -> d.dismiss());
        View ok = button(S.t("نعم"), false, v -> { d.dismiss(); yes.run(); });
        ((TextView) ok).setTextColor(look.warn);
        GlassPaint.follow(ok, look);
        b.addView(ok, weight(1f, 0));
        b.addView(no, weight(1f, 16));
        box.addView(b, gap(20));
        d.show();
        no.requestFocus();   // the safe answer under a stray press
    }

    // ---------------------------------------------------------------- pieces

    /** a stock's row in a list: the name, its price and its change; OK opens its card */
    private View stockRow(String code, boolean withLine) {
        MarketData.Quote q = data.of(code);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(8), dp(16), dp(8));
        r.addView(text(S.tv(MarketData.name(code)), 17, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(ltr(text(price(q.p), 16, "Regular", DIM)), new LinearLayout.LayoutParams(dp(96), -2));
        r.addView(pct(q.pct(), 16), new LinearLayout.LayoutParams(dp(96), -2));
        focusRow(r, 12);
        r.setOnClickListener(v -> stockCard(code, null));
        return r;
    }

    private View volumeRow(String code) {
        MarketData.Quote q = data.of(code);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(8), dp(16), dp(8));
        r.addView(text(S.tv(MarketData.name(code)), 17, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(text(S.t("%s م سهم", MarketData.show(MarketData.num(q.vol / 1e6, 1), Digits.latin())), 16, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        focusRow(r, 12);
        r.setOnClickListener(v -> stockCard(code, null));
        return r;
    }

    private View nameCol(String code, float sp) {
        LinearLayout nm = new LinearLayout(this);
        nm.setOrientation(LinearLayout.VERTICAL);
        nm.addView(text(S.tv(MarketData.name(code)), sp, "Medium", TEXT), wrap());
        nm.addView(ltrStart(text(code, 14, "Regular", DIM)), wrap());
        return nm;
    }

    /** the big price and its change: «٩٬٨٨٢٫٨٥  +٤٤٫٥٧ (+٠٫٤٥٪)» */
    private View priceLine(MarketData.Quote q, MarketData.Range r, float sp) {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.BOTTOM);
        l.addView(ltr(text(price(q.p), sp, "Medium", TEXT)), new LinearLayout.LayoutParams(-2, -2));
        if (!Double.isNaN(r.pct(q.p))) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.setMarginStart(dp(16));
            p.bottomMargin = dp(4);
            String s = MarketData.signed(r.change(q.p), 2) + "  (" + MarketData.signed(r.pct(q.p), 2) + "%)";
            l.addView(ltr(text(MarketData.show(s, Digits.latin()), sp * 0.5f, "Medium", r.pct(q.p) >= 0 ? UP : DOWN)), p);
        }
        return l;
    }

    private TextView pct(double v, float sp) {
        TextView t = text(Double.isNaN(v) ? "—" : MarketData.show(MarketData.signed(v, 2) + "%", Digits.latin()), sp, "Medium", Double.isNaN(v) ? DIM : v >= 0 ? UP : DOWN);
        return ltrStart(t);
    }

    private int color(MarketData.Quote q) {
        Boolean up = q.up();
        return up == null ? DIM : up ? UP : DOWN;
    }

    private String price(double v) { return MarketData.show(MarketData.num(v, 2), Digits.latin()); }

    private String money(double v, int dec, String unit) {
        String n = MarketData.show(MarketData.num(v, dec), Digits.latin());
        return "$".equals(unit) ? "$ " + n : n + " " + unit;
    }

    private String signedMoney(double v) { return MarketData.show(MarketData.signed(v, 2), Digits.latin()); }

    private static String trim(double v) { return Holdings.plain(v); }

    private String digits(int n) { return Digits.of(String.valueOf(n)); }

    /** a panel the screen is made of */
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(16), dp(20), dp(16));
        c.setBackground(GlassPaint.panel(this, look, 18));
        return c;
    }

    /** a row inside a panel: nothing at rest, the focus fill while focused, its words in the focus ink */
    private void focusRow(View r, float radius) {
        r.setId(View.generateViewId());
        r.setFocusable(true);
        r.setClickable(true);
        r.setBackground(GlassPaint.row(this, look, radius));
        GlassPaint.follow(r, look);
    }

    private View button(String label, boolean primary, View.OnClickListener click) {
        TextView b = text(label, 17, "Medium", TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(20), dp(12), dp(20), dp(12));
        b.setId(View.generateViewId());
        b.setFocusable(true);
        b.setClickable(true);
        GlassPaint.card(b, look, 14);
        b.setOnClickListener(click);
        return b;
    }

    private LinearLayout.LayoutParams buttonParams(int startGap) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.setMarginStart(dp(startGap));
        return p;
    }

    private Dialog dialog() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    private LinearLayout dialogBox(Dialog d, int widthDp) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Dir.layout());
        box.setPadding(dp(32), dp(28), dp(32), dp(28));
        box.setBackground(GlassPaint.dialog(this, look));
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(widthDp)), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return box;
    }

    private TextView note(String s) { return text(s, 16, "Regular", DIM); }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);   // already said in «اللغة» by the caller (S.t, S.tv)
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
        t.setTextDirection(Dir.text());
        t.setIncludeFontPadding(true);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    /** a number or a code: its own left-to-right order, at the start of its box */
    private TextView ltrStart(TextView t) {
        t.setTextDirection(View.TEXT_DIRECTION_LTR);
        t.setGravity(Dir.start() | Gravity.CENTER_VERTICAL);
        return t;
    }

    /** a number on its own: its own left-to-right order */
    private TextView ltr(TextView t) {
        t.setTextDirection(View.TEXT_DIRECTION_LTR);
        return t;
    }

    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }

    private LinearLayout.LayoutParams gap(int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(topDp);
        return p;
    }

    private LinearLayout.LayoutParams sized(int widthDp, int heightDp, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(widthDp < 0 ? -1 : dp(widthDp), dp(heightDp));
        p.topMargin = dp(topDp);
        return p;
    }

    private LinearLayout.LayoutParams weight(float w, int startDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, w);
        p.setMarginStart(dp(startDp));
        return p;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
