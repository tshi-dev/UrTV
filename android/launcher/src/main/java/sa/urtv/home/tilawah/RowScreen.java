package sa.urtv.home.tilawah;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sa.urtv.home.OnOff;
import sa.urtv.home.R;

/**
 * 4.4: the screens of «صوتياتي» and «الأدعية والتذكيرات» look and move like the adhan tab of «تلاوة» (its layout, its rows, its focus): a title,
 * a list of rows (a title, a line under it), OK on a row, a long OK, left and right on a row that has steps. A question (a choice, a confirmation,
 * the red warning of «سعة أكبر») is a page of its own on the same list; Back goes back one page. No dialogs, no pills.
 */
abstract class RowScreen extends Activity {
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(sa.urtv.home.S.wrap(b)); }

    /** one row: a heading (header), or a row with what OK, a long OK and left/right do */
    static final class Row {
        final boolean header, warn;
        final String title, detail;
        Runnable ok, longOk;
        java.util.function.IntConsumer step;   // -1 left, +1 right (in the reading direction)
        Row(boolean header, boolean warn, String title, String detail) { this.header = header; this.warn = warn; this.title = title; this.detail = detail; }
        static Row head(String t) { return new Row(true, false, t, null); }
        static Row warning(String t) { return new Row(true, true, t, null); }
        static Row of(String t, String d, Runnable ok) { Row r = new Row(false, false, t, d); r.ok = ok; return r; }
        static Row red(String t, String d, Runnable ok) { Row r = new Row(false, true, t, d); r.ok = ok; return r; }
        Row onLong(Runnable r) { longOk = r; return this; }
        Row onStep(java.util.function.IntConsumer s) { step = s; return this; }
    }

    /** a page: its title, its line, and how its rows are made (made again on every refresh) */
    interface Page { String title(); String tagline(); void rows(List<Row> out); }

    private final List<Page> pages = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private ListView list;
    private Adapter adapter;
    private View lastSelected;
    private TextView title, tagline, hint;

    /** the first page */
    abstract Page first();

    /** the line at the bottom */
    String hintText() { return sa.urtv.home.S.t("حسنًا للاختيار، ومطوّلًا لخيارات أكثر، ورجوع للعودة"); }

    @Override protected void onCreate(Bundle state) {
        setTheme(Ui.theme(this));
        super.onCreate(state);
        sa.urtv.home.Digits.init(this);
        setContentView(R.layout.activity_main);
        Ui.localize(this);
        Ui.glassPage(this, R.id.bg);
        findViewById(R.id.tabs).setVisibility(View.GONE);
        title = findViewById(R.id.title);
        tagline = findViewById(R.id.tagline);
        hint = findViewById(R.id.hint);
        Ui.text(title, "ThmanyahDisplay-Bold.otf", 44, R.color.sand);
        Ui.text(tagline, "Thmanyah-Regular.otf", 17, R.color.dim);
        Ui.text(hint, "Thmanyah-Regular.otf", 14, R.color.dim);
        list = findViewById(R.id.list);
        adapter = new Adapter();
        list.setAdapter(adapter);
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { if (list.hasFocus() && pos >= 0 && pos < rows.size() && !rows.get(pos).header) highlight(v); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        list.setOnFocusChangeListener((v, has) -> {
            if (!has) { if (lastSelected != null) reset(lastSelected); lastSelected = null; }
            else { int s = list.getSelectedItemPosition(); if (s >= 0 && s < rows.size() && !rows.get(s).header) highlight(list.getSelectedView()); }
        });
        list.setOnItemClickListener((p, v, pos, id) -> { if (pos >= 0 && pos < rows.size() && rows.get(pos).ok != null) rows.get(pos).ok.run(); });
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            if (pos < 0 || pos >= rows.size() || rows.get(pos).longOk == null) return false;
            rows.get(pos).longOk.run();
            return true;
        });
        pages.add(first());
        show(0);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    /** a new page on top (Back closes it) */
    void open(Page p) { pages.add(p); show(0); }

    /** the page on top goes; the one under it is made again */
    void back() {
        if (pages.size() <= 1) { finish(); return; }
        pages.remove(pages.size() - 1);
        show(0);
    }

    /** the same page made again (what it shows changed), the focus kept on the same row */
    void refresh() {
        int at = list.getSelectedItemPosition();
        show(Math.max(0, at));
    }

    /** a question with its answers; OK on an answer runs it and closes the question */
    void ask(String question, String line, boolean warn, String[] answers, Runnable[] runs) {
        open(new Page() {
            @Override public String title() { return question; }
            @Override public String tagline() { return ""; }
            @Override public void rows(List<Row> out) {
                if (line != null) out.add(warn ? Row.warning(line) : Row.head(line));
                for (int i = 0; i < answers.length; i++) {
                    final Runnable r = runs[i];
                    out.add(Row.of(answers[i], "", () -> { back(); if (r != null) r.run(); }));
                }
            }
        });
    }

    void toast(String text) { android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show(); }

    private void show(int focus) {
        Page p = pages.get(pages.size() - 1);
        title.setText(p.title());
        tagline.setText(p.tagline());
        hint.setText(hintText());
        rows.clear();
        p.rows(rows);
        adapter.notifyDataSetChanged();
        int at = Math.min(focus, Math.max(0, rows.size() - 1));
        while (at < rows.size() && rows.get(at).header) at++;
        if (at >= rows.size()) at = 0;
        final int target = at;
        list.setSelection(target);
        list.post(() -> {
            list.requestFocus();
            list.setSelection(target);
            if (target < rows.size() && !rows.get(target).header) highlight(list.getSelectedView());
        });
    }

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) { back(); return true; }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            int at = list.getSelectedItemPosition();
            if (at >= 0 && at < rows.size() && rows.get(at).step != null) {
                boolean forward = (code == KeyEvent.KEYCODE_DPAD_LEFT) == sa.urtv.home.Dir.rtl();   // Arabic: left is «more»
                rows.get(at).step.accept(forward ? 1 : -1);
                refresh();
                return true;
            }
        }
        return super.onKeyDown(code, e);
    }

    private void highlight(View v) {
        if (lastSelected != null && lastSelected != v) reset(lastSelected);
        if (v == null) { lastSelected = null; return; }
        v.setBackgroundResource(R.drawable.row_selected);
        v.setSelected(true);
        View c = v.findViewById(R.id.cursor);
        if (c != null) c.setVisibility(View.VISIBLE);
        lastSelected = v;
    }

    private void reset(View v) {
        v.setBackgroundResource(R.drawable.row_bg);
        v.setSelected(false);
        View c = v.findViewById(R.id.cursor);
        if (c != null) c.setVisibility(View.INVISIBLE);
    }

    /** the red of a warning that must be read (the storage of «سعة أكبر», a delete) */
    static final int WARN = 0xFFFF6B60;

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int p) { return rows.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public boolean areAllItemsEnabled() { return false; }
        @Override public boolean isEnabled(int p) { return !rows.get(p).header; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int p) { return rows.get(p).header ? 0 : 1; }

        @Override public View getView(int p, View convert, ViewGroup parent) {
            Row row = rows.get(p);
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (row.header) {
                TextView h = (TextView) (convert != null ? convert : inf.inflate(R.layout.item_header, parent, false));
                h.setText(Ui.line(row.title));
                h.setSingleLine(false);
                h.setTypeface(Ui.font(RowScreen.this, "Thmanyah-Medium.otf"));
                if (row.warn) h.setTextColor(WARN); else h.setTextColor(getColorStateList(R.color.gold));
                return h;
            }
            View v = convert != null ? convert : inf.inflate(R.layout.item_row, parent, false);
            reset(v);
            TextView name = v.findViewById(R.id.name), detail = v.findViewById(R.id.detail);
            name.setText(Ui.line(row.title));
            detail.setText(Ui.line(row.detail == null ? "" : row.detail));
            detail.setVisibility(row.detail == null || row.detail.isEmpty() ? View.GONE : View.VISIBLE);
            name.setTypeface(Ui.font(RowScreen.this, "Thmanyah-Medium.otf"));
            detail.setTypeface(Ui.font(RowScreen.this, "Thmanyah-Light.otf"));
            if (row.warn) name.setTextColor(WARN); else name.setTextColor(getColorStateList(R.color.sand));
            detail.setTextColor(getColorStateList(R.color.dim));
            ImageView mark = v.findViewById(R.id.mark);
            if (mark != null) mark.setVisibility(OnOff.state(row.detail) == 1 ? View.VISIBLE : View.INVISIBLE);
            return v;
        }
    }
}
