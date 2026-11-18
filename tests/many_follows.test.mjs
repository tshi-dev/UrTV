// 4.4 more than 8 leagues and 20 clubs (FollowLimit: past them one warning, once; at most 24 and 40) in a real browser, offline:
// the page takes 40 clubs and 24 leagues, asks for the ones past the soft limits every 2nd load (3rd on a weak TV), downloads the
// league files 4 at a time, builds the bar's games once a minute (not at each page turn) with at most 48 of the leagues' own (24 on a weak
// TV), its «تابع» shows the one warning and the ceiling from the app, and «فرقي» still fits at 720p.
//   node tests/many_follows.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T04:13:00Z');
const TEAMS = Array.from({ length: 40 }, (_, i) => String(136000 + i));
// the built-in leagues (each its own file, the page's FB list), then catalogue ids up to 24
const BUILTIN = [...fs.readFileSync(path.join(ROOT, 'android/app/src/main/java/sa/urtv/screensaver/Prefs.java'), 'utf8')
  .split('static final String[][] LEAGUES = {')[1].split('};')[0].matchAll(/\{"([^"]+)", "/g)].map(m => m[1]).filter(n => /^[A-Za-z0-9 \-]{2,40}$/.test(n));
const LEAGUES = BUILTIN.slice(0, 16).concat(Array.from({ length: 8 }, (_, i) => 'id' + (5000 + i)));
const iso = ms => new Date(ms).toISOString().slice(0, 19);
const ev = (i, lg, at) => ({ idEvent: String(900000 + i), idLeague: lg, idHomeTeam: String(200000 + 2 * i), idAwayTeam: String(200001 + 2 * i), strHomeTeam: 'Home ' + i, strAwayTeam: 'Away ' + i,
  strStatus: 'NS', strTimestamp: iso(at), dateEvent: iso(at).slice(0, 10), strTime: iso(at).slice(11), strSport: 'Soccer', strSeason: '2026-2027', intRound: '13' });

const browser = await chromium.launch();
async function open(hash, { width = 1920, height = 1080, delay = 0, init = null } = {}) {
  const ctx = await browser.newContext({ viewport: { width, height }, timezoneId: 'Asia/Riyadh' });
  if (init) await ctx.addInitScript(init);
  const seen = { next: [], files: 0, inflight: 0, peak: 0 };
  await ctx.route(/^https?:/, async r => {
    const u = r.request().url();
    const ok = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    if (/eventsnext\.php\?id=/.test(u)) { seen.next.push(u.split('id=')[1]); return ok({ events: null }); }
    if (/eventslast\.php|eventsnextleague|eventspastleague/.test(u)) return ok({ events: null, results: null });
    if (/\/data-leagues\/leagues\/(?!tournaments)/.test(u) || /\/football\/leagues\/(?!tournaments)/.test(u)) {
      seen.files++; seen.inflight++; seen.peak = Math.max(seen.peak, seen.inflight);
      await new Promise(res => setTimeout(res, delay));
      seen.inflight--;
      return ok({ league: 'x', id: '1', updated: '2026-11-04T04:00:00Z', events: [] });
    }
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#city=الرياض&lat=24.7136&lon=46.6753&tz=Asia/Riyadh&gap=0&' + hash);
  await p.waitForTimeout(1500);
  return { p, ctx, errors, seen };
}
const all = `teams=${TEAMS.join(',')}&leagues=${encodeURIComponent(LEAGUES.join(','))}`;

{   // the page takes them all; the clubs past 20 are asked every 2nd load; the files 4 at a time
  const { p, ctx, errors, seen } = await open(all + '&matchpage=1', { delay: 150 });
  const n = await p.evaluate(() => [CONFIG.teamIds.length, CONFIG.leagues.length]);
  check('the page takes 40 clubs and 24 leagues (FollowLimit\'s ceilings)', n[0] === 40 && n[1] === 24 && !errors.length, JSON.stringify(n) + errors.join('|'));
  const first = new Set(seen.next).size;
  check('the first load asks for every club', first === 40, first);
  check('the league files: four at a time, never all together', seen.peak > 0 && seen.peak <= 4 && seen.files >= 16, `peak ${seen.peak}, files ${seen.files}`);
  const loads = [];
  for (let k = 0; k < 3; k++) { seen.next.length = 0; await p.evaluate(() => loadMatches()); loads.push(new Set(seen.next).size); }
  check('then the clubs past 20 every 2nd load: 20, 40, 20', JSON.stringify(loads) === '[20,40,20]', JSON.stringify(loads));
  await ctx.close();
}
{   // a weak TV (#lowram=1): every 3rd load
  const { p, ctx, seen } = await open(all + '&matchpage=1&lowram=1');
  const loads = [];
  for (let k = 0; k < 3; k++) { seen.next.length = 0; await p.evaluate(() => loadMatches()); loads.push(new Set(seen.next).size); }
  check('a weak TV: the clubs past 20 every 3rd load: 20, 20, 40', JSON.stringify(loads) === '[20,20,40]', JSON.stringify(loads));
  await ctx.close();
}
for (const [lowram, max] of [['', 48], ['&lowram=1', 24]]) {   // the bar on the screensaver: built once a minute, the leagues' own games at most 48 (24)
  const { p, ctx, errors } = await open(`teams=136013&leagues=${encodeURIComponent(LEAGUES.slice(0, 2).join(','))}${lowram}`);
  const r = await p.evaluate(([games]) => {
    LEAGUE_DATA[CONFIG.leagues[0]] = { updated: new Date().toISOString(), events: games };
    let calls = 0; const real = leagueShow; leagueShow = (...a) => { calls++; return real(...a); };
    MATCH_REV++; renderMatches(); renderMatches(true); renderMatches(true); renderMatches(true);
    return { calls, n: MATCH_MEMO.games.length, pages: MATCH_PAGES };
  }, [Array.from({ length: 150 }, (_, i) => ev(i, '4668', NOW + (i + 1) * 3600000))]);
  check(`the bar${lowram ? ' on a weak TV' : ''}: the leagues' games built once for four page turns, at most ${max} of them`, !errors.length && r.calls === 1 && r.n <= max + 1 && r.n >= max - 1, JSON.stringify(r) + errors.join('|'));
  await ctx.close();
}
{   // «تابع» on the page: the app's answer (add / warn / full), the one warning and its second OK, an older screensaver's old limits
  const host = `window.__calls = []; window.__state = 'warn'; window.UrtvMatches = { followCheck: k => (window.__calls.push('check:' + k), window.__state),
    followWarned: k => (window.__calls.push('warned:' + k), window.__state = 'add', true), follow: (id) => (window.__calls.push('follow:' + id), true),
    followLeague: (id) => (window.__calls.push('league:' + id), true), unfollow: () => true, unfollowLeague: () => true };`;
  const { p, ctx, errors } = await open(`teams=${TEAMS.slice(0, 20).join(',')}&matchpage=1`, { init: host });
  const step = () => p.evaluate(() => { mpFollowTeam('999001'); return { note: MP.note, calls: window.__calls.slice() }; });
  const a = await step();
  check('past 20 clubs, the first OK: the warning, nothing followed', a.note.includes('إضافة أكثر من ٢٠ قد تبطّئ التلفزيون إذا كان ضعيف') && !a.calls.some(c => c.startsWith('follow')), JSON.stringify(a));
  const b = await step();
  check('the second OK is «أضف»: the app remembers it and the club is followed (no third OK)', b.calls.includes('warned:teams') && b.calls.includes('follow:999001'), JSON.stringify(b));
  const c = await p.evaluate(() => { window.__calls = []; mpFollowTeam('999002'); mpFollowTeam('999002'); return { calls: window.__calls, note: MP.note }; });
  check('after «أضف» never again: two OKs follow the next one as always', !c.calls.some(x => x.startsWith('warned')) && c.calls.includes('follow:999002'), JSON.stringify(c));
  const d = await p.evaluate(() => { window.__state = 'full'; window.__calls = []; mpFollowTeam('999003'); mpFollowTeam('999003'); return { calls: window.__calls, note: MP.note }; });
  check('at the ceiling: the words that say so, never a silent no', d.note.includes('٤٠') && !d.calls.some(x => x.startsWith('follow')), JSON.stringify(d));
  const e = await p.evaluate(() => { window.__state = 'warn'; window.__calls = []; mpFollowLeague({ id: '5099', ar: 'دوري', en: 'X League' }); return { calls: window.__calls, note: MP.note }; });
  check('a league past 8: its own warning', e.note.includes('إضافة أكثر من ٨') && e.calls.includes('check:leagues') && !e.calls.some(x => x.startsWith('league:')), JSON.stringify(e));
  check('no script error', !errors.length, errors.join('|'));
  await ctx.close();
  const old = `window.UrtvMatches = { follow: () => (window.__f = 1, true), unfollow: () => true };`;   // an older screensaver: no followCheck
  const o = await open(`teams=${TEAMS.slice(0, 20).join(',')}&matchpage=1`, { init: old });
  const f = await o.p.evaluate(() => { mpFollowTeam('999004'); mpFollowTeam('999004'); return { note: MP.note, f: window.__f || 0 }; });
  check('an older screensaver: 20 clubs stay the limit, said so', f.note.includes('٢٠') && !f.f, JSON.stringify(f));
  await o.ctx.close();
}
for (const [w, h] of [[1280, 720], [960, 540]]) {   // «فرقي» with 40 clubs and 24 leagues at 720p: the focused row on screen, no sideways scroll
  const { p, ctx, errors } = await open(all + '&matchpage=1', { width: w, height: h });
  await p.evaluate(() => mpGo({ k: 'mine' }));
  await p.waitForTimeout(200);
  const res = [];
  for (let k = 0; k < 70; k++) {
    await p.keyboard.press('ArrowDown');
    if (k % 10 === 9 || k === 69) res.push(await p.evaluate(() => { const f = document.querySelector('#mp .ring, #mp li.on, #mp [data-on="1"]'), b = f && f.getBoundingClientRect();
      return { has: !!f, inside: !!b && b.top >= -1 && b.bottom <= innerHeight + 1, wide: document.documentElement.scrollWidth <= innerWidth + 1, text: f ? f.textContent.slice(0, 20) : '' }; }));
  }
  const bad = res.filter(r => !r.has || !r.inside || !r.wide);
  check(`«فرقي» at ${w}x${h} with 40 clubs and 24 leagues: the focused row always on screen, nothing wider than the screen`, !bad.length && !errors.length, JSON.stringify(bad.slice(0, 3)) + errors.join('|'));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all many-follows tests passed');
