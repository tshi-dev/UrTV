package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import sa.urtv.home.sports.KickoffPlan;
import sa.urtv.home.sports.KickoffScheduler;
import sa.urtv.home.sports.KickoffStore;
import sa.urtv.home.tilawah.AdhanFront;
import sa.urtv.home.tilawah.NowPlaying;
import sa.urtv.home.tilawah.Whistle;

/**
 * 4.4 «تنبيه بداية المباراة»: a followed club's kick-off (the alarm KickoffScheduler set from the times the home's page handed over). One check
 * of the game (lookupevent: no answer, postponed or cancelled: nothing), then the short whistle (KickoffPlan.sound: chosen, never in kids
 * mode, the quiet hours or with the adhan) and the notice where the person is: the home's page draws it (spKick, told by a broadcast of our
 * own), the screensaver's page draws its own from its clock, over another app a small overlay with «فوق تطبيق آخر» and the permission
 * (KickoffOverlay), else a text notice (a toast, like the reminders'). Each game once; the alarm is put back after a reboot, a clock change
 * or an update.
 */
public class KickoffReceiver extends BroadcastReceiver {
    /** the home draws the notice now (an own broadcast, with setPackage): extra "json" */
    static final String SHOW = "sa.urtv.home.KICKOFF_SHOW";
    private static final Uri CONFIG = Uri.parse("content://sa.urtv.screensaver.config");

    @Override public void onReceive(Context c, Intent intent) {
        final Context app = c.getApplicationContext();
        if (intent == null || !KickoffScheduler.ACTION.equals(intent.getAction())) { KickoffScheduler.schedule(app); return; }
        final PendingResult done = goAsync();
        new Thread(() -> {
            try { run(app); } catch (RuntimeException e) { Log.i("UrTVKickoff", "not shown: " + e.getClass().getSimpleName()); }
            finally { KickoffScheduler.schedule(app); done.finish(); }
        }, "kickoff").start();
    }

    /** the settings: {on, whistle, over}; the screensaver app missing or older: on, the whistle, not over other apps */
    static boolean[] settings(Context c) {
        boolean[] s = {true, true, false};
        try (Cursor cur = c.getContentResolver().query(CONFIG, null, null, null, null)) {
            int i = cur == null ? -1 : cur.getColumnIndex("kickoff");
            if (i >= 0 && cur.moveToFirst() && cur.getString(i) != null) {
                String[] v = cur.getString(i).split("\\|", -1);
                s[0] = !"0".equals(v[0]);
                s[1] = v.length < 2 || !"none".equals(v[1]);
                s[2] = v.length > 2 && "1".equals(v[2]);
            }
        } catch (RuntimeException ignored) { }
        return s;
    }

    private static void run(Context app) {
        long now = System.currentTimeMillis();
        List<KickoffPlan.Game> due = KickoffPlan.due(KickoffStore.games(app), now, KickoffStore.done(app));
        if (due.isEmpty()) return;
        boolean[] set = settings(app);
        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        boolean screenOn = pm == null || pm.isInteractive();
        for (KickoffPlan.Game g : due) {
            KickoffStore.markDone(app, g.id);   // each game once, shown or not
            if (!set[0] || !screenOn || NowPlaying.adhanLive()) continue;   // never with the adhan: dropped, not delayed
            String[] check = lookup(g.id);
            if (!KickoffPlan.confirmed(check != null, check == null ? "" : check[0], check != null && "yes".equalsIgnoreCase(check[1]))) continue;
            int front = AdhanFront.whereNow(app);
            boolean kids = Parental.state(app).kids;
            final int where = KickoffPlan.where(front == AdhanFront.AT_HOME ? 0 : front == AdhanFront.AT_SAVER ? 1 : 2, kids, set[2], Settings.canDrawOverlays(app));
            if (KickoffPlan.sound(set[1], kids, Whistle.quietNow(app), NowPlaying.adhanLive(), where)) Whistle.play(app);
            new Handler(Looper.getMainLooper()).post(() -> show(app, g, where));
            Log.i("UrTVKickoff", "kick-off shown: " + where);
        }
    }

    private static void show(Context app, KickoffPlan.Game g, int where) {
        S.init(app);
        if (where == KickoffPlan.HOME) app.sendBroadcast(new Intent(SHOW).setPackage(app.getPackageName()).putExtra("json", g.noticeJson()).putExtra("line", g.line(S.t("بدأت المباراة"))));
        else if (where == KickoffPlan.OVERLAY && KickoffOverlay.show(app, g)) return;
        else if (where == KickoffPlan.OVERLAY || where == KickoffPlan.TOAST) {
            try { Toast.makeText(app, g.line(S.t("بدأت المباراة")), Toast.LENGTH_LONG).show(); } catch (RuntimeException ignored) { }
        }
    }

    /** {strStatus, strPostponed} of the game, or null when there is no answer (no network, the server down) */
    static String[] lookup(String id) {
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL("https://www.thesportsdb.com/api/v1/json/123/lookupevent.php?id=" + id).openConnection();
            NetLog.count(String.valueOf(h.getURL()));   // G3: the request log (NetLog)
            h.setConnectTimeout(10_000);
            h.setReadTimeout(10_000);
            if (h.getResponseCode() != 200) return null;
            try (InputStream in = h.getInputStream()) {
                org.json.JSONArray evs = new org.json.JSONObject(new String(Bounded.readAll(in, 200_000), "UTF-8")).optJSONArray("events");
                org.json.JSONObject e = evs == null ? null : evs.optJSONObject(0);
                if (e == null) return null;
                return new String[] {e.optString("strStatus", ""), e.optString("strPostponed", "")};
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (h != null) h.disconnect();
        }
    }
}
