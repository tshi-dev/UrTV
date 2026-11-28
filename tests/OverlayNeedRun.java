package sa.urtv.screensaver;

/** tests/overlay_need_test.py: OverlayNeed (#42, 4.4 R-473) on a plain JVM. One line per case: «ok …» or «BAD …». */
public class OverlayNeedRun {
    static int bad;
    static void check(String n, boolean ok) { System.out.println((ok ? "ok " : "BAD ") + n); if (!ok) bad++; }

    public static void main(String[] a) {
        check("a dream that ends by itself within 3 s counts; two in a row is the flash", OverlayNeed.afterDream(0, 800, false, false) == 1
            && OverlayNeed.afterDream(1, 2999, false, false) == 2 && OverlayNeed.QUICK_TIMES == 2);
        check("a key, our own end (the setup's prompt, a renderer lost) or a dream that stayed 3 s starts the count again",
            OverlayNeed.afterDream(1, 500, true, false) == 0 && OverlayNeed.afterDream(1, 500, false, true) == 0 && OverlayNeed.afterDream(1, 3000, false, false) == 0
            && OverlayNeed.afterDream(1, -1, false, false) == 0);
        check("a Google TV without the grant that had it once (the lost appop): asked", OverlayNeed.needed(30, true, false, true, 0));
        check("a Google TV without the grant whose screensaver flashed twice: asked", OverlayNeed.needed(30, true, false, false, 2));
        check("never on a TV that does not need it: the grant is there", !OverlayNeed.needed(30, true, true, true, 5));
        check("never on a TV that does not need it: not a Google TV", !OverlayNeed.needed(30, false, false, true, 5));
        check("never on a TV that does not need it: the screensaver works (no loss, under two flashes)", !OverlayNeed.needed(30, true, false, false, 1)
            && !OverlayNeed.needed(30, true, false, false, 0));
        check("never before Android 6 (granted at install)", !OverlayNeed.needed(22, true, false, true, 5));
        check("opens by itself once; the row stays while needed", OverlayNeed.openByItself(true, false) && !OverlayNeed.openByItself(true, true)
            && !OverlayNeed.openByItself(false, false));
        check("plain words, at most 8 a line, the owner's line", OverlayNeed.WHY.equals("عشان تشتغل شاشة التوقف بعد كل تشغيل")
            && words(OverlayNeed.TITLE) <= 8 && words(OverlayNeed.WHY) <= 8 && words(OverlayNeed.HOW) <= 8 && words(OverlayNeed.HIDDEN) <= 8
            && !(OverlayNeed.TITLE + OverlayNeed.WHY + OverlayNeed.HOW + OverlayNeed.ROW + OverlayNeed.HIDDEN).contains("·"));
        System.exit(bad == 0 ? 0 : 1);
    }

    static int words(String s) { return s.trim().split("\\s+").length; }
}
