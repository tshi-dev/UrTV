package sa.urtv.home;

/** 4.4 (R-501): ReceiverHeal's rule on a plain JVM (tests/receiver_heal_test.py). The states are PackageManager.COMPONENT_ENABLED_STATE_*. */
final class ReceiverState {
    static final int DEFAULT = 0, ENABLED = 1, DISABLED = 2, DISABLED_USER = 3, DISABLED_UNTIL_USED = 4;

    private ReceiverState() { }

    /** true: a receiver the manifest enables, now in one of the «off» states, goes back to DEFAULT; one the manifest declares off stays off */
    static boolean heal(boolean manifestEnabled, int state) {
        return manifestEnabled && (state == DISABLED || state == DISABLED_USER || state == DISABLED_UNTIL_USED);
    }
}
