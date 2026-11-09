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
    private static final java.util.Map<String, String> pages = new java.util.HashMap<>();
    private static final java.util.Map<String, byte[]> fontBytes = new java.util.HashMap<>();

    private PhoneHost() { }

    public static synchronized PhoneControl get(Context c) {
        if (control == null) {
            final Context app = c.getApplicationContext();
            final PhoneActions actions = new PhoneActions(app);
            control = new PhoneControl(new PhoneServer.DefaultBinder(), new PhoneControl.Clock() {
                @Override public long now() { return System.currentTimeMillis(); }
            }, actions, new PhoneControl.Page() {
                @Override public String html() { sa.urtv.home.S.init(app); return page(app, actions.lang()); }
                @Override public byte[] font(String name) { return fontAsset(app, name); }
                @Override public String portfolioHtml() { sa.urtv.home.S.init(app); return portfolioPage(app, actions.lang()); }
                @Override public String photosHtml() { return photosPage(app, actions.lang()); }
                @Override public String drawHtml() { return onePage(app, "phone/draw.html", actions.lang()); }
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

    /**
     * The page in the language of «اللغة» (Tr.phonePageName), as the screensaver says it at each load (PhoneActions.lang), so the page a phone
     * opens right after its «English / العربية» link is in the new language. Each file is read once; an English file missing from the APK gives
     * the Arabic one.
     */
    private static synchronized String page(Context c, String lang) {
        String name = sa.urtv.home.Tr.phonePageName(lang);
        if (!pages.containsKey(name)) {
            byte[] b = asset(c, name);
            pages.put(name, b == null ? null : new String(b, StandardCharsets.UTF_8));
        }
        String text = pages.get(name);
        if (text != null) return text;
        String ar = sa.urtv.home.Tr.phonePageName(sa.urtv.home.Tr.AR);
        return name.equals(ar) ? "<!doctype html><title>x</title>" : page(c, sa.urtv.home.Tr.AR);
    }

    /** 4.3 «ارسم»'s page, like «صوري»'s: one file for both languages, English by <html lang="en">. */
    private static synchronized String onePage(Context c, String name, String lang) {
        if (!pages.containsKey(name)) {
            byte[] b = asset(c, name);
            pages.put(name, b == null ? null : new String(b, StandardCharsets.UTF_8));
        }
        String text = pages.get(name);
        return text == null || !sa.urtv.home.Tr.EN.equals(lang) ? text : text.replace("<html lang=\"ar\" dir=\"rtl\">", "<html lang=\"en\" dir=\"ltr\">");
    }

    /** 4.3 «صوري»'s page: one file for both languages; «اللغة» English sets <html lang="en"> and its script picks the English words. */
    private static synchronized String photosPage(Context c, String lang) {
        if (!pages.containsKey("phone/photos.html")) {
            byte[] b = asset(c, "phone/photos.html");
            pages.put("phone/photos.html", b == null ? null : new String(b, StandardCharsets.UTF_8));
        }
        String text = pages.get("phone/photos.html");
        return text == null || !sa.urtv.home.Tr.EN.equals(lang) ? text : text.replace("<html lang=\"ar\" dir=\"rtl\">", "<html lang=\"en\" dir=\"ltr\">");
    }

    /** «محفظتي»'s page in the language of «اللغة» (portfolio.html, portfolio.en.html), read once; the Arabic one when the English is missing. */
    private static synchronized String portfolioPage(Context c, String lang) {
        String name = sa.urtv.home.Tr.EN.equals(lang) ? "phone/portfolio.en.html" : "phone/portfolio.html";
        if (!pages.containsKey(name)) {
            byte[] b = asset(c, name);
            pages.put(name, b == null ? null : new String(b, StandardCharsets.UTF_8));
        }
        String text = pages.get(name);
        return text != null || name.equals("phone/portfolio.html") ? text : portfolioPage(c, sa.urtv.home.Tr.AR);
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
