package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * «قائمة الأسهم»: what a person types, made into the watchlist's codes, with no Android in it (tests/stocks_search_test.py runs it). A
 * Tadawul code (2222, also ٢٢٢٢ or 2222.SR), a company's name (أرامكو, مصرف الراجحي, بنك الرياض; with or without «ال», hamza or tashkeel;
 * a unique start such as «راجح» is enough) or a US ticker (AAPL), in any mix. The names are the page's (TADAWUL in src/template.html, the
 * test keeps them the same) and their English ones (i18n/en/page.tsv). An English name counts when it cannot be a US ticker: more than
 * one word (Al Rajhi, Riyad Bank) or six letters and more (Aramco, Almarai); a shorter Latin word stays a ticker, as it always was.
 */
final class Stocks {
    /** code, Arabic name (the page's), English name (the page's), then other ways people write it */
    static final String[][] TADAWUL = {
        {"2222", "أرامكو", "Aramco", "أرامكو السعودية", "Saudi Aramco"},
        {"1120", "الراجحي", "Al Rajhi", "مصرف الراجحي", "بنك الراجحي", "Alrajhi"},
        {"1180", "الأهلي", "SNB", "البنك الأهلي", "الأهلي السعودي", "البنك الأهلي السعودي"},
        {"2010", "سابك", "SABIC"},
        {"7010", "stc", "stc", "اس تي سي", "إس تي سي", "الاتصالات السعودية"},
        {"1211", "معادن", "Maaden"},
        {"2082", "أكوا", "ACWA", "أكوا باور", "ACWA Power"},
        {"5110", "كهرباء السعودية", "SEC", "الكهرباء", "السعودية للكهرباء", "Saudi Electricity"},
        {"1150", "الإنماء", "Alinma", "مصرف الإنماء", "بنك الإنماء"},
        {"1060", "ساب", "SAB", "البنك السعودي البريطاني"},
        {"1010", "بنك الرياض", "Riyad Bank"},
        {"1050", "السعودي الفرنسي", "BSF", "البنك السعودي الفرنسي"},
        {"1140", "البلاد", "Albilad", "بنك البلاد"},
        {"1020", "الجزيرة", "AlJazira", "بنك الجزيرة", "Bank AlJazira"},
        {"1080", "العربي", "ANB", "البنك العربي", "العربي الوطني", "البنك العربي الوطني"},
        {"2280", "المراعي", "Almarai"},
        {"2380", "بترورابغ", "Petro Rabigh", "بترو رابغ"},
        {"2310", "سبكيم", "Sipchem"},
        {"2020", "سافكو", "SAFCO"},
        {"2290", "ينساب", "Yansab"},
        {"2350", "كيان", "Kayan"},
        {"4013", "الحبيب", "Al Habib", "سليمان الحبيب", "د سليمان الحبيب"},
        {"4190", "جرير", "Jarir", "مكتبة جرير"},
        {"7020", "موبايلي", "Mobily"},
        {"7030", "زين", "Zain", "زين السعودية"},
        {"7203", "علم", "Elm"},
        {"4030", "البحري", "Bahri"},
        {"1810", "سيرا", "Seera"},
        {"8010", "التعاونية", "Tawuniya"},
        {"4300", "دار الأركان", "Dar Al Arkan"},
    };
    /** the longest name in words (دار الأركان, البنك العربي الوطني…): how far a name is looked for across spaces */
    private static final int MAX_WORDS = 4;

    private Stocks() {}

    /** What parse found: the codes in order (each once), which ones came from a name or Arabic digits, and the first word that is nothing. */
    static final class Parsed {
        final List<String> codes = new ArrayList<>();
        final List<String> matched = new ArrayList<>();
        String bad;
    }

    static Parsed parse(String text) {
        Parsed p = new Parsed();
        String[] w = text == null ? new String[0] : text.trim().split("[\\s,،]+");
        for (int i = 0; i < w.length; ) {
            if (w[i].isEmpty()) { i++; continue; }
            String code = null;
            int used = 1;
            for (int n = Math.min(MAX_WORDS, w.length - i); n >= 2 && code == null; n--) {   // a name of several words first
                String code2 = byName(String.join(" ", java.util.Arrays.asList(w).subList(i, i + n)), true);
                if (code2 != null) { code = code2; used = n; }
            }
            boolean named = code != null;
            if (code == null) {
                code = one(w[i]);
                named = code != null && !code.equals(w[i].trim().toUpperCase(Locale.ROOT));
            }
            if (code == null) { p.bad = w[i]; return p; }
            if (!p.codes.contains(code)) {
                p.codes.add(code);
                if (named) p.matched.add(code);
            }
            i += used;
        }
        return p;
    }

    /** One word: a code (any digits, «.SR» allowed), else a name, else a US ticker; null for none. */
    static String one(String word) {
        String s = ascii(word.trim());
        String up = s.toUpperCase(Locale.ROOT);
        if (up.matches("\\d{4}(\\.SR)?")) return up.substring(0, 4);
        String code = byName(s, false);
        if (code != null) return code;
        return up.matches(Prefs.STOCK_PATTERN) ? up : null;
    }

    /** The code of a company by its name; several words only when {@code exactOnly} (a start of several words would eat a ticker). */
    static String byName(String name, boolean exactOnly) {
        String key = norm(name);
        if (key.isEmpty()) return null;
        boolean latin = !hasArabic(name);
        if (latin && !name.trim().contains(" ") && key.length() < 6) return null;   // a short Latin word is a ticker, as before
        for (String[] row : TADAWUL)
            for (int k = 1; k < row.length; k++) if (norm(row[k]).equals(key) && (latin || hasArabic(row[k]) || k == 1)) return row[0];
        if (exactOnly || latin || key.length() < 3) return null;
        String found = null;
        for (String[] row : TADAWUL)
            for (int k = 1; k < row.length; k++)
                if (hasArabic(row[k]) && norm(row[k]).startsWith(key)) {
                    if (found != null && !found.equals(row[0])) return null;   // two companies start so: none
                    found = row[0];
                }
        return found;
    }

    /** The company's short name in the language asked, null for a code we have no name for (a US ticker, a smaller company). */
    static String name(String code, boolean english) {
        for (String[] row : TADAWUL) if (row[0].equals(code)) return english ? row[2] : row[1];
        return null;
    }

    /** A code as a person reads it: «أرامكو 2222», or the code alone. */
    static String label(String code, boolean english) {
        String n = name(code, english);
        return n == null ? code : n + " " + code;
    }

    /** Arabic-Indic and Persian digits as 0-9 (Java's \d is ASCII only). */
    static String ascii(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '٠' && c <= '٩') c = (char) ('0' + c - '٠');
            else if (c >= '۰' && c <= '۹') c = (char) ('0' + c - '۰');
            b.append(c);
        }
        return b.toString();
    }

    private static boolean hasArabic(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= '؀' && s.charAt(i) <= 'ۿ') return true;
        return false;
    }

    /** One spelling for comparing: no tashkeel or tatweel, one alef, ه for ة, ي for ى, no «ال» at a word's start, Latin in lower case, no spaces. */
    static String norm(String s) {
        StringBuilder out = new StringBuilder();
        for (String word : s.trim().toLowerCase(Locale.ROOT).split("[\\s\\-_.]+")) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if ((c >= 'ً' && c <= 'ْ') || c == 'ٰ' || c == 'ـ') continue;   // tashkeel, dagger alef, tatweel
                if (c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ') c = 'ا';
                else if (c == 'ة') c = 'ه';
                else if (c == 'ى') c = 'ي';
                b.append(c);
            }
            String w = b.toString();
            if (w.startsWith("ال") && w.length() > 3) w = w.substring(2);
            out.append(w);
        }
        return out.toString();
    }
}
