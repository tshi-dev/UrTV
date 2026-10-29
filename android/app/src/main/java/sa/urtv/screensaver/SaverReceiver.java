package sa.urtv.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The screensaver's settings from ADB, for agents (people use SettingsActivity). Only the shell can send
 * these: the manifest requires android.permission.DUMP. optimize/saver.sh wraps them.
 *
 *   am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.GET
 *   am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.CITIES
 *   am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET
 *       [--es city 'الرس'] [--es lat 25.86 --es lon 43.49 --es tz Asia/Kuwait]   (tz is needed for a place that is not in the list) [--es teams '136013:Al-Hilal,150643:Al-Zulfi']
 *       [--es panel prayers] [--es panel2 hourly|none] [--es home_panel prayers,hourly,verse|none] [--es greeting on|off] [--es listenpill on|off] [--es updatebell on|off] [--es watchnext on|off] [--es appearance dark|light|auto] [--es look glass|graphite|navy|clean] [--es adhanbar on|off] [--es yearstrip on|off] [--es minrating 7]
 *       [--es sunrise on|off] [--es prayerbar on|off] [--es wxmore on|off] [--es months default|levant] [--es genres Drama,Comedy] [--es langs English]
 *       [--es photos 30] [--es idle 10] [--es digits hindi|latin] [--es hijrioffset -2..2] [--es clock24 on|off]
 *       [--es cardstyle current|dark1|glass1|dark3|glass3] [--es national auto|off]
 *       [--es markets tasi,gold,btc] [--es mkstyle arrows|prices] [--es mkbar on|off] [--es clocksize large|medium|small] [--es stocks 2222,1120,AAPL]
 *       [--es leagues 'Saudi-Arabian Pro League,NBA']
 *       [--es prayermethod jafari|gulf|auto]   («طريقة الحساب»: auto = the country's own, gulf = the Gulf method for any place, jafari = Shia Ithna-Ashari)
 *       [--es prayeradj on|off] [--es adj_fajr +2 --es adj_dhuhr 0 --es adj_asr 0 --es adj_maghrib -1 --es adj_isha 0]   («تعديل وقت الصلاة», minutes -20..20)
 *       [--es slot matches|weather|fasting|countdown|markets|none] [--es countdown 2026-12-01|clear --es cdname 'رحلة الصيف']
 *   am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.UPDATE
 *   am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.TASKS
 *       --es op list | add --es text 'نص' | done --ei n 1 | del --ei n 1 | clear
 *   am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.PHOTOS
 *       --es op list | add --ei size BYTES | add --ei size BYTES --es path /data/local/tmp/urtv-own/in.jpg | del --ei n 1 | clear | mode --es mode mix|only
 *
 * updatechannel = latest (the default) or next: where «تحديث تلفزيوني» (the TV's screen and the phone page) and the newest-version line read the
 * version and the APKs, the apk-latest or the apk-next release. For TESTING an update only, set by agents; no screen shows it, the settings export leaves
 * it out, and every signature, newer-version and permission check is the same.
 * leagues = whole leagues (Prefs.LEAGUES, up to Prefs.MAX_LEAGUES; the English or the Arabic name; "" = none): every game of every
 * club in them joins the followed teams' games.
 * PHOTOS = «صوري», the owner's own photos (Own, up to 10; optimize/photos.sh): "add" keeps the photo the shell just wrote
 * through ConfigProvider (.../own-in), or, with path, one in /data/local/tmp/urtv-own/ (only where the app may read it);
 * mode = mix (they join ours, as always) or only.
 * appearance = «المظهر ← الإضاءة» of the home screen: dark (the default), light, or auto (light from sunrise until Maghrib in the city).
 * look = «المظهر ← الشكل» of all our screens: glass (the default), graphite, navy or clean.
 * listenpill = the «تلاوة وأذان» button on the home screen (on, the default, or off: only the button goes, the adhan and the Quran stay).
 * watchnext = «أكمل المشاهدة» under the home screen's favourites (on, the default, or off); it needs the home app's TV listings permission.
 * updatebell = the «جرس التحديثات» on the home screen (on, the default, or off: only the bell goes; it shows only while the Play Store has updates waiting).
 * greeting = the greeting and the weather line above the home screen's clock (on, the default, or off: the plain good-morning).
 * yearstrip = the slim year strip under the screensaver's clock (on, or off, the default).
 * sunrise, prayerbar, wxmore = on or off (the default): the sunrise after Fajr in the prayer panel, the bar under it, the humidity and wind.
 * months = default («أكتوبر») or levant («تشرين الأول»), the Gregorian month's names on both screens.
 * slot = what the bottom-left slot shows (Prefs.SLOTS). countdown = the date (YYYY-MM-DD) slot=countdown counts down to, or
 * "clear"; cdname = its name (up to 40 characters).
 * national = auto (as always from now: the quiet green look on National Day, 21-25 September, and Founding Day, 20-24 February) or off.
 * cardstyle = the look of the card beside the clock (Prefs.CARD_STYLES; current is the plain card).
 * panel2 = «بطاقة ثانية», a second card beside the clock while it is small (SecondCard): one of the screensaver's cards but the
 * first one, or none (the default). Kept with any clock size; the page shows it only beside the small clock.
 * mkbar = the markets line at the top, on (default) or off (the weather stays; the list is kept); clocksize = the screensaver's clock.
 * markets = up to four of Prefs.MARKETS at the top left, in order; mkstyle = arrows only (as always) or the price and the day's change.
 * stocks = the watchlist of the stocks card (panel stocks): up to Prefs.MAX_STOCKS Tadawul codes (4 digits) or US tickers.
 * clock24 = «ساعة ٢٤»: every time of day as 24-hour (on) or 12-hour with ص and م (off, the default; ClockSetting).
 * digits = hindi (١٢٣, as always) or latin (123). photos = minutes between background photos (Prefs.PHOTO_MINUTES). idle = minutes before the TV starts the
 * screensaver (Prefs.IDLE_MINUTES): this is the system's screen_off_timeout, which a receiver (it runs as this
 * app, not as the shell) may write only when the app holds WRITE_SETTINGS; otherwise it says so and
 * optimize/saver.sh sets it from the shell instead. Nothing changes unless the extra is given.
 *
 * The answer is in the "data" of am's output. Unknown values are refused with a message, nothing half-saved.
 */
public class SaverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Prefs p = new Prefs(c);
        String action = String.valueOf(i.getAction());
        switch (action) {
            case "sa.urtv.screensaver.GET":
                setResultData(describe(c, p));
                return;
            case "sa.urtv.screensaver.CITIES": {
                StringBuilder b = new StringBuilder();
                for (Cities.Region r : Cities.REGIONS) {
                    b.append(r.name).append(": ");
                    for (String[] city : r.cities) b.append(city[0]).append("، ");
                    b.setLength(b.length() - 2);
                    b.append('\n');
                }
                setResultData(b.toString().trim());
                return;
            }
            case "sa.urtv.screensaver.UPDATE": {
                final PendingResult pending = goAsync();   // a download: off the main thread
                final Context app = c.getApplicationContext();
                new Thread(() -> {
                    pending.setResultData("update: " + Updater.check(app, true));
                    pending.finish();
                }).start();
                return;
            }
            case "sa.urtv.screensaver.SET":
                setResultData(set(c, p, i));
                return;
            case "sa.urtv.screensaver.TASKS":
                setResultData(tasks(p, i));
                return;
            case "sa.urtv.screensaver.PHOTOS":
                setResultData(photos(c, p, i));
                return;
            default:
                setResultData("error: unknown action " + action);
        }
    }

    /** The name is a built-in city, in full or as the short form that fits only one. */
    private static boolean inList(String name) {
        for (Cities.Region r : Cities.REGIONS) for (String[] x : r.cities) if (x[0].equals(name)) return true;
        return CityMatch.find(name).size() == 1;
    }

    /** The one place a setting is checked and saved: the ADB receiver and the launcher's phone control (ConfigProvider.call) both come here. */
    static String set(Context c, Prefs p, Intent i) {
        // check everything first, then save: a typo never leaves half the settings changed
        String[] city = null;
        String name = i.getStringExtra("city"), lat = i.getStringExtra("lat"), lon = i.getStringExtra("lon");
        String tz = i.getStringExtra("tz");   // only for a place of your own; a built-in city is Asia/Riyadh
        if (tz != null && (name == null || lat == null || lon == null))
            return "error: tz goes with city, lat and lon (a built-in city is Asia/Riyadh)";
        if (tz != null && !Arrays.asList(java.util.TimeZone.getAvailableIDs()).contains(tz))
            return "error: tz is a time zone like Asia/Kuwait or Africa/Cairo";
        if (name != null) {
            if (lat != null && lon != null) {
                if (!lat.matches("-?\\d{1,2}(\\.\\d+)?") || !lon.matches("-?\\d{1,3}(\\.\\d+)?"))
                    return "error: lat/lon must be numbers like 25.86 / 43.49";
                // a place of your own has its own time zone, and a missing one would be guessed (the TV's, Riyadh's): ask for it
                if (tz == null && !inList(name))
                    return "error: add the time zone for a place that is not in the list, e.g. --es tz Asia/Riyadh or Asia/Kuwait";
                if (tz == null) tz = CityMatch.zoneOf(name);   // a list name with coordinates of its own: its country's zone, not the TV's
                city = new String[] {name, lat, lon};
            } else {
                for (Cities.Region r : Cities.REGIONS) for (String[] x : r.cities) if (x[0].equals(name)) city = x;
                if (city == null) {   // a short form of a built-in name: «مكة» for «مكة المكرمة»
                    List<String[]> hits = CityMatch.find(name);
                    if (hits.size() == 1) city = hits.get(0);
                    else if (hits.size() > 1) {
                        List<String> names = new ArrayList<>();
                        for (String[] h : hits) names.add(h[0]);
                        return "error: «" + name + "» fits several cities: " + Prefs.join(names, "، ") + " — give one of them in full";
                    }
                }
                if (city == null) return "error: city not in the built-in list; give --es lat and --es lon too (CITIES lists them)";
            }
        }
        List<String> teams = null;
        String t = i.getStringExtra("teams");
        if (t != null) {
            teams = new ArrayList<>();
            for (String part : t.split(",")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                String[] kv = part.split(":", 2);
                if (!kv[0].matches("\\d+")) return "error: teams are id:EnglishName, e.g. 136013:Al-Hilal";
                String en = kv.length > 1 ? kv[1].trim() : "";
                // the bundled list knows the club by id (its Arabic name as the screen shows it); a club outside
                // the list gets the name made from its English one
                TeamSearch.Team known = Teams.byId(c, kv[0]);
                String ar = known != null && !known.ar.isEmpty() ? known.ar
                    : en.isEmpty() ? "#" + kv[0] : Arabic.team(c, en);
                teams.add(kv[0] + "|" + ar.replace("|", " ").replace(";", " "));
            }
            if (teams.size() > Prefs.MAX_TEAMS) return "error: at most " + Prefs.MAX_TEAMS + " teams";
        }
        String panel = i.getStringExtra("panel"), home = i.getStringExtra("home_panel");
        if (panel != null && Prefs.homeOnly(panel)) return "error: " + panel + " is a home-screen card only (the screensaver's panel already shows it): optimize/saver.sh home-card " + panel;
        if (panel != null && !isPanel(panel)) return "error: panel is one of " + panels();
        String panel2 = i.getStringExtra("panel2");   // checked against the first card it will sit beside: this one's new one, else today's
        if (panel2 != null) {
            String why = SecondCard.refuse(panel2, panel != null ? panel : p.panel(), Prefs.SAVER_PANELS);
            if (why != null) return why;
        }
        List<String> homeCards = null;
        if (home != null) {
            homeCards = new ArrayList<>();
            for (String h : home.split(",")) {
                h = h.trim();
                if (h.isEmpty() || "none".equals(h)) continue;
                if (!isPanel(h)) return "error: home_panel is up to " + Prefs.HOME_CARDS + " of " + panels() + ", separated by commas";
                if (!homeCards.contains(h)) homeCards.add(h);
            }
            if (homeCards.size() > Prefs.HOME_CARDS) return "error: home_panel is at most " + Prefs.HOME_CARDS + " cards";
        }
        String adhanBar = i.getStringExtra("adhanbar");
        if (adhanBar != null && !"on".equals(adhanBar) && !"off".equals(adhanBar))
            return "error: adhanbar is one of on off";
        String updateChannel = i.getStringExtra("updatechannel");
        if (updateChannel != null && !"latest".equals(updateChannel) && !"next".equals(updateChannel))
            return "error: updatechannel is one of latest next";
        String prayerAdj = i.getStringExtra("prayeradj");
        if (prayerAdj != null && !"on".equals(prayerAdj) && !"off".equals(prayerAdj))
            return "error: prayeradj is one of on off";
        String prayerMethod = i.getStringExtra("prayermethod");
        if (prayerMethod != null && PrayerMethod.parse(prayerMethod) == null)
            return "error: prayermethod is one of jafari gulf auto";
        int[] adj = null;   // «تعديل وقت الصلاة»: each given prayer is a whole number of minutes, −20..20; the rest stay
        for (int k = 0; k < PrayerAdj.COUNT; k++) {
            String v = i.getStringExtra("adj_" + PrayerAdj.KEYS[k]);
            if (v == null) continue;
            Integer n = PrayerAdj.parse(v.trim());
            if (n == null) return "error: " + PrayerAdj.KEYS[k] + " is a whole number of minutes from -" + PrayerAdj.MAX + " to " + PrayerAdj.MAX + ", like +2 or -1";
            if (adj == null) adj = p.prayerAdj();
            adj[k] = n;
        }
        Integer hijriOffset = null;   // «فرق التاريخ الهجري»: a whole number of days, -2..2
        String hijriExtra = i.getStringExtra("hijrioffset");
        if (hijriExtra != null) {
            hijriOffset = HijriOffset.parse(hijriExtra.trim());
            if (hijriOffset == null) return "error: hijrioffset is a whole number of days from -" + HijriOffset.MAX + " to " + HijriOffset.MAX + ", like -1 or +1 (0 = Umm al-Qura as it is)";
        }
        String yearStrip = i.getStringExtra("yearstrip");
        if (yearStrip != null && !"on".equals(yearStrip) && !"off".equals(yearStrip))
            return "error: yearstrip is one of on off";
        String sunrise = i.getStringExtra("sunrise"), prayerBar = i.getStringExtra("prayerbar"), wxMore = i.getStringExtra("wxmore");
        for (String[] x : new String[][] {{"sunrise", sunrise}, {"prayerbar", prayerBar}, {"wxmore", wxMore}})
            if (x[1] != null && !"on".equals(x[1]) && !"off".equals(x[1])) return "error: " + x[0] + " is one of on off";
        String months = i.getStringExtra("months");
        if (months != null && !"default".equals(months) && !"levant".equals(months)) return "error: months is one of default levant";
        String greeting = i.getStringExtra("greeting");
        if (greeting != null && !"on".equals(greeting) && !"off".equals(greeting))
            return "error: greeting is one of on off";
        String listenPill = i.getStringExtra("listenpill");
        if (listenPill != null && !"on".equals(listenPill) && !"off".equals(listenPill))
            return "error: listenpill is one of on off";
        String watchNext = i.getStringExtra("watchnext");
        if (watchNext != null && !"on".equals(watchNext) && !"off".equals(watchNext))
            return "error: watchnext is one of on off";
        String updateBell = i.getStringExtra("updatebell");
        if (updateBell != null && !"on".equals(updateBell) && !"off".equals(updateBell))
            return "error: updatebell is one of on off";
        String appearance = i.getStringExtra("appearance");
        if (appearance != null && !Arrays.asList(Prefs.APPEARANCES).contains(appearance))
            return "error: appearance is one of " + Prefs.join(Arrays.asList(Prefs.APPEARANCES), " ");
        String look = i.getStringExtra("look");
        if (look != null && !Arrays.asList(GlassStyle.LOOKS).contains(look))
            return "error: look is one of " + Prefs.join(Arrays.asList(GlassStyle.LOOKS), " ");
        String rating = i.getStringExtra("minrating");
        if (rating != null && !Arrays.asList(Prefs.MIN_RATINGS).contains(rating))
            return "error: minrating is one of " + Prefs.join(Arrays.asList(Prefs.MIN_RATINGS), " ");
        String photos = i.getStringExtra("photos");
        if (photos != null && !Arrays.asList(Prefs.PHOTO_MINUTES).contains(photos))
            return "error: photos is one of " + Prefs.join(Arrays.asList(Prefs.PHOTO_MINUTES), " ");
        String digits = i.getStringExtra("digits");
        if (digits != null && !isChoice(Prefs.DIGITS, digits))
            return "error: digits is one of " + choices(Prefs.DIGITS);
        String clock24 = i.getStringExtra("clock24");
        if (clock24 != null && !ClockSetting.valid(clock24))
            return "error: clock24 is one of on off";
        String national = i.getStringExtra("national");
        if (national != null && !isChoice(Prefs.NATIONAL, national))
            return "error: national is one of " + choices(Prefs.NATIONAL);
        String cardStyle = i.getStringExtra("cardstyle");
        if (cardStyle != null && !isChoice(Prefs.CARD_STYLES, cardStyle))
            return "error: cardstyle is one of " + choices(Prefs.CARD_STYLES);
        String mkStyle = i.getStringExtra("mkstyle");
        if (mkStyle != null && !isChoice(Prefs.MK_STYLES, mkStyle))
            return "error: mkstyle is one of " + choices(Prefs.MK_STYLES);
        String mkBar = i.getStringExtra("mkbar");
        if (mkBar != null && !"on".equals(mkBar) && !"off".equals(mkBar))
            return "error: mkbar is one of on off";
        String clockSize = i.getStringExtra("clocksize");
        if (clockSize != null && !isChoice(Prefs.CLOCK_SIZES, clockSize))
            return "error: clocksize is one of " + choices(Prefs.CLOCK_SIZES);
        List<String> markets = codes(i.getStringExtra("markets"), Prefs.MARKETS);
        if (markets != null) {
            if (markets.contains(null)) return "error: markets are " + names(Prefs.MARKETS);
            if (markets.isEmpty() || markets.size() > Prefs.MAX_MARKETS || new java.util.HashSet<>(markets).size() != markets.size())
                return "error: markets is 1 to " + Prefs.MAX_MARKETS + " different ones of " + names(Prefs.MARKETS);
        }
        List<String> stocks = null;
        if (i.getStringExtra("stocks") != null) {
            stocks = new ArrayList<>();
            for (String s : i.getStringExtra("stocks").split(",")) {
                s = s.trim().toUpperCase(java.util.Locale.ROOT);
                if (s.isEmpty()) continue;
                if (!s.matches(Prefs.STOCK_PATTERN) || stocks.contains(s))
                    return "error: stocks are 4-digit Tadawul codes or US tickers, each once, e.g. 2222,1120,AAPL";
                stocks.add(s);
            }
            if (stocks.isEmpty() || stocks.size() > Prefs.MAX_STOCKS)
                return "error: stocks is 1 to " + Prefs.MAX_STOCKS + " codes or tickers";
        }
        String slot = i.getStringExtra("slot");
        if (slot != null && !isChoice(Prefs.SLOTS, slot))
            return "error: slot is one of " + choices(Prefs.SLOTS);
        String cd = i.getStringExtra("countdown"), cdName = i.getStringExtra("cdname");
        if (cd != null && !"clear".equals(cd) && !isDate(cd))
            return "error: countdown is a date like 2026-12-01, or clear";
        if (cdName != null && cd == null) return "error: cdname goes with countdown DATE";
        if (cdName != null && cdName.length() > 40) return "error: cdname is at most 40 characters";
        String idle = i.getStringExtra("idle");
        if (idle != null) {
            if (!Arrays.asList(Prefs.IDLE_MINUTES).contains(idle))
                return "error: idle is one of " + Prefs.join(Arrays.asList(Prefs.IDLE_MINUTES), " ");
            if (!Prefs.canWriteSettings(c))
                return "error: idle not allowed: this app may not change system settings on this TV"
                    + " (the TV's own screensaver settings can, or the shell: settings put system screen_off_timeout)";
        }
        List<String> genres = codes(i.getStringExtra("genres"), Prefs.GENRES);
        List<String> langs = codes(i.getStringExtra("langs"), Prefs.LANGS);
        if (genres != null && genres.contains(null)) return "error: genres are " + names(Prefs.GENRES);
        if (langs != null && langs.contains(null)) return "error: langs are " + names(Prefs.LANGS);
        List<String> events = codes(i.getStringExtra("events"), Prefs.EVENTS);
        if (events != null && events.contains(null)) return "error: events are " + names(Prefs.EVENTS);
        List<String> leagues = null;
        if (i.getStringExtra("leagues") != null) {
            leagues = new ArrayList<>();
            for (String s : i.getStringExtra("leagues").split(",")) {
                if (s.trim().isEmpty()) continue;
                String[] l = Prefs.leagueByName(s);
                if (l == null) return "error: leagues are (English or Arabic name) " + names(Prefs.LEAGUES);
                if (!leagues.contains(l[0])) leagues.add(l[0]);
            }
            if (leagues.size() > Prefs.MAX_LEAGUES) return "error: at most " + Prefs.MAX_LEAGUES + " leagues";
        }

        if (idle != null) {   // first: a refusal here leaves everything else unsaved
            try {
                Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
                    Integer.parseInt(idle) * 60000);
            } catch (SecurityException e) {
                return "error: idle not allowed: the TV refused the change";
            }
        }
        if (city != null) p.setCity(city[0], city[1], city[2], tz == null ? "" : tz);
        if (teams != null) p.setList("teams", teams);
        if (panel != null) p.setPanel(panel);
        if (panel2 != null) p.setPanel2(panel2);
        if (homeCards != null) p.setHomePanels(homeCards);
        if (greeting != null) p.setGreeting("on".equals(greeting));
        if (listenPill != null) p.setListenPill("on".equals(listenPill));
        if (updateBell != null) p.setUpdateBell("on".equals(updateBell));
        if (watchNext != null) p.setWatchNext("on".equals(watchNext));
        if (appearance != null) p.setAppearance(appearance);
        if (look != null) p.setLook(look);
        if (adhanBar != null) p.setAdhanBar("on".equals(adhanBar));
        if (updateChannel != null && !updateChannel.equals(p.updateChannel())) {   // another release now: the number shown as the newest is read again
            p.setUpdateChannel(updateChannel);
            Updater.refreshLatestAsync(c);
        }
        if (yearStrip != null) p.setYearStrip("on".equals(yearStrip));
        if (sunrise != null) p.setSunrise("on".equals(sunrise));
        if (prayerBar != null) p.setPrayerBar("on".equals(prayerBar));
        if (wxMore != null) p.setWxMore("on".equals(wxMore));
        if (months != null) p.setMonths(months);
        if (prayerMethod != null) p.setPrayerMethod(PrayerMethod.parse(prayerMethod));
        if (adj != null) p.setPrayerAdj(adj);
        if (hijriOffset != null) p.setHijriOffset(hijriOffset);
        if (prayerAdj != null) p.setPrayerAdjOn("on".equals(prayerAdj));
        if (rating != null) p.setMinRating(rating);
        if (photos != null) p.setPhotoMinutes(photos);
        if (digits != null) p.setDigits(digits);
        if (clock24 != null) p.setClock24("on".equals(clock24));
        if (national != null) p.setNational(national);
        if (cardStyle != null) p.setCardStyle(cardStyle);
        if (mkStyle != null) p.setMkStyle(mkStyle);
        if (markets != null) p.setMarkets(markets);
        if (mkBar != null) p.setMarketsOn("on".equals(mkBar));
        if (clockSize != null) p.setClockSize(clockSize);
        if (stocks != null) p.setStocks(stocks);
        if (slot != null) p.setSlot(slot);
        if (cd != null) {
            if ("clear".equals(cd)) p.setCountdown("", "");
            else p.setCountdown(cd, cdName == null ? "" : cdName.trim());
        }
        if (genres != null) p.setList("genres", genres);
        if (langs != null) p.setList("langs", langs);
        if (events != null) p.setList("events", events);
        if (leagues != null) p.setList("leagues", leagues);
        // the page reads its settings once, when it starts: a screensaver (or preview) already on the screen would go on showing the old ones
        c.sendBroadcast(new Intent(Prefs.CHANGED).setPackage(c.getPackageName()));
        return "saved\n" + describe(c, p);
    }

    /** «المهام»: n is the number list shows (from 1, done ones included). */
    private static String tasks(Prefs p, Intent i) {
        String op = String.valueOf(i.getStringExtra("op"));
        switch (op) {
            case "list":
                return listing(p);
            case "add": {
                String why = p.addTask(i.getStringExtra("text"));
                return why != null ? "error: " + why : "saved\n" + listing(p);
            }
            case "done":
            case "del": {
                int n = i.getIntExtra("n", 0);
                boolean ok = "done".equals(op) ? p.doneTask(n) : p.deleteTask(n);
                return !ok ? "error: no task number " + n + " (list shows the numbers)"
                    : "saved\n" + listing(p);
            }
            case "clear":
                p.clearTasks();
                return "saved\nno tasks";
            default:
                return "error: op is one of list add done del clear";
        }
    }

    /** «صوري»: n is the number list shows (from 1). */
    private static String photos(Context c, Prefs p, Intent i) {
        String op = String.valueOf(i.getStringExtra("op"));
        switch (op) {
            case "list":
                return photoListing(c, p);
            case "add": {
                String path = i.getStringExtra("path");
                java.io.File src = Own.incoming(c);
                if (path != null) {
                    if (!path.matches("/data/local/tmp/urtv-own/[A-Za-z0-9_.-]+") || path.contains(".."))
                        return "error: path must be a file in /data/local/tmp/urtv-own/";
                    src = new java.io.File(path);
                }
                int size = i.getIntExtra("size", -1);   // what the shell sent: a short or stale file is refused
                String why = size >= 0 && src.length() != size ? "the file is not the one sent (" + src.length() + " of " + size + " bytes)"
                    : Own.add(c, src);
                if (why != null && src.equals(Own.incoming(c))) src.delete();
                return why != null ? "error: " + why : "saved\n" + photoListing(c, p);
            }
            case "del": {
                int n = i.getIntExtra("n", 0);
                return !Own.delete(c, n) ? "error: no photo number " + n + " (list shows the numbers)"
                    : "saved\n" + photoListing(c, p);
            }
            case "clear":
                Own.clear(c);
                return "saved\n" + photoListing(c, p);
            case "mode": {
                String m = i.getStringExtra("mode");
                if (!Own.MIX.equals(m) && !Own.ONLY.equals(m)) return "error: mode is mix or only";
                p.setOwnMode(m);
                p.bumpOwnRev();
                return "saved\n" + photoListing(c, p);
            }
            default:
                return "error: op is one of list add del clear mode";
        }
    }

    private static String photoListing(Context c, Prefs p) {
        int n = Own.count(c);
        StringBuilder b = new StringBuilder("photos: " + n + " of " + Own.MAX + "\nmode: " + p.ownMode());
        if (n == 0) b.append(" (no photos: ours only)");
        else if (!Own.ONLY.equals(Own.mode(c))) b.append(" (they join ours)");
        else b.append(" (only theirs)");
        for (int k = 1; k <= n; k++) b.append('\n').append(k).append(". ").append(Own.describe(c, k));
        return b.toString();
    }

    private static String listing(Prefs p) {
        List<Task> all = p.tasks();
        if (all.isEmpty()) return "no tasks";
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < all.size(); k++)
            b.append(k + 1).append(all.get(k).done ? ". [done] " : ". ").append(all.get(k).text).append('\n');
        return b.toString().trim();
    }

    private static String describe(Context c, Prefs p) {
        String[] city = p.city();
        String latest = p.latestVersion();   // what Updater last saw on the release of the current channel
        int newest = Versions.buildOf(latest);
        return "version: " + Versions.displayEn(BuildConfig.MARKETING, BuildConfig.VERSION_CODE)
            + (newest >= 0 ? " (newest: " + Versions.displayEn(BuildConfig.MARKETING, newest) + ("next".equals(p.updateChannel()) ? ", on the next channel" : "") + ")" : "")
            + "\ncity: " + city[0] + " (" + city[1] + ", " + city[2] + ")"
            + "\ntimezone: " + p.cityTz()
            + "\nteams: " + Prefs.join(p.teams(), ", ")
            + "\npanel: " + p.panel() + "\npanel2: " + p.panel2()
            + "\nhome_panel: " + p.homePanel()
            + "\ngreeting: " + (p.greeting() ? "on" : "off")
            + "\nlistenpill: " + (p.listenPill() ? "on" : "off")
            + "\nupdatebell: " + (p.updateBell() ? "on" : "off")
            + "\nwatchnext: " + (p.watchNext() ? "on" : "off")
            + "\nappearance: " + p.appearance()
            + "\nlook: " + p.look()
            + "\nadhanbar: " + (p.adhanBar() ? "on" : "off")
            + "\nupdatechannel: " + p.updateChannel()
            + "\nyearstrip: " + (p.yearStrip() ? "on" : "off")
            + "\nsunrise: " + (p.sunrise() ? "on" : "off") + "\nprayerbar: " + (p.prayerBar() ? "on" : "off")
            + "\nwxmore: " + (p.wxMore() ? "on" : "off") + "\nmonths: " + p.months()
            + "\nprayermethod: " + p.prayerMethod()
            + "\nprayeradj: " + (p.prayerAdjOn() ? "on" : "off") + " " + PrayerAdj.describe(p.prayerAdj())
            + "\nhijrioffset: " + HijriOffset.describe(p.hijriOffset())
            + "\nminrating: " + p.minRating()
            + "\nphotos: " + p.photoMinutes()
            + "\ndigits: " + p.digits()
            + "\nclock24: " + ClockSetting.describe(p.clock24())
            + "\nnational: " + p.national()
            + "\ncardstyle: " + p.cardStyle()
            + "\nmarkets: " + (p.marketsOn() ? "" : "off ") + Prefs.join(p.markets(), ",") + "\nclocksize: " + p.clockSize() + "\nmkstyle: " + p.mkStyle()
            + "\nstocks: " + Prefs.join(p.stocks(), ",")
            + "\nslot: " + p.slot()
            + "\ncountdown: " + (p.countdownDate().isEmpty() ? "none" : p.countdownDate() + " " + p.countdownName())
            + "\nidle: " + idle(Prefs.idleMs(c))
            + "\ngenres: " + Prefs.join(p.list("genres"), ",") + "\nlangs: " + Prefs.join(p.list("langs"), ",")
            + "\nevents: " + Prefs.join(p.list("events"), ",")
            + "\nleagues: " + Prefs.join(p.leagues(), ",")
            + "\nautoupdate: " + p.autoUpdate();
    }

    /** minutes the TV waits before the screensaver, "unknown" when it does not say */
    private static String idle(long ms) {
        if (ms <= 0) return "unknown";
        return ms % 60000 == 0 ? String.valueOf(ms / 60000) : (ms / 1000) + "s";
    }

    /** a real calendar date written YYYY-MM-DD */
    private static boolean isDate(String v) {
        if (!v.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        return f.parse(v, pos) != null && pos.getIndex() == v.length();
    }

    private static boolean isChoice(String[][] all, String v) {
        for (String[] x : all) if (x[0].equals(v)) return true;
        return false;
    }

    private static String choices(String[][] all) {
        List<String> out = new ArrayList<>();
        for (String[] x : all) out.add(x[0]);
        return Prefs.join(out, " ");
    }

    private static boolean isPanel(String v) {
        for (String[] x : Prefs.PANELS) if (x[0].equals(v)) return true;
        return false;
    }

    private static String panels() {
        return names(Prefs.PANELS);
    }

    private static String names(String[][] table) {
        List<String> out = new ArrayList<>();
        for (String[] x : table) out.add(x[0]);
        return Prefs.join(out, " ");
    }

    /** "Drama,Comedy" -> the same codes, checked against the table; null entry = unknown; "" = all. */
    private static List<String> codes(String raw, String[][] table) {
        if (raw == null) return null;
        List<String> out = new ArrayList<>();
        for (String s : raw.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            String hit = null;
            for (String[] x : table) if (x[0].equalsIgnoreCase(s)) hit = x[0];
            out.add(hit);
        }
        return out;
    }
}
