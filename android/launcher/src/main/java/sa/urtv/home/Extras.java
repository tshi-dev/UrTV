package sa.urtv.home;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

/**
 * 4.3.3 «إضافات تلفزيونك» (UrTV Extras): the home has no accessibility service any more (Play Protect blocked the home for them, «بيانات حساسة»); what one
 * can do (the sleep timer's screen-off, the phone's keys and typing in other apps, the Settings key outside our home, the Home-button watch on a Google TV)
 * is in the add-on sa.urtv.extras, asked here through its provider (ExtrasWords). Without the add-on, or with its service off, every call answers «off»
 * and the screen that asked offers the add-on (ExtrasActivity). Each call is short; a call that fails is «off», never an error.
 */
public final class Extras {
    private Extras() { }

    private static volatile Context app;

    static void init(Context c) { app = c.getApplicationContext(); }

    /** the add-on is installed */
    public static boolean installed(Context c) {
        try { c.getPackageManager().getPackageInfo(ExtrasWords.PACKAGE, 0); return true; } catch (PackageManager.NameNotFoundException | RuntimeException e) { return false; }
    }

    private static Bundle call(String method, String arg, Bundle extras) {
        Context c = app;
        if (c == null) return null;
        try { return c.getContentResolver().call(Uri.parse("content://" + ExtrasWords.EXTRAS_AUTHORITY), method, arg, extras); }
        catch (RuntimeException e) { return null; }   // not installed (unknown authority), or another key: «off»
    }

    private static volatile long askedAt;
    private static volatile boolean lastOn;

    /** the add-on is installed AND its service is on (Android runs it); asked again at most every 2 s (the screens ask it while they draw) */
    public static boolean enabled() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (askedAt != 0 && now - askedAt < 2000) return lastOn;
        Bundle b = call(ExtrasWords.STATUS, null, null);
        lastOn = b != null && b.getBoolean(ExtrasWords.ON, false);
        askedAt = now;
        return lastOn;
    }

    /** the sleep timer's end: the screen off; false when the add-on cannot (not installed, its service off, Android older than 9) */
    static boolean sleepNow() {
        Bundle b = call(ExtrasWords.SLEEP, null, null);
        return b != null && b.getBoolean(ExtrasWords.RESULT, false);
    }

    /** Home pressed for the person (UpdateCarry) */
    static boolean goHome() {
        Bundle b = call(ExtrasWords.GO_HOME, null, null);
        return b != null && b.getBoolean(ExtrasWords.RESULT, false);
    }

    /** a phone key into the app in front: one of RemoteNav's words (service_off without the add-on) */
    static String press(String key) {
        Bundle b = call(ExtrasWords.PRESS, key, null);
        String r = b == null ? null : b.getString(ExtrasWords.RESULT);
        return r == null ? RemoteNav.OFF : r;
    }

    /** the phone's text into the app in front: one of ExtrasWords' words (service_off without the add-on) */
    static String typeText(String text, boolean append, boolean search) {
        Bundle x = new Bundle();
        x.putBoolean(ExtrasWords.APPEND, append);
        x.putBoolean(ExtrasWords.SEARCH, search);
        Bundle b = call(ExtrasWords.TYPE, text, x);
        String r = b == null ? null : b.getString(ExtrasWords.RESULT);
        return r == null ? ExtrasWords.OFF : r;
    }

    /** «أزرار الريموت»: the owner's choices to the add-on (it filters keys over other apps only for a button given an app); quiet without the add-on */
    static void keyApps(Context c, String line) {
        try { c.getContentResolver().call(Uri.parse("content://" + ExtrasWords.EXTRAS_AUTHORITY), ExtrasWords.KEY_APPS, line, null); }
        catch (RuntimeException e) { /* no add-on, or another key: the home answers its own buttons */ }
    }

    /**
     * 4.4 (step 86 B): whether the person's chosen home is ours (the setup confirmed a switch, «تراجع» not pressed). The add-on's Home watch then brings
     * our home forward even when the TV gave the HOME role back to its own home. Quiet without the add-on.
     */
    public static boolean homeWanted(Context c, boolean wanted) {
        try {
            Bundle b = c.getContentResolver().call(Uri.parse("content://" + ExtrasWords.EXTRAS_AUTHORITY), ExtrasWords.WATCH, wanted ? ExtrasWords.WANTED : ExtrasWords.UNWANTED, null);
            return b != null;
        } catch (RuntimeException e) {
            return false;   // no add-on: told again at the next start of the home
        }
    }

    /** 4.4 R3 (on by default on a Google TV): the add-on catches the Home key itself; "on", "off" or null to read; null when the add-on did not answer */
    public static Boolean homeKey(Context c, String set) {
        try {
            Bundle b = c.getContentResolver().call(Uri.parse("content://" + ExtrasWords.EXTRAS_AUTHORITY), ExtrasWords.WATCH,
                "on".equals(set) ? ExtrasWords.HOME_KEY_ON : "off".equals(set) ? ExtrasWords.HOME_KEY_OFF : null, null);
            return b == null ? null : b.getBoolean(ExtrasWords.HOME_KEY);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** the Home-button watch: "on", "off", or null to only read it; null back when the add-on did not answer */
    static Boolean watch(String set) {
        Bundle b = call(ExtrasWords.WATCH, set, null);
        return b == null ? null : b.getBoolean(ExtrasWords.WATCH);
    }

    /** the TV's settings, the first of SettingsKey.TV_SETTINGS_ACTIONS this TV opens (the Settings key's way, also for the PIN screen) */
    static Intent tvSettingsIntent(Context c) {
        for (String a : SettingsKey.TV_SETTINGS_ACTIONS) {
            Intent i = new Intent(a);
            if (c.getPackageManager().resolveActivity(i, 0) != null) return i;
        }
        return new Intent(SettingsKey.TV_SETTINGS_ACTIONS[SettingsKey.TV_SETTINGS_ACTIONS.length - 1]);
    }

    /** A feature that needs the add-on was asked for and it cannot act: the offer (install it, or turn its service on). From an Activity or the app. */
    static void offer(Context c) {
        offer(c, null);
    }

    /** the same, naming what asked: "sleep", "type" or "keys" */
    static void offer(Context c, String what) {
        try { c.startActivity(intent(c, what).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (RuntimeException ignored) { }
    }

    private static volatile long lastOffer;

    /** from the phone's keys and typing: the offer on the TV, at most once in two minutes (a phone presses many keys) */
    static void offerOnce(String what) {
        Context c = app;
        long now = System.currentTimeMillis();
        if (c == null || now - lastOffer < 120_000) return;
        lastOffer = now;
        offer(c, what);
    }

    static Intent intent(Context c, String what) {
        Intent i = new Intent(c, ExtrasActivity.class);
        if (what != null) i.putExtra(ExtrasActivity.EXTRA_FOR, what);
        return i;
    }
}
