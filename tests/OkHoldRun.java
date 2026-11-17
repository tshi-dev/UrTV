package sa.urtv.screensaver;

/** tests/ok_hold_test.py: steps "k code action t" (a key; prints due or -) and "r t" (render asks; prints wait or go), one answer a line */
public final class OkHoldRun {
    public static void main(String[] args) {
        OkHold h = new OkHold();
        for (String a : args) {
            String[] p = a.split(" ");
            if (p[0].equals("k")) System.out.println(h.key(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Long.parseLong(p[3])) ? "due" : "-");
            else System.out.println(h.defer(Long.parseLong(p[1])) ? "wait" : "go");
        }
    }
}
