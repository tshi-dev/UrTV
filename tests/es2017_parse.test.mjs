// #19: some Android 9 TVs are stuck on WebView 66 (Chrome 66), and a page script it cannot parse never starts: the
// screensaver stays a blank layout. Every <script> of the pages the apps load (dist/tv.html, dist/tv-en.html) must parse
// as ES2017: no `?.` / `??` (Chrome 80), no class fields, no object spread, no regex lookbehind. tests/check.py looks for
// `?.` and `??` in the template; this reads the built pages with a real parser (acorn), so nothing newer slips in.
//
//   node tests/es2017_parse.test.mjs      (acorn: `npm install --no-save acorn@8.14.0`, or a global eslint's)
import { readFileSync, existsSync } from 'node:fs';
import { createRequire } from 'node:module';
import { execSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => {
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail));
  if (!ok) failed++;
};

function findAcorn(){   // the repository's own node_modules first, then a global install (or a global eslint's, which has it)
  const from = [path.join(ROOT, 'package.json')];
  try{ from.push(path.join(execSync('npm root -g', { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim(), 'x')); }catch(e){}
  for(const r of from.slice()){
    for(const host of ['eslint', 'espree']){ try{ from.push(createRequire(r).resolve(host)); }catch(e){} }
  }
  for(const r of from){ try{ return createRequire(r)('acorn'); }catch(e){} }
  return null;
}
const acorn = findAcorn();
if(!acorn){
  if(process.env.CI){ console.log('FAIL no acorn: npm install --no-save acorn@8.14.0'); process.exit(1); }
  console.log('SKIP no acorn on this machine (npm install --no-save acorn@8.14.0)');
  process.exit(0);
}

const scripts = html => [...html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/g)]
  .filter(m => !/type\s*=\s*["']?(application\/(ld\+)?json|text\/template)/i.test(m[1])).map(m => m[2]);
const where = (src, e) => {
  const l = src.split('\n')[e.loc.line - 1] || '';
  return `${e.message}: …${l.slice(Math.max(0, e.loc.column - 50), e.loc.column + 30)}…`;
};

for(const f of ['dist/tv.html', 'dist/tv-en.html']){
  const p = path.join(ROOT, f);
  check(`${f} is built`, existsSync(p), 'run python3 src/build.py');
  if(!existsSync(p)) continue;
  const list = scripts(readFileSync(p, 'utf8'));
  check(`${f}: has its script`, list.length > 0);
  list.forEach((src, k) => {
    let err = '';
    try{ acorn.parse(src, { ecmaVersion: 2017, sourceType: 'script' }); }catch(e){ err = where(src, e); }
    check(`${f}: script ${k + 1} parses as ES2017 (WebView 66 runs it)`, !err, err);
  });
}

// the parser itself refuses what WebView 66 cannot run (so a PASS above means something)
for(const [what, code] of [['?.', 'a?.b'], ['??', 'a ?? b'], ['object spread', 'x = {...a}'], ['lookbehind', '/(?<!a)b/'],
                           ['class field', 'class A { x = 1 }']]){
  let refused = false;
  try{ acorn.parse(code, { ecmaVersion: 2017 }); }catch(e){ refused = true; }
  check(`the ES2017 parser refuses ${what}`, refused);
}

console.log(failed ? `${failed} failed` : 'all passed');
process.exit(failed ? 1 : 0);
