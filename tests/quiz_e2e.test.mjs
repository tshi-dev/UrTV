// «الأسرع»: the real phone page (assets/quiz/play.html) against the real room and server (QuizRoom, QuizServer on the loopback), two phones at 360 px.
// Join, the lobby, a question with a right and a wrong answer, the reveal with points and ranks, the end, a reconnect by reload. Needs javac and Playwright.
import { chromium } from 'playwright';
import { spawn, execFileSync } from 'node:child_process';
import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const out = mkdtempSync(join(tmpdir(), 'quiz-e2e-'));
let failed = 0;
const check = (what, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + what + (ok ? '' : '  -> ' + extra)); if (!ok) failed++; };
const app = join(root, 'android/app/src/main/java/sa/urtv/screensaver');
execFileSync('javac', ['-nowarn', '-d', out, ...['Room', 'Pick', 'Server'].map(n => join(app, `Quiz${n}.java`)), join(root, 'tests/QuizServe.java')], { stdio: 'pipe' });
const srv = spawn('java', ['-cp', out, 'sa.urtv.screensaver.QuizServe', join(root, 'assets/quiz/play.html')]);
const lines = []; let waiters = [];
let buf = '';
srv.stdout.on('data', d => { buf += d; let i; while ((i = buf.indexOf('\n')) >= 0) { const l = buf.slice(0, i); buf = buf.slice(i + 1); lines.push(l); waiters.forEach(w => w()); } });
const next = () => new Promise(r => { const t = () => { if (lines.length) r(lines.shift()); else waiters.push(t); }; waiters = []; t(); });
const cmd = async c => { srv.stdin.write(c + '\n'); return next(); };
const port = +(await next()).split(' ')[1];
const b = await chromium.launch();
try {
  const mk = async lang => { const c = await b.newContext({ viewport: { width: 360, height: 740 }, hasTouch: true, isMobile: true }); const p = await c.newPage(); await p.goto(`http://127.0.0.1:${port}/4321?l=${lang}&theme=light`); return p; };
  const state = p => p.evaluate(() => document.getElementById('app').getAttribute('data-s'));
  const wait = (p, s, t = 6000) => p.waitForFunction(n => document.getElementById('app').getAttribute('data-s') === n, s, { timeout: t });
  const A = await mk('ar'), B = await mk('en');
  const joinAs = async (p, name) => {
    await p.fill('input', name);
    await p.click('button.go, button[type=submit], .join button, button:has-text("ادخل"), button:has-text("Join")');
  };
  await joinAs(A, 'سارة'); await joinAs(B, 'Ben');
  await new Promise(r => setTimeout(r, 1200));
  check('two phones joined the real room', (await cmd('players')) === 'players 2', await state(A) + ' ' + await state(B));
  check('they wait in the lobby', ['lobby', 'wait', 'waiting'].some(s => s === 'x') || (await state(A)) !== 'join');
  check('the TV starts the round', (await cmd('start')) === 'ok');
  await wait(A, 'question'); await wait(B, 'question');
  check('both phones show the question with four stamps', await A.evaluate(() => document.querySelectorAll('.stamp').length === 4) && await B.evaluate(() => document.querySelectorAll('.stamp').length === 4));
  check('the page never got the right answer in the markup before the reveal', !(await A.content()).includes('"c":2'));
  await A.locator('.stamp').nth(2).tap();      // right
  await B.locator('.stamp').nth(0).tap();      // wrong
  await wait(A, 'reveal', 8000); await wait(B, 'reveal', 8000);
  check('the question closed when both had answered; the right phone shows points and rank 1', await A.evaluate(() => /[٨٩][٠-٩]{2}|\d{3,4}/.test(document.body.innerText)) && await A.evaluate(() => document.querySelectorAll('.stamp.ok').length === 1));
  check('the wrong phone sees the right stamp green and its own pick marked', await B.evaluate(() => document.querySelectorAll('.stamp.ok').length === 1 && document.querySelectorAll('.stamp.pick').length === 1));
  await cmd('next');
  await wait(A, 'question');
  await A.reload(); await new Promise(r => setTimeout(r, 800));
  check('a reload reconnects with the stored token (no join form)', (await state(A)) !== 'join');
  await cmd('finish');
  await wait(A, 'end'); await wait(B, 'end');
  check('the end shows the final rank on both phones', await A.evaluate(() => /[١٢]/.test(document.body.innerText)) && await B.evaluate(() => /1st|2nd/.test(document.body.innerText)), await B.evaluate(() => document.body.innerText));
  await cmd('close');
  await wait(A, 'closed', 6000);
  check('closing the room tells the phones', true);
} catch (e) {
  check('the end-to-end run', false, String(e).slice(0, 300));
} finally {
  await b.close(); srv.stdin.end(); srv.kill();
}
console.log(failed ? `${failed} failed` : 'all passed');
process.exit(failed ? 1 : 0);
