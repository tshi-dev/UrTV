package sa.urtv.home;

/**
 * For tests/input_once_test.py: InputDisplay.distinct on a few made-up source lists. Each line printed is the kept indexes, comma-separated.
 * An input is key|type:ownName|port(1/0)|passthrough(1/0); a list is inputs joined by ";".
 */
public final class InputOnceRun {
    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        for (String list : args) {
            String[] in = list.split(";");
            String[] key = new String[in.length], same = new String[in.length];
            boolean[] port = new boolean[in.length], pass = new boolean[in.length];
            for (int i = 0; i < in.length; i++) {
                String[] f = in[i].split("\\|");
                key[i] = f[0]; same[i] = f[1]; port[i] = f[2].equals("1"); pass[i] = f[3].equals("1");
            }
            StringBuilder line = new StringBuilder();
            for (int i : InputDisplay.distinct(key, same, port, pass)) line.append(line.length() == 0 ? "" : ",").append(i);
            out.append(line).append('\n');
        }
        System.out.print(out);
    }
}
