package sa.urtv.home;

/** For tests/fragment_keep_test.py: FragmentKeep's choices, one per line. */
public class FragmentKeepRun {
    public static void main(String[] a) {
        System.out.println("save_new " + FragmentKeep.shouldSave("city=x", null));
        System.out.println("save_same " + FragmentKeep.shouldSave("city=x", "city=x"));
        System.out.println("save_none " + FragmentKeep.shouldSave(null, "city=x"));
        System.out.println("save_empty " + FragmentKeep.shouldSave("", null));
        System.out.println("choose_fresh " + FragmentKeep.choose("city=new", "city=old"));
        System.out.println("choose_kept " + FragmentKeep.choose(null, "city=old"));
        System.out.println("choose_nothing " + FragmentKeep.choose(null, null));
        System.out.println("choose_emptykept " + FragmentKeep.choose(null, ""));
        System.out.println("used " + FragmentKeep.usedKept(null, "city=old") + " " + FragmentKeep.usedKept("city=new", "city=old") + " " + FragmentKeep.usedKept(null, null));
    }
}
