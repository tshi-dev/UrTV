package sa.urtv.home;

/** For tests/scene_home_test.py: ScenePlan's rules on a plain JVM. Args: the bundled pack's scenes.tsv, then (may be missing) a folder of the cut-outs made small (pixels()). One line per answer. */
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
        out.append("tower\t").append(ScenePlan.legible(tower, box, 1920, 1080, 1f)).append('\t').append(ScenePlan.draws(null, ScenePlan.CLEAN, false, tower48, tower, box, 1920, 1080))
           .append('\t').append(ScenePlan.draws(null, ScenePlan.CLEAN, false, tower48, null, box, 1920, 1080)).append('\n');
        // the layouts: the gate's list (depth) and the place; with the cards beside the clock never; the owner's word waives the rule, not the list
        String[][] ds = {{null, "clean"}, {",clean,", "clean"}, {",clean,", "couch"}, {",clean,couch,", "couch"}, {",-,", "clean"}, {null, null}};
        StringBuilder dw = new StringBuilder();
        for (String[] d : ds) dw.append(ScenePlan.draws(d[0], d[1], false, null, null, box, 1920, 1080) ? 1 : 0);
        dw.append(' ').append(ScenePlan.draws(",clean,", "clean", true, tower48, tower, box, 1920, 1080) ? 1 : 0).append(ScenePlan.draws(",clean,", "couch", true, tower48, tower, box, 1920, 1080) ? 1 : 0);
        out.append("draws\t").append(dw).append('\t').append(ScenePlan.layout(false, false)).append(',').append(ScenePlan.layout(false, true)).append(',').append(ScenePlan.layout(true, false)).append('\n');
        int[] dot = new int[192];
        java.util.Arrays.fill(dot, 1080);
        dot[95] = dot[96] = 580 - 98;   // a narrow tower at the colon of «١٢:٣٠» (the middle), 35 % of the digits' height: up to its lower dot (30 %)
        out.append("colon\t").append(String.format("%.2f", ScenePlan.cover(dot, box, 1920, 1080, 1f, true))).append('\t').append(ScenePlan.legible(dot, box, 1920, 1080, 1f)).append('\n');
        if (args.length >= 2) {   // a folder the test filled: per scene with a cut-out <id>-48.bin and <id>-192.bin (SceneHome.skylineOf)
            java.io.File dir = new java.io.File(args[1]);
            for (ScenePlan.Scene sc : ScenePlan.parse("saudi", tsv)) {
                if (sc.mask == null || !new java.io.File(dir, sc.id + "-48.bin").isFile()) continue;
                int[][] n = pixels(new java.io.File(dir, sc.id + "-48.bin").getPath()), f = pixels(new java.io.File(dir, sc.id + "-192.bin").getPath());
                int[] l48 = ScenePlan.framed(ScenePlan.skyline(n[1], n[0][0], n[0][1], sc.mask), sc.frame), l192 = ScenePlan.framed(ScenePlan.skyline(f[1], f[0][0], f[0][1], sc.mask, 192), sc.frame);
                out.append("sceneframe\t").append(sc.id).append('\t').append(sc.frame[0]).append(',').append(sc.frame[1]).append(',').append(sc.frame[2]).append('\n');
                for (String place : new String[] {"clean", "couch", "cards-rtl", "cards-ltr"}) {
                    boolean cards = place.startsWith("cards"), rtl = !place.equals("cards-ltr");
                    int[] b2 = ScenePlan.clockBox(sc, 1920, 1080, cards, rtl);
                    boolean draw = ScenePlan.draws(sc.depth, ScenePlan.layout(cards, place.equals("couch")), sc.is(ScenePlan.OWNER_OK), l48, l192, b2, 1920, 1080);
                    out.append("depth\t").append(sc.id).append('\t').append(place).append('\t').append(draw ? "FIT" : "FRONT-ONLY").append('\t')
                       .append(String.format("%.3f", ScenePlan.cover(l192, b2, 1920, 1080, 1f))).append('\t').append(String.format("%.3f", ScenePlan.cover(l192, b2, 1920, 1080, 1f, true))).append('\t')
                       .append(b2[0]).append(',').append(b2[1]).append(',').append(b2[2]).append(',').append(b2[3]).append('\t').append(sc.is(ScenePlan.OWNER_OK) && !cards).append('\t')
                       .append(!draw || sc.is(ScenePlan.OWNER_OK) || ScenePlan.legible(l192, b2, 1920, 1080, 1f)).append('\n');
                }
            }
        }
        // never a misaligned cut-out: a made-up photo (480 x 270) and its cut-out taken from it at its place, then the same 2 px off, then another picture
        int[] ph = new int[480 * 270];
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < ph.length; i++) { int v = (i % 480) * 255 / 480, u = (i / 480) * 255 / 270; ph[i] = 0xFF000000 | (v << 16) | (u << 8) | rnd.nextInt(256); }
        int[] place = {0, 400, 1920, 680};
        int cw = 480, chh = 170;
        for (String kind : new String[] {"own", "off2", "other"}) {
            int[] c = new int[cw * chh];
            for (int y = 0; y < chh; y++) for (int x = 0; x < cw; x++) {
                int sx = kind.equals("off2") ? Math.min(479, x + 2) : x, sy = 100 + y;
                c[y * cw + x] = kind.equals("other") ? (0xFF000000 | rnd.nextInt(0xFFFFFF)) : ph[sy * 480 + sx];
            }
            out.append("aligned\t").append(kind).append('\t').append(ScenePlan.aligned(ph, 480, 270, c, cw, chh, place)).append('\n');
        }
        ScenePlan.Scene ok = ScenePlan.parse("x", "aa\tx\tx\t100\t400\t0,10,1920,600\t0\tc\tnight,owner_ok").get(0);
        out.append("ownerok\t").append(ok.is(ScenePlan.OWNER_OK)).append('\t').append(ok.is("night")).append('\t').append(s.is(ScenePlan.OWNER_OK)).append('\n');
        // 4.4 the clock in one place: scenes with other digits_top/bottom/x get the same box; the crop (11th column) frames the picture only
        java.util.List<ScenePlan.Scene> xs = ScenePlan.parse("x", "aa\tx\tx\t100\t400\t-\t0\tc\t\t1200\t120,60,1600\nbb\tx\tx\t300\t560\t-\t0\tc\tnight\t-\t-\n"
            + "cc\tx\tx\t40\t250\t-\t0\tc\ndd\tx\tx\t100\t400\t-\t0\tc\t\t\t500,100,1600");
        java.util.Set<String> boxes = new java.util.TreeSet<>();
        for (ScenePlan.Scene x : xs) for (boolean cards : new boolean[] {false, true}) for (boolean rtl : new boolean[] {true, false}) {
            int[] c = ScenePlan.clockBox(x, 1920, 1080, cards, rtl);
            boxes.add(cards + "" + rtl + ":" + c[0] + "," + c[1] + "," + c[2] + "," + c[3]);
        }
        out.append("samebox\t").append(boxes.size()).append('\n');
        for (ScenePlan.Scene x : xs) out.append("crop\t").append(x.id).append('\t').append(x.frame[0]).append(',').append(x.frame[1]).append(',').append(x.frame[2]).append('\n');
        float[] f1 = ScenePlan.frame(null, 1920, 1080, 1f), f2 = ScenePlan.frame(null, 1920, 1080, 1.2f), f3 = ScenePlan.frame(new int[] {120, 60, 1600}, 1920, 1080, 1f);
        out.append("frame\t").append(String.format("%.3f,%.1f,%.1f;%.3f,%.1f,%.1f;%.3f,%.1f,%.1f", f1[0], f1[1], f1[2], f2[0], f2[1], f2[2], f3[0], f3[1], f3[2])).append('\n');
        int[] src = new int[48];
        java.util.Arrays.fill(src, 1080);
        src[24] = 500;   // a tower at x 960-1000 of the photo, its top at 500
        int[] fr = ScenePlan.framed(src, new int[] {120, 60, 1600});
        StringBuilder at = new StringBuilder();
        for (int c = 0; c < 48; c++) if (fr[c] < 1080) at.append(c).append(':').append(fr[c]).append(' ');
        out.append("framed\t").append(at.toString().trim()).append('\t').append(ScenePlan.framed(src, ScenePlan.WHOLE) == src).append('\n');
        System.out.print(out);
    }
}
