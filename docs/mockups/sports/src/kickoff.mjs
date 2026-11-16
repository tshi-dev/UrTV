// «تنبيه بداية المباراة» (mockups only): the corner notice at kick-off over the home, the screensaver and another app's video, a goal and the
// final whistle (off by default), and the settings rows in both settings layouts.   FONT_DIR=… node docs/mockups/sports/src/kickoff.mjs
import { chromium } from 'playwright';
import { CARDS } from './cards.mjs';
import { PLACES, shooter } from './places.mjs';

const C = { hilal: ['الهلال', '#1E4FA1', '#FFFFFF'], nassr: ['النصر', '#F7C600', '#1B3A8A'] };
const sq = k => `<i class="kq" style="--a:${C[k][1]};--b:${C[k][2]}"></i>`;
const WHISTLE = '<svg viewBox="0 0 24 24"><path d="M3 10.5a5.5 5.5 0 1 0 11 0V9h6.5l1-3H8.5A5.5 5.5 0 0 0 3 10.5z"/><circle cx="8.5" cy="10.5" r="1.6"/></svg>';
const BALL = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5l3.5 2.5-1.3 4h-4.4L8.5 10z"/></svg>';
// the notice: 340x82 dp, 24 dp from the top and the right; slides in from the right, stays 8 s, slides back
const notice = (kind = 'start') => {
  const top = kind === 'goal' ? `${BALL}<span>هدف، الدقيقة ٦٧</span>` : kind === 'end' ? `${WHISTLE}<span>انتهت المباراة</span>` : `${WHISTLE}<span>بدأت المباراة</span>`;
  const mid = kind === 'start' ? '<b class="kx">×</b>' : `<b class="ks">${kind === 'goal' ? '٢ - ١' : '٣ - ١'}</b>`;
  return `<div class="kick${kind === 'goal' ? ' goal' : ''}"><div class="kt">${top}<em>دوري روشن، الجولة ١١</em></div>
    <div class="km"><span>${sq('hilal')}<b>${C.hilal[0]}</b></span>${mid}<span><b>${C.nassr[0]}</b>${sq('nassr')}</span></div><div class="kbar"></div></div>`;
};
const KCSS = `
.kick{position:fixed;top:24px;right:24px;width:340px;border-radius:14px;background:linear-gradient(rgba(255,255,255,.10),rgba(255,255,255,.10)),rgba(10,12,16,.78);
  border:1px solid rgba(233,196,106,.55);box-shadow:0 12px 32px rgba(0,0,0,.45);padding:10px 14px 0;overflow:hidden;z-index:9;color:#fff;text-align:center}
.kt{display:flex;align-items:center;gap:6px;font-size:11px;color:var(--gold-3);font-weight:500}
.kt svg{width:15px;height:15px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linejoin:round}
.kt em{font-style:normal;color:var(--sub);font-weight:400;margin-right:auto}
.km{display:flex;align-items:center;justify-content:center;gap:16px;margin:8px 0 10px}
.km span{display:flex;align-items:center;gap:7px}.km b{font-size:20px;font-weight:700}
.kx{color:var(--sub);font-size:16px!important}.ks{font-size:22px!important}
.kq{display:inline-block;width:12px;height:12px;border-radius:3px;background:linear-gradient(90deg,var(--a) 50%,var(--b) 50%)}
.kbar{height:3px;margin:0 -14px;background:linear-gradient(90deg,rgba(255,255,255,.12) 38%,var(--gold) 38%)}
.kick.goal{border-color:#3BB273}
.video{position:absolute;inset:0;background:radial-gradient(120% 90% at 30% 40%,#2C6E49,#123524 60%,#081A10)}
.video .pitch{position:absolute;left:8%;right:8%;top:22%;bottom:18%;border:2px solid rgba(255,255,255,.35)}
.video .pitch:before{content:"";position:absolute;left:50%;top:0;bottom:0;border-left:2px solid rgba(255,255,255,.35)}
.video .pitch:after{content:"";position:absolute;left:50%;top:50%;width:90px;height:90px;margin:-45px;border:2px solid rgba(255,255,255,.35);border-radius:50%}
.video .tag{position:absolute;left:24px;top:22px;font:700 12px Plex;color:rgba(255,255,255,.6);direction:ltr}
.video .ctl{position:absolute;left:40px;right:40px;bottom:30px;height:4px;border-radius:2px;background:rgba(255,255,255,.25)}
.video .ctl:before{content:"";position:absolute;left:0;top:0;bottom:0;width:42%;background:#E52D27;border-radius:2px}
.video .note{position:absolute;left:0;right:0;bottom:46px;text-align:center;font-size:11px;color:rgba(255,255,255,.55)}
/* settings */
.st{position:absolute;inset:0;background:radial-gradient(80% 70% at 70% 30%,#2B2730,#121216)}
.st h1{position:absolute;right:58px;top:42px;font-size:34px;font-weight:700}
.side{position:absolute;right:48px;top:92px;width:270px}
.side .q{border-radius:10px;background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.1);padding:9px 16px;font-size:15px;color:#ddd;margin-bottom:14px}
.side li{list-style:none;display:flex;align-items:center;gap:12px;justify-content:flex-start;font-size:15px;padding:5px 16px;border-radius:10px}
.side li i{width:22px;height:22px;border-radius:50%;background:rgba(255,255,255,.1);display:inline-block}
.side li.on{background:rgba(255,255,255,.1)}
.pane{position:absolute;right:347px;left:48px;top:40px}
.pane h2{font-size:24px;font-weight:700}.pane p{font-size:12px;color:var(--sub);margin:4px 0 8px}
.pane .sec{font-size:11px;color:var(--sub);margin:6px 6px 5px}
.rw{display:flex;justify-content:space-between;align-items:center;border-radius:10px;background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.08);padding:6px 14px;margin-bottom:5px}
.rw b{font-size:14px;font-weight:500}.rw small{display:block;font-size:10.5px;color:var(--sub);margin-top:3px}.rw span{font-size:13px;color:#ccc}
.rw .new{font-size:10px;color:var(--gold);margin-right:8px;font-weight:400}
.rw.f{background:#fff;color:#111;border-color:#fff;transform:scale(1.02)}.rw.f small,.rw.f span{color:#555}
.rw.y span{color:var(--gold)}
.hint{position:absolute;bottom:10px;left:48px;right:347px;text-align:center;font-size:11px;color:#999}
/* the 4.3.2 screen: big buttons, the path on top */
.old .path{position:absolute;right:58px;top:46px;font-size:12px;color:var(--sub)}
.old h1{top:64px}
.old .pane{right:58px;left:58px;top:120px;display:grid;grid-template-columns:1fr 1fr;gap:8px 14px}
.old .rw{padding:12px 16px;margin:0}
.old .rw b{font-size:15px}
`;
const ROWS = (f, layout) => {
  const r = (t, s, v, cls = '', isNew = true) => `<div class="rw ${cls}"><div><b>${t}${isNew ? '<em class="new">جديد</em>' : ''}</b>${s ? `<small>${s}</small>` : ''}</div><span>${v}</span></div>`;
  return [
    layout === 'old' ? r('الفرق المتابعة', '', '٣ فرق', '', false) + r('تطبيق المباريات', '', 'تلقائي', '', false) : r('الدوريات كاملة', '', 'دوريان', '', false),
    r('تنبيه بداية المباراة', 'صافرة قصيرة وتنبيه صغير في الزاوية، لفرقك فقط', 'يعمل', f === 0 ? 'f' : 'y'),
    r('صوت التنبيه', 'صافرة أقل من ثانية ونصف، بمستوى صوت التنبيهات', 'صافرة', f === 1 ? 'f' : ''),
    r('عند الهدف', 'النتيجة في الزاوية', 'لا', f === 2 ? 'f' : ''),
    r('صافرة النهاية', 'النتيجة النهائية في الزاوية', 'لا', f === 3 ? 'f' : ''),
    r('فوق تطبيق آخر', 'فوق يوتيوب وشاهد وغيرها، يحتاج إذن الظهور فوق التطبيقات', 'لا', f === 4 ? 'f' : ''),
    r('جرّب التنبيه', 'يظهر الآن بصوته', '', f === 5 ? 'f' : ''),
  ].join('');
};
const SIDE = ['اللغة', 'الموقع', 'الطقس', 'الشكل والمشاهد', 'التطبيقات', 'الأشخاص', 'الأزرار السريعة', 'الصلاة والأذان', 'المباريات والأسواق', 'شاشة التوقف', 'حول'];
const settingsCouch = f => `<div class="st"><h1>الإعدادات</h1><div class="side"><div class="q">بحث في الإعدادات</div><ul>${SIDE.map(s => `<li class="${s === 'المباريات والأسواق' ? 'on' : ''}"><i></i>${s}</li>`).join('')}</ul></div>
  <div class="pane"><h2>المباريات والأسواق</h2><p>فرقك ودورياتك، وصفحة الأسواق.</p><div class="sec">المباريات، بعد الفرق المتابعة وتطبيق المباريات</div>${ROWS(f, 'couch')}</div></div>`;
const settingsOld = f => `<div class="st old"><div class="path">الإعدادات، المزيد، المباريات والأسواق</div><h1>المباريات</h1><div class="pane">${ROWS(f, 'old')}</div><div class="hint" style="right:58px">حسنًا للتغيير، ورجوع للعودة</div></div>`;
const video = `<div class="video"><div class="pitch"></div><div class="tag">VIDEO APP (stand-in)</div><div class="note">إطار بديل لتطبيق فيديو آخر</div><div class="ctl"></div></div>`;

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 960, height: 540 }, deviceScaleFactor: 2 });
const shot = await shooter(page);
const card = CARDS.find(c => c.id === '04-next');
await shot('11-kickoff-home-tv', PLACES['h-tv'](card) + notice(), KCSS);
await shot('11-kickoff-home-couch', PLACES['h-couch'](card) + notice(), KCSS);
await shot('11-kickoff-saver-tv', PLACES['s-tv'](card) + notice(), KCSS);
await shot('11-kickoff-saver-couch', PLACES['s-couch'](card) + notice(), KCSS);
await shot('11-kickoff-over-video', video + notice(), KCSS);
await shot('11-kickoff-goal-over-video', video + notice('goal'), KCSS);
await shot('11-kickoff-end-over-video', video + notice('end'), KCSS);
await shot('11-kickoff-settings-couch', settingsCouch(0), KCSS);
await shot('11-kickoff-settings-couch-over', settingsCouch(4), KCSS);
await shot('11-kickoff-settings-432', settingsOld(0), KCSS);
await browser.close();
