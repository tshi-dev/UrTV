package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** For tests/apps_edit_test.py: AppsEdit (the «تعديل التطبيقات» state machine), the order the grid shows, kids mode, the PIN gate and the
 *  adhan rule over its keys, on tests/apps_edit_cases.tsv. One answer line per case line; a "new" line starts a fresh grid. Columns are the
 *  command and its arguments, then the expected answer and a note (the test reads those two from the end).
 *  new COLS FAVOURITES MAX FOCUS APPS HIDDEN | ok|back|undo|up|down NOW | move NOW DELTA | key NOW CODE RTL | show NOW PKG PLACE (-1 the end) | expire NOW |
 *  saved (what the screen saves) | arrange BYNAME SAVED | allowed KIDS | gate PINSET LOCKED OPENED | adhanreset | adhan CODE ACTION REPEAT
 *  A list "-" is empty. An AppsEdit answer is RESULT|apps|at|flags|hidden|to favourites: flags c carrying, x on a ×, u on «تراجع», h on
 *  «المخفية», "-" none. */
public final class AppsEditRun {
    static final String[] NAMES = {"NOTHING", "FOCUS", "PICKED", "MOVED", "DROPPED", "CANCELLED", "HIDDEN", "UNDONE", "EXIT",
        "TO_FAVOURITES", "FAVOURITES_FULL", "LIST", "SHOWN"};

    static List<String> list(String s) { return "-".equals(s) || s.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(s.split(","))); }
    static String str(List<String> l) { return l.isEmpty() ? "-" : String.join(",", l); }

    static String state(int result, AppsEdit e) {
        String flags = (e.carrying() ? "c" : "") + (e.onX() ? "x" : "") + (e.onUndo() ? "u" : "") + (e.onHiddenList() ? "h" : "");
        return NAMES[result] + "|" + str(e.order()) + "|" + e.at() + "|" + (flags.isEmpty() ? "-" : flags) + "|" + str(e.hidden()) + "|" + str(e.toFavourites());
    }

    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        AppsEdit e = null;
        AdhanKeys keys = new AdhanKeys();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "new":
                    e = new AppsEdit(list(f[5]), list(f[6]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]));
                    out.append("-\n");
                    break;
                case "ok": out.append(state(e.ok(Long.parseLong(f[1])), e)).append('\n'); break;
                case "back": out.append(state(e.back(Long.parseLong(f[1])), e)).append('\n'); break;
                case "undo": out.append(state(e.undo(Long.parseLong(f[1])), e)).append('\n'); break;
                case "up": out.append(state(e.up(Long.parseLong(f[1])), e)).append('\n'); break;
                case "down": out.append(state(e.down(Long.parseLong(f[1])), e)).append('\n'); break;
                case "move": out.append(state(e.move(Integer.parseInt(f[2]), Long.parseLong(f[1])), e)).append('\n'); break;
                case "key": out.append(state(e.move(FavEdit.step(Integer.parseInt(f[2]), "1".equals(f[3])), Long.parseLong(f[1])), e)).append('\n'); break;
                case "show": out.append(state(e.show(f[2], Integer.parseInt(f[3]), Long.parseLong(f[1])), e)).append('\n'); break;
                case "expire": out.append(e.expire(Long.parseLong(f[1])) ? "expired" : "kept").append('|').append(e.undoAvailable(Long.parseLong(f[1]))).append('\n'); break;
                case "saved":   // what the screen saves when the mode ends: changed, the order (only when moved), hidden now, shown again, to the favourites
                    out.append(e.changed()).append('|').append(e.moved() ? str(e.order()) : "keep").append('|').append(str(e.hiddenNow()))
                       .append('|').append(str(e.shownNow())).append('|').append(str(e.toFavourites())).append('\n');
                    break;
                case "topup": out.append(str(AppsEdit.topUp(list(f[1]), list(f[2]), Integer.parseInt(f[3])))).append('\n'); break;   // CHOSEN REST MIN
                case "arrange": out.append(str(AppsEdit.arrange(list(f[1]), "-".equals(f[2]) ? null : f[2]))).append('\n'); break;
                case "allowed": out.append(AppsEdit.allowed("1".equals(f[1])) ? "edit" : "no").append('\n'); break;
                case "gate": {   // entering the mode asks the settings' question: ParentalPolicy.MENU_ARRANGE, the «قفل الإعدادات» target, as «تعديل المفضلة»
                    ParentalPolicy.State s = ParentalPolicy.stateFrom("1".equals(f[1]), false, f[2], "");
                    ParentalPolicy.Session session = new ParentalPolicy.Session();
                    if ("1".equals(f[3])) session.unlock(ParentalPolicy.targets(ParentalPolicy.MENU_ARRANGE, null));
                    out.append(ParentalPolicy.needsPin(s, session, ParentalPolicy.targets(ParentalPolicy.MENU_ARRANGE, null)) ? "ASK" : "GO").append('\n');
                    break;
                }
                case "adhanreset": keys = new AdhanKeys(); out.append("-\n"); break;
                case "adhan": {   // adhan playing, «إيقاف الأذان بأي زر» on, no bar: what the home screen's first question says to a key
                    int r = keys.decide(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]), true, false, false, true);
                    out.append(r == AdhanKeys.STOP ? "STOP" : r == AdhanKeys.EAT ? "EAT" : "PASS").append('\n');
                    break;
                }
                default: out.append("?\n");
            }
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
