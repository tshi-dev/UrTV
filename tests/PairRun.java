package sa.urtv.home.setup.pair;

/** For tests/pair_test.py: PairCode and PairState on a plain JVM. One line per case. */
public class PairRun {
    public static void main(String[] a) {
        System.out.println("code1 " + PairCode.clean("123456") + " " + PairCode.clean("12 34-56") + " " + PairCode.clean("١٢٣٤٥٦") + " " + PairCode.clean("۱۲۳۴۵۶"));
        System.out.println("code2 " + PairCode.clean("12345") + " " + PairCode.clean("1234567") + " " + PairCode.clean("12a456") + " " + PairCode.clean(null) + " " + PairCode.clean(""));
        System.out.println("port " + PairCode.port("41233") + " " + PairCode.port("80") + " " + PairCode.port("70000") + " " + PairCode.port("x") + " " + PairCode.port(null));
        PairState s = new PairState();
        long t = 1_000_000L;
        System.out.println("s1 " + s.state(t) + " " + s.begin(t));                                                // off: a code is refused
        s.arm(t);
        System.out.println("s2 " + s.state(t + 1000) + " " + s.state(t + PairState.ARMED_MS - 1) + " " + s.state(t + PairState.ARMED_MS));   // armed, still armed, ran out: off
        s.arm(t);
        boolean first = s.begin(t + 10), second = s.begin(t + 20);
        System.out.println("s3 " + first + " " + second + " " + s.state(t + 30));                                    // one at a time: pairing
        s.finish(false, "wrong");
        System.out.println("s4 " + s.state(t + 40) + " " + s.why() + " " + s.begin(t + 50));                         // failed, with the reason; a code is refused until retry
        s.retry(t + 60);
        System.out.println("s5 " + s.state(t + 70) + " [" + s.why() + "] " + s.begin(t + 80));                       // armed again, then pairing
        s.finish(true, "");
        System.out.println("s6 " + s.state(t + 90) + " [" + s.why() + "]");
        s.disarm();
        System.out.println("s7 " + s.state(t + 100));
    }
}
