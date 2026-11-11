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

    @Override protected void onEndRound() { screen = Screen.SETUP; }

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
        if (back) { if (game.delete()) { message = ""; redraw(); } else openPause(); return; }
        if (!ok) {
            // the keys are Arabic: «next» is to the left in both languages, and dx is already turned toward the end of the interface's line
            keys.move(dy < 0 ? Word5Keys.UP : dy > 0 ? Word5Keys.DOWN : (dx > 0) == rtl ? Word5Keys.NEXT : Word5Keys.PREV);
            redraw();
            return;
        }
        int k = keys.key();
        if (k == Word5Keys.DELETE) { game.delete(); message = ""; }
        else if (k != Word5Keys.ENTER) {
            if (game.type(k)) { message = ""; if (game.typing().length() == Word5Text.LENGTH) keys.set(Word5Keys.ENTER); }   // a full row: the next OK enters it
            else play(QuizSound.LOW);
        } else {
            Word5Game.Entered e = game.enter();
            if (e == Word5Game.Entered.SHORT) { message = S.t("الكلمة خمسة حروف"); play(QuizSound.LOW); }
            else if (e == Word5Game.Entered.UNKNOWN) { message = S.t("ما نعرف هذي الكلمة"); shake = true; keys.set(Word5Keys.DELETE); play(QuizSound.LOW); }
            else if (e == Word5Game.Entered.NEXT) { message = ""; keys.set(0); play(QuizSound.CHIME); }
            else { screen = Screen.END; sel = 0; play(e == Word5Game.Entered.WON ? QuizSound.FANFARE : QuizSound.LOW); }
        }
        redraw();
    }

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
        float tile = Math.min((864f - (rows - 1) * GAP) / rows, (GRID_W - 4f * GAP) / 5), gridW = 5 * tile + 4 * GAP;
        for (int r = 0; r < rows; r++) {
            String shown = r < game.row() ? game.guess(r) : r == game.row() && !end ? game.typing() : "";
            int[] cs = r < game.row() ? game.colours(r) : null;
            boolean current = r == game.row() && !end;
            for (int i = 0; i < 5; i++) {
                FrameLayout t = cs != null ? stamp(colour(cs[i]), L.ink, 6, false) : stamp(L.tile, current ? L.ink : L.ink(0x59), current && i == shown.length() ? 12 : 6, false);
                if (i < shown.length()) t.addView(centered(String.valueOf(shown.charAt(i)), tile * .62f, cs == null ? L.ink : cs[i] == Word5Game.GREEN ? L.onDeep : cs[i] == Word5Game.YELLOW ? L.onSaff : L.onInk, true), new FrameLayout.LayoutParams(-1, -1));
                root.addView(t, at(tile, tile, 96 + col(i, tile, gridW, 5), 96 + r * (tile + GAP)));
                if (current && shake && animate) t.animate().translationX(px(16)).setDuration(60).withEndAction(() -> t.animate().translationX(-px(16)).setDuration(90).withEndAction(() -> t.animate().translationX(0).setDuration(60)));
            }
        }
        // the end side: a line of words, then the keyboard (or, at the end, the word and the buttons)
        float x = 96 + GRID_W + 64, w = 1728 - GRID_W - 64;
        root.addView(text(gameName(), 64, L.ink, true), at(w, 88, x, 96));
        if (end) { drawEnd(x, w); return; }
        String line = message.isEmpty() ? S.t("المحاولة %s من %s", num(Math.min(game.row() + 1, game.tries)), num(game.tries)) : message;
        root.addView(text(line, 48, message.isEmpty() ? L.ink(0xCC) : L.pink, true), at(w, 72, x, 196));
        float kw = (w - 6 * GAP) / 7;
        for (int k = 0; k < Word5Keys.ENTER; k++) {
            boolean focus = keys.key() == k; int c = game.key(k);
            FrameLayout key = stamp(focus ? L.ink : c == 0 ? L.tile : colour(c), L.ink, focus && c != 0 ? 12 : 6, false);
            int wordsColour = focus ? (c == Word5Game.GREEN ? L.ok : c == Word5Game.YELLOW ? L.saff : L.onInk) : c == 0 ? L.ink : c == Word5Game.GREEN ? L.onDeep : c == Word5Game.YELLOW ? L.onSaff : L.onInk;
            TextView lt = centered(String.valueOf(Word5Text.ALPHABET.charAt(k)), 72, wordsColour, true);
            if (focus && c == Word5Game.GRAY) lt.setAlpha(.55f);
            key.addView(lt, new FrameLayout.LayoutParams(-1, -1));
            root.addView(key, at(kw, KEY, x + col(k % Word5Keys.COLS, kw, w, 7), KEYS_Y + (k / Word5Keys.COLS) * (KEY + GAP)));
        }
        float wideEnter = 4 * kw + 3 * GAP, wideDelete = 3 * kw + 2 * GAP, y = KEYS_Y + 4 * (KEY + GAP);
        // «إدخال» under columns 0 to 3 (the right), «حذف» under 4 to 6
        root.addView(button(S.t("إدخال"), keys.key() == Word5Keys.ENTER), at(wideEnter, KEY, x + (rtl ? 0 : w - wideEnter), y));
        root.addView(button(S.t("حذف"), keys.key() == Word5Keys.DELETE), at(wideDelete, KEY, x + (rtl ? w - wideDelete : 0), y));
        hints(S.t("الأسهم"), S.t("الحرف"), S.t("اختيار"), S.t("اكتب"), S.t("رجوع"), game.typing().isEmpty() ? S.t("توقف") : S.t("حذف"));
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
