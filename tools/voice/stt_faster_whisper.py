#!/usr/bin/env python3
import os
import sys
import tempfile
import wave

from faster_whisper import WhisperModel

if len(sys.argv) != 2:
    raise SystemExit("usage: stt_faster_whisper.py <pcm16k_mono_file>")

pcm_path = sys.argv[1]
model_name = os.environ.get("TWR_STT_MODEL", "small")
device = os.environ.get("TWR_STT_DEVICE", "cpu")
compute_type = os.environ.get("TWR_STT_COMPUTE", "int8" if device == "cpu" else "float16")
language = os.environ.get("TWR_STT_LANGUAGE", "es")

with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
    wav_path = tmp.name

try:
    with wave.open(wav_path, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(16000)
        with open(pcm_path, "rb") as pcm:
            wav.writeframes(pcm.read())

    model = WhisperModel(model_name, device=device, compute_type=compute_type)
    segments, _ = model.transcribe(wav_path, language=language, beam_size=5, vad_filter=True)
    print(" ".join(segment.text.strip() for segment in segments).strip())
finally:
    try:
        os.unlink(wav_path)
    except OSError:
        pass
