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
    /** «ألبوم صوري»: not a real address; the page asks for N.jpg?s=900 and dates.json here and the screensaver app's provider answers (READ_CONFIG, read-only) */
    private static final String OWN_BASE = "https://own.urtv.invalid/";
    private static final Uri OWN_URI = Uri.parse("content://sa.urtv.screensaver.config/own");

    private WebResourceResponse ownPhoto(String name) {
        try {
            if ("dates.json".equals(name)) {
                StringBuilder b = new StringBuilder("[");
                try (Cursor c = activity.getContentResolver().query(OWN_URI, null, null, null, null)) {
                    String d = c != null && c.moveToFirst() ? c.getString(c.getColumnIndex("dates")) : "";
                    if (d != null && !d.isEmpty()) {
                        String[] all = d.split(",");
                        for (int k = 0; k < all.length; k++) if (all[k].matches("[0-9-]{10}")) b.append(k > 0 ? "," : "").append('"').append(all[k]).append('"');
                    }
                }
                return new WebResourceResponse("application/json", "UTF-8", new java.io.ByteArrayInputStream(b.append(']').toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("([1-9]|[1-4][0-9]|50)\\.jpg(\\?s=([0-9]{3,4}))?").matcher(name);
            if (!m.matches()) return null;
            int n = Integer.parseInt(m.group(1)), max = m.group(3) == null ? 1920 : Math.min(1920, Integer.parseInt(m.group(3)));
            // one photo at a time, decoded smaller than the screen when the card is small: the album never holds two big bitmaps
            try (java.io.InputStream in = new java.io.FileInputStream(activity.getContentResolver().openFileDescriptor(Uri.withAppendedPath(OWN_URI, String.valueOf(n)), "r").getFileDescriptor())) {
                java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(1 << 18);
                byte[] buf = new byte[16384];
                for (int r; (r = in.read(buf)) > 0; ) o.write(buf, 0, r);
                return new WebResourceResponse("image/jpeg", null, new java.io.ByteArrayInputStream(PhotoShrink.toSize(o.toByteArray(), max)));
            }
        } catch (Exception | OutOfMemoryError e) {
            return new WebResourceResponse("image/jpeg", null, 404, "Not Found", Collections.<String, String>emptyMap(), new java.io.ByteArrayInputStream(new byte[0]));
        }
    }

    private static final String FONTS = "https://raw.githubusercontent.com/tshi-dev/UrTV/master/assets/fonts/";

    /** Sent inside the app when the cards were changed from the phone: the home screen in front follows at once. */
    static final String CHANGED = "sa.urtv.home.CARDS_CHANGED";

    /** The box in pixels, {width, height}, for that many cards; asked again whenever the screen is laid out. */
    /** {width, height} of the box for that many cards; with {@code row} (4.4 «التخطيط: كنبة») {width, height, its distance from the screen's bottom} of the strip above the tray */
    interface Sizer { int[] box(int cards, boolean row); }

    /** What the home screen does with the «المهام» card (and the stocks card): it gained or lost the focus, OK was pressed on it. */
    interface Live { void focus(View target, boolean has); void open(); default void markets() { } }

    private final Activity activity;
    private final ViewGroup parent;
    private final FrameLayout.LayoutParams params;
    private final Sizer sizer;
    private WebView web;
    private String url;
    private volatile int cards;   // how many the page in `web` shows
    /** how many cards the page shows now (0: none, or the football layout) */
    int shownCards() { return cards; }
    private volatile boolean tasks;   // «المهام» is one of them: `target` exists
    private View target;
    private FrameLayout.LayoutParams targetParams;
    private volatile boolean stocks;   // «الأسهم» is one of them: `market` exists
    private View market;
    private FrameLayout.LayoutParams marketParams;
    private volatile boolean stocksFirst;   // the stocks card comes before «المهام» among the cards
    private final Live live;
    private final RenderGuard guard = new RenderGuard();
    private boolean resumed;
    /**
     * «المظهر ← الواجهة» is «كرة القدم» (docs/PROPOSALS-NOV2.md §1): the page shows the football home's three widgets instead of the cards,
     * from the screen's top to above the favourites' tray, and FootballFocus puts a focus target over each. Nothing else changes.
     */
    private volatile boolean football;
    /** 4.4 «التخطيط: كنبة»: the cards in one low strip above the apps tray, the screen's width (the page's #row=1); the big clock is then alone in the middle */
    private volatile boolean row;
    /** 4.4 beta 2 the full look «tvOS» (layout tvos): the row above the tvOS bar, its cards as glass (the page's html.tvos), each slot's CardFit decision */
    private volatile boolean tvos;
    boolean tvos() { return tvos && cards > 0; }

    /** «tvOS»: three cards across; two at 720p with the TV's large font (the third takes turns in the second slot, the page's rowOverflow) */
    private int tvosSlots() {
        android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        return CardBox.tvosSlots(dm.heightPixels, activity.getResources().getConfiguration().fontScale);
    }
    /** the cards are in the strip above the tray (not beside the clock) */
    boolean inRow() { return row && cards > 0; }
    /** the layout the home showed last, for the report (a word, nothing else) */
    static volatile boolean footballShown;
    private FootballFocus.Host footballHost;
    private FootballFocus footballFocus;
    /** the cards' own place, put back when the home leaves the football layout */
    private final int gravity0, left0, top0, right0;

    HomeCard(Activity activity, ViewGroup parent, FrameLayout.LayoutParams params, Sizer sizer, Live live) {
        this.activity = activity;
        this.parent = parent;
        this.params = params;
        this.sizer = sizer;
        this.live = live;
        gravity0 = params.gravity; left0 = params.leftMargin; top0 = params.topMargin; right0 = params.rightMargin;
    }

    /** What the home does with the football layout's widgets (set once, before the first refresh). */
    void football(FootballFocus.Host host) { footballHost = host; }

    /** The football layout is on (the page in the WebView shows its widgets). */
    /** a page is being loaded now: the home's last picture (HomeSnapshot) stays in front of it until it has painted */
    boolean loading() { return loading; }
    private boolean loading;

    boolean isFootball() { return web != null && footballFocus != null; }

    /** A football widget's focus target ("wx", "pr", "mt"), null when the layout is off. */
    View footballTarget(String key) { return footballFocus == null ? null : footballFocus.target(key); }

    /** The home's look changed: the widgets' glass follows (the cards keep theirs). */
    void light(boolean on) { lightNow = on; if (footballFocus != null) footballFocus.light(on); }
    /** the look of the home now (GlassStyle.LOOKS): the cards' material is this look's */
    void look(String look) { lookNow = GlassStyle.look(look); }
    private volatile String lookNow = GlassStyle.LOOKS[0];

    private volatile boolean lightNow;

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
        if (footballFocus != null) { footballFocus.fit(); return; }   // the widgets' box is the screen's width, always: only where they start moves
        int[] b = sizer.box(cards, row);
        if (b[0] == params.width && b[1] == params.height && (!row || b[2] == params.bottomMargin)) return;
        place(b);
        web.setLayoutParams(params);
        placeTarget();
        placeMarket();
    }

    /**
     * onResume: follow the current settings; a changed choice gets a fresh page. P1: the settings, the page's address and the copy of the screensaver's newer page are read OFF the UI
     * thread (they were a provider query and a file read in onResume, the TCL's flicker with B7): the card that is there goes on at once, with the last frame it drew, and a changed
     * address is applied when it comes.
     */
    void refresh() {
        resumed = true;
        idle.removeCallbacks(drop);
        idle.removeCallbacks(night);   // in use again: never let go now
        if (web != null && !loading) { idle.removeCallbacks(kickPull); idle.postDelayed(kickPull, 5_000); }
        if (web != null) resumeWeb();
        final int gen = ++refreshGen;
        new Thread(new Runnable() {
            @Override public void run() {
                final String want = readUrl();
                activity.runOnUiThread(new Runnable() {
                    @Override public void run() { if (gen == refreshGen && resumed) applyUrl(want); appliedGen = gen; }
                });
            }
        }, "homecard-url").start();
    }

    private int refreshGen, appliedGen;
    private volatile String previewLayout, previewCards;
    /** 4.5 «المظهر»: the layout and the cards a look under the focus would give (null: the saved ones); the next refresh shows them */
    void preview(String layout, String cards) { previewLayout = layout; previewCards = cards; }

    /** 4.5 HomeLook: the last refresh's url is in place and its page has drawn (or there is no page): the cover over a change of look may go */
    boolean settled() { return appliedGen == refreshGen && !loading && (web == null || web.getAlpha() >= 1f); }

    /** P9 (weak TVs only): the card's page is let go after this long behind another app; the return builds it again (a page is some tens of MB of the renderer's memory) */
    static final long IDLE_DROP_MS = 10 * 60 * 1000;
    private final android.os.Handler idle = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable drop = new Runnable() { @Override public void run() { if (!resumed) destroy(); } };

    /** 4.4 beta 3 (NightRebuild): when the page was built (elapsedRealtime, the TV's sleep counted) */
    private long builtAt;
    /** NightRebuild: while the home is away, the page is let go once a day at night (never in use); the next return builds it again */
    private final Runnable night = new Runnable() {
        @Override public void run() {
            if (resumed || web == null) return;
            if (NightRebuild.due(false, android.os.SystemClock.elapsedRealtime() - builtAt, java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY))) {
                android.util.Log.i("UrTVCard", "card rebuild=nightly");   // a fixed word
                destroy();
            } else idle.postDelayed(this, NightRebuild.CHECK_MS);
        }
    };

    /** «UrTV النظيف» without the card: its page is not made (and the one that was is let go) */
    void hide(boolean on) { hidden = on; if (on) destroy(); }
    private volatile boolean hidden;

    private void applyUrl(String want) {
        if (want == null || hidden) { destroy(); return; }
        if (want.equals(url)) return;
        destroy();
        if (!guard.mayCreate(android.os.SystemClock.elapsedRealtime())) return;   // its renderer keeps dying: the cards stay off for now
        if (!create(want)) return;   // 4.3.5: no WebView on this TV: the home without its card, never a crash (WebViews)
        web.onResume();
        web.resumeTimers();
        ConfigSnapshot k = ConfigSnapshot.known();
        if (android.os.Build.VERSION.SDK_INT >= 23 && k != null && k.uiSmooth) {   // 4.4 beta 2 «تبديل الشكل بلا وميض»: a new page is shown from its first drawn frame (onPageCommitVisible), never as an empty (black) box while it loads
            final WebView w = web;
            w.setAlpha(0f);
            w.postDelayed(new Runnable() { @Override public void run() { w.setAlpha(1f); } }, HomeLook.WAIT_MS);   // at the latest
        }
    }

    /** the page runs again; B7 (first-frame guard): after another app the card flickered (a blank or half-drawn frame before the page painted again): it is shown once the page has drawn its first frame after the return (visual state), at the latest after 700 ms */
    private void resumeWeb() {
        web.onResume();
        web.resumeTimers();
        if (guardNext && android.os.Build.VERSION.SDK_INT >= 23) {
            final WebView w = web;
            w.setAlpha(0f);
            w.postVisualStateCallback(++guardId, new WebView.VisualStateCallback() {
                @Override public void onComplete(long id) { if (w.getAlpha() < 1f) w.setAlpha(1f); dropStill(); }
            });
            w.postDelayed(new Runnable() { @Override public void run() { w.setAlpha(1f); dropStill(); } }, 700);
        } else dropStill();
        guardNext = false;
    }

    /**
     * 4.4 beta 3: the card's last frame, drawn once into a picture as an app comes in front, shown in the page's place (the same box, under it)
     * until the page has drawn again after the return: the way back shows the cards as they were, never a black or empty box. A page that cannot
     * be drawn this way leaves a clear picture (the box as before: no worse). Dropped as soon as the page is back, or with it.
     */
    private android.widget.ImageView still;

    private void keepStill() {
        if (web == null || web.getWidth() <= 0 || web.getHeight() <= 0 || !(web.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        dropStill();
        try {
            android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(web.getWidth(), web.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
            web.draw(new android.graphics.Canvas(b));
            still = new android.widget.ImageView(activity);
            still.setImageBitmap(b);
            still.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
            still.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            still.setTranslationX(web.getTranslationX());
            still.setTranslationY(web.getTranslationY());
            parent.addView(still, parent.indexOfChild(web), new FrameLayout.LayoutParams((FrameLayout.LayoutParams) web.getLayoutParams()));
        } catch (RuntimeException | OutOfMemoryError e) {
            still = null;   // no picture: the card as before
        }
    }

    private void dropStill() {
        if (still == null) return;
        parent.removeView(still);
        still = null;
    }

    /** the card was paused (an app was in front): the next return holds the card until its first frame (B7) */
    private boolean guardNext;
    private long guardId;

    /** onPause: no timers or network while an app is in front. pauseTimers()/resumeTimers() are process-wide: safe here only because this card is the
     *  launcher's only WebView (tests/home_card_test.py checks it), so whoever adds a second WebView has to revisit both. */
    void pause() {
        resumed = false;
        idle.removeCallbacks(kickPull);   // 4.4 the alarms already set cover the next 8 days
        guardNext = web != null;
        keepStill();   // 4.4 beta 3 (users of 4.3.6: «البطاقات تصير صورة سوداء» on the way back): the last frame stays in the card's place
        if (web != null) { web.onPause(); web.pauseTimers(); }
        if (web != null && Ram.tight(activity)) idle.postDelayed(drop, IDLE_DROP_MS);   // P9: a weak TV does not keep a page it is not showing for long
        idle.removeCallbacks(night);
        if (web != null) idle.post(night);   // NightRebuild: asked now, then every half hour while away
    }

    void destroy() {
        if (web == null) return;
        idle.removeCallbacks(kickPull);
        idle.removeCallbacks(night);
        if (target != null) { parent.removeView(target); target = null; targetParams = null; }
        if (footballFocus != null) { footballFocus.remove(); footballFocus = null; }
        if (market != null) { parent.removeView(market); market = null; marketParams = null; }
        dropStill();
        parent.removeView(web);
        web.destroy();
        web = null;
        url = null;
    }

    private String readUrl() {
        String frag = null, panel = null, layout = null, fit = "";
        try (Cursor c = activity.getContentResolver().query(CONFIG, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                frag = c.getString(0);
                panel = c.getString(1);
                int all = c.getColumnIndex("home_panels");   // "a,b,c": every card; an older screensaver has only the first
                if (all >= 0 && c.getString(all) != null) panel = c.getString(all);
                int l = c.getColumnIndex("home_layout");   // «المظهر ← الواجهة»; an older screensaver says nothing: classic
                layout = l >= 0 && c.getString(l) != null ? c.getString(l) : "classic";
                int cf = c.getColumnIndex("card_fit");   // 4.4 beta 2: each slot's decision (CardFit: "", wide, row4, picks, no); an older screensaver has none
                if (cf >= 0 && c.getString(cf) != null) fit = c.getString(cf);
            }
        } catch (RuntimeException e) {
            // the screensaver's settings are out of reach
        }
        SharedPreferences sp = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (frag != null && panel != null) {   // remember what was read, for when it can't be read; P6: written only when it changed, not at every return
            if (!frag.equals(sp.getString("frag", null)) || !panel.equals(sp.getString("panel", null)) || (layout != null && !layout.equals(sp.getString("layout", null))))
                sp.edit().putString("frag", frag).putString("panel", panel).putString("layout", layout).apply();
        }
        if (layout == null) layout = sp.getString("layout", "classic");
        // Unreadable settings (the screensaver is missing, too old, or signed with another key, e.g. reinstalled
        // before this app): keep the last cards chosen, «بدون» included. Only with nothing remembered, the prayers.
        if (previewLayout != null) layout = previewLayout;   // 4.5 «المظهر»'s preview: what is under the focus, never saved
        if (previewCards != null) panel = previewCards;
        String[] choice = HomeCardChoice.pick(frag, panel, sp.getString("frag", null), sp.getString("panel", null));
        football = HomeCardChoice.football(layout, choice) && footballHost != null;   // 4.3.4: «بدون» turns the football home's widgets off too
        footballShown = football;   // «أرسل تقرير» says which layout the home had
        if (football) { cards = 0; tasks = false; stocks = false; stocksFirst = false; row = false; tvos = false; return withGlass(HomePage.fresh(activity, HomeCardChoice.footballUrl(choice))); }
        row = "couch".equals(layout);
        tvos = "tvos".equals(layout);
        if (tvos) row = true;   // «tvOS»: a row too, above the tvOS bar (CardBox.tvosRow)
        cards = HomeCardChoice.count(choice);
        tasks = HomeCardChoice.hasTasks(choice);
        stocks = HomeCardChoice.hasStocks(choice);
        stocksFirst = HomeCardChoice.stocksFirst(choice);
        String u = HomePage.fresh(activity, HomeCardChoice.liveUrl(choice));   // the screensaver's newer page when it has one
        if (tvos && u != null && u.contains("#")) u = u + "&tvos=1&fit=" + android.net.Uri.encode(fit) + "&slots=" + tvosSlots();   // the page's tvOS glass, how each slot fits, and how many show
        return withGlass(row && u != null && u.contains("#") ? u + "&row=1" : u);
    }

    /**
     * design/43 «توحيد المظهر»: the cards wear the home's one material (GlassStyle.material, CARDS), the same as the upper buttons and the apps bar, so the stocks, the prayers, the
     * games and the tasks are one family with them in every look. The page reads it as #hpanel= and #hline= (CSS html.hcard) and, with the switch on, its ink (html.hsync).
     */
    private String withGlass(String u) {
        if (u == null || !u.contains("#") || DebugFlags.on(activity, "nocardglass")) return u;   // (a tester's switch: the cards keep their own look)
        return u + glassQuery(GlassStyle.material(lookNow, lightNow, GlassStyle.CARDS));
    }

    /**
     * the cards' material as the page reads it: its fill and line always; with «توحيد المظهر» on its ink too (#hink=, #hsub=, #hacc=: the look's text, second text and accent, so on
     * a light look the cards are the white bar with dark words). Off, the cards keep the page's own words.
     */
    static String glassQuery(GlassStyle.Material m) {
        String q = "&hpanel=" + GlassStyle.rgba(m.fill) + "&hline=" + GlassStyle.rgba(m.line);
        return m.ownInk ? q + "&hink=" + GlassStyle.rgba(m.text) + "&hsub=" + GlassStyle.rgba(m.sub) + "&hacc=" + GlassStyle.rgba(m.accent) : q;
    }

    /** 4.4 the followed clubs' coming games: the app asks the page for them (spKickList, a JSON string) after it loads and every 15 minutes while the home is in
     *  front; the page is given no JavaScript interface. KickoffScheduler keeps them and sets the alarm. */
    static final long KICK_PULL_MS = 15 * 60 * 1000;
    private final Runnable kickPull = new Runnable() { @Override public void run() {
        if (web == null) return;
        web.evaluateJavascript("typeof spKickList==='function'?spKickList():''", new ValueCallback<String>() {
            @Override public void onReceiveValue(String v) {
                try {
                    Object o = v == null ? null : new org.json.JSONTokener(v).nextValue();
                    if (o instanceof String && ((String) o).startsWith("[") && ((String) o).length() < 20_000)
                        sa.urtv.home.sports.KickoffScheduler.handOver(activity.getApplicationContext(), (String) o);
                } catch (org.json.JSONException ignored) { }
            }
        });
        idle.removeCallbacks(this);
        if (resumed) idle.postDelayed(this, KICK_PULL_MS);
    } };

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    private boolean create(String u) {
        WebView made = WebViews.make(activity);
        if (made == null) return false;
        web = made;
        builtAt = android.os.SystemClock.elapsedRealtime();
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
                guard.gone(android.os.SystemClock.elapsedRealtime());
                destroy();
                if (resumed) refresh();
                return true;
            }

            /** 4.5: the new page's first frame is on the screen: the card shows from it (applyUrl hid it while it loaded) */
            @Override
            public void onPageCommitVisible(WebView v, String u) { if (v.getAlpha() < 1f) v.setAlpha(1f); }

            // the page asks GitHub for its fonts; answer from the APK
            @Override
            public void onPageFinished(WebView v, String u) {
                loading = false;
                idle.removeCallbacks(kickPull);
                idle.postDelayed(kickPull, 20_000);   // 4.4 the page has read its games by then
                if (footballFocus != null) {   // the widgets are laid out once the page is: where they start, the look, the targets
                    footballFocus.fit();
                    v.postDelayed(() -> { if (footballFocus != null) footballFocus.fit(); }, 1500);
                    return;
                }
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
                if ("file".equals(r.getUrl().getScheme()) && !FileGate.allowed(r.getUrl().getPath(), url == null ? null : Uri.parse(url).getPath())) {   // S1: the page reads its own file and the assets, nothing of the app's folders
                    return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", Collections.<String, String>emptyMap(), new java.io.ByteArrayInputStream(new byte[0]));
                }
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
                if (s.startsWith(OWN_BASE)) return ownPhoto(s.substring(OWN_BASE.length()));
                NetLog.count(s);   // G3: what the card's page asks the network (the request log)
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
        s.setCacheMode(WebSettings.LOAD_DEFAULT);   // G3: the HTTP cache (the page revalidates its data: fetchT, cache:'no-cache')
        // The page sizes its text to the screen; the TV's font size setting (Android scales WebView text by it) made
        // it larger than its boxes, so the left column spilled off screen and moved as the clock and the matches changed.
        s.setTextZoom(100);
        if (football) {   // the screen's width, from its top to the open row's end (FootballFocus.HEIGHT_1080 at 1080p)
            android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            params.gravity = android.view.Gravity.TOP | Dir.start();   // the screen's whole width: either side is the same
            params.leftMargin = params.topMargin = params.rightMargin = 0;
            params.width = dm.widthPixels;
            params.height = Math.round(FootballFocus.HEIGHT_1080 * dm.heightPixels / 1080f);
        } else place(sizer.box(cards, row));
        parent.addView(web, params);
        if (football) footballFocus = new FootballFocus(activity, parent, web, params, footballHost);
        if (tasks) addTarget();
        if (stocks) addMarket();
        url = u;
        loading = true;   // HomeSnapshot's picture stays until the page has painted (onPageFinished)
        web.loadUrl(u);
        return true;
    }

    /** the cards' own place beside the clock, or (row) the strip above the tray: the screen's width less its margins, its distance from the bottom b[2] */
    private void place(int[] b) {
        if (row) {
            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            params.leftMargin = params.rightMargin = params.topMargin = 0;
            params.bottomMargin = b[2];
        } else {
            params.gravity = gravity0; params.leftMargin = left0; params.topMargin = top0; params.rightMargin = right0;
            params.bottomMargin = 0;
        }
        params.width = b[0];
        params.height = b[1];
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
                    for (String n : r) {   // S11: «NaN», «Infinity» or an absurd size from the page is no place
                        float f = Float.parseFloat(n);
                        if (Float.isNaN(f) || Float.isInfinite(f) || Math.abs(f) > 20000f) return;
                    }
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
