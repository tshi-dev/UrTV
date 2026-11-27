package sa.urtv.home;

/** For tests/photo_size_test.py: the sizes of a photo for «صوري» (F89). One line a check: name TAB ok|FAIL. */
public final class PhotoSizeRun {
    static void c(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        c("a 12 MP photo (4032 x 3024) for a 1920 screen is sub-sampled by 2 (2016 x 1512, 12 MB), never decoded whole (the old short-side rule gave 1)", PhotoSize.sample(4032, 3024, 1920) == 2);
        c("a portrait photo is read by its long side too", PhotoSize.sample(3024, 4032, 1920) == 2 && PhotoSize.sample(3000, 6000, 1920) == 2);
        c("a photo that is not bigger than the limit is not sub-sampled", PhotoSize.sample(1920, 1080, 1920) == 1 && PhotoSize.sample(1280, 720, 1920) == 1);
        c("a big one on a 1280 TV: 8000 px long is sub-sampled by 4 (2000 px, not under 1280)", PhotoSize.sample(8000, 6000, 1280) == 4);
        c("the sub-sampled long side is never under the limit", java.util.stream.IntStream.of(1921, 2559, 2560, 3840, 4032, 7000, 12000).allMatch(l -> l / PhotoSize.sample(l, l / 2, 1920) >= 1920));
        int[] f = PhotoSize.fit(2016, 1512, 1920);
        c("the exact fit keeps the shape: 2016 x 1512 to 1920 x 1440", f[0] == 1920 && f[1] == 1440);
        int[] g = PhotoSize.fit(1512, 2016, 1920);
        c("portrait: 1512 x 2016 to 1440 x 1920", g[0] == 1440 && g[1] == 1920);
        int[] h = PhotoSize.fit(1000, 600, 1920);
        c("a photo smaller than the limit is not enlarged", h[0] == 1000 && h[1] == 600);
        c("the JPEG sent in one provider call is at most 700 KB (Binder carries about 1 MB), at qualities from 88 down", PhotoSize.MAX_JPEG_BYTES <= 700_000 && PhotoSize.QUALITIES[0] == 88 && PhotoSize.QUALITIES.length >= 4);
    }
}
