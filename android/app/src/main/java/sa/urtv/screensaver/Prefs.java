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
        {"dark3", "داكنة بإطار"}, {"glass3", "زجاجية بإطار"}, {"boxes", "مربعات"}, {"classic", "الشكل السابق"}};

    // markets at the top left (sent to the page as #markets=, up to MAX_MARKETS, in this order of choice; tasi,spx,btc is what it always showed)
    static final String[][] MARKETS = {
        {"tasi", "تاسي"}, {"aramco", "أرامكو"}, {"spx", "S&P 500"}, {"btc", "بيتكوين"},
        {"gold", "الذهب (الجرام بالريال)"}, {"gold21", "ذهب عيار ٢١ (الجرام بالريال)"}, {"silver", "الفضة (الجرام بالريال)"}, {"oil", "نفط برنت (للبرميل)"}, {"xrp", "ريبل"},
    };
    static final int MAX_MARKETS = 4;
    static final String DEFAULT_MARKETS = "tasi,spx,btc";

    // 4.4 the screensaver's design (#design=; the owner approved them, docs/mockups/saver-designs): «معلومات» is the page as it always was
    static final String[][] SAVER_DESIGNS = {{"info", "معلومات"}, {"clean", "نظيفة"}, {"big", "ساعة كبيرة"}, {"frame", "لوحة"}, {"dayline", "خط اليوم"}};
    // a centred bar with any design (#bar=): the day's prayers, the left slot's item (a match, the countdown or the markets), or the weather
    static final String[][] SAVER_BARS = {{"none", "بدون"}, {"prayers", "الصلوات"}, {"slot", "الخانة اليسرى"}, {"weather", "الطقس"}};
    static final String[][] BAR_PLACES = {{"bottom", "أسفل الشاشة"}, {"clock", "تحت الساعة"}};
    // where the weather goes (#wx=): auto is as the design has it (its place at the top in «معلومات» and «خط اليوم», hidden in the others)
    static final String[][] WX_PLACES = {{"auto", "كما في التصميم"}, {"usual", "مكانه المعتاد"}, {"clock", "تحت الساعة"}, {"corner", "في الزاوية"}, {"off", "مخفي"}};

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
        if (!sp.contains("tournaments")) sp.edit().putBoolean("tournaments", sp.getAll().isEmpty()).apply();   // 4.3.1 «البطولات الكبرى»: on for a new install (nothing saved yet), off for one that already had settings
        GlassStyle.setTweaks(tweaks());
        if (ZoneFix.logger == null) ZoneFix.logger = line -> android.util.Log.i("UrTVZone", line);
        if (NetLog.logger == null) NetLog.logger = new NetLogcat();   // G3: one line per 10 minutes of requests (tag UrTVNet)
        if (CitiesMore.opener == null) { final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c; CitiesMore.opener = () -> app.getAssets().open(CitiesMore.FILE); }   // 4.3.3: read only on a search
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

    /** 4.4 the clubs: 20 as before without a word, then one warning once (FollowLimit), at most 40 */
    static final int SOFT_TEAMS = FollowLimit.SOFT_TEAMS, MAX_TEAMS = FollowLimit.MAX_TEAMS;
    /**
     * Whole leagues followed (#leagues=): the page downloads one small file for each built-in one, every 30 minutes; a league of the
     * catalogue (CatalogueLeagues) is asked of TheSportsDB itself, 2 requests every 30 minutes. 4.4: 8 as before without a word, then
     * one warning once (FollowLimit), at most 24; past 8 the page asks for the extra ones every second load (every third on a weak TV), the free key's rate.
     */
    static final int SOFT_LEAGUES = FollowLimit.SOFT_LEAGUES, MAX_LEAGUES = FollowLimit.MAX_LEAGUES;

    /** 4.4 the warning past the soft limit was shown and answered «أضف» (FollowLimit.LEAGUES or TEAMS): it is never shown again */
    boolean followWarned(String kind) { return sp.getBoolean("followWarned_" + kind, false); }
    void setFollowWarned(String kind) { sp.edit().putBoolean("followWarned_" + kind, true).apply(); }
    /** what one more league or club means now: FollowLimit.ADD, WARN or FULL */
    int followState(String kind) {
        return FollowLimit.check(kind, FollowLimit.LEAGUES.equals(kind) ? leagues().size() : teams().size(), followWarned(kind));
    }

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

    /** 4.3.3 a league followed from «المباريات»: a built-in one by its English name (its own file), else the catalogue's entry; false when there or full */
    boolean addLeague(String id, String ar, String en) {
        String[] b = en == null ? null : leagueByName(en);
        String e = b != null ? b[0] : CatalogueLeagues.entry(id, ar, en);
        if (e == null) return false;
        List<String> v = leagues();
        for (String s : v) if (sameLeague(s, id, en)) return false;
        if (FollowLimit.check(FollowLimit.LEAGUES, v.size(), followWarned(FollowLimit.LEAGUES)) != FollowLimit.ADD) return false;   // the ceiling, or the warning not yet answered
        v.add(e);
        setList("leagues", v);
        return true;
    }

    /** …and unfollowed: false when it was not followed */
    boolean removeLeague(String id, String en) {
        List<String> v = leagues();
        boolean had = v.removeIf(s -> sameLeague(s, id, en));
        if (had) setList("leagues", v);
        return had;
    }

    /** the stored league is this id (a catalogue entry) or this built-in one (by its English name) */
    static boolean sameLeague(String stored, String id, String en) {
        if (id != null && id.matches("\\d{1,6}") && CatalogueLeagues.same(stored, "id" + id)) return true;
        String[] b = en == null || en.trim().isEmpty() ? null : leagueByName(en);
        return b != null && stored.equalsIgnoreCase(b[0]);
    }

    /** 4.3.1 «البطولات الكبرى»: the big tournaments' games in the matches page and the cards (leagues/tournaments.json, one request every 30 minutes) */
    boolean tournaments() { return sp.getBoolean("tournaments", false); }
    void setTournaments(boolean v) { sp.edit().putBoolean("tournaments", v).apply(); }
    /** the ids of the tournaments the person unticked one by one */
    List<String> tournamentsOff() { List<String> v = new ArrayList<>(); for (String s : list("tournamentsOff")) if (s.matches("\\d{1,6}")) v.add(s); return v; }
    void setTournamentsOff(List<String> v) { setList("tournamentsOff", v); }

    /** What #leagues= and #lgn= carry (CatalogueLeagues.pageToken, names). */
    String leaguesFragment() {
        List<String> all = leagues(), tokens = new ArrayList<>();
        for (String s : all) tokens.add(CatalogueLeagues.pageToken(s));
        String names = CatalogueLeagues.names(all, Tr.EN.equals(lang()));
        List<String> off = tournamentsOff();
        return "&leagues=" + Uri.encode(join(tokens, ",")) + (names.isEmpty() ? "" : "&lgn=" + Uri.encode(names))
            + (tournaments() ? "&tour=1" + (off.isEmpty() ? "" : "&tourx=" + Uri.encode(join(off, ","))) : "");
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

    /** false when already followed, at the ceiling, or past the soft limit before its warning was answered «أضف» */
    boolean addTeam(String id, String name) {
        List<String> v = teams();
        for (String t : v) if (t.startsWith(id + "|")) return false;
        if (FollowLimit.check(FollowLimit.TEAMS, v.size(), followWarned(FollowLimit.TEAMS)) != FollowLimit.ADD) return false;   // the ceiling, or the warning not yet answered
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
        {"matches", "مباريات فرقك القادمة"}, {"nextmatch", "مباراة فريقك القادمة"}, {"matchday", "مباراة اليوم"}, {"livenow", "مباشر الآن"}, {"minitable", "جدول الترتيب"}, {"nextmatches", "مباراة كل فريق القادمة"}, {"yesterday", "نتائج أمس"}, {"weekend", "نهاية الأسبوع"}, {"cuppath", "طريق الكأس"}, {"formula", "فورمولا ١"}, {"national", "المنتخب السعودي"}, {"verse", "آية أو حديث اليوم"}, {"hadith", "حديث اليوم"}, {"wisdom", "حكمة اليوم"},
        {"adhkar", "أذكار الصباح والمساء"}, {"stocks", "الأسهم"}, {"tasks", "المهام"}, {"year", "السنة"},
        {"smart", "البطاقة الذكية"}, {"today", "يومك"}, {"album", "ألبوم صوري"},
        {"none", "لا شيء"},
    };

    /** a card's name (PANELS), or its key */
    static String panelName(String key) { for (String[] p : PANELS) if (p[0].equals(key)) return p[1]; return key; }

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
        {"nextmatch", "مباراة فرقك الجاية بس، وتحتها آخر ٥ مواجهات بين الفريقين"},
        {"matchday", "يوم مباراة فريقك: العد التنازلي ثم النتيجة والدقيقة ثم النتيجة النهائية، وتأخذ وسط شاشة التوقف"},
        {"livenow", "مباريات فرقك ودورياتك الجارية الآن بنتيجتها ودقيقتها"},
        {"minitable", "الأربعة الأوائل وفريقك وجاراه، ومستوى كل فريق في آخر ٥"},
        {"nextmatches", "مربع لكل فريق تتابعه: الخصم واليوم والوقت والبطولة وعلى أرضه أو خارجها"},
        {"yesterday", "نتائج أمس، فرقك أولًا ثم دورياتك"},
        {"weekend", "مباريات الخميس والجمعة والسبت في دورياتك، ومباراة فريقك ذهبية"},
        {"cuppath", "أدوار البطولة الإقصائية وطريق فريقك فيها"},
        {"formula", "السباق القادم وجلساته بتوقيت مدينتك"},
        {"national", "مباراة المنتخب السعودي القادمة: الخصم واليوم والوقت وآخر مواجهة"},
        {"verse", "آية يوم، وحديث صحيح من الكتب الستة اليوم اللي بعده، ومعه مصدره"},
        {"hadith", "حديث اليوم وحده؛ مع بطاقة «آية أو حديث اليوم» تبقى تلك للآية"},
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
        {"boxes", "مواقيت اليوم مربعات في صف، والصلاة القادمة مضيئة وتحتها كم باقي"},
        {"classic", "مواقيت اليوم في صف والمباريات والعدّ التنازلي كما كانت قبل ٤٫٤"},
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

    /** the label of a choice of one of the tables above; the first one when the value is not in it */
    static String labelOf(String[][] table, String v) {
        for (String[] c : table) if (c[0].equals(v)) return c[1];
        return table[0][1];
    }

    // the values are checked where they are set (the settings' lists, SaverReceiver); the page takes anything else as the default
    String design() { return sp.getString("design", "info"); }
    void setDesign(String v) { sp.edit().putString("design", v).apply(); }
    String bar() { return sp.getString("bar", "none"); }
    void setBar(String v) { sp.edit().putString("bar", v).apply(); }
    String barAt() { return sp.getString("barAt", "bottom"); }
    void setBarAt(String v) { sp.edit().putString("barAt", v).apply(); }
    String wxAt() { return sp.getString("wxAt", "auto"); }
    void setWxAt(String v) { sp.edit().putString("wxAt", v).apply(); }

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
    /** 4.4 «إجازات المدارس»: cd=school, the page counts to the next school break of the official calendar (urtv-data), with its name */
    static final String SCHOOL = "school";
    static final String SCHOOL_LABEL = "إجازات المدارس";
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

    /** A14 (4.3.3): a TV with this many cores or fewer is LOW whatever its memory (the same number as the home's Ram.LITE_CORES) */
    static final int LITE_CORES = 2;

    /** The rule with the cores (0 = not known): few cores is LOW too. */
    static int ramTier(boolean lowRamFlag, long mb, int cores) { return cores > 0 && cores <= LITE_CORES ? RAM_LOW : ramTier(lowRamFlag, mb); }

    /**
     * A14 «الوضع الخفيف»: the person's choice over what the TV found: "on" is LOW (every cut: fewer animations, slower refresh, smaller photos, no blur),
     * "off" is never LOW (a TV found LOW keeps MID's cheap memory cuts, nothing the person sees), "auto" (the default) what was found. The same rule as the home's Ram.liteTier.
     */
    static int liteTier(int found, String lite) { return "on".equals(lite) ? RAM_LOW : "off".equals(lite) ? Math.max(found, RAM_MID) : found; }

    static final String[] LITE = {"auto", "on", "off"};
    String lite() { String v = sp.getString("lite", "auto"); return "on".equals(v) || "off".equals(v) ? v : "auto"; }
    void setLite(String v) { sp.edit().putString("lite", "on".equals(v) || "off".equals(v) ? v : "auto").apply(); ramTier = -1; }
    /** what the settings row says */
    static String liteLabel(String v, boolean lowNow) { return "on".equals(v) ? "مفعّل" : "off".equals(v) ? "مطفأ" : lowNow ? "تلقائي، ومفعّل على هذا التلفزيون" : "تلقائي، ومطفأ على هذا التلفزيون"; }

    static int ramNamed(String n) { return "low".equals(n) ? RAM_LOW : "mid".equals(n) ? RAM_MID : "normal".equals(n) ? RAM_NORMAL : -1; }

    /** This TV's tier, read once (a forced tier from `saver.sh ramtier` wins, from the next start of the app). The one place that decides. */
    static int ramTier(Context c) {
        if (ramTier < 0) {
            boolean flag = false;
            try { flag = ((android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).isLowRamDevice(); } catch (RuntimeException ignored) { }
            int t = ramTier(flag, ramMb(c), Runtime.getRuntime().availableProcessors());
            try {
                android.content.SharedPreferences s = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
                t = liteTier(t, s.getString("lite", "auto"));   // A14 «الوضع الخفيف»
                int f = ramNamed(s.getString("ramtier", ""));
                if (f >= 0) t = f;   // the tester's switch wins
            } catch (RuntimeException ignored) { }
            ramTier = t;
        }
        return ramTier;
    }
    private static int ramTier = -1;

    /** LOW: every cut */
    static boolean lowRam(Context c) { return ramTier(c) == RAM_LOW; }
    /** «الوضع الخفيف» in effect now: on, or auto on a weak TV (less motion: a fade where a stronger TV moves) */
    boolean liteNow(Context c) { String v = lite(); return "on".equals(v) || ("auto".equals(v) && lowRam(c)); }
    /** MID: only the cheap memory cuts (photos at 1280, own photos 25, the markets a little slower) */
    static boolean midRam(Context c) { return ramTier(c) == RAM_MID; }
    /** LOW or MID */
    static boolean tightRam(Context c) { return ramTier(c) != RAM_NORMAL; }
    /** the debug switch: low | mid | normal | auto (auto = the real tier) */
    void setRamTier(String v) { sp.edit().putString("ramtier", "auto".equals(v) ? "" : v).apply(); }
    String ramTierSetting() { String v = sp.getString("ramtier", ""); return v.isEmpty() ? "auto" : v; }
    void setPhotoMinutes(String v) { sp.edit().putString("photoMinutes", v).apply(); }

    /**
     * The release «تحديث تلفزيوني» (the home screen's screen and the phone page) and the newest-version line read: "latest" (default), "beta"
     * (the row «النسخ التجريبية») or "next", the test build (agents only, `saver.sh updatechannel`). The settings export leaves it out.
     */
    String updateChannel() { String v = sp.getString("updateChannel", "latest"); return "next".equals(v) || "beta".equals(v) ? v : "latest"; }
    void setUpdateChannel(String v) { sp.edit().putString("updateChannel", "next".equals(v) || "beta".equals(v) ? v : "latest").apply(); }
    /** «النسخ التجريبية» (4.3.3, off by default): «تحديث تلفزيوني» reads the apk-beta release; off is the latest. The agents' "next" is its own word. */
    boolean betaOn() { return "beta".equals(updateChannel()); }

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
    /** 4.3.3 «المباريات المنتهية» on the football home: shown (the default, as always) or hidden; #fbhide=1 only while hidden */
    boolean fbHideDone() { return sp.getBoolean("fbHideDone", false); }
    /** 4.3.3 «التاريخ بالأرقام»: the dates in words (the default, as always) or as day/month/year, Hijri and Gregorian; #datenum=1 only while on. The digits stay «الأرقام»'s. */
    boolean dateNum() { return sp.getBoolean("dateNum", false); }
    /** 4.3.3 «زر البحث»: the home's round button to the TV's own search inside the apps; off by default, #searchpill=1 only while on */
    boolean searchPill() { return sp.getBoolean("searchPill", false); }
    void setSearchPill(boolean v) { sp.edit().putBoolean("searchPill", v).apply(); }
    void setDateNum(boolean v) { sp.edit().putBoolean("dateNum", v).apply(); }
    void setFbHideDone(boolean v) { sp.edit().putBoolean("fbHideDone", v).apply(); }
    /** 4.4 «تنبيه بداية المباراة»: at a followed club's kick-off a short whistle and a small notice (on, the default); its sound (the whistle or
     *  «بدون صوت»), a goal and the final whistle (off), and «فوق تطبيق آخر» (off: the home and the screensaver only) */
    boolean kickoff() { return sp.getBoolean("kickoff", true); }
    void setKickoff(boolean v) { sp.edit().putBoolean("kickoff", v).apply(); }
    boolean kickoffWhistle() { return sp.getBoolean("kickoffWhistle", true); }
    void setKickoffWhistle(boolean v) { sp.edit().putBoolean("kickoffWhistle", v).apply(); }
    boolean kickoffGoal() { return sp.getBoolean("kickoffGoal", false); }
    void setKickoffGoal(boolean v) { sp.edit().putBoolean("kickoffGoal", v).apply(); }
    boolean kickoffEnd() { return sp.getBoolean("kickoffEnd", false); }
    void setKickoffEnd(boolean v) { sp.edit().putBoolean("kickoffEnd", v).apply(); }
    boolean kickoffOver() { return sp.getBoolean("kickoffOver", false); }
    void setKickoffOver(boolean v) { sp.edit().putBoolean("kickoffOver", v).apply(); }
    /** what the launcher reads (the provider's column "kickoff"): "on|whistle|over", e.g. "1|whistle|0" */
    String kickoffColumn() { return (kickoff() ? "1" : "0") + "|" + (kickoffWhistle() ? "whistle" : "none") + "|" + (kickoffOver() ? "1" : "0"); }
    void setBackSaver(boolean v) { sp.edit().putBoolean("backSaver", v).apply(); }

    /**
     * «أزرار الريموت»: the app each of the remote's YouTube, Netflix and Prime Video keys opens ("" = the key's own app). The home keeps its own
     * copy (Dock.key_app_*, the one `dock.sh key` sets) and takes this one when it changes (Dock.followKeyApps), through ConfigProvider's key_apps.
     */
    /** This TV's remote (RemoteButtons, the home's same table): the buttons that get a row. Read once; NONE (no rows) if the build's fields cannot be read */
    static int remoteTv() {
        if (remoteTv == -1) {
            try {
                remoteTv = RemoteButtons.tv(android.os.Build.MANUFACTURER, android.os.Build.BRAND, android.os.Build.MODEL, android.os.Build.DEVICE,
                    android.os.Build.VERSION.SDK_INT);
            } catch (RuntimeException | LinkageError e) {
                remoteTv = RemoteButtons.NONE;   // never on a TV; the settings' test run on a computer has no Build
            }
        }
        return remoteTv;
    }
    private static int remoteTv = -1;
    static boolean remoteKeyOk(String key) { return key != null && RemoteButtons.byId(remoteTv(), key) != null; }
    static boolean keyAppOk(String pkg) { return pkg != null && (pkg.isEmpty() || pkg.length() <= 200 && pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")); }
    String keyApp(String key) { String v = sp.getString("keyApp_" + key, ""); return keyAppOk(v) ? v : ""; }
    void setKeyApp(String key, String pkg) { if (remoteKeyOk(key) && keyAppOk(pkg)) sp.edit().putString("keyApp_" + key, pkg).apply(); }
    /** "youtube=PKG,netflix=,prime=": what ConfigProvider hands the home */
    String keyApps() {
        StringBuilder b = new StringBuilder();
        for (RemoteButtons.Button x : RemoteButtons.buttons(remoteTv())) b.append(b.length() > 0 ? "," : "").append(x.id).append('=').append(keyApp(x.id));
        return b.toString();
    }

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

    /**
     * 4.3.3 today's published prayer corrections for the city (PrayerFix), from the kept copy; fetched again in the background twice a day.
     * 4.4 QA-13: the copy is held in memory (prayerFixKnown); a provider or worker thread that finds none yet waits for the first read (a helper
     * thread, as it always waited), and the UI thread never does: it takes what is known and the screen draws again when the read is done.
     */
    int[] prayerFix() {
        if (!prayerFixLoaded && android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            final Context app = ctx.getApplicationContext();
            Thread first = new Thread(() -> {
                ContentFile.refresh(app, PrayerFix.PATH, PrayerFix.NAME, 12 * 3600_000L);
                PRAYER_FIX.load(app);
                prayerFixLoaded = true;
            }, "prayer-fix-first");
            first.start();
            try { first.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return prayerFixKnown(null);
    }

    private static final ContentFile.Kept PRAYER_FIX = new ContentFile.Kept(PrayerFix.NAME);
    private static volatile long prayerFixAskedAt = -60_000L;
    private static volatile boolean prayerFixLoaded;

    /**
     * 4.4 QA-13: prayerFix() for a screen's draw on the UI thread: the kept copy as this process last read it (never the file), and a thread that
     * fetches and reads it again at most once a minute; {@code changed} runs on the main thread when what it read is another copy than the one
     * drawn from (the first read of a process, or a new file), so the screen draws again with it.
     */
    int[] prayerFixKnown(final Runnable changed) {
        final Context app = ctx.getApplicationContext();
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - prayerFixAskedAt >= 60_000L) {
            prayerFixAskedAt = now;
            Thread t = new Thread(() -> {
                Object before = PRAYER_FIX.value();
                ContentFile.refresh(app, PrayerFix.PATH, PrayerFix.NAME, 12 * 3600_000L);
                PRAYER_FIX.load(app);
                prayerFixLoaded = true;
                if (PRAYER_FIX.value() != before) {
                    if (changed != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(changed);
                    // 4.4 review F2: no screen of ours to draw again (the dream's start): tell the ones that are up, which rebuild only when their address differs; a repeat read is the same copy, so no second broadcast
                    else app.sendBroadcast(new android.content.Intent(CHANGED).setPackage(app.getPackageName()));
                }
            }, "prayer-fix");
            t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
        }
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
        f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        long t0 = System.currentTimeMillis();
        return PrayerFix.forDay(PRAYER_FIX.value(), cityCountry(), city()[0], f.format(new java.util.Date(t0 + ZoneFix.offset(cityTz(), t0))));
    }

    /** the adj= of the fragment: the person's own minutes where set, the published correction elsewhere; nothing when both are none */
    String prayerAdjFragment() {
        int[] fix = prayerFix(), own = prayerAdj();
        boolean on = prayerAdjOn();
        return PrayerAdj.fragment(on || PrayerAdj.any(fix), PrayerFix.combine(on, own, fix));
    }
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
        String url = dashboardUrl(c) + CleanMask.fragment(CleanMask.state(c))   // «UrTV النظيف»: the page shows the photo and the clock only
            + (kickoff() && NowPlayingBridge.kidsOn(c) ? "&kids=1" : "");   // 4.4 kids mode: no kick-off notice over the screensaver
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
            + (clearText() ? "&clear=1" : "")   // «نص أوضح»: the page rewrites its greys to full ink
            + (lowRam(ctx) ? "&lowram=1" : midRam(ctx) ? "&midram=1" : "")   // a weak TV (a user report on 4.1, lag): the page's slower timers, no seconds, no transitions
            + HomeExit.fragment(homeExit())
            + (occasion() ? "" : "&occasion=0") + (nightDim() ? "" : "&nightdim=0") + (tasksPill() ? "" : "&taskspill=0")
            + (backSaver() ? "&backsaver=1" : "")
            + (fbHideDone() ? "&fbhide=1" : "") + (kickoff() ? "" : "&ko=0") + (kickoffGoal() ? "&kogoal=1" : "") + (kickoffEnd() ? "&koend=1" : "") + (dateNum() ? "&datenum=1" : "") + (searchPill() ? "&searchpill=1" : "")   // the football home without the games that ended
            + "&cardstyle=" + cardStyle()
            + "&design=" + design() + "&bar=" + bar() + "&barat=" + barAt() + "&wx=" + wxAt()
            + "&adhanbar=" + (adhanBar() ? "1" : "0")
            + "&yearstrip=" + (yearStrip() ? "1" : "0")
            + (sunrise() ? "&sunrise=1" : "") + (prayerBar() ? "&prayerbar=1" : "") + (wxMore() ? "&wxmore=1" : "")
            + ("levant".equals(months()) ? "&months=levant" : "")
            + prayerAdjFragment()   // 4.3.3: the person's own minutes, and a published correction where they set none (PrayerFix)
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
    /** how often the album's photo changes: "seconds" (every 4 s, the default), "ten" (every 10 s) or "minute"; the older yes/no «a minute» setting is read as it was */
    String albumMode() {
        String m = sp.getString("albumMode", null);
        if (m == null) return sp.getBoolean("albumMinute", false) ? "minute" : "seconds";
        return "ten".equals(m) || "minute".equals(m) ? m : "seconds";
    }
    void setAlbumMode(String v) { sp.edit().putString("albumMode", v).apply(); }
    boolean albumMinute() { return "minute".equals(albumMode()); }
    void setAlbumMinute(boolean v) { setAlbumMode(v ? "minute" : "seconds"); }
    /** the title «ألبوم صوري» above the photo: shown (the default) or hidden */
    boolean albumTitle() { return sp.getBoolean("albumTitle", true); }
    void setAlbumTitle(boolean v) { sp.edit().putBoolean("albumTitle", v).apply(); }
    /** "fit" (the whole photo, never cropped: the default) or "fill" (the card is filled and the top third kept, so faces stay in) */
    String albumFit() { return "fill".equals(sp.getString("albumFit", "fit")) ? "fill" : "fit"; }
    void setAlbumFit(String v) { sp.edit().putString("albumFit", "fill".equals(v) ? "fill" : "fit").apply(); }
    /** "normal" or "tall" (about twice the photo's height; the page steps the card down where it does not fit) */
    String albumSize() { return "tall".equals(sp.getString("albumSize", "normal")) ? "tall" : "normal"; }
    void setAlbumSize(String v) { sp.edit().putString("albumSize", "tall".equals(v) ? "tall" : "normal").apply(); }
    boolean albumDate() { return sp.getBoolean("albumDate", true); }
    void setAlbumDate(boolean v) { sp.edit().putBoolean("albumDate", v).apply(); }
    private String albumParam() {
        String m = albumMode();
        return ("seconds".equals(m) ? "" : "&albummode=" + m) + (albumDate() ? "" : "&albumdate=0") + (albumTitle() ? "" : "&albumtitle=0")
            + ("fill".equals(albumFit()) ? "&albumfit=fill" : "") + ("tall".equals(albumSize()) ? "&albumsize=tall" : "");   // only what is not the default, so the page's address is as before
    }

    /** «صور شاشة التوقف»: the packs that are on (ids), and whether our own set («صورنا», on by default) is. Sent as #packs=id:count,... and #ours=0 only when set, so the fragment is as before. */
    java.util.Set<String> packsOn() { return PhotoPacks.parseOn(sp.getString("packsOn", "")); }
    void setPacksOn(java.util.Set<String> v) { sp.edit().putString("packsOn", PhotoPacks.joinOn(v)).apply(); }
    /** 4.3.3 «النسبة»: each pack's share of the packs' turns in percent (none: by its size, as always) */
    java.util.Map<String, Integer> packShares() { return PhotoPacks.parseShares(sp.getString("packShares", "")); }
    void setPackShare(String id, int pct) {
        java.util.Map<String, Integer> m = packShares();
        if (pct > 0) m.put(id, pct); else m.remove(id);
        sp.edit().putString("packShares", PhotoPacks.joinShares(m)).apply();
    }
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
        return PhotoPacks.fragment(packsOn(), have, packShares()) + (ours ? "" : "&ours=0");
    }

    /** the home's own choice for «صوري»: "mix" (with ours), "only", "off"; null (never chosen) follows the screensaver's own mode, as before */
    String homeMine() { return sp.getString("homeMine", null); }
    void setHomeMine(String v) { if (v == null) sp.edit().remove("homeMine").apply(); else sp.edit().putString("homeMine", v).apply(); bumpOwnRev(); }   // the home chooses its photo again at once
    /** what the home uses now: its own choice, else the screensaver's mode */
    String homeMineEffective() { String v = homeMine(); return v != null ? v : Own.ALBUM.equals(ownMode()) ? "off" : ownMode(); }   // the screensaver's «للألبوم فقط» keeps them out of the home's wallpaper too

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
    /**
     * 4.4 the owner: the look changed (layout, preset): each home card that does not suit it is swapped for the nearest of its kind (CardFit), the
     * person's own cards kept to come back when the look suits them again; the note for the toast is kept until the settings take it (takeFitNote)
     */
    void fitHomeCards() {
        List<String> mine = sp.contains("homePanelMine") ? parseHomePanels(sp.getString("homePanelMine", "none")) : homePanels();
        List<String[]> swaps = new ArrayList<>();
        List<String> fit = CardFit.fitted(mine, homeLayout(), swaps);
        if (swaps.isEmpty()) {   // nothing to swap: the person's own cards, as they picked them
            if (sp.contains("homePanelMine")) { setHomePanels(mine); sp.edit().remove("homePanelMine").apply(); }
            return;
        }
        sp.edit().putString("homePanelMine", mine.isEmpty() ? "none" : join(mine, ",")).putString("fitNote", CardFit.swapNote(swaps, PANELS)).apply();
        setHomePanels(fit);
    }
    /** the swap's note, once (the settings show it as a toast) */
    String takeFitNote() { String n = sp.getString("fitNote", null); if (n != null) sp.edit().remove("fitNote").apply(); return n; }

    void setHomePanelAt(int i, String card) {
        sp.edit().remove("homePanelMine").apply();   // the person's own pick: theirs now, never swapped (an unsuited one only gets its note)
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
        return "light".equals(v) ? "فاتح" : "auto".equals(v) ? "تلقائي، فاتح من الشروق إلى المغرب، وداكن بعده" : "داكن";
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
    /** «نص أوضح»: the grey words and numbers of the home and the screensaver in the look's full text colour (off by default) */
    boolean clearText() { return sp.getBoolean("clearText", false); }
    void setClearText(boolean v) { sp.edit().putBoolean("clearText", v).apply(); }

    /** «توهّج لون التطبيق»: off (the default, for new installs and old ones alike), light or on */
    String glowMode() { String v = sp.getString("glowMode", "off"); return "on".equals(v) || "light".equals(v) ? v : "off"; }
    void setGlowMode(String v) { sp.edit().putString("glowMode", "on".equals(v) || "light".equals(v) ? v : "off").apply(); GlassStyle.setTweaks(tweaks()); }

    String tweaks() {
        String a = accent(), f = focusStyle();
        int b = barPercent(), d = wallDim();
        boolean sync = lookSync();
        boolean clear = clearText();
        if (a.isEmpty() && b == 100 && d == 0 && "solid".equals(f) && sync && !clear && "off".equals(glowMode())) return "";
        StringBuilder s = new StringBuilder("accent=" + a + ";bar=" + b + ";dim=" + d + ";focus=" + f);
        if (!sync) {
            s.append(";sync=off");
            for (int i = 0; i < GlassStyle.SURFACES.length; i++) s.append(';').append(GlassStyle.SURFACES[i]).append('=').append(surfacePercent(i)).append(':').append(surfaceFill(i));
        }
        if (clear) s.append(";text=clear");
        if (!"off".equals(glowMode())) s.append(";glow=").append(glowMode());
        return s.toString();
    }
    static String lookLabel(String v) { return GlassStyle.LOOK_NAMES[java.util.Arrays.asList(GlassStyle.LOOKS).indexOf(GlassStyle.look(v))]; }

    /** The greeting and the weather line on the launcher's home screen (default on). Off: the plain good-morning it had. */
    boolean greeting() { return sp.getBoolean("greeting", true); }
    void setGreeting(boolean v) { sp.edit().putBoolean("greeting", v).apply(); }
    /** Based on PR #45 by AlaaAlmasri: a name after the home's greeting («صباح الخير، علاء»); empty (the default) is the greeting as it always was. At most NAME_MAX letters, one line. */
    static final int NAME_MAX = 30;
    String greetingName() { return sp.getString("greeting_name", ""); }
    void setGreetingName(String v) { sp.edit().putString("greeting_name", cleanName(v)).apply(); }
    /** the name as it is kept: the spaces folded, no line breaks or control characters, at most NAME_MAX characters (a bidi isolate is added where it is shown, not here) */
    static String cleanName(String v) {
        if (v == null) return "";
        String n = v.replaceAll("[\\p{Cntrl}\\u2066-\\u2069\\u202A-\\u202E\\u200E\\u200F]", " ").replaceAll("\\s+", " ").trim();
        if (n.length() > NAME_MAX) n = n.substring(0, NAME_MAX).trim();
        return n;
    }

    /**
     * «زر تلاوة وأذان» on the launcher's home screen (default shown). Hidden removes only the button: the adhan, its alarm and bar, the
     * any-key stop, the «يُتلى الآن» card and a Quran that was started elsewhere go on; the screen opens from the app menu (long OK on an
     * app) or from the «يُتلى الآن» card instead.
     */
    /** the home's quick buttons the person hid (#52): any of markets, matches, center, phone, inputs («المصادر») (the others have their own switch: «زر المهام», «زر تلاوة وأذان»); "" = all shown */
    static final String[] QUICK_BUTTONS = {"markets", "matches", "center", "phone", "inputs"};
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
    /**
     * 4.4 the look in three independent choices (the owner): «التخطيط» (our layout, the default, or the football home), «الخلفية» (the photos as always,
     * or the scenes) and «الساعة» (plain, or glass behind the photo's scenery). The presets «شكل تلفزيونك» (the default) and «شكل كنبة» set the three.
     * A layout «scenes» kept by a test build of 4.3.3 reads as our layout with the scenes and the glass clock.
     */
    static final String[] HOME_LAYOUTS = {"classic", "couch", "football"};
    static final String[] HOME_LAYOUT_NAMES = {"تلفزيونك", "كنبة", "كرة القدم"};
    static final String[] HOME_BGS = {"photos", "scenes"};
    static final String[] HOME_BG_NAMES = {"صوري وحزم الصور", "الآفاق"};
    static final String[] HOME_CLOCKS = {"plain", "glass"};
    static final String[] HOME_CLOCK_NAMES = {"عادية", "زجاجية خلف الصورة"};
    /**
     * 4.4 «شكل التطبيقات», the row's three ways (the owner approved them): «بنرات» each app's TV banner, «أيقونات» each app's icon in a
     * rounded square (the two with «تكبير وظل» and the name under the focused one, no tray), «مربعات ملونة» today's tray. Stored as
     * "banners", "icons", or, for the tray, 4.4's first values: "frame" (the ring, the default) or "zoom" (bigger with a shadow), which
     * the person keeps (a TV that had «تكبير وظل» keeps it on its tiles). The launcher reads the stored value (FavBar.look, FavBar.zoom).
     */
    static final String[] HOME_APPS = {"banners", "icons", "tiles", "glass"};
    static final String[] HOME_APPS_NAMES = {"بنرات", "أيقونات", "مربعات ملونة", "tvOS"};   // 4.4 beta 2 (the owner): what comes from tvOS Launcher is named «tvOS»
    static final String[] APPS_STORED = {"banners", "icons", "frame", "zoom", "glass"};
    /**
     * 4.5 «زجاجي» (the owner approved docs/mockups/glass/): a liquid-glass bar at the bottom, each app's icon as a rounded square, the name above
     * the focused one only. Its size, 50 to 150 % in steps of 10 (100 the default), and its rows, 1 to 3 (1): «حجم شريط tvOS», «صفوف شريط tvOS».
     */
    static final String[] GLASS_SIZES = {"50", "60", "70", "80", "90", "100", "110", "120", "130", "140", "150"};
    static final String[] GLASS_ROWS = {"1", "2", "3"};
    static final String[] GLASS_ROWS_NAMES = {"صف واحد", "صفّان", "ثلاثة صفوف"};
    String glassSize() { String v = sp.getString("glassSize", "100"); return java.util.Arrays.asList(GLASS_SIZES).contains(v) ? v : "100"; }
    void setGlassSize(String v) { if (java.util.Arrays.asList(GLASS_SIZES).contains(v)) sp.edit().putString("glassSize", v).apply(); }
    String glassRows() { String v = sp.getString("glassRows", "1"); return java.util.Arrays.asList(GLASS_ROWS).contains(v) ? v : "1"; }
    void setGlassRows(String v) { if (java.util.Arrays.asList(GLASS_ROWS).contains(v)) sp.edit().putString("glassRows", v).apply(); }
    static String glassSizeLabel(String v) { return DigitsText.convert(v, false) + "٪"; }
    static String glassRowsLabel(String v) { int i = java.util.Arrays.asList(GLASS_ROWS).indexOf(v); return GLASS_ROWS_NAMES[Math.max(0, i)]; }
    /** the presets: name, layout, background, clock, the apps' focus */
    static final String[][] LOOKS = {{"urtv", "شكل تلفزيونك", "classic", "photos", "plain", "frame"}, {"couch", "شكل كنبة", "couch", "scenes", "glass", "icons"}};
    /** the apps' value of a preset as stored: the same, or Couch's before «أيقونات» ("zoom": a TV set to Couch then is still on Couch) */
    private static boolean presetApps(String want, String have) { return want.equals(have) || ("icons".equals(want) && "zoom".equals(have)); }
    private boolean oldScenes() { return "scenes".equals(sp.getString("homeLayout", "")); }
    String homeLayout() { String v = sp.getString("homeLayout", HOME_LAYOUTS[0]); return java.util.Arrays.asList(HOME_LAYOUTS).contains(v) ? v : HOME_LAYOUTS[0]; }
    void setHomeLayout(String v) {
        if (!java.util.Arrays.asList(HOME_LAYOUTS).contains(v)) return;
        boolean was = oldScenes();
        if (was) sp.edit().putString("homeBg", homeBg()).putString("homeClock", homeClock()).apply();   // the old value's scenes and clock kept as their own choices
        sp.edit().putString("homeLayout", v).apply();
        fitHomeCards();
    }
    static String homeLayoutLabel(String v) { int i = java.util.Arrays.asList(HOME_LAYOUTS).indexOf(v); return HOME_LAYOUT_NAMES[Math.max(0, i)]; }
    /** the look the home's cards are decided for (CardFit.LOOKS): «tvOS» with the full tvOS look, else the home's layout */
    String cardLook() { return "tvos".equals(sp.getString("preset", "")) ? "tvos" : homeLayout(); }
    String homeBg() { String v = sp.getString("homeBg", oldScenes() ? "scenes" : "photos"); return java.util.Arrays.asList(HOME_BGS).contains(v) ? v : "photos"; }
    void setHomeBg(String v) { if (java.util.Arrays.asList(HOME_BGS).contains(v)) sp.edit().putString("homeBg", v).apply(); }
    static String homeBgLabel(String v) { int i = java.util.Arrays.asList(HOME_BGS).indexOf(v); return HOME_BG_NAMES[Math.max(0, i)]; }
    String homeClock() { String v = sp.getString("homeClock", oldScenes() ? "glass" : "plain"); return java.util.Arrays.asList(HOME_CLOCKS).contains(v) ? v : "plain"; }
    void setHomeClock(String v) { if (java.util.Arrays.asList(HOME_CLOCKS).contains(v)) sp.edit().putString("homeClock", v).apply(); }
    static String homeClockLabel(String v) { int i = java.util.Arrays.asList(HOME_CLOCKS).indexOf(v); return HOME_CLOCK_NAMES[Math.max(0, i)]; }
    /** the stored value (APPS_STORED), sent to the launcher as it is */
    String appsStyle() { String v = sp.getString("appsStyle", "frame"); return java.util.Arrays.asList(APPS_STORED).contains(v) ? v : "frame"; }
    /** the way it is (HOME_APPS): "banners", "icons" or "tiles" */
    static String appsWay(String stored) { return "banners".equals(stored) || "icons".equals(stored) || "glass".equals(stored) ? stored : "tiles"; }
    /** a way (HOME_APPS) or a stored value (APPS_STORED); «مربعات ملونة» keeps the tiles' own focus when it is already the way */
    void setAppsStyle(String v) {
        if ("tiles".equals(v)) { if (!"tiles".equals(appsWay(appsStyle()))) sp.edit().putString("appsStyle", "frame").apply(); return; }
        if (java.util.Arrays.asList(APPS_STORED).contains(v)) sp.edit().putString("appsStyle", v).apply();
    }
    static String appsStyleLabel(String v) { int i = java.util.Arrays.asList(HOME_APPS).indexOf(appsWay(v)); return HOME_APPS_NAMES[Math.max(0, i)]; }
    /** the preset the three choices match ("urtv", "couch"), or "" (custom) */
    String look3() {
        for (String[] l : LOOKS) if (l[2].equals(homeLayout()) && l[3].equals(homeBg()) && l[4].equals(homeClock()) && presetApps(l[5], appsStyle())) return l[0];
        return "";
    }
    void setLook3(String key) {
        for (String[] l : LOOKS) if (l[0].equals(key)) { sp.edit().putString("homeLayout", l[2]).putString("homeBg", l[3]).putString("homeClock", l[4]).putString("appsStyle", l[5]).apply(); fitHomeCards(); return; }
    }
    /**
     * 4.4 the owner: «أشكال جاهزة» (docs/mockups/presets): six looks, each a set of values for rows that already exist (no cost when the TV runs).
     * Choosing one keeps the person's own values once (presetBefore, the first time a look is left from «شكلك المعتاد»), puts them back, then sets
     * the look's own; «شكلك المعتاد» puts them back exactly. A row changed after reads «مخصّص». The city, the adhan, the teams and the locks are never
     * touched: a look is the look only. Two of the owner's open questions are flags here, both on until he says otherwise.
     */
    // ---- 4.4 beta 2 (the owner: «with a protective way»): each new look item behind its own switch, off by default and after an update
    /** 4.4 beta 2 «شكل شاشة الإعدادات» (the owner): the settings screen's own style, apart from the looks: «أزرار» (the 4.3.2 hub, the default) or
     *  «كنبة» (couch-tv's side list and compact rows), or «tvOS» (the 4.3.2 screen's logic in the tvOS manner, TvosStyle; approved and built for beta 2).
     *  Every place of the hub is in the side list (tests/settings_one_form_test.py lists each with its new place). */
    static final String[] SETTINGS_STYLES = {"buttons", "couch", "tvos"};
    static final String[] SETTINGS_STYLE_NAMES = {"أزرار", "كنبة", "tvOS"};
    /** the style; read the first time after the update as the screen the TV shows now («كنبة» with the look «كنبة», else «أزرار»): no surprise switch */
    String settingsStyle() {
        String v = sp.getString("settingsStyle", null);
        if (v == null) {
            v = couchOn() || sp.getBoolean("uiOneForm", false) ? "couch" : "buttons";
            SharedPreferences.Editor e = sp.edit().putString("settingsStyle", v);
            if (couchOn() && !sp.getBoolean("uiOneForm", false)) e.putString("settingsStyleBefore", "buttons");   // the look «كنبة» gave it: «شكلك المعتاد» gives the buttons back, as before
            e.apply();
        }
        return java.util.Arrays.asList(SETTINGS_STYLES).contains(v) ? v : "buttons";
    }
    /** the person's own choice: kept as theirs («شكلك المعتاد» no longer puts an older one back) */
    void setSettingsStyle(String v) { if (java.util.Arrays.asList(SETTINGS_STYLES).contains(v)) sp.edit().putString("settingsStyle", v).remove("settingsStyleBefore").apply(); }
    static String settingsStyleLabel(String v) { int i = java.util.Arrays.asList(SETTINGS_STYLES).indexOf(v); return SETTINGS_STYLE_NAMES[Math.max(0, i)]; }
    /** «لوحة المظهر على الرئيسية»: the row that opens LookPanelActivity, and the panel itself (saver.sh lookpanel) */
    boolean uiPanel() { return sp.getBoolean("uiPanel", false); }
    void setUiPanel(boolean on) { sp.edit().putBoolean("uiPanel", on).apply(); }
    /** «زر رجّع الشكل السابق»: the first row of «المظهر» after a change of the look */
    boolean uiUndo() { return sp.getBoolean("uiUndo", false); }
    void setUiUndo(boolean on) { sp.edit().putBoolean("uiUndo", on).apply(); }
    /** «تبديل الشكل بلا وميض»: the home told at once (LOOK_CHANGED) and applying a look whole behind its still picture; off: as in 4.4 (the next return or minute) */
    boolean uiSmooth() { return sp.getBoolean("uiSmooth", false); }
    void setUiSmooth(boolean on) { sp.edit().putBoolean("uiSmooth", on).apply(); }
    static final boolean PRESETS_REPLACE_COUCH = true;    // «أشكال جاهزة» replaces the «شكل كنبة» switch («كنبة» is the Couch look)
    static final boolean CLEAN_HIDES_BUTTONS = true;      // «صورة فقط» hides the home's round buttons (they stay in the control centre and the menu key)
    static final String[] PRESETS = {"tv", "couch", "clean", "family", "sport", "calm"};
    static final String[] PRESET_NAMES = {"شكلك المعتاد", "كنبة", "صورة فقط", "مواقيت يومي", "مباريات اليوم", "ليل هادئ"};
    static final String[] PRESET_ABOUT = {
        "اختياراتك كما كانت قبل أي شكل، والتطبيقات مربعات ملونة",
        "تخطيط كنبة، الآفاق، ساعة زجاجية خلف الصورة، التطبيقات أيقونات، داكن",
        CLEAN_HIDES_BUTTONS ? "الصورة والساعة الزجاجية فقط، بلا بطاقات ولا تحية ولا أزرار" : "الصورة والساعة الزجاجية فقط، بلا بطاقات ولا تحية",
        "ساعة زجاجية، والصلوات الست في صف، ومواقيت اليوم في شاشة التوقف",
        "تخطيط كرة القدم، مباشر الآن ومباريات فرقك والترتيب، داكن",
        "الصورة معتّمة، ساعة كبيرة، الصلاة القادمة فقط، داكن مع تعتيم الليل"};
    /** what a look sets, in this order; its Arabic names for the «مخصّص» line */
    static final String[] PRESET_FIELDS = {"layout", "bg", "clock", "apps", "cards", "greeting", "buttons", "appearance", "nightDim", "saverPanel", "clockSize", "wallDim"};
    static final String[] PRESET_FIELD_NAMES = {"التخطيط", "الخلفية", "الساعة", "شكل التطبيقات", "البطاقات", "التحية والطقس", "الأزرار", "الإضاءة", "تعتيم الليل",
        "بطاقة شاشة التوقف", "حجم الساعة", "تعتيم الصورة"};
    /** each look's values (null: the person's own); «شكلك المعتاد» is the person's own, these are its values when nothing was kept */
    static final String[][] PRESET_VALUES = {
        {"classic", "photos", "plain", "tiles", null, null, null, null, null, null, null, null},
        {"couch", "scenes", "glass", "icons", null, null, null, "dark", null, "none", null, null},
        {"classic", null, "glass", null, "none", "false", CLEAN_HIDES_BUTTONS ? "hidden" : null, null, null, "none", null, null},
        {"classic", null, "glass", null, "prayerrow", null, null, null, null, "prayers", null, null},
        {"football", null, "plain", null, "livenow,nextmatches,minitable", null, null, "dark", null, "matchday", null, null},
        {"classic", null, "plain", null, "nextprayer", "false", null, "dark", "true", "none", "large", "40"}};
    /** «صورة فقط» (the owner): the sources, the tasks, the recitation, the markets, the games, the phone and the search hidden; the control centre (the way
     *  to the settings) and «تحديث جديد» (only while an update is out) stay, and the remote's own keys keep working. pillsHidden|listenPill|tasksPill|searchPill */
    static final String BUTTONS_HIDDEN = "markets,matches,phone,inputs|false|false|false";
    String presetField(String f) {
        switch (f) {
            case "layout": return homeLayout();
            case "bg": return homeBg();
            case "clock": return homeClock();
            case "apps": return appsStyle();
            case "cards": return homePanel();
            case "greeting": return String.valueOf(greeting());
            case "buttons": return pillsHidden() + "|" + listenPill() + "|" + tasksPill() + "|" + searchPill();
            case "appearance": return appearance();
            case "nightDim": return String.valueOf(nightDim());
            case "saverPanel": return panel();
            case "clockSize": return clockSize();
            case "wallDim": return String.valueOf(wallDim());
        }
        return "";
    }
    void setPresetField(String f, String v) {
        if (v == null) return;
        switch (f) {
            case "layout": setHomeLayout(v); break;
            case "bg": setHomeBg(v); break;
            case "clock": setHomeClock(v); break;
            case "apps": setAppsStyle(v); break;
            case "cards": setHomePanels(java.util.Arrays.asList(v.split(","))); break;
            case "greeting": setGreeting(Boolean.parseBoolean(v)); break;
            case "buttons": {
                String[] b = ("hidden".equals(v) ? BUTTONS_HIDDEN : v).split("\\|", -1);
                if (b.length != 4) return;
                setPillsHidden(b[0]); setListenPill(Boolean.parseBoolean(b[1])); setTasksPill(Boolean.parseBoolean(b[2])); setSearchPill(Boolean.parseBoolean(b[3]));
                break;
            }
            case "appearance": setAppearance(v); break;
            case "nightDim": setNightDim(Boolean.parseBoolean(v)); break;
            case "saverPanel": setPanel(v); break;
            case "clockSize": setClockSize(v); break;
            case "wallDim": try { setWallDim(Integer.parseInt(v)); } catch (NumberFormatException e) { } break;
        }
    }
    private String presetNow() {
        StringBuilder b = new StringBuilder();
        for (String f : PRESET_FIELDS) b.append(b.length() == 0 ? "" : "\n").append(f).append('=').append(presetField(f));
        return b.toString();
    }
    private void presetPut(String saved) {
        for (String line : saved.split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0) setPresetField(line.substring(0, eq), line.substring(eq + 1));
        }
    }
    /**
     * 4.4 the owner: «تركيبات مقترحة», one tile among the six: four mixes, each a look and one or two rows changed; choosing one applies it all, and
     * it reads as itself (its name) until a row is changed after (then «مخصّص»). key, name, its line, the look, then field=value pairs.
     */
    static final String[][] COMBOS = {
        {"couch-prayers", "كنبة + مواقيت اليوم", "الآفاق، ومربعات المواقيت في الصف", "couch", "cards=prayerrow"},
        {"clean-icons", "صورة فقط + أيقونات التطبيقات", "الصورة والساعة، والتطبيقات أيقونات", "clean", "apps=icons"},
        {"sport-night", "مباريات اليوم + ليل هادئ", "المباريات في الليل، خافتة", "sport", "nightDim=true", "wallDim=40"},
        {"tv-glass", "شكلك المعتاد + الساعة الزجاجية", "اختياراتك، والساعة خلف الصورة", "tv", "clock=glass"}};
    static String[] combo(String key) { for (String[] c : COMBOS) if (c[0].equals(key)) return c; return null; }
    /** the mix chosen, "" when a look alone (or none) is */
    String presetCombo() { String v = sp.getString("presetCombo", ""); return combo(v) == null ? "" : v; }
    void setCombo(String key) {
        String[] c = combo(key);
        if (c == null) return;
        synchronized (LOOK_LOCK) { rememberLook(); comboNow(c, key); }
        lookChanged();
    }
    private void comboNow(String[] c, String key) {
        writePreset(c[3]);
        if ("tv".equals(c[3])) sp.edit().putString("presetBefore", presetNow()).apply();   // the person's own, kept: «شكلك المعتاد» alone puts them back
        for (int k = 4; k < c.length; k++) { int eq = c[k].indexOf('='); setPresetField(c[k].substring(0, eq), c[k].substring(eq + 1)); }
        sp.edit().putString("preset", c[3]).putString("presetCombo", key).putString("presetApplied", presetNow()).apply();
    }

    /** the look chosen ("tv" when none); a TV that had «شكل كنبة» on reads «كنبة», its four values kept as the person's own */
    String preset() {
        if (!sp.contains("preset") && sp.getBoolean("couchOn", false)) {
            String[] b = sp.getString("couchBefore", "").split(",");
            String before = b.length == 4 ? "layout=" + b[0] + "\nbg=" + b[1] + "\nclock=" + b[2] + "\napps=" + b[3] : "";
            sp.edit().putString("preset", "couch").putString("presetBefore", before).putString("presetApplied", presetNow()).apply();
        }
        String v = sp.getString("preset", "tv");
        return java.util.Arrays.asList(PRESETS).contains(v) ? v : "tv";
    }
    void setPreset(String key) { setPreset(key, false); }
    /**
     * 4.4 beta 2 (the owner, docs/mockups/full-look 06): a full-design look with the cards fitted into it (noCards false: each card that does not
     * fit swapped for its nearest by CardFit, as always), or the pure design without cards (noCards true): the cards hidden, not lost, their list kept
     * in «homePanelHidden» for «رجّع بطاقاتك»; the look is then as chosen (not «مخصّص»), and «شكلك المعتاد» puts the person's own cards back.
     */
    void setPreset(String key, boolean noCards) {
        if (combo(key) != null) { setCombo(key); return; }   // saver.sh preset takes a mix's key too
        if (!java.util.Arrays.asList(PRESETS).contains(key)) return;
        synchronized (LOOK_LOCK) {
            rememberLook();
            List<String> mine = homePanels();
            writePreset(key);
            if (noCards && !mine.isEmpty()) {
                sp.edit().putString("homePanelHidden", join(mine, ",")).remove("homePanelMine").apply();
                setHomePanels(new ArrayList<String>());
                sp.edit().putString("presetApplied", presetNow()).apply();
            } else if (!noCards) sp.edit().remove("homePanelHidden").apply();
        }
        lookChanged();
    }
    /** the cards hidden by a design «بدون بطاقات», or none */
    List<String> hiddenCards() { return sp.contains("homePanelHidden") ? parseHomePanels(sp.getString("homePanelHidden", "none")) : new ArrayList<String>(); }
    /** «رجّع بطاقاتك»: the hidden cards back, fitted to the look as any card is */
    void restoreHiddenCards() {
        List<String> h = hiddenCards();
        sp.edit().remove("homePanelHidden").apply();
        if (h.isEmpty()) return;
        synchronized (LOOK_LOCK) { setHomePanels(h); fitHomeCards(); }
        lookChanged();
    }
    /** a full-design look («كنبة», «tvOS»): it asks about the cards and changes the settings screen too */
    static boolean fullDesign(String key) { return FULL_DESIGN_STYLE.containsKey(key); }
    /** the look written, every field of it, under LOOK_LOCK (the caller's): the home never reads it half done */
    private void writePreset(String key) {
        int i = java.util.Arrays.asList(PRESETS).indexOf(key);
        if (i < 0) return;
        sp.edit().remove("presetCombo").apply();
        String before = sp.getString("presetBefore", null);
        if ("tv".equals(key)) {
            if (before != null) presetPut(before);
            else for (int k = 0; k < PRESET_FIELDS.length; k++) setPresetField(PRESET_FIELDS[k], PRESET_VALUES[0][k]);
            sp.edit().remove("preset").remove("presetBefore").remove("presetApplied").remove("presetCombo").putBoolean("couchOn", false).remove("couchBefore").remove("homePanelHidden").apply();
            String sb = sp.getString("settingsStyleBefore", null);   // the settings screen the person had before a full-design look
            if (sb != null) sp.edit().putString("settingsStyle", sb).remove("settingsStyleBefore").apply();
            return;
        }
        if (before == null) { before = presetNow(); sp.edit().putString("presetBefore", before).apply(); }   // the person's own, kept once
        else presetPut(before);                                                                                // back to them, then the look
        for (int k = 0; k < PRESET_FIELDS.length; k++) setPresetField(PRESET_FIELDS[k], PRESET_VALUES[i][k]);
        sp.edit().putString("preset", key).putString("presetApplied", presetNow()).putBoolean("couchOn", "couch".equals(key)).remove("couchBefore").apply();
        String full = FULL_DESIGN_STYLE.get(key);   // the owner (4.4 beta 2): a full-design look changes the settings screen too; the other looks never do
        if (full != null && !full.equals(settingsStyle())) {
            if (!sp.contains("settingsStyleBefore")) sp.edit().putString("settingsStyleBefore", settingsStyle()).apply();   // kept once, for «شكلك المعتاد»
            sp.edit().putString("settingsStyle", full).apply();
        }
    }
    /** the looks that are a whole design, with the settings screen of that design («tvOS» joins once its look and screen are approved and built) */
    static final java.util.Map<String, String> FULL_DESIGN_STYLE = java.util.Collections.singletonMap("couch", "couch");
    /** the rows changed since the look was chosen (their Arabic names), none while it is as chosen */
    List<String> presetChanged() {
        List<String> out = new ArrayList<>();
        if ("tv".equals(preset()) && presetCombo().isEmpty()) return out;
        for (String line : sp.getString("presetApplied", "").split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            int k = java.util.Arrays.asList(PRESET_FIELDS).indexOf(line.substring(0, eq));
            if (k >= 0 && !line.substring(eq + 1).equals(presetField(PRESET_FIELDS[k]))) out.add(PRESET_FIELD_NAMES[k]);
        }
        return out;
    }
    static String presetName(String key) { int i = java.util.Arrays.asList(PRESETS).indexOf(key); return PRESET_NAMES[Math.max(0, i)]; }
    /** the row's line: the look, or «مخصّص (كنبة، وتغيّر: الساعة)», or «…، وتغيّر أكثر من صف» */
    String presetLabel() {
        String key = preset(), mix = presetCombo();
        String name = mix.isEmpty() ? presetName(key) : combo(mix)[1];
        List<String> ch = presetChanged();
        if (ch.isEmpty()) return name;
        return ch.size() == 1 ? String.format(java.util.Locale.ROOT, "مخصّص (%s، وتغيّر: %s)", name, ch.get(0)) : String.format(java.util.Locale.ROOT, "مخصّص (%s، وتغيّر أكثر من صف)", name);
    }
    // ---- 4.5 (docs/ui/PROPOSAL.md, approved): one way a look is written, the look before it kept, the home told at once

    /** held while a look is written (setPreset, setCombo, setLookPart, restorePreviousLook) and while ConfigProvider reads it: never half a look */
    static final Object LOOK_LOCK = new Object();
    /** the home's own action: the saved look changed, read it now (not at the next minute); sent only to sa.urtv.home, under LOOK_PERMISSION */
    static final String LOOK_CHANGED = "sa.urtv.home.LOOK_CHANGED";
    static final String LOOK_PERMISSION = "sa.urtv.home.LOOK";
    private static final String[] PREV_KEYS = {"preset", "presetCombo", "presetBefore", "presetApplied"};

    /** the look as it is now, whole: its fields and which look and mix it was (a line each; the stored strings with their new lines escaped) */
    private String lookWhole() {
        StringBuilder b = new StringBuilder(presetNow());
        for (String k : PREV_KEYS) b.append("\n#").append(k).append('=').append(sp.contains(k) ? sp.getString(k, "").replace("\\", "\\\\").replace("\n", "\\n") : "\u0000");
        return b.toString();
    }
    private void lookPut(String whole) {
        SharedPreferences.Editor e = sp.edit();
        StringBuilder fields = new StringBuilder();
        for (String line : whole.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            if (line.startsWith("#")) {
                String k = line.substring(1, eq), v = line.substring(eq + 1);
                if ("\u0000".equals(v)) e.remove(k); else e.putString(k, unesc(v));
            } else fields.append(line).append('\n');
        }
        presetPut(fields.toString());
        e.putBoolean("couchOn", whole.contains("\n#preset=couch\n")).remove("couchBefore").apply();
    }
    private static String unesc(String v) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '\\' && i + 1 < v.length()) { char d = v.charAt(++i); b.append(d == 'n' ? '\n' : d); } else b.append(c);
        }
        return b.toString();
    }
    /** keeps the look as it is as «the look before» (with the time), once per change; «رجّع الشكل السابق» puts it back */
    void rememberLook() {
        sp.edit().putString("lookPrev", lookWhole()).putLong("lookPrevAt", System.currentTimeMillis()).apply();
    }
    /** there is a look before this one, and it is not this one */
    boolean hasPreviousLook() { String v = sp.getString("lookPrev", null); return v != null && !v.equals(lookWhole()); }
    /** its name for the row: the look's or the mix's, «مخصّص» when it was changed after */
    String previousLookName() {
        String v = sp.getString("lookPrev", "");
        String key = "tv", mix = "";
        for (String line : v.split("\n")) {
            if (line.startsWith("#preset=") && !line.endsWith("\u0000")) key = line.substring(8);
            if (line.startsWith("#presetCombo=") && !line.endsWith("\u0000")) mix = line.substring(13);
        }
        return combo(mix) != null ? combo(mix)[1] : presetName(java.util.Arrays.asList(PRESETS).contains(key) ? key : "tv");
    }
    long previousLookAt() { return sp.getLong("lookPrevAt", 0); }
    /** «رجّع الشكل السابق»: the look before put back whole, and this one kept as the look before (pressed again, it comes back) */
    void restorePreviousLook() {
        synchronized (LOOK_LOCK) {
            String prev = sp.getString("lookPrev", null);
            if (prev == null) return;
            String now = lookWhole();
            lookPut(prev);
            sp.edit().putString("lookPrev", now).putLong("lookPrevAt", System.currentTimeMillis()).apply();
        }
        lookChanged();
    }
    /** one part of the look (layout, bg, clock, apps: PRESET_FIELDS) changed by a row or the panel: the look before kept, the home told */
    void setLookPart(String field, String value) {
        synchronized (LOOK_LOCK) { rememberLook(); setPresetField(field, value); }
        lookChanged();
    }
    /** the home's part of a look, as choosing it would make it, without saving anything (LookPanelActivity's preview): layout, bg, clock, apps,
     *  appearance, cards («شكلك المعتاد» is the person's own values, kept or as they are) */
    String[] lookOf(String key) {
        java.util.Map<String, String> v = new java.util.HashMap<>();
        for (String f : PRESET_FIELDS) v.put(f, presetField(f));
        String before = sp.getString("presetBefore", null);
        if (before != null) for (String line : before.split("\n")) { int eq = line.indexOf('='); if (eq > 0) v.put(line.substring(0, eq), line.substring(eq + 1)); }
        int i = java.util.Arrays.asList(PRESETS).indexOf(key);
        if (i > 0 || (i == 0 && before == null)) for (int k = 0; k < PRESET_FIELDS.length; k++) if (PRESET_VALUES[Math.max(0, i)][k] != null) v.put(PRESET_FIELDS[k], PRESET_VALUES[Math.max(0, i)][k]);
        return new String[] {v.get("layout"), v.get("bg"), v.get("clock"), v.get("apps"), v.get("appearance"), v.get("cards")};
    }

    /** the home reads the look now (its LOOK_CHANGED receiver); an older home, or none, simply reads it at its next minute as before */
    void lookChanged() {
        if (ctx == null || !uiSmooth()) return;   // 4.4 beta 2: only with «تبديل الشكل بلا وميض» on; off, the home reads at its next return or minute, as in 4.4
        try { ctx.sendBroadcast(new android.content.Intent(LOOK_CHANGED).setPackage("sa.urtv.home"), LOOK_PERMISSION); } catch (RuntimeException e) { }
    }

    /** saver.sh show: "family" or "family (custom)" */
    String presetShow() { return (presetCombo().isEmpty() ? preset() : presetCombo()) + (presetChanged().isEmpty() ? "" : " (custom)"); }

    /**
     * 4.4 the owner: «شكل كنبة» was one switch. With PRESETS_REPLACE_COUCH it is the look «كنبة»: on chooses it, off chooses «شكلك المعتاد».
     * Without it, the switch as before: on, the Couch preset at once and the person's own four values kept; off, those put back exactly.
     */
    boolean couchOn() {
        if (PRESETS_REPLACE_COUCH) return "couch".equals(preset());
        return sp.contains("couchOn") ? sp.getBoolean("couchOn", false) : "couch".equals(look3());
    }
    void setCouch(boolean on) {
        if (PRESETS_REPLACE_COUCH) { if (on != couchOn()) setPreset(on ? "couch" : "tv"); return; }
        if (on == couchOn() && sp.contains("couchOn")) return;
        if (on) {
            sp.edit().putString("couchBefore", homeLayout() + "," + homeBg() + "," + homeClock() + "," + appsStyle()).putBoolean("couchOn", true).apply();
            setLook3("couch");
            return;
        }
        String[] b = sp.getString("couchBefore", "").split(",");
        if (b.length == 4) { setHomeLayout(b[0]); setHomeBg(b[1]); setHomeClock(b[2]); setAppsStyle(b[3]); } else setLook3("urtv");
        sp.edit().putBoolean("couchOn", false).remove("couchBefore").apply();
    }
    /** the switch's line: off, on as the preset, or on with a row changed since («مخصّص») */
    String couchLabel() {
        if (!couchOn()) return "متوقف";
        return "couch".equals(look3()) ? "مفعّل (التخطيط كنبة، الخلفية الآفاق، الساعة زجاجية، التطبيقات أيقونات؛ إيقافه يرجّع اختياراتك)"
            : "مفعّل (مخصّص: غيّرت صفًا من الأربعة؛ إيقافه يرجّع اختياراتك قبل التشغيل)";
    }
    static String look3Label(String key) {
        for (String[] l : LOOKS) if (l[0].equals(key)) return l[1];
        return "مخصّص";
    }
    /** 4.3.3 «حزم الآفاق» (the scene home): «آفاق السعودية» is in the app, «آفاق عالمية» is downloaded when chosen; none chosen is the Saudi pack */
    static final String[][] SCENE_PACKS = {{"saudi", "آفاق السعودية"}, {"couch", "آفاق عالمية"}};
    List<String> scenePacks() {
        List<String> out = new ArrayList<>();
        for (String s : list("scenePacks")) for (String[] k : SCENE_PACKS) if (k[0].equals(s) && !out.contains(s)) out.add(s);
        if (out.isEmpty()) out.add("saudi");
        return out;
    }
    void setScenePacks(List<String> v) { setList("scenePacks", v); }
    /** «بقاء كل أفق» in seconds (20 the default); «الأفق يتبع الوقت» (the night scenes from Maghrib to Fajr) and «الأفق يتبع الطقس» (the fog scenes in fog): off by default */
    static final String[] SCENE_STAYS = {"10", "20", "60", "300"};
    static final String[] SCENE_STAY_NAMES = {"١٠ ثوانٍ", "٢٠ ثانية", "دقيقة واحدة", "خمس دقائق"};
    String sceneStay() { String v = sp.getString("sceneStay", "20"); return java.util.Arrays.asList(SCENE_STAYS).contains(v) ? v : "20"; }
    void setSceneStay(String v) { if (java.util.Arrays.asList(SCENE_STAYS).contains(v)) sp.edit().putString("sceneStay", v).apply(); }
    static String sceneStayLabel(String v) { int i = java.util.Arrays.asList(SCENE_STAYS).indexOf(v); return SCENE_STAY_NAMES[Math.max(0, i)]; }
    boolean sceneByTime() { return sp.getBoolean("sceneByTime", false); }
    void setSceneByTime(boolean on) { sp.edit().putBoolean("sceneByTime", on).apply(); }
    boolean sceneByWeather() { return sp.getBoolean("sceneByWeather", false); }
    void setSceneByWeather(boolean on) { sp.edit().putBoolean("sceneByWeather", on).apply(); }
    /** «تطبيقات في الشريط»: how many favourites the home's tray shows whole at once, 5 (the default, as always) to 10, or "all" (up to 10, never a scroll) */
    static final String[] FAV_BARS = {"5", "6", "7", "8", "9", "10", "all"};
    /** 9 and 10 were taken as 8 for a while (step 28); the owner wants them whole again (4.3.1), so a saved word stays as it is */
    static String favBarFix(String v) { return v; }
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
