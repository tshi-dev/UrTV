package sa.urtv.home.pair;

import java.util.ArrayList;
import java.util.List;

/** For tests/pair_pool_test.py: ConnPool's rules, one "name<TAB>got" line per check. */
public final class ConnPoolRun {
    public static void main(String[] a) {
        List<String> closed = new ArrayList<>();
        ConnPool<String> p = new ConnPool<>(2, closed::add);
        System.out.println("two\t" + p.add("h1", "c1") + " " + p.add("h2", "c2") + " " + p.size() + " " + closed);
        System.out.println("full evicts the oldest idle\t" + p.add("h3", "c3") + " " + p.size() + " " + closed + " " + p.use("h1") + " " + p.use("h3"));
        p.done("h3");
        List<String> c2 = new ArrayList<>();
        ConnPool<String> q = new ConnPool<>(2, c2::add);
        q.add("a", "ca"); q.add("b", "cb");
        q.use("a"); q.use("b");   // both in a call
        System.out.println("all in a call: refused, nothing closed\t" + q.add("c", "cc") + " " + q.size() + " " + c2);
        q.done("b");
        System.out.println("an idle one goes (the oldest idle, not the one in a call)\t" + q.add("c", "cc") + " " + q.size() + " " + c2 + " " + q.use("a") + " " + q.use("b"));
        for (int i = 0; i < 20; i++) p.add("x" + i, "cx" + i);
        System.out.println("never over max\t" + p.size());
        p.remove("x19");
        System.out.println("remove closes\t" + p.size() + " " + closed.contains("cx19"));
        p.remove("nope"); p.remove(null);
        System.out.println("unknown ignored\t" + p.size());
        p.clear();
        System.out.println("clear\t" + p.size() + " " + closed.contains("cx18"));
    }
}
