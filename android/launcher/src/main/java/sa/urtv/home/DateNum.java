package sa.urtv.home;

/**
 * 4.3.3 «التاريخ بالأرقام» (#datenum=1, the screensaver's settings or `optimize/saver.sh datenum on`): the home's date line as day/month/year,
 * the Hijri and the Gregorian, «الاثنين، 06/06/1448 هـ  –  16/11/2026». The digits stay the person's choice («الأرقام»): this writes Latin ones and
 * the home's arabicDigits turns them. No Android in it: tests/date_num_test.py runs it.
 */
final class DateNum {
    private DateNum() { }

    /** the settings' fragment asks for numbers */
    static boolean on(String fragment) {
        return fragment != null && ("&" + fragment + "&").contains("&datenum=1&");
    }

    /** 6, 6, 1448 -> "06/06/1448" */
    static String ddmmyyyy(int day, int month, int year) {
        return (day < 10 ? "0" : "") + day + "/" + (month < 10 ? "0" : "") + month + "/" + year;
    }

    /** the line from the weekday and numericParts' "hijri|greg": Arabic «الاثنين، … هـ  –  …», English «Monday, … AH  –  …» */
    static String line(String weekday, String parts, boolean arabic) {
        String[] p = parts.split("\\|", 2);
        if (p.length < 2) return weekday;
        return weekday + (arabic ? "، " : ", ") + p[0] + (arabic ? " هـ" : " AH") + "  –  " + p[1];
    }
}
