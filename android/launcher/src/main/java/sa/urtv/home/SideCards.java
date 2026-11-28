package sa.urtv.home;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * 4.4 beta 4 (the owner's idea, docs/mockups/couch-side-cards/): small cards down the left of «كنبة», beside the always-centred big clock, above the
 * cards strip and the apps row. Two options, A (two cards: the next prayer with the iqama's countdown, the hours' weather) and B (three: those and the
 * followed club's next match). Off by default: the screensaver's setting «sidecards» (Prefs.SIDE_CARDS_DEFAULT, the owner's pick a one-line change)
 * travels in the page's fragment as #sidecards=a|b; with none, nothing of this exists (no view, no page, no read).
 *
 * The cards are the page's own (the same cards as the home's, in its one-column widget form, #side=1 for their small type); they never take the focus,
 * so the D-pad's ways on the home are unchanged. The settings are read off the UI thread (the caller's loader) and the view is placed on it.
 */
final class SideCards {
    private static final Uri CONFIG = Uri.parse("content://sa.urtv.screensaver.config");
    /** the column in 1080p px (FavBar.px scales it): x 122 to 462 from the screen's left, y 120 to 554 (20 above the strip's top) */
    static final int X = 122, W = 340, TOP = 120, BOTTOM = 554, GAP = 20;

    /** where the cards strip starts (window px), -1 when there is none (HomeCard.stripTop) */
    interface Strip { int top(); }

    private final Activity activity;
    private final ViewGroup parent;
    private final Strip strip;
    /** the column never runs into the strip (run 22: with the plain clock the strip is higher, and the column's second card lay over it): checked at
     *  every layout of the screen, the column hidden while its whole height does not fit above the strip, never cut */
    private final android.view.ViewTreeObserver.OnGlobalLayoutListener fitter = this::fit;
    private WebView web;
    private final RenderGuard guard = new RenderGuard();   // a renderer that keeps dying: the column stays away for a while (as HomeCard's card)
    private String shown;   // the page's address now on the screen, null for none

    SideCards(Activity activity, ViewGroup parent, Strip strip) {
        this.activity = activity;
        this.parent = parent;
        this.strip = strip;
    }

    /** the option in a page fragment: "a", "b", or null for off (anything else is off) */
    static String option(String frag) {
        if (frag == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:^|&)sidecards=([ab])(?:&|$)").matcher(frag);
        return m.find() ? m.group(1) : null;
    }

    /** the cards of an option, in order from the top */
    static String cards(String option) {
        return "a".equals(option) ? "nextprayer,hourly" : "b".equals(option) ? "nextprayer,hourly,nextmatch" : null;
    }

    /** The page's address for the side column, or null when it is off or not for this layout (only «كنبة» has the room). Plain Java: tests run it. */
    static String url(String frag, String layout) {
        String opt = option(frag);
        if (opt == null || !"couch".equals(layout)) return null;
        String u = HomeCardChoice.url(new String[] {frag.replaceAll("(^|&)sidecards=[^&]*", ""), cards(opt)});
        return u == null ? null : u + "&side=1";
    }

    /** Off the UI thread: what the side column should show now (null: nothing). */
    String read() {
        String frag = null, layout = null;
        try (Cursor c = activity.getContentResolver().query(CONFIG, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                frag = c.getString(0);
                int l = c.getColumnIndex("home_layout");
                layout = l >= 0 ? c.getString(l) : null;
            }
        } catch (RuntimeException e) {
            return null;   // the screensaver's settings are out of reach: no side cards
        }
        String u = url(frag, layout);
        return u == null ? null : HomePage.fresh(activity, u);
    }

    /** On the UI thread: show what read() gave (null takes the column away whole). */
    void show(String url) {
        if (url == null) { drop(); return; }
        if (url.equals(shown) && web != null) { run(); return; }
        if (web == null) {
            if (!guard.mayCreate(android.os.SystemClock.elapsedRealtime())) return;
            web = WebViews.make(activity);
            if (web == null) return;
            setUp();
            web.setBackgroundColor(Color.TRANSPARENT);
            web.setFocusable(false);
            web.setFocusableInTouchMode(false);
            web.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            web.setVerticalScrollBarEnabled(false);
            web.setHorizontalScrollBarEnabled(false);
            web.setOverScrollMode(View.OVER_SCROLL_NEVER);
            web.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            int h = activity.getResources().getDisplayMetrics().heightPixels;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FavBar.px(W, h), FavBar.px(BOTTOM - TOP, h));
            lp.leftMargin = FavBar.px(X, h);   // the screen's left in both directions (the clock is centred, the greeting is on the right)
            lp.topMargin = FavBar.px(TOP, h);
            parent.addView(web, lp);
            parent.getViewTreeObserver().addOnGlobalLayoutListener(fitter);
        }
        shown = url;
        web.loadUrl(url);
        run();
    }

    /** the home is in front (show is called from its onResume only): the page runs. pauseTimers()/resumeTimers() are process-wide, and HomeCard's
     *  card may be «بدون» (no page of its own to resume them), so the column resumes them itself (tests/home_card_test.py) */
    private void run() {
        web.onResume();
        web.resumeTimers();
    }

    /** the home's onPause, with HomeCard.pause(): no timers or network while an app is in front */
    void pause() {
        if (web != null) { web.onPause(); web.pauseTimers(); }
    }

    /** the page as HomeCard's: its script, its own file and the assets (HomeCard.served), the fonts from the APK, the TV's font size not applied;
     *  a dead renderer takes the column away, never reloaded (tests/render_gone_test.py); the next resume makes it again if the guard allows */
    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    private void setUp() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient() {
            @TargetApi(26)
            @Override
            public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail detail) {
                if (v == web) { guard.gone(android.os.SystemClock.elapsedRealtime()); parent.removeView(v); web = null; shown = null; }
                v.destroy();
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                WebResourceResponse fixed = HomeCard.served(activity, r, shown);
                if (fixed != null) return fixed;
                NetLog.count(r.getUrl().toString());   // G3: the request log, as the card's page
                return super.shouldInterceptRequest(v, r);
            }
        });
    }

    /** shown only while the column's whole height ends GAP above the strip (or there is no strip) */
    private void fit() {
        if (web == null) return;
        int h = activity.getResources().getDisplayMetrics().heightPixels, top = strip.top();
        int vis = top < 0 || FavBar.px(BOTTOM + GAP, h) <= top ? View.VISIBLE : View.INVISIBLE;
        if (web.getVisibility() != vis) web.setVisibility(vis);
    }

    /** the column goes, page and all */
    void drop() {
        shown = null;
        if (web == null) return;
        parent.getViewTreeObserver().removeOnGlobalLayoutListener(fitter);
        parent.removeView(web);
        web.destroy();
        web = null;
    }
}
