package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.3.3 «المشاهد» (the scene home, docs/mockups/PERFORMANCE.md): the rules, with no Android in them (tests/ScenePlanRun.java runs them).
 * A scene is a photo, a blurred and brightened copy of it (the clock's glass) and, for depth, the scenery cut out of it with the sky
 * transparent (the mask), drawn over the clock. A pack's scenes are listed in its scenes.tsv, one line each:
 * id, Arabic name, English name, the digits' top and bottom (px of a 1080 screen), the mask's box "x,y,w,h" (px of 1080p) or "-",
 * dark ("1": a dark clock, for a pale scene), the photo's credit, and (4.3.3, may be missing) its tags: "night" (shown from Maghrib to Fajr when
 * «المشهد يتبع الوقت» is on), "fog" (shown in fog when «المشهد يتبع الطقس» is on), comma between.
 */
final class ScenePlan {
    private ScenePlan() { }

    /** the memory tiers, Ram's numbers (Ram itself needs Android; tests/scene_home_test.py keeps the two the same) */
    static final int LOW = 0, MID = 1, NORMAL = 2;

    /** the two packs: «مشاهد السعودية» is in the app; «مشاهد Couch TV» is downloaded when chosen (SceneDownload) */
    static final String SAUDI = "saudi", COUCH = "couch";

    /** how long a scene stays (s), as the settings say; LOW changes at most every 5 minutes, with no fade */
    static final int STAY_S = 20, MIN_STAY_S = 10, LOW_STAY_S = 300, FADE_MS = 1600;

    static final class Scene {
        final String pack, id, name, nameEn, credit, tags;
        final int digitsTop, digitsBottom;   // px of a 1080-line screen
        final int[] mask;                    // x, y, w, h of the cut-out in px of 1080p; null: no cut-out (the clock is drawn over the photo)
        final boolean dark;

        Scene(String pack, String id, String name, String nameEn, int top, int bottom, int[] mask, boolean dark, String credit, String tags) {
            this.pack = pack; this.id = id; this.name = name; this.nameEn = nameEn; this.digitsTop = top; this.digitsBottom = bottom;
            this.mask = mask; this.dark = dark; this.credit = credit; this.tags = "," + tags.trim() + ",";
        }

        boolean is(String tag) { return tags.contains("," + tag + ","); }
    }

    /** A pack's scenes.tsv: the good lines only (a bad line is left out, never half read). */
    static List<Scene> parse(String pack, String tsv) {
        List<Scene> out = new ArrayList<>();
        if (tsv == null) return out;
        for (String line : tsv.split("\n")) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            if (f.length < 8 || !f[0].matches("[a-z][a-z0-9-]{1,23}")) continue;
            try {
                int top = Integer.parseInt(f[3].trim()), bottom = Integer.parseInt(f[4].trim());
                if (top < 0 || bottom > 1080 || bottom - top < 120) continue;   // a clock too small to read is not a scene
                int[] mask = null;
                if (!"-".equals(f[5].trim())) {
                    String[] m = f[5].trim().split(",");
                    if (m.length != 4) continue;
                    mask = new int[] {Integer.parseInt(m[0]), Integer.parseInt(m[1]), Integer.parseInt(m[2]), Integer.parseInt(m[3])};
                    if (mask[0] < 0 || mask[1] < 0 || mask[2] <= 0 || mask[3] <= 0 || mask[0] + mask[2] > 1920 || mask[1] + mask[3] > 1080) continue;
                }
                out.add(new Scene(pack, f[0], f[1], f[2], top, bottom, mask, "1".equals(f[6].trim()), f[7], f.length > 8 ? f[8] : ""));
            } catch (NumberFormatException e) {
                // left out
            }
        }
        return out;
    }

    /** The packs that are on ("saudi,couch"): the known ones, in that order; none named is the Saudi pack alone. */
    static List<String> packs(String setting) {
        List<String> out = new ArrayList<>();
        String s = "," + (setting == null ? "" : setting) + ",";
        if (s.contains("," + SAUDI + ",")) out.add(SAUDI);
        if (s.contains("," + COUCH + ",")) out.add(COUCH);
        if (out.isEmpty()) out.add(SAUDI);
        return out;
    }

    /** LOW: the photo only, decoded at 1280 wide; MID and NORMAL: the screen's width (at most 1920: the files are 1080p). */
    static int decodeWidth(int tier, int screenW) { return tier == LOW ? Math.min(1280, screenW) : Math.min(1920, screenW); }

    /** the cut-out over the clock: not on LOW (no layer, no memory), the clock is drawn over the photo */
    static boolean mask(int tier) { return tier != LOW; }

    /** a fade between two scenes: not on LOW (it cuts) */
    static boolean fade(int tier) { return tier != LOW; }

    /** how long a scene stays, in ms */
    static long stayMs(int tier, int seconds) {
        if (tier == LOW) return LOW_STAY_S * 1000L;
        return Math.max(MIN_STAY_S, seconds <= 0 ? STAY_S : seconds) * 1000L;
    }

    /**
     * Where the clock's digits go on a screen of w x h: {left, top, right, bottom} in px. The scene says the height (its digits sit low enough for its
     * scenery to cross them); with the home's cards on the screen (they are on the left of an Arabic home) the clock takes the other half, at the start
     * side, else the middle. {@code rtl}: the start side is the right.
     */
    static int[] clockBox(Scene s, int w, int h, boolean cardsShown, boolean rtl) {
        float k = h / 1080f;
        int top = Math.round(s.digitsTop * k), bottom = Math.round(s.digitsBottom * k);
        int width = Math.round((bottom - top) * 2.6f);   // «١٢:٣٠» is about 2.6 digit-heights wide
        int max = cardsShown ? Math.round(w * 0.44f) : Math.round(w * 0.7f);
        if (width > max) {   // narrower: the digits shrink about their bottom line, so the scenery still crosses them
            int nh = Math.round(max / 2.6f);
            top = bottom - nh;
            width = max;
        }
        int centre = !cardsShown ? w / 2 : rtl ? Math.round(w * 0.73f) : Math.round(w * 0.27f);
        return new int[] {centre - width / 2, top, centre + width / 2, bottom};
    }

    /**
     * The scenes to take turns now: with «المشهد يتبع الطقس» and fog in the city, the fog scenes; with «المشهد يتبع الوقت», the night scenes from Maghrib
     * to Fajr and the others by day. A rule that would leave none is let go (the packs on may have no night or no fog scene): never an empty home.
     */
    static List<Scene> choose(List<Scene> all, boolean byTime, boolean night, boolean byWeather, boolean fog) {
        if (byWeather && fog) {
            List<Scene> f = new ArrayList<>();
            for (Scene s : all) if (s.is("fog")) f.add(s);
            if (!f.isEmpty()) return f;
        }
        if (byTime) {
            List<Scene> t = new ArrayList<>();
            for (Scene s : all) if (s.is("night") == night) t.add(s);
            if (!t.isEmpty()) return t;
        }
        return all;
    }

    /** Open-Meteo's weather codes for fog (45 fog, 48 rime fog) */
    static boolean fog(int wmo) { return wmo == 45 || wmo == 48; }

    /** «بقاء كل مشهد» as the screensaver gives it (10, 20, 60 or 300 s); anything else: 20 */
    static int stay(String v) {
        try { int s = Integer.parseInt(v); return s == 10 || s == 20 || s == 60 || s == 300 ? s : STAY_S; } catch (RuntimeException e) { return STAY_S; }
    }

    /** The next scene to show after {@code i} of {@code n} (they take turns in order); -1 for none. */
    static int next(int i, int n) { return n <= 0 ? -1 : (i + 1) % n; }
}
