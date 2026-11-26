// 4.4 beta 5 the club page (docs/research/LEAGUE-DESIGN-STUDY.md §2c, mockup 6), offline in a real browser: tabs by key («النادي»
// «المباريات» «النتائج» «البطولات» «المواجهات», then follow); the header from the league's table file («الثاني في دوري روشن، ٢٦ نقطة من ١١
// مباراة») and the honours («آخر بطولة»); the overview: the last five as tiles (its goals first, newest at the start, in «شكل آخر المباريات»),
// the next game, its place in the table (a window of 5 rows, its own ringed, the real places, OK: the full table), the coming games, its titles.
// Nothing that the files do not say: no table file, no place line; no title, no «آخر بطولة»; no scorer line (the scorers file has no club).
//   node tests/club_page.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T16:30:00Z');
const iso = ms => new Date(ms).toISOString().slice(0, 19) + 'Z';
const AR = ['الهلال', 'النصر', 'الاتحاد', 'القادسية', 'الأهلي', 'التعاون', 'الاتفاق', 'الشباب'];
const ID = i => String(136000 + i);
const TABLE = AR.map((a, i) => ({ id: ID(i), n: 'Club ' + i, ar: a, p: 11, w: 9 - i, d: 1, l: 1 + i, gf: 30 - i, ga: 8 + i, pts: 28 - 2 * i, f: 'wwdlw', mv: 0 }));
const SPORTS = { v: 1, id: '4668', season: '2026-2027', table: TABLE, zones: [{ from: 1, to: 3, k: 'acle' }], last: [], next: [], seasons: ['2026-2027'], champs: [] };
// النصر (ID 1): five results, newest first 4-0 (away at الرياض… here الشباب), and its next game at home to الأهلي
const opp = [7, 6, 5, 4, 3, 2];
const LAST = opp.map((o, k) => ({ id: 'l' + k, lg: '4668', s: '2026-2027', t: iso(NOW - (k + 1) * 7 * 86400000), home: k % 2 === 0, vs: ID(o), vsn: 'Club ' + o,
  gf: [4, 2, 3, 1, 2, 0][k], ga: [0, 0, 1, 1, 0, 3][k], st: 'FT', r: String(10 - k) }));
const TEAM = { v: 1, id: ID(1), en: 'Club 1', ar: 'النصر', updated: 'x', last: LAST,
  next: [{ id: 'n1', lg: '4668', s: '2026-2027', t: iso(NOW + 3 * 3600000), home: true, vs: ID(4), vsn: 'Club 4', gf: null, ga: null, st: 'NS', r: '11' }] };
const CHAMPS = { v: 1, id: '4668', seasons: [{ s: '2025-2026', id: ID(1), en: 'Club 1', ar: 'النصر', src: 'archive+wikidata' }] };

const browser = await chromium.launch();
async function open(files, size = [1920, 1080], page = 'dist/tv.html', hash = '') {
  const ctx = await browser.newContext({ viewport: { width: size[0], height: size[1] }, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, r => {
    const u = r.request().url(), ok = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    const js = /\/master\/dist\/(sports(?:-en)?\.js)$/.exec(u);
    if (js) return r.fulfill({ path: path.join(ROOT, 'dist', js[1]), headers: { 'Access-Control-Allow-Origin': '*' } });
    const h = /data-honours\/(.+)$/.exec(u);
    if (h) return files['h:' + h[1]] ? ok(files['h:' + h[1]]) : r.fulfill({ status: 404, body: '' });
    const a = /\/(?:data-archive|football\/archive)\/(.+)$/.exec(u);
    if (a) return files[a[1]] ? ok(files[a[1]]) : r.fulfill({ status: 404, body: '' });
    if (/thesportsdb/.test(u)) return ok({ events: null, results: null, table: null });
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, page) + '#gap=0&matchpage=1&teams=' + ID(1) + hash);
  await p.waitForTimeout(900);
  await p.waitForFunction(() => typeof SPORTS !== 'undefined' && SPORTS && SPORTS.hon, null, { timeout: 8000 }).catch(() => {});
  await p.evaluate(id => mpGo({ k: 'club', id }), ID(1));
  await p.waitForTimeout(900);
  return { p, ctx, errors };
}
const ALL = { 'teams/136001.json': TEAM, 'sports/4668.json': SPORTS, 'h:champions/4668.json': CHAMPS };
{
  const { p, ctx, errors } = await open(ALL);
  const tabs = await p.evaluate(() => [...document.querySelectorAll('#mp-days li')].map(li => li.textContent + (li.classList.contains('on') ? '*' : '')).join('|'));
  check('the tabs by key: «النادي» (on) «المباريات» «النتائج» «البطولات» (a title is known) «المواجهات» (the next game\'s opponent), then follow', tabs === 'النادي*|المباريات|النتائج|البطولات|المواجهات|ألغِ متابعة النصر', tabs + errors.join('|'));
  const sub = await p.textContent('#mp-age');
  check('the header: its place, points and games from the league\'s table file, and its latest title from the honours',
    sub === 'الثاني في دوري روشن، ٢٦ نقطة من ١١ مباراة، آخر بطولة: دوري روشن ٢٠٢٥-٢٠٢٦', sub);
  const tiles = await p.evaluate(() => [...document.querySelectorAll('.mpcl .tl')].map(t => [t.querySelector('b').textContent, t.querySelector('span').textContent, t.querySelector('.fo i').className]));
  check('the last five as tiles: its goals first, newest at the start, the opponent under, the mark in the dots style',
    tiles.length === 5 && tiles[0][0] === '٤ : ٠' && tiles[0][1] === 'كلوب ٧' && tiles[0][2] === 'fdot w' && tiles[4][0] === '٢ : ٠' && tiles[3][2] === 'fdot d', JSON.stringify(tiles));
  const nx = await p.evaluate(() => document.querySelector('.mpcl .clnx').innerText.replace(/\s+/g, ' '));
  check('the next game beside them: the round, the two clubs, the day and time', /^المباراة القادمة، .*الجولة ١١ .+ – .+ اليوم /.test(nx), nx);
  const win = await p.evaluate(() => [...document.querySelectorAll('#mp-list ul.mpt li.tr')].map(li => li.querySelector('.rk').textContent + ':' + li.querySelector('.nm span').textContent + (li.classList.contains('mine') ? '*' : '')));
  check('«في الترتيب»: a window of five rows round it with their real places, its own row marked', JSON.stringify(win) === JSON.stringify(['١:الهلال', '٢:النصر*', '٣:الاتحاد', '٤:القادسية', '٥:الأهلي']), JSON.stringify(win));
  const go = await p.evaluate(() => { const x = MP.rows.find(r => r.tr); x.act(); return MP.view.k + ':' + MP.view.id; });
  check('OK on a row of the window: the full table', go === 'league:4668', go);
  await p.evaluate(() => mpBack());
  const t = await p.evaluate(() => document.getElementById('mp-list').innerText);
  check('the overview also has the coming games and «البطولات»; no scorer line (the scorers file names no club)', t.includes('المباريات القادمة') && t.includes('البطولات') && !t.includes('هدّاف'), t.slice(0, 300));
  await p.evaluate(() => { MP.view.tab = 2; mpRender(); });
  check('«النتائج»: its last results (all ten kept, six here)', await p.evaluate(() => MP.rows.length === 6));
  await p.evaluate(() => { MP.view.tab = 1; mpRender(); });
  check('«المباريات»: its coming games by round', await p.evaluate(() => MP.rows.length === 1 && /الجولة ١١/.test(document.getElementById('mp-list').innerText)));
  check('no script error', !errors.length, errors.join('|'));
  await ctx.close();
}
{   // nothing invented: no table file, no honours: no place, no title, no window; the tiles and the games stay
  const { p, ctx, errors } = await open({ 'teams/136001.json': TEAM });
  const sub = await p.textContent('#mp-age'), tabs = await p.evaluate(() => [...document.querySelectorAll('#mp-days li')].map(li => li.textContent).join('|'));
  check('no table file and no title known: no header line, no «البطولات» tab, no table window', sub === '' && tabs === 'النادي|المباريات|النتائج|المواجهات|ألغِ متابعة النصر'
    && !(await p.evaluate(() => !!document.querySelector('ul.mpt'))) && (await p.evaluate(() => document.querySelectorAll('.mpcl .tl').length)) === 5, sub + ' / ' + tabs + errors.join('|'));
  await ctx.close();
}
for (const [w, h] of [[1280, 720], [960, 540]]) for (const style of ['dots', 'boxes-letters']) {   // the sizes: nothing under the rows' muted size, the tiles inside the screen
  const { p, ctx, errors } = await open(ALL, [w, h], 'dist/tv.html', '&formstyle=' + style);
  const r = await p.evaluate(() => {
    const floor = innerWidth / 1920 * 24 - 0.5, small = [], out = [];
    document.querySelectorAll('.mpcl *, #mp-list ul.mpt li *').forEach(el => { if (!el.childElementCount && el.textContent.trim() && getComputedStyle(el).display !== 'none' && parseFloat(getComputedStyle(el).fontSize) < floor) small.push(el.className + ':' + el.textContent); });
    const b = document.querySelector('.mpcl').getBoundingClientRect();
    if (b.left < innerWidth * .05 - 1 || b.right > innerWidth * .95 + 1) out.push(Math.round(b.left) + '-' + Math.round(b.right));
    return { small: small.slice(0, 4), out };
  });
  check(`${w}x${h} ${style}: no text under the rows' muted size, the tiles and the next game inside the safe area`, !r.small.length && !r.out.length && !errors.length, JSON.stringify(r) + errors.join('|'));
  await ctx.close();
}
{   // English
  const { p, ctx, errors } = await open(ALL, [1920, 1080], 'dist/tv-en.html');
  const sub = await p.textContent('#mp-age');
  check('English: the header line in English', /^2nd in .+, 26 points from 11 games, Latest title: .+ 2025-2026$/.test(sub), sub + errors.join('|'));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all club page tests passed');
