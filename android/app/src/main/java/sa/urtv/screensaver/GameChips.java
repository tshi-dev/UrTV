package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;

/**
 * The quick setup of a game (no Android, tests/GamesKitTest.java): a few rows of chips and one start button. The focus STARTS ON THE BUTTON, so OK starts a
 * round with the choices of last time (games/COMMON.md: a round starts in under 10 seconds, nothing is asked). Up reaches the rows; left and right move
 * along a row and OK picks. A chip can be switched off («قريبًا», dashed) by the game. What was picked is one short string (`save`), kept by GamePrefs.
 */
final class GameChips {
    static final class Row {
        final String key, label; final String[] opts; final int def; int picked; boolean[] off;
        Row(String key, String label, String[] opts, int def) { this.key = key; this.label = label; this.opts = opts; this.def = def; this.picked = def; this.off = new boolean[opts.length]; }
    }

    final List<Row> rows = new ArrayList<>();
    /** the focus: row == rows.size() is the start button */
    int row, col;

    GameChips add(String key, String label, String[] opts, int def) { rows.add(new Row(key, label, opts, Math.max(0, Math.min(opts.length - 1, def)))); row = rows.size(); return this; }

    Row get(String key) { for (Row r : rows) if (r.key.equals(key)) return r; return null; }
    int picked(String key) { Row r = get(key); return r == null ? 0 : r.picked; }
    boolean onButton() { return row >= rows.size(); }

    /** an arrow: true when the focus moved. `dx` is toward the end of the line (the caller has turned it for Arabic). */
    boolean move(int dx, int dy) {
        if (dy != 0) {
            int to = Math.max(0, Math.min(rows.size(), row + dy));
            if (to == row) return false;
            row = to; col = onButton() ? 0 : rows.get(row).picked;
            return true;
        }
        if (dx == 0 || onButton()) return false;
        int to = Math.max(0, Math.min(rows.get(row).opts.length - 1, col + dx));
        if (to == col) return false;
        col = to;
        return true;
    }

    /** OK on a chip: picks it (false when it is switched off, or when the focus is on the button: the caller starts the game then) */
    boolean pick() {
        if (onButton()) return false;
        Row r = rows.get(row);
        if (r.off[col]) return false;
        r.picked = col;
        return true;
    }

    /** a chip the game cannot offer now; a picked one falls back to the row's first offered chip */
    void setOff(String key, int i, boolean off) {
        Row r = get(key);
        if (r == null || i < 0 || i >= r.opts.length) return;
        r.off[i] = off;
        if (off && r.picked == i) for (int k = 0; k < r.opts.length; k++) if (!r.off[k]) { r.picked = k; break; }
    }

    /** "key=index;key=index": only the keys, never a name or a score */
    String save() {
        StringBuilder b = new StringBuilder();
        for (Row r : rows) { if (b.length() > 0) b.append(';'); b.append(r.key).append('=').append(r.picked); }
        return b.toString();
    }

    /** what `save` wrote; an unknown key, a bad number or an index out of the row is ignored (the default stays) */
    void load(String s) {
        if (s == null) return;
        for (String kv : s.split(";")) {
            int eq = kv.indexOf('=');
            if (eq <= 0) continue;
            Row r = get(kv.substring(0, eq));
            if (r == null) continue;
            try { int v = Integer.parseInt(kv.substring(eq + 1)); if (v >= 0 && v < r.opts.length) r.picked = v; } catch (NumberFormatException ignored) { /* the default */ }
        }
    }
}
