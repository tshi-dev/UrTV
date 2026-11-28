#!/usr/bin/env python3
"""Known bug (tester step 33): `music.sh radio N` sends the station as an int; MusicReceiver read the same key with getIntExtra and then getStringExtra, and Android logged a
ClassCastException for the second read (nothing crashed). The string is read only when the int is missing."""
import sys
from pathlib import Path
s = (Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home/tilawah/MusicReceiver.java").read_text(encoding="utf-8")
blk = s[s.index("case MusicService.ACTION_PLAY_RADIO: {"):]
blk = blk[:blk.index("break;")]
ok = ('int index = intent.getIntExtra("station", -1);' in blk and 'String name = index < 0 ? intent.getStringExtra("station") : null;' in blk
      and blk.index("getIntExtra") < blk.index("getStringExtra") and "if (index < 0 && name != null) index = cat.stationIndex(name);" in blk)
print(("PASS" if ok else "FAIL") + " music receiver: the station is read as an int first, and as a string only when the int is missing (no ClassCastException in the log)")
sys.exit(0 if ok else 1)
