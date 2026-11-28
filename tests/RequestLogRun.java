package sa.urtv.home.phone;

/** For tests/phone_request_log_test.py: RequestLog's rules, one "name<TAB>got" line per check. */
public final class RequestLogRun {
    public static void main(String[] a) {
        String chrome = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36";
        String iphone = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1";
        System.out.println("android chrome\t" + RequestLog.family(chrome));
        System.out.println("iphone\t" + RequestLog.family(iphone));
        System.out.println("none\t" + RequestLog.family(null) + " " + RequestLog.family(" "));
        System.out.println("desktop\t" + RequestLog.family("Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0"));
        System.out.println("path\t" + RequestLog.path("/api/state?code=123456&x=1"));
        System.out.println("token\t" + RequestLog.path("/s/0123456789abcdef0123/photo"));
        System.out.println("root\t" + RequestLog.path("/") + " " + RequestLog.path(null));
        String l = RequestLog.line("GET", "/y?pin=1234", chrome);
        System.out.println("line\t" + l);
        System.out.println("no ua text\t" + (!l.contains("Mozilla") && !l.contains("SM-S918B") && !l.contains("1234")));
    }
}
