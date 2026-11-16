package sa.urtv.screensaver;

/** For tests/cache_sweep_test.py: CacheSweep on the folders given. Args: size|clear, then folders. Prints the bytes. */
public final class CacheSweepRun {
    public static void main(String[] a) {
        java.io.File[] roots = new java.io.File[a.length - 1];
        for (int i = 1; i < a.length; i++) roots[i - 1] = new java.io.File(a[i]);
        System.out.println("size".equals(a[0]) ? CacheSweep.size(roots) : CacheSweep.clear(roots));
    }
}
