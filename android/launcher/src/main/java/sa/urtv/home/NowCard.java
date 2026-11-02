package sa.urtv.home;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import sa.urtv.home.tilawah.NowPlaying;

/**
 * «يُتلى الآن»: the card at the right end of the pills row while a surah or a station is on. HEIGHT tall, 10 dp more than a pill, hanging
 * 5 dp above and below the row (MainActivity's negative margins), so the pills keep their places; a play/pause mark, the surah or station
 * on its first line at a pill's name size (14 sp) and who recites it (and the sleep timer) on its second (12 sp); a long name ends in «…»,
 * the size never shrinks. Measured in Thmanyah: «سورة الشعراء» 73 dp, the longest station 169 dp, the longest reciter with a timer 184
 * dp: LINE_MAX holds them, and the card is then at most 250 dp wide, no wider than the old one at its widest real text (252). The
 * focused hint (262 dp) gets HINT_MAX: a focused card means no pill is open, so the row is still narrower than before at its widest.
 * It takes the focus like a pill; then the second line says what the keys do. MainActivity binds it from NowPlaying and does the keys.
 */
final class NowCard extends LinearLayout {
    private static final int ON = 0xE6FFFFFF, FOCUSED = 0xFF111111, GOLD = 0xFFD4AF6A, DIM = 0xFFC6BBA4;
    /** dp: the card's height (a pill is Pill.HEIGHT, 34), and how wide a line may be before it ends in «…» */
    static final int HEIGHT = 44, LINE_MAX = 200, HINT_MAX = 270;
    /** sp: the first line as a pill's name, the second smaller */
    static final float TITLE_SP = 14, SUB_SP = 12;

    private final GradientDrawable bg = new GradientDrawable();
    private final Drawable play, pause;
    private final ImageView mark;
    private final TextView title, sub;
    private String subText = "", hintText = "";
    private boolean playing;

    NowCard(Context c, Typeface strong, Typeface plain, Drawable playIcon, Drawable pauseIcon) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(true);
        setPadding(Math.round(12 * d), 0, Math.round(14 * d), 0);
        bg.setCornerRadius(HEIGHT / 2f * d);
        bg.setColor(0x66000000);
        bg.setStroke(Math.round(d), 0x66D4AF6A);
        setBackground(bg);

        play = playIcon.mutate();
        pause = pauseIcon.mutate();
        mark = new ImageView(c);
        addView(mark, new LayoutParams(Math.round(16 * d), Math.round(16 * d)));

        LinearLayout words = new LinearLayout(c);
        words.setOrientation(VERTICAL);
        title = line(c, strong, TITLE_SP, d);
        sub = line(c, plain, SUB_SP, d);
        words.addView(title, new LayoutParams(-2, -2));
        LayoutParams sp = new LayoutParams(-2, -2);
        sp.setMargins(0, Math.round(2 * d), 0, 0);
        words.addView(sub, sp);
        LayoutParams wp = new LayoutParams(-2, -2);
        wp.setMargins(0, 0, Math.round(8 * d), 0);   // the gap to the mark, on the words' right
        addView(words, wp);
        paint(false);
    }

    private static TextView line(Context c, Typeface face, float sp, float d) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(face);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setIncludeFontPadding(false);
        t.setMaxWidth(Math.round(LINE_MAX * d));   // the row stays clear of the greeting block at the right (see MainActivity)
        t.setGravity(Dir.start());
        return t;
    }

    /** what the card says now (MainActivity, on a change and once a minute for the sleep timer) */
    void bind(NowPlaying.State s) {
        playing = s.playing;
        title.setText(Digits.of(s.title));
        subText = Digits.of(s.sub);
        hintText = Digits.of(s.hint);
        setContentDescription(Digits.of(S.t("يُتلى الآن، %s، %s", s.title, s.sub)));
        paint(hasFocus());
    }

    @Override protected void onFocusChanged(boolean gained, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gained, direction, previouslyFocusedRect);
        paint(gained);
    }

    private void paint(boolean focused) {
        bg.setColor(focused ? 0xF2FFFFFF : 0x66000000);
        title.setTextColor(focused ? FOCUSED : ON);
        sub.setTextColor(focused ? FOCUSED : DIM);
        sub.setMaxWidth(Math.round((focused ? HINT_MAX : LINE_MAX) * getResources().getDisplayMetrics().density));
        sub.setText(focused ? hintText : subText);   // the keys, while it has the focus
        Drawable icon = playing ? pause : play;   // the mark says what OK does: pause while it plays, play while it is paused
        icon.setTint(focused ? FOCUSED : GOLD);
        mark.setImageDrawable(icon);
    }
}
