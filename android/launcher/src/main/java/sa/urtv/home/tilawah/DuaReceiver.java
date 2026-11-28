package sa.urtv.home.tilawah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.util.Log;

/**
 * 4.4: a dua's alarm. It plays like the adhan (over the home and the screensaver, the recitation paused and brought back after, a card with its
 * name and «إيقاف»), only while the TV is on, never late (a missed one is not made up), never over the adhan, and never over another app's
 * sound unless the person turned «فوق تطبيق آخر» on for it (DuaPlan says so). One that does not repeat turns itself off once its moment came.
 */
public class DuaReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        // 4.4 «أجزاء UrTV»: «الأدعية» off, none plays; the next is still set
        if (intent == null || !DuaScheduler.ACTION.equals(intent.getAction()) || sa.urtv.home.PartsHome.off(c, "duas")) { DuaScheduler.schedule(c); return; }
        long due = intent.getLongExtra(DuaScheduler.EXTRA_DUE, 0), now = System.currentTimeMillis();
        DuaStore.Dua chosen = null;
        DuaDays days = new DuaDays(c);
        java.util.List<DuaStore.Dua> all = DuaStore.list(c);
        long[] moments = new long[all.size()], last = new long[all.size()];
        for (int i = 0; i < moments.length; i++) {
            DuaStore.Dua d = all.get(i);
            last[i] = d.lastDue;
            // its own moment from SAME_MS before the alarm: the prayer times may have been corrected since the alarm was set (Aladhan's, a new city, «تعديل وقت الصلاة»)
            moments[i] = d.on ? d.rule.next(due - DuaPlan.SAME_MS - 1, days) : 0;
        }
        int k = DuaPlan.pick(moments, last, due);   // only the nearest one is taken; another a minute or two later keeps its own alarm
        if (k >= 0) {
            chosen = all.get(k);
            chosen.lastDue = moments[k];
            if (!chosen.rule.repeat) chosen.on = false;
            DuaStore.put(c, chosen);
        }
        if (chosen != null) {
            AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            String source = DuaStore.source(c, chosen);
            DuaPlan.Go go = DuaPlan.decide(due, now, pm == null || pm.isInteractive(), MusicService.carryAdhanActive(),
                    am != null && am.isMusicActive() && !MusicService.carryOurPlaying(), chosen.overOthers, source != null,
                    new Prefs(c).quietNow(c));   // 4.4 «ساعات الهدوء»
            Log.i("UrTVDua", "dua " + chosen.id + ": " + go);
            if (go == DuaPlan.Go.PLAY) MusicService.dua(c, source, chosen.name, chosen.volume, chosen.fade);
        }
        DuaScheduler.schedule(c);
    }
}
