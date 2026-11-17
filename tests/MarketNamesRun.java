package sa.urtv.home;

/** For tests/market_names_test.py: prints «code TAB name» for each code given, as MarketData.name() says it. Plain JVM. */
public final class MarketNamesRun {
    public static void main(String[] a) throws Exception {
        java.io.PrintStream out = new java.io.PrintStream(System.out, true, "UTF-8");
        for (String code : a) out.println(code + "\t" + MarketData.name(code));
    }
}
