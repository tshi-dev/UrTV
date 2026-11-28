package sa.urtv.screensaver;

/**
 * 4.4 GitHub #42 (a TCL 65C7K, Google TV): there our screensaver flashes and closes unless this app may draw over other apps
 * (SYSTEM_ALERT_WINDOW); the grant the person gave was lost at each reboot while the app did not declare the permission. The manifest
 * declares it now (a grant survives), and the person is asked ONLY on a TV that needs it: a Google TV where this app may not draw over
 * apps, and either the grant was there once and is gone (the lost appop), or our screensaver ended by itself within QUICK_MS, twice in a row
 * (no key, not our own end). Never on another TV. Plain Java (tests/OverlayNeedRun.java); Prefs.overlayNeeded reads the TV.
 */
final class OverlayNeed {
    private OverlayNeed() { }

    /** a dream that ends by itself sooner than this, with no key, is the flash of #42 */
    static final long QUICK_MS = 3000;
    /** that many in a row: the TV needs the permission (one alone may be the TV's own hiccup) */
    static final int QUICK_TIMES = 2;

    /** the plain words of the request (docs/ui/SIMPLE.md: at most 8 words a line) */
    static final String TITLE = "شاشة التوقف تحتاج إذنك";
    static final String WHY = "عشان تشتغل شاشة التوقف بعد كل تشغيل";
    static final String HOW = "فعّل «الظهور فوق التطبيقات» لـUrTV";
    static final String ROW = "إذن شاشة التوقف";
    static final String HIDDEN = "التلفزيون يخفي هذا الإذن، يُعطى من الكمبيوتر";

    /** the count of quick ends in a row after one more end of the dream (shownMs: how long it was on; keyed: a key ended it; own: we ended it) */
    static int afterDream(int before, long shownMs, boolean keyed, boolean own) {
        if (keyed || own || shownMs < 0 || shownMs >= QUICK_MS) return 0;
        return Math.min(Math.max(before, 0) + 1, 99);
    }

    /** Does this TV need the permission now? sdk: Android's API level (before 23 it is granted at install) */
    static boolean needed(int sdk, boolean googleTv, boolean canDraw, boolean hadIt, int quickEnds) {
        return sdk >= 23 && googleTv && !canDraw && (hadIt || quickEnds >= QUICK_TIMES);
    }

    /** the request opens by itself once (until it was shown); its row stays in «شاشة التوقف» while it is needed */
    static boolean openByItself(boolean needed, boolean asked) { return needed && !asked; }
}
