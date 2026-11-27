package sa.urtv.home.pair;

import java.util.ArrayList;
import java.util.List;

/** For tests/pair_pool_test.py: ConnPool's rules, one "name<TAB>got" line per check. */
public final class ConnPoolRun {
    public static void main(String[] a) {
        List<String> closed = new ArrayList<>();
        ConnPool<String> p = new ConnPool<>(2, closed::add);
        p.add("h1", "c1"); p.add("h2", "c2");
        System.out.println("two\t" + p.size() + " " + closed);
        p.add("h3", "c3");   // the caller of h1 died: h1 is closed and gone, h3 is in
        System.out.println("full evicts the oldest\t" + p.size() + " " + closed + " " + p.get("h1") + " " + p.get("h3"));
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
