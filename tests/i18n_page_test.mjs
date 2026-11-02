// «اللغة», phase 2 (docs/ENGLISH.md): dist/tv-en.html in a real browser, offline (the services mocked, as page.test.mjs does): it starts,
// is laid out left to right, and no Arabic is left in its own words on any card or slot (the verses, hadith, adhkar and sayings are
// content and stay Arabic, right to left). Needs no private font. The build side: tests/i18n_page_test.py.
//
//   node tests/i18n_page_test.mjs
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const INDEX = readFileSync(path.join(ROOT, 'photos/index.json'), 'utf8');
let failed = 0;
const check = (name, ok, detail = '') => {
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail));
  if (!ok) failed++;
};
const NOW = Date.parse('2026-11-02T12:17:00Z');   // 15:17 in Riyadh, a Monday: Maghrib next
const at = h => new Date(NOW + h * 36e5).toISOString().slice(0, 19);
const game = (id, h, a, hid, aid, hrs, extra = {}) => ({ idEvent: id, strSport: 'Soccer', idHomeTeam: hid, strHomeTeam: h, idAwayTeam: aid,
  strAwayTeam: a, strTimestamp: at(hrs), strStatus: 'NS', intHomeScore: null, intAwayScore: null, ...extra });
const NEXT = { '133613': [game('g1', 'Manchester City', 'Arsenal', '133613', '133604', 28)],
               '134108': [game('g2', 'Al-Hilal', 'Al-Nassr', '134108', '134109', -0.5, { strStatus: '2H', strProgress: '67', intHomeScore: '2', intAwayScore: '1' })] };
const days = [...Array(7)].map((_, k) => new Date(NOW + k * 864e5).toISOString().slice(0, 10));
const WEATHER = { current: { temperature_2m: 27.4, weather_code: 1, relative_humidity_2m: 30, wind_speed_10m: 12, is_day: 1 },
  daily: { time: days, weather_code: [1, 2, 3, 0, 0, 61, 1], temperature_2m_max: [31, 30, 29, 32, 33, 28, 30], temperature_2m_min: [19, 18, 18, 20, 21, 17, 18] },
  hourly: { time: [...Array(48)].map((_, k) => at(k).slice(0, 13) + ':00'), temperature_2m: [...Array(48)].map((_, k) => 25 + (k % 6)),
            weather_code: Array(48).fill(1), is_day: Array(48).fill(1) } };

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
async function open(file, hash, { width = 1920, height = 1080 } = {}) {
  const ctx = await browser.newContext({ viewport: { width, height }, timezoneId: 'Asia/Riyadh' });
  await ctx.route(/^https?:/, r => {
    const u = new URL(r.request().url()), body = j => r.fulfill({ json: j, headers: { 'Access-Control-Allow-Origin': '*' } });
    const i = u.href.indexOf('/master/'), rel = i > 0 ? u.href.slice(i + 8) : '';
    if (/^(assets\/backgrounds|photos)\//.test(rel)) return r.fulfill({ path: path.join(ROOT, rel), headers: { 'Access-Control-Allow-Origin': '*' } });
    if (/thesportsdb/.test(u.host)) return body(u.pathname.endsWith('eventsnext.php') ? { events: NEXT[u.searchParams.get('id')] || null } : { results: null, events: null });
    if (/open-meteo/.test(u.host) && /forecast/.test(u.pathname)) return body(WEATHER);
    return r.abort();
  });
  await ctx.addInitScript(i => { try { localStorage.setItem('photoIndex', i); } catch (e) {} }, INDEX);
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.clock.setFixedTime(new Date(NOW));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#' + hash + '&gap=0');
  await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 5000 }).catch(() => errors.push('never revealed'));
  await p.waitForTimeout(700);
  return { p, ctx, errors };
}
// the page's own words: everything a person reads but the Arabic content (and the hidden parts)
const ownText = p => p.evaluate(() => {
  const skip = 'li.quote, li.src, li.tafsir, li.adf .s, [hidden], script, style';
  const walk = (el, out) => {
    if (el.matches && el.matches(skip)) return;
    if (el.nodeType === 1 && getComputedStyle(el).display === 'none') return;
    for (const n of el.childNodes) n.nodeType === 3 ? out.push(n.textContent) : walk(n, out);
  };
  const out = []; walk(document.body, out); return out.join(' ').replace(/\s+/g, ' ');
});
const ARABIC = /[؀-ۿ]/g;

// ---- the screensaver, large clock
{
  const { p, ctx, errors } = await open('tv-en.html', 'city=Riyadh&teams=133613,134108&panel=verse&slot=matches&wxmore=1');
  check('tv-en.html starts with no script error', !errors.length, errors.join('; '));
  const r = await p.evaluate(() => ({
    dir: document.documentElement.dir, lang: document.documentElement.lang, title: document.title,
    hijri: document.getElementById('hijri').textContent, greg: document.getElementById('greg').textContent,
    label: document.getElementById('prayer-label').textContent, name: document.getElementById('prayer-name').textContent,
    time: document.getElementById('prayer-time').textContent, count: document.getElementById('prayer-count').textContent,
    ap: document.getElementById('ck-ap').textContent, sub: document.getElementById('clock-sub').textContent,
    dates: document.querySelector('.dates').getBoundingClientRect().left, markets: document.querySelector('.markets').getBoundingClientRect().left,
    prayer: document.querySelector('.p-prayer').getBoundingClientRect().left, match: document.querySelector('.p-match').getBoundingClientRect().left,
    apX: document.getElementById('ck-ap').getBoundingClientRect().left, hX: document.getElementById('ck-h').getBoundingClientRect().left,
    games: document.getElementById('match-row').innerText,
    quoteDir: (document.querySelector('li.quote') && getComputedStyle(document.querySelector('li.quote')).direction) || '',
  }));
  check('lang="en" dir="ltr", the English title', r.dir === 'ltr' && r.lang === 'en' && r.title === 'Your TV Screen', JSON.stringify(r));
  check('the Hijri date in English with the month transliterated', r.hijri === 'Monday, 22 Jumada al-Awwal 1448 AH', r.hijri);
  check('the Gregorian date in English', r.greg === 'Monday, 2 November 2026', r.greg);
  check('the next prayer: «Next prayer», «Maghrib», «5:13 PM», «in 1 hour, 55 min»',
    r.label === 'Next prayer' && r.name === 'Maghrib' && /^\d{1,2}:\d\d PM$/.test(r.time) && /^in \d+ hours?, \d+ min$/.test(r.count), JSON.stringify(r));
  check('the clock: Latin digits by default, PM after the digits', r.ap === 'PM' && r.apX > r.hX, JSON.stringify(r));
  check('the city line: «Time in Riyadh»', r.sub === 'Time in Riyadh', r.sub);
  check('laid out left to right: the dates and the next prayer on the left, the weather and the games on the right',
    r.dates < r.markets && r.prayer < r.match, JSON.stringify(r));
  check('a followed team\'s game under way, big: the English names, «Live · 67\'»', /Al-Hilal 2 - 1 Al-Nassr/.test(r.games) && /Live · 67'/.test(r.games), r.games);
  check('the Arabic content keeps right to left in its card', r.quoteDir === 'rtl', r.quoteDir);
  const t = await ownText(p);
  check('no Arabic in the page\'s own words', !ARABIC.test(t), (t.match(/[^ ]*[؀-ۿ][^ ]*/g) || []).slice(0, 8).join(' | '));
  check('no Arabic-Indic digit with Latin digits', !/[٠-٩]/.test(t), t.slice(0, 200));
  await ctx.close();
}

// ---- a game later this week: «Manchester City × Arsenal», «In 1 day, 4 hours», the date and time in English
{
  const { p, ctx, errors } = await open('tv-en.html', 'teams=133613&slot=matches');
  const g = await p.evaluate(() => document.getElementById('match-row').innerText);
  check('a game this week: the English names, «In 1 day, 4 hours», «Tuesday 3 November · 7:17 PM»',
    !errors.length && /Manchester City × Arsenal/.test(g) && /In 1 day, 4 hours/.test(g) && /Tuesday 3 November · 7:17 PM/.test(g), g + errors.join('; '));
  await ctx.close();
}

// ---- the date words are the page's own names, not Intl's (whose commas differ between WebViews): the occasion, a countdown, the forecast
{
  const { p, ctx, errors } = await open('tv-en.html', 'panel=forecast&slot=countdown&cd=2026-12-25');
  const r = await p.evaluate(() => [document.getElementById('occ-sub').textContent, document.querySelector('.p-match').innerText,
    [...document.querySelectorAll('.week li span:first-child')].map(x => x.textContent).join('|')]);
  check('the dates written by the page: «Monday 8 February · expected», «25 December», «Today|Tomorrow|Wednesday|Thursday|Friday»',
    !errors.length && r[0] === 'Monday 8 February · expected' && /25 December/.test(r[1]) && r[2] === 'Today|Tomorrow|Wednesday|Thursday|Friday',
    JSON.stringify(r) + errors.join('; '));
  await ctx.close();
}

// ---- #digits=hindi still gives Arabic-Indic digits on the English page
{
  const { p, ctx, errors } = await open('tv-en.html', 'city=Riyadh&digits=hindi&panel=prayers');
  const r = await p.evaluate(() => [document.getElementById('ck-h').textContent, document.getElementById('hijri').textContent]);
  check('#digits=hindi: the clock and the dates in Arabic-Indic digits, the words English', !errors.length && /^[٠-٩]+$/.test(r[0]) && /^Monday, [٠-٩]+ Jumada al-Awwal [٠-٩]+ AH$/.test(r[1]), JSON.stringify(r) + errors.join('; '));
  await ctx.close();
}

// ---- every card and slot: no script error, no Arabic in the page's own words
const CASES = [
  ['prayers + weather slot + 24-hour clock', 'panel=prayers&slot=weather&clock=24'],
  ['today + fasting slot', 'panel=today&slot=fasting&teams=133613&tasks=Buy%20milk'],
  ['year + forecast (small clock) + countdown slot', 'clocksize=small&panel=year&panel2=forecast&slot=countdown&cd=2026-12-25'],
  ['hourly (medium clock) + markets slot', 'clocksize=medium&panel=hourly&slot=markets'],
  ['adhkar + matches card', 'panel=adhkar&panel2=matches&clocksize=small&teams=133613,134108'],
  ['wisdom + stocks', 'panel=wisdom&panel2=stocks&clocksize=small&stocks=2222,1120,AAPL'],
  ['smart card, prayer row, sunrise and prayer bar', 'panel=smart&panel2=prayerrow&clocksize=small&sunrise=1&prayerbar=1&teams=134108'],
  ['tasks, shows, the year strip', 'panel=tasks&panel2=shows&clocksize=small&tasks=Call%20Ali&yearstrip=1'],
  ['the city note, a prayer method label', 'panel=verse&citynote=1&pm=3&tz=Europe%2FLondon&city=London'],
  ['the launcher\'s cards (widget)', 'widget=1&home_panel=nextprayer,today,year&teams=133613'],
];
for (const [name, hash] of CASES) {
  const { p, ctx, errors } = await open('tv-en.html', hash);
  const t = await ownText(p);
  check(`${name}: no script error, no Arabic in the page's own words`, !errors.length && !ARABIC.test(t),
    errors.join('; ') + ' ' + (t.match(/[^ ]*[؀-ۿ][^ ]*/g) || []).slice(0, 8).join(' | '));
  await ctx.close();
}

// ---- the Arabic page beside it: still Arabic, right to left
{
  const { p, ctx, errors } = await open('tv.html', 'panel=prayers');
  const r = await p.evaluate(() => [document.documentElement.dir, document.getElementById('prayer-label').textContent, document.querySelector('.dates').getBoundingClientRect().left > innerWidth / 2]);
  check('tv.html is still the Arabic page, right to left', !errors.length && r[0] === 'rtl' && r[1] === 'الصلاة القادمة' && r[2], JSON.stringify(r));
  await ctx.close();
}

await browser.close();
process.exit(failed ? 1 : 0);
