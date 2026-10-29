package sa.urtv.home.phone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** For tests/update_test.py: «تحديث تلفزيوني» (UpdateFlow, UpdateOrder, UpdateRecords, UpdateText, UpdateRules) on a plain JVM. Prints PASS/FAIL lines. */
public final class UpdateRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    /** A record that lives in a string, the way the file does: a new object over the same string is a restart. */
    static final class Mem implements UpdateRecords.Store {
        String text;
        boolean refuse;
        @Override public String read() { return text; }
        @Override public boolean write(String t) { if (refuse) return false; text = t; return true; }
        @Override public void clear() { text = null; }
    }

    static String bare(String s) { return s.replace("⁦", "").replace("⁩", ""); }

    static Map<String, String> verdicts(String saver, String launcher) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(UpdateOrder.SAVER, saver);
        m.put(UpdateOrder.LAUNCHER, launcher);
        return m;
    }

    public static void main(String[] args) {
        // ---- the version comparison
        check("a newer build is newer, the same and an older one are not (no downgrade, no reinstall)",
            UpdateFlow.isNewer(52, 51) && !UpdateFlow.isNewer(51, 51) && !UpdateFlow.isNewer(50, 51), "");
        check("builds are compared as numbers (3.100 over 3.99), version.txt is read the same way",
            UpdateFlow.isNewer(UpdateRules.buildOf("3.100\n"), UpdateRules.buildOf("3.99")) && UpdateRules.buildOf("3.9") == 9, "");

        // ---- step one: what version.txt and the permission say
        check("no network: version.txt unreadable is «OFFLINE», whatever else is true",
            UpdateFlow.first(-1, 50, true).result == UpdateFlow.Result.OFFLINE && UpdateFlow.first(-1, 50, false).result == UpdateFlow.Result.OFFLINE
            && UpdateFlow.first(UpdateRules.buildOf("<html>"), 50, true).result == UpdateFlow.Result.OFFLINE, "");
        check("up to date: the same build", UpdateFlow.first(51, 51, true).result == UpdateFlow.Result.UP_TO_DATE, "");
        check("up to date: the release is older than what is installed (never a downgrade), permission or not",
            UpdateFlow.first(49, 51, true).result == UpdateFlow.Result.UP_TO_DATE && UpdateFlow.first(49, 51, false).result == UpdateFlow.Result.UP_TO_DATE, "");
        check("a newer build without the permission: «NEED_PERMISSION»", UpdateFlow.first(52, 51, false).result == UpdateFlow.Result.NEED_PERMISSION, "");
        check("a newer build with the permission goes on to the files", UpdateFlow.first(52, 51, true).result == UpdateFlow.Result.CONTINUE, "");

        // ---- step two: room, network, and the files
        long[] sizes = {30_000_000L, 40_000_000L};   // both APKs: 70 MB, so 140 MB are needed
        check("room: Wi-Fi or Ethernet down is «CANNOT»", UpdateFlow.room(false, 1L << 40, sizes).result == UpdateFlow.Result.CANNOT
            && "network".equals(UpdateFlow.room(false, 1L << 40, sizes).reason), "");
        check("room: both APKs twice must fit (139 MB refused, 140 MB allowed)", "space".equals(UpdateFlow.room(true, 139_999_999L, sizes).reason)
            && UpdateFlow.room(true, 140_000_000L, sizes).result == UpdateFlow.Result.CONTINUE, "");
        check("room: no space at all is refused", UpdateFlow.room(true, 0, sizes).result == UpdateFlow.Result.CANNOT, "");
        check("every check passes: «AVAILABLE»", UpdateFlow.files(verdicts(null, null)).result == UpdateFlow.Result.AVAILABLE, "");
        check("the screensaver already current is skipped, the launcher still updates", UpdateFlow.files(verdicts("not_newer", null)).result == UpdateFlow.Result.AVAILABLE, "");
        check("a launcher file that is not newer is refused, though version.txt said so", "not_newer".equals(UpdateFlow.files(verdicts(null, "not_newer")).reason)
            && UpdateFlow.files(verdicts(null, "not_newer")).result == UpdateFlow.Result.CANNOT, "");
        Map<String, String> onlySaver = new LinkedHashMap<>();
        onlySaver.put(UpdateOrder.SAVER, null);
        check("no launcher in the plan, or nothing at all, is refused", UpdateFlow.files(onlySaver).result == UpdateFlow.Result.CANNOT
            && UpdateFlow.files(new LinkedHashMap<String, String>()).result == UpdateFlow.Result.CANNOT && UpdateFlow.files(null).result == UpdateFlow.Result.CANNOT, "");

        // ---- the signer
        HashSet<String> mine = new HashSet<>(Arrays.asList("a".repeat(64))), other = new HashSet<>(Arrays.asList("b".repeat(64)));
        String refused = UpdateRules.verify("sa.urtv.home", "sa.urtv.home", 51, 52, mine, other);
        check("another signer is refused by the rules", "other_key".equals(refused), String.valueOf(refused));
        check("and by the decision: «CANNOT», with the reason", UpdateFlow.files(verdicts(null, refused)).result == UpdateFlow.Result.CANNOT
            && "other_key".equals(UpdateFlow.files(verdicts(null, refused)).reason), "");
        check("a signer refusal on the screensaver app refuses the whole update, the launcher is not touched",
            UpdateFlow.files(verdicts("other_key", null)).result == UpdateFlow.Result.CANNOT, "");
        for (String why : new String[] {"other_app", "unsigned", "unreadable"}) {
            check("«" + why + "» refuses the update", UpdateFlow.files(verdicts(null, why)).result == UpdateFlow.Result.CANNOT, "");
        }
        check("same signer, newer build: the rules allow it", UpdateRules.verify("sa.urtv.home", "sa.urtv.home", 51, 52, mine, new HashSet<>(mine)) == null, "");

        // ---- the whole decision, branch by branch
        check("branch: up to date", UpdateFlow.first(51, 51, true).result == UpdateFlow.Result.UP_TO_DATE, "");
        UpdateFlow.Verdict v = UpdateFlow.first(52, 51, true);
        boolean ok = v.result == UpdateFlow.Result.CONTINUE;
        ok &= UpdateFlow.room(true, 1L << 40, sizes).result == UpdateFlow.Result.CONTINUE;
        ok &= UpdateFlow.files(verdicts(null, null)).result == UpdateFlow.Result.AVAILABLE;
        check("branch: update available (newer, permission, room, network, signer, newer files)", ok, "");

        // ---- the installer's answers
        check("blocked by the firmware is a failure: «blocked»", "blocked".equals(UpdateFlow.failure(2)), "");
        check("success and «waiting for the owner» are not failures", UpdateFlow.failure(0) == null && UpdateFlow.failure(-1) == null, "");
        check("the owner's Back is «declined», and only that is the owner's own no", "declined".equals(UpdateFlow.failure(3)) && UpdateFlow.isDeclined("declined")
            && !UpdateFlow.isDeclined("blocked") && !UpdateFlow.isDeclined("failed"), "");
        check("the other failures have a word each, and an unknown status is «failed»", "invalid".equals(UpdateFlow.failure(4)) && "conflict".equals(UpdateFlow.failure(5))
            && "storage".equals(UpdateFlow.failure(6)) && "incompatible".equals(UpdateFlow.failure(7)) && "failed".equals(UpdateFlow.failure(1)) && "failed".equals(UpdateFlow.failure(99)), "");

        // ---- the order
        check("the screensaver app before the launcher, whatever order they come in",
            UpdateOrder.order(Arrays.asList(UpdateOrder.LAUNCHER, UpdateOrder.SAVER)).equals(Arrays.asList(UpdateOrder.SAVER, UpdateOrder.LAUNCHER))
            && UpdateOrder.order(Arrays.asList(UpdateOrder.SAVER, UpdateOrder.LAUNCHER)).equals(Arrays.asList(UpdateOrder.SAVER, UpdateOrder.LAUNCHER)), "");
        check("the launcher is always last, and alone when there is no screensaver app", UpdateOrder.order(Arrays.asList(UpdateOrder.LAUNCHER)).equals(Arrays.asList(UpdateOrder.LAUNCHER)), "");
        check("an app that is not ours is dropped", UpdateOrder.order(Arrays.asList("com.example.x", UpdateOrder.LAUNCHER)).equals(Arrays.asList(UpdateOrder.LAUNCHER))
            && UpdateOrder.order(null).isEmpty(), "");
        check("only the launcher's step replaces the running app", UpdateOrder.replacesUs(UpdateOrder.LAUNCHER) && !UpdateOrder.replacesUs(UpdateOrder.SAVER), "");
        check("the order is the one of UpdateRules.APKS (screensaver, then home)", UpdateRules.APKS[0][0].equals(UpdateOrder.SAVER) && UpdateRules.APKS[1][0].equals(UpdateOrder.LAUNCHER), "");

        // ---- the record: written before the launcher is replaced, read after the restart
        Mem disk = new Mem();
        check("a first start with no record has nothing to say", UpdateRecords.peek(disk, 51).kind == UpdateRecords.Kind.NONE, "");
        check("the record is written with the target and the step", UpdateRecords.begin(disk, 52, UpdateOrder.LAUNCHER) && disk.text != null
            && disk.text.contains("target=52") && disk.text.contains("step=sa.urtv.home"), String.valueOf(disk.text));
        Mem restarted = new Mem();
        restarted.text = disk.text;   // the process is gone: only what was on disk is left
        UpdateRecords.Outcome o = UpdateRecords.peek(restarted, 52);
        check("after the restart, the build reached the target: success, with the target", o.kind == UpdateRecords.Kind.SUCCESS && o.target == 52, o.kind + " " + o.target);
        check("peek removes nothing", restarted.text != null, "");
        o = UpdateRecords.take(restarted, 52);
        check("take says success…", o.kind == UpdateRecords.Kind.SUCCESS, "");
        check("…and removes it: «تم التحديث» is shown once (the next start says nothing)", restarted.text == null && UpdateRecords.take(restarted, 52).kind == UpdateRecords.Kind.NONE, "");
        restarted.text = disk.text;
        o = UpdateRecords.take(restarted, 53);
        check("a build above the target is a success too", o.kind == UpdateRecords.Kind.SUCCESS, "");
        restarted.text = disk.text;
        o = UpdateRecords.take(restarted, 51);
        check("failure mid-way: the launcher still at the old build means «not completed», with the step", o.kind == UpdateRecords.Kind.FAILED
            && UpdateOrder.LAUNCHER.equals(o.step) && o.target == 52, o.kind + " " + o.step);
        check("…shown once as well", restarted.text == null && UpdateRecords.take(restarted, 51).kind == UpdateRecords.Kind.NONE, "");
        restarted.text = "garbage\n";
        check("a record that can't be read is never a success: «not completed», step unknown", UpdateRecords.peek(restarted, 99).kind == UpdateRecords.Kind.FAILED
            && "unknown".equals(UpdateRecords.peek(restarted, 99).step), "");
        restarted.text = "target=52\n";
        check("a record without a step is not a success", UpdateRecords.peek(restarted, 99).kind == UpdateRecords.Kind.FAILED, "");
        restarted.text = "target=52\nstep=$(reboot)\n";
        check("a step that is not a package name is not trusted", UpdateRecords.peek(restarted, 99).kind == UpdateRecords.Kind.FAILED, "");
        restarted.text = "";
        check("an empty record is nothing", UpdateRecords.peek(restarted, 1).kind == UpdateRecords.Kind.NONE, "");
        Mem full = new Mem();
        full.refuse = true;
        check("when the record can't be kept, begin says so (the update must not start)", !UpdateRecords.begin(full, 52, UpdateOrder.LAUNCHER) && full.text == null, "");
        check("a bad step or target is not written", !UpdateRecords.begin(new Mem(), -1, UpdateOrder.LAUNCHER) && !UpdateRecords.begin(new Mem(), 52, "x y") && !UpdateRecords.begin(new Mem(), 52, null), "");

        // ---- the sequence, as the engine runs it: a stand-in with the same order of steps
        List<String> log = new ArrayList<>();
        Mem seq = new Mem();
        for (String pkg : UpdateOrder.order(Arrays.asList(UpdateOrder.LAUNCHER, UpdateOrder.SAVER))) {
            if (UpdateOrder.replacesUs(pkg)) UpdateRecords.begin(seq, 52, pkg);
            log.add((seq.text == null ? "no-record " : "record ") + pkg);
        }
        check("the record exists for the launcher's step and not before it", log.equals(Arrays.asList("no-record sa.urtv.screensaver", "record sa.urtv.home")), log.toString());

        // ---- what the owner reads
        check("up to date: «تلفزيونك محدّث (3.N)»", "تلفزيونك محدّث (3.52)".equals(bare(UpdateText.upToDate("3.52"))), bare(UpdateText.upToDate("3.52")));
        check("no network: «ما فيه اتصال بالإنترنت»", "ما فيه اتصال بالإنترنت".equals(UpdateText.OFFLINE), "");
        check("available: «تحديث جديد 3.N · اضغط للتحديث»", "تحديث جديد 3.52 · اضغط للتحديث".equals(bare(UpdateText.available(UpdateText.versionOf(52)))), "");
        check("done: «تم التحديث ✓ 3.N»", "تم التحديث ✓ 3.52".equals(bare(UpdateText.done("3.52"))), "");
        check("not completed: «ما اكتمل التحديث», and the step", "ما اكتمل التحديث".equals(UpdateText.NOT_DONE) && UpdateText.failedStep(UpdateOrder.label(UpdateOrder.LAUNCHER)).contains("الواجهة")
            && !UpdateOrder.label(UpdateOrder.SAVER).equals(UpdateOrder.label(UpdateOrder.LAUNCHER)), "");
        check("the message for a TV that can't update itself is exactly the three lines",
            "التحديث من التلفزيون ما يشتغل على هذا الجهاز.\nاطلب من مساعد الذكاء الاصطناعي على جهازك:\nحدّث تلفزيوني".equals(UpdateText.AI_MESSAGE), UpdateText.AI_MESSAGE);
        check("and the repository address under it", "github.com/tshi-dev/UrTV".equals(UpdateText.REPO), "");
        check("the screen's name and button", "تحديث تلفزيوني".equals(UpdateText.TITLE) && "تحقق من التحديث".equals(UpdateText.CHECK), "");
        check("the installed version line carries the version", bare(UpdateText.installed("3.51")).endsWith("3.51"), "");
        check("the build number gives the version the screens show: the name and the build, «4.0 (بناء 51)»", "4.0 (بناء 51)".equals(UpdateText.versionOf("4.0", 51)), "");

        // ---- failures after version.txt are never «no connection»
        check("version.txt: no network is «OFFLINE», and only that", UpdateFlow.versionRead(false, 0, -1).result == UpdateFlow.Result.OFFLINE
            && UpdateFlow.versionRead(false, 200, 52).result == UpdateFlow.Result.OFFLINE, "");
        check("version.txt answered with an error (404, 500) is «CANNOT» (download), not offline", UpdateFlow.versionRead(true, 404, -1).result == UpdateFlow.Result.CANNOT
            && "download".equals(UpdateFlow.versionRead(true, 404, -1).reason) && "download".equals(UpdateFlow.versionRead(true, 503, 52).reason), "");
        check("version.txt that is a page instead of the file is «CANNOT» (download)", UpdateFlow.versionRead(true, 200, UpdateRules.buildOf("<html>")).result == UpdateFlow.Result.CANNOT, "");
        check("version.txt read well goes on", UpdateFlow.versionRead(true, 200, 52).result == UpdateFlow.Result.CONTINUE, "");
        check("a file that can't be fetched is «CANNOT» (download), never «OFFLINE»", UpdateFlow.fetchFailed().result == UpdateFlow.Result.CANNOT
            && "download".equals(UpdateFlow.fetchFailed().reason), "");
        check("any other exception is «CANNOT» (exception), never «OFFLINE»", UpdateFlow.crashed().result == UpdateFlow.Result.CANNOT
            && "exception".equals(UpdateFlow.crashed().reason), "");

        // ---- the phone page's check reads version.txt only
        check("check without downloading stops at FOUND when there is a newer build", UpdateFlow.plan(UpdateFlow.first(52, 51, true), false).result == UpdateFlow.Result.FOUND, "");
        check("check with downloading goes on to the files", UpdateFlow.plan(UpdateFlow.first(52, 51, true), true).result == UpdateFlow.Result.CONTINUE, "");
        check("check without downloading changes no other answer", UpdateFlow.plan(UpdateFlow.first(51, 51, true), false).result == UpdateFlow.Result.UP_TO_DATE
            && UpdateFlow.plan(UpdateFlow.first(52, 51, false), false).result == UpdateFlow.Result.NEED_PERMISSION
            && UpdateFlow.plan(UpdateFlow.first(-1, 51, true), false).result == UpdateFlow.Result.OFFLINE, "");

        // ---- a «waiting» nobody answers is not a dead end
        String[] w = UpdateFlow.shown("waiting_owner", "", false, false);
        check("waiting_owner, no longer busy, no confirm screen: shown as the failure «failed» (its screen has the check button)", "error".equals(w[0]) && "failed".equals(w[1]), w[0] + " " + w[1]);
        check("waiting_owner still busy is left as it is", "waiting_owner".equals(UpdateFlow.shown("waiting_owner", "", true, false)[0]), "");
        check("waiting_owner with a confirm screen to open again keeps its «show it» button", "waiting_owner".equals(UpdateFlow.shown("waiting_owner", "", false, true)[0]), "");
        check("the other states are never changed", "error".equals(UpdateFlow.shown("error", "cannot", false, false)[0]) && "cannot".equals(UpdateFlow.shown("error", "cannot", false, false)[1])
            && "uptodate".equals(UpdateFlow.shown("uptodate", null, false, false)[0]) && "".equals(UpdateFlow.shown("uptodate", null, false, false)[1]), "");

        System.exit(failed == 0 ? 0 : 1);
    }
}
