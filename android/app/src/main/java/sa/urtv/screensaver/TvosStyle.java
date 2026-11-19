package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/**
 * 4.4 beta 2 «شكل شاشة الإعدادات: tvOS» (the owner approved docs/mockups/settings-style/, a2145c5c): the settings in the manner of tvOS Launcher
 * (Apache-2.0; named «tvOS» in the app, THIRD_PARTY_NOTICES.md). The 4.3.2 screen's logic (a first screen of sections, Back up a screen, the
 * same rows, search and actions); drawn as one list of large glass rows, the focused one white with dark words and a little bigger, and the
 * shown section's icon large in a glass square on the other side, its line under it. A weak TV («الوضع الخفيف», Motion.lite) gets the light
 * version by itself: the photo blurred once as always, flat fills, no shadow and no growth. Colours and numbers only; SettingsActivity builds
 * the views with them.
 */
final class TvosStyle {
    /** the weak-TV version: flat, nothing grows */
    final boolean light;

    TvosStyle(Context c) { light = Motion.lite(c); }

    /** the veil over the blurred home: a dark one, stronger on a weak TV (so the flat rows read the same over any photo) */
    static final int VEIL = 0x4D0A0C10, VEIL_LIGHT = 0x730A0C10;

    /** the focused row's growth (none on a weak TV) */
    static final float GROW = 1.035f;

    /** the glyph's square: its side as a part of the side pane, at most 280 dp, and its corner as a part of its side */
    static final float GLYPH_SIDE = 0.78f, GLYPH_CORNER = 0.22f, GLYPH_ICON = 0.5f;
    static final int GLYPH_MAX_DP = 280;

    float grow() { return light ? 1f : GROW; }

    int veil() { return light ? VEIL_LIGHT : VEIL; }

    /**
     * The look's colours in the tvOS manner, over the person's look (its warn colour and the page's base kept): white words, glass rows (a
     * white of 8 %, its edge 16 %), a white focused row with dark words. Always on the dark glass: the white focus needs it.
     */
    GlassStyle.Look look(GlassStyle.Look base) {
        return new GlassStyle.Look(false, base.base, 0x0A0C10, light ? 0.45f : 0.30f, 0xFFFFFFFF, 0xA6FFFFFF, 0xFFFFFFFF, base.warn,
                light ? 0x1AFFFFFF : 0x14FFFFFF, 0x29FFFFFF, 0xF01C1E22, 0xFFFFFFFF, 0xFF111111, 0x8C000000, 0xCC14161A);
    }

    /** the glyph's glass square: a soft white gradient with a light edge (flat on a weak TV) */
    Drawable glyphBox(float radius, int stroke) {
        GradientDrawable g = light ? new GradientDrawable()
                : new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0x38FFFFFF, 0x0DFFFFFF});
        if (light) g.setColor(0x14FFFFFF);
        g.setCornerRadius(radius);
        g.setStroke(Math.max(1, stroke), light ? 0x1FFFFFFF : 0x38FFFFFF);
        return g;
    }
}
