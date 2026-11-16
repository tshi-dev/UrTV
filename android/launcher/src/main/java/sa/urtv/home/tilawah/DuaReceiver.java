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
        if (intent == null || !DuaScheduler.ACTION.equals(intent.getAction())) { DuaScheduler.schedule(c); return; }
        long due = intent.getLongExtra(DuaScheduler.EXTRA_DUE, 0), now = System.currentTimeMillis();
        DuaStore.Dua chosen = null;
        DuaDays days = new DuaDays(c);
        for (DuaStore.Dua d : DuaStore.list(c)) {
            if (!d.on || d.lastDue >= due) continue;
            long t = d.rule.next(due - 1, days);   // its own moment is this alarm's
            if (t != due) continue;
            d.lastDue = due;
            if (!d.rule.repeat) d.on = false;
            DuaStore.put(c, d);
            if (chosen == null) chosen = d;   // two at the same minute: the first in the list plays
        }
        if (chosen != null) {
            AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            String source = DuaStore.source(c, chosen);
            DuaPlan.Go go = DuaPlan.decide(due, now, pm == null || pm.isInteractive(), MusicService.carryAdhanActive(),
                    am != null && am.isMusicActive() && !MusicService.carryOurPlaying(), chosen.overOthers, source != null);
            Log.i("UrTVDua", "dua " + chosen.id + ": " + go);
            if (go == DuaPlan.Go.PLAY) MusicService.dua(c, source, chosen.name, chosen.volume);
        }
        DuaScheduler.schedule(c);
    }
}
