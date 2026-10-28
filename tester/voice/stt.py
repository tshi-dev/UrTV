# Writes a word-by-word transcript (with times) of the owner's narration recording, so the supervisor's team,
# who cannot listen, can find each line and each take. Usage: python tester/voice/stt.py <recording.m4a>
# First ElevenLabs Speech to Text with the owner's key (ELEVENLABS_API_KEY, sent only in the xi-api-key header,
# never printed or saved); if that is refused, faster-whisper on this PC (pip install faster-whisper).
import json, os, sys, urllib.request, uuid

src = sys.argv[1]
here = os.path.dirname(os.path.abspath(__file__))
out = os.path.join(here, "owner-transcript.json")

def eleven():
    key = os.environ.get("ELEVENLABS_API_KEY", "")
    if not key:
        raise RuntimeError("no key in this terminal")
    data = open(src, "rb").read()
    b = uuid.uuid4().hex
    parts = []
    for name, val in (("model_id", os.environ.get("EL_STT", "scribe_v1")), ("language_code", "ara"),
                      ("timestamps_granularity", "word"), ("tag_audio_events", "false")):
        parts.append(f'--{b}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{val}\r\n'.encode())
    parts.append(f'--{b}\r\nContent-Disposition: form-data; name="file"; filename="rec.m4a"\r\nContent-Type: audio/mp4\r\n\r\n'.encode() + data + b"\r\n")
    parts.append(f"--{b}--\r\n".encode())
    req = urllib.request.Request("https://api.elevenlabs.io/v1/speech-to-text", data=b"".join(parts), method="POST",
                                 headers={"xi-api-key": key, "Content-Type": f"multipart/form-data; boundary={b}"})
    with urllib.request.urlopen(req, timeout=600) as r:
        j = json.loads(r.read())
    return {"engine": "elevenlabs " + os.environ.get("EL_STT", "scribe_v1"),
            "words": [{"w": w["text"], "s": w["start"], "e": w["end"]} for w in j.get("words", []) if w.get("type", "word") == "word"]}

def whisper():
    from faster_whisper import WhisperModel
    m = WhisperModel(os.environ.get("WHISPER", "large-v3"), device="auto", compute_type="auto")
    segs, _ = m.transcribe(src, language="ar", word_timestamps=True, vad_filter=False)
    return {"engine": "faster-whisper " + os.environ.get("WHISPER", "large-v3"),
            "words": [{"w": w.word.strip(), "s": w.start, "e": w.end} for s in segs for w in (s.words or [])]}

try:
    res = eleven()
except Exception as e:
    print("ElevenLabs speech-to-text did not work:", type(e).__name__, getattr(e, "code", ""), "-> trying faster-whisper")
    res = whisper()
json.dump(res, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=0)
print("words:", len(res["words"]), "engine:", res["engine"], "->", out)
