package sa.urtv.home;

/** tests/photo_glass_test.py runs this: PhotoGlassPlan's answers, one per line, tab separated */
public final class PhotoGlassRun {
    public static void main(String[] a) {
        String[] keys = {"pack:nature:7", "pack:haramain:3", "pack:ksa-cities:1", "pack:x:1", "pack:sea:abc", "photo:9hWJs7iblh8.jpg", "bundled:wallpapers/9hWJs7iblh8.jpg",
                "own:3:12345", "photo:../x.jpg", "photo:a.png", null};
        for (String k : keys) {
            String[] r = PhotoGlassPlan.key(k);
            System.out.println("key\t" + k + "\t" + (r == null ? "-" : r[0] + "/" + r[1]));
        }
        for (String k : new String[] {"own:3:12345", "own:0:1", "own:51:1", "own:x:1", "photo:a.jpg", null}) System.out.println("own\t" + k + "\t" + PhotoGlassPlan.own(k));
        int[] c1 = PhotoGlassPlan.crop(4000, 3000, 1920, 1080), c2 = PhotoGlassPlan.crop(1080, 1920, 1920, 1080), c3 = PhotoGlassPlan.crop(1920, 1080, 1920, 1080);
        System.out.println("crop\t" + java.util.Arrays.toString(c1) + "\t" + java.util.Arrays.toString(c2) + "\t" + java.util.Arrays.toString(c3));
        int[] m = new int[96 * 54];   // a mask 96 x 54: black sky, white from row 27 down, and from row 10 in the first two columns' share
        for (int y = 0; y < 54; y++) for (int x = 0; x < 96; x++) m[y * 96 + x] = (y >= 27 || (x < 4 && y >= 10)) ? 0xFFFFFFFF : 0xFF000000;
        int[] sk = PhotoGlassPlan.skyline(m, 96, 54);
        System.out.println("skyline\t" + sk[0] + "," + sk[1] + "," + sk[2] + "," + sk[47] + "\t" + sk.length);
        int[] flat = new int[48], high = new int[48], low = new int[48];
        for (int i = 0; i < 48; i++) { flat[i] = 540; high[i] = 100; low[i] = 900; }
        print("none-nocards", PhotoGlassPlan.box(null, 1920, 1080, false, true, false));
        print("none-cards-rtl", PhotoGlassPlan.box(null, 1920, 1080, true, true, false));
        print("none-cards-ltr", PhotoGlassPlan.box(null, 1920, 1080, true, false, false));
        print("mid", PhotoGlassPlan.box(flat, 1920, 1080, false, true, false));
        print("high", PhotoGlassPlan.box(high, 1920, 1080, false, true, false));
        print("low", PhotoGlassPlan.box(low, 1920, 1080, false, true, false));
        print("mid720", PhotoGlassPlan.box(flat, 1280, 720, false, true, false));
        int[] lowish = new int[48];
        for (int i = 0; i < 48; i++) lowish[i] = 640;
        print("lowish", PhotoGlassPlan.box(lowish, 1920, 1080, false, true, false));
        print("lowish-row", PhotoGlassPlan.box(lowish, 1920, 1080, false, true, true));
        print("mid-row", PhotoGlassPlan.box(flat, 1920, 1080, false, true, true));
        print("short", PhotoGlassPlan.box(new int[5], 1920, 1080, false, true, false));
    }

    private static void print(String name, int[] b) { System.out.println("box\t" + name + "\t" + b[0] + "," + b[1] + "," + b[2] + "," + b[3]); }
}
