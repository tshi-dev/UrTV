package sa.urtv.screensaver;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** «الأسرع»: our own four sounds, made here from plain tones (a tick for the last 5 seconds, a soft chime for a right answer, a low note for a miss, a short
 *  fanfare for the podium): no melody of any show, no audio file, nothing to download. They can be switched off («الصوت» in the game's chips). */
final class QuizSound {
    private QuizSound() {}

    static final int RATE = 22050;
    static final int TICK = 0, CHIME = 1, LOW = 2, FANFARE = 3;

    /** notes as {frequency Hz, milliseconds, volume 0..1}; a short fade at both ends so nothing clicks */
    static short[] make(int which) {
        double[][] notes = which == TICK ? new double[][]{{1500, 45, .5}}
            : which == CHIME ? new double[][]{{1046, 120, .5}, {1318, 260, .5}}
            : which == LOW ? new double[][]{{196, 320, .6}}
            : new double[][]{{523, 140, .5}, {659, 140, .5}, {784, 140, .5}, {1046, 420, .55}};
        int total = 0;
        for (double[] n : notes) total += (int) (RATE * n[1] / 1000);
        short[] out = new short[total];
        int at = 0;
        for (double[] n : notes) {
            int len = (int) (RATE * n[1] / 1000);
            for (int i = 0; i < len; i++) {
                double fade = Math.min(1, Math.min(i, len - 1 - i) / (RATE * 0.008));
                double decay = which == CHIME || which == LOW ? 1 - 0.6 * i / len : 1;
                out[at + i] = (short) (Math.sin(2 * Math.PI * n[0] * i / RATE) * n[2] * fade * decay * 12000);
            }
            at += len;
        }
        return out;
    }

    static void play(boolean on, int which) {
        if (!on) return;
        try {
            final short[] pcm = make(which);
            final AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(pcm.length * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
            t.write(pcm, 0, pcm.length);
            t.setNotificationMarkerPosition(pcm.length - 1);
            t.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override public void onMarkerReached(AudioTrack a) { a.release(); }
                @Override public void onPeriodicNotification(AudioTrack a) { }
            });
            t.play();
        } catch (RuntimeException ignored) {
            // no audio output: the game is as playable without
        }
    }
}
