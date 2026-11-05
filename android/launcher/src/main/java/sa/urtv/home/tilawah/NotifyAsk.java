package sa.urtv.home.tilawah;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

/**
 * POST_NOTIFICATIONS, a runtime permission on Android 13+ for an app targeting 33 or more (the home app targets 34). Only the player's own
 * notification (what is playing) and the update's «اكتمل التحديث» need it; the adhan's notice and the reminders are toasts, which need none.
 * Asked once in the app's life, when the player first opens (something was just started here), with one line that says why: never at launch.
 * On 30, Android itself asked at that moment (the player's first channel). A «no» is kept: the playing goes on the same, without the notification.
 */
final class NotifyAsk {
    private NotifyAsk() { }

    static final int REQUEST = 41;

    static void once(Activity a) {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            if (a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
            SharedPreferences sp = a.getSharedPreferences("notify_ask", Context.MODE_PRIVATE);
            if (sp.getBoolean("asked", false)) return;
            sp.edit().putBoolean("asked", true).apply();
            Toast.makeText(a, sa.urtv.home.S.t("اسمح بالإشعارات ليظهر ما يُتلى في إشعارات التلفزيون. وإن رفضت، تبقى التلاوة كما هي."), Toast.LENGTH_LONG).show();
            a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST);
        } catch (RuntimeException ignored) {   // a TV without the permission screen: nothing asked, nothing breaks
        }
    }
}
