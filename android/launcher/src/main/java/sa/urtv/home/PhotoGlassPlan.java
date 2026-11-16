package sa.urtv.home;

/**
 * 4.4 «الساعة: زجاجية خلف الصورة» on our own photos: the rules, with no Android in them (tests/PhotoGlassRun.java runs them). Which cut-out belongs to
 * the photo on the screen (by the home's key for it), and where the glass clock's digits go: low enough for the photo's scenery (its skyline, from the
 * cut-out's index) to cross their bottom, high enough to read; a photo with no cut-out has the clock in front, at its usual place.
 */
final class PhotoGlassPlan {
    private PhotoGlassPlan() { }

    /** the set that is never cut (the owner: no clock behind the Kaaba or the Prophet's Mosque) */
    static final String NEVER = "haramain";
    /** the digits are this share of the screen's height tall; with no scenery to cross they stand here */
    static final float DIGITS = 0.26f, TOP = 0.16f;
    /** the scenery crosses at most this share of the digits, and is used only between these heights (a skyline too high or too low: no overlap) */
    static final float CROSS = 0.3f, LOWEST = 0.62f, HIGHEST = 0.25f;
    /** 4.4 «التخطيط: كنبة»: the cards' strip starts about here; the digits end above it */
    static final float ROW_TOP = 0.55f;

    /**
     * {set, name} of the cut-out for the home's key of a photo: "pack:<id>:<n>" (a photo pack's), "photo:<file>" (our hosted set),
     * "bundled:wallpapers/<file>" (our photos in the app, the same names); null for the person's own photos, for «الحرمين», and for anything else.
     */
    static String[] key(String wall) {
        if (wall == null) return null;
        if (wall.startsWith("pack:")) {
            String[] p = wall.split(":");
            if (p.length != 3 || NEVER.equals(p[1]) || !p[1].matches("[a-z][a-z0-9-]{1,30}") || !p[2].matches("[0-9]{1,3}")) return null;
            return new String[] {p[1], p[2]};
        }
        String file = wall.startsWith("photo:") ? wall.substring(6) : wall.startsWith("bundled:wallpapers/") ? wall.substring(19) : null;
        if (file == null || !file.endsWith(".jpg")) return null;
        String stem = file.substring(0, file.length() - 4);
        return stem.matches("[A-Za-z0-9_-]{4,40}") ? new String[] {"photos", stem} : null;
    }

    /**
     * {left, top, right, bottom} of the digits on a screen of w x h. With the cards on the screen (left in Arabic) the clock takes the start half,
     * else the middle. {@code skyline}: the scenery's top in each of 48 columns (px of a 1080 screen), null for a photo with no cut-out.
     * {@code row}: «كنبة»'s cards are in a strip under the clock: the digits never reach it (a scenery that low: the usual place).
     */
    static int[] box(int[] skyline, int w, int h, boolean cardsShown, boolean rtl, boolean row) {
        int digitH = Math.round(h * DIGITS);
        int width = Math.min(Math.round(digitH * 2.6f), cardsShown ? Math.round(w * 0.44f) : Math.round(w * 0.7f));
        int centre = !cardsShown ? w / 2 : rtl ? Math.round(w * 0.73f) : Math.round(w * 0.27f);
        int left = centre - width / 2, right = centre + width / 2;
        int top = Math.round(h * TOP), bottom = top + digitH;
        if (skyline != null && skyline.length == 48) {
            int sy = Integer.MAX_VALUE;
            for (int c = Math.max(0, left * 48 / w); c <= Math.min(47, right * 48 / w); c++) sy = Math.min(sy, skyline[c]);
            int y = Math.round(sy * h / 1080f);
            if (y >= h * HIGHEST && y <= h * LOWEST) {   // the scenery's top is where it can cross the digits' bottom
                bottom = y + Math.round(digitH * CROSS);
                top = bottom - digitH;
                if (top < Math.round(h * 0.1f)) { top = Math.round(h * 0.1f); bottom = top + digitH; }
                if (row && bottom > h * ROW_TOP) { top = Math.round(h * TOP); bottom = top + digitH; }
            }
        }
        return new int[] {left, top, right, bottom};
    }
}
