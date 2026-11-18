package sa.urtv.home.setup;

/**
 * 4.4: the TV's list of accessibility services (enabled_accessibility_services), as the setup changes it for «إضافات تلفزيونك»: one entry added or taken out, the others
 * exactly as they were, the home's own entries of 4.3.2 and before (sa.urtv.home/…, gone from the app) left out. A list with a character the shell would read is not sent
 * (null). No Android in here (tests/WatchListRun.java).
 */
final class WatchList {
    private WatchList() { }

    private static final String SAFE = "[A-Za-z0-9_.$/:]*";

    /** the list with {@code svc} in it (once, at the end); "null" or "" is an empty list; null when the list cannot be sent */
    static String with(String list, String svc) {
        String rest = without(list, svc);
        if (!rest.matches(SAFE) || !svc.matches(SAFE)) return null;
        return rest.isEmpty() ? svc : rest + ":" + svc;
    }

    /** the list without {@code svc} and without the home's dead entries; "" when nothing is left */
    static String without(String list, String svc) {
        if (list == null || list.trim().isEmpty() || list.trim().equals("null")) return "";
        StringBuilder b = new StringBuilder();
        for (String e : list.trim().split(":")) {
            if (e.isEmpty() || e.equals(svc) || e.startsWith("sa.urtv.home/")) continue;
            if (b.length() > 0) b.append(':');
            b.append(e);
        }
        return b.toString();
    }

    /** a «key=value» line of the record */
    static String field(String record, String key) {
        if (record == null) return "";
        for (String l : record.split("\n")) if (l.startsWith(key + "=")) return l.substring(key.length() + 1).trim();
        return "";
    }
}
