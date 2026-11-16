package sa.urtv.home;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;

/**
 * 4.3.3 «إضافات تلفزيونك»: what the add-on asks the home (ExtrasWords): the family lock's answer for the app in front before it types (type_check), whether
 * the TV's settings are behind the PIN for the remote's Settings key (settings_pin), and the Settings key itself when this TV's system keeps it from our
 * home (key: our own key path, RemoteKeys). Calls only; the manifest guards it with ExtrasWords.PERMISSION (signature: only the add-on, our key).
 */
public class ExtrasBridge extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        android.content.Context c = getContext();
        if (c == null || method == null) return out;
        switch (method) {
            case ExtrasWords.TYPE_CHECK: {   // the same three answers the home's own TypeService asked before 4.3.3
                String pkg = arg;
                ParentalPolicy.State st = Parental.state(c);
                out.putBoolean(ExtrasWords.NEEDS_PIN, pkg == null || Parental.needsPin(c, pkg));
                out.putBoolean(ExtrasWords.HIDDEN, pkg == null || !ParentalPolicy.visible(st, pkg));
                out.putBoolean(ExtrasWords.SYSTEM_SCREEN, pkg == null || (Parental.needsPin(c, ParentalPolicy.TV_SETTINGS) && pkg.equals(packageOf(c, Settings.ACTION_SETTINGS)))
                    || (Parental.needsPin(c, ParentalPolicy.WIFI) && pkg.equals(packageOf(c, Settings.ACTION_WIFI_SETTINGS))));
                break;
            }
            case ExtrasWords.SETTINGS_PIN:
                out.putBoolean(ExtrasWords.NEEDS_PIN, Parental.needsPin(c, ParentalPolicy.targets(ParentalPolicy.REMOTE_SETTINGS, arg)));
                break;
            case ExtrasWords.KEY: {
                if (extras == null) break;
                final KeyEvent e = new KeyEvent(extras.getLong(ExtrasWords.DOWN_TIME), extras.getLong(ExtrasWords.EVENT_TIME), extras.getInt(ExtrasWords.ACTION),
                    extras.getInt(ExtrasWords.CODE), extras.getInt(ExtrasWords.REPEAT), extras.getInt(ExtrasWords.META), extras.getInt(ExtrasWords.DEVICE),
                    extras.getInt(ExtrasWords.SCAN), 0, extras.getInt(ExtrasWords.SOURCE));
                if (SettingsKey.isSettingsKey(e.getKeyCode())) new Handler(Looper.getMainLooper()).post(() -> MainActivity.keyFromService(e));   // the Settings key only
                break;
            }
            case ExtrasWords.LEDGER:   // 4.4: «إنقاذ تلفزيونك» asks what our setup turned off (it turns those back on)
                out.putString(ExtrasWords.RESULT, sa.urtv.home.setup.SetupHook.ledger(c));
                break;
            default:
                break;
        }
        return out;
    }

    private static String packageOf(android.content.Context c, String action) {
        ResolveInfo ri = c.getPackageManager().resolveActivity(new Intent(action), 0);
        return ri == null || ri.activityInfo == null ? null : ri.activityInfo.packageName;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
