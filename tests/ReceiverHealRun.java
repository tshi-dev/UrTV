package sa.urtv.home;

/** For tests/receiver_heal_test.py: ReceiverState.heal (which of our receivers go back on). Prints PASS/FAIL lines. */
public final class ReceiverHealRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("turned off by a startup manager: back on", ReceiverState.heal(true, ReceiverState.DISABLED)
            && ReceiverState.heal(true, ReceiverState.DISABLED_USER) && ReceiverState.heal(true, ReceiverState.DISABLED_UNTIL_USED));
        check("as the manifest says, or set on: nothing", !ReceiverState.heal(true, ReceiverState.DEFAULT)
            && !ReceiverState.heal(true, ReceiverState.ENABLED));
        check("off in the manifest itself: never turned on", !ReceiverState.heal(false, ReceiverState.DISABLED)
            && !ReceiverState.heal(false, ReceiverState.DEFAULT));
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
    }
}
