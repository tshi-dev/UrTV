package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

/**
 * One read of the screensaver's ConfigProvider (same signing key) for everything the home screen's minute tick needs: the
 * city's zone, the greeting, the bar before the adhan, the prayer times' place. Each of those used to ask the provider (a
 * cross-process query) for itself, eight times a minute.
 */
final class ConfigSnapshot {
    static final Uri URI = Uri.parse("content://sa.urtv.screensaver.config");

    /** "city=..&lat=..&lon=..&tz=..&..." as the page reads it; null when the screensaver is missing or does not answer */
    final String fragment;
    /** an older screensaver does not say: on, the default */
    final boolean greetingOn, adhanBarOn, listenPillOn, updateBellOn;
    /** «أكمل المشاهدة» under the favourites; an older screensaver does not say: on (the row still needs the TV listings permission) */
    final boolean watchNextOn;
    /** «تطبيقات في الشريط»: "5" to "10" or "all"; an older screensaver does not say: 5, as the tray always was */
    String favBar = "5";
    String pillsHidden = "";   // the quick buttons hidden in the settings (#52): markets, matches, center
    /** 4.3.3 «الواجهة» (classic, football, scenes) and «حزم الآفاق» ("saudi", "couch" or both); an older screensaver says nothing: classic, saudi */
    String homeLayout = "classic", scenePacks = "saudi", sceneStay = "20";
    /** 4.4 «الخلفية» (photos, scenes) and «الساعة» (plain, glass): an older screensaver says nothing: photos and plain, the home as it was */
    String homeBg = "photos", homeClock = "plain", appsStyle = "frame";
    boolean sceneByTime, sceneByWeather;   // «الأفق يتبع الوقت», «الأفق يتبع الطقس»: off unless the screensaver says on
    /** «أزرار الريموت»: "youtube=PKG,netflix=,prime=" from the screensaver's settings; null = an older screensaver said nothing */
    String keyApps;
    /** PR #45: the name after the greeting; "" (also an older screensaver, which says nothing) is the greeting alone */
    String greetingName = "";
    boolean cityChosen = true;   // 4.3.1: a city was ever set (an older screensaver does not say: yes, so nobody is asked)
    String matchApp = "auto";   // 4.3 «تطبيق المباريات»: auto (UrTV Player when installed), none, or a package
    boolean pillHidden(String key) { return ("," + pillsHidden + ",").contains("," + key + ","); }
    String tweaks = "";   // «اللون والتحكم» (GlassStyle.setTweaks); an older screensaver says nothing: the look as it is
    boolean pagesDark = true;   // «صفحات الأسهم والمباريات داكنة»: an older screensaver says nothing: on, the default
    /** «المظهر»: auto, light or dark (Look.mode); an older screensaver does not say: dark, as the home always was */
    private final String appearance;
    private final String look;   // «المظهر ← الشكل» (GlassStyle.look): an older screensaver says nothing: glass
    /** an old copy, handed out because a read failed: fine to show, never to be taken for an answer (see placeFragment) */
    final boolean stale;
    /** taken before the read that made this began (PlaceTicket): a cached or old copy keeps it, so it never beats a newer read as the place */
    private final long placeTicket;

    private ConfigSnapshot(String fragment, boolean greetingOn, boolean adhanBarOn, boolean listenPillOn, boolean updateBellOn, String appearance,
            boolean watchNextOn, String look, boolean stale, long placeTicket) {
        this.stale = stale;
        this.placeTicket = placeTicket;
        this.look = GlassStyle.look(look);
        this.watchNextOn = watchNextOn;
        this.fragment = fragment;
        this.greetingOn = greetingOn;
        this.adhanBarOn = adhanBarOn;
        this.listenPillOn = listenPillOn;
        this.updateBellOn = updateBellOn;
        this.appearance = Look.mode(appearance);
    }

    /** The same values with another fragment (the one kept on disk). */
    private ConfigSnapshot withFragment(String f) { ConfigSnapshot c = new ConfigSnapshot(f, greetingOn, adhanBarOn, listenPillOn, updateBellOn, appearance, watchNextOn, look, stale, placeTicket); c.favBar = favBar; c.tweaks = tweaks; c.pagesDark = pagesDark; c.pillsHidden = pillsHidden; c.matchApp = matchApp; c.cityChosen = cityChosen; c.homeLayout = homeLayout; c.scenePacks = scenePacks; c.sceneStay = sceneStay; c.sceneByTime = sceneByTime; c.sceneByWeather = sceneByWeather; c.homeBg = homeBg; c.homeClock = homeClock; c.appsStyle = appsStyle; c.greetingName = greetingName; return c; }

    private static volatile String savedFragment;   // what this process last wrote (or read) of the kept fragment

    /** 4.3.4 follow-up: the fragment as this process last knew it, never a query (a key on the UI thread: Back): memory, else the copy on disk */
    static String lastFragment(Context c) { String f = savedFragment; return f != null ? f : keptFragment(c); }

    private static String keptFragment(Context c) {
        try { return savedFragment = c.getSharedPreferences("config_last", Context.MODE_PRIVATE).getString("fragment", null); } catch (RuntimeException e) { return null; }
    }

    private static void keep(Context c, String fresh) {
        if (!FragmentKeep.shouldSave(fresh, savedFragment)) return;
        savedFragment = fresh;
        try { c.getSharedPreferences("config_last", Context.MODE_PRIVATE).edit().putString("fragment", fresh).apply(); } catch (RuntimeException ignored) { }
    }

    /** The same values, marked as an old copy. */
    ConfigSnapshot asStale() { ConfigSnapshot c = new ConfigSnapshot(fragment, greetingOn, adhanBarOn, listenPillOn, updateBellOn, appearance, watchNextOn, look, true, placeTicket); c.favBar = favBar; c.tweaks = tweaks; c.pagesDark = pagesDark; c.pillsHidden = pillsHidden; c.matchApp = matchApp; c.cityChosen = cityChosen; c.homeLayout = homeLayout; c.scenePacks = scenePacks; c.sceneStay = sceneStay; c.sceneByTime = sceneByTime; c.sceneByWeather = sceneByWeather; c.homeBg = homeBg; c.homeClock = homeClock; c.appsStyle = appsStyle; c.keyApps = keyApps; c.greetingName = greetingName; return c; }

    /**
     * 4.5 «المظهر»'s preview (LookPreview): the same values with the look's parts a choice under the focus would give, never saved: the layout,
     * the background, the clock, the apps' way and the light (null: the saved one). A copy marked as the answer it was made from (not stale).
     */
    ConfigSnapshot withLook(String layout, String bg, String clock, String apps, String light) {
        ConfigSnapshot c = new ConfigSnapshot(fragment, greetingOn, adhanBarOn, listenPillOn, updateBellOn, light != null ? light : appearance, watchNextOn, look, stale, placeTicket); c.favBar = favBar; c.tweaks = tweaks; c.pagesDark = pagesDark; c.pillsHidden = pillsHidden; c.matchApp = matchApp; c.cityChosen = cityChosen; c.homeLayout = homeLayout; c.scenePacks = scenePacks; c.sceneStay = sceneStay; c.sceneByTime = sceneByTime; c.sceneByWeather = sceneByWeather; c.homeBg = homeBg; c.homeClock = homeClock; c.appsStyle = appsStyle; c.keyApps = keyApps; c.greetingName = greetingName;
        if (layout != null) c.homeLayout = layout;
        if (bg != null) c.homeBg = bg;
        if (clock != null) c.homeClock = clock;
        if (apps != null) c.appsStyle = apps;
        return c;
    }


    /**
     * The settings fragment for the PRAYER PLACE (Place, the adhan alarm, the day parts): null when this is an old copy, so that only a real
     * answer of the screensaver can become the remembered place or move the alarm. Display values (zone, digits, clock) may use the old copy.
     */
    String placeFragment() { return stale ? null : fragment; }

    /** The ticket to give with placeFragment() (Place, DayParts.now, AdhanSync.onConfig). */
    long placeTicket() { return placeTicket; }

    String appearance() { return appearance; }
    String look() { return look; }

    /**
     * One home-screen return reads this four times (the clock, the row of pills, the cards, the digits), each a cross-process query that starts
     * the screensaver's process again when the TV's memory manager has killed it. Reads within this many ms share the one answer; a setting
     * saved a moment ago reaches the screen at the next tick or return, as before.
     */
    static final long SHARE_MS = 3000;

    private static final LastGood<ConfigSnapshot> LAST = new LastGood<>();

    /** 4.5: a look was just changed (Prefs.LOOK_CHANGED): the next read asks the screensaver again, not the 3-second copy */
    static void expire() { LAST.expire(); }

    /** 4.3.4 QA-1: the last answer this process had, never a query (the UI thread's first draw); null when it has none yet */
    static ConfigSnapshot known() { return LAST.peek(); }

    /**
     * 4.4 QA-11 (the TCL freeze's shape): what a screen's onCreate, a return or a key on the UI thread asks for its look, digits and buttons. Never a
     * query: the answer this process already has (known), else the last real answer kept on disk (marked old); a read on a thread of its own then
     * brings the fresh one (and the digits and language), which the next draw, return or tick shows, as a setting saved a moment ago always did.
     */
    static ConfigSnapshot ui(Context c) {
        ConfigSnapshot k = LAST.peek();
        if (k == null) k = kept(c);
        refreshSoon(c);
        return k;
    }

    private static final java.util.concurrent.atomic.AtomicBoolean asking = new java.util.concurrent.atomic.AtomicBoolean();
    private static volatile long askedAt = -SHARE_MS;

    /** one read at a time, off the UI thread, not more than once in SHARE_MS */
    private static void refreshSoon(Context c) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - askedAt < SHARE_MS || !asking.compareAndSet(false, true)) return;
        askedAt = now;
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                try { Digits.remember(app, read(app)); } catch (RuntimeException ignored) { } finally { asking.set(false); }
            }
        }, "config-ui").start();
    }

    /** the last real answer kept on disk (its fragment and the look, appearance and buttons), marked old; the defaults when there is none */
    private static ConfigSnapshot kept(Context c) {
        SharedPreferences p = c.getApplicationContext().getSharedPreferences("config_last", Context.MODE_PRIVATE);
        String f = lastFragment(c);
        ConfigSnapshot s = new ConfigSnapshot(f, !"0".equals(p.getString("greeting", "1")), !"0".equals(p.getString("adhan_bar", "1")),
                !"0".equals(p.getString("listen_pill", "1")), !"0".equals(p.getString("update_bell", "1")), p.getString("appearance", null),
                !"0".equals(p.getString("watch_next", "1")), p.getString("look", null), true, 0);
        s.tweaks = p.getString("tweaks", "");
        s.pagesDark = !"0".equals(p.getString("pages_dark", "1"));
        s.matchApp = p.getString("match_app", "auto");
        // 4.4 review: the rest of what the first draw reads (a cold start must not flash the defaults and then rebuild)
        s.favBar = p.getString("fav_bar", "5");
        s.pillsHidden = p.getString("pills_hidden", "");
        s.homeLayout = p.getString("home_layout", "classic");
        s.scenePacks = p.getString("scene_packs", "saudi");
        s.sceneStay = p.getString("scene_stay", "20");
        s.sceneByTime = "1".equals(p.getString("scene_time", "0"));
        s.sceneByWeather = "1".equals(p.getString("scene_weather", "0"));
        s.homeBg = p.getString("home_bg", "photos");
        s.homeClock = p.getString("home_clock", "plain");
        s.appsStyle = p.getString("apps_style", "frame");
        s.greetingName = p.getString("greeting_name", "");
        s.cityChosen = !"0".equals(p.getString("city_chosen", "1"));
        GlassStyle.setTweaks(s.tweaks);
        return s;
    }

    /** the values a screen's first draw needs, kept beside the fragment when they change (a real answer only) */
    private static void keepValues(Context c, ConfigSnapshot s) {
        String sig = s.appearance + "|" + s.look + "|" + s.tweaks + "|" + s.pagesDark + "|" + s.matchApp + "|" + s.greetingOn + s.adhanBarOn + s.listenPillOn + s.updateBellOn + s.watchNextOn
                + "|" + s.favBar + "|" + s.pillsHidden + "|" + s.homeLayout + "|" + s.scenePacks + "|" + s.sceneStay + "|" + s.sceneByTime + s.sceneByWeather
                + "|" + s.homeBg + "|" + s.homeClock + "|" + s.appsStyle + "|" + s.greetingName + "|" + s.cityChosen;
        if (sig.equals(keptSig)) return;
        keptSig = sig;
        try {
            c.getApplicationContext().getSharedPreferences("config_last", Context.MODE_PRIVATE).edit()
                    .putString("appearance", s.appearance).putString("look", s.look).putString("tweaks", s.tweaks)
                    .putString("pages_dark", s.pagesDark ? "1" : "0").putString("match_app", s.matchApp)
                    .putString("greeting", s.greetingOn ? "1" : "0").putString("adhan_bar", s.adhanBarOn ? "1" : "0")
                    .putString("listen_pill", s.listenPillOn ? "1" : "0").putString("update_bell", s.updateBellOn ? "1" : "0")
                    .putString("watch_next", s.watchNextOn ? "1" : "0")
                    .putString("fav_bar", s.favBar).putString("pills_hidden", s.pillsHidden).putString("home_layout", s.homeLayout)
                    .putString("scene_packs", s.scenePacks).putString("scene_stay", s.sceneStay)
                    .putString("scene_time", s.sceneByTime ? "1" : "0").putString("scene_weather", s.sceneByWeather ? "1" : "0")
                    .putString("home_bg", s.homeBg).putString("home_clock", s.homeClock).putString("apps_style", s.appsStyle)
                    .putString("greeting_name", s.greetingName).putString("city_chosen", s.cityChosen ? "1" : "0").apply();
        } catch (RuntimeException ignored) { }
    }

    private static volatile String keptSig;

    static ConfigSnapshot read(final Context c) {
        final ConfigSnapshot[] none = {null};
        LastGood.Read<ConfigSnapshot> r = LAST.read(android.os.SystemClock.elapsedRealtime(), SHARE_MS, new LastGood.Query<ConfigSnapshot>() {
            @Override public ConfigSnapshot ask() {
                ConfigSnapshot s = query(c);
                if (s.fragment != null) return s;
                none[0] = s;   // the defaults, for when there is nothing older either
                return null;
            }
        });
        // no answer: the last good values stay on the screen (marked old), not the defaults; the next call asks again, so the real ones
        // return as soon as the screensaver answers; with no older copy at all, the defaults as before
        if (r.value == null) {
            ConfigSnapshot d = none[0] != null ? none[0] : query(c);
            // 4.3.2: nothing in memory either (the home just started while the screensaver app cannot answer): the last real answer kept on disk, marked old
            String kept = keptFragment(c);
            if (d.fragment == null && FragmentKeep.usedKept(null, kept)) return d.withFragment(kept).asStale();
            return d;
        }
        return r.stale ? r.value.asStale() : r.value;
    }

    private static ConfigSnapshot query(Context c) {
        long ticket = sa.urtv.home.tilawah.PlaceTicket.next();   // before the provider is asked
        String fragment = null;
        boolean greeting = true, bar = true, listenPill = true, updateBell = true, watchNext = true;
        String appearance = null, look = null, favBar = "5", tweaks = "";
        boolean pagesDark = true;
        String pillsHidden = "", matchApp = "auto", keyApps = null, greetingName = "";
        boolean cityChosen = true;
        String homeLayout = "classic", scenePacks = "saudi", sceneStay = "20";
        boolean sceneByTime = false, sceneByWeather = false;
        String homeBg = "photos", homeClock = "plain", appsStyle = "frame";
        try (Cursor cur = c.getContentResolver().query(URI, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                fragment = cur.getString(0);
                int g = cur.getColumnIndex("greeting");
                if (g >= 0 && "0".equals(cur.getString(g))) greeting = false;
                int b = cur.getColumnIndex("adhan_bar");
                if (b >= 0 && "0".equals(cur.getString(b))) bar = false;
                int l = cur.getColumnIndex("listen_pill");   // an older screensaver does not say: the button is shown, as it always was
                if (l >= 0 && "0".equals(cur.getString(l))) listenPill = false;
                int u = cur.getColumnIndex("update_bell");   // an older screensaver does not say: on, the default
                if (u >= 0 && "0".equals(cur.getString(u))) updateBell = false;
                int w = cur.getColumnIndex("watch_next");
                if (w >= 0 && "0".equals(cur.getString(w))) watchNext = false;
                int a = cur.getColumnIndex("appearance");
                if (a >= 0) appearance = cur.getString(a);
                int k = cur.getColumnIndex("look");
                if (k >= 0) look = cur.getString(k);
                int pd = cur.getColumnIndex("pages_dark");
                if (pd >= 0 && "0".equals(cur.getString(pd))) pagesDark = false;
                int ph = cur.getColumnIndex("pills_hidden");   // an older screensaver does not say: none hidden
                if (ph >= 0 && cur.getString(ph) != null) pillsHidden = cur.getString(ph);
                int tw = cur.getColumnIndex("look_tweaks");
                if (tw >= 0 && cur.getString(tw) != null) tweaks = cur.getString(tw);
                int ma = cur.getColumnIndex("match_app");   // an older screensaver does not say: auto
                if (ma >= 0 && cur.getString(ma) != null) matchApp = cur.getString(ma);
                int kk = cur.getColumnIndex("key_apps");   // «أزرار الريموت»; an older screensaver says nothing (null: the home's own stays)
                if (kk >= 0) keyApps = cur.getString(kk);
                int gn = cur.getColumnIndex("greeting_name");   // PR #45; an older screensaver says nothing: no name
                if (gn >= 0 && cur.getString(gn) != null) greetingName = cur.getString(gn);
                int pz = cur.getColumnIndex("page_zone");   // 4.3 (issue #55): the page's offset for the city's zone; an older screensaver says nothing
                if (pz >= 0 && cur.getString(pz) != null) ZoneFix.report(cur.getString(pz));
                int cc = cur.getColumnIndex("city_chosen");
                if (cc >= 0 && "0".equals(cur.getString(cc))) cityChosen = false;
                int li = cur.getColumnIndex("lite");   // A14 «الوضع الخفيف»; an older screensaver says nothing: auto, as before
                if (li >= 0) Ram.lite(c, cur.getString(li));
                int hl = cur.getColumnIndex("home_layout");
                if (hl >= 0 && cur.getString(hl) != null) homeLayout = cur.getString(hl);
                int sp = cur.getColumnIndex("scene_packs");
                if (sp >= 0 && cur.getString(sp) != null) scenePacks = cur.getString(sp);
                int ss = cur.getColumnIndex("scene_stay");
                if (ss >= 0 && cur.getString(ss) != null) sceneStay = cur.getString(ss);
                int st = cur.getColumnIndex("scene_time");
                sceneByTime = st >= 0 && "1".equals(cur.getString(st));
                int sw = cur.getColumnIndex("scene_weather");
                sceneByWeather = sw >= 0 && "1".equals(cur.getString(sw));
                int hb = cur.getColumnIndex("home_bg");
                if (hb >= 0 && "scenes".equals(cur.getString(hb))) homeBg = "scenes";
                int hc = cur.getColumnIndex("home_clock");
                if (hc >= 0 && "glass".equals(cur.getString(hc))) homeClock = "glass";
                int as = cur.getColumnIndex("apps_style");   // 4.4 «شكل التطبيقات»; an older screensaver says nothing: the ring
                if (as >= 0) { String v = cur.getString(as); if (java.util.Arrays.asList("zoom", "banners", "icons", "tiles").contains(v)) appsStyle = v; }   // 4.4: the three ways too (FavBar.look)
                if ("scenes".equals(homeLayout)) { homeLayout = "classic"; homeBg = "scenes"; homeClock = "glass"; }   // a 4.3.3 test build's screensaver
                int fb = cur.getColumnIndex("fav_bar");   // an older screensaver does not say: 5
                if (fb >= 0 && cur.getString(fb) != null) favBar = cur.getString(fb);
                int uc = cur.getColumnIndex("update_channel");   // 4.3.3 hotfix: kept for the UI thread (UpdateChannel.cached), read here off it
                if (uc >= 0) sa.urtv.home.phone.UpdateChannel.remember(cur.getString(uc));
            }
        } catch (RuntimeException ignored) {
            // the screensaver is missing or signed with another key: the defaults (Riyadh, on)
        }
        keep(c, fragment);   // a real answer: kept for a start when the screensaver cannot answer
        ConfigSnapshot made = new ConfigSnapshot(fragment, greeting, bar, listenPill, updateBell, appearance, watchNext, look, false, ticket);
        made.favBar = favBar;
        made.tweaks = tweaks;
        GlassStyle.setTweaks(tweaks);   // the glass of every home piece reads it (accent, focus style, bar strength, dimming)
        made.pagesDark = pagesDark;
        made.pillsHidden = pillsHidden;
        made.matchApp = matchApp;
        made.cityChosen = cityChosen;
        made.homeLayout = homeLayout;
        made.scenePacks = scenePacks;
        made.sceneStay = sceneStay;
        made.sceneByTime = sceneByTime;
        made.sceneByWeather = sceneByWeather;
        made.homeBg = homeBg;
        made.homeClock = homeClock;
        made.appsStyle = appsStyle;
        made.keyApps = keyApps;
        made.greetingName = greetingName;
        if (fragment != null) keepValues(c, made);   // a real answer: its look and buttons are what a cold first draw shows
        return made;
    }

    /** {city, zone id}; null when the screensaver gave nothing: the TV's own zone then */
    String[] cityZone() { return fragment == null ? null : ClockZone.parse(fragment); }

    /** «&clock=24» is in the fragment only while the setting is on; an older screensaver says nothing: the 12-hour clock */
    boolean clock24() { return fragment != null && (fragment.contains("&clock=24") || fragment.startsWith("clock=24")); }

    /** «اللغة»: «&lang=en» is in the fragment only for English; an older screensaver (or none) says nothing: ar, as always (Tr.fromFragment) */
    String lang() { return Tr.fromFragment(fragment); }

    /** «زر المهام»: «&taskspill=0» is in the fragment only while the button is hidden; an older screensaver (or none) says nothing: shown */
    boolean tasksPillOn() { return fragment == null || !(fragment.contains("&taskspill=0") || fragment.startsWith("taskspill=0")); }

    /**
     * «الرجوع يفتح شاشة التوقف» on the screensaver's settings: «&backsaver=1» only while on. null when there is no real answer (an old copy,
     * or no screensaver), so nothing is moved on a guess; Dock.followBackSaver moves the home's switch only when this one changes.
     */
    Boolean backSaver() { return stale || fragment == null ? null : (fragment.contains("&backsaver=1") || fragment.startsWith("backsaver=1")); }

    boolean latin() { return fragment != null && (fragment.contains("&digits=latin") || fragment.startsWith("digits=latin")); }
}
