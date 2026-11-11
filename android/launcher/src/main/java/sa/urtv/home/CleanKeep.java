package sa.urtv.home;

import java.util.LinkedHashSet;
import java.util.Set;

/** «UrTV النظيف»'s list of what stays, as plain rules (no Android: tests/CleanKeepRun.java runs it): the names, their order, the default, parse and join. */
final class CleanKeep {
    private CleanKeep() { }

    /** what the person may keep, in the order of the customise sheet; the clock, the date and the apps bar are not here: they always stay. There is no card choice: the home's own card is never shown in clean mode (the prayer card below is the only one) */
    static final String WIFI = "wifi", GREETING = "greeting", SETTINGS = "settings";
    static final String[] OPTIONS = {WIFI, GREETING, SETTINGS};
    /** before the person chooses: the Wi-Fi mark and the settings button; the greeting goes */
    static final String DEFAULT_KEEP = "wifi,settings";
    /** «بطاقة الصلاة»: none / the next-prayer line under the date (the default) / one card of today's five (PrayerCardBox says where: «مكان البطاقة») */
    static final String NONE = "none", LINE = "line", CARD = "card", DEFAULT_PRAYER = LINE;
    static final String[] PRAYER_MODES = {NONE, LINE, CARD}, PRAYER_PLACES = {PrayerCardBox.CENTER, PrayerCardBox.BELOW, PrayerCardBox.OPPOSITE};

    static String prayer(String v) { return NONE.equals(v) || CARD.equals(v) ? v : DEFAULT_PRAYER; }

    static String place(String v) { return PrayerCardBox.pos(v); }

    /** "card,wifi" → {card, wifi}: only the known names, in the order of OPTIONS; null (never set) is the default */
    static Set<String> parse(String csv) {
        String v = csv == null ? DEFAULT_KEEP : csv;
        Set<String> given = new LinkedHashSet<>();
        for (String s : v.split(",")) given.add(s.trim());
        Set<String> out = new LinkedHashSet<>();
        for (String o : OPTIONS) if (given.contains(o)) out.add(o);
        return out;
    }

    static String join(Set<String> keep) {
        StringBuilder b = new StringBuilder();
        for (String o : OPTIONS) if (keep.contains(o)) b.append(b.length() > 0 ? "," : "").append(o);
        return b.toString();
    }

}
