package sa.urtv.home;

/**
 * 4.4 beta 4 (review F89): sizes for a photo that goes to «صوري». The screen's limit is on the LONG side (PhotoShrink.screenMax: 1920 or 1280 px); the icon editor's decode reads
 * its size as the SHORT side, so a 12 MP photo (4032 x 3024) was decoded whole (about 48 MB) and its 3 to 5 MB JPEG sent through Binder (about 1 MB at most). No Android in it
 * (tests/PhotoSizeRun.java).
 */
public final class PhotoSize {
    private PhotoSize() { }

    /** the biggest power of two the decoder may sub-sample by while the long side stays at least {@code maxLong} (1 when the photo is not bigger) */
    public static int sample(int w, int h, int maxLong) {
        int longSide = Math.max(w, h), s = 1;
        if (maxLong <= 0) return 1;
        while (longSide / (s * 2) >= maxLong) s *= 2;
        return s;
    }

    /** {width, height} with the long side {@code maxLong} and the shape kept; never bigger than the photo (no enlarging) */
    public static int[] fit(int w, int h, int maxLong) {
        int longSide = Math.max(w, h);
        if (w <= 0 || h <= 0 || maxLong <= 0 || longSide <= maxLong) return new int[] {Math.max(w, 0), Math.max(h, 0)};
        double k = (double) maxLong / longSide;
        return new int[] {Math.max(1, (int) Math.round(w * k)), Math.max(1, (int) Math.round(h * k))};
    }

    /** the most bytes of JPEG one provider call carries (Binder's whole transaction is about 1 MB: a margin is left) */
    public static final int MAX_JPEG_BYTES = 700_000;

    /** quality steps to try, best first: the first whose JPEG fits {@link #MAX_JPEG_BYTES} is sent */
    public static final int[] QUALITIES = {88, 80, 72, 64, 56};

    /**
     * What an EXIF orientation tag (1 to 8, as ExifInterface reads it) asks of the decoded picture: {degrees clockwise, mirror left-right 1 or 0}. A portrait photo from a
     * camera is stored sideways with a tag; BitmapFactory ignores it (review F98). Anything else is {0, 0}.
     */
    public static int[] orient(int exif) {
        switch (exif) {
            case 2: return new int[]{0, 1};
            case 3: return new int[]{180, 0};
            case 4: return new int[]{180, 1};
            case 5: return new int[]{270, 1};
            case 6: return new int[]{90, 0};
            case 7: return new int[]{90, 1};
            case 8: return new int[]{270, 0};
            default: return new int[]{0, 0};
        }
    }
}
