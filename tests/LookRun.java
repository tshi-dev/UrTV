package sa.urtv.home;

/**
 * For tests/look_test.py: Look on a plain JVM. Prints "light MODE SUN MINUTE = 0|1" for a set of cases, "mode RAW = MODE", and the
 * contrast of each look's text over its scrim laid on a photo: "contrast LOOK PART PHOTO = ratio".
 */
public final class LookRun {
    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        for (String raw : new String[]{"auto", "light", "dark", "", "night", null})
            out.append("mode ").append(raw).append(" = ").append(Look.mode(raw)).append('\n');
        String[] modes = {"auto", "light", "dark"};
        Boolean[] suns = {Boolean.TRUE, Boolean.FALSE, null};
        int[] minutes = {0, 5 * 60 + 59, 6 * 60, 12 * 60, 18 * 60 + 29, 18 * 60 + 30, 23 * 60 + 59};
        for (String m : modes) for (Boolean s : suns) for (int t : minutes)
            out.append("light ").append(m).append(' ').append(s).append(' ').append(t).append(" = ").append(Look.light(m, s, t) ? 1 : 0).append('\n');
        // photos: a mid grey, a dark one, a bright one; the top band (the clock) and the bottom (the title and the dock) of the scrim
        int[] photos = {0xFF808080, 0xFF202020, 0xFFE0E0E0};
        String[] names = {"grey", "dark", "bright"};
        for (int light = 0; light < 4; light++) {   // the home's two, then «رملي»'s two
            Look.Palette p = light < 2 ? Look.of(light == 1) : Look.of("sand", light == 3);
            for (int k = 0; k < photos.length; k++) {
                int top = Look.over(p.scrim[0], photos[k]), bottom = Look.over(p.scrim[4], photos[k]), grid = Look.over(p.shade, photos[k]);
                String look = new String[] {"dark", "light", "sanddark", "sandlight"}[light];
                out.append("contrast ").append(look).append(" clock ").append(names[k]).append(" = ").append(Look.contrast(p.text, top)).append('\n');
                out.append("contrast ").append(look).append(" date ").append(names[k]).append(" = ").append(Look.contrast(p.soft, top)).append('\n');
                out.append("contrast ").append(look).append(" greeting ").append(names[k]).append(" = ").append(Look.contrast(p.gold, top)).append('\n');
                out.append("contrast ").append(look).append(" title ").append(names[k]).append(" = ").append(Look.contrast(p.ink, bottom)).append('\n');
                out.append("contrast ").append(look).append(" gridname ").append(names[k]).append(" = ").append(Look.contrast(p.ink, grid)).append('\n');
                int thin = Look.over(p.scrim[2], photos[k]);   // the thinnest middle of the veil: the rows' names under the favourites
                out.append("contrast ").append(look).append(" rowname ").append(names[k]).append(" = ").append(Look.contrast(p.gold, thin)).append('\n');
                out.append("contrast ").append(look).append(" rownamegrid ").append(names[k]).append(" = ").append(Look.contrast(p.gold, grid)).append('\n');
                out.append("contrast ").append(look).append(" softthin ").append(names[k]).append(" = ").append(Look.contrast(p.soft, thin)).append('\n');
            }
        }
        System.out.print(out);
    }
}
