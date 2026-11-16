// apps-script/sync.gs under node, with stand-ins for the Google services it uses (the sheet, Script Properties, the lock, UrlFetch,
// Utilities, Logger) and a fake GitHub contents API. Made-up rows only.  node tests/sync_gs.test.mjs
import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';

const SRC = fs.readFileSync(new URL('../apps-script/sync.gs', import.meta.url), 'utf8');
const MANIFEST = JSON.parse(fs.readFileSync(new URL('../apps-script/appsscript.json', import.meta.url), 'utf8'));
let failed = 0;
const check = (name, fn) => { try { fn(); console.log('PASS ' + name); } catch (e) { failed++; console.log('FAIL ' + name + '\n  ' + e.message); } };

function world({ rows, props = {}, files = {}, failPut = false }) {
  const log = [], requests = [];
  const sheet = {
    getLastRow: () => rows.length,
    getRange: (r, c, n, w) => ({ getValues: () => rows.slice(r - 1, r - 1 + n).map(x => x.slice(c - 1, c - 1 + w)) }),
  };
  const store = { ...props };
  const ctx = {
    SpreadsheetApp: { getActiveSpreadsheet: () => ({ getSheets: () => [sheet], getSheetByName: n => (n === 'Form Responses 1' ? sheet : null) }) },
    PropertiesService: { getScriptProperties: () => ({ getProperty: k => (k in store ? store[k] : null), setProperty: (k, v) => { store[k] = v; } }) },
    LockService: { getScriptLock: () => ({ tryLock: () => true, releaseLock: () => {} }) },
    Logger: { log: s => log.push(String(s)) },
    Utilities: {
      formatDate: () => '2026-11-16',
      base64Encode: s => Buffer.from(s, 'utf8').toString('base64'),
      base64Decode: s => [...Buffer.from(s, 'base64')],
      newBlob: bytes => ({ getDataAsString: () => Buffer.from(bytes).toString('utf8') }),
      Charset: { UTF_8: 'UTF-8' },
    },
    UrlFetchApp: {
      fetch: (url, o) => {
        requests.push({ url, ...o });
        const path = url.replace(/^https:\/\/api\.github\.com\/repos\/tshi-dev\/urtv-reports\/contents\//, '').replace(/\?.*$/, '');
        const reply = (code, body) => ({ getResponseCode: () => code, getContentText: () => JSON.stringify(body) });
        if (o.method === 'get') return path in files ? reply(200, { sha: 'sha-' + path, content: Buffer.from(files[path]).toString('base64') }) : reply(404, {});
        if (failPut) return reply(500, { message: 'down' });
        const b = JSON.parse(o.payload);
        if ((path in files) !== Boolean(b.sha)) return reply(409, { message: 'sha' });
        files[path] = Buffer.from(b.content, 'base64').toString('utf8');
        return reply(path in files ? 200 : 201, {});
      },
    },
  };
  vm.createContext(ctx);
  vm.runInContext(SRC, ctx);
  return { ctx, log, requests, store, files };
}

const ROWS = [
  ['Timestamp', 'Report'],
  [new Date('2026-11-15T18:00:00Z'), 'UrTV setup report, 2026-11-15, UrTV 4.3.2\n\nTV: TCL X (g07), Android 11 (SDK 30), firmware F'],
  [new Date('2026-11-15T19:00:00Z'), 'He said "ok", then left'],
];

check('the manifest asks for the spreadsheet (this one only) and outside requests, nothing else', () => {
  assert.deepEqual(MANIFEST.oauthScopes.sort(), ['https://www.googleapis.com/auth/script.external_request', 'https://www.googleapis.com/auth/spreadsheets.currentonly']);
  assert.match(SRC, /@OnlyCurrentDoc/);
  for (const svc of ['DriveApp', 'GmailApp', 'ScriptApp', 'MailApp', 'CalendarApp', 'DocumentApp', 'FormApp']) assert.ok(!SRC.includes(svc + '.'), svc);
});

check('no token in the file; the token is read from Script Properties and never logged', () => {
  assert.ok(!/gh[pousr]_[A-Za-z0-9]{20,}|github_pat_/.test(SRC));
  const w = world({ rows: ROWS, props: { GITHUB_TOKEN: 'secret-token' } });
  w.ctx.syncReports();
  assert.ok(w.log.every(l => !l.includes('secret-token')));
  assert.equal(w.requests[0].headers.Authorization, 'Bearer secret-token');
});

check('dryRun: logs what would go, sends nothing, changes nothing, needs no token', () => {
  const w = world({ rows: ROWS });
  const r = w.ctx.dryRun();
  assert.equal(r.rows, 2);
  assert.equal(w.requests.length, 0);
  assert.equal(w.store.LAST_ROW, undefined);
  assert.ok(w.log.some(l => l.includes('DRY RUN') && l.includes('reports/2026-11-16.csv')));
  assert.ok(w.log.some(l => l.includes('GITHUB_TOKEN set: NO')));
});

check('a run: the new rows as reports/<day>.csv, quoted, then LAST_ROW moves', () => {
  const w = world({ rows: ROWS, props: { GITHUB_TOKEN: 't' } });
  w.ctx.syncReports();
  assert.equal(w.files['reports/2026-11-16.csv'],
    'row,timestamp,report\n"2","2026-11-15T18:00:00.000Z","UrTV setup report, 2026-11-15, UrTV 4.3.2\n\nTV: TCL X (g07), Android 11 (SDK 30), firmware F"\n'
    + '"3","2026-11-15T19:00:00.000Z","He said ""ok"", then left"\n');
  assert.equal(w.store.LAST_ROW, '3');
  const put = w.requests.find(q => q.method === 'put');
  assert.equal(JSON.parse(put.payload).branch, 'main');
});

check('nothing new: no request at all', () => {
  const w = world({ rows: ROWS, props: { GITHUB_TOKEN: 't', LAST_ROW: '3' } });
  assert.equal(w.ctx.syncReports().rows, 0);
  assert.equal(w.requests.length, 0);
});

check('a second run the same day adds to that day\'s file (with its sha)', () => {
  const w = world({ rows: [...ROWS, [new Date('2026-11-16T01:00:00Z'), 'third']], props: { GITHUB_TOKEN: 't', LAST_ROW: '3' },
    files: { 'reports/2026-11-16.csv': 'row,timestamp,report\n"2","x","y"' } });
  w.ctx.syncReports();
  assert.equal(w.files['reports/2026-11-16.csv'], 'row,timestamp,report\n"2","x","y"\n"4","2026-11-16T01:00:00.000Z","third"\n');
  assert.equal(JSON.parse(w.requests.find(q => q.method === 'put').payload).sha, 'sha-reports/2026-11-16.csv');
  assert.equal(w.store.LAST_ROW, '4');
});

check('GitHub fails: an error, and LAST_ROW stays (the rows go the next night)', () => {
  const w = world({ rows: ROWS, props: { GITHUB_TOKEN: 't', LAST_ROW: '1' }, failPut: true });
  assert.throws(() => w.ctx.syncReports(), /GitHub answered 500/);
  assert.equal(w.store.LAST_ROW, '1');
});

check('no token: an error before any request', () => {
  const w = world({ rows: ROWS });
  assert.throws(() => w.ctx.syncReports(), /No GITHUB_TOKEN/);
  assert.equal(w.requests.length, 0);
});

check('SHEET_NAME picks the responses sheet by name', () => {
  const w = world({ rows: ROWS, props: { SHEET_NAME: 'Form Responses 1' } });
  assert.equal(w.ctx.dryRun().rows, 2);
  const bad = world({ rows: ROWS, props: { SHEET_NAME: 'nope' } });
  assert.throws(() => bad.ctx.dryRun(), /No sheet named nope/);
});

console.log(failed ? `\n${failed} failed` : '\nall passed');
process.exit(failed ? 1 : 0);
