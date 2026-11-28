package sa.urtv.screensaver;

/**
 * 4.4 beta 4 (a user: the settings' text was too large on their TV): the settings' words are in sp, sized for the TV's usual screen of 960 x 540 dp
 * (1080p at density 2, 720p at 1.33). A TV that reports a higher density for its size (720p at density 2: 640 x 360 dp) made every word half again
 * as large against the screen, so rows ran out of room. The settings now scale their text to the screen: below 960 x 540 dp the font scale is
 * multiplied by the screen's share of it (never under MIN), at or above it nothing changes; the person's own font size (Android's «حجم الخط»)
 * is kept on top. No Android in it (tests/TextScaleRun.java).
 */
final class TextScale {
    private TextScale() {}

    static final int WIDTH_DP = 960, HEIGHT_DP = 540;
    static final float MIN = 0.75f;

    /** the factor for a screen of widthDp x heightDp (0: not known, 1) */
    static float factor(int widthDp, int heightDp) {
        if (widthDp <= 0 || heightDp <= 0) return 1f;
        float f = Math.min(widthDp / (float) WIDTH_DP, heightDp / (float) HEIGHT_DP);
        return f >= 1f ? 1f : Math.max(MIN, f);
    }

    /** the context with its font scale fitted to its screen (the same one when nothing changes) */
    static android.content.Context fit(android.content.Context c) {
        android.content.res.Configuration now = c.getResources().getConfiguration();
        float f = factor(now.screenWidthDp, now.screenHeightDp);
        if (f >= 1f) return c;
        android.content.res.Configuration cfg = new android.content.res.Configuration(now);
        cfg.fontScale = now.fontScale * f;
        return c.createConfigurationContext(cfg);
    }
}
