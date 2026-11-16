// The places a card or a notice is seen (shared by shoot.mjs and kickoff.mjs): the screensaver and the home of both looks, drawn approximately.
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';

export const HERE = path.dirname(fileURLToPath(import.meta.url)), ROOT = path.resolve(HERE, '../../../..'), OUT = path.resolve(HERE, '..');
export const FONT = process.env.FONT_DIR;
if (!FONT) { console.error('FONT_DIR is needed'); process.exit(1); }
export const css = fs.readFileSync(path.join(HERE, 'context.css'), 'utf8').replaceAll('url(FONT/', `url(file://${FONT}/`);
const BG = { tv: 'f5ZB0LHwi0s.jpg', couch: 'LAj90eAXOZA.jpg' };
export const bg = k => `<div class="bg" style="background-image:url(file://${path.join(ROOT, 'assets/backgrounds', BG[k])})"></div><div class="shade"></div>`;
const TRAY = [['يوتيوب', '#E52D27'], ['شاهد', '#1E8A5F'], ['نتفليكس', '#141414', '#E50914'], ['برايم فيديو', '#1A73E8'], ['ثمانية', '#2A2A2A', '#EBC77F'], ['تلاوة وأذان', '#221A10', '#E9C46A'], ['المتصفح', '#3A3D42']]
  .map(([t, b, c]) => `<span style="background:${b};color:${c || '#fff'}">${t}</span>`).join('');

export const PLACES = {
  // شكل تلفزيونك, the screensaver «معلومات»: the card where the card beside the clock is; «مباراة اليوم» takes the whole middle (wide)
  's-tv': (c, wide) => `<div class="s-tv">${bg('tv')}<div class="date"><b>الخميس، ١١ جمادى الأولى ١٤٤٨ هـ</b><i>الخميس، ٢٢ أكتوبر ٢٠٢٦</i></div>
    <div class="wx"><b>٣٣° صافٍ غالبًا</b><i>الرياض، ٣٦° / ٢٢°</i></div>
    ${wide ? '<div class="clock top">٥:٠٠<small>م</small></div>' : '<div class="clock">٥:٠٠<small>م</small></div><div class="zone">بتوقيت الرياض</div>'}
    <div class="card${wide ? ' wide' : ''}">${c.tv}</div>
    <div class="bar"><div>الصلاة القادمة<b>المغرب ٥:٢١ م</b><i>بعد ٢١ دقيقة</i></div><div>رمضان ١٤٤٨<b>١٠٩ أيام</b><i>الاثنين ٨ فبراير</i></div><div>الطقس<b>٣٣°</b><i>الرياض</i></div></div></div>`,
  // شكل تلفزيونك, the home: the card at 48,84 dp (461x250), the clock on the right, the apps under
  'h-tv': c => `<div class="h-tv">${bg('tv')}<div class="pills"><span></span><span></span><span></span><span></span></div><div class="clock">٥:٠٠<small>م</small></div>
    <div class="greet"><b>مساء الخير</b>الخميس، ١١ جمادى الأولى ١٤٤٨ هـ<br>الرياض ٣٣°</div><div class="card">${c.tv}</div><div class="tray">${TRAY}</div></div>`,
  // شكل كنبة, the home: the three small cards in a row; the sports card is the middle one
  'h-couch': c => `<div class="h-couch">${bg('couch')}<div class="greet"><b>مساء الخير</b><i>سماء صافية في الرياض، ٣٣°</i></div><div class="date">الخميس ١١ جمادى الأولى ١٤٤٨ هـ، ٢٢ أكتوبر</div>
    <div class="clock">٥:٠٠</div><div class="row"><div class="mini"><i>الصلاة القادمة</i><b>المغرب ٥:٢١ م</b><em>بعد ٢١ دقيقة</em></div>
    <div class="mini sport"><i>${c.couch[0]}</i><b>${c.couch[1]}</b><em>${c.couch[2]}</em></div><div class="mini"><i>أكمل المشاهدة</i><b>وثائقي البحر الأحمر</b><em>الحلقة ٣</em></div></div>
    <div class="tray">${TRAY}</div></div>`,
  // شكل كنبة, the screensaver «نظيفة»: the glass clock, the date, and the card's one line in the bar
  's-couch': c => `<div class="s-couch">${bg('couch')}<div class="clock">٥:٠٠</div><div class="date">الخميس، ١١ جمادى الأولى ١٤٤٨ هـ، ٢٢ أكتوبر ٢٠٢٦</div>
    <div class="next">المغرب ٥:٢١ م، بعد ٢١ دقيقة</div><div class="sbar"><i>${c.bar[0]}</i><b>${c.bar[1]}</b><em>${c.bar[2]}</em></div></div>`,
};


// one page drawn to OUT/<name>.png at 960x540 dp, 2x (a file page, so the file: photos and fonts load)
export async function shooter(page){
  return async (name, body, extraCss = '') => {
    const tmp = path.join(process.env.TMPDIR || '/tmp', 'sports-mockup.html');
    fs.writeFileSync(tmp, `<!doctype html><html lang="ar"><head><meta charset="utf-8"><style>${css}${extraCss}</style></head><body>${body}</body></html>`);
    await page.goto('file://' + tmp, { waitUntil: 'load' });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(150);
    await page.screenshot({ path: path.join(OUT, name + '.png') });
    console.log('wrote', name + '.png');
  };
}
