package sa.urtv.home;

/**
 * How big and how strong the coloured glow behind the focused app is, as plain numbers so tests/glow_look_test.py can hold them
 * without Android. Two reports called the first look distracting (a radius of 0.62 of the screen's width, 50% then 14% opaque, and
 * a 14 dp lift under the tile); it is smaller and softer now. The tile's gold-white frame and its 10% growth are not touched.
 */
final class GlowLook {
    private GlowLook() { }

    /** The radial gradient's radius, as a share of the screen's width. */
    static final float RADIUS = 0.40f;
    /** Opacity at the centre and halfway out; the edge is transparent. */
    static final float CENTRE_ALPHA = 0.26f, MID_ALPHA = 0.07f;
    /** «خفيف»: the glow's opacity is this share of «مفعّل»'s; «مطفأ» draws none (and the view is never made). */
    static final float LIGHT_SHARE = 1f / 3f;
    static float share(String mode) { return "on".equals(mode) ? 1f : "light".equals(mode) ? LIGHT_SHARE : 0f; }
    /** How far the focused tile is lifted (its shadow), in dp. */
    static final int LIFT_DP = 8;
}
