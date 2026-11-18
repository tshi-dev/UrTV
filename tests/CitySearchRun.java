package sa.urtv.screensaver;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.TimeZone;

/** For tests/cities_more_test.py. Args: the asset, queries.txt (one per line, "CC|" before a name keeps to that country).
 *  Prints per query «Q\tquery\tCC|Arabic|English…» (the first five, best first), «M\tquery\tnames» (CityMatch.find, saver.sh city),
 *  then one «P\t…» line per place (id, country, zone, the zone Java knows, lat, lon, built-in), and «N\tcount». */
public final class CitySearchRun {
    public static void main(String[] args) throws IOException {
        final String asset = args[0];
        CitiesMore.opener = () -> new FileInputStream(asset);
        StringBuilder out = new StringBuilder();
        for (String q : Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8)) {
            String cc = "", name = q;
            if (q.length() > 3 && q.charAt(2) == '|') { cc = q.substring(0, 2); name = q.substring(3); }
            StringBuilder b = new StringBuilder();
            for (CitiesMore.Place p : CitySearch.find(name, cc, 5)) b.append(b.length() > 0 ? "\t" : "").append(p.row[5]).append('|').append(p.row[0]).append('|').append(p.row[4]);
            out.append("Q\t").append(q).append('\t').append(b).append('\n');
            StringBuilder m = new StringBuilder();
            List<String[]> hits = CityMatch.find(q);
            for (String[] h : hits) m.append(m.length() > 0 ? "\t" : "").append(h[0]).append('|').append(CityMatch.zone(h));
            out.append("M\t").append(q).append('\t').append(m).append('\n');
        }
        for (CitiesMore.Place p : CitiesMore.all()) {
            String tz = p.row[3];
            out.append("P\t").append(p.id).append('\t').append(p.row[5]).append('\t').append(tz).append('\t').append(TimeZone.getTimeZone(tz).getID())
               .append('\t').append(p.row[1]).append('\t').append(p.row[2]).append('\t').append(p.builtin).append('\t')
               .append(p.builtin ? String.valueOf(Cities.find(p.row[0], p.row[1], p.row[2]) != null) : "-").append('\n');
        }
        out.append("N\t").append(CitiesMore.all().size()).append('\n');
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
