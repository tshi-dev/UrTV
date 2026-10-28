# Makes the release video's narration with ElevenLabs, one mp3 per line.
# The key is read from the environment (ELEVENLABS_API_KEY) and only ever sent in the xi-api-key header:
# never print it, never write it to a file.
import json, os, sys, time, urllib.request

VOICE_ID = "rpGHcNQJvO8dFNNFNj1v"   # the owner's chosen voice (not secret)
# The owner asked for ElevenLabs' newest model. The script asks the account which models it has and takes the newest
# full (not turbo/flash) one that speaks Arabic: v4, else v3, else multilingual v2. EL_MODEL=<id> forces one.
PREFER = ["eleven_v4", "eleven_v3", "eleven_multilingual_v2"]

key = os.environ.get("ELEVENLABS_API_KEY", "")
if not key:
    sys.exit("ELEVENLABS_API_KEY is not set in this terminal: ask the owner to open a new terminal (it's in his Windows environment)")
def pick_model():
    if os.environ.get("EL_MODEL"):
        return os.environ["EL_MODEL"]
    req = urllib.request.Request("https://api.elevenlabs.io/v1/models", headers={"xi-api-key": key})
    with urllib.request.urlopen(req, timeout=30) as r:
        models = json.loads(r.read())
    arabic = {m["model_id"] for m in models
              if m.get("can_do_text_to_speech", True)
              and any(l.get("language_id") in ("ar", "ara") for l in m.get("languages", []))
              and not any(w in m["model_id"] for w in ("turbo", "flash"))}
    print("models with Arabic on this account:", ", ".join(sorted(arabic)) or "none listed")
    for want in PREFER:
        for mid in sorted(arabic, reverse=True):
            if mid.startswith(want):
                return mid
    return "eleven_multilingual_v2"

MODEL = pick_model()
# v3 and newer take stability only as 0.0 / 0.5 / 1.0 (creative / natural / robust) and do not take previous_text
NEW = not MODEL.startswith("eleven_multilingual")
SETTINGS = ({"stability": 0.5, "similarity_boost": 0.75} if NEW
            else {"stability": 0.45, "similarity_boost": 0.75, "style": 0.1, "use_speaker_boost": True})
print("model:", MODEL)
here = os.path.dirname(os.path.abspath(__file__))
LINES = os.environ.get("EL_LINES", "lines.txt")   # which text file to read (lines-v2.txt, lines-gtest.txt)
lines = [l.strip() for l in open(os.path.join(here, LINES), encoding="utf-8") if l.strip()]
out = os.path.join(here, os.environ.get("EL_OUT", "out")); os.makedirs(out, exist_ok=True)
prev = ""
for i, text in enumerate(lines, 1):
    nxt = lines[i] if i < len(lines) else ""
    body = {"text": text, "model_id": MODEL, "voice_settings": SETTINGS}
    if not NEW:
        body.update(previous_text=prev, next_text=nxt)   # keeps the tone even from line to line (older models only)
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
print("done:", len(lines), "files in", out, "with", MODEL)
