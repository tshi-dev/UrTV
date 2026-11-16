package sa.urtv.home.tilawah;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.4 «الأدعية والتذكيرات»: the list, kept in the "duas" preferences as JSON. Each one: a name, its sound (a file of «صوتياتي» "a:<id>", or a
 * surah of «تلاوة» "q:<reciter>:<surah>"), its rule (DuaRule), its own volume, «فوق تطبيق آخر» (off by default: another app's sound is
 * never cut), on or off, and the last moment it played (so one alarm never plays it twice).
 */
final class DuaStore {
    static final class Dua {
        int id;
        String name = "";
        String sound = "";
        DuaRule rule = new DuaRule();
        int volume = 30;
        boolean overOthers;
        boolean on = true;
        long lastDue;

        Dua copy() {
            Dua d = new Dua();
            d.id = id; d.name = name; d.sound = sound; d.rule = rule.copy() != null ? rule.copy() : new DuaRule();
            d.volume = volume; d.overOthers = overOthers; d.on = on; d.lastDue = lastDue;
            return d;
        }
    }

    private DuaStore() { }

    /** at most this many: one alarm looks them all up */
    static final int MAX = 40;

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences("duas", Context.MODE_PRIVATE); }

    static synchronized List<Dua> list(Context c) {
        List<Dua> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                DuaRule r = DuaRule.parse(o.optString("rule", ""));
                if (r == null) continue;   // a rule of a newer version we cannot read: left out, never guessed
                Dua d = new Dua();
                d.id = o.getInt("id");
                d.name = o.optString("name", "");
                d.sound = o.optString("sound", "");
                d.rule = r;
                d.volume = Math.max(10, Math.min(100, o.optInt("volume", 30)));
                d.overOthers = o.optBoolean("over", false);
                d.on = o.optBoolean("on", true);
                d.lastDue = o.optLong("last", 0);
                out.add(d);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static Dua get(Context c, int id) {
        for (Dua d : list(c)) if (d.id == id) return d;
        return null;
    }

    private static void save(Context c, List<Dua> items) {
        JSONArray a = new JSONArray();
        try {
            for (Dua d : items) {
                a.put(new JSONObject().put("id", d.id).put("name", d.name).put("sound", d.sound).put("rule", d.rule.text())
                        .put("volume", d.volume).put("over", d.overOthers).put("on", d.on).put("last", d.lastDue));
            }
        } catch (Exception ignored) { }
        sp(c).edit().putString("items", a.toString()).commit();
    }

    /** a new one (id given here) or a changed one; false when the list is full */
    static synchronized boolean put(Context c, Dua d) {
        List<Dua> items = list(c);
        if (d.id <= 0) {
            if (items.size() >= MAX) return false;
            d.id = sp(c).getInt("next", 1);
            sp(c).edit().putInt("next", d.id + 1).apply();
            items.add(d);
        } else {
            boolean found = false;
            for (int i = 0; i < items.size(); i++) if (items.get(i).id == d.id) { items.set(i, d); found = true; }
            if (!found) items.add(d);
        }
        save(c, items);
        return true;
    }

    static synchronized void remove(Context c, int id) {
        List<Dua> keep = new ArrayList<>();
        for (Dua d : list(c)) if (d.id != id) keep.add(d);
        save(c, keep);
    }

    /** a file of «صوتياتي» was deleted: the duas that played it are turned off (they stay, the person picks another sound) */
    static synchronized void soundGone(Context c, int audioId) {
        List<Dua> items = list(c);
        for (Dua d : items) if (("a:" + audioId).equals(d.sound)) d.on = false;
        save(c, items);
    }

    /** what the sound plays: a file's path, a surah's address, or null when it is gone */
    static String source(Context c, Dua d) {
        if (d.sound.startsWith("a:")) {
            try {
                AudioStore.Item it = AudioStore.get(c, Integer.parseInt(d.sound.substring(2)));
                return it == null ? null : AudioStore.file(c, it).getPath();
            } catch (NumberFormatException e) { return null; }
        }
        if (d.sound.startsWith("q:")) {
            String[] p = d.sound.split(":");
            if (p.length != 3) return null;
            Catalog cat = Catalog.get(c);
            int r = cat.reciterIndex(p[1]);
            try {
                int n = Integer.parseInt(p[2]);
                if (r < 0 || n < 1 || n > 114) return null;
                return cat.reciters.get(r).server + String.format(java.util.Locale.US, "%03d", n) + ".mp3";
            } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /** the sound's name for the screens */
    static String soundName(Context c, Dua d) {
        if (d.sound.startsWith("a:")) {
            try {
                AudioStore.Item it = AudioStore.get(c, Integer.parseInt(d.sound.substring(2)));
                return it == null ? sa.urtv.home.S.t("الملف محذوف") : it.name;
            } catch (NumberFormatException e) { return ""; }
        }
        if (d.sound.startsWith("q:")) {
            String[] p = d.sound.split(":");
            Catalog cat = Catalog.get(c);
            int r = p.length == 3 ? cat.reciterIndex(p[1]) : -1;
            try {
                return r < 0 ? "" : sa.urtv.home.S.t("سورة %1$s، %2$s", cat.surahName(Integer.parseInt(p[2])), cat.reciters.get(r).name);
            } catch (NumberFormatException e) { return ""; }
        }
        return sa.urtv.home.S.t("اختر الصوت");
    }
}
