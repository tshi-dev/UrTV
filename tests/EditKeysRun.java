package sa.urtv.home;

/** tests/edit_keys_test.py: each argument a key code; prints «own» or «pass», one a line */
public final class EditKeysRun {
    public static void main(String[] args) {
        for (String a : args) System.out.println(FavEdit.owns(Integer.parseInt(a)) ? "own" : "pass");
    }
}
