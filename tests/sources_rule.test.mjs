// 4.4, the owner's rule for every card and screen: where the data comes from (Open-Meteo, TheSportsDB, the markets' «آخر تحديث», …)
// is at most a tiny dim line, 60% of the line it sits by and half its light, or nothing; all of it is on «مصادر البيانات» in the settings (حول).
// No visible text names a provider outside such a line. The screensaver in each design, the matches page, at two sizes.
//   python3 src/build.py && node tests/sources_rule.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const K = 0.61, LIGHT = 0.5;
// what the page measures: each source line against the line it sits by, its light (its own and its parents'), and any provider named elsewhere
const measure = () => {
  const fs = e => parseFloat(getComputedStyle(e).fontSize);
  const light = e => { let o = 1; for (let x = e; x && x.nodeType === 1; x = x.parentElement) o *= +getComputedStyle(x).opacity; return o; };
  const own = e => { let o = 1; for (let x = e; x && x.nodeType === 1 && !x.matches('.markets,#mp,.fbw,body'); x = x.parentElement) o *= +getComputedStyle(x).opacity; return o; };
  const shown = e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 && getComputedStyle(e).visibility !== 'hidden'; };
  const out = [];
  const pairs = [['#wx-src', '.wxsub'], ['#mk-up', '.mk']];
  for (const [s, by] of pairs) { const e = document.querySelector(s), b = document.querySelector(by);
    if (e && b && shown(e) && e.textContent.trim()) out.push({ what: s, k: fs(e) / fs(b), o: own(e), text: e.textContent }); }
  document.querySelectorAll('.srcl').forEach(e => { if (shown(e)) out.push({ what: '.srcl in #' + (e.parentElement.id || e.parentElement.className), k: fs(e) / fs(e.parentElement), o: +getComputedStyle(e).opacity, text: e.textContent }); });
  const loose = [], walk = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  for (let n; (n = walk.nextNode());) {
    if (!/Open-Meteo|TheSportsDB|Yahoo|TVmaze|QuranEnc|Unsplash/.test(n.nodeValue)) continue;
    const e = n.parentElement;
    if (!e || !shown(e) || e.closest('script,style')) continue;
    if (!e.closest('.srcl,.wxsrc,.mkup')) loose.push(n.nodeValue.trim().slice(0, 60));
  }
  return { out, loose };
};
for (const [w, h] of [[1920, 1080], [960, 540]]) {
  for (const design of ['info', 'clean', 'big', 'frame', 'dayline']) {
    const ctx = await browser.newContext({ viewport: { width: w, height: h } });
    await ctx.route(/^https?:/, r => r.abort());
    const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
    await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + `#gap=0&design=${design}&wx=usual&markets=tasi,spx,btc&mkstyle=prices`);
    await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 8000 }).catch(() => errors.push('never revealed'));
    await p.evaluate(() => { WX.at = Date.now() - 5 * 60000; wxSource();
      const up = document.getElementById('mk-up'); if (up) { up.hidden = false; up.textContent = 'آخر تحديث ٣:١٥ م'; } });
    const r = await p.evaluate(measure);
    const where = `${w} ${design}`;
    for (const x of r.out) check(`${where}: ${x.what} «${x.text}» at most 60% of its line and half its light`, x.k <= K && x.o <= LIGHT + 1e-6, JSON.stringify(x));
    check(`${where}: no provider named outside a source line`, !r.loose.length, r.loose.join(' | '));
    if (design === 'info') check(`${where}: the weather's source is there (Open-Meteo's CC BY credit stays on screen)`, r.out.some(x => x.what === '#wx-src'), JSON.stringify(r.out));
    check(`${where}: no script errors`, !errors.length, errors.join('; '));
    await ctx.close();
  }
}
{ // «المباريات»: the source after how old the games are, tiny
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#gap=0&matchpage=1&teams=136013');
  await p.waitForSelector('#mp-age', { timeout: 15000 }).catch(() => errors.push('no matches page'));
  await p.evaluate(() => mpRender());
  const r = await p.evaluate(measure);
  const s = r.out.filter(x => /mp-age/.test(x.what));
  check('matches page: TheSportsDB a tiny dim line', s.length === 1 && s[0].k <= K && s[0].o <= LIGHT, JSON.stringify(r.out));
  check('matches page: no provider named outside a source line', !r.loose.length, r.loose.join(' | '));
  check('matches page: no script errors', !errors.length, errors.join('; '));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all passed');
