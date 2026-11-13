package sa.urtv.home;

/**
 * 4.3.1 the home never shows black when it comes back (a Mi Box on 4.3: the cards on black for a few seconds, no photo and no apps bar): the rules, with no Android in it
 * (tests/wall_first_frame_test.py runs it). The home keeps a small decoded copy of the photo in front (480 px wide, in memory and on disk) and draws it in the first frame
 * of a start or a return when no photo is in a view; the full photo fades in over it later. A picture of the home (HomeSnapshot) is kept only when it shows a photo and the apps bar.
 */
final class WallFirstFrame {
    private WallFirstFrame() { }

    /** what to put in the photo view before the first frame */
    static final int KEEP = 0, MEMORY = 1, DISK = 2, NONE = 3;

    /** the view already has a photo: keep it; else the small copy in memory, else the one on disk; else nothing (the look's solid colour stays behind) */
    static int source(boolean viewHasPhoto, boolean smallInMemory, boolean smallOnDisk) {
        if (viewHasPhoto) return KEEP;
        if (smallInMemory) return MEMORY;
        return smallOnDisk ? DISK : NONE;
    }

    /** the width of the small copy, in px (about 30 KB as WebP; 480 x 270 x 4 = 0.5 MB in memory) */
    static final int SMALL_W = 480;

    static int smallHeight(int w, int h) { return w <= 0 || h <= 0 ? SMALL_W * 9 / 16 : Math.max(1, Math.round((float) SMALL_W * h / w)); }

    /**
     * A picture of the home worth keeping for the next start: the full photo is in front (not the stand-in), the apps bar is built, and the home is not trimmed. A picture taken
     * without them would paint a black, bar-less home over the next return.
     */
    static boolean snapshotWorthKeeping(boolean fullPhotoShown, boolean appsBarBuilt, boolean trimmed) { return fullPhotoShown && appsBarBuilt && !trimmed; }
}
