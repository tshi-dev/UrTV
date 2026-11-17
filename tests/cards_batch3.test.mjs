// 4.4 card review, batch 3 (the owner approved cards 12, 13, 14, 16, 17 and 18: docs/cards/REVIEW.md on work/44-cards): each card has its
// new look by default and its old one with «شكل البطاقة: الشكل السابق» (#cardstyle=classic). Measured on the page, the two side by side.
//   python3 src/build.py && node tests/cards_batch3.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const TASKS = encodeURIComponent('اشتر الخبز\nاتصل بالوالدة\nموعد الطبيب');
const read = async (style) => {
  const out = {};
  for (const [panel, extra] of [['tasks', `&tasks=${TASKS}`], ['year', ''], ['shows', ''], ['today', ''], ['adhkar', ''], ['wisdom', '']]) {
    const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
    await ctx.route(/^https?:/, r => r.abort());
    const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
    await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + `#gap=0&panel=${panel}${extra}${style ? '&cardstyle=' + style : ''}`);
    await p.waitForFunction(() => typeof CARDS !== 'undefined' && CARDS.length, null, { timeout: 8000 }).catch(() => errors.push('no cards'));
    if (panel === 'shows') await p.evaluate(() => { WEEK_POOL = [{ name: 'مسلسل', ep: 3, rating: 8.1, date: '2026-11-18' }]; WEEK_LOADED = true; renderPanel(false, 'shows'); });
    out[panel] = await p.evaluate((panel) => {
      const cs = (sel, pseudo) => { const e = document.querySelector(sel); return e ? getComputedStyle(e, pseudo) : null; };
      const r = { errors: [] };
      if (panel === 'tasks') { const b = cs('#week-list li.task', '::before'), li = cs('#week-list li.task');
        Object.assign(r, { ring: b && b.backgroundColor === 'rgba(0, 0, 0, 0)' && b.borderTopWidth === '2px', size: li && parseFloat(li.fontSize), hd: document.getElementById('week-hd').textContent }); }
      if (panel === 'year') { const yn = cs('#week-list li.yr .yn'); r.yn = yn && parseFloat(yn.fontSize); }
      if (panel === 'shows') { const rt = cs('#week-list li .rt', '::before'); r.star = rt ? rt.content : 'none'; }
      if (panel === 'today') { const k = cs('#week-list li.td .k'), v = cs('#week-list li.td .v'); r.k = k && v ? parseFloat(k.fontSize) / parseFloat(v.fontSize) : null; r.kc = k && k.color; }
      if (panel === 'adhkar' || panel === 'wisdom') { const q = document.querySelector('#week-list li.quote'); r.quote = q ? [q.className, getComputedStyle(q).textAlign] : null; }
      const np = cs('.np .nps'); r.nps = np && np.marginInlineStart;
      return r;
    }, panel);
    out[panel].errors = errors;
    await ctx.close();
  }
  return out;
};
const now = await read(''), old = await read('classic');
check('13 «المهام»: an open ring before each, not a dot', now.tasks.ring && !old.tasks.ring, JSON.stringify([now.tasks, old.tasks]));
check('13 «المهام»: the text a step larger', now.tasks.size > old.tasks.size, JSON.stringify([now.tasks.size, old.tasks.size]));
check('13 «المهام»: the count on the header («٣ مهام»), the title alone in the old look', now.tasks.hd === 'المهام٣ مهام' && old.tasks.hd === 'المهام', JSON.stringify([now.tasks.hd, old.tasks.hd]));
check('16 «السنة»: the year bigger (as far as the card fit lets it)', now.year.yn > old.year.yn * 1.1, JSON.stringify([now.year.yn, old.year.yn]));
check('18 «هذا الأسبوع»: a star before the rating', /★/.test(now.shows.star) && !/★/.test(old.shows.star), JSON.stringify([now.shows.star, old.shows.star]));
check('17 «يومك»: the small names readable (smaller than the value, and lighter than before)', now.today.k !== null && now.today.k < 1 && now.today.kc !== old.today.kc, JSON.stringify([now.today, old.today]));
for (const k of ['adhkar', 'wisdom']) {
  const n = now[k].quote, o = old[k].quote;
  if (n && /q[234]/.test(n[0])) check(`12 «${k}»: a long text from the line's start`, n[1] === 'start' && o[1] === 'center', JSON.stringify([n, o]));
  else if (n) check(`12 «${k}»: a short text stays centred`, n[1] === 'center', JSON.stringify(n));
}
check('14 «يُتلى الآن»: the «،» on the word before it', now.tasks.nps !== old.tasks.nps, JSON.stringify([now.tasks.nps, old.tasks.nps]));
for (const [k, v] of [...Object.entries(now), ...Object.entries(old)]) check(`${k}: no script errors`, !v.errors.length, v.errors.join('; '));
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all passed');
