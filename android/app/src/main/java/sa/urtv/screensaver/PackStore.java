package sa.urtv.screensaver;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The photo packs on the TV (rules: PhotoPacks). The index (photos/packs.json on our repository) is read when the picker opens and the last
 * good copy kept; a pack is fetched only when the person chooses it, written to a temporary file, checked against the index's sha256 and only
 * then opened, entry by entry through PhotoPacks.safeEntry, into files/packs/ID/ (N.jpg and credits.json). It is built in a folder of its own
 * and moved into place whole, so a pack is either all there or not there. Nothing here runs on the UI thread.
 */
final class PackStore {
    static final String INDEX_URL = PhotoCache.REPO + "photos/packs.json";
    private PackStore() { }

    static File root(Context c) { return new File(c.getFilesDir(), "packs"); }

    static File dir(Context c, String id) { return new File(root(c), id); }

    static File photo(Context c, String id, int n) { return new File(dir(c, id), n + ".jpg"); }

    static File credits(Context c, String id) { return new File(dir(c, id), "credits.json"); }

    /** a cover kept beside the packs, so the picker has it offline */
    static File cover(Context c, String id) { return new File(root(c), id + ".cover.jpg"); }

    /** how many photos each downloaded pack holds (a folder with a gap or without its credits is not a pack on the TV) */
    static Map<String, Integer> installed(Context c) {
        Map<String, Integer> out = new HashMap<>();
        File[] dirs = root(c).listFiles(File::isDirectory);
        if (dirs == null) return out;
        for (File d : dirs) {
            if (!PhotoPacks.validId(d.getName()) || !new File(d, "credits.json").isFile()) continue;
            boolean[] has = new boolean[PhotoPacks.PHOTOS_MAX + 2];
            int n = 0;
            for (int k = 1; k <= PhotoPacks.PHOTOS_MAX; k++) { has[k] = new File(d, k + ".jpg").isFile(); if (has[k]) n = k; }
            if (PhotoPacks.complete(has, n)) out.put(d.getName(), n);
        }
        return out;
    }

    /** the packs of the index: the network's answer (kept for next time), else the kept copy, else none; valid ones only */
    static List<PhotoPacks.Pack> index(Context c) {
        File keep = new File(root(c), "index.json");
        String json = null;
        try {
            byte[] data = get(INDEX_URL, Bounded.JSON_MAX);
            json = new String(data, "UTF-8");
            List<PhotoPacks.Pack> v = parse(json);
            if (v == null) json = null;
            else {
                root(c).mkdirs();
                try (FileOutputStream out = new FileOutputStream(keep)) { out.write(data); }
                return v;
            }
        } catch (IOException | RuntimeException ignored) { }
        try (InputStream in = new FileInputStream(keep)) {
            List<PhotoPacks.Pack> v = parse(new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8"));
            if (v != null) return v;
        } catch (IOException | RuntimeException ignored) { }
        return new ArrayList<>();
    }

    /** id → the pack's name in the app's language, from the copy of the index kept on the TV (never the network: the screens call this on the UI thread); empty when none is kept */
    static Map<String, String> names(Context c) {
        Map<String, String> out = new java.util.HashMap<>();
        try (InputStream in = new FileInputStream(new File(root(c), "index.json"))) {
            List<PhotoPacks.Pack> v = parse(new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8"));
            if (v != null) for (PhotoPacks.Pack p : v) out.put(p.id, S.en() ? p.nameEn : p.name);
        } catch (IOException | RuntimeException ignored) { }
        return out;
    }

    /** null when the text is not an index at all */
    static List<PhotoPacks.Pack> parse(String json) {
        try {
            JSONObject o = new JSONObject(json);
            if (o.optInt("version") != 1) return null;
            JSONArray a = o.getJSONArray("packs");
            List<PhotoPacks.Pack> all = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject p = a.optJSONObject(i);
                if (p == null) continue;
                all.add(new PhotoPacks.Pack(p.optString("id"), p.optString("name"), p.optString("name_en", p.optString("name")),
                    p.optString("source"), p.optString("source_en", p.optString("source")), p.optString("cover"),
                    p.optString("url"), p.optString("sha256"), p.optInt("count"), p.optLong("bytes")));
            }
            return PhotoPacks.keepValid(all);
        } catch (org.json.JSONException e) {
            return null;
        }
    }

    /** The cover for a pack: the kept file, fetched once when the index says where (at most one photo's size); null when none. */
    static File coverOf(Context c, PhotoPacks.Pack p) {
        File f = cover(c, p.id);
        if (f.isFile()) return f;
        if (p.cover.isEmpty()) return null;
        try {
            byte[] data = get(p.cover, (int) PhotoPacks.PHOTO_MAX);
            if (!PhotoPacks.looksJpeg(data) || !PhotoCache.validImage(data)) return null;
            root(c).mkdirs();
            try (FileOutputStream out = new FileOutputStream(f)) { out.write(data); }
            return f;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** null when the pack is now on the TV, else why not (the words are the picker's: the reason is one of Reason's) */
    static String download(Context c, PhotoPacks.Pack p) {
        if (!PhotoPacks.valid(p)) return "bad";
        File root = root(c);
        if (!root.isDirectory() && !root.mkdirs()) return "space";
        File zip = new File(root, p.id + ".zip.tmp"), work = new File(root, p.id + ".work");
        try {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(p.url).openConnection();
                con.setConnectTimeout(15000);
                con.setReadTimeout(30000);
                try {
                    if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return "net";
                    long len = con.getContentLengthLong();
                    if (len > PhotoPacks.ZIP_MAX) return "bad";
                    try (InputStream in = con.getInputStream(); FileOutputStream out = new FileOutputStream(zip)) {
                        byte[] buf = new byte[16384];
                        long total = 0;
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            total += n;
                            if (total > PhotoPacks.ZIP_MAX) return "bad";
                            out.write(buf, 0, n);
                        }
                    }
                } finally {
                    con.disconnect();
                }
            } catch (IOException e) {
                return zip.length() > 0 && !root.canWrite() ? "space" : "net";
            }
            // the check: nothing of the zip is opened before this
            String digest;
            try (InputStream in = new FileInputStream(zip)) { digest = PhotoPacks.sha256(in, PhotoPacks.ZIP_MAX); } catch (IOException e) { return "bad"; }
            if (!PhotoPacks.matches(digest, p.sha256)) return "sha";
            deleteTree(work);
            if (!work.mkdirs()) return "space";
            String why = unpack(zip, work, p);
            if (why != null) { deleteTree(work); return why; }
            File dest = dir(c, p.id);
            deleteTree(dest);
            if (!work.renameTo(dest)) { deleteTree(work); return "space"; }
            return null;
        } finally {
            zip.delete();
            deleteTree(work);
        }
    }

    /** the zip's files into work: only safe names, each photo a JPEG that decodes, all 1..count there; null when good */
    private static String unpack(File zip, File work, PhotoPacks.Pack p) {
        boolean[] has = new boolean[PhotoPacks.PHOTOS_MAX + 2];
        boolean credits = false;
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            int entries = 0;
            while ((e = z.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (!PhotoPacks.safeEntry(name) || ++entries > PhotoPacks.PHOTOS_MAX + 1) return "bad";
                boolean isJpg = name.endsWith(".jpg");
                byte[] data = Bounded.readAll(z, (int) (isJpg ? PhotoPacks.PHOTO_MAX : Bounded.JSON_MAX));
                if (isJpg) {
                    if (!PhotoPacks.looksJpeg(data) || !PhotoCache.validImage(data)) return "bad";
                    int n = Integer.parseInt(name.substring(0, name.length() - 4));
                    if (n > PhotoPacks.PHOTOS_MAX) return "bad";
                    has[n] = true;
                } else {
                    try { new JSONObject(new String(data, "UTF-8")); } catch (org.json.JSONException x) { return "bad"; }
                    credits = true;
                }
                try (FileOutputStream out = new FileOutputStream(new File(work, name))) { out.write(data); }
            }
        } catch (IOException | RuntimeException e) {
            return "bad";
        }
        return credits && PhotoPacks.complete(has, p.count) ? null : "bad";
    }

    /** «حذف»: the pack's files are gone */
    static void delete(Context c, String id) {
        if (PhotoPacks.validId(id)) deleteTree(dir(c, id));
    }

    /** the credits of an installed pack as the page reads them: {"1":"author · licence", ...} */
    static String creditsJson(Context c, String id) {
        if (!PhotoPacks.validId(id)) return null;
        try (InputStream in = new FileInputStream(credits(c, id))) {
            JSONObject o = new JSONObject(new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8"));
            JSONArray a = o.optJSONArray("photos");
            JSONObject out = new JSONObject();
            for (int i = 0; a != null && i < a.length(); i++) {
                JSONObject x = a.optJSONObject(i);
                if (x == null) continue;
                String f = x.optString("file");
                if (f.endsWith(".jpg")) out.put(f.substring(0, f.length() - 4), PhotoPacks.creditLine(x.optString("author"), x.optString("licence")));
            }
            return out.toString();
        } catch (IOException | RuntimeException | org.json.JSONException e) {
            return null;
        }
    }

    /** the credits of an installed pack as lines (the file, the author, the licence, the link), for «حقوق الصور» */
    static List<String[]> creditRows(Context c, String id) {
        List<String[]> rows = new ArrayList<>();
        if (!PhotoPacks.validId(id)) return rows;
        try (InputStream in = new FileInputStream(credits(c, id))) {
            JSONArray a = new JSONObject(new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8")).optJSONArray("photos");
            for (int i = 0; a != null && i < a.length(); i++) {
                JSONObject x = a.optJSONObject(i);
                if (x != null) rows.add(new String[] {x.optString("file"), x.optString("author"), x.optString("licence"), x.optString("link")});
            }
        } catch (IOException | RuntimeException | org.json.JSONException ignored) { }
        return rows;
    }

    private static byte[] get(String url, int max) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(15000);
        con.setReadTimeout(30000);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("http " + con.getResponseCode());
            try (InputStream in = con.getInputStream()) { return Bounded.readAll(in, max); }
        } finally {
            con.disconnect();
        }
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }
}
