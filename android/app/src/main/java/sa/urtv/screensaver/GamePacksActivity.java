package sa.urtv.screensaver;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * «حزم المحتوى»: the packs of the games with their size, «احتفظ دائمًا» or «تلقائي» (a pack nobody opened for 14 days deletes itself and comes back when its game
 * opens) and «حذف الآن». Opened from the games list. docs/GAMES-PACKS.md.
 */
public class GamePacksActivity extends GameActivity {
    private List<GamePacks.Pack> rows = new ArrayList<>();
    private int sel, col, first;   // col: 0 «احتفظ دائمًا» / «تلقائي», 1 «حذف الآن»; first: the row at the top (the list scrolls)
    private static final int FIT = 6;   // the rows that stand on the screen at once

    @Override protected String id() { return "packs"; }
    @Override protected boolean inPlay() { return false; }

    @Override protected void onCreate(Bundle b) { super.onCreate(b); load(); }
    private void load() {
        rows = new ArrayList<>();
        for (GamePacks.Pack p : GamePackStore.index(this)) rows.add(p);
        for (java.io.File f : new java.io.File(getFilesDir(), "gamepacks").listFiles() == null ? new java.io.File[0] : new java.io.File(getFilesDir(), "gamepacks").listFiles()) {   // a pack on this TV that the index no longer lists
            String n = f.getName();
            if (!n.endsWith(".gb")) continue;
            String pid = n.substring(0, n.length() - 3); boolean known = false;
            for (GamePacks.Pack p : rows) known |= p.id.equals(pid);
            if (!known) rows.add(new GamePacks.Pack(pid, pid, pid, 0, f.length(), ""));
        }
        GamePackStore.ensure(this, "", null);   // the newest index, for the next time this screen opens
    }

    @Override protected void draw() {
        title(S.t("حزم المحتوى"), S.t("المحتوى الكبير يُحمَّل ولا يأتي مع التطبيق"));
        if (rows.isEmpty()) { root.addView(centered(S.t("ما فيه حزم الآن"), 64, L.ink(0xCC), true), at(1728, 100, 96, 360)); hints(S.t("رجوع"), S.t("الألعاب")); return; }
        first = scrollTo(rows.size(), sel, first, FIT);
        for (int i = first; i < rows.size() && i < first + FIT; i++) {
            GamePacks.Pack p = rows.get(i);
            boolean on = GamePackStore.installed(this, p.id), keep = GamePackStore.mode(this, p.id) == GamePacks.KEEP;
            float y = 232 + (i - first) * 120;
            long bytes = GamePackStore.size(this, p.id) > 0 ? GamePackStore.size(this, p.id) : p.bytes;
            boolean fresh = p.isNew(System.currentTimeMillis());
            root.addView(text(clip(p.name(en), 52, fresh ? 700 : 900), 52, L.ink, true), at(fresh ? 760 : 960, 64, 96, y));
            if (fresh) { TextView tag = centered(S.t("جديد"), 34, L.onSaff, true); tag.setBackgroundColor(L.saff); root.addView(tag, at(120, 48, 96 + 780, y + 8)); }
            root.addView(text((on ? S.t("محمّلة") : S.t("غير محمّلة")) + ": " + num(Math.max(1, Math.round(bytes / 1000f))) + " KB" + (p.count > 0 ? "   " + S.t("%s سؤال", num(p.count)) : ""), 36, L.ink(0xCC), false), at(960, 48, 96, y + 62));
            root.addView(button(keep ? S.t("احتفظ دائمًا") : S.t("تلقائي"), sel == i && col == 0), at(360, 88, 1100, y));
            root.addView(button(S.t("حذف الآن"), sel == i && col == 1), at(300, 88, 1480, y));
        }
        if (first > 0) root.addView(centered("▲", 36, L.ink(0xCC), true), at(64, 48, 1760, 188));            // more packs above
        if (first + FIT < rows.size()) root.addView(centered("▼", 36, L.ink(0xCC), true), at(64, 48, 1760, 952));   // more packs below
        hints("▲ ▼ ◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("غيّر"), S.t("رجوع"), S.t("الألعاب"));
    }

    /** the first row to show so that `sel` is on the screen (the same offset rule as GameActivity.setup's chip rows) */
    static int scrollTo(int n, int sel, int first, int fit) {
        if (sel < first) first = sel;
        if (sel >= first + fit) first = sel - fit + 1;
        return Math.max(0, Math.min(first, Math.max(0, n - fit)));
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        if (back) { finish(); return; }
        if (rows.isEmpty()) return;
        if (dy != 0) { sel = Math.max(0, Math.min(rows.size() - 1, sel + dy)); redraw(); return; }
        if (dx != 0) { col = dx > 0 ? 1 : 0; redraw(); return; }
        if (!ok) return;
        GamePacks.Pack p = rows.get(sel);
        if (col == 0) GamePackStore.setMode(this, p.id, GamePackStore.mode(this, p.id) == GamePacks.KEEP ? GamePacks.AUTO : GamePacks.KEEP);
        else GamePackStore.delete(this, p.id);
        redraw();
    }
}
