import java.util.*;

/** Runs Parts (the screensaver's table) and HomeCardChoice.withoutParts (the home's cards), part by part: prints one line per fact; tests/parts_test.py reads them. */
public class PartsRun {
    public static void main(String[] a) throws Exception {
        Class<?> parts = Class.forName("sa.urtv.screensaver.Parts");
        String[][] all = (String[][]) field(parts, "ALL"), rows = (String[][]) field(parts, "ROWS");
        java.lang.reflect.Method hides = parts.getDeclaredMethod("hidesRow", String.class, String.class);
        java.lang.reflect.Method with = parts.getDeclaredMethod("with", String.class, String.class, boolean.class);
        java.lang.reflect.Method visible = parts.getDeclaredMethod("visible", List.class, String.class);
        java.lang.reflect.Method without = Class.forName("sa.urtv.home.HomeCardChoice").getDeclaredMethod("withoutParts", String.class, String.class);
        for (java.lang.reflect.Method m : new java.lang.reflect.Method[] {hides, with, visible, without}) m.setAccessible(true);
        String[][] cardParts = (String[][]) field(Class.forName("sa.urtv.home.HomeCardChoice"), "CARD_PARTS");
        String allCards = "";
        for (String[] cp : cardParts) allCards += (allCards.isEmpty() ? "" : ",") + cp[0];
        allCards += ",verse";   // a card of no part
        for (String[] p : all) {
            String id = p[0];
            String off = (String) with.invoke(null, "", id, true);
            // its rows: all hidden while it is off, none while it is on; the others' rows stay
            int mine = 0, hidden = 0, others = 0, otherHidden = 0;
            List<String[]> found = new ArrayList<>();
            for (String[] r : rows) {
                found.add(new String[] {r[0], "x"});
                if (r[1].equals(id)) { mine++; if ((Boolean) hides.invoke(null, off, r[0])) hidden++; }
                else { others++; if ((Boolean) hides.invoke(null, off, r[0])) otherHidden++; }
            }
            @SuppressWarnings("unchecked") List<String[]> seen = (List<String[]>) visible.invoke(null, found, off);
            boolean searchClean = true;
            for (String[] r : seen) for (String[] row : rows) if (row[0].equals(r[0]) && row[1].equals(id)) searchClean = false;
            // its cards: gone from the home while it is off, back when it is on
            String cardsOff = (String) without.invoke(null, allCards, "x=1&off=" + off);
            String cardsOn = (String) without.invoke(null, allCards, "x=1");
            boolean cardsGone = true, othersKept = true;
            for (String[] cp : cardParts) {
                boolean in = Arrays.asList(cardsOff.split(",")).contains(cp[0]);
                if (cp[1].equals(id) && in) cardsGone = false;
                if (!cp[1].equals(id) && !in && !"cards".equals(id)) othersKept = false;
            }
            String back = (String) with.invoke(null, off, id, false);
            System.out.println("part " + id + " rows=" + mine + " hidden=" + hidden + " othersHidden=" + otherHidden + " search=" + searchClean
                + " cardsGone=" + cardsGone + " othersKept=" + othersKept + " cardsOn=" + cardsOn.equals(allCards) + " back=" + back.isEmpty()
                + " verse=" + Arrays.asList(cardsOff.split(",")).contains("verse"));
        }
        System.out.println("two " + with.invoke(null, with.invoke(null, "", "markets", true), "games", true));
        System.out.println("unknown " + with.invoke(null, "markets", "nothing", true));
    }

    static Object field(Class<?> c, String n) throws Exception { java.lang.reflect.Field f = c.getDeclaredField(n); f.setAccessible(true); return f.get(null); }
}
