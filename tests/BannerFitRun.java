package sa.urtv.home;

/** For tests/banner_fit_test.py: each argument «iw,ih,w,h» printed as BannerFit.contain's answer (1 whole, 0 fill), one per line. */
public final class BannerFitRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] f = a.split(",");
            System.out.println(BannerFit.contain(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3])) ? 1 : 0);
        }
    }
}
