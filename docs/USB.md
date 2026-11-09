# A USB stick or drive on the TV

For the person and for the agent helping them (issue #50: «the flash drive is not read» after the setup).

## Why it can stop working after the setup

Before 4.3 a community list for some models turned off the TV's own **viewer** of a USB stick (TCL's `com.tcl.ui_mediaCenter`, Xiaomi's
`com.xiaomi.mitv.mediaexplorer`, MediaTek's `com.mediatek.wwtv.mediaplayer` and others). The stick was mounted, but nothing opened it.
Since 4.3 these viewers are in `optimize/lists/keep.txt`: no list turns them off, and the home's apps row always shows them.

## What to do, in order

1. **Turn the viewer back on** (a TV set up before 4.3): `optimize/keep.sh` (or, on the TV, «إعداد تلفزيوني» → «جهّز كل شي»: its step
   «الميزات المحمية» does the same). It only turns back on what keep.txt lists; nothing else changes.
2. **Open it from the apps row**: «الملفات», «Media Center» or the maker's name for it. A stick is not opened by the home screen itself.
3. **The stick's format**: most TVs read **FAT32** and **exFAT**; many read **NTFS** only to play, some not at all. If the TV says nothing
   when it goes in, try another stick, or format this one as exFAT on a computer (this erases it: copy its files first).
4. **Another port, and power**: try the other USB port; a hard disk without its own power may need a powered hub.
5. **Unplug the TV from the wall for a minute**, then the stick again.

## For the person (Arabic, to send as it is)

«افتح تطبيق الملفات أو «Media Center» من صف التطبيقات بعد ما تركّب الفلاشة. إذا ما ظهرت: جرّب المنفذ الثاني، وطفّ التلفزيون من الفيش دقيقة.
وإذا ما زالت ما تنقرى، الفلاشة غالباً بصيغة ما يقرأها التلفزيون: انسخ ملفاتها على الكمبيوتر وسوّ لها «فورمات» بصيغة exFAT.»

If none of this helps, report it with `optimize/share.sh --preview --problem --owner "<their words>" "<what you checked>"` (AGENTS.md).
