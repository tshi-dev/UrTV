package sa.urtv.home;

import android.app.Activity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * The football home's three widgets as buttons (docs/PROPOSALS-NOV2.md §1): «الطقس», «أوقات الصلاة» and «مباريات اليوم» are drawn by the
 * page (#football=1, in HomeCard's one WebView), and a transparent focus target sits over each, as the «المهام» card's does. The page says
 * where each one is (fbRects); the focus and OK stay Android's. While the games have the focus they open into a row of cards (fbFocus('mt'))
 * that ◀ ▶ walk (fbMove) and the home moves the favourites' tray down 40 px for it (MainActivity.lowerTray); OK opens «المباريات» on the
 * chosen game (fbPick).
 */
final class FootballFocus {
    /** What the home screen does with the widgets. */
    interface Host {
        /** the bottom of the clock and the buttons, in px from the top of the screen: the widgets' titles start under it */
        int headerBottom();
        /** the home is in its light look now */
        boolean light();
        void focus(View target, String which, boolean has);
        /** OK on a widget: "wx", "pr", or "mt" with the chosen game's id ("" when there is none) */
        void open(String which, String eventId);
    }

    static final String WX = "wx", PR = "pr", MT = "mt";
    static final String[] KEYS = {WX, PR, MT};
    /** the WebView's height at 1080p: the open row ends at 824, a little room for its ring's shadow */
    static final int HEIGHT_1080 = 832;

    private final Activity activity;
    private final ViewGroup parent;
    private final WebView web;
    private final FrameLayout.LayoutParams webParams;
    private final Host host;
    private final View[] targets = new View[KEYS.length];

    FootballFocus(Activity activity, ViewGroup parent, WebView web, FrameLayout.LayoutParams webParams, Host host) {
        this.activity = activity;
        this.parent = parent;
        this.web = web;
        this.webParams = webParams;
        this.host = host;
        String[] names = {S.t("الطقس"), S.t("أوقات الصلاة"), S.t("مباريات اليوم")};
        for (int i = 0; i < KEYS.length; i++) {
            final String key = KEYS[i];
            View t = new View(activity);
            t.setId(View.generateViewId());
            t.setFocusable(true);
            t.setContentDescription(names[i]);
            t.setOnFocusChangeListener((v, has) -> {
                if (has) js("fbFocus('" + key + "')", this::place);
                else v.post(() -> { if (target() == null) js("fbFocus('')", this::place); });   // the focus left the widgets
                host.focus(v, key, has);
            });
            t.setOnClickListener(v -> {
                if (MT.equals(key)) js("typeof fbPick==='function'?fbPick():''", id -> host.open(MT, unquote(id)));
                else host.open(key, "");
            });
            if (MT.equals(key)) t.setOnKeyListener((v, code, e) -> {
                if (code != KeyEvent.KEYCODE_DPAD_LEFT && code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                if (e.getAction() == KeyEvent.ACTION_DOWN) js("fbMove(" + (code == Dir.forwardKey() ? 1 : -1) + ")", null);
                return true;   // ◀ ▶ walk the games; the row's ends keep the focus here
            });
            targets[i] = t;
            parent.addView(t, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Dir.start()));
        }
    }

    /** The target of a widget: "wx", "pr" or "mt". */
    View target(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return targets[i];
        return null;
    }

    /** The widget that has the focus now, null for none. */
    private String target() {
        for (int i = 0; i < KEYS.length; i++) if (targets[i].hasFocus()) return KEYS[i];
        return null;
    }

    void remove() { for (View t : targets) parent.removeView(t); }

    /** The page is loaded or laid out again: where the widgets start (under the clock), the look, and the targets over them. */
    void fit() {
        int h = activity.getResources().getDisplayMetrics().heightPixels;
        js("typeof fbTop==='function'?fbTop(" + Math.round(host.headerBottom() * 1080f / Math.max(1, h)) + "):''", null);
        light(host.light());
        place();
    }

    void light(boolean on) { js("typeof fbLight==='function'?fbLight(" + on + "):''", null); }

    /** Each target exactly over its widget, as the page says ("wx:l,t,w,h;pr:…;mt:…" in CSS px); until it has said, they stay 1 px. */
    private void place(String ignored) { place(); }
    void place() {
        js("typeof fbRects==='function'?fbRects():''", value -> {
            float d = activity.getResources().getDisplayMetrics().density;   // the page's px are dp
            for (String part : unquote(value).split(";")) {
                String[] kv = part.split(":");
                View t = kv.length == 2 ? target(kv[0]) : null;
                String[] r = kv.length == 2 ? kv[1].split(",") : new String[0];
                if (t == null || r.length != 4) continue;
                try {
                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) t.getLayoutParams();
                    p.width = Math.max(1, Math.round(Float.parseFloat(r[2]) * d));
                    int x = Math.round(Float.parseFloat(r[0]) * d);   // from the page's left; the targets hang from the reading start (Dir.start())
                    if (Dir.rtl()) p.rightMargin = webParams.width - x - p.width; else p.leftMargin = x;
                    p.topMargin = webParams.topMargin + Math.round(Float.parseFloat(r[1]) * d);
                    p.height = Math.max(1, Math.round(Float.parseFloat(r[3]) * d));
                    t.setLayoutParams(p);
                } catch (NumberFormatException ignoredNumber) { }
            }
        });
    }

    private interface Then { void got(String value); }

    private void js(String code, Then then) {
        web.evaluateJavascript(code, then == null ? null : v -> then.got(v == null ? "" : v));
    }

    /** a JS string as evaluateJavascript hands it back ("\"1\"") to its text */
    static String unquote(String v) {
        if (v == null || v.equals("null")) return "";
        v = v.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
        return v.replace("\\\"", "\"");
    }
}
