package sa.urtv.home.phone;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * For tests/update_optional_test.py (R-288): the beta update that failed with «download» and offered 4.4 over build 525. apk-beta held only the home and the
 * screensaver; the add-on (extras, build 516) can never be updated from there, so it must not count. Plain JVM, prints PASS/FAIL lines.
 */
public final class UpdateOptionalRun {
    static int failed;

    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    static final String SAVER = "sa.urtv.screensaver", EXTRAS = "sa.urtv.extras", HOME = "sa.urtv.home";

    static Map<String, Long> installed(long saver, long extras, long home) {
        Map<String, Long> m = new LinkedHashMap<>();
        if (saver >= 0) m.put(SAVER, saver);
        if (extras >= 0) m.put(EXTRAS, extras);
        if (home >= 0) m.put(HOME, home);
        return m;
    }

    static Set<String> set(String... s) { return new HashSet<>(Arrays.asList(s)); }

    static String say(String why, String file) { return UpdateText.reason(why, file, UpdateText.ARABIC); }

    public static void main(String[] a) {
        // the owner's TV: home and screensaver 525, add-on 516, apk-beta says 525 and has no extras file
        Map<String, Long> tv = installed(525, 516, 525);
        check("the old rule: the add-on at 516 pulls the lowest build down, so 525 looks newer (the bug)", UpdateRules.lowestInstalled(tv, set()) == 516 && UpdateFlow.first(525, 516, true).result == UpdateFlow.Result.CONTINUE);
        check("an optional package whose file is missing at the release does not count in the lowest build", UpdateRules.lowestInstalled(tv, set(EXTRAS)) == 525);
        check("with it ignored the TV is up to date: equal to the newest is never offered", UpdateFlow.first(525, UpdateRules.lowestInstalled(tv, set(EXTRAS)), true).result == UpdateFlow.Result.UP_TO_DATE);
        check("an optional package whose file IS there still counts (a lagging add-on is updated with the others)", UpdateRules.lowestInstalled(tv, set()) == 516 && UpdateRules.lowestInstalled(tv, set(SAVER)) == 516);
        check("the home and the screensaver are required: their missing file is never ignored", UpdateRules.lowestInstalled(installed(500, 516, 525), set(SAVER, HOME)) == 500);
        check("isRequired: home and screensaver yes, the add-on no", UpdateRules.isRequired(HOME) && UpdateRules.isRequired(SAVER) && !UpdateRules.isRequired(EXTRAS));
        check("wanted: the missing optional file is dropped from the download list, the required ones stay", UpdateRules.wanted(Arrays.asList(SAVER, EXTRAS, HOME), set(EXTRAS)).equals(Arrays.asList(SAVER, HOME)));
        check("wanted: nothing missing, nothing dropped; a missing required one stays (its download then fails, loudly)", UpdateRules.wanted(Arrays.asList(SAVER, EXTRAS, HOME), set()).size() == 3 && UpdateRules.wanted(Arrays.asList(SAVER, HOME), set(HOME)).contains(HOME));
        check("the download list in install order without the add-on", UpdateOrder.order(UpdateRules.wanted(Arrays.asList(SAVER, EXTRAS, HOME), set(EXTRAS))).equals(Arrays.asList(SAVER, HOME)));
        check("a file is missing only on a 404 from a server that answered (a timeout or a 500 stays a failure)", UpdateRules.assetMissing(404) && !UpdateRules.assetMissing(500) && !UpdateRules.assetMissing(403) && !UpdateRules.assetMissing(200) && !UpdateRules.assetMissing(-1));
        check("sizeOf's word for «not in the release» is neither a size nor «unknown»", UpdateRules.MISSING < 0 && UpdateRules.MISSING != -1);
        check("an add-on that is not installed (-1) never counts", UpdateRules.lowestInstalled(installed(525, -1, 525), set()) == 525);
        check("nothing installed gives 0 (as before)", UpdateRules.lowestInstalled(installed(-1, -1, -1), set()) == 0);

        // the reasons the failure screen says
        check("download names the file", "تعذّر تنزيل ملف urtv-extras.apk".equals(say("download", "urtv-extras.apk")));
        check("download without a file says so generally", "تعذّر تنزيل ملف التحديث".equals(say("download", "")));
        check("verification: a different key says the file does not match the app's signature", "الملف لا يطابق توقيع التطبيق".equals(say("other_key", "")) && "الملف لا يطابق توقيع التطبيق".equals(say("unsigned", "")));
        check("network, storage, permission each have their own sentence", say("network", "") != null && say("space", "") != null && say("storage", "") != null && say("permission_screen", "") != null);
        check("the sentences differ (network is not storage is not download)", !say("network", "").equals(say("space", "")) && !say("download", "").equals(say("network", "")));
        check("an unknown word gives null (the screen then has only the generic line)", say("zzz", "") == null && say(null, "") == null);
        check("a reason line is never the generic AI line", !say("exception", "").contains("الذكاء الاصطناعي"));

        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
