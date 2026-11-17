package sa.urtv.home;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.5 «نسخة الإعدادات» (docs/BACKUP.md): the home's part of the settings file, schema 2. An allow-list of keys, each checked before anything is
 * applied; the old → new lines of the preview. The JSON itself is read and written by the screen (org.json): here the values are plain Java
 * (String, Boolean, List of String, Map of String to String), so this runs on a plain JVM (tests/BackupFileRun.java). Never in it: the PIN,
 * Wi-Fi, keys, tasks, photos (BACKUP.md).
 */
final class BackupFile {
    private BackupFile() { }

    static final int SCHEMA = 2;
    /** the home's keys, in the order of the preview */
    static final String[] HOME_KEYS = {"dock", "dock_exact", "hidden", "app_names", "input_names", "input_hidden", "keys"};
    static final int MAX_LIST = 300;

    /** the problems in a home part (empty: it may be applied); a key not in HOME_KEYS is a note, not a problem */
    static List<String> check(Map<String, Object> home, List<String> notes) {
        List<String> errs = new ArrayList<>();
        if (home == null) return errs;
        for (Map.Entry<String, Object> e : home.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            switch (k) {
                case "dock": case "hidden":
                    if (!pkgList(v)) errs.add(k + ": a list of package names");
                    break;
                case "input_hidden":
                    if (!list(v, MAX_LIST) || !allMatch((List<?>) v, true)) errs.add(k + ": a list of input ids");
                    break;
                case "dock_exact":
                    if (!(v instanceof Boolean)) errs.add(k + ": true or false");
                    break;
                case "app_names":
                    if (!names(v, false)) errs.add(k + ": package → an Arabic name of one line, up to " + APP_NAME_MAX + " characters");
                    break;
                case "input_names":
                    if (!names(v, true)) errs.add(k + ": input id → a name of one line, up to " + InputNames.MAX_LEN + " characters");
                    break;
                case "keys":
                    if (!keys(v)) errs.add(k + ": button[.2|.3] → a package, @switcher, @center or \"\"");
                    break;
                default:
                    if (notes != null) notes.add(k + ": not a setting of this version, left out");
            }
        }
        return errs;
    }

    /** the preview's lines for the home part: «key: old → new» for each allowed key whose value changes (a missing key is left as it is) */
    static List<String> diff(Map<String, Object> now, Map<String, Object> file) {
        List<String> out = new ArrayList<>();
        if (file == null) return out;
        for (String k : HOME_KEYS) {
            if (!file.containsKey(k)) continue;
            Object a = now == null ? null : now.get(k), b = file.get(k);
            if (!String.valueOf(a).equals(String.valueOf(b))) out.add(k + ": " + show(a) + " → " + show(b));
        }
        return out;
    }

    /** a value as the preview shows it: a list by its count, a map by its count, else itself */
    static String show(Object v) {
        if (v == null) return "—";
        if (v instanceof List) return String.valueOf(((List<?>) v).size());
        if (v instanceof Map) return String.valueOf(((Map<?, ?>) v).size());
        return String.valueOf(v);
    }

    private static boolean list(Object v, int max) { return v instanceof List && ((List<?>) v).size() <= max; }

    private static boolean pkgList(Object v) {
        if (!list(v, MAX_LIST)) return false;
        for (Object o : (List<?>) v) if (!(o instanceof String) || !KeyApps.validChoice((String) o) || ((String) o).isEmpty()) return false;
        return true;
    }

    private static boolean allMatch(List<?> l, boolean inputs) {
        for (Object o : l) if (!(o instanceof String) || (inputs ? !InputHide.validId((String) o) : !KeyApps.validChoice((String) o))) return false;
        return true;
    }

    private static boolean names(Object v, boolean inputs) {
        if (!(v instanceof Map) || ((Map<?, ?>) v).size() > MAX_LIST) return false;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
            if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String)) return false;
            String k = (String) e.getKey(), n = (String) e.getValue();
            if (inputs ? !InputHide.validId(k) : !KeyApps.validChoice(k) || k.isEmpty()) return false;
            if (inputs) { String clean = InputNames.clean(n); if (clean == null || clean.isEmpty()) return false; }
            else if (!appName(n)) return false;
        }
        return true;
    }

    /** AppNames.valid's rule (AppNames uses Android, so it is written here again; tests/backup_file_test.py keeps the two the same) */
    static final int APP_NAME_MAX = 30;

    static boolean appName(String name) {
        if (name == null || name.isEmpty() || name.length() > APP_NAME_MAX || name.indexOf('|') >= 0 || name.indexOf(';') >= 0 || name.indexOf('\n') >= 0) return false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if ((ch >= 0x0600 && ch <= 0x06FF) || (ch >= 0x0750 && ch <= 0x077F) || (ch >= 0xFB50 && ch <= 0xFEFC)) return InputNames.plain(name);   // AppNames.hasArabic
        }
        return false;
    }

    private static boolean keys(Object v) {
        if (!(v instanceof Map) || ((Map<?, ?>) v).size() > 3 * 40) return false;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
            if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String)) return false;
            if (!((String) e.getKey()).matches("[a-z]+(\\.[23])?") || !KeyApps.validTarget((String) e.getValue())) return false;
        }
        return true;
    }

    /** the allowed part of a home part, in HOME_KEYS order (what is applied, after check() found nothing) */
    static Map<String, Object> allowed(Map<String, Object> home) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (home != null) for (String k : HOME_KEYS) if (home.containsKey(k)) out.put(k, home.get(k));
        return out;
    }
}
