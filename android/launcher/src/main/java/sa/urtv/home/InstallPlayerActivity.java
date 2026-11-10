package sa.urtv.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;

/**
 * 4.3 «تحديث» from inside UrTV Player: sa.urtv.home.action.INSTALL_PLAYER with the extra version_code (int), started FOR A RESULT by the Player
 * itself, so getCallingPackage() says who asks. Accepted only from com.urtv.player signed by exactly its pinned certificate (PlayerUpdate.callerOk),
 * and only for the version the newest verified release says, newer than the installed Player (PlayerUpdate.installOk). Then the same path as the
 * «تطبيقات مقترحة» tile (MirrorInstall.mirror): download, SHA-256 = the published one, the package and the pinned certificate, Android's installer;
 * installed over the Player (its data kept, never uninstalled). RESULT_OK once Android's installer has it; RESULT_CANCELED on any refusal or
 * failure, with our usual words on screen a moment first.
 */
public class InstallPlayerActivity extends Activity {
    public static final String ACTION = "sa.urtv.home.action.INSTALL_PLAYER", EXTRA = "version_code";
    private static final String TAG = "UrTVPlayerUpdate", RESULT = "sa.urtv.home.PLAYER_INSTALL_RESULT";

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private TextView line;
    private boolean registered, done;

    private final BroadcastReceiver result = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {   // Android's own install screen: handed over
                Intent screen = i.getParcelableExtra(Intent.EXTRA_INTENT);
                try {
                    if (screen != null) { startActivity(screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); end(RESULT_OK, null); return; }
                } catch (RuntimeException e) { Log.w(TAG, "confirm: " + e.getClass().getSimpleName()); }
                end(RESULT_CANCELED, S.t("ما قدرنا نثبّته. جرّب مرة ثانية"));
                return;
            }
            if (status == PackageInstaller.STATUS_SUCCESS) end(RESULT_OK, null);
            else end(RESULT_CANCELED, S.t("ما قدرنا نثبّته. جرّب مرة ثانية"));
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xF0101216);
        line = new TextView(this);
        line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        line.setTextColor(0xFFFFFFFF);
        line.setGravity(Gravity.CENTER);
        try { line.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Medium.otf")); } catch (RuntimeException ignored) { }
        frame.addView(line, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        setContentView(frame);
        // who asks: only a start for a result names the caller; only the Player with its own key, only for a version that is really there
        String caller = getCallingPackage();
        final int code = getIntent() == null || !ACTION.equals(getIntent().getAction()) ? 0 : getIntent().getIntExtra(EXTRA, 0);
        if (!PlayerUpdate.callerOk(caller, MirrorInstall.signers(getPackageManager(), caller)) || code <= 0) {
            Log.w(TAG, "refused: " + (caller == null ? "no caller (not started for a result)" : code <= 0 ? "no version_code" : "not the Player"));
            end(RESULT_CANCELED, S.t("ما قدرنا نثبّته. جرّب مرة ثانية"));
            return;
        }
        line.setText(S.t("ينزّل…"));
        InternalReceivers.register(this, result, new IntentFilter(RESULT));
        registered = true;
        new Thread(() -> {
            String why;
            try {
                why = MirrorInstall.mirror(this, SuggestedApps.PLAYER_APP, new File(getCacheDir(), "player-update.apk"), RESULT, code);
            } catch (Exception e) {
                Log.w(TAG, "install: " + e.getClass().getSimpleName());
                why = S.t("ما قدرنا نثبّته. جرّب مرة ثانية");
            }
            final String w = why;
            if (w != null) runOnUiThread(() -> end(RESULT_CANCELED, w));   // else: Android's installer answers through RESULT
        }, "player-install").start();
    }

    /** the result to the Player; a refusal or failure shows its words for a moment first */
    private void end(int code, String words) {
        if (done || isFinishing()) return;
        done = true;
        setResult(code);
        if (words == null) { finish(); return; }
        line.setText(words);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { public void run() { finish(); } }, 2500);
    }

    @Override protected void onDestroy() {
        if (registered) try { unregisterReceiver(result); } catch (RuntimeException ignored) { }
        super.onDestroy();
    }
}
