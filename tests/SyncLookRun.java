package sa.urtv.home;

/**
 * 4.3 «توحيد المظهر»: GlassStyle.material over every look, dark and light (tests/sync_look_test.py runs it).
 * On: the three surfaces are one material, the look's own panel, and follow «شفافية الشريط», the accent and the focus style together.
 * Off: each is its own, and with nothing chosen each is what it was before the switch.
 */
public final class SyncLookRun {
    private static int failed, passed;

    private static void check(String name, boolean ok, String detail) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name + "  -> " + detail); }
    }

    private static String key(GlassStyle.Material m) {
        return Integer.toHexString(m.fill) + " " + Integer.toHexString(m.line) + " " + m.radius + " " + m.inner + " " + m.shadow + " " + Integer.toHexString(m.text) + " " + Integer.toHexString(m.sub)
                + " " + Integer.toHexString(m.accent) + " " + Integer.toHexString(m.focus) + " " + Integer.toHexString(m.onFocus) + " " + Integer.toHexString(m.ring) + " " + m.ringWidth + " " + m.focusStyle + " " + m.light + " " + m.ownInk;
    }

    public static void main(String[] a) {
        int[] surfaces = {GlassStyle.QUICK, GlassStyle.CARDS, GlassStyle.BAR};
        String[] tweaks = {"", "accent=teal;bar=70;dim=30;focus=frame", "accent=;bar=30;dim=0;focus=glow", "accent=rose;bar=100;dim=0;focus=solid;sync=on"};
        for (String t : tweaks) for (String look : GlassStyle.LOOKS) for (boolean light : new boolean[] {false, true}) {
            GlassStyle.setTweaks(t);
            String at = look + (light ? " light" : " dark") + " [" + t + "]";
            GlassStyle.Look l = GlassStyle.of(look, light);
            GlassStyle.Material q = GlassStyle.material(look, light, GlassStyle.QUICK), c = GlassStyle.material(look, light, GlassStyle.CARDS), b = GlassStyle.material(look, light, GlassStyle.BAR);
            check("on: the three surfaces are one material, " + at, GlassStyle.sync() && key(q).equals(key(c)) && key(c).equals(key(b)), key(q) + " | " + key(c) + " | " + key(b));
            check("on: the fill is the look's panel as thin as the bar's strength, the line the look's, " + at,
                    q.fill == GlassStyle.scaled(l.panel, GlassStyle.barPercent()) && q.line == l.line, key(q));
            check("on: corners 24 outside and 16 inside, no shadow, " + at, q.radius == 24 && q.inner == 16 && q.shadow == 0, key(q));
            check("on: the ink, the accent and the focus are the look's (with the chosen accent), the cards take them too, " + at,
                    q.text == l.text && q.sub == l.sub && q.accent == l.accent && q.focus == l.focus && q.onFocus == l.onFocus && c.ownInk && q.light == l.light, key(q));
            String f = GlassStyle.focusStyle();
            check("on: the ring is the focus style's, " + at, "frame".equals(f) ? q.ring == l.accent && q.ringWidth == 4 : "glow".equals(f) ? q.ring == ((l.accent & 0xFFFFFF) | 0x80000000) && q.ringWidth == 6
                    : q.ring == (l.ring != 0 ? l.ring : 0xFF000000 | l.focus) && q.ringWidth == 3 && (q.ring >>> 24) == 255, key(q));
            check("on: in a light look the cards are the white bar (a light fill, dark words), " + at, !light || (c.light == l.light && c.fill == b.fill), key(c));
        }
        // off, nothing chosen: each surface as it was before the switch
        for (String look : GlassStyle.LOOKS) for (boolean light : new boolean[] {false, true}) for (int pct : new int[] {100, 70}) {
            GlassStyle.setTweaks("accent=;bar=" + pct + ";dim=0;focus=solid;sync=off");
            String at = look + (light ? " light " : " dark ") + pct;
            GlassStyle.Look l = GlassStyle.of(look, light);
            GlassStyle.Material q = GlassStyle.material(look, light, GlassStyle.QUICK), c = GlassStyle.material(look, light, GlassStyle.CARDS), b = GlassStyle.material(look, light, GlassStyle.BAR);
            check("off: the switch is off, " + at, !GlassStyle.sync(), "");
            check("off, as before: the buttons are the look's panel and line at the bar's strength, " + at, q.fill == GlassStyle.scaled(l.panel, pct) && q.line == GlassStyle.scaled(l.line, pct), key(q));
            check("off, as before: the apps bar is the white glass, " + at, b.fill == GlassStyle.scaled(light ? 0xB8FFFFFF : 0x24FFFFFF, pct) && b.line == GlassStyle.scaled(light ? 0xD9FFFFFF : 0x47FFFFFF, pct), key(b));
            check("off, as before: the cards are the white glass, a neutral dark one in the light, and keep the page's own words, " + at,
                    c.fill == GlassStyle.scaled(light ? 0xA014161A : 0x24FFFFFF, pct) && c.line == GlassStyle.scaled(light ? 0x33FFFFFF : 0x47FFFFFF, pct) && !c.ownInk && !c.light, key(c));
            check("off: the corners and the focus stay for all, " + at, q.radius == 24 && c.radius == 24 && b.radius == 24 && q.ring == b.ring && q.focusStyle.equals(b.focusStyle), key(q));
        }
        // off, each on its own
        GlassStyle.setTweaks("accent=amber;bar=70;dim=0;focus=frame;sync=off;quick=40:glass;cards=90:graphite;tray=100:look");
        for (String look : GlassStyle.LOOKS) for (boolean light : new boolean[] {false, true}) {
            String at = look + (light ? " light" : " dark");
            GlassStyle.Look l = GlassStyle.of(look, light);
            GlassStyle.Material q = GlassStyle.material(look, light, GlassStyle.QUICK), c = GlassStyle.material(look, light, GlassStyle.CARDS), b = GlassStyle.material(look, light, GlassStyle.BAR);
            check("off, own values: the buttons are the white glass at 40, " + at, q.fill == GlassStyle.scaled(light ? 0xB8FFFFFF : 0x24FFFFFF, 40), key(q));
            check("off, own values: the cards are charcoal at 90, a dark fill under the page's light words, " + at, c.fill == GlassStyle.scaled(GlassStyle.GRAPHITE_DARK.panel, 90) && !c.ownInk, key(c));
            check("off, own values: the apps bar is the look's panel at 100, " + at, b.fill == l.panel && b.line == l.line, key(b));
            check("off, own values: the three are separate, " + at, q.fill != c.fill && c.fill != b.fill && q.fill != b.fill, key(q) + " | " + key(c) + " | " + key(b));
            check("off: the accent still reaches all three, " + at, q.accent == l.accent && b.accent == l.accent && b.ring == l.accent && b.ringWidth == 4, key(b));
        }
        GlassStyle.setTweaks("sync=off;quick=5:paint;cards=250:glass;tray=x:look");
        check("off: a strength is held to 30..100 and an unknown fill is the surface's own before", GlassStyle.surfacePercent(0) == 30 && "look".equals(GlassStyle.surfaceFill(0)) && GlassStyle.surfacePercent(1) == 100, GlassStyle.surfacePercent(0) + " " + GlassStyle.surfaceFill(0));
        // what was kept before the switch existed still reads the same, with the switch on
        GlassStyle.setTweaks("accent=teal;bar=70;dim=30;focus=frame");
        check("an older string: the accent, 70, 30 and frame are kept, the switch is on", GlassStyle.sync() && GlassStyle.barPercent() == 70 && GlassStyle.dimPercent() == 30 && "frame".equals(GlassStyle.focusStyle())
                && GlassStyle.of("glass", false).accent == 0xFF2FB5A3, "");
        GlassStyle.setTweaks(null);
        check("nothing kept: the switch is on, everything as the look has it", GlassStyle.sync() && GlassStyle.barPercent() == 100 && "sync".equals(GlassStyle.materialKey()), GlassStyle.materialKey());
        check("the page's colour: rgba with three decimals", "rgba(20,22,26,0.420)".equals(GlassStyle.rgba(0x6B14161A)) && "rgba(255,255,255,1)".equals(GlassStyle.rgba(0xFFFFFFFF)) && "rgba(0,0,0,0)".equals(GlassStyle.rgba(0)), GlassStyle.rgba(0x6B14161A));
        System.out.println(failed == 0 ? "ALL PASS " + passed : failed + " FAILED");
    }
}
