// «اللغة», phase 4 (docs/ENGLISH.md): the phone page in English (index.en.html, written by src/phone_en.py) in a real browser, offline, and
// the language link on both pages. Every /api/* answer is a stub, as in tests/phone_page.test.mjs. The stub's own data (surahs, reciters,
// city, cards) is in English here, as an English TV will send it: so any Arabic a person sees on the English page is the page's own.
// It also saves docs/shots/phone-en.png (390×844, the remote tab).
//
//   CHROMIUM=/path/to/chromium node tests/i18n_phone_test.mjs
import { chromium } from 'playwright';
import { readFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const DIR = path.join(ROOT, 'android/launcher/src/main/assets/phone');
const FILES = { ar: readFileSync(path.join(DIR, 'index.html'), 'utf8'), en: readFileSync(path.join(DIR, 'index.en.html'), 'utf8') };
const ORIGIN = 'http://tv.test', PAGE = ORIGIN + '/phone/', NONCE = 'n0nce123';
const CSP = `default-src 'none'; script-src 'nonce-${NONCE}'; style-src 'unsafe-inline'; connect-src 'self'; img-src 'self' data:; font-src 'self'`;   // PhoneApi's
const AR = /[؀-ۿ]/;

let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const sleep = ms => new Promise(r => setTimeout(r, ms));
async function until(fn, ms = 4000) { const t = Date.now(); while (Date.now() - t < ms) { if (await fn()) return true; await sleep(40); } return false; }

const STATE = (extra = {}) => Object.assign({
  ok: true, settings: true, city: 'Riyadh', lang: 'en', home_cards: ['prayer', 'weather'],
  panels: [{ id: 'prayer', name: 'Prayer' }, { id: 'weather', name: 'Weather' }, { id: 'date', name: 'Date' }],
  adhan: { on: true, voice: 1, fajr_voice: 2, short: false, stopkey: true, volume: 60 },
  voices: [{ id: 1, name: 'Muezzin one', fajr: false }, { id: 2, name: 'Fajr muezzin', fajr: true }],
  reciters: [{ id: 'a', name: 'Reciter one' }, { id: 'b', name: 'Reciter two' }], surahs: ['Al-Fatiha', 'Al-Baqarah'],
  player: { playing: false, source: 'none', reciter: 'a', surah: 0, sleep_left: 0 }, foreground: 'own', update: { status: 'found', build: 52, version: '4.1' },
  seconds_left: 1500, adhan_live: false, adhan_prayer: '', next_adhan: { prayer: 'Asr', in_seconds: 3700 },
  sleep_timer: { left_seconds: 600, options: [15, 30, 60, 90] }, saver_idle: '10', saver_idle_options: ['1', '2', '5', '10'],
  keys_everywhere: true, dpad_everywhere: false, typing_service: true, family: { pin_set: true, kids: false, locked: ['@wifi', 'com.c', 'com.d'] },
  phones: 1, version: '4.0'
}, extra);
const APPS = { ok: true, apps: [{ pkg: 'com.a', name: 'Netflix', locked: false }, { pkg: 'com.c', name: 'Games', locked: true }],
  tools: [{ id: 'tasks', name: 'المهام', locked: false }, { id: 'saver', name: 'شاشة التوقف', locked: false }, { id: 'settings', name: 'الإعدادات', locked: true }] };

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});

async function boot({ lang = 'en', state = STATE(), w = 390, h = 844 } = {}) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h }, colorScheme: 'dark' });
  const reqs = [], errors = [], c = { lang, loads: 0 };
  await ctx.route('**/*', async route => {
    const rq = route.request(), u = new URL(rq.url());
    if (u.origin !== ORIGIN) return route.abort();
    if (u.pathname.startsWith('/fonts/')) return route.fulfill({ status: 404, contentType: 'application/json', body: '{"ok":false}' });
    if (u.pathname === '/phone/') { c.loads++; return route.fulfill({ status: 200, contentType: 'text/html; charset=utf-8', headers: { 'Content-Security-Policy': CSP }, body: FILES[c.lang].replace('__NONCE__', NONCE) }); }
    let body = null; try { body = rq.postData() ? JSON.parse(rq.postData()) : null; } catch (e) { body = '?'; }
    reqs.push({ path: u.pathname, body });
    const J = o => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(o) });
    const p = u.pathname;
    if (p === '/api/pair') return J({ ok: true, id: 'p1' });
    if (p === '/api/pair/status') return J({ ok: true, status: 'approved', session: 'SESS1' });
    if (p === '/api/state') return J(state);
    if (p === '/api/apps') return J(APPS);
    if (p === '/api/family') return J({ ok: true, family: state.family });
    if (p === '/api/set') { if (body && body.lang) c.lang = body.lang; return J({ ok: true, result: 'saved' }); }   // the TV now serves the other file
    if (p.startsWith('/api/icon/')) return route.fulfill({ status: 404, contentType: 'application/json', body: '{"ok":false}' });
    return J({ ok: true, result: 'done' });
  });
  const p = await ctx.newPage();
  p.on('pageerror', e => errors.push(e.message));
  await p.goto(PAGE);
  return { ctx, p, reqs, errors, c };
}
async function login(S) {
  await S.p.fill('#code', '123456'); await S.p.click('#codeGo');
  await S.p.waitForSelector('#app:not(.hidden)', { timeout: 6000 });
  await until(() => S.reqs.some(r => r.path === '/api/state')); await sleep(200);
}
const tab = async (S, t) => { await S.p.click(`#bar button[data-t=${t}]`); await sleep(250); };
// what a person (or a screen reader) gets: the visible text, and every label, placeholder and title
const words = S => S.p.evaluate(() => {
  const out = [];
  const walk = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  for (let n; (n = walk.nextNode());) { const e = n.parentElement; if (e && e.closest('svg,script,style')) continue; if (e && e.getClientRects().length && n.textContent.trim()) out.push(n.textContent.trim()); }
  document.querySelectorAll('[aria-label],[placeholder],[title]').forEach(e => ['aria-label', 'placeholder', 'title'].forEach(a => { if (e.getAttribute(a)) out.push(e.getAttribute(a)); }));
  document.querySelectorAll('option').forEach(o => out.push(o.textContent));
  out.push(document.title, getComputedStyle(document.querySelector('#aOn'), '::after').content);
  return out;
});
const T = async (name, fn) => { try { await fn(); } catch (e) { check(name, false, String(e && e.message || e).split('\n')[0]); } };

await T('english page', async () => {
  const S = await boot();
  const { p } = S;
  check('index.en.html: <html lang="en" dir="ltr">, read left to right', await p.evaluate(() => document.documentElement.lang === 'en' && document.dir === 'ltr' && getComputedStyle(document.body).direction === 'ltr'));
  const lw = await words(S);
  check('the login screen: no Arabic', !lw.some(x => AR.test(x)), lw.filter(x => AR.test(x)).slice(0, 4).join(' | '));
  check('the login screen in English', (await p.textContent('h1')) === 'Phone remote' && (await p.getAttribute('#code', 'placeholder')) === 'Code (6 digits)');
  await p.click('#codeGo'); await sleep(100);
  check('a short code: «The code has 6 digits.»', (await p.textContent('#loginState')) === 'The code has 6 digits.', await p.textContent('#loginState'));
  await login(S);
  const seen = [];
  for (const t of ['remote', 'apps', 'now', 'settings']) { await tab(S, t); seen.push(...await words(S)); }
  const ar = [...new Set(seen.filter(x => AR.test(x) && x !== 'العربية'))];
  check('every tab: no Arabic in its words, labels, placeholders and options (but the link «العربية»)', !ar.length, ar.slice(0, 6).join(' | '));
  check('the header: «25 min left»', (await p.textContent('#left')) === '25 min left', await p.textContent('#left'));
  await tab(S, 'now');
  check('the next adhan: «In 1 hour, 1 min» with an English time', (await p.textContent('#nextA')).endsWith('In 1 hour, 1 min') && /\d (AM|PM)/.test(await p.textContent('#nextA')), await p.textContent('#nextA'));
  check('the sleep timer: «Ends in 10 min», chips «15 min»…', (await p.textContent('#sleepNow')) === 'Ends in 10 min' && (await p.textContent('#sleepOpts button')) === '15 min', await p.textContent('#sleepNow'));
  check('the playing card: «Nothing playing», «Pick a reciter and a surah, then Play.»', (await p.textContent('#npT')) === 'Nothing playing' && (await p.textContent('#playerNow')) === 'Pick a reciter and a surah, then Play.');
  check('the screensaver delay in English minutes', (await p.$$eval('#saverIdle option', o => o.map(x => x.textContent))).join() === '1 min,2 min,5 min,10 min');
  await tab(S, 'apps');
  check('the pills: the page\'s English names, not the TV\'s Arabic ones', (await p.$$eval('#tools [data-tool]', b => b.map(x => x.textContent.trim()))).join() === 'Tasks,Screensaver,Inputs,🔒 Settings,Quran and adhan',
    (await p.$$eval('#tools [data-tool]', b => b.map(x => x.textContent.trim()))).join());
  await tab(S, 'settings');
  check('parental controls: «On», «Kids mode: Off», «Locked: 2 apps, 1 TV button»', (await p.textContent('#famPin')) === 'Parental controls: On' && (await p.textContent('#famKids')) === 'Kids mode: Off'
    && (await p.textContent('#famLocked')) === 'Locked: 2 apps, 1 TV button', await p.textContent('#famLocked'));
  check('the update: «An update is available. Press Install … (4.1)», «UrTV version: 4.0»', (await p.textContent('#upState')).endsWith('(4.1)') && (await p.textContent('#ver')) === 'UrTV version: 4.0', await p.textContent('#upState'));
  check('on/off reads «On»/«Off»', (await p.evaluate(() => getComputedStyle(document.querySelector('#aOn'), '::after').content)) === '"On"');
  check('no script error', !S.errors.length, S.errors.join(' | '));
  await S.ctx.close();
});

await T('left to right', async () => {
  for (const [w, h] of [[360, 740], [390, 844]]) {
    const S = await boot({ w, h }); await login(S);
    const { p } = S;
    const x = sel => p.evaluate(s => document.querySelector(s).getBoundingClientRect().left, sel);
    check(`${w}: the keys in reading order, left to right (Back, Home, Play/Pause)`, await x('[data-k=back]') < await x('[data-k=home]') && await x('[data-k=home]') < await x('[data-k=play_pause]'));
    check(`${w}: the tab bar starts on the left (Remote)`, (await p.$eval('#bar button', b => b.textContent)) === 'Remote' && await x('#bar button[data-t=remote]') < await x('#bar button[data-t=settings]'));
    check(`${w}: «Back» points left (mirrored)`, (await p.$eval('[data-k=back] svg', s => getComputedStyle(s).transform)).startsWith('matrix(-1'));
    for (const t of ['remote', 'apps', 'now', 'settings']) {
      await tab(S, t);
      const over = await p.evaluate(() => [...document.querySelectorAll('button,p,h2,label,span')].filter(e => e.getClientRects().length && !e.closest('svg,.sr') && e.scrollWidth > e.clientWidth + 1 && getComputedStyle(e).overflow !== 'visible').map(e => e.id || e.textContent.slice(0, 20)));
      const wide = await p.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
      check(`${w}: ${t}: nothing wider than the screen, no clipped words`, !wide && !over.length, over.join(' | '));
    }
    if (w === 390) {
      await tab(S, 'remote');
      mkdirSync(path.join(ROOT, 'docs/shots'), { recursive: true });
      await p.screenshot({ path: path.join(ROOT, 'docs/shots/phone-en.png') });
    }
    await S.ctx.close();
  }
});

await T('the language link', async () => {
  const S = await boot(); await login(S);
  const { p } = S;
  await tab(S, 'settings');
  check('English page: the link reads «العربية» (lang="ar")', (await p.textContent('#lang')) === 'العربية' && (await p.getAttribute('#lang', 'lang')) === 'ar' && await p.isVisible('#lang'));
  const loads = S.c.loads;
  await p.click('#lang');
  check('a press sends /api/set {lang:"ar"} and loads the page again, which the TV now serves in Arabic',
    await until(async () => S.c.loads > loads && await p.evaluate(() => document.documentElement.lang === 'ar').catch(() => false)),
    JSON.stringify(S.reqs.filter(r => r.path === '/api/set').map(r => r.body)));
  check('the request had only lang', S.reqs.filter(r => r.path === '/api/set').some(r => JSON.stringify(r.body) === '{"lang":"ar"}'));
  await S.ctx.close();

  const A = await boot({ lang: 'ar', state: STATE({ lang: 'ar' }) }); await login(A);
  await tab(A, 'settings');
  check('Arabic page: the link reads «English» (lang="en")', (await A.p.textContent('#lang')) === 'English' && (await A.p.getAttribute('#lang', 'lang')) === 'en' && await A.p.isVisible('#lang'));
  const l2 = A.c.loads;
  await A.p.click('#lang');
  check('a press sends /api/set {lang:"en"} and the English page comes', await until(async () => A.c.loads > l2 && await A.p.evaluate(() => document.documentElement.lang === 'en').catch(() => false)));
  await A.ctx.close();

  const O = await boot({ lang: 'ar', state: STATE({ lang: undefined }) }); await login(O);
  await tab(O, 'settings');
  check('a TV whose screensaver knows no «اللغة» (no lang in the state): no link', !(await O.p.isVisible('#lang')));
  await O.ctx.close();
});

await browser.close();
console.log(failed ? `\n${failed} FAILED` : '\nall passed');
process.exit(failed ? 1 : 0);
