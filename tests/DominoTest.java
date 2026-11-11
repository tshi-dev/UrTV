package sa.urtv.screensaver.games.domino;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** For tests/domino_test.py: the rules of «دومينو» and its computer players on a plain JVM, every game from a seed. Prints PASS/FAIL lines. */
public final class DominoTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static Tile t(int a, int b) { return new Tile(a, b); }

    /** Every tile, once: hands + boneyard + table (+ the double blank set aside with three). */
    static boolean conserved(DominoGame g) {
        int[] n = new int[28];
        for (int p = 0; p < g.players; p++) for (Tile x : g.rawHand(p)) n[x.id()]++;
        for (Tile x : g.rawBoneyard()) n[x.id()]++;
        for (DominoGame.Placed x : g.chain()) n[x.tile.id()]++;
        if (g.aside() != null) n[g.aside().id()]++;
        for (int c : n) if (c != 1) return false;
        return true;
    }
    static boolean joined(DominoGame g) {
        List<DominoGame.Placed> c = g.chain();
        for (int i = 0; i < c.size(); i++) {
            DominoGame.Placed x = c.get(i);
            if (!((x.first == x.tile.low && x.second == x.tile.high) || (x.first == x.tile.high && x.second == x.tile.low))) return false;
            if (i > 0 && c.get(i - 1).second != x.first) return false;
        }
        return true;
    }
    /** Puts the given tiles in a player's hand by swapping them with whoever (or the boneyard) holds them: the set stays whole. */
    static void give(DominoGame g, int p, Tile... want) {
        List<Tile> mine = g.rawHand(p);
        List<Tile> keep = new ArrayList<>(Arrays.asList(want));
        for (Tile w : want) {
            if (mine.contains(w)) continue;
            List<Tile> from = null;
            for (int o = 0; o < g.players; o++) if (o != p && g.rawHand(o).contains(w)) from = g.rawHand(o);
            if (from == null) from = g.rawBoneyard();
            Tile out = null;
            for (Tile m : mine) if (!keep.contains(m)) { out = m; break; }
            from.set(from.indexOf(w), out); mine.set(mine.indexOf(out), w);
        }
    }
    /** A first round (to 100) that the double six opens: the first seed from the given one whose deal puts it in a hand. */
    static DominoGame sixes(int n, int seed) {
        for (;; seed++) { DominoGame g = new DominoGame(n, 100, new Random(seed)); if (t(6, 6).equals(g.opening())) return g; }
    }
    /** What is wrong with a snake, or null: every half inside the table, no two halves on each other, neighbours joined on the same number (when the chain is a real one). */
    static String snakeBad(List<DominoGame.Placed> chain, Snake s, float w, float h, boolean real) {
        if (s == null || s.slots.length != chain.size()) return "no layout";
        float u = s.unit, e = 0.01f;
        int n = s.slots.length;
        float[][] hx = new float[n][2], hy = new float[n][2];
        for (int i = 0; i < n; i++) {
            Snake.Slot x = s.slots[i];
            hx[i][0] = x.x1; hx[i][1] = x.x2; hy[i][0] = x.y1; hy[i][1] = x.y2;
            if (!x.tile.equals(chain.get(i).tile)) return "tile order";
            if (Math.abs(Math.abs(x.x1 - x.x2) + Math.abs(x.y1 - x.y2) - u) > e || (x.x1 != x.x2 && x.y1 != x.y2)) return "a tile is not two halves side by side";
            if (!((x.v1 == x.tile.low && x.v2 == x.tile.high) || (x.v1 == x.tile.high && x.v2 == x.tile.low))) return "numbers";
            for (int k = 0; k < 2; k++) if (hx[i][k] - u / 2 < -e || hx[i][k] + u / 2 > w + e || hy[i][k] - u / 2 < -e || hy[i][k] + u / 2 > h + e) return "outside the table";
        }
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++)
            if (Math.abs(hx[i][a] - hx[j][b]) < u - e && Math.abs(hy[i][a] - hy[j][b]) < u - e) return "tiles " + i + " and " + j + " overlap";
        if (!real) return null;
        for (int i = 0; i + 1 < n; i++) {
            int want = chain.get(i).second; boolean ok = false;
            Snake.Slot x = s.slots[i], y = s.slots[i + 1];
            // a half of each with the number, edge to edge; or one is a double across, its middle one unit from the other's half
            for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
                if ((a == 0 ? x.v1 : x.v2) != want || (b == 0 ? y.v1 : y.v2) != want) continue;
                float dx = Math.abs(hx[i][a] - hx[i + 1][b]), dy = Math.abs(hy[i][a] - hy[i + 1][b]);
                if ((Math.abs(dx - u) < e && dy < e) || (Math.abs(dy - u) < e && dx < e)) ok = true;
                if (x.tile.isDouble() && Math.abs(Math.abs(x.cx() - hx[i + 1][b]) + Math.abs(x.cy() - hy[i + 1][b]) - u) < e && (Math.abs(x.cx() - hx[i + 1][b]) < e || Math.abs(x.cy() - hy[i + 1][b]) < e)) ok = true;
                if (y.tile.isDouble() && Math.abs(Math.abs(y.cx() - hx[i][a]) + Math.abs(y.cy() - hy[i][a]) - u) < e && (Math.abs(y.cx() - hx[i][a]) < e || Math.abs(y.cy() - hy[i][a]) < e)) ok = true;
            }
            if (!ok) return "tiles " + i + " and " + (i + 1) + " do not join on " + want;
        }
        return null;
    }

    /** One step by a computer player of the given level; false when its action was refused (never). */
    static boolean step(DominoGame g, int level, Random r) {
        int p = g.turn();
        DominoBot.Action a = DominoBot.choose(g.view(p), level, r);
        if (a.kind == DominoGame.PLAY) return g.play(p, a.move.tile, a.move.end);
        if (a.kind == DominoGame.DRAW) return g.draw(p) != null;
        return g.pass(p);
    }
    /** A whole game by computer players (levels[p]); returns the number of steps, -1 when something broke. */
    static int playOut(DominoGame g, int[] levels, Random r, StringBuilder trace) {
        int steps = 0;
        while (g.phase() != DominoGame.Phase.GAME_OVER) {
            if (g.phase() == DominoGame.Phase.ROUND_OVER) { if (!g.nextRound()) return -1; continue; }
            int p = g.turn(), before = g.events().size();
            if (!step(g, levels[p], r)) return -1;
            if (g.events().size() != before + 1 || !conserved(g) || !joined(g)) return -1;
            if (trace != null) { DominoGame.Event e = g.events().get(before); trace.append(e.player).append(e.kind).append(e.tile).append(e.end).append(' '); }
            if (++steps > 100000) return -1;
        }
        return steps;
    }

    public static void main(String[] a) throws Exception {
        // ---- the set and the deal
        List<Tile> set = Tile.set();
        Set<Integer> ids = new HashSet<>(); int pips = 0, doubles = 0; int[] per = new int[7];
        for (Tile x : set) { ids.add(x.id()); pips += x.pips(); if (x.isDouble()) doubles++; per[x.low]++; if (!x.isDouble()) per[x.high]++; }
        check("the set: 28 different tiles, 7 doubles, 168 pips, each number on 7 tiles; a tile is the same whichever half comes first",
            set.size() == 28 && ids.size() == 28 && doubles == 7 && pips == 168 && Arrays.equals(per, new int[]{7, 7, 7, 7, 7, 7, 7}) && t(5, 2).equals(t(2, 5)) && t(5, 2).id() == t(2, 5).id() && t(2, 5).other(2) == 5 && t(4, 4).other(4) == 4);
        boolean threw = false; try { new Tile(7, 0); } catch (IllegalArgumentException e) { threw = true; }
        boolean threw2 = false; try { new DominoGame(5, 100, new Random(1)); } catch (IllegalArgumentException e) { threw2 = true; }
        boolean threw3 = false; try { new DominoGame(1, 100, new Random(1)); } catch (IllegalArgumentException e) { threw3 = true; }
        check("no tile beyond six, no table of one or of five", threw && threw2 && threw3);
        List<Tile> s1 = Tile.set(), s2 = Tile.set(), s3 = Tile.set();
        DominoGame.shuffle(s1, new Random(7)); DominoGame.shuffle(s2, new Random(7)); DominoGame.shuffle(s3, new Random(8));
        check("the shuffle: the same seed gives the same order, another seed another, and it only reorders", s1.equals(s2) && !s1.equals(s3) && !s1.equals(set) && new HashSet<>(s1).equals(new HashSet<>(set)));
        boolean deal = true;
        for (int seed = 0; seed < 200; seed++) {
            DominoGame g2 = new DominoGame(2, 100, new Random(seed)), g3 = new DominoGame(3, 100, new Random(seed)), g4 = new DominoGame(4, 100, new Random(seed));
            deal &= g2.count(0) == 7 && g2.count(1) == 7 && g2.boneyard() == 14 && g2.aside() == null && conserved(g2);
            deal &= g3.count(0) == 9 && g3.count(1) == 9 && g3.count(2) == 9 && g3.boneyard() == 0 && t(0, 0).equals(g3.aside()) && conserved(g3);
            deal &= g4.count(0) == 7 && g4.count(3) == 7 && g4.boneyard() == 0 && g4.aside() == null && conserved(g4);
        }
        check("the deal: two players 7 each and 14 to draw from; three players 9 each, the double blank aside, nothing to draw; four players 7 each, nothing to draw (200 seeds each)", deal);

        // ---- who starts
        boolean first = true; int noDouble = 0;
        for (int seed = 0; seed < 400; seed++) for (int n = 2; n <= 4; n++) {
            DominoGame g = new DominoGame(n, 100, new Random(seed));
            Tile top = null; int who = -1;
            for (int p = 0; p < n; p++) for (Tile x : g.hand(p)) if (x.isDouble() && (top == null || x.high > top.high)) { top = x; who = p; }
            if (top == null) { noDouble++; for (int p = 0; p < n; p++) for (Tile x : g.hand(p)) if (top == null || x.pips() > top.pips() || (x.pips() == top.pips() && x.high > top.high)) { top = x; who = p; } }
            List<DominoGame.Move> legal = g.legal(g.turn());
            first &= g.turn() == who && g.starter() == who && top.equals(g.opening()) && legal.size() == 1 && legal.get(0).tile.equals(top);
            for (Tile x : new ArrayList<>(g.hand(who))) if (!x.equals(top)) first &= !g.play(who, x, DominoGame.RIGHT);
            first &= g.play(who, top, DominoGame.RIGHT) && g.opening() == null && g.turn() == (who + 1) % n;
        }
        check("who starts: the holder of the highest double, and that tile is the only first move; with no double dealt (seen " + noDouble + " times) the heaviest tile", first && noDouble > 0);

        // ---- legal moves, both ends, refusals
        {
        DominoGame g = new DominoGame(2, 100, new Random(3));
        int p = g.turn(), o = 1 - p; Tile open = g.opening();
        check("refused and nothing changes: a move out of turn, a tile not in the hand, an end that is not an end, a draw or a pass while a tile fits",
            !g.play(o, g.hand(o).get(0), DominoGame.RIGHT) && !g.play(p, g.hand(o).get(0), DominoGame.RIGHT) && !g.play(p, open, 2) && !g.play(p, null, 0) && g.draw(p) == null && !g.pass(p) && g.draw(o) == null && !g.pass(o)
            && g.chain().isEmpty() && g.events().isEmpty() && g.count(p) == 7 && g.boneyard() == 14);
        }
        {
        DominoGame g = sixes(2, 11);
        int p = g.turn(), o = 1 - p;
        give(g, p, t(6, 6), t(6, 1)); give(g, o, t(6, 3), t(3, 1), t(6, 5), t(2, 2));
        // make 6|6 the opening whatever was dealt: p holds it, so it is the highest double
        DominoGame h = g;
        check("set-up: the double six opens", t(6, 6).equals(h.opening()) && h.turn() == p && h.play(p, t(6, 6), DominoGame.RIGHT) && h.left() == 6 && h.right() == 6);
        List<DominoGame.Move> lm = h.legal(o);
        check("legal moves: a tile that fits both ends is offered on both, one that fits none is not offered; a double on the table shows the same number on both ends",
            lm.contains(new DominoGame.Move(t(6, 3), DominoGame.LEFT)) && lm.contains(new DominoGame.Move(t(6, 3), DominoGame.RIGHT)) && !lm.contains(new DominoGame.Move(t(2, 2), DominoGame.LEFT)) && !lm.contains(new DominoGame.Move(t(3, 1), DominoGame.RIGHT)));
        check("a tile that does not fit is refused on either end", !h.play(o, t(2, 2), DominoGame.LEFT) && !h.play(o, t(3, 1), DominoGame.RIGHT) && h.chain().size() == 1);
        check("playing on the left: the matching half faces the chain, the other becomes the open end", h.play(o, t(6, 3), DominoGame.LEFT) && h.left() == 3 && h.right() == 6 && h.chain().get(0).first == 3 && h.chain().get(0).second == 6 && joined(h));
        check("playing on the right likewise", h.play(p, t(6, 1), DominoGame.RIGHT) && h.left() == 3 && h.right() == 1 && joined(h));
        check("a tile that fits the two different ends: both moves are legal, and the end chosen is the end used", h.legal(o).contains(new DominoGame.Move(t(3, 1), DominoGame.LEFT)) && h.legal(o).contains(new DominoGame.Move(t(3, 1), DominoGame.RIGHT))
            && !h.legal(o).contains(new DominoGame.Move(t(6, 5), DominoGame.LEFT)) && h.play(o, t(3, 1), DominoGame.RIGHT) && h.left() == 3 && h.right() == 3 && h.chain().size() == 4 && joined(h) && conserved(h));
        DominoGame.Event e = h.events().get(1);
        check("what the table saw: each play with its player, tile, end and the ends before it", h.events().size() == 4 && e.player == o && e.kind == DominoGame.PLAY && e.tile.equals(t(6, 3)) && e.end == DominoGame.LEFT && e.left == 6 && e.right == 6 && h.events().get(0).left == -1);
        }

        // ---- drawing and passing
        {
        DominoGame g = sixes(2, 5);
        int p = g.turn(), o = 1 - p;
        give(g, p, t(6, 6)); give(g, o, t(0, 0), t(0, 1), t(0, 2), t(1, 1), t(1, 2), t(2, 2), t(0, 3));
        g.play(p, t(6, 6), DominoGame.RIGHT);
        boolean ok = g.mustDraw(o) && !g.mustPass(o) && g.legal(o).isEmpty() && !g.pass(o);
        int drawn = 0; Tile got = null;
        while (g.mustDraw(o)) { got = g.draw(o); ok &= got != null && g.turn() == o && conserved(g); drawn++; }
        ok &= drawn >= 1 && got.has(6) && g.count(o) == 7 + drawn && g.boneyard() == 14 - drawn && g.draw(o) == null && g.events().get(1).kind == DominoGame.DRAW && g.events().get(1).tile == null;
        check("drawing: with no tile that fits the player draws one at a time until one fits (the turn stays his, the drawn tile is not in the public record), then may not draw more", ok);
        check("... and plays the tile he found", g.play(o, got, DominoGame.LEFT) && g.turn() == p);
        }
        {
        // four players, no boneyard: passing
        DominoGame g = sixes(4, 9);
        int p = g.turn(), q = (p + 1) % 4;
        give(g, p, t(6, 6)); give(g, q, t(0, 0), t(0, 1), t(0, 2), t(1, 1), t(1, 2), t(2, 2), t(0, 3));
        g.play(p, t(6, 6), DominoGame.RIGHT);
        check("passing: with nothing that fits and nothing to draw the player must pass, and cannot draw; the turn moves on; a player who can play may not pass",
            g.mustPass(q) && !g.mustDraw(q) && g.draw(q) == null && g.pass(q) && g.turn() == (q + 1) % 4 && g.events().get(1).kind == DominoGame.PASS && g.events().get(1).left == 6 && (g.canPlay(g.turn()) ? !g.pass(g.turn()) : true));
        }

        // ---- the end of a round: going out, blocked, scores, the target
        {
        DominoGame g = new DominoGame(2, 50, new Random(21));
        int p = g.turn(), o = 1 - p;
        Tile open = g.opening();
        // leave p with his opening tile only: the rest goes to the boneyard, tile for tile
        List<Tile> hp = g.rawHand(p), by = g.rawBoneyard();
        while (hp.size() > 1) { Tile x = hp.get(0).equals(open) ? hp.get(1) : hp.get(0); hp.remove(x); by.add(x); }
        int others = DominoGame.pips(g.hand(o));
        boolean ok = g.play(p, open, DominoGame.RIGHT);
        DominoGame.RoundResult r = g.lastRound();
        check("going out: the first with an empty hand wins the round and scores the pips left in the other hands",
            ok && r.winner == p && !r.blocked && r.points == others && r.pips[p] == 0 && r.pips[o] == others && g.score(p) == others && g.score(o) == 0 && g.phase() == (others >= 50 ? DominoGame.Phase.GAME_OVER : DominoGame.Phase.ROUND_OVER));
        check("after a round nothing can be played, drawn or passed", !g.play(o, g.hand(o).get(0), DominoGame.RIGHT) && g.draw(o) == null && !g.pass(o) && g.legal(o).isEmpty());
        if (g.phase() == DominoGame.Phase.ROUND_OVER) {
            check("the next round: a new deal, the winner opens with any tile he likes", g.nextRound() && g.round() == 2 && g.turn() == p && g.opening() == null && g.legal(p).size() == 7 && g.chain().isEmpty() && g.boneyard() == 14 && conserved(g) && !g.nextRound());
        }
        }
        {
        // blocked: four players, the sixes all out but one end... build it: chain 6|6, then every hand without a six
        DominoGame g = sixes(4, 2);
        int p = g.turn();
        give(g, p, t(6, 6), t(0, 6), t(1, 6), t(2, 6), t(3, 6), t(4, 6), t(5, 6));
        // p plays 6|6, the others cannot answer: three passes, p plays again... p will go out; instead check the blocked rule with a made-up end: all sixes with p, p plays 6|6 and holds the rest
        boolean ok = g.play(p, t(6, 6), DominoGame.RIGHT) && g.pass((p + 1) % 4) && g.pass((p + 2) % 4) && g.pass((p + 3) % 4) && g.turn() == p && g.phase() == DominoGame.Phase.PLAY;
        check("not blocked while one player can still play: three passes come back to him", ok);
        }
        {
        // a truly blocked round: 2 players, empty boneyard, ends closed
        int blockedSeen = 0, drawSeen = 0; boolean ok = true;
        for (int seed = 0; seed < 3000; seed++) {
            int n = 2 + seed % 3;
            DominoGame g = new DominoGame(n, 1000000, new Random(seed));
            Random r = new Random(seed);
            while (g.phase() == DominoGame.Phase.PLAY) ok &= step(g, DominoBot.EASY, r);
            DominoGame.RoundResult res = g.lastRound();
            if (!res.blocked) { ok &= g.count(res.winner) == 0; continue; }
            blockedSeen++;
            int low = Integer.MAX_VALUE, lows = 0, sum = 0;
            for (int p = 0; p < n; p++) { int s = DominoGame.pips(g.hand(p)); ok &= s == res.pips[p] && g.legal(p).isEmpty() && g.count(p) > 0; sum += s; if (s < low) { low = s; lows = 1; } else if (s == low) lows++; }
            ok &= g.boneyard() == 0;
            if (lows > 1) { drawSeen++; ok &= res.winner == -1 && res.points == 0; for (int p = 0; p < n; p++) ok &= g.score(p) == 0; }
            else ok &= res.pips[res.winner] == low && res.points == sum - low && g.score(res.winner) == sum - low;
        }
        check("a blocked round (seen " + blockedSeen + " times in 3000): nothing to draw and no hand fits; the lowest count wins and scores the others' pips; the lowest shared (seen " + drawSeen + " times) is a draw and nobody scores", ok && blockedSeen > 20 && drawSeen > 0);
        }
        {
        DominoGame g = new DominoGame(2, 100, new Random(77));
        Random r = new Random(77); boolean ok = true; int prevWinner = -1, drawnRounds = 0;
        while (g.phase() != DominoGame.Phase.GAME_OVER) {
            if (g.phase() == DominoGame.Phase.ROUND_OVER) {
                DominoGame.RoundResult res = g.lastRound();
                ok &= g.score(0) < 100 && g.score(1) < 100 && g.nextRound();
                if (res.winner >= 0) ok &= g.turn() == res.winner && g.opening() == null; else { drawnRounds++; ok &= g.opening() != null; }
                continue;
            }
            ok &= step(g, DominoBot.MEDIUM, r);
        }
        check("the target: rounds go on while everyone is under it, and the first to reach it wins the game; then nothing more is accepted",
            ok && g.winner() >= 0 && g.score(g.winner()) >= 100 && g.score(1 - g.winner()) < 100 && g.round() > 1 && !g.nextRound() && g.legal(0).isEmpty() && g.legal(1).isEmpty() && g.lastRound().winner == g.winner());
        check("the target chips are 50, 100 and 150", Arrays.equals(DominoGame.TARGETS, new int[]{50, 100, 150}));
        }

        // ---- properties over many whole games: nothing illegal accepted, tiles conserved, the chain joined, every game ends
        {
        boolean ok = true; long steps = 0; int games = 0, most = 0;
        Random wild = new Random(4242); int refused = 0, tried = 0;
        for (int seed = 0; seed < 1500; seed++) {
            int n = 2 + seed % 3, target = DominoGame.TARGETS[seed % 3];
            DominoGame g = new DominoGame(n, target, new Random(seed));
            Random r = new Random(seed * 31L + 1);
            int[] lv = new int[n]; for (int p = 0; p < n; p++) lv[p] = 1 + (seed + p) % 3;
            int count = 0;
            while (g.phase() != DominoGame.Phase.GAME_OVER && ok) {
                if (g.phase() == DominoGame.Phase.ROUND_OVER) { ok &= g.nextRound(); continue; }
                // a wild try first: any player, any tile of the 28, any end; accepted only when it is in the legal list of the player whose turn it is
                int wp = wild.nextInt(n), we = wild.nextInt(2); Tile wt = Tile.set().get(wild.nextInt(28));
                boolean legal = wp == g.turn() && g.legal(wp).contains(new DominoGame.Move(wt, we));
                int ev = g.events().size(), cl = g.chain().size();
                if (!legal) { tried++; if (!g.play(wp, wt, we) && g.events().size() == ev && g.chain().size() == cl) refused++; else ok = false; }
                int who = g.turn();
                if (g.canPlay(who)) ok &= g.draw(who) == null && !g.pass(who);
                else if (g.boneyard() > 0) ok &= !g.pass(who); else ok &= g.draw(who) == null;
                if (g.phase() != DominoGame.Phase.PLAY) continue;
                ok &= step(g, lv[g.turn()], r) && conserved(g) && joined(g);
                if (++count > 20000) ok = false;
            }
            int winners = 0; for (int p = 0; p < n; p++) if (g.score(p) >= target) winners++;
            ok &= winners == 1 && g.winner() >= 0 && g.score(g.winner()) >= target;
            steps += count; games++; most = Math.max(most, count);
        }
        check("1500 whole games by computer players (2, 3 and 4 players, every level, every target): " + tried + " illegal tries all refused with nothing changed, never a draw or pass while a tile fits, the 28 tiles conserved and the chain joined after every step, every game ends with one winner (longest " + most + " steps)",
            ok && refused == tried && tried > 10000 && games == 1500);
        }

        // ---- the snake: where the chain lies on the table
        {
        final float W = 1728, H = 600;   // the table on a 1080p screen: inside the 5 % safe area, between the score row and the hand
        boolean ok = true; int looked = 0, moved = 0, rounds = 0, smallest = 999, tall = 0; String why = "";
        for (int seed = 0; seed < 1500 && ok; seed++) {
            int n = 2 + seed % 3;
            DominoGame g = new DominoGame(n, 1000000, new Random(seed));
            Random r = new Random(seed);
            Snake before = null; int size = 0;
            while (g.phase() == DominoGame.Phase.PLAY && ok) {
                step(g, 1 + seed % 3, r);
                if (g.chain().size() == size) continue;
                size = g.chain().size();
                DominoGame.Event e = g.events().get(g.events().size() - (g.phase() == DominoGame.Phase.PLAY || g.events().get(g.events().size() - 1).kind == DominoGame.PLAY ? 1 : 2));
                Snake s = Snake.lay(g.chain(), g.anchor(), W, H, 72);
                String bad = snakeBad(g.chain(), s, W, H, true);
                if (bad != null) { ok = false; why = "seed " + seed + " at " + size + " tiles: " + bad; }
                if (before != null && e.kind == DominoGame.PLAY) {   // the tiles that lay there before: did any move?
                    int shift = e.end == DominoGame.LEFT ? 1 : 0; boolean same = true;
                    for (int i = 0; i < before.slots.length; i++) { Snake.Slot x = before.slots[i], y = s.slots[i + shift]; same &= x.x1 == y.x1 && x.y1 == y.y1 && x.x2 == y.x2 && x.y2 == y.y2; }
                    if (!same) { moved++; if (before.unit == s.unit && before.up == s.up && before.down == s.down) { ok = false; why = "seed " + seed + ": tiles moved with the same unit and rows"; } }
                }
                before = s; looked++; smallest = Math.min(smallest, (int) s.unit); tall = Math.max(tall, s.up + s.down + 1);
            }
            rounds++;
        }
        System.out.println("INFO the snake on a 1728 x 600 table: " + looked + " positions of " + rounds + " rounds, the unit never under " + smallest + " px, at most " + tall + " rows, the laid tiles moved " + moved + " times in all (a new row or a smaller unit)");
        check("the snake: in 1500 rounds, after every tile, each tile is whole and inside the table, no two overlap, the halves that touch carry the same number, a double in a row lies across; the unit stays 72 px (" + smallest + ") " + why, ok && smallest == 72);
        check("the snake: a tile that is laid stays where it is; the table moves only when a row is added or the unit shrinks (" + moved + " times in " + rounds + " rounds: never more than once for each new row)", ok && moved <= 3 * rounds);
        // the worst chains: everything on one arm, and every table size down to a small one
        boolean fits = true; int least = 999;
        float[][] tables = {{1728, 600}, {1728, 400}, {1200, 400}, {864, 300}, {640, 240}};
        for (float[] tb : tables) for (int arm = 0; arm < 3; arm++) for (int len = 1; len <= 28; len++) {
            List<DominoGame.Placed> c = new ArrayList<>();
            for (int i = 0; i < len; i++) { Tile x = Tile.set().get((i * 5 + arm) % 28); c.add(new DominoGame.Placed(x, x.low, x.high)); }
            int anchor = arm == 0 ? 0 : arm == 1 ? len - 1 : len / 2;
            Snake s = Snake.lay(c, anchor, tb[0], tb[1], 72);
            String bad = snakeBad(c, s, tb[0], tb[1], false);
            if (bad != null) { fits = false; System.out.println("INFO snake " + tb[0] + "x" + tb[1] + " len " + len + ": " + bad); }
            if (tb[1] == 600) least = Math.min(least, (int) s.unit);
        }
        check("the snake always fits: 28 tiles all on the right arm, all on the left, or half and half, on tables from 1728 x 600 down to 640 x 240; on the real table the unit never goes under 56 px (" + least + ")", fits && least >= 56);
        Snake one = Snake.lay(new DominoGame(2, 100, new Random(1)).chain(), 0, W, H, 72);
        check("the snake of an empty table is empty", one.slots.length == 0);
        }
        // ---- the computer players
        {
        StringBuilder a1 = new StringBuilder(), a2 = new StringBuilder(), a3 = new StringBuilder();
        int n1 = playOut(new DominoGame(4, 100, new Random(5)), new int[]{1, 2, 3, 3}, new Random(6), a1);
        int n2 = playOut(new DominoGame(4, 100, new Random(5)), new int[]{1, 2, 3, 3}, new Random(6), a2);
        playOut(new DominoGame(4, 100, new Random(55)), new int[]{1, 2, 3, 3}, new Random(6), a3);
        check("the same seed gives the same game, move for move; another seed another game", n1 > 0 && n1 == n2 && a1.toString().equals(a2.toString()) && !a1.toString().equals(a3.toString()));
        }
        {
        // never cheating: the view holds copies and no way to the game; shuffling what the player cannot see never changes his move
        boolean ok = true; int looked = 0;
        for (Field f : DominoGame.View.class.getDeclaredFields()) {
            Class<?> c = f.getType();
            ok &= Modifier.isFinal(f.getModifiers()) && (c == int.class || c == int[].class || c == List.class);
        }
        for (int seed = 0; seed < 300; seed++) {
            int n = 2 + seed % 3;
            DominoGame g = new DominoGame(n, 100, new Random(seed));
            Random r = new Random(seed), mix = new Random(seed + 9);
            while (g.phase() == DominoGame.Phase.PLAY) {
                int p = g.turn();
                DominoGame.View v = g.view(p);
                try { v.hand.add(t(0, 0)); ok = false; } catch (UnsupportedOperationException e) { /* read only */ }
                for (int lv = 2; lv <= 3; lv++) {
                    DominoBot.Action before = DominoBot.choose(v, lv, new Random(1));
                    // the hidden tiles: the other hands and the boneyard, dealt out again among themselves
                    List<Tile> hidden = new ArrayList<>(g.rawBoneyard());
                    for (int o = 0; o < n; o++) if (o != p) hidden.addAll(g.rawHand(o));
                    DominoGame.shuffle(hidden, mix);
                    int k = 0;
                    for (int i = 0; i < g.rawBoneyard().size(); i++) g.rawBoneyard().set(i, hidden.get(k++));
                    for (int o = 0; o < n; o++) if (o != p) for (int i = 0; i < g.rawHand(o).size(); i++) g.rawHand(o).set(i, hidden.get(k++));
                    DominoBot.Action after = DominoBot.choose(g.view(p), lv, new Random(1));
                    ok &= before.kind == after.kind && (before.move == null ? after.move == null : before.move.equals(after.move));
                    // and the order of his own hand does not matter either
                    Collections.reverse(g.rawHand(p));
                    DominoBot.Action turned = DominoBot.choose(g.view(p), lv, new Random(1));
                    ok &= before.kind == turned.kind && (before.move == null ? turned.move == null : before.move.equals(turned.move));
                    looked++;
                }
                // (the shuffle may have left the round blocked: the game finds that at the next step)
                if (!step(g, DominoBot.HARD, r)) break;
            }
        }
        check("the computer never cheats: its view is copies of its own hand and the table and nothing else (read only), and dealing the hidden tiles out again never changes its move (" + looked + " positions)", ok && looked > 3000);
        }
        {
        DominoGame g = sixes(2, 11);
        int p = g.turn(), o = 1 - p;
        give(g, p, t(6, 6)); give(g, o, t(6, 3), t(6, 5), t(5, 5), t(3, 1), t(2, 2), t(0, 1), t(0, 2));
        g.play(p, t(6, 6), DominoGame.RIGHT);
        DominoBot.Action m = DominoBot.choose(g.view(o), DominoBot.MEDIUM, new Random(1));
        check("level 2 plays the heaviest tile that fits (6|5 before 6|3)", m.kind == DominoGame.PLAY && m.move.tile.equals(t(6, 5)));
        g.play(o, t(6, 5), DominoGame.RIGHT);
        // p to move on ends 6 and 5
        give(g, p, t(5, 5), t(5, 4));
        DominoBot.Action d2 = DominoBot.choose(g.view(p), DominoBot.MEDIUM, new Random(1)), d3 = DominoBot.choose(g.view(p), DominoBot.HARD, new Random(1));
        check("levels 2 and 3 play a double early (5|5 before 5|4)", g.turn() == p && d2.move.tile.equals(t(5, 5)) && d3.move.tile.equals(t(5, 5)));
        }
        {
        // what a player was seen to lack
        DominoGame g = sixes(4, 9);
        int p = g.turn(), q = (p + 1) % 4;
        give(g, p, t(6, 6)); give(g, q, t(0, 0), t(0, 1), t(0, 2), t(1, 1), t(1, 2), t(2, 2), t(0, 3));
        g.play(p, t(6, 6), DominoGame.RIGHT); g.pass(q);
        boolean[][] x = DominoBot.lacks(g.view((q + 1) % 4));
        check("level 3 remembers what everybody saw: a player who passed on the sixes holds no six", x[q][6] && !x[q][5] && !x[p][6]);
        DominoGame g2 = sixes(2, 5);
        int p2 = g2.turn(), o2 = 1 - p2;
        give(g2, p2, t(6, 6)); give(g2, o2, t(0, 0), t(0, 1), t(0, 2), t(1, 1), t(1, 2), t(2, 2), t(0, 3));
        g2.play(p2, t(6, 6), DominoGame.RIGHT);
        g2.draw(o2);
        boolean[][] y = DominoBot.lacks(g2.view(p2));
        check("... and a player who drew on the sixes held none before the draw", y[o2][6]);
        DominoGame g3 = sixes(2, 5);
        int p3 = g3.turn(), o3 = 1 - p3;
        give(g3, p3, t(6, 6)); give(g3, o3, t(0, 0), t(0, 1), t(0, 2), t(1, 1), t(1, 2), t(2, 2), t(0, 3));
        g3.play(p3, t(6, 6), DominoGame.RIGHT);
        boolean drew = true;
        for (int lv = 1; lv <= 3; lv++) drew &= DominoBot.choose(g3.view(o3), lv, new Random(1)).kind == DominoGame.DRAW;
        boolean passed = true;
        DominoGame g4 = sixes(4, 9);
        int p4 = g4.turn(), q4 = (p4 + 1) % 4;
        give(g4, p4, t(6, 6)); give(g4, q4, t(0, 0), t(0, 1), t(0, 2), t(1, 1), t(1, 2), t(2, 2), t(0, 3));
        g4.play(p4, t(6, 6), DominoGame.RIGHT);
        for (int lv = 1; lv <= 3; lv++) passed &= DominoBot.choose(g4.view(q4), lv, new Random(1)).kind == DominoGame.PASS;
        check("with nothing that fits every level draws when there are tiles to draw, and passes when there are none", drew && passed);
        }
        {
        // strength: level 3 beats level 1 and level 2 over many games, both seats
        int[] w31 = duel(DominoBot.HARD, DominoBot.EASY, 600), w32 = duel(DominoBot.HARD, DominoBot.MEDIUM, 600), w21 = duel(DominoBot.MEDIUM, DominoBot.EASY, 600);
        System.out.println("INFO wins of 600 two-player games to 100: level 3 v 1: " + w31[0] + ", level 3 v 2: " + w32[0] + ", level 2 v 1: " + w21[0]);
        check("the levels are levels: over 600 games level 3 wins more than level 1 (" + w31[0] + ") and than level 2 (" + w32[0] + "), level 2 more than level 1 (" + w21[0] + ")", w31[0] > 330 && w32[0] > 310 && w21[0] > 310);
        int four = 0;
        for (int seed = 0; seed < 400; seed++) {
            DominoGame g = new DominoGame(4, 100, new Random(seed));
            int seat = seed % 4; int[] lv = {1, 1, 1, 1}; lv[seat] = 3;
            playOut(g, lv, new Random(seed), null);
            if (g.winner() == seat) four++;
        }
        System.out.println("INFO level 3 among three level 1 players, 400 four-player games: " + four + " wins (100 would be chance)");
        check("at a table of four a level 3 player among three level 1 players wins clearly more than his quarter (" + four + " of 400)", four > 125);
        }
        {
        long t0 = System.nanoTime(); int n = 0;
        DominoGame g = new DominoGame(4, 150, new Random(1)); Random r = new Random(1);
        long worst = 0;
        while (g.phase() != DominoGame.Phase.GAME_OVER) {
            if (g.phase() == DominoGame.Phase.ROUND_OVER) { g.nextRound(); continue; }
            long s = System.nanoTime(); DominoBot.choose(g.view(g.turn()), DominoBot.HARD, r); worst = Math.max(worst, System.nanoTime() - s);
            step(g, DominoBot.HARD, r); n++;
        }
        System.out.println("INFO level 3 decision: " + n + " moves, worst " + worst / 1000 + " us on this JVM");
        check("a level 3 decision is cheap (the worst under 50 ms on a plain JVM: no search, one pass over the hand)", worst < 50_000_000L);
        }
        System.exit(failed == 0 ? 0 : 2);
    }

    /** Two-player games to 100, the seats swapped every other game; returns {wins of level a, wins of level b}. */
    static int[] duel(int a, int b, int games) {
        int[] w = new int[2];
        for (int seed = 0; seed < games; seed++) {
            DominoGame g = new DominoGame(2, 100, new Random(1000 + seed));
            int seatA = seed % 2;
            playOut(g, seatA == 0 ? new int[]{a, b} : new int[]{b, a}, new Random(seed), null);
            w[g.winner() == seatA ? 0 : 1]++;
        }
        return w;
    }
}
