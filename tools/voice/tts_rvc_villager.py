#!/usr/bin/env python3
"""
Generate a human TTS WAV and convert it through the supplied VillagerTITAN
RVC v2 model.

Required environment:
  TWR_RVC_ROOT   Root of a checkout of RVC-Project/Retrieval-based-Voice-Conversion-WebUI
  TWR_RVC_MODEL  Path to VillagerTITAN.pth

Optional:
  TWR_RVC_PYTHON      Python executable used for RVC (default: current Python)
  TWR_RVC_SPEAKER     Speaker id (default: 0)
  TWR_RVC_F0_METHOD   rmvpe or pm (default: rmvpe)
  TWR_RVC_INDEX_RATE  Retrieval index blend (default: 0; the supplied model has no index)
  TWR_RVC_PROTECT     Consonant/breath protection (default: 0.5)
  TWR_RVC_PITCH       Extra pitch shift in semitones (default: 0)
  TWR_PIPER_BIN       Piper executable (default: piper)
  TWR_PIPER_MODEL     Human Spanish Piper model used as the source voice

The supplied model metadata is RVC v2, F0-enabled, 40 kHz, HuBERT base layer 12,
speaker 0, with no FAISS/index file. RVC is therefore used as the actual
villager timbre stage; Java does not apply an additional villager DSP layer.
"""

from __future__ import annotations

import math
import os
import subprocess
import sys
import tempfile
from pathlib import Path


def env_path(name: str, required: bool = True) -> Path | None:
    value = os.environ.get(name, "").strip()
    if not value:
        if required:
            raise SystemExit(f"Missing environment variable: {name}")
        return None
    return Path(value).expanduser().resolve()


def run_checked(command: list[str], label: str) -> None:
    try:
        completed = subprocess.run(
            command,
            check=False,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )
    except OSError as exc:
        raise SystemExit(f"{label} could not start: {exc}") from exc

    if completed.returncode != 0:
        output = (completed.stdout or "").strip()
        if len(output) > 12000:
            output = output[-12000:]
        raise SystemExit(
            f"{label} failed with exit code {completed.returncode}.\n{output}"
        )


def pitch_to_semitones(pitch: float) -> int:
    if not math.isfinite(pitch) or pitch <= 0:
        return 0
    return max(-12, min(12, round(12.0 * math.log2(pitch))))


def main() -> int:
    if len(sys.argv) < 3:
        raise SystemExit(
            "usage: tts_rvc_villager.py <text> <output.wav> "
            "[language] [model] [rate] [pitch] [expressiveness]"
        )

    text = sys.argv[1].strip()
    output = Path(sys.argv[2]).expanduser().resolve()
    if not text:
        raise SystemExit("Villager TTS text is empty")

    rate = float(sys.argv[5]) if len(sys.argv) > 5 else 1.0
    pitch = float(sys.argv[6]) if len(sys.argv) > 6 else 1.0

    rvc_root = env_path("TWR_RVC_ROOT")
    model = env_path("TWR_RVC_MODEL")
    piper_model = env_path("TWR_PIPER_MODEL")
    assert rvc_root is not None and model is not None and piper_model is not None

    cli = rvc_root / "infer" / "cli.py"
    if not cli.is_file():
        raise SystemExit(f"RVC CLI not found: {cli}")
    if not model.is_file():
        raise SystemExit(f"RVC model not found: {model}")
    if not piper_model.is_file():
        raise SystemExit(f"Piper model not found: {piper_model}")

    output.parent.mkdir(parents=True, exist_ok=True)
    python = os.environ.get("TWR_RVC_PYTHON", sys.executable).strip() or sys.executable
    piper = os.environ.get("TWR_PIPER_BIN", "piper").strip() or "piper"
    speaker = os.environ.get("TWR_RVC_SPEAKER", "0").strip() or "0"
    f0_method = os.environ.get("TWR_RVC_F0_METHOD", "rmvpe").strip() or "rmvpe"
    index_rate = float(os.environ.get("TWR_RVC_INDEX_RATE", "0"))
    protect = float(os.environ.get("TWR_RVC_PROTECT", "0.5"))
    extra_pitch = int(os.environ.get("TWR_RVC_PITCH", "0"))

    index_rate = max(0.0, min(1.0, index_rate))
    protect = max(0.0, min(0.5, protect))
    pitch_shift = max(-12, min(12, extra_pitch + pitch_to_semitones(pitch)))
    length_scale = max(0.55, min(1.6, 1.0 / max(0.5, min(2.0, rate))))

    with tempfile.TemporaryDirectory(prefix="twr-villagertitan-") as temp_dir:
        temp = Path(temp_dir)
        source = temp / "source.wav"
        converted = temp / "converted.wav"

        # Piper provides linguistic content and prosody. RVC replaces the
        # speaker identity with VillagerTITAN's learned target timbre.
        try:
            piper_proc = subprocess.run(
                [
                    piper,
                    "--model", str(piper_model),
                    "--output_file", str(source),
                    "--length_scale", str(length_scale),
                ],
                input=text,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )
        except OSError as exc:
            raise SystemExit(f"Piper could not start: {exc}") from exc

        if piper_proc.returncode != 0:
            raise SystemExit(
                "Piper failed with exit code "
                f"{piper_proc.returncode}.\n{piper_proc.stdout or ''}"
            )
        if not source.is_file() or source.stat().st_size == 0:
            raise SystemExit("Piper produced no source WAV")

        # The supplied checkpoint is 40 kHz, so explicitly request that output
        # rate instead of relying on the CLI/runtime default.
        rvc_command = [
            python,
            str(cli),
            "--model", str(model),
            "--speaker-id", speaker,
            "--input", str(source),
            "--output", str(converted),
            "--pitch", str(pitch_shift),
            "--f0-method", f0_method,
            "--index-rate", str(index_rate),
            "--protect", str(protect),
            "--resample-sr", "40000",
            "--overwrite",
        ]
        run_checked(rvc_command, "RVC")

        if not converted.is_file() or converted.stat().st_size == 0:
            raise SystemExit("RVC produced no output WAV")

        output.write_bytes(converted.read_bytes())

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
