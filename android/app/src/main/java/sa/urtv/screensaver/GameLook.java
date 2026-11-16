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
    /** the sides' colours (teams or players): ten */
    final int[] sides;

    private GameLook(boolean night, int paper, int ink, int tile, int deep, int tickOff, int saff, int onSaff, int onDeep, int ok, int pink, int[] sides) {
        this.night = night; this.paper = paper; this.ink = ink; this.tile = tile; this.deep = deep; this.tickOff = tickOff; this.saff = saff; this.onSaff = onSaff; this.onDeep = onDeep;
        this.onInk = paper; this.ok = ok; this.pink = pink; this.sides = sides;
    }

    /** the sides' colours by day: the eight of «الأسرع», then two more for a tenth player */
    static final int[] SIDES = {0xFF5AA9F0, 0xFFF2705A, 0xFF7BD88F, 0xFFC28BFF, 0xFFFFD15C, 0xFF4FD1C5, 0xFFFFB86B, 0xFF8FD6FF, 0xFFE58FB8, 0xFFB7C76A};
    /** the same ten dimmed a step for the night (still ten different hues, each at least 5:1 on every night ground) */
    static final int[] SIDES_NIGHT = {0xFF6FA3D4, 0xFFDE7B66, 0xFF7FC08C, 0xFFB195DE, 0xFFE0C06A, 0xFF5BBDB3, 0xFFE0A56E, 0xFF8DBFDE, 0xFFD595B4, 0xFFAEBB72};

    /** the day look: the approved paper (design/43 quiz-B*) */
    static final GameLook PAPER = new GameLook(false, 0xFFEFE8DA, 0xFF16140F, 0xFFFBF8F1, 0xFF0E5A45, 0xFF0A4A39, 0xFFF2A93B, 0xFF16140F, 0xFFFFFFFF, 0xFF17A673, 0xFFE4572E, SIDES);
    /** the night looks (games/design/night-4-palettes.png, games/PLAN-NIGHT-PHONE.md): A warm charcoal (the default), B blue-black, C dim sepia; not pure black on white */
    static final GameLook[] NIGHTS = {
        new GameLook(true, 0xFF1C1A17, 0xFFECE4D4, 0xFF2B2824, 0xFF11503F, 0xFF0C3C30, 0xFFE3A33F, 0xFF1A160F, 0xFFF4EFE4, 0xFF22755A, 0xFFD9694C, SIDES_NIGHT),
        new GameLook(true, 0xFF12161C, 0xFFE6E9EE, 0xFF1E242C, 0xFF0F4D4A, 0xFF0B3A38, 0xFFE0A83E, 0xFF14120E, 0xFFF1F4F7, 0xFF217257, 0xFFD9694C, SIDES_NIGHT),
        new GameLook(true, 0xFF24201A, 0xFFE8DCC4, 0xFF332D25, 0xFF1F5A43, 0xFF174632, 0xFFD99A3A, 0xFF1A1409, 0xFFF3EBDA, 0xFF23785C, 0xFFD2694A, SIDES_NIGHT)};

    /** the ink at an alpha 0..255 (quiet words, dashed lines) */
    int ink(int alpha) { return (ink & 0x00FFFFFF) | (alpha << 24); }

    /**
     * The set for this TV, in ONE place: the night look (the palette the person chose) when «وضع ليلي» is on, or automatic and the sun is down at the TV's city
     * (GamePrefs.nightNow); else the paper. It is read when a screen opens, so the switch on the games list redraws that screen and every game opened after it.
     */
    static GameLook of(Context c) { return GamePrefs.nightNow(c) ? NIGHTS[GamePrefs.palette(c)] : PAPER; }
}
