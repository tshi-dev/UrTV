package sa.urtv.screensaver;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.*;

/** For tests/quiz_e2e.test.mjs: the real room and server on the loopback with the real phone page, driven by lines on stdin (start, reveal, next, finish, close). Prints PORT n. */
public final class QuizServe {
    public static void main(String[] a) throws Exception {
        final byte[] page = Files.readAllBytes(Paths.get(a[0]));
        QuizRoom room = new QuizRoom("4321", System::currentTimeMillis, new Random(1));
        QuizServer srv = new QuizServer(room, new QuizServer.Assets() {
            public byte[] page() { return page; }
            public byte[] font(String n) { return null; }
        }, true);
        System.out.println("PORT " + srv.start(InetAddress.getLoopbackAddress()));
        System.out.flush();
        List<QuizRoom.Q> qs = new ArrayList<>();
        qs.add(new QuizRoom.Q("كم عدد أضلاع المسدّس؟", new String[]{"أربعة", "خمسة", "ستة", "ثمانية"}, 2));
        qs.add(new QuizRoom.Q("Second?", new String[]{"a", "b", "c", "d"}, 0));
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String l;
        while ((l = in.readLine()) != null) {
            switch (l.trim()) {
                case "start": System.out.println(room.start(qs) ? "ok" : "no"); break;
                case "reveal": room.reveal(); System.out.println("ok"); break;
                case "next": room.skipReveal(); System.out.println("ok"); break;
                case "finish": room.finish(); System.out.println("ok"); break;
                case "close": room.close(); System.out.println("ok"); break;
                case "players": System.out.println("players " + room.players().size()); break;
                default: System.out.println("?"); break;
            }
            System.out.flush();
        }
        srv.stop();
    }
}
