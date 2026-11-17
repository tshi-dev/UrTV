package sa.urtv.home;

/** tests/options_fit_test.py: each argument is "width cardW widest screenW margin", "left centreX w screenW margin" or "row buttonH lineH room"; one answer a line */
public final class OptionsFitRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] p = a.split(" ");
            int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]), z = Integer.parseInt(p[3]);
            if (p[0].equals("width")) System.out.println(OptionsFit.width(x, y, z, Integer.parseInt(p[4])));
            else if (p[0].equals("left")) System.out.println(OptionsFit.left(x, y, z, Integer.parseInt(p[4])));
            else System.out.println(OptionsFit.rowHeight(x, y, z));
        }
    }
}
