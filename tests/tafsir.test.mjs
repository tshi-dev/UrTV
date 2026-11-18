// «التفسير الميسر» (#tafsir=1): for every verse of the deck the tafsir is shown whole (its pages put together are the text, word for word),
// never «…», each page at most three lines, the card clear of the bottom row; on the launcher's card and in the small clock's pair (half the
// room) it is there whole or not at all: no cut tafsir anywhere.
//   python3 src/build.py && node tests/tafsir.test.mjs
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const TAFSIR = JSON.parse(readFileSync(path.join(ROOT, 'dist/tafsir.json'), 'utf8'));
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
// 4.5: the page's own file:// reads (dist/quotes.json, tafsir.json beside it) as the TV's WebViews allow them (setAllowFileAccessFromFileURLs)
const browser = await chromium.launch({ args: ['--allow-file-access-from-files'], ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}) });

check('dist/tafsir.json: a tafsir for every verse of the deck, none ending in «…»', Object.keys(TAFSIR).length >= 400 && Object.values(TAFSIR).every(t => t && !t.includes('…')), Object.keys(TAFSIR).length);

for (const [file, w, h, hash, label] of [['tv.html', 1920, 1080, '', 'screensaver 1920'], ['tv.html', 1280, 720, '', 'screensaver 1280'], ['tv.html', 960, 540, '', 'screensaver 960'],
    ['tv.html', 960, 540, '&clocksize=small&panel2=hourly', 'the small clock\'s pair 960'], ['tv.html', 1920, 1080, '&clocksize=small&panel2=hourly', 'the small clock\'s pair 1920'], ['tv.html', 461, 250, '&widget=1', 'launcher card'], ['tv-en.html', 1920, 1080, '', 'English page 1920']]) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } });
  const asked = [];
  await ctx.route(/^https?:/, r => {   // the repository's copy, for a page that cannot read beside it
    const u = r.request().url(); asked.push(u);
    if (u.endsWith('/master/dist/tafsir.json')) return r.fulfill({ path: path.join(ROOT, 'dist/tafsir.json'), headers: { 'Access-Control-Allow-Origin': '*' } });
    const m = u.match(/\/master\/(assets\/fonts\/.+)$/); if (m) return r.fulfill({ path: path.join(ROOT, m[1]), headers: { 'Access-Control-Allow-Origin': '*' } }).catch(() => r.abort());
    return r.abort();
  });
  const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
  p.on('request', q => { if (q.url().startsWith('file:')) asked.push(q.url()); });   // its reads beside it count too (one read in all)
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#gap=0&panel=verse&versekind=verse&tafsir=1' + hash);
  await p.waitForFunction(() => typeof TAFSIR !== 'undefined' && TAFSIR, null, { timeout: 8000 }).catch(() => errors.push('tafsir.json never read'));
  const r = await p.evaluate(widget => {
    const card = CARDS.find(c => c.kind === 'verse'), bad = [], none = [];
    let shown = 0, paged = 0;
    const day = riyadhDayNumber;
    for (const [i, v] of VERSES.entries()) {   // the card as the page draws it, on the day this verse is the day's
      const t = tafsirOf(v);
      if (!t) { none.push(v.surah + ':' + v.ayah); continue; }
      riyadhDayNumber = () => i; card.maxLen = 0;
      renderCard(card, false);
      clearInterval(TAFSIR_PG.t);
      if (!card.ul.querySelector('.qt') || card.ul.querySelector('.qt').textContent !== v.text) { bad.push(v.surah + ':' + v.ayah + ' not the day\'s verse'); continue; }
      const el = card.ul.querySelector('li.tafsir');
      if (!el) { if (!widget) bad.push(v.surah + ':' + v.ayah + ' removed'); continue; }
      const pages = el.__pages || [], tt = el.querySelector('.tt'), lh = parseFloat(getComputedStyle(tt).lineHeight);
      const tall = pages.some(pg => { tt.textContent = pg; return Math.round(tt.getBoundingClientRect().height / lh) > 3; });
      if (pages.join(' ') !== t || el.textContent.includes('…') || tall) bad.push(v.surah + ':' + v.ayah + JSON.stringify(pages).slice(0, 80));
      shown++; if (pages.length > 1) paged++;
    }
    riyadhDayNumber = day;
    return { shown, paged, bad: bad.slice(0, 5), nbad: bad.length, none: none.length, verses: VERSES.length };
  }, /widget|panel2/.test(hash));
  const tag = `${file} ${label}`;
  check(`${tag}: no script errors, tafsir.json read once (only with #tafsir=1)`, !errors.length && asked.filter(u => u.endsWith('tafsir.json')).length === 1, errors.join('; ') + asked.join(','));
  check(`${tag}: every verse has its tafsir`, r.none === 0, JSON.stringify(r));
  check(`${tag}: ${/widget|panel2/.test(hash) ? 'whole or not at all' : 'shown for every verse'}, whole (its pages are the text word for word), never «…», three lines a page at most (${r.shown} shown, ${r.paged} in pages)`,
    r.nbad === 0 && (/widget|panel2/.test(hash) || r.shown === r.verses), JSON.stringify(r));
  await ctx.close();
}
{   // off: the file is never asked for
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } }), asked = [];
  await ctx.route(/^https?:/, r => { asked.push(r.request().url()); return r.abort(); });
  const p = await ctx.newPage(); await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#gap=0&panel=verse'); await p.waitForTimeout(1500);
  check('#tafsir off: tafsir.json is not read', !asked.some(u => u.includes('tafsir')), asked.join(','));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
