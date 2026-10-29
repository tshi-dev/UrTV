package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import sa.urtv.home.tilawah.DayParts;

/**
 * The greeting above the home screen's clock («صباح الخير» from Fajr to Dhuhr, «طاب يومك» to Asr, «مساء الخير» to Isha,
 * «تصبح على خير» after) and under it the weather of the screensaver's city and the next sunrise or sunset.
 *
 * Settings come from the screensaver's ConfigProvider (city, digits and «التحية والطقس»). The weather is asked of
 * Open-Meteo at most once every 30 minutes (the current temperature and weather code, and the air's dust as the screensaver's page asks it: two requests), and only while the greeting is on; the last answer is kept, so a TV that
 * is offline shows the greeting alone. With the setting off the line is the plain good-morning it always was.
 */
final class Greeting {
    static final long REFRESH_MS = 30 * 60 * 1000L;
    /** an answer older than this is not shown: «٣١°» from yesterday is worse than nothing */
    private static final long STALE_MS = 3 * 60 * 60 * 1000L;

    private final Context context;
    private final TextView greeting, weather;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean fetching;

    private final java.util.function.BooleanSupplier inFront;   // the home screen is resumed (NetGate)

    Greeting(Context context, TextView greeting, TextView weather, java.util.function.BooleanSupplier inFront) {
        this.inFront = inFront;
        this.context = context;
        this.greeting = greeting;
        this.weather = weather;
    }

    /** Called with the clock (every minute, and on resume). hour: the TV's hour, for the plain greeting. */
    void update(int hour, ConfigSnapshot snap) {
        Config s = Config.of(snap);
        if (!s.on) {
            greeting.setText(hour < 4 ? "تصبح على خير" : hour < 12 ? "صباح الخير" : "مساء الخير");
            weather.setVisibility(android.view.View.GONE);
            return;
        }
        DayParts d = null;
        try { d = DayParts.now(context, snap.placeFragment()); } catch (RuntimeException ignored) { }
        greeting.setText(d == null ? (hour < 4 ? "تصبح على خير" : hour < 12 ? "صباح الخير" : "مساء الخير")
                : GreetingText.greeting(GreetingText.part(System.currentTimeMillis(), d.fajr, d.dhuhr, d.asr, d.isha)));

        SharedPreferences sp = prefs();
        long at = sp.getLong("at", 0);
        boolean sameCity = s.lat.equals(sp.getString("lat", "")) && s.lon.equals(sp.getString("lon", ""));
        if (sameCity && sp.contains("code") && System.currentTimeMillis() - at < STALE_MS && d != null) {
            String line = GreetingText.weather(sp.getInt("code", -1), GreetingText.seenDust(sp.getFloat("dust", 0), sp.getFloat("vis", -1)), s.city, sp.getFloat("temp", 0), s.latin)
                    + "\n" + GreetingText.sun(d.sunLabel, d.sunClock, s.latin, s.clock24);
            weather.setText(line);
            weather.setVisibility(android.view.View.VISIBLE);
        } else {
            weather.setVisibility(android.view.View.GONE);   // offline, or not asked yet: the greeting alone
        }
        // once every 30 minutes; a new city is asked at once, but only once (a failed answer is not asked again at once)
        boolean triedHere = s.lat.equals(sp.getString("triedLat", "")) && s.lon.equals(sp.getString("triedLon", ""));
        // only with the home in front and the display on (NetGate); the next minute tick or the return to the front asks again
        long now = System.currentTimeMillis();
        boolean front = inFront.getAsBoolean(), on = NetGateAndroid.displayOn(context);
        if (!fetching && (triedHere ? NetGate.due(front, on, sp.getLong("tried", 0), REFRESH_MS, now) : NetGate.allowed(front, on))) fetch(s);
    }

    private void fetch(final Config s) {
        fetching = true;
        // counted when asked, not when answered: a TV with no network does not ask again before 30 minutes
        prefs().edit().putLong("tried", System.currentTimeMillis()).putString("triedLat", s.lat).putString("triedLon", s.lon).apply();
        new Thread(() -> {
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + s.lat + "&longitude=" + s.lon
                        + "&timezone=" + URLEncoder.encode(s.tz, "UTF-8") + "&current=temperature_2m,weather_code,visibility";
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(6000);
                con.setReadTimeout(8000);
                con.setRequestProperty("User-Agent", "urtv-home");
                StringBuilder body = new StringBuilder();
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) body.append(line);
                } finally {
                    con.disconnect();
                }
                JSONObject cur = new JSONObject(body.toString()).getJSONObject("current");
                SharedPreferences sp = prefs();
                float dust = dust(s);
                long now = System.currentTimeMillis();
                long dustAt = now;
                if (dust < 0) {   // the air-quality request failed: the last good reading of this place stands, not a clear sky
                    boolean sameCity = s.lat.equals(sp.getString("lat", "")) && s.lon.equals(sp.getString("lon", ""));
                    dustAt = sp.getLong("dustAt", 0);
                    dust = sameCity && now - dustAt < STALE_MS ? sp.getFloat("dust", 0) : 0;
                }
                prefs().edit().putLong("dustAt", dustAt).putLong("at", System.currentTimeMillis())
                        .putString("lat", s.lat).putString("lon", s.lon)
                        .putFloat("temp", (float) cur.getDouble("temperature_2m"))
                        .putFloat("dust", dust)
                        .putFloat("vis", (float) cur.optDouble("visibility", -1))
                        .putInt("code", cur.getInt("weather_code")).apply();
                main.post(() -> { fetching = false; update(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY), ConfigSnapshot.read(context)); });
            } catch (Exception e) {
                main.post(() -> fetching = false);   // offline or Open-Meteo is down: the last answer, if fresh, stays
            }
        }, "greeting-weather").start();
    }

    /** The larger of the air's pm10 and dust (µg/m³), as the page reads them to say «غبار»; -1 when the request fails. */
    private static float dust(Config s) {
        try {
            String url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + s.lat + "&longitude=" + s.lon
                    + "&timezone=" + URLEncoder.encode(s.tz, "UTF-8") + "&current=pm10,dust";
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(6000);
            con.setReadTimeout(8000);
            con.setRequestProperty("User-Agent", "urtv-home");
            StringBuilder body = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) body.append(line);
            } finally {
                con.disconnect();
            }
            JSONObject cur = new JSONObject(body.toString()).getJSONObject("current");
            return (float) Math.max(cur.optDouble("pm10", 0), cur.optDouble("dust", 0));
        } catch (Exception e) {
            return -1;   // no answer: the caller keeps the last good reading
        }
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences("greeting", Context.MODE_PRIVATE);
    }

    /** The screensaver's settings this line needs. */
    private static final class Config {
        boolean on = true;   // an older screensaver does not say: on, the default
        String city = "الرياض", lat = "24.6877", lon = "46.7219", tz = "Asia/Riyadh";
        boolean latin, clock24;

        static Config of(ConfigSnapshot snap) {
            Config s = new Config();
            s.on = snap.greetingOn;
            try {
                String frag = snap.fragment;
                if (frag == null) return s;
                for (String part : frag.split("&")) {
                    int eq = part.indexOf('=');
                    if (eq < 0) continue;
                    String key = part.substring(0, eq), value = URLDecoder.decode(part.substring(eq + 1), "UTF-8");
                    switch (key) {
                        case "city": if (!value.trim().isEmpty()) s.city = value.trim(); break;
                        case "lat": if (value.matches("-?\\d+(\\.\\d+)?")) s.lat = value; break;
                        case "lon": if (value.matches("-?\\d+(\\.\\d+)?")) s.lon = value; break;
                        case "tz": if (!value.trim().isEmpty()) s.tz = value.trim(); break;
                        case "digits": s.latin = "latin".equals(value.toLowerCase(Locale.ROOT)); break;
                        case "clock": s.clock24 = ClockText.isOn(value); break;
                        default: break;
                    }
                }
            } catch (Exception e) {
                // the screensaver is missing or signed with another key: the defaults (Riyadh, on)
            }
            return s;
        }
    }
}
