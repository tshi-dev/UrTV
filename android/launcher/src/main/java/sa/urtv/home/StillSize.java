package sa.urtv.home;

/** 4.4 beta 4 (lag on the way out to an app): the card's still (HomeCard.keepStill) is drawn at half its size, never under 1 px. Plain Java (tests/StillSizeRun.java). */
final class StillSize {
    private StillSize() { }

    static int side(int px) { return Math.max(1, (px + 1) / 2); }
}
