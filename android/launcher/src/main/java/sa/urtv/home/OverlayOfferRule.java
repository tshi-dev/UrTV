package sa.urtv.home;

/**
 * 4.4: «فوق تطبيق آخر» moved from the home to the add-on «إضافات تلفزيونك» (R-274). Whoever used it on 4.3.x granted the permission to the home, so on the first run of
 * 4.4 the home offers the add-on once: the kick-off alert is set to go over other apps and the add-on cannot draw. Never in kids mode, never while a setup runs, once only
 * («لاحقاً» counts as seen). No Android in here.
 */
final class OverlayOfferRule {
    private OverlayOfferRule() { }

    /** bump to offer again in a later release; the home's prefs keep the version last shown */
    static final int VERSION = 1;
    static final String KEY = "overlay_offer_v";

    /** the offer is due: the alert is on and set to «فوق تطبيق آخر», the add-on cannot draw, not kids, no setup running, this version not shown yet */
    static boolean due(boolean kickoffOn, boolean overOtherApps, boolean canDraw, boolean kids, boolean setupRunning, int shownVersion) {
        return kickoffOn && overOtherApps && !canDraw && !kids && !setupRunning && shownVersion < VERSION;
    }
}
