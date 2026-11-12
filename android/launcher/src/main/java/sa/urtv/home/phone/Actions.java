package sa.urtv.home.phone;

import java.util.List;
import java.util.Map;

/**
 * What the phone may do on the TV, implemented by PhoneActions (Android) and by a stand-in in the tests. Everything arrives already checked
 * by Schema. A method answers with a Result: ok with a short word the page explains, or a fixed failure code; never text from the request.
 */
public interface Actions {
    final class Result {
        public final boolean ok;
        public final String code;
        private Result(boolean ok, String code) { this.ok = ok; this.code = code; }
        public static Result ok(String code) { return new Result(true, code); }
        public static Result fail(String code) { return new Result(false, code); }
    }

    /** A remote key (Schema.KEYS). */
    Result key(String key);

    /**
     * Typed text. On our own screens: "typed" (into a field), "task" (added to «المهام»), "copied" (no field was open: put on the TV's clipboard).
     * With another app in front (the accessibility service types into its field, mode "append" or "replace", and search presses the keyboard's
     * action): "typed_other", "typed_other_nosearch", "copied" (the app has no text field), or a failure: service_off, locked, password_field, failed.
     */
    Result text(String text, String mode, boolean search);

    /** The text as a new task with a reminder at `due` (epoch ms, > 0): «task», or a failure. An implementation that knows no reminders refuses. */
    default Result text(String text, String mode, boolean search, long due) { return text(text, mode, search); }

    /** What the page shows: settings and player state. Nothing secret. */
    Map<String, Object> state();

    /** The settings of Schema "set", through the same code as the TV's own settings. */
    Result set(Map<String, Object> values);

    /** The Quran player: Schema "quran". */
    Result quran(Map<String, Object> values);

    /** Built-in city names that match the words, at most twenty. */
    List<String> cities(String query);

    /** The favourites in the TV's order and the pills: {"apps": [{pkg, name, locked}], "tools": [{id, name, locked}]}. */
    Map<String, Object> apps();

    /** A favourite's icon as a PNG, or null for anything that is not one of them. */
    byte[] icon(String pkg);

    /** The glass look's small blurred backdrop of the home's photo (a PNG, 240x135), or null while there is none. */
    byte[] backdrop();

    /** Opens an app or a pill with the home screen's own parental check: ok with "opened" or "pin_on_tv" (the TV asks for the PIN), else a fixed failure. */
    Result launch(String target);

    /** «أوقف الأذان الآن»: ok with "done" (it was playing) or "nothing". */
    Result adhanStop();

    /** One of Schema.DO_OPS: "screensaver" (the home's «شاشة التوقف» pill, locked as it is) or "sleep_cancel". */
    Result doOp(String op);

    /** The family lock, read only: pin_set, kids, locked. Never the PIN. */
    Map<String, Object> family();

    /** «تحديث تلفزيونك»: op check or install; a map with a status word the page explains. */
    Map<String, Object> update(String op);

    /**
     * «محفظتي» for the phone's portfolio page (sa.urtv.home.PortfolioPhone.read): the holdings, the names and codes the search offers; null
     * when the TV is not editing them (the tab open on the TV, or «أسهل من الجوال», within its window), and the API then refuses.
     */
    default Map<String, Object> portfolio() { return null; }

    /** One holding's buys ("qty:price" each; none removes it), only while the TV is editing: ok "saved", else a fixed failure. */
    default Result portfolioSave(String code, List<String> buys) { return Result.fail("portfolio_closed"); }

    /** 4.3 #53: a short message from the phone, shown on the TV for 20 s (PhoneMessage). */
    default Result message(String text) { return Result.fail("message_off"); }

    /** 4.3 «ارسم»: a checked batch of strokes over the TV's screen (PhoneDraw), or clear = «امسح». */
    default Result draw(java.util.List<?> strokes, boolean clear) { return Result.fail("message_off"); }

    /** the wireless-debugging pairing's six digits from the phone page (only while the TV's factory page armed it): ok "ok", else "not_armed", "busy" or "bad_code". */
    default Result adbPair(String code) { return Result.fail("not_armed"); }

    /** 4.3: one photo for «صوري», a JPEG the phone re-encoded (no camera or location data); the screensaver keeps it (Own). */
    default Result photoAdd(byte[] jpeg) { return Result.fail("failed"); }
}
