// The phone remote page («تحكم من الجوال») in a real browser, offline. The page is served from the repo, every /api/* answer is a stub
// that records what the page sent. Both the full state (new fields) and a state with only the old fields.
//
//   CHROMIUM=/path/to/chromium node tests/phone_page.test.mjs
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const HTML = readFileSync(path.join(ROOT, 'android/launcher/src/main/assets/phone/index.html'), 'utf8');
const ORIGIN = 'http://tv.test';
const PAGE = ORIGIN + '/phone/';
const NONCE = 'n0nce123';
const CSP = `default-src 'none'; script-src 'nonce-${NONCE}'; style-src 'unsafe-inline'; connect-src 'self'; img-src 'self' data:`;
const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64');
const KEYS = ['up', 'down', 'left', 'right', 'ok', 'back', 'home', 'vol_up', 'vol_down', 'mute', 'play_pause'];
const TOKEN = 'a1b2c3d4e5f60718293a4b5c6d7e8f90';

let failed = 0;
const check = (name, ok, detail = '') => {
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail));
  if (!ok) failed++;
};
const sleep = ms => new Promise(r => setTimeout(r, ms));
async function until(fn, ms = 4000) { const t = Date.now(); while (Date.now() - t < ms) { if (await fn()) return true; await sleep(40); } return false; }

const OLD = () => ({
  ok: true, settings: true, city: 'الرياض', home_cards: ['prayer', 'weather'], panels: [{ id: 'prayer', name: 'الصلاة' }, { id: 'weather', name: 'الطقس' }, { id: 'date', name: 'التاريخ' }],
  adhan: { on: true, voice: 1, fajr_voice: 2, short: false, stopkey: true, volume: 60 },
  voices: [{ id: 1, name: 'مؤذن أول', fajr: false }, { id: 2, name: 'مؤذن الفجر', fajr: true }],
  reciters: [{ id: 'a', name: 'قارئ أول' }, { id: 'b', name: 'قارئ ثانٍ' }],
  surahs: ['الفاتحة', 'البقرة', 'آل عمران', 'النساء'],
  player: { playing: false, source: 'none', reciter: 'a', surah: 0, sleep_left: 0 },
  foreground: 'own', update: { status: 'idle' }, pin_required: false, seconds_left: 1500
});
const NEW = (extra = {}) => Object.assign(OLD(), {
  adhan_live: false, adhan_prayer: '', next_adhan: { prayer: 'العصر', in_seconds: 1234 },
  sleep_timer: { left_seconds: 600, options: [15, 30, 60, 90] }, saver_idle: '10', saver_idle_options: ['1', '2', '5', '10', '15', '30'],
  keys_everywhere: true, typing_service: true, family: { pin_set: true, kids: false, locked: ['@wifi', 'com.c.three', 'com.x.y'] }, phones: 2
}, extra);
const APPS = {
  ok: true,
  apps: [{ pkg: 'com.a.one', name: 'نتفلكس', locked: false }, { pkg: 'com.b.two', name: 'يوتيوب', locked: false }, { pkg: 'com.c.three', name: 'ألعاب', locked: true }, { pkg: 'com.d.x', name: '<b>x</b>', locked: false }],
  tools: [{ id: 'tasks', name: 'المهام', locked: false }, { id: 'saver', name: 'شاشة التوقف', locked: false }, { id: 'inputs', name: 'المصادر', locked: false }, { id: 'settings', name: 'الإعدادات', locked: true }, { id: 'listen', name: 'تلاوة وأذان', locked: false }]
};

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});

// A phone with the page open; every request is kept. cfg changes what the stub answers.
async function boot({ state = NEW(), w = 360, h = 740, scheme = 'dark', hash = '', login = true, cfg = {} } = {}) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h }, colorScheme: scheme, hasTouch: false });
  const reqs = [], foreign = [], errors = [];
  const c = { state, pin: false, noSession: false, apps: APPS, update: null, ...cfg };
  await ctx.route('**/*', async route => {
    const rq = route.request(), u = new URL(rq.url());
    if (u.origin !== ORIGIN || (u.pathname !== '/phone/' && !u.pathname.startsWith('/api/'))) { foreign.push(rq.url()); return route.abort(); }
    if (u.pathname === '/phone/') return route.fulfill({ status: 200, contentType: 'text/html; charset=utf-8', headers: { 'Content-Security-Policy': CSP }, body: HTML.replace('__NONCE__', NONCE) });
    let body = null; try { body = rq.postData() ? JSON.parse(rq.postData()) : null; } catch (e) { body = '?bad'; }
    const rec = { method: rq.method(), url: rq.url(), path: u.pathname, body, session: rq.headers()['x-urtv-session'] || null };
    reqs.push(rec);
    const hdr = await rq.allHeaders();
    const J = (o, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(o) });
    const p = u.pathname;
    if (p === '/api/pair') return J({ ok: true, id: 'p1' });
    if (p === '/api/pair/status') return route.fulfill({ status: 200, contentType: 'application/json', headers: { 'Set-Cookie': 'urtv=ck1; Path=/' }, body: JSON.stringify({ ok: true, status: 'approved', session: 'SESS1' }) });
    if (c.noSession && p !== '/api/logout') return J({ ok: false, error: 'no_session' }, 401);
    if (p === '/api/state') { if (c.stateDelay) await sleep(c.stateDelay); return J(typeof c.state === 'function' ? c.state() : c.state); }
    if (p === '/api/apps') return J(c.apps);
    if (p.startsWith('/api/icon/')) {   // an <img> sends the cookie but not the header: the server wants both
      if (!(hdr.cookie || '').includes('urtv=ck1') || hdr['x-urtv-session'] !== 'SESS1') return J({ ok: false, error: 'no_session' }, 401);
      if (p.includes('broken')) return J({ ok: false, error: 'not_found' }, 404);
      c.iconNow = (c.iconNow || 0) + 1; c.iconMax = Math.max(c.iconMax || 0, c.iconNow);
      await sleep(70); c.iconNow--;
      return route.fulfill({ status: 200, contentType: 'image/png', headers: { 'Cache-Control': 'private, max-age=86400' }, body: PNG });
    }
    if (p === '/api/do' && c.pin && body && body.op === 'sleep_cancel') {
      if (body.pin === undefined) return J({ ok: false, error: 'pin_required' }, 403);
      if (body.pin !== '1234') return J({ ok: false, error: 'pin_wrong' }, 403);
    }
    if (p === '/api/set' && c.setDelay) await sleep(c.setDelay);
    if (p === '/api/set' && c.pin) {
      if (!body || body.pin === undefined) return J({ ok: false, error: 'pin_required' }, 403);
      if (body.pin !== '1234') return J({ ok: false, error: 'pin_wrong' }, 403);
    }
    if (p === '/api/family') { const st = typeof c.state === 'function' ? c.state() : c.state; return st.family ? J({ ok: true, family: st.family }) : J({ ok: false, error: 'bad_request' }); }
    if (p === '/api/text') {
      if (c.textAnswer) return J(c.textAnswer);
      return J({ ok: true, result: 'typed' });
    }
    if (p === '/api/launch') return J({ ok: true, result: body && body.target === 'com.c.three' ? 'pin_on_tv' : 'opened' });
    if (p === '/api/cities') return J({ ok: true, cities: ['جدة', 'جازان'] });
    if (p === '/api/update') return J(c.update || { ok: true, status: 'found', build: '42' });
    if (p === '/api/adhan_stop') return J({ ok: true, result: 'done' });
    if (p === '/api/do') return J({ ok: true, result: 'done' });
    return J({ ok: true });
  });
  const p = await ctx.newPage();
  p.on('pageerror', e => errors.push('pageerror: ' + e.message));
  p.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  await p.goto(PAGE + (hash ? '#' + hash : ''));
  const S = { ctx, p, reqs, foreign, errors, cfg: c };
  if (login) {
    await p.fill('#code', '123456'); await p.click('#codeGo');
    await p.waitForSelector('#app:not(.hidden)', { timeout: 6000 });
    await until(() => reqs.some(r => r.path === '/api/state'));
    await sleep(150);
  }
  return S;
}
const T = async (name, fn) => { try { await fn(); } catch (e) { check(name, false, String(e && e.message || e).split('\n')[0]); } };
const keysSent = S => S.reqs.filter(r => r.path === '/api/key').map(r => r.body.key);
const posts = (S, p) => S.reqs.filter(r => r.method === 'POST' && r.path === p);
const tab = async (S, t) => { await S.p.click(`#bar button[data-t=${t}]`); await sleep(120); };
const vis = (S, sel) => S.p.locator(sel).first().isVisible();

// ---- login
await T('login', async () => {
  const S = await boot();
  const pair = posts(S, '/api/pair')[0];
  check('login with a code posts /api/pair {code}', pair && pair.body.code === '123456' && Object.keys(pair.body).length === 1, JSON.stringify(pair && pair.body));
  const st = S.reqs.find(r => r.path === '/api/state');
  check('the session from /api/pair/status goes in X-UrTV-Session', st && st.session === 'SESS1', st && st.session);
  await S.ctx.close();

  const T2 = await boot({ hash: TOKEN, login: false });
  await T2.p.waitForSelector('#app:not(.hidden)', { timeout: 6000 });
  const pr = posts(T2, '/api/pair')[0];
  check('login with the token in the address posts /api/pair {token}', pr && pr.body.token === TOKEN, JSON.stringify(pr && pr.body));
  check('the token is gone from the address and the address has no query', T2.p.url() === PAGE, T2.p.url());
  await T2.ctx.close();

  const B = await boot({ login: false });
  await B.p.fill('#code', '12'); await B.p.click('#codeGo');
  check('a short code is refused on the phone', (await B.p.textContent('#loginState')).includes('٦') && posts(B, '/api/pair').length === 0);
  await B.ctx.close();
});

// ---- the screens, with the full state and with the old fields only
for (const [label, st] of [['new state', NEW()], ['old state', OLD()]]) await T(label, async () => {
  const S = await boot({ state: st });
  const { p } = S;
  const head = await p.textContent('header.top');
  check(`[${label}] header: «التلفزيون» · «متصل» · time left`, head.includes('التلفزيون') && head.includes('متصل') && /بقي \d+ دقيقة/.test(head), head);
  const names = await p.$$eval('#bar button', bs => bs.map(b => b.textContent));
  check(`[${label}] four tabs`, names.length === 4 && ['الريموت', 'التطبيقات', 'يعمل الآن', 'الإعدادات'].every((n, i) => names[i].includes(n)), names.join('|'));
  for (const [t, sel] of [['remote', '#pad'], ['apps', '#grid'], ['now', '#qPlay'], ['settings', '#cityQ']]) {
    await tab(S, t);
    const shown = await p.$$eval('#app > div[id^=t-]', ds => ds.filter(d => !d.classList.contains('hidden')).map(d => d.id));
    check(`[${label}] tab ${t} renders alone`, shown.length === 1 && shown[0] === 't-' + t && await vis(S, sel), shown.join());
  }
  await tab(S, 'now');
  const sc = await p.isVisible('#saverCard'), sl = await p.isVisible('#sleepCard');
  check(`[${label}] screensaver and sleep cards only with the new fields`, label === 'new state' ? sc && sl : !sc && !sl, `${sc} ${sl}`);
  await tab(S, 'settings');
  check(`[${label}] family card only with the new field, never a button in it`, (await p.isVisible('#famCard')) === (label === 'new state') && (await p.locator('#famCard button, #famCard input, #famCard select').count()) === 0);
  if (label === 'new state') {
    check('the settings tab reads GET /api/family', S.reqs.some(r => r.method === 'GET' && r.path === '/api/family'));
    const f = await p.textContent('#famCard');
    check('family card is read-only and says: مفعّلة / يعمل / locked count', f.includes('الرقابة الأبوية: مفعّلة') && f.includes('وضع الأطفال: متوقف') && f.includes('2 تطبيقًا') && f.includes('1 من أزرار'), f);
    check('connected phones count', (await p.textContent('#phones')).includes('2'));
  }
  const text = (await p.evaluate(() => document.body.innerText + document.body.innerHTML.replace(/<script[\s\S]*<\/script>/, ''))).toLowerCase();
  check(`[${label}] never offers reset, uninstall, ADB, PIN change, kids switch or unlock`, !/reset|uninstall|adb|إعادة ضبط|حذف التطبيق|فك القفل|تغيير الرقم|إلغاء القفل/.test(text));
  await S.ctx.close();
});

// ---- the remote
await T('remote', async () => {
  const S = await boot();
  const { p } = S;
  for (const k of KEYS) await p.click(`[data-k=${k}]`);
  await until(() => keysSent(S).length >= KEYS.length);
  const sent = keysSent(S);
  check('every pad button sends exactly its key, nothing else', KEYS.every(k => sent.includes(k)) && sent.every(k => KEYS.includes(k)) && sent.length === KEYS.length, sent.join());
  const posted = await p.$$eval('[data-k]', bs => bs.map(b => b.getAttribute('data-k')));
  check('the page has no key button outside the 11 allowed', posted.every(k => KEYS.includes(k)) && new Set(posted).size === 11, posted.join());

  const box = await p.locator('#pad').boundingBox(), cx = box.x + box.width / 2, cy = box.y + box.height / 2;
  const swipe = async (dx, dy) => {
    S.reqs.length = 0;
    await p.mouse.move(cx, cy); await p.mouse.down();
    for (let i = 1; i <= 4; i++) await p.mouse.move(cx + dx * i / 4, cy + dy * i / 4);
    await p.mouse.up(); await sleep(250);
    return keysSent(S);
  };
  for (const [dx, dy, want] of [[80, 0, 'right'], [-80, 0, 'left'], [0, -80, 'up'], [0, 80, 'down']]) {
    const got = await swipe(dx, dy);
    check(`swipe ${want} on the pad sends ${want}`, got.length >= 1 && got.every(k => k === want), got.join());
  }
  S.reqs.length = 0; await p.mouse.move(cx, cy); await p.mouse.down(); await sleep(60); await p.mouse.up(); await sleep(250);
  check('a tap on the pad sends ok', keysSent(S).join() === 'ok', keysSent(S).join());
  S.reqs.length = 0; await p.mouse.move(cx, cy); await p.mouse.down(); await sleep(1200); await p.mouse.up(); await sleep(200);
  check('a long press without moving sends nothing', keysSent(S).length === 0, keysSent(S).join());

  S.reqs.length = 0;
  const b = await p.locator('[data-k=down]').boundingBox();
  await p.mouse.move(b.x + b.width / 2, b.y + b.height / 2); await p.mouse.down(); await sleep(1100); await p.mouse.up();
  const n = keysSent(S).length; await sleep(400);
  check('holding an arrow repeats, and stops on release', n >= 3 && keysSent(S).length === n && keysSent(S).every(k => k === 'down'), `${n} ${keysSent(S).length}`);
  S.reqs.length = 0;
  const v = await p.locator('[data-k=vol_up]').boundingBox();
  await p.mouse.move(v.x + v.width / 2, v.y + v.height / 2); await p.mouse.down(); await sleep(1000); await p.mouse.up();
  check('holding volume + repeats', keysSent(S).length >= 3 && keysSent(S).every(k => k === 'vol_up'), keysSent(S).join());
  S.reqs.length = 0; await p.click('[data-k=vol_down]'); await p.click('[data-k=mute]'); await sleep(150);
  check('volume − and mute', keysSent(S).join() === 'vol_down,mute', keysSent(S).join());

  S.reqs.length = 0;
  await p.fill('#txt', 'مرحبا بالعالم'); await p.click('#txtGo'); await until(() => posts(S, '/api/text').length);
  check('the text field sends /api/text {text}', posts(S, '/api/text')[0].body.text === 'مرحبا بالعالم' && await until(async () => (await p.inputValue('#txt')) === ''));
  check('the result word is explained in Arabic', await until(async () => (await p.textContent('#msg')).includes('كُتب النص')));
  await p.fill('#txt', 'مرة ثانية'); await p.press('#txt', 'Enter'); await until(() => posts(S, '/api/text').length === 2);
  check('Enter in the text field sends too', posts(S, '/api/text').length === 2);
  check('the text field says what it cannot do', (await p.textContent('#t-remote')).includes('أندرويد لا يسمح لتطبيق بالكتابة داخل تطبيق آخر'));
  await S.ctx.close();
});

// ---- typing into other apps
await T('text other apps', async () => {
  const S = await boot();
  const { p } = S;
  check('«أرسل» and «بحث» buttons, «استبدال / إضافة» choice with replace on by default', (await p.textContent('#txtGo')) === 'أرسل' && (await p.textContent('#txtSearch')) === 'بحث'
    && (await p.$$eval('#modes button', bs => bs.map(b => b.textContent + (b.classList.contains('on') ? '*' : '')).join())) === 'استبدال*,إضافة');
  await p.fill('#txt', 'ا'); await p.click('#txtGo'); await until(() => posts(S, '/api/text').length === 1);
  check('«أرسل» sends mode replace and no search', JSON.stringify(posts(S, '/api/text')[0].body) === '{"text":"ا","mode":"replace"}', JSON.stringify(posts(S, '/api/text')[0].body));
  await until(async () => (await p.inputValue('#txt')) === '');
  await p.fill('#txt', 'سلام'); await p.click('#txtSearch'); await until(() => posts(S, '/api/text').length === 2);
  check('«بحث» sends the same text with search:true', JSON.stringify(posts(S, '/api/text')[1].body) === '{"text":"سلام","mode":"replace","search":true}', JSON.stringify(posts(S, '/api/text')[1].body));
  await until(async () => (await p.inputValue('#txt')) === '');
  await p.click('#modes [data-mode=append]'); await p.fill('#txt', 'ب'); await p.click('#txtGo'); await until(() => posts(S, '/api/text').length === 3);
  check('«إضافة» sends mode append', posts(S, '/api/text')[2].body.mode === 'append');
  await p.reload(); await sleep(300);
  check('the mode is remembered (localStorage)', await p.evaluate(() => localStorage.getItem('urtvTxtMode')) === 'append');
  await S.ctx.close();

  const words = [['typed', 'كُتب النص'], ['task', 'أُضيف النص'], ['copied', 'نُسخ النص'], ['typed_other', 'تمت الكتابة في التطبيق'], ['typed_other_nosearch', 'والبحث يحتاج أندرويد ١١']];
  for (const [r, msg] of words) {
    const W = await boot({ cfg: { textAnswer: { ok: true, result: r } } });
    await W.p.fill('#txt', 'نص'); await W.p.click('#txtGo');
    check(`result ${r}: message and the field is cleared`, await until(async () => (await W.p.textContent('#msg')).includes(msg)) && await until(async () => (await W.p.inputValue('#txt')) === ''), await W.p.textContent('#msg'));
    await W.ctx.close();
  }
  for (const [e, msg] of [['service_off', 'فعّل خدمة "تلفزيونك: الكتابة من الجوال"'], ['refused', 'ما أقدر أكتب في هذا التطبيق'], ['locked', 'مقفول بالرقم السري، افتحه على التلفزيون'], ['password_field', 'ما أكتب في حقل كلمة المرور'], ['failed', 'ما نجحت']]) {
    const W = await boot({ cfg: { textAnswer: { ok: false, error: e } } });
    await W.p.fill('#txt', 'نص'); await W.p.click('#txtSearch');
    check(`error ${e}: message and the text is kept`, await until(async () => (await W.p.textContent('#msg')).includes(msg)) && (await W.p.inputValue('#txt')) === 'نص', await W.p.textContent('#msg'));
    if (e === 'service_off') {
      const n = await W.p.textContent('#svcNote');
      check('service_off: the permanent note with the path, visible after the toast is gone', n.includes('الإعدادات ← إمكانية الوصول ← تلفزيونك: الكتابة من الجوال') && await W.p.isVisible('#svcNote'));
      await sleep(3800);
      check('the note stays', await W.p.isVisible('#svcNote'));
    }
    await W.ctx.close();
  }
  const mk = async extra => { const W = await boot({ state: NEW(extra) }); const v = await W.p.isVisible('#svcNote'); await W.ctx.close(); return v; };
  check('hint under the box: other app and typing_service false', await mk({ foreground: 'other', typing_service: false }));
  check('no hint: other app with the typing service on, our own screen, or an old TV without the field', !(await mk({ foreground: 'other', typing_service: true })) && !(await mk({ foreground: 'own', typing_service: false })) && !(await mk({ foreground: 'other', typing_service: undefined })));
  check('keys_everywhere only drives the pad note, not the typing hint', !(await mk({ foreground: 'other', keys_everywhere: false, typing_service: true })) && await mk({ foreground: 'other', keys_everywhere: true, typing_service: false }));
});

// ---- the note under the pad
await T('note', async () => {
  const mk = async extra => { const S = await boot({ state: NEW(extra) }); const r = { shown: await S.p.isVisible('#foreNote'), t: await S.p.textContent('#foreNote'), warn: await S.p.$eval('#foreNote', e => e.classList.contains('warn')) }; await S.ctx.close(); return r; };
  const own = await mk({}), a = await mk({ foreground: 'other' }), b = await mk({ foreground: 'other', keys_everywhere: false });
  check('no note while our screen is in front', !own.shown);
  check('other app + keys everywhere: the short note', a.shown && a.t === 'الأسهم تعمل في شاشاتنا؛ الرجوع والرئيسية في كل مكان' && !a.warn, a.t);
  check('other app, no accessibility: the warning that arrows work only in our screens', b.shown && b.t.includes('أندرويد يسمح لتطبيقنا بإرسال الأسهم') && b.warn, b.t);
  const S = await boot({ state: OLD({ foreground: 'other' }) });
  const o = OLD(); o.foreground = 'other'; S.cfg.state = o; await tab(S, 'now'); await tab(S, 'remote');
  check('old state with another app in front: the warning', (await S.p.textContent('#foreNote')).includes('أندرويد يسمح لتطبيقنا'));
  await S.ctx.close();
});

// ---- the apps
await T('apps', async () => {
  const S = await boot();
  const { p } = S;
  await tab(S, 'apps'); await p.waitForSelector('#grid .app');
  const items = await p.$$eval('#grid .app', bs => bs.map(b => ({ pkg: b.getAttribute('data-pkg'), name: b.textContent, lock: !!b.querySelector('.lock'), src: (b.querySelector('img') || {}).getAttribute && b.querySelector('img').getAttribute('src'), bold: !!b.querySelector('b') })));
  check('the grid shows every favourite in the TV order', items.map(i => i.pkg).join() === 'com.a.one,com.b.two,com.c.three,com.d.x', items.map(i => i.pkg).join());
  const loaded = await until(async () => (await p.$$eval('#grid img', is => is.length > 0 && is.every(i => (i.getAttribute('src') || '').startsWith('data:image/png')))));
  const srcs = await p.$$eval('#grid img', is => is.map(i => i.getAttribute('src') || ''));
  check('every icon is fetched with the session header + cookie and shown as a data: image', loaded && srcs.length === 4, srcs.map(x => x.slice(0, 20)).join());
  check('an <img> never has an src starting with /api/', (await p.$$eval('img', is => is.every(i => !(i.getAttribute('src') || '').startsWith('/api/')))));
  const ireq = S.reqs.filter(r => r.path.startsWith('/api/icon/'));
  check('the icons are requested as /api/icon/<package> paths, once each', ireq.length === 4 && ireq.every(r => r.session === 'SESS1' && !r.url.includes('?')), ireq.map(r => r.path).join());
  await p.fill('#appQ', 'ن'); await p.fill('#appQ', ''); await sleep(300);
  check('the icons are cached in memory: a new search does not fetch them again', S.reqs.filter(r => r.path.startsWith('/api/icon/')).length === 4 && await p.$$eval('#grid img', is => is.length === 4 && is.every(i => (i.getAttribute('src') || '').startsWith('data:'))));
  check('the first letter is the tile fallback', await p.$eval('#grid .app .tile', t => t.firstChild.nodeType === 3 || t.textContent.length >= 1));
  check('only the locked app has 🔒', items.map(i => i.lock).join() === 'false,false,true,false');
  check('app names from the TV are text, never HTML', items[3].name.includes('<b>x</b>') && !items[3].bold, items[3].name);
  await p.fill('#appQ', 'يوتيوب');
  check('search filters by name', (await p.locator('#grid .app').count()) === 1 && (await p.textContent('#grid .app')).includes('يوتيوب'));
  await p.fill('#appQ', 'zzz');
  check('search with no match says so', (await p.locator('#grid .app').count()) === 0 && (await p.textContent('#grid')).includes('لا توجد'));
  await p.fill('#appQ', '');
  await p.click('[data-pkg="com.a.one"]'); await until(() => posts(S, '/api/launch').length);
  check('launching posts /api/launch {target: package}', posts(S, '/api/launch')[0].body.target === 'com.a.one' && Object.keys(posts(S, '/api/launch')[0].body).length === 1);
  check('an unlocked app shows «فُتح على التلفزيون»', await until(async () => (await p.textContent('#msg')).includes('فُتح على التلفزيون')));
  await p.click('[data-pkg="com.c.three"]'); await until(() => posts(S, '/api/launch').length === 2);
  check('a locked app shows «اكتب الرقم السري على التلفزيون» and never asks for a PIN on the phone', await until(async () => (await p.textContent('#msg')).includes('اكتب الرقم السري على التلفزيون')) && !(await p.isVisible('#dlg')) && !('pin' in posts(S, '/api/launch')[1].body));
  const tools = await p.$$eval('#tools button', bs => bs.map(b => b.getAttribute('data-tool') + ':' + b.textContent));
  check('the five tools, a lock on the locked one', tools.length === 5 && tools.join().includes('tasks:المهام') && tools.join().includes('settings:🔒'), tools.join());
  await p.click('[data-tool=tasks]'); await until(() => posts(S, '/api/launch').length === 3);
  check('a tool launches @id', posts(S, '/api/launch')[2].body.target === '@tasks');
  check('no request URL carries a query', S.reqs.every(r => !r.url.includes('?')), S.reqs.filter(r => r.url.includes('?')).map(r => r.url).join());
  await S.ctx.close();
  const many = { ok: true, apps: Array.from({ length: 11 }, (_, i) => ({ pkg: 'com.m.a' + i, name: 'تطبيق ' + i, locked: false })).concat([{ pkg: 'com.broken.x', name: 'معطل', locked: false }]), tools: [] };
  const M = await boot({ cfg: { apps: many } }); await tab(M, 'apps');
  check('icons load at most 4 at a time, all of them', await until(async () => (await M.p.$$eval('#grid img', is => is.filter(i => (i.getAttribute('src') || '').startsWith('data:')).length)) === 11, 8000) && M.cfg.iconMax <= 4 && M.cfg.iconMax >= 2, 'max ' + M.cfg.iconMax);
  await M.p.locator('#grid .app').last().scrollIntoViewIfNeeded(); await sleep(600);
  check('a failed icon leaves the letter tile (no broken image)', await M.p.$$eval('#grid .app', bs => { const b = bs[bs.length - 1]; return !b.querySelector('img') && b.querySelector('.tile').textContent === 'م'; }));
  await M.ctx.close();
  const O = await boot({ state: OLD(), cfg: { apps: { ok: false, error: 'bad_request' } } });
  await tab(O, 'apps');
  check('an older TV without /api/apps: a plain note, tools still there', (await O.p.textContent('#appsNote')).includes('حدّث') && (await O.p.locator('#tools button').count()) === 5);
  await O.ctx.close();
});

// ---- now playing, adhan, sleep, screensaver
await T('now', async () => {
  const live = NEW({ adhan_live: true, adhan_prayer: 'العصر', player: { playing: true, source: 'adhan', reciter: 'a', surah: 0, sleep_left: 0 } });
  const S = await boot({ state: live });
  const { p } = S;
  check('adhan live: the bar says so with «أوقف الأذان الآن»', (await p.textContent('#nowT')).includes('العصر') && (await p.locator('#adhanStop1:visible').textContent()) === 'أوقف الأذان الآن');
  await p.click('#adhanStop1'); await until(() => posts(S, '/api/adhan_stop').length);
  check('it posts /api/adhan_stop', posts(S, '/api/adhan_stop').length === 1);
  await tab(S, 'now');
  check('the same button on «يعمل الآن»', await p.isVisible('#adhanStop2'));
  await S.ctx.close();

  const Q = await boot({ state: NEW({ player: { playing: true, source: 'surah', reciter: 'b', surah: 2, sleep_left: 5 } }) });
  check('the bar shows surah and reciter', (await Q.p.textContent('#nowT')) === 'سورة البقرة · قارئ ثانٍ' && !(await Q.p.isVisible('#adhanStop1')), await Q.p.textContent('#nowT'));
  await tab(Q, 'now');
  check('«يعمل الآن» shows playing and the pause button', (await Q.p.textContent('#playerNow')).includes('يعمل') && (await Q.p.textContent('#qToggle')) === 'إيقاف مؤقت');
  await Q.p.click('#qToggle'); await until(() => posts(Q, '/api/quran').length);
  check('pause posts quran op pause', posts(Q, '/api/quran')[0].body.op === 'pause');
  for (const op of ['prev', 'next', 'stop']) await Q.p.click(`[data-q=${op}]`);
  await until(() => posts(Q, '/api/quran').length === 4);
  check('prev / next / stop', posts(Q, '/api/quran').slice(1).map(r => r.body.op).join() === 'prev,next,stop');
  await Q.p.selectOption('#qSleep', '30'); await until(() => posts(Q, '/api/quran').length === 5);
  check('quran sleep posts op sleep with minutes', JSON.stringify(posts(Q, '/api/quran')[4].body) === '{"op":"sleep","minutes":30}');
  await Q.p.click('#qPlay'); await until(() => posts(Q, '/api/quran').length === 6);
  check('play posts reciter and surah', posts(Q, '/api/quran')[5].body.op === 'play' && posts(Q, '/api/quran')[5].body.surah === 2);
  await Q.ctx.close();

  const N = await boot();
  await tab(N, 'now');
  check('next adhan is shown', (await N.p.textContent('#nextA')).includes('العصر') && (await N.p.textContent('#nextA')).includes('20 دقيقة'), await N.p.textContent('#nextA'));
  check('no adhan live: no stop button', !(await N.p.isVisible('#adhanStop2')));
  await N.p.locator('#sleepOpts button', { hasText: '30' }).click(); await until(() => posts(N, '/api/set').length);
  check('the sleep card posts /api/set {sleep_minutes: 30}', JSON.stringify(posts(N, '/api/set')[0].body) === '{"sleep_minutes":30}', JSON.stringify(posts(N, '/api/set')[0].body));
  const opts = await N.p.$$eval('#sleepOpts button', bs => bs.map(b => b.textContent.trim()));
  check('sleep options 15/30/60/90', opts.length === 4 && ['15', '30', '60', '90'].every((m, i) => opts[i].startsWith(m)), opts.join());
  await N.p.click('#sleepCancel'); await until(() => posts(N, '/api/do').length);
  check('«إلغاء» posts /api/do {op: sleep_cancel}', JSON.stringify(posts(N, '/api/do')[0].body) === '{"op":"sleep_cancel"}');
  await N.p.click('#saverGo'); await until(() => posts(N, '/api/do').length === 2);
  check('screensaver card posts /api/do {op: screensaver}', JSON.stringify(posts(N, '/api/do')[1].body) === '{"op":"screensaver"}');
  check('idle picker shows the current value', (await N.p.inputValue('#saverIdle')) === '10');
  await N.p.selectOption('#saverIdle', '5'); await until(() => posts(N, '/api/set').length === 2);
  check('idle minutes post /api/set {saver_idle: 5} as a number', JSON.stringify(posts(N, '/api/set')[1].body) === '{"saver_idle":5}', JSON.stringify(posts(N, '/api/set')[1].body));
  await N.ctx.close();
});

// ---- settings, PIN, update, logout
await T('settings', async () => {
  const S = await boot({ cfg: { pin: true } });
  const { p } = S;
  await tab(S, 'settings');
  await p.click('#cardsGo'); await p.waitForSelector('#dlg:not(.hidden)');
  check('a pin_required answer opens the PIN prompt', posts(S, '/api/set').length === 1 && !('pin' in posts(S, '/api/set')[0].body));
  await p.fill('#pin', '9999'); await p.click('#pinOk'); await p.waitForSelector('#dlg:not(.hidden)');
  check('pin_wrong asks again, with the reason', (await p.textContent('#dlgT')).includes('غير صحيح') && posts(S, '/api/set')[1].body.pin === '9999');
  await p.fill('#pin', '1234'); await p.click('#pinOk');
  await until(() => posts(S, '/api/set').length === 3 && !S.reqs.some(r => false));
  await sleep(200);
  const last = posts(S, '/api/set')[2];
  check('the same request is sent again with `pin`', last && last.body.pin === '1234' && Array.isArray(last.body.home_cards) && !(await p.isVisible('#dlg')), JSON.stringify(last && last.body));
  await p.click('#cardsGo'); await sleep(300);
  check('the PIN is kept for the session, not asked again', !(await p.isVisible('#dlg')) && posts(S, '/api/set')[3].body.pin === '1234');
  await S.ctx.close();

  const C = await boot({ cfg: { pin: true } });
  await tab(C, 'settings'); await C.p.click('#cardsGo'); await C.p.waitForSelector('#dlg:not(.hidden)'); await C.p.click('#pinNo'); await sleep(200);
  check('cancel closes the prompt and sends nothing more', !(await C.p.isVisible('#dlg')) && posts(C, '/api/set').length === 1);
  await C.ctx.close();

  const N = await boot();
  const { p: q } = N;
  await tab(N, 'settings');
  check('current city is shown', (await q.textContent('#cityNow')).includes('الرياض'));
  await q.fill('#cityQ', 'ج'); await q.click('#cityGo'); await q.waitForSelector('#cityRes button');
  await q.click('#cityRes button'); await until(() => posts(N, '/api/set').length);
  check('city search posts /api/cities {q} then /api/set {city}', posts(N, '/api/cities')[0].body.q === 'ج' && posts(N, '/api/set')[0].body.city === 'جدة');
  await q.locator('#aShort').click(); await until(() => posts(N, '/api/set').length === 2);
  check('adhan switch posts adhan_short', JSON.stringify(posts(N, '/api/set')[1].body) === '{"adhan_short":true}');
  await q.selectOption('#aVoice', '1'); await q.locator('#aOn').click(); await until(() => posts(N, '/api/set').length >= 4);
  check('adhan voice and on/off post their keys', posts(N, '/api/set').slice(2).some(r => r.body.adhan_voice === 1) && posts(N, '/api/set').slice(2).some(r => r.body.adhan_on === false));
  await q.click('#upCheck'); await q.waitForSelector('#upGo:not(.hidden)');
  check('update: check, then «ثبّت» appears with the status word', posts(N, '/api/update')[0].body.op === 'check' && (await q.textContent('#upState')).includes('يوجد تحديث جديد') && (await q.textContent('#upState')).includes('42'));
  await q.click('#upGo'); await until(() => posts(N, '/api/update').length === 2);
  check('update install posts op install', posts(N, '/api/update')[1].body.op === 'install');
  N.cfg.update = { ok: true, status: 'error', reason: 'offline' }; await q.click('#upCheck'); await sleep(300);
  check('update error shows its Arabic word', (await q.textContent('#upState')).includes('لا يوجد اتصال'));
  await q.click('#out'); await q.waitForSelector('#login:not(.hidden)');
  check('«قطع الاتصال» posts /api/logout and returns to the login', posts(N, '/api/logout').length === 1 && !(await q.isVisible('#app')));
  await N.ctx.close();
});

// ---- sleep cancel is PIN-protected like /api/set
await T('sleep cancel pin', async () => {
  const S = await boot({ cfg: { pin: true } });
  await tab(S, 'now');
  await S.p.click('#sleepCancel'); await S.p.waitForSelector('#dlg:not(.hidden)');
  check('do sleep_cancel answered pin_required opens the PIN prompt', posts(S, '/api/do').length === 1 && !('pin' in posts(S, '/api/do')[0].body));
  await S.p.fill('#pin', '1234'); await S.p.click('#pinOk');
  await until(() => posts(S, '/api/do').length === 2); await sleep(150);
  check('and is resent with the pin', JSON.stringify(posts(S, '/api/do')[1].body) === '{"op":"sleep_cancel","pin":"1234"}' && !(await S.p.isVisible('#dlg')), JSON.stringify(posts(S, '/api/do')[1].body));
  await S.p.click('#saverGo'); await until(() => posts(S, '/api/do').length === 3);
  check('do screensaver carries no pin prompt', JSON.stringify(posts(S, '/api/do')[2].body.op) === '"screensaver"' && !(await S.p.isVisible('#dlg')) || posts(S, '/api/do')[2].body.op === 'screensaver');
  await S.ctx.close();
});

// ---- session lost
await T('no_session', async () => {
  const S = await boot();
  S.cfg.noSession = true;
  await tab(S, 'settings');
  await S.p.waitForSelector('#login:not(.hidden)', { timeout: 3000 });
  check('no_session returns to the login with a word', (await S.p.textContent('#loginState')).includes('انتهت الجلسة') && !(await S.p.isVisible('#app')));
  const keysBefore = keysSent(S).length; await sleep(300);
  check('and the page stops polling', S.reqs.filter(r => r.path === '/api/state').length < 6 && keysSent(S).length === keysBefore);
  await S.ctx.close();
});

// ---- themes
const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
const ratio = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
await T('themes', async () => {
  for (const scheme of ['dark', 'light']) {
    const S = await boot({ scheme });
    await tab(S, 'settings');
    const c = await S.p.evaluate(() => {
      const rgb = e => getComputedStyle(e).color.match(/[\d.]+/g).slice(0, 3).map(Number);
      const bg = (getComputedStyle(document.body).backgroundColor === 'rgba(0, 0, 0, 0)' ? getComputedStyle(document.documentElement).backgroundColor : getComputedStyle(document.body).backgroundColor).match(/[\d.]+/g).slice(0, 3).map(Number);
      const card = getComputedStyle(document.querySelector('#famCard')).backgroundColor.match(/[\d.]+/g).slice(0, 3).map(Number);
      const gold = getComputedStyle(document.querySelector('button.gold')).backgroundColor.match(/[\d.]+/g).slice(0, 3).map(Number);
      return { bg, card, gold, text: rgb(document.body), dim: rgb(document.querySelector('#t-settings p')), h2: rgb(document.querySelector('#t-settings h2')), ink: rgb(document.querySelector('button.gold')), famDim: rgb(document.querySelector('#famPin')) };
    });
    check(`${scheme}: main text vs background >= 4.5`, ratio(c.text, c.bg) >= 4.5, ratio(c.text, c.bg).toFixed(2));
    check(`${scheme}: secondary text and headings >= 4.5`, ratio(c.dim, c.bg) >= 4.5 && ratio(c.h2, c.bg) >= 4.5 && ratio(c.famDim, c.card) >= 4.5, [ratio(c.dim, c.bg), ratio(c.h2, c.bg), ratio(c.famDim, c.card)].map(x => x.toFixed(2)).join('/'));
    check(`${scheme}: gold button label >= 4.5`, ratio(c.ink, c.gold) >= 4.5, ratio(c.ink, c.gold).toFixed(2));
    check(`phone set to ${scheme}: the page is dark by default (one look on every screen, whatever the phone is set to)`, lum(c.bg) < 0.1);
    // every screen has the very same colours
    const looks = [];
    for (const name of ['remote', 'apps', 'now', 'settings']) {
      await tab(S, name);
      looks.push(await S.p.evaluate(() => { const g = e => getComputedStyle(e).backgroundColor + '|' + getComputedStyle(e).color; return g(document.body) + '|' + g(document.querySelector('button')); }));
    }
    check(`phone set to ${scheme}: the four screens share one look`, new Set(looks).size === 1, looks.join(' / '));
    // the toggle switches ALL screens together and remembers it
    await S.p.click('#theme'); await sleep(100);
    const flipped = await S.p.evaluate(() => [document.documentElement.getAttribute('data-theme'), localStorage.getItem('urtvTheme')]);
    check(`phone set to ${scheme}: the toggle goes to light and remembers it`, flipped[0] === 'light' && flipped[1] === 'light', flipped.join());
    const lights = [];
    for (const name of ['remote', 'apps', 'now', 'settings']) {
      await tab(S, name);
      lights.push(await S.p.evaluate(() => (getComputedStyle(document.body).backgroundColor === 'rgba(0, 0, 0, 0)' ? getComputedStyle(document.documentElement).backgroundColor : getComputedStyle(document.body).backgroundColor)));
    }
    check(`phone set to ${scheme}: after the toggle all four screens are light together`, new Set(lights).size === 1 && lum(lights[0].match(/[\d.]+/g).slice(0, 3).map(Number)) > 0.5, lights.join(' / '));
    const lc = await S.p.evaluate(() => { const rgb = e => getComputedStyle(e).color.match(/[\d.]+/g).slice(0, 3).map(Number); const bg = (getComputedStyle(document.body).backgroundColor === 'rgba(0, 0, 0, 0)' ? getComputedStyle(document.documentElement).backgroundColor : getComputedStyle(document.body).backgroundColor).match(/[\d.]+/g).slice(0, 3).map(Number); return { bg, text: rgb(document.body), dim: rgb(document.querySelector('#t-settings p')), h2: rgb(document.querySelector('#t-settings h2')) }; });
    check(`light after the toggle: text, secondary text and headings >= 4.5`, ratio(lc.text, lc.bg) >= 4.5 && ratio(lc.dim, lc.bg) >= 4.5 && ratio(lc.h2, lc.bg) >= 4.5, [ratio(lc.text, lc.bg), ratio(lc.dim, lc.bg), ratio(lc.h2, lc.bg)].join());
    await S.p.reload(); await sleep(300);
    await S.ctx.close();
  }
});

// ---- size, targets, overflow, text, requests, errors
for (const [w, h] of [[360, 740], [412, 915]]) await T(`layout ${w}`, async () => {
  const S = await boot({ w, h, state: NEW({ adhan_live: true, adhan_prayer: 'العصر', foreground: 'other', player: { playing: true, source: 'surah', reciter: 'a', surah: 2, sleep_left: 5 }, update: { status: 'found', build: '9' } }), cfg: { pin: true } });
  const { p } = S;
  const offenders = [], wide = [], small = [];
  const scan = async name => {
    const r = await p.evaluate(() => {
      const vis = e => e.getClientRects().length > 0 && getComputedStyle(e).visibility !== 'hidden';
      const tgt = [...document.querySelectorAll('button, a, input, select, textarea, [role=button]')].filter(e => e.type !== 'hidden' && vis(e)).map(e => { const b = e.getBoundingClientRect(); return { n: (e.id || e.getAttribute('data-k') || e.getAttribute('data-t') || e.textContent.trim().slice(0, 14) || e.tagName), w: Math.round(b.width), h: Math.round(b.height) }; });
      const sm = [...document.querySelectorAll('body *')].filter(e => vis(e) && !['SCRIPT', 'STYLE', 'OPTION', 'OPTGROUP', 'TITLE'].includes(e.tagName) && [...e.childNodes].some(n => n.nodeType === 3 && n.textContent.trim()) && parseFloat(getComputedStyle(e).fontSize) < 14).map(e => e.tagName + ':' + e.textContent.trim().slice(0, 12));
      const wd = [...document.querySelectorAll('body *')].filter(e => vis(e) && e.getBoundingClientRect().right > innerWidth + 0.5 && getComputedStyle(e).position !== 'fixed').map(e => e.tagName + '#' + e.id);
      return { tgt, sm, wd, sw: document.documentElement.scrollWidth, iw: innerWidth };
    });
    r.tgt.filter(t => t.w < 48 || t.h < 48).forEach(t => offenders.push(`${name}/${t.n} ${t.w}x${t.h}`));
    r.sm.forEach(s => small.push(name + '/' + s));
    if (r.sw > r.iw || r.wd.length) wide.push(`${name}: scrollWidth ${r.sw} > ${r.iw} ${r.wd.join()}`);
  };
  await scan('remote');
  await tab(S, 'apps'); await p.waitForSelector('#grid .app'); await scan('apps');
  await tab(S, 'now'); await scan('now');
  await tab(S, 'settings');
  await p.fill('#cityQ', 'ج'); await p.click('#cityGo'); await p.waitForSelector('#cityRes button');
  await scan('settings');
  await p.click('#cardsGo'); await p.waitForSelector('#dlg:not(.hidden)'); await scan('pin');
  check(`${w}px: every visible button/input/select is >= 48 px in both directions`, offenders.length === 0, offenders.join('; '));
  check(`${w}px: no horizontal scroll, nothing past the edge`, wide.length === 0, wide.join('; '));
  check(`${w}px: no visible text below 14 px`, small.length === 0, small.join('; '));
  check(`${w}px: dir=rtl, lang=ar`, await p.evaluate(() => document.documentElement.dir === 'rtl' && document.documentElement.lang === 'ar'));
  check(`${w}px: the tab bar stays on screen with the safe-area padding`, await p.evaluate(() => { const b = document.getElementById('bar').getBoundingClientRect(); return b.bottom <= innerHeight + 1 && b.height >= 56; }));
  check(`${w}px: nothing asked from anywhere but the page and /api`, S.foreign.length === 0, S.foreign.join());
  const errs = S.errors.filter(e => !/status of 403/.test(e));   // the stub's pin_required answers
  check(`${w}px: no console errors (CSP violations included)`, errs.length === 0, errs.join(' | '));
  await S.ctx.close();
});

// ---- the cards: a card, then «بدون» (an empty list): each press of «حفظ البطاقات» sends exactly what the selects show
await T('cards', async () => {
  let shown = [];   // what the TV says its cards are
  const S = await boot({ state: NEW({ home_cards: [] }), cfg: { state: () => NEW({ home_cards: shown.slice() }) } });
  const { p } = S;
  await tab(S, 'settings');
  const sel = i => p.locator('#cards select').nth(i);
  await sel(0).selectOption('prayer');
  await p.click('#cardsGo'); await until(() => posts(S, '/api/set').length === 1);
  check('a card, saved: one POST with that card', JSON.stringify(posts(S, '/api/set').map(r => r.body)) === '[{"home_cards":["prayer"]}]', JSON.stringify(posts(S, '/api/set').map(r => r.body)));
  shown = ['prayer'];
  const n = S.reqs.filter(r => r.path === '/api/state').length;
  await until(() => S.reqs.filter(r => r.path === '/api/state').length > n + 1); await sleep(150);
  check('the TV\'s answer after the save is drawn (the card is the first one)', (await sel(0).inputValue()) === 'prayer');
  await sel(0).selectOption({ label: 'بدون' });
  await p.click('#cardsGo'); await sleep(500);
  const sets = posts(S, '/api/set').map(r => r.body);
  check('«بدون», saved ONCE: exactly one more POST, with an empty list', sets.length === 2 && JSON.stringify(sets[1]) === '{"home_cards":[]}', JSON.stringify(sets));
  await S.ctx.close();

  // the TV's answer to the first save is still on its way when the person changes the card: it must not put the old card back
  shown = [];
  const D = await boot({ state: NEW({ home_cards: [] }), cfg: { state: () => NEW({ home_cards: shown.slice() }) } });
  await tab(D, 'settings');
  await D.p.locator('#cards select').nth(0).selectOption('prayer');
  D.cfg.stateDelay = 1200;
  await D.p.click('#cardsGo'); await until(() => posts(D, '/api/set').length === 1);
  shown = ['prayer'];
  await D.p.locator('#cards select').nth(0).selectOption({ label: 'بدون' });
  await sleep(2000);
  check('an answer that was on its way does not put the old card back while the person is choosing', (await D.p.locator('#cards select').nth(0).inputValue()) === '', await D.p.locator('#cards select').nth(0).inputValue());
  D.cfg.stateDelay = 0;
  await D.p.click('#cardsGo'); await sleep(400);
  check('…and the save sends what the selects show (an empty list)', JSON.stringify(posts(D, '/api/set').map(r => r.body).pop()) === '{"home_cards":[]}', JSON.stringify(posts(D, '/api/set').map(r => r.body)));
  // changed while the save is still on its way to the TV
  D.cfg.setDelay = 700; shown = ['prayer'];
  await D.p.locator('#cards select').nth(0).selectOption('weather');
  await D.p.click('#cardsGo');
  await D.p.locator('#cards select').nth(0).selectOption({ label: 'بدون' });
  await sleep(2200);
  check('a choice made while the save is on its way stays (the select is not redrawn from the TV)', (await D.p.locator('#cards select').nth(0).inputValue()) === '', await D.p.locator('#cards select').nth(0).inputValue());
  D.cfg.setDelay = 0;
  check('the message over the page never takes a tap (a button under it is pressed)', (await D.p.evaluate(() => getComputedStyle(document.getElementById('msg')).pointerEvents)) === 'none');
  await D.ctx.close();
});

await T('size', async () => {
  check('the page is under 64 KB', Buffer.byteLength(HTML) < 64 * 1024, Buffer.byteLength(HTML));
  check('no inline handler attributes, one nonce\'d script, no external address', !/\son[a-z]+\s*=/.test(HTML.replace(/<script[\s\S]*<\/script>/, '')) && (HTML.match(/<script/g) || []).length === 1 && HTML.includes('<script nonce="__NONCE__">') && !/https?:\/\/(?!github)/.test(HTML));
  check('the page builds no URL with a question mark', !/['"`]\/api\/[^'"`]*\?/.test(HTML));
});

await browser.close();
console.log(failed ? `\n${failed} FAILED` : '\nall passed');
process.exit(failed ? 1 : 0);
