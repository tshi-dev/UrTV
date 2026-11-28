package sa.urtv.screensaver;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

/**
 * The screen's «يُتلى الآن» card: the page asks window.UrtvNative.nowPlaying() when it starts and once a minute; the answer is what the
 * launcher's NowPlayingProvider says, as {"title":"سورة الكهف","who":"ماهر المعيقلي"}, or "" when nothing is playing (or the launcher is
 * not installed). Nothing else is reachable from the page through this, and it is given to the screen only (not to the settings' previews).
 */
final class NowPlayingBridge {
    private final Context app;

    NowPlayingBridge(Context c) { app = c.getApplicationContext(); }

    /** An adhan for a prayer is playing in the launcher: {"live":true,"prayer":4,"any":false}; live false otherwise (or the launcher is not there). */
    static JSONObject adhanState(Context c) {
        JSONObject o = new JSONObject();
        Cursor cur = null;
        try {
            o.put("live", false);
            cur = c.getApplicationContext().getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/adhan"), null, null, null, null);
            if (cur != null && AdhanReply.valid(cur.getColumnNames()) && cur.moveToFirst()) {   // an older launcher answers with another row
                int prayer = cur.getInt(cur.getColumnIndexOrThrow("prayer"));
                boolean any = cur.getInt(cur.getColumnIndexOrThrow("any_key")) == 1;
                o.put("prayer", prayer);
                o.put("any", any);
                int named = cur.getColumnIndex("name");   // 4.4: a dua's name (a launcher before 4.4 has no such column)
                if (named >= 0 && cur.getString(named) != null && !cur.getString(named).isEmpty()) o.put("name", cur.getString(named));
                o.put("live", true);   // last: nothing half read counts as an adhan
            }
        } catch (Exception ignored) {
        } finally {
            if (cur != null) try { cur.close(); } catch (Exception ignored) { }
        }
        try {   // 4.4 beta 4: the prayers that do not show on the screen (the launcher's «الأذان لكل صلاة»), for the page's bar; an older launcher says nothing: none hidden
            Cursor hc = c.getApplicationContext().getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/adhan_prayers"), null, null, null, null);
            if (hc != null) try { if (hc.getColumnIndex("hide") >= 0 && hc.moveToFirst()) o.put("hide", hc.getInt(hc.getColumnIndex("hide"))); } finally { hc.close(); }
        } catch (Exception ignored) { }
        return o;
    }

    /**
     * The launcher's «إعداد تلفزيوني' is waiting for the TV's «السماح بتصحيح USB؟» (a prompt sent under the screensaver stays stuck until a
     * restart): the screensaver ends at once while this is true. False when the launcher does not say (an older one, not installed, refused).
     */
    static boolean setupWaiting(Context c) {
        Cursor cur = null;
        try {
            cur = c.getApplicationContext().getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/setup_hold"), null, null, null, null);
            if (cur == null || cur.getColumnIndex("held") < 0 || !cur.moveToFirst()) return false;   // an older launcher answers with another row
            return cur.getInt(cur.getColumnIndex("held")) == 1;
        } catch (Exception e) {
            return false;
        } finally {
            if (cur != null) try { cur.close(); } catch (Exception ignored) { }
        }
    }

    /**
     * «إعداد تلفزيوني» is running a setup in the launcher (its TV prompt, its steps): the dream steps aside then (ScreenDream). False when the
     * launcher does not say (an older launcher, or not installed): the screensaver works as always.
     */
    static boolean setupRunning(Context c) {
        Cursor cur = null;
        try {
            cur = c.getApplicationContext().getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/setup"), null, null, null, null);
            return cur != null && cur.getColumnIndex("running") >= 0 && cur.moveToFirst() && cur.getInt(cur.getColumnIndex("running")) == 1;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (cur != null) try { cur.close(); } catch (Exception ignored) { }
        }
    }

    /** 4.4 kids mode is on in the launcher: the page draws no kick-off notice on the screensaver then. null when the launcher did not answer (an error, no row): the last answer stays (KeptAnswer). */
    static Boolean kidsAsk(Context c) {
        Cursor cur = null;
        try {
            cur = c.getApplicationContext().getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/kids"), null, null, null, null);
            if (cur == null || cur.getColumnIndex("on") < 0 || !cur.moveToFirst()) return null;
            return cur.getInt(cur.getColumnIndex("on")) == 1;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (cur != null) try { cur.close(); } catch (Exception ignored) { }
        }
    }

    private static volatile boolean kidsBusy;
    /** kids mode as the launcher last said it (a copy kept in hub_seen, like the clean mask): no query, so it is safe on the UI thread (kidsAsk blocks). The first start after the
     *  install, before any answer, takes the safe side (kids); a thread asks the launcher again for the next one. */
    static boolean kidsOn(final Context c) {
        final Context app = c.getApplicationContext();
        if (!kidsBusy) {
            kidsBusy = true;
            Thread t = new Thread(() -> {
                try {
                    android.content.SharedPreferences sp = app.getSharedPreferences("hub_seen", Context.MODE_PRIVATE);
                    Boolean ans = kidsAsk(app);
                    boolean ever = sp.contains("kids_on");
                    if (ans == null && !ever && !launcherHere(app)) ans = Boolean.FALSE;   // no launcher on this TV: there are no kids mode and no answer to wait for
                    Boolean now = ever ? KeptAnswer.toStore(ans, sp.getBoolean("kids_on", false)) : ans;   // only a real answer that differs; the very first answer is always kept
                    if (now != null) {
                        sp.edit().putBoolean("kids_on", now).apply();   // before the broadcast: the next kidsOn() is this one
                        app.sendBroadcast(new android.content.Intent(Prefs.CHANGED).setPackage(app.getPackageName()));   // the dream is built again with it (as CleanMask), only when its address differs
                    }
                }
                catch (RuntimeException ignored) { /* the last answer stays */ }
                finally { kidsBusy = false; }
            }, "kids-flag");
            t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
        }
        return app.getSharedPreferences("hub_seen", Context.MODE_PRIVATE).getBoolean("kids_on", true);   // never answered yet: kids, the safe side (no kick-off notice before the launcher has said)
    }

    /** on a thread: the launcher is installed, so an answer is to come */
    private static boolean launcherHere(Context c) {
        try { c.getPackageManager().getPackageInfo(CoverRule.HOME_PKG, 0); return true; }
        catch (Exception e) { return false; }
    }

    /** 4.4 «جرّب التنبيه»: the launcher plays its kick-off whistle once (it checks our signature; never over the adhan); false when it could not be asked */
    static boolean kickoffWhistle(Context c) {
        try {
            return c.getApplicationContext().getContentResolver().call(Uri.parse("content://sa.urtv.home.nowplaying"), "kickoff_whistle", null, null) != null;
        } catch (Exception ignored) { return false; }
    }

    /**
     * 4.4 «بدء شاشة التوقف بعد»: the launcher sets the TV's idle delay over its own adb (it checks our signature, and takes only the minutes we
     * offer). Blocks for the adb link: call it off the UI thread. "ok", "no_link", "busy" or "bad"; "no_link" too when the launcher could not be asked.
     */
    static String setIdle(Context c, String minutes) {
        try {
            android.os.Bundle b = c.getApplicationContext().getContentResolver().call(Uri.parse("content://sa.urtv.home.nowplaying"), "set_idle", minutes, null);
            String r = b == null ? null : b.getString("result");
            return r == null ? "no_link" : r;
        } catch (Exception ignored) { return "no_link"; }
    }

    /**
     * 4.4 beta 3 «الوضع الخفيف» on «تلقائي»: the home's line when this TV's own signs turned it on («تلقائي: شغّال الآن لأن …», in the home's language),
     * "" when they did not, null when the home did not answer. A call into its process: never on the UI thread.
     */
    static String liteLine(Context c) {
        try {
            android.os.Bundle b = c.getApplicationContext().getContentResolver().call(Uri.parse("content://sa.urtv.home.nowplaying"), "lite_line", null, null);
            return b == null ? null : b.getString("result");
        } catch (Exception ignored) { return null; }
    }

    /** 4.4 «زر الرئيسية يفتح تلفزيونك مباشرة»: the add-on's Home key catch through the launcher ("on", "off" or null to read); null when either did not answer */
    static Boolean homeKey(Context c, String set) {
        try {
            android.os.Bundle b = c.getApplicationContext().getContentResolver().call(Uri.parse("content://sa.urtv.home.nowplaying"), "home_key", set, null);
            return b == null || !b.containsKey("on") ? null : b.getBoolean("on");
        } catch (Exception ignored) { return null; }
    }

    /** Ends the adhan that is playing in the launcher (it checks our signature, and stops only an adhan). A call into its process: not on the UI thread (stopAdhanSoon). */
    static void stopAdhan(Context c) {
        try {
            c.getApplicationContext().getContentResolver().call(Uri.parse("content://sa.urtv.home.nowplaying"), "stop_adhan", null, null);
        } catch (Exception ignored) { }
    }

    /**
     * 4.4 QA-2: the key that stops the adhan: the call goes on a thread, and what was known is forgotten at once (the next key is an ordinary
     * one; the adhan's bar goes with the page's next ask).
     */
    static void stopAdhanSoon(Context c) {
        final Context app = c.getApplicationContext();
        adhanSeen = null;
        new Thread(() -> stopAdhan(app), "adhan-stop").start();
    }

    // 4.4 QA-2: a key on the screensaver decides from the adhan state the page's own ask (every 3 s, adhan()) or a thread last learned, never from a query on the UI thread
    private static volatile JSONObject adhanSeen;
    private static volatile long adhanSeenAt;
    private static volatile boolean adhanBusy;
    private static final long ADHAN_FRESH_MS = 8000;   // the page asks every 3 s: a copy older than this is no longer what the bar says

    /**
     * What the screensaver knows of the adhan now: {"live":true,...} when the last answer (not older than ADHAN_FRESH_MS) said it plays, else
     * {"live":false}. Never a query: it is read for a key on the UI thread. It also starts a thread that asks the launcher again, so the next
     * key (and the bar) see the change; the first key of an adhan that started a second ago may not know it yet, as the bar did not.
     */
    static JSONObject adhanKnown(Context c) {
        refreshAdhan(c);
        JSONObject o = adhanSeen;
        if (o != null && android.os.SystemClock.elapsedRealtime() - adhanSeenAt <= ADHAN_FRESH_MS) return o;
        JSONObject none = new JSONObject();
        try { none.put("live", false); } catch (org.json.JSONException ignored) { }
        return none;
    }

    /** asks the launcher about the adhan on a thread of its own (one at a time) and keeps the answer */
    static void refreshAdhan(Context c) {
        if (adhanBusy) return;
        adhanBusy = true;
        final Context app = c.getApplicationContext();
        Thread t = new Thread(() -> {
            try { rememberAdhan(adhanState(app)); }
            catch (RuntimeException ignored) { /* the last answer stays until it is too old */ }
            finally { adhanBusy = false; }
        }, "adhan-state");
        t.setPriority(Thread.MIN_PRIORITY); t.setDaemon(true); t.start();
    }

    private static void rememberAdhan(JSONObject o) { adhanSeen = o; adhanSeenAt = android.os.SystemClock.elapsedRealtime(); }

    /** The page asks once in a while, and every few seconds around a prayer time: the adhan's words go on the bar (on the bridge's thread); the key handlers use the same answer. */
    @JavascriptInterface
    public String adhan() {
        JSONObject o = adhanState(app);
        rememberAdhan(o);
        return o.optBoolean("live") ? o.toString() : "";
    }

    /** true while the reminder's card is on the screen: ScreenDream then gives the keys to it (ReminderKeys) */
    private static volatile boolean card;

    static boolean reminderCard() { return card; }

    /** the screen is gone: no card is up any more */
    static void clearCard() { card = false; }

    @JavascriptInterface
    public void reminderShown(boolean on) { card = on; }

    /**
     * 4.3 (issue #55): the page's offset for the city's zone at a moment (its WebView's tz data, newer than the TV's own): kept for this app
     * (ZoneFix) and for the home (ConfigProvider's column page_zone). Only the city's own zone, a moment within a day of now, a sane offset.
     */
    @JavascriptInterface
    public void zoneOffset(String zone, String at, int minutes) {
        try {
            long t = Long.parseLong(at), now = System.currentTimeMillis();
            Prefs p = new Prefs(app);
            if (zone == null || !zone.equals(p.cityTz()) || Math.abs(now - t) > 86_400_000L || minutes < -14 * 60 || minutes > 14 * 60) return;
            ZoneFix.report(zone, t, minutes);
            p.setPageZone(zone + "|" + t + "|" + minutes);
        } catch (RuntimeException ignored) { }
    }

    /** 4.4 beta 4: "1" when the TV's clock is far from the network's (ClockCheck), "0" when it is right, "" while it is not known: the page then says «ساعة التلفزيون غير مضبوطة» instead of a date */
    @JavascriptInterface
    public String clockOff() {
        int s = ClockCheck.state();
        return s < 0 ? "" : String.valueOf(s);
    }

    private static volatile long photoLoggedAt;
    private static volatile int photoLogged;

    /**
     * 4.4 beta 4 (photos stayed black on some Google TVs): the page says a photo did not load. One line under UrTVSaver: what failed (hosted, own,
     * bundled, shown, none, error) and the file's name only; at most 20 lines in 10 minutes, so a page that fails every minute does not fill the log.
     */
    @JavascriptInterface
    public void photoError(String what, String name) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - photoLoggedAt > 600_000L) { photoLoggedAt = now; photoLogged = 0; }
        if (++photoLogged > 20) return;
        String w = what == null ? "" : what.replaceAll("[^a-z]", ""), n = name == null ? "" : name.replaceAll("[^A-Za-z0-9._-]", "");
        android.util.Log.w("UrTVSaver", "photo: " + (w.length() > 12 ? w.substring(0, 12) : w) + " did not load" + (n.isEmpty() ? "" : " (" + (n.length() > 60 ? n.substring(0, 60) : n) + ")"));
    }

    /** «تم» on the card: that task is done (a repeating reminder moves to its next time) */
    @JavascriptInterface
    public void reminderDone(long id) { new Prefs(app).doneTaskId(id); }

    /** «ذكّرني بعد ١٠ دقائق» */
    @JavascriptInterface
    public void reminderSnooze(long id, int minutes) { new Prefs(app).snoozeTaskId(id, minutes); }

    /** the soft chime of «صوت خفيف»: one short two-note tone at a low volume, no file and no permission */
    @JavascriptInterface
    public void reminderChime() {
        try {
            final android.media.ToneGenerator g = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 35);
            g.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 400);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(g::release, 900);
        } catch (RuntimeException ignored) { }
    }

    @JavascriptInterface
    public String nowPlaying() {
        Cursor c = null;
        try {
            c = app.getContentResolver().query(Uri.parse("content://sa.urtv.home.nowplaying/now"), null, null, null, null);
            if (c != null && c.moveToFirst()) {
                JSONObject o = new JSONObject();
                o.put("title", c.getString(c.getColumnIndexOrThrow("title")));
                o.put("who", c.getString(c.getColumnIndexOrThrow("who")));
                return o.toString();
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignored) { }
        }
        return "";
    }
}
