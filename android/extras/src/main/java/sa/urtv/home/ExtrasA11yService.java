package sa.urtv.home;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * «إضافات تلفزيونك» (UrTV Extras, 4.3.3): the one accessibility service of the add-on. Until 4.3.2 the home held two (SleepService, TypeService) and Play
 * Protect blocked the home for them («بيانات حساسة»); the home now has none, and asks this app, through ExtrasCallProvider, for what only such a
 * service can do. What it does, and nothing else:
 *
 * - the sleep timer's end: the screen off like the power key (GLOBAL_ACTION_LOCK_SCREEN, Android 9+);
 * - the phone remote's keys in the app in front (Back, Home, the arrows and OK: global actions, or a focus move before Android 13) and its typing into
 *   that app's text field: the screen is looked at only at that moment, only to find the field, and nothing of it is kept, logged or sent back; never a
 *   password field, never an app the family lock keeps (the home says, HOME_AUTHORITY);
 * - the remote's Settings key outside our home (SettingsKey, GitHub #37): key events are asked for only while it could act; every other key goes back at once;
 * - «أزرار الريموت» over other apps (KeyApps, RemoteButtons; GitHub #59): only while some button of this remote was given an app, and never on TCL's
 *   Android 11 (any key filter doubles its volume steps there); a chosen button's press opens its app (the family lock as its tile, the home asks), its
 *   repeats and release are eaten; every other key, and a button on its default, goes back at once, untouched;
 * - the Home button on a Google TV (HomeWatch): of the window that comes to the front it reads its package and class only, to bring our home back when
 *   the stock home's HOME screen comes up while our home is the chosen one.
 *
 * The log (UrTVType, RemoteTrace's line, UrTVWatch) has fixed words only: never a text, a package name or anything of the screen.
 */
public class ExtrasA11yService extends AccessibilityService {
    private static volatile ExtrasA11yService running;
    static final String TAG = "UrTVType";
    private static final String HOME_PKG = "sa.urtv.home";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final SettingsKey settingsKey = new SettingsKey();
    private final HomeWatch watch = new HomeWatch();
    /** 4.4 R2g: the last app in front that is not a stock home (to tell a Home press over an app from a fight over ours), the one Home was pressed over, our last bring */
    private String prevPkg, pressedOver;
    private long broughtAt;
    private boolean broughtAgain;
    /** our home is in front (from the windows that come to the front) */
    private volatile boolean homeFront;
    /** a stock home answers HOME (it then answers the Settings key itself) */
    private volatile boolean stockHome;
    private volatile Boolean homeKeyLost;
    private Set<String> stockHomes = new HashSet<>();
    private final KeyApps.Outside keyApps = new KeyApps.Outside();
    /** «أزرار الريموت»: the owner's choices (the home tells us when they change, and we ask it when we start); kept so a restart needs no home */
    private volatile java.util.Map<String, String> keyChoices = new java.util.HashMap<>();
    /** 4.4: the stock homes' packages (only their windows are looked at further) */
    private Set<String> stockPackages = new HashSet<>();

    @Override protected void onServiceConnected() {
        running = this;
        watchReplaced();   // beta 2: our home installed again (an update) re-arms the Home key catch and the watch
        roleAnswer = getSharedPreferences("extras", MODE_PRIVATE).getString(ROLE_SEEN, null);   // beta 2 (build 574): the last answer at once, so the catch is right before the home answers
        refreshRole();   // beta 2: whether our home holds the HOME role (the resolver may name Google TV's home while it does)
        stockHomes = stockHomeActivities(this);
        keyChoices = KeyApps.parse(getSharedPreferences("extras", MODE_PRIVATE).getString(ExtrasWords.KEY_APPS, ""));
        stockPackages = HomeWatch.packages(stockHomes);
        watchLog(watchOn(this) ? (wanted(this) ? 1 : 2) : 3);
        watchLog(stockHomes.isEmpty() ? 4 : 5);
        filterKeys();
        new Thread(() -> {   // the home's choices now (it may have changed them while we were off); quiet when it does not answer
            Bundle b = askHome(this, ExtrasWords.KEY_APPS, null, null);
            String line = b == null ? null : b.getString(ExtrasWords.RESULT);
            if (line != null) keyApps(this, line);
        }, "extras-key-apps").start();
    }

    /** «أزرار الريموت»: the home's choices ("youtube=PKG,netflix=,…"), kept and applied (the filter on or off) */
    static void keyApps(Context c, String line) {
        java.util.Map<String, String> m = KeyApps.parse(line);
        c.getSharedPreferences("extras", MODE_PRIVATE).edit().putString(ExtrasWords.KEY_APPS, KeyApps.line(m)).apply();
        ExtrasA11yService s = running;
        if (s == null) return;
        s.keyChoices = m;
        s.main.post(s::filterKeys);
    }
    @Override public boolean onUnbind(Intent i) {
        running = null;
        if (replaced != null) { try { unregisterReceiver(replaced); } catch (RuntimeException ignored) { } replaced = null; }
        return super.onUnbind(i);
    }

    /**
     * 4.4 beta 2 (the tester, build 525: after an update of our home the FIRST Home press opened the stock home; after UrTV ran once, 3 of 3 ours): the
     * key catch is judged when our home's window comes and goes, and while our package was being replaced Android's HOME was not ours for a moment, so
     * the catch went off and nothing judged it again until our home came to the front. Now our home's install (ADDED or REPLACED of sa.urtv.home)
     * lets the watch's pause go and judges the catch again, a moment later (the install's own windows first). Fixed words in the log only.
     */
    private android.content.BroadcastReceiver replaced;

    private void watchReplaced() {
        if (replaced != null) return;
        replaced = new android.content.BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                android.net.Uri d = i == null ? null : i.getData();
                if (d == null || !HOME_PKG.equals(d.getSchemeSpecificPart())) return;
                Log.i("UrTVWatch", "home installed again catch judged again");
                watch.reset();
                main.removeCallbacks(rearm);
                main.postDelayed(rearm, REARM_MS);
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        f.addDataSchemeSpecificPart(HOME_PKG, android.os.PatternMatcher.PATTERN_LITERAL);
        try { registerReceiver(replaced, f); } catch (RuntimeException e) { replaced = null; Log.i("UrTVWatch", "home install watch failed"); }
    }

    /** after our home's install: the windows of the install have come and gone */
    static final long REARM_MS = 1_500;
    private final Runnable rearm = () -> { filterKeys(); refreshRole(); };

    /** beta 2: our home's answer to «do you hold the HOME role» (ExtrasWords.ROLE_*), asked off the main thread; null until it answered */
    private volatile String roleAnswer;
    private volatile long roleAskedAt;
    private final java.util.concurrent.atomic.AtomicBoolean roleAsking = new java.util.concurrent.atomic.AtomicBoolean();
    static final long ROLE_REASK_MS = 2_000;
    static final String ROLE_SEEN = "role_seen";

    /** asks our home on a thread of its own; a changed answer judges the catch again on the main thread */
    private void refreshRole() {
        if (!roleAsking.compareAndSet(false, true)) return;
        roleAskedAt = android.os.SystemClock.uptimeMillis();
        try {
            new Thread(() -> {
                try {
                    Bundle b = askHome(this, ExtrasWords.HOME_ROLE, null, null);
                    String r = b == null ? null : b.getString(ExtrasWords.RESULT);
                    if (r == null) return;   // an older home, or no answer: the last answer stays
                    boolean changed = !r.equals(roleAnswer);
                    roleAnswer = r;
                    getSharedPreferences("extras", MODE_PRIVATE).edit().putString(ROLE_SEEN, r).apply();   // kept for the next connect (an update of this add-on)
                    if (!changed) return;
                    if (ExtrasWords.ROLE_OURS.equals(r)) Log.i("UrTVWatch", "role ours");
                    else Log.i("UrTVWatch", "role not ours");
                    main.post(this::filterKeys);
                } finally {
                    roleAsking.set(false);
                }
            }, "extras-role").start();
        } catch (RuntimeException e) {
            roleAsking.set(false);
        }
    }
    @Override public void onInterrupt() { }

    static boolean enabled() { return running != null; }

    // ---------------------------------------------------------------- the window in front: our home or not, and the Home-button watch

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString(), cls = e.getClassName() == null ? null : e.getClassName().toString();
        if (pkg.equals("com.android.systemui") || pkg.equals("android")) return;   // a volume bar or a system dialog over the app: the app in front did not change
        String before = prevPkg;   // 4.4 R2g: the app in front before this window (kept in memory only, never logged)
        if (!stockPackages.contains(pkg)) prevPkg = pkg;
        long t = System.currentTimeMillis();
        if (!pkg.equals(HOME_PKG) && !stockPackages.contains(pkg) && HomeWatch.bringAgain(pkg, pressedOver, broughtAt, broughtAgain, t)) {
            broughtAgain = true;   // the app Home was pressed over put its own screen back right after our bring (YouTube): ours once more, once
            bringOurs(22);
            return;
        }
        boolean front = pkg.equals(HOME_PKG);
        if (front != homeFront) {
            homeFront = front;
            if (!front) keyApps.leftHome(android.os.SystemClock.uptimeMillis(), KeyButton.burstMs());   // Xiaomi: the rest of the press our home answered
            filterKeys();
        }
        if (!stockPackages.contains(pkg)) return;   // any other app's window: nothing read further, nothing logged
        if (!homeKeyFiltering() && homeKeyActive()) filterKeys();   // beta 2: the catch went off while ours was being installed; on again for the next press
        if (android.os.SystemClock.uptimeMillis() - roleAskedAt > ROLE_REASK_MS) refreshRole();   // beta 2: the role asked again (a thread), for the next press
        // 4.4 (step 86 B): every decision on a stock home's window is logged (the stock home's own class only), so a tester sees why. The watch
        // brings ours while the person's home is ours: Android's default (the role), or the setup's word (WANTED) when the TV took the role back
        boolean on = watchOn(this), chosen = oursChosen(), wantedNow = wanted(this), homeScreen = HomeWatch.homeScreen(pkg, cls, stockHomes);
        long now = System.currentTimeMillis();
        boolean fight = before == null || before.equals(HOME_PKG);   // 4.4 R2g: the stock home over OUR home is a fight; over another app, the person's press
        boolean bring = homeScreen && watch.decide(on, pkg, cls, stockHomes, chosen || wantedNow, now, fight);
        watchLog(fight ? 23 : 24);
        // fixed words only (the log never names a package or a class): which rule matched, then the decision
        watchLog(stockHomes.contains(pkg + "/" + cls) ? 6 : homeScreen ? 7 : 8);
        watchLog(chosen ? (wantedNow ? 9 : 10) : (wantedNow ? 11 : 12));
        watchLog(bring ? 13 : !homeScreen ? 14 : !on ? 15 : !(chosen || wantedNow) ? 16 : watch.paused(now) ? 17 : 18);
        if (bring && bringOurs(21)) {
            broughtAt = System.currentTimeMillis();
            broughtAgain = false;
            pressedOver = before != null && !before.equals(HOME_PKG) ? before : null;
        }
    }

    /** our home in front; {@code said}: the watchLog line on success */
    private boolean bringOurs(int said) {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(new ComponentName(HOME_PKG, HOME_PKG + ".MainActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            watchLog(said);
            return true;
        } catch (RuntimeException ex) {
            Log.i("UrTVWatch", "home=failed");
            return false;
        }
    }

    /**
     * 4.4 (step 86 B): the watch's decisions for the tester, in fixed words only (never a package or a class name of any window): `logcat -s UrTVWatch`.
     */
    private static void watchLog(int what) {
        switch (what) {
            case 1: Log.i("UrTVWatch", "service on watch on wanted yes"); break;
            case 2: Log.i("UrTVWatch", "service on watch on wanted no"); break;
            case 3: Log.i("UrTVWatch", "service on watch off"); break;
            case 4: Log.i("UrTVWatch", "stock home none"); break;
            case 5: Log.i("UrTVWatch", "stock home found"); break;
            case 6: Log.i("UrTVWatch", "stock window its home activity"); break;
            case 7: Log.i("UrTVWatch", "stock window a home activity by name"); break;
            case 8: Log.i("UrTVWatch", "stock window another screen"); break;
            case 9: Log.i("UrTVWatch", "role ours wanted yes"); break;
            case 10: Log.i("UrTVWatch", "role ours wanted no"); break;
            case 11: Log.i("UrTVWatch", "role stock wanted yes"); break;
            case 12: Log.i("UrTVWatch", "role stock wanted no"); break;
            case 13: Log.i("UrTVWatch", "decision bring ours"); break;
            case 14: Log.i("UrTVWatch", "decision not its home screen"); break;
            case 15: Log.i("UrTVWatch", "decision watch off"); break;
            case 16: Log.i("UrTVWatch", "decision ours not chosen"); break;
            case 17: Log.i("UrTVWatch", "decision paused"); break;
            case 18: Log.i("UrTVWatch", "decision left"); break;
            case 19: Log.i("UrTVWatch", "wanted=yes"); break;
            case 21: Log.i("UrTVWatch", "home=brought"); break;
            case 22: Log.i("UrTVWatch", "the app came back after our bring brought again"); break;
            case 23: Log.i("UrTVWatch", "came over ours counted"); break;
            case 24: Log.i("UrTVWatch", "came over an app a press not counted"); break;
            default: Log.i("UrTVWatch", "wanted=no"); break;
        }
    }

    // ---------------------------------------------------------------- 4.4 R3: the Home key caught here (on by default on a Google TV from build 413's T9)

    /**
     * 4.4 (build 413, T9 with the owner's real remote: 3 of 3 presses caught, long press and Home in a YouTube video too, Google TV never started, the
     * Settings and Assistant keys untouched): ON by default on a TV whose stock home is Google TV's (launcherx), off elsewhere; the person's switch
     * («زر الرئيسية يفتح تلفزيونك مباشرة», or the home's DockReceiver HOME_KEY over adb) wins once set. While on, the Home key's events are ours only
     * when the person's home is ours (homeKeyActive): its release opens our home and the TV's own Home never sees it. The window watch stays as the
     * fallback for remotes and TVs whose Home never reaches the filter (adb's own key presses never do).
     */
    static boolean homeKey(Context c) {
        SharedPreferences sp = c.getSharedPreferences("extras", MODE_PRIVATE);
        if (sp.contains(ExtrasWords.HOME_KEY)) return sp.getBoolean(ExtrasWords.HOME_KEY, false);
        return installed(c, "com.google.android.apps.tv.launcherx");
    }

    /** the catch acts: on, and the person's home is ours (the setup's word, or Android's default home); never over a home the person kept */
    private boolean homeKeyActive() { return homeKey(this) && (wanted(this) || oursChosen()); }

    /** whether Android sends us the keys now (the catch can act) */
    private boolean homeKeyFiltering() {
        AccessibilityServiceInfo info = getServiceInfo();
        return info != null && (info.flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0;
    }

    static void setHomeKey(Context c, boolean on) {
        c.getSharedPreferences("extras", MODE_PRIVATE).edit().putBoolean(ExtrasWords.HOME_KEY, on).apply();
        if (on) Log.i("UrTVWatch", "home key catch on");
        else Log.i("UrTVWatch", "home key catch off");
        ExtrasA11yService s = running;
        if (s != null) s.main.post(s::filterKeys);
    }

    private boolean homeKeyCaught(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_UP) return true;
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(new ComponentName(HOME_PKG, HOME_PKG + ".MainActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            Log.i("UrTVWatch", "home key caught ours opened");
        } catch (RuntimeException ex) {
            Log.i("UrTVWatch", "home key caught failed");
        }
        return true;
    }

    /** 4.4: the setup's word that the person's home is ours (WANTED from the home); off by «تراجع» (UNWANTED) */
    static boolean wanted(Context c) { return c.getSharedPreferences("extras", MODE_PRIVATE).getBoolean(ExtrasWords.WANTED, false); }

    static void setWanted(Context c, boolean on) {
        c.getSharedPreferences("extras", MODE_PRIVATE).edit().putBoolean(ExtrasWords.WANTED, on).apply();
        watchLog(on ? 19 : 20);
        ExtrasA11yService s = running;
        if (s != null) s.main.post(s::filterKeys);   // the Home key catch follows the person's home
    }

    /** the watch's switch (the home turns it with WATCH; on by default on a TV whose stock home is Google TV's) */
    static boolean watchOn(Context c) {
        SharedPreferences sp = c.getSharedPreferences("extras", MODE_PRIVATE);
        if (sp.contains("watch")) return sp.getBoolean("watch", false);
        return installed(c, "com.google.android.apps.tv.launcherx");
    }

    static void setWatch(Context c, boolean on) { c.getSharedPreferences("extras", MODE_PRIVATE).edit().putBoolean("watch", on).apply(); }

    private boolean oursChosen() {
        try {
            ResolveInfo ri = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
            return HomeWatch.oursChosen(ri == null || ri.activityInfo == null ? null : ri.activityInfo.packageName, roleAnswer, HOME_PKG);
        } catch (RuntimeException e) {
            return ExtrasWords.ROLE_OURS.equals(roleAnswer);
        }
    }

    /** the stock homes' HOME activities, "package/class" (not ours, not the setup wizard, not a settings app's fallback) */
    private static Set<String> stockHomeActivities(Context c) {
        Set<String> out = new HashSet<>();
        try {
            List<String> pkgs = new ArrayList<>();
            for (ResolveInfo ri : c.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)) {
                if (ri.activityInfo == null) continue;
                pkgs.add(ri.activityInfo.packageName);
                if (SettingsKey.stockHomeOn(java.util.Collections.singletonList(ri.activityInfo.packageName), HOME_PKG)) {
                    out.add(ri.activityInfo.packageName + "/" + ri.activityInfo.name);
                    // 4.4: HOME often answers through an activity-alias; the window that comes to the front is named by the real activity
                    if (ri.activityInfo.targetActivity != null) out.add(ri.activityInfo.packageName + "/" + ri.activityInfo.targetActivity);
                }
            }
        } catch (RuntimeException ignored) { }
        return out;
    }

    private static boolean stockHomeOn(Context c) { return !stockHomeActivities(c).isEmpty(); }

    private static boolean installed(Context c, String pkg) {
        try { c.getPackageManager().getPackageInfo(pkg, 0); return true; } catch (PackageManager.NameNotFoundException e) { return false; }
    }

    // ---------------------------------------------------------------- the remote's Settings key outside our home (SettingsKey)

    private void filterKeys() {
        try {
            boolean front = homeFront, lowRam = Ram.low(this);
            if (!front && !lowRam) stockHome = stockHomeOn(this);   // one package query, only when the home leaves (none on a weak TV: no filter there)
            if (homeKeyLost == null) homeKeyLost = homeKeyLost(this);
            boolean want = Build.VERSION.SDK_INT >= SettingsKey.MIN_FILTER_SDK && (   // 4.3.6: no key filter at all below Android 9 (a Percee OTT box on 8.0 froze)
                SettingsKey.filterWanted(front, stockHome, lowRam, homeKeyLost, Build.VERSION.SDK_INT)
                || KeyApps.filterWanted(front, KeyApps.anyChosen(keyChoices), KeyButton.filterSafe())   // «أزرار الريموت»: only for a button given an app
                || (homeKeyActive() && KeyButton.filterSafe()));   // 4.4 R3: the Home key caught here (on by default on a Google TV, only while the person's home is ours); never where any filter breaks the remote (Xiaomi, TCL's Android 11)
            AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) return;
            boolean has = (info.flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0;
            if (has == want) return;
            info.flags = want ? info.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS : info.flags & ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
            if (want) log("settings key=watch"); else log("settings key=off");
        } catch (RuntimeException e) {
            log("settings key=failed");
        }
    }

    static boolean homeKeyLost(Context c) {
        try {
            if (!SettingsKey.homeKeyLostApplies(Build.VERSION.SDK_INT, c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK))) return false;   // 4.3.6: only an Android TV of 11 or newer (the panel action is Android 11's)
            return c.getPackageManager().queryIntentActivities(new Intent(SettingsKey.NOTIFICATION_PANEL_ACTION), 0).isEmpty();
        }
        catch (RuntimeException e) { return false; }
    }

    @Override protected boolean onKeyEvent(KeyEvent e) {
        try {   // 4.3.6: never throws into Android's key dispatch; anything unexpected gives the key back untouched
            return keyEvent(e);
        } catch (Throwable t) {
            log("settings key=error");   // one fixed word: never the key or the exception's text
            return false;
        }
    }

    private boolean keyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_HOME && homeKeyActive()) return homeKeyCaught(e);   // 4.4 R3: on by default on a Google TV, the person's home ours
        if (appKey(e)) return true;   // «أزرار الريموت»: a button the owner gave an app; every other key goes on below, untouched
        Boolean lost = homeKeyLost;
        int d = settingsKey.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), homeFront, stockHome, lost != null && lost);
        if (d == SettingsKey.PASS) return false;
        if (d == SettingsKey.TO_HOME) {   // the home's own key path (a short press the TV's settings, a long one ours)
            final Bundle b = new Bundle();
            b.putInt(ExtrasWords.CODE, e.getKeyCode()); b.putInt(ExtrasWords.ACTION, e.getAction()); b.putInt(ExtrasWords.REPEAT, e.getRepeatCount());
            b.putLong(ExtrasWords.DOWN_TIME, e.getDownTime()); b.putLong(ExtrasWords.EVENT_TIME, e.getEventTime()); b.putInt(ExtrasWords.SOURCE, e.getSource()); b.putInt(ExtrasWords.DEVICE, e.getDeviceId());   // the Settings key only (TO_HOME)
            new Thread(() -> askHome(this, ExtrasWords.KEY, null, b), "extras-key").start();
            return true;
        }
        if (d == SettingsKey.OPEN_TV_SETTINGS) new Thread(this::openTvSettings, "extras-settings").start();
        return true;
    }

    // ---------------------------------------------------------------- «أزرار الريموت» over other apps (KeyApps)

    /** true: a button given an app that is on this TV, answered here (its press opens the app, its repeats and release are eaten); false: as before */
    private boolean appKey(KeyEvent e) {
        if (!KeyButton.filterSafe()) return false;   // TCL's Android 11: never (the filter is not asked for there either)
        String id = KeyButton.of(e);
        if (id == null) return false;   // every other key: untouched, nothing read or kept
        String choice = keyChoices.get(id);
        if (KeyApps.chosen(choice) && e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0 && launchIntent(this, choice) == null) choice = "";   // its app was removed: the button's own again (asked once a press)
        int k = keyApps.decide(id, e.getKeyCode(), e.getAction(), e.getRepeatCount(), e.getEventTime(), homeFront, choice, KeyButton.burstMs());
        if (k == KeyApps.OPEN) {
            final String pkg = keyApps.pkg;
            new Thread(() -> openKeyApp(pkg), "extras-key-app").start();
        }
        return k != KeyApps.PASS;
    }

    private static Intent launchIntent(Context c, String pkg) {
        try {
            Intent i = c.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            return i != null ? i : c.getPackageManager().getLaunchIntentForPackage(pkg);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * The app over the app in front. Kids mode's hidden apps stay closed; an app behind the family PIN opens our home, which asks the PIN (as for its tile)
     * and then opens it; the home not answering: the home too, never the app unasked.
     */
    private void openKeyApp(String pkg) {
        Bundle r = askHome(this, ExtrasWords.KEY_APP_CHECK, pkg, null);
        try {
            if (r != null && r.getBoolean(ExtrasWords.HIDDEN, true)) { log("key app=hidden"); return; }
            if (r == null || r.getBoolean(ExtrasWords.NEEDS_PIN, true)) {
                startActivity(new Intent().setClassName(HOME_PKG, HOME_PKG + ".MainActivity").putExtra(ExtrasWords.EXTRA_KEY_APP, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                log("key app=pin");
                return;
            }
            Intent i = launchIntent(this, pkg);
            if (i == null) { log("key app=missing"); return; }
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            log("key app=open");
        } catch (RuntimeException e) {
            log("key app=failed");
        }
    }

    static Intent tvSettingsIntent(Context c) {
        for (String a : SettingsKey.TV_SETTINGS_ACTIONS) {
            Intent i = new Intent(a);
            if (c.getPackageManager().resolveActivity(i, 0) != null) return i;
        }
        return new Intent(SettingsKey.TV_SETTINGS_ACTIONS[SettingsKey.TV_SETTINGS_ACTIONS.length - 1]);
    }

    /** the TV's settings over the app in front; when the family lock wants the PIN for them (the home says), the home asks it; the home not answering: the home too */
    private void openTvSettings() {
        Intent i = tvSettingsIntent(this);
        Bundle r = askHome(this, ExtrasWords.SETTINGS_PIN, packageOf(this, i.getAction()), null);
        try {
            if (r == null || r.getBoolean(ExtrasWords.NEEDS_PIN, true)) {
                startActivity(new Intent().setClassName(HOME_PKG, HOME_PKG + ".MainActivity").putExtra("sa.urtv.home.SETTINGS_KEY", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                log("settings key=pin");
                return;
            }
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            log("settings key=open");
        } catch (RuntimeException e) {
            log("settings key=failed");
        }
    }

    /** a call to the home's provider (HOME_AUTHORITY); null when the home did not answer */
    static Bundle askHome(Context c, String method, String arg, Bundle extras) {
        try { return c.getContentResolver().call(Uri.parse("content://" + ExtrasWords.HOME_AUTHORITY), method, arg, extras); }
        catch (RuntimeException e) { return null; }
    }

    // ---------------------------------------------------------------- typing from the phone (TypeInto)

    private static final class Wrapped implements TypeInto.Node {
        final AccessibilityNodeInfo info;
        private final int[] budget;
        private final int depth;
        Wrapped(AccessibilityNodeInfo info, int[] budget, int depth) { this.info = info; this.budget = budget; this.depth = depth; }
        @Override public boolean editable() { return info.isEditable() || editText(info) || canSetText(info); }
        @Override public boolean focused() { return info.isFocused(); }
        @Override public boolean password() { return info.isPassword(); }
        @Override public boolean visible() { return info.isVisibleToUser(); }
        @Override public List<TypeInto.Node> children() {
            List<TypeInto.Node> out = new ArrayList<>();
            if (depth >= 40) return out;
            for (int i = 0; i < info.getChildCount() && budget[0] > 0; i++) {
                AccessibilityNodeInfo c = info.getChild(i);
                if (c == null) continue;
                budget[0]--;
                out.add(new Wrapped(c, budget, depth + 1));
            }
            return out;
        }
    }

    private static boolean editText(AccessibilityNodeInfo i) { CharSequence cn = i.getClassName(); return cn != null && cn.toString().endsWith("EditText"); }

    private static boolean canSetText(AccessibilityNodeInfo i) {
        List<AccessibilityNodeInfo.AccessibilityAction> acts = i.getActionList();
        return acts != null && acts.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);
    }

    static void log(String line) { Log.i(TAG, line); }

    /** the words typeText answers with (ExtrasWords: the phone page knows them) */
    static final String TYPED = ExtrasWords.TYPED, TYPED_NO_SEARCH = ExtrasWords.TYPED_NO_SEARCH, OFF = ExtrasWords.OFF, LOCKED = ExtrasWords.LOCKED, REFUSED = ExtrasWords.REFUSED,
        NO_FIELD = ExtrasWords.NO_FIELD, PASSWORD = ExtrasWords.PASSWORD, FAILED = ExtrasWords.FAILED, SEARCHED = ExtrasWords.SEARCHED;

    /** The phone's text into the app in front (the same rules as the home's TypeService had; the family lock's answers come from the home). One of ExtrasWords' words. */
    static String typeText(Context c, String text, boolean append, boolean search) {
        ExtrasA11yService s = running;
        log(s != null ? "service=on" : "service=off");
        AccessibilityNodeInfo root = s == null ? null : s.getRootInActiveWindow();
        if (s != null) log(root != null ? "root=present" : "root=null");
        String pkg = root == null || root.getPackageName() == null ? null : root.getPackageName().toString();
        boolean ours = pkg != null && (pkg.equals(HOME_PKG) || pkg.equals("sa.urtv.screensaver") || pkg.equals(c.getPackageName()));
        boolean needsPin = true, hidden = true, systemScreen = true;   // the home did not answer: refused (never typed into an app it would lock)
        if (pkg != null && !ours) {
            Bundle b = askHome(c, ExtrasWords.TYPE_CHECK, pkg, null);
            if (b != null) { needsPin = b.getBoolean(ExtrasWords.NEEDS_PIN, true); hidden = b.getBoolean(ExtrasWords.HIDDEN, true); systemScreen = b.getBoolean(ExtrasWords.SYSTEM_SCREEN, true); }
        }
        TypeInto.Refusal r = TypeInto.allowed(s != null, pkg, ours, pkg != null && !ours && needsPin, pkg != null && !ours && hidden, pkg != null && !ours && systemScreen);
        if (r == TypeInto.Refusal.SERVICE_OFF) return done(OFF);
        if (r == TypeInto.Refusal.LOCKED) return done(LOCKED);
        if (r == TypeInto.Refusal.REFUSED) return done(REFUSED);
        if (AppSearch.supports(pkg)) {
            String how = search(c, pkg, text);
            log("search=" + how);
            if ("started".equals(how)) return done(SEARCHED);
        }
        TypeInto.Node picked = null;
        AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focus != null) {
            Wrapped w = new Wrapped(focus, new int[] {0}, 40);
            if (w.editable() && w.visible()) picked = w;
        }
        if (picked == null) picked = TypeInto.pick(new Wrapped(root, new int[] {500}, 0));
        r = TypeInto.fieldProblem(picked);
        log(r == TypeInto.Refusal.NO_FIELD ? "field=none" : r == TypeInto.Refusal.PASSWORD_FIELD ? "field=password" : "field=found");
        if (r == TypeInto.Refusal.NO_FIELD) {
            AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (f == null) f = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            log("focus_class=" + AppSearch.safeClass(f == null ? null : f.getClassName()));
            return done(NO_FIELD);
        }
        if (r == TypeInto.Refusal.PASSWORD_FIELD) return done(PASSWORD);
        AccessibilityNodeInfo field = ((Wrapped) picked).info;
        CharSequence now = Build.VERSION.SDK_INT >= 26 && field.isShowingHintText() ? null : field.getText();
        String out = TypeInto.compose(now == null ? null : now.toString(), text, append);
        if (out == null) return done(FAILED);
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, out);
        if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return done(FAILED);
        if (!search) return done(TYPED);
        if (Build.VERSION.SDK_INT < 30) return done(TYPED_NO_SEARCH);
        return done(field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId()) ? TYPED : TYPED_NO_SEARCH);
    }

    private static String search(Context c, String pkg, String text) {
        String q = AppSearch.query(text);
        if (q == null) return "no_query";
        Intent[] tries = {
            new Intent(Intent.ACTION_SEARCH).setPackage(pkg).putExtra(SearchManager.QUERY, q),
            new Intent(Intent.ACTION_VIEW, Uri.parse(AppSearch.webUrl(q))).setPackage(pkg)
        };
        for (Intent i : tries) {
            try { c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "started"; }
            catch (RuntimeException e) { /* the next way */ }
        }
        return "no_activity";
    }

    private static String done(String word) { log("result=" + word); return word; }

    private static String packageOf(Context c, String action) {
        ResolveInfo ri = c.getPackageManager().resolveActivity(new Intent(action), 0);
        return ri == null || ri.activityInfo == null ? null : ri.activityInfo.packageName;
    }

    // ---------------------------------------------------------------- the sleep timer, Home, the phone's keys

    static boolean sleepNow() {
        ExtrasA11yService s = running;
        return s != null && Build.VERSION.SDK_INT >= 28 && s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    static boolean goHome() {
        ExtrasA11yService s = running;
        return s != null && s.performGlobalAction(GLOBAL_ACTION_HOME);
    }

    static String press(String key) {
        ExtrasA11yService s = running;
        if (s == null) return RemoteNav.OFF;
        RemoteNav.Step st = RemoteNav.plan(key, Build.VERSION.SDK_INT, true);
        String r;
        switch (st.route) {
            case GLOBAL: r = s.performGlobalAction(st.code) ? RemoteNav.DONE : RemoteNav.FAILED; break;
            case FOCUS: r = s.moveFocus(st.code); break;
            case CLICK: r = s.clickFocused(); break;
            default: r = RemoteNav.FAILED; break;
        }
        RemoteTrace.line(key, st.route, r);
        return r;
    }

    private AccessibilityNodeInfo focusedNode(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        return f != null ? f : root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    }

    private String moveFocus(int direction) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return RemoteNav.LIMITED;
        AccessibilityNodeInfo cur = focusedNode(root);
        AccessibilityNodeInfo next = cur == null ? firstFocusable(root, new int[] {500}, 0) : cur.focusSearch(direction);
        return next != null && next.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ? RemoteNav.DONE : RemoteNav.LIMITED;
    }

    private String clickFocused() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return RemoteNav.LIMITED;
        AccessibilityNodeInfo n = focusedNode(root);
        for (int up = 0; n != null && up < 6; up++, n = n.getParent()) {
            if (n.isClickable()) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK) ? RemoteNav.DONE : RemoteNav.FAILED;
        }
        return RemoteNav.LIMITED;
    }

    private static AccessibilityNodeInfo firstFocusable(AccessibilityNodeInfo n, int[] budget, int depth) {
        if (n.isFocusable() && n.isVisibleToUser() && n.isEnabled() && depth > 0) return n;
        if (depth >= 40) return null;
        for (int i = 0; i < n.getChildCount() && budget[0] > 0; i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c == null) continue;
            budget[0]--;
            AccessibilityNodeInfo hit = firstFocusable(c, budget, depth + 1);
            if (hit != null) return hit;
        }
        return null;
    }

}
