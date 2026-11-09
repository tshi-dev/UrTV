package sa.urtv.screensaver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.TextView;

/**
 * Step 41 (build 209): while the home app replaces itself, its process and its update screen die with it, and Android brought the app used
 * before (Apple TV on the owner's TCL) to the front for about 5 s before our home came back. This screen belongs to the screensaver app, a
 * package the install does not touch: the home's update screen opens it right under the installer's confirm screen (PhoneUpdater.showConfirm,
 * one startActivities call from the screen in front), so after the replace this is what shows, not the last app.
 *
 * It ends itself (CoverRule decides; nothing waits on the owner):
 * - the home app was replaced: the TV's Home is pressed from here (this screen is in front, so Android lets it start Home) and it STAYS until the new home says it is up (CLOSE, sent by
 *   the home's own start, PhoneUpdater.homeIsUp) or HOME_WAIT_MS passed: closing at once left the app used before on the screen for ~4 s while the new process started (step 44);
 * - the home app says the install was declined or failed (CLOSE, a broadcast only a holder of PHONE_SETTINGS, our home app, can send): it closes, and the update
 *   screen under it says why;
 * - Back, or LIMIT_MS without either: Home if the home app changed since this opened, else it closes.
 */
public class UpdateCover extends Activity {
    static final String HOME_PKG = CoverRule.HOME_PKG;
    static final String CLOSE = "sa.urtv.screensaver.UPDATE_COVER_CLOSE";
    /** the signature permission our home app already holds for «تحكم من الجوال» (same key): no new permission for the cover */
    static final String PERMISSION = "sa.urtv.screensaver.PHONE_SETTINGS";
    /** the longest it stays: an install that never answers must not leave the TV on this screen */
    static final long LIMIT_MS = 120_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long homeAtOpen;
    private boolean ended;

    private final Runnable limit = new Runnable() { @Override public void run() { end(CoverRule.onLimit(homeChanged())); } };

    private final BroadcastReceiver replaced = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String pkg = i.getData() == null ? null : i.getData().getSchemeSpecificPart();
            end(CoverRule.onPackage(pkg, i.getAction()));
        }
    };

    private final BroadcastReceiver close = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { end(CoverRule.onClose()); }
    };

    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(S.wrap(base)); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        homeAtOpen = homeUpdatedAt();
        TextView t = new TextView(this);
        t.setText(S.t("يكمّل التحديث… ترجع الشاشة الرئيسية بعد لحظات"));
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(Color.BLACK);
        t.setLayoutDirection(Dir.layout());
        setContentView(t);
        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addDataScheme("package");
        IntentFilter cf = new IntentFilter(CLOSE);
        registerReceiver(replaced, pf);   // the system's package broadcasts need no flag
        InternalReceivers.registerFromHome(this, close, cf, PERMISSION);   // only our home app (signature permission)
        handler.postDelayed(limit, LIMIT_MS);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { end(CoverRule.onLimit(homeChanged())); return true; }
        return super.onKeyDown(keyCode, e);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(limit);
        handler.removeCallbacks(homeLate);
        try { unregisterReceiver(replaced); } catch (RuntimeException ignored) { }
        try { unregisterReceiver(close); } catch (RuntimeException ignored) { }
        super.onDestroy();
    }

    private boolean homePressed;
    private final Runnable homeLate = new Runnable() { @Override public void run() { end(CoverRule.CLOSE); } };

    private void end(int what) {
        if (ended || what == CoverRule.STAY) return;
        if (what == CoverRule.HOME_WAIT) {   // the new home is installed: press Home once and STAY in front until it says it is up (the app used before never shows)
            if (homePressed) return;
            homePressed = true;
            try {
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException ignored) { }
            handler.removeCallbacks(limit);
            handler.postDelayed(homeLate, CoverRule.HOME_WAIT_MS);   // it never waits for ever
            return;
        }
        ended = true;
        if (what == CoverRule.HOME) {
            try {
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException ignored) { }
        }
        finish();
    }

    private boolean homeChanged() { long now = homeUpdatedAt(); return now > 0 && now != homeAtOpen; }

    private long homeUpdatedAt() {
        try { return getPackageManager().getPackageInfo(HOME_PKG, 0).lastUpdateTime; } catch (Exception e) { return 0; }
    }
}
