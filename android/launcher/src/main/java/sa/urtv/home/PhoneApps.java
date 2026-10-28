package sa.urtv.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the phone remote («تحكم من الجوال») reads of the home screen and asks of it, in one public place (the phone package cannot see the
 * home's package-private classes): the favourites as the TV shows them, our own pills, the real icons, the family lock's summary (read only),
 * and opening something with the SAME parental check as the home screen's own buttons. An app that is locked is never opened from here: the
 * home screen is brought up with the request and asks for the PIN ON THE TV; the phone never sees or sends it.
 */
public final class PhoneApps {
    /** The extra of the home screen's intent: an app's package or one of TOOLS' targets. MainActivity runs it through the same guard as a tap. */
    public static final String EXTRA_LAUNCH = "phone_launch";
    private static final int ICON_PX = 96;
    /** target, name, the family-lock target it asks about: what the page's second row offers, in this order. */
    private static final String[][] TOOLS = {
        {"tasks", "المهام", ParentalPolicy.TASKS},
        {"saver", "شاشة التوقف", ParentalPolicy.SAVER},
        {"inputs", "المصادر", ParentalPolicy.INPUTS},
        {"settings", "الإعدادات", ParentalPolicy.TV_SETTINGS},
        {"listen", "تلاوة وأذان", ParentalPolicy.LISTEN},
    };
    /** The favourites are looked up at most this often (the phone asks for every icon); a package or favourites change clears it at once. */
    private static final long APPS_TTL_MS = 5_000, ICON_TTL_MS = 10 * 60_000L;
    private static final class Icon { final byte[] png; final long at; Icon(byte[] png, long at) { this.png = png; this.at = at; } }
    private static final LruCache<String, Icon> ICONS = new LruCache<>(64);
    private static List<String[]> appsCache;
    private static long appsAt;

    private PhoneApps() { }

    private static boolean listening(Context c) { return c.getResources().getBoolean(R.bool.show_listening); }

    /** The cards of the home screen were saved from the phone: the home in front shows them now (HomeCard.CHANGED). */
    public static void cardsChanged(Context app) {
        app.sendBroadcast(new Intent(HomeCard.CHANGED).setPackage(app.getPackageName()));
    }

    /** A package was installed, replaced or removed, or the favourites changed: the next ask looks again. */
    public static synchronized void invalidate() {
        appsCache = null;
        ICONS.evictAll();
    }

    /** The favourites as the home screen's row shows them (kids mode already applied): {package, name, "1" when it asks for the PIN}. */
    public static synchronized List<String[]> apps(Context c) {
        long now = System.currentTimeMillis();
        if (appsCache != null && now - appsAt < APPS_TTL_MS) return appsCache;
        appsCache = scan(c);
        appsAt = now;
        return appsCache;
    }

    private static List<String[]> scan(Context c) {
        Context app = c.getApplicationContext();
        Map<String, String> all = Dock.launchable(app);
        ParentalPolicy.State st = Parental.state(app);
        List<String[]> out = new ArrayList<>();
        for (String pkg : ParentalPolicy.visible(st, Dock.shown(app, all))) {
            out.add(new String[] {pkg, all.get(pkg), ParentalPolicy.needsPin(st, new ParentalPolicy.Session(), pkg) ? "1" : "0"});
        }
        return out;
    }

    /** Our pills, {id, name, "1" when locked}; none while kids mode is on (the home shows no pills then). */
    public static List<String[]> tools(Context c) {
        Context app = c.getApplicationContext();
        ParentalPolicy.State st = Parental.state(app);
        List<String[]> out = new ArrayList<>();
        if (st.kids) return out;
        for (String[] t : TOOLS) {
            if (t[0].equals("listen") && !listening(app)) continue;
            out.add(new String[] {t[0], t[1], ParentalPolicy.needsPin(st, new ParentalPolicy.Session(), t[2]) ? "1" : "0"});
        }
        return out;
    }

    /** The app's icon as a PNG (at most 96 px), only for one of {@link #apps}; null otherwise (the phone cannot list what else is installed). */
    public static byte[] icon(Context c, String pkg) {
        Context app = c.getApplicationContext();
        boolean shown = false;
        for (String[] a : apps(app)) shown |= a[0].equals(pkg);
        if (!shown) return null;
        Icon cached = ICONS.get(pkg);
        if (cached != null && System.currentTimeMillis() - cached.at < ICON_TTL_MS) return cached.png;
        try {
            PackageManager pm = app.getPackageManager();
            Drawable d = pm.getApplicationIcon(pkg);
            Bitmap b = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            d.setBounds(0, 0, ICON_PX, ICON_PX);
            d.draw(cv);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
            b.recycle();
            byte[] png = out.toByteArray();
            ICONS.put(pkg, new Icon(png, System.currentTimeMillis()));
            return png;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Opens an app of {@link #apps} or a tool of {@link #tools}: "opened", "pin_on_tv" (it is locked: the TV asks for the PIN), "not_found" or
     * "failed". An app that is not locked starts at once; everything else goes through the home screen's own guard.
     */
    public static String launch(Context c, String target) {
        Context app = c.getApplicationContext();
        if (target == null) return "not_found";
        boolean locked;
        if (target.startsWith("@")) {
            String[] tool = null;
            for (String[] t : TOOLS) if (("@" + t[0]).equals(target)) tool = t;
            if (tool == null || Parental.state(app).kids || (tool[0].equals("listen") && !listening(app))) return "not_found";
            locked = Parental.needsPin(app, tool[2]);
        } else {
            boolean shown = false;
            for (String[] a : apps(app)) shown |= a[0].equals(target);
            if (!shown || !ParentalPolicy.visible(Parental.state(app), target)) return "not_found";   // kids mode is looked at again here, not only in the cached list
            locked = Parental.needsPin(app, target);
            if (!locked) {
                PackageManager pm = app.getPackageManager();
                Intent i = pm.getLeanbackLaunchIntentForPackage(target);
                if (i == null) i = pm.getLaunchIntentForPackage(target);
                if (i == null) return "failed";
                try {
                    if (!i.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(i);
                    return "opened";
                } catch (RuntimeException e) {
                    return "failed";
                }
            }
        }
        try {
            app.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setComponent(new ComponentName(app, MainActivity.class)).putExtra(EXTRA_LAUNCH, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            return locked ? "pin_on_tv" : "opened";
        } catch (RuntimeException e) {
            return "failed";
        }
    }

    /** The family lock as the home screen has it, read only: whether a PIN is set, kids mode, and what is locked. Never the PIN. */
    public static Map<String, Object> family(Context c) {
        ParentalPolicy.State st = Parental.state(c.getApplicationContext());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pin_set", st.pinSet);
        m.put("kids", st.kids);
        List<Object> locked = new ArrayList<>();
        if (st.pinSet) for (String t : new java.util.TreeSet<>(st.locked)) locked.add(t);
        m.put("locked", locked);
        return m;
    }

    /** «مؤقت النوم»: {left_seconds, options}. */
    public static Map<String, Object> sleep(Context c) {
        Map<String, Object> m = new LinkedHashMap<>();
        long end = SleepTimer.endsAt(c.getApplicationContext());
        m.put("left_seconds", end == 0 ? 0L : Math.max(1L, (end - System.currentTimeMillis()) / 1000));
        List<Object> options = new ArrayList<>();
        for (int minutes : SleepTimer.MINUTES) options.add((long) minutes);
        m.put("options", options);
        return m;
    }

    /** Starts «مؤقت النوم» for these minutes (one of SleepTimer.MINUTES), or cancels it with 0. */
    public static boolean setSleep(Context c, int minutes) {
        if (minutes != 0) {
            boolean ok = false;
            for (int m : SleepTimer.MINUTES) ok |= m == minutes;
            if (!ok) return false;
        }
        SleepTimer.start(c.getApplicationContext(), minutes);
        return true;
    }

    /** Is one of our accessibility services on (the typing/remote one or the sleep one)? Then Back and Home work in every app. */
    public static boolean keysEverywhere() { return SleepService.enabled() || TypeService.enabled(); }

    /** Do the arrows and OK reach another app too? From Android 13 any of the services does it; before that only the typing/remote one. */
    public static boolean dpadEverywhere() { return RemoteNav.dpadEverywhere(android.os.Build.VERSION.SDK_INT, keysEverywhere(), TypeService.enabled()); }

    /** A phone key into the app in front through whichever of our services is on (one of RemoteNav's words: done, service_off, other_app_limited, failed). */
    public static String pressKey(String key) {
        String r = TypeService.press(key);
        return RemoteNav.OFF.equals(r) ? SleepService.press(key) : r;
    }

    /** The phone's text into another app (TypeService.typeText); one of its fixed words. */
    public static String typeText(Context c, String text, boolean append, boolean search) { return TypeService.typeText(c.getApplicationContext(), text, append, search); }

    /** Is the typing service on? Without it a phone cannot type into other apps (the page says how to turn it on). */
    public static boolean typingEnabled() { return TypeService.enabled(); }

}
