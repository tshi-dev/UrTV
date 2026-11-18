package sa.urtv.screensaver;

/**
 * 4.4 «تركيبات مقترحة» (the owner, docs/mockups/presets/combos-*.jpg): how the mixes open from their tile. A quarter of a second, slowing at the end
 * (DecelerateInterpolator(EASE) is 1 - (1 - t)^3), the circle growing from the tile's centre to the panel's farthest corner, each mix from a third
 * of its size at the tile. Plain Java: tests/presets_test.py runs it.
 */
final class PresetMotion {
    private PresetMotion() { }

    static final long MS = 250;
    static final float EASE = 1.5f;     // DecelerateInterpolator's factor: 1 - (1 - t)^(2 EASE), the mockup's ease-out
    static final float SCALE0 = 0.35f;  // a mix's size at the tile, before it fans out

    /** the eased share at t (0..1) of the time, as the interpolator gives it */
    static float eased(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return (float) (1 - Math.pow(1 - t, 2 * EASE));
    }

    /** the circle's full radius: from (cx, cy) to the farthest corner of a w x h panel, so that it covers it all */
    static float radius(float cx, float cy, float w, float h) {
        float r = 0;
        for (float x : new float[] {0, w}) for (float y : new float[] {0, h}) r = Math.max(r, (float) Math.hypot(x - cx, y - cy));
        return r;
    }
}
