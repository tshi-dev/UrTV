package sa.urtv.home.tilawah;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.4 «صوتياتي» for the phone page (sa.urtv.home.phone): the list, a file in pieces, and what a file is used for. Answers are fixed codes and the
 * person's own names; no path, no address. The upload itself is AudioStore's (one at a time, checked before the first byte is kept).
 */
public final class AudioBridge {
    private AudioBridge() { }

    /** {"items":[{"id":3,"name":"…","size":"3.1 MB","use":"own"}], "big":false, "file_max":"20 MB", "total_max":"100 MB", "used":"12 MB"} */
    public static Map<String, Object> state(Context c) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Object> items = new ArrayList<>();
        for (AudioStore.Item it : AudioStore.list(c)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", (long) it.id);
            m.put("name", it.name);
            m.put("size", AudioRules.size(it.bytes));
            m.put("use", AudioUse.of(c, it.id));
            items.add(m);
        }
        boolean big = AudioStore.big(c);
        out.put("items", items);
        out.put("big", big);
        out.put("file_max", AudioRules.size(AudioRules.fileMax(big)));
        out.put("total_max", AudioRules.size(AudioRules.totalMax(big)));
        out.put("used", AudioRules.size(AudioStore.used(c)));
        out.put("chunk", (long) AudioRules.CHUNK);
        return out;
    }

    /** One piece of an upload (offset 0 starts a new one): "more", "done", or a refusal code for the page */
    public static String piece(Context c, long offset, long total, String name, byte[] data) {
        if (offset == 0) {
            String why = AudioStore.uploadStart(c, total, name);
            if (!why.isEmpty()) return why;
        }
        String r = AudioStore.uploadPiece(c, offset, data);
        return r.startsWith("done:") ? "done" : r;
    }

    /** op "use" (as: "own", "own_fajr", "none"), "delete"; null when done, else a code */
    public static String op(Context c, String op, long id, String as) {
        AudioStore.Item it = AudioStore.get(c, (int) id);
        if (it == null) return "not_found";
        if ("delete".equals(op)) { AudioUse.forget(c, it.id); AudioStore.remove(c, it.id); return null; }
        if ("use".equals(op)) return AudioUse.set(c, it, as);
        return "bad_op";
    }
}
