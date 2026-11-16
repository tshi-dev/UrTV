package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** For tests/game_packs_test.py: the index, the 14-day rule, the room rule and the sha check of the content packs. Prints PASS/FAIL lines. */
public final class GamePacksTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    public static void main(String[] a) throws Exception {
        String sha = "a".repeat(64);
        List<GamePacks.Pack> v = GamePacks.parse(b("{\"v\":1,\"packs\":[{\"id\":\"quran-text\",\"ar\":\"نص القرآن\",\"en\":\"Quran text\",\"ver\":3,\"bytes\":900000,\"sha\":\"" + sha + "\",\"extra\":1},"
            + "{\"id\":\"BAD ID\",\"ar\":\"x\",\"ver\":1,\"bytes\":10,\"sha\":\"" + sha + "\"},{\"id\":\"no-sha\",\"ar\":\"x\",\"ver\":1,\"bytes\":10},{\"id\":\"huge\",\"ar\":\"x\",\"ver\":1,\"bytes\":999999999,\"sha\":\"" + sha + "\"},"
            + "{\"id\":\"emoji\",\"ar\":\"إيموجي\",\"ver\":1,\"bytes\":50000,\"sha\":\"" + sha + "\"}]}"));
        check("index: complete packs are listed, unknown fields ignored; a bad id, a missing sha and a pack over the cap are skipped, not the file", v.size() == 2 && v.get(0).id.equals("quran-text") && v.get(0).ver == 3 && v.get(1).id.equals("emoji") && v.get(1).en.isEmpty());
        check("index: another version, a bare array, text that is not JSON, nothing and a huge file give no packs, never an exception",
            GamePacks.parse(b("{\"v\":2,\"packs\":[]}")).isEmpty() && GamePacks.parse(b("[1]")).isEmpty() && GamePacks.parse(b("<html>")).isEmpty() && GamePacks.parse(null).isEmpty() && GamePacks.parse(new byte[200_000]).isEmpty());
        check("names: the English name when asked and there is one, else the Arabic", v.get(0).name(true).equals("Quran text") && v.get(1).name(true).equals("إيموجي") && v.get(0).name(false).equals("نص القرآن"));
        long day = 24L * 3600 * 1000, now = 100 * day;
        check("14 days: «تلقائي» goes after 14 days unused, not before; «احتفظ دائمًا» never; a pack never opened (0) is not judged",
            GamePacks.expired(GamePacks.AUTO, now - 15 * day, now) && !GamePacks.expired(GamePacks.AUTO, now - 13 * day, now) && !GamePacks.expired(GamePacks.KEEP, now - 500 * day, now) && !GamePacks.expired(GamePacks.AUTO, 0, now));
        check("room: a TV needs three times the pack and 400 MB free; a full TV is left alone",
            GamePacks.canDownload(500L << 20, 5_000_000) && !GamePacks.canDownload(100L << 20, 5_000_000) && !GamePacks.canDownload(500L << 20, 200_000_000L) && GamePacks.canDownload(700L << 20, 200_000_000L));
        byte[] data = b("hello");
        check("sha256: the right hex passes, a changed byte or another hex fails", GamePacks.shaOk(data, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824") && !GamePacks.shaOk(b("hellp"), "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824") && !GamePacks.shaOk(data, sha));
        check("size: megabytes and kilobytes", GamePacks.size(12_400_000).equals("12.4 MB") && GamePacks.size(830_000).equals("830 KB"));
        System.exit(failed == 0 ? 0 : 1);
    }
}
