package sa.urtv.home.tilawah;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.4 «صوتياتي»: the person's sound files, copied into this app's private folder (files/audio/a<N>.<ext>) with a small index in the
 * "audio_lib" preferences (id, name, bytes, length). AudioRules decides what may come in. Nothing here plays from a USB stick or from the
 * phone, nothing is sent anywhere, and nothing is kept outside the app: removing the app removes them.
 */
final class AudioStore {
    private static final String PREFS = "audio_lib";

    static final class Item {
        final int id;
        final String name, ext;
        final long bytes, ms;
        Item(int id, String name, String ext, long bytes, long ms) { this.id = id; this.name = name; this.ext = ext; this.bytes = bytes; this.ms = ms; }
    }

    /** what an add gave: the new item, or why not (AudioRules.refuse's word, "format", "copy") */
    static final class Added {
        final Item item;
        final String why;
        Added(Item item, String why) { this.item = item; this.why = why; }
    }

    private AudioStore() { }

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "audio");
        if (!d.isDirectory()) d.mkdirs();
        if (!swept) { swept = true; sweep(d, System.currentTimeMillis()); }
        return d;
    }

    private static volatile boolean swept;
    /** a piece of an upload or a copy left by a phone that went away or an app that stopped is removed after this long */
    static final long STALE_MS = 10 * 60_000L;

    /** 4.4: the leftovers of an upload the phone never finished, or a copy the app did not (they are not counted in «المستخدم») */
    static void sweep(File d, long now) {
        File[] all = d.listFiles();
        if (all == null) return;
        for (File f : all) if (AudioRules.leftover(f.getName()) && now - f.lastModified() > STALE_MS) f.delete();
    }

    static File file(Context c, Item it) { return new File(dir(c), "a" + it.id + "." + it.ext); }

    /** «سعة أكبر»: 50 MB a file and 500 MB for all (AudioRules); off by default */
    static boolean big(Context c) { return sp(c).getBoolean("big", false); }

    /** false when turning it off is refused (more is kept than the usual caps allow) */
    static synchronized boolean setBig(Context c, boolean on) {
        if (!on) {
            long largest = 0;
            for (Item it : list(c)) largest = Math.max(largest, it.bytes);
            if (!AudioRules.mayShrink(used(c), largest)) return false;
        }
        sp(c).edit().putBoolean("big", on).apply();
        return true;
    }

    static List<Item> list(Context c) {   // 4.4: a read, never waits for a copy (SharedPreferences is safe to read from any thread)
        List<Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item(o.getInt("id"), o.optString("name", ""), o.optString("ext", "mp3"), o.optLong("bytes", 0), o.optLong("ms", 0));
                if (file(c, it).length() > 0) out.add(it);   // a file that went missing is not listed
            }
        } catch (Exception ignored) { }
        return out;
    }

    static Item get(Context c, int id) {
        for (Item it : list(c)) if (it.id == id) return it;
        return null;
    }

    static long used(Context c) {
        long n = 0;
        for (Item it : list(c)) n += it.bytes;
        return n;
    }

    private static void save(Context c, List<Item> items) {
        JSONArray a = new JSONArray();
        try {
            for (Item it : items) a.put(new JSONObject().put("id", it.id).put("name", it.name).put("ext", it.ext).put("bytes", it.bytes).put("ms", it.ms));
        } catch (Exception ignored) { }
        sp(c).edit().putString("items", a.toString()).commit();
    }

    /** May a file of this size come in now? "" yes, else AudioRules' word (checked again when it is added). */
    static String mayAdd(Context c, long size) { return AudioRules.refuse(size, used(c), list(c).size(), big(c)); }

    /**
     * Copies `in` (size bytes, from a stick or a finished phone upload) into the library under `name`. Refused before any byte is written
     * when it is too big or there is no room; checked by its first bytes and by Android's reader before it is listed.
     */
    static Added add(Context c, InputStream in, long size, String name) {   // 4.4: the copy and Android's reader run outside the lock; only the number and the list take it
        String why = mayAdd(c, size);
        if (!why.isEmpty()) return new Added(null, why);
        int id = reserve(c);
        File tmp = new File(dir(c), "in-" + id + ".tmp");
        long wrote = 0;
        byte[] head = new byte[12];
        int headN = 0;
        try (OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[65536];
            int n;
            long cap = AudioRules.fileMax(big(c));
            while ((n = in.read(buf)) > 0) {
                if (headN < head.length) { int k = Math.min(n, head.length - headN); System.arraycopy(buf, 0, head, headN, k); headN += k; }
                wrote += n;
                if (wrote > cap) { tmp.delete(); return new Added(null, "too_big"); }   // the stick said less than it gave
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            tmp.delete();
            return new Added(null, "copy");
        }
        String ext = AudioRules.kind(head, headN);
        long ms = ext.isEmpty() ? 0 : length(tmp);
        if (ext.isEmpty() || ms <= 0) { tmp.delete(); return new Added(null, "format"); }
        String shown = AudioRules.name(name);
        return keep(c, tmp, new Item(id, shown.isEmpty() ? sa.urtv.home.S.n("صوت") : shown, ext, wrote, ms));
    }

    /** the next number, taken at once: two copies at the same time never get the same one */
    private static synchronized int reserve(Context c) {
        int id = sp(c).getInt("next", 1);
        sp(c).edit().putInt("next", id + 1).commit();
        return id;
    }

    /** the copied file into the list, the caps checked again with whatever came in meanwhile */
    private static synchronized Added keep(Context c, File tmp, Item it) {
        String why = AudioRules.refuse(it.bytes, used(c), list(c).size(), big(c));
        if (!why.isEmpty()) { tmp.delete(); return new Added(null, why); }
        if (!tmp.renameTo(file(c, it))) { tmp.delete(); return new Added(null, "copy"); }
        List<Item> items = list(c);
        items.add(it);
        save(c, items);
        return new Added(it, "");
    }

    static Added add(Context c, File src, String name) {
        try (InputStream in = new FileInputStream(src)) {
            return add(c, in, src.length(), name);
        } catch (IOException | SecurityException e) {
            return new Added(null, "copy");
        }
    }

    static synchronized void remove(Context c, int id) {
        List<Item> items = list(c), keep = new ArrayList<>();
        for (Item it : items) {
            if (it.id == id) file(c, it).delete(); else keep.add(it);
        }
        save(c, keep);
        DuaStore.soundGone(c, id);   // 4.4: the duas that played it are turned off (they stay; the person picks another sound)
        DuaScheduler.schedule(c);
        Prefs prefs = new Prefs(c);
        if (("a" + id).equals(prefs.iqamaSound())) {   // 4.4: the iqama had it: no sound now (its notice stays as chosen)
            prefs.setIqamaSound("");
            AdhanScheduler.schedule(c);
        }
    }

    static synchronized void rename(Context c, int id, String name) {
        List<Item> items = list(c), out = new ArrayList<>();
        String shown = AudioRules.name(name);
        for (Item it : items) out.add(it.id == id && !shown.isEmpty() ? new Item(it.id, shown, it.ext, it.bytes, it.ms) : it);
        save(c, out);
    }

    /** Android's reader gives the length: 0 when it cannot read it (not a sound it can play) */
    static long length(File f) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getPath());
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return d == null ? 0 : Long.parseLong(d);
        } catch (RuntimeException e) {
            return 0;
        } finally {
            try { r.release(); } catch (Exception ignored) { }
        }
    }

    /** the words a refusal is said in, on the TV and on the phone */
    static String refusal(Context c, String why) {
        boolean big = big(c);
        switch (why) {
            case "too_big": return sa.urtv.home.S.t("الملف أكبر من %s", AudioRules.size(AudioRules.fileMax(big)));
            case "full": return sa.urtv.home.S.t("ما بقي مكان: الحد لكل الصوتيات %s", AudioRules.size(AudioRules.totalMax(big)));
            case "too_many": return sa.urtv.home.S.t("وصلت الحد: %s ملفًا", String.valueOf(AudioRules.MAX_FILES));
            case "format": return sa.urtv.home.S.t("هذا ليس ملفًا صوتيًا يشتغل على التلفزيون");
            case "empty": return sa.urtv.home.S.t("الملف فارغ");
            default: return sa.urtv.home.S.t("تعذّر نسخ الملف");
        }
    }

    // ---- a phone upload in pieces (PhoneApi /api/audio): one at a time, kept in a temporary file until its last piece arrives

    private static File upload(Context c) { return new File(dir(c), "phone.part"); }
    private static long upTotal, upLast;
    private static String upName = "";
    private static int upDone;

    /**
     * The first piece of a new upload: "" when it may come (its size checked now), else the refusal word. 4.4: "busy" while another upload
     * sent a piece in the last BUSY_MS (before, a second phone, or a second pick on the same page, started over the first and broke both).
     */
    static synchronized String uploadStart(Context c, long total, String name) {
        String why = mayAdd(c, total);
        if (!why.isEmpty()) return why;
        if (AudioRules.busy(upTotal, upLast, System.currentTimeMillis())) return "busy";
        upload(c).delete();
        upTotal = total;
        upLast = System.currentTimeMillis();
        upName = name == null ? "" : name;
        return "";
    }

    /** One piece; returns "more", "done:<id>" when it was the last and the file is in, or a refusal word. */
    static String uploadPiece(Context c, long offset, byte[] data) {
        String[] name = new String[1];
        Object r = piece(c, offset, data, name);
        if (r instanceof String) return (String) r;
        File whole = (File) r;   // the last piece: the file is moved aside, the next upload may start, and the copy runs outside the lock
        Added a = add(c, whole, name[0]);
        whole.delete();
        return a.item != null ? "done:" + a.item.id : a.why;
    }

    private static synchronized Object piece(Context c, long offset, byte[] data, String[] name) {
        File part = upload(c);
        if (upTotal <= 0 || !AudioRules.nextChunk(part.length(), offset, data.length, upTotal)) return "order";
        try (OutputStream out = new FileOutputStream(part, true)) {
            out.write(data);
        } catch (IOException e) {
            part.delete();
            upTotal = 0;
            return "copy";
        }
        upLast = System.currentTimeMillis();
        if (part.length() < upTotal) return "more";
        File whole = new File(dir(c), "phone-" + (++upDone) + ".done");
        upTotal = 0;
        name[0] = upName;
        if (!part.renameTo(whole)) { part.delete(); return "copy"; }
        return whole;
    }
}
