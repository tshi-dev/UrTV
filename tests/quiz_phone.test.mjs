// Phone page of the live quiz (assets/quiz/play.html), in a real browser, with NO TV: the page is served by page.route()
// and a scripted fake server answers /api/join, /api/state, /api/answer and /api/leave.
//
//   NODE_PATH=$(npm root -g) node tests/quiz_phone.test.mjs        (or from a checkout that has node_modules)
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const FILE = path.join(ROOT, 'assets/quiz/play.html');
const HTML = readFileSync(FILE, 'utf8');
let failed = 0;
const check = (name, ok, detail = '') => {
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail));
  if (!ok) failed++;
};
const run = async (name, fn) => { try { await fn(); } catch (e) { check(name, false, 'threw: ' + e.message.split('\n')[0] + ' @' + [...e.stack.matchAll(/quiz_phone.test.mjs:(\d+)/g)].map(m=>m[1]).join(',')); } };

const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const ORIGIN = 'http://quiz.test';
const AD = s => String(s).replace(/\d/g, d => '٠١٢٣٤٥٦٧٨٩'[d]);

// ---------- the fake server ----------
function makeServer() {
  const s = { calls: [], hosts: new Set(), ver: 0, fail: false, joinReply: { ok: true, t: 'TOK1', n: 'Reem', k: 3 }, answerReply: { ok: true }, state: null };
  s.set = (ph, extra = {}) => { s.t0 = Date.now(); s.state = { ok: true, v: ++s.ver, ph, me: { n: 'Reem', k: 3, s: 0, r: 0, of: 6, left: false }, pl: 6, ...extra }; };
  s.live = () => {   // like the TV: the milliseconds left go down with the time
    const x = s.state;
    if (x && x.ok && x.q && x.q.ms != null) { s.base = s.base && s.base.v === x.v ? s.base : { v: x.v, ms: x.q.ms, t: s.t0 }; return { ...x, q: { ...x.q, ms: Math.max(0, s.base.ms - (Date.now() - s.base.t)) } }; }
    return x;
  };
  s.set('lobby');
  s.err = e => { s.state = { ok: false, e }; };
  s.of = pathname => s.calls.filter(c => c.path === pathname);
  return s;
}
const QA = { i: 2, n: 10, t: 'كم عدد أضلاع المسدّس؟', o: ['أربعة', 'خمسة', 'ستة', 'ثمانية'], ms: 14000, dur: 20000, ans: 2, of: 6, pick: -1, wait: false };
const QE = { ...QA, t: 'How many sides does a hexagon have?', o: ['Four', 'Five', 'Six', 'Eight'] };

async function open({ lang = 'ar', theme = 'light', width = 360, height = 740, reduced = false, query = null, locale = null, server = makeServer(), url = '/1234' } = {}) {
  const ctx = await browser.newContext({
    viewport: { width, height }, deviceScaleFactor: 2, hasTouch: true, isMobile: true,
    colorScheme: theme, reducedMotion: reduced ? 'reduce' : 'no-preference', ...(locale ? { locale } : {})
  });
  await ctx.route(/.*/, async r => {
    const u = new URL(r.request().url());
    server.hosts.add(u.host);
    if (u.host !== 'quiz.test') return r.abort();
    const body = r.request().postData() || '';
    const form = Object.fromEntries(new URLSearchParams(body));
    if (u.pathname.startsWith('/api/')) {
      server.calls.push({ path: u.pathname, form, query: Object.fromEntries(u.searchParams), type: r.request().headers()['content-type'] || '', method: r.request().method() });
      const json = o => r.fulfill({ contentType: 'application/json', body: JSON.stringify(o) });
      if (u.pathname === '/api/join') return json(server.joinReply);
      if (u.pathname === '/api/answer') return json(server.answerReply);
      if (u.pathname === '/api/leave') return json({ ok: true });
      if (u.pathname === '/api/state') return server.fail ? r.abort('failed') : json(server.live() || { ok: false, e: 'token' });
    }
    if (/^\/\d{4}\/?$/.test(u.pathname)) return r.fulfill({ contentType: 'text/html; charset=utf-8', body: HTML });
    return r.fulfill({ status: 404, body: '' });
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  const qs = query != null ? query : `?l=${lang}&theme=${theme}`;
  await p.goto(ORIGIN + url + qs);
  return { p, ctx, server, errors };
}
const at = (p, name, t = 5000) => p.waitForFunction(n => document.getElementById('app').getAttribute('data-s') === n, name, { timeout: t });
const txt = p => p.evaluate(() => document.body.innerText);

// ---------- what every screen must satisfy ----------
const audit = p => p.evaluate(() => {
  const out = [], W = innerWidth;
  if (document.documentElement.scrollWidth > W) out.push('page scrolls sideways (' + document.documentElement.scrollWidth + ')');
  for (const el of document.querySelectorAll('body *')) {
    const cs = getComputedStyle(el);
    if (cs.display === 'none') continue;
    const r = el.getBoundingClientRect();
    if (r.width && (r.right > W + 0.5 || r.left < -0.5)) out.push('too wide: ' + el.tagName + '.' + el.className);
  }
  const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  for (let n; (n = w.nextNode());) {
    if (!n.nodeValue.trim()) continue;
    const el = n.parentElement, cs = getComputedStyle(el);
    if (cs.display === 'none' || cs.visibility === 'hidden' || !el.getClientRects().length) continue;
    if (parseFloat(cs.fontSize) < 18) out.push('small text ' + cs.fontSize + ': ' + n.nodeValue.trim().slice(0, 20));
  }
  if (document.body.textContent.includes('·')) out.push('middle dot in the text');
  return out;
});
const stampCheck = (p, n = 4) => p.evaluate(n => {
  const s = [...document.querySelectorAll('.stamp')].map(e => e.getBoundingClientRect()), out = [];
  if (s.length !== n) return ['stamps: ' + s.length];
  const col = (innerWidth - 32 - 12) / 2;
  s.forEach((r, i) => {
    if (r.height < 56) out.push('stamp ' + i + ' height ' + r.height);
    if (Math.abs(r.width - col) > 1.5) out.push('stamp ' + i + ' width ' + r.width + ' vs ' + col);
  });
  if (Math.abs(s[0].top - s[1].top) > 1 || Math.abs(s[2].top - s[3].top) > 1 || s[2].top < s[0].bottom) out.push('not a 2x2 grid');
  if (s[0].left < 15.5 && s[1].left < 15.5) out.push('gutter');
  return out;
}, n);

// ======================================================================================
// 1. the whole game, for ar/en x dark/light at 360x740
// ======================================================================================
const STR = {
  ar: { inn: 'أنت داخل', saved: 'سجّلنا جوابك', right: 'صح', wrong: 'غلط', pts: '+' + AD(820), end: 'أنت الثالث', score: AD(2480), closed: 'أُغلقت الغرفة', bad: 'الرمز غير صحيح', paused: 'اللعبة متوقفة', wait: 'انتظر السؤال التالي', timeup: 'انتهى الوقت', cnt: [AD(1), AD(1), AD(3), AD(1)], q: QA },
  en: { inn: "You're in", saved: 'Answer saved', right: 'Right', wrong: 'Wrong', pts: '+820', end: "You're 3rd", score: '2480', closed: 'The room is closed', bad: 'The code is not right', paused: 'Game paused', wait: 'Wait for the next question', timeup: "Time's up", cnt: ['1', '1', '3', '1'], q: QE }
};
for (const lang of ['ar', 'en']) for (const theme of ['dark', 'light']) {
  const tag = `${lang}/${theme}: `, X = STR[lang];
  await run(tag + 'game', async () => {
    const { p, ctx, server, errors } = await open({ lang, theme });
    const wrong = lang === 'ar' ? 'خاطئ' : 'x';
    check(tag + 'direction and language', await p.evaluate(() => [document.documentElement.dir, document.documentElement.lang].join()) === (lang === 'ar' ? 'rtl,ar' : 'ltr,en'));
    check(tag + 'theme follows ?theme=', await p.evaluate(() => document.documentElement.getAttribute('data-theme')) === theme);
    await at(p, 'join');
    let a = await audit(p); check(tag + 'join screen: fits, text >= 18px, no middle dot', !a.length, a.join('; '));
    check(tag + 'join: 8 colour swatches, each >= 44px', await p.evaluate(() => { const s = [...document.querySelectorAll('.swb')]; return s.length === 8 && s.every(b => { const r = b.getBoundingClientRect(); return r.width >= 44 && r.height >= 44; }); }));

    // join form
    await p.fill('#nm', 'Reem');
    await p.locator('.swb').nth(3).click();
    await p.click('#go');
    await at(p, 'loading', 3000).catch(() => {});
    server.set('lobby');
    await at(p, 'lobby');
    const j = server.of('/api/join')[0];
    check(tag + 'join posts c,n,k,t,l form-encoded', j && j.method === 'POST' && /x-www-form-urlencoded/.test(j.type) && j.form.c === '1234' && j.form.n === 'Reem' && j.form.k === '3' && j.form.t === '' && j.form.l === lang, JSON.stringify(j));
    check(tag + 'waiting screen says you are in', (await txt(p)).includes(X.inn));
    a = await audit(p); check(tag + 'lobby screen audit', !a.length, a.join('; '));
    const sc = server.of('/api/state')[0];
    check(tag + 'state polled with c and the token', sc && sc.query.c === '1234' && sc.query.t === 'TOK1', JSON.stringify(sc));

    // question
    server.set('question', { q: X.q });
    await at(p, 'question');
    a = await audit(p); check(tag + 'question screen audit', !a.length, a.join('; '));
    a = await stampCheck(p); check(tag + 'stamps: 2x2, full column, >= 56px tall', !a.length, a.join('; '));
    check(tag + 'letter tabs', await p.evaluate(() => [...document.querySelectorAll('.tab')].map(e => e.textContent).join('')) === (lang === 'ar' ? 'أبجد' : 'ABCD'));
    check(tag + '20 ticks, some on', await p.evaluate(() => document.querySelectorAll('.tk').length === 20 && document.querySelectorAll('.tk.on').length > 10));
    check(tag + 'question text and seconds shown', await p.evaluate(q => document.querySelector('.qt').textContent === q, X.q.t) && /\d|[٠-٩]/.test(await p.textContent('.timer')));
    if (lang === 'ar') check(tag + 'Arabic digits in Arabic', !/[0-9]/.test(await p.evaluate(() => document.querySelector('.qn').textContent + document.querySelector('.timer').textContent)));
    else check(tag + 'Latin digits in English', !/[٠-٩]/.test(await txt(p)));

    // lock on tap
    await p.locator('.stamp').nth(2).click();
    await at(p, 'locked');
    check(tag + 'locked screen says the answer is saved', (await txt(p)).includes(X.saved));
    check(tag + 'one stamp ink-filled, the others dashed', await p.evaluate(() => document.querySelectorAll('.stamp.pick').length === 1 && document.querySelectorAll('.stamp.off').length === 3));
    a = await audit(p); check(tag + 'locked screen audit', !a.length, a.join('; '));
    server.set('question', { q: { ...X.q, pick: 2, ans: 3 } });
    await p.waitForTimeout(1000);
    check(tag + 'still locked after the server confirms', await p.getAttribute('#app', 'data-s') === 'locked');

    // reveal, right
    server.set('reveal', { q: { ...X.q, ms: undefined, pick: 2 }, rv: { c: 2, ok: true, p: 820, st: 3, r: 2, d: 1, cnt: [1, 1, 3, 1] }, me: { n: 'Reem', k: 3, s: 820, r: 2, of: 6, left: false } });
    await at(p, 'reveal');
    let t = await txt(p);
    check(tag + 'reveal right: word, points, rank', t.includes(X.right) && t.includes(X.pts) && t.includes(lang === 'ar' ? AD(2) : '2'), t.slice(0, 80));
    check(tag + 'reveal: right stamp green, 4 vote counts', await p.evaluate(c => document.querySelectorAll('.stamp.ok').length === 1 && [...document.querySelectorAll('.cnt')].map(e => e.textContent).join() === c.join(), X.cnt));
    a = await audit(p); check(tag + 'reveal (right) audit', !a.length, a.join('; '));

    // reveal, wrong (picked 0, right answer 2)
    server.set('reveal', { q: { ...X.q, ms: undefined, pick: 0 }, rv: { c: 2, ok: false, p: 0, st: 0, r: 4, d: -1, cnt: [2, 1, 2, 1] }, me: { n: 'Reem', k: 3, s: 820, r: 4, of: 6, left: false } });
    await p.waitForFunction(w => document.body.innerText.includes(w), X.wrong);
    check(tag + 'reveal wrong: picked stamp ink, right one green', await p.evaluate(() => document.querySelectorAll('.stamp.ok').length === 1 && document.querySelectorAll('.stamp.pick').length === 1 && document.querySelectorAll('.arr.d').length === 1));
    a = await audit(p); check(tag + 'reveal (wrong) audit', !a.length, a.join('; '));

    // reveal, no answer
    server.set('reveal', { q: { ...X.q, ms: undefined, pick: -1 }, rv: { c: 2, ok: null, p: 0, st: 0, r: 4, d: 0, cnt: [2, 1, 2, 1] } });
    await p.waitForFunction(w => document.querySelector('.hero.n') && document.body.innerText.includes(w), X.timeup);
    a = await audit(p); check(tag + 'reveal (no answer) audit', !a.length, a.join('; '));

    // late join, paused
    server.set('question', { q: { ...X.q, i: 3, wait: true } });
    await at(p, 'wait');
    check(tag + 'late joiner: wait text and no stamps', (await txt(p)).includes(X.wait) && !(await p.$('.stamp')));
    a = await audit(p); check(tag + 'wait screen audit', !a.length, a.join('; '));
    server.set('paused');
    await at(p, 'paused');
    check(tag + 'paused text', (await txt(p)).includes(X.paused));
    a = await audit(p); check(tag + 'paused screen audit', !a.length, a.join('; '));

    // end
    server.set('end', { end: { r: 3, of: 6, s: 2480 }, me: { n: 'Reem', k: 3, s: 2480, r: 3, of: 6, left: false } });
    await at(p, 'end');
    t = await txt(p);
    check(tag + 'end: ordinal, score, leave button', t.includes(X.end) && t.includes(X.score) && !!(await p.$('#leave')), t.slice(0, 120));
    a = await audit(p); check(tag + 'end screen audit', !a.length, a.join('; '));

    // closed
    server.set('closed');
    await at(p, 'closed');
    check(tag + 'closed text', (await txt(p)).includes(X.closed));
    a = await audit(p); check(tag + 'closed screen audit', !a.length, a.join('; '));
    const n0 = server.of('/api/state').length; await p.waitForTimeout(1600);
    check(tag + 'polling stops when closed', server.of('/api/state').length === n0);

    // wrong code
    server.err('code');
    await p.reload(); await at(p, 'bad');
    check(tag + 'wrong code: message and nothing else to do', (await txt(p)).includes(X.bad) && !(await p.$('button, input, a')));
    a = await audit(p); check(tag + 'wrong code audit', !a.length, a.join('; '));
    check(tag + 'no script errors', !errors.length, errors.join('; '));
    check(tag + 'only same-origin requests', [...server.hosts].every(h => h === 'quiz.test'), [...server.hosts].join());
    await ctx.close();
  });
}

// ======================================================================================
// 2. lock on first tap
// ======================================================================================
await run('lock', async () => {
  const { p, ctx, server } = await open({});
  await p.fill('#nm', 'Reem'); await p.click('#go');
  server.set('question', { q: QA });
  await at(p, 'question');
  await p.locator('.stamp').nth(1).click();
  await p.locator('.stamp').nth(1).click();
  await p.locator('.stamp').nth(3).click();
  await p.waitForTimeout(500);
  const a = server.of('/api/answer');
  check('lock: exactly ONE /api/answer after three taps', a.length === 1, String(a.length));
  check('lock: answer body c,t,q,a', a[0] && a[0].form.c === '1234' && a[0].form.t === 'TOK1' && a[0].form.q === '2' && a[0].form.a === '1' && /x-www-form-urlencoded/.test(a[0].type), JSON.stringify(a[0]));
  check('lock: first tap stays highlighted', await p.evaluate(() => document.querySelectorAll('.stamp.pick').length === 1 && document.querySelectorAll('.stamp')[1].classList.contains('pick')));
  await ctx.close();
});
for (const e of ['done', 'closed']) await run('lock ' + e, async () => {
  const { p, ctx, server } = await open({});
  server.answerReply = { ok: false, e };
  await p.fill('#nm', 'Reem'); await p.click('#go');
  server.set('question', { q: QA });
  await at(p, 'question');
  await p.locator('.stamp').nth(0).click();
  await p.waitForTimeout(500);
  check(`lock: server says ${e}, still shown as locked and not resent`, await p.getAttribute('#app', 'data-s') === 'locked' && server.of('/api/answer').length === 1);
  await ctx.close();
});
await run('time over', async () => {
  const { p, ctx, server } = await open({});
  await p.fill('#nm', 'Reem'); await p.click('#go');
  server.set('question', { q: { ...QA, ms: 600 } });
  await at(p, 'question');
  await p.waitForFunction(() => document.body.innerText.includes('انتهى الوقت'), null, { timeout: 3000 });
  await p.locator('.stamp').nth(0).click();
  await p.waitForTimeout(300);
  check('time over: text shown and a tap sends nothing', server.of('/api/answer').length === 0);
  await ctx.close();
});

// ======================================================================================
// 3. join: errors, name limit, leave
// ======================================================================================
await run('join errors', async () => {
  const { p, ctx, server } = await open({});
  await p.click('#go');
  check('join: empty name is refused before any request', server.of('/api/join').length === 0 && (await txt(p)).includes('اكتب اسمك أولًا'));
  await p.fill('#nm', 'ABCDEFGHIJKLMNOP');
  check('join: name field stops at 12 characters', await p.inputValue('#nm') === 'ABCDEFGHIJKL');
  for (const [e, w] of [['full', 'الغرفة ممتلئة'], ['locked', 'انتظر دقيقة'], ['name', 'اكتب اسمًا آخر'], ['rate', 'حاول مرة ثانية']]) {
    server.joinReply = { ok: false, e };
    await p.click('#go');
    await p.waitForFunction(w => document.body.innerText.includes(w), w);
    check('join error ' + e + ': message shown, form stays', !!(await p.$('#nm')));
  }
  server.joinReply = { ok: false, e: 'code' }; await p.click('#go'); await at(p, 'bad');
  check('join error code: wrong code screen', (await txt(p)).includes('الرمز غير صحيح'));
  await ctx.close();
});
await run('remember', async () => {
  const { p, ctx, server } = await open({});
  await p.fill('#nm', 'Reem'); await p.locator('.swb').nth(5).click(); server.joinReply = { ok: true, t: 'TOK9', n: 'Reem', k: 5 };
  await p.click('#go'); server.set('lobby'); await at(p, 'lobby');
  check('remember: token kept per code, name and colour kept', await p.evaluate(() => localStorage.getItem('urtv.quiz.t.1234') === 'TOK9' && JSON.parse(localStorage.getItem('urtv.quiz.me')).k === 5));
  await ctx.close();
});
await run('leave', async () => {
  const { p, ctx, server } = await open({ lang: 'en' });
  await p.fill('#nm', 'Reem'); await p.click('#go'); server.set('lobby'); await at(p, 'lobby');
  await p.click('#leave');
  check('leave: first tap only asks again, no dialog, nothing sent', (await p.textContent('#leave')) === 'Tap again to leave' && server.of('/api/leave').length === 0);
  await p.click('#leave');
  await at(p, 'join');
  const l = server.of('/api/leave');
  check('leave: second tap posts c and t, back to the join form', l.length === 1 && l[0].form.c === '1234' && l[0].form.t === 'TOK1' && (await p.evaluate(() => localStorage.getItem('urtv.quiz.t.1234'))) === null, JSON.stringify(l));
  check('leave: name remembered for the next join', await p.inputValue('#nm') === 'Reem');
  await ctx.close();
});

// ======================================================================================
// 4. reconnect, connection loss
// ======================================================================================
await run('reconnect', async () => {
  const { p, ctx, server } = await open({});
  await p.fill('#nm', 'Reem'); await p.click('#go'); server.set('lobby'); await at(p, 'lobby');
  server.calls.length = 0;
  server.set('question', { q: QA });
  await p.reload();
  await at(p, 'question');
  check('reconnect: reload does not show the join form', !(await p.$('#nm')));
  const s = server.of('/api/state')[0];
  check('reconnect: first state call carries the stored token', s && s.query.t === 'TOK1' && s.query.c === '1234', JSON.stringify(s));
  check('reconnect: no join call needed', server.of('/api/join').length === 0);
  server.err('token');
  await at(p, 'join');
  check('reconnect: a refused token falls back to the join form', !!(await p.$('#nm')) && (await p.evaluate(() => localStorage.getItem('urtv.quiz.t.1234'))) === null);
  await ctx.close();
});
await run('lost connection', async () => {
  const { p, ctx, server } = await open({});
  await p.fill('#nm', 'Reem'); await p.click('#go'); server.set('lobby'); await at(p, 'lobby');
  server.fail = true;
  await p.waitForFunction(() => { const n = document.getElementById('net'); return n.style.display === 'block' && n.textContent === 'انقطع'; }, null, { timeout: 5000 });
  check('lost: banner says انقطع', true);
  const a = await audit(p); check('lost: banner passes the audit', !a.length, a.join('; '));
  const n0 = server.of('/api/state').length; await p.waitForTimeout(1500);
  check('lost: keeps retrying', server.of('/api/state').length > n0 + 1);
  server.fail = false;
  await p.waitForFunction(() => document.getElementById('net').textContent === 'رجعنا', null, { timeout: 5000 });
  check('lost: comes back by itself and says رجعنا', true);
  await p.waitForFunction(() => document.getElementById('net').style.display === 'none', null, { timeout: 4000 });
  check('lost: the رجعنا line goes away after about 2 s', true);
  await ctx.close();
});
await run('lost connection en', async () => {
  const { p, ctx, server } = await open({ lang: 'en' });
  await p.fill('#nm', 'Reem'); await p.click('#go'); server.set('lobby'); await at(p, 'lobby');
  server.fail = true;
  await p.waitForFunction(() => document.getElementById('net').textContent === 'Connection lost', null, { timeout: 5000 });
  server.fail = false;
  await p.waitForFunction(() => document.getElementById('net').textContent === 'Back', null, { timeout: 5000 });
  check('lost (en): Connection lost, then Back', true);
  await ctx.close();
});
await run('wake from background', async () => {
  const { p, ctx, server } = await open({});
  await p.fill('#nm', 'Reem'); await p.click('#go'); server.set('lobby'); await at(p, 'lobby');
  await p.evaluate(() => { Object.defineProperty(document, 'hidden', { configurable: true, get: () => true }); document.dispatchEvent(new Event('visibilitychange')); });
  await p.waitForTimeout(300); const n0 = server.of('/api/state').length; await p.waitForTimeout(1500);
  check('background: no polling while hidden', server.of('/api/state').length === n0);
  await p.evaluate(() => { Object.defineProperty(document, 'hidden', { configurable: true, get: () => false }); document.dispatchEvent(new Event('visibilitychange')); });
  await p.waitForFunction(() => document.getElementById('net').textContent === 'رجعنا', null, { timeout: 2000 });
  await p.waitForTimeout(300);
  check('background: polls at once on return and says رجعنا', server.of('/api/state').length > n0);
  await ctx.close();
});
await run('wake lock', async () => {
  const { p, ctx } = await open({});
  const ok = await p.evaluate(() => new Promise(res => {
    // the page asks at load; ask again through a visibility change with a stub installed
    let n = 0; Object.defineProperty(navigator, 'wakeLock', { configurable: true, value: { request: k => { n += k === 'screen' ? 1 : 0; return Promise.reject(new Error('no')); } } });
    document.dispatchEvent(new Event('visibilitychange')); setTimeout(() => res(n), 100);
  }));
  check('wake lock: requested again on visibilitychange, errors ignored', ok === 1, String(ok));
  await ctx.close();
});

// ======================================================================================
// 5. motion, long answers, widths, language and theme
// ======================================================================================
await run('reduced motion', async () => {
  for (const reduced of [true, false]) {
    const { p, ctx, server } = await open({ reduced });
    await p.fill('#nm', 'Reem'); await p.click('#go'); server.set('question', { q: QA }); await at(p, 'question');
    await p.locator('.stamp').nth(2).click();
    const r = await p.evaluate(() => {
      const ms = v => v.split(',').map(x => x.trim()).map(x => x.endsWith('ms') ? parseFloat(x) : parseFloat(x) * 1000);
      let maxT = 0, maxA = 0;
      for (const el of document.querySelectorAll('*')) { const cs = getComputedStyle(el); maxT = Math.max(maxT, ...ms(cs.transitionDuration)); maxA = Math.max(maxA, ...ms(cs.animationDuration)); }
      return { maxT, maxA, running: document.getAnimations().length };
    });
    if (reduced) check('reduced motion: no transition or animation durations, no running animation', r.maxT === 0 && r.maxA === 0 && r.running === 0, JSON.stringify(r));
    else check('normal motion: every duration under 400 ms', r.maxT < 400 && r.maxA < 400, JSON.stringify(r));
    await ctx.close();
  }
});
const LONG1 = 'هذه إجابة طويلة جدًا تتجاوز ستين حرفًا لنرى كيف تتصرف ورقة'.padEnd(60, 'ا').slice(0, 60);
const LONG2 = 'W'.repeat(60);
for (const width of [320, 360, 412]) await run('long answers ' + width, async () => {
  for (const lang of ['ar', 'en']) {
    const { p, ctx, server } = await open({ lang, width, height: 740, theme: 'dark' });
    await p.fill('#nm', 'Reem'); await p.click('#go');
    server.set('question', { q: { ...QA, t: 'س'.repeat(2) + ' ' + 'سؤال طويل '.repeat(8), o: [LONG1, LONG2, 'ستة', 'ثمانية'] } });
    await at(p, 'question'); await p.waitForTimeout(150);
    const r = await p.evaluate(() => {
      const out = [], W = innerWidth;
      if (document.documentElement.scrollWidth > W) out.push('page scrolls sideways');
      document.querySelectorAll('.stamp').forEach((s, i) => {
        const a = s.querySelector('.at'), sr = s.getBoundingClientRect(), ar = a.getBoundingClientRect(), fs = parseFloat(getComputedStyle(a).fontSize);
        if (ar.left < sr.left || ar.right > sr.right || ar.bottom > sr.bottom || ar.top < sr.top) out.push('text outside stamp ' + i);
        if (a.scrollWidth > a.clientWidth + 1) out.push('text wider than its box ' + i);
        if (fs < 18) out.push('font ' + fs);
        if (sr.right > W) out.push('stamp past the edge');
        if (i < 2 && fs > 28) out.push('font above 28');
      });
      return { out, fs: [...document.querySelectorAll('.at')].map(a => parseFloat(getComputedStyle(a).fontSize)) };
    });
    check(`long answers ${lang} @${width}: fit their stamps, font 18..28`, !r.out.length, r.out.join('; ') + ' ' + r.fs);
    const a = await audit(p); check(`long answers ${lang} @${width}: audit`, !a.length, a.join('; '));
    const sc = await stampCheck(p); check(`long answers ${lang} @${width}: stamps 2x2 and tall enough`, !sc.length, sc.join('; '));
    if (width === 360 && lang === 'ar') {
      await p.locator('.stamp').nth(0).click(); await p.waitForTimeout(100);
      const b = await audit(p); check('long answers: locked screen audit', !b.length, b.join('; '));
    }
    await ctx.close();
  }
});
await run('language and theme', async () => {
  const get = async o => { const x = await open(o); const r = await x.p.evaluate(() => [document.documentElement.lang, document.documentElement.dir, getComputedStyle(document.body).backgroundColor, document.title].join('|')); await x.ctx.close(); return r; };
  check('language: ?l=en wins', (await get({ query: '?l=en', locale: 'ar-SA' })).startsWith('en|ltr'));
  check('language: browser en-US without ?l', (await get({ query: '', locale: 'en-US' })).startsWith('en|ltr'));
  check('language: other browser language falls back to Arabic', (await get({ query: '', locale: 'fr-FR' })).startsWith('ar|rtl'));
  check('language: browser Arabic', (await get({ query: '', locale: 'ar-SA' })).startsWith('ar|rtl'));
  check('language: ?l=ar wins over an English browser', (await get({ query: '?l=ar', locale: 'en-US' })).startsWith('ar|rtl'));
  check('name constant gives the title', (await get({ query: '?l=ar' })).endsWith('|الأسرع') && (await get({ query: '?l=en' })).endsWith('|Fastest'));
  check('theme: ?theme=dark forces the dark paper', (await get({ query: '?theme=dark', theme: 'light' })).includes('rgb(20, 19, 15)'));
  check('theme: ?theme=light forces the light paper', (await get({ query: '?theme=light', theme: 'dark' })).includes('rgb(239, 232, 218)'));
  check('theme: follows the system without ?theme', (await get({ query: '?l=ar', theme: 'dark' })).includes('rgb(20, 19, 15)') && (await get({ query: '?l=ar', theme: 'light' })).includes('rgb(239, 232, 218)'));
});
await run('no code', async () => {
  const { p, ctx } = await open({ url: '/', query: '?l=ar' });
  // the fake server serves the page only for a four digit path, so serve it for this one too
  await ctx.close();
  const c2 = await browser.newContext(); await c2.route(/.*/, r => r.fulfill({ contentType: 'text/html', body: HTML })); const q = await c2.newPage(); await q.goto(ORIGIN + '/abc?l=ar');
  await at(q, 'bad'); check('no four digit code in the address: wrong code screen', true); await c2.close();
});

// ======================================================================================
// 6. the file itself
// ======================================================================================
check('file: under 40 KB', Buffer.byteLength(HTML) < 40 * 1024, String(Buffer.byteLength(HTML)));
check('file: no http anywhere (relative paths only)', !/https?:/i.test(HTML) && !/\/\/[a-z]/i.test(HTML.replace(/<\/?[a-z][^>]*>/gi, '')));
check('file: every url() and src is a /f/ font', [...HTML.matchAll(/url\(([^)]*)\)/g)].every(m => /^\/f\/[\w.-]+\.woff2$/.test(m[1])) && !/\ssrc=|<link|@import|<img|<iframe/i.test(HTML.replace(/src:url/g, '')));
check('file: no middle dot, no gradient, no blur', !HTML.includes('·') && !/gradient|backdrop-filter|blur\(/i.test(HTML));
check('file: the name is one constant', (HTML.match(/الأسرع/g) || []).length === 1 && (HTML.match(/Fastest/g) || []).length === 1);
check('file: the three fonts from the TV server', ['Thmanyah-Regular', 'Thmanyah-Medium', 'ThmanyahDisplay-Bold'].every(f => HTML.includes('/f/' + f + '.woff2')));
check('file: no border-radius above 8px', ![...HTML.matchAll(/border-radius:\s*(\d+)/g)].some(m => +m[1] > 8) && !/border-radius:\s*\d+px\s+\d+/.test(HTML) && !/50%/.test(HTML.replace(/clip-path:[^;}]*/g, '')));

await browser.close();
console.log(failed ? `\n${failed} FAILED` : '\nall passed');
process.exit(failed ? 1 : 0);
