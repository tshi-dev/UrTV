package sa.urtv.home;

/** For tests/cpu_class_test.py: «weak cores khz», «percent cpuMs wallMs» or «parse FILE», one answer per line. */
public final class CpuClassRun {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "weak": System.out.println(CpuClass.weak(Integer.parseInt(args[++i]), Long.parseLong(args[++i])) ? 1 : 0); break;
                case "percent": System.out.println(CpuClass.percent(Long.parseLong(args[++i]), Long.parseLong(args[++i]))); break;
                default: System.out.println(CpuClass.parse(new java.io.File(args[++i])));
            }
        }
    }
}
