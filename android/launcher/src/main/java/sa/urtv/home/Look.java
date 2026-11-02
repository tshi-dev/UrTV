package sa.urtv.home;

/**
 * «المظهر» of the home screen (the idea from talkies, MIT: THIRD_PARTY_NOTICES.md): dark as it always was, light, or automatic, light
 * from sunrise until Maghrib in the screensaver's city and dark the rest of the day. The two palettes are the home's own colours:
 * the dark one is exactly what the home had, the light one a warm cream veil over the photo with dark text. No Android in here:
 * tests/LookRun.java runs it.
 */
final class Look {
    private Look() { }

    static final String AUTO = "auto", LIGHT = "light", DARK = "dark";

    /** A setting as the screensaver's provider gives it; anything else (an older screensaver says nothing) is the dark look. */
    static String mode(String raw) {
        return AUTO.equals(raw) || LIGHT.equals(raw) ? raw : DARK;
    }

    /**
     * Light now? Automatic: by the sun ({@code dayBySun}: between sunrise and Maghrib, from the prayer times), or, while those are
     * not known (null), from 6:00 until 18:30 on the TV's clock ({@code minuteOfDay}).
     */
    static boolean light(String mode, Boolean dayBySun, int minuteOfDay) {
        if (LIGHT.equals(mode)) return true;
        if (!AUTO.equals(mode)) return false;
        if (dayBySun != null) return dayBySun;
        return minuteOfDay >= 6 * 60 && minuteOfDay < 18 * 60 + 30;
    }

    /** The colours of one look. */
    static final class Palette {
        final int text, ink, soft, gold, shadow, titleShadow, shade, band, bandStroke;
        final int[] scrim;   // top to bottom over the photo
        /** text: the clock; ink: the app's title and the grid's names; soft: the date and the weather; gold: the greeting and labels */
        Palette(int text, int ink, int soft, int gold, int shadow, int titleShadow, int[] scrim, int shade, int band, int bandStroke) {
            this.text = text; this.ink = ink; this.soft = soft; this.gold = gold; this.shadow = shadow; this.titleShadow = titleShadow;
            this.scrim = scrim; this.shade = shade; this.band = band; this.bandStroke = bandStroke;
        }
    }

    /** What the home always had. */
    static final Palette DARK_LOOK = new Palette(0xFFFFFDF6, 0xFFFFFFFF, 0xFFC6BBA4, 0xFFD6AF6B, 0x80000000, 0x66000000,
            new int[]{0xB3000000, 0x40000000, 0x26000000, 0x8C000000, 0xE6000000}, 0xB3000000, 0x1FFFFFFF, 0x14FFFFFF);

    /**
     * Cream over the photo where the text is (the top, the dock and the grid), lighter in the middle; dark ink, a deep gold and a
     * dark soft line that read on the thin middle of the veil too (the rows' names under the favourites), with a light halo.
     */
    static final Palette LIGHT_LOOK = new Palette(0xFF14201A, 0xFF14201A, 0xFF3A3326, 0xFF5C3F0E, 0x66FFFFFF, 0x80FFFFFF,
            new int[]{0xD9F4EEE2, 0x73F4EEE2, 0x40F4EEE2, 0xB3F4EEE2, 0xF2F4EEE2}, 0xD9F4EEE2, 0x1A000000, 0x1F000000);

    static Palette of(boolean light) { return light ? LIGHT_LOOK : DARK_LOOK; }

    /**
     * «رملي» (GlassStyle's sand look, docs/UX-REVIEW.md): in the light the photo shows at the top and fades into sand toward the apps, with
     * ink for the words and the darker flag green (#166A45) where the other looks have gold; in the dark the home as it always was, its
     * gold turned flag green (400, #54C08A).
     */
    static final Palette SAND_LIGHT = new Palette(0xFF1B1F1D, 0xFF1B1F1D, 0xFF3F4A44, 0xFF166A45, 0x66FFFFFF, 0x80FFFFFF,
            new int[]{0xD9F6F4EF, 0x66F6F4EF, 0x40F6F4EF, 0xB8F6F4EF, 0xF2F6F4EF}, 0xE6F6F4EF, 0xB8FFFFFF, 0xD9FFFFFF);
    static final Palette SAND_DARK = new Palette(0xFFFFFDF6, 0xFFFFFFFF, 0xFFC9D0CB, 0xFF54C08A, 0x80000000, 0x66000000,
            new int[]{0xB3000000, 0x40000000, 0x26000000, 0x8C000000, 0xE6000000}, 0xB3000000, 0x24FFFFFF, 0x47FFFFFF);

    /** The palette of «المظهر ← الشكل» (a GlassStyle look key) in the dark or the light: «رملي» its own, every other look the home's two. */
    static Palette of(String look, boolean light) {
        if ("sand".equals(look)) return light ? SAND_LIGHT : SAND_DARK;
        return of(light);
    }

    /** A colour laid over another (both ARGB; the result is opaque when the one under it is). */
    static int over(int top, int under) {
        float a = (top >>> 24) / 255f;
        int r = Math.round(((top >> 16) & 255) * a + ((under >> 16) & 255) * (1 - a));
        int g = Math.round(((top >> 8) & 255) * a + ((under >> 8) & 255) * (1 - a));
        int b = Math.round((top & 255) * a + (under & 255) * (1 - a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** The WCAG contrast ratio of two opaque colours (1 to 21). */
    static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int c) {
        double r = lin(((c >> 16) & 255) / 255.0), g = lin(((c >> 8) & 255) / 255.0), b = lin((c & 255) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double lin(double v) { return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }
}
