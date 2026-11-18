package sa.urtv.home;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 4.3.3 Per-model defaults and fixes (urtv-data devices/devices.json, docs/SCHEMAS.md there): an entry matches this TV by brand, model, device
 * (Build.BRAND, MODEL, DEVICE, any case; every one it names must match, and it names at least one) and an SDK range. Read at the home's start
 * from the kept copy; a default is applied only where the person (or an earlier run on this TV) has not set that setting, and never changes it
 * after. Known settings (any other is left for a later version):
 *  - sound_opens_general_settings: «مخرج الصوت» opens the TV's general settings (ControlCenter's "sound_bad"), for a TV whose sound screen crashes
 *    (the TCL G07), from the start instead of after its first crash.
 * The rule is plain Java on a JVM (tests/DeviceRuleRun.java); DeviceDefaults.apply is the one place that writes.
 */
final class DeviceRule {
    private DeviceRule() { }

    static final String PATH = "devices/devices.json", NAME = "devices.json";

    /** the defaults of every entry that matches, in the file's order (the first entry to set a key wins) */
    static Map<String, Object> defaults(Object doc, String brand, String model, String device, int sdk) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(doc instanceof Map)) return out;
        Object list = ((Map<?, ?>) doc).get("devices");
        if (!(list instanceof List)) return out;
        for (Object o : (List<?>) list) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> e = (Map<?, ?>) o;
            if (!(e.get("match") instanceof Map) || !(e.get("defaults") instanceof Map)) continue;
            if (!matches((Map<?, ?>) e.get("match"), brand, model, device, sdk)) continue;
            for (Map.Entry<?, ?> d : ((Map<?, ?>) e.get("defaults")).entrySet())
                if (d.getKey() instanceof String && !out.containsKey(d.getKey())) out.put((String) d.getKey(), d.getValue());
        }
        return out;
    }

    static boolean matches(Map<?, ?> m, String brand, String model, String device, int sdk) {
        boolean named = false;
        for (String[] k : new String[][] {{"brand", brand}, {"model", model}, {"device", device}}) {
            Object want = m.get(k[0]);
            if (want == null) continue;
            if (!(want instanceof String) || ((String) want).isEmpty()) return false;
            if (k[1] == null || !((String) want).toLowerCase(Locale.ROOT).equals(k[1].trim().toLowerCase(Locale.ROOT))) return false;
            named = true;
        }
        if (!named) return false;   // an entry for every TV is never one
        Object lo = m.get("sdk_min"), hi = m.get("sdk_max");
        if (lo != null && (!(lo instanceof Double) || sdk < (Double) lo)) return false;
        if (hi != null && (!(hi instanceof Double) || sdk > (Double) hi)) return false;
        return true;
    }
}
