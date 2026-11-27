package sa.urtv.home;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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
    static final int X = 122, W = 340, TOP = 120, BOTTOM = 554;

    private final Activity activity;
    private final ViewGroup parent;
    private WebView web;
    private String shown;   // the page's address now on the screen, null for none

    SideCards(Activity activity, ViewGroup parent) {
        this.activity = activity;
        this.parent = parent;
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
        if (url.equals(shown) && web != null) return;
        if (web == null) {
            web = WebViews.make(activity);
            if (web == null) return;
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
        }
        shown = url;
        web.loadUrl(url);
    }

    /** the column goes, page and all */
    void drop() {
        shown = null;
        if (web == null) return;
        parent.removeView(web);
        web.destroy();
        web = null;
    }
}
