package sa.urtv.home;

import java.util.Arrays;

/** For tests/a11y_heal_test.py: A11yList (the TV's list of accessibility services, and when A11yHeal heals). Prints PASS/FAIL lines. */
public final class A11yListRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        String type = "sa.urtv.home/sa.urtv.home.TypeService", sleep = "sa.urtv.home/sa.urtv.home.SleepService";
        String km = "io.github.sds100.keymapper/io.github.sds100.keymapper.system.accessibility.MyAccessibilityService";
        String list = km + ":" + type;
        check("our service is found in the list, in its full or its short form", A11yList.has(list, type) && A11yList.has(km + ":sa.urtv.home/.TypeService", type)
            && !A11yList.has(km, type) && !A11yList.has(null, type) && !A11yList.has("null", type) && !A11yList.has("", type));
        check("the list without ours keeps the others exactly as written, in their order",
            A11yList.without(km + ":" + sleep + ":com.x/.Svc$Inner:" + type, type, sleep).equals(km + ":com.x/.Svc$Inner"));
        check("ours alone: nothing is left", A11yList.without(type, type, sleep).isEmpty());
        check("an entry another app's list had and lost is reported (never re-added)", A11yList.lost(list, type).equals(Arrays.asList(km)));
        check("nothing lost, or nothing seen before: nothing reported", A11yList.lost(list, list).isEmpty() && A11yList.lost(null, list).isEmpty() && A11yList.lost("", type).isEmpty());
        check("heal: listed, the switch 0 (the TV test after the update)", A11yList.decide(true, "0", false, true).equals(A11yList.HEAL));
        check("heal: listed, the switch 1 but Android runs none of it", A11yList.decide(true, "1", false, true).equals(A11yList.HEAL));
        check("heal: running but the switch not set", A11yList.decide(true, null, true, true).equals(A11yList.HEAL));
        check("nothing to do when listed, on and running", A11yList.decide(true, "1", true, true).equals(A11yList.OK));
        check("never when our service is not in the list (the owner turned it off: we do not turn it on)", A11yList.decide(false, "0", false, true).equals(A11yList.NOT_LISTED));
        check("never without WRITE_SECURE_SETTINGS", A11yList.decide(true, "0", false, false).equals(A11yList.NO_PERMISSION));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
