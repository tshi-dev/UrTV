package sa.urtv.home;

/** Prints ParentalText's lines for tests/parental_text_test.py (Arabic, the stub S). */
public class ParentalTextRun {
    public static void main(String[] a) {
        StringBuilder out = new StringBuilder();
        for (int n : new int[] {0, 1, 2, 3, 10, 11, 25}) out.append(ParentalText.apps(n)).append('\n');
        out.append(ParentalText.summary(3, true, false)).append('\n');
        out.append(ParentalText.summary(0, false, true)).append('\n');
        System.out.print(out);
    }
}
