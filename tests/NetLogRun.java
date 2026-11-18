package sa.urtv.screensaver;

/** G3: NetLog's rules on a plain JVM (tests/netlog_test.py). */
public final class NetLogRun {
    static int failed;
    static void check(String what, boolean ok, Object got) { System.out.println((ok ? "PASS " : "FAIL ") + what + (ok ? "" : "  -> " + got)); if (!ok) failed++; }

    public static void main(String[] a) {
        check("a https URL's host, lower case, without port or user", "raw.githubusercontent.com".equals(NetLog.host("https://u@RAW.githubusercontent.com:443/x?y")), NetLog.host("https://u@RAW.githubusercontent.com:443/x?y"));
        check("http counts", "api.open-meteo.com".equals(NetLog.host("http://api.open-meteo.com/v1")), NetLog.host("http://api.open-meteo.com/v1"));
        check("the page's local hosts (.invalid) are not the network", NetLog.host("https://own.urtv.invalid/1.jpg") == null, NetLog.host("https://own.urtv.invalid/1.jpg"));
        check("file, data and blob URLs are not the network", NetLog.host("file:///android_asset/tv.html") == null && NetLog.host("data:image/png;base64,xx") == null && NetLog.host("blob:null/1") == null, "");
        check("nothing for null or garbage", NetLog.host(null) == null && NetLog.host("nothing") == null, "");

        final java.util.List<String> lines = new java.util.ArrayList<>();
        NetLog.logger = (min, n, hosts) -> lines.add(min + "|" + n + "|" + hosts);
        NetLog.reset();
        long t = 1_000_000L;
        NetLog.count("https://a.example/1", t);
        NetLog.count("https://a.example/2", t + 60_000);
        NetLog.count("https://b.example/1", t + 120_000);
        NetLog.count("https://own.urtv.invalid/1.jpg", t + 130_000);
        NetLog.count("file:///x", t + 140_000);
        check("no line within the window", lines.isEmpty(), lines);
        NetLog.count("https://c.example/1", t + NetLog.WINDOW_MS + 5_000);
        check("one line when a request comes after the window: its minutes, its 3 network requests, the most asked host first", lines.size() == 1 && "10|3|a.example 2, b.example 1".equals(lines.get(0)), lines);
        NetLog.count("https://c.example/2", t + 3 * NetLog.WINDOW_MS);
        check("a quiet half hour: the next line covers it (19 whole minutes, the 1 request of the new window)", lines.size() == 2 && "19|1|c.example 1".equals(lines.get(1)), lines);
        NetLog.logger = (min, n, hosts) -> { throw new RuntimeException("broken logger"); };
        NetLog.count("https://c.example/3", t + 5 * NetLog.WINDOW_MS);
        check("a logger that throws never reaches the caller", true, "");
        check("the line: per minute, the bytes, the hosts", "net 10 min: 25 requests (2.5/min), in 1.0 MB, out 40 KB; a.example 20, b.example 5".equals(NetLog.line(10, 25, "a.example 20, b.example 5", 1 << 20, 40 * 1024)), NetLog.line(10, 25, "a.example 20, b.example 5", 1 << 20, 40 * 1024));
        check("the line without bytes (TrafficStats not supported)", "net 10 min: 0 requests (0.0/min)".equals(NetLog.line(10, 0, "", -1, -1)), NetLog.line(10, 0, "", -1, -1));
        System.exit(failed == 0 ? 0 : 1);
    }
}
