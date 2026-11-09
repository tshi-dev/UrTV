/* The looks. Each returns the markup of one 1920x1080 home. Option o.d ('a' chosen, 'b', 'c') picks a divergent direction for the studies. */

/* ---------------------------------------------------------------- 1. لوح — tvOS-like depth: photo plate, frosted glass, tiles that lift and tilt toward you */
function lawh(o = {}) {
  const d = o.d || 'a', focus = o.focus ?? 3;
  const tiles = APPS.map(([n, g, c], i) => {
    const f = i === focus;
    const sz = 168;
    return `<div style="position:relative;width:${sz}px;height:${sz + 56}px;display:flex;flex-direction:column;align-items:center;transition:none;${f ? 'transform:translateY(-28px) scale(1.16);z-index:2' : 'opacity:.94'}">
      <div style="width:${sz}px;height:${sz}px;border-radius:40px;position:relative;overflow:hidden;
        background:linear-gradient(155deg,${c} 0%,color-mix(in srgb,${c} 55%,#000) 100%);
        box-shadow:${f ? `0 44px 70px -18px ${c}aa,0 18px 30px rgba(0,0,0,.45),inset 0 2px 0 rgba(255,255,255,.55)` : '0 14px 24px -10px rgba(0,0,0,.55),inset 0 1.5px 0 rgba(255,255,255,.35)'};
        ${f ? 'transform:perspective(900px) rotateX(7deg) rotateY(-9deg)' : ''}">
        <div style="position:absolute;inset:0;background:linear-gradient(180deg,rgba(255,255,255,.34),rgba(255,255,255,0) 46%)"></div>
        <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:#fff;${f ? 'transform:translate(-7px,-7px) scale(1.06);filter:drop-shadow(10px 14px 8px rgba(0,0,0,.35))' : 'filter:drop-shadow(0 4px 4px rgba(0,0,0,.25))'}">${ic(g, 76, 1.7)}</div>
      </div>
      <div style="margin-top:${f ? 30 : 18}px;font-size:${f ? 28 : 24}px;font-weight:${f ? 600 : 400};color:#fff;white-space:nowrap;text-shadow:0 2px 14px rgba(0,0,0,.55);opacity:${f ? 1 : .8}">${n}</div>
    </div>`;
  }).join('');
  const glass = 'background:linear-gradient(180deg,rgba(255,255,255,.20),rgba(255,255,255,.08));backdrop-filter:blur(36px) saturate(1.7);border-radius:32px;box-shadow:inset 0 1.5px 0 rgba(255,255,255,.45),inset 0 -1px 0 rgba(255,255,255,.08),0 30px 60px -24px rgba(0,0,0,.55)';
  const quick = QUICK.map((n, i) => `<div style="width:64px;height:64px;border-radius:20px;display:flex;align-items:center;justify-content:center;color:#fff;${i === 2 ? 'background:rgba(255,255,255,.92);color:#12203a;box-shadow:0 10px 24px rgba(0,0,0,.3)' : ''}">${ic(n, 30, 1.7)}</div>`).join('');
  const dirCss = d === 'b' ? 'right:96px;top:150px;text-align:right' : d === 'c' ? 'left:50%;transform:translateX(-50%);top:120px;text-align:center' : 'right:96px;top:70px;text-align:right';
  return `
  <div style="position:absolute;inset:0;background:${photo('blue-ridges')} center/cover;transform:scale(1.06)"></div>
  <div style="position:absolute;inset:0;background:linear-gradient(180deg,rgba(6,12,34,.50) 0%,rgba(6,12,34,.05) 38%,rgba(6,12,34,.10) 62%,rgba(4,8,24,.78) 100%)"></div>
  ${at('left:96px;top:54px;padding:8px;display:flex;gap:8px;' + glass.replace('border-radius:32px', 'border-radius:28px'), quick)}
  ${at(dirCss, `<div style="font-size:30px;font-weight:500;color:#FFD9B8;text-shadow:0 2px 14px rgba(0,0,0,.4);display:flex;align-items:center;gap:12px;justify-content:inherit"><span style="display:flex;color:rgba(255,255,255,.78)">${ic('wifi', 24, 1.8)}</span>صباح الخير · الرياض ٢٨°</div>
      <div style="font-size:216px;font-weight:200;line-height:1;margin-top:6px;color:#fff;text-shadow:0 0 90px rgba(180,210,255,.45),0 8px 40px rgba(0,0,0,.35)">٩:٥٧</div>
      <div style="font-size:32px;font-weight:300;color:rgba(255,255,255,.92);margin-top:4px;text-shadow:0 2px 14px rgba(0,0,0,.4)">الأربعاء · ٢٤ جمادى الأولى ١٤٤٨</div>`)}
  ${at('left:96px;top:178px;width:560px;height:318px;padding:32px;display:flex;flex-direction:column;justify-content:space-between;color:#fff;' + glass,
    `<div style="display:flex;justify-content:space-between;align-items:baseline"><div style="font-size:30px;font-weight:500;opacity:.9">الصلاة القادمة</div><div style="font-size:28px;opacity:.85">٢:٤٦ م</div></div>
     <div style="font-size:112px;font-weight:300;line-height:1">العصر</div>
     <div><div style="height:8px;border-radius:4px;background:rgba(255,255,255,.22)"><div style="width:64%;height:8px;border-radius:4px;background:#fff;box-shadow:0 0 18px rgba(255,255,255,.7)"></div></div>
     <div style="font-size:26px;margin-top:14px;opacity:.9">بعد ساعتين و٢٠ دقيقة</div></div>`)}
  ${at('left:96px;right:96px;bottom:54px;display:flex;justify-content:space-between;align-items:flex-end', tiles)}`;
}

/* ---------------------------------------------------------------- 2. مجلس — warm paper and wood, an arch window, serif numerals, brass */
function majlis(o = {}) {
  const d = o.d || 'a', focus = o.focus ?? 3;
  const ivory = '#F2E8D4', ink = '#2A2017', green = '#1E5A44', brass = '#B98A3C', wood = '#3B2A1C';
  const star = "url(\"data:image/svg+xml;utf8," + encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' width='64' height='64' viewBox='0 0 48 48' fill='none' stroke='${brass}' stroke-opacity='.22' stroke-width='1.3'><path d='M24 4l5.5 13.5L44 24l-14.5 6.5L24 44l-5.5-13.5L4 24l14.5-6.5z'/><circle cx='24' cy='24' r='6'/></svg>`) + "\")";
  const grain = "url(\"data:image/svg+xml;utf8," + encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' width='300' height='300'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' seed='4'/><feColorMatrix values='0 0 0 0 .25  0 0 0 0 .18  0 0 0 0 .1  0 0 0 .09 0'/></filter><rect width='300' height='300' filter='url(#n)'/></svg>`) + "\")";
  const plaques = APPS.map(([n, g], i) => {
    const f = i === focus;
    return `<div style="flex:1;height:${f ? 152 : 136}px;position:relative;top:${f ? -10 : 0}px;border-radius:10px;background:${f ? green : ivory};color:${f ? ivory : ink};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;
      box-shadow:${f ? `0 0 0 3px ${brass},0 0 0 7px ${wood},0 22px 30px -8px rgba(0,0,0,.55)` : `inset 0 0 0 1.5px ${brass},inset 0 0 0 5px ${ivory},inset 0 0 0 6px rgba(185,138,60,.5),0 6px 10px -4px rgba(0,0,0,.5)`}">
      ${ic(g, 44, 1.5)}<div style="font-size:24px;font-weight:700">${n}</div></div>`;
  }).join('');
  const prayers = PRAYERS.filter(p => p[0] !== 'الشروق').map(([n, t], i) => {
    const nx = i === 2;
    return `<div style="flex:1;height:112px;border-radius:8px;background:${nx ? green : 'rgba(255,255,255,.5)'};color:${nx ? ivory : ink};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;box-shadow:inset 0 0 0 1.5px ${nx ? brass : 'rgba(185,138,60,.55)'}">
      <div style="font-size:26px;font-weight:700">${n}</div><div style="font-size:30px;font-family:${FONT_SERIF}">${t}</div></div>`;
  }).join('');
  const arch = `<div style="position:absolute;left:96px;top:54px;width:520px;height:700px;border-radius:260px 260px 14px 14px;padding:14px;background:linear-gradient(180deg,${brass},#8a6120);box-shadow:0 30px 50px -20px rgba(40,24,8,.65)">
      <div style="width:100%;height:100%;border-radius:246px 246px 6px 6px;background:${photo('haramain-dome')} 62% center/cover;filter:sepia(.38) saturate(1.08) contrast(1.04);box-shadow:inset 0 0 0 6px ${ivory}"></div>
      <div style="position:absolute;inset:14px;border-radius:246px 246px 6px 6px;box-shadow:inset 0 0 60px rgba(60,30,6,.45)"></div></div>`;
  return `
  <div style="position:absolute;inset:0;background:${ivory}"></div>
  <div style="position:absolute;inset:0;background:${star}"></div>
  <div style="position:absolute;inset:0;background:${grain}"></div>
  <div style="position:absolute;left:0;right:0;bottom:0;height:300px;background:linear-gradient(180deg,rgba(242,232,212,0),${ivory} 60%)"></div>
  ${arch}
  ${at('right:96px;top:54px;width:1080px;text-align:right;color:' + ink,
    `<div style="font-family:${FONT_SERIF};font-size:150px;font-weight:700;line-height:1.1;color:${ink}">٢٤ جمادى الأولى</div>
     <div style="display:flex;align-items:center;gap:18px;margin:6px 0 10px"><div style="flex:1;height:2px;background:${brass}"></div><div style="color:${brass}">${ic('star', 34, 1.4)}</div><div style="width:120px;height:2px;background:${brass}"></div></div>
     <div style="font-size:34px;color:#5b4a38">الأربعاء ٩ نوفمبر ٢٠٢٦ · ١٤٤٨ هـ · الرياض ٢٨°</div>
     <div style="display:flex;align-items:baseline;gap:28px;margin-top:20px"><div style="font-family:${FONT_SERIF};font-size:230px;line-height:1;color:${green};font-weight:700">٩:٥٧</div><div style="font-size:44px;color:${brass};font-weight:700">ص</div></div>`)}
  ${at('right:96px;top:640px;width:1080px;display:flex;gap:16px;flex-direction:row-reverse;direction:ltr', `<div style="display:flex;gap:16px;width:100%;direction:rtl">${prayers}</div>`)}
  ${at(`left:96px;right:96px;bottom:54px;height:196px;border-radius:14px;padding:30px 20px 30px;background:linear-gradient(180deg,#4a3623,${wood});display:flex;gap:16px;align-items:flex-end;box-shadow:0 -1px 0 ${brass},0 20px 40px -14px rgba(40,24,8,.6),inset 0 2px 0 rgba(255,255,255,.12)`, plaques)}`;
}

/* ---------------------------------------------------------------- 3. سينما — a hero and shelves; the room goes dark, the picture is the interface */
function cinema(o = {}) {
  const d = o.d || 'a', focus = o.focus ?? 1;
  const cards = [['nature/aurora', 'aurora', 'الشفق القطبي', 64], ['sea', 'sea-sunset', 'غروب البحر', 22], ['ksa', 'alula-rock', 'العلا', 0], ['sp', 'saturn', 'زحل', 88], ['co', 'coral', 'الشعاب', 40]];
  const shelf = cards.map(([, p, t, prog], i) => {
    const f = i === focus, w = f ? 372 : 300, h = f ? 209 : 169;
    return `<div style="flex:none;width:${w}px;position:relative;${f ? 'z-index:2' : 'opacity:.78'}">
      <div style="width:${w}px;height:${h}px;border-radius:14px;background:${photo(p)} center/cover;position:relative;overflow:hidden;${f ? 'box-shadow:0 0 0 4px #fff,0 40px 60px -10px rgba(0,0,0,.85)' : ''}">
        <div style="position:absolute;left:0;right:0;bottom:0;height:90px;background:linear-gradient(transparent,rgba(0,0,0,.8))"></div>
        <div style="position:absolute;right:16px;bottom:${prog ? 24 : 14}px;font-size:${f ? 30 : 24}px;font-weight:700;color:#fff">${t}</div>
        ${prog ? `<div style="position:absolute;left:0;right:0;bottom:0;height:6px;background:rgba(255,255,255,.28);direction:rtl"><div style="width:${prog}%;height:6px;background:#E5484D"></div></div>` : ''}
      </div></div>`;
  }).join('');
  const dock = APPS.map(([n, g], i) => `<div style="display:flex;flex-direction:column;align-items:center;gap:8px;color:#fff;opacity:${i === 3 ? 1 : .55}">${ic(g, 38, 1.5)}<div style="font-size:22px">${n}</div></div>`).join('');
  return `
  <div style="position:absolute;inset:0;background:#050505"></div>
  <div style="position:absolute;left:0;top:0;width:1500px;height:1080px;background:${photo('haramain-night')} 40% center/cover"></div>
  <div style="position:absolute;inset:0;background:linear-gradient(270deg,#050505 6%,rgba(5,5,5,.86) 34%,rgba(5,5,5,.1) 74%),linear-gradient(180deg,rgba(5,5,5,.55),rgba(5,5,5,0) 30%,rgba(5,5,5,.5) 58%,#050505 94%)"></div>
  ${at('left:96px;top:54px;display:flex;gap:28px;color:#fff;opacity:.85', QUICK.map(n => ic(n, 32, 1.5)).join(''))}
  ${at('left:96px;top:104px;font-size:44px;font-weight:300;color:#fff;opacity:.9', '٩:٥٧')}
  ${at('right:96px;top:150px;width:900px;text-align:right', `
     <div style="font-size:30px;font-weight:700;color:#E5484D">أكمل المشاهدة</div>
     <div style="font-family:${FONT_SERIF};font-size:128px;font-weight:700;line-height:1.1;color:#fff;margin-top:6px;text-shadow:0 10px 50px rgba(0,0,0,.7)">ليالي المدينة</div>
     <div style="font-size:30px;color:rgba(255,255,255,.82);margin-top:10px">وثائقي · ٢٠٢٥ · الحلقة ٤ من ٦ · بقي ٢٣ دقيقة</div>
     <div style="display:flex;gap:16px;margin-top:36px">
       <div style="height:84px;padding:0 44px;border-radius:16px;background:#fff;color:#000;font-size:34px;font-weight:700;display:flex;align-items:center;gap:14px;box-shadow:0 18px 40px -8px rgba(255,255,255,.35)">${ic('play', 34)}تابع المشاهدة</div>
       <div style="height:84px;padding:0 40px;border-radius:16px;background:rgba(255,255,255,.16);backdrop-filter:blur(20px);color:#fff;font-size:30px;display:flex;align-items:center;box-shadow:inset 0 0 0 1.5px rgba(255,255,255,.3)">التفاصيل</div></div>`)}
  ${at('right:96px;top:612px;font-size:28px;font-weight:700;color:#fff', 'يُقترح لك')}
  ${at('right:96px;top:668px;left:96px;display:flex;gap:20px;align-items:flex-start;direction:rtl', shelf)}
  ${at('left:96px;right:96px;bottom:54px;display:flex;justify-content:space-between;padding-top:0', dock)}`;
}

/* ---------------------------------------------------------------- 4. إطار — the TV as a picture on a wall: a mat, a thin frame, a brass plaque, light from above */
function frame(o = {}) {
  const d = o.d || 'a';
  return `
  <div style="position:absolute;inset:0;background:radial-gradient(ellipse at 50% 0%,#3a3631 0%,#1d1b19 55%,#100f0e 100%)"></div>
  <div style="position:absolute;inset:0;background:radial-gradient(ellipse 900px 520px at 50% 6%,rgba(255,236,200,.20),transparent 70%)"></div>
  <div style="position:absolute;inset:0;opacity:.5;background:repeating-linear-gradient(90deg,rgba(255,255,255,.012) 0 3px,transparent 3px 7px)"></div>
  ${at('left:50%;transform:translateX(-50%);top:34px;width:300px;height:14px;border-radius:7px;background:linear-gradient(180deg,#d4b06a,#7c5a22);box-shadow:0 0 0 1px #3b2a10,0 14px 40px 8px rgba(255,226,170,.35)', '')}
  ${at('left:50%;transform:translateX(-50%);top:92px;width:1288px;height:884px;padding:22px;border-radius:6px;background:linear-gradient(145deg,#2b2621,#0d0c0b 60%,#1e1a16);box-shadow:0 70px 90px -30px rgba(0,0,0,.9),0 0 0 2px #050404,inset 0 2px 0 rgba(255,255,255,.12)', `
    <div style="width:100%;height:100%;background:#EDE7DA;padding:98px 98px 150px;position:relative;box-shadow:inset 0 0 0 1px rgba(0,0,0,.25),inset 0 4px 18px rgba(0,0,0,.25)">
      <div style="width:100%;height:100%;background:${photo('alula-rock')} center 60%/cover;box-shadow:0 0 0 1px rgba(0,0,0,.45),inset 0 0 0 1px rgba(255,255,255,.1);position:relative;overflow:hidden"><div style="position:absolute;inset:0;background:linear-gradient(115deg,transparent 38%,rgba(255,255,255,.13) 46%,transparent 56%)"></div><div style="position:absolute;inset:0;box-shadow:inset 0 0 90px rgba(40,20,0,.4)"></div></div>
      <div style="position:absolute;right:98px;bottom:44px;text-align:right;color:#26221c">
        <div style="font-family:${FONT_SERIF};font-size:46px;font-weight:700;line-height:1.2">صخرة الفيل · العلا</div>
        <div style="font-size:24px;color:#6b6356;margin-top:4px">ويكيميديا كومنز · مجموعة «مدن السعودية»</div></div>
      <div style="position:absolute;left:98px;bottom:46px;font-family:${FONT_SERIF};font-size:54px;color:#26221c;opacity:.82">٩:٥٧</div>
    </div>`)}`;
}

/* ---------------------------------------------------------------- 5. يوم — the home follows the day: a sky dial with the five prayers, the palette moves from Fajr to Isha */
const PHASES = [
  { id: 'fajr', ar: 'الفجر', bg: 'linear-gradient(180deg,#0A0E2E 0%,#26357A 55%,#7E8CCB 100%)', text: '#EEF2FF', sub: '#C4CCF0', acc: '#A9BBFF', glass: 'rgba(14,20,60,.46)', line: 'rgba(190,205,255,.32)', veil: 'rgba(8,12,40,.38)', tint: '#3C4FB0', sun: 0.04, dark: true, time: '٥:١٢', nx: 'الشروق · بعد ساعة' },
  { id: 'dhuhr', ar: 'الظهر', bg: 'linear-gradient(180deg,#3F95E6 0%,#8CC8F5 55%,#F4EAD0 100%)', text: '#0E2236', sub: '#27445F', acc: '#C2570C', glass: 'rgba(255,255,255,.46)', line: 'rgba(255,255,255,.7)', veil: 'rgba(255,255,255,.16)', tint: '#6EB6F0', sun: 0.5, dark: false, time: '١٢:٤٠', nx: 'العصر · بعد ٣ ساعات' },
  { id: 'maghrib', ar: 'المغرب', bg: 'linear-gradient(180deg,#2B1456 0%,#B7385F 52%,#F7A23C 100%)', text: '#FFF3E4', sub: '#FFD9B8', acc: '#FFD08A', glass: 'rgba(60,18,60,.42)', line: 'rgba(255,214,170,.38)', veil: 'rgba(60,14,50,.30)', tint: '#E0604A', sun: 0.82, dark: true, time: '٥:٣٠', nx: 'العشاء · بعد ساعة ونصف' },
  { id: 'isha', ar: 'العشاء', bg: 'linear-gradient(180deg,#04050D 0%,#131138 60%,#2A1E5C 100%)', text: '#E9E7FF', sub: '#B9B4E6', acc: '#B7A6FF', glass: 'rgba(18,16,56,.5)', line: 'rgba(180,170,255,.28)', veil: 'rgba(4,4,18,.45)', tint: '#4B3AA0', sun: 0.97, dark: true, time: '٨:١٥', nx: 'الفجر · بعد ٩ ساعات' },
];
function dial(o = {}) {
  const P = PHASES[o.ph ?? 2], focus = o.focus ?? 3, small = o.small;
  const R = 330, cx = 380, cy = 380;
  const pol = (a, r) => [cx + r * Math.cos(a), cy + r * Math.sin(a)];
  const a0 = Math.PI * 0.82, a1 = Math.PI * 2.18;           // the day arc, open at the bottom
  const arcPt = f => a0 + (a1 - a0) * f;
  const marks = [['الفجر', 0.04], ['الظهر', 0.5], ['العصر', 0.68], ['المغرب', 0.82], ['العشاء', 0.97]];
  const arcPath = (f0, f1) => { const [x0, y0] = pol(arcPt(f0), R), [x1, y1] = pol(arcPt(f1), R); return `M${x0},${y0} A${R},${R} 0 ${f1 - f0 > .5 ? 1 : 0} 1 ${x1},${y1}`; };
  const sunP = pol(arcPt(P.sun), R);
  const ticks = marks.map(([n, f]) => { const [x, y] = pol(arcPt(f), R), [lx, ly] = pol(arcPt(f), R + (f > .78 && f < .9 ? 150 : f > .6 ? 96 : 58)); return `<circle cx="${x}" cy="${y}" r="9" fill="${P.dark ? '#fff' : '#0E2236'}" opacity=".9"/><text x="${lx}" y="${ly + 8}" text-anchor="${f > .6 ? 'start' : f < .1 ? 'end' : 'middle'}" font-size="26" font-weight="600" fill="${P.text}" font-family="${FONT_SANS}">${n}</text>`; }).join('');
  const svg = `<svg width="760" height="760" viewBox="0 0 760 760" style="overflow:visible">
    <defs><linearGradient id="arc${P.id}" x1="0" x2="1"><stop offset="0" stop-color="${P.acc}" stop-opacity=".25"/><stop offset="1" stop-color="${P.acc}"/></linearGradient>
    <radialGradient id="sun${P.id}"><stop offset="0" stop-color="#fff"/><stop offset=".35" stop-color="${P.acc}"/><stop offset="1" stop-color="${P.acc}" stop-opacity="0"/></radialGradient></defs>
    <path d="${arcPath(0, 1)}" stroke="${P.line}" stroke-width="10" fill="none" stroke-linecap="round"/>
    <path d="${arcPath(0.04, P.sun)}" stroke="url(#arc${P.id})" stroke-width="10" fill="none" stroke-linecap="round"/>
    ${ticks}
    <circle cx="${sunP[0]}" cy="${sunP[1]}" r="46" fill="url(#sun${P.id})"/><circle cx="${sunP[0]}" cy="${sunP[1]}" r="15" fill="#fff"/></svg>`;
  const tiles = APPS.map(([n, g, c], i) => {
    const f = i === focus;
    return `<div style="flex:1;height:${f ? 168 : 148}px;position:relative;top:${f ? -10 : 0}px;border-radius:30px;background:${f ? P.text : P.glass};color:${f ? (P.dark ? '#14122e' : '#fff') : P.text};backdrop-filter:blur(26px) saturate(1.4);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;box-shadow:${f ? `0 30px 50px -16px ${P.tint}, 0 0 0 2px ${P.acc}` : `inset 0 0 0 1.5px ${P.line}`}">${ic(g, 46, 1.6)}<div style="font-size:24px;font-weight:600">${n}</div></div>`;
  }).join('');
  return `
  <div style="position:absolute;inset:0;background:${P.bg}"></div>
  <div style="position:absolute;inset:0;background:${photo('riyadh-dusk')} center/cover;mix-blend-mode:${P.dark ? 'soft-light' : 'multiply'};opacity:${P.dark ? .7 : .32}"></div>
  <div style="position:absolute;inset:0;background:${P.veil}"></div>
  ${at('left:96px;top:54px;display:flex;gap:8px;padding:8px;border-radius:24px;background:' + P.glass + ';backdrop-filter:blur(24px);box-shadow:inset 0 0 0 1.5px ' + P.line, QUICK.map((n, i) => `<div style="width:60px;height:60px;border-radius:18px;display:flex;align-items:center;justify-content:center;color:${i === 2 ? (P.dark ? '#14122e' : '#fff') : P.text};background:${i === 2 ? P.text : 'transparent'}">${ic(n, 28, 1.7)}</div>`).join(''))}
  ${at('right:96px;top:54px;text-align:right;color:' + P.text, `<div style="font-size:30px;font-weight:600;color:${P.acc}">${P.ar}</div><div style="font-size:26px;color:${P.sub}">سماء صافية · الرياض ٢٨°</div>`)}
  ${at(`left:${small ? 700 : 560}px;top:70px;width:760px;height:760px;color:${P.text}`, svg + `
    <div style="position:absolute;left:0;right:0;top:250px;text-align:center"><div style="font-size:176px;font-weight:200;line-height:1;text-shadow:0 0 60px ${P.tint}">${P.time}</div>
    <div style="font-size:30px;color:${P.sub};margin-top:10px">الأربعاء · ٢٤ جمادى الأولى</div></div>
    <div style="position:absolute;left:-120px;right:-120px;top:568px;text-align:center"><div style="font-size:26px;color:${P.sub}">الصلاة القادمة</div><div style="font-size:60px;font-weight:600;color:${P.acc}">${P.nx}</div></div>`)}
  ${at('left:96px;right:96px;bottom:54px;display:flex;gap:16px;align-items:flex-end', tiles)}`;
}

/* ---------------------------------------------------------------- 6. نقاط — dot-matrix: black, a halftone photo, a clock made of dots, one red signal */
const GLYPHS = { '0': ['01110', '10001', '10011', '10101', '11001', '10001', '01110'], '1': ['00100', '01100', '00100', '00100', '00100', '00100', '01110'], '2': ['01110', '10001', '00001', '00010', '00100', '01000', '11111'],
  '3': ['11110', '00001', '00001', '01110', '00001', '00001', '11110'], '4': ['00010', '00110', '01010', '10010', '11111', '00010', '00010'], '5': ['11111', '10000', '11110', '00001', '00001', '10001', '01110'],
  '6': ['00110', '01000', '10000', '11110', '10001', '10001', '01110'], '7': ['11111', '00001', '00010', '00100', '01000', '01000', '01000'], '8': ['01110', '10001', '10001', '01110', '10001', '10001', '01110'],
  '9': ['01110', '10001', '10001', '01111', '00001', '00010', '01100'], ':': ['0', '0', '1', '0', '1', '0', '0'] };
function dotText(str, pitch, r, color, red) {
  let x = 0, out = '';
  for (const ch of str) {
    const g = GLYPHS[ch]; if (!g) { x += pitch * 2; continue; }
    const w = g[0].length;
    g.forEach((row, y) => [...row].forEach((v, i) => { if (v === '1') out += `<circle cx="${x + i * pitch + r}" cy="${y * pitch + r}" r="${r}" fill="${ch === ':' && red ? red : color}"/>`; }));
    x += (w + 1) * pitch;
  }
  return `<svg width="${x}" height="${7 * pitch}" viewBox="0 0 ${x} ${7 * pitch}">${out}</svg>`;
}
function dots(o = {}) {
  const focus = o.focus ?? 3, red = '#FF3B30', w = '#F4F4F2';
  const tiles = APPS.map(([n, g], i) => {
    const f = i === focus;
    return `<div style="flex:1;height:150px;position:relative;background:${f ? w : 'transparent'};color:${f ? '#000' : w};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;box-shadow:${f ? 'none' : `inset 0 0 0 1.5px #3b3b3b`}">
      ${f ? `<div style="position:absolute;right:14px;top:14px;width:14px;height:14px;border-radius:7px;background:${red}"></div>` : ''}
      ${ic(g, 40, 1.6)}<div style="font-size:24px;font-weight:500">${n}</div>
      <div style="position:absolute;left:8px;top:8px;width:10px;height:10px;border-left:2px solid ${f ? '#000' : '#666'};border-top:2px solid ${f ? '#000' : '#666'}"></div>
      <div style="position:absolute;right:8px;bottom:8px;width:10px;height:10px;border-right:2px solid ${f ? '#000' : '#666'};border-bottom:2px solid ${f ? '#000' : '#666'}"></div></div>`;
  }).join('');
  const prayers = PRAYERS.filter(p => p[0] !== 'الشروق').map(([n, t], i) => `<div style="display:flex;justify-content:space-between;align-items:center;height:48px;font-size:26px;color:${i === 2 ? '#000' : w};background:${i === 2 ? w : 'transparent'};padding:0 14px;box-shadow:inset 0 -1px 0 #333"><span>${n}</span><span style="font-family:${FONT_MONO}">${t.replace(/[٠-٩]/g, d => '٠١٢٣٤٥٦٧٨٩'.indexOf(d))}</span></div>`).join('');
  const halftone = `-webkit-mask-image:radial-gradient(circle at center,#000 34%,transparent 38%);-webkit-mask-size:12px 12px;mask-image:radial-gradient(circle at center,#000 34%,transparent 38%);mask-size:12px 12px`;
  return `
  <div style="position:absolute;inset:0;background:#000"></div>
  <div style="position:absolute;left:0;top:0;width:1500px;height:1080px;background:${photo('saturn')} 55% center/cover;filter:grayscale(1) contrast(1.35) brightness(1.15);${halftone}"></div>
  <div style="position:absolute;inset:0;background:linear-gradient(270deg,#000 4%,rgba(0,0,0,.6) 36%,rgba(0,0,0,0) 70%),linear-gradient(0deg,#000 8%,transparent 40%)"></div>
  ${at('left:96px;top:54px;display:flex;gap:22px;color:' + w, QUICK.map((n, i) => `<div style="width:56px;height:56px;display:flex;align-items:center;justify-content:center;${i === 2 ? `background:${w};color:#000` : 'box-shadow:inset 0 0 0 1.5px #444'}">${ic(n, 28, 1.6)}</div>`).join(''))}
  ${at('right:96px;top:54px;text-align:right;color:' + w, `<div style="font-size:26px;color:#9a9a96">الأربعاء ٢٤ جمادى الأولى · الرياض ٢٨°</div>`)}
  ${at('right:96px;top:120px', dotText('09:57', 24, 9, w, red))}
  ${at('right:96px;top:430px;width:560px;color:' + w, `<div style="font-size:26px;color:#9a9a96;margin-bottom:8px">الصلاة القادمة</div><div style="display:flex;align-items:baseline;gap:18px"><span style="font-size:96px;font-weight:600;line-height:1">العصر</span><span style="font-family:${FONT_MONO};font-size:44px;color:${red}">-02:20</span></div><div style="height:16px"></div>${prayers}`)}
  ${at('left:96px;right:96px;bottom:54px;display:flex;gap:16px', tiles)}`;
}

/* ---------------------------------------------------------------- 7. ألوان — playful precision: flat colour blocks, thick ink, hard shadows that move with the focus */
function blocks(o = {}) {
  const focus = o.focus ?? 3, ink = '#121212', paper = '#F5EFE3';
  const C = { orange: '#FF5A1F', yellow: '#FFD23F', blue: '#2D5BFF', pink: '#FF8FB1', green: '#19B37A' };
  const apps = [['يوتيوب', 'movie', C.orange, 2, 2], ['التلفزيون', 'tv', C.blue, 1, 1], ['الموسيقى', 'music', C.pink, 1, 1], ['ألعاب العائلة', 'game', C.yellow, 2, 1], ['الصور', 'photo', C.green, 1, 1], ['البث', 'cast', C.blue, 1, 1], ['المفضلة', 'star', C.pink, 1, 1], ['الإعدادات', 'sliders', C.green, 1, 1]];
  const bento = apps.map(([n, g, c, cw, ch], i) => {
    const f = i === focus;
    return `<div style="grid-column:span ${cw};grid-row:span ${ch};border-radius:28px;background:${c};border:6px solid ${ink};position:relative;display:flex;flex-direction:${cw === 2 && ch === 2 ? 'column' : 'row'};align-items:${cw === 2 && ch === 2 ? 'flex-start' : 'center'};justify-content:space-between;padding:22px 26px;color:${ink};
      transform:${f ? 'translate(-8px,-8px)' : 'none'};box-shadow:${f ? `12px 12px 0 ${ink}` : `0 0 0 ${ink}`};z-index:${f ? 2 : 1}">
      <div style="display:flex;${cw === 2 && ch === 2 ? 'flex-direction:column' : 'align-items:center'};gap:14px">${ic(g, cw === 2 && ch === 2 ? 96 : 54, 2.4)}</div>
      <div style="font-size:${cw === 2 && ch === 2 ? 48 : 30}px;font-weight:800">${n}</div></div>`;
  }).join('');
  return `
  <div style="position:absolute;inset:0;background:${paper}"></div>
  <div style="position:absolute;inset:0;background-image:radial-gradient(${ink} 1.4px,transparent 1.6px);background-size:36px 36px;opacity:.16"></div>
  ${at('left:96px;top:54px;display:flex;gap:12px', QUICK.map((n, i) => `<div style="width:64px;height:64px;border-radius:18px;border:4px solid ${ink};background:${i === 2 ? C.yellow : '#fff'};display:flex;align-items:center;justify-content:center;color:${ink};${i === 2 ? `transform:translate(-4px,-4px);box-shadow:6px 6px 0 ${ink}` : ''}">${ic(n, 30, 2.2)}</div>`).join(''))}
  ${at('right:96px;top:54px;width:760px;height:372px;border-radius:36px;background:' + C.orange + ';border:6px solid ' + ink + ';padding:20px 36px;color:' + ink + ';box-shadow:12px 12px 0 ' + ink,
    `<div style="display:flex;justify-content:space-between;font-size:30px;font-weight:800"><span>الأربعاء · ٢٤ جمادى الأولى</span><span>٢٨°</span></div>
     <div style="font-size:238px;font-weight:900;line-height:.98;margin-top:2px;letter-spacing:0">٩:٥٧</div>`)}
  ${at('left:96px;top:176px;width:640px;height:250px;border-radius:36px;overflow:hidden;border:6px solid ' + ink + ';background:' + photo('coral') + ' center/cover;box-shadow:12px 12px 0 ' + ink,
    `<div style="position:absolute;right:20px;bottom:20px;height:56px;padding:0 22px;border-radius:14px;background:${paper};color:${ink};border:4px solid ${ink};display:flex;align-items:center;font-size:26px;font-weight:800">الشعاب المرجانية</div>`)}
  ${at('right:96px;top:474px;width:760px;height:128px;border-radius:28px;background:' + C.yellow + ';border:6px solid ' + ink + ';display:flex;align-items:center;justify-content:space-between;padding:0 32px;color:' + ink + ';box-shadow:10px 10px 0 ' + ink,
    `<div><div style="font-size:26px;font-weight:700">الصلاة القادمة</div><div style="font-size:60px;font-weight:900;line-height:1.05">العصر</div></div><div style="text-align:left"><div style="font-size:54px;font-weight:900">٢:٤٦ م</div><div style="font-size:24px;font-weight:700">بعد ساعتين و٢٠ دقيقة</div></div>`)}
  ${at('left:96px;right:96px;bottom:54px;height:360px;display:grid;grid-template-columns:repeat(6,1fr);grid-template-rows:repeat(2,1fr);gap:24px', bento)}`;
}
