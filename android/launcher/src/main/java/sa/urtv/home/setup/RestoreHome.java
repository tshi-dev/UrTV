package sa.urtv.home.setup;

import android.content.Context;
import android.provider.Settings;

import java.io.IOException;

/**
 * «تراجع» / «رجّع واجهة التلفزيون الأصلية»: the one undo of the setup, over the TV's own local ADB (SetupEngine.undo: the stock home and the HOME role
 * first, then the ledger's apps, the screensaver and the permissions as they were; optimize/restore.sh on a computer). The setup screen, the safe
 * mode («الوضع الآمن», its own process) and the phone page all call this, so there is one path. One run at a time across the processes (a small
 * file says one is going; an old one is taken as cut off after RUN_MS).
 */
public final class RestoreHome {
    private RestoreHome() { }

    static final String RUNNING = "restore-running.txt";
    static final long RUN_MS = 10 * 60_000L;

    /** What the person does with the remote when USB debugging is off (the undo needs it). */
    public static final String HOW_TO_DEBUG = "«تصحيح USB» مطفأ، والرجوع يحتاجه. شغّله بالريموت: الإعدادات ← تفضيلات الجهاز (أو «النظام») ← حول ← انزل إلى «Build» "
        + "(أو «إصدار نظام تشغيل Android TV») واضغط OK سبع مرات حتى تظهر «أصبحت مطوّراً» ← ارجع ← «خيارات المطوّر» ← شغّل «تصحيح USB». ثم ارجع هنا واضغط الزر مرة ثانية، "
        + "وحين يسأل التلفزيون «السماح بتصحيح USB؟» اختر «السماح دائماً».";

    public static final class Outcome {
        public final boolean ok, debugOff, busy;
        public final String message;
        Outcome(boolean ok, boolean debugOff, boolean busy, String message) { this.ok = ok; this.debugOff = debugOff; this.busy = busy; this.message = message; }
    }

    /** The lines of the run, and the moment the TV asks «السماح بتصحيح USB؟». */
    public interface Lines {
        void line(String text);
        void waiting();
    }

    /** Settings.Global.ADB_ENABLED: false when USB debugging is off (then nothing can be undone from the TV itself). */
    public static boolean debuggingOn(Context c) {
        try { return Settings.Global.getInt(c.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1; } catch (RuntimeException e) { return true; }   // unknown: try, the connection says
    }

    /** The home switch journal's state (switching, done, restored, aborted), or null without one: for the safe mode's report. */
    public static String switchState(Context c) {
        String j = new FileStore(c).get("home-switch.json");
        if (j == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"state\": \"([a-z]{1,12})\"").matcher(j);
        return m.find() ? m.group(1) : null;
    }

    /** Is an undo running now (in any of the app's processes)? */
    public static boolean running(Context c) {
        String t = new FileStore(c).get(RUNNING);
        try { return t != null && !t.trim().isEmpty() && System.currentTimeMillis() - Long.parseLong(t.trim()) < RUN_MS; } catch (NumberFormatException e) { return false; }
    }

    /** The undo, on the caller's thread (never the screen's: it takes minutes). The ADB key is deleted afterwards unless a safety switch still needs it. */
    public static Outcome run(Context c, final Lines lines) {
        Context app = c.getApplicationContext();
        if (!debuggingOn(app)) return new Outcome(false, true, false, HOW_TO_DEBUG);
        FileStore store = new FileStore(app);
        synchronized (RestoreHome.class) {
            if (running(app)) return new Outcome(false, false, true, "الرجوع يعمل الآن. انتظر حتى ينتهي.");
            try { store.put(RUNNING, System.currentTimeMillis() + "\n"); } catch (IOException ignored) { }
        }
        SetupEngine.Out out = new SetupEngine.Out() { @Override public void line(String t) { if (lines != null) lines.line(t); } };
        try (SetupSession s = SetupSession.open(app, 180_000, new AdbClient.Approval() {
            @Override public void waiting() { if (lines != null) lines.waiting(); }
        }, out)) {
            boolean ok = s.engine.undo().status == SetupEngine.Status.DONE;
            return new Outcome(ok, false, false, ok ? "تم الرجوع. أطفئ «تصحيح USB» من خيارات المطوّر، واختر «إلغاء تراخيص تصحيح USB» لتنظيف التلفزيون."
                : "انتهى الرجوع مع ملاحظات (فوق).");
        } catch (IOException e) {
            String m = e.getMessage();
            return new Outcome(false, m != null && m.startsWith("debugging is off"), false, m != null && m.startsWith("debugging is off") ? HOW_TO_DEBUG : friendly(m));
        } catch (RuntimeException e) {
            return new Outcome(false, false, false, "حدث خطأ غير متوقع، ما غيّرنا شيئاً بعده. جرّب مرة ثانية.");
        } finally {
            SetupSession.deleteKeyIfFinished(app);   // the key is a standing credential; it stays only while a switch is pending
            try { store.put(RUNNING, ""); } catch (IOException ignored) { }
        }
    }

    /** A failed connection, in words for the person. */
    static String friendly(String m) {
        if (m == null) return "انقطع الاتصال بالتلفزيون. إن كنت في منتصف تبديل الشاشة الرئيسية فسترجع الأصلية وحدها بعد 5 دقائق. جرّب مرة ثانية.";
        if (m.startsWith("debugging is off")) return "تصحيح USB مطفأ في التلفزيون. فعّله (خيارات المطوّر ← تصحيح USB) ثم اضغط «ابدأ».";
        if (m.contains("permission was not accepted")) return "لم تُقبل رسالة «السماح بتصحيح USB؟». اضغط «ابدأ» وافتح الرسالة بالريموت واختر «السماح دائماً».";
        if (m.contains("TLS")) return "هذا التلفزيون يطلب التصحيح اللاسلكي؛ استخدم «تصحيح USB» العادي.";
        return "انقطع الاتصال بالتلفزيون. إن كنت في منتصف تبديل الشاشة الرئيسية فسترجع الأصلية وحدها بعد 5 دقائق. جرّب مرة ثانية.";
    }
}
