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
    private int sel, col;   // col: 0 «احتفظ دائمًا» / «تلقائي», 1 «حذف الآن»

    @Override protected String id() { return "packs"; }
    @Override protected boolean inPlay() { return false; }

    @Override protected void onCreate(Bundle b) { super.onCreate(b); load(); }
    private void load() {
        rows = new ArrayList<>();
        for (GamePacks.Pack p : PackStore.index(this)) rows.add(p);
        for (java.io.File f : new java.io.File(getFilesDir(), "packs").listFiles() == null ? new java.io.File[0] : new java.io.File(getFilesDir(), "packs").listFiles()) {   // a pack on this TV that the index no longer lists
            String n = f.getName();
            if (!n.endsWith(".gb")) continue;
            String pid = n.substring(0, n.length() - 3); boolean known = false;
            for (GamePacks.Pack p : rows) known |= p.id.equals(pid);
            if (!known) rows.add(new GamePacks.Pack(pid, pid, pid, 0, f.length(), ""));
        }
        PackStore.ensure(this, "", null);   // the newest index, for the next time this screen opens
    }

    @Override protected void draw() {
        title(S.t("حزم المحتوى"), S.t("المحتوى الكبير يُحمَّل ولا يأتي مع التطبيق"));
        if (rows.isEmpty()) { root.addView(centered(S.t("ما فيه حزم الآن"), 64, L.ink(0xCC), true), at(1728, 100, 96, 360)); hints(S.t("رجوع"), S.t("الألعاب")); return; }
        for (int i = 0; i < rows.size() && i < 6; i++) {
            GamePacks.Pack p = rows.get(i);
            boolean on = PackStore.installed(this, p.id), keep = PackStore.mode(this, p.id) == GamePacks.KEEP;
            float y = 232 + i * 120;
            long bytes = PackStore.size(this, p.id) > 0 ? PackStore.size(this, p.id) : p.bytes;
            root.addView(text(clip(p.name(en), 52, 900), 52, L.ink, true), at(960, 64, 96, y));
            root.addView(text((on ? S.t("محمّلة") : S.t("غير محمّلة")) + ": " + num(Math.max(1, Math.round(bytes / 1000f))) + " KB", 36, L.ink(0xCC), false), at(960, 48, 96, y + 62));
            root.addView(button(keep ? S.t("احتفظ دائمًا") : S.t("تلقائي"), sel == i && col == 0), at(360, 88, 1100, y));
            root.addView(button(S.t("حذف الآن"), sel == i && col == 1), at(300, 88, 1480, y));
        }
        hints("▲ ▼ ◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("غيّر"), S.t("رجوع"), S.t("الألعاب"));
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        if (back) { finish(); return; }
        if (rows.isEmpty()) return;
        if (dy != 0) { sel = Math.max(0, Math.min(Math.min(rows.size(), 6) - 1, sel + dy)); redraw(); return; }
        if (dx != 0) { col = dx > 0 ? 1 : 0; redraw(); return; }
        if (!ok) return;
        GamePacks.Pack p = rows.get(sel);
        if (col == 0) PackStore.setMode(this, p.id, PackStore.mode(this, p.id) == GamePacks.KEEP ? GamePacks.AUTO : GamePacks.KEEP);
        else PackStore.delete(this, p.id);
        redraw();
    }
}
