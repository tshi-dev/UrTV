package sa.urtv.home.tilawah;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import sa.urtv.home.R;

/** The 114 surahs of one reciter, six columns, made for the remote's arrows. */
public class SurahActivity extends Activity {

    private Catalog cat;
    private Catalog.Reciter reciter;
    private GridView grid;
    private long lastMove;   // the last cell move by a held left/right key

    @Override protected void onCreate(Bundle state) {
        setTheme(Ui.theme(this));   // «المظهر»: the look and the light of now, before anything is drawn
        super.onCreate(state);
        sa.urtv.home.Digits.init(this);   // the screensaver's «الأرقام» setting
        cat = Catalog.get(this);
        int index = getIntent().getIntExtra("reciter", -1);
        if (index < 0 || index >= cat.reciters.size()) { finish(); return; }
        reciter = cat.reciters.get(index);
        setContentView(R.layout.activity_surah);
        Ui.glassPage(this, R.id.bg);   // the glass page

        TextView name = findViewById(R.id.reciter);
        name.setText(reciter.name);
        Ui.text(name, "ThmanyahDisplay-Bold.otf", 36, R.color.sand);
        TextView count = findViewById(R.id.count);
        count.setText(getString(R.string.surah_count, Ui.arabic(reciter.surahs)));
        Ui.text(count, "Thmanyah-Regular.otf", 14, R.color.dim);
        Ui.text((TextView) findViewById(R.id.choose), "Thmanyah-Regular.otf", 16, R.color.gold);
        Ui.text((TextView) findViewById(R.id.hint), "Thmanyah-Regular.otf", 14, R.color.dim);

        grid = findViewById(R.id.grid);
        grid.setAdapter(new SurahAdapter());
        grid.setOnItemClickListener((parent, view, position, id) -> {
            MusicService.playSurah(this, reciter.id, position + 1);
            startActivity(new Intent(this, PlayerActivity.class));
        });

        Prefs prefs = new Prefs(this);
        if (prefs.source() == Prefs.SOURCE_SURAH && reciter.id.equals(prefs.reciter())) {
            int last = Math.max(0, prefs.surah() - 1);
            grid.post(() -> grid.setSelection(last));
        }
        grid.post(() -> grid.requestFocus());
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && grid != null) grid.post(() -> grid.requestFocus());
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        // the grid reads from the right, so the cells are walked by hand: right is right. A held key too, a cell
        // at a time: its repeats used to reach Android's own search, which went the other way
        if (e.getAction() == KeyEvent.ACTION_DOWN
                && (e.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT
                    || e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            if (e.getRepeatCount() > 0 && e.getEventTime() - lastMove < 90) return true;
            if (moveCell(e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) { lastMove = e.getEventTime(); return true; }
            // on a cell with nowhere to go (an end of the grid), or held: stay there. Android's own search counts the cells the other way
            if (CellWalk.consume(false, e.getRepeatCount(), onCell())) return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private boolean onCell() {
        View focused = getCurrentFocus();
        return focused != null && grid.indexOfChild(focused) >= 0;
    }

    /** Moves the focus to the cell on the right (true) or on the left (false) of the focused one. */
    private boolean moveCell(boolean right) {
        View focused = getCurrentFocus();
        if (focused == null) return false;
        int child = grid.indexOfChild(focused);
        if (child < 0) return false;
        int position = grid.getFirstVisiblePosition() + child;
        // the first cell of the grid sits on the right, so the cell before is the one on the right
        int next = CellWalk.next(position, grid.getCount(), right);
        if (next == CellWalk.NONE) return false;
        View cell = grid.getChildAt(next - grid.getFirstVisiblePosition());
        if (cell == null) {
            grid.setSelection(next);
            return true;
        }
        cell.requestFocus();
        return true;
    }

    private final class SurahAdapter extends BaseAdapter {
        @Override public int getCount() { return reciter.surahs; }
        @Override public Object getItem(int position) { return position + 1; }
        @Override public long getItemId(int position) { return position + 1; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surah, parent, false);
            TextView number = view.findViewById(R.id.number);
            TextView name = view.findViewById(R.id.name);
            number.setText(Ui.arabic(position + 1));
            name.setText(cat.surahName(position + 1));
            number.setTypeface(Ui.font(SurahActivity.this, "Thmanyah-Regular.otf"));
            name.setTypeface(Ui.font(SurahActivity.this, "Thmanyah-Medium.otf"));
            return view;
        }
    }
}
