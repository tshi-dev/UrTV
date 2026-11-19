package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * beta 2 (the owner's TV, build 574): this add-on was updated (MY_PACKAGE_REPLACED, only the system sends it). Its accessibility service is unbound by the update and this
 * TV binds it again only when the home heals it, which used to wait for the home's start: the FIRST Home after the install then opened the stock home. Now the home is
 * asked at once to heal it (ExtrasWords.HEAL), on a thread; the home answers at once and heals after its settle. A fixed word in the log.
 */
public class ExtrasReplaced extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        final Context app = context.getApplicationContext();
        final PendingResult pending = goAsync();
        try {
            new Thread(() -> {
                try {
                    android.os.Bundle b = ExtrasA11yService.askHome(app, ExtrasWords.HEAL, null, null);
                    if (b != null && b.getBoolean(ExtrasWords.RESULT)) Log.i("UrTVWatch", "replaced heal asked");
                    else Log.i("UrTVWatch", "replaced heal no answer");
                } finally {
                    pending.finish();
                }
            }, "extras-replaced").start();
        } catch (RuntimeException e) {
            pending.finish();
        }
    }
}
