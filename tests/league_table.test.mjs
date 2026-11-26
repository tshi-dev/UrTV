// 4.4 beta 5, the league page phase (a) (docs/research/LEAGUE-V2-PHASE-A-PLAN.md §4), offline in a real browser: «جدول الترتيب» is the full
// table (18 rows, 13 cells: rank, movement, crest, club, P W D L GF GA GD Pts, the last five), the movement only where the file has mv, the zone
// lines only from the file's zones (no legend), the form newest first next to the points in the three styles of «شكل آخر المباريات», OK on a
// row opens the club, focused mode (the header goes, ▲ from row 1 and رجوع bring it back), the live toggle only while a game of the league is
// live (on: the live score added and the rows sorted again, off: the file), the followed club's row, the sizes at 1280x720 and 960x540, the
// large font (له / عليه go first), the safe area, a past season on the same rows (no movement, «البطل»), the English page.
//   node tests/league_table.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T16:30:00Z');
const AR = ['الهلال', 'النصر', 'الاتحاد', 'القادسية', 'الأهلي', 'التعاون', 'الاتفاق', 'الشباب', 'نيوم', 'الفتح', 'الخليج', 'الفيحاء', 'الحزم', 'الرياض', 'ضمك', 'الخلود', 'الأخدود', 'النجمة'];
const ID = i => String(136000 + i);
// 18 clubs, 11 games each; points falling by rank; mv: club 2 up 1, club 5 up 2, club 7 down 2, the rest 0, club 17 none (its first place)
const TABLE = AR.map((a, i) => ({ id: ID(i), n: 'Club ' + i, ar: a, p: 11, w: 9 - Math.floor(i / 2), d: 1, l: 1 + Math.floor(i / 2), gf: 30 - i, ga: 8 + i,
  pts: 28 - i, f: ['wwdlw', 'wdwww', 'lwwdw', 'wwlww', 'dlwdw'][i % 5], ...(i === 17 ? {} : { mv: i === 2 ? 1 : i === 5 ? 2 : i === 7 ? -2 : 0 }) }));
const ZONES = [{ from: 1, to: 3, k: 'acle' }, { from: 4, to: 4, k: 'acl2' }, { from: 16, to: 18, k: 'down' }];
const SPORTS = { v: 1, id: '4668', en: 'Saudi-Arabian Pro League', ar: 'دوري روشن السعودي', season: '2026-2027', updated: 'x', table: TABLE, zones: ZONES, last: [], next: [],
  seasons: ['2026-2027', '2025-2026'], champs: [] };
const PAST = { v: 1, id: '4668', season: '2025-2026', updated: 'x', table: TABLE.map(({ mv, ...r }) => r), results: [], champ: { s: '2025-2026', id: ID(0), n: 'Club 0', ar: 'الهلال', pts: 28 } };
// a live game of the league: club 9 (الفتح, 19 pts) 2-0 up on club 8 (نيوم, 20 pts), 34'
const LIVE = { idEvent: 'lv1', idLeague: '4668', strLeague: 'Saudi Pro League', strSeason: '2026-2027', idHomeTeam: ID(9), idAwayTeam: ID(8), strHomeTeam: 'Club 9', strAwayTeam: 'Club 8',
  intHomeScore: '2', intAwayScore: '0', strStatus: '1H', strProgress: '34', strTimestamp: '2026-11-04T16:00:00', dateEvent: '2026-11-04', strTime: '16:00:00', intRound: '12', strSport: 'Soccer' };

const browser = await chromium.launch();
async function open(hash, { live = false, size = [1920, 1080], files = {}, page = 'dist/tv.html' } = {}) {
  const ctx = await browser.newContext({ viewport: { width: size[0], height: size[1] }, timezoneId: 'Asia/Riyadh' });
  const all = Object.assign({ 'sports/4668.json': SPORTS, 'sports/4668/2025-2026.json': PAST, 'results/4668.json': { v: 1, id: '4668', results: [] } }, files);
  await ctx.route(/^https?:/, r => {
    const u = r.request().url(), ok = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    const js = /\/master\/dist\/(sports(?:-en)?\.js)$/.exec(u);
    if (js) return r.fulfill({ path: path.join(ROOT, 'dist', js[1]), headers: { 'Access-Control-Allow-Origin': '*' } });
    const a = /\/(?:data-archive|football\/archive)\/(.+)$/.exec(u);
    if (a) return all[a[1]] ? ok(all[a[1]]) : r.fulfill({ status: 404, body: '' });
    if (/livescore|eventsday|eventsnextleague|eventspastleague|eventsround/.test(u)) return ok(live ? { events: [LIVE], livescore: null } : { events: null });
    if (/thesportsdb|data-honours/.test(u)) return ok({ events: null, results: null, table: null });
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, page) + '#gap=0&matchpage=1&' + hash);
  await p.waitForTimeout(900);
  if (live) await p.evaluate(ev => { const n = 'Saudi-Arabian Pro League'; LEAGUE_DATA[n] = { updated: new Date().toISOString(), events: [ev] }; if (!CONFIG.leagues.includes(n)) CONFIG.leagues.push(n); MATCH_REV++; }, LIVE);
  await p.evaluate(() => mpGo({ k: 'league', id: '4668' }));
  await p.waitForTimeout(700);
  return { p, ctx, errors };
}
const table = p => p.evaluate(() => [...document.querySelectorAll('#mp-list ul.mpt li.tr')].map(li => ({ cells: li.children.length, text: li.innerText.replace(/\s+/g, ' ').trim(),
  name: li.querySelector('.nm span').textContent, mv: (li.querySelector('.mv') || {}).textContent || '', mvc: (li.querySelector('.mv') || {}).className || '',
  pts: li.querySelector('.pt').textContent, form: [...li.querySelectorAll('.fo i')].map(i => i.className), say: (li.querySelector('.fo') || {}).getAttribute && li.querySelector('.fo').getAttribute('aria-label'),
  f: li.classList.contains('f'), mine: li.classList.contains('mine'), zb: li.classList.contains('zb') })));
const zl = p => p.evaluate(() => [...document.querySelectorAll('#mp-list ul.mpt > li')].map(li => li.classList.contains('zl') ? 'Z:' + li.textContent.trim() : li.classList.contains('tr') ? 'r' : li.classList.contains('th') ? 'h' : li.className).join(' '));

{   // the full table: 18 rows of 13 cells, the movement, the zones, the form, the followed club
  const { p, ctx, errors } = await open('teams=' + ID(6));
  const t = await table(p);
  check('18 rows, each of 13 cells (rank, movement, crest, club, P W D L GF GA GD Pts, the last five), in the file\'s order with the Arabic names',
    t.length === 18 && t.every(r => r.cells === 13) && t[0].name === 'الهلال' && t[17].name === 'النجمة' && /^١ /.test(t[0].text), JSON.stringify(t.slice(0, 2)) + errors.join('|'));
  check('the column heads: # النادي لعب فاز تعادل خسر له عليه الفارق نقاط آخر خمس مباريات', await p.evaluate(() => document.querySelector('ul.mpt li.th').innerText.replace(/\s+/g, ' ').trim())
    === '# النادي لعب فاز تعادل خسر له عليه الفارق نقاط آخر خمس مباريات', await p.evaluate(() => document.querySelector('ul.mpt li.th').innerText));
  check('movement as the file says: «+١» green, «+٢», «−٢» red, «–» grey for 0, nothing without mv', t[2].mv === '+١' && /up/.test(t[2].mvc) && t[5].mv === '+٢' && t[7].mv === '−٢' && /dn/.test(t[7].mvc)
    && t[0].mv === '–' && /eq/.test(t[0].mvc) && t[17].mv === '' && !/up|dn|eq/.test(t[17].mvc), JSON.stringify(t.map(r => r.mv)));
  check('zones only from the file: a named line above the first place of each (no legend), a bare line closing one, none at the card\'s bottom edge',
    (await zl(p)) === 'h Z:دوري أبطال آسيا للنخبة r r r Z:دوري أبطال آسيا ٢ r Z: r r r r r r r r r r r Z:الهبوط r r r', await zl(p));
  check('the zone labels\' numbers follow the digits (٢, not 2)', (await zl(p)).includes('آسيا ٢'));
  check('the form: five dots (the default), newest first, a text for TalkBack', JSON.stringify(t[0].form) === JSON.stringify(['fdot w', 'fdot w', 'fdot d', 'fdot l', 'fdot w'])
    && t[0].say === 'آخر خمس مباريات: فوز، فوز، تعادل، خسارة، فوز', JSON.stringify([t[0].form, t[0].say]));
  const order = await p.evaluate(() => { const li = document.querySelectorAll('ul.mpt li.tr')[0], pt = li.querySelector('.pt').getBoundingClientRect(), d = [...li.querySelectorAll('.fo i')].map(i => i.getBoundingClientRect());
    return [Math.abs(d[0].right - pt.left), Math.abs(d[4].right - pt.left)]; });
  check('…the newest dot is the one next to the points', order[0] < order[1], JSON.stringify(order));
  check('the followed club\'s row: bold on its flat fill, and the table opens with the focus on it', t[6].mine && t[6].f && t.filter(r => r.mine).length === 1, JSON.stringify(t[6]));
  check('alternate rows on the zebra', t[1].zb && !t[0].zb && t[3].zb, JSON.stringify(t.slice(0, 4).map(r => r.zb)));
  check('no live game: no toggle', !(await p.evaluate(() => !!document.querySelector('ul.mpt li.mptg'))));
  check('the zone lines are not focusable (▼ walks the clubs only)', await p.evaluate(() => MP.rows.length === 18 && MP.rows.every(x => x.tr)));
  const opened = await p.evaluate(() => { MP.zone = 'rows'; MP.r = 6; mpRender(); dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' })); return MP.view.k + ':' + MP.view.id; });
  check('OK on row 7 opens that club', opened === 'club:' + ID(6), opened);
  check('no script error', !errors.length, errors.join('|'));
  await ctx.close();
}
{   // focused mode
  const { p, ctx } = await open('teams=' + ID(0));
  const st = () => p.evaluate(() => [document.getElementById('mp').classList.contains('mp-focus'), getComputedStyle(document.querySelector('#mp header')).display, getComputedStyle(document.getElementById('mp-days')).display, MP.zone]);
  check('the table opens with the header shown', JSON.stringify(await st()) === '[false,"flex","flex","rows"]', JSON.stringify(await st()));
  await p.evaluate(() => { MP.zone = 'days'; MP.d = 0; mpRender(); });
  await p.keyboard.press('ArrowDown');
  check('▼ from the strip into the table: focused mode, the header and the strip hidden (the side\'s seasons stay)', JSON.stringify(await st()) === '[true,"none","none","rows"]'
    && await p.evaluate(() => getComputedStyle(document.getElementById('mp-side')).display !== 'none'), JSON.stringify(await st()));
  const fit = await p.evaluate(() => { const l = document.getElementById('mp-list'); return [l.scrollHeight <= l.clientHeight + 1, [...document.querySelectorAll('ul.mpt li.tr')].every(li => li.getBoundingClientRect().bottom <= innerHeight)]; });
  check('…and all 18 rows fit without a scroll at 1920x1080', fit[0] && fit[1], JSON.stringify(fit));
  await p.evaluate(() => { MP.r = 0; mpRender(); });
  await p.keyboard.press('ArrowUp');
  check('▲ from row 1: the header again, the focus on the strip', JSON.stringify(await st()) === '[false,"flex","flex","days"]', JSON.stringify(await st()));
  await p.keyboard.press('Enter');
  check('OK on «جدول الترتيب» (the tab on): focused mode', (await st())[0] === true, JSON.stringify(await st()));
  const b = await p.evaluate(() => [mpBack(), document.getElementById('mp').classList.contains('mp-focus'), MP.view && MP.view.k]);
  check('رجوع: focused mode goes first (the league stays open), then the view under it', JSON.stringify(b) === '["closed",false,"league"]', JSON.stringify(b));
  await ctx.close();
}
{   // the live toggle
  const { p, ctx, errors } = await open('teams=' + ID(0), { live: true });
  const tg = await p.evaluate(() => { const li = document.querySelector('ul.mpt li.mptg'); return li && [...li.querySelectorAll('span')].map(s => s.textContent + (s.classList.contains('on') ? '*' : '')).join('|'); });
  check('a live game of the league: the toggle «الكل» / «مع المباريات الجارية», off (the file)', tg === 'الكل*|مع المباريات الجارية', tg + errors.join('|'));
  const before = await table(p);
  check('…off: the file as it is (نيوم 9th on 20, الفتح 10th on 19)', before[8].name === 'نيوم' && before[8].pts === '٢٠' && before[9].name === 'الفتح', JSON.stringify(before.slice(8, 10).map(r => [r.name, r.pts])));
  await p.evaluate(() => { const i = MP.rows.findIndex(x => x.tg); MP.zone = 'rows'; MP.r = i; mpRender(); dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' })); });
  const on = await table(p);
  const k = on.findIndex(r => r.name === 'الفتح'), n = on.findIndex(r => r.name === 'نيوم');
  check('…on: الفتح +3 (22, red, the minute after the name) goes above نيوم (20, still in its game, red), sorted again', k < n && on[k].pts === '٢٢' && on[n].pts === '٢٠'
    && /٣٤/.test(on[k].text) && on.filter(r => /ptl/.test(r.text) || false).length === 0, JSON.stringify(on.slice(6, 11).map(r => [r.name, r.pts, r.text.slice(0, 30)])));
  check('…the provisional points in red', await p.evaluate(() => document.querySelectorAll('ul.mpt li.tr .pt.ptl').length === 2));
  check('…nothing written back: the file still says 19 and 20', await p.evaluate(() => { const f = ARCH_C[ARCH_BASE + 'sports/4668.json'].j; return f.table[9].pts === 19 && f.table[8].pts === 20; }));
  await p.evaluate(() => { const i = MP.rows.findIndex(x => x.tg); MP.r = i; mpRender(); dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' })); });
  const off = await table(p);
  check('…off again: the file', off[8].name === 'نيوم' && off[9].name === 'الفتح' && off[9].pts === '١٩', JSON.stringify(off.slice(8, 10).map(r => [r.name, r.pts])));
  check('no script error', !errors.length, errors.join('|'));
  await ctx.close();
}
for (const [style, cls, n] of [['boxes', 'fbox w', ''], ['boxes-letters', 'fbox fbl w', 'ف']]) {   // «شكل آخر المباريات»
  const { p, ctx } = await open('formstyle=' + style + '&teams=' + ID(0));
  const t = await table(p);
  check(`formstyle=${style}: ${cls}, newest first${n ? ', the letters ف ت خ' : ''}`, t[0].form[0] === cls && t[0].form.length === 5
    && (await p.evaluate(() => document.querySelector('ul.mpt li.tr .fo').textContent)) === (n ? 'ففتخف' : ''), JSON.stringify(t[0].form));
  await ctx.close();
}
{ const { p, ctx } = await open('formstyle=pills&teams=' + ID(0)); check('formstyle: anything else is dots', (await table(p))[0].form[0] === 'fdot w'); await ctx.close(); }
// the sizes and the safe area at 1280x720 and 960x540 in the three styles, and the large font (the whole page ×1.15 as the mockups did)
for (const [w, h] of [[1920, 1080], [1280, 720], [960, 540]]) for (const style of ['dots', 'boxes', 'boxes-letters']) for (const big of [false, true]) {
  if (big && w === 1920) continue;
  const { p, ctx, errors } = await open('formstyle=' + style + '&teams=' + ID(6), { size: [w, h] });
  if (big) await p.evaluate(() => { document.documentElement.style.setProperty('--u', (innerWidth / 1920 * 1.15) + 'px'); mpRender(); });
  await p.evaluate(() => { MP.zone = 'rows'; MP.fx = 1; MP.r = 17; mpRender(); });
  const r = await p.evaluate(() => {
    const ref = parseFloat(getComputedStyle(document.querySelector('ul.mpt li.th .c')).fontSize), floor = innerWidth / 1920 * 24 - 0.5, small = [], out = [], spill = [];
    document.querySelectorAll('ul.mpt li').forEach(li => li.querySelectorAll('*').forEach(el => {
      if (!el.childElementCount && el.textContent.trim() && getComputedStyle(el).display !== 'none' && parseFloat(getComputedStyle(el).fontSize) < floor) small.push(el.className + ':' + el.textContent);
    }));
    const box = document.querySelector('#mp-list').getBoundingClientRect(), x0 = innerWidth * .05, y0 = innerHeight * .05;
    const ul = document.querySelector('ul.mpt').getBoundingClientRect();
    if (ul.left < x0 - 1 || ul.right > innerWidth - x0 + 1) out.push('table ' + Math.round(ul.left) + '-' + Math.round(ul.right));
    const f = document.querySelector('ul.mpt li.f').getBoundingClientRect();
    if (f.top < y0 - 1 || f.bottom > innerHeight - y0 + 1) out.push('focus ' + Math.round(f.top) + '-' + Math.round(f.bottom));
    document.querySelectorAll('ul.mpt li.tr').forEach(li => { const fo = li.querySelector('.fo'), b = fo.getBoundingClientRect();
      fo.querySelectorAll('i').forEach(i => { if (getComputedStyle(i).display === 'none') return; const c = i.getBoundingClientRect(); if (c.left < b.left - .5 || c.right > b.right + .5) spill.push(li.querySelector('.nm span').textContent); });
      if (li.scrollWidth > li.clientWidth + 1) spill.push('row ' + li.querySelector('.nm span').textContent); });
    const u = document.querySelector('ul.mpt');
    return { small: small.slice(0, 4), out, spill: spill.slice(0, 3), nga: u.classList.contains('nga'), f3: u.classList.contains('f3'), gfa: getComputedStyle(document.querySelector('ul.mpt li.tr .gfa')).display, ref, floor };
  });
  const label = `${w}x${h} ${style}${big ? ' large font' : ''}`;
  check(`${label}: no text under the rows' muted size (.mpi i), nothing outside the 5 % safe area, no form marker out of its column, no row spills`,
    !r.small.length && !r.out.length && !r.spill.length && !errors.length, JSON.stringify(r) + errors.join('|'));
  if (big && w === 1280) check(`${label}: «له / عليه» go first (the GF and GA columns hidden), nothing shrinks`, r.nga && r.gfa === 'none', JSON.stringify(r));
  await ctx.close();
}
{   // a past season (4.5 honours, the side's seasons) on the same rows: no movement, «البطل» after the champion
  const { p, ctx, errors } = await open('teams=' + ID(0));
  await p.waitForFunction(() => typeof SPORTS !== 'undefined' && SPORTS && SPORTS.hon, null, { timeout: 8000 }).catch(() => {});
  await p.evaluate(() => { const pin = MP.leagues.find(g => g.key === 'season2025-2026'); if (pin) pin.pick(); });
  await p.waitForTimeout(700);
  const t = await table(p);
  check('a past season: the same rows, no movement, «البطل» after the champion\'s name, no toggle', t.length === 18 && t.every(r => r.mv === '') && /الهلال البطل/.test(t[0].text)
    && !(await p.evaluate(() => !!document.querySelector('ul.mpt li.mptg'))), JSON.stringify(t.slice(0, 2)) + errors.join('|'));
  check('…and no zone line from the current season', !(await zl(p)).includes('Z:'), await zl(p));
  await ctx.close();
}
{   // the fallback when the archive has no table: TheSportsDB's lines as before (no movement, no zones invented)
  const { p, ctx } = await open('teams=' + ID(0), { files: { 'sports/4668.json': null } });
  check('no archive table: no full table drawn', !(await p.evaluate(() => !!document.querySelector('ul.mpt'))));
  await ctx.close();
}
{   // English
  const { p, ctx, errors } = await open('teams=' + ID(0), { page: 'dist/tv-en.html' });
  const h = await p.evaluate(() => [document.querySelector('ul.mpt li.th').innerText.replace(/\s+/g, ' ').trim(), document.querySelector('ul.mpt li.zl').textContent, document.querySelector('ul.mpt li.tr .fo').getAttribute('aria-label')]);
  check('the English page: the heads, a zone and the form\'s text in English', h[0] === '# Club P W D L GF GA GD Pts Last five' && h[1] === 'AFC Champions League Elite' && /^Last five games: win, win, draw/.test(h[2]), JSON.stringify(h) + errors.join('|'));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all league table tests passed');
