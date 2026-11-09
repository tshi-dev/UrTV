package sa.urtv.home;

/** For tests/look_tweaks_test.py: every accent over every look, dark and light: the accent words read on the dialog and the focus ink reads on the focus fill (4.5:1). */
public final class AccentRun {
    public static void main(String[] a) {
        int bad = 0;
        for (String[] acc : GlassStyle.ACCENTS) {
            GlassStyle.setTweaks("accent=" + acc[0]);
            for (String look : GlassStyle.LOOKS) for (boolean light : new boolean[] {false, true}) {
                GlassStyle.Look l = GlassStyle.of(look, light);
                double words = GlassStyle.contrast(l.accent, GlassStyle.over(l.dialog, l.base)), ink = GlassStyle.contrast(l.onFocus, GlassStyle.over(l.focus, GlassStyle.over(l.dialog, l.base)));
                boolean ok = words >= 4.5 && ink >= 4.5;
                System.out.println((ok ? "PASS " : "FAIL ") + acc[0] + " " + look + (light ? "-light" : "-dark") + String.format(" words=%.1f ink=%.1f", words, ink));
                if (!ok) bad++;
            }
        }
        GlassStyle.setTweaks("");
        System.out.println(bad == 0 ? "ALL PASS" : "FAILED " + bad);
        if (bad != 0) System.exit(1);
    }
}
