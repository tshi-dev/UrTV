// The party games' phone page (assets/quiz/game.html) in a real browser against the REAL room and server (tests/PhoneRoomTest.java `serve`, on the loopback):
// join, the lobby, a secret card seen by its own phone only, a vote, an actor's buttons.
//
//   NODE_PATH=$(npm root -g) CHROMIUM=/opt/pw-browsers/chromium node tests/game_phone_page.test.mjs
import { chromium } from 'playwright';
import { spawn, execFileSync } from 'node:child_process';
import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const APP = path.join(ROOT, 'android/app/src/main/java/sa/urtv/screensaver');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };

const out = mkdtempSync(path.join(tmpdir(), 'gamephone-'));
execFileSync('javac', ['-nowarn', '-encoding', 'UTF-8', '-d', out, ...['PhoneRoom', 'GamePhoneRoom', 'QuizRoom', 'QuizServer', 'DigitsText'].map(n => path.join(APP, n + '.java')), path.join(ROOT, 'tests/PhoneRoomTest.java')], { stdio: 'pipe' });
const jv = spawn('java', ['-cp', out, 'sa.urtv.screensaver.PhoneRoomTest', 'serve', path.join(ROOT, 'assets/quiz/game.html')]);
let buf = '';
const waiters = [];
jv.stdout.on('data', d => { buf += d; let i; while ((i = buf.indexOf('\n')) >= 0) { const l = buf.slice(0, i); buf = buf.slice(i + 1); const w = waiters.shift(); if (w) w(l); } });
const nextLine = () => new Promise(r => waiters.push(r));
const send = l => jv.stdin.write(l + '\n');
const ask = async l => { const p = nextLine(); send(l); return p; };
const port = (await nextLine()).split(' ')[1];

const browser = await chromium.launch({ ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}), args: ['--no-proxy-server'] });   // the loopback is never behind a proxy
const url = `http://127.0.0.1:${port}/4321?l=ar`;
const phone = async (name) => {
  const ctx = await browser.newContext({ viewport: { width: 390, height: 780 } });
  const pg = await ctx.newPage();
  const errs = [];
  pg.on('pageerror', e => errs.push(e.message));
  await pg.goto(url);
  await pg.fill('#nm', name);
  await pg.click('#go');
  await pg.waitForSelector('[data-s="lobby"]', { timeout: 5000 });
  return { pg, errs };
};
try {
  const a = await phone('Sara'), b = await phone('Omar');
  check('join: two phones reach the lobby and are two seats', (await ask('count')) === 'COUNT 2');
  await a.pg.waitForFunction(() => document.querySelector('.app').textContent.includes('٢'), null, { timeout: 5000 });
  check('lobby: the phone says it is in, with the number of players', true);
  send('start'); send('secret 0 وردة'); send('wait 1 انتظر');
  await a.pg.waitForSelector('[data-s="card-secret"]', { timeout: 5000 });
  check('secret: the first phone shows its word', (await a.pg.textContent('.secret')) === 'وردة');
  await b.pg.waitForSelector('[data-s="card-wait"]', { timeout: 5000 });
  check('secret: the other phone never gets the word', !(await b.pg.textContent('.app')).includes('وردة'));
  await a.pg.click('#seen');
  await a.pg.waitForSelector('.msg', { timeout: 5000 });
  check('secret: «شفتها» reaches the TV as answer 0', (await ask('reply 0')) === 'REPLY 0');
  send('vote 0'); send('vote 1');
  await a.pg.waitForSelector('.vb', { timeout: 5000 });
  const dis = await a.pg.$$eval('.vb', xs => xs.map(x => x.disabled));
  check('vote: the names and «ما أدري»; your own name cannot be picked', dis.length === 3 && dis[0] === true && dis[1] === false && dis[2] === false, JSON.stringify(dis));
  await a.pg.click('.vb[data-i="1"]');
  await a.pg.waitForSelector('.vb.pick', { timeout: 5000 });
  check('vote: the tap is the TV\'s answer and the choice stays marked', (await ask('reply 0')) === 'REPLY 1');
  send('act 0 قلم');
  await a.pg.waitForSelector('[data-s="card-act"]', { timeout: 5000 });
  check('act: the word and the three buttons', (await a.pg.textContent('.actw')) === 'قلم' && (await a.pg.$$('button[data-a]')).length === 3);
  await a.pg.click('button[data-a="0"]');
  await a.pg.click('button[data-a="1"]');
  await a.pg.waitForTimeout(400);
  check('act: صح then تخطّي arrive in order', (await ask('take 0')) === 'TAKE [0, 1]');
  check('no script error on either phone', a.errs.length === 0 && b.errs.length === 0, a.errs.concat(b.errs).join('|'));
  const html = await a.pg.content();
  check('the page loads nothing from outside and builds text with text nodes only', !/https?:\/\/(?!127)/.test(html.replace(/http:\/\/www\.w3\.org\/2000\/svg/g, '')) && !/innerHTML|eval\(/.test(html));
} catch (e) { check('the page test ran to the end', false, e.message.split('\n')[0]); }
await browser.close();
jv.stdin.end();
jv.kill();
process.exit(failed ? 1 : 0);
