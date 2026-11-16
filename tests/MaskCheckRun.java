package sa.urtv.screensaver;

/** tests/photo_glass_test.py runs this: MaskCheck's answers, one per line */
public final class MaskCheckRun {
    public static void main(String[] a) {
        System.out.println("shape\t" + MaskCheck.sameShape(1920, 1080, 1280, 720) + "," + MaskCheck.sameShape(1900, 1080, 1920, 1080) + ","
                + MaskCheck.sameShape(1440, 1080, 1920, 1080) + "," + MaskCheck.sameShape(0, 1, 1, 1));
        int[] bw = new int[100], grey = new int[100], black = new int[100];
        for (int i = 0; i < 100; i++) { bw[i] = i < 40 ? 0xFF000000 : 0xFFFFFFFF; grey[i] = i < 5 ? 0xFF000000 : i < 10 ? 0xFFFFFFFF : 0xFF808080; black[i] = 0xFF000000; }
        int[] soft = bw.clone();
        for (int i = 0; i < 12; i++) soft[i * 8] = 0xFF7F7F7F;   // an edge of a few grey pixels is fine
        System.out.println("tones\t" + MaskCheck.twoTone(bw) + "," + MaskCheck.twoTone(soft) + "," + MaskCheck.twoTone(grey) + "," + MaskCheck.twoTone(black) + "," + MaskCheck.twoTone(new int[0]));
            int w = 20, h = 10;   // a skyline: sky above row 6, scenery below; a ragged one jumps every other column; one all scenery; one sky only at the bottom
        int[] sky = new int[w * h], rag = new int[w * h], full = new int[w * h], flip = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x;
            sky[i] = y >= 6 ? 0xFFFFFFFF : 0xFF000000;
            rag[i] = y >= (x % 2 == 0 ? 2 : 8) ? 0xFFFFFFFF : 0xFF000000;
            full[i] = 0xFFFFFFFF;
            flip[i] = y < 6 ? 0xFFFFFFFF : 0xFF000000;
        }
        System.out.println("cover\t" + MaskCheck.cover(sky, w, h) + "," + MaskCheck.cover(full, w, h) + "," + MaskCheck.cover(flip, w, h) + "," + MaskCheck.cover(sky, w, h + 1));
        System.out.println("smooth\t" + MaskCheck.smooth(sky, w, h) + "," + MaskCheck.smooth(rag, w, h));
    }
}
