package sa.urtv.home;

/**
 * 4.4 (the owner: «It's good to have big stuff, but they're just toooo much»): the sizes of the setup, update and install screens, the rescue app and the
 * add-on's offer, matching the compact settings (work/433-design, docs/mockups/settings-compact): a window at most 560 dp wide in the middle of the
 * safe area, one text style (22 sp title, 17 sp body, rows and buttons, 14 sp second lines), buttons 44 dp tall. Before/after:
 * docs/mockups/compact-setup/. tests/compact_test.py reads every size of those screens against these.
 */
public final class Compact {
    private Compact() { }

    public static final int WINDOW_DP = 560, BUTTON_DP = 44;
    /** 4.4 (the owner: «ONE BOX for everything»): every screen's box (Box), the same size always; capped by the 90 % safe area */
    public static final int BOX_W_DP = 560, BOX_H_DP = 600;
    /** 4.4 (the owner: «ONE TEXT STYLE»): the only text sizes of these screens: one title, one body (rows and buttons too), one secondary */
    public static final int TITLE_SP = 22, BODY_SP = 17, SECONDARY_SP = 14;
    /** the QR of a setup screen: 88 dp, about 180 px on a 1080p TV */
    public static final int QR_DP = 88;
}
