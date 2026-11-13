// «صفحة النادي» (4.3.1, mockup only, no app code): OK on a team (from a match sheet or «فرقك») opens its page: «المواجهات السابقة» against the NEXT opponent (last 5 meetings),
// «آخر المباريات» (last 5 results with a ف/ت/خ mark) and «القادمة» (next 3). Drawn in the matches page's own look: its real CSS (dist/tv.html of the repository), its dialog (.mdx), its rows (.mdr).
//   node design/431-club/club.mjs <repo-root> <out-dir>      (the real fonts are private: DejaVu Sans stands in)
import { chromium } from 'playwright';
import path from 'node:path';

const [root, out] = [process.argv[2], process.argv[3]];
const mark = (k) => ({ w: ['ف', '#1B8354', '#E3F3EA'], d: ['ت', '#8A5600', '#FBEFD9'], l: ['خ', '#B3261E', '#FBE3E1'] })[k];
const chip = (k) => { const [t, c, b] = mark(k); return `<em style="font-style:normal;display:inline-flex;align-items:center;justify-content:center;width:calc(var(--u)*44);height:calc(var(--u)*44);border-radius:calc(var(--u)*12);background:${b};color:${c};font-weight:700;font-size:calc(var(--u)*26)">${t}</em>`; };
const row = (left, mid, score, right) => `<li style="padding:calc(var(--u)*14) calc(var(--u)*24)"><span style="min-width:calc(var(--u)*190);font-weight:400;color:var(--fs);font-size:calc(var(--u)*24)">${left}</span><span style="flex:1;font-weight:700">${mid}</span><span style="font-weight:700;direction:ltr">${score}</span><i style="min-width:calc(var(--u)*120);text-align:end">${right || ''}</i></li>`;
const sect = (title, rows, note = '') => `<div><h3 style="margin:0 0 calc(var(--u)*10);font-size:calc(var(--u)*28);color:var(--fs);font-weight:700">${title}${note ? ` <small style="font-weight:400">${note}</small>` : ''}</h3><ul class="mdr" style="font-size:calc(var(--u)*26)">${rows.join('')}</ul></div>`;
const cols = (a, b) => `<div style="display:grid;grid-template-columns:1fr 1fr;gap:calc(var(--u)*32);margin-top:calc(var(--u)*20)">${a}${b}</div>`;
const card = (when, teams) => `<div style="background:var(--fbg);border:1px solid var(--fln);border-radius:calc(var(--u)*20);padding:calc(var(--u)*14) calc(var(--u)*24)"><div style="color:var(--fs);font-size:calc(var(--u)*24)">${when}</div><b style="font-size:calc(var(--u)*28)">${teams}</b></div>`;
const nextRow = (a, b, c) => `<h3 style="margin:calc(var(--u)*20) 0 calc(var(--u)*10);font-size:calc(var(--u)*28);color:var(--fs);font-weight:700">القادمة</h3><div style="display:grid;grid-template-columns:repeat(3,1fr);gap:calc(var(--u)*20)">${a}${b}${c}</div>`;
const html = `<div class="mdx" style="width:calc(var(--u)*1728);padding:calc(var(--u)*32) calc(var(--u)*48)">
  <div class="mdh"><span>الدوري السعودي، الجولة ١٣</span><span>الأربعاء</span></div>
  <div style="display:flex;align-items:center;gap:calc(var(--u)*24);margin:calc(var(--u)*16) 0 calc(var(--u)*8)"><span class="mdc" style="background:#D8E7DF;border-radius:50%;display:inline-block"></span><b style="font-size:calc(var(--u)*48)">الهلال</b><span style="color:var(--fs);font-size:calc(var(--u)*26)">يلعب بعد ١٢ ساعة ضد الأهلي</span></div>
  ${cols(sect('المواجهات السابقة مع الأهلي', [row('٢٠٢٦/٠٣/١٤', 'دوري روشن', '٢ - ١', chip('w')), row('٢٠٢٥/١١/٢٢', 'دوري روشن', '١ - ١', chip('d')), row('٢٠٢٥/٠٥/٠٣', 'كأس الملك', '٠ - ١', chip('l')), row('٢٠٢٥/٠١/١٩', 'دوري روشن', '٣ - ٠', chip('w')), row('٢٠٢٤/٠٩/٢٨', 'دوري روشن', '٢ - ٢', chip('d'))], 'آخر ٥ لقاءات'),
    sect('آخر المباريات', [row('الأحد ٣٠ أكتوبر', 'الهلال × الشباب', '٢ - ١', chip('w')), row('الأربعاء ٢٦ أكتوبر', 'الاتفاق × الهلال', '٠ - ٠', chip('d')), row('الأحد ٢٣ أكتوبر', 'الهلال × الرياض', '٤ - ٠', chip('w')), row('الخميس ٢٠ أكتوبر', 'النصر × الهلال', '٢ - ١', chip('l')), row('الأحد ١٦ أكتوبر', 'الهلال × الفتح', '١ - ٠', chip('w'))]))}
  ${nextRow(card('الأربعاء، ٨:٠٠ م', 'الهلال × الأهلي'), card('الأحد ٩ نوفمبر', 'الخليج × الهلال'), card('الأربعاء ١٢ نوفمبر', 'الهلال × الاتحاد'))}
  <div class="mdq" style="margin-top:calc(var(--u)*20)"><button class="f" tabindex="-1"><span>رجوع</span></button></div>
</div>`;
const none = `<div class="mdx" style="width:calc(var(--u)*1728);padding:calc(var(--u)*32) calc(var(--u)*48)">
  <div class="mdh"><span>الدوري السعودي، الجولة ١٣</span><span>الأربعاء</span></div>
  <div style="display:flex;align-items:center;gap:calc(var(--u)*24);margin:calc(var(--u)*16) 0 calc(var(--u)*8)"><span class="mdc" style="background:#D8E7DF;border-radius:50%;display:inline-block"></span><b style="font-size:calc(var(--u)*48)">الاتفاق</b></div>
  ${cols(sect('المواجهات السابقة مع الشباب', [`<li style="justify-content:center;color:var(--fs);padding:calc(var(--u)*40)">ما لقينا مواجهات سابقة</li>`]),
    sect('آخر المباريات', [row('الأحد ٣٠ أكتوبر', 'الاتفاق × الفتح', '١ - ٠', chip('w')), row('الأربعاء ٢٦ أكتوبر', 'الاتفاق × الهلال', '٠ - ٠', chip('d'))]))}
  ${nextRow(card('الجمعة، ٩:٠٠ م', 'الاتفاق × الشباب'), '', '')}
  <div class="mdq" style="margin-top:calc(var(--u)*20)"><button class="f" tabindex="-1"><span>رجوع</span></button></div>
</div>`;
const browser = await chromium.launch();
for (const [name, dark, body] of [['club-01-light', false, html], ['club-02-dark-no-meetings', true, none]]) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, timezoneId: 'Asia/Riyadh', deviceScaleFactor: 1 });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  await p.goto('file://' + path.join(root, 'dist/tv.html') + '#matchpage=1&teams=136013&gap=0' + (dark ? '&pagedark=1' : ''));
  await p.waitForTimeout(800);
  await p.evaluate(h => { const d = document.getElementById('mp-detail'); d.innerHTML = h; d.hidden = false; }, body);
  await p.waitForTimeout(300);
  await p.screenshot({ path: path.join(out, name + '.png') });
  await ctx.close();
}
await browser.close();
