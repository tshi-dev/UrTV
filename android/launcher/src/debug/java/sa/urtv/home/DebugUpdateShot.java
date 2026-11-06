package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Debug builds only: `am broadcast -n sa.urtv.home/.DebugUpdateShot --es preview downloading` shows «تحديث تلفزيوني» in that state (available, downloading, waiting_owner, done). */
public class DebugUpdateShot extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        c.startActivity(new Intent(c, UpdateActivity.class).putExtra("preview", i.getStringExtra("preview")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
