package sa.urtv.home;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * For tests/tv_vendor_test.py: TvVendor.java on one made-up TV. Arguments: the configured live TV, the configured picker ("-" for empty),
 * then what this TV can open (packages and package/.Activity). "boom" among them makes the probe throw for com.tcl.tv.
 * Prints: the live TV, the picker ("none" for null), and every usable live TV joined by commas.
 */
public final class TvVendorRun {
    public static void main(String[] a) {
        String live = a[0].equals("-") ? "" : a[0], picker = a[1].equals("-") ? "" : a[1];
        Set<String> has = new HashSet<>(Arrays.asList(a).subList(2, a.length));
        TvVendor.Probe p = c -> {
            if (has.contains("boom") && c.equals("com.tcl.tv")) throw new IllegalStateException("x");
            return has.contains(c);
        };
        String l = TvVendor.first(live, TvVendor.LIVE_TV, p), k = TvVendor.first(picker, TvVendor.PICKERS, p);
        System.out.println((l == null ? "none" : l) + "|" + (k == null ? "none" : k) + "|" + String.join(",", TvVendor.usable(live, TvVendor.LIVE_TV, p)));
    }
}
