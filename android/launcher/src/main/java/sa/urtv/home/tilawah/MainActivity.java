package sa.urtv.home.tilawah;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.ListView;
import android.widget.TextView;

import sa.urtv.home.R;
import java.util.ArrayList;
import java.util.List;

/**
 * The «تلاوة» screen: three sections that never share a page. The tab bar switches between the
 * reciters, the live stations and the adhan (today's times, the switch and the muezzin).
 */
public class MainActivity extends Activity {

    /** opens the screen on its «الأذان» tab: the adhan's settings from the home's app menu and from the screensaver's settings (value "adhan") */
    public static final String EXTRA_OPEN = "open";
    private static final int TAB_RECITERS = 0, TAB_STATIONS = 1, TAB_ADHAN = 2;

    private static final int KIND_RESUME = 0, KIND_RECITER = 1, KIND_STATION = 2,
            KIND_PRAYER = 3, KIND_ADHAN_TOGGLE = 4, KIND_VOICE = 5, KIND_VOLUME = 6, KIND_EXACT = 7, KIND_FAJR_VOICE = 8, KIND_SHORT = 9, KIND_STOPKEY = 10;

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
            } else if (row.kind == KIND_STOPKEY) {
                prefs.setAdhanStopKey(!prefs.adhanStopKey());
                adapter.rebuild();
                focusList(list.getSelectedItemPosition());
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
        selectTab("adhan".equals(getIntent().getStringExtra(EXTRA_OPEN)) ? TAB_ADHAN : TAB_RECITERS);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

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

    /** The tab changes what the list holds; the selected row becomes the first one of that section. */
    private void selectTab(int which) {
        tab = Math.max(TAB_RECITERS, Math.min(TAB_ADHAN, which));
        for (int i = 0; i < tabViews.length; i++) tabViews[i].setSelected(i == tab);
        if (adapter != null) adapter.rebuild();
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
        // on «صوت الأذان» left/right are the volume: left louder, right quieter (the bar fills from the right, like the page)
        if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && list.hasFocus()
                && tab == TAB_ADHAN && selectedKind() == KIND_VOLUME) {
            changeVolume(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? 10 : -10);
            return true;
        }
        // left/right walk the tab bar from anywhere on the screen: the sections are one key apart
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // RTL: the first tab sits on the right, so right is «previous»
            int delta = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? -1 : 1;
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

    private final class RowsAdapter extends BaseAdapter {
        final List<Row> rows = new ArrayList<>();

        void rebuild() {
            rows.clear();
            if (tab == TAB_RECITERS) {
                if (prefs.source() == Prefs.SOURCE_SURAH) {
                    int index = cat.reciterIndex(prefs.reciter());
                    if (index >= 0) {
                        rows.add(new Row(false, KIND_RESUME, getString(R.string.continue_listening),
                                getString(R.string.continue_detail,
                                        cat.surahName(prefs.surah()), cat.reciters.get(index).name), index));
                    }
                }
                for (int i = 0; i < cat.reciters.size(); i++) {
                    Catalog.Reciter reciter = cat.reciters.get(i);
                    rows.add(new Row(false, KIND_RECITER, reciter.name,
                            getString(R.string.surah_count, Ui.arabic(reciter.surahs)), i));
                }
            } else if (tab == TAB_STATIONS) {
                if (prefs.source() == Prefs.SOURCE_RADIO && prefs.station() < cat.stations.size()) {
                    rows.add(new Row(false, KIND_RESUME, getString(R.string.continue_listening),
                            cat.stations.get(prefs.station()).name, -1));
                }
                for (int i = 0; i < cat.stations.size(); i++) {
                    rows.add(new Row(false, KIND_STATION, cat.stations.get(i).name, getString(R.string.live), i));
                }
            } else {
                // the adhan section: today's times with the next one marked, then the switch and the muezzin
                PrayerTimes times = PrayerTimes.today(MainActivity.this);
                PrayerTimes.Next next = PrayerTimes.next(MainActivity.this);
                for (int prayer : PrayerTimes.PRAYERS) {
                    boolean isNext = next != null && next.index == prayer;
                    String detail = PrayerTimes.clockText(times.clock[prayer]);
                    if (isNext) detail += "  •  " + PrayerTimes.remaining(next.at - System.currentTimeMillis());
                    rows.add(new Row(false, KIND_PRAYER, PrayerTimes.NAMES[prayer], detail, prayer, isNext));
                }
                String toggle = prefs.adhanOn() ? getString(R.string.adhan_on) : getString(R.string.adhan_off);
                if (prefs.adhanOn() && next != null) {
                    toggle += "  •  " + next.name() + " " + PrayerTimes.remaining(next.at - System.currentTimeMillis());
                }
                rows.add(new Row(false, KIND_ADHAN_TOGGLE, getString(R.string.adhan_auto), toggle, -1));
                if (prefs.adhanOn() && !AdhanScheduler.exactAllowed(MainActivity.this)) {   // the alarm may be minutes late
                    rows.add(new Row(false, KIND_EXACT, getString(R.string.adhan_exact_title), getString(R.string.adhan_exact_detail), -1));
                }
                rows.add(new Row(false, KIND_VOICE, getString(R.string.adhan_voice), Adhan.label(prefs.playVoice(MainActivity.this)), -1));
                rows.add(new Row(false, KIND_FAJR_VOICE, getString(R.string.adhan_fajr_voice), Adhan.label(prefs.playFajrVoice(MainActivity.this)), -1));
                rows.add(new Row(false, KIND_SHORT, getString(R.string.adhan_short),
                        getString(prefs.adhanShort() ? R.string.adhan_short_on : R.string.adhan_short_off), -1));
                rows.add(new Row(false, KIND_STOPKEY, getString(R.string.adhan_stopkey),
                        getString(prefs.adhanStopKey() ? R.string.adhan_stopkey_on : R.string.adhan_stopkey_off), -1));
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
                header.setText(row.title);
                header.setTypeface(Ui.font(MainActivity.this, "Thmanyah-Medium.otf"));
                return header;
            }
            View view = convertView != null ? convertView : inflater.inflate(R.layout.item_row, parent, false);
            resetRow(view);
            TextView name = view.findViewById(R.id.name);
            TextView detail = view.findViewById(R.id.detail);
            name.setText(row.title);
            detail.setText(row.detail);
            name.setTypeface(Ui.font(MainActivity.this, "Thmanyah-Medium.otf"));
            detail.setTypeface(Ui.font(MainActivity.this, "Thmanyah-Light.otf"));
            name.setTextColor(getColorStateList(row.next ? R.color.gold : R.color.sand));   // the focus ink when selected
            return view;
        }
    }
}
