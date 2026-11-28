package sa.urtv.home.tilawah;

import java.util.Arrays;

/** For tests/surah_buffer_test.py: args[0] the type, the rest decoder names; prints DecoderOrder's order as the names, comma-separated. */
public final class DecoderOrderRun {
    public static void main(String[] args) {
        java.util.List<String> names = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
        StringBuilder b = new StringBuilder();
        for (int i : DecoderOrder.order(args[0], names)) b.append(b.length() == 0 ? "" : ",").append(names.get(i));
        System.out.println(b);
    }
}
