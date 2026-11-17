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

    /** each card's kind, and within a kind the nearest compact card (none: it stays, with its note) */
    static final String[][] NEAREST = {{"prayers", "prayerrow"}, {"forecast", "hourly"}, {"adhkar", "verse"}};

    static final String NOTE = "ما يناسب هذا الشكل";

    static boolean compact(String card) { for (String c : COMPACT) if (c.equals(card)) return true; return false; }

    /** whether the card suits the home's layout («couch»: the strip) */
    static boolean suits(String card, String layout) { return !"couch".equals(layout) || "none".equals(card) || compact(card); }

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

    /** the toast for the swaps (names from Prefs.PANELS), or null */
    static String toast(List<String[]> swaps, String[][] names) {
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
