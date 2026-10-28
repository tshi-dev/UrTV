package sa.urtv.home.tilawah;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import sa.urtv.home.R;

/**
 * The adhan recordings: OK picks one and plays a few seconds of it. When the person has put a recording of their
 * own on this TV (optimize/music.sh adhan add), «أذان من ملفي» is the first row. «المؤذن» lists the 17 normal ones (the four Fajr
 * recordings are for Fajr only); «مؤذن الفجر» (EXTRA_FAJR) lists the four Fajr ones first and then the normal ones.
 */
public class AdhanActivity extends Activity {
    static final String EXTRA_FAJR = "fajr";

    /** the numbered voices of this list, in the order of its rows (after the person's own, when there is one) */
    private int[] voices;
    private boolean fajr;

    private Prefs prefs;
    private ListView list;
    private Voices adapter;
    private View lastSelected;
    /** 1 when the list starts with the person's own recording, else 0 */
    private int own;

    @Override protected void onCreate(Bundle state) {
        setTheme(Ui.theme(this));   // «المظهر»: the look and the light of now, before anything is drawn
        super.onCreate(state);
        sa.urtv.home.Digits.init(this);   // the screensaver's «الأرقام» setting
        setContentView(R.layout.activity_adhan);
        Ui.glassPage(this, R.id.bg);   // the glass page
        prefs = new Prefs(this);
        own = OwnAdhan.exists(this) ? 1 : 0;
        fajr = getIntent().getBooleanExtra(EXTRA_FAJR, false);
        voices = fajr ? AdhanVoices.fajrList() : AdhanVoices.normal();
        if (fajr) ((TextView) findViewById(R.id.title)).setText(R.string.adhan_fajr_voices);

        Ui.text((TextView) findViewById(R.id.title), "ThmanyahDisplay-Bold.otf", 40, R.color.sand);
        Ui.text((TextView) findViewById(R.id.hint), "Thmanyah-Regular.otf", 14, R.color.dim);

        list = findViewById(R.id.list);
        adapter = new Voices();
        list.setAdapter(adapter);
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                highlight(view);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            int voice = voiceAt(position);
            if (fajr) {
                if (voice == Prefs.OWN) prefs.setFajrOwn(true); else prefs.setFajrVoice(voice);
                Adhan.prefetchChosen(this);   // the new Fajr recording is on the TV before Fajr
            } else if (voice == Prefs.OWN) prefs.setOwnChosen(true);
            else { prefs.setVoice(voice); prefs.setOwnChosen(false); }
            MusicService.adhan(this, voice, Adhan.name(voice), 12);
            finish();
        });

        int current = chosenVoice();
        int selected = 0;
        for (int i = 0; i < voices.length + own; i++) if (voiceAt(i) == current) selected = i;
        list.setSelection(selected);
        list.post(() -> {
            list.requestFocus();
            highlight(list.getSelectedView());
        });
    }

    /** the voice a row stands for: 0 (the person's own), or 1..21 */
    private int voiceAt(int position) { return own == 1 && position == 0 ? Prefs.OWN : voices[position - own]; }

    /** the voice in use for the prayers this list is for */
    private int chosenVoice() { return fajr ? prefs.playFajrVoice(this) : prefs.playVoice(this); }

    private void highlight(View view) {
        if (lastSelected != null && lastSelected != view) resetRow(lastSelected);
        if (view == null) { lastSelected = null; return; }
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

    private final class Voices extends BaseAdapter {
        @Override public int getCount() { return voices.length + own; }
        @Override public Object getItem(int position) { return voiceAt(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
            resetRow(view);
            int voice = voiceAt(position);
            TextView name = view.findViewById(R.id.name);
            TextView detail = view.findViewById(R.id.detail);
            name.setText(Adhan.name(voice));
            name.setTypeface(Ui.font(AdhanActivity.this, "Thmanyah-Medium.otf"));
            int playing = chosenVoice();
            boolean chosen = voice == playing;
            detail.setText(chosen
                    ? getString(R.string.adhan_chosen, Adhan.place(voice))
                    : Adhan.place(voice));
            detail.setTypeface(Ui.font(AdhanActivity.this, "Thmanyah-Light.otf"));
            detail.setTextColor(getColorStateList(chosen ? R.color.gold : R.color.dim));   // the focus ink when selected
            return view;
        }
    }
}
