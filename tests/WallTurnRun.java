package sa.urtv.home;

/** For tests/wall_turn_test.py: each argument «at,now,interval» printed as «due left» (WallTurn), one per line. */
public final class WallTurnRun {
    public static void main(String[] args) {
        for (String a : args) {
            String[] f = a.split(",");
            long at = Long.parseLong(f[0]), now = Long.parseLong(f[1]), iv = Long.parseLong(f[2]);
            System.out.println((WallTurn.due(at, now, iv) ? 1 : 0) + " " + WallTurn.left(at, now, iv));
        }
    }
}
