package sa.urtv.home;

import android.content.Context;

/**
 * 4.4 (R-401, RoleBootRule): «the TV took Home back» said once by the home (HomeNotice) at its next start after the boot that found it. The boot writes
 * PENDING into the setup's file (SetupHook.ROLE_NOTICE); the home's start thread reads it into memory here; the home shows it after its first frame and
 * then it becomes TOLD (off the UI thread). Cleared when a boot finds the role ours again.
 */
public final class RoleNotice {
    private RoleNotice() { }

    public static final String TEXT = S.n("التلفزيون رجّع زر الرئيسية لشاشته الأصلية. ترجّعه لتلفزيونك من الإعدادات ← «إعداد تلفزيوني».");

    private static volatile String pendingText;
    private static volatile Context told;

    /** the home's start thread: the boot left one waiting */
    public static void waiting(Context app) { told = app; pendingText = S.tv(TEXT); }

    /** the UI thread: the text to show, or null */
    static String pending() { return pendingText; }

    /** shown: said once, marked TOLD on a thread of its own */
    static void shown() {
        pendingText = null;
        final Context app = told;
        if (app == null) return;
        try { new Thread(() -> sa.urtv.home.setup.SetupHook.roleNoticeTold(app), "role-notice").start(); } catch (RuntimeException ignored) { }
    }
}
