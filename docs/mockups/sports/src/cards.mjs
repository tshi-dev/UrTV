// The sports cards of «الرياضة» (mockups only): each card as the big card (شكل تلفزيونك: the home's card, the screensaver «معلومات»),
// the small card of the couch home's row, and the line of the couch screensaver's bar. Sample data: the Saudi league, round 11, invented players.
const T = {   // short names and two colours (no crest is bundled)
  hilal: ['الهلال', '#1E4FA1', '#FFFFFF'], nassr: ['النصر', '#F7C600', '#1B3A8A'], ittihad: ['الاتحاد', '#F2C200', '#111111'], ahli: ['الأهلي', '#0A7A3A', '#FFFFFF'],
  shabab: ['الشباب', '#FFFFFF', '#1A1A1A'], ettifaq: ['الاتفاق', '#0B8A3E', '#D4232B'], qadsiah: ['القادسية', '#C8102E', '#F2C200'], fateh: ['الفتح', '#1D6B3A', '#FFFFFF'],
  taawoun: ['التعاون', '#F2C200', '#2B5BA8'], khaleej: ['الخليج', '#F2C200', '#0A7A3A'], riyadh: ['الرياض', '#111111', '#E2001A'], fayha: ['الفيحاء', '#F28C00', '#1A1A1A'],
  damac: ['ضمك', '#E30613', '#111111'], hazem: ['الحزم', '#E30613', '#FFFFFF'], okhdood: ['الأخدود', '#5B2C83', '#FFFFFF'], kholood: ['الخلود', '#C8102E', '#FFFFFF'],
  sadd: ['السد', '#111111', '#FFFFFF'], ain: ['العين', '#6B2C91', '#FFFFFF'], sepahan: ['سباهان', '#F2C200', '#111111'], shabab_ahli: ['شباب الأهلي', '#C8102E', '#111111'],
  sharjah: ['الشارقة', '#ffffff', '#C8102E'], jordan: ['الأردن', '#FFFFFF', '#C8102E'], ksa: ['السعودية', '#0A7A3A', '#FFFFFF'],
};
const n = k => T[k][0];
const sw = k => `<i class="sw" style="--a:${T[k][1]};--b:${T[k][2]}"></i>`;
const band = k => `<i class="band" style="--a:${T[k][1]};--b:${T[k][2]}"></i>`;
const form = s => `<span class="form">${[...s].map(c => `<i class="${c}"></i>`).join('')}</span>`;
const nm = k => `${sw(k)} ${n(k)}`;

export const CARDS = [
  { id: '01-matchday', title: 'مباراة اليوم',
    states: {
      before: { tv: `<h3>مباراة اليوم<span>دوري روشن، الجولة ١١</span></h3>
        <div class="row" style="gap:18px;margin-top:6px"><div class="team">${band('hilal')}<b class="big">${n('hilal')}</b><span class="s">الأول، ٣٠ نقطة</span>${form('wwdww')}</div>
        <div class="team"><span class="s">تبدأ بعد</span><b class="huge">٣:١٢</b><span class="s">ساعة ودقيقة</span></div>
        <div class="team">${band('nassr')}<b class="big">${n('nassr')}</b><span class="s">الثاني، ٢٦ نقطة</span>${form('wdwlw')}</div></div>
        <div class="boxes" style="grid-template-columns:repeat(3,1fr);margin-top:12px"><div class="box"><div class="s">الانطلاقة</div><b>٨:٠٠ م</b></div><div class="box"><div class="s">الملعب</div><b>المملكة أرينا</b></div><div class="box"><div class="s">القناة</div><b>ثمانية</b></div></div>`,
        couch: ['مباراة اليوم', `${n('hilal')} × ${n('nassr')}`, '٨:٠٠ م، بعد ٣ ساعات و١٢ دقيقة'], bar: ['مباراة اليوم', `${n('hilal')} × ${n('nassr')}`, '٨:٠٠ م، بعد ٣ ساعات'] },
      live: { tv: `<h3>مباراة اليوم<span class="live">مباشر</span></h3>
        <div class="row" style="gap:22px;margin-top:6px"><div class="team">${band('hilal')}<b class="big">${n('hilal')}</b></div>
        <div class="team"><b class="huge">٢ - ١</b><span class="s live">الدقيقة ٦٧</span></div><div class="team">${band('nassr')}<b class="big">${n('nassr')}</b></div></div>
        <div style="margin:12px 6px 0;height:6px;border-radius:3px;background:rgba(255,255,255,.12)"><div style="width:74%;height:100%;border-radius:3px;background:var(--gold)"></div></div>
        <div class="boxes" style="grid-template-columns:1fr 1fr;margin-top:12px"><div class="box"><div class="s">أهداف ${n('hilal')}</div><b>سالم ١٢، فهد ٥٥</b></div><div class="box"><div class="s">أهداف ${n('nassr')}</div><b>ماجد ٣٨</b></div></div>`,
        couch: ['مباراة اليوم', `${n('hilal')} ٢ - ١ ${n('nassr')}`, 'مباشر، الدقيقة ٦٧'], bar: ['مباشر', `${n('hilal')} ٢ - ١ ${n('nassr')}`, 'الدقيقة ٦٧'] },
      after: { tv: `<h3>مباراة اليوم<span>انتهت</span></h3>
        <div class="row" style="gap:22px;margin-top:6px"><div class="team">${band('hilal')}<b class="big">${n('hilal')}</b><span class="s">فاز</span></div>
        <div class="team"><b class="huge">٣ - ١</b><span class="s">النتيجة النهائية</span></div><div class="team">${band('nassr')}<b class="big">${n('nassr')}</b></div></div>
        <div class="boxes" style="grid-template-columns:1fr 1fr;margin-top:14px"><div class="box"><div class="s">أهداف ${n('hilal')}</div><b>سالم ١٢، فهد ٥٥، تركي ٨٨</b></div><div class="box"><div class="s">أهداف ${n('nassr')}</div><b>ماجد ٣٨</b></div></div>
        <div class="s" style="margin-top:10px">${n('hilal')} الأول بـ٣٣ نقطة، بفارق ٧ عن ${n('nassr')}</div>`,
        couch: ['مباراة اليوم', `${n('hilal')} ٣ - ١ ${n('nassr')}`, 'انتهت، سالم ١٢، فهد ٥٥، تركي ٨٨'], bar: ['انتهت', `${n('hilal')} ٣ - ١ ${n('nassr')}`, 'سالم، فهد، تركي'] },
    } },
  { id: '02-live', title: 'مباشر الآن',
    tv: `<h3>مباشر الآن<span>فرقك ودورياتك</span></h3><div class="boxes" style="gap:6px">${[['hilal','nassr','٢ - ١','٦٧'],['ittihad','shabab','٠ - ٠','٣١'],['ahli','ettifaq','١ - ١','٥٨'],['qadsiah','fateh','٣ - ٠','٨٢']].map(([a,b,s,m],i)=>`<div class="box${i===0?' on':''}" style="display:grid;grid-template-columns:1fr auto 1fr 64px;align-items:center;padding:6px 10px"><b style="text-align:left">${nm(a)}</b><b style="font-size:17px;margin:0 14px">${s}</b><b style="text-align:right">${nm(b)}</b><span class="s live" ${i===0?'style="color:#9B2B21"':''}>الدقيقة ${m}</span></div>`).join('')}</div>`,
    couch: ['مباشر الآن', `${n('hilal')} ٢ - ١ ${n('nassr')}`, 'الدقيقة ٦٧، و٣ مباريات أخرى'], bar: ['مباشر', `${n('hilal')} ٢ - ١ ${n('nassr')}`, 'الدقيقة ٦٧، و٣ أخرى'] },
  { id: '03-table', title: 'الترتيب',
    tv: `<h3>دوري روشن<span>الجولة ١١</span></h3><div class="boxes" style="gap:3px">${[['١','hilal','٣٠','wwdww'],['٢','nassr','٢٦','wdwlw'],['٣','ittihad','٢٤','wwlwd'],['٤','ahli','٢٢','dwwlw'],null,['٧','taawoun','١٧','lwdwl'],['٨','ettifaq','١٦','dlwdw',1],['٩','fateh','١٥','wlldw']].map(r=>r?`<div class="box${r[4]?' on':''}" style="display:grid;grid-template-columns:22px 1fr 60px 60px;align-items:center;padding:2px 10px;text-align:right"><span>${r[0]}</span><b>${nm(r[1])}</b>${form(r[3])}<b style="text-align:left">${r[2]}</b></div>`:'<div class="s" style="line-height:8px">…</div>').join('')}</div>`,
    couch: ['الترتيب', `${n('ettifaq')} الثامن، ١٦ نقطة`, `بعد ${n('taawoun')} بنقطة، والأول ${n('hilal')} بـ٣٠`], bar: ['الترتيب', `${n('ettifaq')} الثامن`, '١٦ نقطة، والأول الهلال بـ٣٠'] },
  { id: '04-next', title: 'مباريات فرقك القادمة',
    tv: `<h3>مباريات فرقك القادمة</h3><div class="boxes" style="grid-template-columns:repeat(3,1fr)">${[['hilal','nassr','الجمعة','٩:٠٠ م','دوري روشن، ١١','على أرضه'],['ksa','jordan','الثلاثاء','٧:٣٠ م','كأس العرب','خارج أرضه'],['ettifaq','damac','السبت','٦:٠٠ م','دوري روشن، ١١','على أرضه']].map(([a,b,d,t,c,h])=>`<div class="box" style="padding:10px 6px">${band(a)}<b style="display:block;font-size:17px;margin-top:6px">${n(a)}</b><div class="s">× ${n(b)}</div><b style="display:block;font-size:19px;margin-top:8px">${d}</b><b style="display:block">${t}</b><div class="s" style="margin-top:6px">${c}</div><div class="s">${h}</div></div>`).join('')}</div>`,
    couch: ['مباريات فرقك القادمة', `${n('hilal')} × ${n('nassr')}`, 'الجمعة ٩:٠٠ م، على أرضه'], bar: ['القادمة', `${n('hilal')} × ${n('nassr')}`, 'الجمعة ٩:٠٠ م'] },
  { id: '05-yesterday', title: 'نتائج أمس',
    tv: `<h3>نتائج أمس<span>الأربعاء</span></h3><div class="boxes" style="gap:4px">${[['فرقك'],['hilal','٢ - ١','nassr',1],['دوري روشن'],['ittihad','٠ - ٠','shabab'],['ahli','٣ - ١','ettifaq'],['الدوري الإنجليزي'],['اسم','١ - ٢','اسم2']].map(r=>r.length===1?`<div class="s" style="text-align:right;margin-top:3px">${r[0]}</div>`:r[0]==='اسم'?`<div class="box" style="display:grid;grid-template-columns:1fr auto 1fr;padding:4px 10px"><b style="text-align:left">آرسنال</b><b style="margin:0 14px">١ - ٢</b><b style="text-align:right">تشيلسي</b></div>`:`<div class="box${r[3]?' on':''}" style="display:grid;grid-template-columns:1fr auto 1fr;padding:4px 10px"><b style="text-align:left">${nm(r[0])}</b><b style="margin:0 14px">${r[1]}</b><b style="text-align:right">${nm(r[2])}</b></div>`).join('')}</div>`,
    couch: ['نتائج أمس', `${n('hilal')} ٢ - ١ ${n('nassr')}`, 'و٧ نتائج من دورياتك'], bar: ['أمس', `${n('hilal')} ٢ - ١ ${n('nassr')}`, 'و٧ نتائج'] },
  { id: '06-h2h', title: 'المواجهات',
    tv: `<h3>قبل الكلاسيكو<span>الجمعة ٩:٠٠ م</span></h3><div class="row" style="gap:40px"><div class="team">${band('hilal')}<b class="big">${n('hilal')}</b></div><b class="s">آخر ٥ مواجهات</b><div class="team">${band('nassr')}<b class="big">${n('nassr')}</b></div></div>
      <div class="boxes" style="grid-template-columns:repeat(5,1fr);margin-top:12px">${[['٢ - ١','h','مايو ٢٦'],['١ - ١','d','فبراير ٢٦'],['٣ - ٠','h','أكتوبر ٢٥'],['٠ - ١','a','أبريل ٢٥'],['٢ - ٠','h','ديسمبر ٢٤']].map(([s,w,d])=>`<div class="box" style="border-top:4px solid ${w==='h'?T.hilal[1]:w==='a'?T.nassr[1]:'#9A9A9A'}"><b style="font-size:17px">${s}</b><div class="s">${d}</div></div>`).join('')}</div>
      <div style="margin-top:12px;font-size:15px"><b>${n('hilal')} ٣ فوز</b>، تعادل واحد، <b>${n('nassr')} ١</b></div>`,
    couch: ['قبل المباراة', `${n('hilal')} ٣ فوز، ${n('nassr')} ١`, 'وتعادل واحد في آخر ٥ مواجهات'], bar: ['آخر ٥ مواجهات', `${n('hilal')} ٣، ${n('nassr')} ١`, 'وتعادل واحد'] },
  { id: '07-weekend', title: 'مباريات نهاية الأسبوع',
    tv: `<h3>نهاية الأسبوع<span>دوري روشن، الجولة ١١</span></h3><div class="boxes" style="grid-template-columns:repeat(3,1fr);gap:8px">${[['الخميس',[['ittihad','shabab','٦:٠٠ م'],['fateh','riyadh','٨:٣٠ م']]],['الجمعة',[['hilal','nassr','٩:٠٠ م',1],['khaleej','okhdood','٦:٢٠ م']]],['السبت',[['ettifaq','damac','٦:٠٠ م'],['ahli','fayha','٨:٣٠ م'],['qadsiah','hazem','٩:٠٠ م']]]].map(([d,ms])=>`<div class="box" style="padding:8px 6px"><b style="color:var(--gold-3)">${d}</b>${ms.map(([a,b,t,f])=>`<div style="margin-top:6px;${f?'color:#1A1712;background:#EBC77F;border-radius:6px;padding:2px 0':''}"><div style="font-size:12px;font-weight:700">${n(a)} × ${n(b)}</div><div class="s" ${f?'style="color:#4A3F2A"':''}>${t}</div></div>`).join('')}</div>`).join('')}</div>`,
    couch: ['نهاية الأسبوع', '٧ مباريات في دوري روشن', `أولها الخميس ٦:٠٠ م، ${n('ittihad')} × ${n('shabab')}`], bar: ['نهاية الأسبوع', '٧ مباريات', 'الخميس ٦:٠٠ م أولها'] },
  { id: '08-cup', title: 'طريق الكأس',
    tv: `<h3>أبطال آسيا للنخبة<span>طريق ${n('hilal')}</span></h3><div class="boxes" style="grid-template-columns:repeat(4,1fr);gap:6px;align-items:center">
      <div class="boxes" style="gap:6px"><div class="s">دور الـ١٦</div><div class="box on" style="padding:5px"><b>${n('hilal')} ٣ - ١</b><div class="s">${n('sepahan')}</div></div><div class="box" style="padding:5px"><b>${n('sadd')} ٢ - ٠</b><div class="s">${n('sharjah')}</div></div></div>
      <div class="boxes" style="gap:6px"><div class="s">ربع النهائي</div><div class="box on" style="padding:8px 5px"><b>${n('hilal')} × ${n('sadd')}</b><div class="s">٨ أبريل</div></div></div>
      <div class="boxes" style="gap:6px"><div class="s">نصف النهائي</div><div class="box" style="padding:8px 5px;opacity:.75"><b>الفائز × ${n('ain')}</b><div class="s">أو ${n('shabab_ahli')}</div></div></div>
      <div class="boxes" style="gap:6px"><div class="s">النهائي</div><div class="box" style="padding:8px 5px;opacity:.6"><b>جدة</b><div class="s">٢ مايو</div></div></div></div>
      <div class="s" style="margin-top:12px">ذهاب وإياب حتى نصف النهائي، والنهائي مباراة واحدة</div>`,
    couch: ['أبطال آسيا للنخبة', `${n('hilal')} في ربع النهائي`, `× ${n('sadd')}، الأربعاء ٨ أبريل`], bar: ['آسيا', `${n('hilal')} في ربع النهائي`, `× ${n('sadd')}، ٨ أبريل`] },
  { id: '09-scorers', title: 'الهدافون',
    tv: `<h3>هدافو دوري روشن<span>لا مصدر مجاني موثوق</span></h3><div class="boxes" style="gap:4px">${[['١','سالم المطيري','hilal','١١'],['٢','ماجد الحربي','nassr','٩'],['٣','عبدالله الزهراني','ittihad','٨'],['٤','فهد الشمري','hilal','٧'],['٥','تركي العنزي','ahli','٧']].map((r,i)=>`<div class="box${i===0?' on':''}" style="display:grid;grid-template-columns:22px 1fr 1fr 40px;align-items:center;padding:4px 10px;text-align:right"><span>${r[0]}</span><b>${r[1]}</b><span class="s">${nm(r[2])}</span><b style="text-align:left">${r[3]}</b></div>`).join('')}</div><div class="s" style="margin-top:8px">أسماء مخترعة للنموذج</div>`,
    couch: ['الهدافون', 'سالم المطيري ١١ هدفًا', `${n('hilal')}، والثاني ماجد الحربي ٩`], bar: ['الهدافون', 'سالم المطيري ١١', `${n('hilal')}`] },
  { id: '10a-f1', title: 'فورمولا ١',
    tv: `<h3>فورمولا ١<span>بتوقيت الرياض</span></h3><div style="font-size:19px;font-weight:700">جائزة أبوظبي الكبرى</div><div class="s">حلبة ياس مارينا، الجولة الأخيرة</div>
      <div class="boxes" style="grid-template-columns:repeat(4,1fr);margin-top:12px">${[['التجارب الحرة','الجمعة','٣:٣٠ م'],['التأهيل','السبت','٥:٠٠ م'],['السباق','الأحد','٤:٠٠ م',1],['يبدأ بعد','','٣ أيام']].map(([a,b,c,on])=>`<div class="box${on?' on':''}"><div class="s">${a}</div><b style="display:block">${b||'&nbsp;'}</b><b style="display:block;font-size:17px">${c}</b></div>`).join('')}</div>`,
    couch: ['فورمولا ١', 'جائزة أبوظبي الكبرى', 'السباق الأحد ٤:٠٠ م'], bar: ['فورمولا ١', 'أبوظبي', 'السباق الأحد ٤:٠٠ م'] },
  { id: '10b-tennis', title: 'التنس',
    tv: `<h3>التنس<span>لاعبوك</span></h3><div class="boxes" style="gap:6px">${[['نادر العتيبي','× كريم سالم','ربع نهائي الدوحة','اليوم ٧:٠٠ م',1],['ليان الشهري','فازت ٦-٤، ٧-٥','دور الـ٣٢، دبي','أمس'],['يوسف القحطاني','× علي حسن','دور الـ١٦، الدوحة','غدًا ٥:٣٠ م']].map(([a,b,c,d,on])=>`<div class="box${on?' on':''}" style="display:grid;grid-template-columns:1fr 1fr 1fr;align-items:center;padding:6px 10px"><b style="text-align:right">${a}</b><span class="s">${b}، ${c}</span><b style="text-align:left">${d}</b></div>`).join('')}</div><div class="s" style="margin-top:8px">أسماء مخترعة للنموذج</div>`,
    couch: ['التنس', 'نادر العتيبي × كريم سالم', 'اليوم ٧:٠٠ م، ربع نهائي الدوحة'], bar: ['التنس', 'نادر العتيبي', 'اليوم ٧:٠٠ م'] },
  { id: '10c-national', title: 'المنتخب',
    tv: `<h3>المنتخب السعودي<span>كأس العرب</span></h3><div class="row" style="gap:30px"><div class="team">${band('ksa')}<b class="big">${n('ksa')}</b>${form('wwdlw')}</div><div class="team"><b class="huge" style="font-size:30px">الثلاثاء</b><span class="s">٧:٣٠ م، استاد البيت</span></div><div class="team">${band('jordan')}<b class="big">${n('jordan')}</b>${form('wdwww')}</div></div>
      <div class="boxes" style="grid-template-columns:repeat(3,1fr);margin-top:12px"><div class="box"><div class="s">الدور</div><b>نصف النهائي</b></div><div class="box"><div class="s">آخر مواجهة</div><b>٢ - ٠ للسعودية</b></div><div class="box"><div class="s">القناة</div><b>الكأس</b></div></div>`,
    couch: ['المنتخب', `${n('ksa')} × ${n('jordan')}`, 'الثلاثاء ٧:٣٠ م، نصف نهائي كأس العرب'], bar: ['المنتخب', `${n('ksa')} × ${n('jordan')}`, 'الثلاثاء ٧:٣٠ م'] },
];
