package sa.urtv.home.tilawah;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import sa.urtv.home.R;

/** Now playing. The whole screen is the control surface: OK, arrows, down for the sleep timer. */
public class PlayerActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Catalog cat;
    private TextView kind, title, artist, live, pos, dur, sleepInfo, hint;
    private final SeekPace seekPace = new SeekPace();   // a held left/right: one seek per 250 ms
    private View progressBlock, prev, next;
    private ProgressBar progress;
    private ImageView play;
    private long openedAt;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            render();
            handler.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(Bundle state) {
        setTheme(Ui.theme(this));   // «المظهر»: the look and the light of now, before anything is drawn
        super.onCreate(state);
        sa.urtv.home.Digits.init(this);   // the screensaver's «الأرقام» setting
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);
        Ui.glassPage(this, R.id.bg);   // the glass page
        cat = Catalog.get(this);
        openedAt = SystemClock.elapsedRealtime();

        kind = findViewById(R.id.kind);
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        live = findViewById(R.id.live);
        pos = findViewById(R.id.pos);
        dur = findViewById(R.id.dur);
        sleepInfo = findViewById(R.id.sleepInfo);
        progressBlock = findViewById(R.id.progressBlock);
        progress = findViewById(R.id.progress);
        play = findViewById(R.id.play);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
        hint = findViewById(R.id.hint);

        Ui.text(kind, "Thmanyah-Medium.otf", 15, R.color.gold);
        Ui.text(title, "ThmanyahDisplay-Bold.otf", 46, R.color.sand);
        Ui.text(artist, "Thmanyah-Regular.otf", 22, R.color.dim);
        Ui.text(live, "Thmanyah-Medium.otf", 14, R.color.gold);
        Ui.text(pos, "Thmanyah-Regular.otf", 14, R.color.dim);
        Ui.text(dur, "Thmanyah-Regular.otf", 14, R.color.dim);
        Ui.text(sleepInfo, "Thmanyah-Medium.otf", 14, R.color.gold);
        Ui.text(hint, "Thmanyah-Regular.otf", 14, R.color.dim);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
    }

    private void render() {
        int source = MusicService.currentSource();
        if (source == Prefs.SOURCE_NONE) {
            if (SystemClock.elapsedRealtime() - openedAt > 4000) finish();
            return;
        }

        boolean adhan = source == Prefs.SOURCE_ADHAN;
        boolean radio = source == Prefs.SOURCE_RADIO;
        // an adhan is not a track: no skipping and no seeking, only stop/play and the sleep timer
        prev.setVisibility(adhan ? View.GONE : View.VISIBLE);
        next.setVisibility(adhan ? View.GONE : View.VISIBLE);
        hint.setText(adhan ? R.string.hint_adhan : R.string.hint_player);
        if (radio) {
            int index = MusicService.currentStation();
            if (index >= 0 && index < cat.stations.size()) title.setText(cat.stations.get(index).name);
            artist.setText("");
            kind.setVisibility(View.GONE);
            live.setVisibility(View.VISIBLE);
            progressBlock.setVisibility(View.GONE);
        } else if (source == Prefs.SOURCE_ADHAN) {
            title.setText(R.string.adhan);
            artist.setText(MusicService.adhanName() != null ? MusicService.adhanName() : "");
            kind.setVisibility(View.GONE);
            live.setVisibility(View.GONE);
            progressBlock.setVisibility(View.GONE);
        } else {
            title.setText("سورة " + cat.surahName(MusicService.currentSurah()));
            int index = MusicService.reciterIndex();
            if (index >= 0 && index < cat.reciters.size()) artist.setText(cat.reciters.get(index).name);
            kind.setText(R.string.now_playing);
            kind.setVisibility(View.VISIBLE);
            live.setVisibility(View.GONE);
            progressBlock.setVisibility(View.VISIBLE);

            int duration = MusicService.currentDuration();
            int position = MusicService.currentPosition();
            pos.setText(Ui.time(position));
            dur.setText(duration > 0 ? Ui.time(duration) : "--:--");
            progress.setProgress(duration > 0 ? (int) Math.min(1000L, 1000L * position / duration) : 0);
        }

        String error = MusicService.currentError();
        if (error != null) artist.setText(error);
        else if (MusicService.isBuffering()) artist.setText(R.string.buffering);
        play.setImageResource(MusicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

        long left = MusicService.sleepAt() - System.currentTimeMillis();
        if (left > 0) {
            sleepInfo.setText(getString(R.string.sleep_left, Ui.arabic((int) Math.ceil(left / 60000.0))));
            sleepInfo.setVisibility(View.VISIBLE);
        } else {
            sleepInfo.setVisibility(View.GONE);
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (event.getRepeatCount() == 0) MusicService.toggle(this);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (event.getRepeatCount() == 0) MusicService.toggle(this);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                MusicService.resume(this);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                MusicService.pause(this);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (adhanPlaying()) return true;
                if (seekPace.due(event.getRepeatCount(), event.getEventTime())) MusicService.seekBy(this, 10_000);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (adhanPlaying()) return true;
                if (seekPace.due(event.getRepeatCount(), event.getEventTime())) MusicService.seekBy(this, -10_000);
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (adhanPlaying()) return true;
                MusicService.nextAction(this);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (adhanPlaying()) return true;
                MusicService.prevAction(this);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.getRepeatCount() == 0) showSleep();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private boolean adhanPlaying() {
        return MusicService.currentSource() == Prefs.SOURCE_ADHAN;
    }

    private void showSleep() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sleep);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.62f);
            window.setLayout((int) (getResources().getDisplayMetrics().density * 620),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Ui.text((TextView) dialog.findViewById(R.id.sleep_title), "ThmanyahDisplay-Bold.otf", 27, R.color.sand);
        Ui.text((TextView) dialog.findViewById(R.id.sleep_subtitle), "Thmanyah-Regular.otf", 14, R.color.dim);

        LinearLayout items = dialog.findViewById(R.id.sleep_items);
        final String[] labels = {
                Ui.digits(getString(R.string.sleep_30)), Ui.digits(getString(R.string.sleep_60)), Ui.digits(getString(R.string.sleep_90)),
                getString(R.string.sleep_off), getString(R.string.stop_now)
        };
        for (int i = 0; i < labels.length; i++) {
            TextView row = (TextView) getLayoutInflater().inflate(R.layout.item_sleep, items, false);
            row.setText(labels[i]);
            Ui.text(row, "Thmanyah-Medium.otf", 20, i == 4 ? R.color.red : R.color.sand);
            final int which = i;
            row.setOnClickListener(v -> {
                applySleep(which);
                dialog.dismiss();
            });
            items.addView(row);
        }

        dialog.setOnShowListener(d -> {
            View first = items.getChildAt(0);
            if (first != null) first.requestFocus();
        });
        dialog.show();
    }

    private void applySleep(int which) {
        switch (which) {
            case 0:
            case 1:
            case 2: {
                int minutes = new int[]{30, 60, 90}[which];
                MusicService.sleep(this, minutes);
                Toast.makeText(this, getString(R.string.sleep_left, Ui.arabic(minutes)),
                        Toast.LENGTH_SHORT).show();
                break;
            }
            case 3:
                MusicService.sleep(this, 0);
                break;
            case 4:
                MusicService.stopAction(this);
                finish();
                break;
            default:
                break;
        }
    }
}
