package sa.urtv.screensaver.games.oddone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** «واحد مو معنا»: the topics and their words, and which word a round gets (no Android, tested on a plain JVM: tests/PartyGamesTest.java).
 *  The file (assets/games/odd-one.json, made by .github/tools/make_party_words.py from games/odd-one/topics.json):
 *  {"v":1,"topics":[{"i":"farm","n":["حيوانات المزرعة","Farm animals"],"w":[["بقرة","cow",1], …]}, …]}.
 *  games/odd-one/SPEC.md: a word is never repeated inside one game; a topic that runs out hands over to another; and (ours) not one of the
 *  last RECENT_MAX words this TV played unless nothing else is left. */
public final class OddOneWords {
    private OddOneWords() {}

    public static final int RECENT_MAX = 120, TOPICS_MIN = 3, WORDS_MIN = 5, TEXT_MAX = 40;

    public static final class Word {
        public final String id, topic; public final String[] text; public final int diff;
        Word(String topic, String ar, String en, int diff) { this.id = topic + "/" + ar; this.topic = topic; this.text = new String[] {ar, en}; this.diff = diff; }
        public String text(boolean en) { return text[en ? 1 : 0]; }
    }
    public static final class Topic {
        public final String id; public final String[] name; public final List<Word> words = new ArrayList<>();
        Topic(String id, String ar, String en) { this.id = id; this.name = new String[] {ar, en}; }
        public String name(boolean en) { return name[en ? 1 : 0]; }
    }

    private static String str(Object o) {
        if (!(o instanceof String)) return null;
        String s = ((String) o).trim();
        return s.isEmpty() || s.length() > TEXT_MAX || s.indexOf('·') >= 0 || s.indexOf('•') >= 0 ? null : s;
    }

    /** the parsed JSON tree (Maps, Lists, Doubles, Strings: what MiniJson gives) → the topics; null when anything is not as the file must be (the whole file is refused) */
    public static List<Topic> parse(Object tree) {
        if (!(tree instanceof Map)) return null;
        Object ts = ((Map<?, ?>) tree).get("topics");
        if (!(ts instanceof List) || ((List<?>) ts).size() < TOPICS_MIN) return null;
        List<Topic> out = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (Object o : (List<?>) ts) {
            if (!(o instanceof Map)) return null;
            Map<?, ?> m = (Map<?, ?>) o;
            String id = str(m.get("i"));
            Object n = m.get("n"), w = m.get("w");
            if (id == null || !ids.add(id) || !(n instanceof List) || ((List<?>) n).size() != 2 || !(w instanceof List) || ((List<?>) w).size() < WORDS_MIN) return null;
            String ar = str(((List<?>) n).get(0)), en = str(((List<?>) n).get(1));
            if (ar == null || en == null) return null;
            Topic t = new Topic(id, ar, en); Set<String> seen = new HashSet<>();
            for (Object x : (List<?>) w) {
                if (!(x instanceof List) || ((List<?>) x).size() != 3) return null;
                List<?> l = (List<?>) x;
                String wa = str(l.get(0)), we = str(l.get(1));
                if (wa == null || we == null || !seen.add(wa) || !(l.get(2) instanceof Double)) return null;
                int d = (int) (double) (Double) l.get(2);
                if (d < 1 || d > 3) return null;
                t.words.add(new Word(id, wa, we, d));
            }
            out.add(t);
        }
        return out;
    }

    public static Topic topic(List<Topic> topics, String id) {
        for (Topic t : topics) if (t.id.equals(id)) return t;
        return null;
    }

    /** The next round's word. `topicId` null = «عشوائي» (a topic this game has not had, while one is left). In order: the wanted topics' unplayed words that are not
     *  recent; their recent ones, the oldest first; then the same of every other topic. Null only when every word of the file was played in this game. */
    public static Word pick(List<Topic> topics, String topicId, Set<String> usedWords, Set<String> usedTopics, List<String> recent, Random rnd) {
        List<Topic> wanted = new ArrayList<>(), rest = new ArrayList<>();
        for (Topic t : topics) ((topicId == null ? !usedTopics.contains(t.id) : t.id.equals(topicId)) ? wanted : rest).add(t);
        Word w = from(wanted, usedWords, recent, rnd);
        return w != null ? w : from(rest, usedWords, recent, rnd);
    }
    private static Word from(List<Topic> ts, Set<String> used, List<String> recent, Random rnd) {
        List<Word> fresh = new ArrayList<>(); Word oldest = null; int at = Integer.MAX_VALUE;
        for (Topic t : ts) for (Word w : t.words) {
            if (used.contains(w.id)) continue;
            int r = recent == null ? -1 : recent.indexOf(w.id);
            if (r < 0) fresh.add(w); else if (r < at) { at = r; oldest = w; }
        }
        return fresh.isEmpty() ? oldest : fresh.get(rnd.nextInt(fresh.size()));
    }

    /** the last words this TV played, the oldest first, at most `max`: `id` goes to the end */
    public static List<String> remember(List<String> recent, String id, int max) {
        List<String> out = new ArrayList<>(recent == null ? new ArrayList<String>() : recent);
        out.remove(id); out.add(id);
        while (out.size() > max) out.remove(0);
        return out;
    }
}
