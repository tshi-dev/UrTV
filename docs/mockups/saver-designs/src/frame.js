H.classList.add('sd-frame');
lift();
put('sd-vig', '');
put('sd-fdate', dayLine());
const v = VERSES.length ? VERSES.find(x => x.text.length < 70) : null;
if (v) put('sd-verse', `<div class="q"><span class="o">﴿</span>${v.text}<span class="o">﴾</span></div><div class="s">${v.surah}${SEP}الآية ${ar(v.ayah)}</div>`);
