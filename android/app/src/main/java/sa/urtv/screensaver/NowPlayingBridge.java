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
                o.put("live", true);   // last: nothing half read counts as an adhan
            }
        } catch (Exception ignored) {
        } finally {
            if (cur != null) try { cur.close(); } catch (Exception ignored) { }
        }
        return o;
    }

    /**
     * The launcher's «إعداد تلفزيوني» is waiting for the TV's «السماح بتصحيح USB؟» (a prompt sent under the screensaver stays stuck until a
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

    /** Ends the adhan that is playing in the launcher (it checks our signature, and stops only an adhan). */
    static void stopAdhan(Context c) {
        try {
            c.getApplicationContext().getContentResolver().call(Uri.parse("content://sa.urtv.home.nowplaying"), "stop_adhan", null, null);
        } catch (Exception ignored) { }
    }

    /** The page asks once in a while, and every few seconds around a prayer time: the adhan's words go on the bar. */
    @JavascriptInterface
    public String adhan() {
        JSONObject o = adhanState(app);
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
