// 4.4 the screensaver's designs (#design=, #bar=, #barat=, #wx=; the owner approved them: docs/mockups/saver-designs): each one shows what it should and
// hides the rest; the prayer's name and its time never touch (the owner: «المغرب» ran into «٥:٢١»), in every place the page prints them; in «خط اليوم»
// the «now» dot never sits on a time; the default (info) is the page as it always was; the launcher's cards never take a design.
//   python3 src/build.py && node tests/saver_designs.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const NOW = new Date('2026-10-22T13:58:00Z');   // Thursday 16:58 in Riyadh: Maghrib is next

async function open(hash, w = 1920, h = 1080) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h }, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, r => {
    const m = r.request().url().match(/\/master\/(assets\/fonts\/.+)$/);
    return m ? r.fulfill({ path: path.join(ROOT, m[1]), headers: { 'Access-Control-Allow-Origin': '*' } }).catch(() => r.abort()) : r.abort();
  });
  const p = await ctx.newPage(); await p.clock.setFixedTime(NOW);
  const errors = []; p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#city=' + encodeURIComponent('الرياض') + '&lat=24.7136&lon=46.6753&gap=0&panel=verse&slot=countdown&cd=2026-12-18&cdname=' + encodeURIComponent('إجازة نهاية الفصل') + hash);
  await p.waitForFunction(() => typeof DAY_TIMES !== 'undefined' && DAY_TIMES && document.getElementById('prayer-time').textContent, null, { timeout: 8000 }).catch(() => errors.push('no prayer times'));
  await p.evaluate(() => { tickPrayer(); designTick();
    window.shown = sel => { const e = document.querySelector(sel); return !!e && !e.hidden && getComputedStyle(e).display !== 'none' && e.getBoundingClientRect().width > 0; }; });
  return { p, ctx, errors };
}
// two boxes do not overlap, and the gap between them (px) on the line
const apart = (a, b) => a && b && (a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top);

for (const [w, h] of [[1920, 1080], [1280, 720], [960, 540]]) {
  {   // info: the page as it was; its prayer panel's name and time apart
    const { p, ctx, errors } = await open('', w, h);
    const r = await p.evaluate(() => {
      const n = document.getElementById('prayer-name').getBoundingClientRect(), t = document.getElementById('prayer-time').getBoundingClientRect();
      return { cls: [...document.documentElement.classList].filter(c => c.startsWith('sd-')), n: n.toJSON(), t: t.toJSON(), extra: document.querySelectorAll('body > .sd-line, body > .sd-bar, body > .sd-dl').length,
        top: getComputedStyle(document.querySelector('.top')).display !== 'none', bottom: getComputedStyle(document.querySelector('.bottom')).display !== 'none' };
    });
    check(`${w}: info (the default): no design class, nothing added, the top and the bottom row as they were; no script errors`,
      !r.cls.length && !r.extra && r.top && r.bottom && !errors.length, JSON.stringify(r) + errors.join('; '));
    check(`${w}: info: the next prayer's name and its time do not touch (the prayer panel)`, apart(r.n, r.t) && Math.min(Math.abs(r.n.left - r.t.right), Math.abs(r.t.left - r.n.right)) >= 4, JSON.stringify(r));
    await ctx.close();
  }
  {   // «نظيفة»: the clock, the day, the next prayer; the name and the time apart, the time its own isolated run
    const { p, ctx, errors } = await open('&design=clean', w, h);
    const r = await p.evaluate(() => {
      const b = document.querySelector('.sd-line .n b'), t = document.querySelector('.sd-line .n .pt');
      const gap = b && t ? Math.min(Math.abs(b.getBoundingClientRect().left - t.getBoundingClientRect().right), Math.abs(t.getBoundingClientRect().left - b.getBoundingClientRect().right)) : -1;
      return { b: b && b.getBoundingClientRect().toJSON(), t: t && t.getBoundingClientRect().toJSON(), gap, em: b ? parseFloat(getComputedStyle(b).fontSize) : 0,
        iso: t ? getComputedStyle(t).unicodeBidi : '', text: (document.querySelector('.sd-line') || {}).textContent || '',
        top: getComputedStyle(document.querySelector('.top')).display, bottom: getComputedStyle(document.querySelector('.bottom')).display, clock: document.querySelector('.clockwrap').parentElement === document.body };
    });
    check(`${w}: «نظيفة»: the day and the next prayer under the clock; the top and the bottom row hidden; no script errors`,
      r.text.includes('هـ') && r.text.includes('المغرب') && r.top === 'none' && r.bottom === 'none' && r.clock && !errors.length, JSON.stringify(r) + errors.join('; '));
    check(`${w}: «نظيفة»: «المغرب» and «٥:٢١ م» never touch: the time an isolated run, a gap of a quarter of the text's size at least`,
      apart(r.b, r.t) && r.gap >= r.em * .25 && r.iso === 'isolate', JSON.stringify(r));
    await ctx.close();
  }
  {   // «خط اليوم»: the six times on the line, the «now» dot clear of every time and name
    const { p, ctx, errors } = await open('&design=dayline', w, h);
    const r = await p.evaluate(() => {
      const dot = document.querySelector('.sd-dl .now'), d = dot && dot.getBoundingClientRect();
      const labels = [...document.querySelectorAll('.sd-dl .p b, .sd-dl .p i, .sd-dl .left')].map(e => e.getBoundingClientRect());
      const hit = labels.filter(l => !(l.right <= d.left || d.right <= l.left || l.bottom <= d.top || d.bottom <= l.top)).length;
      return { n: document.querySelectorAll('.sd-dl .p').length, next: (document.querySelector('.sd-dl .p.next b') || {}).textContent, hit };
    });
    check(`${w}: «خط اليوم»: the six prayers on the line, the next one (المغرب) lit, the «now» dot on no name or time; no script errors`,
      r.n === 6 && r.next === 'المغرب' && r.hit === 0 && !errors.length, JSON.stringify(r) + errors.join('; '));
    await ctx.close();
  }
}
for (const [hash, what, test] of [
  ['&design=big', '«ساعة كبيرة»: the clock alone, the day under it', () => shown('.sd-big-d') && getComputedStyle(document.querySelector('.top')).display === 'none'],
  ['&design=frame', '«لوحة»: the frame, the day, the time in the corner', () => shown('.sd-vig') && shown('.sd-fdate') && document.querySelector('.clockwrap').getBoundingClientRect().right > innerWidth * .8],
  ['&design=clean&bar=prayers', 'the bar: the day\'s six prayers, the next one lit with how long; «نظيفة» then leaves the prayer out of its own line', () => document.querySelectorAll('.sd-bar .c').length === 6 && document.querySelector('.sd-bar .c.next em') && !document.querySelector('.sd-line .n')],
  ['&design=clean&bar=slot', 'the bar: the slot (here the countdown), at the bottom', () => shown('.sd-bar') && document.querySelector('.sd-bar').textContent.includes('إجازة نهاية الفصل') && document.querySelector('.sd-bar').classList.contains('at-bottom')],
  ['&design=clean&bar=prayers&barat=clock', 'the bar under the clock, clear of the day\'s line', () => document.querySelector('.sd-bar.at-clock') && document.querySelector('.sd-bar').getBoundingClientRect().top >= document.querySelector('.sd-line').getBoundingClientRect().bottom],
  ['&design=clean&wx=corner', 'the weather in the corner (here none came: nothing shown, no empty box)', () => { const e = document.querySelector('.sd-wx'); return !!e && (e.hidden || e.textContent.trim().length > 0); }],
  ['&design=info&wx=off', 'info with the weather off: the rest of the top stays', () => document.documentElement.classList.contains('sd-wx-off') && getComputedStyle(document.querySelector('.top')).display !== 'none'],
  ['&design=nonsense&bar=x&wx=y', 'an unknown value: info, no bar, the weather where it was', () => !document.querySelector('body > .sd-bar') && ![...document.documentElement.classList].some(c => c.startsWith('sd-'))],
]) {
  const { p, ctx, errors } = await open(hash);
  check(`${what}; no script errors`, await p.evaluate(test) && !errors.length, errors.join('; ') + await p.evaluate(() => document.documentElement.className));
  await ctx.close();
}
{   // the launcher's card never takes a design
  const { p, ctx, errors } = await open('&widget=1&design=clean&bar=prayers', 461, 250);
  check('the launcher\'s card (#widget=1) never takes a design or a bar', await p.evaluate(() => !document.querySelector('body > .sd-bar, body > .sd-line') && ![...document.documentElement.classList].some(c => c.startsWith('sd-'))), errors.join('; '));
  await ctx.close();
}
await browser.close();
process.exit(failed ? 1 : 0);
