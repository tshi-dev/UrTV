package sa.urtv.home;

/** tests/overlay_offer_test.py: the one-time offer of the add-on's «فوق تطبيق آخر» (OverlayOfferRule) on a plain JVM. */
public class OverlayOfferRun {
    static int failed = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        check("due: alert on, over other apps, add-on cannot draw", OverlayOfferRule.due(true, true, false, false, false, 0));
        check("not due: the add-on can draw", !OverlayOfferRule.due(true, true, true, false, false, 0));
        check("not due: «فوق تطبيق آخر» is off", !OverlayOfferRule.due(true, false, false, false, false, 0));
        check("not due: the kick-off alert is off", !OverlayOfferRule.due(false, true, false, false, false, 0));
        check("not due: kids mode", !OverlayOfferRule.due(true, true, false, true, false, 0));
        check("not due: a setup is running", !OverlayOfferRule.due(true, true, false, false, true, 0));
        check("not due: already shown for this version", !OverlayOfferRule.due(true, true, false, false, false, OverlayOfferRule.VERSION));
        check("due again for a later version", OverlayOfferRule.due(true, true, false, false, false, OverlayOfferRule.VERSION - 1));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
