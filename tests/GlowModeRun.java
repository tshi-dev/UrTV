package sa.urtv.home;

/** Prints, for tests/glow_mode_test.py, the glow's mode and strength share for a few settings strings. */
public class GlowModeRun {
    public static void main(String[] a) {
        String[] in = {null, "", "accent=;bar=100;dim=0;focus=solid", "glow=light", "glow=on", "glow=off", "glow=bogus", "text=clear;glow=on"};
        StringBuilder out = new StringBuilder();
        for (String s : in) {
            GlassStyle.setTweaks(s);
            out.append(GlassStyle.glowMode()).append(':').append(GlowLook.share(GlassStyle.glowMode())).append(':').append(GlassStyle.materialKey()).append('\n');
        }
        System.out.print(out);
    }
}
