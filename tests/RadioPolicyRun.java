package sa.urtv.home.tilawah;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * For tests/radio_policy_test.py: a live station's road from StreamUrl to RadioPolicy against a scripted local server (a raw socket, so it can
 * answer «ICY 200 OK»): a 302 to an allowed cleartext host, an ICY stream, a stream that drops after 5 s, a 500 every time. TestPlayer stands in
 * for the TV's player (it reads the stream: sound when 2 KB came, a failure when it ends or breaks), and the loop is MusicService's: a failure
 * goes to RadioPolicy.after, which says reconnect, retry, the second source, or the fault. One line per check: PASS/FAIL name.
 */
public final class RadioPolicyRun {
    static int failed;
    static void check(String name, boolean ok, Object got) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + got));
        if (!ok) failed++;
    }

    static int port;
    static volatile int hits500;

    /** the scripted server: /icy (ICY 200 OK, 3 s of bytes), /drop (HTTP 200, bytes for ?ms, then closed), /r302 (to http://localhost/icy), /e500 */
    static void serve(ServerSocket ss) {
        while (true) {
            try {
                final Socket s = ss.accept();
                new Thread(() -> {
                    try (Socket k = s) {
                        InputStream in = k.getInputStream();
                        StringBuilder head = new StringBuilder();
                        int c;
                        while ((c = in.read()) >= 0) { head.append((char) c); if (head.toString().endsWith("\r\n\r\n")) break; }
                        String path = head.toString().split(" ")[1];
                        OutputStream o = k.getOutputStream();
                        if (path.startsWith("/icy")) {
                            o.write("ICY 200 OK\r\nicy-name: test\r\nicy-metaint: 8192\r\nContent-Type: audio/mpeg\r\n\r\n".getBytes("ISO-8859-1"));
                            long end = System.currentTimeMillis() + 3000;
                            while (System.currentTimeMillis() < end) { o.write(new byte[1024]); o.flush(); Thread.sleep(50); }
                        } else if (path.startsWith("/drop")) {
                            long ms = Long.parseLong(path.replaceAll(".*ms=(\\d+).*", "$1"));
                            o.write("HTTP/1.0 200 OK\r\nContent-Type: audio/mpeg\r\nicy-br: 64\r\n\r\n".getBytes("ISO-8859-1"));
                            long end = System.currentTimeMillis() + ms;
                            while (System.currentTimeMillis() < end) { o.write(new byte[1024]); o.flush(); Thread.sleep(50); }
                        } else if (path.startsWith("/r302")) {
                            o.write(("HTTP/1.1 302 Found\r\nLocation: http://localhost:" + port + "/icy?rj-tok=once\r\nContent-Length: 0\r\n\r\n").getBytes("ISO-8859-1"));
                        } else {
                            hits500++;
                            byte[] b = new byte[177];
                            o.write(("HTTP/1.1 500 Internal Server Error\r\nContent-Length: 177\r\n\r\n").getBytes("ISO-8859-1"));
                            o.write(b);
                        }
                        o.flush();
                    } catch (Exception ignored) { }
                }).start();
            } catch (Exception e) {
                return;
            }
        }
    }

    /** the TV's player in the test: true when sound came (2 KB) and the stream then ended or broke within waitMs; "sound" also when it is still going */
    static final class Play { boolean sound; boolean ended; long ms; }
    static Play play(String address, long waitMs) {
        Play p = new Play();
        long t0 = System.currentTimeMillis();
        try (Socket s = new Socket()) {   // a raw read, as ExoPlayer's would be: it takes «ICY 200 OK» as well as HTTP
            URL u = new URL(address);
            s.connect(new java.net.InetSocketAddress(InetAddress.getByName(u.getHost()), u.getPort()), 3000);
            s.setSoTimeout(3000);
            s.getOutputStream().write(("GET " + u.getFile() + " HTTP/1.0\r\nHost: " + u.getHost() + "\r\nIcy-MetaData: 1\r\n\r\n").getBytes("ISO-8859-1"));
            InputStream in = s.getInputStream();
            byte[] buf = new byte[4096];
            long got = 0;
            int n;
            while (System.currentTimeMillis() - t0 < waitMs && (n = in.read(buf)) > 0) { got += n; if (got > 2048) p.sound = true; }
            p.ended = System.currentTimeMillis() - t0 < waitMs;
        } catch (Exception e) {
            p.ended = true;
        }
        p.ms = System.currentTimeMillis() - t0;
        return p;
    }

    /** MusicService's loop for one pick, with the steps it took and the words it said (null: none) */
    static List<String> steps = new ArrayList<>();
    static String said;
    static StreamUrl.HttpHosts local = h -> h.equals("localhost") || h.equals("127.0.0.1");

    static void pick(String url, String alt, long playMs, int maxSteps, boolean unsupportedAlways) {
        steps.clear();
        said = null;
        RadioPolicy.State st = new RadioPolicy.State();
        String address = url, second = alt;
        long clock = 0;
        for (int i = 0; i < maxSteps; i++) {
            String ready;
            int what = 0, extra;
            try {
                ready = StreamUrl.resolve(address, 3000, local);
                if (unsupportedAlways) { extra = PlayerFault.UNSUPPORTED; steps.add("unsupported"); }
                else {
                    Play p = play(ready, playMs);
                    if (p.sound) RadioPolicy.started(st);
                    if (p.sound && !p.ended) { steps.add("playing"); return; }
                    steps.add(p.sound ? "dropped@" + (p.ms / 1000) + "s" : "silent");
                    extra = PlayerFault.IO;
                }
            } catch (Exception e) {
                steps.add(e.getMessage().startsWith("server error") ? "500" : "refused");
                extra = PlayerFault.IO;
                if (!st.played) st.tries = StreamUrl.TRIES - 1;   // MusicService: the resolve's own 3 tries spent this source
            }
            clock += 1000;
            RadioPolicy.Step step = RadioPolicy.after(st, what, extra, second != null, clock);
            steps.add(step.name());
            if (step == RadioPolicy.Step.FAIL) { said = RadioPolicy.message(st, what, extra); return; }
            if (step == RadioPolicy.Step.ALT) { address = second; second = null; }
        }
    }

    public static void main(String[] a) throws Exception {
        ServerSocket ss = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        port = ss.getLocalPort();
        new Thread(() -> serve(ss)).start();
        String base = "http://127.0.0.1:" + port;
        try {
            // 302 to an allowed cleartext host, handed over unasked, then an ICY stream: it plays
            pick(base + "/r302", null, 2000, 3, false);
            check("a 302 to an allowed cleartext host, handed over unasked (its single-use token), then «ICY 200 OK»: it plays", steps.equals(java.util.Arrays.asList("playing")) && said == null, steps);
            check("StreamUrl takes «ICY 200 OK» as a stream answering (HttpURLConnection reads no status)", tryResolve(base + "/icy").equals(base + "/icy"), tryResolve(base + "/icy"));
            // a stream that drops after 5 s: joined again quietly, nothing said
            pick(base + "/drop?ms=5000", null, 8000, 2, false);
            check("a stream that drops after 5 s is joined again quietly (RECONNECT, nothing said)", steps.size() >= 2 && steps.get(0).equals("dropped@5s") && steps.get(1).equals("RECONNECT") && said == null, steps);
            // it keeps dropping: RECONNECTS times in the window, then the tries, then said as a connection fault, never as a format
            pick(base + "/drop?ms=400", null, 2000, 30, false);
            long reconnects = steps.stream().filter("RECONNECT"::equals).count();
            check("a stream that keeps dropping: " + RadioPolicy.RECONNECTS + " quiet reconnects, then the tries, then «تعذّر الاتصال بالبث»", reconnects == RadioPolicy.RECONNECTS
                && steps.get(steps.size() - 1).equals("FAIL") && "تعذّر الاتصال بالبث، تحقق من الإنترنت".equals(said), steps + " " + said);
            // a 500 every time with a second source: the second source plays
            hits500 = 0;
            pick(base + "/e500", base + "/icy", 2000, 6, false);
            check("a 500 every time (qurango's 177 bytes): this source is done, the second source plays", steps.equals(java.util.Arrays.asList("500", "ALT", "playing")) && said == null, steps);
            // a 500 every time and no second source: said as a connection fault
            pick(base + "/e500", null, 2000, 6, false);
            check("a 500 every time and no second source: «تعذّر الاتصال بالبث», never «صيغة البث غير مدعومة»", steps.equals(java.util.Arrays.asList("500", "FAIL")) && "تعذّر الاتصال بالبث، تحقق من الإنترنت".equals(said), steps + " " + said);
            // a station whose format the TV cannot play, every try and never a sound: only then «صيغة»
            pick(base + "/icy", null, 2000, 6, true);
            check("never a sound and «unsupported» every try: then (and only then) «صيغة البث غير مدعومة»", steps.get(steps.size() - 1).equals("FAIL") && "صيغة البث غير مدعومة".equals(said)
                && steps.stream().filter("RETRY"::equals).count() == StreamUrl.TRIES - 1, steps + " " + said);
        } finally {
            ss.close();
        }
        // the pure rules
        RadioPolicy.State s = new RadioPolicy.State();
        RadioPolicy.started(s);
        check("a station that played and then failed with «unsupported» mid-stream (4.2's report) is joined again, not said", RadioPolicy.after(s, 1, PlayerFault.UNSUPPORTED, false, 1000) == RadioPolicy.Step.RECONNECT, "");
        for (int i = 0; i < RadioPolicy.RECONNECTS; i++) RadioPolicy.after(s, 1, PlayerFault.UNSUPPORTED, false, 2000 + i);
        RadioPolicy.State late = new RadioPolicy.State();
        RadioPolicy.started(late);
        for (int i = 0; i < RadioPolicy.RECONNECTS; i++) RadioPolicy.after(late, 1, PlayerFault.IO, false, i);
        check("reconnects are counted in a 5-minute window: after it, quiet again", RadioPolicy.after(late, 1, PlayerFault.IO, false, RadioPolicy.WINDOW_MS + 10) == RadioPolicy.Step.RECONNECT, "");
        RadioPolicy.State f = new RadioPolicy.State();
        RadioPolicy.started(f);
        while (RadioPolicy.after(f, 1, PlayerFault.UNSUPPORTED, false, 5) != RadioPolicy.Step.FAIL) { }
        check("a station that played, whatever its last error, is said as a connection fault", "تعذّر الاتصال بالبث، تحقق من الإنترنت".equals(RadioPolicy.message(f, 1, PlayerFault.UNSUPPORTED)), RadioPolicy.message(f, 1, PlayerFault.UNSUPPORTED));
        check("the platform player's own death keeps its words", "توقف المشغّل".equals(RadioPolicy.message(new RadioPolicy.State(), PlayerFault.SERVER_DIED, 0)), "");
        check("ExoPlayer's codes as MediaPlayer's: 2001 io, 2002 timeout, 3001 malformed, 4001 unsupported, 1000 unknown",
            RadioPolicy.extraForExo(2001) == PlayerFault.IO && RadioPolicy.extraForExo(2002) == PlayerFault.TIMED_OUT && RadioPolicy.extraForExo(3001) == PlayerFault.MALFORMED
            && RadioPolicy.extraForExo(4001) == PlayerFault.UNSUPPORTED && RadioPolicy.extraForExo(1000) == 0, "");
        check("the waits grow: 2 s, 4 s, 8 s at most", StreamUrl.backoffMs(1) == 2000 && StreamUrl.backoffMs(3) == 8000, "");
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static String tryResolve(String url) {
        try { return StreamUrl.resolve(url, 3000, local); } catch (Exception e) { return "ERR " + e.getMessage(); }
    }
}
