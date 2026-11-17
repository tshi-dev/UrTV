package sa.urtv.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.5 «نسخة الإعدادات» (docs/BACKUP.md): the home's part of the file on this TV. current() reads it from the home's preferences, apply() writes a
 * part BackupFile.check() found nothing wrong in, in one edit, and the home and the add-on are told once. The JSON conversions (org.json ↔ plain
 * Java) are here so BackupFile stays pure.
 */
final class HomeBackup {
    private HomeBackup() { }

    /** the home's part as it is now (only what is set) */
    static Map<String, Object> current(Context c) {
        Map<String, Object> m = new LinkedHashMap<>();
        SharedPreferences p = Dock.prefs(c);
        if (p.contains("dock")) m.put("dock", new ArrayList<>(Dock.saved(c)));
        m.put("dock_exact", Dock.exact(c));
        List<String> hidden = new ArrayList<>(Dock.prefSet(c, "hidden"));
        hidden.remove("");
        Collections.sort(hidden);
        m.put("hidden", hidden);
        m.put("app_names", new LinkedHashMap<String, Object>(AppNames.given(c)));
        m.put("input_names", new LinkedHashMap<String, Object>(InputLabels.given(c)));
        m.put("input_hidden", new ArrayList<>(InputLabels.hidden(c)));
        Map<String, Object> keys = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : Dock.keyApps(c).entrySet()) if (!e.getValue().isEmpty()) keys.put(e.getKey(), e.getValue());
        m.put("keys", keys);
        return m;
    }

    /** writes an allowed, checked part (BackupFile.allowed after BackupFile.check found nothing); a key the part does not have is left as it is */
    @SuppressWarnings("unchecked")
    static void apply(Context c, Map<String, Object> part) {
        SharedPreferences.Editor e = Dock.prefs(c).edit();
        if (part.containsKey("dock")) e.putString("dock", String.join(",", (List<String>) part.get("dock")));
        if (part.containsKey("dock_exact")) e.putBoolean("dock_exact", Boolean.TRUE.equals(part.get("dock_exact")));
        if (part.containsKey("hidden")) {
            List<String> h = (List<String>) part.get("hidden");
            List<String> shown = new ArrayList<>(Dock.prefSet(c, "shown"));
            shown.removeAll(h);
            shown.remove("");
            e.putString("hidden", String.join(",", h)).putString("shown", String.join(",", shown));
        }
        if (part.containsKey("app_names")) {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<String, Object> x : ((Map<String, Object>) part.get("app_names")).entrySet()) b.append(x.getKey()).append('|').append(x.getValue()).append('\n');
            e.putString("names", b.toString());
        }
        if (part.containsKey("input_names")) {
            Map<String, String> n = new LinkedHashMap<>();
            for (Map.Entry<String, Object> x : ((Map<String, Object>) part.get("input_names")).entrySet()) n.put(x.getKey(), String.valueOf(x.getValue()));
            e.putString("input_names", InputNames.serialize(n));
        }
        if (part.containsKey("input_hidden")) e.putString("input_hidden", InputHide.serialize(new java.util.LinkedHashSet<>((List<String>) part.get("input_hidden"))));
        if (part.containsKey("keys")) {
            Map<String, Object> k = (Map<String, Object>) part.get("keys");
            for (RemoteButtons.Button b : RemoteButtons.buttons(Dock.remoteTv()))   // this remote's buttons only; a button another remote has is left out
                for (String n : new String[]{"", ".2", ".3"}) e.putString("key_app_" + b.id + n, k.containsKey(b.id + n) ? String.valueOf(k.get(b.id + n)) : "");
        }
        e.commit();
        c.sendBroadcast(new Intent(Dock.CHANGED).setPackage(c.getPackageName()));
        Dock.tellExtras(c);
    }

    // ---------------------------------------------------------------- org.json ↔ plain Java (Map, List, String, Boolean, Integer, Double)

    static Object plain(Object v) throws JSONException {
        if (v == null || v == JSONObject.NULL) return null;
        if (v instanceof JSONObject) {
            Map<String, Object> m = new LinkedHashMap<>();
            JSONObject o = (JSONObject) v;
            for (Iterator<String> it = o.keys(); it.hasNext(); ) { String k = it.next(); m.put(k, plain(o.get(k))); }
            return m;
        }
        if (v instanceof JSONArray) {
            List<Object> l = new ArrayList<>();
            JSONArray a = (JSONArray) v;
            for (int i = 0; i < a.length(); i++) l.add(plain(a.get(i)));
            return l;
        }
        return v;
    }

    static Object json(Object v) throws JSONException {
        if (v instanceof Map) {
            JSONObject o = new JSONObject();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) o.put(String.valueOf(e.getKey()), json(e.getValue()));
            return o;
        }
        if (v instanceof List) {
            JSONArray a = new JSONArray();
            for (Object x : (List<?>) v) a.put(json(x));
            return a;
        }
        return v == null ? JSONObject.NULL : v;
    }
}
