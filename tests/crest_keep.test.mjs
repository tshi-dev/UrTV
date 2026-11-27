// 4.4 beta 4 (users on X, 27 Nov: the club logos disappear and come back): the crest cache's cut (crestTrim in src/template.html) never throws out a club that is
// on the page while others can go, keeps at most CREST_MAX, and with few clubs keeps 60 as before. Plain node, no browser: the function is read from the source.
import fs from 'node:fs';
import vm from 'node:vm';
const src = fs.readFileSync(new URL('../src/template.html', import.meta.url), 'utf8');
const consts = src.match(/const CREST_PX = 48, CREST_KEEP = (\d+), CREST_PER_ROUND = (\d+), CREST_MAX = (\d+);/);
const fn = src.match(/function crestTrim\(map, inView\)\{[\s\S]*?\n\}/);
let failed = 0;
const check = (name, ok, d = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + d)); if (!ok) failed++; };
check('the constants and crestTrim are in the template', !!consts && !!fn);
const ctx = vm.createContext({});
vm.runInContext(`const CREST_KEEP = ${consts[1]}, CREST_PER_ROUND = ${consts[2]}, CREST_MAX = ${consts[3]};\n${fn[0]}\nglobalThis.crestTrim = crestTrim;`, ctx);
const make = n => { const m = {}; for (let i = 0; i < n; i++) m['t' + i] = {u: 'u' + i}; return m; };
let m = make(70);
ctx.crestTrim(m, new Set());
check('with no club in view, 60 are kept (as before), the oldest cut', Object.keys(m).length === 60 && !('t0' in m) && 't69' in m, Object.keys(m).length);
m = make(200);
const view = new Set(Array.from({length: 150}, (_, i) => 't' + i));
ctx.crestTrim(m, view);
check('150 clubs in view (whole leagues): all of them stay, the others go (160 kept)', Object.keys(m).length === 160 && [...view].every(k => k in m), Object.keys(m).length);
m = make(1000);
ctx.crestTrim(m, new Set(Object.keys(m)));
check('never more than CREST_MAX (400), even all in view', Object.keys(m).length === +consts[3], Object.keys(m).length);
m = make(30);
ctx.crestTrim(m, new Set(['t3']));
check('few clubs: nothing is cut', Object.keys(m).length === 30);
process.exit(failed ? 1 : 0);
