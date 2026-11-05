package sa.urtv.home;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * What a person said to «المهام»: «أضف مهمة …» / «أضف …» adds, «خلصت …» / «تم …» finishes the best-matching
 * open task, «احذف …» deletes the best-matching task. No Android classes, so tests/task_voice_test.py runs it.
 * Words are compared the way the team search compares names (TeamSearch.java): harakat and tatweel gone,
 * أ إ آ ٱ -> ا, ة -> ه, ى ئ -> ي, ؤ -> و, the article ال dropped.
 */
final class TaskVoice {
    enum Kind { ADD, DONE, DELETE, NONE, NO_MATCH, AMBIGUOUS }

    static final class Result {
        final Kind kind;
        final String text;   // ADD: the new task; DONE/DELETE: the task found
        final int index;     // DONE/DELETE: its position in the list given, from 0
        Result(Kind kind, String text, int index) { this.kind = kind; this.text = text; this.index = index; }
    }

    private static final Pattern HARAKAT = Pattern.compile("[ً-ٰٟـ]");
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private TaskVoice() { }

    /** A task typed in the field (or dictated by the keyboard's mic): spaces tidied, cut to {@code max} characters; "" when there is nothing to add. */
    static String typed(String raw, int max) {
        String s = (raw == null ? "" : raw).replaceAll("\\s+", " ").trim();
        return s.length() > max ? s.substring(0, max).trim() : s;
    }

    /** The speech window came back at once with nothing and no result code at all: it never listened (Google TV's search), so the mic goes. */
    static boolean micDead(boolean cancelledEmpty, long millisOpen) {
        return cancelledEmpty && millisOpen >= 0 && millisOpen < 1000;
    }

    static String letters(String s) {   // أ إ آ ٱ -> ا, ة -> ه, ى ئ -> ي, ؤ -> و
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'أ': case 'إ': case 'آ': case 'ٱ': b.append('ا'); break;
                case 'ة': b.append('ه'); break;
                case 'ى': case 'ئ': b.append('ي'); break;
                case 'ؤ': b.append('و'); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    /** One word, normalised, without its ال. */
    static String word(String w) {
        String s = letters(HARAKAT.matcher(Normalizer.normalize(w, Normalizer.Form.NFKC)).replaceAll(""));
        s = s.toLowerCase(java.util.Locale.ROOT);
        return s.length() > 3 && s.startsWith("ال") ? s.substring(2) : s;
    }

    static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        java.util.regex.Matcher m = WORD.matcher(letters(HARAKAT.matcher(text == null ? "" : text).replaceAll("")));
        while (m.find()) out.add(word(m.group()));
        return out;
    }

    /** heard: what the recognizer returned; tasks/done: the list as stored (done tasks stay in it). */
    static Result understand(String heard, List<String> tasks, List<Boolean> done) {
        List<String> said = new ArrayList<>();
        for (String w : (heard == null ? "" : heard).trim().split("\\s+")) if (!w.isEmpty()) said.add(w);
        if (said.isEmpty()) return new Result(Kind.NONE, "", -1);
        String verb = word(said.get(0));
        int skip = 1;
        Kind kind;
        if (verb.equals("اضف")) kind = Kind.ADD;
        else if (verb.equals("خلصت") || verb.equals("تم")) kind = Kind.DONE;
        else if (verb.equals("احذف")) kind = Kind.DELETE;
        else return new Result(Kind.NONE, "", -1);
        if (said.size() > skip && word(said.get(skip)).equals("مهمه")) skip++;
        if (kind == Kind.DONE && said.size() > skip && word(said.get(skip)).equals("من")) skip++;
        StringBuilder rest = new StringBuilder();
        for (int k = skip; k < said.size(); k++) rest.append(rest.length() > 0 ? " " : "").append(said.get(k));
        if (rest.length() == 0) return new Result(Kind.NONE, "", -1);
        if (kind == Kind.ADD) return new Result(Kind.ADD, rest.toString(), -1);

        List<String> q = words(rest.toString());
        double best = 0;
        int at = -1;
        boolean tie = false;
        for (int k = 0; k < tasks.size(); k++) {
            if (kind == Kind.DONE && done.get(k)) continue;
            List<String> t = words(tasks.get(k));
            double hits = 0;   // a whole word counts 1, part of a word («بالمهندس» for «المهندس», «مكيفات» for «المكيف») .6
            for (String w : q) {
                double hit = 0;
                for (String x : t) {
                    if (x.equals(w)) hit = 1;
                    else if (w.length() >= 3 && x.contains(w)) hit = Math.max(hit, .6);
                }
                hits += hit;
            }
            double score = q.isEmpty() ? 0 : hits / q.size();
            if (score < 0.5) continue;
            if (score > best) { best = score; at = k; tie = false; }
            else if (score == best) tie = true;
        }
        if (at < 0) return new Result(Kind.NO_MATCH, rest.toString(), -1);
        if (tie) return new Result(Kind.AMBIGUOUS, rest.toString(), -1);
        return new Result(kind, tasks.get(at), at);
    }
}
