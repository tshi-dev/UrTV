package sa.urtv.home;

import android.content.Context;
import android.os.Bundle;

import sa.urtv.home.sports.KickoffPlan;

/**
 * 4.4 «فوق تطبيق آخر»: the short corner notice over another app (the kick-off alert, «تنبيه الإقامة»). The home holds no SYSTEM_ALERT_WINDOW (Play
 * Protect blocked 4.3.4 for «sensitive data»): the optional add-on «إضافات تلفزيونك» holds it and draws the notice (ExtrasNotice), asked through its
 * provider (Extras.notice). The notice in our own home needs none (KickoffOverlay.showIn). Every call here is IPC: OFF the UI thread only.
 */
public final class OverlayNotice {
    private OverlayNotice() { }

    /** kids mode is on (the notice stays on the home then: KickoffPlan.where) */
    public static boolean kids(Context c) {
        try { return Parental.state(c).kids; } catch (RuntimeException e) { return true; }   // not known: as kids mode, nothing over another app
    }

    /** the add-on is installed and the TV lets it draw over apps (the `canDraw` KickoffPlan.where wants); off the UI thread */
    public static boolean ready() { return Extras.canDraw(); }

    /** a short line over another app; false when it could not be shown (the caller says it another way). Off the UI thread */
    public static boolean show(String head, String line) {
        Bundle b = new Bundle();
        b.putString(ExtrasWords.HEAD, head);
        b.putString(ExtrasWords.LINE, line);
        b.putBoolean(ExtrasWords.RTL, Dir.rtl());
        return Extras.notice(b);
    }

    /** the kick-off notice over another app; false when it could not be shown. Off the UI thread */
    public static boolean kick(KickoffPlan.Game g) {
        Bundle b = new Bundle();
        b.putString(ExtrasWords.HEAD, S.t("بدأت المباراة"));
        b.putString(ExtrasWords.SUB, g.lg);
        b.putString(ExtrasWords.LINE, "  " + g.h + "   ×   " + g.a + "  ");
        b.putStringArray(ExtrasWords.C1, g.hc);
        b.putStringArray(ExtrasWords.C2, g.ac);
        b.putBoolean(ExtrasWords.RTL, Dir.rtl());
        return Extras.notice(b);
    }
}
