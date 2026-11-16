package sa.urtv.home;

/** tests/device_rule_test.py: DeviceRule on a plain JVM */
public class DeviceRuleRun {
    public static void main(String[] a) {
        Object doc = MiniJson.parse("{\"v\":1,\"devices\":["
            + "{\"id\":\"tcl-g07\",\"match\":{\"brand\":\"TCL\",\"device\":\"G07\",\"sdk_min\":30,\"sdk_max\":30},\"defaults\":{\"sound_opens_general_settings\":true},\"why\":\"x\"},"
            + "{\"id\":\"tcl-all\",\"match\":{\"brand\":\"tcl\"},\"defaults\":{\"sound_opens_general_settings\":false,\"other\":1},\"why\":\"x\"},"
            + "{\"id\":\"every\",\"match\":{\"sdk_min\":19},\"defaults\":{\"everyone\":true},\"why\":\"x\"},"
            + "{\"id\":\"empty-brand\",\"match\":{\"brand\":\"\"},\"defaults\":{\"blank\":true},\"why\":\"x\"}]}");
        System.out.println("G07 " + DeviceRule.defaults(doc, "TCL", "Smart TV", "G07", 30));
        System.out.println("G07LOWER " + DeviceRule.defaults(doc, "tcl", "Smart TV", "g07 ", 30));
        System.out.println("G07SDK " + DeviceRule.defaults(doc, "TCL", "Smart TV", "G07", 31));
        System.out.println("OTHERTCL " + DeviceRule.defaults(doc, "TCL", "X", "G10", 30));
        System.out.println("SONY " + DeviceRule.defaults(doc, "Sony", "Bravia", "BRAVIA_4K", 30));
        System.out.println("NULLS " + DeviceRule.defaults(doc, null, null, null, 30));
        System.out.println("NODOC " + DeviceRule.defaults(null, "TCL", "Smart TV", "G07", 30));
    }
}
