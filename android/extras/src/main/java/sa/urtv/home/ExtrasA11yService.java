package sa.urtv.home;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
    /** our home is in front (from the windows that come to the front) */
    private volatile boolean homeFront;
    /** a stock home answers HOME (it then answers the Settings key itself) */
    private volatile boolean stockHome;
    private volatile Boolean homeKeyLost;
    private Set<String> stockHomes = new HashSet<>();
    /** 4.4: the stock homes' packages (only their windows are looked at further) */
    private Set<String> stockPackages = new HashSet<>();

    @Override protected void onServiceConnected() {
        running = this;
        stockHomes = stockHomeActivities(this);
        stockPackages = HomeWatch.packages(stockHomes);
        watchLog(watchOn(this) ? (wanted(this) ? 1 : 2) : 3);
        watchLog(stockHomes.isEmpty() ? 4 : 5);
        filterKeys();
    }
    @Override public boolean onUnbind(Intent i) { running = null; return super.onUnbind(i); }
    @Override public void onInterrupt() { }

    static boolean enabled() { return running != null; }

    // ---------------------------------------------------------------- the window in front: our home or not, and the Home-button watch

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString(), cls = e.getClassName() == null ? null : e.getClassName().toString();
        if (pkg.equals("com.android.systemui") || pkg.equals("android")) return;   // a volume bar or a system dialog over the app: the app in front did not change
        boolean front = pkg.equals(HOME_PKG);
        if (front != homeFront) { homeFront = front; filterKeys(); }
        if (!stockPackages.contains(pkg)) return;   // any other app's window: nothing read further, nothing logged
        // 4.4 (step 86 B): every decision on a stock home's window is logged (the stock home's own class only), so a tester sees why. The watch
        // brings ours while the person's home is ours: Android's default (the role), or the setup's word (WANTED) when the TV took the role back
        boolean on = watchOn(this), chosen = oursChosen(), wantedNow = wanted(this), homeScreen = HomeWatch.homeScreen(pkg, cls, stockHomes);
        long now = System.currentTimeMillis();
        boolean bring = homeScreen && watch.decide(on, pkg, cls, stockHomes, chosen || wantedNow, now);
        // fixed words only (the log never names a package or a class): which rule matched, then the decision
        watchLog(stockHomes.contains(pkg + "/" + cls) ? 6 : homeScreen ? 7 : 8);
        watchLog(chosen ? (wantedNow ? 9 : 10) : (wantedNow ? 11 : 12));
        watchLog(bring ? 13 : !homeScreen ? 14 : !on ? 15 : !(chosen || wantedNow) ? 16 : watch.paused(now) ? 17 : 18);
        if (bring) {
            try {
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(new ComponentName(HOME_PKG, HOME_PKG + ".MainActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                Log.i("UrTVWatch", "home=brought");
            } catch (RuntimeException ex) {
                Log.i("UrTVWatch", "home=failed");
            }
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
            default: Log.i("UrTVWatch", "wanted=no"); break;
        }
    }

    /** 4.4: the setup's word that the person's home is ours (WANTED from the home); off by «تراجع» (UNWANTED) */
    static boolean wanted(Context c) { return c.getSharedPreferences("extras", MODE_PRIVATE).getBoolean(ExtrasWords.WANTED, false); }

    static void setWanted(Context c, boolean on) {
        c.getSharedPreferences("extras", MODE_PRIVATE).edit().putBoolean(ExtrasWords.WANTED, on).apply();
        watchLog(on ? 19 : 20);
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
            return ri != null && ri.activityInfo != null && HOME_PKG.equals(ri.activityInfo.packageName);
        } catch (RuntimeException e) {
            return false;
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
            boolean want = SettingsKey.filterWanted(front, stockHome, lowRam, homeKeyLost);
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
        try { return c.getPackageManager().queryIntentActivities(new Intent(SettingsKey.NOTIFICATION_PANEL_ACTION), 0).isEmpty(); }
        catch (RuntimeException e) { return false; }
    }

    @Override protected boolean onKeyEvent(KeyEvent e) {
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
