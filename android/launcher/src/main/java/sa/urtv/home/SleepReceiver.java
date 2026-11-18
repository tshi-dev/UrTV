package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** The sleep timer's alarm went off (SleepTimer set it): the TV goes to sleep. Not exported: only our own alarm reaches it. */
public class SleepReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (!SleepTimer.ACTION.equals(i.getAction())) return;
        // 4.4 review F6: the thread that asks the add-on outlives onReceive; goAsync() keeps the process at receiver priority until it is done
        final PendingResult pr = goAsync();
        try {
            SleepTimer.fire(c, () -> { try { pr.finish(); } catch (RuntimeException ignored) { } });
        } catch (RuntimeException e) {
            pr.finish();
        }
    }
}
