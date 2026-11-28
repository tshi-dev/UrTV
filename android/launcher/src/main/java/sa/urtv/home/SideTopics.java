package sa.urtv.home;

/**
 * 4.4 beta 4 «كنبة»'s side cards (SideCards) beside the strip of cards (HomeCard): the column never repeats what the strip shows. Plain Java (no
 * Android class): tests/SideTopicsRun.java runs it.
 */
final class SideTopics {
    private SideTopics() { }

    /** what each card is about: two cards about the same thing never show at once, one in the strip and one beside it */
    static String topic(String card) {
        switch (card) {
            case "nextprayer": case "prayers": case "prayerrow": return "prayer";
            case "hourly": case "forecast": return "weather";
            case "nextmatch": case "matches": case "nextmatches": case "matchday": case "livenow": return "match";
            case "today": return "prayer,match";   // «يومك»: the prayers and the games
            case "smart": return "prayer,weather,match";
            default: return card;
        }
    }

    /**
     * 4.4 beta 4 (the supervisor, run 23: the strip and the column both showed the next prayer and the hours' weather): the option's cards, less any
     * whose topic the strip has; a card that goes is replaced by the next match when that is not there either; null when none is left
     */
    static String beside(String wanted, String strip) {
        if (wanted == null) return null;
        java.util.Set<String> taken = new java.util.HashSet<>();
        if (strip != null && !"none".equals(strip))
            for (String k : strip.split(",")) if (!k.isEmpty()) taken.addAll(java.util.Arrays.asList(topic(k).split(",")));
        String[] want = wanted.split(",");
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.List<String> pool = new java.util.ArrayList<>(java.util.Arrays.asList(want));
        pool.add("nextmatch");   // the one card that can stand in
        for (String k : pool) {
            if (out.size() == want.length) break;
            if (out.contains(k) || taken.contains(topic(k))) continue;
            out.add(k);
            taken.add(topic(k));
        }
        if (out.isEmpty()) return null;
        StringBuilder b = new StringBuilder();
        for (String k : out) b.append(b.length() > 0 ? "," : "").append(k);
        return b.toString();
    }
}
