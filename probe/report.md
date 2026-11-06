# Station 0 probe (300 s)

master: `https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8` -> `https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8` (636 ms, application/x-mpegURL)

```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=192062,BANDWIDTH=192748,CODECS="mp4a.40.2",CLOSED-CAPTIONS=NONE
chunks.m3u8

```

variant addresses the same at the end: **True**

responses cut before their end (chunked, no last chunk): **0 of 167** []

master headers: {"Server": "KWIKcache", "MT-Cache": "EXPIRED", "PB-Cache": "on", "Vary": "Accept-Encoding", "Expires": "Fri, 06 Nov 2026 21:05:09 GMT", "Cache-Control": "max-age=0, no-cache, no-store", "Pragma": "no-cache", "Date": "Fri, 06 Nov 2026 21:05:09 GMT", "Alt-Svc": "h3=\":443\"; ma=93600,h3-29=\":443\"; ma=93600", "Content-Length": "137", "Connection": "close", "Akamai-Mon-Iucid-Del": "1718464", "Content-Type": "application/x-mpegURL", "Access-Control-Max-Age": "86400", "Access-Control-Allow-Credentials": "true", "Access-Control-Expose-Headers": "Server,range,hdntl,hdnts,Akamai-Mon-Iucid-Ing,Akamai-Mon-Iucid-Del,Akamai-Request-BC", "Access-Control-Allow-Headers": "origin,range,hdntl,hdnts,CMCD-Request,CMCD-Object,CMCD-Status,CMCD-Session", "Access-Control-Allow-Methods": "GET,POST,OPTIONS", "Access-Control-Allow-Origin": "*"}

| variant | bandwidth | codecs | target s | seg s | segs | disc | seg kbps | streams |
|---|---|---|---|---|---|---|---|---|
| `https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/chunks.m3u8` | 192748 | mp4a.40.2 | 5.0 | [3.968, 4.0, 4.021] | 3 | 0 | 192.0 | audio:aac 48000Hz |

## Live watch (a player's loop)

| stream | segments | missed | late playlists | slow fetches | fetch ms median/max | kbps | errors |
|---|---|---|---|---|---|---|---|
| variant 192748 | 78 | 0 | 0 | 0 | 1007/1532 | 192.2 |  |

## ffmpeg

| stream | wall s | audio s | behind s | gap warnings | kinds | opens | input |
|---|---|---|---|---|---|---|---|
| master `https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8` | 295.3 | 300.0 | -4.7 | 0 |  | 143 | Input #0, hls, from 'https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8': / Duration: N/A, start: 61596.029722, bitrate: 191 kb/s / Stream #0:0: Audio: aac (LC), 48000 Hz, stereo, fltp, 191 kb/s, start 61596.029722 / Stream #0:0 -> #0:0 (aac (native) -> pcm_s16le (native))  |
| variant 192748 `https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/chunks.m3u8` | 295.3 | 300.0 | -4.7 | 0 |  | 141 | Input #0, hls, from 'https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/chunks.m3u8': / Duration: N/A, start: 61596.029722, bitrate: 191 kb/s / Stream #0:0: Audio: aac (LC), 48000 Hz, stereo, fltp, 191 kb/s, start 61596.029722 / Stream #0:0 -> #0:0 (aac (native) -> pcm_s16le (native)) /  |
| candidate 1 `https://stream.radiojar.com/0tpy1h0kxtzuv` | 295.5 | 300.0 | -4.5 | 0 |  | 0 | Input #0, mp3, from 'https://stream.radiojar.com/0tpy1h0kxtzuv': / Duration: N/A, start: 0.000000, bitrate: 90 kb/s / Stream #0:0: Audio: mp3 (mp3float), 44100 Hz, stereo, fltp, 90 kb/s / Stream #0:0 -> #0:0 (mp3 (mp3float) -> pcm_s16le (native)) / Stream #0:0: Audio: pcm_s16le, 44100 Hz, stereo, s1 |
| candidate 2 `https://edge.mixlr.com/channel/rwumx` | 300.3 | 300.0 | 0.3 | 0 |  | 0 | Input #0, mp3, from 'https://edge.mixlr.com/channel/rwumx': / Duration: N/A, start: 0.000000, bitrate: 128 kb/s / Stream #0:0: Audio: mp3 (mp3float), 44100 Hz, stereo, fltp, 128 kb/s / Stream #0:0 -> #0:0 (mp3 (mp3float) -> pcm_s16le (native)) / Stream #0:0: Audio: pcm_s16le, 44100 Hz, stereo, s16,  |
