package sa.urtv.home.tilawah;

import android.media.MediaDataSource;

import java.io.IOException;

/** A live station's audio for MediaPlayer.setDataSource(MediaDataSource): the bytes come from LiveFeed (size unknown, nothing to seek). */
final class LiveSource extends MediaDataSource {
    private final LiveFeed feed;

    LiveSource(LiveFeed feed) { this.feed = feed; }

    @Override public int readAt(long position, byte[] buffer, int offset, int size) throws IOException { return feed.readAt(position, buffer, offset, size, 15_000); }

    @Override public long getSize() { return -1; }

    @Override public void close() { feed.close(); }
}
