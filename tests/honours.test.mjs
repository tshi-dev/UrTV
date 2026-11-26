// 4.5 «الأبطال», «الهدّافون» and a league's past seasons (docs/HONOURS-STUDY.md), offline in a real browser: the side's seasons, a past
// season's final table and results from its own file, the champions newest first (the archive's with points, older ones by name, the season
// under way «لم يُحسم بعد» and OK opens its table), the top scorers with their source, the club page's titles (a count only when the
// league's list is complete), English, 720p. The honours part is in dist/sports.js, loaded beside the page.
//   node tests/honours.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const NOW = Date.parse('2026-11-04T04:13:00Z');
const row = (id, n, ar, p, pts) => ({ id, n, ar, p, w: 0, d: 0, l: 0, gf: 0, ga: 0, pts, f: '' });
const SPORTS = { v: 1, id: '4668', en: 'Saudi-Arabian Pro League', ar: 'دوري روشن السعودي', season: '2026-2027', updated: 'x', last: [], next: [],
  table: [row('136013', 'Al-Hilal', 'الهلال', 10, 26), row('136014', 'Al-Nassr', 'النصر', 10, 22)],
  seasons: ['2026-2027', '2025-2026', '2024-2025', '2023-2024'],
  champs: [{ s: '2025-2026', id: '136014', n: 'Al-Nassr', ar: 'النصر', pts: 86, gap: 2, vs: '136013', vn: 'Al-Hilal', var: 'الهلال' },
           { s: '2024-2025', id: '136015', n: 'Al-Ittihad', ar: 'الاتحاد', pts: 83, gap: 8, vs: '136013', vn: 'Al-Hilal', var: 'الهلال' },
           { s: '2023-2024', id: '136013', n: 'Al-Hilal', ar: 'الهلال', pts: 96, gap: 14, vs: '136014', vn: 'Al-Nassr', var: 'النصر' }] };
const PAST = { v: 1, id: '4668', season: '2025-2026', updated: 'x',
  table: [row('136014', 'Al-Nassr', 'النصر', 34, 86), row('136013', 'Al-Hilal', 'الهلال', 34, 84), row('136015', 'Al-Ittihad', 'الاتحاد', 34, 71)],
  results: [{ id: 'p2', lg: '4668', t: '2026-05-20T18:00:00Z', r: '34', h: '136014', a: '136013', hn: 'Al-Nassr', an: 'Al-Hilal', hs: '2', as: '1', st: 'FT' },
            { id: 'p1', lg: '4668', t: '2026-05-13T18:00:00Z', r: '33', h: '136013', a: '136015', hn: 'Al-Hilal', an: 'Al-Ittihad', hs: '3', as: '0', st: 'FT' }],
  champ: SPORTS.champs[0] };
// the weekly file: 2023-24 cross-checked, an old season by name only; 2024-25 held (its sources disagreed): the archive's is not shown then
const CHAMPS = { v: 1, id: '4668', complete: false, updated: 'x', held: ['2024-2025'],
  seasons: [{ s: '2025-2026', id: '136014', en: 'Al-Nassr', ar: 'النصر', src: 'archive+wikidata', pts: 86, gap: 2, vs: '136013', vn: 'Al-Hilal', var: 'الهلال' },
            { s: '2023-2024', id: '136013', en: 'Al-Hilal', ar: 'الهلال', src: 'archive+wikidata', pts: 96, gap: 14, vs: '136014', vn: 'Al-Nassr', var: 'النصر' },
            { s: '2017-2018', id: '136013', en: 'Al Hilal SFC', ar: 'الهلال', src: 'wikidata' },
            { s: '1977-1978', en: 'Al-Ahli', ar: 'الأهلي', src: 'wikipedia', url: ['https://ar.wikipedia.org/wiki/x'] }] };
const SCORERS = { v: 1, id: '4668', lic: 'CC BY-SA 4.0', updated: 'x', seasons: {
  '2024-2025': { top: [{ en: 'Cristiano Ronaldo', ar: 'كريستيانو رونالدو', goals: 25 }], src: ['https://ar.wikipedia.org/wiki/a', 'https://en.wikipedia.org/wiki/b'] },
  '2023-2024': { top: [{ en: 'Cristiano Ronaldo', ar: 'كريستيانو رونالدو', goals: 35 }], src: [] },
  '2021-2022': { top: [{ en: 'Odion Ighalo', ar: '', goals: 24 }, { en: 'Second Player', ar: 'لاعب ثان', goals: 24 }], src: [] } } };
const TEAM = { v: 1, id: '136013', en: 'Al-Hilal', ar: 'الهلال', updated: 'x', last: [{ id: 'l1', lg: '4668', s: '2026-2027', t: '2026-10-30T18:00:00Z', home: true, vs: '136014', vsn: 'Al-Nassr', gf: 2, ga: 1, st: 'FT', r: '9' }], next: [] };

const browser = await chromium.launch();
async function open(files, { en = false, width = 1920, height = 1080 } = {}) {
  const ctx = await browser.newContext({ viewport: { width, height }, timezoneId: 'Asia/Riyadh' });
  const asked = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    asked.push(u);
    const ok = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    const js = /\/master\/dist\/(sports(?:-en)?\.js)$/.exec(u);   // the honours part is in dist/sports.js (the test's browser reads no file: XHR, the TV's WebView does)
    if (js) return r.fulfill({ path: path.join(ROOT, 'dist', js[1]), headers: { 'Access-Control-Allow-Origin': '*' } });
    const a = /\/(?:data-archive|football\/archive|data-honours|football\/honours)\/(.+)$/.exec(u);
    const k = a && (/honours/.test(u) ? 'h:' : '') + a[1];
    if (a && files[k] !== undefined) return files[k] === null ? r.fulfill({ status: 404, body: '' }) : ok(files[k]);
    if (a) return r.fulfill({ status: 404, body: '' });
    if (/thesportsdb/.test(u)) return ok({ table: null, events: null, results: null });
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(NOW);
  await p.goto('file://' + path.join(ROOT, en ? 'dist/tv-en.html' : 'dist/tv.html') + '#gap=0&matchpage=1&teams=136013');
  await p.waitForTimeout(1000);
  return { p, ctx, errors, asked };
}
const ALL = { 'sports/4668.json': SPORTS, 'sports/4668/2025-2026.json': PAST, 'results/4668.json': { v: 1, id: '4668', results: [] },
  'h:champions/4668.json': CHAMPS, 'h:scorers/4668.json': SCORERS, 'teams/136013.json': TEAM };
const side = p => p.evaluate(() => [...document.querySelectorAll('#mp-side li')].map(li => li.innerText.replace(/\s+/g, ' ').trim()));
const rows = p => p.evaluate(() => [...document.querySelectorAll('#mp li.mpg, #mp ul.mpt li.tr')].map(li => li.innerText.replace(/\s+/g, ' ').trim()));   // 4.4 beta 5: a table's rows are the full table's
const text = p => p.evaluate(() => document.getElementById('mp').innerText.replace(/\s+/g, ' '));
const key = async (p, k, n = 1) => { for (let i = 0; i < n; i++) { await p.keyboard.press(k); await p.waitForTimeout(60); } await p.waitForTimeout(300); };

{   // the league page: the seasons in the side, a past season by the remote: its final table, then its results
  const { p, ctx, errors } = await open(ALL);
  await p.evaluate(() => mpGo({ k: 'league', id: '4668' }));
  await p.waitForTimeout(800);
  const s = await side(p);
  check('the side: the archive\'s seasons, the current one first and marked «الحالي»', s.length === 4 && /^٢٠٢٦-٢٠٢٧ الحالي$/.test(s[0]) && s[1] === '٢٠٢٥-٢٠٢٦', JSON.stringify(s));
  check('the current season: its table (4.4 beta 5 the full table\'s rows)', /^١ الهلال /.test((await rows(p))[0] || ''), JSON.stringify(await rows(p)));
  await key(p, 'ArrowRight');   // ◀ in RTL is ArrowRight: to the side
  await key(p, 'ArrowDown');
  await key(p, 'Enter');
  const r = await rows(p), t = await text(p);
  check('OK on 2025-26: its final table from its own file, the champion said on its row, OK on a row is that club',
    !errors.length && t.includes('جدول الترتيب النهائي ٢٠٢٥-٢٠٢٦') && /^١ النصر البطل ٣٤ .* ٨٦$/.test(r[0] || '') && r.length === 3, JSON.stringify(r) + errors.join('|'));
  const lit = await p.evaluate(() => document.querySelector('#mp-side li.on').innerText.trim());
  check('…the chosen season is the lit one in the side', lit.startsWith('٢٠٢٥-٢٠٢٦'), lit);
  await p.evaluate(() => MP.tabs[1].act());
  await p.waitForTimeout(400);
  const t2 = await text(p);
  check('«النتائج» of 2025-26: its results by round, newest first', t2.includes('الجولة ٣٤') && t2.indexOf('الجولة ٣٤') < t2.indexOf('الجولة ٣٣'), t2.slice(0, 300));
  const asked = await p.evaluate(() => Object.keys(ARCH_C).filter(u => /sports\/4668\//.test(u)).map(u => u.split('/').pop()));
  check('only the chosen season\'s file is downloaded', JSON.stringify(asked) === '["2025-2026.json"]', JSON.stringify(asked));
  await ctx.close();
}
{   // «الأبطال»
  const { p, ctx, errors } = await open(ALL);
  await p.evaluate(() => mpGo({ k: 'league', id: '4668', tab: 3 }));
  await p.waitForTimeout(800);
  const r = await rows(p), t = await text(p);
  check('«الأبطال»: the season under way first «لم يُحسم بعد، افتح الجدول» (the focus on it), then the archive\'s champions with points and the margin',
    !errors.length && r[0] === '٢٠٢٦-٢٠٢٧ لم يُحسم بعد، افتح الجدول' && r[1] === '٢٠٢٥-٢٠٢٦: النصر ٨٦ نقطة، بفارق ٢ عن الهلال', JSON.stringify(r) + errors.join('|'));
  check('…a season the weekly check held (its sources disagreed) is not shown, even though the archive names it', !t.includes('٢٠٢٤-٢٠٢٥'), t.slice(0, 400));
  check('…older seasons under «قبل ذلك», by name only', t.includes('قبل ذلك') && r.includes('٢٠١٧-٢٠١٨: الهلال') && r.includes('١٩٧٧-١٩٧٨: الأهلي'), JSON.stringify(r));
  check('…the source under the list, Wikipedia\'s licence named because a season came from it', t.includes('المصدر: نتائج المباريات وويكي بيانات وويكيبيديا (CC BY-SA 4.0)'), t.slice(-200));
  check('the focus starts on «لم يُحسم بعد»', await p.evaluate(() => /لم يُحسم بعد/.test((document.querySelector('#mp li.mpg.f') || {}).innerText || '')));
  const v = await p.evaluate(() => { MP.zone = 'rows'; MP.r = 0; MP.rows[0].act(); return [MP.view.tab || 0, MP.view.season || '']; });
  await p.waitForTimeout(300);
  check('OK on «لم يُحسم بعد»: the season under way\'s table', JSON.stringify(v) === '[0,""]' && /^١ الهلال /.test((await rows(p))[0] || ''), JSON.stringify(v));
  const c = await p.evaluate(() => { MP.view.tab = 3; mpRender(); MP.zone = 'rows'; MP.r = 1; MP.rows[1].act(); return MP.view.k + ':' + MP.view.id; });
  check('OK on a champion: that club\'s page', c === 'club:136014', c);
  check('no «·» or «•»', !/[·•]/.test(t), '');
  await ctx.close();
}
{   // 4.4 beta 2: before the data exists (no seasons in the archive's file, no weekly files): no new tab, no seasons, no «البطولات»
  const OLD = { 'sports/4668.json': Object.assign({}, SPORTS, { seasons: undefined, champs: undefined }), 'results/4668.json': { v: 1, id: '4668', results: [] }, 'teams/136013.json': TEAM };
  const { p, ctx, errors } = await open(OLD);
  await p.evaluate(() => mpGo({ k: 'league', id: '4668', tab: 3 }));
  await p.waitForTimeout(900);
  const tabs = await p.$$eval('#mp-days li', ls => ls.map(l => l.textContent).join());
  check('no data yet: the three tabs as before (no empty «الأبطال» or «الهدّافون»), a tab asked for that is not there opens the table',
    !errors.length && tabs === 'جدول الترتيب,النتائج,المباريات' && await p.evaluate(() => /الهلال/.test(document.getElementById('mp-list').innerText)), tabs + errors.join('|'));
  check('…and no seasons in the side', (await side(p)).length <= 1 && !(await side(p)).some(x => /^٢٠٢٥/.test(x)), JSON.stringify(await side(p)));
  await p.evaluate(() => mpGo({ k: 'club', id: '136013' }));
  await p.waitForTimeout(700);
  check('…and the club page has no «البطولات»', !(await text(p)).includes('البطولات'), (await text(p)).slice(0, 300));
  await ctx.close();
}
{   // only scorers known: «الهدّافون» alone, at its place after «المباريات»
  const ONE = { 'sports/4668.json': Object.assign({}, SPORTS, { champs: [] }), 'h:scorers/4668.json': SCORERS, 'results/4668.json': { v: 1, id: '4668', results: [] } };
  const { p, ctx } = await open(ONE);
  await p.evaluate(() => mpGo({ k: 'league', id: '4668' }));
  await p.waitForTimeout(900);
  check('only scorers known: four tabs, «الهدّافون» the fourth, and it opens', await p.$$eval('#mp-days li', ls => ls.map(l => l.textContent).join()) === 'جدول الترتيب,النتائج,المباريات,الهدّافون'
    && await p.evaluate(() => { MP.tabs[3].act(); return MP.view.tab === 4; }), await p.$$eval('#mp-days li', ls => ls.map(l => l.textContent).join()));
  await ctx.close();
}
{   // the named parts the league page v2's cards call (SPORTS.hon), and what they give
  const { p, ctx, errors } = await open(ALL);
  await p.evaluate(() => mpGo({ k: 'league', id: '4668', tab: 3 }));
  await p.waitForTimeout(800);
  const r = await p.evaluate(() => { const H = SPORTS.hon, sp = archWant('sports/4668.json'), sc = H.want('scorers', '4668'), last = H.scorerSeasons(sc, sp).find(x => H.top(sc, x).length);
    return { names: ['league', 'club', 'has', 'champList', 'champRow', 'top', 'scorers', 'scorerSeasons', 'want'].filter(k => typeof H[k] === 'function').length,
      champs: H.champList('4668', sp).list.slice(0, 3).map(c => H.champRow({}, c).t), last, top: H.top(sc, last).map(t => t.goals), has: H.has('4668') }; });
  check('SPORTS.hon names its parts for the v2 cards: the last 3 champions\' rows, last season\'s top scorer, has()',
    !errors.length && r.names === 9 && r.champs[0] === '٢٠٢٥-٢٠٢٦: النصر' && r.champs.length === 3 && r.last === '2024-2025' && r.top[0] === 25 && r.has.champs && r.has.scorers, JSON.stringify(r) + errors.join('|'));
  await ctx.close();
}
{   // «الأبطال» with no weekly file yet: the archive's own champions
  const { p, ctx } = await open(Object.assign({}, ALL, { 'h:champions/4668.json': null }));
  await p.evaluate(() => mpGo({ k: 'league', id: '4668', tab: 3 }));
  await p.waitForTimeout(800);
  const r = await rows(p), t = await text(p);
  check('no weekly file: the archive\'s three champions, and the plain source line', r.length === 4 && r[2] === '٢٠٢٤-٢٠٢٥: الاتحاد ٨٣ نقطة، بفارق ٨ عن الهلال'
    && t.includes('المصدر: نتائج المباريات وويكي بيانات') && !t.includes('ويكيبيديا'), JSON.stringify(r));
  await ctx.close();
}
{   // «الهدّافون»
  const { p, ctx, errors } = await open(ALL);
  await p.evaluate(() => mpGo({ k: 'league', id: '4668', tab: 4 }));
  await p.waitForTimeout(800);
  const t = await text(p), r = await rows(p), s = await side(p);
  check('«الهدّافون»: the newest season that has one (the season under way has none), its player, his goals, the source and its licence',
    !errors.length && t.includes('الهدّافون ٢٠٢٤-٢٠٢٥') && r[0] === 'كريستيانو رونالدو ٢٥ هدفًا' && t.includes('المصدر: ويكيبيديا وويكي بيانات (CC BY-SA 4.0)'), t.slice(0, 300) + errors.join('|'));
  check('…the side: the archive\'s seasons and the file\'s, newest first', s.map(x => x.split(' ')[0]).join() === '٢٠٢٦-٢٠٢٧,٢٠٢٥-٢٠٢٦,٢٠٢٤-٢٠٢٥,٢٠٢٣-٢٠٢٤,٢٠٢١-٢٠٢٢', JSON.stringify(s));
  await p.evaluate(() => { const i = MP.leagues.findIndex(g => g.key === 'season2021-2022'); MP.leagues[i].pick(); });
  await p.waitForTimeout(300);
  const r2 = await rows(p);
  check('a shared top scorer: both, the Latin name when the data has no Arabic one', JSON.stringify(r2) === '["Odion Ighalo ٢٤ هدفًا","لاعب ثان ٢٤ هدفًا"]', JSON.stringify(r2));
  await p.evaluate(() => { const i = MP.leagues.findIndex(g => g.key === 'season2025-2026'); MP.leagues[i].pick(); });
  await p.waitForTimeout(300);
  const t3 = await text(p);
  check('a season with none: said so, no source line', t3.includes('لا نعرف هدّاف هذا الموسم بعد') && !t3.includes('المصدر'), t3.slice(0, 300));
  await ctx.close();
}
{   // the club page: its titles, the latest only while the list is incomplete; the count once it is complete
  const { p, ctx, errors } = await open(ALL);
  await p.evaluate(() => mpGo({ k: 'club', id: '136013' }));
  await p.waitForTimeout(1000);
  const t = await text(p);
  check('the club page: «البطولات», the league and its latest title only (the list is not complete: no count)',
    !errors.length && t.includes('البطولات') && t.includes('آخرها ٢٠٢٣-٢٠٢٤') && !/لقب/.test(t), t.slice(0, 500) + errors.join('|'));
  const go = await p.evaluate(() => { const i = MP.rows.findIndex(x => x.it && /آخرها/.test(x.sub)); MP.zone = 'rows'; MP.r = i; MP.rows[i].act(); return [MP.view.k, MP.view.tab]; });
  check('OK on it: the league\'s «الأبطال»', JSON.stringify(go) === '["league",3]', JSON.stringify(go));
  await ctx.close();
  const done = Object.assign({}, ALL, { 'h:champions/4668.json': Object.assign({}, CHAMPS, { complete: true, titles: { '136013': 19, '136014': 11 } }) });
  const o = await open(done);
  await o.p.evaluate(() => mpGo({ k: 'club', id: '136013' }));
  await o.p.waitForTimeout(1000);
  const t2 = await text(o.p);
  check('a complete list: the count and the latest «١٩ لقبًا، آخرها ٢٠٢٣-٢٠٢٤»', t2.includes('١٩ لقبًا، آخرها ٢٠٢٣-٢٠٢٤'), t2.slice(0, 500));
  await o.ctx.close();
}
{   // English: the English names, the words in English
  const { p, ctx, errors } = await open(ALL, { en: true });
  await p.evaluate(() => mpGo({ k: 'league', id: '4668', tab: 3 }));
  await p.waitForTimeout(800);
  const r = await rows(p), t = await text(p);
  check('English «Champions»: English club names and words, Latin digits', !errors.length && r[1] === '2025-2026: Al-Nassr 86 pts, 2 ahead of Al-Hilal' && t.includes('Earlier')
    && !/[؀-ۿ]/.test(t), JSON.stringify(r) + errors.join('|'));
  await p.evaluate(() => { MP.view.tab = 4; mpRender(); });
  await p.waitForTimeout(300);
  const r2 = await rows(p);
  check('English «Top scorers»: the English name, «25 goals», the source in English', r2[0] === 'Cristiano Ronaldo 25 goals' && (await text(p)).includes('Source: Wikipedia and Wikidata (CC BY-SA 4.0)'), JSON.stringify(r2));
  await p.evaluate(() => { MP.view.tab = 0; mpRender(); });
  await p.waitForTimeout(300);
  check('English: the archive\'s table shows the English names too (not the Arabic ones the file has)', /^1 Al-Hilal /.test((await rows(p))[0] || ''), JSON.stringify(await rows(p)));
  await ctx.close();
}
for (const [w, h] of [[1280, 720], [960, 540]]) {   // 720p: the five tabs and the seasons fit, nothing wider than the screen
  const { p, ctx, errors } = await open(ALL, { width: w, height: h });
  await p.evaluate(() => mpGo({ k: 'league', id: '4668', tab: 3 }));
  await p.waitForTimeout(800);
  const m = await p.evaluate(() => { const d = document.getElementById('mp-days'), last = d.lastElementChild.getBoundingClientRect();
    return { wide: document.documentElement.scrollWidth <= innerWidth + 1, tabs: last.left >= -1 && last.right <= innerWidth + 1 }; });
  check(`${w}x${h}: the five tabs on screen, nothing wider than the screen`, m.wide && m.tabs && !errors.length, JSON.stringify(m) + errors.join('|'));
  const sz = await p.evaluate(() => [parseFloat(getComputedStyle(document.querySelector('#mp .mpsrc')).fontSize), parseFloat(getComputedStyle(document.querySelector('#mp li.mpi i')).fontSize)]);
  check(`${w}x${h}: the source line as big as the rows' muted text (never smaller: read from the couch)`, sz[0] >= sz[1] && sz[1] > 0, JSON.stringify(sz));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all honours tests passed');
