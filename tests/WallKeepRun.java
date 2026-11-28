package sa.urtv.home;

/** For tests/wall_keep_test.py: «keep LEVEL» or «reuse kept due scenes» (0/1 each), one answer (1 or 0) per line. */
public final class WallKeepRun {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("keep")) System.out.println(WallKeep.keep(Integer.parseInt(args[++i])) ? 1 : 0);
            else System.out.println(WallKeep.reuse(args[++i].equals("1"), args[++i].equals("1"), args[++i].equals("1")) ? 1 : 0);
        }
    }
}
