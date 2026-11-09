/* «ألعاب العائلة»: a party game, not a form. Big heavy type, saturated colour on deep violet, shapes as answers, team avatars, moments that celebrate. */
const GAME = { violet: '#1B0F4D', violet2: '#3B1E8F', pink: '#FF4D6D', blue: '#3DA5FF', yellow: '#FFC933', green: '#2FD39A', ink: '#120A33' };
const AVATARS = {
  falcon: '<path d="M6 40L32 12l26 28-12-3-14 17-14-17z" fill="#fff"/><circle cx="32" cy="30" r="3" fill="#120A33"/>',
  star: '<path d="M32 8l7.2 15 16.3 2.2-11.9 11.4 3 16.2L32 44.6 17.4 52.8l3-16.2L8.5 25.2 24.8 23z" fill="#fff"/>',
  lion: '<circle cx="32" cy="33" r="26" fill="none" stroke="#fff" stroke-width="6" stroke-dasharray="7 5"/><circle cx="32" cy="33" r="15" fill="#fff"/><circle cx="26" cy="30" r="2.4" fill="#120A33"/><circle cx="38" cy="30" r="2.4" fill="#120A33"/>',
  palm: '<path d="M30 56V30h4v26z" fill="#fff"/><path d="M32 30C20 30 12 22 10 16c8 0 16 2 22 14zM32 30c12 0 20-8 22-14-8 0-16 2-22 14zM32 30C26 20 28 12 32 6c4 6 6 14 0 24z" fill="#fff"/>',
};
const TEAMS = [['الصقور', GAME.pink, 'falcon'], ['النجوم', GAME.blue, 'star'], ['الأسود', GAME.yellow, 'lion'], ['الواحة', GAME.green, 'palm']];
const avatar = (i, s = 96, ring = false) => { const [, c, a] = TEAMS[i]; return `<div style="width:${s}px;height:${s}px;border-radius:${Math.round(s * .3)}px;background:${c};display:flex;align-items:center;justify-content:center;flex:none;box-shadow:${ring ? `0 0 0 5px #fff,0 0 40px ${c}` : `0 10px 20px -6px ${c}88`}"><svg width="${Math.round(s * .66)}" height="${Math.round(s * .66)}" viewBox="0 0 64 64">${AVATARS[a]}</svg></div>`; };
const SHAPES = {
  tri: '<path d="M32 8L58 54H6z" fill="#fff"/>', dia: '<path d="M32 4l28 28-28 28L4 32z" fill="#fff"/>', cir: '<circle cx="32" cy="32" r="26" fill="#fff"/>', sq: '<rect x="8" y="8" width="48" height="48" rx="6" fill="#fff"/>',
};
const shape = (k, s = 56) => `<svg width="${s}" height="${s}" viewBox="0 0 64 64">${SHAPES[k]}</svg>`;
function rnd(seed) { let x = seed; return () => (x = (x * 16807) % 2147483647) / 2147483647; }
function confetti(n = 46, seed = 7, density = 1, W = 1920, H = 1080) {
  const r = rnd(seed), cols = [GAME.pink, GAME.blue, GAME.yellow, GAME.green, '#fff'];
  let out = '';
  for (let i = 0; i < n; i++) {
    const x = r() * W, y = r() * H, c = cols[Math.floor(r() * 5)], a = Math.floor(r() * 360), k = Math.floor(r() * 3), s = 14 + r() * 26;
    out += `<div style="position:absolute;left:${x}px;top:${y}px;width:${s}px;height:${k === 1 ? s : s * .5}px;border-radius:${k === 1 ? '50%' : '4px'};background:${c};opacity:${.2 + r() * .45 * density};transform:rotate(${a}deg)"></div>`;
  }
  return out;
}
const gameBg = (seed = 7, n = 46) => `<div style="position:absolute;inset:0;background:radial-gradient(ellipse 1300px 900px at 30% 10%,${GAME.violet2},${GAME.violet} 62%,#0d0630)"></div>${confetti(n, seed)}`;
const heavy = 'font-weight:900;letter-spacing:0';

/* ---------------------------------------------------------------- hub */
function gamesHub() {
  const cards = [
    ['سؤال وجواب', '٢–٤ فرق · أسئلة إسلامية وعامة', 'linear-gradient(150deg,#FF4D6D,#FF8A3D)', -3, true,
      `<div style="font-size:300px;font-weight:900;line-height:.8;color:#fff;text-shadow:0 14px 0 rgba(0,0,0,.18)">؟</div>`],
    ['ذاكرة الأزواج', '١–٤ لاعبين · اقلب وجد الزوج', 'linear-gradient(150deg,#3DA5FF,#6C4DFF)', 2, false,
      `<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:10px;width:360px">${Array.from({ length: 12 }, (_, i) => `<div style="height:70px;border-radius:14px;background:${[1, 4, 7, 10].includes(i) ? '#fff' : 'rgba(255,255,255,.24)'};display:flex;align-items:center;justify-content:center;color:${['#FF4D6D', '#FFC933', '#2FD39A', '#3DA5FF'][i % 4]}">${[1, 4, 7, 10].includes(i) ? ic('star', 36, 2.4) : ''}</div>`).join('')}</div>`],
    ['خمّن الكلمة', '١–٢ لاعب · كلمة من خمسة حروف', 'linear-gradient(150deg,#2FD39A,#C9D83A)', -2, false,
      `<div style="display:grid;grid-template-columns:repeat(5,70px);gap:10px;direction:rtl">${[['م', GAME.green], ['د', '#fff'], ['ر', GAME.yellow], ['س', '#fff'], ['ة', GAME.green]].map(([l, c]) => `<div style="height:84px;border-radius:16px;background:${c === '#fff' ? 'rgba(255,255,255,.34)' : c};color:${c === '#fff' ? '#fff' : GAME.ink};font-size:54px;font-weight:900;display:flex;align-items:center;justify-content:center">${l}</div>`).join('')}</div>`],
  ];
  const cs = cards.map(([n, d, bg, rot, f, art]) => `<div style="width:520px;height:600px;border-radius:56px;background:${bg};position:relative;padding:36px;display:flex;flex-direction:column;justify-content:space-between;color:#fff;
    transform:rotate(${f ? 0 : rot}deg) scale(${f ? 1.08 : 1}) translateY(${f ? -20 : 0}px);box-shadow:${f ? '0 0 0 8px #fff,0 50px 80px -10px rgba(255,77,109,.6)' : '0 30px 50px -20px rgba(0,0,0,.6)'}">
      <div style="flex:1;display:flex;align-items:center;justify-content:center">${art}</div>
      <div><div style="font-size:80px;${heavy};line-height:1.1;text-shadow:0 6px 0 rgba(0,0,0,.18)">${n}</div><div style="font-size:30px;font-weight:600;margin-top:8px;opacity:.95">${d}</div></div></div>`).join('');
  const key = (c, t) => `<div style="display:flex;align-items:center;gap:12px;font-size:26px;color:#fff"><div style="width:30px;height:30px;border-radius:15px;background:${c}"></div>${t}</div>`;
  return `${gameBg(11)}
  ${at('right:96px;top:54px', `<div style="font-size:104px;${heavy};line-height:1.1;color:#fff;text-shadow:0 8px 0 rgba(0,0,0,.25)">ألعاب العائلة</div><div style="font-size:34px;font-weight:600;color:#D9CCFF;margin-top:6px">بدون إنترنت · بدون إعلانات · بالريموت فقط</div>`)}
  ${at('left:96px;right:96px;top:318px;display:flex;justify-content:space-between;align-items:center', cs)}
  ${at('left:96px;right:96px;bottom:54px;display:flex;justify-content:space-between;align-items:center;color:#fff', `<div style="display:flex;gap:36px">${key(GAME.pink, 'الأحمر: ابدأ')}${key(GAME.green, 'الأخضر: الفرق')}${key(GAME.yellow, 'الأصفر: التعليمات')}${key(GAME.blue, 'الأزرق: الأصوات')}</div><div style="font-size:26px;opacity:.9">◀ ▶ للتنقل · OK للعب</div>`)}`;
}

/* ---------------------------------------------------------------- the quiz */
function teamBar(scores, active, bump) {
  return `<div style="display:flex;gap:24px;justify-content:center;align-items:flex-end;direction:rtl">${TEAMS.map(([n, c], i) => {
    const on = i === active, b = bump === i;
    return `<div style="display:flex;align-items:center;gap:16px;height:${on ? 132 : 112}px;padding:0 26px 0 30px;border-radius:30px;background:${on ? 'rgba(255,255,255,.96)' : 'rgba(255,255,255,.12)'};color:${on ? GAME.ink : '#fff'};box-shadow:${on ? `0 0 0 5px ${c},0 24px 40px -10px ${c}aa` : 'inset 0 0 0 2px rgba(255,255,255,.18)'};transform:${b ? 'translateY(-14px) scale(1.06)' : 'none'}">
      ${avatar(i, on ? 92 : 76)}<div><div style="font-size:${on ? 32 : 28}px;font-weight:800">${n}</div><div style="font-size:${on ? 64 : 52}px;${heavy};line-height:1;color:${on ? c : '#fff'};filter:${on ? 'brightness(.82)' : 'none'}">${scores[i]}</div></div></div>`;
  }).join('')}</div>`;
}
function timerRing(sec, pct) {
  const R = 70, C = 2 * Math.PI * R;
  return `<div style="position:relative;width:176px;height:176px"><svg width="176" height="176" viewBox="0 0 176 176"><circle cx="88" cy="88" r="${R}" stroke="rgba(255,255,255,.2)" stroke-width="16" fill="none"/><circle cx="88" cy="88" r="${R}" stroke="${GAME.yellow}" stroke-width="16" fill="none" stroke-linecap="round" stroke-dasharray="${C}" stroke-dashoffset="${C * (1 - pct)}" transform="rotate(-90 88 88)"/></svg>
    <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;font-size:78px;${heavy};color:#fff">${sec}</div></div>`;
}
function quiz(o = {}) {
  const reveal = o.reveal, pick = o.pick ?? 2, right = 2;
  const ans = [['٣', GAME.pink, 'tri'], ['٤', GAME.blue, 'dia'], ['٥', GAME.yellow, 'cir'], ['٦', GAME.green, 'sq']];
  const blocks = ans.map(([t, c, s], i) => {
    const f = !reveal && i === pick, good = reveal && i === right, dim = reveal && i !== right;
    return `<div style="height:150px;border-radius:36px;background:${c};display:flex;align-items:center;gap:28px;padding:0 40px;color:${i === 2 ? GAME.ink : '#fff'};position:relative;
      transform:${f ? 'scale(1.045)' : good ? 'scale(1.06)' : 'none'};opacity:${dim ? .3 : 1};box-shadow:${f ? `0 0 0 8px #fff,0 30px 60px -10px ${c}` : good ? `0 0 0 10px #fff,0 0 90px 10px ${c}` : '0 14px 0 rgba(0,0,0,.22)'}">
      <div style="width:84px;height:84px;border-radius:22px;background:rgba(0,0,0,.2);display:flex;align-items:center;justify-content:center;flex:none">${shape(s, 54)}</div>
      <div style="font-size:104px;${heavy};flex:1">${t}</div>
      ${good ? `<div style="position:absolute;left:30px;top:50%;transform:translateY(-50%);width:84px;height:84px;border-radius:42px;background:#fff;color:${GAME.green};display:flex;align-items:center;justify-content:center">${ic('check', 54, 3.2)}</div>` : ''}</div>`;
  }).join('');
  return `${gameBg(reveal ? 21 : 5, reveal ? 90 : 40)}
  ${at('right:96px;top:54px;display:flex;align-items:center;gap:28px', timerRing(reveal ? '٠' : '١٢', reveal ? 0 : .6))}
  ${at('left:96px;top:70px;text-align:left;color:#fff', `<div style="font-size:34px;font-weight:700;color:#D9CCFF">السؤال ٤ من ١٠</div><div style="font-size:54px;${heavy};margin-top:2px">${reveal ? 'إجابة صحيحة!' : 'دور الصقور'}</div>`)}
  ${at('left:96px;right:96px;top:228px;text-align:center', `<div style="font-size:112px;${heavy};line-height:1.25;color:#fff;text-shadow:0 8px 0 rgba(0,0,0,.25)">كم عدد <span style="color:${GAME.yellow}">أركان</span> الإسلام؟</div>`)}
  ${at('left:96px;right:96px;top:452px;display:grid;grid-template-columns:1fr 1fr;gap:28px', blocks)}
  ${reveal ? at('right:50%;top:382px;transform:translateX(50%)', `<div style="padding:0 40px;height:84px;border-radius:24px;background:#fff;color:${GAME.ink};font-size:44px;${heavy};display:flex;align-items:center;gap:16px;box-shadow:0 20px 40px rgba(0,0,0,.4);white-space:nowrap"><span style="color:${GAME.green}">+١٠</span> للصقور</div>`) : ''}
  ${at('left:96px;right:96px;bottom:42px', teamBar(reveal ? ['٤٠', '٢٠', '٢٠', '١٠'] : ['٣٠', '٢٠', '٢٠', '١٠'], 0, reveal ? 0 : -1))}`;
}

/* ---------------------------------------------------------------- score animation: the points leave the answer, fly, land and tick up */
function scoreStrip() {
  const frame = (ms, token, scoreNow, bump, caption) => `<div style="position:relative;width:560px;height:700px;border-radius:36px;overflow:hidden;background:radial-gradient(ellipse at 50% 0%,${GAME.violet2},${GAME.violet});box-shadow:0 0 0 2px rgba(255,255,255,.22);flex:none">
    ${confetti(10, ms + 3, .8, 560, 620)}
    <div style="position:absolute;right:24px;top:20px;font-size:28px;font-weight:700;color:#D9CCFF">${ms} ms</div>
    <div style="position:absolute;left:36px;right:36px;top:76px;height:120px;border-radius:28px;background:${GAME.green};opacity:${ms === 0 ? 1 : .35};display:flex;align-items:center;gap:20px;padding:0 24px;color:#fff;box-shadow:${ms === 0 ? `0 0 0 6px #fff,0 0 60px ${GAME.green}` : 'none'}"><div style="font-size:70px;${heavy}">٥</div><div style="flex:1"></div>${ms === 0 ? `<div style="width:60px;height:60px;border-radius:30px;background:#fff;color:${GAME.green};display:flex;align-items:center;justify-content:center">${ic('check', 38, 3.2)}</div>` : ''}</div>
    ${token}
    <div style="position:absolute;left:0;right:0;bottom:88px;display:flex;justify-content:center">
      <div style="display:flex;align-items:center;gap:16px;height:128px;padding:0 28px;border-radius:30px;background:#fff;color:${GAME.ink};box-shadow:0 0 0 5px ${GAME.pink},0 24px 40px -10px ${GAME.pink}aa;transform:${bump ? 'translateY(-16px) scale(1.1)' : 'none'}">${avatar(0, 88)}<div><div style="font-size:30px;font-weight:800">الصقور</div><div style="font-size:66px;${heavy};line-height:1;color:${GAME.pink};filter:brightness(.82)">${scoreNow}</div></div></div></div>
    ${bump ? `<div style="position:absolute;left:50%;bottom:112px;width:300px;height:180px;margin-left:-150px;border-radius:50%;box-shadow:0 0 0 6px ${GAME.pink}66, 0 0 0 30px ${GAME.pink}2a"></div>` : ''}
    <div style="position:absolute;left:24px;right:24px;bottom:18px;text-align:center;font-size:26px;color:#D9CCFF;line-height:1.4">${caption}</div></div>`;
  const tok = (x, y, s, o = 1) => `<div style="position:absolute;left:${x}px;top:${y}px;transform:scale(${s});opacity:${o};height:84px;padding:0 30px;border-radius:24px;background:#fff;color:${GAME.green};font-size:54px;${heavy};display:flex;align-items:center;box-shadow:0 20px 30px rgba(0,0,0,.4)">+١٠</div>`;
  return `<div style="position:absolute;inset:0;background:#0d0630"></div>
  ${at('left:96px;right:96px;top:54px;color:#fff;font-size:52px;font-weight:900;text-align:right', 'النقاط تطير من الإجابة إلى الفريق')}
  ${at('left:96px;right:96px;top:128px;color:#D9CCFF;font-size:28px;text-align:right', '٣ لقطات من حركة مدّتها ٦٠٠ مللي ثانية، بمنحنى cubic-bezier(.2,.9,.2,1.2): الانطلاق سريع، والهبوط يرتدّ قليلًا')}
  ${at('left:96px;right:96px;top:236px;display:flex;justify-content:space-between;direction:ltr', [
    frame(0, tok(190, 220, .6, 0), '٣٠', false, '١ · الإجابة تلمع ويظهر +١٠'),
    frame(300, tok(210, 330, 1.05), '٣٠', false, '٢ · +١٠ تطير نحو الفريق وتكبر قليلًا'),
    frame(600, tok(190, 330, .7, 0), '٤٠', true, '٣ · تحطّ: الرقم يقفز ٣٠ ← ٤٠ والبطاقة ترتدّ'),
  ].join(''))}`;
}

/* ---------------------------------------------------------------- memory pairs */
function memory() {
  const sym = [['star', GAME.yellow], ['music', GAME.pink], ['sun', '#FF8A3D'], ['moon', GAME.blue], ['mosque', GAME.green], ['game', '#B07CFF'], ['photo', '#2CC6E8'], ['movie', '#FF6E9A']];
  const state = ['m', 'd', 'd', 'f', 'd', 'm', 'd', 'd', 'd', 'd', 'f', 'd', 'm', 'd', 'd', 'd'];
  const face = [0, 3, 5, 1, 6, 1, 2, 4, 7, 5, 1, 7, 0, 2, 4, 3];
  const cards = state.map((s, i) => {
    const [g, c] = sym[face[i] % 8], focus = i === 6;
    if (s === 'd') return `<div style="height:168px;border-radius:30px;background:linear-gradient(150deg,#5B3DDB,#2A1A7A);box-shadow:${focus ? '0 0 0 8px #fff,0 26px 50px -6px rgba(140,110,255,.9)' : '0 10px 0 rgba(0,0,0,.25)'};transform:${focus ? 'scale(1.07) translateY(-8px)' : 'none'};display:flex;align-items:center;justify-content:center;color:rgba(255,255,255,.28)">${ic('star', 64, 1.6)}</div>`;
    const m = s === 'm';
    return `<div style="height:168px;border-radius:30px;background:${m ? 'rgba(255,255,255,.1)' : '#fff'};color:${c};display:flex;align-items:center;justify-content:center;opacity:${m ? .45 : 1};box-shadow:${m ? `inset 0 0 0 4px ${c}` : `0 0 0 6px ${c},0 22px 40px -8px ${c}`};transform:${s === 'f' ? 'rotate(-4deg) scale(1.06)' : 'none'}">${ic(g, 88, 2.2)}</div>`;
  }).join('');
  return `${gameBg(33, 36)}
  ${at('right:96px;top:54px;text-align:right', `<div style="font-size:84px;${heavy};color:#fff;text-shadow:0 8px 0 rgba(0,0,0,.25)">ذاكرة الأزواج</div><div style="font-size:34px;font-weight:600;color:#D9CCFF">دور <span style="color:${GAME.pink}">الصقور</span> · وجدوا ٢ من ٨ أزواج</div>`)}
  ${at('left:96px;top:54px;display:flex;gap:20px;direction:ltr', [1, 0].map(i => avatar(i, 100, i === 0)).join(''))}
  ${at('left:380px;right:380px;top:236px;display:grid;grid-template-columns:repeat(4,1fr);gap:26px', cards)}`;
}

/* ---------------------------------------------------------------- the Arabic word game */
function wordGame() {
  const G = GAME.green, Y = GAME.yellow, N = 'rgba(255,255,255,.22)';
  const rows = [['كتابة', [N, Y, N, N, N]], ['مدينة', [G, N, N, N, Y]], ['مدرسة', [G, G, G, G, G]], ['', []], ['', []], ['', []]];
  const board = rows.map(([w, f]) => `<div style="display:flex;gap:14px;direction:rtl">${[0, 1, 2, 3, 4].map(i => {
    const ch = w ? [...w][i] : '', c = f[i] || 'rgba(255,255,255,.08)', filled = !!w;
    return `<div style="width:112px;height:112px;border-radius:26px;background:${c};color:${c === Y ? GAME.ink : '#fff'};font-size:76px;${heavy};display:flex;align-items:center;justify-content:center;box-shadow:${filled ? '0 8px 0 rgba(0,0,0,.2)' : 'inset 0 0 0 3px rgba(255,255,255,.18)'}">${ch}</div>`;
  }).join('')}</div>`).join('');
  const letters = ['ا', 'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'و', 'ي'];
  const used = { 'ك': N, 'ت': N, 'ا': N, 'ب': N, 'م': G, 'د': G, 'ي': N, 'ن': N, 'ر': G, 'س': G };
  const keys = letters.map(l => { const f = l === 'ع'; return `<div style="height:84px;border-radius:20px;background:${f ? '#fff' : used[l] === G ? G : used[l] ? 'rgba(255,255,255,.1)' : 'rgba(255,255,255,.2)'};color:${f ? GAME.ink : used[l] === N ? 'rgba(255,255,255,.4)' : '#fff'};font-size:46px;font-weight:800;display:flex;align-items:center;justify-content:center;transform:${f ? 'scale(1.18)' : 'none'};box-shadow:${f ? `0 0 0 6px ${GAME.pink},0 20px 30px rgba(0,0,0,.4)` : '0 6px 0 rgba(0,0,0,.2)'};z-index:${f ? 2 : 0}">${l}</div>`; }).join('');
  return `${gameBg(44, 30)}
  ${at('right:96px;top:54px;text-align:right', `<div style="font-size:84px;${heavy};color:#fff;text-shadow:0 8px 0 rgba(0,0,0,.25)">خمّن الكلمة</div><div style="font-size:34px;font-weight:600;color:#D9CCFF">كلمة من خمسة حروف · حاولت ٣ من ٦</div>`)}
  ${at('left:96px;top:236px;display:flex;flex-direction:column;gap:14px', board)}
  ${at('right:96px;top:248px;width:760px;text-align:right', `<div style="font-size:72px;${heavy};color:${G};line-height:1.2">أحسنت!</div><div style="font-size:38px;font-weight:600;color:#fff;line-height:1.7;margin-top:8px">«مدرسة»: وجدتها في المحاولة الثالثة.<br>الأخضر في مكانه، والأصفر في الكلمة لكن في غير مكانه.</div>`)}
  ${at('right:96px;bottom:54px;width:780px;display:grid;grid-template-columns:repeat(7,1fr);gap:12px;direction:rtl', keys)}`;
}
