package sa.urtv.home;

/**
 * 4.4 (the owner: «It's good to have big stuff, but they're just toooo much»): the sizes of the setup, update and install screens, the rescue app and the
 * add-on's offer, matching the compact settings (work/433-design, docs/mockups/settings-compact): a window at most 560 dp wide in the middle of the
 * safe area, a 24 sp title, 16-18 sp rows and buttons, 13-14 sp second lines (never under 13), buttons 44 dp tall with 18 sp words. Before/after:
 * docs/mockups/compact-setup/. tests/compact_test.py reads every size of those screens against these.
 */
public final class Compact {
    private Compact() { }

    public static final int WINDOW_DP = 560, BUTTON_DP = 44, TITLE_SP = 24, BUTTON_SP = 18, BODY_MAX_SP = 18, MIN_SP = 13;
    /** the QR of a setup screen: 88 dp, about 180 px on a 1080p TV */
    public static final int QR_DP = 88;
}
