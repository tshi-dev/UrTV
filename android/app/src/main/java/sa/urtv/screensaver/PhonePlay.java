package sa.urtv.screensaver;

import android.content.Context;

import java.net.InetAddress;
import java.util.Random;

/**
 * «اللعب بالجوالات»: one room and its game-only server for the party games (docs/GAMES-PHONES.md). It is the same server and the same limits as «الأسرع»
 * (QuizServer: the local network only, a private address, small bodies, no route outside the game); only the page and the room are the party games'.
 * Opened when the person turns the switch on and presses start, closed with the game's screen (Back from the lobby, leaving the game). Nothing is kept.
 */
final class PhonePlay {
    final GamePhoneRoom room; final String url, code; private final QuizServer server;

    private PhonePlay(GamePhoneRoom room, QuizServer server, String url) { this.room = room; this.server = server; this.url = url; this.code = room.code; }

    /** null when the TV has no local network address or the server cannot open */
    static PhonePlay open(final Context ctx) {
        InetAddress lan = QuizActivity.lanAddress();
        if (lan == null) return null;
        Random rnd = new java.security.SecureRandom();   // the room code and the tokens are not guessable from a seed
        GamePhoneRoom room = new GamePhoneRoom(QuizRoom.freshCode(rnd, System.currentTimeMillis()), System::currentTimeMillis, rnd);
        QuizServer server = new QuizServer(room, new QuizServer.Assets() {
            @Override public byte[] page() { return asset(ctx, "quiz/game.html"); }
            @Override public byte[] font(String n) { return asset(ctx, "fonts/" + n + ".woff2"); }
        }, true);
        try {
            int port = server.start(lan);
            return new PhonePlay(room, server, "http://" + lan.getHostAddress() + ":" + port + "/" + room.code);
        } catch (java.io.IOException e) {
            server.stop(); room.close();
            return null;
        }
    }

    void close() { server.stop(); room.close(); }

    private static byte[] asset(Context c, String name) {
        try (java.io.InputStream in = c.getAssets().open(name)) { return Bounded.readAll(in, 4 << 20); } catch (Exception e) { return null; }
    }
}
