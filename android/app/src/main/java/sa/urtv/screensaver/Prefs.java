package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Everything the user picks in settings, and the dashboard URL built from it. */
final class Prefs {
    static final String TZ = "Asia/Riyadh";

    // TheSportsDB id, Arabic name; more teams come from the in-app search
    static final String[][] TEAM_PRESETS = {
        {"136013", "الهلال"}, {"136137", "المنتخب السعودي"},
    };

    // League name exactly as TheSportsDB spells it (checked from CI by .github/tools/team_ids.py), Arabic label.
    // Their clubs come from assets/shared/teams.json (Teams), which CI builds from these leagues' matches
    // (.github/tools/team_ids.py reads this list); a new league here needs its sport and minimum team count there.
    static final String[][] LEAGUES = {
        {"Saudi-Arabian Pro League", "دوري روشن السعودي"},
        {"Saudi First Division League", "دوري يلو (الدرجة الأولى)"},
        {"English Premier League", "الدوري الإنجليزي"},
        {"Spanish La Liga", "الدوري الإسباني"},
        {"Italian Serie A", "الدوري الإيطالي"},
        {"German Bundesliga", "الدوري الألماني"},
        {"French Ligue 1", "الدوري الفرنسي"},
        {"Portuguese Primeira Liga", "الدوري البرتغالي"},
        {"Dutch Eredivisie", "الدوري الهولندي"},
        {"Turkish Super Lig", "الدوري التركي"},
        {"Egyptian Premier League", "الدوري المصري"},
        {"UAE Pro League", "الدوري الإماراتي"},
        {"Qatar Stars League", "الدوري القطري"},
        {"American Major League Soccer", "الدوري الأمريكي"},
        {"NBA", "دوري كرة السلة الأمريكي (NBA)"},
        {"NFL", "دوري كرة القدم الأمريكية (NFL)"},
    };

    // Other sports shown in the matches slot and card beside the followed teams' games: only their next event
    // (#events= key, Arabic label). The page finds each one's league ids in assets/shared/events.json (CI).
    static final String[][] EVENTS = {
        {"f1", "فورمولا 1"}, {"ufc", "UFC"}, {"boxing", "الملاكمة"},
    };

    // TVMaze genre (sent to the page), Arabic label
    static final String[][] GENRES = {
        {"Drama", "دراما"}, {"Comedy", "كوميديا"}, {"Action", "أكشن"}, {"Crime", "جريمة"},
        {"Thriller", "إثارة"}, {"Mystery", "غموض"}, {"Science-Fiction", "خيال علمي"},
        {"Fantasy", "فانتازيا"}, {"Adventure", "مغامرة"}, {"Romance", "رومانسي"},
        {"Family", "عائلي"}, {"History", "تاريخي"}, {"Horror", "رعب"},
        {"Anime", "أنمي"}, {"Documentary", "وثائقي"}, {"Sports", "رياضة"},
    };

    // TVMaze language, Arabic label
    static final String[][] LANGS = {
        {"English", "الإنجليزية"}, {"Arabic", "العربية"}, {"Korean", "الكورية"},
        {"Japanese", "اليابانية"}, {"Turkish", "التركية"}, {"Spanish", "الإسبانية"},
        {"French", "الفرنسية"}, {"Hindi", "الهندية"}, {"German", "الألمانية"},
    };

    static final String[] MIN_RATINGS = {"0", "6", "6.5", "7", "7.5", "8"};

    // minutes between background photos (sent to the page as #photos=; 30 is what it always did)
    static final String[] PHOTO_MINUTES = {"5", "10", "15", "30", "60"};

    // digits on the screen (sent to the page as #digits=; hindi is what it always showed)
    static final String[][] DIGITS = {{"hindi", "هندية ١٢٣"}, {"latin", "عربية 123"}};

    // the national days (sent to the page as #national=; auto shows them on their days, off never)
    static final String[][] NATIONAL = {{"auto", "تلقائي"}, {"off", "إيقاف"}};

    // look of the card beside the clock (sent to the page as #cardstyle=; current is the plain card it always had)
    static final String[][] CARD_STYLES = {{"current", "الحالية"}, {"dark1", "داكنة"}, {"glass1", "زجاجية"},
        {"dark3", "داكنة بإطار"}, {"glass3", "زجاجية بإطار"}};

    // markets at the top left (sent to the page as #markets=, up to MAX_MARKETS, in this order of choice; tasi,spx,btc is what it always showed)
    static final String[][] MARKETS = {
        {"tasi", "تاسي"}, {"aramco", "أرامكو"}, {"spx", "S&P 500"}, {"btc", "بيتكوين"},
        {"gold", "الذهب (الجرام بالريال)"}, {"silver", "الفضة (الجرام بالريال)"}, {"oil", "نفط برنت (للبرميل)"}, {"xrp", "ريبل"},
    };
    static final int MAX_MARKETS = 4;
    static final String DEFAULT_MARKETS = "tasi,spx,btc";

    // how the markets are shown (#mkstyle=; arrows is what it always showed)
    static final String[][] MK_STYLES = {{"arrows", "الاتجاه فقط ▲▼"}, {"prices", "السعر والنسبة"}};
    // the size of the screensaver's clock (sent as #clocksize=; large is what it always was)
    static final String[][] CLOCK_SIZES = {{"large", "كبيرة"}, {"medium", "متوسطة"}, {"small", "صغيرة"}};
    // what the bottom-left slot shows (sent to the page as #slot=; matches is what it always showed)
    static final String[][] SLOTS = {{"matches", "المباريات"}, {"weather", "الطقس"}, {"fasting", "الصيام"},
        {"countdown", "عدّ تنازلي"}, {"markets", "الأسواق"}, {"none", "بدون"}};

    // minutes of idle before Android starts the screensaver (the TV's own screen_off_timeout; not stored here)
    static final String[] IDLE_MINUTES = {"1", "2", "5", "10", "15", "30"};

    /** May this app change system settings (API 23+, and only if the owner allowed it for the app)? */
    static boolean canWriteSettings(Context c) {
        return Build.VERSION.SDK_INT >= 23 && Settings.System.canWrite(c);
    }

    /** The TV's idle delay in ms; readable without any permission. -1 when the TV does not say. */
    static long idleMs(Context c) {
        return Settings.System.getLong(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, -1);
    }

    private final SharedPreferences sp;
    private final Context ctx;

    /** sent (to this app only) when a SET from ADB or the launcher saved something: a screensaver that is already showing starts over with it */
    static final String CHANGED = "sa.urtv.screensaver.SETTINGS_CHANGED";

    Prefs(Context c) { ctx = c; sp = c.getSharedPreferences("settings", Context.MODE_PRIVATE); }

    /** {name, latitude, longitude}; Riyadh until the user picks */
    String[] city() {
        return new String[] {sp.getString("cityName", "الرياض"), sp.getString("cityLat", "24.6877"), sp.getString("cityLon", "46.7219")};
    }
    void setCity(String name, String lat, String lon) { setCity(name, lat, lon, ""); }
    /** tz: the place's own time zone, "" for a built-in city or when only the TV's is known (see cityTz) */
    void setCity(String name, String lat, String lon, String tz) {
        sp.edit().putString("cityName", name).putString("cityLat", lat).putString("cityLon", lon).putString("cityTz", tz).apply();
    }

    /** The time zone the page shows: Asia/Riyadh for a built-in Saudi city, the country's own for a Gulf one; a place of your own gets the one given
     *  with it, else the TV's own. */
    String cityTz() {
        String own = sp.getString("cityTz", "");
        if (!own.isEmpty()) return own;
        String[] c = city();
        for (Cities.Region r : Cities.REGIONS)
            for (String[] x : r.cities)
                if (x[0].equals(c[0]) && x[1].equals(c[1]) && x[2].equals(c[2])) return x.length > 3 ? x[3] : TZ;   // the Gulf cities carry their own
        return java.util.TimeZone.getDefault().getID();
    }

    /** «طريقة الحساب» (default auto = the country's own): PrayerMethod.AUTO, GULF or JAFARI; sent as #pm=gulf / #pm=jafari only for those. */
    String prayerMethod() { String v = sp.getString("prayerMethod", "auto"); return PrayerMethod.JAFARI.equals(v) ? PrayerMethod.JAFARI : PrayerMethod.GULF.equals(v) ? PrayerMethod.GULF : PrayerMethod.AUTO; }
    void setPrayerMethod(String v) { sp.edit().putString("prayerMethod", v).apply(); }

    static final int MAX_TEAMS = 20;
    /** Whole leagues followed (#leagues=): the page downloads one small file for each, every 30 minutes. */
    static final int MAX_LEAGUES = 5;

    /** The league of LEAGUES that name (English as TheSportsDB spells it, the Arabic label, or its slug) means, else null. */
    static String[] leagueByName(String name) {
        String n = name.trim().toLowerCase().replaceAll("[^a-z0-9\\u0600-\\u06ff]+", "");
        for (String[] l : LEAGUES)
            if (n.equals(l[0].toLowerCase().replaceAll("[^a-z0-9]+", "")) || n.equals(l[1].toLowerCase().replaceAll("[^a-z0-9\\u0600-\\u06ff]+", "")))
                return l;
        return null;
    }

    /** Followed leagues, by their English names (default none). */
    List<String> leagues() {
        List<String> v = new ArrayList<>();
        for (String s : list("leagues")) {
            String[] l = leagueByName(s);
            if (l != null && !v.contains(l[0]) && v.size() < MAX_LEAGUES) v.add(l[0]);
        }
        return v;
    }

    /** Followed teams in priority order, "id|name". Migrates the old two team fields once. */
    List<String> teams() {
        if (!sp.contains("teams")) {
            List<String> v = new ArrayList<>();
            for (int slot = 1; slot <= 2; slot++) {
                String[] def = TEAM_PRESETS[slot - 1];
                String id = sp.getString("team" + slot + "Id", def[0]);
                if (!id.isEmpty()) v.add(id + "|" + sp.getString("team" + slot + "Name", def[1]));
            }
            return v;
        }
        return list("teams");
    }

    /** false when already followed or the list is full */
    boolean addTeam(String id, String name) {
        List<String> v = teams();
        for (String t : v) if (t.startsWith(id + "|")) return false;
        if (v.size() >= MAX_TEAMS) return false;
        v.add(id + "|" + name.replace("|", " ").replace(";", " "));
        setList("teams", v);
        return true;
    }

    // The card beside the clock: value sent to the page (#panel=), Arabic label
    static final String[][] PANELS = {
        {"shows", "جديد المسلسلات"}, {"prayers", "مواقيت الصلاة اليوم"}, {"forecast", "طقس الأيام القادمة"}, {"hourly", "طقس الساعات القادمة"}, {"prayerrow", "الصلوات في صف"}, {"nextprayer", "الصلاة القادمة"},
        {"matches", "مباريات فرقك القادمة"}, {"verse", "آية أو حديث اليوم"}, {"wisdom", "حكمة اليوم"},
        {"adhkar", "أذكار الصباح والمساء"}, {"stocks", "الأسهم"}, {"tasks", "المهام"}, {"year", "السنة"},
        {"smart", "البطاقة الذكية"}, {"today", "يومك"},
        {"none", "لا شيء"},
    };

    /** Cards for the home screen only: on the screensaver the always-on panel at the bottom right already says the same. */
    static boolean homeOnly(String v) { return "nextprayer".equals(v); }

    /** Older builds had an on/off switch for the card; off becomes "none", otherwise the verse card. A home-only card is the verse there. */
    String panel() {
        String v = sp.getString("panel", sp.getBoolean("showWeek", true) ? "verse" : "none");
        return homeOnly(v) ? "verse" : v;
    }
    void setPanel(String v) { sp.edit().putString("panel", v).apply(); }

    /** The screensaver's choices for a card beside the clock: PANELS without the home-only ones («لا شيء» included). */
    static final String[][] SAVER_PANELS = saverPanels();
    private static String[][] saverPanels() {
        List<String[]> out = new ArrayList<>();
        for (String[] p : PANELS) if (!homeOnly(p[0])) out.add(p);
        return out.toArray(new String[0][]);
    }

    /** «بطاقة ثانية» beside the small clock (SecondCard): none by default, never the first card again nor the next prayer. */
    String panel2() { return SecondCard.read(sp.getString("panel2", "none"), panel(), SAVER_PANELS); }
    void setPanel2(String v) { sp.edit().putString("panel2", v).apply(); }

    // One plain line about each card, in the words of docs/FEATURES.md (tests/check.py compares them): the pickers show it under the name
    static final String[][] PANEL_ABOUT = {
        {"shows", "حلقات هذا الأسبوع"},
        {"prayers", "الست أوقات، والجاية مميّزة"},
        {"forecast", "خمسة أيام"},
        {"hourly", "الست ساعات الجاية بالساعة والأيقونة والحرارة، وقمر بعد الغروب"},
        {"prayerrow", "الخمس صلوات في صف واحد، والجاية مضيئة"},
        {"nextprayer", "سطر واحد: الصلاة الجاية ووقتها وكم باقي لها"},
        {"matches", "مباريات الأسبوع ونتائج آخر يوم لفرقك، وكل مباراة بسطر واحد"},
        {"verse", "آية يوم، وحديث صحيح من الكتب الستة اليوم اللي بعده، ومعه مصدره"},
        {"wisdom", "مثل أو قول عربي معروف مع مصدره"},
        {"adhkar", "من الفجر للظهر أذكار الصباح، ومن العصر للمغرب أذكار المساء"},
        {"stocks", "أسهمك بالسعر والتغيّر"},
        {"tasks", "قائمة مهامك"},
        {"year", "٣٦٥ نقطة، الأيام اللي راحت تولّع من الغامق للذهبي"},
        {"smart", "تختار لك البطاقة المناسبة للوقت"},
        {"today", "الصلاة القادمة ووقتها، والمباراة القادمة لفرقك، وأول مهمة مفتوحة، والمناسبة القادمة وكم يوم باقي"},
        {"none", "بدون بطاقة"},
    };
    static final String[][] CARD_STYLE_ABOUT = {
        {"current", "البطاقة كما كانت، بخلفية داكنة شفافة"},
        {"dark1", "داكنة بزوايا مدوّرة وخط ذهبي على جهة الساعة"},
        {"glass1", "زجاجية ضبابية فوق الصورة"},
        {"dark3", "داكنة بإطار ذهبي داخلي ومعيّن على الحافتين"},
        {"glass3", "زجاجية بإطار ذهبي داخلي ومعيّن على الحافتين"},
    };

    static String about(String[][] table, String v) {
        for (String[] r : table) if (r[0].equals(v)) return r[1];
        return "";
    }

    static String panelLabel(String v) {
        for (String[] p : PANELS) if (p[0].equals(v)) return p[1];
        return PANELS[0][1];
    }

    /** The label of a card for a place that may be empty: «none» reads as emptyLabel there. */
    static String panelLabel(String v, String emptyLabel) {
        return "none".equals(v) ? emptyLabel : panelLabel(v);
    }

    List<String> list(String key) {
        List<String> out = new ArrayList<>();
        for (String s : sp.getString(key, "").split(";")) if (!s.isEmpty()) out.add(s);
        return out;
    }
    void setList(String key, List<String> v) { sp.edit().putString(key, join(v, ";")).apply(); }

    /** shows picked one by one, stored as "id|name" */
    List<String> shows() { return list("shows"); }

    String minRating() { return sp.getString("minRating", "7"); }
    void setMinRating(String v) { sp.edit().putString("minRating", v).apply(); }

    /** «فرق التاريخ الهجري» (default 0): the Hijri day moved by -2..+2 days, for moon-sighting countries (HijriOffset); sent to the page only when not 0 */
    int hijriOffset() { return HijriOffset.decode(sp.getInt("hijriOffset", 0)); }
    void setHijriOffset(int v) { sp.edit().putInt("hijriOffset", HijriOffset.decode(v)).apply(); }

    /** «ساعة ٢٤» (default off: the 12-hour clock as always); sent to the page as #clock=24, and only while on. */
    boolean clock24() { return sp.getBoolean("clock24", false); }
    void setClock24(boolean v) { sp.edit().putBoolean("clock24", v).apply(); }

    String digits() { return sp.getString("digits", "hindi"); }
    void setDigits(String v) { sp.edit().putString("digits", v).apply(); }

    static String digitsLabel(String v) {
        for (String[] d : DIGITS) if (d[0].equals(v)) return d[1];
        return DIGITS[0][1];
    }

    // the stocks card's watchlist (sent as #stocks=): 4-digit Tadawul codes and US tickers, up to MAX_STOCKS
    static final int MAX_STOCKS = 8;
    static final String DEFAULT_STOCKS = "2222,1120,7010,2010";
    static final String STOCK_PATTERN = "\\d{4}|[A-Za-z][A-Za-z.\\-]{0,5}";

    List<String> stocks() {
        List<String> v = list("stocks");
        return v.isEmpty() ? java.util.Arrays.asList(DEFAULT_STOCKS.split(",")) : v;
    }
    void setStocks(List<String> v) { setList("stocks", v); }

    /** Chosen markets in order, the keys of MARKETS. */
    List<String> markets() {
        List<String> v = list("markets");
        return v.isEmpty() ? java.util.Arrays.asList(DEFAULT_MARKETS.split(",")) : v;
    }
    void setMarkets(List<String> v) { setList("markets", v); }

    /** The markets line at the top (default on); off hides it and the page asks for nothing for it. The list above is kept. */
    boolean marketsOn() { return sp.getBoolean("marketsOn", true); }
    void setMarketsOn(boolean v) { sp.edit().putBoolean("marketsOn", v).apply(); }

    String clockSize() {
        String v = sp.getString("clockSize", "large");
        for (String[] c : CLOCK_SIZES) if (c[0].equals(v)) return v;
        return "large";
    }
    void setClockSize(String v) { sp.edit().putString("clockSize", v).apply(); }

    static String clockSizeLabel(String v) {
        for (String[] c : CLOCK_SIZES) if (c[0].equals(v)) return c[1];
        return CLOCK_SIZES[0][1];
    }

    String mkStyle() { return sp.getString("mkStyle", "arrows"); }
    void setMkStyle(String v) { sp.edit().putString("mkStyle", v).apply(); }

    static String mkStyleLabel(String v) {
        for (String[] m : MK_STYLES) if (m[0].equals(v)) return m[1];
        return MK_STYLES[0][1];
    }

    String national() { return "off".equals(sp.getString("national", "auto")) ? "off" : "auto"; }
    void setNational(String v) { sp.edit().putString("national", v).apply(); }

    static String nationalLabel(String v) {
        for (String[] n : NATIONAL) if (n[0].equals(v)) return n[1];
        return NATIONAL[0][1];
    }

    String cardStyle() { return sp.getString("cardStyle", "current"); }
    void setCardStyle(String v) { sp.edit().putString("cardStyle", v).apply(); }

    static String cardStyleLabel(String v) {
        for (String[] c : CARD_STYLES) if (c[0].equals(v)) return c[1];
        return CARD_STYLES[0][1];
    }

    String slot() { return sp.getString("slot", "matches"); }
    void setSlot(String v) { sp.edit().putString("slot", v).apply(); }

    static String slotLabel(String v) {
        for (String[] s : SLOTS) if (s[0].equals(v)) return s[1];
        return SLOTS[0][1];
    }

    /** the countdown's date (YYYY-MM-DD, "" = none) and its name; sent to the page as #cd= and #cdname= */
    String countdownDate() { return sp.getString("cd", ""); }
    String countdownName() { return sp.getString("cdName", ""); }
    void setCountdown(String date, String name) { sp.edit().putString("cd", date).putString("cdName", name).apply(); }

    String photoMinutes() { return sp.getString("photoMinutes", "30"); }
    void setPhotoMinutes(String v) { sp.edit().putString("photoMinutes", v).apply(); }

    /**
     * The release «تحديث تلفزيوني» (the home screen's screen and the phone page) and the newest-version line read: "latest" (default), or "next",
     * the test build. For agents only (`saver.sh updatechannel`); no screen shows it and the settings export leaves it out.
     */
    String updateChannel() { return "next".equals(sp.getString("updateChannel", "latest")) ? "next" : "latest"; }
    void setUpdateChannel(String v) { sp.edit().putString("updateChannel", "next".equals(v) ? "next" : "latest").apply(); }

    /**
     * The newest version Updater last read, but only one that came from the release of the CURRENT channel (an older app stored none: that was the
     * latest's); a value read before the channel changed is not shown as the newest of this one.
     */
    String latestVersion() {
        String v = sp.getString("latestVersion", null);
        return v != null && updateChannel().equals(sp.getString("latestChannel", "latest")) ? v : null;
    }

    /** The slim bar before the adhan, on the screensaver and on the home screen (default on); sent to the page as #adhanbar=. */
    boolean adhanBar() { return sp.getBoolean("adhanBar", true); }
    void setAdhanBar(boolean v) { sp.edit().putBoolean("adhanBar", v).apply(); }

    /** From PR #28, each off by default and sent to the page only when on: the sunrise after Fajr in the bottom-right panel (#sunrise=1),
     *  the bar of the time between two prayers under it (#prayerbar=1), and the humidity and wind at the top left (#wxmore=1). */
    boolean sunrise() { return sp.getBoolean("sunrise", false); }
    void setSunrise(boolean v) { sp.edit().putBoolean("sunrise", v).apply(); }
    boolean prayerBar() { return sp.getBoolean("prayerBar", false); }
    void setPrayerBar(boolean v) { sp.edit().putBoolean("prayerBar", v).apply(); }
    boolean wxMore() { return sp.getBoolean("wxMore", false); }
    void setWxMore(boolean v) { sp.edit().putBoolean("wxMore", v).apply(); }

    /** The Gregorian month's names: "default" («أكتوبر») or "levant" («تشرين الأول»), on the screensaver and the home screen; #months=levant. */
    String months() { return "levant".equals(sp.getString("months", "default")) ? "levant" : "default"; }
    void setMonths(String v) { sp.edit().putString("months", "levant".equals(v) ? "levant" : "default").apply(); }
    static String monthsLabel(String v) { return "levant".equals(v) ? "كانون الثاني، شباط، آذار…" : "يناير، فبراير، مارس…"; }

    /** The slim year strip under the clock, on the screensaver only (default off); sent to the page as #yearstrip=. */
    boolean yearStrip() { return sp.getBoolean("yearStrip", false); }
    void setYearStrip(boolean v) { sp.edit().putBoolean("yearStrip", v).apply(); }

    /** «تعديل وقت الصلاة» (default off): the five prayers moved by their own minutes, −20..20 (PrayerAdj); sent to the page and the home screen as #adj=. */
    boolean prayerAdjOn() { return sp.getBoolean("prayerAdjOn", false); }
    void setPrayerAdjOn(boolean v) { sp.edit().putBoolean("prayerAdjOn", v).apply(); }
    int[] prayerAdj() { return PrayerAdj.decode(sp.getString("prayerAdj", "")); }
    void setPrayerAdj(int[] v) { sp.edit().putString("prayerAdj", PrayerAdj.encode(v)).apply(); }

    boolean autoUpdate() { return sp.getBoolean("autoUpdate", true); }
    void setAutoUpdate(boolean v) { sp.edit().putBoolean("autoUpdate", v).apply(); }

    SharedPreferences raw() { return sp; }

    /** The page reads its settings from the fragment; a ?query fails on file:// in WebView. */
    String dashboardUrl(Context c) { return pageBase(c) + "#" + fragment(); }

    /** Was a city ever set, from the settings or the computer? Until then it is the install's Riyadh (Location.note). */
    boolean cityChosen() { return sp.contains("cityName"); }

    /** The first-run note is due now (Location.noteDue): the settings' top row and the screensaver's start ask the same. */
    boolean cityNoteDue() { return Location.noteDue(FreshInstall.is(ctx), cityChosen(), sp.getInt("cityNoteShown", 0)); }

    /**
     * The screensaver's page, with #citynote=1 while the note is due. start: a real start of the screensaver (ScreenDream attached),
     * the only thing that counts toward its three; the settings' preview shows it without counting. Not in fragment(): the launcher's
     * cards never show it.
     */
    String screenUrl(Context c, boolean start) {
        String url = dashboardUrl(c);
        if (!cityNoteDue()) return url;
        if (start) sp.edit().putInt("cityNoteShown", sp.getInt("cityNoteShown", 0) + 1).apply();
        return url + "&citynote=1";
    }

    /**
     * The page for a preview in the settings: the card alone (#widget=1) with the current settings, but this card in this style.
     * panel= and cardstyle= are taken out of the fragment first: the page reads the first one it finds.
     */
    String previewUrl(Context c, String panel, String cardStyle) {
        String frag = fragment().replaceAll("(^|&)(panel|panel2|week|cardstyle|home_panel)=[^&]*", "");
        return pageBase(c) + "#" + frag + "&panel=" + panel + "&cardstyle=" + cardStyle + "&widget=1";
    }

    /**
     * The page without its settings: the downloaded copy when its build stamp is newer than the one the app carries, else the app's own. By
     * the stamp in each page (PageStamp), not by file time: a download from a stale CDN copy gets a newer file time than the APK's install
     * and used to win for hours.
     */
    private String pageBase(Context c) {
        File remote = Updater.pageFile(c);
        boolean useRemote = false;
        if (autoUpdate() && remote.isFile()) {
            try { useRemote = PageStamp.newer(Updater.fileStamp(remote), Updater.assetStamp(c)); }
            catch (Exception ignored) { }
        }
        return useRemote ? Uri.fromFile(remote).toString() : "file:///android_asset/tv.html";
    }

    /** Everything the page needs, as "city=..&lat=..&..."; also read by the launcher (ConfigProvider). */
    String fragment() {
        String[] city = city();
        List<String> ids = new ArrayList<>();
        for (String s : shows()) ids.add(s.split("\\|", 2)[0]);
        return "city=" + Uri.encode(city[0]) + "&lat=" + city[1] + "&lon=" + city[2]
            + "&tz=" + Uri.encode(cityTz())
            + "&teams=" + join(teamIds(), ",")
            + "&events=" + join(list("events"), ",")
            + "&leagues=" + Uri.encode(join(leagues(), ","))
            + "&panel=" + panel()
            + SecondCard.fragment(panel2(), SAVER_PANELS)
            + "&week=" + ("none".equals(panel()) ? "0" : "1")   // for a page older than #panel
            + "&genres=" + Uri.encode(join(list("genres"), ","))
            + "&langs=" + Uri.encode(join(list("langs"), ","))
            + "&shows=" + join(ids, ",")
            + "&minrating=" + minRating()
            + "&photos=" + photoMinutes()
            + "&digits=" + digits()
            + ClockSetting.fragment(clock24())
            + "&cardstyle=" + cardStyle()
            + "&adhanbar=" + (adhanBar() ? "1" : "0")
            + "&yearstrip=" + (yearStrip() ? "1" : "0")
            + (sunrise() ? "&sunrise=1" : "") + (prayerBar() ? "&prayerbar=1" : "") + (wxMore() ? "&wxmore=1" : "")
            + ("levant".equals(months()) ? "&months=levant" : "")
            + PrayerAdj.fragment(prayerAdjOn(), prayerAdj())
            + PrayerMethod.fragment(prayerMethod())
            + HijriOffset.fragment(hijriOffset())
            + "&national=" + national()
            + "&markets=" + join(markets(), ",")
            + "&mkstyle=" + mkStyle()
            + "&mkbar=" + (marketsOn() ? "on" : "off")
            + "&clocksize=" + clockSize()
            + "&stocks=" + join(stocks(), ",")
            + "&slot=" + slot()
            + "&cd=" + countdownDate()
            + "&cdname=" + Uri.encode(countdownName())
            + openTasksParam()
            + ownParam();
    }

    /** The owner's own photos: how many and mix/only; nothing at all while there are none, so the page is as before. */
    private String ownParam() {
        int n = Own.count(ctx);
        return n == 0 ? "" : "&own=" + n + "&ownmode=" + Own.mode(ctx);
    }

    String ownMode() { return sp.getString("ownMode", Own.MIX); }
    void setOwnMode(String v) { sp.edit().putString("ownMode", v).apply(); }

    /** counts every change to the photos, so the launcher can tell it should show them */
    long ownRev() { return sp.getLong("ownRev", 0); }
    void bumpOwnRev() { sp.edit().putLong("ownRev", ownRev() + 1).apply(); }

    static final int MAX_TASKS = 12;
    static final int TASK_CHARS = 60;

    /** Changes to the list are read-modify-write of one JSON string: one at a time, from the provider, the receiver and the screens alike. */
    private static final Object TASKS_LOCK = new Object();

    /**
     * All tasks, oldest first (a JSON array of {t, done, at, id}); the number a person sees is the position + 1, but a change names the
     * task by its id. A list saved before ids existed is given them here, once, with everything else as it was.
     */
    List<Task> tasks() {
        synchronized (TASKS_LOCK) {
            List<Task> v = parseTasks(sp.getString("tasks", "[]"));
            if (TaskList.needsIds(v)) {
                v = TaskList.withIds(v, sp.getLong("taskSeq", 0));
                setTasks(v);
            }
            return v;
        }
    }

    /** The tasks of the saved JSON; an entry that is not a task is skipped, not the whole list lost. */
    static List<Task> parseTasks(String json) {
        List<Task> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(json);
            for (int k = 0; k < a.length(); k++) {
                try {
                    JSONObject o = a.getJSONObject(k);
                    out.add(new Task(o.getString("t"), o.optBoolean("done"), o.optLong("at"), o.optLong("id")));
                } catch (JSONException bad) {
                    // one entry that is not a task is skipped; the others are kept (the list is saved back whole)
                }
            }
        } catch (JSONException e) {
            out.clear();
        }
        return out;
    }

    private void setTasks(List<Task> v) {
        JSONArray a = new JSONArray();
        try {
            for (Task t : v) a.put(new JSONObject().put("t", t.text).put("done", t.done).put("at", t.at).put("id", t.id));
        } catch (JSONException ignored) { }
        sp.edit().putString("tasks", a.toString()).putLong("taskSeq", TaskList.lastId(v, sp.getLong("taskSeq", 0))).apply();
    }

    /** Trimmed, one line, at most TASK_CHARS characters; "" when nothing is left. */
    static String cleanTask(String raw) {
        String s = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        if (s.length() > TASK_CHARS) s = s.substring(0, TASK_CHARS).trim();
        return s;
    }

    /** null when added; otherwise why not (full of open tasks, or empty text). Done tasks make room, oldest first. */
    String addTask(String raw) {
        String text = cleanTask(raw);
        if (text.isEmpty()) return "the task is empty";
        synchronized (TASKS_LOCK) {
            List<Task> v = tasks();
            List<Task> out = TaskList.add(v, text, System.currentTimeMillis(), TaskList.lastId(v, sp.getLong("taskSeq", 0)) + 1, MAX_TASKS);
            if (out == null) return "at most " + MAX_TASKS + " tasks; finish or delete one first";
            setTasks(out);
            return null;
        }
    }

    /** n counts from 1 (what `list` shows); false when there is no such task */
    boolean doneTask(int n) {
        synchronized (TASKS_LOCK) {
            List<Task> v = tasks();
            return n >= 1 && n <= v.size() && doneTaskId(v.get(n - 1).id);
        }
    }

    boolean deleteTask(int n) {
        synchronized (TASKS_LOCK) {
            List<Task> v = tasks();
            return n >= 1 && n <= v.size() && deleteTaskId(v.get(n - 1).id);
        }
    }

    /** the task with this id is marked done; false when it is gone */
    boolean doneTaskId(long id) {
        synchronized (TASKS_LOCK) {
            List<Task> out = TaskList.markDone(tasks(), id);
            if (out == null) return false;
            setTasks(out);
            return true;
        }
    }

    boolean deleteTaskId(long id) {
        synchronized (TASKS_LOCK) {
            List<Task> out = TaskList.remove(tasks(), id);
            if (out == null) return false;
            setTasks(out);
            return true;
        }
    }

    void clearTasks() {
        synchronized (TASKS_LOCK) { sp.edit().remove("tasks").apply(); }
    }

    /** "&tasks=" and the open tasks, one per line, URL-encoded; nothing when there are none (the page shows «لا مهام اليوم»). */
    private String openTasksParam() {
        List<String> open = new ArrayList<>();
        for (Task t : tasks()) if (!t.done) open.add(t.text);
        return open.isEmpty() ? "" : "&tasks=" + Uri.encode(join(open, "\n"));
    }

    /** The cards on the launcher's home screen: up to HOME_CARDS of PANELS, none until the user picks one. */
    static final int HOME_CARDS = 3;

    /**
     * "prayers,hourly" (a single "prayers" is what older builds stored) as the cards it names, in order: each once, "none" and
     * anything unknown left out, at most HOME_CARDS. An empty list is no card.
     */
    static List<String> parseHomePanels(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw.split(",")) {
            s = s.trim();
            boolean known = false;
            for (String[] p : PANELS) if (p[0].equals(s) && !"none".equals(s)) known = true;
            if (known && !out.contains(s) && out.size() < HOME_CARDS) out.add(s);
        }
        return out;
    }

    List<String> homePanels() { return parseHomePanels(sp.getString("homePanel", "none")); }
    void setHomePanels(List<String> v) {
        List<String> ok = parseHomePanels(join(v, ","));
        sp.edit().putString("homePanel", ok.isEmpty() ? "none" : join(ok, ",")).apply();
    }
    /** What the page and saver.sh see: "none", or the cards joined by commas. */
    String homePanel() { List<String> v = homePanels(); return v.isEmpty() ? "none" : join(v, ","); }
    /** The card in place i (0 to 2) of the home screen, or "none". */
    String homePanelAt(int i) { List<String> v = homePanels(); return i < v.size() ? v.get(i) : "none"; }
    /** Place i gets that card ("none" empties it and the ones after it move up); a card already in another place moves here. */
    void setHomePanelAt(int i, String card) {
        List<String> now = homePanels();
        String[] slot = new String[HOME_CARDS];
        for (int k = 0; k < slot.length && k < now.size(); k++) slot[k] = now.get(k);
        for (int k = 0; k < slot.length; k++) if (k != i && card.equals(slot[k])) slot[k] = null;
        slot[i] = "none".equals(card) ? null : card;
        List<String> v = new ArrayList<>();
        for (String c : slot) if (c != null) v.add(c);
        setHomePanels(v);
    }

    /** «المظهر» of the launcher's home screen: dark (the default, as it always was), light, or auto (light from sunrise until Maghrib). */
    static final String[] APPEARANCES = {"dark", "light", "auto"};
    String appearance() { String v = sp.getString("appearance", "dark"); return java.util.Arrays.asList(APPEARANCES).contains(v) ? v : "dark"; }
    void setAppearance(String v) { if (java.util.Arrays.asList(APPEARANCES).contains(v)) sp.edit().putString("appearance", v).apply(); }
    static String appearanceLabel(String v) {
        return "light".equals(v) ? "فاتح" : "auto".equals(v) ? "تلقائي · فاتح من الشروق إلى المغرب، وداكن بعده" : "داكن";
    }

    /** «المظهر ← الشكل» of all our screens: glass (the default), graphite, navy or clean (GlassStyle.LOOKS); anything else is glass. */
    String look() { return GlassStyle.look(sp.getString("look", GlassStyle.LOOKS[0])); }
    void setLook(String v) { if (java.util.Arrays.asList(GlassStyle.LOOKS).contains(v)) sp.edit().putString("look", v).apply(); }
    static String lookLabel(String v) { return GlassStyle.LOOK_NAMES[java.util.Arrays.asList(GlassStyle.LOOKS).indexOf(GlassStyle.look(v))]; }

    /** The greeting and the weather line on the launcher's home screen (default on). Off: the plain good-morning it had. */
    boolean greeting() { return sp.getBoolean("greeting", true); }
    void setGreeting(boolean v) { sp.edit().putBoolean("greeting", v).apply(); }

    /**
     * «زر تلاوة وأذان» on the launcher's home screen (default shown). Hidden removes only the button: the adhan, its alarm and bar, the
     * any-key stop, the «يُتلى الآن» card and a Quran that was started elsewhere go on; the screen opens from the app menu (long OK on an
     * app) or from the «يُتلى الآن» card instead.
     */
    boolean listenPill() { return sp.getBoolean("listenPill", true); }
    void setListenPill(boolean v) { sp.edit().putBoolean("listenPill", v).apply(); }

    /** «جرس التحديثات» on the home screen: on by default (it shows only on evidence of waiting updates, so it cannot hurt) */
    boolean updateBell() { return sp.getBoolean("updateBell", true); }
    /** «أكمل المشاهدة» under the home screen's favourites (default on; it shows only once the home app may read the TV's Watch Next list). */
    boolean watchNext() { return sp.getBoolean("watchNext", true); }
    void setWatchNext(boolean v) { sp.edit().putBoolean("watchNext", v).apply(); }
    void setUpdateBell(boolean v) { sp.edit().putBoolean("updateBell", v).apply(); }

    List<String> teamIds() {
        List<String> ids = new ArrayList<>();
        for (String t : teams()) ids.add(t.split("\\|", 2)[0]);
        return ids;
    }

    static String join(List<String> v, String sep) {
        StringBuilder b = new StringBuilder();
        for (String s : v) { if (b.length() > 0) b.append(sep); b.append(s); }
        return b.toString();
    }
}
