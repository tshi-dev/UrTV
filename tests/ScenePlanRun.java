package sa.urtv.home;

/** For tests/scene_home_test.py: ScenePlan's rules on a plain JVM. Args: the bundled pack's scenes.tsv, then (may be missing) its first scene's cut-out made 96 and 384 wide (pixels()). One line per answer. */
public final class ScenePlanRun {
    /** a picture the test wrote: its width and height, then its ARGB ints (big-endian) */
    static int[][] pixels(String path) throws java.io.IOException {
        try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(path)))) {
            int w = in.readInt(), h = in.readInt();
            int[] px = new int[w * h];
            for (int i = 0; i < px.length; i++) px[i] = in.readInt();
            return new int[][] {{w, h}, px};
        }
    }

    public static void main(String[] args) throws Exception {
        StringBuilder out = new StringBuilder();
        String tsv = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0])), "UTF-8");
        for (ScenePlan.Scene s : ScenePlan.parse("saudi", tsv))
            out.append("scene\t").append(s.id).append('\t').append(s.name).append('\t').append(s.digitsTop).append('\t').append(s.digitsBottom).append('\t')
               .append(s.mask == null ? "-" : s.mask[0] + "," + s.mask[1] + "," + s.mask[2] + "," + s.mask[3]).append('\t').append(s.dark).append('\n');
        out.append("bad\t").append(ScenePlan.parse("saudi", "Bad\tx\tx\t1\t900\t-\t0\tc\nok\tx\tx\t100\t150\t-\t0\tc\nok2\tx\tx\t100\t400\t5,5\t0\tc\nok3\tx\tx\t100\t400\t-\t0\tc").size()).append('\n');
        for (String p : new String[] {"", "saudi", "couch", "couch,saudi", "nope"}) out.append("packs\t").append(p).append('\t').append(String.join(",", ScenePlan.packs(p))).append('\n');
        for (int t = 0; t < 3; t++)
            out.append("tier\t").append(t).append('\t').append(ScenePlan.decodeWidth(t, 1920)).append('\t').append(ScenePlan.mask(t)).append('\t').append(ScenePlan.fade(t))
               .append('\t').append(ScenePlan.stayMs(t, 0)).append('\t').append(ScenePlan.stayMs(t, 5)).append('\n');
        ScenePlan.Scene s = ScenePlan.parse("saudi", tsv).get(0);
        for (boolean cards : new boolean[] {false, true}) for (boolean rtl : new boolean[] {true, false}) {
            int[] b = ScenePlan.clockBox(s, 1920, 1080, cards, rtl);
            out.append("box\t").append(cards).append('\t').append(rtl).append('\t').append(b[0]).append(',').append(b[1]).append(',').append(b[2]).append(',').append(b[3]).append('\n');
        }
        int[] b = ScenePlan.clockBox(s, 1280, 720, true, true);
        out.append("box720\t").append(b[0]).append(',').append(b[1]).append(',').append(b[2]).append(',').append(b[3]).append('\n');
        out.append("next\t").append(ScenePlan.next(-1, 5)).append(',').append(ScenePlan.next(4, 5)).append(',').append(ScenePlan.next(0, 0)).append('\n');
        java.util.List<ScenePlan.Scene> all = ScenePlan.parse("saudi", tsv);
        java.util.List<ScenePlan.Scene> some = ScenePlan.parse("x", "aa\tx\tx\t100\t400\t-\t0\tc\tnight\nbb\tx\tx\t100\t400\t-\t0\tc\tfog\ncc\tx\tx\t100\t400\t-\t0\tc");
        for (boolean[] m : new boolean[][] {{false, false, false, false}, {true, true, false, false}, {true, false, false, false}, {false, false, true, true}, {true, true, true, true}, {false, false, true, false}}) {
            StringBuilder ids = new StringBuilder();
            for (ScenePlan.Scene x : ScenePlan.choose(some, m[0], m[1], m[2], m[3])) ids.append(x.id);
            StringBuilder sa = new StringBuilder();
            for (ScenePlan.Scene x : ScenePlan.choose(all, m[0], m[1], m[2], m[3])) sa.append(x.id).append(' ');
            out.append("choose\t").append(m[0]).append(m[1]).append(m[2]).append(m[3]).append('\t').append(ids).append('\t').append(sa.toString().trim()).append('\n');
        }
        out.append("stay\t").append(ScenePlan.stay("10")).append(',').append(ScenePlan.stay("300")).append(',').append(ScenePlan.stay("7")).append(',').append(ScenePlan.stay(null)).append('\n');
        out.append("fog\t").append(ScenePlan.fog(45)).append(',').append(ScenePlan.fog(48)).append(',').append(ScenePlan.fog(3)).append('\n');
        out.append("tiers\t").append(ScenePlan.LOW).append(',').append(ScenePlan.MID).append(',').append(ScenePlan.NORMAL).append('\n');
        // 4.4 legibility (ScenePlan.cover/legible/depth): a box 280 tall at the middle, 1920 x 1080
        int[] box = {960 - 364, 300, 960 + 364, 580};
        for (int lift : new int[] {56, 84}) {   // a flat scenery 20 % and 30 % up the digits
            int[] flat = new int[192];
            java.util.Arrays.fill(flat, 580 - lift);
            out.append("flat\t").append(lift).append('\t').append(String.format("%.2f", ScenePlan.cover(flat, box, 1920, 1080, 1f))).append('\t').append(ScenePlan.legible(flat, box, 1920, 1080, 1f)).append('\n');
        }
        int[] tower = new int[192];
        java.util.Arrays.fill(tower, 1080);
        tower[96] = tower[97] = 320;   // a narrow tower at the colon, over the digits' top: 2 of 192 columns, inside one of 48
        int[] tower48 = new int[48];
        java.util.Arrays.fill(tower48, 1080);
        out.append("tower\t").append(ScenePlan.legible(tower, box, 1920, 1080, 1f)).append('\t').append(ScenePlan.depth(tower48, tower, box, 1920, 1080)).append('\t')
           .append(ScenePlan.depth(tower48, null, box, 1920, 1080)).append('\n');
        int[] low = new int[192];
        java.util.Arrays.fill(low, 1080);
        for (int c = 60; c < 132; c++) low[c] = 500;   // a broad block 29 % up: a little zoom brings it under 25 %
        float z = ScenePlan.depth(tower48, low, box, 1920, 1080);
        out.append("zoomed\t").append(z > 1f && z <= ScenePlan.MAX_ZOOM).append('\t').append(ScenePlan.legible(low, box, 1920, 1080, z)).append('\n');
        if (args.length >= 3) {   // the bundled scene's cut-out, made small by the test (96 nearest, as zoom() reads it; 384 smoothed, as legible() does)
            int[][] n = pixels(args[1]), f = pixels(args[2]);
            int[] l48 = ScenePlan.skyline(n[1], n[0][0], n[0][1], s.mask), l192 = ScenePlan.skyline(f[1], f[0][0], f[0][1], s.mask, 192);
            for (boolean cards : new boolean[] {false, true}) for (boolean rtl : new boolean[] {true, false}) {
                int[] b2 = ScenePlan.clockBox(s, 1920, 1080, cards, rtl);
                float d = ScenePlan.depth(l48, l192, b2, 1920, 1080);
                out.append("depth\t").append(s.id).append('\t').append(cards).append('\t').append(rtl).append('\t').append(String.format("%.2f", d)).append('\t')
                   .append(String.format("%.2f", ScenePlan.cover(l192, b2, 1920, 1080, 1f))).append('\t').append(d == 0f || ScenePlan.legible(l192, b2, 1920, 1080, d)).append('\n');
            }
        }
        System.out.print(out);
    }
}
