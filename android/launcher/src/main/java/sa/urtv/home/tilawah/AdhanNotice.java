package sa.urtv.home.tilawah;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * «تنبيه عند الأذان»: a small text toast for 5 seconds, over whatever is in front. The system draws it, so it needs no permission and does not
 * take the focus or a key (nothing to dismiss, nothing blocked). This app holds no SYSTEM_ALERT_WINDOW and the setup grants none, so an overlay
 * window of ours would show nowhere; a text toast is the one thing Android lets an app show from the background without it (a custom
 * toast view is not). A long toast lasts 3.5 s, so a short one follows and the whole is cut at 5 s.
 */
final class AdhanNotice {
    private AdhanNotice() { }

    /** Shows it now; the main thread (the alarm's receiver). Never throws: a TV that shows no toast loses the notice, not the adhan. */
    static void show(Context c, int prayer) {
        final Context app = c.getApplicationContext();
        sa.urtv.home.S.init(app);   // «اللغة»: the notice's words
        final String text = AdhanNoticeText.text(prayer);
        try {
            final Handler h = new Handler(Looper.getMainLooper());
            final Toast first = Toast.makeText(app, text, Toast.LENGTH_LONG);
            first.show();
            h.postDelayed(() -> {
                try {
                    Toast second = Toast.makeText(app, text, Toast.LENGTH_SHORT);
                    second.show();
                    h.postDelayed(() -> { try { second.cancel(); } catch (RuntimeException ignored) { } }, AdhanNoticeText.SHOW_MS - AdhanNoticeText.FIRST_MS);
                } catch (RuntimeException ignored) { }
            }, AdhanNoticeText.FIRST_MS);
            Log.i("UrTVAdhan", "notice shown: " + text);
        } catch (RuntimeException e) {
            Log.i("UrTVAdhan", "notice could not be shown: " + e.getClass().getSimpleName());
        }
    }
}
