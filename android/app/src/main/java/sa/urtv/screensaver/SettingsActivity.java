package sa.urtv.screensaver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Remote-friendly settings: every row is a button that opens a list. */
public class SettingsActivity extends Activity {
    private Prefs prefs;
    private LinearLayout list;
    private ScrollView scroll;
    private int sideWidth, pageWidth;   // px, fixed in onCreate: the side pane and the whole page
    private int gapPx;                  // px between the side and the rows
    private TvosStyle ts;               // 4.4 beta 2 «tvOS»: the 4.3.2 screen's logic drawn in the tvOS manner (TvosStyle); null otherwise
    private CouchStyle cs;              // 4.4 with «كنبة» (the side list): couch-tv's settings, exactly (CouchStyle); null on the 4.3.2 screen
    private Page page;                  // a picker shown in place of the rows (couch-tv's choice pages), or null
    private EditText searchIn;          // the search's typing box (couch-tv's), kept while the search is shown

    /** The settings' scroll: up and down only; nothing can move the page sideways (scrollTo keeps x at 0). */
    static final class UpDownScroll extends ScrollView {
        UpDownScroll(android.content.Context c) { super(c); }
        @Override public void scrollTo(int x, int y) { super.scrollTo(0, y); }
    }
    private String pendingFocus;   // the row to focus once the window has the focus back (a dialog was still open when the list was rebuilt)
    // the first screen is the categories (SettingsHub); a tile opens its rows, Back goes back to that tile
    private String screen;           // null: the categories; else the category whose rows are shown
    private String hubAt;            // the category opened last: its tile has the focus on the way back
    private boolean entering = true; // the screen just changed: the focus goes to its first row (or that tile), not one kept by name
    private String groupCat;   // while render runs: the category of the group being built (a row with no name of its own goes with it)
    private TextView hint;
    private LinearLayout side;       // 4.3.3 the side list (the mockup): «الإعدادات», the search and the ten sections, always there on the right; the rows on the left
    private final java.util.Map<String, View> sideItems = new java.util.LinkedHashMap<>();   // each section's item in the side list, by its key
    private boolean keyed;           // 4.4 beta 4: a key was pressed since the settings opened (before it, a redraw never takes the focus into the rows)
    private boolean sideFocus;       // the next render() leaves the focus on the side list (a section shown while the focus moves along the list)
    /** 4.4 the owner: «شكل كنبة» off = the 4.3.2 settings screen (a first screen of big buttons, «البطاقات» and «المزيد» screens of buttons, Back up a
     *  screen); on = the side list. The same rows either way (render builds each once); SettingsHub says where they sit in the layout chosen. */
    private boolean classic;
    private View sideBox;            // the side's scroll view: the side list, or (classic) a section's title, hidden on a screen of buttons
    private String forceWant;   // the key the next render() puts the focus on (the row or button that opened another screen), whatever has the focus when the list is built
    private String returnWant; private long returnUntil;   // the row the focus came back to: kept for the next builds of the list until a key is pressed
    private String launchedRow;   // the key of the row whose press opened another screen (the clean screen, the TV's settings): the focus returns to it
    private final java.util.Map<String, String> lastRow = new java.util.HashMap<>();   // each category's row last focused, for the way back in

    /**
     * Arabic locale for this screen, so every dialog title, list and button is laid out right-to-left. With «اللغة» English (S.wrap) an
     * English one instead, left to right; with Arabic, the default, S.wrap gives base back and the screen is set up as it always was.
     */
    @Override
    protected void attachBaseContext(android.content.Context base) {
        android.content.Context en = S.wrap(base);
        if (S.en()) { super.attachBaseContext(TextScale.fit(en)); return; }   // 4.4 beta 4: the words scaled to the screen (TextScale)
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar"));
        c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(TextScale.fit(base.createConfigurationContext(c)));
    }

    private FrameLayout root;
    GlassStyle.Look look = GlassStyle.DARK;   // the glass look of now (GlassSource.page), set before each render

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // no window animation: on the TV test one screen of settings was drawn over another while one came in and the other went out
        getWindow().setWindowAnimations(0);
        overridePendingTransition(0, 0);
        prefs = new Prefs(this);
        classic = !"couch".equals(prefs.settingsStyle());   // 4.4 beta 2 «شكل شاشة الإعدادات»: the settings' own style, whatever the look («أزرار» by default); the 4.3.2 hub only with the flag off
        SettingsHub.classic(classic);
        if (!classic) cs = new CouchStyle(this);
        if ("tvos".equals(prefs.settingsStyle())) ts = new TvosStyle(this);   // classic stays true: the same screens, Back and search
        if (getIntent().hasExtra("hubAt")) { screen = SettingsHub.canon(getIntent().getStringExtra("screen")); hubAt = SettingsHub.canon(getIntent().getStringExtra("hubAt")); }   // pickLang: back on its button
        if (getIntent().hasExtra("want")) { forceWant = getIntent().getStringExtra("want"); getIntent().removeExtra("want"); }   // 4.4: back on «شكل كنبة» after it flipped the layout
        UpdateJob.schedule(this);
        final boolean askCity = getIntent().getBooleanExtra("pickCity", false);   // the home's «المدينة» (control center, the question bar): 4.3.3 the one city row, its chooser open
        getIntent().removeExtra("pickCity");
        if (askCity) cityRow();
        if (screen == null && !classic) { screen = SettingsHub.PRAY; sideFocus = true; }   // the side list: the first section shown, the focus on its item there
        final boolean askTeams = getIntent().getBooleanExtra("manageTeams", false);   // 4.3.1: the matches page's «فرقي»
        getIntent().removeExtra("manageTeams");
        // the glass look of all our screens: the home's photo, small and blurred once (GlassSource), under its veil; glass tiles and rows
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        look = GlassSource.page(this, root); couchVeil(); tvosVeil();
        scroll = new UpDownScroll(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(Dir.layout());
        list.setPadding(dp(8), dp(32), dp(8), dp(16));   // 64 px above the first line: with the group header (32 + its text) the first row is not 124 px down
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(144), dp(1100));
        gapPx = dp(36);
        if (cs != null) {   // couch-tv: the whole screen within its safe area, a 36 rem side, a 4 rem gap; the rows' box 1.75 rem wider each side (the focus scales in it)
            width = getResources().getDisplayMetrics().widthPixels - 2 * cs.px(CouchStyle.SAFE_X);
            gapPx = cs.px(CouchStyle.GAP) - cs.px(CouchStyle.PAD);
            list.setPadding(cs.px(CouchStyle.PAD), cs.px(1f), cs.px(CouchStyle.PAD), cs.px(3f));
        }
        frame.addView(list, new FrameLayout.LayoutParams(-1, -2));
        scroll.addView(frame);
        LinearLayout panes = new LinearLayout(this);   // right to left: the side (a category's title), then the rows
        panes.setOrientation(LinearLayout.HORIZONTAL);
        panes.setLayoutDirection(Dir.layout());
        side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setLayoutDirection(Dir.layout());
        side.setPadding(dp(4), dp(24), dp(4), dp(16));
        if (cs != null) side.setPadding(0, 0, 0, cs.px(1f));
        ScrollView sideScroll = new ScrollView(this);   // eleven items: on a small screen the list scrolls with the focus
        sideScroll.setVerticalScrollBarEnabled(false);
        sideScroll.setClipToPadding(false);
        sideScroll.addView(side, new FrameLayout.LayoutParams(-1, -2));
        sideBox = sideScroll;
        // fixed widths, worked out once (no weights): the side and the rows never move sideways (the TV test of 3.147: after 5 down and
        // 5 up the list went from the left half to the right half and back); the rows' width changes only between the categories and a category
        sideWidth = cs != null ? cs.px(CouchStyle.SIDE) : Math.round(width * 0.36f) - dp(36);
        pageWidth = width;
        LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(sideWidth, -1);
        sideLp.setMarginEnd(gapPx);
        if (ts == null) {
        panes.addView(sideScroll, sideLp);
        panes.addView(scroll, new LinearLayout.LayoutParams(width - sideWidth - gapPx + (cs != null ? cs.px(CouchStyle.PAD) : 0), -1));
        } else {   // «tvOS»: the rows at the start side, the section's icon at the other
            sideLp.setMarginEnd(0);
            sideLp.setMarginStart(gapPx);
            panes.addView(scroll, new LinearLayout.LayoutParams(width - sideWidth - gapPx, -1));
            panes.addView(sideScroll, sideLp);
        }
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams panesLp = new LinearLayout.LayoutParams(width, 0, 1f);
        panesLp.gravity = Gravity.CENTER_HORIZONTAL;
        if (cs != null) panesLp.topMargin = cs.px(CouchStyle.SAFE_Y);
        panesLp.bottomMargin = Math.round(getResources().getDisplayMetrics().heightPixels * 0.03f);   // the list down to the 3 % safe area (the hint is on top now)
        column.addView(panes, panesLp);
        hint = new TextView(this);
        hint.setTextColor(look.sub);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setTypeface(font("Thmanyah-Regular"));
        if (cs != null) hint.setTextColor(CouchStyle.DIM);
        hint.setTextDirection(Dir.text());
        // 4.4 the owner (build 413): the hint took the whole band under the list; it is small, on the title's line at the far end, and the list
        // has the band
        // 4.4 the sweep: it was the smallest text on the screen; now the rows' secondary size and colour (HINT_SP; couch-tv HINT, its info line's 1.4375 rem)
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, HINT_SP);
        if (cs != null) { hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.HINT)); hint.setTextColor(CouchStyle.SUB); }
        hint.setGravity(Gravity.END);
        hint.setPadding(0, 0, 0, 0);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams hintAt = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        hintAt.topMargin = cs != null ? cs.px(CouchStyle.SAFE_Y) + cs.px(0.75f) : dp(16);
        hintAt.setMarginEnd(cs != null ? cs.px(CouchStyle.SAFE_X) : dp(36));
        root.addView(hint, hintAt);
        setContentView(root);
        render();
        if (askCity) root.post(new Runnable() { public void run() { pickCity(); } });
        if (askTeams) root.post(new Runnable() { public void run() { manageTeams(); } });
    }

    @Override protected void onNewIntent(Intent in) {   // 4.3.1: «المدينة» of the home again while the settings are already open
        super.onNewIntent(in);
        if (in.getBooleanExtra("pickCity", false)) { in.removeExtra("pickCity"); cityRow(); render(); pickCity(); }
        if (in.getBooleanExtra("manageTeams", false)) { in.removeExtra("manageTeams"); manageTeams(); }
    }

    /**
     * 4.3.3: the city is chosen in one place, «الموقع والطقس» ← «المدينة»; every way to it (the home's «المدينة», the control center, the question bar,
     * an old link) lands on that row, the focus on it, and its chooser opens over it. The phone page sets the same choice (SaverReceiver).
     */
    private void cityRow() {
        screen = SettingsHub.LANG;   // 4.4 beta 4: the city is in «اللغة والموقع»
        hubAt = SettingsHub.LANG;
        entering = true;
        forceWant = "المدينة";
    }

    /** A picker's live preview (a WebView running the page) is let go while another app is in front, and drawn again on return; null when no picker is open. */
    private Runnable previewOff, previewOn;

    @Override
    protected void onPause() {
        super.onPause();
        prefs.lookChanged();   // 4.5: whatever was changed here (the colours, the cards…), the home reads it now, while it is still behind this screen
    }

    @Override
    protected void onStop() {
        if (previewOff != null) previewOff.run();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (previewOn != null) previewOn.run();
    }

    @Override
    protected void onResume() {   // back in front (from the TV's settings, or opened again over itself): show what is set now
        super.onResume();
        final String back = launchedRow;   // read before render() builds the rows again
        launchedRow = null;
        adhanAsked = false;
        cleanAsked = false;   // asked again each time the settings come to the front (it was asked once per visit: the row said «مفعّل» only after reopening)
        homeKeyAsked = false;   // 4.4 (QA-7): the same for «زر الرئيسية يفتح تلفزيونك مباشرة»
        liteLineAsked = false;   // 4.4 beta 3: the same for «الوضع الخفيف»'s line from the home
        extrasLook();   // 4.4 review: «فوق تطبيق آخر» says whether the add-on is here; asked on a thread, once per return
        look =GlassSource.page(this, root); couchVeil(); tvosVeil();   // the home's photo or «المظهر» may have changed meanwhile
        hint.setTextColor(look.sub);
        if (back != null) { returnWant = back; returnUntil = System.currentTimeMillis() + 4000; }   // the home answers a moment later and the list is built again: that build keeps the focus on this row too, until a key is pressed (it landed on the row above)
        forceWant = back;   // back from another screen: the focus goes to the row or button that opened it, in the SAME render that builds the list again (it landed on the one above)
        render();
        // 4.4 #42 (R-473): a TV that needs «الظهور فوق التطبيقات» for the screensaver is asked once by itself (OverlayNeed), never another TV
        if (OverlayNeed.openByItself(prefs.overlayNeeded(), prefs.overlayAsked())) askSaverOverlay();
        // the home writes its state a moment after «UrTV النظيف» closes: one more question 1.5 s later, drawn only when the answer differs
        root.postDelayed(() -> { if (!isFinishing() && !isDestroyed()) { cleanAsked = false; homeCleanSeen(); } }, 1500);
    }

    /** Back on a category's rows (or on «البطاقات», «المزيد») goes back to the screen its button is on, the focus on that button; on the first screen it leaves. */
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent e) {
        if (e.getAction() == android.view.KeyEvent.ACTION_DOWN) returnWant = null;   // the person moves: the focus is theirs again
        if (e.getAction() == android.view.KeyEvent.ACTION_DOWN) keyed = true;   // 4.4 beta 4: from now on a redraw keeps the row the person is on
        if (okHold.key(e.getKeyCode(), e.getAction(), android.os.SystemClock.uptimeMillis())) {
            // a build waited for this release: the row that saw the press takes its click first, then the list is built
            boolean done = super.dispatchKeyEvent(e);
            if (!isFinishing() && !isDestroyed()) render();
            return done;
        }
        return super.dispatchKeyEvent(e);
    }

    /** Back in the rows goes to their section's item in the side list; Back on the side list leaves the settings. */
    @Override
    public void onBackPressed() {
        if (classic) {   // 4.3.2: Back goes up a screen, the focus on the button it came from; on the first screen it leaves
            if (screen == null) { super.onBackPressed(); return; }
            View r = focusedRow();
            if (r != null) lastRow.put(screen, (String) r.getTag());
            hubAt = screen;
            open(SettingsHub.parent(screen));
            return;
        }
        if (page != null) { closePage(true); return; }   // couch-tv: a picker's page back to its section, on the row that opened it
        if (cs != null && SettingsHub.SEARCH.equals(screen)) {   // the search: back to its button, the typing kept until another section opens
            View b = sideItems.get(SettingsHub.SEARCH);
            if (b != null && !b.hasFocus()) { b.requestFocus(); return; }
            super.onBackPressed();
            return;
        }
        View f = focusedRow();
        if (f == null || screen == null || sideItems.get(screen) == null) { super.onBackPressed(); return; }
        lastRow.put(screen, (String) f.getTag());   // the way back in starts there
        sideItems.get(screen).requestFocus();
    }

    /** Shows a section's rows, from the top. */
    private void open(String category) {
        if (category == null || !category.equals(screen)) page = null;
        screen = category;
        entering = true;
        scroll.scrollTo(0, 0);   // the new screen from its top at once, never drawn at the old one's place first
        render();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);   // nothing fades over the screen behind it
    }

    /**
     * The first row (on a screen of buttons: its first line) brings the page back to its very top, so the title and the line over
     * it show again (the TV test: after going down and up again, the header stayed out of sight). After the scroll view's own move.
     */
    private void topWhenFirst(View v) {
        int at = rows().indexOf(v);
        boolean pairs = classic && SettingsHub.hasTiles(screen) && !SettingsHub.wide(screen);   // 4.3.2: a first line of two buttons (the first screen's is the wide one alone)
        if (at == 0 || (pairs && at == 1)) scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private boolean on(String category) { return screen != null && screen.equals(category); }

    /**
     * Builds the list again (on every change, and back in front): the focus stays on the row that had it, by its key (FocusKey), and the
     * list stays where it was scrolled to. A screen just opened starts at its top: the first row, or on the categories the tile last
     * opened (the first one the first time).
     */
    private final OkHold okHold = new OkHold();   // 4.3.4: a build while OK is held waits for its release (the first press was lost)

    private void render() {
        if (okHold.defer(android.os.SystemClock.uptimeMillis())) return;   // done at OK's release, after its click (dispatchKeyEvent)
        latinDigits = DigitsText.isLatin(prefs.digits());
        String fitNote = prefs.takeFitNote();   // 4.4 a home card swapped for one that suits the new look: say so once
        if (fitNote != null) notice(fitNote);
        List<View> before = rows();
        View had = entering ? null : focusedRow();
        // 4.4 beta 4: a render while no row has the focus (the home's answer about the adhan arriving just after the settings open on «الصلاة
        // والأذان», before the side item's focus has even landed) keeps it on the side list; only the person's key takes it into the rows (the
        // emulator's shots: it jumped to the first row)
        if (!classic && !entering && (had == null || !keyed) && page == null && !SettingsHub.SEARCH.equals(screen)) sideFocus = true;   // no row had it (the side item, or its focus still on the way): the side list keeps it
        if (screen == null && !classic) screen = SettingsHub.PRAY;
        final boolean tiles = classic && SettingsHub.hasTiles(screen);   // 4.3.2: the first screen, «البطاقات», «المزيد» are buttons, not rows
        if (returnWant != null && System.currentTimeMillis() > returnUntil) returnWant = null;
        final String want = forceWant != null ? forceWant : returnWant != null && !entering ? returnWant : entering ? (tiles ? (hubAt != null ? "hub:" + hubAt : null) : lastRow.get(screen)) : had == null ? null : (String) had.getTag();
        forceWant = null;
        final int wasIndex = had == null ? 0 : before.indexOf(had), scrollY = entering ? 0 : scroll.getScrollY();
        entering = false;
        groupCat = null;
        hint.setVisibility(View.GONE);   // the audit: «اختيار يفتح، ورجوع يرجّعك خطوة» on every screen, over the first row (the remote's keys say it)
        list.removeAllViews();
        if (classic) classicHead(tiles); else {
        sidePane();
        if (cs != null) couchHead(); else {
        // the pane's head: the section's name, its line, and what is set in it now (the mockup)
        TextView title = new TextView(this);
        title.setText(conv(SettingsHub.name(screen)));
        title.setTextColor(look.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        title.setTextDirection(Dir.text());
        title.setPadding(dp(10), 0, dp(10), dp(4));
        list.addView(title);
        line(SettingsHub.about(screen), look.sub, 0);
        String now = hubSummary(screen);
        if (!now.isEmpty()) line(now, look.accent, dp(8));
        }
        }
        if (cs != null && (page != null || SettingsHub.SEARCH.equals(screen))) {   // a picker or the search in place of the rows
            if (page != null) pageRows(); else searchRows();
            return;
        }
        // on the categories only: on «اللغة والموقع» the city's own row is right under it and said it twice (the audit, S14)
        if (screen == null && prefs.cityNoteDue()) line(Location.note(Words.city(prefs.city())), look.accent, dp(14));   // still the install's Riyadh: say where to change it
        actions.clear();

        row("معاينة الشاشة", "شاهد الشاشة الآن", R.drawable.ic_play, new Runnable() { public void run() {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class)); } });

        // the place, then how its times are worked out, then the minutes of one's own mosque; the line under them says which is which
        header("الموقع");
        row("الدولة", Location.countryLabel(prefs.cityCountry(), prefs.cityTz()), R.drawable.ic_place, new Runnable() { public void run() { pickCountry(); } });
        row("المدينة", Words.city(prefs.city()), R.drawable.ic_place, new Runnable() { public void run() { pickCity(); } });

        row("طريقة الحساب", PrayerMethod.label(prefs.prayerMethod(), prefs.cityCountry(), prefs.cityTz()), R.drawable.ic_place,
            new Runnable() { public void run() { pickPrayerMethod(); } });
        row("تعديل وقت الصلاة", prefs.prayerAdjOn() ? "مفعّل، لكل صلاة دقائقها من −٢٠ إلى +٢٠ (الشروق ثابت)" : "متوقف، الأوقات كما تُحسب", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPrayerAdjOn(!prefs.prayerAdjOn()); render(); } });
        if (prefs.prayerAdjOn()) for (int k = 0; k < PrayerAdj.COUNT; k++) adjRow(k);
        String fixed = PrayerFix.applied(prefs.prayerAdjOn(), prefs.prayerAdj(), prefs.prayerFixKnown(() -> { if (!isFinishing() && !isDestroyed()) render(); }));   // 4.3.3: never silent
        if (!fixed.isEmpty()) { line("تصحيح منشور لأوقات اليوم، وتعديلك لأي صلاة يبقى هو المعتمد", look.sub, 0); line(fixed, look.sub, 0); }
        if (on(SettingsHub.LANG) || on(SettingsHub.PRAY)) line(Location.HELP, look.sub, 0);

        header("المباريات");
        // «المباريات»: the home app's page of the games (docs/PROPOSALS-NOV2.md §1), the days and the leagues, the followed teams first
        row("مباريات اليوم", "الأيام والدوريات، وفرقك أولاً", R.drawable.ic_trophy, new Runnable() { public void run() { openMatches(); } });
        row("الفرق المتابعة (" + arabicNum(prefs.teams().size()) + (prefs.teamCap() == 0 ? "" : "/" + arabicNum(prefs.teamCap())) + ")", teamsLabel(),
            R.drawable.ic_trophy, new Runnable() { public void run() { manageTeams(); } });
        // 4.4 the owner (R-472): the clubs' ceiling, 40 by default; raised to 80 or none only after the warning (FollowLimit.TEAM_CAP_WARNING)
        row("حد الفرق", FollowLimit.teamCapName(prefs.teamCap()) + (prefs.teamCap() == FollowLimit.MAX_TEAMS ? "، الأصل" : ""), R.drawable.ic_trophy,
            new Runnable() { public void run() { pickTeamCap(); } });

        // 4.3 «تطبيق المباريات»: «شاهد المباراة» on a game opens it (UrTV Player is told which game; another app just opens)
        row("تطبيق المباريات", matchAppLabel(), R.drawable.ic_play, new Runnable() { public void run() { pickMatchApp(); } });

        row("الدوريات كاملة", leaguesLabel(), R.drawable.ic_trophy,
            new Runnable() { public void run() { startActivity(new Intent(SettingsActivity.this, LeaguesActivity.class)); } });   // 4.3.1 «اختر دورياتك»: one screen (the older dialogs below stay for saver.sh's sake)
        // «بطاقة المباريات لليوم فقط»: the «المباريات» card (beside the clock, on the home) shows only the games of the city's day; off by default
        row("بطاقة المباريات لليوم فقط", prefs.matchesToday() ? "مفعّل، كل مباراة في يومها" : "متوقف، أقرب مباريات الأسبوع", R.drawable.ic_trophy,
            new Runnable() { public void run() { prefs.setMatchesToday(!prefs.matchesToday()); render(); } });

        row("رياضات أخرى", labels(Prefs.EVENTS, prefs.list("events"), "بدون"), R.drawable.ic_trophy,
            new Runnable() { public void run() { pickMany("events", "رياضات أخرى", Prefs.EVENTS, "بدون اختيار = بدون"); } });

        // 4.4 «تنبيه بداية المباراة» (the home app plays the whistle and draws the notice; this app keeps the choices)
        row("تنبيه بداية المباراة", prefs.kickoff() ? "مفعّل لفرقك" : "متوقف", R.drawable.ic_trophy,
            new Runnable() { public void run() { prefs.setKickoff(!prefs.kickoff()); render(); } });
        row("صوت التنبيه", prefs.kickoffWhistle() ? "مفعّل" : "متوقف", R.drawable.ic_trophy,
            new Runnable() { public void run() { prefs.setKickoffWhistle(!prefs.kickoffWhistle()); render(); } });
        row("عند الهدف", prefs.kickoffGoal() ? "مفعّل" : "متوقف", R.drawable.ic_trophy,
            new Runnable() { public void run() { prefs.setKickoffGoal(!prefs.kickoffGoal()); render(); } });
        row("صافرة النهاية", prefs.kickoffEnd() ? "مفعّل" : "متوقف", R.drawable.ic_trophy,
            new Runnable() { public void run() { prefs.setKickoffEnd(!prefs.kickoffEnd()); render(); } });
        row("فوق تطبيق آخر", prefs.kickoffOver() ? (Boolean.FALSE.equals(extrasHere) ? "مفعّل، لكنه يحتاج «إضافات تلفزيونك»" : extrasHere == null ? "مفعّل" : "مفعّل لو سمحت للإضافة بالظهور فوق التطبيقات") : "متوقف", R.drawable.ic_trophy,
            new Runnable() { public void run() { prefs.setKickoffOver(!prefs.kickoffOver()); render(); if (prefs.kickoffOver()) askOverlay(); } });
        row("جرّب التنبيه", "يظهر الآن بفريقين للتجربة", R.drawable.ic_trophy,
            new Runnable() { public void run() { KickoffTry.show(SettingsActivity.this, prefs.kickoffWhistle()); } });

        header("العرض");
        noConv = true;
        row("الأرقام", Prefs.digitsLabel(prefs.digits()), R.drawable.ic_language,
            new Runnable() { public void run() { pickDigits(); } });
        noConv = false;
        // 4.3.3 «التاريخ بالأرقام»: the Hijri and the Gregorian as day/month/year on the screensaver and the home, in the digits above
        row("التاريخ بالأرقام", prefs.dateNum() ? "مفعّل" : "متوقف", R.drawable.ic_language,
            new Runnable() { public void run() { prefs.setDateNum(!prefs.dateNum()); render(); } });
        // GitHub #15 «التاريخ الهجري»: off = the Gregorian date alone (the screensaver, the home's cards and the home's date line)
        row("التاريخ الهجري", prefs.hijriShown() ? "مفعّل" : "متوقف", R.drawable.ic_language,
            new Runnable() { public void run() { prefs.setHijriShown(!prefs.hijriShown()); render(); } });
        // OK goes round: on with 10 minutes, on with 15 minutes (4.4 beta 4), off
        row("تنبيه قبل الأذان", prefs.adhanBar() ? (prefs.adhanLead() == 15 ? "مفعّل، آخر ١٥ دقيقة قبل الأذان" : "مفعّل، آخر ١٠ دقائق قبل الأذان") : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() {
                if (!prefs.adhanBar()) { prefs.setAdhanLead(10); prefs.setAdhanBar(true); }
                else if (prefs.adhanLead() == 10) prefs.setAdhanLead(15);
                else prefs.setAdhanBar(false);
                render(); } });
        // from PR #28 and the requests: each off until chosen, OK flips it
        row("الشروق بعد الفجر", prefs.sunrise() ? "مفعّل، كم باقي على الشروق بعد الفجر" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setSunrise(!prefs.sunrise()); render(); } });
        row("شريط الوقت بين الصلاتين", prefs.prayerBar() ? "مفعّل، خط رفيع تحت الصلاة الجاية يمتلي لين يجي وقتها" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPrayerBar(!prefs.prayerBar()); render(); } });
        row("الرطوبة والرياح", prefs.wxMore() ? "مفعّل، تحت المدينة أعلى اليسار" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setWxMore(!prefs.wxMore()); render(); } });
        row("أسماء الأشهر الميلادية", Prefs.monthsLabel(prefs.months()), R.drawable.ic_language,
            new Runnable() { public void run() { prefs.setMonths("levant".equals(prefs.months()) ? "default" : "levant"); render(); } });
        row("شريط السنة تحت الساعة", prefs.yearStrip() ? "مفعّل، نقاط السنة واليوم يلمع" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setYearStrip(!prefs.yearStrip()); render(); } });
        // «ألبوم صوري» (the card): how fast the photos change, and the date under each
        row("سرعة ألبوم صوري", "minute".equals(prefs.albumMode()) ? "كل دقيقة" : "ten".equals(prefs.albumMode()) ? "كل ١٠ ثوانٍ" : "كل ٤ ثوانٍ", R.drawable.ic_eye,
            new Runnable() { public void run() { String m = prefs.albumMode(); prefs.setAlbumMode("seconds".equals(m) ? "ten" : "ten".equals(m) ? "minute" : "seconds"); render(); } });
        row("تاريخ الصورة في ألبوم صوري", prefs.albumDate() ? "ظاهر تحت الصورة" : "مخفي", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setAlbumDate(!prefs.albumDate()); render(); } });
        row("عنوان ألبوم صوري", prefs.albumTitle() ? "ظاهر فوق الصورة" : "مخفي", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setAlbumTitle(!prefs.albumTitle()); render(); } });
        row("شكل صورة ألبوم صوري", "fill".equals(prefs.albumFit()) ? "تعبّي البطاقة ويبان أعلاها" : "كاملة بلا قص", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setAlbumFit("fill".equals(prefs.albumFit()) ? "fit" : "fill"); render(); } });
        row("حجم ألبوم صوري", "tall".equals(prefs.albumSize()) ? "طويل بضعف الارتفاع" : "عادي", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setAlbumSize("tall".equals(prefs.albumSize()) ? "normal" : "tall"); render(); } });
        // «توهّج لون التطبيق»: the wash behind the focused app on the home; off until the person turns it on
        row("توهّج لون التطبيق", "on".equals(prefs.glowMode()) ? "مفعّل" : "light".equals(prefs.glowMode()) ? "خفيف" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setGlowMode("off".equals(prefs.glowMode()) ? "light" : "light".equals(prefs.glowMode()) ? "on" : "off"); render(); } });
        // A14 «الوضع الخفيف»: auto (a weak TV: little memory or few cores) by default; on and off are the person's. Less motion, slower refresh, smaller photos, no blur
        final String liteLine = "auto".equals(prefs.lite()) ? liteLineSeen() : null;   // 4.4 beta 3: the home's own line when this TV's signs turned it on
        row("الوضع الخفيف", liteLine != null && !liteLine.isEmpty() ? liteLine : Prefs.liteLabel(prefs.lite(), Prefs.lowRam(this)), R.drawable.ic_eye,
            new Runnable() { public void run() { String v = prefs.lite(); prefs.setLite("auto".equals(v) ? "on" : "on".equals(v) ? "off" : "auto"); render(); } });
        // «نص أوضح»: the grey words and numbers of the home and the screensaver in the look's full text colour (white; the dark ink in a light look)
        row("نص أوضح", prefs.clearText() ? "مفعّل الكلام الرمادي بلون النص الكامل" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setClearText(!prefs.clearText()); render(); } });
        // shown by default as always; OK hides it (and shows it again)
        row("المناسبة القادمة", prefs.occasion() ? "ظاهرة، كم باقي على رمضان والأعياد" : "مخفية، كم باقي على رمضان والأعياد", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setOccasion(!prefs.occasion()); render(); } });
        row("تعتيم الليل", prefs.nightDim() ? "مفعّل، من ٩ مساءً إلى ٧ الصباح" : "متوقف، نفس الإضاءة طول الليل", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setNightDim(!prefs.nightDim()); render(); } });
        row("المناسبات الوطنية", Prefs.nationalLabel(prefs.national()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickNational(); } });
        // 4.4 the screensaver's design, its centred bar and the weather's place (the owner approved them)
        row("تصميم شاشة التوقف", Prefs.labelOf(Prefs.SAVER_DESIGNS, prefs.design()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickChoice("تصميم شاشة التوقف", Prefs.SAVER_DESIGNS, prefs.design(), v -> prefs.setDesign(v)); } });
        // 4.4 beta 3: Back twice within a second on our home starts the screensaver (off by default, with the screensaver's settings; a single Back is untouched)
        row("رجوع مرتين يفتح شاشة التوقف", prefs.backTwice() ? "مفعّل" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setBackTwice(!prefs.backTwice()); render(); } });
        row("الشريط في الوسط", Prefs.labelOf(Prefs.SAVER_BARS, prefs.bar()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickChoice("الشريط في الوسط", Prefs.SAVER_BARS, prefs.bar(), v -> prefs.setBar(v)); } });
        if (!"none".equals(prefs.bar()))   // where the bar goes: only with a bar
            row("مكان الشريط", Prefs.labelOf(Prefs.BAR_PLACES, prefs.barAt()), R.drawable.ic_eye,
                new Runnable() { public void run() { pickChoice("مكان الشريط", Prefs.BAR_PLACES, prefs.barAt(), v -> prefs.setBarAt(v)); } });
        row("مكان الطقس", Prefs.labelOf(Prefs.WX_PLACES, prefs.wxAt()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickChoice("مكان الطقس", Prefs.WX_PLACES, prefs.wxAt(), v -> prefs.setWxAt(v)); } });
        row("حجم الساعة", Prefs.clockSizeLabel(prefs.clockSize()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickClockSize(); } });
        // «نظام الساعة»: 12 or 24 hours, the same setting as `saver.sh clock24`; the page (screensaver and the home's cards) and the home's clock follow it
        row("نظام الساعة", ClockSetting.label(prefs.clock24()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickClock24(); } });
        // «فرق التاريخ الهجري» (was shell-only: `saver.sh hijrioffset`): where the months start by moon sighting, the Hijri day moved by whole days
        row("فرق التاريخ الهجري", HIJRI_CHOICES[prefs.hijriOffset() + HijriOffset.MAX], R.drawable.ic_clock,
            new Runnable() { public void run() { pickHijri(); } });
        // «إظهار الثواني» (off by default): the seconds on the screensaver's clock, small beside the minutes
        row("إظهار الثواني", prefs.seconds() ? "مفعّل" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setSeconds(!prefs.seconds()); render(); } });
        row("الخانة اليسرى", Prefs.slotLabel(prefs.slot()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickSlot(); } });
        if ("countdown".equals(prefs.slot()) || !prefs.countdownDate().isEmpty())
            row("العدّ التنازلي", countdownLabel(), R.drawable.ic_eye,
                new Runnable() { public void run() { manageCountdown(); } });

        // «الأسواق»: the home's own page (docs/PROPOSALS-NOV2.md §6, §7): TASI, the watchlist, gold and the pegs, and «محفظتي» on this TV only
        header("صفحة الأسواق");
        row("افتح صفحة الأسواق", "تاسي وقائمتك والذهب والعملات، ومحفظتك", R.drawable.ic_trophy,
            new Runnable() { public void run() { openMarkets(); } });

        header("الأسواق (أعلى اليسار)");
        row("شريط الأسواق", prefs.marketsOn() ? "مفعّل" : "مخفي، يبقى طقس اليوم فقط", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setMarketsOn(!prefs.marketsOn()); render(); sendBroadcast(new android.content.Intent(Prefs.CHANGED).setPackage(getPackageName())); } });   // 4.4 beta 4: a screensaver that is up takes it at once (ScreenDream, PagePush)
        row("الأسواق (" + arabicNum(prefs.markets().size()) + "/" + arabicNum(Prefs.MAX_MARKETS) + ")", marketsLabel(),
            R.drawable.ic_trophy, new Runnable() { public void run() { pickMarkets(); } });
        row("عرض الأسواق", Prefs.mkStyleLabel(prefs.mkStyle()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickMkStyle(); } });

        header("بجانب الساعة");
        row("المحتوى", Prefs.panelLabel(prefs.panel()), R.drawable.ic_eye, new Runnable() { public void run() { pickPanel(-1); } });
        // «بطاقة ثانية»: only beside the small clock (there is room for two cards there), and only with a first card (SecondCard)
        if (SecondCard.rowShown(prefs.clockSize(), prefs.panel(), Prefs.SAVER_PANELS))
            row("بطاقة ثانية", Prefs.panelLabel(prefs.panel2(), "بدون"), R.drawable.ic_eye, new Runnable() { public void run() { pickPanel2(); } });
        row("شكل البطاقة", Prefs.cardStyleLabel(prefs.cardStyle()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickCardStyle(); } });
        // 4.4 beta 4 (the owner, «شكل البطاقات»): a size step and the corners, for the home's cards and the screensaver's (the page draws both);
        // in «كنبة» the strip's height is fixed, so the line says «أكبر» is as big as it gets there
        row("حجم البطاقات", Prefs.cardSizeLabel(prefs.cardSize()) + ("couch".equals(prefs.homeLayout()) && "l".equals(prefs.cardSize()) ? "، وفي «كنبة» بقدر الشريط" : ""), R.drawable.ic_eye,
            new Runnable() { public void run() {
                pickOne("حجم البطاقات", Prefs.CARD_SIZE_NAMES, java.util.Arrays.asList(Prefs.CARD_SIZES).indexOf(prefs.cardSize()), k -> prefs.setCardSize(Prefs.CARD_SIZES[k]));
            } });
        row("زوايا البطاقات", Prefs.cardCornerLabel(prefs.cardCorner()), R.drawable.ic_eye,
            new Runnable() { public void run() {
                pickOne("زوايا البطاقات", Prefs.CARD_CORNER_NAMES, java.util.Arrays.asList(Prefs.CARD_CORNERS).indexOf(prefs.cardCorner()), k -> prefs.setCardCorner(Prefs.CARD_CORNERS[k]));
            } });
        // «تبديل الآية والحديث»: only while the verse-or-hadith card is shown somewhere (here, as the second card, or on the home screen)
        if ("verse".equals(prefs.panel()) || "verse".equals(prefs.panel2()) || prefs.homePanels().contains("verse"))
            row("تبديل الآية والحديث", QuoteEvery.label(prefs.quoteEvery()), R.drawable.ic_sync,
                new Runnable() { public void run() { pickQuoteEvery(); } });
        // «الآية والحديث»: both (the default), verses only or hadith only; shown with the row above
        if ("verse".equals(prefs.panel()) || "verse".equals(prefs.panel2()) || prefs.homePanels().contains("verse"))
            row("الآية والحديث", VerseKind.label(prefs.verseKind()), R.drawable.ic_eye,
                new Runnable() { public void run() { pickVerseKind(); } });

        header("الشاشة الرئيسية (تلفزيونك)");
        // «خلّ UrTV هي الشاشة الرئيسية»: not the Home yet, OK opens «إعداد تلفزيوني» (the launcher's, the one place the switch is done); the Home already, it says so
        final boolean isHome = isOurHome();
        row("خلّ UrTV هي الشاشة الرئيسية", isHome ? "✓ UrTV هو الشاشة الرئيسية" : "اضغط وكمّل إعداد تلفزيوني (دقيقتان)", R.drawable.ic_eye,
            new Runnable() { public void run() { if (!isHome) openSetup(); } });
        // «إعداد تلفزيوني» (4.3.1): always there, even when UrTV is already the home, to run «جهّز كل شي» again (after a system update) or «تراجع»
        row("إعداد تلفزيوني", "شغّل الإعداد من جديد، أو رجّع اللي غيّره", R.drawable.ic_eye, new Runnable() { public void run() { openSetup(); } });
        // «تخلّص من واجهة المصنع»: who the TV's home screen is (the launcher's FactoryActivity: the checks, two confirmations, the PIN); «رجّع واجهة المصنع» while it is on
        homeCleanSeen();
        final boolean factoryOn = Boolean.TRUE.equals(factorySeen);
        final Runnable factoryGo = new Runnable() { public void run() { openFactory(); } };
        if (factoryOn) row("رجّع واجهة المصنع", "مفعّل واجهة المصنع معطّلة، وبضغطة ترجع", R.drawable.ic_eye, factoryGo);
        else row("تخلّص من واجهة المصنع", "عطّل واجهة المصنع وخلّ UrTV هو الأساس، وبضغطة ترجع", R.drawable.ic_eye, factoryGo);
        // 4.3.2 «قبل ما تحذف التطبيق…»: «تخلّص من واجهة المصنع» turns the TV's own home off; deleting UrTV after it left a black screen (no home at all). The row says so and
        // where the undo is, and opens «رجّع واجهة المصنع» from its own button.
        row("قبل ما تحذف التطبيق…", factoryOn ? "واجهة المصنع معطّلة الحين: ارجعها قبل ما تحذف UrTV" : "اقرأ هذا قبل ما تحذف UrTV من التلفزيون", R.drawable.ic_eye,
            new Runnable() { public void run() { uninstallWarning(factoryOn); } });
        // «UrTV النظيف»: the essentials only; the launcher's CleanActivity says what it does first and asks for the confirmation (and the family PIN)
        // the line says «مفعّل» (the on/off rule: the value starts with the state word) while the mask is on; the home answers (read once per visit, the last answer kept)
        row("\u2066UrTV\u2069 النظيف", Boolean.TRUE.equals(homeCleanSeen()) ? "مفعّل، الأساسيات بس، وبضغطة ترجع الشاشة كاملة" : "الأساسيات فقط، وبضغطة ترجع الشاشة الكاملة", R.drawable.ic_eye, new Runnable() { public void run() { openClean(); } });
        // «المظهر»: one page for everything about the look of both apps (AppearanceActivity), each choice with its picture
        // 4.3.3 «الواجهة»: the home as it was (the default), the football home, or «الآفاق» (a scene with the glass clock behind its scenery); and the scene packs
        // 4.4 the owner: one switch; on applies the Couch preset and keeps the person's own values, off puts them back (Prefs.setCouch)
        // 4.4 the owner: «أشكال جاهزة», six looks at once (Prefs.setPreset); with PRESETS_REPLACE_COUCH it takes the place of «شكل كنبة»
        // 4.5 «المظهر» over the live home (LookPanelActivity): the looks and the parts, each shown on the home under the focus, kept with OK
        if (prefs.uiPanel()) row("المظهر على الرئيسية", "الأشكال الجاهزة والتخطيط والخلفية والساعة وشكل التطبيقات، تراها على الرئيسية قبل ما تختار", R.drawable.ic_eye,
            new Runnable() { public void run() { LookPanelActivity.open(SettingsActivity.this); } });
        // 4.5 «رجّع الشكل السابق» (the owner approved it): the first row after a change of the look, the look before by name (its time is in the panel's row); pressed again, it comes back
        if (prefs.uiUndo() && prefs.hasPreviousLook()) row("رجّع الشكل السابق", prefs.previousLookName(), R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.restorePreviousLook(); render(); } });
        row("أشكال جاهزة", prefs.presetLabel(), R.drawable.ic_eye, new Runnable() { public void run() { pickPreset(); } });
        // 4.4 «ابدأ بـ» and «أجزاء UrTV» (the owner, docs/mockups/start-presets/): the starts again, and each part on or off
        row("ابدأ من جديد", startLabel(), R.drawable.ic_eye, new Runnable() { public void run() { startActivity(new Intent(SettingsActivity.this, StartActivity.class)); } });
        row("أجزاء UrTV", partsLabel(), R.drawable.ic_eye, new Runnable() { public void run() { pickParts(); } });
        // 4.4 beta 2 «شكل شاشة الإعدادات» (the owner): the settings' own style, in both screens at the same place, so one can always go back
        row("شكل شاشة الإعدادات", Prefs.settingsStyleLabel(prefs.settingsStyle()), R.drawable.ic_eye, new Runnable() { public void run() {
            pickOne("شكل شاشة الإعدادات", Prefs.SETTINGS_STYLE_NAMES, java.util.Arrays.asList(Prefs.SETTINGS_STYLES).indexOf(prefs.settingsStyle()), i -> {
                if (Prefs.SETTINGS_STYLES[i].equals(prefs.settingsStyle())) return;
                prefs.setSettingsStyle(Prefs.SETTINGS_STYLES[i]);
                startActivity(new Intent(SettingsActivity.this, SettingsActivity.class).putExtra("screen", "look").putExtra("hubAt", "look").putExtra("want", "شكل شاشة الإعدادات")
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));   // the other style now, the focus on this row
                finish();
            }); } });
        // 4.4 beta 2 (the owner approved docs/mockups/full-look 05): «تخصيص», the look's parts gathered under one row (each still its own row
        // below, so the search and a link reach it as before); a part changed since the look was chosen says «غيّرتها»
        row("تخصيص", customLine(), R.drawable.ic_eye, new Runnable() { public void run() { pickCustom(); } });
        // 4.4 beta 2 (the owner, docs/mockups/full-look 06): after «… بدون بطاقات», the hidden cards come back from here, fitted to the look
        if (!prefs.hiddenCards().isEmpty()) row("رجّع بطاقاتك", "بطاقاتك قبل التصميم، بنفس ترتيبها", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.restoreHiddenCards(); String n = prefs.takeFitNote(); render(); if (n != null) notice(n); } });
        if (!Prefs.PRESETS_REPLACE_COUCH) row("شكل كنبة", prefs.couchLabel(), R.drawable.ic_eye, new Runnable() { public void run() { flipCouch(); } });
        row("التخطيط", Prefs.homeLayoutLabel(prefs.homeLayout()), R.drawable.ic_eye, new Runnable() { public void run() { pickHomeLayout(); } });
        row("الخلفية", Prefs.homeBgLabel(prefs.homeBg()), R.drawable.ic_eye, new Runnable() { public void run() {
            pickOne("الخلفية", Prefs.HOME_BG_NAMES, java.util.Arrays.asList(Prefs.HOME_BGS).indexOf(prefs.homeBg()), i -> prefs.setLookPart("bg", Prefs.HOME_BGS[i])); } });
        row("الساعة", Prefs.homeClockLabel(prefs.homeClock()), R.drawable.ic_eye, new Runnable() { public void run() {
            pickOne("الساعة", Prefs.HOME_CLOCK_NAMES, java.util.Arrays.asList(Prefs.HOME_CLOCKS).indexOf(prefs.homeClock()), i -> prefs.setLookPart("clock", Prefs.HOME_CLOCKS[i])); } });
        row("شكل التطبيقات", Prefs.appsStyleLabel(prefs.appsStyle()), R.drawable.ic_eye, new Runnable() { public void run() {
            pickOne("شكل التطبيقات", Prefs.HOME_APPS_NAMES, java.util.Arrays.asList(Prefs.HOME_APPS).indexOf(Prefs.appsWay(prefs.appsStyle())), i -> prefs.setLookPart("apps", Prefs.HOME_APPS[i])); } });
        // 4.4 beta 2 (the owner): «محاذاة التطبيقات», the apps row in the middle (the default), right or left, in every look; the clock never moves
        row("محاذاة التطبيقات", Prefs.appsAlignLabel(prefs.appsAlign()), R.drawable.ic_eye, new Runnable() { public void run() {
            pickOne("محاذاة التطبيقات", Prefs.APPS_ALIGN_NAMES, java.util.Arrays.asList(Prefs.APPS_ALIGNS).indexOf(prefs.appsAlign()), i -> { prefs.setAppsAlign(Prefs.APPS_ALIGNS[i]); prefs.lookChanged(); }); } });
        // 4.5 «زجاجي»: its size and rows (the two rows always here, so the way can be set up before it is chosen)
        row("حجم شريط tvOS", Prefs.glassSizeLabel(prefs.glassSize()), R.drawable.ic_eye, new Runnable() { public void run() {
            String[] names = new String[Prefs.GLASS_SIZES.length];
            for (int k = 0; k < names.length; k++) names[k] = Prefs.glassSizeLabel(Prefs.GLASS_SIZES[k]);
            pickOne("حجم شريط tvOS", names, java.util.Arrays.asList(Prefs.GLASS_SIZES).indexOf(prefs.glassSize()), i -> prefs.setGlassSize(Prefs.GLASS_SIZES[i])); } });
        row("صفوف شريط tvOS", Prefs.glassRowsLabel(prefs.glassRows()), R.drawable.ic_eye, new Runnable() { public void run() {
            pickOne("صفوف شريط tvOS", Prefs.GLASS_ROWS_NAMES, java.util.Arrays.asList(Prefs.GLASS_ROWS).indexOf(prefs.glassRows()), i -> prefs.setGlassRows(Prefs.GLASS_ROWS[i])); } });
        row("حزم الآفاق", labels(Prefs.SCENE_PACKS, prefs.scenePacks(), "آفاق السعودية"), R.drawable.ic_eye,
            new Runnable() { public void run() { pickMany("scenePacks", "حزم الآفاق", Prefs.SCENE_PACKS, "بدون اختيار = آفاق السعودية"); } });
        row("بقاء كل أفق", Prefs.sceneStayLabel(prefs.sceneStay()), R.drawable.ic_eye, new Runnable() { public void run() { pickSceneStay(); } });
        row("الأفق يتبع الوقت", prefs.sceneByTime() ? "مفعّل (أفق الليل من المغرب إلى الفجر)" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setSceneByTime(!prefs.sceneByTime()); render(); } });
        row("الأفق يتبع الطقس", prefs.sceneByWeather() ? "مفعّل (أفق الضباب وقت الضباب)" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setSceneByWeather(!prefs.sceneByWeather()); render(); } });
        row("الألوان والإضاءة", "الشكل: " + Prefs.lookLabel(prefs.look()) + "، والإضاءة: " + Prefs.appearanceLabel(prefs.appearance()), R.drawable.ic_eye,
            new Runnable() { public void run() { startActivity(new Intent(SettingsActivity.this, AppearanceActivity.class)); } });
        row("صفحات الأسهم والمباريات داكنة", prefs.pagesDark() ? "مفعّل، بالمظهر الداكن دائمًا" : "متوقف، تتبع مظهر الرئيسية", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPagesDark(!prefs.pagesDark()); render(); } });
        // 4.4 beta 2 (the owner, «with a protective way»): the new look items, each its own switch, off by default and after an update
        row("لوحة المظهر على الرئيسية", prefs.uiPanel() ? "مفعّل، تشوف الشكل على الرئيسية قبل ما تختار" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setUiPanel(!prefs.uiPanel()); render(); } });
        row("زر رجّع الشكل السابق", prefs.uiUndo() ? "مفعّل، أول صف بعد تغيير الشكل" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setUiUndo(!prefs.uiUndo()); render(); } });
        row("تبديل الشكل بلا وميض", prefs.uiSmooth() ? "مفعّل، الرئيسية تتغير مرة وحدة بتلاشٍ قصير" : "متوقف، كما كان", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setUiSmooth(!prefs.uiSmooth()); render(); } });
        // up to three cards left of the clock; each place takes any card, or «بدون» (the ones after it move up)
        for (int k = 0; k < Prefs.HOME_CARDS; k++) {
            final int place = k;
            row(HOME_CARD_NAMES[k], Prefs.panelLabel(prefs.homePanelAt(k), "بدون"), R.drawable.ic_eye,
                new Runnable() { public void run() { pickPanel(place); } });
        }
        row("التحية والطقس", prefs.greeting() ? "مفعّل" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setGreeting(!prefs.greeting()); render(); } });
        // PR #45 «اسمك في التحية»: a name after the greeting, empty (the default) is the greeting alone
        row("اسمك في التحية", prefs.greetingName().isEmpty() ? "بدون اسم" : prefs.greetingName(), R.drawable.ic_eye,
            new Runnable() { public void run() { askGreetingName(); } });
        // the adhan's settings (on or off, the voice, the Fajr voice, short, any key stops it, the volume): the same screen as the player's adhan tab,
        // opened through the home (behind its family lock), so they are reachable with the «تلاوة وأذان» button hidden too
        row("الأذان", "تشغيله، والصوت، والإيقاف بأي زر", R.drawable.ic_settings,
            new Runnable() { public void run() { openAdhanSettings(); } });
        // 4.4 beta 4: the sound and the screen of each of the five prayers apart (the home's screen, behind the same lock)
        row("الأذان لكل صلاة", prayersValue(), R.drawable.ic_settings,
            new Runnable() { public void run() { openHome("prayers"); } });
        // 4.4 beta 4 (the UI session's tree): the three screens of the تلاوة app that were only reached through «الأذان»: the muezzin, the duas and reminders, and my sounds
        row("المؤذن", homeValue("adhan_voice", "name", "اختر صوت المؤذن"), R.drawable.ic_settings,
            new Runnable() { public void run() { openHome("voice"); } });
        // 4.4 beta 4 (the emulator's count of the findability review: as «المؤذن»'s last choice it was ~37 presses): one's own adhan, a row of its own
        row("أذاني من ملف", "من فلاشة أو التنزيلات أو الجوال، ويصير هو الأذان", R.drawable.ic_settings,
            new Runnable() { public void run() { openHome("ownadhan"); } });
        row("الأدعية والتذكيرات", duasValue(), R.drawable.ic_settings,
            new Runnable() { public void run() { openHome("duas"); } });
        row("صوتياتي", "أدعيتك وأذاني من ملفاتك", R.drawable.ic_settings,
            new Runnable() { public void run() { openHome("sounds"); } });
        // «تلاوة وأذان» itself, through the home (behind its family lock), as its button there opens it
        row("افتح تلاوة وأذان", "القرآن والإذاعات ومواقيت الأذان", R.drawable.ic_play,
            new Runnable() { public void run() { openHome("listen"); } });
        // hiding removes only the button; the help line says where the screen opens from then
        row("زر تلاوة وأذان", prefs.listenPill() ? "ظاهر" : "مخفي، الأذان والتلاوة تعمل، وتفتح الشاشة من «افتح تلاوة وأذان» هنا أو من بطاقة «يُتلى الآن»", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setListenPill(!prefs.listenPill()); render(); } });
        // #52: the other quick buttons of the home can be hidden too; a hidden one only leaves the row (its page opens from elsewhere: the markets and games by the phone, the Control Center by the Quick Settings key)
        row("زر المصادر", prefs.pillHidden("inputs") ? "مخفي، والمصادر تنفتح من زرّها في الريموت ومن مركز التحكم" : "ظاهر", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPillHidden("inputs", !prefs.pillHidden("inputs")); render(); } });
        row("زر الأسواق", prefs.pillHidden("markets") ? "مخفي" : "ظاهر", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPillHidden("markets", !prefs.pillHidden("markets")); render(); } });
        row("زر المباريات", prefs.pillHidden("matches") ? "مخفي" : "ظاهر", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPillHidden("matches", !prefs.pillHidden("matches")); render(); } });
        row("زر الجوال", prefs.pillHidden("phone") ? "مخفي" : "ظاهر", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPillHidden("phone", !prefs.pillHidden("phone")); render(); } });
        // 4.3.3 «زر البحث»: the TV's own search inside the apps, a round button before «الجوال»; off by default, and only on a TV that has that search
        row("زر البحث", prefs.searchPill() ? "ظاهر" : "مخفي", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setSearchPill(!prefs.searchPill()); render(); } });
        row("زر مركز التحكم", prefs.pillHidden("center") ? "مخفي، يفتح من زر الإعدادات السريعة في الريموت" : "ظاهر", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPillHidden("center", !prefs.pillHidden("center")); render(); } });
        // hiding removes only the button: the list stays in the «المهام» card and on the phone
        row("زر المهام", prefs.tasksPill() ? "ظاهر" : "مخفي، المهام تبقى في بطاقة «المهام» ومن الجوال", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setTasksPill(!prefs.tasksPill()); render(); } });
        // Back on our home (nothing to go back to) starts the screensaver; the home takes it on its next tick or return (Dock.followBackSaver)
        row("الرجوع يفتح شاشة التوقف", prefs.backSaver() ? "مفعّل" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setBackSaver(!prefs.backSaver()); render(); } });
        // 4.4 (build 413, T9): «إضافات تلفزيونك» catches the remote's Home key and opens this home directly (Google TV's home never starts); on by
        // default on a Google TV. Shown only when the add-on answers (installed and on)
        final Boolean homeKey = homeKeySeen();   // 4.4 (QA-7, R1): the home's last answer; asked on a thread, never at a render
        if (homeKey != null)
            row("زر الرئيسية يفتح تلفزيونك مباشرة", homeKey ? "مفعّل" : "متوقف", R.drawable.ic_eye,
                new Runnable() { public void run() { setHomeKey(!homeKey); } });
        // 4.3.3 «المباريات المنتهية»: on the football home only; hidden, today's list keeps the games still to come (or the next day's)
        if ("football".equals(prefs.homeLayout()))
            row("إخفاء المباريات المنتهية", prefs.fbHideDone() ? "مفعّل" : "متوقف", R.drawable.ic_eye,
                new Runnable() { public void run() { prefs.setFbHideDone(!prefs.fbHideDone()); render(); } });
        // «أزرار الريموت»: which app the remote's YouTube, Netflix and Prime Video keys open (the home follows it: Dock.followKeyApps)
        // only the buttons this TV's remote was read to send (RemoteButtons): a brand nobody has read gets no row
        for (final RemoteButtons.Button k : RemoteButtons.buttons(Prefs.remoteTv())) {
            row(k.title, keyAppLabel(k.id), R.drawable.ic_play, new Runnable() { public void run() { pickKeyApp(k.id, k.title); } });
        }
        // «الخروج من تلفزيونك»: only where the TV keeps its own home (ours opened from its tile); one Back as always, a long Back or three
        row("الخروج من تلفزيونك", HomeExit.label(prefs.homeExit()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickHomeExit(); } });
        // the bell shows only while the Play Store has updates waiting (the home screen asks on return, at most every 6 hours)
        row("جرس التحديثات", prefs.updateBell() ? "ظاهر عند وجود تحديثات للتطبيقات" : "مخفي", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setUpdateBell(!prefs.updateBell()); render(); } });
        row("تطبيقات في الشريط", Prefs.favBarLabel(prefs.favBar()), R.drawable.ic_star,
            new Runnable() { public void run() { pickFavBar(); } });
        row("ترتيب التطبيقات المفضلة", "قائمة ترتّب فيها المفضلة بالأسهم", R.drawable.ic_star,
            new Runnable() { public void run() { openDockSettings(); } });
        // 4.3 «تطبيقات مقترحة»: SmartTube, TV Bro and VLC, each with why and its install (the home app's screen)
        row("تطبيقات مقترحة", "تطبيقات نرشّحها، وتثبّتها من هنا", R.drawable.ic_star,
            new Runnable() { public void run() { openSuggested(); } });
        // «كمّل المشاهدة»: on or off; turned on while the home app may not read the TV's list yet, it opens the home app's one question
        row("كمّل المشاهدة", watchNextLabel(), R.drawable.ic_play,
            new Runnable() { public void run() {
                if (prefs.watchNext() && !homeMayReadWatchNext()) { openWatchNextPermission(); return; }   // on, not allowed yet: the question
                prefs.setWatchNext(!prefs.watchNext());
                if (prefs.watchNext() && !homeMayReadWatchNext()) openWatchNextPermission();
                render(); } });
        row("أسماء المصادر", "غيّر اسم كل مصدر، مثل PS5", R.drawable.ic_settings,
            new Runnable() { public void run() { openInputNames(); } });
        row("تعديل المفضلة", "تحريك التطبيقات على الشاشة الرئيسية وحذفها", R.drawable.ic_star,
            new Runnable() { public void run() { openDockEdit(); } });
        // first in «الجوال والتحديث» (TV6 row 21: a tester did not find it, and phone control stayed on). This app cannot see the home's
        // switch, so the line never says «متوقف»: the screen it opens shows the real state, with «إيقاف التحكم من الجوال» on it
        row("الجوالات المسموح لها", "ربط جوال والتحكم منه", R.drawable.ic_settings,
            new Runnable() { public void run() { openPhoneControl(); } });
        // «كل التطبيقات»: the home screen's «تعديل التطبيقات», or the order by name again (what is hidden stays hidden)
        row("ترتيب كل التطبيقات", "تحريكها وإخفاؤها وإرجاع المخفية، أو الرجوع للترتيب الأصلي", R.drawable.ic_star,
            new Runnable() { public void run() { pickAppsOrder(); } });
        // 4.4 beta 4: the row the hide message names (the findability review: it did not exist; the way was five screens deep)
        row("التطبيقات المخفية", "أرجع تطبيقًا أخفيته", R.drawable.ic_star,
            new Runnable() { public void run() { openAppsOrder(false, true); } });
        // what an app's options box does (a long OK or Menu on the app, docs/UX-REVIEW.md) is here too: move, hide and the favourites
        // above, the locks and the app's details here
        row("قفل التطبيقات", "اختر التطبيقات التي تطلب الرمز", R.drawable.ic_lock,
            new Runnable() { public void run() { openParental(); } });
        row("تفاصيل التطبيقات", "قائمة التلفزيون: التخزين والأذونات والحذف", R.drawable.ic_settings,
            new Runnable() { public void run() { openAppDetails(); } });
        row("تحديث تلفزيوني", "تحديث التطبيقين: يعرض الإصدار ويتحقق من التحديث", R.drawable.ic_sync,
            new Runnable() { public void run() { openUpdate(); } });
        // two buttons of the first screen that do one thing (SettingsHub.direct): the home's «مركز التحكم», and the report's QR code
        row("مركز التحكم", "مؤقت النوم والشبكة والصوت وإعدادات التلفزيون", R.drawable.ic_settings,
            new Runnable() { public void run() { openHome("center"); } });
        // the first screen's last button (SettingsHub.SEARCH): type a word, pick the row, land on it in its section
        row("بحث في الإعدادات", "اكتب كلمة، مثل: صوت، لون، مدينة", R.drawable.ic_search, new Runnable() { public void run() { searchSettings(); } });
        // 4.3.3 «مسح الملفات المؤقتة»: the two apps' caches only (CacheSweep): never the settings, your photos, the packs or the adhan's sounds
        row("مسح الملفات المؤقتة", "الصور والبيانات المؤقتة للتطبيقين؛ الإعدادات وصورك والحزم تبقى", R.drawable.ic_sync,
            new Runnable() { public void run() { clearCaches(); } });
        row("أرسل تقرير", "رمز QR يفتح صفحة الإرسال في جوالك", R.drawable.ic_settings,
            new Runnable() { public void run() { openReport(); } });
        // 4.4 «مصادر البيانات»: where the data comes from, once, here (Sources); on the cards a source is a tiny dim line at most
        row("مصادر البيانات", "من وين تجي البيانات، وترخيص كل مصدر", R.drawable.ic_report, new Runnable() { public void run() { showSources(); } });

        // «الرقابة الأبوية» (docs/UX-REVIEW.md): the home's own page (the PIN, the locked apps, kids mode); it asks for the PIN itself
        row("الرقابة الأبوية", "رمز يقفل التطبيقات والإعدادات، ووضع الأطفال", R.drawable.ic_lock,
            new Runnable() { public void run() { openParental(); } });
        row("تصفية المواقع", "تفعيل أو إيقاف، ويرجع الإعداد القديم تلقائياً", R.drawable.ic_eye,
            new Runnable() { public void run() { openFamilyDns(); } });
        // «اللغة» (docs/ENGLISH.md): العربية, the default, or English; a button of «المزيد» that opens its two choices at once
        row("اللغة", langLabel(), R.drawable.ic_language, new Runnable() { public void run() { pickLang(); } });

        if (on(SettingsHub.of("التصنيفات", null)) || "shows".equals(prefs.panel()) || prefs.homePanels().contains("shows")) {   // only matter for the shows card; always on its own screen
        header("جديد المسلسلات");
        row("التصنيفات", labels(Prefs.GENRES, prefs.list("genres"), "الكل"),
            R.drawable.ic_category, new Runnable() { public void run() { pickMany("genres", "التصنيفات", Prefs.GENRES); } });
        row("اللغات", labels(Prefs.LANGS, prefs.list("langs"), "الكل"),
            R.drawable.ic_language, new Runnable() { public void run() { pickMany("langs", "اللغات", Prefs.LANGS); } });
        row("مسلسلات مختارة", showsLabel(), R.drawable.ic_star, new Runnable() { public void run() { manageShows(); } });
        row("أقل تقييم", "0".equals(prefs.minRating()) ? "بدون حد" : prefs.minRating(),
            R.drawable.ic_star, new Runnable() { public void run() { pickRating(); } });
        }

        if ("stocks".equals(prefs.panel()) || prefs.homePanels().contains("stocks")) {   // only matters for the stocks card
        header("الأسهم");
        row("قائمة الأسهم (" + arabicNum(prefs.stocks().size()) + "/" + arabicNum(Prefs.MAX_STOCKS) + ")", stocksLabel(),
            R.drawable.ic_star, new Runnable() { public void run() { askStocks(); } });
        row("تغيّر الأسهم", labelOf(Prefs.STK_CHG, prefs.stkChg()), R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setStkChg("amount".equals(prefs.stkChg()) ? "pct" : "amount"); render(); } });
        }

        header("التوقيت");
        row("تبديل الصورة كل", PhotoInterval.label(prefs.photoMinutes()), R.drawable.ic_sync,
            new Runnable() { public void run() { pickPhotoMinutes(); } });
        row("صور شاشة التوقف", packsLabel(), R.drawable.ic_eye,
            new Runnable() { public void run() { startActivity(new Intent(SettingsActivity.this, PhotoPacksActivity.class)); } });
        // 4.4 beta 4 (the findability review: deleting a photo took ~41 presses, adding one was never said): «صوري والخلفيات» opens with them
        row("أضف صوري من الجوال", "من صفحة الجوال: «صوري»، تضيف وتحذف", R.drawable.ic_phone,
            new Runnable() { public void run() { openPhoneControl(); } });
        row("احذف صورة", Own.count(this) == 0 ? null : "اختر من صورك", R.drawable.ic_eye,
            new Runnable() { public void run() { if (Own.count(SettingsActivity.this) == 0) toast("ما فيه صور بعد: أضفها من الجوال"); else pickOwnToDelete(); } });
        row("صوري (" + arabicNum(Own.count(this)) + "/" + arabicNum(Own.max(this)) + ")", ownLabel(), R.drawable.ic_eye,
            new Runnable() { public void run() { pickOwn(); } });
        // 4.4 beta 4: the photos of a USB stick or of Downloads, picked in the home (which holds the read permission; this app has none) and handed to «صوري»
        row("أضف من فلاشة أو التنزيلات", "صور من فلاشة USB أو من التنزيلات تُضاف إلى صوري", R.drawable.ic_eye,
            new Runnable() { public void run() { openHome("photos"); } });
        row("بدء شاشة التوقف بعد", idleLabel(), R.drawable.ic_screensaver,
            new Runnable() { public void run() { pickIdle(); } });
        if (prefs.overlayNeeded())   // 4.4 #42 (R-473): only on a TV that needs it
            row("إذن شاشة التوقف", OverlayNeed.WHY, R.drawable.ic_screensaver, new Runnable() { public void run() { askSaverOverlay(); } });

        header("التطبيق");
        View autoRow = row("التحديث التلقائي للصفحة", prefs.autoUpdate() ? "مفعّل، كل ٦ ساعات" : "متوقف", R.drawable.ic_sync, new Runnable() { public void run() {
            prefs.setAutoUpdate(!prefs.autoUpdate()); render(); } });
        // a switch that flips with one OK sat right under the «تحديث تلفزيوني» action (the tester flipped it by mistake): a clear gap of 32 above it
        if (autoRow != null && autoRow.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) ((android.view.ViewGroup.MarginLayoutParams) autoRow.getLayoutParams()).topMargin += dp(32);
        // the page only, never the apps: people took it for the app update (rows 421, 441), so its line points to «تحديث تلفزيوني»
        View last = row("تحديث صفحة شاشة التوقف", "ينزّل أحدث صفحة للشاشة فقط، لتحديث التطبيقين: «تحديث تلفزيوني» في نفس القسم", R.drawable.ic_sync, new Runnable() { public void run() { updateNow(); } });
        if (last.getParent() == list) { TextView ver = note(last, Updater.versionLines(this)); ver.setFocusable(true); ver.setClickable(true); ver.setOnClickListener(v -> versionTap()); }
        // «النسخ التجريبية» (4.3.3): the beta release first, off by default. Off again never downgrades: the newer build stays until the normal one reaches its number
        row("النسخ التجريبية", prefs.betaOn() ? "مفعّل، تجيك التحديثات قبل الكل، وفيها أخطاء أحيانًا" : "متوقف، تحديثات قبل الكل، وفيها أخطاء أحيانًا", R.drawable.ic_sync, new Runnable() { public void run() { toggleBeta(); } });
        // only while it is on: the report screen (its QR code carries «channel: beta» and the versions), the person types their words on the phone page
        if (prefs.betaOn()) row("أرسل ملاحظة عن النسخة التجريبية", "رمز QR يفتح صفحة الإرسال في جوالك، وتكتب ملاحظتك هناك", R.drawable.ic_settings,
            new Runnable() { public void run() { openReport(); } });
        arrange();   // the rows in their section's order, with its headers
        if (tiles) { hub(want, wasIndex); return; }   // 4.3.2: a screen of buttons, after the rows were made (none of them is shown on it)
        if (sideFocus) {   // the focus stays on (or goes to) the section's item in the side list: the rows are only shown
            sideFocus = false;
            final View item = sideItems.get(screen);
            if (item != null && !item.hasFocus()) item.post(item::requestFocus);
        } else restoreFocus(want, wasIndex, scrollY);   // a section just opened: its first row (or the row last focused in it)
    }

    /**
     * «بحث»: a word typed on the TV's keyboard (or from the phone page, which types into the field in front: «على الشاشة»), the rows that answer it
     * (SettingsHub.search), and the choice opens the row's section with the focus on that row.
     */
    /**
     * «بحث في الإعدادات»: the rows found as the words are typed (SettingsHub.search is in memory: a list, no file, no network), shown once the
     * typing rests SEARCH_MS; OK on one opens its section on that row. «بحث» still shows the list of every row found.
     */
    private void searchSettings() {
        if (cs != null) { openSearch(); return; }
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText in = new EditText(this);
        in.setHint(Words.of("اكتب كلمة، مثل: صوت، لون، مدينة"));
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        box.addView(in);
        final android.widget.ListView found = new android.widget.ListView(this);
        found.setDividerHeight(0);
        found.setSelector(GlassPaint.focused(this, look, 12));
        final List<String[]> rows = new ArrayList<>();
        final android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override public View getView(int i, View old, android.view.ViewGroup parent) {
                TextView t = (TextView) super.getView(i, old, parent);   // recycled: the list makes only the lines it shows
                t.setTextColor(focusColors(look.text, look.onFocus));
                t.setTypeface(font("Thmanyah-Regular"));
                return t;
            }
        };
        found.setAdapter(adapter);
        box.addView(found, new LinearLayout.LayoutParams(-1, dp(260)));
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable look_ = () -> {
            rows.clear();
            rows.addAll(Parts.visible(SettingsHub.search(in.getText().toString().trim()), prefs.partsOff()));
            adapter.clear();
            for (int i = 0; i < Math.min(rows.size(), 12); i++) adapter.add(conv(rows.get(i)[0]) + S.sep() + conv(SettingsHub.name(rows.get(i)[1])));
        };
        in.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) { h.removeCallbacks(look_); h.postDelayed(look_, SEARCH_MS); }
        });
        final AlertDialog dialog = new Dlg(this).setTitle("بحث في الإعدادات").setView(box)
            .setPositiveButton("بحث", (d, w) -> showFound(in.getText().toString().trim()))
            .setNegativeButton("إلغاء", null).create();
        found.setOnItemClickListener((p, v, i, id) -> {
            dialog.dismiss();
            hubAt = rows.get(i)[1];
            forceWant = rows.get(i)[0];
            open(rows.get(i)[1]);
        });
        dialog.setOnDismissListener(d -> h.removeCallbacks(look_));
        dialog.show();
        in.requestFocus();
    }
    static final long SEARCH_MS = 120;

    /** 4.4 «مصادر البيانات»: each source and what it gives; OK on one says its terms, and «الإعداد» lands on the row of what it gives */
    private void showSources() {
        String[] labels = new String[Sources.LIST.length];
        for (int i = 0; i < labels.length; i++) labels[i] = conv(Sources.LIST[i][0]) + "\n" + conv(Sources.LIST[i][1]);
        new Dlg(this).setTitle("مصادر البيانات").setItems(labels, (d, i) -> { d.dismiss(); showSource(Sources.LIST[i]); })
            .setNegativeButton("رجوع", null).show();
    }

    private void showSource(final String[] src) {
        new Dlg(this).setTitle(conv(src[0])).setMessage(conv(src[1]) + "\n\n" + conv(src[2]))
            .setPositiveButton("الإعداد", (d, w) -> { hubAt = SettingsHub.of(src[3], null); forceWant = src[3]; open(hubAt); })
            .setNegativeButton("رجوع", (d, w) -> showSources()).show();
    }

    private void showFound(String query) {
        final List<String[]> found = Parts.visible(SettingsHub.search(query), prefs.partsOff());
        if (found.isEmpty()) { toast("ما فيه صف بهذا الاسم"); return; }
        int n = Math.min(found.size(), 12);
        String[] labels = new String[n];
        for (int i = 0; i < n; i++) labels[i] = conv(found.get(i)[0]) + S.sep() + conv(SettingsHub.name(found.get(i)[1]));
        new Dlg(this).setTitle("بحث في الإعدادات").setItems(labels, (d, i) -> {
            d.dismiss();
            hubAt = found.get(i)[1];
            forceWant = found.get(i)[0];
            open(found.get(i)[1]);
        }).show();
    }


    // ---- 4.4: the 4.3.2 screen (classic), as it was on master

    /** 4.3.2's head: «تلفزيونك» and the screen's name over a screen of buttons; a section's title and line are on the side */
    private void classicHead(boolean tiles) {
        if (ts != null) { tvosHead(); return; }   // «tvOS»: the screen's name over the rows, the icon on the side, on every screen
        classicSide();
        if (!tiles) return;
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, 0, 0, dp(16));
        TextView title = new TextView(this);
        title.setText(Words.of(screen == null ? "الإعدادات" : SettingsHub.name(screen)));   // the audit: one title (it was «تلفزيونك» over «الإعدادات»)
        title.setTextColor(look.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        head.addView(title);
        list.addView(head);
    }

    /**
     * «شكل كنبة» flipped here: the home's look changes with it (Prefs.setCouch), and so does this screen: the settings open again in the other layout,
     * on its look section with the focus on the switch.
     */
    /**
     * «أشكال جاهزة»: the six looks, each with its picture (assets/presets/<key>.jpg, the home with its screensaver small in a corner) and what it
     * sets; OK applies it (Prefs.setPreset). When the look changes the settings screen's layout (the Couch look's side list), the settings open again
     * in the other layout on this row, as «شكل كنبة» did.
     */
    private void pickPreset() {
        // 4.4 beta 2 (the owner approved docs/mockups/full-look 01): two parts, each under its plain heading: «تصميم كامل» (the looks that change
        // everything, the settings screen too: Prefs.fullDesign) and «أشكال بسيطة» (the home only), then «تركيبات مقترحة» (one tile that opens
        // the four mixes when the focus is on it). A heading is not a choice: the focus passes over it.
        final List<Integer> order = new ArrayList<>();   // an index of Prefs.PRESETS, HEAD_FULL / HEAD_SIMPLE for a heading, MIX for the mixes' tile
        order.add(HEAD_FULL);
        for (int i = 0; i < Prefs.PRESETS.length; i++) if (Prefs.fullDesign(Prefs.PRESETS[i])) order.add(i);
        order.add(HEAD_SIMPLE);
        for (int i = 0; i < Prefs.PRESETS.length; i++) if (!Prefs.fullDesign(Prefs.PRESETS[i])) order.add(i);
        order.add(MIX);
        final int n = order.size(), mixAt = n - 1;
        final String[] names = new String[n], about = new String[n], pics = new String[n];
        final String[] keys = new String[n];
        for (int k = 0; k < n; k++) {
            int i = order.get(k);
            if (i == HEAD_FULL) { names[k] = "تصميم كامل"; about[k] = "الرئيسية والتطبيقات والساعة والخلفية وشاشة الإعدادات"; continue; }
            if (i == HEAD_SIMPLE) { names[k] = "أشكال بسيطة"; about[k] = "الرئيسية فقط، والإعدادات كما هي"; continue; }
            if (i == MIX) { names[k] = "تركيبات مقترحة"; about[k] = "شكل من الستة مع تغيير أو اثنين"; pics[k] = "combos"; continue; }
            names[k] = Prefs.PRESET_NAMES[i]; about[k] = Prefs.PRESET_ABOUT[i]; pics[k] = keys[k] = Prefs.PRESETS[i];
            if (Prefs.PRESETS[i].equals(prefs.preset()) && prefs.presetCombo().isEmpty()) about[k] = String.format(java.util.Locale.ROOT, "الحالي، %s", about[k]);   // the owner: the one on now says so
        }
        final FrameLayout frame = new FrameLayout(this);
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setLayoutDirection(Dir.layout());
        box.setPadding(dp(12), dp(4), dp(12), dp(4));
        frame.addView(box, new FrameLayout.LayoutParams(-1, -2));
        final android.widget.ListView lv = new android.widget.ListView(this);
        lv.setDividerHeight(0);
        lv.setSelector(GlassPaint.focused(this, look, 12));
        lv.setAdapter(new android.widget.BaseAdapter() {
            @Override public int getCount() { return names.length; }
            @Override public Object getItem(int i) { return pics[i]; }
            @Override public long getItemId(int i) { return i; }
            @Override public boolean areAllItemsEnabled() { return false; }
            @Override public boolean isEnabled(int i) { return pics[i] != null; }   // a heading is not a choice
            @Override public View getView(int i, View old, android.view.ViewGroup parent) {
                LinearLayout r = new LinearLayout(SettingsActivity.this);
                r.setOrientation(LinearLayout.VERTICAL);
                r.setPadding(dp(16), dp(8), dp(16), dp(8));
                if (pics[i] == null) {   // a part's heading: its name and its one line, muted, never focused
                    r.setPadding(dp(16), dp(i == 0 ? 2 : 14), dp(16), dp(4));
                    TextView h = new TextView(SettingsActivity.this);
                    h.setText(conv(names[i] + "، " + about[i]));
                    h.setTextColor(look.sub);
                    h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    h.setTypeface(font("Thmanyah-Medium"));
                    r.addView(h);
                    return r;
                }
                TextView t = new TextView(SettingsActivity.this);
                t.setText(conv(names[i]));
                t.setTextColor(focusColors(look.text, look.onFocus));
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                t.setTypeface(font("Thmanyah-Medium"));
                r.addView(t);
                TextView a = new TextView(SettingsActivity.this);
                a.setText(conv(about[i]));
                a.setTextColor(focusColors(look.sub, look.onFocusSub));
                a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                a.setTypeface(font("Thmanyah-Regular"));
                r.addView(a);
                return r;
            }
        });
        box.addView(lv, new LinearLayout.LayoutParams(0, dp(420), 1f));
        final ImageView pic = new ImageView(this);
        pic.setScaleType(ImageView.ScaleType.FIT_CENTER);   // the owner: a look's picture whole, never cropped
        pic.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(18)); }
        });
        pic.setClipToOutline(true);
        final LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(480), dp(270));
        pp.setMarginStart(dp(16));
        pp.gravity = Gravity.CENTER_VERTICAL;
        box.addView(pic, pp);
        final int[] shown = {-1};
        final int nowAt = Math.max(1, java.util.Arrays.asList(keys).indexOf(prefs.preset()));
        final Runnable update = () -> {
            int i = lv.getSelectedItemPosition();
            if (i < 0 || pics[i] == null) i = nowAt;
            if (i == shown[0]) return;
            shown[0] = i;
            setAssetPicture(pic, "presets/" + pics[i] + ".jpg");
        };
        final AlertDialog dialog = new Dlg(this).setTitle("أشكال جاهزة").setView(frame).setNegativeButton("إلغاء", null).create();
        final MixPanel mixes = new MixPanel(frame, lv, dialog);
        lv.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                update.run();
                if (pos == mixAt) mixes.open(v);   // the focus on the tile opens it
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        lv.setOnItemClickListener((p, v, pos, id) -> {
            if (keys[pos] == null && pos != mixAt) return;   // a heading
            if (pos == mixAt) { mixes.open(v); return; }
            dialog.dismiss();
            if (Prefs.fullDesign(keys[pos]) && !prefs.homePanels().isEmpty()) askCards(keys[pos]);   // the owner: asked once, Back cancels
            else applyPreset(keys[pos]);
        });
        dialog.setOnKeyListener((d, code, e) -> code == android.view.KeyEvent.KEYCODE_BACK && e.getAction() == android.view.KeyEvent.ACTION_UP && mixes.close());
        dialog.setOnShowListener(d -> {
            dress(dialog);
            dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(940)), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            MixFocus.calm(dialog.getWindow());   // the owner: a quiet backdrop, the settings page behind does not show through
            lv.requestFocus();
            lv.setSelection(prefs.presetCombo().isEmpty() ? nowAt : mixAt);
            update.run();
        });
        dialog.setOnDismissListener(d -> { pic.setImageDrawable(null); mixes.release(); });
        dialog.show();
    }

    private static final int HEAD_FULL = -1, HEAD_SIMPLE = -2, MIX = -3;

    /** the parts «تخصيص» gathers, each the label of its own row (its action is that row's), and the look's field it is (null: none) */
    private static final String[][] CUSTOM_PARTS = {
        {"التخطيط", "layout"}, {"الخلفية", "bg"}, {"الساعة", "clock"}, {"شكل التطبيقات", "apps"}, {"شكل شاشة الإعدادات", null},
        {"البطاقة الأولى", "cards"}, {"البطاقة الثانية", "cards"}, {"البطاقة الثالثة", "cards"}, {"الألوان والإضاءة", "appearance"}};

    /** «تخصيص»'s line: what changed since the look was chosen, or what it holds */
    private String customLine() {
        List<String> ch = prefs.presetChanged();
        if (ch.isEmpty()) return "التخطيط والخلفية والساعة والتطبيقات والبطاقات";
        StringBuilder b = new StringBuilder();
        for (String c : ch) { if (b.length() > 0) b.append("، "); b.append(c); }
        return String.format(java.util.Locale.ROOT, "غيّرت: %s", b);
    }

    /**
     * «تخصيص»: the look's parts in one list, each with its value, «غيّرتها» on a part changed since the look was chosen; OK on one opens that part's
     * own choice (its row's action, so there is one way to change it); then «رجّع بطاقاتك» while cards are hidden, and «رجّع شكلك المعتاد» while a look is on.
     */
    private void pickCustom() {
        final List<String> labels = new ArrayList<>();
        final List<Runnable> acts = new ArrayList<>();
        List<String> changed = prefs.presetChanged();
        for (String[] part : CUSTOM_PARTS) {
            final Runnable act = actions.get(FocusKey.key(part[0]));
            if (act == null) continue;   // a row not on this TV (a switch off): not offered
            int k = part[1] == null ? -1 : java.util.Arrays.asList(Prefs.PRESET_FIELDS).indexOf(part[1]);
            boolean mine = k >= 0 && changed.contains(Prefs.PRESET_FIELD_NAMES[k]);
            labels.add(mine ? String.format(java.util.Locale.ROOT, "%s، غيّرتها", part[0]) : part[0]);
            acts.add(act);
        }
        if (!prefs.hiddenCards().isEmpty()) { labels.add("رجّع بطاقاتك"); acts.add(() -> { prefs.restoreHiddenCards(); String n = prefs.takeFitNote(); render(); if (n != null) notice(n); }); }
        if (!"tv".equals(prefs.preset()) || !prefs.presetCombo().isEmpty()) { labels.add("رجّع شكلك المعتاد"); acts.add(() -> applyPreset("tv")); }
        final CharSequence[] shown = new CharSequence[labels.size()];
        for (int i = 0; i < shown.length; i++) shown[i] = conv(labels.get(i));
        new Dlg(this).setTitle("تخصيص").setItems(shown, (d, i) -> acts.get(i).run()).show();
    }

    /**
     * 4.4 beta 2 (the owner approved docs/mockups/full-look 06): a full-design look and the home's cards. One question, two plain choices: the cards
     * kept inside the design (each that does not fit it named with the one that takes its place, CardFit), or the pure design without cards (hidden,
     * not lost: «رجّع بطاقاتك» in «المظهر», and «شكلك المعتاد»). Back cancels the change: nothing is applied half-way.
     */
    private void askCards(final String key) {
        final String name = Prefs.presetName(key);
        List<String> mine = prefs.homePanels();
        List<String[]> swaps = new ArrayList<>();
        String layout = Prefs.PRESET_VALUES[java.util.Arrays.asList(Prefs.PRESETS).indexOf(key)][0];
        List<String> kept = CardFit.fitted(mine, "tvos".equals(key) ? "tvos" : layout == null ? prefs.homeLayout() : layout, swaps);
        StringBuilder keptNames = new StringBuilder();
        for (String c : kept) { if (keptNames.length() > 0) keptNames.append("، "); keptNames.append(Prefs.panelName(c)); }
        String intro = mine.size() == 1 ? "عندك على الرئيسية بطاقة واحدة." : mine.size() == 2 ? "عندك على الرئيسية بطاقتان."
            : String.format(java.util.Locale.ROOT, "عندك على الرئيسية %s بطاقات.", mine.size());
        for (String[] w : swaps)
            intro += " " + String.format(java.util.Locale.ROOT, "«%s» ما تناسب %s، فبنحط مكانها «%s».", Prefs.panelName(w[0]), name, Prefs.panelName(w[1]));
        final String[] labels = {"خلّ بطاقاتي داخل التصميم", String.format(java.util.Locale.ROOT, "%s بدون بطاقات", name)};
        final String[] subs = {String.format(java.util.Locale.ROOT, "%s، على %s", keptNames, name), "الساعة والتطبيقات بس. بطاقاتك محفوظة، وترجّعها من «المظهر»"};
        pickOneWithLines(String.format(java.util.Locale.ROOT, "%s: وش تبي مع بطاقاتك؟", name), intro, labels, subs, i -> applyPreset(key, i == 1));
    }

    private interface OnChoice { void on(int i); }

    /** a short question: its line, then the choices each with its plain line under it; OK picks, Back cancels (nothing changes) */
    private void pickOneWithLines(String title, String line, final String[] labels, final String[] subs, final OnChoice then) {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Dir.layout());
        box.setPadding(dp(16), dp(4), dp(16), dp(8));
        TextView intro = new TextView(this);
        intro.setText(conv(line));
        intro.setTextColor(look.sub);
        intro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        intro.setTypeface(font("Thmanyah-Regular"));
        intro.setTextDirection(Dir.text());
        intro.setPadding(dp(4), 0, dp(4), dp(12));
        box.addView(intro);
        final android.widget.ListView lv = new android.widget.ListView(this);
        lv.setDividerHeight(dp(8));
        lv.setDivider(new android.graphics.drawable.ColorDrawable(0));
        lv.setSelector(GlassPaint.focused(this, look, 12));
        lv.setAdapter(new android.widget.BaseAdapter() {
            @Override public int getCount() { return labels.length; }
            @Override public Object getItem(int i) { return labels[i]; }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View old, android.view.ViewGroup parent) {
                LinearLayout r = new LinearLayout(SettingsActivity.this);
                r.setOrientation(LinearLayout.VERTICAL);
                r.setPadding(dp(16), dp(10), dp(16), dp(10));
                TextView t = new TextView(SettingsActivity.this);
                t.setText(conv(labels[i]));
                t.setTextColor(focusColors(look.text, look.onFocus));
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                t.setTypeface(font("Thmanyah-Medium"));
                r.addView(t);
                TextView a = new TextView(SettingsActivity.this);
                a.setText(conv(subs[i]));
                a.setTextColor(focusColors(look.sub, look.onFocusSub));
                a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                a.setTypeface(font("Thmanyah-Regular"));
                r.addView(a);
                return r;
            }
        });
        box.addView(lv, new LinearLayout.LayoutParams(-1, -2));
        final AlertDialog dialog = new Dlg(this).setTitle(title).setView(box).create();
        lv.setOnItemClickListener((p, v, pos, id) -> { dialog.dismiss(); then.on(pos); });
        dialog.setOnShowListener(d -> { dress(dialog); lv.requestFocus(); lv.setSelection(0); });
        dialog.show();
    }

    /**
     * 4.4 QA-13: the pictures of the app's own assets (a row's thumbnail, a mix's picture, the stage's photo) are decoded on this one thread and
     * put in their view when they are here; the row is drawn at once without it. It ends by itself when idle.
     */
    private final java.util.concurrent.ThreadPoolExecutor pics = newPics();

    private static java.util.concurrent.ThreadPoolExecutor newPics() {
        java.util.concurrent.ThreadPoolExecutor e = new java.util.concurrent.ThreadPoolExecutor(1, 1, 5, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());
        e.allowCoreThreadTimeOut(true);
        return e;
    }

    /** the small thumbnails already decoded in this process (a row is drawn again at every render): a few hundred KB, never recycled */
    private static final android.util.LruCache<String, Bitmap> THUMBS = new android.util.LruCache<String, Bitmap>(6 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };

    /** a picture of the app (assets) into the view, the one before it let go (on the pictures' thread; the view keeps its old one until the new one is here) */
    private void setAssetPicture(final ImageView v, final String path) {
        pics.execute(() -> {
            Bitmap b = null;
            try (InputStream in = getAssets().open(path)) { b = android.graphics.BitmapFactory.decodeStream(in); } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) { }
            final Bitmap got = b;
            runOnUiThread(() -> {
                android.graphics.drawable.Drawable old = v.getDrawable();
                v.setImageBitmap(got);
                if (old instanceof android.graphics.drawable.BitmapDrawable && ((android.graphics.drawable.BitmapDrawable) old).getBitmap() != got) ((android.graphics.drawable.BitmapDrawable) old).getBitmap().recycle();
            });
        });
    }

    /**
     * «تركيبات مقترحة» opened (the owner, docs/mockups/presets/combos-*.jpg): a panel over the picker that grows as a circle from the tile while the four
     * mixes fan out of it, 250 ms, slowing at the end (PresetMotion); Back, or up from the mixes, closes it the same way back into the tile. On a weak TV
     * («الوضع الخفيف») it only fades in and out. OK on a mix applies it (Prefs.setCombo).
     */
    private final class MixPanel {
        final FrameLayout frame; final android.widget.ListView list; final AlertDialog dialog;
        LinearLayout panel; final List<View> cards = new ArrayList<>(); final List<ImageView> pics = new ArrayList<>();
        View from; boolean shown, moving;
        MixPanel(FrameLayout frame, android.widget.ListView list, AlertDialog dialog) { this.frame = frame; this.list = list; this.dialog = dialog; }

        private void build() {
            panel = new LinearLayout(SettingsActivity.this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setLayoutDirection(Dir.layout());
            panel.setPadding(dp(22), dp(16), dp(22), dp(18));
            panel.setBackground(GlassPaint.fill(SettingsActivity.this, 0xF2161210, 22, 0x1FFFFFFF));
            TextView h = new TextView(SettingsActivity.this);
            h.setText(conv("تركيبات مقترحة"));
            h.setTextColor(look.text);
            h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
            h.setTypeface(font("Thmanyah-Medium"));
            panel.addView(h);
            TextView sub = new TextView(SettingsActivity.this);
            sub.setText(conv("أربع تركيبات، كل وحدة شكل جاهز مع تغيير أو اثنين؛ اختيارها يضبطها كلها"));
            sub.setTextColor(look.sub);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            sub.setTypeface(font("Thmanyah-Regular"));
            panel.addView(sub);
            LinearLayout row = new LinearLayout(SettingsActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(Dir.layout());
            // 4.4 beta 2 (the owner's test of build 525): every mix reachable, the list scrolls with the focus and says where it is («٢ من ٤»)
            android.widget.HorizontalScrollView strip = new android.widget.HorizontalScrollView(SettingsActivity.this);
            strip.setHorizontalScrollBarEnabled(false);
            strip.setHorizontalFadingEdgeEnabled(true);
            strip.setFadingEdgeLength(dp(40));
            strip.addView(row);
            final TextView where = new TextView(SettingsActivity.this);
            where.setTextColor(look.sub);
            where.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            where.setTypeface(font("Thmanyah-Regular"));
            where.setPadding(0, dp(8), 0, 0);
            final String now = prefs.presetCombo();
            for (final String[] c : Prefs.COMBOS) {
                final int at = cards.size();
                LinearLayout card = new LinearLayout(SettingsActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(7), dp(7), dp(7), dp(9));
                card.setFocusable(true);
                card.setBackground(MixFocus.card(SettingsActivity.this, look, 14));   // the owner: at rest a quiet panel, focused the fill with a white ring
                card.setOnFocusChangeListener((v, has) -> {
                    if (!has) return;
                    where.setText(conv(String.format(java.util.Locale.ROOT, "التركيبة %s من %s", at + 1, Prefs.COMBOS.length)));
                });
                ImageView im = new ImageView(SettingsActivity.this);
                im.setScaleType(ImageView.ScaleType.FIT_CENTER);   // the whole picture, never cropped
                im.setClipToOutline(true);
                im.setOutlineProvider(new ViewOutlineProvider() {
                    @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(10)); }
                });
                card.addView(im, new LinearLayout.LayoutParams(dp(192), dp(108)));
                pics.add(im);
                TextView t = new TextView(SettingsActivity.this);
                t.setText(conv(c[1]));
                t.setTextColor(focusColors(look.text, look.onFocus));
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                t.setTypeface(font("Thmanyah-Medium"));
                card.addView(t, new LinearLayout.LayoutParams(dp(192), -2));
                TextView a = new TextView(SettingsActivity.this);
                a.setText(conv(c[2]));
                a.setTextColor(focusColors(look.sub, look.onFocusSub));
                a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                a.setTypeface(font("Thmanyah-Regular"));
                card.addView(a, new LinearLayout.LayoutParams(dp(192), -2));
                if (c[0].equals(now)) {   // the owner: the one on now says so
                    TextView cur = new TextView(SettingsActivity.this);
                    cur.setText(conv("الحالي"));
                    cur.setTextColor(focusColors(MixFocus.NOW, MixFocus.NOW_ON_FOCUS));
                    cur.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    cur.setTypeface(font("Thmanyah-Medium"));
                    card.addView(cur, new LinearLayout.LayoutParams(dp(192), -2));
                }
                card.setOnClickListener(v -> { dialog.dismiss(); applyCombo(c[0]); });
                card.setOnKeyListener((v, code, e) -> {   // up from a mix: moving away closes it, the focus back on the tile
                    if (code == android.view.KeyEvent.KEYCODE_DPAD_UP && e.getAction() == android.view.KeyEvent.ACTION_DOWN) { close(); return true; }
                    return false;
                });
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
                cp.setMarginEnd(dp(12));
                cp.topMargin = dp(12);
                row.addView(card, cp);
                cards.add(card);
            }
            panel.addView(strip, new LinearLayout.LayoutParams(-1, -2));
            panel.addView(where);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
            frame.addView(panel, lp);
            panel.setVisibility(View.INVISIBLE);
            for (int i = 0; i < cards.size(); i++) setAssetPicture(pics.get(i), "presets/combo-" + Prefs.COMBOS[i][0] + ".jpg");
        }

        /** the focus reached the tile (or OK on it): the panel grows out of it */
        void open(View tile) {
            if (shown || moving) return;
            if (panel == null) build();
            from = tile;
            shown = true;
            panel.setVisibility(View.VISIBLE);
            panel.post(() -> {
                int at = 0;   // the focus on the mix on now, else the first
                for (int i = 0; i < Prefs.COMBOS.length; i++) if (Prefs.COMBOS[i][0].equals(prefs.presetCombo())) at = i;
                cards.get(at).requestFocus();
                animate(true, null);
            });
        }

        /** Back or up: closed the same way, into the tile; false when it was not open (Back then closes the picker) */
        boolean close() {
            if (!shown || panel == null) return false;
            if (moving) return true;
            shown = false;
            animate(false, () -> {
                panel.setVisibility(View.INVISIBLE);
                list.requestFocus();
                list.setSelection(list.getCount() - 2);   // the focus to the look above the tile (the tile is last): it does not open again at once
            });
            return true;
        }

        private void animate(boolean in, Runnable after) {
            moving = true;
            final Runnable done = () -> { moving = false; if (after != null) after.run(); };
            float[] c = tileCentre();
            if (prefs.liteNow(SettingsActivity.this)) {   // «الوضع الخفيف»: a fade only
                panel.setAlpha(in ? 0f : 1f);
                panel.animate().alpha(in ? 1f : 0f).setDuration(PresetMotion.MS).withEndAction(done).start();
                return;
            }
            float r = PresetMotion.radius(c[0], c[1], panel.getWidth(), panel.getHeight());
            android.animation.Animator reveal = android.view.ViewAnimationUtils.createCircularReveal(panel, (int) c[0], (int) c[1], in ? 0f : r, in ? r : 0f);
            reveal.setDuration(PresetMotion.MS);
            reveal.setInterpolator(Motion.ease());
            reveal.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) { done.run(); }
            });
            for (View card : cards) {   // each mix comes out of the tile's centre: from there, small and clear, to its place
                float dx = c[0] - (card.getLeft() + ((View) card.getParent()).getLeft() + card.getWidth() / 2f);
                float dy = c[1] - (card.getTop() + ((View) card.getParent()).getTop() + card.getHeight() / 2f);
                if (in) { card.setTranslationX(dx); card.setTranslationY(dy); card.setScaleX(PresetMotion.SCALE0); card.setScaleY(PresetMotion.SCALE0); card.setAlpha(0f); }
                card.animate().translationX(in ? 0 : dx).translationY(in ? 0 : dy).scaleX(in ? 1f : PresetMotion.SCALE0).scaleY(in ? 1f : PresetMotion.SCALE0)
                    .alpha(in ? 1f : 0f).setDuration(PresetMotion.MS)
                    .setInterpolator(Motion.ease()).start();
            }
            reveal.start();
        }

        /** the tile's centre in the panel's coordinates */
        private float[] tileCentre() {
            if (from == null) return new float[] {panel.getWidth() / 2f, panel.getHeight() / 2f};
            int[] a = new int[2], b = new int[2];
            from.getLocationInWindow(a);
            panel.getLocationInWindow(b);
            return new float[] {a[0] - b[0] + from.getWidth() / 2f, a[1] - b[1] + from.getHeight() / 2f};
        }

        void release() {
            for (ImageView v : pics) v.setImageDrawable(null);
            if (panel != null) panel.animate().cancel();
        }
    }

    /** a mix chosen: applied as a look is, and drawn again here (4.4 beta 2: a look never changes the settings screen; «شكل شاشة الإعدادات» does) */
    private void applyCombo(String key) {
        String style = prefs.settingsStyle();
        prefs.setCombo(key);
        afterLook(style);
    }

    /** a look chosen: applied, and drawn again here; 4.4 beta 2 (the owner): only a full-design look («كنبة») changes the settings screen too */
    private void applyPreset(String key) { applyPreset(key, false); }
    private void applyPreset(String key, boolean noCards) {
        String style = prefs.settingsStyle();
        prefs.setPreset(key, noCards);
        afterLook(style);
    }

    /** the same screen drawn again, or, when the look changed the settings screen's style, the settings opened again in it, on «أشكال جاهزة» */
    private void afterLook(String styleBefore) {
        if (styleBefore.equals(prefs.settingsStyle())) { render(); return; }
        startActivity(new Intent(this, SettingsActivity.class).putExtra("screen", "look").putExtra("hubAt", "look").putExtra("want", "أشكال جاهزة").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        finish();
    }

    private void flipCouch() {
        prefs.setCouch(!prefs.couchOn());
        render();   // 4.4 beta 2: the settings screen stays as it is
    }

    private void classicSide() {
        side.removeAllViews();
        sideItems.clear();
        final boolean tiles = SettingsHub.hasTiles(screen);
        sideBox.setVisibility(tiles ? View.GONE : View.VISIBLE);
        int rowsWidth = tiles ? pageWidth : pageWidth - sideWidth - dp(36);   // the buttons: the whole width; a category: beside the side
        if (scroll.getLayoutParams() != null && scroll.getLayoutParams().width != rowsWidth) {
            scroll.getLayoutParams().width = rowsWidth;
            scroll.requestLayout();
        }
        if (tiles) return;
        final ImageView ic = new ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(classicIcon(screen)).mutate();
        d.setTint(look.accent);
        ic.setImageDrawable(d);
        side.addView(ic, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView t = new TextView(this);
        t.setText(conv(SettingsHub.name(screen)));
        t.setTextColor(look.text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
        t.setTypeface(font("ThmanyahDisplay-Bold"));
        t.setPadding(0, dp(16), 0, dp(10));
        side.addView(t);
        TextView a = new TextView(this);
        a.setText(conv(SettingsHub.about(screen)));
        a.setTextColor(look.sub);
        a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        a.setTypeface(font("Thmanyah-Regular"));
        a.setLineSpacing(dp(4), 1f);
        a.setTextDirection(Dir.text());
        side.addView(a);
        TextView up = new TextView(this);
        up.setText(Words.of(SettingsHub.parent(screen) == null ? "رجوع إلى الأقسام" : "رجوع إلى " + SettingsHub.name(SettingsHub.parent(screen))));   // words, not «‹›» (mirrored or not by the renderer)
        up.setTextColor(look.sub);
        up.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        up.setTypeface(font("Thmanyah-Regular"));
        up.setPadding(0, dp(22), 0, 0);
        side.addView(up);
    }

    private void hub(String want, int wasIndex) {
        final String[] cats = tiles(screen);
        final boolean wide = SettingsHub.wide(screen);
        final List<View> tiles = new ArrayList<>();
        LinearLayout line = null;
        int inLine = 0;
        for (int i = 0; i < cats.length; i++) {
            boolean alone = (wide && i == 0) || ts != null;   // «tvOS»: one section a line
            if (alone || inLine == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setLayoutDirection(Dir.layout());
                list.addView(line, new LinearLayout.LayoutParams(-1, -2));
            }
            final String cat = cats[i];
            View t = tile(cat, SettingsHub.name(cat), classicIcon(cat), classicSummary(cat));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.bottomMargin = dp(12);
            if (!alone && inLine == 0) lp.setMarginEnd(dp(12));
            line.addView(t, lp);
            tiles.add(t);
            inLine = alone ? 0 : (inLine + 1) % 2;
        }
        if (inLine == 1) line.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));   // a last button alone keeps its width
        // SettingsHub.next counts the rows from the right (2 left, 3 right); an English screen runs from the left: the two sides swap
        final int left = Dir.rtl() ? 2 : 3, right = Dir.rtl() ? 3 : 2;
        for (int i = 0; i < tiles.size(); i++) {   // explicit ways (SettingsHub.next): nothing wraps, and right is right in the RTL rows
            View t = tiles.get(i);
            t.setNextFocusUpId(tiles.get(SettingsHub.next(i, tiles.size(), 0, wide)).getId());
            t.setNextFocusDownId(tiles.get(SettingsHub.next(i, tiles.size(), 1, wide)).getId());
            t.setNextFocusLeftId(tiles.get(SettingsHub.next(i, tiles.size(), left, wide)).getId());
            t.setNextFocusRightId(tiles.get(SettingsHub.next(i, tiles.size(), right, wide)).getId());
        }
        if (ts != null) for (int i = 0; i < tiles.size(); i++) {   // «tvOS»: one column, up and down; the sides stay
            View t = tiles.get(i);
            t.setNextFocusUpId(tiles.get(Math.max(0, i - 1)).getId());
            t.setNextFocusDownId(tiles.get(Math.min(tiles.size() - 1, i + 1)).getId());
            t.setNextFocusLeftId(t.getId());
            t.setNextFocusRightId(t.getId());
        }
        restoreFocus(want, wasIndex, 0);
    }

    private View tile(final String cat, String name, int icon, String summary) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setLayoutDirection(Dir.layout());
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(18), dp(26), dp(18));
        card.setFocusable(true);
        card.setClickable(true);
        card.setId(View.generateViewId());
        card.setTag("hub:" + cat);
        final int state = OnOff.state(summary);
        final ImageView mark = onMark(state);
        card.addView(mark, new LinearLayout.LayoutParams(dp(10), dp(30)));
        final ImageView ic = new ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(icon).mutate();
        d.setTint(look.accent);
        ic.setImageDrawable(d);
        ic.setTag(GlassPaint.ICON);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(36), dp(36));
        ip.setMarginStart(dp(8));
        card.addView(ic, ip);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView n = new TextView(this);
        n.setText(conv(name));
        n.setTextColor(look.text);
        n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        n.setTypeface(font("Thmanyah-Medium"));
        n.setSingleLine(true);
        texts.addView(n);
        final TextView v = new TextView(this);
        if (!summary.isEmpty()) {
            v.setTextColor(look.sub);
            v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            v.setTypeface(font("Thmanyah-Regular"));
            v.setSingleLine(true);
            v.setEllipsize(android.text.TextUtils.TruncateAt.END);
            v.setTextDirection(Dir.text());
            texts.addView(v);
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(dp(18));
        card.addView(texts, tp);
        final String shown = conv(summary);
        paintState(v, mark, shown, state, false);
        card.setOnClickListener(x -> {
            String row = SettingsHub.direct(cat);
            Runnable act = row == null ? null : actions.get(row);
            if (act != null) { launchedRow = "hub:" + cat; hubAt = cat; act.run(); return; }   // a button of the first screen that opens another screen (the clean screen): the focus comes back to IT
            hubAt = cat;
            open(cat);
        });
        card.setOnFocusChangeListener((x, has) -> { paintState(v, mark, shown, state, has); if (has) topWhenFirst(x); if (ts != null) tvosFocus(x, has, cat); });
        glassCard(card);
        return card;
    }

    /** a section button's line under its name (the first screen of «أزرار» and tvOS): what is set now in it */
    private String classicSummary(String cat) {
        switch (cat) {
            case "pray": {
                Boolean on = homeAdhanSeen();   // the home's switch as last known; null when the home cannot say (not installed, or older)
                String m = PrayerMethod.label(prefs.prayerMethod(), prefs.cityCountry(), prefs.cityTz());
                return on == null ? m : (on ? "الأذان يعمل" : "الأذان متوقف") + S.sep() + m;
            }
            case "photos": return "صوري " + arabicNum(Own.count(this)) + S.sep() + packsLabel();
            case "look": return Prefs.lookLabel(prefs.look()) + S.sep() + Prefs.appearanceLabel(prefs.appearance());
            case "saver": return idleSummary() + S.sep() + Prefs.panelLabel(prefs.panel());
            case "apps": return "المفضلة والمخفية والأزرار";
            case "content": return "الفرق " + arabicNum(prefs.teams().size()) + "، الأسواق " + (prefs.marketsOn() ? arabicNum(prefs.markets().size()) : "مخفية");
            case "family": return "الرقابة الأبوية والجوالات";
            case "lang": return langLabel() + S.sep() + Words.city(prefs.city());
            case "update": return Updater.versionLines(this)[0];
            case "search": return "اكتب كلمة وانتقل إلى الصف";
            default: return "";
        }
    }

    private static int classicIcon(String cat) {
        switch (cat) {
            case "pray": return R.drawable.ic_bell;
            case "photos": return R.drawable.ic_eye;
            case "look": return R.drawable.ic_category;
            case "saver": return R.drawable.ic_screensaver;
            case "apps": return R.drawable.ic_star;
            case "content": return R.drawable.ic_trophy;
            case "family": return R.drawable.ic_shield;
            case "lang": return R.drawable.ic_language;
            case "update": return R.drawable.ic_sync;
            case "search": return R.drawable.ic_search;
            default: return R.drawable.ic_settings;
        }
    }

    /** Each row's action by its FocusKey, made by render on every screen: a DIRECT button (SettingsHub.direct) runs its row's. */
    private final java.util.Map<String, Runnable> actions = new java.util.HashMap<>();

    /**
     * 4.3.3 the side list (docs/mockups/settings-main.png): «الإعدادات», then «بحث في الإعدادات» and the ten sections, always on the right, the rows on the
     * left. Built once; each render only marks the section shown. Moving the focus along the list shows each section's rows at once (the focus stays
     * on the list); OK or the arrow toward the rows goes into them; Back from a row comes back to its section's item.
     */
    private void sidePane() {
        int rowsWidth = pageWidth - sideWidth - gapPx + (cs != null ? cs.px(CouchStyle.PAD) : 0);
        if (scroll.getLayoutParams() != null && scroll.getLayoutParams().width != rowsWidth) {
            scroll.getLayoutParams().width = rowsWidth;
            scroll.requestLayout();
        }
        if (sideItems.isEmpty() && cs != null) { couchSide(); } else
        if (sideItems.isEmpty()) {
            side.removeAllViews();
            TextView t = new TextView(this);
            t.setText(conv("الإعدادات"));
            t.setTextColor(look.text);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            t.setTypeface(font("ThmanyahDisplay-Bold"));
            t.setTextDirection(Dir.text());
            t.setPadding(dp(12), 0, dp(12), dp(12));
            side.addView(t);
            for (String cat : SettingsHub.tiles(null)) {
                if (sectionGone(cat)) continue;
                View item = sideItem(cat);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = SettingsHub.SEARCH.equals(cat) ? dp(12) : dp(4);
                side.addView(item, lp);
                sideItems.put(cat, item);
            }
        }
        for (java.util.Map.Entry<String, View> e : sideItems.entrySet()) {   // the section shown: its bar, and its item a little brighter
            if (cs != null) { e.getValue().setSelected(e.getKey().equals(screen)); continue; }   // couch-tv: the shown section's own fill
            View bar = e.getValue().findViewWithTag("bar");
            boolean shown = e.getKey().equals(screen);
            if (bar != null) bar.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
            e.getValue().setAlpha(shown || e.getValue().hasFocus() ? 1f : 0.82f);
        }
    }

    /** One item of the side list: the section's icon and name; the search item opens the search. */
    private View sideItem(final String cat) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setLayoutDirection(Dir.layout());
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(9), dp(12), dp(9));
        card.setFocusable(true);
        card.setClickable(true);
        card.setId(View.generateViewId());
        card.setTag(SIDE);
        View bar = new View(this);
        bar.setBackgroundColor(look.accent);
        bar.setTag("bar");
        bar.setVisibility(View.INVISIBLE);
        card.addView(bar, new LinearLayout.LayoutParams(dp(3), dp(20)));
        final ImageView ic = new ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(hubIcon(cat)).mutate();
        d.setTint(look.accent);
        ic.setImageDrawable(d);
        ic.setTag(GlassPaint.ICON);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(22), dp(22));
        ip.setMarginStart(dp(8));
        card.addView(ic, ip);
        TextView n = new TextView(this);
        n.setText(conv(SettingsHub.name(cat)));
        n.setTextColor(look.text);
        n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        n.setTypeface(font("Thmanyah-Medium"));
        n.setSingleLine(true);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        n.setTextDirection(Dir.text());
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(dp(12));
        card.addView(n, tp);
        card.setOnClickListener(x -> {
            if (SettingsHub.SEARCH.equals(cat)) { searchSettings(); return; }
            if (!cat.equals(screen)) open(cat);
            else restoreFocus(lastRow.get(screen), 0, scroll.getScrollY());   // into the rows: the one last focused there, else the first
        });
        if (cs != null) couchSection(card, bar, ic, n, cat);
        card.setOnFocusChangeListener((x, has) -> {
            if (cs != null) couchSectionFocus(x, has); else
            x.setAlpha(has || cat.equals(screen) ? 1f : 0.82f);
            side.removeCallbacks(sideOpen);
            if (has && !SettingsHub.SEARCH.equals(cat) && !cat.equals(screen)) {   // the section's rows, the focus kept on the list
                // 4.4 smoother: once the focus rests on the item (SIDE_MS): a held arrow went through every section, each built whole
                sideOpen = () -> {
                    if (!x.hasFocus() || cat.equals(screen)) return;
                    View f = focusedRow();
                    if (f != null && screen != null) lastRow.put(screen, (String) f.getTag());
                    sideFocus = true;
                    open(cat);
                };
                side.postDelayed(sideOpen, SIDE_MS);
            }
        });
        if (cs == null) glassCard(card);
        return card;
    }

    // ---- 4.4 «كنبة»: the settings in couch-tv's style (CouchStyle; docs/mockups/settings-couchtv/, approved by the owner) ----

    /** the veil over the home under the settings: couch-tv's rgba(10,8,12,.8) over the blurred home; nearly solid on a weak TV («الوضع الخفيف») */
    private void couchVeil() {
        if (cs == null || root == null) return;
        boolean lite = "on".equals(prefs.lite()) || ("auto".equals(prefs.lite()) && Prefs.lowRam(this));
        android.graphics.drawable.Drawable bg = root.getBackground();
        android.graphics.drawable.Drawable veil = new android.graphics.drawable.ColorDrawable(lite ? CouchStyle.VEIL_LITE : CouchStyle.VEIL);
        root.setBackground(bg == null ? veil : new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[] {bg, veil}));
    }

    // ---- 4.4 beta 2 «tvOS»: the 4.3.2 screen drawn in the tvOS manner (TvosStyle; docs/mockups/settings-style/, approved by the owner) ----

    /** the tvOS colours over the person's look, and its veil over the blurred home (stronger on a weak TV) */
    private void tvosVeil() {
        if (ts == null || root == null) return;
        look = ts.look(look);
        android.graphics.drawable.Drawable bg = root.getBackground();
        android.graphics.drawable.Drawable veil = new android.graphics.drawable.ColorDrawable(ts.veil());
        root.setBackground(bg == null ? veil : new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[] {bg, veil}));
    }

    private ImageView tvosGlyph;
    private TextView tvosAbout;

    /** the screen's name over its rows («الإعدادات» on the first), and the side: the shown section's icon large in its glass square, its line under it */
    private void tvosHead() {
        TextView title = new TextView(this);
        title.setText(Words.of(screen == null ? "الإعدادات" : SettingsHub.name(screen)));
        title.setTextColor(look.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, dp(14));
        list.addView(title, new LinearLayout.LayoutParams(-1, -2));
        side.removeAllViews();
        sideItems.clear();
        sideBox.setVisibility(View.VISIBLE);
        int rowsWidth = pageWidth - sideWidth - gapPx;
        if (scroll.getLayoutParams() != null && scroll.getLayoutParams().width != rowsWidth) {
            scroll.getLayoutParams().width = rowsWidth;
            scroll.requestLayout();
        }
        side.setGravity(Gravity.CENTER_HORIZONTAL);
        int box = Math.min(Math.round(sideWidth * TvosStyle.GLYPH_SIDE), dp(TvosStyle.GLYPH_MAX_DP));
        FrameLayout square = new FrameLayout(this);
        square.setBackground(ts.glyphBox(box * TvosStyle.GLYPH_CORNER, dp(1)));
        tvosGlyph = new ImageView(this);
        tvosGlyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int icon = Math.round(box * TvosStyle.GLYPH_ICON);
        square.addView(tvosGlyph, new FrameLayout.LayoutParams(icon, icon, Gravity.CENTER));
        LinearLayout.LayoutParams sq = new LinearLayout.LayoutParams(box, box);
        sq.topMargin = dp(56);
        side.addView(square, sq);
        tvosAbout = new TextView(this);
        tvosAbout.setTextColor(look.sub);
        tvosAbout.setTextSize(TypedValue.COMPLEX_UNIT_SP, HINT_SP);
        tvosAbout.setTypeface(font("Thmanyah-Regular"));
        tvosAbout.setGravity(Gravity.CENTER_HORIZONTAL);
        tvosAbout.setLineSpacing(dp(4), 1f);
        tvosAbout.setTextDirection(Dir.text());
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(box + dp(40), -2);
        ap.topMargin = dp(18);
        side.addView(tvosAbout, ap);
        tvosShow(screen == null ? (hubAt != null ? hubAt : SettingsHub.LOOK) : screen);
    }

    /** the side shows this section: its icon and its line */
    private void tvosShow(String cat) {
        if (tvosGlyph == null || cat == null) return;
        android.graphics.drawable.Drawable d = getDrawable(classicIcon(cat)).mutate();
        d.setTint(look.text);
        tvosGlyph.setImageDrawable(d);
        tvosAbout.setText(conv(SettingsHub.about(cat)));
    }

    /** a section's button with the focus: the side shows it, and it grows a little (not on a weak TV) */
    private void tvosFocus(View x, boolean has, String cat) {
        if (has) tvosShow(cat);
        float s = has ? ts.grow() : 1f;
        x.animate().scaleX(s).scaleY(s).setDuration(ts.light ? 0 : FOCUS_MS).setInterpolator(Motion.ease()).start();
        x.setTranslationZ(has && !ts.light ? dp(4) : 0);
    }

    /** couch-tv's side: «الإعدادات», the round search button beside it (couch-tv has no search), then the sections */
    private void couchSide() {
        side.removeAllViews();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setLayoutDirection(Dir.layout());
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPaddingRelative(cs.px(CouchStyle.TITLE_SIDE), cs.px(CouchStyle.TITLE_TOP), 0, cs.px(CouchStyle.TITLE_BOTTOM));
        TextView t = new TextView(this);
        t.setText(conv("الإعدادات"));
        t.setTextColor(CouchStyle.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.TITLE));
        t.setTypeface(font("ThmanyahDisplay-Bold"));
        t.setTextDirection(Dir.text());
        t.setSingleLine(true);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        final ImageView find = new ImageView(this);
        find.setId(View.generateViewId());
        find.setFocusable(true);
        find.setClickable(true);
        find.setTag(SIDE);
        find.setContentDescription(conv("بحث في الإعدادات"));   // 4.4 beta 4: the icon has a name (a screen reader, and the emulator's count, could not find it)
        find.setImageDrawable(getDrawable(R.drawable.ic_search).mutate());
        find.setImageTintList(CouchStyle.ink(CouchStyle.TEXT, CouchStyle.WHITE, CouchStyle.INK));
        int pad = (cs.px(CouchStyle.FIND) - cs.px(CouchStyle.SEC_ICON)) / 2;
        find.setPadding(pad, pad, pad, pad);
        find.setBackground(cs.card(CouchStyle.ICON_BOX, CouchStyle.SEC_ON, CouchStyle.FIND / 2));
        find.setOnClickListener(v -> openSearch());
        find.setOnFocusChangeListener((v, has) -> CouchStyle.focus(v, has, CouchStyle.SEC_SCALE));
        top.addView(find, new LinearLayout.LayoutParams(cs.px(CouchStyle.FIND), cs.px(CouchStyle.FIND)));
        side.addView(top);
        sideItems.put(SettingsHub.SEARCH, find);
        for (String cat : SettingsHub.tiles(null)) {
            if (SettingsHub.SEARCH.equals(cat) || sectionGone(cat)) continue;
            View item = sideItem(cat);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = cs.px(CouchStyle.SEC_GAP);
            side.addView(item, lp);
            sideItems.put(cat, item);
        }
    }

    /**
     * one section of the side in couch-tv's look, 4.4 beta 2 v2 (the owner approved docs/mockups/settings-style/couch-v2-*): a taller bar, the
     * name only (no icon, no mark); white when focused, lit while it is the one shown. The focused bar shows its name large and, under it, one plain
     * line of what is inside (SettingsHub.SHORT): it grows by its words, never by a scale, so a weak TV has no motion at all.
     */
    private void couchSection(LinearLayout card, View bar, ImageView ic, TextView n, String cat) {
        bar.setVisibility(View.GONE);
        ic.setVisibility(View.GONE);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        card.setMinimumHeight(cs.px(CouchStyle.SEC2_H));
        card.setPaddingRelative(cs.px(CouchStyle.SEC2_PAD_X), cs.px(CouchStyle.SEC2_PAD_Y), cs.px(CouchStyle.SEC2_PAD_X), cs.px(CouchStyle.SEC2_PAD_Y));
        card.setBackground(cs.card(0, CouchStyle.SEC_ON, CouchStyle.SEC_RADIUS));
        n.setDuplicateParentStateEnabled(true);
        n.setTextColor(CouchStyle.ink(CouchStyle.SEC, CouchStyle.WHITE, CouchStyle.INK));
        n.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.SEC2_TEXT));
        n.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        TextView line = new TextView(this);
        line.setTag(SEC_LINE);
        line.setText(conv(SettingsHub.shortLine(cat)));
        line.setTextColor(CouchStyle.INK_SUB);
        line.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.SEC2_LINE));
        line.setTypeface(font("Thmanyah-Regular"));
        line.setTextDirection(Dir.text());
        line.setMaxLines(3);   // wraps (a large font at 720p), never cut with «…»
        line.setVisibility(View.GONE);
        card.addView(line, new LinearLayout.LayoutParams(-1, -2));
    }

    /** a section's focus in «كنبة» v2: its name large and its line shown; back to the name alone when the focus leaves (a size change, no animation) */
    private void couchSectionFocus(View card, boolean has) {
        TextView n = null;
        View line = card.findViewWithTag(SEC_LINE);
        for (int k = 0; k < ((ViewGroup) card).getChildCount(); k++) {
            View c = ((ViewGroup) card).getChildAt(k);
            if (c instanceof TextView && c != line && c.getVisibility() == View.VISIBLE) { n = (TextView) c; break; }
        }
        if (n != null) {
            n.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(has ? CouchStyle.SEC2_BIG : CouchStyle.SEC2_TEXT));
            n.setTypeface(font(has ? "ThmanyahDisplay-Bold" : "Thmanyah-Medium"));
        }
        if (line != null && ((TextView) line).length() > 0) line.setVisibility(has ? View.VISIBLE : View.GONE);
        card.setTranslationZ(has ? 8f : 0f);
    }
    private static final String SEC_LINE = "secLine";

    /** couch-tv's head of the rows: a breadcrumb (a picker's section) over the title */
    private void couchHead() {
        boolean search = SettingsHub.SEARCH.equals(screen);
        TextView crumb = new TextView(this);
        crumb.setText(page != null ? conv(SettingsHub.name(screen)) : "");
        crumb.setTextColor(CouchStyle.DIM);
        crumb.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.CRUMB));
        crumb.setTypeface(font("Thmanyah-Medium"));
        crumb.setTextDirection(Dir.text());
        crumb.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        crumb.setPaddingRelative(cs.px(CouchStyle.PAD), cs.px(0.75f), cs.px(CouchStyle.PAD), 0);
        list.addView(crumb, new LinearLayout.LayoutParams(-1, cs.px(CouchStyle.CRUMB_H + 0.75f)));
        TextView head = new TextView(this);
        head.setText(page != null ? page.title : conv(search ? "بحث في الإعدادات" : SettingsHub.name(screen)));
        head.setTextColor(CouchStyle.WHITE);
        head.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.HEAD));
        head.setTypeface(font("ThmanyahDisplay-Bold"));
        head.setTextDirection(Dir.text());
        head.setPaddingRelative(cs.px(CouchStyle.PAD), cs.px(0.25f), cs.px(CouchStyle.PAD), cs.px(1.5f));
        list.addView(head);
    }

    private CharSequence tx(String s) { return noConv ? Words.of(s) : conv(s); }

    /**
     * One row in couch-tv's look: the name (its line under it when there is one), and at its far end the value, the toggle's ring (green with a
     * tick when on), a choice's tick or a link's chevron; a thumbnail at its start for a scene pack. White with dark words when focused, x1.02.
     * mark: 0 none, or CouchStyle.Mark's kind.
     */
    private LinearLayout couchCard(CharSequence label, CharSequence sub, CharSequence value, int mark, String thumb, String tag) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutDirection(Dir.layout());
        card.setMinimumHeight(cs.px(CouchStyle.ROW_H));
        card.setPaddingRelative(cs.px(CouchStyle.PAD), cs.px(CouchStyle.ROW_PAD_Y), cs.px(CouchStyle.PAD), cs.px(CouchStyle.ROW_PAD_Y));
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(tag);
        card.setBackground(cs.card(CouchStyle.ROW, CouchStyle.ROW, CouchStyle.ROW_RADIUS));
        int gap = cs.px(1.25f);
        if (thumb != null) {
            ImageView th = new ImageView(this);
            th.setScaleType(ImageView.ScaleType.CENTER_CROP);
            final Bitmap hit = THUMBS.get(thumb);
            if (hit != null) th.setImageBitmap(hit);
            else {
                final String name = thumb;
                final ImageView target = th;
                pics.execute(() -> {
                    Bitmap b = null;
                    try (InputStream in = getAssets().open(name)) { b = BitmapFactory.decodeStream(in); } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) { }
                    final Bitmap got = b;
                    if (got != null) { THUMBS.put(name, got); runOnUiThread(() -> target.setImageBitmap(got)); }
                });
            }
            th.setClipToOutline(true);
            th.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), cs.px(CouchStyle.THUMB_RADIUS)); }
            });
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(cs.px(CouchStyle.THUMB_W), cs.px(CouchStyle.THUMB_H));
            tl.setMarginEnd(gap);
            card.addView(th, tl);
        }
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setDuplicateParentStateEnabled(true);
        TextView name = new TextView(this);
        name.setTag("name");   // adjRow changes it in place
        name.setText(label);
        name.setDuplicateParentStateEnabled(true);
        name.setTextColor(CouchStyle.ink(CouchStyle.TEXT, CouchStyle.TEXT, CouchStyle.INK));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.ROW_TEXT));
        name.setTypeface(font("Thmanyah-Medium"));
        name.setTextDirection(Dir.text());
        texts.addView(name);
        if (sub != null && sub.length() > 0) {
            TextView sm = new TextView(this);
            sm.setText(sub);
            sm.setDuplicateParentStateEnabled(true);
            sm.setTextColor(CouchStyle.ink(CouchStyle.SUB, CouchStyle.SUB, CouchStyle.INK_SUB));
            sm.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.ROW_SUB));
            sm.setTypeface(font("Thmanyah-Regular"));
            sm.setTextDirection(Dir.text());
            sm.setMaxLines(2);
            sm.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(sm);
        }
        card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
        if (value != null && value.length() > 0) {
            TextView v = new TextView(this);
            v.setText(value);
            v.setDuplicateParentStateEnabled(true);
            v.setTextColor(CouchStyle.ink(CouchStyle.VALUE, CouchStyle.VALUE, CouchStyle.INK_SUB));
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.ROW_TEXT));
            v.setTypeface(font("Thmanyah-Regular"));
            v.setMaxWidth(Math.round((pageWidth - sideWidth) * 0.48f));
            v.setMaxLines(2);   // a long value wraps at the far end (tester S7), the row stays one size
            v.setEllipsize(android.text.TextUtils.TruncateAt.END);
            v.setTextDirection(Dir.text());
            LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(-2, -2);
            vl.setMarginStart(gap);
            card.addView(v, vl);
        }
        if (mark != 0) {
            CouchStyle.Mark m = new CouchStyle.Mark(this, mark, Dir.rtl());
            int size = mark == CouchStyle.Mark.RING_ON || mark == CouchStyle.Mark.RING_OFF ? cs.px(CouchStyle.BOX) : cs.px(CouchStyle.MARK);
            LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(size, size);
            ml.setMarginStart(gap);
            card.addView(m, ml);
        }
        card.setOnFocusChangeListener((x, has) -> {
            CouchStyle.focus(x, has, CouchStyle.ROW_SCALE);
            if (has && x.getTag() instanceof String) topWhenFirst(x);
        });
        return card;
    }

    /** a registry row in couch-tv's look: an on/off value is a toggle (its words after «مفعّل» under the name), a short value at the far end, a long one under the name; a row with no value a link */
    private LinearLayout couchRow(String label, String value, Runnable onClick, String key) {
        int state = OnOff.state(value), mark = 0;
        String sub = null, val = null;
        // one line, one size (the owner, build 413): the value at the far end (a long one wraps there), a toggle's words before its ring
        if (value != null && state >= 0) { mark = state > 0 ? CouchStyle.Mark.RING_ON : CouchStyle.Mark.RING_OFF; val = CouchText.after(value, OnOff.wordEnd(value)); }
        else if (value != null && !value.isEmpty()) val = value;
        if (mark == 0 && !CouchText.short_(val) && onClick != null) mark = CouchStyle.Mark.CHEVRON;
        String where = SettingsHub.scopeWords(key);   // 4.4 the owner: where the row shows, after its value
        if (!where.isEmpty()) val = val == null || val.isEmpty() ? where : val + "، " + where;
        if (val != null && val.isEmpty()) val = null;
        final LinearLayout card = couchCard(tx(label), sub == null ? null : tx(sub), val == null ? null : tx(val), mark, null, key);
        if (onClick != null) card.setOnClickListener(v -> { launchedRow = key; onClick.run(); });
        return card;
    }

    /** a picker that opens in place of the rows (Dlg.show with the Couch look): its choices as rows under the section's breadcrumb */
    static final class Page {
        CharSequence title; CharSequence[] labels; int checked = -1; boolean[] checks; String[] thumbs;
        DialogInterface.OnClickListener one, item; DialogInterface.OnMultiChoiceClickListener many; Runnable save; String back; int at = -1;
    }

    void showPage(Page p) {
        p.back = launchedRow;
        if (Prefs.SCENE_PACKS.length == p.labels.length && String.valueOf(p.title).equals(String.valueOf(Words.of("حزم الآفاق"))))
            p.thumbs = new String[] {"scene-thumbs/saudi.jpg", "scene-thumbs/couch.jpg"};   // couch-tv's scene rows: a thumbnail each
        page = p;
        scroll.scrollTo(0, 0);
        render();
    }

    /** the page left: by its choice, or by Back (a list of ticks is saved by Back, as its dialog is) */
    private void closePage(boolean back) {
        Page p = page;
        if (p == null) return;
        page = null;
        if (back && p.save != null) p.save.run();
        forceWant = p.back;
        render();
    }

    private void pageRows() {
        final Page p = page;
        final DialogInterface d = new DialogInterface() {
            @Override public void cancel() { if (page == p) closePage(true); }
            @Override public void dismiss() { if (page == p) { page = null; forceWant = p.back; } }
        };
        View first = null;
        for (int i = 0; i < p.labels.length; i++) {
            final int k = i;
            int mark = p.checks != null ? (p.checks[i] ? CouchStyle.Mark.RING_ON : CouchStyle.Mark.RING_OFF)
                : p.one != null ? (i == p.checked ? CouchStyle.Mark.TICK : 0) : CouchStyle.Mark.CHEVRON;
            LinearLayout card = couchCard(p.labels[i], null, null, mark, p.thumbs != null && i < p.thumbs.length ? p.thumbs[i] : null, "page:" + i);
            card.setOnClickListener(v -> {
                if (p.checks != null) {   // a tick list: each one flips where it is, saved by Back
                    p.checks[k] = !p.checks[k];
                    p.at = k;
                    if (p.many != null) p.many.onClick(d, k, p.checks[k]);
                    if (page == p) render();
                    return;
                }
                if (p.one != null) p.one.onClick(d, k); else if (p.item != null) p.item.onClick(d, k);
                if (page == p) closePage(false); else render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = cs.px(CouchStyle.ROW_GAP);
            list.addView(card, lp);
            if (i == (p.at >= 0 ? p.at : Math.max(0, p.checked))) first = card;
        }
        final View f = first;
        if (f != null) f.post(f::requestFocus);
    }

    /** couch-tv has no search: ours opens from the round button, in its typing box, the rows found as the words are typed */
    private void openSearch() {
        searchIn = null;
        page = null;
        open(SettingsHub.SEARCH);
    }

    private LinearLayout searchFound;
    private final android.os.Handler searchH = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable searchNow = this::searchList;

    private void searchRows() {
        boolean fresh = searchIn == null;
        if (fresh) {
            searchIn = new EditText(this);
            searchIn.setSingleLine(true);
            searchIn.setInputType(InputType.TYPE_CLASS_TEXT);
            searchIn.setBackground(null);
            searchIn.setPadding(0, 0, 0, 0);
            searchIn.setTextColor(CouchStyle.WHITE);
            searchIn.setHintTextColor(CouchStyle.DIM);
            searchIn.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.TYPE_TEXT));
            searchIn.setTypeface(font("Thmanyah-Medium"));
            searchIn.setTextDirection(Dir.text());
            searchIn.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence c, int a, int b, int e) { }
                @Override public void onTextChanged(CharSequence c, int a, int b, int e) { }
                @Override public void afterTextChanged(android.text.Editable e) { searchH.removeCallbacks(searchNow); searchH.postDelayed(searchNow, SEARCH_MS); }
            });
        }
        if (searchIn.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) searchIn.getParent()).removeView(searchIn);
        LinearLayout box = new LinearLayout(this);   // couch-tv's typing box: a light ground, a white edge
        box.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable g = cs.round(CouchStyle.TYPE_BG, CouchStyle.ROW_RADIUS);
        g.setStroke(cs.px(0.1875f), CouchStyle.WHITE);
        box.setBackground(g);
        box.setPaddingRelative(cs.px(CouchStyle.PAD), cs.px(0.875f), cs.px(CouchStyle.PAD), cs.px(1f));
        TextView label = new TextView(this);
        label.setText(conv("اكتب كلمة، مثل: صوت، لون، مدينة"));
        label.setTextColor(0xB3FFFFFF);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.TYPE_LABEL));
        label.setTypeface(font("Thmanyah-Medium"));
        label.setTextDirection(Dir.text());
        box.addView(label);
        box.addView(searchIn, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
        bl.bottomMargin = cs.px(1.25f);
        list.addView(box, bl);
        searchFound = new LinearLayout(this);
        searchFound.setOrientation(LinearLayout.VERTICAL);
        list.addView(searchFound, new LinearLayout.LayoutParams(-1, -2));
        searchList();
        if (fresh) searchIn.post(searchIn::requestFocus);
    }

    /** the rows found now, each saying its section; OK opens that section on that row */
    private void searchList() {
        if (searchFound == null || searchIn == null) return;
        searchFound.removeAllViews();
        String q = searchIn.getText().toString().trim();
        if (q.isEmpty()) return;
        List<String[]> found = Parts.visible(SettingsHub.search(q), prefs.partsOff());
        for (int i = 0; i < Math.min(found.size(), 12); i++) {
            final String[] r = found.get(i);
            LinearLayout card = couchCard(tx(r[0]), null, tx(SettingsHub.name(r[1])), CouchStyle.Mark.CHEVRON, null, "found:" + r[0]);
            card.setOnClickListener(v -> { searchIn = null; hubAt = r[1]; forceWant = r[0]; open(r[1]); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = cs.px(CouchStyle.ROW_GAP);
            searchFound.addView(card, lp);
        }
    }

    private static final Object SIDE = new Object();
    static final long SIDE_MS = 120;
    private Runnable sideOpen = () -> { };

    /** Each button's icon. */
    private static int hubIcon(String cat) {
        switch (cat) {
            case SettingsHub.SEARCH: return R.drawable.ic_search;
            case SettingsHub.LANG: return R.drawable.ic_language;
            case SettingsHub.PHOTOS: return R.drawable.ic_eye;
            case SettingsHub.LOOK: return R.drawable.ic_category;
            case SettingsHub.APPS: return R.drawable.ic_star;
            case SettingsHub.PRAY: return R.drawable.ic_listen;
            case SettingsHub.CONTENT: return R.drawable.ic_trophy;
            case SettingsHub.SAVER: return R.drawable.ic_screensaver;
            case SettingsHub.FAMILY: return R.drawable.ic_lock;
            case SettingsHub.UPDATE: return R.drawable.ic_sync;
            default: return R.drawable.ic_settings;
        }
    }

    /** What is set now in a category, on one line under its name ("" for nothing to say). */
    private String hubSummary(String cat) {
        switch (cat) {
            case SettingsHub.SEARCH: return "اكتب كلمة وانتقل إلى الصف";
            case SettingsHub.LANG: return langLabel() + "، " + Words.city(prefs.city());
            case SettingsHub.PHOTOS: return "صوري " + arabicNum(Own.count(this)) + "، " + packsLabel();
            case SettingsHub.LOOK: return Prefs.lookLabel(prefs.look()) + "، " + Prefs.appearanceLabel(prefs.appearance());
            case SettingsHub.APPS: return "المفضلة والمخفية والأزرار";
            case SettingsHub.PRAY: {
                Boolean on = homeAdhanSeen();   // the home's switch as last known; null when the home cannot say (not installed, or older)
                String m = PrayerMethod.label(prefs.prayerMethod(), prefs.cityCountry(), prefs.cityTz());
                return on == null ? m : "الأذان " + (on ? OnOff.ON_WORD : OnOff.OFF_WORD) + "، " + m;
            }
            case SettingsHub.CONTENT: return "الفرق " + arabicNum(prefs.teams().size()) + "، الأسواق " + (prefs.marketsOn() ? arabicNum(prefs.markets().size()) : "مخفية");
            case SettingsHub.SAVER: return idleSummary();
            case SettingsHub.FAMILY: return "الرقابة الأبوية والجوالات";
            case SettingsHub.UPDATE: return Updater.versionLines(this)[0];
            default: return "";
        }
    }

    /**
     * The home's adhan switch (its «الأذان التلقائي»), read from the home (NowPlayingProvider .../adhan_on, the same permission as «يُتلى الآن»):
     * null when the home cannot say (not installed, older than this question, or it refused).
     */
    private Boolean adhanSeen;       // what the hub shows: the last answer (kept on disk too), so opening the settings never waits for the other app
    private boolean adhanAsked;      // asked once per time the settings come to the front

    /**
     * The adhan tile's state without a call to the home on the UI thread (the call may start the home's process: seconds on a weak TV): the copy kept from the
     * last time, and one question asked on a thread each time the settings come to the front; the tile is drawn again only when the answer differs.
     */
    private Boolean homeAdhanSeen() {
        if (adhanSeen == null) {
            int v = getSharedPreferences("hub_seen", MODE_PRIVATE).getInt("adhan_on", -1);
            adhanSeen = v < 0 ? null : v == 1;
        }
        if (!adhanAsked) {
            adhanAsked = true;
            new Thread(() -> {
                final Boolean now = homeAdhanOn();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    boolean changed = !java.util.Objects.equals(now, adhanSeen);
                    adhanSeen = now;
                    getSharedPreferences("hub_seen", MODE_PRIVATE).edit().putInt("adhan_on", now == null ? -1 : now ? 1 : 0).apply();
                    if (changed) render();
                });
            }, "hub-adhan").start();
        }
        return adhanSeen;
    }

    private String prayersSeen;        // the five states as the home last said them ("00200"), kept on disk too
    private boolean prayersAsked;

    /** «الأذان لكل صلاة»'s value: «كلها صوت وشاشة» when no prayer differs, else the first that does («الفجر: شاشة فقط»), or the count («٢ مختلفة»); asked as the adhan's is */
    private String prayersValue() {
        if (prayersSeen == null) prayersSeen = getSharedPreferences("hub_seen", MODE_PRIVATE).getString("adhan_prayers", "");
        if (!prayersAsked) {
            prayersAsked = true;
            new Thread(() -> {
                String got = "";
                android.database.Cursor c = null;
                try {
                    c = getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/adhan_prayers"), null, null, null, null);
                    if (c != null && c.moveToFirst() && c.getColumnIndex("states") >= 0) got = c.getString(c.getColumnIndex("states"));
                } catch (RuntimeException ignored) {
                } finally { if (c != null) c.close(); }
                final String answer = got == null ? "" : got;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || answer.isEmpty()) return;
                    boolean changed = !answer.equals(prayersSeen);
                    prayersSeen = answer;
                    getSharedPreferences("hub_seen", MODE_PRIVATE).edit().putString("adhan_prayers", answer).apply();
                    if (changed) render();
                });
            }, "hub-prayers").start();
        }
        String v = PrayerSummary.of(prayersSeen, n -> DigitsText.convert(String.valueOf(n), latinDigits));
        return v != null ? v : "الصوت والشاشة لكل صلاة على حدة";   // the home has not answered yet
    }

    private final java.util.Map<String, String> homeSeen = new java.util.HashMap<>();   // the home's last answers by path, kept on disk too (hub_seen)
    private final java.util.Set<String> homeAsked = new java.util.HashSet<>();

    /**
     * A value the launcher keeps (its provider's path, one column), for a row's second line: the last answer at once (kept on disk), one question on a thread each time
     * the settings come to the front, the row drawn again only when the answer is another. `fallback` while the home has not answered.
     */
    private String homeValue(final String path, final String column, String fallback) {
        String seen = homeSeen.get(path);
        if (seen == null) { seen = getSharedPreferences("hub_seen", MODE_PRIVATE).getString("home_" + path, ""); homeSeen.put(path, seen); }
        if (homeAsked.add(path)) {
            new Thread(() -> {
                String got = "";
                android.database.Cursor c = null;
                try {
                    c = getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/" + path), null, null, null, null);
                    if (c != null && c.moveToFirst() && c.getColumnIndex(column) >= 0) got = c.getString(c.getColumnIndex(column));
                } catch (RuntimeException ignored) {
                } finally { if (c != null) c.close(); }
                final String answer = got == null ? "" : got;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || answer.isEmpty()) return;
                    boolean changed = !answer.equals(homeSeen.get(path));
                    homeSeen.put(path, answer);
                    getSharedPreferences("hub_seen", MODE_PRIVATE).edit().putString("home_" + path, answer).apply();
                    if (changed) render();
                });
            }, "hub-" + path).start();
        }
        return seen.isEmpty() ? fallback : seen;
    }

    /** «الأدعية والتذكيرات»: «جديد من الجوال: N» while duas added from «صوتياتي» wait to be timed on the TV, else what the row is */
    private String duasValue() {
        String n = homeValue("duas_new", "n", "");
        int k = 0;
        try { k = Integer.parseInt(n); } catch (NumberFormatException ignored) { }
        return k > 0 ? "جديد من الجوال: " + DigitsText.convert(String.valueOf(k), latinDigits) : "أدعية وتذكيرات بوقتها، بصوتك أو بصوتنا";
    }

    private Boolean cleanSeen, factorySeen;   // «UrTV النظيف» and «واجهة المصنع» off, as the home last said
    private boolean cleanAsked;

    /** «UrTV النظيف» is on in the home: the last answer at once, one question on a thread each time the settings come to the front (as the adhan's) */
    private Boolean homeCleanSeen() {
        if (cleanSeen == null) {
            int v = getSharedPreferences("hub_seen", MODE_PRIVATE).getInt("clean_on", -1);
            cleanSeen = v < 0 ? null : v == 1;
            int f = getSharedPreferences("hub_seen", MODE_PRIVATE).getInt("factory_on", -1);
            factorySeen = f < 0 ? null : f == 1;
        }
        if (!cleanAsked) {
            cleanAsked = true;
            new Thread(() -> {
                Boolean now = null, fact = null;
                android.database.Cursor c = null;
                try {
                    c = getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/clean_on"), null, null, null, null);
                    if (c != null && c.moveToFirst() && c.getColumnIndex("on") >= 0) now = c.getInt(c.getColumnIndex("on")) == 1;
                    if (c != null && c.getColumnIndex("factory") >= 0) fact = c.getInt(c.getColumnIndex("factory")) == 1;
                } catch (RuntimeException ignored) {
                } finally { if (c != null) c.close(); }
                final Boolean answer = now, factAnswer = fact;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    boolean changed = !java.util.Objects.equals(answer, cleanSeen) || !java.util.Objects.equals(factAnswer, factorySeen);
                    cleanSeen = answer;
                    factorySeen = factAnswer;
                    getSharedPreferences("hub_seen", MODE_PRIVATE).edit().putInt("clean_on", answer == null ? -1 : answer ? 1 : 0).putInt("factory_on", factAnswer == null ? -1 : factAnswer ? 1 : 0).apply();
                    if (changed) render();
                });
            }, "hub-clean").start();
        }
        return cleanSeen;
    }

    // 4.4 (QA-7, R1): «زر الرئيسية يفتح تلفزيونك مباشرة» is the home's (and its add-on's) answer: asked once per return on a thread, kept, and the
    // list is built again only when it changed (render asked the home on the UI thread at every open, return and toggle)
    private Boolean homeKeyKnown;
    private boolean homeKeyAsked;

    // 4.4 beta 3: «الوضع الخفيف»'s line from the home (LiteAutoStore.rowLine): asked once per return on a thread, kept, the list built again when it changed
    private String liteLineKnown;
    private boolean liteLineAsked;

    private String liteLineSeen() {
        if (!liteLineAsked) {
            liteLineAsked = true;
            final android.content.Context app = getApplicationContext();
            new Thread(() -> {
                final String now = NowPlayingBridge.liteLine(app);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || java.util.Objects.equals(now, liteLineKnown)) return;
                    liteLineKnown = now;
                    render();
                });
            }, "lite-line").start();
        }
        return liteLineKnown;
    }

    private Boolean homeKeySeen() {
        if (!homeKeyAsked) {
            homeKeyAsked = true;
            final android.content.Context app = getApplicationContext();
            new Thread(() -> {
                final Boolean now = NowPlayingBridge.homeKey(app, null);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || java.util.Objects.equals(now, homeKeyKnown)) return;
                    homeKeyKnown = now;
                    render();
                });
            }, "home-key").start();
        }
        return homeKeyKnown;
    }

    /** the row pressed: the home is told on a thread, and its answer drawn */
    private void setHomeKey(final boolean on) {
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            final Boolean now = NowPlayingBridge.homeKey(app, on ? "on" : "off");
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (now != null) homeKeyKnown = now;
                render();
            });
        }, "home-key-set").start();
    }

    private Boolean homeAdhanOn() {
        android.database.Cursor c = null;
        try {
            c = getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/adhan_on"), null, null, null, null);
            if (c == null || !c.moveToFirst() || c.getColumnIndex("on") < 0) return null;
            return c.getInt(c.getColumnIndex("on")) == 1;
        } catch (RuntimeException e) {
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    /** The mark of an on/off value: the three triangles, shown while it is on, an empty place of the same size otherwise (the icons stay in line). */
    private ImageView onMark(int state) {
        ImageView mark = new ImageView(this);
        mark.setImageDrawable(getDrawable(R.drawable.ic_on).mutate());
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setVisibility(state == 1 ? View.VISIBLE : View.INVISIBLE);
        return mark;
    }

    /** An on/off value's word in the green that reads on what is under it (the panel at rest, the focus fill while focused), and its mark the same. */
    private void paintState(TextView v, ImageView mark, String text, int state, boolean focused) {
        // 4.4 smoother: the text is set once; a focus change only recolours (a new text on each move made the row measured and laid out again)
        if (state < 0) { if (!text.isEmpty() && !text.contentEquals(v.getText())) v.setText(text); return; }
        int bg = focused ? look.focus : (look.light ? 0xFFFFFFFF : 0xFF000000);
        int green = OnOff.green(bg);
        if (mark.getDrawable() != null) mark.getDrawable().setTint(green);
        if (state == 0) { if (!text.contentEquals(v.getText())) v.setText(text); return; }
        CharSequence now = v.getText();
        Green[] g = now instanceof android.text.Spanned && text.contentEquals(now) ? ((android.text.Spanned) now).getSpans(0, now.length(), Green.class) : null;
        if (g != null && g.length == 1) { if (g[0].color != green) { g[0].color = green; v.invalidate(); } return; }
        android.text.SpannableString sp = new android.text.SpannableString(text);
        sp.setSpan(new Green(green), 0, OnOff.wordEnd(text), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        v.setText(sp);
    }

    /** the on/off word's green: its colour changed in place (drawn again, not laid out again) */
    private static final class Green extends android.text.style.CharacterStyle implements android.text.style.UpdateAppearance {
        int color;
        Green(int color) { this.color = color; }
        @Override public void updateDrawState(android.text.TextPaint p) { p.setColor(color); }
    }

    static final float FOCUS_SCALE = 1.02f;
    /** a row's one text size and its padding (the owner, build 413: one line, at most about 80 px at 1080p, so 5 or 6 rows show) */
    static final int ROW_SP = 18, ROW_PAD = 8;
    /** the hint at the top: the size of a row's secondary line (its value), not smaller */
    static final int HINT_SP = 14;
    static final long FOCUS_MS = Motion.MS;   // 4.4 the same easing everywhere (Motion)

    /** The rows of the list now (the views row() made: their tag is their FocusKey; the categories' tiles, two to a line), in order. */
    private List<View> rows() {
        List<View> out = new ArrayList<>();
        for (int i = 0; i < list.getChildCount(); i++) {
            View c = list.getChildAt(i);
            if (c.getTag() instanceof String) out.add(c);
            else if (c instanceof LinearLayout && c.getTag() == null)
                for (int j = 0; j < ((LinearLayout) c).getChildCount(); j++)
                    if (((LinearLayout) c).getChildAt(j).getTag() instanceof String) out.add(((LinearLayout) c).getChildAt(j));
        }
        return out;
    }

    /** The row (or tile) that has the focus in this window (a dialog over it leaves it there), or null. */
    private View focusedRow() {
        List<View> all = rows();
        for (View v = getCurrentFocus(); v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null)
            if (all.contains(v)) return v;
        return null;
    }

    /** After a rebuild: back to where the list was, then the focus on the same row (FocusKey.pick), once the layout has run. */
    private void restoreFocus(final String want, final int wasIndex, final int scrollY) {
        pendingFocus = want;
        scroll.post(() -> {
            List<View> now = rows();
            List<String> keys = new ArrayList<>();
            for (View r : now) keys.add((String) r.getTag());
            int at = want == null ? (now.isEmpty() ? -1 : 0) : FocusKey.pick(keys, want, wasIndex);   // nothing kept: the first
            if (at < 0) return;
            scroll.scrollTo(0, scrollY);
            now.get(at).requestFocus();
            if (hasWindowFocus()) pendingFocus = null;   // done; with a dialog still open, again when the window has the focus back
        });
    }

    @Override
    public void onWindowFocusChanged(boolean has) {
        super.onWindowFocusChanged(has);
        if (!has || pendingFocus == null) return;
        final String want = pendingFocus;
        pendingFocus = null;
        for (View r : rows()) if (want.equals(r.getTag())) { r.requestFocus(); break; }
    }

    // ---------- pickers ----------

    /** «الدولة»: a filter, not a setting of its own; it opens the city list of that country (its regions, or its cities) and its search. */
    private void pickCountry() {
        String[] labels = new String[Location.COUNTRIES.length];
        for (int i = 0; i < labels.length; i++) labels[i] = Location.COUNTRIES[i][1];
        new Dlg(this).setTitle("الدولة")
            .setSingleChoiceItems(labels, Location.countryOf(prefs.cityCountry(), prefs.cityTz()), (d, i) -> { d.dismiss(); pickCity(i); })
            .show();
    }

    private void pickCity() { pickCity(-1); }

    /** country: an index of Location.COUNTRIES, or -1 for every region of the list and a search of all the searchable countries */
    private void pickCity(final int country) {
        final Cities.Region[] regions = country < 0 ? Cities.REGIONS : Location.regions(country);
        if (country > 0 && regions.length == 1) { pickCityIn(regions[0], country); return; }   // any other country is one region: its cities
        String[] labels = new String[regions.length + 1];
        labels[0] = "بحث عن مدينة أو قرية";
        for (int i = 0; i < regions.length; i++) labels[i + 1] = regions[i].name + "  (" + DigitsText.convert(String.valueOf(regions[i].cities.length), latinDigits) + ")";   // the counts in the digits of the setting
        new Dlg(this).setTitle(country < 0 ? "المنطقة" : Location.COUNTRIES[country][1])
            .setItems(labels, (d, i) -> { if (i == 0) searchCity(country); else pickCityIn(regions[i - 1], country); })
            .show();
    }

    private void pickCityIn(final Cities.Region r, final int country) {
        String[] names = new String[r.cities.length];
        int sel = -1;
        for (int i = 0; i < names.length; i++) {
            names[i] = Words.display(r.cities[i]);
            if (r.cities[i][0].equals(prefs.city()[0]) && r.cities[i][1].equals(prefs.city()[1]) && r.cities[i][2].equals(prefs.city()[2])) sel = i;
        }
        AlertDialog.Builder b = new Dlg(this).setTitle(r.name)
            .setSingleChoiceItems(names, sel, (d, i) -> {
                prefs.setCity(r.cities[i][0], r.cities[i][1], r.cities[i][2], CityMatch.zone(r.cities[i]), r.country);
                d.dismiss();
                render();
            });
        if (country > 0 && Location.searchable(country)) b.setNeutralButton("بحث", (d, w) -> searchCity(country));   // a place not in the list, in that country (not Palestine: PrayerMethod)
        b.show();
    }

    /**
     * 4.3.3: the offline list first (CitySearch: the built-in cities and assets/cities-more.txt), read off the main thread; its last row asks the
     * online search. Nothing offline: the online search at once, as before.
     */
    private void searchCity(final int country) {
        ask("اسم المدينة أو القرية", q -> {
            final String code = country < 0 ? "" : Location.COUNTRIES[country][0];
            toast("جارٍ البحث…");   // the first search reads the list: a second or two on a weak TV
            new Thread(() -> {
                final List<CitiesMore.Place> found = CitySearch.find(q, code, 30);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (found.isEmpty()) { searchOnline(country, q); return; }
                    final int n = found.size();
                    boolean online = country < 0 || Location.searchable(country);
                    String[] rows = new String[n + (online ? 1 : 0)];
                    for (int i = 0; i < n; i++) rows[i] = placeRow(found.get(i).row);
                    if (online) rows[n] = "ابحث على الإنترنت…";
                    new Dlg(this).setTitle("اختر المكان").setItems(rows, (d, i) -> {
                        if (i >= n) { searchOnline(country, q); return; }
                        String[] c = found.get(i).row;
                        prefs.setCity(c[0], c[1], c[2], c[3], c[5]);
                        render();
                    }).show();
                });
            }).start();
        });
    }

    /** a row of the offline results: «أبها · Abha  ·  السعودية», with English «Abha  ·  Saudi Arabia» */
    private static String placeRow(String[] c) {
        String land = S.en() ? Places.country(c[5]) : Location.countryLabel(c[5], c[3]);
        if (S.en()) return (c[4] != null && !c[4].isEmpty() ? c[4] : c[0]) + (land == null ? "" : S.sep() + land);
        return c[0] + (c[4] != null && !c[4].isEmpty() ? S.sep() + c[4] : "") + (land == null ? "" : S.sep() + land);
    }

    // Any searchable country, or the one country picked, from Open-Meteo's GeoNames search.
    // Without a country filter, ask for the world's hundred best matches; PS keeps its checked list.
    private void searchOnline(final int country, final String q) {
        fetch("https://geocoding-api.open-meteo.com/v1/search?count=100&language=" + (S.en() ? "en" : "ar") + "&name="
            + Uri.encode(q) + (country < 0 ? "" : "&countryCode=" + Location.COUNTRIES[country][0]), json -> {
            JSONArray all = new JSONObject(json).optJSONArray("results"), a = new JSONArray();
            for (int k = 0; all != null && k < all.length() && a.length() < 20; k++)
                if (PrayerMethod.searchable(all.getJSONObject(k).optString("country_code"))) a.put(all.getJSONObject(k));
            if (a.length() == 0) { toast("لا توجد نتائج"); return; }
            final int n = a.length();
            final String[] names = new String[n], lats = new String[n], lons = new String[n], zones = new String[n], countries = new String[n], rows = new String[n];
            for (int i = 0; i < n; i++) {
                JSONObject c = a.getJSONObject(i);
                names[i] = c.optString("name");
                countries[i] = c.optString("country_code");
                lats[i] = String.valueOf(c.optDouble("latitude"));
                lons[i] = String.valueOf(c.optDouble("longitude"));
                zones[i] = c.optString("timezone", "");   // the place's own; empty: the TV's (Prefs.cityTz)
                String region = c.optString("admin1", "");
                String land = c.optString("country", "");   // several countries now: the row says which
                rows[i] = names[i] + (region.isEmpty() ? "" : S.sep() + region) + (land.isEmpty() ? "" : S.sep() + land);
            }
            new Dlg(this).setTitle("اختر المكان")
                .setItems(rows, (d, i) -> { prefs.setCity(names[i], lats[i], lons[i], zones[i], countries[i]); render(); }).show();
        });
    }

    /** 4.4 «فوق تطبيق آخر»: the notice over another app is drawn by the add-on «إضافات تلفزيونك» (the home holds no overlay permission: Play Protect). Without the
     *  add-on the person is told so on screen and its offer opens (the home's own screen); with it, the TV's «الظهور فوق التطبيقات» screen for the add-on;
     *  a TV that hides it: the computer's command, said once */
    private void askOverlay() {
        if (Boolean.FALSE.equals(extrasHere)) { offerExtras(); return; }
        Intent i = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:sa.urtv.extras"));
        try {
            if (getPackageManager().resolveActivity(i, 0) != null) { startActivity(i); return; }
        } catch (RuntimeException ignored) { }
        toast("هذا التلفزيون يخفي هذا الإذن، فيُعطى من الكمبيوتر");
    }

    /** 4.4 #42 (R-473): the plain request; «اسمح» opens the TV's own screen for this app (a TV that hides it: the computer's command, said) */
    private void askSaverOverlay() {
        prefs.setOverlayAsked();
        new Dlg(this).setTitle(OverlayNeed.TITLE).setMessage(OverlayNeed.WHY + "\n" + OverlayNeed.HOW)
            .setPositiveButton("اسمح", (d, w) -> {
                Intent i = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName()));
                try {
                    if (getPackageManager().resolveActivity(i, 0) != null) { startActivity(i); return; }
                } catch (RuntimeException ignored) { }
                toast(OverlayNeed.HIDDEN);
            })
            .setNegativeButton("مو الحين", null)
            .show();
    }

    /** «إضافات تلفزيونك» is installed (only read); never in a render: extrasLook asks on a thread */
    private static boolean extrasInstalled(android.content.pm.PackageManager pm) {
        try { pm.getPackageInfo("sa.urtv.extras", 0); return true; } catch (Exception e) { return false; }
    }

    /** the last answer of extrasLook: null = not asked yet (the row then says only «يعمل»; the add-on's own permission is not known here) */
    private volatile Boolean extrasHere;

    private void extrasLook() {
        final android.content.pm.PackageManager pm = getPackageManager();
        new Thread(() -> {
            final boolean here = extrasInstalled(pm);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                Boolean was = extrasHere;
                extrasHere = here;
                if (was == null || was != here) render();
            });
        }, "extras-look").start();
    }

    /** the home's offer of the add-on (install it), told what it is for; the home opens it only for this app (signature permission, same key) */
    private void offerExtras() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.ExtrasActivity").putExtra("sa.urtv.home.EXTRAS_FOR", "overlay"));
            toast("«فوق تطبيق آخر» يحتاج «إضافات تلفزيونك»: بنفتح لك تثبيتها");
        } catch (RuntimeException e) {
            toast("«فوق تطبيق آخر» يحتاج «إضافات تلفزيونك»، ولم نقدر نفتح تثبيتها");
        }
    }

    /** Followed teams: untick to remove, or add one (presets, league browsing, search). Up to 20. */
    private void manageTeams() {
        warmTeams();   // 4.4 beta 2: the club list read now, off the UI thread, so «إضافة فريق» opens and searches without a freeze
        final List<String> cur = prefs.teams();
        String[] labels = new String[cur.size()];
        final boolean[] keep = new boolean[cur.size()];
        for (int i = 0; i < cur.size(); i++) { labels[i] = teamName(cur.get(i)); keep[i] = true; }
        AlertDialog.Builder b = new Dlg(this).multi(() -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < cur.size(); i++) if (keep[i]) v.add(cur.get(i));
                prefs.setList("teams", v);
                render();
            })
            .setTitle(cur.isEmpty() ? "لا توجد فرق متابعة" : "الفرق المتابعة (ألغِ التحديد للحذف)");
        b.setNeutralButton("إضافة فريق", (d, w) -> askFollow(FollowLimit.TEAMS, () -> pickTeam(0)));   // 4.4 at the ceiling it says so, never a missing button
        if (!cur.isEmpty()) b.setMultiChoiceItems(labels, keep, (d, i, checked) -> keep[i] = checked);
        b.show();
    }

    private void addTeam(String id, String name) {
        for (String t : prefs.teams()) if (t.startsWith(id + "|")) { toast("متابَع مسبقاً"); return; }
        askFollow(FollowLimit.TEAMS, () -> {
            if (prefs.addTeam(id, name)) toast("تمت متابعة " + name);
            render();
        });
    }

    /**
     * 4.4 one more league or club (FollowLimit): at once up to the soft limit; past it, the first time only, the warning that a weak TV may
     * slow down with «أضف» / «لا» («أضف» is remembered, never asked again); at the ceiling the words that say so (never a silent no).
     */
    private void askFollow(final String kind, final Runnable go) {
        int st = prefs.followState(kind);
        if (st == FollowLimit.ADD) { go.run(); return; }
        if (st == FollowLimit.FULL) { new Dlg(this).setTitle(FollowLimit.full(kind, prefs.teamCap())).setPositiveButton("حسنًا", null).show(); return; }
        new Dlg(this).setTitle(FollowLimit.warning(kind))
            .setPositiveButton("أضف", (d, w) -> { prefs.setFollowWarned(kind); go.run(); })
            .setNegativeButton("لا", null)
            .show();
    }

    /** 4.4 the owner (R-472) «حد الفرق»: 40, 80 or none; a higher one only after «كثرة الفرق ممكن تبطّئ التلفزيون» and «ارفع الحد» (lower: at once, no club removed) */
    private void pickTeamCap() {
        final int from = prefs.teamCap();
        int at = 0;
        for (int k = 0; k < FollowLimit.TEAM_CAPS.length; k++) if (FollowLimit.TEAM_CAPS[k] == from) at = k;
        pickOne("حد الفرق", FollowLimit.TEAM_CAP_NAMES, at, k -> {
            final int to = FollowLimit.TEAM_CAPS[k];
            if (!FollowLimit.raises(from, to)) { prefs.setTeamCap(to); return; }
            new Dlg(this).setTitle(FollowLimit.TEAM_CAP_WARNING)
                .setPositiveButton("ارفع الحد", (d, w) -> { prefs.setTeamCap(to); render(); })
                .setNegativeButton("لا", null)
                .show();
        });
    }

    /** 4.4 the same in a list of ticks (the count is the dialog's own): past the soft limit the tick waits for the warning's «أضف», at the ceiling it stays off */
    private void tickLeague(final AlertDialog d, final boolean[] on, final int i, int elsewhere) {
        int before = elsewhere - 1;   // the ones ticked before this one (and those followed outside this list)
        for (boolean b : on) if (b) before++;
        int st = FollowLimit.check(FollowLimit.LEAGUES, before, prefs.followWarned(FollowLimit.LEAGUES));
        if (st == FollowLimit.ADD) return;
        on[i] = false;
        d.getListView().setItemChecked(i, false);
        if (st == FollowLimit.FULL) { new Dlg(this).setTitle(FollowLimit.full(FollowLimit.LEAGUES)).setPositiveButton("حسنًا", null).show(); return; }
        new Dlg(this).setTitle(FollowLimit.warning(FollowLimit.LEAGUES))
            .setPositiveButton("أضف", (x, w) -> { prefs.setFollowWarned(FollowLimit.LEAGUES); on[i] = true; d.getListView().setItemChecked(i, true); })
            .setNegativeButton("لا", null)
            .show();
    }

    private void pickTeam(final int slot) {
        final List<String[]> opts = new ArrayList<>();
        for (String[] t : Prefs.TEAM_PRESETS) opts.add(t);
        String[] labels = new String[opts.size() + 2];
        for (int i = 0; i < opts.size(); i++) labels[i] = teamName(opts.get(i)[0] + "|" + opts.get(i)[1]);
        labels[opts.size()] = "تصفح الدوريات";
        labels[opts.size() + 1] = "بحث عن أي فريق أو منتخب";
        new Dlg(this).setTitle("إضافة فريق")
            .setItems(labels, (d, i) -> {
                if (i < opts.size()) addTeam(opts.get(i)[0], opts.get(i)[1]);
                else if (i == opts.size()) pickLeague(slot);
                else searchTeam(slot);
            }).show();
    }

    /** Our bundled list first (every club of these leagues, offline); TheSportsDB only if that is missing. */
    private void pickLeague(final int slot) {
        final String[] labels = new String[Prefs.LEAGUES.length + 1];
        for (int i = 0; i < Prefs.LEAGUES.length; i++) labels[i] = Prefs.LEAGUES[i][1];
        labels[Prefs.LEAGUES.length] = "المنتخبات";
        new Dlg(this).setTitle("الدوري")
            .setItems(labels, (d, i) -> {
                final String league = i < Prefs.LEAGUES.length ? Prefs.LEAGUES[i][0] : Teams.NATIONAL;
                final android.content.Context app = getApplicationContext();
                new Thread(() -> {
                    final List<TeamSearch.Team> clubs = Teams.inLeague(app, league);   // 4.4 beta 2: read off the UI thread
                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        if (!clubs.isEmpty()) showOurs(clubs, labels[i], false);
                        else if (i < Prefs.LEAGUES.length)
                            fetch(SPORTS + "search_all_teams.php?l=" + Uri.encode(league), json -> showTeams(slot, json, labels[i], league));
                        else toast("ما قدرنا نحمّل القائمة، جرّب تدوّر على الفريق");
                    });
                }, "teams-league").start();
            }).show();
    }

    /** 4.4 beta 2 (the owner, build 570: «the search between clubs lags»): the club list (teams.json) read and searched off the UI thread */
    private static void warmTeams(final android.content.Context c) { new Thread(() -> Teams.all(c), "teams-load").start(); }
    private void warmTeams() { warmTeams(getApplicationContext()); }

    private void searchTeam(final int slot) {
        ask("اسم الفريق بالعربية أو الإنجليزية (مثل الهلال أو \u2066Real Madrid\u2069)", q -> {
            final android.content.Context app = getApplicationContext();
            new Thread(() -> {
                final List<TeamSearch.Team> found = Teams.search(app, q);   // the list read and searched here, never on the UI thread
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (!found.isEmpty()) showOurs(found.subList(0, Math.min(found.size(), 30)), "اختر الفريق", true);
                    else if (!Arabic.hasArabic(q))   // a club outside our list: TheSportsDB's search, in English only
                        fetch(SPORTS + "searchteams.php?t=" + Uri.encode(q), json -> showTeams(slot, json, "اختر الفريق", null));
                    else toast("لا توجد نتائج، جرّب اسم الفريق بالإنجليزية");
                });
            }, "team-search").start();
        });
    }

    /** Clubs from our list: their Arabic names are already there, so following one needs no network. */
    private void showOurs(final List<TeamSearch.Team> clubs, String title, boolean withLeague) {
        String[] rows = new String[clubs.size()];
        for (int i = 0; i < rows.length; i++) {
            TeamSearch.Team t = clubs.get(i);
            rows[i] = Teams.label(this, t);
            if (withLeague && t.leagues.length > 0)
                rows[i] += S.sep() + (Teams.NATIONAL.equals(t.leagues[0]) ? "منتخب" : leagueAr(t.leagues[0]));
        }
        new Dlg(this).setTitle(title)
            .setItems(rows, (d, i) -> {
                TeamSearch.Team t = clubs.get(i);
                addTeam(t.id, t.ar.isEmpty() ? Arabic.translit(t.en) : t.ar);
            }).show();
    }

    private static final String SPORTS = "https://www.thesportsdb.com/api/v1/json/123/";

    /** TheSportsDB's answer, when our list has nothing. league != null: keep only clubs that really play in it. */
    private void showTeams(final int slot, String json, String title, String league) throws Exception {
        JSONArray all = new JSONObject(json).optJSONArray("teams");
        JSONArray a = new JSONArray();
        for (int i = 0; all != null && i < all.length(); i++) {
            JSONObject t = all.getJSONObject(i);
            if (!"Soccer".equalsIgnoreCase(t.optString("strSport", "Soccer"))) continue;
            boolean inLeague = league == null;
            for (int k = 1; !inLeague && k <= 7; k++)
                inLeague = league.equalsIgnoreCase(t.optString(k == 1 ? "strLeague" : "strLeague" + k));
            if (inLeague) a.put(t);
        }
        if (a.length() == 0) {
            toast(league == null ? "لا توجد نتائج" : "تعذّر تحميل هذا الدوري، جرّب البحث عن الفريق");
            return;
        }
        final int n = Math.min(a.length(), 60);
        final String[] ids = new String[n], names = new String[n], rows = new String[n];
        final TeamSearch.Team[] ours = new TeamSearch.Team[n];
        for (int i = 0; i < n; i++) {
            JSONObject t = a.getJSONObject(i);
            ids[i] = t.optString("idTeam");
            names[i] = t.optString("strTeam");
            ours[i] = Teams.byId(this, ids[i]);
            rows[i] = (ours[i] != null ? Teams.label(this, ours[i]) : Arabic.team(this, names[i]))
                + S.sep() + leagueAr(t.optString("strLeague", ""));
        }
        new Dlg(this).setTitle(title)
            .setItems(rows, (d, i) -> {
                if (ours[i] != null && !ours[i].ar.isEmpty()) addTeam(ids[i], ours[i].ar);
                else inArabic(names[i], Arabic.TEAM, ar -> addTeam(ids[i], ar));
            })
            .show();
    }

    private void pickMany(final String key, String title, final String[][] options) {
        pickMany(key, title, options, "بدون اختيار = الكل");
    }

    private void pickMany(final String key, String title, final String[][] options, String hint) {
        List<String> cur = prefs.list(key);
        String[] labels = new String[options.length];
        final boolean[] on = new boolean[options.length];
        for (int i = 0; i < options.length; i++) { labels[i] = options[i][1]; on[i] = cur.contains(options[i][0]); }
        new Dlg(this).multi(() -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < options.length; i++) if (on[i]) v.add(options[i][0]);
                prefs.setList(key, v);
                render();
            })
            .setTitle(title + " (" + hint + ")")
            .setMultiChoiceItems(labels, on, (d, i, checked) -> on[i] = checked)
            .show();
    }

    /** Whole leagues: every game of every club in them is shown, up to Prefs.MAX_LEAGUES leagues (past Prefs.SOFT_LEAGUES after one warning). */
    private String leaguesLabel() {
        List<String> out = new ArrayList<>();
        for (String s : prefs.leagues()) out.add(leagueLabel(s));
        return out.isEmpty() ? "بدون" : Prefs.join(out, "، ");
    }

    /** A built-in league's Arabic, a catalogue one's name in the setting's language (CatalogueLeagues). */
    private static String leagueLabel(String stored) {
        String[] e = CatalogueLeagues.parse(stored);
        return e == null ? leagueAr(stored) : CatalogueLeagues.name(e, S.en());
    }

    /**
     * The built-in leagues, then the catalogue ones already picked (to unpick them here too); «كل الدوريات…» opens the catalogue
     * (CatalogueLeagues: read from the internet only now). Up to Prefs.MAX_LEAGUES in all (tickLeague).
     */
    private void pickLeagues() {
        final List<String> cur = prefs.leagues();
        final List<String> keys = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        for (String[] l : Prefs.LEAGUES) { keys.add(l[0]); labels.add(l[1]); }
        for (String s : cur) if (CatalogueLeagues.parse(s) != null) { keys.add(s); labels.add(leagueLabel(s)); }
        final boolean[] on = new boolean[keys.size()];
        for (int i = 0; i < on.length; i++) on[i] = cur.contains(keys.get(i));
        new Dlg(this).multi(() -> {   // «حفظ», and Back too: the list is long, the buttons under it (DialogFit)
                List<String> v = ticked(keys, on);
                prefs.setList("leagues", v);
                render();
            })
            .setNeutralButton("كل الدوريات…", (d, w) -> { prefs.setList("leagues", ticked(keys, on)); render(); openCatalogue(); })   // what is ticked is kept
            .setTitle("الدوريات كاملة (بدون اختيار = بدون)")
            .setMultiChoiceItems(labels.toArray(new String[0]), on, (d, i, checked) -> { if (checked) tickLeague((AlertDialog) d, on, i, 0); })
            .show();
    }

    private static List<String> ticked(List<String> keys, boolean[] on) {
        List<String> v = new ArrayList<>();
        for (int i = 0; i < on.length; i++) if (on[i]) v.add(keys.get(i));
        return v;
    }

    private boolean catalogueBusy;   // one picker at a time: a second press while the first is loading or open does nothing

    /**
     * 4.3 «كل الدوريات»: the catalogue. The copy of the last time opens AT ONCE (the file is fetched again quietly for next time); with no copy yet a loading
     * dialog (with «إلغاء») shows while it downloads. The tester's TV waited 54 s for the first one: raw.githubusercontent.com has four addresses and a connection
     * that goes nowhere is tried on each in turn with the whole connect timeout (4 x 12 s); now 5 s each, and nothing waits when a copy exists.
     */
    private void openCatalogue() {
        if (catalogueBusy) return;
        catalogueBusy = true;
        final java.io.File cache = new java.io.File(getCacheDir(), "league-catalogue.json");
        List<String[]> kept = cache.isFile() ? catalogueRows(readFile(cache)) : new ArrayList<>();
        // a recent, whole copy at once (refreshed for next time); an older one, or one with a country not in Arabic: the published one first
        if (CatalogueLeagues.useKept(kept, cache.lastModified(), System.currentTimeMillis())) {
            new Thread(() -> fetchCatalogue(cache), "catalogue-refresh").start();   // for next time, nobody waits for it
            catalogueGroups(kept);
            return;
        }
        final boolean[] cancelled = {false};
        final AlertDialog wait = new Dlg(this).setTitle("كل الدوريات").setMessage("جارٍ تنزيل قائمة الدوريات…")
            .setNegativeButton("إلغاء", (d, w) -> { cancelled[0] = true; catalogueBusy = false; })
            .setOnCancelListener(d -> { cancelled[0] = true; catalogueBusy = false; }).show();
        new Thread(() -> {
            final String got = fetchCatalogue(cache);
            runOnUiThread(() -> {
                if (cancelled[0] || isFinishing()) return;
                wait.dismiss();
                List<String[]> rows = catalogueRows(got);
                if (rows.isEmpty()) { catalogueBusy = false; toast("ما فيه اتصال"); return; }
                catalogueGroups(rows);
            });
        }, "catalogue").start();
    }

    /** the file from the internet (5 s to connect, 15 s to read), kept as the copy when it is a whole answer; else the copy; null when there is neither */
    static String fetchCatalogue(java.io.File cache) {
        String body = null;
        try {
            Net.Reply r = DataHost.cached(new java.io.File(cache.getParentFile(), "net"), CatalogueLeagues.URL, 15000, 12000, Bounded.JSON_MAX, 0, false);   // urtv-data, then the old branch (DataHost); IPv4 first, 4 s per address, 12 s in all (Net); G3: with the copy's ETag
            if (r.status != 200) throw new java.io.IOException("status " + r.status);
            body = r.text();
            new JSONObject(body).getJSONArray("leagues");   // a whole answer before it replaces the copy
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(cache)) { out.write(body.getBytes("UTF-8")); }
        } catch (Exception e) {
            body = cache.isFile() ? readFile(cache) : null;
        }
        return body;
    }

    static String readFile(java.io.File f) {
        try (InputStream in = new java.io.FileInputStream(f)) { return new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8"); }
        catch (Exception e) { return null; }
    }

    static List<String[]> catalogueRows(String json) {
        List<String[]> rows = new ArrayList<>();
        if (json == null) return rows;
        try {
            JSONArray a = new JSONObject(json).getJSONArray("leagues");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                String id = o.optString("id", "");
                if (!id.matches("\\d{1,6}")) continue;
                rows.add(new String[] {id, o.optString("en", ""), o.optString("ar", ""), o.optString("country", ""),
                    o.optString("country_ar", ""), o.optString("sport", ""), o.optBoolean("top", false) ? "1" : ""});
            }
        } catch (Exception e) { rows.clear(); }
        return rows;
    }

    /** The first list: «الأشهر» (the hand-named ones), 4.3.3 «كل الدول» (every league of the catalogue), then each country. */
    private void catalogueGroups(final List<String[]> rows) {
        final List<String[]> groups = new ArrayList<>();
        groups.add(new String[] {"", "الأشهر"});
        groups.add(new String[] {"*", "كل الدول"});
        groups.addAll(CatalogueLeagues.countries(rows, S.en()));
        String[] labels = new String[groups.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = groups.get(i)[1];
        final boolean[] nextList = {false};
        new Dlg(this).setTitle("كل الدوريات")
            .setItems(labels, (d, i) -> {
                List<String[]> in = new ArrayList<>();
                for (String[] r : rows) if (i == 0 ? !r[6].isEmpty() : i == 1 || r[3].equalsIgnoreCase(groups.get(i)[0])) in.add(r);
                nextList[0] = true;
                catalogueLeagues(labels[i], in);
            })
            .setNegativeButton("إلغاء", null)
            .setOnDismissListener(d -> catalogueBusy = nextList[0])   // true while the leagues of one group are open on top: still one picker
            .show();
    }

    /** The leagues of one group, those picked checked; «حفظ» keeps the built-in ones and the other groups' as they were. */
    private void catalogueLeagues(String title, final List<String[]> rows) {
        final List<String> cur = prefs.leagues();
        final String[] labels = new String[rows.size()];
        final boolean[] on = new boolean[rows.size()];
        for (int i = 0; i < labels.length; i++) {
            String[] r = rows.get(i);
            String n = S.en() || r[2].isEmpty() ? r[1] : r[2];
            labels[i] = r[6].isEmpty() || r[4].isEmpty() && r[3].isEmpty() ? n : n + S.sep() + (S.en() || r[4].isEmpty() ? r[3] : r[4]);
            for (String s : cur) on[i] |= CatalogueLeagues.same(s, "id" + r[0]);
        }
        new Dlg(this).multi(() -> {
                List<String> v = new ArrayList<>();
                for (String s : cur) {   // what is not in this group stays
                    boolean here = false;
                    for (String[] r : rows) here |= CatalogueLeagues.same(s, "id" + r[0]);
                    if (!here) v.add(s);
                }
                for (int i = 0; i < on.length; i++) {
                    String e = on[i] ? CatalogueLeagues.entry(rows.get(i)[0], rows.get(i)[2], rows.get(i)[1]) : null;
                    if (e != null && v.size() < Prefs.MAX_LEAGUES) v.add(e);
                }
                prefs.setList("leagues", v);
                render();
            })
            .setTitle(title)
            .setMultiChoiceItems(labels, on, (d, i, checked) -> {
                if (!checked) return;
                int n = 0;   // the leagues followed outside this group
                for (String s : cur) {
                    boolean here = false;
                    for (String[] r : rows) here |= CatalogueLeagues.same(s, "id" + r[0]);
                    if (!here) n++;
                }
                tickLeague((AlertDialog) d, on, i, n);
            })
            .setOnDismissListener(d -> catalogueBusy = false)
            .show();
    }

    private void manageShows() {
        final List<String> cur = prefs.shows();
        String[] labels = new String[cur.size()];
        final boolean[] keep = new boolean[cur.size()];
        for (int i = 0; i < cur.size(); i++) { labels[i] = cur.get(i).split("\\|", 2)[1]; keep[i] = true; }
        AlertDialog.Builder b = new Dlg(this).multi(() -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < cur.size(); i++) if (keep[i]) v.add(cur.get(i));
                prefs.setList("shows", v);
                render();
            })
            .setTitle(cur.isEmpty() ? "لا توجد مسلسلات مختارة" : "المسلسلات المختارة (ألغِ التحديد للحذف)")
            .setNeutralButton("إضافة مسلسل", (d, w) -> searchShow());
        if (!cur.isEmpty()) b.setMultiChoiceItems(labels, keep, (d, i, checked) -> keep[i] = checked);
        b.show();
    }

    private void searchShow() {
        ask("اسم المسلسل", q -> fetch("https://api.tvmaze.com/search/shows?q=" + Uri.encode(q), json -> {
            JSONArray a = new JSONArray(json);
            if (a.length() == 0) { toast("لا توجد نتائج"); return; }
            final int n = Math.min(a.length(), 15);
            final String[] ids = new String[n], names = new String[n], rows = new String[n];
            for (int i = 0; i < n; i++) {
                JSONObject s = a.getJSONObject(i).getJSONObject("show");
                ids[i] = String.valueOf(s.optInt("id"));
                names[i] = s.optString("name").replace("|", " ").replace(";", " ");
                String year = s.optString("premiered", "");
                rows[i] = names[i] + (year.length() >= 4 ? " (" + year.substring(0, 4) + ")" : "")
                    + "، " + labelOf(Prefs.LANGS, s.optString("language", ""));
            }
            new Dlg(this).setTitle("اختر المسلسل")
                .setItems(rows, (d, i) -> {
                    for (String s : prefs.shows()) if (s.startsWith(ids[i] + "|")) { toast("مضاف من قبل"); return; }
                    List<String> v = prefs.shows();
                    v.add(ids[i] + "|" + names[i]);
                    prefs.setList("shows", v);
                    toast("تمت إضافة " + names[i]);
                    render();
                }).show();
        }));
    }

    /** is our home app the one the Home key opens now? (HomeBack's rule, in the launcher: the resolved Home activity's package is ours) */
    private boolean isOurHome() {
        android.content.pm.ResolveInfo ri = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && "sa.urtv.home".equals(ri.activityInfo.packageName);
    }

    /** «إعداد تلفزيوني» is the launcher's screen; the home switch is its step, so the run is the full setup (nothing here touches the TV's settings) */
    private void openSetup() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.setup.SetupActivity"));
        } catch (SecurityException | ActivityNotFoundException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** «تصفية المواقع (DNS)» lives in the launcher too: it holds the permission to write the Private DNS setting. */
    private void openFamilyDns() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.FamilyDnsActivity"));
        } catch (SecurityException | ActivityNotFoundException e) {   // permission not granted, or an old launcher without the activity
            boolean installed = true;
            try { getPackageManager().getPackageInfo("sa.urtv.home", 0); }
            catch (android.content.pm.PackageManager.NameNotFoundException gone) { installed = false; }
            toast(installed ? "ما قدرنا نفتح تصفية المواقع: حدّث الواجهة الرئيسية «تلفزيونك»" : "ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** «صفحة الأسواق» lives in the launcher, with «محفظتي» (kept there only; its own lock asks for the PIN). */
    private void openMarkets() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.MarketsActivity"));
        } catch (ActivityNotFoundException e) {
            toast("حدّث الواجهة الرئيسية «تلفزيونك» عشان تطلع صفحة الأسواق");
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** «الرقابة الأبوية» lives in the launcher (it keeps the PIN); its page asks for the PIN itself once one is set. */
    private void openParental() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.ParentalActivity"));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** «تفاصيل التطبيقات»: Android's own list of apps, where each app's details and its uninstall are. */
    private void openAppDetails() {
        for (String action : new String[] {android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, android.provider.Settings.ACTION_APPLICATION_SETTINGS}) {
            try { startActivity(new Intent(action)); return; } catch (RuntimeException ignored) { }
        }
        toast("ما قدرنا نفتح قائمة التطبيقات في التلفزيون");
    }

    /** «ترتيب المفضلة» lives in the launcher (it owns the row). */
    private void openDockSettings() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.DockActivity"));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** The home app may read the TV's Watch Next list (its READ_TV_LISTINGS, granted once). */
    private boolean homeMayReadWatchNext() {
        return getPackageManager().checkPermission("android.permission.READ_TV_LISTINGS", "sa.urtv.home") == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private String watchNextLabel() {
        if (!prefs.watchNext()) return "متوقف";
        return homeMayReadWatchNext() ? "مفعّل، ما وقفته في نصه، تحت المفضلة" : "مفعّل، يحتاج السماح مرة واحدة: اختيار";
    }

    /** The home app's one question for the TV's list («السماح»); it closes by itself when that is allowed already. */
    private void openWatchNextPermission() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.WatchNextActivity"));
        } catch (RuntimeException e) {   // not installed, or an older launcher without the screen
            toast("حدّث الواجهة الرئيسية «تلفزيونك» أو ثبّتها أولاً");
        }
    }

    /** «أسماء المصادر» lives in the launcher (it owns the sources list). */
    private void openInputNames() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.InputNamesActivity"));
        } catch (RuntimeException e) {   // not installed, or an older launcher without the screen
            toast("حدّث الواجهة الرئيسية «تلفزيونك» أو ثبّتها أولاً");
        }
    }

    /** The adhan's settings are the launcher's: its home opens them (behind the family lock) when asked with open=adhan. */
    private void openAdhanSettings() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.MainActivity").putExtra("open", "adhan"));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** «تعديل المفضلة» is a mode of the launcher's home screen (it owns the row); the extra asks it to start in it. */
    private void openDockEdit() {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setClassName("sa.urtv.home", "sa.urtv.home.MainActivity")
                .putExtra("edit_favourites", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** «ترتيب كل التطبيقات»: the mode on the home screen, or «رجوع للترتيب الأصلي»; both are the launcher's (it owns the grid). */
    private void pickAppsOrder() {
        new Dlg(this).setTitle("ترتيب كل التطبيقات")
            .setItems(new String[] {"تعديل التطبيقات", "التطبيقات المخفية", "رجوع للترتيب الأصلي"}, (d, i) -> openAppsOrder(i == 2, i == 1))
            .show();
    }

    /** hidden (4.3.4): the mode on «المخفية» with its list; a home older than 4.3.4 opens the mode, where «المخفية» is the last place */
    private void openAppsOrder(boolean reset, boolean hidden) {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setClassName("sa.urtv.home", "sa.urtv.home.MainActivity")
                .putExtra(reset ? "reset_apps_order" : "edit_apps", true).putExtra("apps_hidden", hidden).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** «تحديث تلفزيوني» lives in the launcher (it is the app that gets replaced last). */
    private void openUpdate() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.UpdateActivity"));
        } catch (SecurityException e) {   // the home is newer than this app (or the other way round): it asks for a permission this one does not have yet
            toast("حدّث التطبيقين «شاشة تلفزيونك» و«تلفزيونك»: واحد منهم أقدم من الثاني");
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /**
     * Something of the home's own, opened through it (behind its family lock, as its own button there): open=center its «مركز التحكم»,
     * open=listen «تلاوة وأذان». An older home that does not know the word just comes to the front.
     */
    private void openHome(String what) {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.MainActivity").putExtra("open", what));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    /** 4.3 «تطبيقات مقترحة»: the home app's screen (SuggestedAppsActivity) */
    private void openSuggested() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.SuggestedAppsActivity"));
        } catch (RuntimeException e) {   // not installed, or an older home without the screen
            toast("حدّث الواجهة الرئيسية «تلفزيونك» أو ثبّتها أولاً");
        }
    }

    /** «أرسل تقرير»: the home's report screen (a QR code to docs/share with a few lines, nothing personal; ReportActivity). */
    /** this app's caches: its cache folder and the hosted photos kept for the screensaver (PhotoCache); the person's own photos (Own) and the packs (PackStore) are elsewhere */
    private java.io.File[] ownCaches() { return new java.io.File[] {getCacheDir(), new java.io.File(getFilesDir(), "photos")}; }

    /** the home's cache folder, through its provider (same key): its size, or the sweep; -1 when the home cannot say (not installed, or older) */
    private long homeCache(String method) {
        try {
            Bundle b = getContentResolver().call(Uri.parse("content://sa.urtv.home.nowplaying"), method, null, null);
            return b == null ? -1 : b.getLong("bytes", -1);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** «مسح الملفات المؤقتة»: says how much the two apps' caches hold, asks, then clears them and says what was freed. The sizes are read off the UI thread. */
    private void clearCaches() {
        new Thread(() -> {
            final long mine = CacheSweep.size(ownCaches()), home = homeCache("cache_size");
            runOnUiThread(() -> {
                if (isFinishing()) return;
                final long all = mine + Math.max(0, home);
                new Dlg(this).setTitle("مسح الملفات المؤقتة")
                    .setMessage(conv("تُمسح " + megabytes(all) + " ميغابايت من الصور والبيانات المؤقتة، وتنزل من جديد وقت الحاجة. الإعدادات وصورك وحزم الصور والمشاهد وصوت الأذان تبقى."))
                    .setPositiveButton("امسح", (d, w) -> new Thread(() -> {
                        final long freed = CacheSweep.clear(ownCaches()) + Math.max(0, homeCache("clear_cache"));
                        runOnUiThread(() -> { if (!isFinishing()) new Dlg(this).setTitle("مسح الملفات المؤقتة").setMessage(conv("مُسح " + megabytes(freed) + " ميغابايت.")).setPositiveButton("تم", null).show(); });
                    }).start())
                    .setNegativeButton("إلغاء", null)
                    .show();
            });
        }).start();
    }

    /** megabytes with one decimal, «12.5» (conv gives the digits the setting asks for) */
    static String megabytes(long bytes) { return String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, bytes) / (1024.0 * 1024.0)); }

    private void openReport() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.ReportActivity"));
        } catch (RuntimeException e) {   // not installed, or an older home without the screen
            toast("حدّث الواجهة الرئيسية «تلفزيونك» أو ثبّتها أولاً");
        }
    }

    /** «UrTV النظيف» lives in the launcher (it holds the home it masks and the family PIN) */
    private void openFactory() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.setup.FactoryActivity"));
        } catch (RuntimeException e) {
            toast("حدّث الواجهة الرئيسية «تلفزيونك» أو ثبّتها أولاً");
        }
    }

    /** 4.3.2: why UrTV is not deleted while the TV's own home is off, and how to undo it (the screen opens from «رجّع واجهة المصنع» too). */
    private void uninstallWarning(boolean factoryOff) {
        AlertDialog.Builder d = new Dlg(this).setTitle("قبل ما تحذف التطبيق…")
            .setMessage(factoryOff
                ? "واجهة المصنع معطّلة الحين، وUrTV هو الشاشة الرئيسية. إذا حذفت UrTV وهي معطّلة يبقى التلفزيون بلا شاشة رئيسية، وتطلع شاشة سوداء. اضغط «رجّع واجهة المصنع» أولاً، ثم احذف."
                : "إذا شغّلت «تخلّص من واجهة المصنع» ثم حذفت UrTV، يبقى التلفزيون بلا شاشة رئيسية وتطلع شاشة سوداء. لا تحذفه قبل «رجّع واجهة المصنع» من هنا، أو «إعداد تلفزيوني» ثم «تراجع».")
            .setNegativeButton("إغلاق", null);
        if (factoryOff) d.setPositiveButton("رجّع واجهة المصنع", (x, w) -> openFactory());
        d.show();
    }

    private void openClean() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.CleanActivity"));
        } catch (RuntimeException e) {
            toast("حدّث الواجهة الرئيسية «تلفزيونك» أو ثبّتها أولاً");
        }
    }

    /** «تحكم من الجوال» lives in the launcher (it holds the server and the pairing screen). */
    private void openPhoneControl() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.PhoneActivity"));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
    }

    private static final String[] HOME_CARD_NAMES = {"البطاقة الأولى", "البطاقة الثانية", "البطاقة الثالثة"};

    /** The card beside the screensaver's clock (place -1), or place 0 to 2 of the cards on the launcher's home screen. */
    private void pickPanel(final int place) {
        final boolean home = place >= 0;
        String current = home ? prefs.homePanelAt(place) : prefs.panel();
        // the home places can also be left empty; «لا شيء» is that choice there, so it is «بدون» in the list
        // a home-only card («الصلاة القادمة») is not offered beside the screensaver's clock: its panel there says the same
        List<String[]> rows = new ArrayList<>();
        for (String[] p : Prefs.PANELS) if (home || !Prefs.homeOnly(p[0])) rows.add(p);
        String[] labels = new String[rows.size()];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = home && "none".equals(rows.get(i)[0]) ? "بدون" : rows.get(i)[1];
            if (rows.get(i)[0].equals(current)) sel = i;
        }
        String[] values = new String[labels.length], about = new String[labels.length];
        final boolean[] off = new boolean[labels.length];
        String cardLook = prefs.cardLook(), bg = prefs.homeBg();
        for (int i = 0; i < labels.length; i++) {
            values[i] = rows.get(i)[0];
            about[i] = Prefs.about(Prefs.PANEL_ABOUT, values[i]);
            if (!home) continue;
            // 4.4 beta 2 (the owner, docs/mockups/full-look 07): a card that does not fit the look and the background (CardFit.TABLE) is shown off,
            // its reason the one line under its name, and is never applied; one that fits in its own way says how («بعرض بطاقتين»)
            String why = CardFit.why(values[i], cardLook, bg);
            off[i] = !CardFit.fits(values[i], cardLook, bg) && !values[i].equals(current);   // the card shown now stays choosable (it is there already)
            if (!why.isEmpty()) about[i] = off[i] ? why : why + "، " + about[i];
        }
        pickWithPreview(home ? HOME_CARD_NAMES[place] : "بجانب الساعة", values, labels, about, off, sel,
            v -> prefs.previewUrl(this, v, prefs.cardStyle()),
            v -> { if (home) prefs.setHomePanelAt(place, v); else prefs.setPanel(v); });
    }

    /**
     * «بطاقة ثانية» beside the small clock: the screensaver's cards but the first one («بدون» = none). The preview is that card in
     * today's style, alone, as the other pickers draw it.
     */
    private void pickPanel2() {
        List<String[]> rows = new ArrayList<>();
        for (String[] p : Prefs.SAVER_PANELS) if (!p[0].equals(prefs.panel())) rows.add(p);
        String[] values = new String[rows.size()], labels = new String[rows.size()], about = new String[rows.size()];
        int sel = 0;
        for (int i = 0; i < values.length; i++) {
            values[i] = rows.get(i)[0];
            labels[i] = "none".equals(values[i]) ? "بدون" : rows.get(i)[1];
            about[i] = "none".equals(values[i]) ? "بطاقة واحدة بجانب الساعة" : Prefs.about(Prefs.PANEL_ABOUT, values[i]);
            if (values[i].equals(prefs.panel2())) sel = i;
        }
        pickWithPreview("بطاقة ثانية", values, labels, about, null, sel,
            v -> prefs.previewUrl(this, v, prefs.cardStyle()), v -> prefs.setPanel2(v));
    }

    private interface Url { String of(String value); }
    private interface Pick { void on(String value); }

    /**
     * A picker with the list on one side, one plain line under each name, and on the other side the focused choice drawn live by the
     * page itself: the card, over a dimmed photo of ours as it looks on the screen, in a box that is only as tall as the card (measured
     * from the page once it has drawn). The dialog is in the screen's glass design (Dlg). The WebView is made when the picker opens and
     * destroyed when it closes; it keeps the TV's text size out of it (setTextZoom(100) in ScreenView) like every other one of ours.
     */
    private void pickWithPreview(String title, final String[] values, final String[] labels, final String[] about, final boolean[] off, int selected,
                                 final Url urlFor, final Pick onPick) {
        final int previewW = 480, viewH = 300, pad = 22;   // dp: the page is laid out in a box like the home screen's, 480 wide
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setLayoutDirection(Dir.layout());
        box.setPadding(dp(12), dp(4), dp(12), dp(4));
        final android.widget.ListView lv = new android.widget.ListView(this);
        lv.setDividerHeight(0);
        lv.setSelector(GlassPaint.focused(this, look, 12));
        lv.setAdapter(new android.widget.BaseAdapter() {
            @Override public int getCount() { return values.length; }
            @Override public Object getItem(int i) { return values[i]; }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View old, android.view.ViewGroup parent) {
                LinearLayout r = new LinearLayout(SettingsActivity.this);
                r.setOrientation(LinearLayout.VERTICAL);
                r.setPadding(dp(16), dp(10), dp(16), dp(10));
                TextView n = new TextView(SettingsActivity.this);
                n.setText(conv(labels[i]));
                n.setTextColor(focusColors(look.text, look.onFocus));   // the selected row is the solid focus fill
                n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                n.setTypeface(font("Thmanyah-Medium"));
                r.addView(n);
                TextView a = new TextView(SettingsActivity.this);
                a.setText(conv(about[i]));
                a.setTextColor(focusColors(look.sub, look.onFocusSub));
                a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                a.setTypeface(font("Thmanyah-Regular"));
                r.addView(a);
                if (off != null && off[i]) { n.setAlpha(0.5f); a.setTextColor(OFF_REASON); }   // shown off: dimmed, its reason in a warm tone, still focusable to read
                return r;
            }
        });
        box.addView(lv, new LinearLayout.LayoutParams(0, dp(330), 1f));
        // the stage: our photo, dimmed, with the card on it; as tall as the card needs (setStageHeight), round-cornered, no empty space
        final FrameLayout stage = new FrameLayout(this);
        stage.setBackgroundColor(0xFF07150E);
        final ImageView bgPhoto = new ImageView(this);   // the photo comes from the pictures' thread (the stage shows its ground colour until then)
        bgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgPhoto.setColorFilter(new PorterDuffColorFilter(0x8C000000, PorterDuff.Mode.SRC_OVER));   // dim, as the screen's own veil does
        stage.addView(bgPhoto, new FrameLayout.LayoutParams(-1, -1));
        pics.execute(() -> {
            final Bitmap photo = photo(3);
            if (photo != null) runOnUiThread(() -> bgPhoto.setImageBitmap(photo));
        });
        stage.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(18)); }
        });
        stage.setClipToOutline(true);
        final LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(previewW), dp(2 * pad + 150));
        sp.setMarginStart(dp(16));
        sp.gravity = Gravity.CENTER_VERTICAL;
        box.addView(stage, sp);

        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        final android.webkit.WebView[] web = new android.webkit.WebView[1];
        final int[] shown = {-1}, loads = {0};
        // the card's height in CSS px (= dp) once the page has drawn it: asked twice, the data of some cards comes a moment later
        final Runnable measure = new Runnable() { public void run() {
            final android.webkit.WebView w = web[0];
            if (w == null) return;
            w.evaluateJavascript("(function(){var c=document.querySelector('.week');return c?Math.ceil(c.getBoundingClientRect().height):0})()", value -> {
                int h;
                try { h = Integer.parseInt(value.trim()); } catch (RuntimeException e) { return; }
                if (web[0] != w || h <= 0) return;
                sp.height = dp(Math.min(2 * pad + h, 2 * pad + viewH));
                stage.setLayoutParams(sp);
            });
        } };
        final Runnable update = new Runnable() { public void run() {
            int i = lv.getSelectedItemPosition();
            if (i < 0) i = Math.max(0, selected);   // nothing selected yet (touch mode): the current choice
            if (i == shown[0]) return;
            shown[0] = i;
            String u = urlFor.of(values[i]);
            // a different query each time makes it a real navigation: with only the #fragment changed it is the same document
            int hash = u.indexOf('#');
            u = hash < 0 ? u + "?p=" + (++loads[0]) : u.substring(0, hash) + "?p=" + (++loads[0]) + u.substring(hash);
            if (web[0] == null) {
                web[0] = ScreenView.createPreview(SettingsActivity.this, u, dead -> {   // its renderer died: the card goes blank, nothing is made again
                    ScreenView.release(dead);
                    if (web[0] == dead) { web[0] = null; shown[0] = -1; }
                });
                FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(-1, dp(viewH));   // the page's own box: a fixed one, the card is at its top
                wp.topMargin = dp(pad);
                wp.leftMargin = wp.rightMargin = dp(pad);
                stage.addView(web[0], wp);
            } else {   // the page reads its settings once, when it starts: load it again, no reload() (that would redo the old URL)
                web[0].loadUrl(u);
            }
            handler.removeCallbacks(measure);
            handler.postDelayed(measure, 900);
            handler.postDelayed(measure, 2600);
        } };
        lv.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                handler.removeCallbacks(update);
                handler.postDelayed(update, 200);   // a long press of the arrow key goes past choices without drawing each
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        final AlertDialog dialog = new Dlg(this).setTitle(title).setView(box)
            .setNegativeButton("إلغاء", null).create();
        lv.setOnItemClickListener((p, v, pos, id) -> {
            if (off != null && off[pos]) {   // the owner: never applied, never silent: the row says no (a short nudge, none on a weak TV) and its reason is read again
                if (!Motion.lite(this) && v != null) v.animate().translationX(dp(8)).setDuration(90).withEndAction(() -> v.animate().translationX(0).setDuration(90).start()).start();
                if (v != null) v.announceForAccessibility(about[pos]);
                return;
            }
            onPick.on(values[pos]); dialog.dismiss(); render();
        });
        previewOff = () -> {
            handler.removeCallbacks(update);
            handler.removeCallbacks(measure);
            if (web[0] != null) { ScreenView.release(web[0]); web[0] = null; }
            shown[0] = -1;
        };
        previewOn = () -> handler.postDelayed(update, 50);
        dialog.setOnShowListener(d -> {
            dress(dialog);
            dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(940)), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            MixFocus.calm(dialog.getWindow());   // the owner: a quiet backdrop, the settings page behind does not show through
            lv.requestFocus();
            lv.setSelection(Math.max(0, selected));
            handler.postDelayed(update, 50);
        });
        dialog.setOnDismissListener(d -> {   // nothing of the preview outlives the picker
            handler.removeCallbacks(update);
            handler.removeCallbacks(measure);
            if (web[0] != null) { ScreenView.release(web[0]); web[0] = null; }
            previewOff = previewOn = null;
        });
        dialog.show();
    }

    private void pickRating() {
        String[] labels = Prefs.MIN_RATINGS.clone();
        labels[0] = "بدون حد";
        int sel = 0;
        for (int i = 0; i < Prefs.MIN_RATINGS.length; i++) if (Prefs.MIN_RATINGS[i].equals(prefs.minRating())) sel = i;
        new Dlg(this).setTitle("أقل تقييم")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setMinRating(Prefs.MIN_RATINGS[i]); d.dismiss(); render(); })
            .show();
    }

    private String marketsLabel() {
        List<String> out = new ArrayList<>();
        for (String k : prefs.markets()) out.add(labelOf(Prefs.MARKETS, k));
        return Prefs.join(out, "، ");
    }

    /** Up to MAX_MARKETS; the order they are shown in is the order of the list. */
    private void pickMarkets() {
        List<String> cur = prefs.markets();
        String[] labels = new String[Prefs.MARKETS.length];
        final boolean[] on = new boolean[Prefs.MARKETS.length];
        for (int i = 0; i < labels.length; i++) { labels[i] = Prefs.MARKETS[i][1]; on[i] = cur.contains(Prefs.MARKETS[i][0]); }
        new Dlg(this).multi(() -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < on.length; i++) if (on[i]) v.add(Prefs.MARKETS[i][0]);
                if (v.isEmpty() || v.size() > Prefs.MAX_MARKETS) {
                    toast("اختر من سوق إلى " + arabicNum(Prefs.MAX_MARKETS) + " أسواق");
                    return;
                }
                prefs.setMarkets(v);
                prefs.setMarketsOn(true);
                render();
            })
            .setTitle("الأسواق (حتى " + arabicNum(Prefs.MAX_MARKETS) + ")")
            .setMultiChoiceItems(labels, on, (d, i, checked) -> on[i] = checked)
            .show();
    }

    private String stocksLabel() {
        List<String> out = new ArrayList<>();
        for (String k : prefs.stocks()) out.add(Stocks.label(k, S.en()));   // «أرامكو 2222»: the code and the company it is
        return Prefs.join(out, S.en() ? ", " : "، ");
    }

    /** Codes (2222 = أرامكو, also ٢٢٢٢), company names (الراجحي) and US tickers (AAPL), separated by spaces or commas (Stocks). */
    private void askGreetingName() {
        final EditText in = new EditText(this);
        in.setHint(Words.of("مثال: علاء"));
        in.setText(prefs.greetingName());
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new Dlg(this).setTitle("اسمك في التحية (حتى " + arabicNum(Prefs.NAME_MAX) + " حرفًا)")
            .setView(in)
            .setPositiveButton("حفظ", (d, w) -> { prefs.setGreetingName(in.getText().toString()); render(); })
            .setNeutralButton("بدون اسم", (d, w) -> { prefs.setGreetingName(""); render(); })
            .setNegativeButton("إلغاء", null).show();
        in.requestFocus();
    }

    private void askStocks() {
        final EditText in = new EditText(this);
        in.setHint(Words.of("مثال: \u20662222 1120 AAPL NVDA\u2069"));
        in.setText(Prefs.join(prefs.stocks(), " "));
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        new Dlg(this).setTitle("قائمة الأسهم (رمز تداول من ٤ أرقام أو اسم الشركة أو رمز أمريكي، حتى " + arabicNum(Prefs.MAX_STOCKS) + ")")
            .setView(in)
            .setPositiveButton("حفظ", (d, w) -> {
                Stocks.Parsed got = Stocks.parse(in.getText().toString());
                if (got.bad != null) { toast("رمز غير صالح: " + got.bad); return; }
                List<String> v = got.codes;
                if (v.isEmpty() || v.size() > Prefs.MAX_STOCKS) {
                    toast("أدخل من رمز إلى " + arabicNum(Prefs.MAX_STOCKS) + " رموز");
                    return;
                }
                if (!got.matched.isEmpty()) {   // a name or ٢٢٢٢ was typed: say what it was taken as
                    List<String> shown = new ArrayList<>();
                    for (String k : got.matched) shown.add(Stocks.label(k, S.en()));
                    toast("الأسهم: " + Prefs.join(shown, S.en() ? ", " : "، "));
                }
                prefs.setStocks(v);
                render();
            })
            .setNegativeButton("إلغاء", null).show();
        in.requestFocus();
    }

    /** one choice of a table (Prefs' {value, label} lists): the current one marked; picking saves it and draws the list again */
    private void pickChoice(String title, final String[][] table, String current, final Pick onPick) {
        String[] labels = new String[table.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = table[i][1];
            if (table[i][0].equals(current)) sel = i;
        }
        new Dlg(this).setTitle(title)
            .setSingleChoiceItems(labels, sel, (d, i) -> { onPick.on(table[i][0]); d.dismiss(); render(); })
            .show();
    }

    private void pickMkStyle() {
        String[] labels = new String[Prefs.MK_STYLES.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Prefs.MK_STYLES[i][1];
            if (Prefs.MK_STYLES[i][0].equals(prefs.mkStyle())) sel = i;
        }
        new Dlg(this).setTitle("عرض الأسواق")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setMkStyle(Prefs.MK_STYLES[i][0]); d.dismiss(); render(); })
            .show();
    }

    private void pickClockSize() {
        String[] labels = new String[Prefs.CLOCK_SIZES.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Prefs.CLOCK_SIZES[i][1];
            if (Prefs.CLOCK_SIZES[i][0].equals(prefs.clockSize())) sel = i;
        }
        new Dlg(this).setTitle("حجم الساعة")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setClockSize(Prefs.CLOCK_SIZES[i][0]); d.dismiss(); render(); })
            .show();
    }

    /** «نظام الساعة»: «١٢ ساعة» (with ص and م, the default) or «٢٤ ساعة»; the screensaver's page reads it when it starts, the home at its next tick */
    private void pickClock24() {
        new Dlg(this).setTitle("نظام الساعة")
            .setSingleChoiceItems(ClockSetting.CHOICES, ClockSetting.choice(prefs.clock24()),
                (d, i) -> { prefs.setClock24(ClockSetting.fromChoice(i)); d.dismiss(); render(); })
            .show();
    }

    private static final String[] HIJRI_CHOICES = {"يومان قبل", "يوم قبل", "كما هو (أم القرى)", "يوم بعد", "يومان بعد"};

    private void pickHijri() {
        new Dlg(this).setTitle("فرق التاريخ الهجري")
            .setSingleChoiceItems(HIJRI_CHOICES, prefs.hijriOffset() + HijriOffset.MAX,
                (d, i) -> { prefs.setHijriOffset(i - HijriOffset.MAX); d.dismiss(); render(); })
            .show();
    }

    private void pickQuoteEvery() {
        new Dlg(this).setTitle("تبديل الآية والحديث")
            .setSingleChoiceItems(QuoteEvery.CHOICES, QuoteEvery.choice(prefs.quoteEvery()),
                (d, i) -> { prefs.setQuoteEvery(QuoteEvery.fromChoice(i)); d.dismiss(); render(); })
            .show();
    }

    private void pickVerseKind() {
        new Dlg(this).setTitle("الآية والحديث")
            .setSingleChoiceItems(VerseKind.CHOICES, VerseKind.choice(prefs.verseKind()),
                (d, i) -> { prefs.setVerseKind(VerseKind.fromChoice(i)); d.dismiss(); render(); })
            .show();
    }

    private void pickHomeExit() {
        new Dlg(this).setTitle("الخروج من تلفزيونك")
            .setSingleChoiceItems(HomeExit.CHOICES, HomeExit.choice(prefs.homeExit()),
                (d, i) -> { prefs.setHomeExit(HomeExit.fromChoice(i)); d.dismiss(); render(); })
            .show();
    }

    private void pickCardStyle() {
        String[] labels = new String[Prefs.CARD_STYLES.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Prefs.CARD_STYLES[i][1];
            if (Prefs.CARD_STYLES[i][0].equals(prefs.cardStyle())) sel = i;
        }
        String[] values = new String[labels.length], about = new String[labels.length];
        for (int i = 0; i < labels.length; i++) { values[i] = Prefs.CARD_STYLES[i][0]; about[i] = Prefs.about(Prefs.CARD_STYLE_ABOUT, values[i]); }
        // the preview is the card you have now, in the style under the cursor (the prayer times when there is no card)
        final String card = "none".equals(prefs.panel()) ? "prayers" : prefs.panel();
        pickWithPreview("شكل البطاقة", values, labels, about, null, sel, v -> prefs.previewUrl(this, card, v), v -> prefs.setCardStyle(v));
    }

    private void pickSlot() {
        String[] labels = new String[Prefs.SLOTS.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Prefs.SLOTS[i][1];
            if (Prefs.SLOTS[i][0].equals(prefs.slot())) sel = i;
        }
        new Dlg(this).setTitle("الخانة اليسرى")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setSlot(Prefs.SLOTS[i][0]); d.dismiss(); render(); })
            .show();
    }

    private String countdownLabel() {
        if (prefs.countdownDate().isEmpty()) return "لم يُحدَّد";
        if (Prefs.SCHOOL.equals(prefs.countdownDate())) return Prefs.SCHOOL_LABEL;
        String name = prefs.countdownName();
        return CountdownText.label(prefs.countdownDate(), name);
    }

    /** The date is set from the computer (optimize/saver.sh countdown); here it can be cleared. */
    private void manageCountdown() {
        if (prefs.countdownDate().isEmpty()) {
            // 4.4: the next school break needs no date from anyone (the official calendar comes with the data); another date, from the computer
            new Dlg(this).setTitle("العدّ التنازلي")
                .setMessage("ما فيه تاريخ محدد. اختر «إجازات المدارس» لأقرب إجازة، أو يحدد المساعد تاريخ ثاني من الكمبيوتر.")
                .setPositiveButton(Prefs.SCHOOL_LABEL, (d, w) -> { prefs.setCountdown(Prefs.SCHOOL, ""); render(); })
                .setNegativeButton("إلغاء", null).show();
            return;
        }
        new Dlg(this).setTitle("العدّ التنازلي")
            .setMessage(countdownLabel())
            .setPositiveButton("مسح", (d, w) -> { prefs.setCountdown("", ""); render(); })
            .setNegativeButton("إلغاء", null).show();
    }

    /** «اللغة» as its row and button say it: the language in use, in its own words; with Arabic, English beside it to be found */
    private String langLabel() { return Tr.EN.equals(prefs.lang()) ? "English" : "العربية، English"; }

    /**
     * «اللغة»: العربية or English, each in its own language. The choice is kept (Prefs.setLang: the page and the home follow it at their
     * next start), and this screen is made again in it at once, on the same button.
     */
    private void pickLang() {
        final String[] values = {Tr.AR, Tr.EN};
        new Dlg(this).raw().own().setTitle(S.en() ? "Language، اللغة" : "اللغة، Language")
            .setSingleChoiceItems(new String[] {"العربية", "English"}, Tr.EN.equals(prefs.lang()) ? 1 : 0, (d, i) -> {
                d.dismiss();
                if (values[i].equals(prefs.lang())) return;
                prefs.setLang(values[i]);
                // the screen again in the new language (its direction and words are set when it starts), on the same button
                startActivity(new Intent(this, SettingsActivity.class).putExtra("screen", screen).putExtra("hubAt", screen));
                finish();
            })
            .show();
    }

    private void pickDigits() {
        String[] labels = new String[Prefs.DIGITS.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Prefs.DIGITS[i][1];
            if (Prefs.DIGITS[i][0].equals(prefs.digits())) sel = i;
        }
        new Dlg(this).raw().setTitle("الأرقام")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setDigits(Prefs.DIGITS[i][0]); d.dismiss(); render(); })
            .show();
    }

    private void pickPrayerMethod() {
        final String[] values = {PrayerMethod.AUTO, PrayerMethod.GULF, PrayerMethod.JAFARI};
        String[] labels = {PrayerMethod.label(PrayerMethod.AUTO, prefs.cityCountry(), prefs.cityTz()), PrayerMethod.label(PrayerMethod.GULF, prefs.cityCountry(), prefs.cityTz()), PrayerMethod.label(PrayerMethod.JAFARI, prefs.cityCountry(), prefs.cityTz())};
        new Dlg(this).raw().setTitle("طريقة الحساب")
            .setSingleChoiceItems(labels, PrayerMethod.JAFARI.equals(prefs.prayerMethod()) ? 2 : PrayerMethod.GULF.equals(prefs.prayerMethod()) ? 1 : 0,
                (d, i) -> { prefs.setPrayerMethod(values[i]); d.dismiss(); render(); })
            .show();
    }

    private void pickNational() {
        String[] labels = new String[Prefs.NATIONAL.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Prefs.NATIONAL[i][1];
            if (Prefs.NATIONAL[i][0].equals(prefs.national())) sel = i;
        }
        new Dlg(this).setTitle("المناسبات الوطنية")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setNational(Prefs.NATIONAL[i][0]); d.dismiss(); render(); })
            .show();
    }

    /** The choices of this TV (PhotoInterval.choices: 30 seconds only with 2 GB of memory), the one in use checked. */
    private void pickPhotoMinutes() {
        final String[] values = PhotoInterval.choices(Prefs.ramMb(this));
        String[] labels = new String[values.length];
        int sel = 0;
        for (int i = 0; i < values.length; i++) {
            labels[i] = conv(PhotoInterval.label(values[i]));
            if (values[i].equals(prefs.photoMinutes())) sel = i;
        }
        new Dlg(this).setTitle("تبديل الصورة كل")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setPhotoMinutes(values[i]); d.dismiss(); render(); })
            .show();
    }

    /** «صورنا» and the packs that are on and on the TV: «صورنا» · ٢ مجموعات */
    private String packsLabel() {
        int n = 0;
        java.util.Map<String, Integer> have = PackStore.installed(this);
        for (String id : prefs.packsOn()) if (have.containsKey(id)) n++;
        return n == 0 ? "صورنا" : n == 1 ? "صورنا، مجموعة واحدة" : n == 2 ? "صورنا، مجموعتان" : "صورنا، " + n + (n <= 10 ? " مجموعات" : " مجموعة");
    }

    private String ownLabel() {
        if (Own.count(this) == 0) return "لا توجد صور، أضفها من الجوال";
        return Own.ONLY.equals(Own.mode(this)) ? "صوري فقط" : Own.ALBUM.equals(Own.mode(this)) ? "للألبوم فقط" : "تُضاف إلى صور التطبيق";
    }

    /** «صوري»: photos only arrive through the owner's assistant; here the owner picks how they show, or removes them all. */
    private void pickOwn() {
        if (Own.count(this) == 0) {
            toast("أضف صورك من الجوال: «أضف صوري من الجوال» (حتى ١٠)");
            return;
        }
        String[] items = {"تُضاف إلى صور التطبيق", "صوري فقط", "للألبوم فقط، خارج دوران الصور", "اختر صورة لحذفها", "حذف كل صوري"};
        new Dlg(this).setTitle("صوري")
            .setSingleChoiceItems(items, Own.ONLY.equals(Own.mode(this)) ? 1 : Own.ALBUM.equals(Own.mode(this)) ? 2 : 0, (d, i) -> {
                d.dismiss();
                if (i == 3) { pickOwnToDelete(); return; }   // 4.4: one photo
                if (i == 4) { confirmClearOwn(); return; }
                prefs.setOwnMode(i == 1 ? Own.ONLY : i == 2 ? Own.ALBUM : Own.MIX);
                prefs.bumpOwnRev();
                render();
            })
            .show();
    }

    /**
     * 4.4 «اختر صورة لحذفها»: the photos as a grid of small pictures (made off the screen's thread, at most OWN_THUMB px), OK on one asks
     * «حذف هذه الصورة؟». The pick is the photo's number and file time (OwnPick), so a photo the phone deleted meanwhile is never mistaken.
     */
    private void pickOwnToDelete() {
        final int n = Own.count(this);
        if (n == 0) { render(); return; }
        final float dp = getResources().getDisplayMetrics().density;
        final android.widget.GridView grid = new android.widget.GridView(this);
        grid.setNumColumns(5);
        grid.setVerticalSpacing(Math.round(8 * dp));
        grid.setHorizontalSpacing(Math.round(8 * dp));
        grid.setPadding(Math.round(12 * dp), Math.round(8 * dp), Math.round(12 * dp), Math.round(8 * dp));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0x00000000));
        final String[] picks = new String[n];
        final android.graphics.Bitmap[] thumbs = new android.graphics.Bitmap[n];
        for (int i = 0; i < n; i++) picks[i] = OwnPick.id(i + 1, Own.stamp(this, i + 1));
        final android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
            @Override public int getCount() { return n; }
            @Override public Object getItem(int i) { return picks[i]; }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View convert, android.view.ViewGroup parent) {
                android.widget.ImageView v = convert instanceof android.widget.ImageView ? (android.widget.ImageView) convert : new android.widget.ImageView(SettingsActivity.this);
                v.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, Math.round(84 * dp)));
                v.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                int pad = Math.round(3 * dp);
                v.setPadding(pad, pad, pad, pad);
                android.graphics.drawable.StateListDrawable ring = new android.graphics.drawable.StateListDrawable();
                android.graphics.drawable.GradientDrawable on = new android.graphics.drawable.GradientDrawable();
                on.setColor(0xFFFFFFFF);
                on.setCornerRadius(8 * dp);
                ring.addState(new int[]{android.R.attr.state_selected}, on);
                ring.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(0x00000000));
                v.setBackground(ring);
                v.setImageBitmap(thumbs[i]);
                v.setContentDescription(Words.of("صورة") + " " + (i + 1));
                return v;
            }
        };
        grid.setAdapter(adapter);
        new Thread(() -> {   // the small pictures, one by one; the grid shows each as it comes
            for (int i = 0; i < n; i++) {
                thumbs[i] = Own.thumb(this, i + 1, OWN_THUMB);
                runOnUiThread(adapter::notifyDataSetChanged);
            }
        }, "own-thumbs").start();
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        grid.setOnItemClickListener((parent, view, i, id) -> {
            if (dlg[0] != null) dlg[0].dismiss();
            confirmDeleteOne(picks[i], thumbs[i]);
        });
        dlg[0] = new Dlg(this).setTitle("اختر صورة لحذفها").setView(grid).setNegativeButton("إلغاء", null).create();
        dlg[0].setOnShowListener(x -> { dress(dlg[0]); grid.requestFocus(); grid.setSelection(0); });
        dlg[0].show();
    }

    /** the small pictures of «اختر صورة لحذفها»: their long side, in pixels */
    static final int OWN_THUMB = 240;

    private void confirmDeleteOne(String pick, android.graphics.Bitmap thumb) {
        Dlg d = new Dlg(this);
        d.setTitle("حذف هذه الصورة؟");
        if (thumb != null) {
            android.widget.ImageView v = new android.widget.ImageView(this);
            v.setImageBitmap(thumb);
            v.setAdjustViewBounds(true);
            v.setMaxHeight(Math.round(220 * getResources().getDisplayMetrics().density));
            d.setView(v);
        }
        d.setPositiveButton("نعم", (x, w) -> {
            if (!Own.deletePick(this, OwnPick.parse(pick))) toast("الصورة تغيّرت أو حُذفت من الجوال، افتح القائمة مرة ثانية");
            render();
        });
        d.setNegativeButton("إلغاء", null);
        d.show();
    }

    private void confirmClearOwn() {
        new Dlg(this).setTitle("حذف كل صوري؟")
            .setPositiveButton("حذف", (d, i) -> { Own.clear(this); render(); })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    /**
     * The TV's own idle delay. This app writes it where it may (WRITE_SETTINGS, which TVs rarely grant); otherwise the home writes it over its
     * own adb link (NowPlayingBridge.setIdle). Without that link: one line that says how, never the maker's settings (they have no such timer).
     */
    private void pickIdle() {
        String[] labels = new String[Prefs.IDLE_MINUTES.length];
        int sel = -1;
        long now = Prefs.idleMs(this);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = minutesLabel(Integer.parseInt(Prefs.IDLE_MINUTES[i]));
            if (Long.parseLong(Prefs.IDLE_MINUTES[i]) * 60000 == now) sel = i;
        }
        new Dlg(this).setTitle("بدء شاشة التوقف بعد")
            .setSingleChoiceItems(labels, sel, (d, i) -> {
                d.dismiss();
                setIdle(Prefs.IDLE_MINUTES[i]);
            })
            .show();
    }

    private void setIdle(final String minutes) {
        if (Prefs.canWriteSettings(this)) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, Integer.parseInt(minutes) * 60000);
                IdleKeep.keep(this, minutes);   // R-400: put back at boot if the firmware resets it to «never»
                render();
                return;
            } catch (SecurityException e) {
                // the home's adb link below
            }
        }
        toast("جارٍ الضبط…");
        new Thread(() -> {
            final String r = NowPlayingBridge.setIdle(getApplicationContext(), minutes);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if ("ok".equals(r)) { IdleKeep.keep(getApplicationContext(), minutes); toast("تم"); }
                else if ("busy".equals(r)) toast("إعداد التلفزيون شغّال الحين، جرّب بعده");
                else new Dlg(this).setTitle("بدء شاشة التوقف بعد").setMessage(IDLE_NO_LINK).setPositiveButton("تم", null).show();
                render();
            });
        }, "idle-set").start();
    }

    /** no adb link from the home: the one line that says how (the setup makes the link; the TV's own settings have no such timer) */
    static final String IDLE_NO_LINK = "نحتاج نضبطها من الرئيسية: شغّل «إعداد تلفزيوني» من إعدادات الرئيسية مرة ووافق على سؤال تصحيح USB، ثم اختر الوقت هنا من جديد.";

    /** «أبداً» when the TV turned its screensaver off (2147483647 ms: IdleRule.NEVER), never the raw number */
    static final String IDLE_NEVER = "أبداً (التلفزيون أوقف شاشة التوقف)";

    /** the hub's line under «شاشة التوقف»: when it starts, or that the TV turned it off */
    private String idleSummary() {
        int k = IdleRule.kind(Prefs.idleMs(this));
        if (k == IdleRule.UNKNOWN) return "يبدأ وقت ما يحدده التلفاز";
        if (k == IdleRule.NEVER) return "ما تشتغل: التلفزيون موقّف شاشة التوقف";
        return "تبدأ بعد " + idleLabel();
    }

    private String idleLabel() {
        long ms = Prefs.idleMs(this);
        switch (IdleRule.kind(ms)) {
            case IdleRule.UNKNOWN: return "يحدده التلفاز";
            case IdleRule.NEVER: return IDLE_NEVER;
            case IdleRule.SECONDS: return arabicNum((int) (ms / 1000)) + " ثانية";
            default: return minutesLabel((int) (ms / 60000));
        }
    }

    /** دقيقة واحدة · دقيقتان · ٥ دقائق · ١٥ دقيقة */
    private static String minutesLabel(int n) {
        if (n == 1) return "دقيقة واحدة";
        if (n == 2) return "دقيقتان";
        return arabicNum(n) + (n <= 10 ? " دقائق" : " دقيقة");
    }

    private void updateNow() {
        toast("جارٍ التحقق…");
        new Thread(() -> {
            final String msg = Updater.check(getApplicationContext(), true);
            runOnUiThread(() -> { toast(msg); if (!isFinishing()) render(); });
        }).start();
    }

    // ---------- labels ----------

    /** Wikidata's Arabic name (or a transliteration) fetched off the UI thread. */
    private void inArabic(final String en, final java.util.regex.Pattern kind, final Text then) {
        toast("جارٍ جلب الاسم بالعربية…");
        new Thread(() -> {
            String k = kind == Arabic.TEAM ? Arabic.known(getApplicationContext(), en) : null;
            final String ar = k != null ? k : Arabic.lookup(en, kind);
            runOnUiThread(() -> { if (!isFinishing()) then.got(ar); });
        }).start();
    }

    private static String leagueAr(String en) {
        for (String[] l : Prefs.LEAGUES) if (l[0].equalsIgnoreCase(en)) return l[1];
        return S.en() ? en : Arabic.translit(en);   // English: the league's own name, not a transliteration of it
    }

    private static String labelOf(String[][] options, String key) {
        for (String[] o : options) if (o[0].equalsIgnoreCase(key)) return o[1];
        return S.en() ? key : Arabic.translit(key);
    }

    private String teamsLabel() {
        List<String> names = new ArrayList<>();
        for (String t : prefs.teams()) names.add(teamName(t));
        return names.isEmpty() ? "لا شيء" : Prefs.join(names, "، ");
    }

    /** A followed team ("id|Arabic name") as shown: its Arabic name, or with «اللغة» English the English one of our list (data, not the table). */
    private String teamName(String saved) {
        String[] p = saved.split("\\|", 2);
        if (!S.en() || p.length < 2) return p.length < 2 ? saved : p[1];
        TeamSearch.Team t = Teams.byId(this, p[0]);
        return t != null && !t.en.isEmpty() ? t.en : p[1];
    }

    /** the «الأرقام» setting as this screen draws it (read at every render): every number and label goes through conv() */
    private static volatile boolean latinDigits;
    private boolean noConv;   // the «الأرقام» row itself says «هندية ١٢٣» and «عربية 123»: both stay as they are

    private static String conv(String s) { s = Words.of(s); return DigitsText.convert(s, latinDigits); }   // «اللغة» first (Words: the text itself in Arabic)

    private static String arabicNum(int n) { return conv(String.valueOf(n)); }

    private String showsLabel() {
        List<String> v = prefs.shows();
        if (v.isEmpty()) return "لا شيء";
        List<String> names = new ArrayList<>();
        for (String s : v) names.add(s.split("\\|", 2)[1]);
        return Prefs.join(names, "، ");
    }

    private static String labels(String[][] options, List<String> picked, String none) {
        List<String> out = new ArrayList<>();
        for (String[] o : options) if (picked.contains(o[0])) out.add(o[1]);
        return out.isEmpty() ? none : Prefs.join(out, "، ");
    }

    // ---------- small UI helpers ----------

    private final java.util.Map<String, Typeface> fonts = new java.util.HashMap<>();

    /** Thmanyah, shared with the launcher's assets; falls back to the system font. */
    private Typeface font(String name) {
        Typeface tf = fonts.get(name);
        if (tf == null) {
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/" + name + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.DEFAULT; }
            fonts.put(name, tf);
        }
        return tf;
    }

    /** A tile or a row in the glass look: GlassPaint.card (the focus fill, and its words and icons in the focus colours then). */
    private void glassCard(LinearLayout card) { GlassPaint.card(card, look, 14); }

    static android.content.res.ColorStateList focusColors(int rest, int focused) { return GlassPaint.focusColors(rest, focused); }

    // ---------- «المظهر»: «الشكل» and «الإضاءة», each with a live preview ----------

    /** «المظهر»: which of the two to choose; each opens its own list, the focused choice drawn beside it. */
    /** «تطبيقات في الشريط»: how many favourites the tray shows whole at once (5 as always, up to 10, or all of them); more scroll inside the tray */
    /** «تطبيق المباريات»: «تلقائي» (UrTV Player when it is installed), «بدون», or one of the TV's apps */
    private String matchAppLabel() {
        String v = prefs.matchApp();
        if (Prefs.MATCH_NONE.equals(v)) return "بدون";
        if (Prefs.MATCH_AUTO.equals(v)) return "تلقائي (UrTV Player إن كان مثبّتاً)";
        try { return String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(v, 0))); }
        catch (Exception e) { return v; }
    }

    private String keyAppLabel(String key) {
        String v = prefs.keyApp(key);
        if (v.isEmpty()) return "الافتراضي";
        String name;
        try { name = String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(v, 0))); }
        catch (Exception e) { name = v; }
        if (!RemoteButtons.filterSafe(Prefs.remoteTv(), android.os.Build.VERSION.SDK_INT)) return name + "، على الشاشة الرئيسية فقط في هذا التلفزيون";
        if (!extrasOn()) return name + "، على الشاشة الرئيسية فقط (وفوق التطبيقات مع «إضافات تلفزيونك»)";
        return name;
    }

    /** «إضافات تلفزيونك» is installed and Android runs its service (it answers the buttons over other apps); only read, from Android's own list */
    private boolean extrasOn() {
        try {
            getPackageManager().getPackageInfo("sa.urtv.extras", 0);
            android.view.accessibility.AccessibilityManager am = (android.view.accessibility.AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
            if (am == null) return false;
            for (android.accessibilityservice.AccessibilityServiceInfo i : am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
                if (i.getId() != null && i.getId().startsWith("sa.urtv.extras/")) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** «أزرار الريموت»: the key's own app, or one of the TV's apps (the same list as «تطبيق المباريات») */
    private void pickKeyApp(final String key, String title) {
        final java.util.List<String> values = new java.util.ArrayList<>(java.util.Collections.singletonList(""));
        final java.util.List<String> labels = new java.util.ArrayList<>(java.util.Collections.singletonList("الافتراضي"));
        android.content.pm.PackageManager pm = getPackageManager();
        java.util.TreeMap<String, String> apps = new java.util.TreeMap<>();
        for (String cat : new String[] {android.content.Intent.CATEGORY_LEANBACK_LAUNCHER, android.content.Intent.CATEGORY_LAUNCHER}) {
            for (android.content.pm.ResolveInfo r : pm.queryIntentActivities(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = r.activityInfo.packageName;
                if (pkg.startsWith("sa.urtv.") || apps.containsValue(pkg)) continue;
                apps.put(String.valueOf(r.loadLabel(pm)) + "\u0000" + pkg, pkg);
            }
        }
        for (java.util.Map.Entry<String, String> e : apps.entrySet()) { values.add(e.getValue()); labels.add(e.getKey().substring(0, e.getKey().indexOf('\u0000'))); }
        new Dlg(this).setTitle(title)
            .setSingleChoiceItems(labels.toArray(new String[0]), Math.max(0, values.indexOf(prefs.keyApp(key))), (d, i) -> {
                prefs.setKeyApp(key, values.get(i));
                d.dismiss();
                render();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void pickMatchApp() {
        final java.util.List<String> values = new java.util.ArrayList<>(java.util.Arrays.asList(Prefs.MATCH_AUTO, Prefs.MATCH_NONE));
        final java.util.List<String> labels = new java.util.ArrayList<>(java.util.Arrays.asList("تلقائي (UrTV Player إن كان مثبّتاً)", "بدون"));
        android.content.pm.PackageManager pm = getPackageManager();
        java.util.TreeMap<String, String> apps = new java.util.TreeMap<>();
        for (String cat : new String[] {android.content.Intent.CATEGORY_LEANBACK_LAUNCHER, android.content.Intent.CATEGORY_LAUNCHER}) {
            for (android.content.pm.ResolveInfo r : pm.queryIntentActivities(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = r.activityInfo.packageName;
                if (pkg.startsWith("sa.urtv.") || apps.containsValue(pkg)) continue;
                apps.put(String.valueOf(r.loadLabel(pm)) + "\u0000" + pkg, pkg);
            }
        }
        for (java.util.Map.Entry<String, String> e : apps.entrySet()) { values.add(e.getValue()); labels.add(e.getKey().substring(0, e.getKey().indexOf('\u0000'))); }
        new Dlg(this).setTitle("تطبيق المباريات")
            .setSingleChoiceItems(labels.toArray(new String[0]), Math.max(0, values.indexOf(prefs.matchApp())), (d, i) -> {
                prefs.setMatchApp(values.get(i));
                d.dismiss();
                render();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void pickFavBar() {
        final String[] labels = new String[Prefs.FAV_BARS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = Prefs.favBarLabel(Prefs.FAV_BARS[i]);
        new Dlg(this).setTitle("تطبيقات في الشريط")
            .setSingleChoiceItems(labels, java.util.Arrays.asList(Prefs.FAV_BARS).indexOf(prefs.favBar()), (d, i) -> {
                prefs.setFavBar(Prefs.FAV_BARS[i]);
                d.dismiss();
                render();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }


    /** one of a few choices, saved at once, the list drawn again */
    private void pickOne(String title, String[] labels, int at, java.util.function.IntConsumer save) {
        new Dlg(this).setTitle(title)
            .setSingleChoiceItems(labels, at, (d, k) -> { save.accept(k); d.dismiss(); render(); })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    /** «بقاء كل أفق»: 10 s, 20 s (the default), a minute or 5 minutes; a weak TV changes at most every 5 minutes whatever is chosen */
    private void pickSceneStay() {
        new Dlg(this).setTitle("بقاء كل أفق")
            .setSingleChoiceItems(Prefs.SCENE_STAY_NAMES, java.util.Arrays.asList(Prefs.SCENE_STAYS).indexOf(prefs.sceneStay()), (d, i) -> {
                prefs.setSceneStay(Prefs.SCENE_STAYS[i]);
                d.dismiss();
                render();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    /** «الواجهة»: the home's layout, «العادية» (the default), «كرة القدم» (the weather, the prayers and today's games over the same favourites bar) or «الآفاق» (4.3.3). */
    private void pickHomeLayout() {
        // 4.4 beta 2 (the owner): «tvOS» comes with the full look «tvOS» only; listed here only while it is the layout, so the row never reads empty
        final List<String> keys = new ArrayList<>(), names = new ArrayList<>();
        for (int i = 0; i < Prefs.HOME_LAYOUTS.length; i++) {
            if (Prefs.TVOS_LAYOUT.equals(Prefs.HOME_LAYOUTS[i]) && !Prefs.TVOS_LAYOUT.equals(prefs.homeLayout())) continue;
            keys.add(Prefs.HOME_LAYOUTS[i]);
            names.add(Prefs.HOME_LAYOUT_NAMES[i]);
        }
        final String[] labels = names.toArray(new String[0]);
        new Dlg(this).setTitle("التخطيط")
            .setSingleChoiceItems(labels, keys.indexOf(prefs.homeLayout()), (d, i) -> {
                prefs.setLookPart("layout", keys.get(i));   // 4.5: the look before kept, the home told at once
                d.dismiss();
                render();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    /** «مباريات اليوم» lives in the home app (the page of the games, docs/PROPOSALS-NOV2.md §1). */
    private void openMatches() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.MatchesActivity"));
        } catch (RuntimeException e) {
            toast("حدّث الواجهة الرئيسية «تلفزيونك» أو ثبّتها أولاً");
        }
    }

    /**
     * «الشكل» (shape) or «الإضاءة»: the list, and beside it a small page in the focused choice, drawn live from its palette over the one
     * backdrop (GlassSource.pageIn: no image of its own, nothing stored); OK keeps it and every screen of ours takes it.
     */
    private void pickLookPart(final boolean shape) {
        final String[] values = shape ? GlassStyle.LOOKS : Prefs.APPEARANCES;
        final String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = Words.of(shape ? GlassStyle.LOOK_NAMES[i] : Prefs.appearanceLabel(values[i]).split("، ")[0]);
        final String current = shape ? prefs.look() : prefs.appearance();
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setLayoutDirection(Dir.layout());
        box.setPadding(dp(12), dp(4), dp(12), dp(4));
        final android.widget.ListView lv = new android.widget.ListView(this);
        lv.setDividerHeight(0);
        lv.setSelector(GlassPaint.focused(this, look, 12));
        lv.setAdapter(new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            @Override public View getView(int i, View old, android.view.ViewGroup parent) {
                TextView t = (TextView) super.getView(i, old, parent);
                t.setTypeface(font("Thmanyah-Medium"));
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                t.setTextColor(focusColors(look.text, look.onFocus));
                t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                return t;
            }
        });
        box.addView(lv, new LinearLayout.LayoutParams(dp(170), dp(260)));
        final FrameLayout stage = new FrameLayout(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(360), dp(203));
        sp.setMarginStart(dp(16));
        sp.gravity = Gravity.CENTER_VERTICAL;
        box.addView(stage, sp);
        final Runnable show = () -> {
            int i = Math.max(0, lv.getSelectedItemPosition());
            stage.removeAllViews();
            String sh = shape ? values[i] : prefs.look();
            if (!shape && "auto".equals(values[i])) {   // automatic: the dark and the light side by side
                LinearLayout two = new LinearLayout(this);
                two.addView(lookPreview(GlassStyle.of(sh, false), true), new LinearLayout.LayoutParams(0, -1, 1f));
                two.addView(lookPreview(GlassStyle.of(sh, true), true), new LinearLayout.LayoutParams(0, -1, 1f));
                stage.addView(two, new FrameLayout.LayoutParams(-1, -1));
            } else {
                boolean light = shape ? look.light : "light".equals(values[i]);
                stage.addView(lookPreview(GlassStyle.of(sh, light), false), new FrameLayout.LayoutParams(-1, -1));
            }
        };
        lv.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { show.run(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        final AlertDialog dialog = new Dlg(this).setTitle(shape ? "المظهر: الشكل" : "المظهر: الإضاءة").setView(box)
            .setNegativeButton("إلغاء", null).create();
        lv.setOnItemClickListener((p, v, pos, id) -> {
            if (shape) prefs.setLook(values[pos]); else prefs.setAppearance(values[pos]);
            dialog.dismiss();
            look = GlassSource.page(this, root); couchVeil(); tvosVeil();   // this screen at once; the home and the others as they come up
            hint.setTextColor(look.sub);
            render();
        });
        dialog.setOnShowListener(x -> {
            dress(dialog);
            int at = Math.max(0, java.util.Arrays.asList(values).indexOf(current));
            lv.requestFocus();
            lv.setSelection(at);
            show.run();
        });
        dialog.show();
    }

    /** A preview's page: its words keep the colours of ITS look (dressView leaves it alone; it dresses the dialog in the look of now). */
    private static final class LookPage extends FrameLayout {
        LookPage(android.content.Context c) { super(c); }
    }

    /** A small page in a look: its title, two tiles (one focused) and a row, on the backdrop under that look's veil. */
    private View lookPreview(GlassStyle.Look l, boolean half) {
        FrameLayout page = new LookPage(this);
        page.setBackground(GlassSource.pageIn(this, l));
        page.setClipToOutline(true);
        page.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(14)); }
        });
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        col.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView title = new TextView(this);
        title.setText(Words.of(half ? (l.light ? "فاتح" : "داكن") : "الإعدادات"));
        title.setTextColor(l.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        col.addView(title);
        LinearLayout tiles = new LinearLayout(this);
        tiles.setLayoutDirection(Dir.layout());
        for (int k = 0; k < (half ? 1 : 2); k++) {
            TextView t = new TextView(this);
            t.setText(Words.of(k == 0 ? "الموقع" : "الأذان"));
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            t.setTypeface(font("Thmanyah-Medium"));
            t.setGravity(Gravity.CENTER);
            t.setTextColor(k == 0 ? l.onFocus : l.text);
            t.setBackground(k == 0 ? GlassPaint.focused(this, l, 10) : GlassPaint.panel(this, l, 10));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            if (k == 0) tp.setMarginEnd(dp(8));
            tiles.addView(t, tp);
        }
        col.addView(tiles, llp(-1, -2, dp(8)));
        TextView row = new TextView(this);
        row.setText(Words.of("المدينة، الرياض"));
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        row.setTypeface(font("Thmanyah-Regular"));
        row.setTextColor(l.sub);
        row.setPadding(dp(10), dp(6), dp(10), dp(6));
        row.setBackground(GlassPaint.panel(this, l, 8));
        col.addView(row, llp(-1, -2, dp(8)));
        page.addView(col, new FrameLayout.LayoutParams(-1, -1));
        return page;
    }

    private LinearLayout.LayoutParams llp(int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = top;
        return p;
    }

    /** the dialogs of this screen: the glass theme (styles.xml), and Thmanyah on every word of them once they show */
    private static final class Dlg extends WordsBuilder {   // its words in «اللغة» (WordsBuilder)
        private final SettingsActivity owner;
        private boolean raw;   // its words keep their own digits (the «الأرقام» picker shows both kinds)

        Dlg(SettingsActivity a) { super(a, a.look.light ? R.style.UrDialogLight : R.style.UrDialog); owner = a; }   // the glass of now

        Dlg raw() { raw = true; return this; }

        @Override Dlg own() { super.own(); return this; }

        // 4.4 «كنبة»: a plain list (one choice, ticks, or a list) opens as couch-tv's page in place of the rows, not as a dialog
        private CharSequence title;
        private CharSequence[] items;
        private int checked = -1;
        private boolean[] checks;
        private DialogInterface.OnClickListener one, item;
        private DialogInterface.OnMultiChoiceClickListener many;
        private boolean plain = true;   // no view, message or button of its own (then it stays a dialog)
        @Override public AlertDialog.Builder setTitle(CharSequence t) { title = shown(t); return super.setTitle(t); }
        @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] t, int c, DialogInterface.OnClickListener l) { items = shown(t); checked = c; one = l; return super.setSingleChoiceItems(t, c, l); }
        @Override public AlertDialog.Builder setItems(CharSequence[] t, DialogInterface.OnClickListener l) { items = shown(t); item = l; return super.setItems(t, l); }
        @Override public AlertDialog.Builder setMultiChoiceItems(CharSequence[] t, boolean[] c, DialogInterface.OnMultiChoiceClickListener l) { items = shown(t); checks = c; many = l; return super.setMultiChoiceItems(t, c, l); }
        @Override public AlertDialog.Builder setView(View v) { plain = false; return super.setView(v); }
        @Override public AlertDialog.Builder setMessage(CharSequence t) { plain = false; return super.setMessage(t); }
        @Override public AlertDialog.Builder setPositiveButton(CharSequence t, DialogInterface.OnClickListener l) { if (save == null) plain = false; return super.setPositiveButton(t, l); }
        @Override public AlertDialog.Builder setNeutralButton(CharSequence t, DialogInterface.OnClickListener l) { plain = false; return super.setNeutralButton(t, l); }
        @Override public AlertDialog.Builder setOnDismissListener(DialogInterface.OnDismissListener l) { plain = false; return super.setOnDismissListener(l); }
        @Override public AlertDialog.Builder setOnCancelListener(DialogInterface.OnCancelListener l) { if (save == null) plain = false; return super.setOnCancelListener(l); }
        @Override public AlertDialog show() {
            if (owner.cs != null && plain && !raw && items != null && (checks == null || save != null)) {
                Page p = new Page();
                p.title = title == null ? "" : title;
                p.labels = items; p.checked = checked; p.checks = checks; p.one = one; p.item = item; p.many = many; p.save = save;
                owner.showPage(p);
                return null;
            }
            return super.show();
        }

        private Runnable save;   // multi(): a list of choices with «حفظ»

        /**
         * A list of choices with «حفظ» and «إلغاء» (a user could not save «الدوريات كاملة»: on a TV the long list pushed the buttons off the
         * screen): the list gets a height that leaves the buttons on screen, DOWN on its last row goes to «حفظ», and Back saves as «حفظ»
         * does; only «إلغاء» leaves without saving (DialogFit, fitList). Called first, before the title and the items.
         */
        Dlg multi(final Runnable onSave) {
            save = onSave;
            setPositiveButton("حفظ", (d, w) -> onSave.run());
            setNegativeButton("إلغاء", null);
            setOnCancelListener(d -> onSave.run());   // Back (a button's own press dismisses, it does not cancel)
            return this;
        }

        @Override public AlertDialog create() {
            final AlertDialog d = super.create();
            d.setOnShowListener(x -> {
                owner.dress(d, raw);
                if (save != null) owner.fitList(d);
            });
            return d;
        }
    }

    void dress(AlertDialog d) { dress(d, false); }

    /** Dlg.multi: the list leaves room for the title and the buttons (DialogFit.listHeight), and DOWN on its last row goes to «حفظ». */
    void fitList(final AlertDialog d) {
        final android.widget.ListView lv = d.getListView();
        final android.widget.Button save = d.getButton(AlertDialog.BUTTON_POSITIVE);
        if (lv == null || save == null) return;   // no rows (no team followed yet): the buttons are all there is
        if (lv.getId() == View.NO_ID) lv.setId(View.generateViewId());
        for (int which : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            android.widget.Button b = d.getButton(which);
            if (b != null) b.setNextFocusUpId(lv.getId());   // UP from a button goes back to the list
        }
        lv.setOnKeyListener((v, code, ev) -> {
            if (ev.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
            // LEFT or RIGHT (a list of ticks has no use for them) goes to «حفظ» from any row, so a long list (the catalogue's hundreds) never hides it; DOWN on the last row does too
            if (code == android.view.KeyEvent.KEYCODE_DPAD_LEFT || code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) { save.requestFocus(); return true; }
            if (code != android.view.KeyEvent.KEYCODE_DPAD_DOWN) return false;
            if (!DialogFit.downToSave(lv.getSelectedItemPosition(), lv.getCount())) return false;
            save.requestFocus();
            return true;
        });
        lv.post(() -> {   // laid out by now: its width is known; its rows' height, measured, decides whether it is capped
            android.widget.ListAdapter a = lv.getAdapter();
            if (a == null || !d.isShowing()) return;
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int w = lv.getWidth() > 0 ? lv.getWidth() : dm.widthPixels / 2, content = lv.getPaddingTop() + lv.getPaddingBottom();
            int across = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), free = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < a.getCount(); i++) {
                View row = a.getView(i, null, lv);
                row.measure(across, free);
                content += row.getMeasuredHeight();
            }
            content += lv.getDividerHeight() * Math.max(0, a.getCount() - 1);
            int h = DialogFit.listHeight(content, dm.heightPixels, dm.density);
            if (h < 0) return;   // it fits: as it was
            ViewGroup.LayoutParams lp = lv.getLayoutParams();
            lp.height = h;
            lv.setLayoutParams(lp);
        });
    }

    void dress(AlertDialog d, boolean raw) {
        if (d.getWindow() == null) return;
        // the look of now on top of the dark or light theme: its dialog glass laid over its solid colour (opaque), its words, its focus
        d.getWindow().setBackgroundDrawable(GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line));
        if (d.getListView() != null) {
            android.graphics.drawable.StateListDrawable sel = new android.graphics.drawable.StateListDrawable();
            sel.addState(new int[] {android.R.attr.state_focused}, GlassPaint.focused(this, look, 12));
            sel.addState(new int[] {android.R.attr.state_pressed}, GlassPaint.focused(this, look, 12));
            sel.addState(new int[] {}, new android.graphics.drawable.ColorDrawable(0));
            d.getListView().setSelector(sel);
        }
        dressView(d.getWindow().getDecorView(), raw);
    }

    /** Thmanyah on every text under this view, and on the ones that come later (a list makes its rows when it is shown) */
    private void dressView(View v, final boolean raw) {
        if (v instanceof LookPage) return;   // a preview of a look: its own colours, its own fonts
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            t.setTypeface(font("Thmanyah-Medium"));
            if (v instanceof android.widget.Button) t.setTextColor(focusColors(look.accent, look.accent));
            else if (!(v instanceof EditText)) t.setTextColor(focusColors(look.text, look.onFocus));   // the focus ink on a focused row
            else t.setTextColor(look.text);
            if (v instanceof android.widget.CheckedTextView) ((android.widget.CheckedTextView) v).setCheckMarkTintList(focusColors(look.accent, look.onFocus));
            if (!raw && !(v instanceof EditText)) {   // the numbers of the dialogs follow the «الأرقام» setting too
                String was = t.getText().toString(), now = conv(was);
                if (!now.equals(was)) t.setText(now);
            }
        }
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) dressView(g.getChildAt(i), raw);
        g.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override public void onChildViewAdded(View parent, View child) { dressView(child, raw); }
            @Override public void onChildViewRemoved(View parent, View child) { }
        });
    }

    /** One of our backgrounds (assets/backgrounds, the screen's own photos), a different one each day, at 1/sample of its size; null if none can be read */
    private Bitmap photo(int sample) {
        try {
            List<String> names = new ArrayList<>();
            String[] all = getAssets().list("backgrounds");
            if (all != null) for (String n : all) if (n.endsWith(".jpg")) names.add(n);
            if (names.isEmpty()) return null;
            java.util.Collections.sort(names);
            InputStream in = getAssets().open("backgrounds/" + names.get((int) ((System.currentTimeMillis() / 86400000L) % names.size())));
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = sample;
                return BitmapFactory.decodeStream(in, null, o);
            } finally { try { in.close(); } catch (Exception ignored) { } }
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** A group's header: drawn only on its own category's screen (SettingsHub.HEADERS), just above the first of its rows shown there. */
    /** A group in the code: its rows with no name of their own go with its category; the headers that show are SettingsHub.ORDER's (arrange). */
    private void header(String text) {
        groupCat = SettingsHub.ofHeader(text);
    }

    private View headerView(String text) {
        if (cs != null) {   // couch-tv's group heading: small, dim, above its rows
            TextView t = new TextView(this);
            t.setText(conv(text));
            t.setTextColor(CouchStyle.DIM);
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(CouchStyle.SHEAD));
            t.setTypeface(font("Thmanyah-Medium"));
            t.setTextDirection(Dir.text());
            t.setPaddingRelative(cs.px(CouchStyle.PAD), cs.px(CouchStyle.SHEAD_TOP), cs.px(CouchStyle.PAD), cs.px(0.125f));
            t.setTag(HEADER);
            t.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            return t;
        }
        TextView t = new TextView(this);
        t.setText(conv(text));
        t.setTextColor(look.accent);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTypeface(font("Thmanyah-Medium"));
        t.setPadding(dp(10), dp(16), dp(10), dp(8));   // 32 above and 16 under a group's header: on the 8 px scale
        t.setTag(HEADER);
        t.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    private static final Object HEADER = new Object();

    /**
     * The rows of a section in SettingsHub.ORDER's order, with its headers: the code builds them in the order it grew, and a section now holds rows from
     * several places in it. A row with no entry (the prayers' minute rows) stays after the one before it; the lines before the first row stay on top,
     * the ones after (the help, the version lines) at the bottom; a header with no row under it is left out.
     */
    private void arrange() {
        if (screen == null) return;
        List<View> lead = new ArrayList<>(), tail = new ArrayList<>();
        List<double[]> key = new ArrayList<>();
        List<View> item = new ArrayList<>();
        double last = 0;
        int seq = 0;
        for (int i = 0; i < list.getChildCount(); i++) {
            View v = list.getChildAt(i);
            if (v.getTag() instanceof String) {
                int r = SettingsHub.rank(screen, (String) v.getTag());
                double at = r >= 0 ? r : last + 0.5;
                if (r >= 0) last = r;
                key.add(new double[]{at, seq++});
                item.add(v);
            } else if (v.getTag() == HEADER) {
                continue;
            } else if (item.isEmpty()) lead.add(v);
            else tail.add(v);
        }
        List<String> heads = SettingsHub.headers(screen);
        for (String h : heads) {
            key.add(new double[]{SettingsHub.rank(screen, SettingsHub.H + h), -1});
            item.add(headerView(h));
        }
        Integer[] order = new Integer[item.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> key.get(a)[0] != key.get(b)[0] ? Double.compare(key.get(a)[0], key.get(b)[0]) : Double.compare(key.get(a)[1], key.get(b)[1]));
        list.removeAllViews();
        for (View v : lead) list.addView(v);
        for (int i = 0; i < order.length; i++) {
            View v = item.get(order[i]);
            if (v.getTag() == HEADER) {   // only above a row
                boolean row = i + 1 < order.length && item.get(order[i + 1]).getTag() instanceof String;
                if (!row) continue;
            }
            list.addView(v);
        }
        for (View v : tail) list.addView(v);
    }

    /** One plain line in the list, in no button (nothing to focus): the first-run note at the top, the help under «الموقع». */
    private void line(String text, int color, int bottom) {
        TextView t = new TextView(this);
        t.setText(conv(text));
        t.setTextColor(cs != null ? 0xADFFFFFF : color);   // couch-tv's info line: 68 %
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        if (cs != null) t.setTextSize(TypedValue.COMPLEX_UNIT_PX, cs.px(1.4375f));
        t.setTypeface(font("Thmanyah-Regular"));
        t.setGravity(Gravity.START);
        t.setTextDirection(Dir.text());
        t.setPadding(dp(10), dp(8), dp(10), bottom);
        list.addView(t);
    }

    /** Plain lines at the end, in no button: nothing to focus, so the last row scrolls them into view. */
    private TextView note(View last, String... lines) {
        TextView t = new TextView(this);
        t.setText(conv(android.text.TextUtils.join("\n", lines)));
        t.setTextColor(look.sub);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(font("Thmanyah-Regular"));   // a version line has digits and Latin letters: Thmanyah has them
        t.setLineSpacing(dp(4), 1f);
        t.setGravity(Gravity.START);
        t.setTextDirection(Dir.text());
        t.setPadding(dp(6), dp(18), dp(6), dp(24));
        list.addView(t);
        final View.OnFocusChangeListener was = last.getOnFocusChangeListener();
        last.setOnFocusChangeListener((v, has) -> {
            if (was != null) was.onFocusChange(v, has);
            if (!has) return;
            // 4.3.4: the note in view, the focus left where it is. fullScroll(FOCUS_DOWN) also moves the focus to the lowest row: since 4.3.3 put
            // «النسخ التجريبية» under this row, a step down onto it jumped straight past it
            v.post(() -> t.requestRectangleOnScreen(new android.graphics.Rect(0, 0, t.getWidth(), t.getHeight())));
        });
        return t;
    }

    /**
     * The owner's pass (GameRelease, docs/GAMES-RELEASE.md): OK seven times in a row on the version lines, each within four seconds of the last, opens a box for a code. Nothing on any screen
     * says there is a pass or a group; a code that does not open it leaves nothing behind and says nothing; one that does shows «تم». The check runs off the UI thread.
     */
    private int verTaps; private long verAt;
    private void versionTap() {
        long now = System.currentTimeMillis();
        verTaps = now - verAt > 4000 ? 1 : verTaps + 1; verAt = now;
        if (verTaps < 7) return;
        verTaps = 0;
        // six square boxes, one sign each; the field that takes the typing lies over them, clear, so the remote and the keyboard work as on any field. Six signs of the code's alphabet
        // (GameCrypt.ALPHABET), capitals, and the check starts by itself at the sixth: nothing more to press, and nothing said when it is wrong.
        final int boxW = dp(52), boxH = dp(64), gap = dp(8), n = GameCrypt.CODE_LEN;
        final FrameLayout box = new FrameLayout(this);
        final LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        final TextView[] cells = new TextView[n];
        for (int k = 0; k < n; k++) {
            TextView c = new TextView(this); c.setGravity(Gravity.CENTER); c.setTextColor(look.text); c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26); c.setTypeface(font("Thmanyah-Regular"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(boxW, boxH); lp.setMarginEnd(k < n - 1 ? gap : 0); row.addView(c, lp); cells[k] = c;
        }
        final EditText in = new EditText(this);
        in.setSingleLine(true); in.setAlpha(0f); in.setCursorVisible(false); in.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        in.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.AllCaps(), (src, st, en, d, ds, de) -> { StringBuilder b = new StringBuilder(); for (int q = st; q < en; q++) if (Character.isLetterOrDigit(src.charAt(q))) b.append(src.charAt(q)); return b.length() == en - st ? null : b.toString(); }, new android.text.InputFilter.LengthFilter(n)});
        final Runnable paint = () -> {
            String t = in.getText().toString();
            for (int k = 0; k < n; k++) {
                cells[k].setText(k < t.length() ? String.valueOf(t.charAt(k)) : "");
                android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(); g.setColor(0); g.setStroke(dp(k == t.length() ? 3 : 2), k == t.length() ? look.text : look.sub);
                cells[k].setBackground(g);
            }
        };
        paint.run();
        box.addView(row, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL));
        box.addView(in, new FrameLayout.LayoutParams(-1, boxH));
        box.setPadding(dp(16), dp(12), dp(16), dp(4));
        final AlertDialog dialog = new Dlg(this).setTitle("اكتب الرمز").setView(box).setNegativeButton("إلغاء", null).create();
        in.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) {
                paint.run();
                if (e.length() == n) { String typed = e.toString(); dialog.dismiss(); GameReleaseStore.tryCode(SettingsActivity.this, typed, () -> runOnUiThread(() -> toast("تم"))); }
            }
        });
        dialog.show();
    }

    /**
     * One prayer of «تعديل وقت الصلاة»: «الفجر +٢ د». Left adds a minute and right takes one off (the reading direction of an Arabic
     * screen runs leftwards; an English one rightwards: Dir.forwardKey), from −20 to +20; OK puts it back to zero. The text changes where it is, so the focus and the scroll stay.
     */
    private void adjRow(final int k) {
        final View card = row(PrayerAdj.label(k, prefs.prayerAdj()[k]), "يسار +، يمين −، موافق للتصفير", R.drawable.ic_eye, null);
        final TextView name = (TextView) card.findViewWithTag("name");
        final Runnable show = new Runnable() { public void run() { name.setText(conv(PrayerAdj.label(k, prefs.prayerAdj()[k]))); } };
        card.setOnClickListener(v -> { int[] a = prefs.prayerAdj(); a[k] = 0; prefs.setPrayerAdj(a); show.run(); });
        card.setOnKeyListener((v, code, ev) -> {
            int d = code == Dir.forwardKey() ? 1 : code == Dir.backKey() ? -1 : 0;
            if (d == 0) return false;
            if (ev.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                int[] a = prefs.prayerAdj();
                a[k] = PrayerAdj.step(a[k], d);
                prefs.setPrayerAdj(a);
                show.run();
            }
            return true;
        });
    }

    /**
     * A card row: icon, label and its current value on a second line, chevron; the focus fill when focused. An on/off value (OnOff: «يعمل»,
     * «متوقف», «ظاهر»…) is drawn the same way everywhere: three green triangles at the row's start while it is on, and its first word in green.
     */
    private View row(String label, String value, int icon, final Runnable onClick) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutDirection(Dir.layout());
        card.setPadding(dp(20), dp(ROW_PAD), dp(16), dp(ROW_PAD));   // 4.4 the owner (build 413): rows of one line, 5 or 6 on the screen (they were 180 px)
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(FocusKey.key(label));   // which row it is when the list is built again (render keeps the focus on it)
        final String cat = SettingsHub.of(FocusKey.key(label), groupCat);   // its action on every screen (the search runs it), its views only on its own
        if (Parts.hidesRow(prefs.partsOff(), FocusKey.key(label))) return offscreen();   // 4.4 «أجزاء UrTV»: a part that is off takes its rows with it
        if (onClick != null) actions.put(FocusKey.key(label), onClick);
        if (!on(cat)) return offscreen();   // 4.4 smoother: a row of another section is not built (a section shows about 12 of the 135)
        if (cs != null) {   // «كنبة»: couch-tv's row, the same registry row (its key, its action, its section)
            final LinearLayout couch = couchRow(label, value, onClick, FocusKey.key(label));
            View toSide = sideItems.get(screen);
            if (toSide != null) { if (Dir.rtl()) couch.setNextFocusRightId(toSide.getId()); else couch.setNextFocusLeftId(toSide.getId()); }
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
            clp.bottomMargin = cs.px(CouchStyle.ROW_GAP);
            list.addView(couch, clp);
            return couch;
        }

        final int state = OnOff.state(value);
        final ImageView mark = onMark(state);
        card.addView(mark, new LinearLayout.LayoutParams(dp(9), dp(28)));
        final android.widget.ImageView ic = new android.widget.ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(icon).mutate();
        d.setTint(look.accent);
        ic.setImageDrawable(d);
        ic.setTag(GlassPaint.ICON);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(24), dp(24));
        ip.setMarginStart(dp(6));
        card.addView(ic, ip);

        // one line, one size (the owner, build 413): the name at the start, the value at the far end (a long one wraps there, tester S7), then
        // where the row shows (4.4 the owner) after it in the same line
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.HORIZONTAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);
        final TextView name = new TextView(this);
        name.setTag("name");   // adjRow changes it in place
        name.setText(noConv ? Words.of(label) : conv(label));
        name.setTextColor(look.text);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SP);
        name.setTypeface(font("Thmanyah-Medium"));
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);   // only past 60 % of the pane, which no row's name reaches
        name.setMaxWidth(Math.round((pageWidth - sideWidth) * 0.6f));
        // the audit (the owner: «ما تخلي شيء صعب»): the name kept its weight and the value up to half the pane, so names were cut («الفرق المتابع…»,
        // «تحديث تلفزي…»); now the name is whole and the value wraps beside it in what is left
        texts.addView(name, new LinearLayout.LayoutParams(-2, -2));
        String where = SettingsHub.scopeWords(FocusKey.key(label));   // 4.4 the owner: where the row shows (the home, the screensaver, both, the whole TV)
        String full = value == null ? (where.isEmpty() ? null : where) : where.isEmpty() ? value : value + "، " + where;
        final TextView val = new TextView(this);
        final String shown = full == null ? "" : noConv ? Words.of(full) : conv(full);
        if (full != null) {
            val.setText(shown);
            val.setTextColor(look.sub);
            val.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SP);
            val.setTypeface(font("Thmanyah-Regular"));
            val.setMaxLines(2);   // a long line wraps to a second one instead of being cut with «..» (tester S7); only a third is cut
            val.setEllipsize(android.text.TextUtils.TruncateAt.END);
            val.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0, -2, 1f);
            vp.setMarginStart(dp(16));
            texts.addView(val, vp);
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(dp(14));
        card.addView(texts, tp);
        final TextView chevron = new TextView(this);
        chevron.setText("›");   // shown mirrored (‹) in the RTL row
        chevron.setTextColor(look.sub);
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SP);
        LinearLayout.LayoutParams chp = new LinearLayout.LayoutParams(-2, -2);
        chp.setMarginStart(dp(10));
        card.addView(chevron, chp);

        if (onClick != null) card.setOnClickListener(v -> { launchedRow = (String) card.getTag(); onClick.run(); });   // the row that was pressed: the focus comes back to it after another screen
        paintState(val, mark, shown, state, false);
        card.setOnFocusChangeListener((v, has) -> {
            // The glass changes by itself (glassCard). 4.4 smoother: a short scale (FOCUS_SCALE in FOCUS_MS), lifted over its neighbours so it never
            // goes under them; small enough that the text stays sharp. No layout: paintState only recolours.
            paintState(val, mark, shown, state, has);
            v.setTranslationZ(has ? dp(4) : 0);
            v.animate().scaleX(has ? FOCUS_SCALE : 1f).scaleY(has ? FOCUS_SCALE : 1f).setDuration(Motion.lite(SettingsActivity.this) ? 0 : FOCUS_MS).setInterpolator(Motion.ease()).start();
            if (has) topWhenFirst(v);
            if (ts != null && ts.light) { v.animate().cancel(); v.setScaleX(1f); v.setScaleY(1f); v.setTranslationZ(0); }   // «tvOS» on a weak TV: nothing grows
        });
        glassCard(card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        if (!on(cat)) return card;
        View item = sideItems.get(screen);   // 4.3.3: toward the side list a row goes to its own section's item, never to the one beside it
        if (item != null) { if (Dir.rtl()) card.setNextFocusRightId(item.getId()); else card.setNextFocusLeftId(item.getId()); }
        list.addView(card, lp);
        return card;
    }

    /**
     * What row() gives for a row of another section: one view, never added, with the «name» text adjRow looks for. Building every row of every
     * section on each render made about 1,080 views for the 12 or so shown (and the side list rebuilt them on each focus move).
     */
    /** «شاشة التوقف»'s own section goes with its part (4.4 «أجزاء UrTV») */
    private boolean sectionGone(String cat) { return SettingsHub.SAVER.equals(cat) && prefs.partOff(Parts.SAVER); }
    /** the sections shown */
    private String[] tiles(String at) {
        List<String> out = new ArrayList<>();
        for (String c : SettingsHub.tiles(at)) if (!sectionGone(c)) out.add(c);
        return out.toArray(new String[0]);
    }

    /** «ابدأ من جديد»'s line: the start chosen, or what it does */
    private String startLabel() {
        String[] st = Parts.start(prefs.start());
        return st == null || st[4] == null ? "نظيف، كامل، كنبة، والمزيد" : st[1];
    }

    /** «أجزاء UrTV»'s line: all on, or which are off (with the adhan said when the prayers are off: the owner) */
    private String partsLabel() {
        String off = prefs.partsOff();
        if (off.isEmpty()) return "كلها مفعّلة";
        List<String> names = new ArrayList<>();
        for (String[] p : Parts.ALL) if (Parts.off(off, p[0])) names.add(p[1]);
        return String.format(Parts.off(off, Parts.PRAYER) ? "متوقف: %s، والأذان ما يشتغل" : "متوقف: %s", Prefs.join(names, "، "));
    }

    /** «أجزاء UrTV»: each part ticked while on; OK keeps the list. A part off disappears everywhere, its own settings kept for when it is back. */
    private void pickParts() {
        final String[][] all = Parts.ALL;
        String[] labels = new String[all.length];
        final boolean[] on = new boolean[all.length];
        String off = prefs.partsOff();
        for (int i = 0; i < all.length; i++) {
            labels[i] = Parts.PRAYER.equals(all[i][0]) ? String.format(java.util.Locale.ROOT, "%s (إيقافها يوقف الأذان)", all[i][1]) : all[i][1];
            on[i] = !Parts.off(off, all[i][0]);
        }
        new Dlg(this).multi(() -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < all.length; i++) if (!on[i]) v.add(all[i][0]);
                prefs.setPartsOff(Prefs.join(v, ","));
                prefs.lookChanged();   // the home and the screensaver read it at once
                render();
            })
            .setTitle("أجزاء UrTV")
            .setMultiChoiceItems(labels, on, (d, i, checked) -> on[i] = checked)
            .show();
    }

    private View offscreen() {
        if (offscreenRow == null) {
            LinearLayout v = new LinearLayout(this);
            TextView n = new TextView(this);
            n.setTag("name");
            v.addView(n);
            offscreenRow = v;
        }
        return offscreenRow;
    }
    private View offscreenRow;

    interface Text { void got(String s); }
    interface Json { void got(String json) throws Exception; }

    private void ask(String hint, final Text then) {
        final EditText in = new EditText(this);
        in.setHint(Words.of(hint));
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        new Dlg(this).setTitle("بحث").setView(in)
            .setPositiveButton("بحث", (d, w) -> {
                String q = in.getText().toString().trim();
                if (!q.isEmpty()) then.got(q);
            })
            .setNegativeButton("إلغاء", null).show();
        in.requestFocus();
    }

    /** GET on a background thread, result handled on the UI thread. */
    private void fetch(final String url, final Json then) {
        toast("جارٍ البحث…");
        new Thread(() -> {
            String body = null;
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(url).openConnection();
                NetLog.count(String.valueOf(con.getURL()));   // G3: the request log (NetLog)
                con.setConnectTimeout(12000);
                con.setReadTimeout(15000);
                try (InputStream in = con.getInputStream()) {
                    body = new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8");
                }
            } catch (Exception ignored) {
            } finally {
                if (con != null) con.disconnect();
            }
            final String result = body;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (result == null) { toast("ما فيه اتصال"); return; }
                try { then.got(result); } catch (Exception e) { toast("تعذرت قراءة النتائج"); }
            });
        }).start();
    }

    /** «النسخ التجريبية»: flips the channel; turning it off reads the normal release and, when the TV is ahead of it, says it will wait (never a downgrade) */
    private void toggleBeta() {
        final boolean wasOn = prefs.betaOn();
        prefs.setUpdateChannel(wasOn ? "latest" : "beta");
        render();
        final android.content.Context app = getApplicationContext();
        new Thread(new Runnable() { @Override public void run() {
            String v = Updater.checkLatest(app);   // the newest version of the release the TV reads now
            final int newest = Versions.buildOf(v);
            if (wasOn && newest >= 0 && newest < BuildConfig.VERSION_CODE) runOnUiThread(new Runnable() { public void run() { toast(Updater.BACK_LINE); render(); } });
            else if (wasOn) runOnUiThread(new Runnable() { public void run() { render(); } });
        } }).start();
    }

    private void toast(String s) { Toast.makeText(this, conv(s), Toast.LENGTH_SHORT).show(); }

    /**
     * 4.4 the app's own one-line notice, for a note that needs no answer (the home card swapped for one that suits the look): bottom-centre,
     * a small-radius rectangle (never a capsule: the owner's rule; the system Toast is one on some TVs), it fades out after 4 s and takes no key.
     */
    private TextView noticeView;
    private final Runnable hideNotice = () -> { if (noticeView != null) noticeView.animate().alpha(0f).setDuration(300).start(); };
    /** the reason under a choice shown off (CardFit), a warm tone readable on the glass and on the focus fill */
    static final int OFF_REASON = 0xFFE0A07F;

    private void notice(String s) {
        if (root == null) return;
        if (noticeView == null) {
            noticeView = new TextView(this);
            noticeView.setSingleLine(true);
            noticeView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            noticeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            noticeView.setTypeface(font("Thmanyah-Regular"));
            noticeView.setTextColor(0xFFF1EBDD);
            noticeView.setPadding(dp(22), dp(12), dp(22), dp(12));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(0xF02B2E36);
            noticeView.setBackground(bg);
            noticeView.setFocusable(false);
            noticeView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            noticeView.setAlpha(0f);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.bottomMargin = dp(36);
            root.addView(noticeView, lp);
        }
        noticeView.setText(conv(s));
        noticeView.bringToFront();
        noticeView.removeCallbacks(hideNotice);
        noticeView.animate().cancel();
        noticeView.animate().alpha(1f).setDuration(200).start();
        noticeView.postDelayed(hideNotice, 4000);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
