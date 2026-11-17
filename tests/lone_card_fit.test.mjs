// 4.4 the owner (the home's stocks card cut at the bottom, «SABIC» half shown): a lone card on the launcher (#widget=1, one card) is fitted to its box
// as a pair is: fewer rows or a smaller size, never a row cut by the card's bottom edge. At the sizes a home card takes on 1080p TVs (density 1.33
// and 2), in a row («كنبة») and beside the clock.
//   python3 src/build.py && node tests/lone_card_fit.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const chart = (p, c) => JSON.stringify({ chart: { result: [{ meta: { regularMarketPrice: p, chartPreviousClose: c }, indicators: { quote: [{ close: Array.from({ length: 20 }, (_, i) => c + (p - c) * i / 19) }] } }] } });
for (const [file, w, h, row] of [['tv-en.html', 862, 109, 1], ['tv.html', 862, 109, 1], ['tv.html', 862, 109, 0], ['tv.html', 1297, 164, 1], ['tv.html', 461, 250, 0], ['tv.html', 700, 130, 1]]) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } });
  await ctx.route(/^https?:/, r => {
    const m = r.request().url().match(/chart\/([^?]+)\?/);
    return m ? r.fulfill({ contentType: 'application/json', headers: { 'Access-Control-Allow-Origin': '*' }, body: chart(40, 39) }) : r.abort();
  });
  const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#widget=1&panel=stocks&stocks=2222,1120,7010,2010,1180&row=' + row);
  await p.waitForFunction(() => document.querySelectorAll('#week-list li.stk .px').length > 0, null, { timeout: 8000 }).catch(() => errors.push('no stock rows'));
  await p.waitForTimeout(400);
  const r = await p.evaluate(() => {
    const rows = [...document.querySelectorAll('#week-list li')].map(li => li.getBoundingClientRect());
    return { n: rows.length, cut: rows.filter(b => b.bottom > innerHeight + .5).length, H: innerHeight };
  });
  check(`${file} ${w}x${h}${row ? ' (row)' : ''}: the lone stocks card fits its box (${r.n} rows), none cut at the bottom; no script errors`, r.n >= 1 && r.cut === 0 && !errors.length, JSON.stringify(r) + errors.join('; '));
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
