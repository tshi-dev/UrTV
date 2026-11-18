package sa.urtv.screensaver;

/** tests/presets_test.py: PresetMotion's numbers, printed as key=value */
public final class PresetMotionRun {
    public static void main(String[] a) {
        System.out.println("ms=" + PresetMotion.MS);
        System.out.println("e0=" + PresetMotion.eased(0f) + " e1=" + PresetMotion.eased(1f));
        System.out.println("e50=" + PresetMotion.eased(0.5f) + " e21=" + PresetMotion.eased(52f / 250f));
        System.out.println("r=" + PresetMotion.radius(0f, 0f, 600f, 800f));   // 3-4-5: the far corner is 1000 away
    }
}
