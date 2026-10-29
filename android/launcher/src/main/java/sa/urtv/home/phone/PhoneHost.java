package sa.urtv.home.phone;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * The one PhoneControl of this app, built on first use by the screen that asks for it (PhoneActivity). Building it opens nothing: no socket
 * and no thread exist until the owner presses «تشغيل» (PhoneControl.start). It follows the control's changes to start and stop the
 * tracking of our front screen, and passes them to the screen.
 */
public final class PhoneHost {
    private static PhoneControl control;
    private static Runnable ui;
    private static String pageText;
    private static final java.util.Map<String, byte[]> fontBytes = new java.util.HashMap<>();

    private PhoneHost() { }

    public static synchronized PhoneControl get(Context c) {
        if (control == null) {
            final Context app = c.getApplicationContext();
            control = new PhoneControl(new PhoneServer.DefaultBinder(), new PhoneControl.Clock() {
                @Override public long now() { return System.currentTimeMillis(); }
            }, new PhoneActions(app), new PhoneControl.Page() {
                @Override public String html() { return page(app); }
                @Override public byte[] font(String name) { return fontAsset(app, name); }
            }, new java.util.function.Predicate<java.net.InetAddress>() {
                @Override public boolean test(java.net.InetAddress a) { return Guard.privateClient(a); }
            }, new SecureRandom());
            control.setListener(new PhoneControl.Listener() {
                @Override public void changed() {
                    if (!control.isOn()) Foreground.untrack((Application) app);
                    Runnable r = ui;
                    if (r != null) r.run();
                }
            });
        }
        return control;
    }

    /** The screen showing the state wants to hear about changes (called from any thread); null to stop. */
    public static void setUi(Runnable r) { ui = r; }

    /** The owner's «تشغيل». */
    public static PhoneControl.Start turnOn(Activity a) {
        PhoneControl.Start r = get(a).start(PhoneNet.wifi());
        if (r == PhoneControl.Start.OK) Foreground.track((Application) a.getApplicationContext(), a);
        return r;
    }

    private static synchronized String page(Context c) {
        if (pageText == null) {
            byte[] b = asset(c, "phone/index.html");
            pageText = b == null ? "<!doctype html><title>x</title>" : new String(b, StandardCharsets.UTF_8);
        }
        return pageText;
    }

    /** A font the APK carries (CI puts Thmanyah in assets/fonts; a local build has none), read once; null when it is not there or not allowed. */
    private static synchronized byte[] fontAsset(Context c, String name) {
        if (!PhoneApi.FONTS.contains(name)) return null;
        if (!fontBytes.containsKey(name)) fontBytes.put(name, asset(c, "fonts/" + name));   // a missing one is remembered too: the assets never change
        return fontBytes.get(name);
    }

    private static byte[] asset(Context c, String path) {
        try {
            InputStream in = c.getAssets().open(path);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toByteArray();
            } finally {
                in.close();
            }
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
