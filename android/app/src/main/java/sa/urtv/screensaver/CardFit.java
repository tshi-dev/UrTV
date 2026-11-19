package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.4 the owner: a home card that does not suit the look says so on its choice, and when the look changes to one a card does not suit, the card is
 * swapped for the nearest of its own kind (never across kinds), with a short note; the person's own picks come back when the look does. No Android
 * in it (tests/card_fit_test.py runs it).
 *
 * Not suited: the strip under the clock (layout «couch»: the cards in one short row above the apps; with the big glass clock it is shorter still,
 * starting under the digits, CardBox.row) takes only a card of one line or a few short ones; a list card (the week's shows, the day's prayers, the
 * coming days' weather, the matches, the dhikr, the stocks, the tasks...) is cut there to two rows or less.
 */
final class CardFit {
    private CardFit() { }

    /** the cards a short strip holds whole */
    static final String[] COMPACT = {"nextprayer", "prayerrow", "hourly", "verse", "hadith", "wisdom", "year", "smart", "countdown"};

    // ---- 4.4 beta 2 (the owner approved docs/mockups/full-look, §٧): one table, every card with every look and every background

    /** the looks a card is decided for: the home's layouts, and «tvOS» (the full tvOS look: glass cards above the tvOS bar) */
    static final String[] LOOKS = {"classic", "football", "couch", "tvos"};
    static final String[] LOOK_NAMES = {"تلفزيونك", "كرة القدم", "كنبة", "tvOS"};
    /** the home's backgrounds (Prefs.HOME_BGS) */
    static final String[] BGS = {"photos", "scenes"};
    /** a decision: "" fits as it is; WIDE fits two cards wide; ROW4 fits drawn as a row of four; PICKS fits by picking only cards that fit; NO does not */
    static final String FITS = "", WIDE = "wide", ROW4 = "row4", PICKS = "picks", NO = "no";
    /** what each «fits, but» says, under the card's name */
    static final String[][] HOW = {{WIDE, "بعرض بطاقتين"}, {ROW4, "صف من ٤ أيام"}, {PICKS, "من البطاقات المناسبة"}};
    /** why a card does not fit a look (the one line under the dimmed choice) */
    static final String[][] WHY = {{"couch", "ما تناسب كنبة: قائمة طويلة تنقطع في الشريط"}, {"tvos", "ما تناسب tvOS: أطول من بطاقة tvOS"}};

    /**
     * The table: each card (Prefs.PANELS' keys, «none» aside), then one decision per look × background, in LOOKS × BGS order
     * (classic/photos, classic/scenes, football/photos, football/scenes, couch/photos, couch/scenes, tvos/photos, tvos/scenes).
     * The background decides nothing today (each card sits on its own glass, a flat fill on a weak TV, readable on both), but it is a column of
     * its own so a future background that cannot host a card says so here. tests/card_fit_test.py: every pair decided, and every card of PANELS in it.
     */
    static final String[][] TABLE = {
        {"nextprayer", FITS, FITS, FITS, FITS, FITS, FITS, FITS, FITS},
        {"prayerrow", FITS, FITS, FITS, FITS, FITS, FITS, FITS, FITS},
        {"prayers", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"hourly", FITS, FITS, FITS, FITS, FITS, FITS, FITS, FITS},
        {"forecast", FITS, FITS, FITS, FITS, NO, NO, ROW4, ROW4},
        {"nextmatch", FITS, FITS, FITS, FITS, NO, NO, FITS, FITS},
        {"matchday", FITS, FITS, FITS, FITS, NO, NO, FITS, FITS},
        {"livenow", FITS, FITS, FITS, FITS, NO, NO, FITS, FITS},
        {"matches", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"nextmatches", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"minitable", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"yesterday", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"weekend", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"cuppath", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"formula", FITS, FITS, FITS, FITS, NO, NO, FITS, FITS},
        {"national", FITS, FITS, FITS, FITS, NO, NO, FITS, FITS},
        {"verse", FITS, FITS, FITS, FITS, FITS, FITS, WIDE, WIDE},
        {"hadith", FITS, FITS, FITS, FITS, FITS, FITS, WIDE, WIDE},
        {"wisdom", FITS, FITS, FITS, FITS, FITS, FITS, FITS, FITS},
        {"adhkar", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"shows", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"stocks", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"tasks", FITS, FITS, FITS, FITS, NO, NO, NO, NO},
        {"year", FITS, FITS, FITS, FITS, FITS, FITS, FITS, FITS},
        {"smart", FITS, FITS, FITS, FITS, PICKS, PICKS, PICKS, PICKS},
        {"countdown", FITS, FITS, FITS, FITS, FITS, FITS, FITS, FITS},
        {"today", FITS, FITS, FITS, FITS, NO, NO, FITS, FITS},
        {"album", FITS, FITS, FITS, FITS, NO, NO, FITS, FITS},
    };

    /** the decision for a card on a look and a background; a card, look or background not in the table: FITS (as before 4.4 beta 2), «none» fits */
    static String decide(String card, String look, String bg) {
        int l = java.util.Arrays.asList(LOOKS).indexOf(look), b = java.util.Arrays.asList(BGS).indexOf(bg == null ? BGS[0] : bg);
        if (l < 0 || b < 0 || card == null || "none".equals(card)) return FITS;
        for (String[] r : TABLE) if (r[0].equals(card)) return r[1 + l * BGS.length + b];
        return FITS;
    }
    static boolean fits(String card, String look, String bg) { return !NO.equals(decide(card, look, bg)); }
    /** the one line under the card's name: why it does not fit, or how it fits («بعرض بطاقتين»), or "" */
    static String why(String card, String look, String bg) {
        String d = decide(card, look, bg);
        String[][] from = NO.equals(d) ? WHY : HOW;
        String key = NO.equals(d) ? look : d;
        for (String[] w : from) if (w[0].equals(key)) return w[1];
        return NO.equals(d) ? NOTE : "";
    }

    /** each card's kind, and within a kind the nearest compact card (none: it stays, with its note) */
    static final String[][] NEAREST = {{"prayers", "prayerrow"}, {"forecast", "hourly"}, {"adhkar", "verse"}};

    static final String NOTE = "ما يناسب هذا الشكل";

    static boolean compact(String card) { for (String c : COMPACT) if (c.equals(card)) return true; return false; }

    /** whether the card suits the home's layout («couch»: the strip) */
    static boolean suits(String card, String layout) { return fits(card, layout, null); }

    /** the note under the card's name in the picker, or "" */
    static String note(String card, String layout) { return suits(card, layout) ? "" : NOTE; }

    static String nearest(String card) { for (String[] n : NEAREST) if (n[0].equals(card)) return n[1]; return null; }

    /**
     * the cards the home shows with this layout: each unsuited card swapped for its nearest (when that one is not shown already), the others kept
     * (with their note). {@code out} gets one «بدّلنا X بـ Y» per swap.
     */
    static List<String> fitted(List<String> cards, String layout, List<String[]> swaps) {
        List<String> out = new ArrayList<>();
        for (String c : cards) {
            String n = suits(c, layout) ? null : nearest(c);
            if (n != null && !cards.contains(n) && !out.contains(n)) { out.add(n); if (swaps != null) swaps.add(new String[] {c, n}); }
            else if (!out.contains(c)) out.add(c);
        }
        return out;
    }

    /** the note for the swaps (shown as the settings' own notice, SettingsActivity.notice) (names from Prefs.PANELS), or null */
    static String swapNote(List<String[]> swaps, String[][] names) {
        if (swaps == null || swaps.isEmpty()) return null;
        StringBuilder b = new StringBuilder("بدّلنا ");
        for (int i = 0; i < swaps.size(); i++) {
            if (i > 0) b.append(" و");
            b.append(name(swaps.get(i)[0], names)).append(" بـ").append(name(swaps.get(i)[1], names));
        }
        return b.append(" لأنها ما تناسب هذا الشكل").toString();
    }

    private static String name(String k, String[][] names) { for (String[] n : names) if (n[0].equals(k)) return "«" + n[1] + "»"; return k; }
}
