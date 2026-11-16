package sa.urtv.home;

/** For tests/scene_home_test.py: ScenePlan's rules on a plain JVM. Args: the bundled pack's scenes.tsv. One line per answer. */
public final class ScenePlanRun {
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
        System.out.print(out);
    }
}
