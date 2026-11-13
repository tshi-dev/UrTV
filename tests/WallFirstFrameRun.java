package sa.urtv.home;

/** For tests/wall_first_frame_test.py: WallFirstFrame's rules. One line per check: name TAB ok|FAIL. */
public final class WallFirstFrameRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        check("a view that has a photo keeps it", WallFirstFrame.source(true, true, true) == WallFirstFrame.KEEP && WallFirstFrame.source(true, false, false) == WallFirstFrame.KEEP);
        check("resume with the bitmap gone: the small copy in memory first, else the one on disk", WallFirstFrame.source(false, true, true) == WallFirstFrame.MEMORY && WallFirstFrame.source(false, true, false) == WallFirstFrame.MEMORY
            && WallFirstFrame.source(false, false, true) == WallFirstFrame.DISK);
        check("a cold start with neither: nothing (the look's solid colour stays behind, never black)", WallFirstFrame.source(false, false, false) == WallFirstFrame.NONE);
        check("the small copy keeps the screen's shape", WallFirstFrame.smallHeight(1920, 1080) == 270 && WallFirstFrame.smallHeight(1280, 720) == 270 && WallFirstFrame.smallHeight(0, 0) == 270 && WallFirstFrame.smallHeight(1000, 1) == 1);
        check("a picture of the home is kept only with the full photo, the apps bar and no trim", WallFirstFrame.snapshotWorthKeeping(true, true, false) && !WallFirstFrame.snapshotWorthKeeping(false, true, false)
            && !WallFirstFrame.snapshotWorthKeeping(true, false, false) && !WallFirstFrame.snapshotWorthKeeping(true, true, true));
        System.exit(0);
    }
}
