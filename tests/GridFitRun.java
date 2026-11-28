package sa.urtv.home;

/** For tests/card_focus_grid_test.py: each argument «room,cell,gap,padTop,padBottom» printed as GridFit.height, one per line. */
public final class GridFitRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] f = a.split(",");
            System.out.println(GridFit.height(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4])));
        }
    }
}
