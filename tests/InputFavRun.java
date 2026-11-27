package sa.urtv.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** For tests/input_fav_test.py: InputFav's rules, one "name<TAB>got" line per check. */
public final class InputFavRun {
    public static void main(String[] args) {
        StringBuilder o = new StringBuilder();
        String id = "com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128";
        List<String> dock = new ArrayList<>(Arrays.asList("com.netflix.ninja", "net.mbc.shahidTV"));
        o.append("token\t").append(InputFav.token(id)).append('\n');
        o.append("isInput token\t").append(InputFav.isInput(InputFav.token(id))).append('\n');
        o.append("isInput app\t").append(InputFav.isInput("com.netflix.ninja")).append('\n');
        o.append("isInput bare prefix\t").append(InputFav.isInput("@input:")).append('\n');
        o.append("idOf\t").append(InputFav.idOf(InputFav.token(id))).append('\n');
        List<String> added = InputFav.toggle(dock, id, 10);
        o.append("added\t").append(String.join(",", added)).append('\n');
        o.append("has\t").append(InputFav.has(added, id)).append('\n');
        o.append("removed\t").append(String.join(",", InputFav.toggle(added, id, 10))).append('\n');
        o.append("full\t").append(InputFav.toggle(dock, id, 2).size()).append('\n');
        o.append("comma refused\t").append(InputFav.toggle(dock, "a,b", 10).size()).append('\n');
        o.append("original untouched\t").append(dock.size()).append('\n');
        List<String> kept = InputFav.keepInputs(added, Arrays.asList("a", "b"), 10);
        o.append("keep inputs\t").append(String.join(",", kept)).append('\n');
        o.append("keep inputs full\t").append(String.join(",", InputFav.keepInputs(added, Arrays.asList("a", "b"), 2))).append('\n');
        o.append("keep inputs none\t").append(String.join(",", InputFav.keepInputs(dock, Arrays.asList("a"), 10))).append('\n');
        System.out.print(o);
    }
}
