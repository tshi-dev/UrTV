package sa.urtv.home;

import android.accessibilityservice.AccessibilityService;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Types the phone remote's text into ANOTHER app: the text field of the app in front is found and set through this accessibility service. A
 * service of its own, apart from the sleep timer's (SleepService, which reads nothing): this one can read the screen, so the owner turns it
 * on on its own (the TV's accessibility settings, or optimize/dock.sh type-setup). It asks Android for window-change events only so that
 * Android keeps track of which window is in front (with no event type declared, an app's window events are never sent and
 * getRootInActiveWindow stays null); onAccessibilityEvent is empty, so none is read. It looks at the screen only at the moment a phone sends
 * text, only to find the field, and nothing it sees is kept, logged or sent back (the phone gets one fixed word).
 *
 * The log (tag UrTVType, one line per hop, in PhoneActions and here) has only fixed words and the length of the text: never the text, a
 * package name or anything of a node. The tester reads it to see how far a typing went.
 */
public class TypeService extends AccessibilityService {
    private static volatile TypeService running;
    static final String TAG = "UrTVType";

    @Override protected void onServiceConnected() { running = this; }
    @Override public boolean onUnbind(android.content.Intent i) { running = null; return super.onUnbind(i); }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
    @Override public void onInterrupt() { }

    /** Is the service on? (the owner or ADB enabled it, and Android has started it) */
    static boolean enabled() { return running != null; }

    /** The words typeText answers with; the phone gets nothing else of what it saw. */
    static final String TYPED = "typed_other", TYPED_NO_SEARCH = "typed_other_nosearch", OFF = "service_off", LOCKED = "locked", REFUSED = "refused",
        NO_FIELD = "no_field", PASSWORD = "password_field", FAILED = "failed", SEARCHED = "searched";

    /** One node of the real screen, for TypeInto. A budget keeps a huge tree from holding the call up. */
    private static final class Wrapped implements TypeInto.Node {
        final AccessibilityNodeInfo info;
        private final int[] budget;
        private final int depth;
        Wrapped(AccessibilityNodeInfo info, int[] budget, int depth) { this.info = info; this.budget = budget; this.depth = depth; }
        /** Editable by any of the ways an app can say it: the flag (some apps set it only while focused), an EditText, or it takes ACTION_SET_TEXT. */
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

    private static boolean editText(AccessibilityNodeInfo i) {
        CharSequence cn = i.getClassName();
        return cn != null && cn.toString().endsWith("EditText");
    }

    private static boolean canSetText(AccessibilityNodeInfo i) {
        List<AccessibilityNodeInfo.AccessibilityAction> acts = i.getActionList();
        return acts != null && acts.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);
    }

    /** One line of the log: fixed words only (see the class comment). */
    static void log(String line) { Log.i(TAG, line); }

    /**
     * Types the phone's text into the text field of the app in front: the focused editable field, else the first one on screen. Only when the service is
     * on, and never into an app the home screen would ask the PIN for (or, in kids mode, hide), nor into a password field. "append" adds to what the
     * field holds, otherwise the text replaces it; "search" then presses the keyboard's action (Android 11 and later). Answers one of the words above.
     */
    static String typeText(Context c, String text, boolean append, boolean search) {
        TypeService s = running;
        log(s != null ? "service=on" : "service=off");
        AccessibilityNodeInfo root = s == null ? null : s.getRootInActiveWindow();
        if (s != null) log(root != null ? "root=present" : "root=null");
        String pkg = root == null || root.getPackageName() == null ? null : root.getPackageName().toString();
        ParentalPolicy.State st = Parental.state(c);
        boolean ours = pkg != null && (pkg.equals(c.getPackageName()) || pkg.equals("sa.urtv.screensaver"));
        boolean systemScreen = pkg != null && ((Parental.needsPin(c, ParentalPolicy.TV_SETTINGS) && pkg.equals(packageOf(c, Settings.ACTION_SETTINGS)))
            || (Parental.needsPin(c, ParentalPolicy.WIFI) && pkg.equals(packageOf(c, Settings.ACTION_WIFI_SETTINGS))));
        TypeInto.Refusal r = TypeInto.allowed(s != null, pkg, ours, pkg != null && Parental.needsPin(c, pkg), pkg != null && !ParentalPolicy.visible(st, pkg), systemScreen);
        if (r == TypeInto.Refusal.SERVICE_OFF) return done(OFF);
        if (r == TypeInto.Refusal.LOCKED) return done(LOCKED);
        if (r == TypeInto.Refusal.REFUSED) return done(REFUSED);
        // YouTube TV's own search first, on any of its screens (its search box is not an editable node to Android): the words go there by intent
        if (AppSearch.supports(pkg)) {
            String how = search(c, pkg, text);
            log("search=" + how);
            if ("started".equals(how)) return done(SEARCHED);
        }
        // the field that has the input focus first (Android knows it even for a box the tree walk would not call editable), else the walk
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
            // for the TV test: what the focused node is (its class name only, never its text or id), to see why no field was found
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

    /**
     * The app's own search (YouTube TV): the words go there by Intent.ACTION_SEARCH, else by its web search link, bound to the app's package. No
     * package query first: Android 11+ hides other apps from it without a manifest query, and the answer would always be "none". One fixed word:
     * started, no_query, or no_activity.
     */
    private static String search(Context c, String pkg, String text) {
        String q = AppSearch.query(text);
        if (q == null) return "no_query";
        Intent[] tries = {
            new Intent(Intent.ACTION_SEARCH).setPackage(pkg).putExtra(SearchManager.QUERY, q),
            new Intent(Intent.ACTION_VIEW, Uri.parse(AppSearch.webUrl(q))).setPackage(pkg)
        };
        for (Intent i : tries) {
            try {
                c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "started";
            } catch (RuntimeException e) {
                // ActivityNotFoundException or a refusal: the next way
            }
        }
        return "no_activity";
    }

    /**
     * A phone key into the app in front (arrows, OK, Back, Home): a global action where Android has one (Back and Home always, the arrows and
     * OK from Android 13), else, before that, a focus move on the window in front: the input-focused (else accessibility-focused) node's
     * focusSearch, then ACTION_FOCUS; OK is ACTION_CLICK on the focused node or the nearest clickable parent. One of RemoteNav's fixed
     * words; the log (RemoteTrace) has the key, the route and that word, nothing of the window.
     */
    static String press(String key) {
        TypeService s = running;
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
        // nothing focused yet: the first focusable thing on screen takes the focus
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

    private static String done(String word) {
        log("result=" + word);
        return word;
    }

    /** The package an intent opens, or null. */
    private static String packageOf(Context c, String action) {
        ResolveInfo ri = c.getPackageManager().resolveActivity(new Intent(action), 0);
        return ri == null || ri.activityInfo == null ? null : ri.activityInfo.packageName;
    }
}
