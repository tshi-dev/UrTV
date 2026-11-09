package sa.urtv.home.tilawah;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

import sa.urtv.home.OnOff;

import sa.urtv.home.R;
import java.util.ArrayList;
import java.util.List;

/**
 * The «تلاوة» screen: three sections that never share a page. The tab bar switches between the
 * reciters, the live stations and the adhan (today's times, the switch and the muezzin).
 */
public class MainActivity extends Activity {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(sa.urtv.home.S.wrap(b)); }


    /** opens the screen on its «الأذان» tab: the adhan's settings from the home's app menu and from the screensaver's settings (value "adhan") */
    public static final String EXTRA_OPEN = "open";
    private static final int TAB_RECITERS = 0, TAB_STATIONS = 1, TAB_ADHAN = 2;

    private static final int KIND_RESUME = 0, KIND_RECITER = 1, KIND_STATION = 2,
            KIND_PRAYER = 3, KIND_ADHAN_TOGGLE = 4, KIND_VOICE = 5, KIND_VOLUME = 6, KIND_EXACT = 7, KIND_FAJR_VOICE = 8, KIND_SHORT = 9, KIND_STOPKEY = 10, KIND_SAVER_ONLY = 11, KIND_NOTICE = 12, KIND_TAKBIR = 13, KIND_SAVER_AT = 14, KIND_LINK = 15;

    private Catalog cat;
    private Prefs prefs;
    private ListView list;
    private ViewGroup tabs;
    private TextView[] tabViews;
    private int tab = TAB_RECITERS;
    private RowsAdapter adapter;
    private View lastSelected;
    private boolean focusedOnce;

    /** singleTop: the settings' «الأذان» row asked again while this is already open: show the adhan tab. */
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && "adhan".equals(intent.getStringExtra(EXTRA_OPEN))) selectTab(TAB_ADHAN);
    }

    @Override protected void onCreate(Bundle state) {
        setTheme(Ui.theme(this));   // «المظهر»: the look and the light of now, before anything is drawn
        super.onCreate(state);
        sa.urtv.home.Digits.init(this);   // the screensaver's «الأرقام» setting
        setContentView(R.layout.activity_main);
        Ui.localize(this);   // «اللغة»: English reads left to right, the layout's words in English
        Ui.glassPage(this, R.id.bg);   // the glass page
        cat = Catalog.get(this);
        prefs = new Prefs(this);

        Ui.text((TextView) findViewById(R.id.title), "ThmanyahDisplay-Bold.otf", 44, R.color.sand);
        Ui.text((TextView) findViewById(R.id.tagline), "Thmanyah-Regular.otf", 17, R.color.dim);
        Ui.text((TextView) findViewById(R.id.hint), "Thmanyah-Regular.otf", 14, R.color.dim);

        list = findViewById(R.id.list);
        adapter = new RowsAdapter();
        list.setAdapter(adapter);
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (list.hasFocus() && position >= 0 && position < adapter.rows.size()
                        && !adapter.rows.get(position).header) {
                    highlight(view);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        // one focus ring at a time: the row keeps its gold frame only while the list owns the focus
        list.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (lastSelected != null) resetRow(lastSelected);
                lastSelected = null;
            } else {
                int selected = list.getSelectedItemPosition();
                if (selected >= 0 && selected < adapter.rows.size() && !adapter.rows.get(selected).header) {
                    highlight(list.getSelectedView());
                }
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            Row row = adapter.rows.get(position);
            if (row.kind == KIND_RESUME) {
                MusicService.resumeLast(this);
                openPlayer();
            } else if (row.kind == KIND_RECITER) {
                startActivity(new Intent(this, SurahActivity.class).putExtra("reciter", row.index));
            } else if (row.kind == KIND_STATION) {
                MusicService.playRadio(this, row.index);
                openPlayer();
            } else if (row.kind == KIND_PRAYER) {
                int voice = prefs.playVoiceFor(this, row.index == PrayerTimes.FAJR);   // Fajr has its own muezzin
                MusicService.adhan(this, voice, Adhan.name(voice), 0);
                openPlayer();
            } else if (row.kind == KIND_ADHAN_TOGGLE) {
                prefs.setAdhanOn(!prefs.adhanOn());
                AdhanScheduler.schedule(this);
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
            } else if (row.kind == KIND_VOICE) {
                startActivity(new Intent(this, AdhanActivity.class));
            } else if (row.kind == KIND_FAJR_VOICE) {
                startActivity(new Intent(this, AdhanActivity.class).putExtra(AdhanActivity.EXTRA_FAJR, true));
            } else if (row.kind == KIND_SHORT) {
                prefs.setAdhanShort(!prefs.adhanShort());
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
            } else if (row.kind == KIND_TAKBIR) {
                prefs.setAdhanTakbir(!prefs.adhanTakbir());
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
            } else if (row.kind == KIND_STOPKEY) {
                prefs.setAdhanStopKey(!prefs.adhanStopKey());
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
            } else if (row.kind == KIND_SAVER_ONLY) {
                prefs.setAdhanSaverOnly(!prefs.adhanSaverOnly());
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
            } else if (row.kind == KIND_NOTICE) {
                prefs.setAdhanNotice(!prefs.adhanNotice());
                AdhanScheduler.schedule(this);   // the alarm is wanted for the notice alone, too
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
            } else if (row.kind == KIND_SAVER_AT) {
                prefs.setAdhanSaverAt(!prefs.adhanSaverAt());
                AdhanScheduler.schedule(this);   // the alarm is wanted for the screensaver alone, too
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
            } else if (row.kind == KIND_LINK) {
                openHaramLive(row.index);
            } else if (row.kind == KIND_EXACT) {
                openExactAlarmSettings();
            }   // «صوت الأذان» is set with left/right (changeVolume); OK on it does nothing
        });

        tabs = findViewById(R.id.tabs);
        tabViews = new TextView[]{
                findViewById(R.id.tab_reciters),
                findViewById(R.id.tab_stations),
                findViewById(R.id.tab_adhan)
        };
        for (int i = 0; i < tabViews.length; i++) {
            final int which = i;
            tabViews[i].setTypeface(Ui.font(this, "Thmanyah-Medium.otf"));
            tabViews[i].setTextSize(19);
            tabViews[i].setOnClickListener(v -> {
                selectTab(which);
                focusList(firstSelectable());
            });
        }
        buildLetters();
        selectTab("adhan".equals(getIntent().getStringExtra(EXTRA_OPEN)) ? TAB_ADHAN : TAB_RECITERS);

        // the notifications permission is asked once, by the player when something first plays (NotifyAsk), not each time this screen opens

        // today's times from Aladhan when the network answers, and the recordings kept for offline
        // (refresh also puts the alarm back for the new times)
        PrayerTimes.refresh(this, () -> { adapter.rebuild(); focusList(list.getSelectedItemPosition()); });
        Adhan.prefetchAll(this);
        AdhanScheduler.schedule(this);
    }

    @Override protected void onResume() {
        super.onResume();
        adapter.rebuild();
        int target;
        if (!focusedOnce) {
            target = firstReciter();
            focusedOnce = true;
        } else {
            target = list.getSelectedItemPosition();
            if (target < 0 || target >= adapter.getCount()) {
                target = Math.min(Math.max(0, list.getFirstVisiblePosition()), adapter.getCount() - 1);
            }
        }
        while (target < adapter.getCount() && adapter.rows.get(target).header) target++;
        if (target >= adapter.getCount()) target = 0;
        focusList(target);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && list != null) {
            focusList(Math.max(0, list.getSelectedItemPosition()));
        }
    }

    /**
     * The broadcaster's live page in its own app or the browser (ACTION_VIEW): nothing plays here. A TV with nothing to open it says so; the
     * player and the radio are not touched.
     */
    /** HaramLive.LIVE's titles, marked for the table (HaramLive is plain Java; the rows say them with Ui.line) */
    static final String[] LIVE_WORDS = {sa.urtv.home.S.n("مكة المكرمة · قناة القرآن الكريم"), sa.urtv.home.S.n("المدينة المنورة · قناة السنة النبوية")};

    /** One live channel (HaramLive.LIVE[which]): in the YouTube app, else SmartTube, else the browser (HaramLive.openOrder). */
    private void openHaramLive(int which) {
        if (which < 0 || which >= HaramLive.LIVE.length || !HaramLive.shown(HaramLive.LIVE[which][1])) return;
        for (String pkg : HaramLive.openOrder(p -> {
            try { getPackageManager().getPackageInfo(p, 0); return true; } catch (Exception e) { return false; }
        })) {
            Uri link = Uri.parse(HaramLive.linkFor(which, pkg));   // the browser gets SBA's own Aloula page (4.3)
            if (!HaramLive.shown(link.toString())) continue;
            Intent i = new Intent(Intent.ACTION_VIEW, link).addCategory(Intent.CATEGORY_BROWSABLE);
            if (pkg != null) i.setPackage(pkg);
            try {
                startActivity(i);
                return;
            } catch (android.content.ActivityNotFoundException | SecurityException e) {
                // this app does not take the link: the next one
            }
        }
        android.widget.Toast.makeText(this, Ui.str(this, R.string.haram_live_none), android.widget.Toast.LENGTH_LONG).show();
    }

    /** The tab changes what the list holds; the selected row becomes the first one of that section. */
    private void selectTab(int which) {
        tab = Math.max(TAB_RECITERS, Math.min(TAB_ADHAN, which));
        for (int i = 0; i < tabViews.length; i++) tabViews[i].setSelected(i == tab);
        if (adapter != null) adapter.rebuild();
        paintLetters();
        if (list != null) list.setSelection(firstSelectable());
    }

    private int firstSelectable() {
        int i = 0;
        while (i < adapter.getCount() && adapter.rows.get(i).header) i++;
        return Math.min(i, Math.max(0, adapter.getCount() - 1));
    }

    private int firstReciter() {
        for (int i = 0; i < adapter.rows.size(); i++) {
            if (adapter.rows.get(i).kind == KIND_RECITER) return i;
        }
        return firstSelectable();
    }

    private void focusList(int position) {
        list.setSelection(position);
        list.post(() -> {
            list.requestFocus();
            int selected = list.getSelectedItemPosition();
            if (selected >= 0 && selected < adapter.rows.size() && !adapter.rows.get(selected).header) {
                highlight(list.getSelectedView());
            }
        });
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        // long OK on a reciter marks it ★ (or takes it off); the OK that ends the long press opens nothing
        int kc = event.getKeyCode();
        boolean center = kc == KeyEvent.KEYCODE_DPAD_CENTER || kc == KeyEvent.KEYCODE_ENTER || kc == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (center && list != null && list.hasFocus() && tab == TAB_RECITERS) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 1) { longFav = true; toggleFavourite(); return true; }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 1 && longFav) return true;
            if (event.getAction() == KeyEvent.ACTION_UP && longFav) { longFav = false; return true; }
        }
        // the list swallows «up» even on its first row, so the jump back to the tabs happens here
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0
                && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP
                && list != null && list.hasFocus()
                && list.getSelectedItemPosition() <= firstSelectable()) {
            tabViews[tab].requestFocus();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        // on «صوت الأذان» left/right are the volume: left louder, right quieter (the bar fills from the right, like the page); English mirrored
        if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && list.hasFocus()
                && tab == TAB_ADHAN && selectedKind() == KIND_VOLUME) {
            changeVolume(keyCode == sa.urtv.home.Dir.forwardKey() ? 10 : -10);
            return true;
        }
        // the letter index: the key toward the column (the start edge: right in Arabic, left in English) enters it from the list, the other key leaves it
        if (tab == TAB_RECITERS && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            if (lettersHaveFocus()) {
                if (keyCode == sa.urtv.home.Dir.forwardKey()) focusList(list.getSelectedItemPosition());   // back to the list, where the letter put it
                return true;
            }
            if (list.hasFocus() && keyCode == sa.urtv.home.Dir.backKey() && enterLetters()) return true;
        }
        // left/right walk the tab bar from anywhere on the screen: the sections are one key apart
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // RTL: the first tab sits on the right, so right is «previous» (English: left)
            int delta = keyCode == sa.urtv.home.Dir.backKey() ? -1 : 1;
            int next = Math.max(TAB_RECITERS, Math.min(TAB_ADHAN, tab + delta));
            if (next == tab) return true;
            boolean fromTabs = tabs.hasFocus();
            selectTab(next);
            if (fromTabs) tabViews[next].requestFocus();
            else focusList(firstSelectable());
            return true;
        }
        if (tabs.hasFocus()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                focusList(firstSelectable());
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return true;
        }
        // up from the first row lands on the tab bar instead of doing nothing
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && list.hasFocus()
                && list.getSelectedItemPosition() <= firstSelectable()) {
            tabViews[tab].requestFocus();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /** The TV is far away: the selected row gets a solid gold frame and a chevron, not just a hairline. */
    private void highlight(View view) {
        if (lastSelected != null && lastSelected != view) resetRow(lastSelected);
        if (view == null) {
            lastSelected = null;
            return;
        }
        view.setBackgroundResource(R.drawable.row_selected);
        view.setSelected(true);   // its words in the focus ink (res/color)
        View cursor = view.findViewById(R.id.cursor);
        if (cursor != null) cursor.setVisibility(View.VISIBLE);
        lastSelected = view;
    }

    /** The green of an on row (OnOff.green, the DGA greens): the one that reads on the panel at rest, the one that reads on the focus fill when selected. */
    private android.content.res.ColorStateList onGreens() {
        android.util.TypedValue t = new android.util.TypedValue();
        int text = getTheme().resolveAttribute(R.attr.gText, t, true) ? t.data : 0xFFFFFFFF;
        int focus = getTheme().resolveAttribute(R.attr.gFocus, t, true) ? t.data : 0xFFFFFFFF;
        int rest = OnOff.green(text) == OnOff.GREEN_ON_LIGHT ? OnOff.GREEN_ON_DARK : OnOff.GREEN_ON_LIGHT;   // light words: a dark panel
        int on = OnOff.green(focus);
        return new android.content.res.ColorStateList(new int[][] {{android.R.attr.state_focused}, {android.R.attr.state_selected}, {}}, new int[] {on, on, rest});
    }

    private void resetRow(View view) {
        view.setBackgroundResource(R.drawable.row_bg);
        view.setSelected(false);
        View cursor = view.findViewById(R.id.cursor);
        if (cursor != null) cursor.setVisibility(View.INVISIBLE);
    }

    private int selectedKind() {
        int at = list.getSelectedItemPosition();
        return at >= 0 && at < adapter.rows.size() ? adapter.rows.get(at).kind : -1;
    }

    /** The adhan's volume in steps of 10 (10 to 100, no wrapping), heard for two seconds at the new level. */
    private void changeVolume(int step) {
        int was = prefs.adhanVolume(), now = Math.max(10, Math.min(100, was + step));
        if (now == was) return;
        prefs.setAdhanVolume(now);
        adapter.rebuild();
        focusList(list.getSelectedItemPosition());
        MusicService.adhan(this, prefs.voice(), Adhan.name(prefs.voice()), 2);
    }

    /** ٣٠٪ and a bar of ten: the filled part starts at the right, as the page does */
    private static String volumeText(int value) {
        int filled = Math.max(0, Math.min(10, value / 10));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) bar.append(i < filled ? '█' : '░');
        return percent(value) + "  " + bar;
    }

    /** From Android 12 exact alarms are the user's to allow: the system screen for it, else the app's own page. */
    private void openExactAlarmSettings() {
        Uri app = Uri.parse("package:" + getPackageName());
        Intent[] tries = Build.VERSION.SDK_INT >= 31
                ? new Intent[]{new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, app),
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, app)}
                : new Intent[]{new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, app)};
        for (Intent intent : tries) {
            try { startActivity(intent); return; } catch (RuntimeException ignored) { }
        }
    }

    private void openPlayer() {
        startActivity(new Intent(this, PlayerActivity.class));
    }

    /** ٣٠٪ (or 30%): the adhan volume in the digits the rest of the screen uses. */
    private static String percent(int value) {
        return Ui.digits(value + "%");
    }

    private static final class Row {
        final boolean header;
        final int kind, index;
        final String title, detail;
        final boolean next;
        Row(boolean header, int kind, String title, String detail, int index) {
            this(header, kind, title, detail, index, false);
        }
        Row(boolean header, int kind, String title, String detail, int index, boolean next) {
            this.header = header; this.kind = kind; this.title = title; this.detail = detail;
            this.index = index; this.next = next;
        }
        static Row header(String title) { return new Row(true, -1, title, null, -1); }
    }

    /** the reciters tab's rows as ReciterOrder made them, and where they start among the adapter's rows (a «متابعة التلاوة» row may come first) */
    private List<ReciterOrder.Item> reciterItems = new ArrayList<>();
    private int reciterOffset;

    private List<String> reciterIds() {
        List<String> ids = new ArrayList<>();
        for (Catalog.Reciter r : cat.reciters) ids.add(r.id);
        return ids;
    }

    private List<String> reciterNames() {
        List<String> names = new ArrayList<>();
        for (Catalog.Reciter r : cat.reciters) names.add(r.name);
        return names;
    }

    // ---- the letter index of the reciters tab: a column at the list's start edge; a letter no reciter starts with is dimmed and skipped

    private LinearLayout letters;
    private TextView[] letterViews;
    private boolean[] letterLit;
    private String[] alphabet;
    private boolean longFav;

    private void buildLetters() {
        letters = findViewById(R.id.letters);
        alphabet = ReciterOrder.alphabet(sa.urtv.home.S.en());
        letterViews = new TextView[alphabet.length];
        letters.removeAllViews();
        for (int i = 0; i < alphabet.length; i++) {
            final int which = i;
            TextView t = new TextView(this);
            t.setText(alphabet[i]);
            t.setTypeface(Ui.font(this, "Thmanyah-Medium.otf"));
            t.setTextSize(16);   // 16 sp at least (11 was unreadable on a TV, step 28); the letters no reciter starts with are not drawn, so the lit ones have the room
            t.setGravity(android.view.Gravity.CENTER);
            t.setIncludeFontPadding(false);
            t.setTextColor(0xFFE7DCC4);
            t.setOnFocusChangeListener((v, has) -> {
                v.setBackgroundResource(has ? R.drawable.tab_bg : 0);
                v.setSelected(has);
                if (has) jumpToLetter(which);   // just focusing it jumps the list
            });
            t.setOnClickListener(v -> jumpToLetter(which));
            letters.addView(t, new LinearLayout.LayoutParams(-1, 0, 1f));
            letterViews[i] = t;
        }
        paintLetters();
    }

    /** lit letters: those a reciter's name starts with; the others are dimmed and cannot take the focus (the D-pad skips them) */
    private void paintLetters() {
        if (letters == null) return;
        letterLit = ReciterOrder.present(alphabet, reciterNames());
        for (int i = 0; i < letterViews.length; i++) {
            letterViews[i].setFocusable(letterLit[i]);
            letterViews[i].setClickable(letterLit[i]);
            letterViews[i].setAlpha(letterLit[i] ? 1f : 0.3f);
            letterViews[i].setVisibility(letterLit[i] ? View.VISIBLE : View.GONE);
        }
        letters.setVisibility(tab == TAB_RECITERS ? View.VISIBLE : View.GONE);
    }

    private void jumpToLetter(int which) {
        int at = ReciterOrder.jump(reciterItems, reciterNames(), alphabet[which]);
        if (at >= 0) list.setSelectionFromTop(reciterOffset + at, 0);
    }

    /** the letter column gets the focus (the letter of the row the list is on, else the first lit one) */
    private boolean enterLetters() {
        if (tab != TAB_RECITERS || letters == null || letters.getVisibility() != View.VISIBLE) return false;
        int sel = list.getSelectedItemPosition(), target = -1;
        if (sel >= 0 && sel < adapter.rows.size() && adapter.rows.get(sel).kind == KIND_RECITER) {
            String l = ReciterOrder.letterOf(cat.reciters.get(adapter.rows.get(sel).index).name);
            for (int i = 0; i < alphabet.length; i++) if (alphabet[i].equals(l) && letterLit[i]) target = i;
        }
        for (int i = 0; target < 0 && i < letterLit.length; i++) if (letterLit[i]) target = i;
        if (target < 0) return false;
        letterViews[target].requestFocus();
        return true;
    }

    private boolean lettersHaveFocus() {
        if (letters == null) return false;
        for (TextView t : letterViews) if (t.hasFocus()) return true;
        return false;
    }

    /** long OK on a reciter: ★ on or off, the list kept on that reciter */
    private void toggleFavourite() {
        int sel = list.getSelectedItemPosition();
        if (sel < 0 || sel >= adapter.rows.size() || adapter.rows.get(sel).kind != KIND_RECITER) return;
        int reciter = adapter.rows.get(sel).index;
        String id = cat.reciters.get(reciter).id;
        boolean on = !ReciterOrder.parse(prefs.reciterFavs()).contains(id);
        prefs.setReciterFavs(ReciterOrder.toggle(prefs.reciterFavs(), id, on));
        adapter.rebuild();
        paintLetters();
        for (int p = 0; p < adapter.rows.size(); p++) if (adapter.rows.get(p).kind == KIND_RECITER && adapter.rows.get(p).index == reciter) { focusList(p); break; }
    }

    private final class RowsAdapter extends BaseAdapter {
        final List<Row> rows = new ArrayList<>();

        void rebuild() {
            rows.clear();
            if (tab == TAB_RECITERS) {
                if (prefs.source() == Prefs.SOURCE_SURAH) {
                    int index = cat.reciterIndex(prefs.reciter());
                    if (index >= 0) {
                        rows.add(new Row(false, KIND_RESUME, getString(R.string.continue_listening),
                                Ui.str(MainActivity.this, R.string.continue_detail,
                                        cat.surahName(prefs.surah()), cat.reciters.get(index).name), index));
                    }
                }
                reciterOffset = rows.size();
                reciterItems = ReciterOrder.order(reciterIds(), new java.util.HashSet<>(ReciterOrder.parse(prefs.reciterFavs())));
                for (ReciterOrder.Item it : reciterItems) {
                    if (it.header) { rows.add(Row.header(sa.urtv.home.S.t("المفضلون"))); continue; }   // the ★ reciters first, under their heading
                    Catalog.Reciter reciter = cat.reciters.get(it.reciter);
                    rows.add(new Row(false, KIND_RECITER, (it.fav ? "★ " : "") + reciter.name,
                            Ui.str(MainActivity.this, R.string.surah_count, Ui.arabic(reciter.surahs)), it.reciter));
                }
            } else if (tab == TAB_STATIONS) {
                if (prefs.source() == Prefs.SOURCE_RADIO && prefs.station() < cat.stations.size()) {
                    rows.add(new Row(false, KIND_RESUME, getString(R.string.continue_listening),
                            cat.stations.get(prefs.station()).name, -1));
                }
                for (int i = 0; i < cat.stations.size(); i++) {
                    rows.add(new Row(false, KIND_STATION, cat.stations.get(i).name, getString(R.string.live), i));
                }
                // «البث المباشر من الحرم»: a link out to the broadcaster's own page, only once its official link is filled in (HaramLive)
                for (int h = 0; h < HaramLive.LIVE.length; h++) {
                    if (HaramLive.shown(HaramLive.LIVE[h][1])) rows.add(new Row(false, KIND_LINK, HaramLive.LIVE[h][0], getString(R.string.haram_live_detail), h));
                }
            } else {
                // the adhan section: the switch first (the settings' «الأذان» button lands on it: OK, OK turns it on), then today's times
                // with the next one marked, then the muezzin and the rest
                PrayerTimes times = PrayerTimes.today(MainActivity.this);
                PrayerTimes.Next next = PrayerTimes.next(MainActivity.this);
                String toggle = prefs.adhanOn() ? getString(R.string.adhan_on) : getString(R.string.adhan_off);
                if (prefs.adhanOn() && next != null) {
                    toggle += "  •  " + sa.urtv.home.S.tv(next.name()) + " " + PrayerTimes.remaining(next.at - System.currentTimeMillis());
                }
                rows.add(new Row(false, KIND_ADHAN_TOGGLE, getString(R.string.adhan_auto), toggle, -1));
                if (prefs.adhanOn() && !AdhanScheduler.exactAllowed(MainActivity.this)) {   // the alarm may be minutes late
                    rows.add(new Row(false, KIND_EXACT, getString(R.string.adhan_exact_title), getString(R.string.adhan_exact_detail), -1));
                }
                for (int prayer : PrayerTimes.PRAYERS) {
                    boolean isNext = next != null && next.index == prayer;
                    String detail = PrayerTimes.clockText(times.clock[prayer]);
                    if (isNext) detail += "  •  " + PrayerTimes.remaining(next.at - System.currentTimeMillis());
                    rows.add(new Row(false, KIND_PRAYER, PrayerTimes.NAMES[prayer], detail, prayer, isNext));
                }
                rows.add(new Row(false, KIND_VOICE, getString(R.string.adhan_voice), Adhan.label(prefs.playVoice(MainActivity.this)), -1));
                rows.add(new Row(false, KIND_FAJR_VOICE, getString(R.string.adhan_fajr_voice), Adhan.label(prefs.playFajrVoice(MainActivity.this)), -1));
                rows.add(new Row(false, KIND_SHORT, getString(R.string.adhan_short),
                        getString(prefs.adhanShort() ? R.string.adhan_short_on : R.string.adhan_short_off), -1));
                rows.add(new Row(false, KIND_TAKBIR, getString(R.string.adhan_takbir),
                        getString(prefs.adhanTakbir() ? R.string.adhan_on : R.string.adhan_off), -1));
                rows.add(new Row(false, KIND_STOPKEY, getString(R.string.adhan_stopkey),
                        getString(prefs.adhanStopKey() ? R.string.adhan_stopkey_on : R.string.adhan_stopkey_off), -1));
                rows.add(new Row(false, KIND_SAVER_ONLY, getString(R.string.adhan_saver_only),
                        getString(prefs.adhanSaverOnly() ? R.string.adhan_on : R.string.adhan_off), -1));
                rows.add(new Row(false, KIND_NOTICE, getString(R.string.adhan_notice),
                        getString(prefs.adhanNotice() ? R.string.adhan_on : R.string.adhan_off), -1));
                rows.add(new Row(false, KIND_SAVER_AT, getString(R.string.adhan_saver_at),
                        getString(prefs.adhanSaverAt() ? R.string.adhan_on : R.string.adhan_off), -1));
                rows.add(new Row(false, KIND_VOLUME, getString(R.string.adhan_volume),
                        volumeText(prefs.adhanVolume()), -1));
            }
            notifyDataSetChanged();
        }

        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int position) { return rows.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean areAllItemsEnabled() { return false; }
        @Override public boolean isEnabled(int position) { return !rows.get(position).header; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int position) { return rows.get(position).header ? 0 : 1; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Row row = rows.get(position);
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (row.header) {
                TextView header = (TextView) (convertView != null ? convertView
                        : inflater.inflate(R.layout.item_header, parent, false));
                header.setText(Ui.line(row.title));
                header.setTypeface(Ui.font(MainActivity.this, "Thmanyah-Medium.otf"));
                return header;
            }
            View view = convertView != null ? convertView : inflater.inflate(R.layout.item_row, parent, false);
            resetRow(view);
            TextView name = view.findViewById(R.id.name);
            TextView detail = view.findViewById(R.id.detail);
            name.setText(Ui.line(row.title));   // the rows keep our Arabic words (OnOff reads them below); the screen says them in «اللغة»
            detail.setText(Ui.line(row.detail));
            name.setTypeface(Ui.font(MainActivity.this, "Thmanyah-Medium.otf"));
            detail.setTypeface(Ui.font(MainActivity.this, "Thmanyah-Light.otf"));
            name.setTextColor(getColorStateList(row.next ? R.color.gold : R.color.sand));   // the focus ink when selected
            // an on/off row (OnOff: its detail starts «مفعّل» or «متوقف»): the three green triangles at its start while on, its line in green
            int state = OnOff.state(row.detail);
            ImageView mark = view.findViewById(R.id.mark);
            if (mark != null) mark.setVisibility(state == 1 ? View.VISIBLE : View.INVISIBLE);
            if (state == 1) {
                android.content.res.ColorStateList green = onGreens();
                detail.setTextColor(green);
                if (mark != null) mark.setImageTintList(green);
            } else {
                detail.setTextColor(getColorStateList(R.color.dim));
            }
            return view;
        }
    }
}
