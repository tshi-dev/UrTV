/* Studies: for each look, three divergent directions at thumbnail size (A is the one taken to full size), and the focus motion strip. */
const TOK = {
  lawh: { name: 'لوح', bg: `${photo('blue-ridges')} center/cover`, veil: 'linear-gradient(180deg,rgba(6,12,34,.5),rgba(4,8,24,.7))', panel: 'background:linear-gradient(180deg,rgba(255,255,255,.22),rgba(255,255,255,.08));backdrop-filter:blur(30px);box-shadow:inset 0 1.5px 0 rgba(255,255,255,.4);border-radius:32px', tile: 'border-radius:40px;box-shadow:0 14px 24px -10px rgba(0,0,0,.55),inset 0 1.5px 0 rgba(255,255,255,.35)', foc: 'transform:translateY(-24px) scale(1.16);box-shadow:0 44px 70px -18px #f59e0baa', text: '#fff', clock: 200, font: FONT_SANS, colorTiles: true },
  majlis: { name: 'مجلس', bg: '#F2E8D4', veil: 'transparent', panel: 'background:#F7F0E0;border-radius:10px;box-shadow:inset 0 0 0 2px #B98A3C', tile: 'background:#F2E8D4;border-radius:10px;box-shadow:inset 0 0 0 2px #B98A3C;color:#2A2017', foc: 'background:#1E5A44;color:#F2E8D4;box-shadow:0 0 0 4px #B98A3C', text: '#2A2017', clock: 210, font: FONT_SERIF, colorTiles: false },
  cinema: { name: 'سينما', bg: `${photo('haramain-night')} 40% center/cover`, veil: 'linear-gradient(90deg,#050505,rgba(5,5,5,.5))', panel: 'background:rgba(255,255,255,.1);backdrop-filter:blur(20px);border-radius:14px', tile: 'border-radius:14px;background:rgba(255,255,255,.08)', foc: 'box-shadow:0 0 0 4px #fff;transform:scale(1.1)', text: '#fff', clock: 110, font: FONT_SERIF, colorTiles: false },
  frame: { name: 'إطار', bg: 'radial-gradient(ellipse at 50% 0%,#3a3631,#100f0e)', veil: 'transparent', panel: 'background:#EDE7DA;border-radius:2px;box-shadow:0 0 0 12px #17130f', tile: 'background:#EDE7DA;border-radius:2px;color:#26221c', foc: 'box-shadow:0 0 0 6px #B98A3C', text: '#EDE7DA', clock: 90, font: FONT_SERIF, colorTiles: false },
  dial: { name: 'يوم', bg: PHASES[2].bg, veil: 'transparent', panel: 'background:rgba(60,18,60,.42);backdrop-filter:blur(24px);border-radius:30px;box-shadow:inset 0 0 0 1.5px rgba(255,214,170,.38)', tile: 'background:rgba(60,18,60,.42);border-radius:30px;box-shadow:inset 0 0 0 1.5px rgba(255,214,170,.38)', foc: 'background:#FFF3E4;color:#2b1456', text: '#FFF3E4', clock: 170, font: FONT_SANS, colorTiles: false },
  dots: { name: 'نقاط', bg: '#000', veil: 'transparent', panel: 'box-shadow:inset 0 0 0 1.5px #3b3b3b;border-radius:0', tile: 'box-shadow:inset 0 0 0 1.5px #3b3b3b;border-radius:0', foc: 'background:#F4F4F2;color:#000', text: '#F4F4F2', clock: 150, font: FONT_MONO, colorTiles: false },
  blocks: { name: 'ألوان', bg: '#F5EFE3', veil: 'transparent', panel: 'background:#FFD23F;border:6px solid #121212;border-radius:32px;box-shadow:10px 10px 0 #121212', tile: 'background:#2D5BFF;border:6px solid #121212;border-radius:26px', foc: 'transform:translate(-8px,-8px);box-shadow:12px 12px 0 #121212', text: '#121212', clock: 200, font: FONT_SANS, colorTiles: false },
};
const TILE_C = ['#E5484D', '#3B82F6', '#14B8A6', '#F59E0B', '#8B5CF6', '#10B981', '#EC4899'];
/* two other layouts the same look can take */
function layout(k, t, kind) {
  const base = `<div style="position:absolute;inset:0;background:${t.bg}"></div><div style="position:absolute;inset:0;background:${t.veil}"></div>`;
  const tile = (i, f, big) => `<div style="${t.tile};${f ? t.foc : ''};${t.colorTiles ? `background:linear-gradient(155deg,${TILE_C[i]},color-mix(in srgb,${TILE_C[i]} 50%,#000))` : ''};display:flex;align-items:center;justify-content:center;color:${f && !t.colorTiles ? '' : t.text};flex:1;height:${big || 150}px">${ic(APPS[i][1], 52, 1.7)}</div>`;
  const clock = (css, sz) => at(css, `<div style="font-family:${t.font};font-size:${sz}px;font-weight:200;line-height:1;color:${t.text}">٩:٥٧</div>`);
  if (kind === 'b') {   // B: centred, three wide cards in the middle, a small dock of icons
    return base + clock('left:0;right:0;top:90px;text-align:center', t.clock * 1.15) +
      at('left:200px;right:200px;top:430px;display:flex;gap:28px', [0, 1, 2].map(i => `<div style="${t.panel};height:330px;flex:1;display:flex;flex-direction:column;justify-content:flex-end;padding:30px;color:${t.text}"><div style="font-size:44px;font-weight:700">${['العصر', '٢٨°', 'الصور'][i]}</div><div style="font-size:28px;opacity:.8">${['بعد ساعتين', 'صافٍ', 'ألبوم'][i]}</div></div>`).join('')) +
      at('left:50%;transform:translateX(-50%);bottom:54px;width:760px;display:flex;gap:14px', [0, 1, 2, 3, 4].map(i => tile(i, i === 2, 96)).join(''));
  }
  // C: split, a tall picture on one side, a vertical list on the other
  return base + at('left:0;top:0;width:900px;height:1080px;background:' + (t.bg.includes('url') ? t.bg : photo('sea-sunset') + ' center/cover'), '') +
    at('left:96px;top:54px', `<div style="font-family:${t.font};font-size:${t.clock * .7}px;font-weight:200;color:#fff;text-shadow:0 4px 30px rgba(0,0,0,.5)">٩:٥٧</div>`) +
    at('right:96px;top:150px;width:760px;display:flex;flex-direction:column;gap:18px', [0, 1, 2, 3, 4].map(i => `<div style="${t.tile};${i === 1 ? t.foc : ''};height:116px;display:flex;align-items:center;gap:28px;padding:0 36px;color:${i === 1 && !t.colorTiles ? '' : t.text};font-size:40px;font-weight:600;${t.colorTiles ? 'background:rgba(255,255,255,.14)' : ''}">${ic(APPS[i][1], 46, 1.7)}${APPS[i][0]}</div>`).join(''));
}
const CHOSEN = { lawh, majlis, cinema, frame, dial: () => dial({ ph: 2 }), dots, blocks };
const WHY = {
  lawh: ['أ · طبقات: صورة، زجاج، بلاطات تقفز نحوك وتميل (المختار)', 'ب · ساعة في الوسط وبطاقات عريضة وشريط أيقونات', 'ج · نصف صورة ونصف قائمة عمودية'],
  majlis: ['أ · قوس من الصورة وخط ثلث ضخم ورفّ خشبي (المختار)', 'ب · ساعة في الوسط فوق بطاقات ورقية', 'ج · صورة بنصف الشاشة وقائمة ورقية'],
  cinema: ['أ · بطل كبير ورفوف صور بلا لوحات (المختار)', 'ب · ساعة وبطاقات في الوسط', 'ج · صورة ممتدة وقائمة جانبية'],
  frame: ['أ · لوحة بإطار وحاشية ولوحة نحاسية (المختار)', 'ب · لوحة صغيرة وساعة كبيرة', 'ج · لوحة نصف الشاشة وقائمة'],
  dial: ['أ · قرص السماء: الصلوات على قوس والشمس تتحرك (المختار)', 'ب · بطاقات وسطية ولون اليوم في الخلفية فقط', 'ج · نصف صورة ونصف قائمة بلون الوقت'],
  dots: ['أ · ساعة من نقاط وصورة بنقاط نصفية وإشارة حمراء (المختار)', 'ب · ساعة وسطية وثلاث بطاقات مستطيلة', 'ج · عمود قائمة وصورة'],
  blocks: ['أ · قوالب ملوّنة بحواف سميكة وظل صلب يتحرك (المختار)', 'ب · قوالب في الوسط وشريط أيقونات', 'ج · قائمة قوالب وصورة'],
};
function studySheet(k) {
  const t = TOK[k], cell = (html, cap, on, w) => `<div style="width:${w}px"><div style="${on ? 'box-shadow:0 0 0 6px #fff;border-radius:14px' : ''};width:${w}px">${mini(html, w)}</div><div style="font-size:26px;color:${on ? '#fff' : '#B7B7C8'};margin-top:20px;line-height:1.5;font-weight:${on ? 700 : 400}">${cap}</div></div>`;
  return `<div style="position:absolute;inset:0;background:#15151c"></div>
  ${at('right:96px;top:54px;font-size:56px;font-weight:800;color:#fff', `${t.name} · ثلاثة اتجاهات`)}
  ${at('right:96px;top:170px;width:1000px', cell(CHOSEN[k](), WHY[k][0], true, 1000))}
  ${at('left:96px;top:170px;width:592px', cell(layout(k, t, 'b'), WHY[k][1], false, 592))}
  ${at('left:96px;top:600px;width:592px', cell(layout(k, t, 'c'), WHY[k][2], false, 592))}
  ${at('right:96px;top:790px;width:1000px;font-size:26px;color:#8e8ea3;line-height:1.6', 'الاتجاهان ب وج بخامة الشكل نفسها لكن بتركيب مختلف؛ اخترنا أ لأن التسلسل (الساعة ← الصلاة ← التطبيقات) أوضح فيه.')}`;
}

/* ---------------------------------------------------------------- the focus move in «لوح»: lift, tilt, glow, and the label hands over from one tile to the next in 280 ms */
function focusStrip() {
  const tile = (i, a) => {
    const c = APPS[i][2], n = APPS[i][0], g = APPS[i][1], sz = 168, hex = Math.round(60 + 110 * a).toString(16).padStart(2, '0');
    return `<div style="width:${sz}px;display:flex;flex-direction:column;align-items:center;transform:translateY(${-28 * a}px) scale(${1 + .16 * a});position:relative;z-index:${a > .5 ? 2 : 1}">
      <div style="width:${sz}px;height:${sz}px;border-radius:40px;position:relative;overflow:hidden;background:linear-gradient(155deg,${c},color-mix(in srgb,${c} 55%,#000));box-shadow:0 ${14 + 30 * a}px ${24 + 46 * a}px -${10 + 8 * a}px ${a > .05 ? c + hex : 'rgba(0,0,0,.5)'},inset 0 2px 0 rgba(255,255,255,${.35 + .2 * a});transform:perspective(900px) rotateX(${7 * a}deg) rotateY(${-9 * a}deg)">
        <div style="position:absolute;inset:0;background:linear-gradient(180deg,rgba(255,255,255,.34),rgba(255,255,255,0) 46%)"></div>
        <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:#fff;transform:translate(${-7 * a}px,${-7 * a}px)">${ic(g, 76, 1.7)}</div></div>
      <div style="margin-top:${18 + 12 * a}px;font-size:${24 + 4 * a}px;font-weight:${a > .5 ? 600 : 400};color:#fff;white-space:nowrap;opacity:${.8 + .2 * a}">${n}</div></div>`;
  };
  const strip = (ms, amts, cap) => `<div style="position:relative;width:576px;height:440px;border-radius:28px;overflow:hidden;flex:none;background:${photo('blue-ridges')} center/cover;box-shadow:0 0 0 2px rgba(255,255,255,.25)">
    <div style="position:absolute;inset:0;background:linear-gradient(180deg,rgba(6,12,34,.2),rgba(4,8,24,.75))"></div>
    <div style="position:absolute;right:22px;top:16px;font-size:28px;font-weight:700;color:#fff">${ms} ms</div>
    <div style="position:absolute;left:8px;right:8px;bottom:84px;display:flex;justify-content:space-between;direction:ltr;transform:scale(.9);transform-origin:bottom center">${[1, 2, 3].map(j => tile(j, amts[j - 1])).join('')}</div>
    <div style="position:absolute;left:0;right:0;bottom:16px;text-align:center;font-size:24px;color:#fff;opacity:.9">${cap}</div></div>`;
  return `<div style="position:absolute;inset:0;background:#0f1018"></div>
  ${at('left:96px;right:96px;top:54px;color:#fff;font-size:52px;font-weight:900;text-align:right', 'التركيز في «لوح»: يرتفع ويميل ويتوهّج')}
  ${at('left:96px;right:96px;top:128px;color:#B7B7C8;font-size:28px;text-align:right;line-height:1.6', 'من البلاطة الثانية إلى الثالثة في ٢٨٠ مللي ثانية بمنحنى cubic-bezier(.2,.8,.2,1): تهبط الأولى وتصغر، وترتفع الثانية وتميل ٧° وتكبر ١٦٪ ويكبر اسمها، وتتحرك الأيقونة ٧ بكسل عكس الميل فيظهر العمق (parallax)')}
  ${at('left:96px;right:96px;top:330px;display:flex;justify-content:space-between;direction:ltr', [
    strip(0, [0, 1, 0], 'قبل: البلاطة ٢ في التركيز'), strip(140, [0, .5, .5], 'في الطريق: تسلّم الأولى الثانية'), strip(280, [0, 0, 1], 'بعد: البلاطة ٣ مستقرة بتوهّجها')].join(''))}`;
}
