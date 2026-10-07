#!/usr/bin/env python3
import os
import subprocess
import sys

if len(sys.argv) < 3:
    raise SystemExit("usage: tts_piper.py <text> <output.wav> [language] [model] [rate] [pitch] [expressiveness]")

text = sys.argv[1]
output = sys.argv[2]
model = sys.argv[4] if len(sys.argv) > 4 and sys.argv[4] and sys.argv[4] != "piper" else os.environ.get("TWR_PIPER_MODEL", "")
rate = float(sys.argv[5]) if len(sys.argv) > 5 else 1.0
piper = os.environ.get("TWR_PIPER_BIN", "piper")

if not model:
    raise SystemExit("Set TWR_PIPER_MODEL to a Piper .onnx voice model or configure the TTS model field.")

length_scale = max(0.55, min(1.6, 1.0 / max(0.5, min(2.0, rate))))
command = [
    piper,
    "--model", model,
    "--output_file", output,
    "--length_scale", str(length_scale),
]
subprocess.run(command, input=text, text=True, check=True)
