# Makes the release video's narration with ElevenLabs, one mp3 per line.
# The key is read from the environment (ELEVENLABS_API_KEY) and only ever sent in the xi-api-key header:
# never print it, never write it to a file.
import json, os, sys, time, urllib.request

VOICE_ID = "rpGHcNQJvO8dFNNFNj1v"   # the owner's chosen voice (not secret)
MODEL = os.environ.get("EL_MODEL", "eleven_multilingual_v2")
SETTINGS = {"stability": 0.45, "similarity_boost": 0.75, "style": 0.1, "use_speaker_boost": True}

key = os.environ.get("ELEVENLABS_API_KEY", "")
if not key:
    sys.exit("ELEVENLABS_API_KEY is not set in this terminal: ask the owner to open a new terminal (it's in his Windows environment)")
here = os.path.dirname(os.path.abspath(__file__))
lines = [l.strip() for l in open(os.path.join(here, "lines.txt"), encoding="utf-8") if l.strip()]
out = os.path.join(here, "out"); os.makedirs(out, exist_ok=True)
prev = ""
for i, text in enumerate(lines, 1):
    nxt = lines[i] if i < len(lines) else ""
    body = {"text": text, "model_id": MODEL, "voice_settings": SETTINGS,
            "previous_text": prev, "next_text": nxt}   # keeps the tone even from line to line
    req = urllib.request.Request(
        f"https://api.elevenlabs.io/v1/text-to-speech/{VOICE_ID}?output_format=mp3_44100_192",
        data=json.dumps(body).encode(), method="POST",
        headers={"xi-api-key": key, "Content-Type": "application/json", "Accept": "audio/mpeg"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                open(os.path.join(out, f"{i:02d}.mp3"), "wb").write(r.read())
            break
        except Exception as e:
            print(f"line {i}: try {attempt + 1} failed: {type(e).__name__} {getattr(e, 'code', '')}")
            time.sleep(3)
    else:
        sys.exit(f"line {i} failed 3 times")
    print(f"line {i:02d}/{len(lines)} ok")
    prev = text
print("done:", len(lines), "files in", out)
