# Globecast SBA Quran / Sunnah streams (report only)

## quran: https://cdn-globecast.akamaized.net/live/eds/saudi_quran/hls_roku/index.m3u8
- status 200, final address https://cdn-globecast.akamaized.net/live/eds/saudi_quran/hls_roku/index.m3u8
- headers: Access-Control-Allow-Origin=*, Server=NEA-CDN-SW, Expires=Mon, 09 Nov 2026 16:05:48 GMT, Cache-Control=max-age=0, no-cache, no-store, Content-Type=application/x-mpegURL
- token or expiry in the address: no
- variants: 2; token in any: no
  - saudi_quran-avc1_249984=1-mp4a_145600_ara=3.m3u8
  - saudi_quran-avc1_600000=2-mp4a_145600_ara=3.m3u8
  #EXT-X-STREAM-INF:BANDWIDTH=453241,AVERAGE-BANDWIDTH=412039,CODECS="avc1.4d400d,mp4a.40.2",RESOLUTION=320x256,FRAME-RATE=25.000
  #EXT-X-STREAM-INF:BANDWIDTH=861360,AVERAGE-BANDWIDTH=783055,CODECS="avc1.4d4015,mp4a.40.2",RESOLUTION=480x384,FRAME-RATE=25.000

## sunnah: https://cdn-globecast.akamaized.net/live/eds/saudi_sunnah/hls_roku/index.m3u8
- status 200, final address https://cdn-globecast.akamaized.net/live/eds/saudi_sunnah/hls_roku/index.m3u8
- headers: Access-Control-Allow-Origin=*, Server=NEA-CDN-SW, Expires=Mon, 09 Nov 2026 16:05:49 GMT, Cache-Control=max-age=0, no-cache, no-store, Content-Type=application/x-mpegURL
- token or expiry in the address: no
- variants: 2; token in any: no
  - saudi_sunnah-avc1_249984=1-mp4a_145600_ara=3.m3u8
  - saudi_sunnah-avc1_600000=2-mp4a_145600_ara=3.m3u8
  #EXT-X-STREAM-INF:BANDWIDTH=453241,AVERAGE-BANDWIDTH=412039,CODECS="avc1.4d400d,mp4a.40.2",RESOLUTION=320x256,FRAME-RATE=25.000
  #EXT-X-STREAM-INF:BANDWIDTH=861360,AVERAGE-BANDWIDTH=783055,CODECS="avc1.4d4015,mp4a.40.2",RESOLUTION=480x384,FRAME-RATE=25.000

## SBA's own sites: do they name Globecast?
- https://sba.sa -> None: 0 mention(s)
- https://www.sba.sa/ar -> None: 0 mention(s)
- https://aloula.sba.sa -> 200: 0 mention(s)
- https://aloula.sba.sa/live/quran -> 200: 0 mention(s)

## 600 s of playback each (ffmpeg, in parallel)
- quran: ran 587 s, exit 0, NOT OK
  - mime type is not rfc8216 compliant
- sunnah: ran 583 s, exit 0, NOT OK
  - mime type is not rfc8216 compliant
