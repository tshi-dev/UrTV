// 4.4 (a user report): «آية» and «حديث» at once: the card «حديث اليوم» (hadith) beside «آية أو حديث اليوم» (verse), on the home screen and in the
// screensaver's pair: the second always a hadith, the first then always a verse (never two hadith, never the same text twice), any day.
//   python3 src/build.py && node tests/verse_hadith.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
for (const [hash, w, h, what] of [['#widget=1&home_panel=verse,hadith', 922, 250, 'home, two cards'], ['#widget=1&home_panel=hadith', 461, 250, 'home, the hadith card alone'],
    ['#gap=0&panel=verse&panel2=hadith&clocksize=small', 1920, 1080, 'the screensaver\'s pair']]) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + hash);
  await p.waitForFunction(() => typeof CARDS !== 'undefined' && CARDS.length, null, { timeout: 8000 });
  const r = await p.evaluate(() => {
    const day = riyadhDayNumber, out = [];
    for (let d = 0; d < 14; d++) {
      riyadhDayNumber = () => 20000 + d;
      CARDS.forEach(c => renderCard(c, false));
      out.push(CARDS.map(c => [c.kind, c.ul.querySelector('.quote') ? (c.ul.querySelector('.quote').classList.contains('quran') ? 'verse' : 'hadith') : '-', c.hd.textContent.trim()]));
    }
    riyadhDayNumber = day;
    return out;
  });
  const ok = r.every(day => day.every(([kind, got]) => kind === 'hadith' ? got === 'hadith' : kind === 'verse' ? (day.some(x => x[0] === 'hadith') ? got === 'verse' : true) : true));
  check(`${what}: the hadith card always a hadith; beside it the verse card always a verse (14 days); no script errors`, ok && r[0].length >= 1 && !errors.length, JSON.stringify(r.slice(0, 3)) + errors.join('; '));
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
