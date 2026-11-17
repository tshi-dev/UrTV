// 4.4 (the owner): the screensaver's bottom strip jumped as the slot turned (a game in a week, one under way with its score, two games,
// none, the weather, the markets). The slot's row is one fixed box now: whatever it shows, the strip's top stays where it is, every item
// hangs from the box's top and stays on the screen, and the clubs stay on one line. At three screen sizes, with the shortest and the longest items.
//   python3 src/build.py && node tests/strip_steady.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
for (const [w, h] of [[1920, 1080], [1280, 720], [960, 540]]) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage(); const errors = []; p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#gap=0&slot=matches');
  await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 8000 }).catch(() => errors.push('never revealed'));
  const r = await p.evaluate(() => {
    const now = Date.now(), at = ms => { const d = new Date(now + ms); return { dateEvent: d.toISOString().slice(0, 10), strTime: d.toISOString().slice(11, 19) }; };
    const ev = (h, a, ms, x = {}) => Object.assign({ strHomeTeam: h, strAwayTeam: a, idHomeTeam: '1', idAwayTeam: '2', strStatus: 'NS' }, at(ms), x);
    const L1 = 'نادي الاتحاد الرياضي السعودي لكرة القدم الأول', L2 = 'نادي الفيحاء الرياضي بالمجمعة والمنطقة';
    const live = { intHomeScore: 2, intAwayScore: 1, strStatus: '1H', strProgress: '30' };
    const items = {
      'a game in three hours, short names': gameBlock(ev('Hilal', 'Nassr', 3 * 3600e3), now, false),
      'a game in three days, long names': gameBlock(ev(L1, L2, 3 * 86400e3), now, false),
      'a game under way, its score big, long names': gameBlock(ev(L1, L2, -30 * 60e3, live), now, true),
      'a game under way, short names': gameBlock(ev('Hilal', 'Nassr', -30 * 60e3, live), now, false),
      'no score yet, the time not set': gameBlock(ev(L1, 'Nassr', 2 * 86400e3, { strTime: '00:00:00', strTimestamp: '' }), now, false),
      'two games': gameBlock(ev(L1, 'Nassr', 3 * 86400e3), now, false) + gameBlock(ev('Hilal', L2, 2 * 86400e3), now, false),
      'no games': '<div class="none-week">لا مباريات هذا الأسبوع</div>',
      'the weather': slotBlock('مشمس <span class="num">٣٨°</span>', 'العظمى ٣٨ والصغرى ٢٠', 'غدًا غائم جزئيًا'),
      'a long line under it': slotBlock('اليوم', 'سطر طويل جدًا تحت الاسم '.repeat(8)),
      'three markets': [1, 2, 3].map(i => slotBlock('تاسي <span class="num">١٢٬٣٤٥٫٦٧</span>', '▲ ١٫٢٣٪')).join(''),
    };
    const row = document.getElementById('match-row'), bot = document.querySelector('.bottom'), out = {};
    for (const k in items) {
      row.innerHTML = items[k];
      fitMatches(row);
      const rr = row.getBoundingClientRect(), lh = parseFloat(getComputedStyle(row.querySelector('.big > span') || row.firstElementChild).lineHeight) || 0;
      const kids = [...row.querySelectorAll('.mblock, .none-week')].map(b => b.getBoundingClientRect());
      const bigs = [...row.querySelectorAll('.mblock .big')].map(b => b.getBoundingClientRect().height);
      const subs = [...row.querySelectorAll('.mblock .sub')].map(s => [s.getBoundingClientRect().height, parseFloat(getComputedStyle(s).fontSize)]);
      out[k] = { top: Math.round(bot.getBoundingClientRect().top), inside: kids.every(b => b.top >= rr.top - 1 && b.bottom <= innerHeight - 4),
        bigOne: bigs.every(x => x < 2 * Math.max(lh, 1) * 0.95 || x < 1.9 * (parseFloat(getComputedStyle(row.querySelector('.big')).fontSize) * 1.1)),
        subOne: subs.every(([hh, fs]) => hh < fs * 2.2) };
    }
    return out;
  });
  const tops = Object.values(r).map(x => x.top), first = tops[0];
  check(`${w}: the strip's top is the same whatever the slot shows (${first})`, tops.every(t => t === first), JSON.stringify(Object.fromEntries(Object.entries(r).map(([k, v]) => [k, v.top]))));
  for (const [k, v] of Object.entries(r)) {
    check(`${w}: ${k}: from the box's top, on the screen`, v.inside);
    check(`${w}: ${k}: the clubs on one line, each line under them one line`, v.bigOne && v.subOne, JSON.stringify(v));
  }
  check(`${w}: no script errors`, !errors.length, errors.join('; '));
  await ctx.close();
}
await browser.close();
if (failed) { console.log(failed + ' failed'); process.exit(1); }
console.log('all passed');
