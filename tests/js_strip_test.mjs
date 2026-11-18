// The built pages ship without the script's comments (src/build.py strip_js_comments, about 50 KB: the template keeps them). This proves
// nothing the page does changed: the pages are built again with the comments kept (URTV_JS_COMMENTS=keep, into a scratch folder), and each
// script is parsed with acorn before and after: the two trees must be the same, node for node (only the positions differ). It also checks
// that no comment is left and that dist/ is that build.
//   node tests/js_strip_test.mjs      (acorn: `npm install --no-save acorn@8.14.0`, or a global eslint's)
import { readFileSync, mkdtempSync, rmSync } from 'node:fs';
import { createRequire } from 'node:module';
import { execSync, execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { tmpdir } from 'node:os';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };

function findAcorn(){
  const from = [path.join(ROOT, 'package.json')];
  try{ from.push(path.join(execSync('npm root -g', { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim(), 'x')); }catch(e){}
  for(const r of from.slice()) for(const host of ['eslint', 'espree']){ try{ from.push(createRequire(r).resolve(host)); }catch(e){} }
  for(const r of from){ try{ return createRequire(r)('acorn'); }catch(e){} }
  return null;
}
const acorn = findAcorn();
if(!acorn){
  if(process.env.CI){ console.log('FAIL no acorn: npm install --no-save acorn@8.14.0'); process.exit(1); }
  console.log('SKIP no acorn on this machine (npm install --no-save acorn@8.14.0)');
  process.exit(0);
}

const script = html => (html.match(/<script>([\s\S]*?)<\/script>/) || [])[1] || '';
const POS = new Set(['start', 'end', 'loc', 'range']);
const tree = src => JSON.stringify(acorn.parse(src, { ecmaVersion: 2020, sourceType: 'script' }), (k, v) => POS.has(k) ? undefined : v);
const stamp = h => h.replace(/(<meta name="urtv-build" content=")\d{14}/, '$1');

const tmp = mkdtempSync(path.join(tmpdir(), 'urtv-js-'));
try{
  execFileSync('python3', [path.join(ROOT, 'src/build.py')], { env: { ...process.env, URTV_DIST: tmp, URTV_JS_COMMENTS: 'keep' }, stdio: 'ignore' });
  for(const f of ['tv.html', 'tv-en.html']){
    const kept = readFileSync(path.join(tmp, f), 'utf8'), shipped = readFileSync(path.join(ROOT, 'dist', f), 'utf8');
    const a = script(kept), b = script(shipped);
    let same = false, err = '';
    try{ same = tree(a) === tree(b); }catch(e){ err = e.message; }
    check(`${f}: the script without its comments is the same program (acorn trees equal)`, same, err || 'the trees differ');
    let comments = 0;
    try{ acorn.parse(b, { ecmaVersion: 2020, onComment: () => comments++ }); }catch(e){}
    check(`${f}: no comment is left in the shipped script`, comments === 0, comments);
    check(`${f}: the comments were most of the saving (at least 40 KB smaller)`, Buffer.byteLength(a) - Buffer.byteLength(b) > 40000, Buffer.byteLength(a) - Buffer.byteLength(b));
    check(`${f}: only the script changed (the rest of the page is the same build)`, stamp(kept.replace(a, '')) === stamp(shipped.replace(b, '')));
  }
}finally{ rmSync(tmp, { recursive: true, force: true }); }
console.log(failed ? `${failed} failed` : 'all passed');
process.exit(failed ? 1 : 0);
