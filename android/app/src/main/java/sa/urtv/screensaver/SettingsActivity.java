package sa.urtv.screensaver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
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
    private String groupCat, pendingHeader;   // while render runs: the category of the group being built, its header not drawn yet
    private TextView hint;
    private LinearLayout side;       // a category's screen: its title and its line on the right, the rows on the left (hidden on the first)
    private final java.util.Map<String, String> lastRow = new java.util.HashMap<>();   // each category's row last focused, for the way back in

    /** Arabic locale for this screen, so every dialog title, list and button is laid out right-to-left. */
    @Override
    protected void attachBaseContext(android.content.Context base) {
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar"));
        c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
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
        UpdateJob.schedule(this);
        // the glass look of all our screens: the home's photo, small and blurred once (GlassSource), under its veil; glass tiles and rows
        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        look = GlassSource.page(this, root);
        scroll = new UpDownScroll(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        list.setPadding(dp(8), dp(40), dp(8), dp(16));
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(144), dp(1100));
        frame.addView(list, new FrameLayout.LayoutParams(-1, -2));
        scroll.addView(frame);
        LinearLayout panes = new LinearLayout(this);   // right to left: the side (a category's title), then the rows
        panes.setOrientation(LinearLayout.HORIZONTAL);
        panes.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        side.setPadding(dp(8), dp(48), dp(8), dp(16));
        // fixed widths, worked out once (no weights): the side and the rows never move sideways (the TV test of 3.147: after 5 down and
        // 5 up the list went from the left half to the right half and back); the rows' width changes only between the categories and a category
        sideWidth = Math.round(width * 0.36f) - dp(36);
        pageWidth = width;
        LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(sideWidth, -1);
        sideLp.setMarginEnd(dp(36));
        panes.addView(side, sideLp);
        panes.addView(scroll, new LinearLayout.LayoutParams(width - sideWidth - dp(36), -1));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams panesLp = new LinearLayout.LayoutParams(width, 0, 1f);
        panesLp.gravity = Gravity.CENTER_HORIZONTAL;
        column.addView(panes, panesLp);
        hint = new TextView(this);
        hint.setTextColor(look.sub);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setTypeface(font("Thmanyah-Regular"));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(72), dp(10), dp(72), dp(18));
        column.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        render();
    }

    /** A picker's live preview (a WebView running the page) is let go while another app is in front, and drawn again on return; null when no picker is open. */
    private Runnable previewOff, previewOn;

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
        look = GlassSource.page(this, root);   // the home's photo or «المظهر» may have changed meanwhile
        hint.setTextColor(look.sub);
        render();
    }

    /** Back on a category's rows goes back to the categories, the focus on its tile; on the categories it leaves. */
    @Override
    public void onBackPressed() {
        if (screen == null) { super.onBackPressed(); return; }
        View f = focusedRow();
        if (f != null) lastRow.put(screen, (String) f.getTag());   // the way back in starts there
        hubAt = screen;
        open(null);
    }

    /** Shows the categories (null) or one category's rows, from the top. */
    private void open(String category) {
        if (category != null) hubAt = category;
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
     * The first row (on the categories: the first line of tiles) brings the page back to its very top, so the title and the line over
     * it show again (the TV test: after going down and up again, the header stayed out of sight). After the scroll view's own move.
     */
    private void topWhenFirst(View v) {
        int at = rows().indexOf(v);
        if (at == 0 || (screen == null && at == 1)) scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private boolean on(String category) { return screen != null && screen.equals(category); }

    /**
     * Builds the list again (on every change, and back in front): the focus stays on the row that had it, by its key (FocusKey), and the
     * list stays where it was scrolled to. A screen just opened starts at its top: the first row, or on the categories the tile last
     * opened (the first one the first time).
     */
    private void render() {
        latinDigits = DigitsText.isLatin(prefs.digits());
        List<View> before = rows();
        View had = entering ? null : focusedRow();
        final String want = entering ? (screen == null ? (hubAt != null ? "hub:" + hubAt : null) : lastRow.get(screen)) : had == null ? null : (String) had.getTag();
        final int wasIndex = had == null ? 0 : before.indexOf(had), scrollY = entering ? 0 : scroll.getScrollY();
        entering = false;
        groupCat = null;
        pendingHeader = null;
        hint.setText(screen == null ? "الأسهم بين الأقسام  •  اختيار لفتح القسم  •  رجوع للخروج"
            : "أعلى/أسفل بين الصفوف  •  اختيار لفتح الصف  •  رجوع للأقسام");
        list.removeAllViews();
        sidePane();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, 0, 0, dp(14));
        TextView title = new TextView(this);
        title.setText("تلفزيونك");
        title.setTextColor(look.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        head.addView(title);
        TextView sub = new TextView(this);
        sub.setText(screen == null ? "الإعدادات" : SettingsHub.name(screen));
        sub.setTextColor(look.sub);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        sub.setTypeface(font("Thmanyah-Regular"));
        LinearLayout.LayoutParams sp0 = new LinearLayout.LayoutParams(-2, -2);
        sp0.setMarginStart(dp(20));
        head.addView(sub, sp0);
        if (screen == null) list.addView(head);   // a category's screen has its title on the side instead
        if (screen == null || on(SettingsHub.PLACE))   // on the categories and on «الموقع والصلاة»
            if (prefs.cityNoteDue()) line(Location.note(prefs.city()[0]), look.accent, dp(14));   // still the install's Riyadh: say where to change it
        if (screen == null) { hub(want, wasIndex); return; }

        row("معاينة الشاشة", "شاهد الشاشة الآن", R.drawable.ic_play, new Runnable() { public void run() {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class)); } });

        // the place, then how its times are worked out, then the minutes of one's own mosque; the line under them says which is which
        header("الموقع");
        row("الدولة", Location.countryLabel(prefs.cityTz()), R.drawable.ic_place, new Runnable() { public void run() { pickCountry(); } });
        row("المدينة", prefs.city()[0], R.drawable.ic_place, new Runnable() { public void run() { pickCity(); } });

        row("طريقة الحساب", PrayerMethod.label(prefs.prayerMethod(), prefs.cityTz()), R.drawable.ic_place,
            new Runnable() { public void run() { pickPrayerMethod(); } });
        row("تعديل وقت الصلاة", prefs.prayerAdjOn() ? "يعمل · لكل صلاة دقائقها، من −٢٠ إلى +٢٠ (الشروق لا يتغيّر)" : "متوقف · الأوقات كما تُحسب", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPrayerAdjOn(!prefs.prayerAdjOn()); render(); } });
        if (prefs.prayerAdjOn()) for (int k = 0; k < PrayerAdj.COUNT; k++) adjRow(k);
        if (on(SettingsHub.PLACE)) line(Location.HELP, look.sub, 0);

        header("المباريات");
        row("الفرق المتابعة (" + arabicNum(prefs.teams().size()) + "/" + arabicNum(Prefs.MAX_TEAMS) + ")", teamsLabel(),
            R.drawable.ic_trophy, new Runnable() { public void run() { manageTeams(); } });

        row("الدوريات كاملة", leaguesLabel(), R.drawable.ic_trophy,
            new Runnable() { public void run() { pickLeagues(); } });

        row("رياضات أخرى", labels(Prefs.EVENTS, prefs.list("events"), "بدون"), R.drawable.ic_trophy,
            new Runnable() { public void run() { pickMany("events", "رياضات أخرى", Prefs.EVENTS, "بدون اختيار = بدون"); } });

        header("العرض");
        noConv = true;
        row("الأرقام", Prefs.digitsLabel(prefs.digits()), R.drawable.ic_language,
            new Runnable() { public void run() { pickDigits(); } });
        noConv = false;
        row("تنبيه قبل الأذان", prefs.adhanBar() ? "يعمل · شريط في أعلى الشاشة آخر ١٠ دقائق، ثم حتى الإقامة" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setAdhanBar(!prefs.adhanBar()); render(); } });
        // from PR #28 and the requests: each off until chosen, OK flips it
        row("الشروق بعد الفجر", prefs.sunrise() ? "يعمل · بعد إقامة الفجر: كم باقي على الشروق، ثم نصف ساعة بعده" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setSunrise(!prefs.sunrise()); render(); } });
        row("شريط الوقت بين الصلاتين", prefs.prayerBar() ? "يعمل · خط رفيع تحت الصلاة القادمة يمتلئ حتى وقتها" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setPrayerBar(!prefs.prayerBar()); render(); } });
        row("الرطوبة والرياح", prefs.wxMore() ? "يعمل · تحت المدينة أعلى اليسار" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setWxMore(!prefs.wxMore()); render(); } });
        row("أسماء الأشهر الميلادية", Prefs.monthsLabel(prefs.months()), R.drawable.ic_language,
            new Runnable() { public void run() { prefs.setMonths("levant".equals(prefs.months()) ? "default" : "levant"); render(); } });
        row("شريط السنة تحت الساعة", prefs.yearStrip() ? "يعمل · نقاط السنة تحت الساعة، واليوم يلمع" : "متوقف", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setYearStrip(!prefs.yearStrip()); render(); } });
        row("المناسبات الوطنية", Prefs.nationalLabel(prefs.national()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickNational(); } });
        row("حجم الساعة", Prefs.clockSizeLabel(prefs.clockSize()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickClockSize(); } });
        row("الخانة اليسرى", Prefs.slotLabel(prefs.slot()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickSlot(); } });
        if ("countdown".equals(prefs.slot()) || !prefs.countdownDate().isEmpty())
            row("العدّ التنازلي", countdownLabel(), R.drawable.ic_eye,
                new Runnable() { public void run() { manageCountdown(); } });

        header("الأسواق (أعلى اليسار)");
        row("شريط الأسواق", prefs.marketsOn() ? "يعمل" : "مخفي · يبقى طقس اليوم فقط", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setMarketsOn(!prefs.marketsOn()); render(); } });
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

        header("الشاشة الرئيسية (تلفزيونك)");
        // «المظهر», the first row: «الشكل» (glass, graphite, navy, clean) and «الإضاءة» (dark, light, auto), each picked with a live preview
        row("المظهر", Prefs.lookLabel(prefs.look()) + " · " + Prefs.appearanceLabel(prefs.appearance()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickLook(); } });
        // up to three cards left of the clock; each place takes any card, or «بدون» (the ones after it move up)
        for (int k = 0; k < Prefs.HOME_CARDS; k++) {
            final int place = k;
            row(HOME_CARD_NAMES[k], Prefs.panelLabel(prefs.homePanelAt(k), "بدون"), R.drawable.ic_eye,
                new Runnable() { public void run() { pickPanel(place); } });
        }
        row("التحية والطقس", prefs.greeting() ? "تعمل" : "متوقفة", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setGreeting(!prefs.greeting()); render(); } });
        // the adhan's settings (on or off, the voice, the Fajr voice, short, any key stops it, the volume): the same screen as the player's adhan tab,
        // opened through the home (behind its family lock), so they are reachable with the «تلاوة وأذان» button hidden too
        row("الأذان", "تشغيله وصوته وصوت الفجر والمختصر والإيقاف بأي زر والمستوى", R.drawable.ic_settings,
            new Runnable() { public void run() { openAdhanSettings(); } });
        // hiding removes only the button; the help line says where the screen opens from then
        row("زر تلاوة وأذان", prefs.listenPill() ? "ظاهر" : "مخفي  •  الأذان والتلاوة تعمل، وتفتح الشاشة من قائمة التطبيق (اختيار مطوّلة) أو من بطاقة «يُتلى الآن»", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setListenPill(!prefs.listenPill()); render(); } });
        // the bell shows only while the Play Store has updates waiting (the home screen asks on return, at most every 6 hours)
        row("جرس التحديثات", prefs.updateBell() ? "ظاهر عند وجود تحديثات للتطبيقات" : "مخفي", R.drawable.ic_eye,
            new Runnable() { public void run() { prefs.setUpdateBell(!prefs.updateBell()); render(); } });
        row("ترتيب التطبيقات المفضلة", "قائمة ترتّب فيها المفضلة بالأسهم", R.drawable.ic_star,
            new Runnable() { public void run() { openDockSettings(); } });
        // «أكمل المشاهدة»: on or off; turned on while the home app may not read the TV's list yet, it opens the home app's one question
        row("أكمل المشاهدة", watchNextLabel(), R.drawable.ic_play,
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
        row("تحكم من الجوال", "تشغيله وإيقافه وربط جوال، وفي الشاشة حالته الآن", R.drawable.ic_settings,
            new Runnable() { public void run() { openPhoneControl(); } });
        // «كل التطبيقات»: the home screen's «تعديل التطبيقات», or the order by name again (what is hidden stays hidden)
        row("ترتيب كل التطبيقات", "تحريكها وإخفاؤها على الشاشة الرئيسية، أو الرجوع للترتيب الأصلي", R.drawable.ic_star,
            new Runnable() { public void run() { pickAppsOrder(); } });
        row("تحديث تلفزيوني", "يعرض الإصدار ويتحقق من التحديث", R.drawable.ic_sync,
            new Runnable() { public void run() { openUpdate(); } });

        row("حماية العائلة", "تفعيل أو إيقاف، ويرجع الإعداد القديم تلقائياً", R.drawable.ic_eye,
            new Runnable() { public void run() { openFamilyDns(); } });

        if ("shows".equals(prefs.panel()) || prefs.homePanels().contains("shows")) {   // only matter for the shows card
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
        }

        header("التوقيت");
        row("تبديل الصورة كل", minutesLabel(Integer.parseInt(prefs.photoMinutes())), R.drawable.ic_sync,
            new Runnable() { public void run() { pickPhotoMinutes(); } });
        row("صوري (" + arabicNum(Own.count(this)) + "/" + arabicNum(Own.MAX) + ")", ownLabel(), R.drawable.ic_eye,
            new Runnable() { public void run() { pickOwn(); } });
        row("بدء شاشة التوقف بعد", idleLabel(), R.drawable.ic_screensaver,
            new Runnable() { public void run() { pickIdle(); } });

        header("التطبيق");
        row("التحديث التلقائي للصفحة", prefs.autoUpdate() ? "يعمل · عند بدء الشاشة، كل ٦ ساعات" : "متوقف", R.drawable.ic_sync, new Runnable() { public void run() {
            prefs.setAutoUpdate(!prefs.autoUpdate()); render(); } });
        View last = row("تحديث الشاشة الآن", "ينزّل أحدث نسخة من الشاشة", R.drawable.ic_sync, new Runnable() { public void run() { updateNow(); } });
        if (last.getParent() == list) note(last, Updater.versionLines(this));
        restoreFocus(want, wasIndex, scrollY);   // a screen just opened: its first row
    }

    /** The side of a category's screen: its icon, its name, its line, and the way back; nothing on the first screen. */
    private void sidePane() {
        side.removeAllViews();
        side.setVisibility(screen == null ? View.GONE : View.VISIBLE);
        int rowsWidth = screen == null ? pageWidth : pageWidth - sideWidth - dp(36);   // the categories: the whole width; a category: beside the side
        if (scroll.getLayoutParams() != null && scroll.getLayoutParams().width != rowsWidth) {
            scroll.getLayoutParams().width = rowsWidth;
            scroll.requestLayout();
        }
        if (screen == null) return;
        int at = SettingsHub.index(screen);
        final ImageView ic = new ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(at >= 0 && at < HUB_ICONS.length ? HUB_ICONS[at] : R.drawable.ic_settings).mutate();
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
        a.setTextDirection(View.TEXT_DIRECTION_RTL);
        side.addView(a);
        TextView up = new TextView(this);
        up.setText("رجوع إلى الأقسام");   // words, not «‹›» (mirrored or not by the renderer)
        up.setTextColor(look.sub);
        up.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        up.setTypeface(font("Thmanyah-Regular"));
        up.setPadding(0, dp(22), 0, 0);
        side.addView(up);
    }

    private static final int[] HUB_ICONS = {R.drawable.ic_place, R.drawable.ic_settings, R.drawable.ic_screensaver, R.drawable.ic_category,
        R.drawable.ic_trophy, R.drawable.ic_star, R.drawable.ic_eye, R.drawable.ic_sync};   // in the order of SettingsHub.CATEGORIES

    /** The first screen: the categories, two big tiles to a row from the right, each with what is set in it now. */
    private void hub(String want, int wasIndex) {
        final String[][] cats = SettingsHub.CATEGORIES;
        final List<View> tiles = new ArrayList<>();
        LinearLayout line = null;
        for (int i = 0; i < cats.length; i++) {
            if (i % 2 == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                list.addView(line, new LinearLayout.LayoutParams(-1, -2));
            }
            final String cat = cats[i][0];
            View t = tile(cat, cats[i][1], i < HUB_ICONS.length ? HUB_ICONS[i] : R.drawable.ic_settings, hubSummary(cat));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.bottomMargin = dp(12);
            if (i % 2 == 0) lp.setMarginEnd(dp(12));
            line.addView(t, lp);
            tiles.add(t);
        }
        if (cats.length % 2 == 1) line.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));   // a last tile alone keeps its width
        for (int i = 0; i < tiles.size(); i++) {   // explicit ways (SettingsHub.next): nothing wraps, and right is right in the RTL rows
            View t = tiles.get(i);
            t.setNextFocusUpId(tiles.get(SettingsHub.next(i, tiles.size(), 0)).getId());
            t.setNextFocusDownId(tiles.get(SettingsHub.next(i, tiles.size(), 1)).getId());
            t.setNextFocusLeftId(tiles.get(SettingsHub.next(i, tiles.size(), 2)).getId());
            t.setNextFocusRightId(tiles.get(SettingsHub.next(i, tiles.size(), 3)).getId());
        }
        restoreFocus(want, wasIndex, 0);
    }

    /** What is set now in a category, on one line under its name. */
    private String hubSummary(String cat) {
        switch (cat) {
            case SettingsHub.PLACE: return prefs.city()[0] + " · " + PrayerMethod.label(prefs.prayerMethod(), prefs.cityTz());
            case SettingsHub.ADHAN: return "صوته ومستواه · التنبيه قبل الأذان " + (prefs.adhanBar() ? "يعمل" : "متوقف");
            case SettingsHub.SAVER: return (Prefs.idleMs(this) <= 0 ? "يبدأ وقت ما يحدده التلفاز" : "تبدأ بعد " + idleLabel()) + " · " + Prefs.panelLabel(prefs.panel());
            case SettingsHub.HOME: {
                List<String> cards = new ArrayList<>();
                for (String k : prefs.homePanels()) cards.add(Prefs.panelLabel(k, "بدون"));
                return cards.isEmpty() ? "بدون بطاقات · التحية والطقس" : Prefs.join(cards, "، ");
            }
            case SettingsHub.CONTENT: return "الفرق " + arabicNum(prefs.teams().size()) + " · الأسواق " + (prefs.marketsOn() ? arabicNum(prefs.markets().size()) : "مخفية");
            case SettingsHub.APPS: return "ترتيب المفضلة وتعديلها، وأسماء المصادر";
            case SettingsHub.FAMILY: return "حماية العائلة: تفعيل أو إيقاف";
            case SettingsHub.PHONE: return "التحكم من الجوال · " + Updater.versionLines(this)[0];
            default: return "";
        }
    }

    /** A category tile: a big icon, the name, the value now; gold when focused, OK opens the category. */
    private View tile(final String cat, String name, int icon, String summary) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setGravity(Gravity.START);
        card.setPadding(dp(26), dp(22), dp(26), dp(22));
        card.setFocusable(true);
        card.setClickable(true);
        card.setId(View.generateViewId());
        card.setTag("hub:" + cat);
        final ImageView ic = new ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(icon).mutate();
        d.setTint(look.accent);
        ic.setImageDrawable(d);
        ic.setTag(GlassPaint.ICON);
        card.addView(ic, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView n = new TextView(this);
        n.setText(conv(name));
        n.setTextColor(look.text);
        n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        n.setTypeface(font("Thmanyah-Medium"));
        n.setSingleLine(true);
        n.setPadding(0, dp(12), 0, dp(2));
        card.addView(n);
        TextView v = new TextView(this);
        v.setText(conv(summary));
        v.setTextColor(look.sub);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        v.setTypeface(font("Thmanyah-Regular"));
        v.setSingleLine(true);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v.setTextDirection(View.TEXT_DIRECTION_RTL);
        card.addView(v);
        card.setOnClickListener(x -> open(cat));
        card.setOnFocusChangeListener((x, has) -> { if (has) topWhenFirst(x); });
        glassCard(card);
        return card;
    }

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
            .setSingleChoiceItems(labels, Location.countryOf(prefs.cityTz()), (d, i) -> { d.dismiss(); pickCity(i); })
            .show();
    }

    private void pickCity() { pickCity(-1); }

    /** country: an index of Location.COUNTRIES, or -1 for every region of the list and a search of all the searchable countries */
    private void pickCity(final int country) {
        final Cities.Region[] regions = country < 0 ? Cities.REGIONS : Location.regions(country);
        if (country > 0 && regions.length == 1) { pickCityIn(regions[0], country); return; }   // any other country is one region: its cities
        String[] labels = new String[regions.length + 1];
        labels[0] = "بحث عن مدينة أو قرية…";
        for (int i = 0; i < regions.length; i++) labels[i + 1] = regions[i].name + "  (" + regions[i].cities.length + ")";
        new Dlg(this).setTitle(country < 0 ? "المنطقة" : Location.COUNTRIES[country][1])
            .setItems(labels, (d, i) -> { if (i == 0) searchCity(country); else pickCityIn(regions[i - 1], country); })
            .show();
    }

    private void pickCityIn(final Cities.Region r, final int country) {
        String[] names = new String[r.cities.length];
        int sel = -1;
        for (int i = 0; i < names.length; i++) {
            names[i] = r.cities[i][0];
            if (names[i].equals(prefs.city()[0])) sel = i;
        }
        AlertDialog.Builder b = new Dlg(this).setTitle(r.name)
            .setSingleChoiceItems(names, sel, (d, i) -> {
                prefs.setCity(r.cities[i][0], r.cities[i][1], r.cities[i][2]);   // the zone follows from the list (Prefs.cityTz)
                d.dismiss();
                render();
            });
        if (country > 0 && Location.searchable(country)) b.setNeutralButton("بحث", (d, w) -> searchCity(country));   // a place not in the list, in that country (not Palestine: PrayerMethod)
        b.show();
    }

    // any place in Saudi Arabia, the other Gulf states, Egypt, the Levant and North Africa (PrayerMethod.SEARCH_COUNTRIES), or in the one
    // country picked, from Open-Meteo's GeoNames search (it takes one country at a time, so without a country the hundred best matches of
    // the world are asked for and those countries' kept)
    private void searchCity(final int country) {
        ask("اسم المدينة أو القرية", q -> fetch("https://geocoding-api.open-meteo.com/v1/search?count=100&language=ar&name="
            + Uri.encode(q) + (country < 0 ? "" : "&countryCode=" + Location.COUNTRIES[country][0]), json -> {
                JSONArray all = new JSONObject(json).optJSONArray("results"), a = new JSONArray();
                for (int k = 0; all != null && k < all.length() && a.length() < 20; k++)
                    if (PrayerMethod.searchable(all.getJSONObject(k).optString("country_code"))) a.put(all.getJSONObject(k));
                if (a.length() == 0) { toast("لا توجد نتائج"); return; }
                final int n = a.length();
                final String[] names = new String[n], lats = new String[n], lons = new String[n], zones = new String[n], rows = new String[n];
                for (int i = 0; i < n; i++) {
                    JSONObject c = a.getJSONObject(i);
                    names[i] = c.optString("name");
                    lats[i] = String.valueOf(c.optDouble("latitude"));
                    lons[i] = String.valueOf(c.optDouble("longitude"));
                    zones[i] = c.optString("timezone", "");   // the place's own; empty: the TV's (Prefs.cityTz)
                    String region = c.optString("admin1", "");
                    String land = c.optString("country", "");   // several countries now: the row says which
                    rows[i] = names[i] + (region.isEmpty() ? "" : "  ·  " + region) + (land.isEmpty() ? "" : "  ·  " + land);
                }
                new Dlg(this).setTitle("اختر المكان")
                    .setItems(rows, (d, i) -> { prefs.setCity(names[i], lats[i], lons[i], zones[i]); render(); }).show();
            }));
    }

    /** Followed teams: untick to remove, or add one (presets, league browsing, search). Up to 20. */
    private void manageTeams() {
        final List<String> cur = prefs.teams();
        String[] labels = new String[cur.size()];
        final boolean[] keep = new boolean[cur.size()];
        for (int i = 0; i < cur.size(); i++) { labels[i] = cur.get(i).split("\\|", 2)[1]; keep[i] = true; }
        AlertDialog.Builder b = new Dlg(this)
            .setTitle(cur.isEmpty() ? "لا توجد فرق متابعة" : "الفرق المتابعة (ألغِ التحديد للحذف)")
            .setPositiveButton("حفظ", (d, w) -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < cur.size(); i++) if (keep[i]) v.add(cur.get(i));
                prefs.setList("teams", v);
                render();
            })
            .setNegativeButton("إلغاء", null);
        if (cur.size() < Prefs.MAX_TEAMS) b.setNeutralButton("إضافة فريق", (d, w) -> pickTeam(0));
        if (!cur.isEmpty()) b.setMultiChoiceItems(labels, keep, (d, i, checked) -> keep[i] = checked);
        b.show();
    }

    private void addTeam(String id, String name) {
        if (prefs.addTeam(id, name)) toast("تمت متابعة " + name);
        else toast(prefs.teams().size() >= Prefs.MAX_TEAMS ? "الحد الأعلى ٢٠ فريقاً" : "متابَع مسبقاً");
        render();
    }

    private void pickTeam(final int slot) {
        final List<String[]> opts = new ArrayList<>();
        for (String[] t : Prefs.TEAM_PRESETS) opts.add(t);
        String[] labels = new String[opts.size() + 2];
        for (int i = 0; i < opts.size(); i++) labels[i] = opts.get(i)[1];
        labels[opts.size()] = "تصفح الدوريات…";
        labels[opts.size() + 1] = "بحث عن أي فريق أو منتخب…";
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
                String league = i < Prefs.LEAGUES.length ? Prefs.LEAGUES[i][0] : Teams.NATIONAL;
                List<TeamSearch.Team> clubs = Teams.inLeague(this, league);
                if (!clubs.isEmpty()) showOurs(clubs, labels[i], false);
                else if (i < Prefs.LEAGUES.length)
                    fetch(SPORTS + "search_all_teams.php?l=" + Uri.encode(league), json -> showTeams(slot, json, labels[i], league));
                else toast("تعذّر تحميل القائمة، جرّب البحث عن الفريق");
            }).show();
    }

    private void searchTeam(final int slot) {
        ask("اسم الفريق بالعربية أو الإنجليزية (مثل الهلال أو \u2066Real Madrid\u2069)", q -> {
            List<TeamSearch.Team> found = Teams.search(this, q);
            if (!found.isEmpty()) showOurs(found.subList(0, Math.min(found.size(), 30)), "اختر الفريق", true);
            else if (!Arabic.hasArabic(q))   // a club outside our list: TheSportsDB's search, in English only
                fetch(SPORTS + "searchteams.php?t=" + Uri.encode(q), json -> showTeams(slot, json, "اختر الفريق", null));
            else toast("لا توجد نتائج، جرّب اسم الفريق بالإنجليزية");
        });
    }

    /** Clubs from our list: their Arabic names are already there, so following one needs no network. */
    private void showOurs(final List<TeamSearch.Team> clubs, String title, boolean withLeague) {
        String[] rows = new String[clubs.size()];
        for (int i = 0; i < rows.length; i++) {
            TeamSearch.Team t = clubs.get(i);
            rows[i] = Teams.label(this, t);
            if (withLeague && t.leagues.length > 0)
                rows[i] += "  ·  " + (Teams.NATIONAL.equals(t.leagues[0]) ? "منتخب" : leagueAr(t.leagues[0]));
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
                + "  ·  " + leagueAr(t.optString("strLeague", ""));
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
        new Dlg(this).setTitle(title + " (" + hint + ")")
            .setMultiChoiceItems(labels, on, (d, i, checked) -> on[i] = checked)
            .setPositiveButton("حفظ", (d, w) -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < options.length; i++) if (on[i]) v.add(options[i][0]);
                prefs.setList(key, v);
                render();
            })
            .setNegativeButton("إلغاء", null).show();
    }

    /** Whole leagues: every game of every club in them is shown, up to Prefs.MAX_LEAGUES leagues. */
    private String leaguesLabel() {
        List<String> out = new ArrayList<>();
        for (String en : prefs.leagues()) out.add(leagueAr(en));
        return out.isEmpty() ? "بدون" : Prefs.join(out, "، ");
    }

    private void pickLeagues() {
        final List<String> cur = prefs.leagues();
        final String[] labels = new String[Prefs.LEAGUES.length];
        final boolean[] on = new boolean[Prefs.LEAGUES.length];
        for (int i = 0; i < labels.length; i++) { labels[i] = Prefs.LEAGUES[i][1]; on[i] = cur.contains(Prefs.LEAGUES[i][0]); }
        new Dlg(this)
            .setTitle("الدوريات كاملة (حتى " + arabicNum(Prefs.MAX_LEAGUES) + "، بدون اختيار = بدون)")
            .setMultiChoiceItems(labels, on, (d, i, checked) -> {
                int n = 0;
                for (boolean b : on) if (b) n++;
                if (checked && n > Prefs.MAX_LEAGUES) {
                    on[i] = false;
                    ((AlertDialog) d).getListView().setItemChecked(i, false);
                    toast("حتى " + arabicNum(Prefs.MAX_LEAGUES) + " دوريات");
                }
            })
            .setPositiveButton("حفظ", (d, w) -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < on.length; i++) if (on[i]) v.add(Prefs.LEAGUES[i][0]);
                prefs.setList("leagues", v);
                render();
            })
            .setNegativeButton("إلغاء", null).show();
    }

    private void manageShows() {
        final List<String> cur = prefs.shows();
        String[] labels = new String[cur.size()];
        final boolean[] keep = new boolean[cur.size()];
        for (int i = 0; i < cur.size(); i++) { labels[i] = cur.get(i).split("\\|", 2)[1]; keep[i] = true; }
        AlertDialog.Builder b = new Dlg(this)
            .setTitle(cur.isEmpty() ? "لا توجد مسلسلات مختارة" : "المسلسلات المختارة (ألغِ التحديد للحذف)")
            .setPositiveButton("حفظ", (d, w) -> {
                List<String> v = new ArrayList<>();
                for (int i = 0; i < cur.size(); i++) if (keep[i]) v.add(cur.get(i));
                prefs.setList("shows", v);
                render();
            })
            .setNeutralButton("إضافة مسلسل", (d, w) -> searchShow())
            .setNegativeButton("إلغاء", null);
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
                    for (String s : prefs.shows()) if (s.startsWith(ids[i] + "|")) { toast("مضاف مسبقاً"); return; }
                    List<String> v = prefs.shows();
                    v.add(ids[i] + "|" + names[i]);
                    prefs.setList("shows", v);
                    toast("تمت إضافة " + names[i]);
                    render();
                }).show();
        }));
    }

    /** «حماية العائلة (DNS)» lives in the launcher too: it holds the permission to write the Private DNS setting. */
    private void openFamilyDns() {
        try {
            startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.FamilyDnsActivity"));
        } catch (SecurityException | ActivityNotFoundException e) {   // permission not granted, or an old launcher without the activity
            boolean installed = true;
            try { getPackageManager().getPackageInfo("sa.urtv.home", 0); }
            catch (android.content.pm.PackageManager.NameNotFoundException gone) { installed = false; }
            toast(installed ? "ما قدرنا نفتح حماية العائلة: حدّث الواجهة الرئيسية «تلفزيونك»" : "ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «تلفزيونك» أولاً");
        }
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
        return homeMayReadWatchNext() ? "يعمل · ما وقفته في نصه، تحت المفضلة" : "يعمل · يحتاج السماح مرة واحدة: اختيار";
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
            .setItems(new String[] {"تعديل التطبيقات", "رجوع للترتيب الأصلي"}, (d, i) -> openAppsOrder(i == 1))
            .show();
    }

    private void openAppsOrder(boolean reset) {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setClassName("sa.urtv.home", "sa.urtv.home.MainActivity")
                .putExtra(reset ? "reset_apps_order" : "edit_apps", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
        for (int i = 0; i < labels.length; i++) { values[i] = rows.get(i)[0]; about[i] = Prefs.about(Prefs.PANEL_ABOUT, values[i]); }
        pickWithPreview(home ? HOME_CARD_NAMES[place] : "بجانب الساعة", values, labels, about, sel,
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
        pickWithPreview("بطاقة ثانية", values, labels, about, sel,
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
    private void pickWithPreview(String title, final String[] values, final String[] labels, final String[] about, int selected,
                                 final Url urlFor, final Pick onPick) {
        final int previewW = 480, viewH = 300, pad = 22;   // dp: the page is laid out in a box like the home screen's, 480 wide
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setPadding(dp(12), dp(4), dp(12), dp(4));
        final android.widget.ListView lv = new android.widget.ListView(this);
        lv.setDividerHeight(0);
        lv.setSelector(GlassPaint.fill(this, look.focus, 12, 0));
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
                return r;
            }
        });
        box.addView(lv, new LinearLayout.LayoutParams(0, dp(330), 1f));
        // the stage: our photo, dimmed, with the card on it; as tall as the card needs (setStageHeight), round-cornered, no empty space
        final FrameLayout stage = new FrameLayout(this);
        stage.setBackgroundColor(0xFF07150E);
        Bitmap photo = photo(3);
        if (photo != null) {
            ImageView bgPhoto = new ImageView(this);
            bgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            bgPhoto.setImageBitmap(photo);
            bgPhoto.setColorFilter(new PorterDuffColorFilter(0x8C000000, PorterDuff.Mode.SRC_OVER));   // dim, as the screen's own veil does
            stage.addView(bgPhoto, new FrameLayout.LayoutParams(-1, -1));
        }
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
        lv.setOnItemClickListener((p, v, pos, id) -> { onPick.on(values[pos]); dialog.dismiss(); render(); });
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
        new Dlg(this).setTitle("الأسواق (حتى " + arabicNum(Prefs.MAX_MARKETS) + ")")
            .setMultiChoiceItems(labels, on, (d, i, checked) -> on[i] = checked)
            .setPositiveButton("حفظ", (d, w) -> {
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
            .setNegativeButton("إلغاء", null).show();
    }

    private String stocksLabel() { return Prefs.join(prefs.stocks(), "، "); }

    /** Codes (2222 = أرامكو) and US tickers (AAPL), separated by spaces or commas. */
    private void askStocks() {
        final EditText in = new EditText(this);
        in.setHint("مثال: \u20662222 1120 AAPL NVDA\u2069");
        in.setText(Prefs.join(prefs.stocks(), " "));
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        new Dlg(this).setTitle("قائمة الأسهم (رمز تداول من ٤ أرقام أو رمز أمريكي، حتى " + arabicNum(Prefs.MAX_STOCKS) + ")")
            .setView(in)
            .setPositiveButton("حفظ", (d, w) -> {
                List<String> v = new ArrayList<>();
                for (String s : in.getText().toString().split("[\\s,،]+")) {
                    s = s.trim().toUpperCase(java.util.Locale.ROOT);
                    if (s.isEmpty()) continue;
                    if (!s.matches(Prefs.STOCK_PATTERN)) { toast("رمز غير صالح: " + s); return; }
                    if (!v.contains(s)) v.add(s);
                }
                if (v.isEmpty() || v.size() > Prefs.MAX_STOCKS) {
                    toast("أدخل من رمز إلى " + arabicNum(Prefs.MAX_STOCKS) + " رموز");
                    return;
                }
                prefs.setStocks(v);
                render();
            })
            .setNegativeButton("إلغاء", null).show();
        in.requestFocus();
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
        pickWithPreview("شكل البطاقة", values, labels, about, sel, v -> prefs.previewUrl(this, card, v), v -> prefs.setCardStyle(v));
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
        String name = prefs.countdownName();
        return CountdownText.label(prefs.countdownDate(), name);
    }

    /** The date is set from the computer (optimize/saver.sh countdown); here it can be cleared. */
    private void manageCountdown() {
        if (prefs.countdownDate().isEmpty()) {
            new Dlg(this).setTitle("العدّ التنازلي")
                .setMessage("لم يُحدَّد تاريخ. يحدده المساعد على الكمبيوتر.")
                .setPositiveButton("حسنًا", null).show();
            return;
        }
        new Dlg(this).setTitle("العدّ التنازلي")
            .setMessage(countdownLabel())
            .setPositiveButton("مسح", (d, w) -> { prefs.setCountdown("", ""); render(); })
            .setNegativeButton("إلغاء", null).show();
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
        String[] labels = {PrayerMethod.label(PrayerMethod.AUTO, prefs.cityTz()), PrayerMethod.label(PrayerMethod.GULF, prefs.cityTz()), PrayerMethod.label(PrayerMethod.JAFARI, prefs.cityTz())};
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

    private void pickPhotoMinutes() {
        String[] labels = new String[Prefs.PHOTO_MINUTES.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = minutesLabel(Integer.parseInt(Prefs.PHOTO_MINUTES[i]));
            if (Prefs.PHOTO_MINUTES[i].equals(prefs.photoMinutes())) sel = i;
        }
        new Dlg(this).setTitle("تبديل الصورة كل")
            .setSingleChoiceItems(labels, sel, (d, i) -> { prefs.setPhotoMinutes(Prefs.PHOTO_MINUTES[i]); d.dismiss(); render(); })
            .show();
    }

    private String ownLabel() {
        if (Own.count(this) == 0) return "لا توجد صور · يضيفها مساعدك الذكي";
        return Own.ONLY.equals(Own.mode(this)) ? "صوري فقط" : "تُضاف إلى صور التطبيق";
    }

    /** «صوري»: photos only arrive through the owner's assistant; here the owner picks how they show, or removes them all. */
    private void pickOwn() {
        if (Own.count(this) == 0) {
            toast("اطلب من مساعدك الذكي إضافة صورك (حتى ١٠)");
            return;
        }
        String[] items = {"تُضاف إلى صور التطبيق", "صوري فقط", "حذف كل صوري"};
        new Dlg(this).setTitle("صوري")
            .setSingleChoiceItems(items, Own.ONLY.equals(Own.mode(this)) ? 1 : 0, (d, i) -> {
                d.dismiss();
                if (i == 2) { confirmClearOwn(); return; }
                prefs.setOwnMode(i == 1 ? Own.ONLY : Own.MIX);
                prefs.bumpOwnRev();
                render();
            })
            .show();
    }

    private void confirmClearOwn() {
        new Dlg(this).setTitle("حذف كل صوري؟")
            .setPositiveButton("حذف", (d, i) -> { Own.clear(this); render(); })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    /** The TV's own idle delay. Writing it needs WRITE_SETTINGS, which TVs rarely grant: then its own screen opens. */
    private void pickIdle() {
        if (!Prefs.canWriteSettings(this)) {
            toast("غيّرها من إعدادات التلفاز، أو بأمر من مساعدك");
            openDreamSettings();
            return;
        }
        String[] labels = new String[Prefs.IDLE_MINUTES.length];
        int sel = -1;
        long now = Prefs.idleMs(this);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = minutesLabel(Integer.parseInt(Prefs.IDLE_MINUTES[i]));
            if (Long.parseLong(Prefs.IDLE_MINUTES[i]) * 60000 == now) sel = i;
        }
        new Dlg(this).setTitle("بدء شاشة التوقف بعد")
            .setSingleChoiceItems(labels, sel, (d, i) -> {
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
                        Integer.parseInt(Prefs.IDLE_MINUTES[i]) * 60000);
                } catch (SecurityException e) {
                    toast("غيّرها من إعدادات التلفاز، أو بأمر من مساعدك");
                }
                d.dismiss();
                render();
            })
            .show();
    }

    private String idleLabel() {
        long ms = Prefs.idleMs(this);
        if (ms <= 0) return "يحدده التلفاز";
        if (ms % 60000 != 0) return arabicNum((int) (ms / 1000)) + " ثانية";
        return minutesLabel((int) (ms / 60000));
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

    private void openDreamSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
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
        return Arabic.translit(en);
    }

    private static String labelOf(String[][] options, String key) {
        for (String[] o : options) if (o[0].equalsIgnoreCase(key)) return o[1];
        return Arabic.translit(key);
    }

    private String teamsLabel() {
        List<String> names = new ArrayList<>();
        for (String t : prefs.teams()) names.add(t.split("\\|", 2)[1]);
        return names.isEmpty() ? "لا شيء" : Prefs.join(names, "، ");
    }

    /** the «الأرقام» setting as this screen draws it (read at every render): every number and label goes through conv() */
    private static volatile boolean latinDigits;
    private boolean noConv;   // the «الأرقام» row itself says «هندية ١٢٣» and «عربية 123»: both stay as they are

    private static String conv(String s) { return DigitsText.convert(s, latinDigits); }

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
    private void pickLook() {
        new Dlg(this).setTitle("المظهر")
            .setItems(new String[] {"الشكل: " + Prefs.lookLabel(prefs.look()), "الإضاءة: " + Prefs.appearanceLabel(prefs.appearance())},
                (d, i) -> pickLookPart(i == 0))
            .show();
    }

    /**
     * «الشكل» (shape) or «الإضاءة»: the list, and beside it a small page in the focused choice, drawn live from its palette over the one
     * backdrop (GlassSource.pageIn: no image of its own, nothing stored); OK keeps it and every screen of ours takes it.
     */
    private void pickLookPart(final boolean shape) {
        final String[] values = shape ? GlassStyle.LOOKS : Prefs.APPEARANCES;
        final String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = shape ? GlassStyle.LOOK_NAMES[i] : Prefs.appearanceLabel(values[i]).split(" · ")[0];
        final String current = shape ? prefs.look() : prefs.appearance();
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setPadding(dp(12), dp(4), dp(12), dp(4));
        final android.widget.ListView lv = new android.widget.ListView(this);
        lv.setDividerHeight(0);
        lv.setSelector(GlassPaint.fill(this, look.focus, 12, 0));
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
            look = GlassSource.page(this, root);   // this screen at once; the home and the others as they come up
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
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        col.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView title = new TextView(this);
        title.setText(half ? (l.light ? "فاتح" : "داكن") : "الإعدادات");
        title.setTextColor(l.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        col.addView(title);
        LinearLayout tiles = new LinearLayout(this);
        tiles.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for (int k = 0; k < (half ? 1 : 2); k++) {
            TextView t = new TextView(this);
            t.setText(k == 0 ? "الموقع" : "الأذان");
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            t.setTypeface(font("Thmanyah-Medium"));
            t.setGravity(Gravity.CENTER);
            t.setTextColor(k == 0 ? l.onFocus : l.text);
            t.setBackground(k == 0 ? GlassPaint.fill(this, l.focus, 10, 0) : GlassPaint.panel(this, l, 10));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            if (k == 0) tp.setMarginEnd(dp(8));
            tiles.addView(t, tp);
        }
        col.addView(tiles, llp(-1, -2, dp(8)));
        TextView row = new TextView(this);
        row.setText("المدينة · الرياض");
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
    private static final class Dlg extends AlertDialog.Builder {
        private final SettingsActivity owner;
        private boolean raw;   // its words keep their own digits (the «الأرقام» picker shows both kinds)

        Dlg(SettingsActivity a) { super(a, a.look.light ? R.style.UrDialogLight : R.style.UrDialog); owner = a; }   // the glass of now

        Dlg raw() { raw = true; return this; }

        @Override public AlertDialog create() {
            final AlertDialog d = super.create();
            d.setOnShowListener(x -> owner.dress(d, raw));
            return d;
        }
    }

    void dress(AlertDialog d) { dress(d, false); }

    void dress(AlertDialog d, boolean raw) {
        if (d.getWindow() == null) return;
        // the look of now on top of the dark or light theme: its dialog glass laid over its solid colour (opaque), its words, its focus
        d.getWindow().setBackgroundDrawable(GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line));
        if (d.getListView() != null) {
            android.graphics.drawable.StateListDrawable sel = new android.graphics.drawable.StateListDrawable();
            sel.addState(new int[] {android.R.attr.state_focused}, GlassPaint.fill(this, look.focus, 12, 0));
            sel.addState(new int[] {android.R.attr.state_pressed}, GlassPaint.fill(this, look.focus, 12, 0));
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
    private void header(String text) {
        groupCat = SettingsHub.ofHeader(text);
        pendingHeader = on(groupCat) ? text : null;
    }

    private void drawHeader(String text) {
        TextView t = new TextView(this);
        t.setText(conv(text));
        t.setTextColor(look.accent);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTypeface(font("Thmanyah-Medium"));
        t.setPadding(dp(10), dp(18), dp(10), dp(6));
        list.addView(t);
    }

    /** One plain line in the list, in no button (nothing to focus): the first-run note at the top, the help under «الموقع». */
    private void line(String text, int color, int bottom) {
        TextView t = new TextView(this);
        t.setText(conv(text));
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(font("Thmanyah-Regular"));
        t.setGravity(Gravity.START);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setPadding(dp(10), dp(8), dp(10), bottom);
        list.addView(t);
    }

    /** Plain lines at the end, in no button: nothing to focus, so the last row scrolls them into view. */
    private void note(View last, String... lines) {
        TextView t = new TextView(this);
        t.setText(conv(android.text.TextUtils.join("\n", lines)));
        t.setTextColor(look.sub);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(font("Thmanyah-Regular"));   // a version line has digits and Latin letters: Thmanyah has them
        t.setLineSpacing(dp(4), 1f);
        t.setGravity(Gravity.START);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setPadding(dp(6), dp(18), dp(6), dp(24));
        list.addView(t);
        final View.OnFocusChangeListener was = last.getOnFocusChangeListener();
        last.setOnFocusChangeListener((v, has) -> {
            if (was != null) was.onFocusChange(v, has);
            if (!has) return;
            for (android.view.ViewParent p = list.getParent(); p != null; p = p.getParent())   // list sits in a frame in the scroll view
                if (p instanceof ScrollView) { final ScrollView sv = (ScrollView) p; v.post(() -> sv.fullScroll(View.FOCUS_DOWN)); break; }
        });
    }

    /**
     * One prayer of «تعديل وقت الصلاة»: «الفجر +٢ د». Left adds a minute and right takes one off (the reading direction of an Arabic
     * screen runs leftwards), from −20 to +20; OK puts it back to zero. The text changes where it is, so the focus and the scroll stay.
     */
    private void adjRow(final int k) {
        final View card = row(PrayerAdj.label(k, prefs.prayerAdj()[k]), "يسار + · يمين − · موافق للتصفير", R.drawable.ic_eye, null);
        final TextView name = (TextView) ((LinearLayout) ((LinearLayout) card).getChildAt(1)).getChildAt(0);
        final Runnable show = new Runnable() { public void run() { name.setText(conv(PrayerAdj.label(k, prefs.prayerAdj()[k]))); } };
        card.setOnClickListener(v -> { int[] a = prefs.prayerAdj(); a[k] = 0; prefs.setPrayerAdj(a); show.run(); });
        card.setOnKeyListener((v, code, ev) -> {
            int d = code == android.view.KeyEvent.KEYCODE_DPAD_LEFT ? 1 : code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT ? -1 : 0;
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

    /** A card row: icon, label and its current value on a second line, chevron; gold when focused. */
    private View row(String label, String value, int icon, final Runnable onClick) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(24), dp(14), dp(20), dp(14));
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(FocusKey.key(label));   // which row it is when the list is built again (render keeps the focus on it)
        final String cat = SettingsHub.of(FocusKey.key(label), groupCat);   // made on every screen, added only on its own

        final android.widget.ImageView ic = new android.widget.ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(icon).mutate();
        d.setTint(look.accent);
        ic.setImageDrawable(d);
        ic.setTag(GlassPaint.ICON);
        card.addView(ic, new LinearLayout.LayoutParams(dp(28), dp(28)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        final TextView name = new TextView(this);
        name.setText(noConv ? label : conv(label));
        name.setTextColor(look.text);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        name.setTypeface(font("Thmanyah-Medium"));
        name.setSingleLine(true);
        texts.addView(name);
        final TextView val = new TextView(this);
        if (value != null) {
            val.setText(noConv ? value : conv(value));
            val.setTextColor(look.sub);
            val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            val.setTypeface(font("Thmanyah-Regular"));
            val.setSingleLine(true);
            val.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(val);
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(dp(18));
        card.addView(texts, tp);

        final TextView chevron = new TextView(this);
        chevron.setText("›");   // shown mirrored (‹) in the RTL row
        chevron.setTextColor(look.sub);
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        card.addView(chevron);

        if (onClick != null) card.setOnClickListener(v -> onClick.run());
        card.setOnFocusChangeListener((v, has) -> {
            // No scaling: a scaled rounded card overlapped its neighbours and blurred the text. The glass changes by itself (glassCard).
            if (has) topWhenFirst(v);
        });
        glassCard(card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(9);
        if (!on(cat)) return card;
        if (pendingHeader != null && cat.equals(groupCat)) { drawHeader(pendingHeader); pendingHeader = null; }
        list.addView(card, lp);
        return card;
    }

    interface Text { void got(String s); }
    interface Json { void got(String json) throws Exception; }

    private void ask(String hint, final Text then) {
        final EditText in = new EditText(this);
        in.setHint(hint);
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
                if (result == null) { toast("لا يوجد اتصال"); return; }
                try { then.got(result); } catch (Exception e) { toast("تعذرت قراءة النتائج"); }
            });
        }).start();
    }

    private void toast(String s) { Toast.makeText(this, conv(s), Toast.LENGTH_SHORT).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
