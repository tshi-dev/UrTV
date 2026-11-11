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
        {"Iraqi Premier League", "دوري نجوم العراق"},   // 4.3: TheSportsDB 5056 («Iraq Stars League»)
        {"AFC Champions League Elite", "دوري أبطال آسيا للنخبة"},   // 4.3: 4719; a cup: its clubs come from their own leagues
        {"UEFA Champions League", "دوري أبطال أوروبا"},   // 4.3: 4480
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

    // the time between background photos (PhotoInterval: minutes, or 30s on a TV with 2 GB; sent to the page as #photos=; 30 is what it always did)
    static final String[] PHOTO_MINUTES = PhotoInterval.ALL;

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
    // how the stocks card shows every stock's change (#stkchg=amount; the percentage is what it always showed).
    static final String[][] STK_CHG = {{"pct", "بالنسبة ٪"}, {"amount", "بالمبلغ"}};
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

    Prefs(Context c) {
        ctx = c;
        sp = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
        GlassStyle.setTweaks(tweaks());
        if (ZoneFix.logger == null) ZoneFix.logger = line -> android.util.Log.i("UrTVZone", line);
        ZoneFix.report(pageZone());   // issue #55: the page's last word on the city's zone, after a restart too
    }

    /** {name, latitude, longitude}; Riyadh until the user picks */
    String[] city() {
        return new String[] {sp.getString("cityName", "الرياض"), sp.getString("cityLat", "24.6877"), sp.getString("cityLon", "46.7219")};
    }
    void setCity(String name, String lat, String lon) { setCity(name, lat, lon, ""); }
    /** tz: the place's own time zone, "" for a built-in city or when only the TV's is known (see cityTz) */
    void setCity(String name, String lat, String lon, String tz) {
        setCity(name, lat, lon, tz, "");
    }
    void setCity(String name, String lat, String lon, String tz, String country) {
        String[] builtin = Cities.find(name, lat, lon);
        String code = country == null ? "" : country.trim().toUpperCase(java.util.Locale.ROOT);
        if (code.isEmpty()) code = builtin != null ? Cities.country(builtin) : Location.codeOf(tz);
        sp.edit().putString("cityName", name).putString("cityLat", lat).putString("cityLon", lon)
            .putString("cityTz", tz).putString("cityCountry", code).apply();
    }

    /** Also migrates pre-world built-in cities without guessing from a shared time zone. */
    String cityCountry() {
        String code = sp.getString("cityCountry", "");
        if (!code.isEmpty()) return code;
        String[] c = city(), builtin = Cities.find(c[0], c[1], c[2]);
        if (builtin == null) return Location.codeOf(cityTz());   // a place of one's own: from its zone, never saved (the TV's zone may change)
        code = Cities.country(builtin);
        if (!code.isEmpty()) sp.edit().putString("cityCountry", code).apply();   // a city saved before 4.1 has none: found once, kept (a user report on 4.1: lag)
        return code;
    }

    /** 4.3 (issue #55): the page's offset for the city's zone «zone|ms|minutes» (NowPlayingBridge.zoneOffset); "" until the page tells it */
    String pageZone() { return sp.getString("pageZone", ""); }
    void setPageZone(String v) { sp.edit().putString("pageZone", v == null ? "" : v).apply(); ZoneFix.report(v); }

    /** The time zone the page shows: Asia/Riyadh for a built-in Saudi city, the country's own for a Gulf one; a place of your own gets the one given
     *  with it, else the TV's own. */
    String cityTz() {
        String own = sp.getString("cityTz", "");
        if (!own.isEmpty()) return own;
        String[] c = city(), x = Cities.find(c[0], c[1], c[2]);   // the map, not a scan of every city (Cities.find)
        if (x != null) return x.length > 3 && x[3] != null ? x[3] : TZ;   // the Gulf and world cities carry their own
        return java.util.TimeZone.getDefault().getID();
    }

    /** «طريقة الحساب»: auto resolves the saved country to a numeric #pm; explicit legacy choices retain their existing values. */
    String prayerMethod() { String v = sp.getString("prayerMethod", "auto"); return PrayerMethod.GULF.equals(v) ? PrayerMethod.GULF : PrayerMethod.AUTO; }   // 4.3 A4: a saved «jafari» is «تلقائي»
    void setPrayerMethod(String v) { sp.edit().putString("prayerMethod", v).apply(); }

    static final int MAX_TEAMS = 20;
    /**
     * Whole leagues followed (#leagues=): the page downloads one small file for each built-in one, every 30 minutes; a league of the
     * catalogue (CatalogueLeagues) is asked of TheSportsDB itself, 2 requests every 30 minutes: 8 in all keeps the free key's rate.
     */
    static final int MAX_LEAGUES = 8;

    /** The league of LEAGUES that name (English as TheSportsDB spells it, the Arabic label, or its slug) means, else null. */
    static String[] leagueByName(String name) {
        String n = name.trim().toLowerCase().replaceAll("[^a-z0-9\\u0600-\\u06ff]+", "");
        for (String[] l : LEAGUES)
            if (n.equals(l[0].toLowerCase().replaceAll("[^a-z0-9]+", "")) || n.equals(l[1].toLowerCase().replaceAll("[^a-z0-9\\u0600-\\u06ff]+", "")))
                return l;
        return null;
    }

    /** Followed leagues: a built-in one by its English name, a catalogue one as "id4719|ar|en" (CatalogueLeagues); default none. */
    List<String> leagues() {
        List<String> v = new ArrayList<>();
        for (String s : list("leagues")) {
            if (v.size() >= MAX_LEAGUES) break;
            if (CatalogueLeagues.parse(s) != null) {
                boolean dup = false;
                for (String x : v) dup |= CatalogueLeagues.same(x, s);
                if (!dup) v.add(s);
                continue;
            }
            String[] l = leagueByName(s);
            if (l != null && !v.contains(l[0])) v.add(l[0]);
        }
        return v;
    }

    /** What #leagues= and #lgn= carry (CatalogueLeagues.pageToken, names). */
    String leaguesFragment() {
        List<String> all = leagues(), tokens = new ArrayList<>();
        for (String s : all) tokens.add(CatalogueLeagues.pageToken(s));
        String names = CatalogueLeagues.names(all, Tr.EN.equals(lang()));
        return "&leagues=" + Uri.encode(join(tokens, ",")) + (names.isEmpty() ? "" : "&lgn=" + Uri.encode(names));
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

    /** «أزل … من فرقي» on a game's card: false when the team was not followed */
    boolean removeTeam(String id) {
        List<String> v = teams();
        boolean had = v.removeIf(t -> t.startsWith(id + "|"));
        if (had) setList("teams", v);
        return had;
    }

    // The card beside the clock: value sent to the page (#panel=), Arabic label
    static final String[][] PANELS = {
        {"shows", "جديد المسلسلات"}, {"prayers", "مواقيت الصلاة اليوم"}, {"forecast", "طقس الأيام القادمة"}, {"hourly", "طقس الساعات القادمة"}, {"prayerrow", "الصلوات في صف"}, {"nextprayer", "الصلاة القادمة"},
        {"matches", "مباريات فرقك القادمة"}, {"verse", "آية أو حديث اليوم"}, {"wisdom", "حكمة اليوم"},
        {"adhkar", "أذكار الصباح والمساء"}, {"stocks", "الأسهم"}, {"tasks", "المهام"}, {"year", "السنة"},
        {"smart", "البطاقة الذكية"}, {"today", "يومك"}, {"album", "ألبوم صوري"},
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
        {"album", "صورك واحدة بعد الأخرى بتلاشٍ ناعم، وتاريخ الصورة تحتها"},
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

    /**
     * «اللغة» (docs/ENGLISH.md): ar (the default, as always) or en. Sent to the page and the home as &lang=en, only for English (Tr.fragment),
     * so with Arabic the fragment and every screen are exactly what they were. S reads it for the words, Dir for the direction.
     */
    String lang() { return Tr.norm(sp.getString("lang", Tr.AR)); }
    void setLang(String v) { sp.edit().putString("lang", Tr.norm(v)).apply(); S.set(v); }

    /** «الأرقام»: hindi (١٢٣, the default) or latin; with «اللغة» English and the digits never chosen, latin (123) */
    String digits() { return sp.getString("digits", Tr.EN.equals(lang()) ? "latin" : "hindi"); }
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

    String stkChg() { return "amount".equals(sp.getString("stkChg", "pct")) ? "amount" : "pct"; }
    void setStkChg(String v) { sp.edit().putString("stkChg", v).apply(); }
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

    /** the value in use on this TV: "30s" saved on a TV under 2 GB (or moved from one) is 1 minute here (PhotoInterval.read) */
    String photoMinutes() { return PhotoInterval.read(sp.getString("photoMinutes", PhotoInterval.DEFAULT), ramMb(ctx)); }

    private static long ramMb = -1;
    /** The TV's memory in MB (MemTotal, what the setup reports' «free of 2048» counts); read once, it does not change. 0 when unknown. */
    static long ramMb(Context c) {
        if (ramMb < 0) {
            long mb = 0;
            try {
                android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
                android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                mb = mi.totalMem / (1024 * 1024);
            } catch (RuntimeException ignored) { }
            ramMb = mb;
        }
        return ramMb;
    }
    /** the tiers, the same as the home's Ram: LOW does every cut, MID the cheap memory ones, NORMAL none */
    static final int RAM_LOW = 0, RAM_MID = 1, RAM_NORMAL = 2;
    /** the same numbers as the home's Ram.LOW_MB and MID_MB (tests/ram_tier_test.py keeps them equal) */
    static final long LOW_MB = 2000, MID_MB = 2600;

    /** The rule, with no Android in it: the low-RAM flag or a known total under LOW_MB is LOW, under MID_MB MID; 0 = unknown: NORMAL. */
    static int ramTier(boolean lowRamFlag, long mb) {
        if (lowRamFlag || (mb > 0 && mb < LOW_MB)) return RAM_LOW;
        return mb > 0 && mb < MID_MB ? RAM_MID : RAM_NORMAL;
    }

    static int ramNamed(String n) { return "low".equals(n) ? RAM_LOW : "mid".equals(n) ? RAM_MID : "normal".equals(n) ? RAM_NORMAL : -1; }

    /** This TV's tier, read once (a forced tier from `saver.sh ramtier` wins, from the next start of the app). The one place that decides. */
    static int ramTier(Context c) {
        if (ramTier < 0) {
            boolean flag = false;
            try { flag = ((android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).isLowRamDevice(); } catch (RuntimeException ignored) { }
            int t = ramTier(flag, ramMb(c));
            try {
                int f = ramNamed(c.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("ramtier", ""));
                if (f >= 0) t = f;
            } catch (RuntimeException ignored) { }
            ramTier = t;
        }
        return ramTier;
    }
    private static int ramTier = -1;

    /** LOW: every cut */
    static boolean lowRam(Context c) { return ramTier(c) == RAM_LOW; }
    /** MID: only the cheap memory cuts (photos at 1280, own photos 25, the markets a little slower) */
    static boolean midRam(Context c) { return ramTier(c) == RAM_MID; }
    /** LOW or MID */
    static boolean tightRam(Context c) { return ramTier(c) != RAM_NORMAL; }
    /** the debug switch: low | mid | normal | auto (auto = the real tier) */
    void setRamTier(String v) { sp.edit().putString("ramtier", "auto".equals(v) ? "" : v).apply(); }
    String ramTierSetting() { String v = sp.getString("ramtier", ""); return v.isEmpty() ? "auto" : v; }
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
    static String monthsLabel(String v) { return "levant".equals(v) ? "كانون الثاني، شباط، آذار" : "يناير، فبراير، مارس"; }

    /**
     * Three parts of the screens that can be hidden, each shown by default as it always was, and sent only when hidden so the fragment is
     * otherwise as it was: «المناسبة القادمة» at the bottom middle of the screensaver (the countdown to Ramadan and the feasts, the iftar and
     * suhoor in Ramadan; #occasion=0), «تعتيم الليل» (the screensaver darkens from 21:00, most from 23:00 to 05:00; #nightdim=0) and the
     * «المهام» button on the home screen (#taskspill=0: the home reads it, the page ignores it).
     */
    boolean occasion() { return sp.getBoolean("occasion", true); }
    void setOccasion(boolean v) { sp.edit().putBoolean("occasion", v).apply(); }
    boolean nightDim() { return sp.getBoolean("nightDim", true); }
    void setNightDim(boolean v) { sp.edit().putBoolean("nightDim", v).apply(); }
    boolean tasksPill() { return sp.getBoolean("tasksPill", true); }
    void setTasksPill(boolean v) { sp.edit().putBoolean("tasksPill", v).apply(); }

    /**
     * «الرجوع يفتح شاشة التوقف» (off by default): Back on our home starts the screensaver. The home keeps its own switch (Dock.back_saver,
     * the one `dock.sh backsaver` sets) and moves it when this one changes (Dock.followBackSaver); #backsaver=1 only while on.
     */
    boolean backSaver() { return sp.getBoolean("backSaver", false); }
    void setBackSaver(boolean v) { sp.edit().putBoolean("backSaver", v).apply(); }

    /** «تبديل الآية والحديث»: the verse-or-hadith card once a day (default, as always) or every 3 minutes (QuoteEvery); #versemin=3 only then. */
    String quoteEvery() { return QuoteEvery.read(sp.getString("quoteEvery", QuoteEvery.DAY)); }
    void setQuoteEvery(String v) { sp.edit().putString("quoteEvery", QuoteEvery.read(v)).apply(); }

    /** «الآية والحديث»: the verse-or-hadith card with both (default, as always), verses only or hadith only (VerseKind); #versekind= only then. */
    String verseKind() { return VerseKind.read(sp.getString("verseKind", VerseKind.BOTH)); }
    void setVerseKind(String v) { sp.edit().putString("verseKind", VerseKind.read(v)).apply(); }

    /** «إظهار الثواني» (off by default): the seconds on the screensaver's clock; #seconds=1 only while on, so the page is otherwise as it was. */
    boolean seconds() { return sp.getBoolean("seconds", false); }
    void setSeconds(boolean v) { sp.edit().putBoolean("seconds", v).apply(); }

    /** «الخروج من تلفزيونك»: one Back (default, as always), a long Back or three Backs leave our home opened from its tile (HomeExit). */
    String homeExit() { return HomeExit.read(sp.getString("homeExit", HomeExit.PRESS)); }
    void setHomeExit(String v) { sp.edit().putString("homeExit", HomeExit.read(v)).apply(); }

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
        String url = dashboardUrl(c) + CleanMask.fragment(CleanMask.state(c));   // «UrTV النظيف»: the page shows the photo and the clock only
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
        String own = Updater.pageBase(c, lang(), autoUpdate());   // «اللغة» English: tv-en.html (Updater.pageBase); null for Arabic
        if (own != null) return own;
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
        return "city=" + Uri.encode(pageCity(city)) + "&lat=" + city[1] + "&lon=" + city[2]
            + "&tz=" + Uri.encode(cityTz())
            + "&teams=" + join(teamIds(), ",")
            + "&events=" + join(list("events"), ",")
            + leaguesFragment()
            + "&panel=" + panel()
            + SecondCard.fragment(panel2(), SAVER_PANELS)
            + "&week=" + ("none".equals(panel()) ? "0" : "1")   // for a page older than #panel
            + "&genres=" + Uri.encode(join(list("genres"), ","))
            + "&langs=" + Uri.encode(join(list("langs"), ","))
            + "&shows=" + join(ids, ",")
            + "&minrating=" + minRating()
            + "&photos=" + PhotoInterval.fragmentValue(photoMinutes())
            + "&digits=" + digits()
            + ClockSetting.fragment(clock24())
            + Tr.fragment(lang())
            + QuoteEvery.fragment(quoteEvery())
            + VerseKind.fragment(verseKind())
            + (seconds() ? "&seconds=1" : "")
            + (lowRam(ctx) ? "&lowram=1" : midRam(ctx) ? "&midram=1" : "")   // a weak TV (a user report on 4.1, lag): the page's slower timers, no seconds, no transitions
            + HomeExit.fragment(homeExit())
            + (occasion() ? "" : "&occasion=0") + (nightDim() ? "" : "&nightdim=0") + (tasksPill() ? "" : "&taskspill=0")
            + (backSaver() ? "&backsaver=1" : "")
            + "&cardstyle=" + cardStyle()
            + "&adhanbar=" + (adhanBar() ? "1" : "0")
            + "&yearstrip=" + (yearStrip() ? "1" : "0")
            + (sunrise() ? "&sunrise=1" : "") + (prayerBar() ? "&prayerbar=1" : "") + (wxMore() ? "&wxmore=1" : "")
            + ("levant".equals(months()) ? "&months=levant" : "")
            + PrayerAdj.fragment(prayerAdjOn(), prayerAdj())
            + PrayerMethod.fragment(prayerMethod(), cityCountry(), cityTz())
            + HijriOffset.fragment(hijriOffset())
            + "&national=" + national()
            + "&markets=" + join(markets(), ",")
            + "&mkstyle=" + mkStyle()
            + "&mkbar=" + (marketsOn() ? "on" : "off")
            + "&clocksize=" + clockSize()
            + "&stocks=" + join(stocks(), ",")
            + ("amount".equals(stkChg()) ? "&stkchg=amount" : "")   // only when chosen: the link is what it was otherwise
            + "&slot=" + slot()
            + "&cd=" + countdownDate()
            + "&cdname=" + Uri.encode(countdownName())
            + openTasksParam() + remindersParam()
            + (wallDim() > 0 ? "&walldim=" + wallDim() : "")
            + ownParam()
            + packsParam();
    }

    /** The city's name for the page and the home: as saved, or with «اللغة» English the built-in city's English name (Places; a place of one's own keeps its name) */
    private String pageCity(String[] city) {
        if (!Tr.EN.equals(lang())) return city[0];
        String en = Places.city(city[0], city[1], city[2]);
        return en != null ? en : city[0];
    }

    /** The owner's own photos: how many and mix/only; nothing at all while there are none, so the page is as before. «ألبوم صوري» adds its two choices only when they are not the defaults. */
    private String ownParam() {
        int n = Own.count(ctx);
        return n == 0 ? "" : "&own=" + n + "&ownmode=" + Own.mode(ctx) + albumParam();
    }

    /** «ألبوم صوري» (card album): a photo every 4 s with a crossfade (the default) or one a minute; the photo's date under it (on by default). */
    boolean albumMinute() { return sp.getBoolean("albumMinute", false); }
    void setAlbumMinute(boolean v) { sp.edit().putBoolean("albumMinute", v).apply(); }
    boolean albumDate() { return sp.getBoolean("albumDate", true); }
    void setAlbumDate(boolean v) { sp.edit().putBoolean("albumDate", v).apply(); }
    private String albumParam() { return (albumMinute() ? "&albummode=minute" : "") + (albumDate() ? "" : "&albumdate=0"); }

    /** «صور شاشة التوقف»: the packs that are on (ids), and whether our own set («صورنا», on by default) is. Sent as #packs=id:count,... and #ours=0 only when set, so the fragment is as before. */
    java.util.Set<String> packsOn() { return PhotoPacks.parseOn(sp.getString("packsOn", "")); }
    void setPacksOn(java.util.Set<String> v) { sp.edit().putString("packsOn", PhotoPacks.joinOn(v)).apply(); }
    boolean oursOn() { return sp.getBoolean("oursOn", true); }
    void setOursOn(boolean v) { sp.edit().putBoolean("oursOn", v).apply(); }

    /** 4.3 «الصور في الشاشة الرئيسية»: the home's wallpaper uses the same packs as the screensaver («مثل شاشة التوقف», the default) or a set of its own */
    boolean homePhotosSame() { return sp.getBoolean("homeSame", true); }
    void setHomePhotosSame(boolean v) { sp.edit().putBoolean("homeSame", v).apply(); }
    java.util.Set<String> homePacksOn() { return PhotoPacks.parseOn(sp.getString("homePacksOn", "")); }
    void setHomePacksOn(java.util.Set<String> v) { sp.edit().putString("homePacksOn", PhotoPacks.joinOn(v)).apply(); }
    boolean homeOursOn() { return sp.getBoolean("homeOursOn", true); }
    void setHomeOursOn(boolean v) { sp.edit().putBoolean("homeOursOn", v).apply(); }

    /** the packs the home rotates (id -> photos), and whether «صورنا» (the bundled photos) is in its rotation */
    java.util.Map<String, Integer> homePacks() { return PhotoPacks.homeSet(homePhotosSame(), packsOn(), homePacksOn(), PackStore.installed(ctx)); }
    boolean homeOurs() {
        java.util.Map<String, Integer> have = PackStore.installed(ctx);
        boolean on = homePhotosSame() ? oursOn() : homeOursOn();
        return on || homePacks().isEmpty();
    }
    /** changes whenever what the home rotates does, so the home chooses a photo again at once */
    String homePacksRev() { return homePacks() + "|" + homeOurs() + "|" + homePhotosSame(); }

    private String packsParam() {
        java.util.Map<String, Integer> have = PackStore.installed(ctx);
        // «صورنا» off counts only while a pack that is on is on the TV: a pack deleted meanwhile brings ours back (nothing is left blank)
        boolean ours = oursOn() || !PhotoPacks.mayTurnOursOff(packsOn(), have);
        return PhotoPacks.fragment(packsOn(), have) + (ours ? "" : "&ours=0");
    }

    /** the home's own choice for «صوري»: "mix" (with ours), "only", "off"; null (never chosen) follows the screensaver's own mode, as before */
    String homeMine() { return sp.getString("homeMine", null); }
    void setHomeMine(String v) { if (v == null) sp.edit().remove("homeMine").apply(); else sp.edit().putString("homeMine", v).apply(); bumpOwnRev(); }   // the home chooses its photo again at once
    /** what the home uses now: its own choice, else the screensaver's mode */
    String homeMineEffective() { String v = homeMine(); return v != null ? v : ownMode(); }

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
            long now = System.currentTimeMillis();
            boolean ids = TaskList.needsIds(v);
            if (ids) v = TaskList.withIds(v, sp.getLong("taskSeq", 0));
            boolean roll = Reminders.needsRoll(v, now);   // a repeating reminder nobody answered moves to its next time
            if (roll) v = Reminders.roll(v, now, Reminders.zone(cityTz()));   // whole days in the TV's city (summer time keeps the hour)
            if (ids || roll) setTasks(v);
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
                    out.add(new Task(o.getString("t"), o.optBoolean("done"), o.optLong("at"), o.optLong("id"), Math.max(0, o.optLong("due")),
                        Reminders.beforeOf(o.optInt("bf")), Math.max(0, Math.min(2, o.optInt("rp"))), o.optBoolean("snd"), Math.max(0, Math.min(2, o.optInt("wh"))),
                        Math.max(0, o.optLong("sn"))));
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
            for (Task t : v) {
                JSONObject o = new JSONObject().put("t", t.text).put("done", t.done).put("at", t.at).put("id", t.id);
                if (t.due > 0) o.put("due", t.due).put("bf", t.before).put("rp", t.repeat).put("snd", t.sound).put("wh", t.where);   // a task with no reminder is saved as it always was
                if (t.snooze > 0) o.put("sn", t.snooze);
                a.put(o);
            }
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

    /** null when added; otherwise why not. A reminder is a task with its due time (epoch ms), the minutes before it, repeat 0/1/2, a chime and where it shows. */
    String addReminder(String raw, long due, int before, int repeat, boolean sound, int where) {
        String text = cleanTask(raw);
        if (text.isEmpty()) return "the task is empty";
        synchronized (TASKS_LOCK) {
            List<Task> v = tasks();
            List<Task> out = TaskList.addReminder(v, text, System.currentTimeMillis(), TaskList.lastId(v, sp.getLong("taskSeq", 0)) + 1, MAX_TASKS,
                due, Reminders.beforeOf(before), Math.max(0, Math.min(2, repeat)), sound, Math.max(0, Math.min(2, where)));
            if (out == null) return "at most " + MAX_TASKS + " tasks; finish or delete one first";
            setTasks(out);
            return null;
        }
    }

    /** this task's reminder is set (due 0 takes it off); false when it is gone */
    boolean remindTaskId(long id, long due, int before, int repeat, boolean sound, int where) {
        synchronized (TASKS_LOCK) {
            List<Task> out = TaskList.remind(tasks(), id, Math.max(0, due), Reminders.beforeOf(before), Math.max(0, Math.min(2, repeat)), sound, Math.max(0, Math.min(2, where)));
            if (out == null) return false;
            setTasks(out);
            return true;
        }
    }

    /** the card for this task comes again in `minutes`; false when it is gone */
    boolean snoozeTaskId(long id, int minutes) {
        synchronized (TASKS_LOCK) {
            List<Task> out = TaskList.snooze(tasks(), id, System.currentTimeMillis() + Math.max(1, minutes) * Reminders.MINUTE);
            if (out == null) return false;
            setTasks(out);
            return true;
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
            List<Task> out = TaskList.markDone(tasks(), id, System.currentTimeMillis(), Reminders.zone(cityTz()));   // a repeating reminder is done for this time only
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

    /**
     * "&rem=" and the open reminders for the page's card, one per line, URL-encoded: id|card time|appointment|repeat|chime|where|text (epoch
     * milliseconds). The page shows the card by its own clock, so a reminder set while a screen is up reaches it on the next load (the screen
     * starts over when the settings change); nothing when there are none.
     */
    private String remindersParam() {
        List<String> lines = new ArrayList<>();
        for (Task t : tasks())
            if (Reminders.has(t) && t.where != 2) lines.add(t.id + "|" + Reminders.fireAt(t) + "|" + t.due + "|" + t.repeat + "|" + (t.sound ? 1 : 0) + "|" + t.where + "|" + t.text.replace('|', ' '));
        return lines.isEmpty() ? "" : "&rem=" + Uri.encode(join(lines, "\n"));
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
    // 4.3 «اللون والتحكم»: the accent (one of GlassStyle.ACCENTS, "" = the look's own), the tray's and cards' fill strength 30..100, the wallpaper's extra dimming 0..90, the focus style; each default is the look as it was
    String accent() { String v = sp.getString("accent", ""); for (String[] a : GlassStyle.ACCENTS) if (a[0].equals(v)) return v; return ""; }
    int barPercent() { return Math.max(30, Math.min(100, sp.getInt("barPercent", 100))); }
    int wallDim() { return Math.max(0, Math.min(90, sp.getInt("wallDim", 0))); }
    String focusStyle() { String v = sp.getString("focusStyle", "solid"); return java.util.Arrays.asList(GlassStyle.FOCUS_STYLES).contains(v) ? v : "solid"; }
    void setAccent(String v) { sp.edit().putString("accent", v).apply(); GlassStyle.setTweaks(tweaks()); }
    void setBarPercent(int v) { sp.edit().putInt("barPercent", Math.max(30, Math.min(100, v))).apply(); GlassStyle.setTweaks(tweaks()); }
    void setWallDim(int v) { sp.edit().putInt("wallDim", Math.max(0, Math.min(90, v))).apply(); GlassStyle.setTweaks(tweaks()); }
    void setFocusStyle(String v) { if (java.util.Arrays.asList(GlassStyle.FOCUS_STYLES).contains(v)) { sp.edit().putString("focusStyle", v).apply(); GlassStyle.setTweaks(tweaks()); } }
    // 4.3 «توحيد المظهر» (GlassStyle.material): on (the default), the home's upper buttons, cards and apps bar are one material. Off, each has its own strength (30..100; until it
    // is set, «شفافية الشريط») and fill (GlassStyle.FILLS; until it is set, what the surface wore before the switch)
    boolean lookSync() { return sp.getBoolean("lookSync", true); }
    void setLookSync(boolean v) { sp.edit().putBoolean("lookSync", v).apply(); GlassStyle.setTweaks(tweaks()); }
    int surfacePercent(int surface) { return Math.max(30, Math.min(100, sp.getInt("surfacePercent" + surface, barPercent()))); }
    String surfaceFill(int surface) { String v = sp.getString("surfaceFill" + surface, GlassStyle.FILL_BEFORE[surface]); return java.util.Arrays.asList(GlassStyle.FILLS).contains(v) ? v : GlassStyle.FILL_BEFORE[surface]; }
    void setSurfacePercent(int surface, int v) { sp.edit().putInt("surfacePercent" + surface, Math.max(30, Math.min(100, v))).apply(); GlassStyle.setTweaks(tweaks()); }
    void setSurfaceFill(int surface, String v) { if (java.util.Arrays.asList(GlassStyle.FILLS).contains(v)) { sp.edit().putString("surfaceFill" + surface, v).apply(); GlassStyle.setTweaks(tweaks()); } }
    /** the one string both apps read (GlassStyle.setTweaks): empty while nothing was touched; the surfaces' own values are in it only while the switch is off */
    String tweaks() {
        String a = accent(), f = focusStyle();
        int b = barPercent(), d = wallDim();
        boolean sync = lookSync();
        if (a.isEmpty() && b == 100 && d == 0 && "solid".equals(f) && sync) return "";
        StringBuilder s = new StringBuilder("accent=" + a + ";bar=" + b + ";dim=" + d + ";focus=" + f);
        if (!sync) {
            s.append(";sync=off");
            for (int i = 0; i < GlassStyle.SURFACES.length; i++) s.append(';').append(GlassStyle.SURFACES[i]).append('=').append(surfacePercent(i)).append(':').append(surfaceFill(i));
        }
        return s.toString();
    }
    static String lookLabel(String v) { return GlassStyle.LOOK_NAMES[java.util.Arrays.asList(GlassStyle.LOOKS).indexOf(GlassStyle.look(v))]; }

    /** The greeting and the weather line on the launcher's home screen (default on). Off: the plain good-morning it had. */
    boolean greeting() { return sp.getBoolean("greeting", true); }
    void setGreeting(boolean v) { sp.edit().putBoolean("greeting", v).apply(); }

    /**
     * «زر تلاوة وأذان» on the launcher's home screen (default shown). Hidden removes only the button: the adhan, its alarm and bar, the
     * any-key stop, the «يُتلى الآن» card and a Quran that was started elsewhere go on; the screen opens from the app menu (long OK on an
     * app) or from the «يُتلى الآن» card instead.
     */
    /** the home's quick buttons the person hid (#52): any of markets, matches, center, phone (the others have their own switch: «زر المهام», «زر تلاوة وأذان»); "" = all shown */
    static final String[] QUICK_BUTTONS = {"markets", "matches", "center", "phone"};
    String pillsHidden() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String k : sp.getString("pillsHidden", "").split(",")) if (java.util.Arrays.asList(QUICK_BUTTONS).contains(k) && !out.contains(k)) out.add(k);
        return android.text.TextUtils.join(",", out);
    }
    void setPillsHidden(String csv) { sp.edit().putString("pillsHidden", csv == null || csv.equals("none") ? "" : csv).apply(); }
    boolean pillHidden(String key) { return java.util.Arrays.asList(pillsHidden().split(",")).contains(key); }
    void setPillHidden(String key, boolean hidden) {
        java.util.List<String> l = new java.util.ArrayList<>(java.util.Arrays.asList(pillsHidden().split(",")));
        l.remove("");
        l.remove(key);
        if (hidden) l.add(key);
        setPillsHidden(android.text.TextUtils.join(",", l));
    }
    boolean listenPill() { return sp.getBoolean("listenPill", true); }
    void setListenPill(boolean v) { sp.edit().putBoolean("listenPill", v).apply(); }

    /** «جرس التحديثات» on the home screen: on by default (it shows only on evidence of waiting updates, so it cannot hurt) */
    boolean updateBell() { return sp.getBoolean("updateBell", true); }
    /** «أكمل المشاهدة» under the home screen's favourites (default on; it shows only once the home app may read the TV's Watch Next list). */
    /** «المظهر ← الواجهة»: the home's layout, classic (the default, the home as it always was) or football (docs/PROPOSALS-NOV2.md §1) */
    static final String[] HOME_LAYOUTS = {"classic", "football"};
    static final String[] HOME_LAYOUT_NAMES = {"العادية", "كرة القدم"};
    String homeLayout() { String v = sp.getString("homeLayout", HOME_LAYOUTS[0]); return java.util.Arrays.asList(HOME_LAYOUTS).contains(v) ? v : HOME_LAYOUTS[0]; }
    void setHomeLayout(String v) { if (java.util.Arrays.asList(HOME_LAYOUTS).contains(v)) sp.edit().putString("homeLayout", v).apply(); }
    static String homeLayoutLabel(String v) { int i = java.util.Arrays.asList(HOME_LAYOUTS).indexOf(v); return HOME_LAYOUT_NAMES[Math.max(0, i)]; }
    /** «تطبيقات في الشريط»: how many favourites the home's tray shows whole at once, 5 (the default, as always) to 10, or "all" (up to 10, never a scroll) */
    static final String[] FAV_BARS = {"5", "6", "7", "8", "all"};
    /** 9 and 10 were offered once; ten tiles across were too small to read (step 28), so they mean 8 (eight in view, the rest scroll) */
    static String favBarFix(String v) { return "9".equals(v) || "10".equals(v) ? "8" : v; }
    /** 4.3 «تطبيق المباريات»: "auto" (UrTV Player when installed, the default), "none", or an app's package; «شاهد المباراة» on a game hands it over */
    static final String MATCH_AUTO = "auto", MATCH_NONE = "none";
    static boolean matchAppOk(String v) { return MATCH_AUTO.equals(v) || MATCH_NONE.equals(v) || (v != null && v.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+") && v.length() <= 120); }
    String matchApp() { String v = sp.getString("matchApp", MATCH_AUTO); return matchAppOk(v) ? v : MATCH_AUTO; }
    void setMatchApp(String v) { if (matchAppOk(v)) sp.edit().putString("matchApp", v).apply(); }

    String favBar() { String v = favBarFix(sp.getString("favBar", "5")); return java.util.Arrays.asList(FAV_BARS).contains(v) ? v : "5"; }
    void setFavBar(String v) { v = favBarFix(v); if (java.util.Arrays.asList(FAV_BARS).contains(v)) sp.edit().putString("favBar", v).apply(); }
    /** the choice as the settings row says it */
    static String favBarLabel(String v) { return "all".equals(v) ? "كل المفضلة" : v; }
    /** «صفحات الأسهم والمباريات داكنة»: the home's markets and matches pages in the dark look whatever the home's look (on, the default), or in the home's look as before (off) */
    boolean pagesDark() { return sp.getBoolean("pagesDark", true); }
    void setPagesDark(boolean v) { sp.edit().putBoolean("pagesDark", v).apply(); }
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
