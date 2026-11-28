package sa.urtv.home;

/**
 * 4.4 beta 4 (a user: some apps' banners were cut on their tiles): a banner fills its 16:9 tile (CENTER_CROP) when its own shape is close to the
 * tile's, as the TV makers draw them (320 x 180). An app whose banner is another shape (a 2:1 strip, a 4:3 or square picture) lost its name or
 * its edges that way; such a banner is now shown whole, inside the tile (MainActivity.fitted draws it so). Close = at most TOLERANCE of the
 * width or the height would be cut. No Android in here (tests/BannerFitRun.java).
 */
final class BannerFit {
    private BannerFit() { }

    /** the part of a side a fill may cut: 6 % (a 320 x 180 banner on a 16:9 tile loses nothing; one 1.5 % off its shape, a hairline) */
    static final float TOLERANCE = 0.06f;

    /** true: show the iw x ih banner whole inside the w x h tile; false: fill the tile (what cut away is within TOLERANCE) */
    static boolean contain(int iw, int ih, int w, int h) {
        if (iw <= 0 || ih <= 0 || w <= 0 || h <= 0) return false;
        float own = iw / (float) ih, tile = w / (float) h;
        float cut = 1f - Math.min(own, tile) / Math.max(own, tile);   // what a fill cuts off the longer side, as a part of it
        return cut > TOLERANCE;
    }
}
