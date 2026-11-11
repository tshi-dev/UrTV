package sa.urtv.screensaver;

import android.content.Context;

/**
 * The colours of the family games, the «ورقة» look (design/43 quiz-B*, docs/GAMES-KIT.md): paper, ink, deep green, saffron, a coral for «wrong» and the
 * last seconds, a green for «right». Two sets: PAPER (day; the approved look, the one «الأسرع» has always had) and NIGHT (the same paper at night: ink
 * ground, bone words, the same green block and saffron). A game never writes a colour of its own: it reads `L.paper`, `L.ink`… from its base class.
 * Which set a TV shows is decided in ONE place, `of`.
 */
final class GameLook {
    final boolean night;
    /** the ground, the words and lines, a raised tile, the question block, the block's quiet ticks */
    final int paper, ink, tile, deep, tickOff;
    /** saffron (picked, the timer), what is written on saffron, on the green block and on an ink-filled (focused) shape */
    final int saff, onSaff, onDeep, onInk;
    /** right, wrong / hurry */
    final int ok, pink;

    private GameLook(boolean night, int paper, int ink, int tile) {
        this.night = night; this.paper = paper; this.ink = ink; this.tile = tile;
        deep = 0xFF0E5A45; tickOff = 0xFF0A4A39; saff = 0xFFF2A93B; onSaff = 0xFF16140F; onDeep = 0xFFFFFFFF; onInk = paper; ok = 0xFF17A673; pink = 0xFFE4572E;
    }

    static final GameLook PAPER = new GameLook(false, 0xFFEFE8DA, 0xFF16140F, 0xFFFBF8F1), NIGHT = new GameLook(true, 0xFF16140F, 0xFFEFE8DA, 0xFF26221B);

    /** the sides' colours (teams or players): the eight of «الأسرع», then two more for a tenth player */
    static final int[] SIDES = {0xFF5AA9F0, 0xFFF2705A, 0xFF7BD88F, 0xFFC28BFF, 0xFFFFD15C, 0xFF4FD1C5, 0xFFFFB86B, 0xFF8FD6FF, 0xFFE58FB8, 0xFFB7C76A};

    /** the ink at an alpha 0..255 (quiet words, dashed lines) */
    int ink(int alpha) { return (ink & 0x00FFFFFF) | (alpha << 24); }

    /**
     * The set for this TV. Today every UrTV look, dark or light, plays on PAPER: it is the look the owner approved and the one «الأسرع» ships with, so the
     * games are one family. NIGHT is ready and drawn by the same code; switching a look to it is this one line.
     */
    static GameLook of(Context c) { return PAPER; }
}
