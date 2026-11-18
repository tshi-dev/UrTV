// 4.4 the tafsir card, short (the owner approved it: docs/mockups/tafsir-short/ on work/44-cards): «التفسير الميسر» whole and word for word,
// then the book and the verse's Madinah mushaf page, small, under it («التفسير الميسر، صفحة ٣٤١»). «الشكل السابق» keeps the name over the text.
// The pages are stored with each verse (fetch_verses.py --pages, from api.alquran.cloud's page table).
//   python3 src/build.py && node tests/tafsir_short.test.mjs
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const TAFSIR = JSON.parse(readFileSync(path.join(ROOT, 'dist/tafsir.json'), 'utf8'));
const DATA = JSON.parse(readFileSync(path.join(ROOT, 'assets/shared/verses.json'), 'utf8'));
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };

// the data: every verse has its page, 1 to 604, in the mushaf's order (a later surah never on an earlier page)
const pages = DATA.map(v => [v.ref.split('-')[0].split(':').map(Number), v.page]);
check('every verse has its mushaf page (1 to 604)', pages.every(([, p]) => Number.isInteger(p) && p >= 1 && p <= 604), pages.filter(([, p]) => !p).length);
const sorted = [...pages].sort((a, b) => a[0][0] - b[0][0] || a[0][1] - b[0][1]);
check('the pages follow the mushaf\'s order', sorted.every((x, i) => i === 0 || x[1] >= sorted[i - 1][1]));
const ref = r => DATA.find(v => v.ref === r);
check('known pages: الإخلاص ٣ on 604, التكاثر ٨ on 600, الحج ٧٧ on 341, الفاتحة on 1 (where in the deck)',
  ref('112:3').page === 604 && ref('102:8').page === 600 && ref('22:77').page === 341 && (!ref('1:1') || ref('1:1').page === 1));

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const AR = n => String(n).replace(/\d/g, d => '٠١٢٣٤٥٦٧٨٩'[d]);
for (const [file, pick, page, style] of [['tv.html', 'الإخلاص:3', 604, ''], ['tv.html', 'التكاثر:8', 600, ''], ['tv.html', 'الحج:77', 341, ''],
    ['tv.html', 'الحج:77', 341, 'classic'], ['tv-en.html', 'الإخلاص:3', 604, '']]) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  await ctx.route(/^https?:/, r => r.request().url().endsWith('/master/dist/tafsir.json')
    ? r.fulfill({ path: path.join(ROOT, 'dist/tafsir.json'), headers: { 'Access-Control-Allow-Origin': '*' } }) : r.abort());
  const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#gap=0&panel=verse&versekind=verse&tafsir=1' + (style ? '&cardstyle=' + style : ''));
  await p.waitForFunction(() => typeof TAFSIR !== 'undefined' && TAFSIR, null, { timeout: 8000 }).catch(() => errors.push('tafsir.json never read'));
  const r = await p.evaluate(pick => {
    const i = VERSES.findIndex(v => (v.surah + ':' + v.ayah).endsWith(pick)); if (i < 0) return null;
    window.riyadhDayNumber = () => i; renderPanel(false);
    const li = document.querySelector('#week-list li.tafsir'); if (!li) return { none: true };
    const tk = li.querySelector('.tk'), tt = li.querySelector('.tt'), kids = [...li.children].map(e => e.className);
    // the whole text, every page in turn (fitTafsir keeps them in li.__pages)
    return { key: VERSES[i].surah + ':' + VERSES[i].ayah, kids, src: tk.textContent, under: tk.getBoundingClientRect().top >= tt.getBoundingClientRect().bottom - 1,
      small: parseFloat(getComputedStyle(tk).fontSize) < parseFloat(getComputedStyle(tt).fontSize), whole: (li.__pages || [tt.textContent]).join(' ') };
  }, pick);
  const tag = `${file} ${pick}${style ? ' ' + style : ''}`;
  if (!r || r.none) { check(`${tag}: the tafsir is there`, false, JSON.stringify(r)); await ctx.close(); continue; }
  check(`${tag}: the text whole, word for word`, r.whole.split(/\s+/).join(' ') === TAFSIR[r.key].split(/\s+/).join(' '), r.whole.slice(0, 80));
  if (style === 'classic') check(`${tag}: «الشكل السابق» keeps the name over the text, no page`, r.kids[0] === 'tk' && r.src === 'التفسير الميسر', JSON.stringify(r.kids) + r.src);
  else if (file === 'tv-en.html') check(`${tag}: English says the book and the page`, /Muyassar.*page 604/.test(r.src), r.src);
  else {
    check(`${tag}: «التفسير الميسر، صفحة ${AR(page)}»`, r.src === `التفسير الميسر، صفحة ${AR(page)}`, r.src);
    check(`${tag}: under the text, small`, r.kids.indexOf('tk under') > r.kids.indexOf('tt') && r.under && r.small, JSON.stringify(r));
  }
  check(`${tag}: no script errors`, !errors.length, errors.join('; '));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all passed');
