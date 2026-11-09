/* «مركز التحكم» as a compact side panel (design/43, round 3): slides in from the start edge, the home stays visible behind it. Rest, focused, customise, light. */
Object.assign(ICONS, {
  bt: '<path d="M7 7l10 10-5 4V3l5 4L7 17"/>',
  power: '<path d="M12 3v8"/><path d="M6.3 6.8a8 8 0 1011.4 0"/>',
  timer: '<circle cx="12" cy="13" r="7.5"/><path d="M12 9v4l2.5 2M9.5 3h5"/>',
  speaker: '<path d="M4 9.5h3.5L12 5.5v13l-4.5-4H4z"/><path d="M15.5 9a4 4 0 010 6M18 6.5a8 8 0 010 11"/>',
  gauge: '<path d="M4 17a8.5 8.5 0 1116 0"/><path d="M12 17l4-5"/>',
  screensaver: '<rect x="3" y="4.5" width="18" height="12.5" rx="2.5"/><path d="M8 20.5h8M9.5 9.5l1.5-1.5 1.5 1.5 1.5-1.5"/>',
  up: '<path d="M6 14l6-6 6 6"/>', down: '<path d="M6 10l6 6 6-6"/>', drag: '<path d="M9 7h.01M9 12h.01M9 17h.01M15 7h.01M15 12h.01M15 17h.01"/>',
  chevL: '<path d="M15 5l-7 7 7 7"/>',
});
const CC = {
  dark: { panel: 'linear-gradient(180deg,rgba(34,38,52,.78),rgba(18,20,30,.86))', line: 'rgba(255,255,255,.16)', text: '#F4F6FB', sub: '#B7BDCE', accent: '#7FB2FF', foc: '#7FB2FF', focInk: '#0B1530', tile: 'rgba(255,255,255,.08)', veil: 'rgba(4,8,20,.34)', shadow: '0 40px 80px -20px rgba(0,0,0,.7)' },
  light: { panel: 'linear-gradient(180deg,rgba(255,255,255,.86),rgba(246,248,252,.9))', line: 'rgba(20,24,34,.12)', text: '#161B26', sub: '#515A6E', accent: '#0B5E8F', foc: '#0B5E8F', focInk: '#FFFFFF', tile: 'rgba(20,24,34,.06)', veil: 'rgba(255,255,255,.18)', shadow: '0 40px 80px -24px rgba(30,40,70,.45)' },
};
function ccPanel(mode, state) {
  const T = CC[mode];
  const toggles = [['bt', 'بلوتوث', true], ['screensaver', 'الشاشة الآن', false], ['timer', 'النوم', false], ['power', 'إيقاف', false]];
  const rows = [['wifi', 'الشبكة', 'ATS5G'], ['speaker', 'مخرج الصوت', 'سماعات التلفزيون'], ['gauge', 'اختبار السرعة', '٩٢ م.ب/ث'], ['sliders', 'إعدادات التلفزيون', ''], ['tv', 'إعدادات UrTV', '']];
  const focusT = state.focus === 't0' ? 0 : state.focus && state.focus[0] === 't' ? +state.focus[1] : -1, focusR = state.focus && state.focus[0] === 'r' ? +state.focus.slice(1) : -1;
  const tog = toggles.map(([g, n, on], i) => {
    const f = i === focusT;
    return `<div style="display:flex;flex-direction:column;align-items:center;gap:10px;width:96px"><div style="width:80px;height:80px;border-radius:40px;display:flex;align-items:center;justify-content:center;
      background:${f ? T.foc : on ? T.accent : T.tile};color:${f ? T.focInk : on ? T.focInk : T.text};box-shadow:${f ? `0 0 0 4px ${T.panel.includes('255,255,255') ? '#fff' : 'rgba(255,255,255,.9)'},0 14px 28px -8px ${T.accent}` : `inset 0 0 0 1.5px ${T.line}`};transform:${f ? 'scale(1.08)' : 'none'}">${ic(g, 34, 1.8)}</div>
      <div style="font-size:22px;color:${T.text};white-space:nowrap;font-weight:${f ? 700 : 500}">${n}</div></div>`;
  }).join('');
  const list = rows.map(([g, n, v], i) => {
    const f = i === focusR;
    return `<div style="height:76px;border-radius:20px;padding:0 22px;display:flex;align-items:center;gap:16px;background:${f ? T.foc : 'transparent'};color:${f ? T.focInk : T.text};${f ? `box-shadow:0 14px 26px -12px ${T.accent}` : ''}">
      <div style="opacity:${f ? 1 : .9}">${ic(g, 34, 1.7)}</div><div style="font-size:27px;font-weight:${f ? 700 : 500};flex:1;white-space:nowrap">${n}</div>
      ${v ? `<div style="font-size:23px;color:${f ? T.focInk : T.sub};white-space:nowrap;opacity:${f ? .9 : 1}">${v}</div>` : `<div style="opacity:.55">${ic('chevL', 26, 2)}</div>`}</div>`;
  }).join('<div style="height:1px;margin:0 24px;background:' + T.line + '"></div>');
  const header = (t, right) => `<div style="display:flex;align-items:center;justify-content:space-between;height:64px"><div style="font-size:36px;font-weight:800;color:${T.text}">${t}</div>${right}</div>`;
  const cust = state.focus === 'c';
  const body = state.custom ? customise(T, state) : `
      ${header('مركز التحكم', `<div style="height:48px;padding:0 20px;border-radius:14px;display:flex;align-items:center;font-size:24px;font-weight:600;${cust ? `background:${T.foc};color:${T.focInk}` : `color:${T.accent}`}">تخصيص</div>`)}
      <div style="display:flex;justify-content:space-between;margin:28px 0 20px;padding:0 4px">${tog}</div>
      <div style="height:1px;background:${T.line};margin-bottom:12px"></div>
      <div style="display:flex;flex-direction:column;gap:6px">${list}</div>`;
  return `<div style="position:absolute;inset:0;background:${T.veil}"></div>
  ${at('right:54px;top:54px;bottom:54px;width:560px;border-radius:36px;padding:32px 28px;background:' + T.panel + ';backdrop-filter:blur(40px) saturate(1.5);box-shadow:inset 0 0 0 1.5px ' + T.line + ',inset 0 1.5px 0 rgba(255,255,255,.35),' + T.shadow, body)}`;
}
function customise(T, st) {
  const items = [['wifi', 'الشبكة', 1], ['speaker', 'مخرج الصوت', 1], ['gauge', 'اختبار السرعة', 1], ['sliders', 'إعدادات التلفزيون', 1], ['tv', 'إعدادات UrTV', 1], ['bt', 'بلوتوث', 1], ['screensaver', 'الشاشة الآن', 0], ['timer', 'مؤقت النوم', 0], ['power', 'إيقاف التشغيل', 0]];
  const fi = st.row ?? 1, on = items.filter(i => i[2]).length;
  const rows = items.map(([g, n, c], i) => {
    const f = i === fi;
    return `<div style="height:68px;border-radius:18px;padding:0 18px;display:flex;align-items:center;gap:16px;background:${f ? T.foc : 'transparent'};color:${f ? T.focInk : T.text};opacity:${c || f ? 1 : .62}">
      <div style="width:32px;height:32px;border-radius:9px;display:flex;align-items:center;justify-content:center;${c ? `background:${f ? T.focInk : T.accent};color:${f ? T.foc : T.focInk}` : `box-shadow:inset 0 0 0 2px ${f ? T.focInk : T.sub}`}">${c ? ic('check', 22, 3) : ''}</div>
      ${ic(g, 30, 1.7)}<div style="font-size:26px;font-weight:${f ? 700 : 500};flex:1;white-space:nowrap">${n}</div>
      ${f ? `<div style="display:flex;gap:6px;color:${T.focInk}">${ic('up', 30, 2.2)}${ic('down', 30, 2.2)}</div>` : `<div style="opacity:.4">${ic('drag', 26, 2.6)}</div>`}</div>`;
  }).join('');
  return `<div style="display:flex;align-items:center;gap:14px;height:64px;color:${T.text}"><div style="opacity:.8">${ic('chevL', 30, 2).replace('<svg', '<svg style="transform:scaleX(-1)"')}</div><div style="font-size:36px;font-weight:800;flex:1">تخصيص</div><div style="font-size:24px;color:${T.sub}">${String(on).replace(/\d/g, d => '٠١٢٣٤٥٦٧٨٩'[d])} من ٦</div></div>
    <div style="margin:10px 0 14px;font-size:22px;color:${T.sub}">اختر ما يظهر ورتّبه بالأسهم</div>
    <div style="display:flex;flex-direction:column;gap:4px">${rows}</div>
    <div style="position:absolute;left:28px;right:28px;bottom:28px;display:flex;gap:12px">
      <div style="flex:1;height:64px;border-radius:18px;display:flex;align-items:center;justify-content:center;background:${T.accent};color:${T.focInk};font-size:28px;font-weight:800">تم</div>
      <div style="height:64px;padding:0 24px;border-radius:18px;display:flex;align-items:center;font-size:24px;color:${T.sub};box-shadow:inset 0 0 0 1.5px ${T.line}">إعادة الضبط</div></div>`;
}
const ccBg = mode => (mode === 'dark' ? lawh({ focus: -1 }) : dial({ ph: 1, focus: -1 }));
const SCREENS_CC = {
  'c-01-control-center-rest': () => ccBg('dark') + ccPanel('dark', { focus: 't0' }),
  'c-02-control-center-focus': () => ccBg('dark') + ccPanel('dark', { focus: 'r1' }),
  'c-03-control-center-customise': () => ccBg('dark') + ccPanel('dark', { custom: true, row: 1 }),
  'c-04-control-center-light': () => ccBg('light') + ccPanel('light', { focus: 'r2' }),
};
Object.assign(SCREENS, SCREENS_CC);
