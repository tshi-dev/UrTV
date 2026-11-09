package sa.urtv.screensaver;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Hands the launcher (same signing key, see READ_CONFIG in the manifest) what its home-screen card needs:
 * the page settings as a URL fragment, which card to show ("none" = off) and whether the greeting is on ("1"/"0").
 *
 * «المهام»: content://sa.urtv.screensaver.config/tasks lists the tasks (i from 1, t, done, at). The launcher
 * may also change them, which needs WRITE_TASKS (signature, same key as READ_CONFIG):
 *   insert  ContentValues t = the text               -> .../tasks/N, or null when refused (empty, full)
 *           with due (epoch ms), bf (minutes before: 0 10 30 60), rp (repeat 0 no 1 daily 2 weekly), snd (chime 0/1), wh (0 both, 1 screensaver, 2 home): a reminder
 *   update  ContentValues id = ID (or i = N)         -> 1 when that task is marked done (a repeating reminder moves to its next time)
 *           id with due (0 = none) bf rp snd wh      -> 1 when that task's reminder is set;  id with snooze = minutes -> 1 when its card is asked again then
 *   query   also due, bf, rp, snd, wh, sn (the snooze time) after id; an older launcher reads the first five
 *   delete  .../tasks/id/ID (or .../tasks/N)         -> 1 when that task is gone
 * N is the place in the list, which moves when the list does; id is the task's own and stays (the screens use it, `list` shows N).
 *
 * «صوري»: the owner's own photos (Own). query .../own gives n (how many), mode (mix|only) and rev (changes whenever
 * the photos do); openFile .../own/N opens photo N (1..n) read-only, which READ_CONFIG allows the launcher. Nothing
 * else can be read or written there. .../own-in takes the next photo from the shell (`content write`), which the
 * manifest lets in with DUMP, as for SaverReceiver; SaverReceiver PHOTOS then checks and keeps it.
 *
 * 4.3: query .../homepacks gives the packs the home's wallpaper rotates (id, n, ours, rev; id "" and n 0 when none); openFile .../homepack/ID/N opens photo N of one of them, read only.
 *
 * .../dreaming: one row (dreaming 1|0), whether the dream or its preview is on the screen (the home's «الأذان على شاشة التوقف فقط»).
 *
 * «تحكم من الجوال» (the launcher's phone control) uses call(), and only with PHONE_SETTINGS (signature, same key):
 *   phone_state                         -> city, home_cards (list), panels (codes) and panel_names (Arabic, same order), idle (minutes), lang (ar|en)
 *   phone_set  extras city, home_panel, idle, lang -> ok (and idle_denied when the TV does not let this app change the idle time), the same checks as SaverReceiver SET (it is that code)
 *              also stocks and markets: the home's «الأسواق» page («أضف إلى قائمتي», «أظهره في شريط الأسواق»); the phone's API never sends them
 *   phone_cities  arg = words           -> cities: the built-in names that match (at most 20)
 * Nothing else can be set this way.
 */
public class ConfigProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (uri != null && "dreaming".equals(uri.getLastPathSegment())) {   // the home's «الأذان على شاشة التوقف فقط»: is the dream (or its preview) on the screen now
            MatrixCursor d = new MatrixCursor(new String[] {"dreaming"});
            d.addRow(new Object[] {Showing.now() ? 1 : 0});
            return d;
        }
        Prefs p = new Prefs(getContext());
        if (uri != null && uri.getPathSegments().size() == 1 && "homepacks".equals(uri.getPathSegments().get(0))) {   // the home's wallpaper: which packs (id, photos), whether ours is in, and a revision that moves with the set
            MatrixCursor h = new MatrixCursor(new String[] {"id", "n", "ours", "rev"});
            java.util.Map<String, Integer> set = p.homePacks();
            String rev = String.valueOf(p.homePacksRev().hashCode());
            if (set.isEmpty()) h.addRow(new Object[] {"", 0, p.homeOurs() ? 1 : 0, rev});
            for (java.util.Map.Entry<String, Integer> e : set.entrySet()) h.addRow(new Object[] {e.getKey(), e.getValue(), p.homeOurs() ? 1 : 0, rev});
            return h;
        }
        if (isOwn(uri)) {
            MatrixCursor o = new MatrixCursor(new String[] {"n", "mode", "rev"});
            o.addRow(new Object[] {Own.count(getContext()), Own.mode(getContext()), p.ownRev()});
            return o;
        }
        if (isTasks(uri)) {
            MatrixCursor t = new MatrixCursor(new String[] {"i", "t", "done", "at", "id", "due", "bf", "rp", "snd", "wh", "sn", "tz"});
            List<Task> all = p.tasks();
            String tz = p.cityTz();   // the zone the repeats count whole days in (the home's alarm plans the next one with it)
            for (int k = 0; k < all.size(); k++) {
                Task x = all.get(k);
                t.addRow(new Object[] {k + 1, x.text, x.done ? 1 : 0, x.at, x.id, x.due, x.before, x.repeat, x.sound ? 1 : 0, x.where, x.snooze, tz});
            }
            return t;
        }
        // home_panel: the first card alone (what an older launcher reads); home_panels: all of them, "a,b,c" or "none"
        List<String> cards = p.homePanels();
        MatrixCursor c = new MatrixCursor(new String[] {"fragment", "home_panel", "greeting", "home_panels", "adhan_bar", "listen_pill", "update_channel", "appearance", "update_bell", "watch_next", "look", "home_layout", "fav_bar", "pages_dark", "pills_hidden"});
        c.addRow(new Object[] {p.fragment(), cards.isEmpty() ? "none" : cards.get(0), p.greeting() ? "1" : "0", p.homePanel(), p.adhanBar() ? "1" : "0", p.listenPill() ? "1" : "0", p.updateChannel(), p.appearance(), p.updateBell() ? "1" : "0", p.watchNext() ? "1" : "0", p.look(), p.homeLayout(), p.favBar(), p.pagesDark() ? "1" : "0", p.pillsHidden()});
        return c;
    }

    @Override public String getType(Uri uri) { return null; }

    static final String PHONE_PERMISSION = "sa.urtv.screensaver.PHONE_SETTINGS";

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method == null || !method.startsWith("phone_")) return super.call(method, arg, extras);
        Context c = getContext();
        if (c.checkCallingPermission(PHONE_PERMISSION) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("phone control only");
        Prefs p = new Prefs(c);
        Bundle out = new Bundle();
        switch (method) {
            case "phone_state": {
                ArrayList<String> codes = new ArrayList<>(), names = new ArrayList<>();
                for (String[] x : Prefs.PANELS) { codes.add(x[0]); names.add(x[1]); }
                out.putString("city", p.city()[0]);
                out.putStringArrayList("home_cards", new ArrayList<>(p.homePanels()));
                out.putStringArrayList("panels", codes);
                out.putStringArrayList("panel_names", names);
                out.putString("idle", String.valueOf(Prefs.idleMs(c) / 60000));
                out.putString("idle_s", String.valueOf(Prefs.idleMs(c) / 1000));   // the real delay: 45 s is not a whole minute ("idle" says 0)
                out.putString("lang", p.lang());   // «اللغة»: an older screensaver says nothing, and the phone takes that as ar
                out.putString("fav_bar", p.favBar());   // «تطبيقات في الشريط»
                return out;
            }
            case "phone_set": {
                android.content.Intent i = new android.content.Intent();
                for (String k : new String[] {"city", "home_panel", "idle", "lang", "favbar"}) if (extras != null && extras.getString(k) != null) i.putExtra(k, extras.getString(k));
                // the home's «الأسواق» page: the watchlist and the markets bar (the phone's API never sends these two)
                for (String k : new String[] {"stocks", "markets"}) if (extras != null && extras.getString(k) != null) i.putExtra(k, extras.getString(k));
                String r = SaverReceiver.set(c, p, i);
                out.putBoolean("ok", r.startsWith("saved"));
                out.putBoolean("idle_denied", r.startsWith("error: idle not allowed"));
                return out;
            }
            case "phone_follow": {   // «تابع» on «مباريات اليوم» (the launcher's MatchesActivity): arg "id:EnglishName", as saver.sh teams takes it
                String[] kv = arg == null ? new String[0] : arg.split(":", 2);
                boolean ok = kv.length == 2 && kv[0].matches("\\d{1,9}");
                if (ok) {
                    TeamSearch.Team known = Teams.byId(c, kv[0]);
                    String name = known != null && !known.ar.isEmpty() ? known.ar : kv[1].trim().isEmpty() ? "#" + kv[0] : Arabic.team(c, kv[1].trim());
                    ok = p.addTeam(kv[0], name);
                }
                out.putBoolean("ok", ok);
                return out;
            }
            case "phone_unfollow": {   // «أزل … من فرقي» on a game's card: arg is the team's id
                out.putBoolean("ok", arg != null && arg.matches("\\d{1,9}") && p.removeTeam(arg));
                return out;
            }
            case "phone_cities": {
                ArrayList<String> names = new ArrayList<>();
                for (String[] city : CityMatch.find(arg == null ? "" : arg)) if (names.size() < 20) names.add(city[0]);
                out.putStringArrayList("cities", names);
                return out;
            }
            default:
                return super.call(method, arg, extras);
        }
    }

    private static boolean isOwn(Uri uri) {
        return uri != null && uri.getPathSegments().size() == 1 && "own".equals(uri.getPathSegments().get(0));
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Context c = getContext();
        java.util.List<String> seg = uri.getPathSegments();
        if (seg.size() == 2 && "own".equals(seg.get(0)) && "r".equals(mode)) {
            int n;
            try { n = Integer.parseInt(seg.get(1)); } catch (NumberFormatException e) { throw new FileNotFoundException(); }
            if (n < 1 || n > Own.count(c)) throw new FileNotFoundException();
            return ParcelFileDescriptor.open(Own.file(c, n), ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if (seg.size() == 3 && "homepack".equals(seg.get(0)) && "r".equals(mode)) {   // .../homepack/ID/N: photo N of a pack the home rotates, read only
            String id = seg.get(1);
            int n;
            try { n = Integer.parseInt(seg.get(2)); } catch (NumberFormatException e) { throw new FileNotFoundException(); }
            Integer count = new Prefs(c).homePacks().get(id);
            if (count == null || n < 1 || n > count) throw new FileNotFoundException();
            File f = PackStore.photo(c, id, n);
            if (!f.isFile()) throw new FileNotFoundException();
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if (seg.size() == 2 && "page".equals(seg.get(0)) && "r".equals(mode)) {   // the home card's page (the launcher's HomePage)
            File f = sharedPage(c, seg.get(1));
            if (f == null) throw new FileNotFoundException();
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if (seg.size() == 1 && "own-in".equals(seg.get(0)) && mode.contains("w") && !mode.contains("a")) {
            File dir = Own.dir(c);
            if (!dir.isDirectory() && !dir.mkdirs()) throw new FileNotFoundException();
            return ParcelFileDescriptor.open(Own.incoming(c), ParcelFileDescriptor.MODE_WRITE_ONLY
                | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        throw new FileNotFoundException();
    }

    /**
     * .../page/tv.html (or tv-en.html): the page this app downloaded (Updater), only when it is newer than the one in this APK and the
     * owner left the automatic update on: the launcher's own copy never gets the page's fixes otherwise (its stocks card stayed stale
     * while this one was fresh). null: nothing newer to share, the launcher keeps its own.
     */
    static File sharedPage(Context c, String name) {
        String lang = "tv-en.html".equals(name) ? Tr.EN : "tv.html".equals(name) ? Tr.AR : null;
        if (lang == null || !new Prefs(c).autoUpdate()) return null;
        File f = Updater.pageFile(c, lang);
        if (!f.isFile() || !PageStamp.newer(Updater.fileStamp(f), Updater.assetStamp(c, lang))) return null;
        return f;
    }

    private static boolean isTasks(Uri uri) {
        return uri != null && uri.getPathSegments().size() >= 1 && "tasks".equals(uri.getPathSegments().get(0));
    }

    private static int intOf(ContentValues v, String k) {
        Integer n = v.getAsInteger(k);
        return n == null ? 0 : n;
    }

    /** a screen that is up (the dream, the home) starts over with the reminders as they are now */
    private void changed() {
        getContext().sendBroadcast(new Intent(Prefs.CHANGED).setPackage(getContext().getPackageName()));
    }

    @Override
    public Uri insert(Uri uri, ContentValues v) {
        if (!isTasks(uri) || v == null) return null;
        Prefs p = new Prefs(getContext());
        Long due = v.getAsLong("due");
        if (due != null && due > 0) {   // with a reminder: the time, «قبلها بـ» minutes, repeat 0/1/2, chime 0/1, where 0/1/2
            if (p.addReminder(v.getAsString("t"), due, intOf(v, "bf"), intOf(v, "rp"), intOf(v, "snd") == 1, intOf(v, "wh")) != null) return null;
            changed();
        } else if (p.addTask(v.getAsString("t")) != null) return null;
        return Uri.withAppendedPath(uri, String.valueOf(p.tasks().size()));
    }

    @Override
    public int delete(Uri uri, String s, String[] a) {
        if (!isTasks(uri) || uri.getPathSegments().size() < 2) return 0;
        List<String> seg = uri.getPathSegments();
        try {
            Prefs p = new Prefs(getContext());
            if (seg.size() == 3 && "id".equals(seg.get(1))) return p.deleteTaskId(Long.parseLong(seg.get(2))) ? 1 : 0;
            return p.deleteTask(Integer.parseInt(uri.getLastPathSegment())) ? 1 : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues v, String s, String[] a) {
        if (!isTasks(uri) || v == null) return 0;
        Prefs p = new Prefs(getContext());
        Long id = v.getAsLong("id");
        if (id != null && v.getAsLong("due") != null) {   // «ذكّرني» of one task: the reminder is set (due 0 takes it off)
            boolean ok = p.remindTaskId(id, v.getAsLong("due"), intOf(v, "bf"), intOf(v, "rp"), intOf(v, "snd") == 1, intOf(v, "wh"));
            if (ok) changed();
            return ok ? 1 : 0;
        }
        if (id != null && v.getAsInteger("snooze") != null) {   // «ذكّرني بعد ١٠ دقائق»
            boolean ok = p.snoozeTaskId(id, v.getAsInteger("snooze"));
            if (ok) changed();
            return ok ? 1 : 0;
        }
        if (id != null) {
            boolean ok = p.doneTaskId(id);
            if (ok) changed();
            return ok ? 1 : 0;
        }
        if (v.getAsInteger("i") == null) return 0;
        return p.doneTask(v.getAsInteger("i")) ? 1 : 0;
    }
}
