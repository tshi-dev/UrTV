package sa.urtv.home;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * «إضافات تلفزيونك»: what the home asks of the add-on (ExtrasWords): status, type, press, sleep, go_home, watch, notice (the corner notice over another app). Calls only (no rows); the manifest
 * guards it with ExtrasWords.PERMISSION, a signature permission: only our home (our key) gets in. Each answer is one fixed word or flag.
 */
public class ExtrasCallProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        if (getContext() == null || method == null) return out;
        switch (method) {
            case ExtrasWords.STATUS:
                out.putBoolean(ExtrasWords.ON, ExtrasA11yService.enabled());
                out.putBoolean(ExtrasWords.WATCH, ExtrasA11yService.watchOn(getContext()));
                out.putBoolean(ExtrasWords.CAN_DRAW, ExtrasNotice.canDraw(getContext()));   // 4.4: the TV lets this app draw over apps (no service needed for that)
                break;
            case ExtrasWords.NOTICE:   // 4.4 «فوق تطبيق آخر»: the corner notice over the app in front
                out.putBoolean(ExtrasWords.RESULT, ExtrasNotice.show(getContext(), extras));
                break;
            case ExtrasWords.TYPE:
                out.putString(ExtrasWords.RESULT, ExtrasA11yService.typeText(getContext(), arg == null ? "" : arg,
                    extras != null && extras.getBoolean(ExtrasWords.APPEND), extras != null && extras.getBoolean(ExtrasWords.SEARCH)));
                break;
            case ExtrasWords.PRESS:
                out.putString(ExtrasWords.RESULT, ExtrasA11yService.press(arg));
                break;
            case ExtrasWords.SLEEP:
                out.putBoolean(ExtrasWords.RESULT, ExtrasA11yService.sleepNow());
                break;
            case ExtrasWords.GO_HOME:
                out.putBoolean(ExtrasWords.RESULT, ExtrasA11yService.goHome());
                break;
            case ExtrasWords.WATCH:
                if ("on".equals(arg) || "off".equals(arg)) ExtrasA11yService.setWatch(getContext(), "on".equals(arg));
                if (ExtrasWords.WANTED.equals(arg) || ExtrasWords.UNWANTED.equals(arg)) ExtrasA11yService.setWanted(getContext(), ExtrasWords.WANTED.equals(arg));   // 4.4
                out.putBoolean(ExtrasWords.WATCH, ExtrasA11yService.watchOn(getContext()));
                out.putBoolean(ExtrasWords.WANTED, ExtrasA11yService.wanted(getContext()));
                if (ExtrasWords.HOME_KEY_ON.equals(arg) || ExtrasWords.HOME_KEY_OFF.equals(arg)) ExtrasA11yService.setHomeKey(getContext(), ExtrasWords.HOME_KEY_ON.equals(arg));   // 4.4 R3
                out.putBoolean(ExtrasWords.HOME_KEY, ExtrasA11yService.homeKey(getContext()));
                break;
            case ExtrasWords.KEY_APPS:   // «أزرار الريموت»: the home's choices changed
                ExtrasA11yService.keyApps(getContext(), arg);
                break;
            default:
                break;
        }
        return out;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
