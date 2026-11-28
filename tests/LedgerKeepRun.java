package sa.urtv.home.setup;

/** For tests/ledger_keep_test.py: LedgerKeep's rules, one "name<TAB>got" line per check. */
public final class LedgerKeepRun {
    public static void main(String[] a) {
        System.out.println("clean\t" + LedgerKeep.clean(" com.a.b \nbad name\ncom.a.b\n\ncom.c;rm\nx.y_z \n").replace("\n", ","));
        System.out.println("names none\t" + LedgerKeep.names(null).size() + " " + LedgerKeep.names("").size());
        System.out.println("keep new\t" + LedgerKeep.keep("old.pkg\n", "new.one\nnew.two\n").replace("\n", ","));
        System.out.println("keep empty\t" + LedgerKeep.keep("old.pkg\n", "").replace("\n", ",") + " " + LedgerKeep.keep("old.pkg\n", null).replace("\n", ",") + " " + LedgerKeep.keep("old.pkg\n", "?? bad\n").replace("\n", ","));
        System.out.println("keep nothing\t[" + LedgerKeep.keep(null, null) + "]");
        System.out.println("push\t" + LedgerKeep.worthPushing("", "a.b\n") + " " + LedgerKeep.worthPushing("a.b\n", "a.b\n") + " " + LedgerKeep.worthPushing("a.b\n", "") + " " + LedgerKeep.worthPushing(null, "x.y\n") + " " + LedgerKeep.worthPushing("a.b\n", "a.b\nc.d\n"));
        System.out.println("long\t" + LedgerKeep.names("a".repeat(201)).size());
    }
}
