package sa.urtv.screensaver;

import java.io.ByteArrayInputStream;
import java.util.*;

/** For tests/photo_packs_test.py: the photo packs' rules (PhotoPacks.java) on a plain JVM. */
public final class PhotoPacksRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    static final String SHA = "a".repeat(64);

    static PhotoPacks.Pack pack(String id, String url, String sha, int count, long bytes) {
        return new PhotoPacks.Pack(id, "فضاء", "Space", "ناسا", "NASA", "https://example.org/c.jpg", url, sha, count, bytes);
    }

    static String url(String id) { return PhotoPacks.RELEASES + "photos-" + id + "/" + id + ".zip"; }

    public static void main(String[] args) throws Exception {
        PhotoPacks.Pack good = pack("space", url("space"), SHA, 8, 3 << 20);
        check("a pack with every field and numbers in range is valid", PhotoPacks.valid(good), "");
        check("its address must be our release, photos-ID/ID.zip: another host, another pack's, http, a suffix are refused",
            !PhotoPacks.valid(pack("space", "https://evil.example/space.zip", SHA, 8, 1000))
            && !PhotoPacks.valid(pack("space", url("nature"), SHA, 8, 1000))
            && !PhotoPacks.valid(pack("space", url("space").replace("https", "http"), SHA, 8, 1000))
            && !PhotoPacks.valid(pack("space", url("space") + "?x=1", SHA, 8, 1000)), "");
        check("the sha256 is 64 lower-case hex: short, upper-case or empty is refused",
            !PhotoPacks.valid(pack("space", url("space"), "abc", 8, 1000)) && !PhotoPacks.valid(pack("space", url("space"), SHA.toUpperCase(), 8, 1000))
            && !PhotoPacks.valid(pack("space", url("space"), "", 8, 1000)), "");
        check("the id is a safe folder name (no slash, dots, capitals, leading digit)",
            PhotoPacks.validId("world-capitals") && !PhotoPacks.validId("../x") && !PhotoPacks.validId("A") && !PhotoPacks.validId("1ab")
            && !PhotoPacks.validId("a/b") && !PhotoPacks.validId(null) && !PhotoPacks.validId(""), "");
        check("count and size are in range (0 photos, 201 photos, 0 bytes, over 80 MB are refused)",
            !PhotoPacks.valid(pack("space", url("space"), SHA, 0, 1000)) && !PhotoPacks.valid(pack("space", url("space"), SHA, 201, 1000))
            && !PhotoPacks.valid(pack("space", url("space"), SHA, 5, 0)) && !PhotoPacks.valid(pack("space", url("space"), SHA, 5, 81L << 20)), "");
        List<PhotoPacks.Pack> all = new ArrayList<>();
        all.add(good);
        all.add(pack("space", url("space"), SHA, 9, 2000));   // the same id again
        all.add(pack("bad", "x", SHA, 9, 2000));
        for (int i = 0; i < 20; i++) all.add(pack("p" + (char) ('a' + i), url("p" + (char) ('a' + i)), SHA, 3, 1000));
        List<PhotoPacks.Pack> kept = PhotoPacks.keepValid(all);
        check("the index keeps the valid ones, each id once, in order, at most 12", kept.size() == PhotoPacks.PACKS_MAX && kept.get(0) == good && kept.get(1).id.equals("pa"), String.valueOf(kept.size()));

        // the check before use
        byte[] data = "hello".getBytes("UTF-8");
        String d = PhotoPacks.sha256(new ByteArrayInputStream(data), 100);
        check("sha256 of «hello» is the known digest", d.equals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"), d);
        check("the zip is accepted only when its digest is the index's", PhotoPacks.matches(d, d) && !PhotoPacks.matches(d, SHA) && !PhotoPacks.matches(d, null)
            && !PhotoPacks.matches(d, d.toUpperCase()), "");
        boolean threw = false;
        try { PhotoPacks.sha256(new ByteArrayInputStream(new byte[200]), 100); } catch (java.io.IOException e) { threw = true; }
        check("a stream longer than the bound is refused, not read on", threw, "");

        // the zip's entries
        check("a zip may hold only N.jpg and credits.json", PhotoPacks.safeEntry("1.jpg") && PhotoPacks.safeEntry("120.jpg") && PhotoPacks.safeEntry("credits.json"), "");
        check("a path, a parent folder, an absolute name, 0.jpg, 01.jpg, 1000.jpg, another extension or empty is refused",
            !PhotoPacks.safeEntry("../1.jpg") && !PhotoPacks.safeEntry("a/1.jpg") && !PhotoPacks.safeEntry("/etc/passwd") && !PhotoPacks.safeEntry("0.jpg")
            && !PhotoPacks.safeEntry("01.jpg") && !PhotoPacks.safeEntry("1000.jpg") && !PhotoPacks.safeEntry("1.png") && !PhotoPacks.safeEntry("x.sh")
            && !PhotoPacks.safeEntry("") && !PhotoPacks.safeEntry(null) && !PhotoPacks.safeEntry("1.jpg/../../x"), "");
        check("a JPEG by its first bytes", PhotoPacks.looksJpeg(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}) && !PhotoPacks.looksJpeg(new byte[] {'P', 'K', 3})
            && !PhotoPacks.looksJpeg(new byte[0]) && !PhotoPacks.looksJpeg(null), "");
        boolean[] has = new boolean[6];
        for (int k = 1; k <= 5; k++) has[k] = true;
        boolean ok = PhotoPacks.complete(has, 5);
        has[3] = false;
        check("a pack is complete only with every photo 1..count", ok && !PhotoPacks.complete(has, 5) && !PhotoPacks.complete(null, 5) && !PhotoPacks.complete(new boolean[3], 5), "");

        // which packs are on
        Set<String> on = PhotoPacks.parseOn("space,bad id,nature,space,../x");
        check("the saved list reads back valid ids once each", String.join(",", on).equals("space,nature"), String.join(",", on));
        check("round trip", PhotoPacks.joinOn(on).equals("space,nature") && PhotoPacks.parseOn(null).isEmpty() && PhotoPacks.parseOn("").isEmpty(), "");
        check("toggled turns one on or off and leaves the rest",
            PhotoPacks.toggled(on, "art", true).size() == 3 && !PhotoPacks.toggled(on, "space", false).contains("space") && PhotoPacks.toggled(on, "../x", true).size() == 2, "");
        Map<String, Integer> have = new HashMap<>();
        have.put("space", 8);
        check("the page's #packs= lists the packs that are on AND on the TV, with their counts; nothing otherwise (the fragment stays as it was)",
            PhotoPacks.fragment(on, have).equals("&packs=space:8") && PhotoPacks.fragment(new LinkedHashSet<>(), have).isEmpty()
            && PhotoPacks.fragment(on, new HashMap<>()).isEmpty(), PhotoPacks.fragment(on, have));
        check("«صورنا» can go off only while a pack that is on is on the TV", PhotoPacks.mayTurnOursOff(on, have) && !PhotoPacks.mayTurnOursOff(on, new HashMap<>())
            && !PhotoPacks.mayTurnOursOff(new LinkedHashSet<>(), have), "");
        check("size in MB rounds up, never 0", PhotoPacks.megabytes(1) == 1 && PhotoPacks.megabytes(1 << 20) == 1 && PhotoPacks.megabytes((1 << 20) + 1) == 2, "");
        check("credit line", PhotoPacks.creditLine("NASA", "Public domain").equals("NASA · Public domain") && PhotoPacks.creditLine("", "CC0").equals("CC0")
            && PhotoPacks.creditLine("X", "").equals("X"), "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
