package sa.urtv.home;

/** For tests/scene_home_test.py: ScenePlan's rules on a plain JVM. Args: the bundled pack's scenes.tsv, then (may be missing) a folder of the clock's glyphs and the cut-outs made small (glyphs(), pixels()). One line per answer. */
public final class ScenePlanRun {
    /** the glyphs the test drew with the clock's font: n, then per glyph its char, x0, y0, w, h, advance x1000 and w*h ink bytes */
    static ScenePlan.Glyphs glyphs(java.io.File f) throws java.io.IOException {
        if (!f.isFile()) return null;
        try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(f)))) {
            ScenePlan.Glyphs g = new ScenePlan.Glyphs();
            for (int n = in.readInt(); n > 0; n--) {
                char c = (char) in.readInt();
                int x0 = in.readInt(), y0 = in.readInt(), w = in.readInt(), h = in.readInt();
                float adv = in.readInt() / 1000f;
                boolean[] ink = new boolean[w * h];
                for (int i = 0; i < ink.length; i++) ink[i] = in.readByte() != 0;
                g.put(c, x0, y0, w, h, adv, ink);
            }
            return g;
        }
    }

    /** made-up scenery on a 1920 x 1080 screen (480 x 270 px): "x<n>" a tower 8 px wide at x n from the box's top down; "colon" one through the middle (the colon of «١٢:٣٠»); "top" a band over the digits' top; "base<p>" a block over p % of their height from the base */
    static ScenePlan.Cut made(String kind, int[] box) {
        int[] a = new int[480 * 270];
        float bh = box[3] - box[1];
        for (int y = 0; y < 270; y++) for (int x = 0; x < 480; x++) {
            float sx = x * 4 + 2, sy = y * 4 + 2;
            boolean on;
            if (kind.startsWith("x")) on = Math.abs(sx - Integer.parseInt(kind.substring(1))) < 4 && sy > box[1];
            else if (kind.equals("colon")) on = Math.abs(sx - (box[0] + box[2]) / 2f) < 6 && sy > box[1];
            else if (kind.equals("top")) on = sy > box[1] - 20 && sy < box[1] + bh * 0.4f;
            else on = sy > box[3] - bh * Integer.parseInt(kind.substring(4)) / 100f;
            a[y * 480 + x] = on ? 255 : 0;
        }
        return new ScenePlan.Cut(a, 480, 270, new int[] {0, 0, 1920, 1080});
    }

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
        if (args.length >= 2) {   // a folder the test filled: glyphs.bin (the clock's font), and per scene with a cut-out <id>-48/-192/-cut.bin
            java.io.File dir = new java.io.File(args[1]);
            ScenePlan.Glyphs g = glyphs(new java.io.File(dir, "glyphs.bin"));
            out.append("glyphs\t").append(g != null && g.has("٠١٢٣٤٥٦٧٨٩:0123456789")).append('\n');
            // the ink rule on made-up scenery: a narrow tower in the gap between two digits; the same through the colon; a block over 20 % of the base
            int[] box2 = {960 - 364, 300, 960 + 364, 580};
            int free = -1;   // a column where no digit (any of ٠-٩ in any place, either layout) nor the colon has ink: a tower may run there
            for (int x = box2[0] + 8; x < box2[2] - 8 && free < 0; x += 4) {
                float[] l = ScenePlan.inkLost(g, false, made("x" + x, box2), box2, 1920, 1080, 1f);
                if (l != null && l[0] == 0f && l[1] == 0f && l[2] == 0f) free = x;
            }
            out.append("free\t").append(free).append('\n');
            for (String kind : new String[] {"free", "colon", "top", "base2", "base40"}) {
                if (kind.equals("free") && free < 0) continue;
                float[] lost = ScenePlan.inkLost(g, false, made(kind.equals("free") ? "x" + free : kind, box2), box2, 1920, 1080, 1f);
                out.append("ink\t").append(kind).append('\t').append(lost == null ? "null" : String.format("%.3f,%.3f,%.3f", lost[0], lost[1], lost[2]))
                   .append('\t').append(ScenePlan.readable(lost)).append('\n');
            }
            for (ScenePlan.Scene sc : ScenePlan.parse("saudi", tsv)) {
                if (sc.mask == null || !new java.io.File(dir, sc.id + "-cut.bin").isFile()) continue;
                int[][] n = pixels(new java.io.File(dir, sc.id + "-48.bin").getPath()), f = pixels(new java.io.File(dir, sc.id + "-192.bin").getPath()), c = pixels(new java.io.File(dir, sc.id + "-cut.bin").getPath());
                int[] l48 = ScenePlan.skyline(n[1], n[0][0], n[0][1], sc.mask), l192 = ScenePlan.skyline(f[1], f[0][0], f[0][1], sc.mask, 192);
                int[] al = new int[c[1].length];
                for (int i = 0; i < al.length; i++) al[i] = c[1][i] >>> 24;
                ScenePlan.Cut cut = new ScenePlan.Cut(al, c[0][0], c[0][1], sc.mask);
                for (boolean cards : new boolean[] {false, true}) for (boolean rtl : new boolean[] {true, false}) {
                    int[] b2 = ScenePlan.clockBox(sc, 1920, 1080, cards, rtl);
                    float d = ScenePlan.depthInk(l48, l192, g, false, cut, b2, 1920, 1080);
                    float[] lost = d == 0f ? null : ScenePlan.inkLost(g, false, cut, b2, 1920, 1080, d);
                    out.append("depth\t").append(sc.id).append('\t').append(cards).append('\t').append(rtl).append('\t').append(String.format("%.2f", d)).append('\t')
                       .append(lost == null ? "-" : String.format("%.3f,%.3f,%.3f", lost[0], lost[1], lost[2])).append('\t').append(d == 0f || ScenePlan.readable(lost)).append('\n');
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
        java.util.List<ScenePlan.Scene> xs = ScenePlan.parse("x", "aa\tx\tx\t100\t400\t-\t0\tc\t\t1200\nbb\tx\tx\t100\t400\t-\t0\tc\tnight\t-\ncc\tx\tx\t100\t400\t-\t0\tc\ndd\tx\tx\t100\t400\t-\t0\tc\t\t1900");
        for (ScenePlan.Scene x : xs) {
            int[] c0 = ScenePlan.clockBox(x, 1920, 1080, false, true), c1 = ScenePlan.clockBox(x, 1920, 1080, true, true), c7 = ScenePlan.clockBox(x, 1280, 720, false, true);
            out.append("digitsx\t").append(x.id).append('\t').append(x.digitsX).append('\t').append((c0[0] + c0[2]) / 2).append('\t').append((c1[0] + c1[2]) / 2).append('\t').append((c7[0] + c7[2]) / 2).append('\n');
        }
        System.out.print(out);
    }
}
