package sa.urtv.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.Collections;

/**
 * The optional cards on the home screen: the screensaver's page in card-only mode (#widget=1), showing
 * up to three cards picked in the screensaver's settings («البطاقة الأولى» … «الثالثة»), all in one WebView
 * whose box grows with their number (see Sizer). Off by default; while off no WebView is created. Settings
 * come from the screensaver app's ConfigProvider (same signing key).
 *
 * «المهام» among the cards is a button: a transparent focus target (live()) sits over that card inside the same
 * FrameLayout, and the page (#live=1) draws its mic mark and, while the target has the focus, its ring (setLiveFocus).
 * The WebView itself stays unfocusable; the focus and OK are the Android side's, like a pill's. The target is moved over
 * the card by what the page says about where it is (liveRect), so the page stays the one place that lays the cards out.
 *
 * «الأسهم» among the cards is a button the same way (docs/PROPOSALS-NOV2.md §6): its own target (market()), the page's ring and no mark;
 * OK opens «الأسواق» (Live.markets()).
 */
final class HomeCard {
    private static final Uri CONFIG = Uri.parse("content://sa.urtv.screensaver.config");
    private static final String PREFS = "homecard";   // the last settings read from the screensaver
    private static final String FONTS = "https://raw.githubusercontent.com/tshi-dev/UrTV/master/assets/fonts/";

    /** Sent inside the app when the cards were changed from the phone: the home screen in front follows at once. */
    static final String CHANGED = "sa.urtv.home.CARDS_CHANGED";

    /** The box in pixels, {width, height}, for that many cards; asked again whenever the screen is laid out. */
    interface Sizer { int[] box(int cards); }

    /** What the home screen does with the «المهام» card (and the stocks card): it gained or lost the focus, OK was pressed on it. */
    interface Live { void focus(View target, boolean has); void open(); default void markets() { } }

    private final Activity activity;
    private final ViewGroup parent;
    private final FrameLayout.LayoutParams params;
    private final Sizer sizer;
    private WebView web;
    private String url;
    private int cards;   // how many the page in `web` shows
    private boolean tasks;   // «المهام» is one of them: `target` exists
    private View target;
    private FrameLayout.LayoutParams targetParams;
    private boolean stocks;   // «الأسهم» is one of them: `market` exists
    private View market;
    private FrameLayout.LayoutParams marketParams;
    private boolean stocksFirst;   // the stocks card comes before «المهام» among the cards
    private final Live live;
    private final RenderGuard guard = new RenderGuard();
    private boolean resumed;

    HomeCard(Activity activity, ViewGroup parent, FrameLayout.LayoutParams params, Sizer sizer, Live live) {
        this.activity = activity;
        this.parent = parent;
        this.params = params;
        this.sizer = sizer;
        this.live = live;
    }

    /** The focus target over the «المهام» card, or null when it is not among the cards. */
    View live() { return target; }

    /** The focus target over the stocks card, or null when it is not among the cards. */
    View market() { return market; }

    /** The cards' focus targets in the cards' order (none, one or two). */
    java.util.List<View> targets() {
        java.util.List<View> l = new java.util.ArrayList<>();
        if (target != null) l.add(target);
        if (market != null) l.add(stocksFirst ? 0 : l.size(), market);
        return l;
    }

    /** The screen was laid out again (the dock's names, the clock's width): the box may have to move. */
    void fit() {
        if (web == null) return;
        int[] b = sizer.box(cards);
        if (b[0] == params.width && b[1] == params.height) return;
        params.width = b[0];
        params.height = b[1];
        web.setLayoutParams(params);
        placeTarget();
        placeMarket();
    }

    /** onResume: follow the current settings; a changed choice gets a fresh page. */
    void refresh() {
        resumed = true;
        String want = readUrl();
        if (want == null) { destroy(); return; }
        if (!want.equals(url)) {
            destroy();
            if (!guard.mayCreate(System.currentTimeMillis())) return;   // its renderer keeps dying: the cards stay off for now
            create(want);
        }
        web.onResume();
        web.resumeTimers();
    }

    /** onPause: no timers or network while an app is in front. pauseTimers()/resumeTimers() are process-wide: safe here only because this card is the
     *  launcher's only WebView (tests/home_card_test.py checks it), so whoever adds a second WebView has to revisit both. */
    void pause() {
        resumed = false;
        if (web != null) { web.onPause(); web.pauseTimers(); }
    }

    void destroy() {
        if (web == null) return;
        if (target != null) { parent.removeView(target); target = null; targetParams = null; }
        if (market != null) { parent.removeView(market); market = null; marketParams = null; }
        parent.removeView(web);
        web.destroy();
        web = null;
        url = null;
    }

    private String readUrl() {
        String frag = null, panel = null;
        try (Cursor c = activity.getContentResolver().query(CONFIG, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                frag = c.getString(0);
                panel = c.getString(1);
                int all = c.getColumnIndex("home_panels");   // "a,b,c": every card; an older screensaver has only the first
                if (all >= 0 && c.getString(all) != null) panel = c.getString(all);
            }
        } catch (RuntimeException e) {
            // the screensaver's settings are out of reach
        }
        SharedPreferences sp = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (frag != null && panel != null) {   // remember what was read, for when it can't be read
            sp.edit().putString("frag", frag).putString("panel", panel).apply();
        }
        // Unreadable settings (the screensaver is missing, too old, or signed with another key, e.g. reinstalled
        // before this app): keep the last cards chosen, «بدون» included. Only with nothing remembered, the prayers.
        String[] choice = HomeCardChoice.pick(frag, panel, sp.getString("frag", null), sp.getString("panel", null));
        cards = HomeCardChoice.count(choice);
        tasks = HomeCardChoice.hasTasks(choice);
        stocks = HomeCardChoice.hasStocks(choice);
        stocksFirst = HomeCardChoice.stocksFirst(choice);
        return HomePage.fresh(activity, HomeCardChoice.liveUrl(choice));   // the screensaver's newer page when it has one
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    private void create(String u) {
        web = new WebView(activity);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setFocusable(false);
        web.setFocusableInTouchMode(false);
        web.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setWebViewClient(new WebViewClient() {
            /** The renderer died or was killed for memory: the dead view is thrown away (never reloaded) and, if the home screen is in front, made again at most twice in ten minutes. */
            @Override
            public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail detail) {
                if (v != web) { v.destroy(); return true; }
                guard.gone(System.currentTimeMillis());
                destroy();
                if (resumed) refresh();
                return true;
            }

            // the page asks GitHub for its fonts; answer from the APK
            @Override
            public void onPageFinished(WebView v, String u) {
                if (target == null && market == null) return;
                // the page has laid its cards out; they may still be filling (fonts, the first rows): ask again a little later
                v.postDelayed(HomeCard.this::placeTarget, 300);
                v.postDelayed(HomeCard.this::placeTarget, 2500);
                v.postDelayed(HomeCard.this::placeMarket, 300);
                v.postDelayed(HomeCard.this::placeMarket, 2500);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String s = r.getUrl().toString();
                if (s.startsWith(FONTS)) {
                    String name = s.substring(FONTS.length());
                    if (name.matches("[A-Za-z0-9_-]+\\.woff2")) {
                        try {
                            return new WebResourceResponse("font/woff2", null, 200, "OK",
                                Collections.singletonMap("Access-Control-Allow-Origin", "*"),
                                activity.getAssets().open("fonts/" + name));
                        } catch (java.io.IOException ignored) { }
                    }
                }
                return super.shouldInterceptRequest(v, r);
            }
        });
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        // same as the screensaver: the page calls its data APIs directly from file://
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // The page sizes its text to the screen; the TV's font size setting (Android scales WebView text by it) made
        // it larger than its boxes, so the left column spilled off screen and moved as the clock and the matches changed.
        s.setTextZoom(100);
        int[] b = sizer.box(cards);
        params.width = b[0];
        params.height = b[1];
        parent.addView(web, params);
        if (tasks) addTarget();
        if (stocks) addMarket();
        url = u;
        web.loadUrl(u);
    }

    /** The transparent button over the «المهام» card: it takes the focus like a pill, and OK opens the list. */
    private void addTarget() {
        target = new View(activity);
        target.setId(View.generateViewId());
        target.setFocusable(true);
        target.setContentDescription(S.t("المهام"));
        target.setOnFocusChangeListener((v, has) -> {
            if (web != null) web.evaluateJavascript("setLiveFocus(" + has + ")", null);
            if (has) placeTarget();   // exactly over the card while it is the one in front
            live.focus(v, has);
        });
        target.setOnClickListener(v -> live.open());
        targetParams = new FrameLayout.LayoutParams(params);
        parent.addView(target, targetParams);
    }

    /** The transparent button over the stocks card: it takes the focus like a pill, and OK opens «الأسواق». */
    private void addMarket() {
        market = new View(activity);
        market.setId(View.generateViewId());
        market.setFocusable(true);
        market.setContentDescription(S.t("الأسواق"));
        market.setOnFocusChangeListener((v, has) -> {
            if (web != null) web.evaluateJavascript("setLiveFocus(" + has + ",'stocks')", null);
            if (has) placeMarket();
            live.focus(v, has);
        });
        market.setOnClickListener(v -> live.markets());
        marketParams = new FrameLayout.LayoutParams(params);
        parent.addView(market, marketParams);
    }

    private void placeMarket() {
        if (web == null || market == null) return;
        place(market, marketParams, "liveRect('stocks')");
    }

    /** Puts the target over the card, where the page says it is; until it has said, it covers the whole box. */
    private void placeTarget() {
        if (web == null || target == null) return;
        place(target, targetParams, "liveRect()");
    }

    private void place(final View target, final FrameLayout.LayoutParams targetParams, String ask) {
        web.evaluateJavascript("typeof liveRect==='function'?" + ask + ":''", new ValueCallback<String>() {
            @Override public void onReceiveValue(String value) {
                if (target.getParent() == null) return;
                String[] r = value == null ? new String[0] : value.replace("\"", "").split(",");
                if (r.length != 4) return;
                try {
                    float d = activity.getResources().getDisplayMetrics().density;   // the page's px are dp
                    if (Dir.rtl()) targetParams.leftMargin = params.leftMargin + Math.round(Float.parseFloat(r[0]) * d);
                    else targetParams.rightMargin = params.rightMargin + params.width   // English: the card hangs from the right (Dir.end())
                            - Math.round(Float.parseFloat(r[0]) * d) - Math.max(1, Math.round(Float.parseFloat(r[2]) * d));
                    targetParams.topMargin = params.topMargin + Math.round(Float.parseFloat(r[1]) * d);
                    targetParams.width = Math.max(1, Math.round(Float.parseFloat(r[2]) * d));
                    targetParams.height = Math.max(1, Math.round(Float.parseFloat(r[3]) * d));
                    target.setLayoutParams(targetParams);
                } catch (NumberFormatException ignored) { }
            }
        });
    }
}
