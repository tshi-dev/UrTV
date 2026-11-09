package sa.urtv.home;

/** The home's wallpaper rotation order (4.3): «صورنا», the owner's photos and every pack take turns in a shuffled order, a new one for each round. No Android in here. */
final class WallShuffle {
    private WallShuffle() { }

    /** the positions 0..n-1 in a shuffled order: the same for one round (the seed), another for the next */
    static int[] of(int n, long seed) {
        int[] a = new int[Math.max(0, n)];
        for (int i = 0; i < a.length; i++) a[i] = i;
        long x = (seed + 1) * 2654435761L & 0xFFFFFFFFL;
        if (x == 0) x = 1;
        for (int i = a.length - 1; i > 0; i--) {
            x = (x * 1664525L + 1013904223L) & 0xFFFFFFFFL;
            int j = (int) (x % (i + 1)), t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
        return a;
    }
}
