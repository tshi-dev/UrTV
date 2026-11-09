package sa.urtv.home;

public final class WallShuffleRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }
    static boolean perm(int[] a) { boolean[] seen = new boolean[a.length]; for (int v : a) { if (v < 0 || v >= a.length || seen[v]) return false; seen[v] = true; } return true; }

    public static void main(String[] x) {
        int[] r0 = WallShuffle.of(60, 0), r0b = WallShuffle.of(60, 0), r1 = WallShuffle.of(60, 1);
        check("a round is a permutation: every photo once", perm(r0) && perm(r1));
        check("the same order for one round, another for the next", java.util.Arrays.equals(r0, r0b) && !java.util.Arrays.equals(r0, r1));
        int moved = 0;
        for (int i = 0; i < 60; i++) if (r0[i] != i) moved++;
        check("it is a real shuffle, not the order of the packs", moved > 45);
        check("no photos, one photo", WallShuffle.of(0, 3).length == 0 && WallShuffle.of(1, 3)[0] == 0);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
