package sa.urtv.screensaver;

/** For tests/text_scale_test.py: each argument «widthDp,heightDp» printed as TextScale.factor (3 decimals), one per line. */
public final class TextScaleRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] f = a.split(",");
            System.out.println(String.format(java.util.Locale.ROOT, "%.3f", TextScale.factor(Integer.parseInt(f[0]), Integer.parseInt(f[1]))));
        }
    }
}
