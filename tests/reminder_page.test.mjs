// «المهام» with a time, on the page (the screensaver's card): it comes at the card time, says «بعد ٣٠ دقيقة» and then «الآن», «تم» and «ذكّرني بعد ١٠ دقائق»
// go to the app, any other key puts it away, it waits for the adhan, and the English page says it in English. Offline; the app's bridge is a stub.
//   node tests/reminder_page.test.mjs          (the fonts are not needed: the card is looked at as text)
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => {
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail));
  if (!ok) failed++;
};
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const T0 = Date.UTC(2026, 10, 5, 13, 0, 0);   // 5 Nov 2026, 16:00 in Riyadh

// rem: [[id, fire, due, rep, snd, where, text]]; bridge: what the app's UrtvNative says (adhan live or not)
async function open(file, rem, { adhan = false, bridge = true } = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  p.on('pageerror', e => console.log('pageerror: ' + e.message));
  await p.clock.install({ time: T0 });
  await p.addInitScript(({ adhan, bridge }) => {
    window.__calls = [];
    if (bridge) window.UrtvNative = {
      nowPlaying: () => '', adhan: () => window.__adhan ? '{"live":true,"prayer":4,"any":false}' : '',
      reminderShown: b => window.__calls.push(['shown', b]), reminderDone: id => window.__calls.push(['done', id]),
      reminderSnooze: (id, m) => window.__calls.push(['snooze', id, m]), reminderChime: () => window.__calls.push(['chime']),
    };
    window.__adhan = adhan;
  }, { adhan, bridge });
  const lines = rem.map(r => r.join('|')).join('\n');
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#tz=Asia%2FRiyadh&rem=' + encodeURIComponent(lines));
  await p.clock.runFor(1500);
  return p;
}
const state = p => p.evaluate(() => {
  const c = document.getElementById('rm');
  return { shown: !c.hidden, title: document.getElementById('rm-t').textContent, sub: document.getElementById('rm-s').textContent,
           ok: document.getElementById('rm-ok').textContent, later: document.getElementById('rm-sz').textContent, calls: window.__calls,
           focus: document.getElementById('rm-ok').classList.contains('on') ? 0 : 1 };
});
const MIN = 60000;
const rem = (id, fireIn, dueIn, snd = 0, text = 'موعد طبيب الأسنان') => [id, T0 + fireIn * MIN, T0 + dueIn * MIN, 0, snd, 0, text];

// a card whose time has come: 30 minutes ahead
let p = await open('tv.html', [rem(7, 0, 30)]);
let s = await state(p);
check('the card is up at its time, with the task', s.shown && s.title === 'موعد طبيب الأسنان', JSON.stringify(s));
check('it says the hour of the appointment and the minutes left', /^الساعة ٤:٣٠ م، بعد ٣٠ دقيقة$/.test(s.sub), s.sub);
check('two buttons: «تم» and «ذكّرني بعد ١٠ دقائق»', s.ok === 'تم' && s.later === 'ذكّرني بعد ١٠ دقائق', s.ok + ' | ' + s.later);
check('the app is told a card is up (it keeps the keys)', JSON.stringify(s.calls) === '[["shown",true]]', JSON.stringify(s.calls));
check('no chime unless asked', !s.calls.some(c => c[0] === 'chime'));
await p.clock.runFor(31 * MIN);
s = await state(p);
check('at the appointment it says «الآن»', s.shown && /، الآن$/.test(s.sub), s.sub);
await p.clock.runFor(31 * MIN);
s = await state(p);
check('an unanswered card goes 30 minutes after the appointment (the task stays)', !s.shown && !s.calls.some(c => c[0] === 'done'), JSON.stringify(s));

// not yet
p = await open('tv.html', [rem(7, 60, 90)]);
s = await state(p);
check('before its time there is no card', !s.shown);
await p.clock.runFor(61 * MIN);
s = await state(p);
check('it comes when its time does', s.shown);

// keys
p = await open('tv.html', [rem(7, 0, 30)]);
await p.keyboard.press('Enter');
s = await state(p);
check('OK on «تم» marks the task done and the card goes', !s.shown && s.calls.some(c => c[0] === 'done' && c[1] === 7), JSON.stringify(s));
p = await open('tv.html', [rem(7, 0, 30)]);
await p.keyboard.press('ArrowLeft');
s = await state(p);
check('an arrow moves the ring to the other button', s.focus === 1, JSON.stringify(s));
await p.keyboard.press('Enter');
s = await state(p);
check('OK on the snooze asks the app again in 10 minutes and the card goes', !s.shown && s.calls.some(c => c[0] === 'snooze' && c[1] === 7 && c[2] === 10), JSON.stringify(s));
await p.clock.runFor(9 * MIN);
check('it stays away for the 10 minutes', !(await state(p)).shown);
await p.clock.runFor(2 * MIN);
check('and comes back after them', (await state(p)).shown);
p = await open('tv.html', [rem(7, 0, 30)]);
await p.keyboard.press('ArrowDown');
s = await state(p);
check('any other key puts it away without finishing the task', !s.shown && !s.calls.some(c => c[0] === 'done' || c[0] === 'snooze'), JSON.stringify(s));

// the chime, the adhan, two at once, no bridge
p = await open('tv.html', [rem(7, 0, 30, 1)]);
check('the chime once when the reminder asks for it', (await state(p)).calls.filter(c => c[0] === 'chime').length === 1);
await p.clock.runFor(5000);
check('and not again', (await state(p)).calls.filter(c => c[0] === 'chime').length === 1);
p = await open('tv.html', [rem(7, 0, 30)], { adhan: true });
await p.clock.runFor(4000);
check('it waits while the adhan plays', !(await state(p)).shown);
await p.evaluate(() => { window.__adhan = false; });
await p.clock.runFor(4000);
check('and is there when it ends', (await state(p)).shown);
p = await open('tv.html', [rem(8, 0, 90, 0, 'ثاني'), rem(7, 0, 30, 0, 'الأول')]);
s = await state(p);
check('two due at once: the sooner appointment first', s.title === 'الأول', s.title);
await p.keyboard.press('Enter');
s = await state(p);
check('then the other', s.shown && s.title === 'ثاني', JSON.stringify(s));
p = await open('tv.html', [rem(7, 0, 30)], { bridge: false });
check('with no app (a browser, another TV) the card still shows and OK puts it away', (await state(p)).shown && (await p.keyboard.press('Enter'), !(await state(p)).shown));
p = await open('tv.html', []);
check('no reminders: the card never shows', !(await state(p)).shown);

// English
p = await open('tv-en.html', [rem(7, 0, 30)]);
s = await state(p);
check('the English page: Done and Remind me in 10 minutes', s.ok === 'Done' && s.later === 'Remind me in 10 minutes', s.ok + ' | ' + s.later);
check('the English page: «At 4:30 PM, in 30 minutes»', /^At 4:30 PM, in 30 min/.test(s.sub), s.sub);

await browser.close();
process.exit(failed ? 1 : 0);
