package sa.urtv.screensaver;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import sa.urtv.screensaver.games.word5.Word5Game;
import sa.urtv.screensaver.games.word5.Word5Keys;
import sa.urtv.screensaver.games.word5.Word5Text;
import sa.urtv.screensaver.games.word5.Word5Words;

/**
 * «خمّن الكلمة» (games/word5/SPEC.md, docs/GAMES-KIT.md): everyone against the word, one remote. The rows of tiles stand at the start side and the keyboard
 * (28 letters, «إدخال», «حذف») at the end side, together they fill the screen. Arrows move over the keys and wrap around, OK types, Back deletes a letter
 * (and opens the pause card on an empty row). The tiles and the keys are Arabic in both languages, so they run right to left even in the English interface;
 * a tile shows the letter exactly as its key does, and the end shows the word in its own spelling. The rules are Word5Game (tests/Word5Test.java).
 */
public class Word5Activity extends GameActivity {
    private enum Screen { SETUP, PLAY, END }
    private static final int GRID_W = 718, KEY = 124, GAP = 12, KEYS_Y = 292;

    private Screen screen = Screen.SETUP;
    private GameChips chips;
    private Word5Words words;
    private Word5Game game;
    private Word5Words.Answer answer;
    private final Word5Keys keys = new Word5Keys();
    private String message = "";
    private boolean shake;
    private int sel;

    @Override protected String id() { return "word5"; }
    @Override protected boolean inPlay() { return screen == Screen.PLAY; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Map<?, ?> a = GameData.load(this, "word5-answers", GamesBoardData::answers, GamesBoardData::answers);
        Map<?, ?> g = GameData.load(this, "word5-guesses", GamesBoardData::guesses, GamesBoardData::guesses);
        words = GamesBoardData.word5(a, g);
        GameData.refresh(this, "word5-answers", GamesBoardData::answers, GamesBoardData.ANSWERS_BYTES);
        GameData.refresh(this, "word5-guesses", GamesBoardData::guesses, GamesBoardData.GUESSES_BYTES);
        chips = new GameChips();
        chips.add("tries", S.t("المحاولات"), new String[] {num(Word5Game.TRIES[0]), num(Word5Game.TRIES[1]), num(Word5Game.TRIES[2])}, 0);
        chips.add("kids", S.t("الفئة"), new String[] {S.t("للجميع"), S.t("أطفال")}, 0);
        remembered(soundChip(chips));
    }

    private void start() {
        if (words == null) { toast(S.t("قريبًا")); return; }
        List<String> recent = new ArrayList<>(GamePrefs.recent(this, id()));
        answer = words.free(chips.picked("kids") == 1, recent, new Random());
        GamePrefs.remember(this, id(), Collections.singletonList(answer.word));
        game = new Word5Game(words, answer.word, Word5Game.TRIES[chips.picked("tries")]);
        keys.set(0); message = ""; shake = false;
        screen = Screen.PLAY;
        redraw();
    }

    @Override protected void onEndRound() { screen = Screen.END; sel = 0; }   // «إنهاء الجولة» shows the word before leaving (the result screen says «الكلمة كانت»)

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        if (screen == Screen.SETUP) { if (keySetup(chips, back, ok, dx, dy)) start(); return; }
        if (screen == Screen.END) {
            if (back) { finish(); return; }
            int v = dy != 0 ? keyButtons(sel, 3, false, dy) : keyButtons(sel, 3, ok, 0);
            if (v >= 0) { sel = v; redraw(); }
            else if (v == -1) start(); else if (v == -2) { screen = Screen.SETUP; redraw(); } else finish();
            return;
        }
        shake = false;
        // only what a key changes is drawn again (a key press used to rebuild the whole board: 46 % janky frames while typing)
        if (back) { if (game.delete()) { message = ""; typedChanged(); } else openPause(); return; }
        if (!ok) {
            // the keys are Arabic: «next» is to the left in both languages, and dx is already turned toward the end of the interface's line
            int was = keys.key();
            keys.move(dy < 0 ? Word5Keys.UP : dy > 0 ? Word5Keys.DOWN : (dx > 0) == rtl ? Word5Keys.NEXT : Word5Keys.PREV);
            drawKey(was); drawKey(keys.key());
            return;
        }
        int k = keys.key();
        if (k == Word5Keys.DELETE) { if (game.delete()) { message = ""; typedChanged(); } }
        else if (k != Word5Keys.ENTER) {
            if (game.type(letterOf(k))) {
                message = "";
                if (game.typing().length() == Word5Text.LENGTH) { int was = keys.key(); keys.set(Word5Keys.ENTER); drawKey(was); drawKey(Word5Keys.ENTER); }   // a full row: the next OK enters it
                typedChanged();
            } else play(QuizSound.LOW);
        } else {
            Word5Game.Entered e = game.enter();
            if (e == Word5Game.Entered.SHORT) { message = S.t("الكلمة خمسة حروف"); play(QuizSound.LOW); refreshLine(); return; }
            else if (e == Word5Game.Entered.UNKNOWN) { message = S.t("ما نعرف هذي الكلمة"); shake = true; keys.set(Word5Keys.DELETE); play(QuizSound.LOW); }
            else if (e == Word5Game.Entered.NEXT) { message = ""; keys.set(0); play(QuizSound.CHIME); }
            else { screen = Screen.END; sel = 0; play(e == Word5Game.Entered.WON ? QuizSound.FANFARE : QuizSound.LOW); }
            redraw();
        }
    }

    /** the keyboard key a letter key types: ة is ه, ى is ي and ء is ا (Word5Text folds them the same way) */
    private int letterOf(int k) {
        return k == Word5Keys.TAA ? Word5Text.ALPHABET.indexOf('ه') : k == Word5Keys.YAA ? Word5Text.ALPHABET.indexOf('ي') : k == Word5Keys.HAMZA ? Word5Text.ALPHABET.indexOf('ا') : k;
    }
    private static final String EXTRA = "ةىء";

    // ---------------------------------------------------------------- the screens

    /** x of column `i` of an Arabic row (column 0 at the right in both languages), from the start edge of a block `width` wide */
    private float col(int i, float size, float width, int n) {
        float fromRight = i * (size + GAP);
        return rtl ? fromRight : width - size - fromRight;
    }

    private int colour(int c) { return c == Word5Game.GREEN ? L.ok : c == Word5Game.YELLOW ? L.saff : L.ink(0x8C); }

    @Override protected void draw() {
        if (screen == Screen.SETUP) { setup(chips, S.t("ابدأ")); return; }
        boolean end = screen == Screen.END;
        // the rows of tiles, at the start side, the whole height
        int rows = game.tries;
        tile = Math.min((864f - (rows - 1) * GAP) / rows, (GRID_W - 4f * GAP) / 5); gridW = 5 * tile + 4 * GAP;
        rowViews = new java.util.ArrayList[rows];
        for (int r = 0; r < rows; r++) drawRow(r);
        // the end side: a line of words, then the keyboard (or, at the end, the word and the buttons)
        float x = 96 + GRID_W + 64, w = 1728 - GRID_W - 64;
        keyX = x; keyW = w;
        root.addView(text(gameName(), 64, L.ink, true), at(w, 88, x, 96));
        if (end) { drawEnd(x, w); return; }
        lineView = text("", 48, L.ink, true);
        root.addView(lineView, at(w, 72, x, 196));
        refreshLine();
        keyView = new View[Word5Keys.KEYS];
        for (int k = 0; k < Word5Keys.KEYS; k++) drawKey(k);
        drawHints();
    }

    private float tile, gridW, keyX, keyW;
    private java.util.List<View>[] rowViews;
    private View[] keyView = new View[Word5Keys.KEYS];
    private TextView lineView; private View hintsRow;

    /** row r of the tiles (the typed one with its cursor), replacing what was there */
    private void drawRow(int r) {
        boolean end = screen == Screen.END;
        if (rowViews[r] != null) for (View v : rowViews[r]) root.removeView(v);
        rowViews[r] = new java.util.ArrayList<>();
        String shown = r < game.row() ? game.guess(r) : r == game.row() && !end ? game.typing() : "";
        int[] cs = r < game.row() ? game.colours(r) : null;
        boolean current = r == game.row() && !end;
        for (int i = 0; i < 5; i++) {
            final FrameLayout t = cs != null ? stamp(colour(cs[i]), L.ink, 6, false) : stamp(L.tile, current ? L.ink : L.ink(0x59), current && i == shown.length() ? 12 : 6, false);
            if (i < shown.length()) t.addView(centered(String.valueOf(shown.charAt(i)), tile * .62f, cs == null ? L.ink : cs[i] == Word5Game.GREEN ? L.onDeep : cs[i] == Word5Game.YELLOW ? L.onSaff : L.onInk, true), new FrameLayout.LayoutParams(-1, -1));
            root.addView(t, at(tile, tile, 96 + col(i, tile, gridW, 5), 96 + r * (tile + GAP)));
            rowViews[r].add(t);
            if (current && shake && animate) t.animate().translationX(px(16)).setDuration(60).withEndAction(() -> t.animate().translationX(-px(16)).setDuration(90).withEndAction(() -> t.animate().translationX(0).setDuration(60)));
        }
    }

    /** key k (a letter, one of ة ى ء, «إدخال» or «حذف») with or without the focus, replacing what was there */
    private void drawKey(int k) {
        if (screen != Screen.PLAY || keyView == null || k < 0 || k >= keyView.length) return;
        if (keyView[k] != null) root.removeView(keyView[k]);
        float kw = (keyW - 6 * GAP) / 7, y = KEYS_Y + (k < Word5Keys.LETTERS ? k / Word5Keys.COLS : 4) * (KEY + GAP);
        boolean focus = keys.key() == k;
        View v; float width = kw, at;
        if (k == Word5Keys.ENTER || k == Word5Keys.DELETE) {   // columns 3 and 4, and 5 and 6
            width = 2 * kw + GAP;
            int first = k == Word5Keys.ENTER ? 3 : 5;
            at = Math.min(col(first, kw, keyW, 7), col(first + 1, kw, keyW, 7));
            v = button(k == Word5Keys.ENTER ? S.t("إدخال") : S.t("حذف"), focus);
        } else {
            int c = game.key(letterOf(k));
            FrameLayout key = stamp(focus ? L.ink : c == 0 ? L.tile : colour(c), L.ink, focus && c != 0 ? 12 : 6, false);
            int wordsColour = focus ? (c == Word5Game.GREEN ? L.ok : c == Word5Game.YELLOW ? L.saff : L.onInk) : c == 0 ? L.ink : c == Word5Game.GREEN ? L.onDeep : c == Word5Game.YELLOW ? L.onSaff : L.onInk;
            TextView lt = centered(k < Word5Keys.LETTERS ? String.valueOf(Word5Text.ALPHABET.charAt(k)) : String.valueOf(EXTRA.charAt(k - Word5Keys.TAA)), 72, wordsColour, true);
            if (focus && c == Word5Game.GRAY) lt.setAlpha(.55f);
            key.addView(lt, new FrameLayout.LayoutParams(-1, -1));
            v = key;
            at = col(k < Word5Keys.LETTERS ? k % Word5Keys.COLS : k - Word5Keys.TAA, kw, keyW, 7);
        }
        keyView[k] = v;
        root.addView(v, at(width, KEY, keyX + at, y));
    }

    private void refreshLine() {
        if (lineView == null) return;
        lineView.setText(message.isEmpty() ? S.t("المحاولة %s من %s", num(Math.min(game.row() + 1, game.tries)), num(game.tries)) : message);
        lineView.setTextColor(message.isEmpty() ? L.ink(0xCC) : L.pink);
    }

    private void drawHints() {
        if (hintsRow != null) root.removeView(hintsRow);
        hints(S.t("الأسهم"), S.t("الحرف"), S.t("اختيار"), S.t("اكتب"), S.t("رجوع"), game.typing().isEmpty() ? S.t("توقف") : S.t("حذف"));
        hintsRow = root.getChildAt(root.getChildCount() - 1);
    }

    /** a letter typed or deleted: the typed row, the line and the last hint, nothing else */
    private void typedChanged() {
        drawRow(game.row());
        refreshLine();
        drawHints();
    }

    private void drawEnd(float x, float w) {
        boolean won = game.won();
        root.addView(text(won ? S.t("أحسنتم!") : S.t("الكلمة كانت"), 56, won ? L.ok : L.ink(0xCC), true), at(w, 80, x, 208));
        FrameLayout word = stamp(won ? L.ok : L.deep, L.ink, 6, false);
        word.addView(centered(answer.display, 168, L.onDeep, true), new FrameLayout.LayoutParams(-1, -1));
        root.addView(word, at(w, 260, x, 304));
        if (en && !answer.en.isEmpty()) root.addView(text(answer.en, 48, L.ink(0xCC), false), at(w, 64, x, 580));
        String[] labels = endLabels();
        for (int i = 0; i < 3; i++) root.addView(button(labels[i], sel == i), at(640, 88, x, 672 + i * 96));
        if (won && confetti) root.addView(new QuizActivity.ConfettiView(this), new FrameLayout.LayoutParams(-1, -1));
        hints("▲ ▼", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"));
    }
}
