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
        return ExtrasTrust.call(c, method, arg, extras);   // F88: only when the app that owns the authority is the add-on, signed as this home is; null: not installed, not trusted, or another key: «off»
    }

    /** 4.4 (QA-6, R1): the last REAL answer (null: none yet; a failed call keeps the one before), and when it was asked */
    private static volatile Boolean known;
    private static volatile long askedAt;
    private static final java.util.concurrent.atomic.AtomicBoolean asking = new java.util.concurrent.atomic.AtomicBoolean();
    static final long FRESH_MS = 2000;

    /**
     * The add-on is installed AND its service is on (Android runs it), as last asked: this NEVER calls into the add-on's process (a TCL that killed
     * it would start it here, on the UI thread: the 4.3.3 freeze's shape); it asks again on a thread when the answer is older than FRESH_MS.
     * Null: not known yet. A thread that may wait (the phone page) asks with enabledNow.
     */
    public static Boolean enabledKnown() {
        refresh();
        return known;
    }

    /** off the UI thread only (the phone page's server threads): asks the add-on now */
    public static boolean enabledNow() { return Boolean.TRUE.equals(askNow()); }

    /** enabledKnown, «not known yet» read as off (a screen's line); A11yHeal never reads it so (stuck and listedAndBound want a real answer) */
    public static boolean enabled() { return Boolean.TRUE.equals(enabledKnown()); }

    /** asks again on a thread when the answer is older than FRESH_MS (the home's return and its minute tick call it) */
    static void refresh() {
        if (askedAt != 0 && android.os.SystemClock.elapsedRealtime() - askedAt < FRESH_MS) return;
        if (!asking.compareAndSet(false, true)) return;
        new Thread(() -> { try { askNow(); } finally { asking.set(false); } }, "extras-status").start();
    }

    /** off the UI thread: one call; not installed is a real «off», a failed call keeps the last real answer */
    static Boolean askNow() {
        Context c = app;
        if (c == null) return known;
        Bundle b = call(ExtrasWords.STATUS, null, null);
        if (b != null) known = b.getBoolean(ExtrasWords.ON, false);
        else if (!installed(c)) known = false;
        askedAt = android.os.SystemClock.elapsedRealtime();
        return known;
    }

    /**
     * 4.4: the add-on is installed and the TV lets it draw over other apps (its SYSTEM_ALERT_WINDOW). Asks the add-on: OFF the UI thread only (the
     * kick-off and iqama receivers' threads), never from a screen's thread.
     */
    static boolean canDraw() {
        return Boolean.TRUE.equals(canDrawKnown());
    }

    /** as canDraw, but null when the call failed (the add-on is installed and gave no answer): a failed call is not «cannot draw». Not installed is a real false. */
    static Boolean canDrawKnown() {
        Bundle b = call(ExtrasWords.STATUS, null, null);
        if (b != null) return b.getBoolean(ExtrasWords.CAN_DRAW, false);
        Context c = app;
        return c != null && !installed(c) ? Boolean.FALSE : null;
    }

    /** 4.4: the add-on draws the corner notice over the app in front; false when it cannot (not installed, no permission, the TV refused). Off the UI thread only. */
    static boolean notice(Bundle what) {
        Bundle b = call(ExtrasWords.NOTICE, null, what);
        return b != null && b.getBoolean(ExtrasWords.RESULT, false);
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
        ExtrasTrust.call(c, ExtrasWords.KEY_APPS, line, null);   // no add-on, not trusted, or another key: the home answers its own buttons
    }

    /**
     * 4.4 (step 86 B): whether the person's chosen home is ours (the setup confirmed a switch, «تراجع» not pressed). The add-on's Home watch then brings
     * our home forward even when the TV gave the HOME role back to its own home. Quiet without the add-on.
     */
    public static boolean homeWanted(Context c, boolean wanted) {
        Bundle b = ExtrasTrust.call(c, ExtrasWords.WATCH, wanted ? ExtrasWords.WANTED : ExtrasWords.UNWANTED, null);
        return b != null;   // false with no add-on (or one that is not ours): told again at the next start of the home
    }

    /**
     * 4.4 beta 2 (the tester, build 525: the FIRST Home press after an update of this app opened the stock home): while this app was being replaced, the
     * add-on judged its Home key catch with our home not installed for a moment, turned it off, and judged it again only when our home came to the front.
     * After our replace, the person's choice the add-on holds is told to it again, as it is (WANTED or UNWANTED, never changed here): that makes it judge
     * the catch again, now with our home installed. Works with an add-on that was not updated. Off the UI thread (a call into the add-on).
     * A fixed word back for the log: rearmed, catch-off (the person's switch is off: nothing to arm), no-answer, no-addon.
     */
    static String rearmHomeKey(Context c) {
        if (!ExtrasTrust.ok(c)) return "no-addon";   // not installed, or not ours (F88)
        try {
            Bundle now = ExtrasTrust.call(c, ExtrasWords.WATCH, null, null);
            if (now == null) return "no-answer";
            Bundle again = ExtrasTrust.call(c, ExtrasWords.WATCH, now.getBoolean(ExtrasWords.WANTED) ? ExtrasWords.WANTED : ExtrasWords.UNWANTED, null);
            if (again == null) return "no-answer";
            return again.getBoolean(ExtrasWords.HOME_KEY) ? "rearmed" : "catch-off";
        } catch (RuntimeException e) {
            return "no-addon";   // not installed (unknown authority) or not ours
        }
    }

    /** 4.4 beta 2: whether this app holds the HOME role (Android 10+; RoleManager answers it for the app itself): ExtrasWords.ROLE_OURS, ROLE_NOT or ROLE_UNKNOWN */
    private static final String ROLE_HOME = "android.app.role.HOME";

    static String homeRole(Context c) {
        if (android.os.Build.VERSION.SDK_INT < 29) return ExtrasWords.ROLE_UNKNOWN;
        try {   // RoleManager by name (public API of Android 10; the class is not in the older SDKs the compile guards build against)
            Object rm = c.getSystemService("role");
            if (rm == null) return ExtrasWords.ROLE_UNKNOWN;
            if (!Boolean.TRUE.equals(rm.getClass().getMethod("isRoleAvailable", String.class).invoke(rm, ROLE_HOME))) return ExtrasWords.ROLE_UNKNOWN;
            return Boolean.TRUE.equals(rm.getClass().getMethod("isRoleHeld", String.class).invoke(rm, ROLE_HOME)) ? ExtrasWords.ROLE_OURS : ExtrasWords.ROLE_NOT;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ExtrasWords.ROLE_UNKNOWN;
        }
    }

    /** 4.4 R3 (on by default on a Google TV): the add-on catches the Home key itself; "on", "off" or null to read; null when the add-on did not answer */
    public static Boolean homeKey(Context c, String set) {
        Bundle b = ExtrasTrust.call(c, ExtrasWords.WATCH, "on".equals(set) ? ExtrasWords.HOME_KEY_ON : "off".equals(set) ? ExtrasWords.HOME_KEY_OFF : null, null);
        return b == null ? null : b.getBoolean(ExtrasWords.HOME_KEY);
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
