package sa.urtv.home;

public final class ControlShapeRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        check("no choice (a TV as it is today), or any word but «عريض»: the tall panel; «عريض» the wide page", !ControlShape.isWide(null) && !ControlShape.isWide("") && !ControlShape.isWide("tall") && !ControlShape.isWide("x") && ControlShape.isWide("wide"));
        int[][] screens = {{1920, 1080}, {1280, 720}, {3840, 2160}, {1366, 768}, {960, 540}};
        for (int[] s : screens) for (boolean wide : new boolean[] {true, false}) {
            int[] z = ControlShape.size(s[0], s[1], wide);
            boolean fits = z[0] > 0 && z[1] > 0 && z[0] <= s[0] && z[1] <= s[1];
            if (wide) fits &= z[0] * 100 <= s[0] * 80 && z[1] * 100 <= s[1] * 85 && z[0] >= s[0] / 2;   // a page in the middle, clear of the edges, not a sliver
            else fits &= z[1] <= s[1] * 91 / 100 && z[0] * 1920 <= s[0] * 561 + 1919;   // the safe area; 560 of 1920
            check((wide ? "wide " : "tall ") + s[0] + "x" + s[1] + " fits the screen: " + z[0] + "x" + z[1], fits);
        }
        // the grid: 7 rows, wide, LTR then RTL
        int n = 7;
        for (boolean rtl : new boolean[] {false, true}) {
            int[] r0 = ControlShape.go(0, n, true, rtl), r1 = ControlShape.go(1, n, true, rtl), r6 = ControlShape.go(6, n, true, rtl), r5 = ControlShape.go(5, n, true, rtl);
            check("wide " + (rtl ? "RTL" : "LTR") + ": the first of a pair is at the start side, the other is one key away the other way", (rtl ? r0[1] == 1 && r0[0] == ControlShape.STAY && r1[0] == 0 : r0[0] == 1 && r0[1] == ControlShape.STAY && r1[1] == 0));
            check("wide " + (rtl ? "RTL" : "LTR") + ": up from the first line is the toggles, down moves a line, an odd last line is reached from both cells above", r0[2] == ControlShape.ABOVE && r1[2] == ControlShape.ABOVE && r0[3] == 2 && r5[3] == 6 && go(4, n, rtl)[3] == 6 && r6[2] == 4 && r6[3] == ControlShape.STAY);
        }
        int[] t3 = ControlShape.go(3, 6, false, false);
        check("tall: one to a line, left and right stay, up and down by one, no wrapping", t3[0] == ControlShape.STAY && t3[1] == ControlShape.STAY && t3[2] == 2 && t3[3] == 4 && ControlShape.go(0, 6, false, true)[2] == ControlShape.ABOVE && ControlShape.go(5, 6, false, true)[3] == ControlShape.STAY);
        boolean reach = true;   // every row is reachable by DOWN/RIGHT from the first, for 1..8 rows
        for (int rows = 1; rows <= 8 && reach; rows++) {
            boolean[] seen = new boolean[rows];
            java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
            q.add(0); seen[0] = true;
            while (!q.isEmpty()) { int i = q.poll(); for (int d : ControlShape.go(i, rows, true, false)) if (d >= 0 && !seen[d]) { seen[d] = true; q.add(d); } }
            for (boolean s : seen) reach &= s;
        }
        check("every row is reachable from the first with the D-pad, for 1 to 8 rows", reach);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }

    static int[] go(int i, int n, boolean rtl) { return ControlShape.go(i, n, true, rtl); }
}
