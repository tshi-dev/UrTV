# الساعة خلف صورتك: كيف تصنع القناع

مع «الساعة: زجاجية خلف الصورة» (الإعدادات ← الشكل والمشاهد ← الساعة) تظهر ساعة كبيرة زجاجية على الصورة. في صورنا اللي سماؤها واضحة، المباني أو الجبال
تكون قدّام الأرقام كأن الساعة ورا المشهد. صورك أنت تقدر تصير كذا بعد، إذا أرسلت معها **قناعًا**: صورة بالأبيض والأسود بنفس مقاسها.

- **الأبيض:** اللي يكون قدّام الساعة (المباني، الجبال، الناس، الأشجار).
- **الأسود:** السماء والخلفية البعيدة.

بدون قناع تظهر صورتك كالعادة والساعة قدّامها. لا شيء يتغيّر إذا ما سوّيت شي.

## الخصوصية: اقرأها قبل

القناع تصنعه أنت بأداة الذكاء الاصطناعي اللي تختارها (مثل ChatGPT أو Gemini أو Claude). **صورتك تروح لهذه الأداة وتخضع لشروطها، مو لنا.** صفحة الجوال
حقّتنا ما ترسل الصورة ولا القناع لأي مكان إلا لتلفزيونك في بيتك. إذا ما تبي صورتك تروح لأي أداة، لا تسوّي قناعًا، أو استعمل طريقة الكمبيوتر تحت (كل شي على
جهازك).

## بأداة ذكاء اصطناعي (من الجوال)

1. افتح أداة الصور اللي تستعملها، وأرفق صورتك.
2. انسخ الجملة هذي كما هي وأرسلها:

   > اصنع قناعًا أبيض وأسود لهذه الصورة بنفس المقاس بالضبط. اجعل باللون الأبيض كل ما هو في المقدّمة: المباني والجبال والناس والأشجار وأي شيء على الأرض.
   > واجعل السماء وكل ما خلفها باللون الأسود. أبيض وأسود فقط، بدون رمادي ولا ألوان، وبدون أي تغيير آخر في الصورة. أعطني الصورة بصيغة PNG.

3. نزّل القناع على جوالك.
4. افتح صفحة الجوال من التلفزيون (امسح الرمز من «أسهل من الجوال»)، ثم «صوري» ← **«صورة مع قناعها»**: اختر الصورة، ثم القناع.
5. الصفحة تفحص القناع وتريك كيف بتطلع الساعة، ثم «أرسلها مع القناع».

### إذا رفضت الصفحة القناع

| ما تقوله الصفحة | وش تسوّي |
|---|---|
| القناع مو بنفس مقاس الصورة | اطلب من الأداة: «بنفس مقاس الصورة بالضبط، لا تقصّها ولا تغيّر أبعادها». |
| فيه ألوان أو رمادي كثير | اطلب: «اجعله أبيض وأسود فقط، بدون رمادي». |
| القناع أكبر من ١ م.ب | اطلب PNG أصغر، أو أرسل الصورة بدون قناع. |

الصفحة تقبل قناعًا بنفس شكل الصورة (الفرق ٢٪ بالأكثر)، أغلبه أبيض وأسود، و١ م.ب بالأكثر. أي مقاس يصلح ما دام الشكل نفسه: الصفحة تكبّره أو تصغّره.

## بالكمبيوتر (لوكيل يشتغل على كمبيوتر الشخص)

نفس الأداة اللي نصنع بها أقنعة صورنا، تشتغل على الكمبيوتر بدون إنترنت، وما ترسل الصورة لأي مكان. تنجح فقط مع صورة سماؤها واضحة الحد (سماء صافية أو بلون
واحد فوق المشهد)؛ غير كذا تقول إنها ما قدرت، ولا تكتب شيئًا.

```
python3 -m venv mask-venv && mask-venv/bin/pip install numpy scipy opencv-python-headless Pillow
mask-venv/bin/python optimize/photo_mask.py صورتي.jpg
```

تكتب `صورتي-mask.png` بجانب الصورة. أرسل الاثنين من صفحة الجوال كما في الخطوة ٤ فوق. الصورة لازم تكون بشكل الشاشة (١٦:٩)؛ غير كذا تقول الأداة اقصّها أولًا.

---

# The clock behind your photo: making the mask

With «الساعة: زجاجية خلف الصورة» (Settings ← الشكل والمشاهد ← الساعة) a big glass clock sits on the photo. On our photos with a clear sky, the buildings or
mountains stand in front of the digits. Your own photo can do the same if you send a **mask** with it: a black-and-white image of the same size.

- **White:** what stands in front of the clock (buildings, mountains, people, trees).
- **Black:** the sky and the far background.

Without a mask your photo shows as always, the clock in front. Nothing changes unless you do this.

## Privacy: read this first

You make the mask with the AI tool you choose (ChatGPT, Gemini, Claude…). **Your photo goes to that tool, under its terms, not to us.** Our phone page sends the
photo and the mask nowhere but to your TV at home. If you don't want your photo to go to any tool, make no mask, or use the computer way below (all on your own
machine).

## With an AI tool (from the phone)

1. Open the image tool you use and attach your photo.
2. Copy this exactly and send it:

   > Make a black-and-white mask of this photo at exactly the same size. Make everything in the foreground white: buildings, mountains, people, trees and
   > anything standing on the ground. Make the sky and everything behind it black. Pure black and white only, no grey and no colours, and no other change
   > to the image. Give me a PNG.

3. Save the mask on your phone.
4. Open the phone page from the TV (scan the code in «أسهل من الجوال»), then «صوري» ← **«صورة مع قناعها»**: pick the photo, then the mask.
5. The page checks the mask and shows how the clock will look; then «أرسلها مع القناع».

If the page refuses the mask: ask the tool for "exactly the same size as the photo, not cropped", or "pure black and white, no grey", or a smaller PNG
(1 MB at most). Any size works as long as the shape is the photo's (2 % at most apart).

## With a computer (for an agent on the person's computer)

The same offline tool we make our photos' masks with; nothing leaves the machine. It works only on a photo whose sky has a clean edge (a clear or evenly
coloured sky above the scenery); otherwise it says so and writes nothing.

```
python3 -m venv mask-venv && mask-venv/bin/pip install numpy scipy opencv-python-headless Pillow
mask-venv/bin/python optimize/photo_mask.py myphoto.jpg
```

It writes `myphoto-mask.png` beside the photo; send both from the phone page as in step 4. The photo must be 16:9 like the screen; otherwise the tool asks you
to crop it first.
