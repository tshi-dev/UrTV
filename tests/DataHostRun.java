package sa.urtv.screensaver;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

/** tests/data_host_test.py: DataHost.urls on the map's datasets, and get() asking the new address first, the old on a 404 */
public class DataHostRun {
    public static void main(String[] a) throws Exception {
        for (String u : new String[] {
                "https://raw.githubusercontent.com/tshi-dev/UrTV/data-leagues/leagues/saudi-arabian-pro-league.json",
                "https://raw.githubusercontent.com/tshi-dev/UrTV/data-catalogue/catalogue/catalogue.json",
                "https://raw.githubusercontent.com/tshi-dev/UrTV/data-archive/h2h/1-2.json",
                "https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/quiz.json",
                "https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/games/odd-one.json",
                "https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/markets.json",
                "https://raw.githubusercontent.com/tshi-dev/UrTV/data-stations/stations/stations.json",
                "https://raw.githubusercontent.com/tshi-dev/UrTV/master/photos/x.jpg",
                "https://example.com/data-games/games/x.json"})
            System.out.println("URLS " + String.join(" ", DataHost.urls(u)));
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        StringBuilder asked = new StringBuilder();
        s.createContext("/", x -> {
            String p = x.getRequestURI().getPath();
            asked.append(p).append(' ');
            byte[] b = ("got " + p).getBytes("UTF-8");
            x.sendResponseHeaders(p.startsWith("/new/") && p.contains("missing") ? 404 : 200, p.contains("missing") && p.startsWith("/new/") ? -1 : b.length);
            if (!(p.startsWith("/new/") && p.contains("missing"))) x.getResponseBody().write(b);
            x.close();
        });
        s.start();
        String base = "http://127.0.0.1:" + s.getAddress().getPort();
        System.out.println("NEWFIRST " + DataHost.getWith(base + "/old/", base + "/new/", base + "/old/data-markets/markets/markets.json", 3000, 5000, 1 << 20).text());
        System.out.println("FALLBACK " + DataHost.getWith(base + "/old/", base + "/new/", base + "/old/data-games/games/missing.json", 3000, 5000, 1 << 20).text());
        System.out.println("ASKED " + asked);
        s.stop(0);
    }
}
