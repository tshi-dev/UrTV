package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small strict JSON reader with no Android in it (the quiz bank's check runs on a plain JVM): objects are Maps, arrays Lists, numbers Doubles, plus String, Boolean
 *  and null. Anything malformed, nested deeper than 8 levels or followed by more text returns null, never throws. */
final class MiniJson {
    private final String s; private int i; private static final int DEPTH_MAX = 8;
    private MiniJson(String s) { this.s = s; }

    static Object parse(String text) {
        try {
            MiniJson p = new MiniJson(text);
            p.ws();
            Object v = p.value(0);
            p.ws();
            return p.i == text.length() ? v : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void ws() { while (i < s.length() && " \t\r\n".indexOf(s.charAt(i)) >= 0) i++; }
    private RuntimeException bad() { return new IllegalStateException("json"); }
    private char peek() { if (i >= s.length()) throw bad(); return s.charAt(i); }

    private Object value(int depth) {
        if (depth > DEPTH_MAX) throw bad();
        char c = peek();
        if (c == '[') {
            i++; List<Object> v = new ArrayList<>(); ws();
            if (peek() == ']') { i++; return v; }
            while (true) { ws(); v.add(value(depth + 1)); ws(); char d = peek(); i++; if (d == ']') return v; if (d != ',') throw bad(); }
        }
        if (c == '{') {
            i++; Map<String, Object> v = new LinkedHashMap<>(); ws();
            if (peek() == '}') { i++; return v; }
            while (true) {
                ws(); if (peek() != '"') throw bad(); String k = string(); ws();
                if (peek() != ':') throw bad(); i++; ws();
                v.put(k, value(depth + 1)); ws(); char d = peek(); i++; if (d == '}') return v; if (d != ',') throw bad();
            }
        }
        if (c == '"') return string();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (st == i) throw bad();
        return Double.valueOf(s.substring(st, i));
    }

    private String string() {
        i++;
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = peek(); i++;
            if (c == '"') return b.toString();
            if (c < 0x20) throw bad();
            if (c != '\\') { b.append(c); continue; }
            char e = peek(); i++;
            switch (e) {
                case '"': b.append('"'); break; case '\\': b.append('\\'); break; case '/': b.append('/'); break;
                case 'b': b.append('\b'); break; case 'f': b.append('\f'); break; case 'n': b.append('\n'); break; case 'r': b.append('\r'); break; case 't': b.append('\t'); break;
                case 'u': if (i + 4 > s.length()) throw bad(); b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                default: throw bad();
            }
        }
    }
}
