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
    static final String[] TABS = {"market", "watch", "portfolio", "fx"};
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
    private LinearLayout tabRow, actions, content, ctl;   // ctl: the controls row's tab-dependent button («أخفِ المبالغ» on «محفظتي»)
    private View fxBtn;
    private ScrollView scroll;
    private int ticks;

    // ---- «مباشر | عادي»: while this page (or a stock card on it) is in front, the TV asks Yahoo for the symbols on screen every 30 s (60, then 120 s after
    // failures, the 5-minute file meanwhile); a pause, a stop or «عادي» ends it. Nothing is stored: the last good answer lives in `data`.
    private boolean liveOn = true, resumed, shownLive;
    private int liveFails, liveRun;
    private long liveOkAt;
    private final java.util.LinkedHashSet<String> onScreen = new java.util.LinkedHashSet<>();
    private final List<TextView> liveButtons = new ArrayList<>();
    private LinearLayout cardBlock;
    private TextView cardNote;
    private String cardCode;
    private final Runnable liveTick = this::liveAsk;

    private View liveSwitch() {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutDirection(Dir.layout());
        String[] names = {S.t("مباشر"), S.t("عادي")};
        for (int i = 0; i < 2; i++) {
            final boolean on = i == 0;
            TextView t = text(names[i], 16, "Medium", DIM);
            t.setFocusable(true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(16), dp(8), dp(16), dp(8));
            t.setOnFocusChangeListener((v, has) -> paintLive());
            t.setOnClickListener(v -> setLive(on));
            liveButtons.add(t);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            if (i > 0) p.setMarginStart(dp(12));
            row.addView(t, p);
        }
        paintLive();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, dp(12), 0);   // 24 px to «صرف العملات»
        return row;
    }

    /** toggle style I, as everywhere in the settings: the chosen word green with the triangles at its start; the focused one on the focus fill; no box, no bar */
    private void paintLive() {
        for (int i = 0; i < liveButtons.size(); i++) {
            TextView t = liveButtons.get(i);
            boolean chosen = (i == 0) == liveOn;
            t.setTextColor(t.hasFocus() ? look.onFocus : chosen ? ACCENT : DIM);
            t.setBackground(t.hasFocus() ? GlassPaint.focused(this, look, 12) : new ColorDrawable(0));
            android.graphics.drawable.Drawable on = chosen ? getDrawable(R.drawable.ic_on).mutate() : null;
            if (on != null) { on.setBounds(0, 0, dp(7), dp(22)); on.setTint(t.hasFocus() ? look.onFocus : ACCENT); }
            t.setCompoundDrawablesRelative(on, null, null, null);
            t.setCompoundDrawablePadding(dp(8));
        }
    }

    /** live mode keeps the screen awake while the page is in front (our screensaver started over it after 45 s of no key, step 27) */
    private void keepAwake() {
        if (liveOn && resumed) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setLive(boolean on) {
        if (on == liveOn) return;
        liveOn = on;
        keepAwake();
        liveRun++;
        liveFails = 0;
        main.removeCallbacks(liveTick);
        paintLive();
        if (on) { if (resumed) main.postDelayed(liveTick, 500); return; }
        liveOkAt = 0;
        MarketData file = MarketFile.last();   // the file's own prices again (memory only)
        if (file != null) data = file;
        redraw();
    }

    private void liveAsk() {
        if (!liveOn || !resumed || isFinishing() || isDestroyed()) return;
        final int run = liveRun;
        final java.util.LinkedHashSet<String> syms = new java.util.LinkedHashSet<>(onScreen);
        if (cardCode != null) syms.add(cardCode);
        if (syms.isEmpty() || data == null) { main.postDelayed(liveTick, MarketData.liveDelay(0)); return; }
        new Thread(() -> {
            java.util.Map<String, MarketData.Live> m = MarketFile.live(syms);   // one request for all of them
            main.post(() -> liveDone(run, m));
        }, "markets-live").start();
    }

    private void liveDone(int run, java.util.Map<String, MarketData.Live> m) {
        if (run != liveRun || !resumed || !liveOn || isFinishing() || isDestroyed()) return;   // paused, stopped or switched off meanwhile: dropped
        long now = System.currentTimeMillis();
        if (m.isEmpty() || data == null) liveFails++;
        else {
            liveFails = 0;
            liveOkAt = now;
            data = data.withLive(m);
            redraw();
            if (cardCode != null && cardBlock != null && data.of(cardCode) != null) {
                View f = cardBlock.findFocus();
                ViewGroup head = (ViewGroup) cardBlock.getChildAt(0);
                int at = f == null || head == null ? -1 : head.indexOfChild(f) - 1;
                fillChart(cardBlock, null, data.of(cardCode), 40, 130, at);
            }
        }
        if (shownLive != MarketData.liveFresh(liveOkAt, now)) redraw();   // «مباشر» to «الأسعار متأخرة» (or back) in the footer
        if (cardNote != null) cardNote.setText(sourceText());
        header();
        main.postDelayed(liveTick, MarketData.liveDelay(liveFails));
    }

    /** the live prices a data object holds, as a map (to put them over a newer file) */
    private static java.util.Map<String, MarketData.Live> liveOf(MarketData m) {
        java.util.Map<String, MarketData.Live> out = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String, Long> e : m.liveAt.entrySet()) {
            MarketData.Quote q = m.quotes.get(e.getKey());
            if (q != null) out.put(e.getKey(), new MarketData.Live(q.p, q.v, e.getValue()));
        }
        return out;
    }

    private String sourceText() {
        shownLive = liveOn && MarketData.liveFresh(liveOkAt, System.currentTimeMillis());
        String name = data == null ? "Yahoo Finance" : data.sourceName();
        return shownLive ? S.t("مباشر · المصدر: %s", name) : S.t("الأسعار متأخرة · المصدر: %s", name);
    }

    /** a row's live label: Tadawul's «متأخرة ~١٥ د» (delayed at the source), any other market its quote's time; nothing when it is not live */
    private String liveTag(String code) {
        Long at = data == null || !liveOn || !MarketData.liveFresh(liveOkAt, System.currentTimeMillis()) ? null : data.liveAt.get(code);
        if (at == null) return "";
        if ("delayed".equals(MarketData.liveKind(code))) return S.t("متأخرة ~%s د", digits(15));
        Calendar k = Calendar.getInstance(TimeZone.getTimeZone(zone()));
        k.setTimeInMillis(at == 0 ? System.currentTimeMillis() : at);
        return clock(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE));
    }

    /** the tab drawn again with the focus and the scroll where they were (a price changed; the buttons keep their place) */
    private void redraw() {
        if (tab < 0) return;
        List<View> before = new ArrayList<>();
        collectFocusable(content, before);
        collectFocusable(actions, before);
        int at = before.indexOf(getCurrentFocus()), sy = scroll.getScrollY();
        show(tab, false);
        List<View> after = new ArrayList<>();
        collectFocusable(content, after);
        collectFocusable(actions, after);
        if (at >= 0 && at < after.size()) after.get(at).requestFocus();
        else if (!(getCurrentFocus() != null && (tabViews.contains(getCurrentFocus()) || liveButtons.contains(getCurrentFocus())))) {   // nothing of the page had the focus (a dialog just closed): «أضف سهمًا» on «محفظتي», else the first thing in the tab, never «مباشر»
            View to = tab == 2 && actions.getChildCount() > 0 ? actions.getChildAt(0) : after.isEmpty() ? null : after.get(0);
            if (to != null) to.requestFocus();
        }
        scroll.post(() -> scroll.scrollTo(0, sy));
    }

    private static void collectFocusable(View v, List<View> out) {
        if (v.isFocusable() && v.getVisibility() == View.VISIBLE) out.add(v);
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) collectFocusable(((ViewGroup) v).getChildAt(i), out);
    }

    /** the file again, on the worker, while the page stays open; the same lifecycle guard as the first load */
    private void refreshPrices() {
        final int gen = loadGen;
        new Thread(() -> deliver(gen, MarketFile.load(getApplicationContext(), false)), "markets-refresh").start();
    }

    private final Runnable minute = new Runnable() {
        @Override public void run() {
            header();
            // the phone may edit while the tab is on the screen (and, with «اقفل محفظتي», only after the PIN on this screen)
            if (tab == 2 && (unlocked || !Portfolio.locked(MarketsActivity.this) || !Parental.state(MarketsActivity.this).pinSet)) Portfolio.openEditing(Portfolio.now());
            if (++ticks % 5 == 0) refreshPrices();   // CI's file is new every 5 minutes (a late one shows its own time, no error)
            main.postDelayed(this, 60_000);
        }
    };
    private final Runnable changed = () -> main.post(() -> { if (tab == 2 && !isFinishing()) redraw(); });

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
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();   // the 5 % safe area on all four sides, from the real viewport (at density 1.5, 60/28 dp were 90/42 px, outside it); the lists scroll in above the bottom one
        page.setPadding(Math.round(dm.widthPixels * 0.05f), Math.round(dm.heightPixels * 0.05f), Math.round(dm.widthPixels * 0.05f), Math.round(dm.heightPixels * 0.05f));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        // the title; under it ONE controls row (the live switch, «صرف العملات», «أخفِ المبالغ» on «محفظتي»: small buttons, one height, 24 px apart); then the one quiet
        // status line; then the tabs (السوق / قائمتي / محفظتي) with the tab's own buttons at their end
        page.addView(text(S.t("الأسواق"), 27, "Medium", TEXT), new LinearLayout.LayoutParams(-2, -2));
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.addView(liveSwitch(), new LinearLayout.LayoutParams(-2, dp(40)));
        fxBtn = ctlButton(S.t("صرف العملات"), v -> want(3));
        controls.addView(fxBtn, new LinearLayout.LayoutParams(-2, -2));   // its text's height plus its own padding (a fixed 40 dp cut the letters in half, step 28)
        ctl = new LinearLayout(this);
        ctl.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cp2 = new LinearLayout.LayoutParams(-2, -2);
        cp2.setMarginStart(dp(12));
        controls.addView(ctl, cp2);
        LinearLayout.LayoutParams ctp = new LinearLayout.LayoutParams(-1, -2);
        ctp.topMargin = dp(8);
        page.addView(controls, ctp);
        state = text("", 12, "Regular", DIM);   // «تداول مغلق · يفتح الأربعاء ١٠:٠٠ ص · آخر تحديث ٧:٠١ م»: one quiet line, wrapped once if it must
        state.setMaxLines(2);
        updated = state;
        LinearLayout.LayoutParams sp0 = new LinearLayout.LayoutParams(-1, -2);
        sp0.topMargin = dp(8);
        page.addView(state, sp0);

        // the tabs, and at the row's end the tab's own buttons
        tabRow = new LinearLayout(this);
        tabRow.setGravity(Gravity.BOTTOM);
        String[] names = NEWS ? new String[] {S.t("السوق"), S.t("قائمتي"), S.t("محفظتي"), S.t("الأخبار")} : new String[] {S.t("السوق"), S.t("قائمتي"), S.t("محفظتي")};
        for (int i = 0; i < names.length; i++) {
            final int k = i == 3 ? 4 : i;   // «صرف العملات» is tab 3 (its button, not a tab); the news would be 4
            TextView t = text(names[i], 17, "Medium", DIM);
            t.setId(View.generateViewId());
            t.setFocusable(true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(16), dp(8), dp(16), dp(8));
            t.setOnFocusChangeListener((v, has) -> { if (has && k != tab && !rebuilding) want(k); paintTabs(); });
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

        data = MarketFile.last();   // memory only: the files are read on the worker (onResume); until then «جارٍ التحميل…»
        loading = data == null;
        int start = Arrays.asList(TABS).indexOf(getIntent().getStringExtra(EXTRA_TAB));
        tab = -1;
        want(Math.max(0, start));
        (tab >= 0 && tab < tabViews.size() ? tabViews.get(tab) : fxBtn).requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Portfolio.listener = changed;
        main.removeCallbacks(minute);
        minute.run();
        resumed = true;
        keepAwake();
        if (builtIn == null) builtIn = S.lang();
        main.removeCallbacks(langWatch);
        main.postDelayed(langWatch, 2000);
        liveRun++;
        liveFails = 0;
        main.removeCallbacks(liveTick);
        if (liveOn) main.postDelayed(liveTick, 1500);   // live while the page is in front
        final int gen = ++loadGen;
        new Thread(() -> {
            android.content.Context app = getApplicationContext();
            if (data == null) deliver(gen, MarketFile.cached(app));   // the copy on the TV first (parsed here, not on the UI thread)
            MarketFile.loadDaily(app);   // the year's closes: only here, once a day
            MarketFile.loadFx(app);      // the currencies, once a day (and into memory for the screen)
            deliver(gen, MarketFile.load(app, false));
        }, "markets").start();
    }

    /** which load may still hand its result over: a pause, a stop or a newer load makes an older one drop it */
    private int loadGen;
    private boolean loading = true;

    private void deliver(int gen, MarketData d) {
        main.post(() -> {
            if (gen != loadGen || isFinishing() || isDestroyed()) return;
            loading = false;
            if (d != null) data = liveOn && d != data && data != null && !data.liveAt.isEmpty() ? d.withLive(liveOf(data)) : d;   // the file's news, with the live prices kept over it
            header();
            redraw();
        });
    }

    @Override
    protected void onPause() {
        loadGen++;   // a load still running delivers nothing to a page that is not in front
        resumed = false;   // live mode ends with the page being in front
        keepAwake();
        closeDialogs();
        main.removeCallbacks(langWatch);
        liveRun++;
        main.removeCallbacks(liveTick);
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
        s.append(" · ");
        if (open) {
            s.append(S.t("يغلق بعد %s", duration(MarketData.until(now, MarketData.closeAt(now)))));
        } else {
            long at = MarketData.nextOpen(now);
            s.append(S.t("يفتح %s %s", S.tv(DAYS[Math.min(DAYS.length - 1, MarketData.weekday(at) - 1)]), clock(10, 0)));   // «يفتح الأربعاء ١٠:٠٠ ص»: short enough for the line
        }
        s.append(" · ");
        if (data == null && loading) s.append(S.t("جارٍ التحميل…"));
        else if (data == null || data.updated == 0) s.append(S.t("لا أسعار بعد: يلزم اتصال بالإنترنت"));
        else {
            Calendar k = Calendar.getInstance(TimeZone.getTimeZone(zone()));
            k.setTimeInMillis(data.updated);
            s.append(S.t("آخر تحديث %s · أسعار متأخرة", clock(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE))));
        }
        state.setText(s);
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
        return Digits.of(h12 + ":" + mm) + "\u00A0" + (h < 12 ? S.t("ص") : S.t("م"));   // no line break inside a time («10:00 / AM»)
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

    /** The same tab drawn again (a button pressed in it): the focus that falls on the tabs while its buttons are rebuilt must not switch the tab («أخفِ المبالغ» jumped to «السوق»). */
    private boolean rebuilding;

    private void show(int k, boolean top) {
        if (k < 0) return;
        if (k == tab) {
            rebuilding = true;
            main.post(() -> main.post(() -> rebuilding = false));   // after the layout that hands the lost focus on
        }
        tab = k;
        onScreen.clear();
        onScreen.add(MarketData.TASI);
        paintTabs();
        content.removeAllViews();
        actions.removeAllViews();
        ctl.removeAllViews();
        if (k == 0) market();
        else if (k == 1) watch();
        else if (k == 3) currencies();
        else portfolio();
        if (data != null && k != 3) content.addView(sourceNote(), gap(16));
        if (top) scroll.scrollTo(0, 0);
        // down from the tabs to the first thing that takes the focus
        View first = firstFocusable(content);
        for (TextView t : tabViews) t.setNextFocusDownId(first != null ? first.getId() : View.NO_ID);
        // on the currency view «صرف العملات» keeps ▲/▼ inside it (▼ went to the tab «محفظتي» and left the view, step 28): down to the view's own buttons, up stays
        View below = actions.getChildCount() > 0 ? actions.getChildAt(0) : first;
        fxBtn.setNextFocusDownId(k == 3 && below != null ? below.getId() : View.NO_ID);
        fxBtn.setNextFocusUpId(k == 3 ? fxBtn.getId() : View.NO_ID);
    }

    private void paintTabs() {
        for (int i = 0; i < tabViews.size(); i++) {
            TextView t = tabViews.get(i);
            boolean on = i == tab;
            t.setTextColor(t.hasFocus() ? look.onFocus : on ? ACCENT : DIM);   // the chosen tab is the green word; no bar under it (the thin rule under the row stays)
            t.setBackground(t.hasFocus() ? GlassPaint.focused(this, look, 12) : new ColorDrawable(0));
        }
        if (fxBtn instanceof TextView) {   // «صرف العملات» is chosen while its page is shown
            TextView f = (TextView) fxBtn;
            f.setTextColor(f.hasFocus() ? look.onFocus : tab == 3 ? ACCENT : TEXT);
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
        if (data == null) { content.addView(note(loading ? S.t("جارٍ التحميل…") : S.t("لا أسعار بعد: يلزم اتصال بالإنترنت")), wrap()); return; }
        LinearLayout row1 = new LinearLayout(this);
        // TASI: value, change, and its day against yesterday's close
        MarketData.Quote tasi = data.quotes.get(MarketData.TASI);
        LinearLayout t = card();
        if (tasi != null) t.addView(chartBlock(S.t("تاسي"), tasi, 34, 96), wrap());
        else t.addView(text(S.t("تاسي"), 18, "Medium", TEXT), wrap());
        row1.addView(t, weight(1.6f, 0));

        // «اتجاه السوق»: the 30 companies we fetch (and it says so), and the session
        LinearLayout br = card();
        LinearLayout bh = new LinearLayout(this);
        bh.setGravity(Gravity.BOTTOM);
        TextView trendTitle = text(S.t("اتجاه السوق"), 18, "Medium", TEXT);
        trendTitle.setSingleLine(true);   // the title never wraps («Market direction» did in English); the small note beside it gives way
        bh.addView(trendTitle, new LinearLayout.LayoutParams(-2, -2));
        TextView trendNote = text(S.t("أكبر %s شركة", digits(MarketData.TADAWUL.length)), 13, "Regular", DIM);
        trendNote.setSingleLine(true);
        trendNote.setEllipsize(android.text.TextUtils.TruncateAt.END);
        trendNote.setGravity(Dir.end());
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(0, -2, 1f);
        noteLp.setMarginStart(dp(12));
        bh.addView(trendNote, noteLp);
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
        row1.addView(br, weight(1.15f, 16));
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
        hd.addView(text(S.t("السهم"), 12, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 1.3f));
        hd.addView(text(S.t("اليوم"), 12, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 1.2f));
        hd.addView(text(S.t("السعر"), 12, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 0.8f));
        hd.addView(text(S.t("التغيّر"), 12, "Regular", DIM), new LinearLayout.LayoutParams(0, -2, 0.7f));
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
        // two lines, so a narrow card never squeezes the name into a column of single letters (build 180): the name on the first, the line, the price and the change on the second
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.addView(text(name, 16, "Regular", TEXT), wrap());
        LinearLayout second = new LinearLayout(this);
        second.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(20), 1f);
        lp.setMarginEnd(dp(16));   // at least 16 px between the line and the price on either side (the line touched the price, step 27)
        lp.setMarginStart(dp(0));
        second.addView(new MarketChart(this).line(q.c, Double.NaN, color(q), DIM, false, 1.5f), lp);
        TextView px = text(money(q.p * mul, 2, unit), 16, "Medium", TEXT);
        px.setGravity(Dir.end());
        px.setSingleLine(true);
        LinearLayout.LayoutParams pxl = new LinearLayout.LayoutParams(-2, -2);
        pxl.setMarginStart(dp(16));
        second.addView(px, pxl);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-2, -2);
        pl.setMarginStart(dp(12));
        second.addView(pct(q.pct(), 14), pl);
        r.addView(second, gap(4));
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
        onScreen.add(code);
        MarketData.Quote q = data == null ? null : data.of(code);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(16), dp(12));
        r.setMinimumHeight(dp(60));   // 120 px a row (it was 152), the 24 px padding kept
        r.addView(nameCol(code, 17), new LinearLayout.LayoutParams(0, -2, 1.3f));
        FrameLayout sp = new FrameLayout(this);
        if (q != null) sp.addView(new MarketChart(this).line(q.c, q.v, color(q), DIM, false, 2f), new FrameLayout.LayoutParams(dp(112), dp(28), Gravity.CENTER_VERTICAL | Dir.start()));
        r.addView(sp, new LinearLayout.LayoutParams(0, dp(32), 1.2f));
        r.addView(ltr(text(q == null ? "—" : price(q.p), 18, "Medium", TEXT)), new LinearLayout.LayoutParams(0, -2, 0.8f));
        r.addView(q == null ? text("", 15, "Regular", DIM) : pct(q.pct(), 16), new LinearLayout.LayoutParams(0, -2, 0.7f));
        focusRow(r, 12);
        r.setOnClickListener(v -> stockCard(code, null));
        return r;
    }

    // ---------------------------------------------------------------- «صرف العملات»

    /** every currency the page knows: code and name (the first six are the pegs, the rest need CI's rate); USD and SAR are the converter's ends */
    private static final String[][] CCY = {
        {"USD", S.n("دولار أمريكي")}, {"SAR", S.n("ريال سعودي")}, {"KWD", S.n("دينار كويتي")}, {"AED", S.n("درهم إماراتي")}, {"QAR", S.n("ريال قطري")}, {"BHD", S.n("دينار بحريني")},
        {"OMR", S.n("ريال عماني")}, {"JOD", S.n("دينار أردني")}, {"EGP", S.n("جنيه مصري")}, {"EUR", S.n("يورو")}, {"GBP", S.n("جنيه إسترليني")}, {"TRY", S.n("ليرة تركية")},
        {"INR", S.n("روبية هندية")}, {"PKR", S.n("روبية باكستانية")},
        {"IQD", S.n("دينار عراقي")}, {"MAD", S.n("درهم مغربي")}, {"DZD", S.n("دينار جزائري")}, {"TND", S.n("دينار تونسي")}, {"LYD", S.n("دينار ليبي")}, {"SDG", S.n("جنيه سوداني")},
        {"YER", S.n("ريال يمني")}, {"LBP", S.n("ليرة لبنانية")}, {"SYP", S.n("ليرة سورية")}, {"MRU", S.n("أوقية موريتانية")},
        {"PHP", S.n("بيزو فلبيني")}, {"IDR", S.n("روبية إندونيسية")}, {"BDT", S.n("تاكا بنغلاديشية")}, {"LKR", S.n("روبية سريلانكية")}, {"NPR", S.n("روبية نيبالية")},
        {"MYR", S.n("رينغيت ماليزي")}, {"ETB", S.n("بر إثيوبي")}, {"KES", S.n("شلن كيني")},
        {"JPY", S.n("ين ياباني")}, {"CNY", S.n("يوان صيني")}, {"CHF", S.n("فرنك سويسري")}, {"CAD", S.n("دولار كندي")}, {"AUD", S.n("دولار أسترالي")}, {"SGD", S.n("دولار سنغافوري")},
        {"HKD", S.n("دولار هونغ كونغ")}, {"KRW", S.n("وون كوري")}, {"RUB", S.n("روبل روسي")}, {"ZAR", S.n("راند جنوب أفريقي")},
    };
    /** what shows at first, in this order (the dollar first); the rest are chosen in «اختر العملات» */
    static final String[] FX_DEFAULT = {"USD", "KWD", "AED", "QAR", "BHD", "OMR", "JOD", "EGP", "EUR", "GBP", "TRY", "INR", "PKR"};

    private android.content.SharedPreferences fxPrefs() { return getSharedPreferences("markets_fx", MODE_PRIVATE); }

    private String ccyName(String code) {
        for (String[] c : CCY) if (c[0].equals(code)) return S.tv(c[1]);
        return code;
    }

    /** the currencies chosen for the list, in the order of CCY, USD first; the defaults until the person chooses (saved on this TV only, like the stocks) */
    private List<String> fxChosen() {
        String raw = fxPrefs().getString("list", null);
        List<String> out = new ArrayList<>();
        for (String c : raw == null ? FX_DEFAULT : raw.split(",")) for (String[] k : CCY) if (k[0].equals(c.trim()) && !out.contains(k[0]) && !"SAR".equals(k[0])) out.add(k[0]);
        return out;
    }

    private java.util.Map<String, Double> fxRates() { return MarketData.parseFx(MarketFile.fxText()); }

    /** «1 دولار أمريكي = ٣٫٧٥ ريال» : the rate of one unit against the riyal, «—» when the file has none */
    private String fxVsSar(String code, java.util.Map<String, Double> rates) {
        return MarketData.show(MarketData.fxNumber(MarketData.convert(1, code, "SAR", rates)), Digits.latin());
    }

    private void currencies() {
        final java.util.Map<String, Double> rates = fxRates();
        LinearLayout list = card();
        LinearLayout hd = new LinearLayout(this);
        hd.setGravity(Gravity.BOTTOM);
        hd.addView(text(S.t("الأسعار مقابل الريال السعودي"), 18, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        long at = MarketData.fxUpdated(MarketFile.fxText());
        Calendar k = Calendar.getInstance(TimeZone.getTimeZone(zone()));
        k.setTimeInMillis(at);
        hd.addView(text(at == 0 ? S.t("لا أسعار بعد: يلزم اتصال بالإنترنت") : S.t("آخر تحديث %s", clock(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE))), 13, "Regular", DIM), new LinearLayout.LayoutParams(-2, -2));
        list.addView(hd, wrap());
        for (String code : fxChosen()) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout nm = new LinearLayout(this);
            nm.setOrientation(LinearLayout.VERTICAL);
            nm.addView(text(ccyName(code), 17, "Medium", TEXT), wrap());
            nm.addView(ltrStart(text(MarketData.pegged(code) ? code + " · " + S.t("مربوط بالدولار") : code, 13, "Regular", DIM)), wrap());
            r.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView v = text(fxVsSar(code, rates), 20, "Medium", TEXT);
            v.setSingleLine(true);
            v.setGravity(Dir.end());
            r.addView(ltr(v), new LinearLayout.LayoutParams(-2, -2));
            focusRow(r, 12);
            r.setOnClickListener(x -> converter(code, "SAR"));
            list.addView(r, gap(code.equals("USD") ? 8 : 4));
        }
        content.addView(list, wrap());
        content.addView(text(S.t("أسعار يومية · المصدر: Yahoo Finance. الريال والدرهم والريال القطري والدينار البحريني والريال العماني والدينار الأردني مربوطة بالدولار."), 12, "Regular", DIM), gap(16));
        actions.addView(button(S.t("حوّل"), true, v -> converter(fxPrefs().getString("from", "USD"), fxPrefs().getString("to", "SAR"))), buttonParams(0));
        actions.addView(button(S.t("اختر العملات"), false, v -> chooseCurrencies()), buttonParams(12));
    }

    /** «اختر العملات»: each currency a row that turns on and off (the on mark), saved on this TV */
    /** the picker drawn again after a pick keeps the pick's row focused and the list where it was (it jumped to the top, step 28) */
    private String pickedCode;
    private int pickedScroll;

    private void chooseCurrencies() {
        final String keepCode = pickedCode;
        final int keepScroll = pickedScroll;
        pickedCode = null;
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 640);
        box.addView(text(S.t("اختر العملات"), 24, "Medium", TEXT), wrap());
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        sv.addView(rows);
        final List<String> chosen = new ArrayList<>(fxChosen());
        final boolean[] changed = {false}, again = {false};
        View first = null, kept = null;
        for (String[] c : CCY) {
            if (c[0].equals("SAR")) continue;
            final String code = c[0];
            View[] holder = new View[1];
            // the state word first and green while on, with the three green triangles at the start (toggle style I, as everywhere in the settings)
            holder[0] = settingRow(ccyName(code), (chosen.contains(code) ? S.t("مفعّلة") : S.t("متوقفة")) + " · " + code, chosen.contains(code), v -> {
                if (chosen.contains(code)) chosen.remove(code); else chosen.add(code);
                List<String> ordered = new ArrayList<>();
                for (String[] k : CCY) if (chosen.contains(k[0])) ordered.add(k[0]);
                fxPrefs().edit().putString("list", String.join(",", ordered)).apply();
                changed[0] = true;
                again[0] = true;
                pickedCode = code;
                pickedScroll = sv.getScrollY();
                d.dismiss();
                chooseCurrencies();   // drawn again with the mark moved
            });
            if (first == null) first = holder[0];
            if (code.equals(keepCode)) kept = holder[0];
            rows.addView(holder[0], gap(rows.getChildCount() == 0 ? 12 : 4));
        }
        box.addView(sv, sized(-1, 300, 12));   // the list leaves room for the button under it on a 1080p screen (at 380 the button was squeezed to 64 px and its label vanished)
        box.addView(button(S.t("تم"), true, v -> d.dismiss()), gap(16));
        d.setOnDismissListener(x -> {
            if (changed[0]) redraw();
            if (again[0]) return;   // a row was pressed: the list opens again, the page behind keeps what it has
            main.post(() -> { View b = actions.getChildCount() > 1 ? actions.getChildAt(1) : null; if (b != null && !isFinishing()) b.requestFocus(); });   // back on «اختر العملات»
        });
        d.show();
        if (kept != null) {
            final View k = kept;
            k.requestFocus();
            sv.post(() -> sv.scrollTo(0, keepScroll));
        } else if (first != null) first.requestFocus();
    }

    /** one currency picked from the whole list, for the converter's two ends */
    private void pickCurrency(java.util.function.Consumer<String> done) {
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 640);
        box.addView(text(S.t("اختر العملة"), 24, "Medium", TEXT), wrap());
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        sv.addView(rows);
        View first = null;
        for (String[] c : CCY) {
            final String code = c[0];
            View row = settingRow(ccyName(code), code, false, v -> { d.dismiss(); done.accept(code); });
            if (first == null) first = row;
            rows.addView(row, gap(rows.getChildCount() == 0 ? 12 : 4));
        }
        box.addView(sv, sized(-1, 380, 12));
        d.show();
        if (first != null) first.requestFocus();
    }

    /** the converter: an amount typed with the number pad (or the remote's digits), two currencies, the result through the dollar; the two ends are kept on this TV */
    private void converter(String from0, String to0) {
        final java.util.Map<String, Double> rates = fxRates();
        final String[] ends = {from0, to0};
        Dialog d = dialog();
        LinearLayout box = dialogBox(d, 560);
        box.addView(text(S.t("حوّل العملات"), 24, "Medium", TEXT), wrap());
        final StringBuilder amount = new StringBuilder("1");
        final boolean[] untouched = {true};   // the first key replaces the 1 that shows at first
        final TextView shown = ltrStart(text("", 30, "Medium", TEXT)), result = ltrStart(text("", 22, "Medium", ACCENT)), fromB = (TextView) button("", false, null), toB = (TextView) button("", false, null);
        Runnable paint = () -> {
            fromB.setText(ccyName(ends[0]) + " · " + ends[0]);
            toB.setText(ccyName(ends[1]) + " · " + ends[1]);
            shown.setText(MarketData.show(MarketData.typedAmount(amount.toString()), Digits.latin()) + " " + ends[0]);
            double a = amount.length() == 0 ? 0 : Double.parseDouble(amount.toString().endsWith(".") ? amount + "0" : amount.toString());
            double r = MarketData.convert(a, ends[0], ends[1], rates);
            result.setText("= " + MarketData.show(MarketData.fxNumber(r), Digits.latin()) + " " + ends[1]);   // «—» when a rate is missing, never 0
            fxPrefs().edit().putString("from", ends[0]).putString("to", ends[1]).apply();
        };
        fromB.setOnClickListener(v -> pickCurrency(c -> { ends[0] = c; paint.run(); }));
        toB.setOnClickListener(v -> pickCurrency(c -> { ends[1] = c; paint.run(); }));
        LinearLayout pair = new LinearLayout(this);
        pair.setLayoutDirection(Dir.layout());
        pair.addView(fromB, weight(1f, 0));
        View swap = button("⇄", false, v -> { String t = ends[0]; ends[0] = ends[1]; ends[1] = t; paint.run(); });
        pair.addView(swap, buttonParams(8));
        pair.addView(toB, weight(1f, 8));
        box.addView(pair, gap(12));
        box.addView(shown, gap(12));
        box.addView(result, gap(4));
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫"};
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        View five = null;
        for (int r = 0; r < 4; r++) {
            LinearLayout line = new LinearLayout(this);
            line.setLayoutDirection(Dir.layout());
            for (int c = 0; c < 3; c++) {
                final String key = keys[r * 3 + c];
                View b = button(padLabel(key), false, v -> {
                    if (untouched[0] && !key.equals("⌫")) { amount.setLength(0); untouched[0] = false; }
                    if (key.equals("⌫")) { untouched[0] = false; if (amount.length() > 0) amount.setLength(amount.length() - 1); }
                    else if (key.equals(".")) { if (amount.indexOf(".") < 0 && amount.length() < 12) amount.append(amount.length() == 0 ? "0." : "."); }
                    else if (amount.length() < 12) { if (amount.toString().equals("0")) amount.setLength(0); amount.append(key); }
                    paint.run();
                });
                ((TextView) b).setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                padStyle((TextView) b, key, 22);
                if (key.equals("5")) five = b;
                line.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1f));
                if (c > 0) ((LinearLayout.LayoutParams) b.getLayoutParams()).setMarginStart(dp(8));
            }
            pad.addView(line, gap(r == 0 ? 0 : 8));
        }
        box.addView(pad, gap(12));
        d.setOnKeyListener((di, keyCode, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) { if (untouched[0]) { amount.setLength(0); untouched[0] = false; } if (amount.length() < 12) amount.append((char) ('0' + keyCode - KeyEvent.KEYCODE_0)); paint.run(); return true; }
            if (keyCode == KeyEvent.KEYCODE_DEL) { if (amount.length() > 0) amount.setLength(amount.length() - 1); paint.run(); return true; }
            return false;
        });
        paint.run();
        d.show();
        if (five != null) five.requestFocus();
    }

    // ---------------------------------------------------------------- «محفظتي»

    private void portfolio() {
        Portfolio.openEditing(Portfolio.now());
        boolean hide = Portfolio.hidden(this);
        // «أخفِ المبالغ» is in the controls row, beside «صرف العملات» (only on this tab); «أضف سهمًا» and «خيارات محفظتي» stay at the end of the tab row
        final View hideBtn = ctlButton(hide ? S.t("أظهر المبالغ") : S.t("أخفِ المبالغ"), v -> {
            Portfolio.setHidden(this, !Portfolio.hidden(this));
            show(2, false);
            ctl.getChildAt(0).requestFocus();   // «أخفِ/أظهر المبالغ» stays under the focus, on «محفظتي»
        });
        ctl.addView(hideBtn, new LinearLayout.LayoutParams(-2, -2));
        actions.addView(button(S.t("أضف سهمًا"), false, v -> pick()), buttonParams(0));
        actions.addView(button(S.t("خيارات محفظتي"), false, v -> options()), buttonParams(12));
        // the tabs reach the buttons with the arrow that goes on in reading order
        TextView last = tabViews.get(2);
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
        row.setTag(code);   // after «احفظ» the focus goes to this row (focusHolding)
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
        cardCode = code;   // live mode asks for it too while the card is open
        d.setOnDismissListener(x -> { cardCode = null; cardBlock = null; cardNote = null; });
        LinearLayout box = dialogBox(d, 760);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.BOTTOM);
        head.addView(text(title != null ? title : S.tv(MarketData.name(code)), 26, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        boolean sa = code.startsWith("^") || MarketData.isSaudi(code);
        String sub = code.startsWith("^") ? S.t("مؤشر السوق الرئيسية · تداول") : sa ? S.t("%s · تداول · ر.س", code) : S.t("%s · بالدولار", code);
        String tag = liveTag(code);
        head.addView(ltrStart(text(tag.isEmpty() ? sub : sub + " · " + tag, 14, "Regular", DIM)), new LinearLayout.LayoutParams(-2, -2));
        box.addView(head, wrap());
        if (q != null) {
            cardBlock = (LinearLayout) chartBlock(null, q, 40, 130);
            box.addView(cardBlock, gap(8));
            LinearLayout stats = new LinearLayout(this);
            stats.addView(stat(S.t("الافتتاح"), price(q.open())), weight(1f, 0));
            stats.addView(stat(S.t("نطاق اليوم"), price(q.low()) + " – " + price(q.high())), weight(1f, 12));
            if (!Double.isNaN(q.yl) && !Double.isNaN(q.yh)) stats.addView(stat(S.t("نطاق ٥٢ أسبوعًا"), price(q.yl) + " – " + price(q.yh)), weight(1f, 12));
            if (!Double.isNaN(q.vol) && q.vol > 0) stats.addView(stat(S.t("الكمية"), S.t("%s مليون", MarketData.show(MarketData.num(q.vol / 1e6, 1), Digits.latin()))), weight(1f, 12));
            box.addView(stats, gap(16));
        } else {
            box.addView(note(S.t("لا سعر لهذا الرمز في ملف الأسعار بعد")), gap(12));
        }
        cardNote = (TextView) sourceNote();
        box.addView(cardNote, gap(12));
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
                redraw();
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

    /** «الأسعار متأخرة · المصدر: Yahoo Finance»: small, in the footer's style, under the prices (the file says whose they are) */
    private View sourceNote() {
        TextView t = text(sourceText(), 12, "Regular", DIM);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        return t;
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
                View b = button(padLabel(k), false, v -> {
                    StringBuilder s = values[field[0]];
                    if (k.equals("⌫")) { if (s.length() > 0) s.setLength(s.length() - 1); }
                    else if (k.equals(".")) { if (s.indexOf(".") < 0 && s.length() < 9) s.append(s.length() == 0 ? "0." : "."); }
                    else if (s.length() < 10) s.append(k);
                    paint.run();
                });
                ((TextView) b).setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                padStyle((TextView) b, k, 24);
                if (k.equals("5")) five = b;
                line.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1f));
                if (c > 0) ((LinearLayout.LayoutParams) b.getLayoutParams()).setMarginStart(dp(8));
            }
            pad.addView(line, gap(r == 0 ? 0 : 8));
        }
        box.addView(pad, gap(16));
        final TextView missing = text("", 16, "Medium", look.warn);   // what «احفظ» still needs, in the dialog (a toast is not seen on every TV)
        missing.setVisibility(View.GONE);
        box.addView(missing, gap(12));
        LinearLayout bottom = new LinearLayout(this);
        View save = button(S.t("احفظ"), true, v -> {
            Holdings.Buy b = Holdings.buy(values[0].toString(), values[1].toString());
            if (b == null) { missing.setText(Holdings.missing(values[0].toString(), values[1].toString()) == 0 ? S.t("اكتب الكمية") : Holdings.missing(values[0].toString(), values[1].toString()) == 1 ? S.t("اكتب متوسط التكلفة") : S.t("اكتب الكمية ومتوسط التكلفة")); missing.setVisibility(View.VISIBLE); return; }
            List<Holdings.Holding> next = Holdings.put(Portfolio.load(this), code, java.util.Collections.singletonList(b));
            if (next == null) { toast(S.t("المحفظة ممتلئة: %s سهمًا", digits(Holdings.MAX))); return; }
            if (!Portfolio.save(this, next)) { toast(S.t("ما حُفظ")); return; }
            d.dismiss();
            redraw();
            scroll.post(() -> focusHolding(code));   // after redraw's own scroll: the new holding's row, else «أضف سهمًا» (it landed on «أخفِ المبالغ», step 27)
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

    /** the focus after a holding was saved: its row on the page, else «أضف سهمًا» */
    private void focusHolding(String code) {
        if (isFinishing() || isDestroyed() || tab != 2) return;
        View row = null;
        for (View v : focusables(content)) if (code.equals(v.getTag())) { row = v; break; }
        View to = row != null ? row : actions.getChildCount() > 0 ? actions.getChildAt(0) : null;
        if (to == null) return;
        to.requestFocus();
        if (to == row) {   // the new row is scrolled into view, a margin above and below (it stayed 8 px in at the page's end, step 28)
            final View target = row;
            scroll.post(() -> {
                android.graphics.Rect r = new android.graphics.Rect(0, 0, target.getWidth(), target.getHeight());
                content.offsetDescendantRectToMyCoords(target, r);
                int pad = dp(24), top = scroll.getScrollY(), h = scroll.getHeight();
                if (r.bottom + pad > top + h) scroll.scrollTo(0, r.bottom + pad - h);
                else if (r.top - pad < top) scroll.scrollTo(0, Math.max(0, r.top - pad));
            });
        }
    }

    private static List<View> focusables(View root) {
        List<View> l = new ArrayList<>();
        collectFocusable(root, l);
        return l;
    }

    /** «أسهل من الجوال»: the phone page's QR (pairing), opened on «محفظتي»; the phone may edit for a while */
    private void fromPhone() {
        Portfolio.openEditing(Portfolio.now());
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
            redraw();
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
        onScreen.add(code);
        MarketData.Quote q = data.of(code);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(16), dp(12));
        r.addView(text(S.tv(MarketData.name(code)), 17, "Medium", TEXT), new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(ltr(text(price(q.p), 16, "Regular", DIM)), new LinearLayout.LayoutParams(dp(96), -2));
        r.addView(pct(q.pct(), 16), new LinearLayout.LayoutParams(dp(96), -2));
        focusRow(r, 12);
        r.setOnClickListener(v -> stockCard(code, null));
        return r;
    }

    private View volumeRow(String code) {
        onScreen.add(code);
        MarketData.Quote q = data.of(code);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(16), dp(12));
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
        String tag = liveTag(code);
        nm.addView(ltrStart(text(tag.isEmpty() ? code : code + " · " + tag, 12, "Regular", DIM)), wrap());
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
            l.addView(ltr(text(MarketData.changeLine(r.change(q.p), r.pct(q.p), Digits.latin()), sp * 0.5f, "Medium", r.pct(q.p) >= 0 ? UP : DOWN)), p);
        }
        return l;
    }

    private TextView pct(double v, float sp) {
        TextView t = text(Double.isNaN(v) ? "—" : MarketData.isolate(MarketData.show(MarketData.signed(v, 2) + "%", Digits.latin())), sp, "Medium", Double.isNaN(v) ? DIM : v >= 0 ? UP : DOWN);
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
        c.setPadding(dp(12), dp(16), dp(12), dp(16));   // 24 px at the sides (it was 40)
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

    /** a small button of the controls row, the same height and type size as «مباشر | عادي» (16 sp, 16 / 8 dp inside, no box, the focus fill only; step 28: a boxed 15 sp one was clipped) */
    private TextView ctlButton(String label, View.OnClickListener click) {
        final TextView t = text(label, 16, "Medium", TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(8), dp(16), dp(8));
        t.setId(View.generateViewId());
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(new ColorDrawable(0));
        t.setOnClickListener(click);
        t.setOnFocusChangeListener((v, has) -> {
            t.setBackground(has ? GlassPaint.focused(this, look, 12) : new ColorDrawable(0));
            t.setTextColor(has ? look.onFocus : t == fxBtn && tab == 3 ? ACCENT : TEXT);
        });
        return t;
    }

    /**
     * A number pad key's label. The Arabic zero «٠» is a dot and read as a decimal point next to the real one (step 28, the owner's photo): the zero key
     * shows a Latin-style 0, the decimal key the separator «٫» (English «.») as a large mark, and the backspace key a ⌫ that points the way text deletes.
     */
    private static String padLabel(String key) {
        if (key.equals("⌫")) return "⌫";
        if (key.equals("0") && !Digits.latin()) return "0";
        return MarketData.show(key, Digits.latin());
    }

    private void padStyle(TextView b, String key, int sp) {
        if (key.equals(".")) {   // the separator, large enough to read as a key (no vertical padding: the key's height is fixed)
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp + 10);
            b.setPadding(dp(20), 0, dp(20), 0);
        }
        if (key.equals("⌫") && Dir.rtl()) b.setScaleX(-1f);   // ⌫ points left (English); in Arabic the text deletes from the left, so the key points right
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

    /** the dialogs this page opened: ended with the page's pause so none leaks its window when HOME is pressed (WindowLeaked from the converter, step 27) */
    private final List<Dialog> dialogs = new ArrayList<>();

    private Dialog dialog() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        for (int i = dialogs.size() - 1; i >= 0; i--) if (!dialogs.get(i).isShowing()) dialogs.remove(i);
        dialogs.add(d);
        return d;
    }

    private void closeDialogs() {
        for (Dialog d : new ArrayList<>(dialogs)) { try { if (d.isShowing()) d.dismiss(); } catch (RuntimeException ignored) { } }
        dialogs.clear();
    }

    /** the language this page was built in: a change while it is open (the setting, or `lang`) recreates it, as the other pages do not need to (it is cheap here) */
    private String builtIn;
    private final Runnable langWatch = new Runnable() {
        @Override public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (builtIn != null && !builtIn.equals(S.lang())) { recreate(); return; }
            main.postDelayed(this, 2000);
        }
    };

    private LinearLayout dialogBox(Dialog d, int widthDp) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Dir.layout());
        box.setPadding(dp(32), dp(28), dp(32), dp(28));
        // a SOLID surface (the look's dialog fill laid on its base colour: nothing of the page shows through it) and the page behind dimmed, like a dialog should be (step 27)
        box.setBackground(GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line));
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.6f);
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
