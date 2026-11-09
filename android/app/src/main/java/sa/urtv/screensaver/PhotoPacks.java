package sa.urtv.screensaver;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Screensaver photo packs («صور شاشة التوقف»): the rules, with no Android in them (tests/PhotoPacksRun.java runs them on a JVM).
 * A pack is a zip on our GitHub releases, listed in photos/packs.json with its sha256. It is downloaded only when the person asks, is
 * checked against that sha256 BEFORE a byte of it is opened, and only then unpacked: the files that may come out of it are
 * N.jpg and credits.json, nothing else, so a zip cannot write anywhere it likes. docs/PHOTO-PACKS.md has the index format.
 */
final class PhotoPacks {
    private PhotoPacks() { }

    /** the only place a pack may be downloaded from: our releases (an index that points elsewhere is not followed) */
    static final String RELEASES = "https://github.com/tshi-dev/UrTV/releases/download/";
    /** the largest zip taken, and the most photos in one */
    static final long ZIP_MAX = 80L << 20;
    static final int PHOTOS_MAX = 200, PACKS_MAX = 12;
    /** one photo is a 1920x1080 JPEG: more than this is not a photo of ours */
    static final long PHOTO_MAX = 3L << 20;

    /** One line of the index. */
    static final class Pack {
        final String id, name, nameEn, source, sourceEn, cover, url, sha256;
        final int count;
        final long bytes;

        Pack(String id, String name, String nameEn, String source, String sourceEn, String cover, String url, String sha256, int count, long bytes) {
            this.id = id; this.name = name; this.nameEn = nameEn; this.source = source; this.sourceEn = sourceEn;
            this.cover = cover; this.url = url; this.sha256 = sha256;
            this.count = count; this.bytes = bytes;
        }
    }

    /** a file name inside Arabic text, kept left to right (an isolate: «IMG_2041.jpg» does not reorder its dots and digits) */
    static String isolate(String name) { return "\u2066" + name + "\u2069"; }

    /** a pack's id: lowercase letters, digits and dashes (it is a folder name and part of the address) */
    static boolean validId(String id) { return id != null && id.matches("[a-z][a-z0-9-]{1,30}"); }

    static boolean validSha(String s) { return s != null && s.matches("[0-9a-f]{64}"); }

    /** the zip's address: https on our releases, photos-ID/ID.zip, nothing after it */
    static boolean validUrl(String id, String url) {
        return validId(id) && (RELEASES + "photos-" + id + "/" + id + ".zip").equals(url);
    }

    /** every field a pack needs, and numbers in range; a pack that fails is left out of the list, never half shown */
    static boolean valid(Pack p) {
        return p != null && validId(p.id) && validSha(p.sha256) && validUrl(p.id, p.url)
            && p.name != null && !p.name.trim().isEmpty() && p.name.length() <= 40
            && p.nameEn != null && p.nameEn.length() <= 40 && p.source != null && p.source.length() <= 80 && p.sourceEn != null && p.sourceEn.length() <= 80
            && p.count >= 1 && p.count <= PHOTOS_MAX && p.bytes >= 1 && p.bytes <= ZIP_MAX
            && p.cover != null && (p.cover.isEmpty() || p.cover.startsWith("https://"));
    }

    /** the valid ones, in the index's order, each id once, at most PACKS_MAX */
    static List<Pack> keepValid(List<Pack> all) {
        List<Pack> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Pack p : all) if (valid(p) && out.size() < PACKS_MAX && seen.add(p.id)) out.add(p);
        return out;
    }

    /** hex sha256 of what the stream gives (bounded by max: a longer one throws, nothing past it is read) */
    static String sha256(InputStream in, long max) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[16384];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > max) throw new IOException("longer than " + max + " bytes");
                md.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("no sha256");
        }
    }

    /** the zip is this pack's: its digest is the one the index gives (both lower-case hex) */
    static boolean matches(String digest, String indexed) { return validSha(indexed) && indexed.equals(digest); }

    /** the one kind of name a pack's zip may hold: 1.jpg ... N.jpg, or credits.json; a folder, a path or «..» is refused */
    static boolean safeEntry(String name) {
        return name != null && (name.matches("[1-9][0-9]{0,2}\\.jpg") || name.equals("credits.json"));
    }

    /** a JPEG by its first bytes (the size and the decode are checked where Android is: PackStore) */
    static boolean looksJpeg(byte[] head) {
        return head != null && head.length >= 3 && (head[0] & 0xff) == 0xFF && (head[1] & 0xff) == 0xD8 && (head[2] & 0xff) == 0xFF;
    }

    /** the photos 1..count are all there (has[k] for photo k): a pack with a gap or short of its count is not used */
    static boolean complete(boolean[] has, int count) {
        if (has == null || count < 1 || has.length < count + 1) return false;
        for (int k = 1; k <= count; k++) if (!has[k]) return false;
        return true;
    }

    // ---- which packs are on ----

    /** the ids saved as "a,b" read back: valid ones only, each once */
    static Set<String> parseOn(String saved) {
        Set<String> out = new LinkedHashSet<>();
        if (saved != null) for (String s : saved.split(",")) if (validId(s.trim())) out.add(s.trim());
        return out;
    }

    static String joinOn(Set<String> on) {
        StringBuilder sb = new StringBuilder();
        for (String s : on) if (validId(s)) sb.append(sb.length() == 0 ? "" : ",").append(s);
        return sb.toString();
    }

    /** the pack turned on or off */
    static Set<String> toggled(Set<String> on, String id, boolean want) {
        Set<String> out = new LinkedHashSet<>(on);
        if (want && validId(id)) out.add(id); else out.remove(id);
        return out;
    }

    /**
     * The page's #packs=: "id:count" for each pack that is on AND on the TV (a pack not downloaded gives the page nothing to ask for),
     * comma separated; nothing when there is none, so the fragment is as it was. installed maps id to its number of photos.
     */
    static String fragment(Set<String> on, Map<String, Integer> installed) {
        StringBuilder sb = new StringBuilder();
        for (String id : on) {
            Integer n = installed.get(id);
            if (n != null && n > 0 && validId(id)) sb.append(sb.length() == 0 ? "" : ",").append(id).append(':').append(n);
        }
        return sb.length() == 0 ? "" : "&packs=" + sb;
    }

    /** «صورنا» may be turned off only while a pack that is on and downloaded gives the screen something else to show */
    static boolean mayTurnOursOff(Set<String> on, Map<String, Integer> installed) {
        for (String id : on) { Integer n = installed.get(id); if (n != null && n > 0) return true; }
        return false;
    }

    /** the size a person reads: megabytes, rounded up so a pack is never bigger than it says */
    static long megabytes(long bytes) { return Math.max(1, (bytes + (1L << 20) - 1) >> 20); }

    /** «author · licence» for the credits line of a photo */
    static String creditLine(String author, String licence) {
        String a = author == null ? "" : author.trim(), l = licence == null ? "" : licence.trim();
        return a.isEmpty() ? l : l.isEmpty() ? a : a + " · " + l;
    }
}
