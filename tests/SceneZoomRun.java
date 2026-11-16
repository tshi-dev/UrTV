package sa.urtv.home;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * tests/scene_zoom_test.py runs this. "scene TSV ID PIXELS": the scene's cut-out made small (PIXELS: "sw sh" then sw*sh ARGB ints), its skyline, the
 * clock's box, and the visible share before and after ScenePlan.zoom. "flat Y": a made-up flat skyline at Y (px of 1080) under a 1080 clock box.
 */
public final class SceneZoomRun {
    public static void main(String[] a) throws Exception {
        if (a[0].equals("scene")) {
            List<ScenePlan.Scene> all = ScenePlan.parse("saudi", new String(Files.readAllBytes(Paths.get(a[1])), "UTF-8"));
            ScenePlan.Scene s = null;
            for (ScenePlan.Scene x : all) if (x.id.equals(a[2])) s = x;
            String[] t = new String(Files.readAllBytes(Paths.get(a[3])), "UTF-8").trim().split("\\s+");
            int sw = Integer.parseInt(t[0]), sh = Integer.parseInt(t[1]);
            int[] px = new int[sw * sh];
            for (int i = 0; i < px.length; i++) px[i] = (int) Long.parseLong(t[2 + i]);
            int[] sky = ScenePlan.skyline(px, sw, sh, s.mask);
            for (boolean cards : new boolean[] {false, true}) {
                int[] box = ScenePlan.clockBox(s, 1920, 1080, cards, true);
                float k = ScenePlan.zoom(sky, box, 1920, 1080);
                System.out.println("scene\t" + s.id + "\t" + cards + "\t" + ScenePlan.visible(sky, box, 1920, 1080, 1f) + "\t" + k + "\t" + ScenePlan.visible(sky, box, 1920, 1080, k));
            }
        } else {
            int[] sky = new int[48];
            java.util.Arrays.fill(sky, Integer.parseInt(a[1]));
            int[] box = {600, 300, 1320, 600};
            float k = ScenePlan.zoom(sky, box, 1920, 1080);
            System.out.println("flat\t" + a[1] + "\t" + ScenePlan.visible(sky, box, 1920, 1080, 1f) + "\t" + k + "\t" + ScenePlan.visible(sky, box, 1920, 1080, k));
        }
    }
}
