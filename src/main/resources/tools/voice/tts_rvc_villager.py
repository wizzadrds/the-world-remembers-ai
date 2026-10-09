#!/usr/bin/env python3
"""Generate Spanish Piper speech and convert it through VillagerTITAN RVC.

Bundled with the mod so Minecraft does not depend on shell environment variables."""

from __future__ import annotations

import base64
import json
import math
import os
import subprocess
import sys
import tempfile
from pathlib import Path

DEFAULT_RVC_ROOT = Path(r"C:\RVC")
DEFAULT_RVC_MODEL = Path(r"C:\Voces\VillagerTITAN.pth")
DEFAULT_PIPER_MODEL = Path(r"C:\Voces\es_ES-mls_9972-low.onnx")
DEFAULT_PIPER_BIN = Path(r"C:\Users\ismae\AppData\Local\Programs\Python\Python312\Scripts\piper.exe")
DEFAULT_RVC_PYTHON = DEFAULT_RVC_ROOT / ".venv" / "Scripts" / "python.exe"


def env_path(name: str, default: Path | None = None, required: bool = True) -> Path | None:
    value = os.environ.get(name, "").strip()
    path = Path(value).expanduser().resolve() if value else (default.resolve() if default else None)
    if path is None and required:
        raise SystemExit(f"Missing environment variable: {name}")
    return path


def run_checked(command: list[str], label: str, cwd: Path | None = None) -> None:
    try:
        # Minecraft on Windows may launch us with a legacy cp1252/charmap
        # console. RVC emits Unicode diagnostics (for example 【...】), so
        # force UTF-8 for the child process and decode its output as UTF-8.
        child_env = os.environ.copy()
        child_env["PYTHONUTF8"] = "1"
        child_env["PYTHONIOENCODING"] = "utf-8"
        completed = subprocess.run(
            command, check=False, text=True, encoding="utf-8", errors="replace",
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            cwd=str(cwd) if cwd else None, env=child_env,
        )
    except OSError as exc:
        raise SystemExit(f"{label} could not start: {exc}") from exc
    if completed.returncode != 0:
        output = (completed.stdout or "").strip()
        if len(output) > 12000:
            output = output[-12000:]
        raise SystemExit(f"{label} failed with exit code {completed.returncode}.\n{output}")


def pitch_to_semitones(pitch: float) -> int:
    if not math.isfinite(pitch) or pitch <= 0:
        return 0
    return max(-12, min(12, round(12.0 * math.log2(pitch))))


def _piper_to_source(text: str, source: Path, piper: Path, piper_model: Path, length_scale: float) -> None:
    piper_command = [
        str(piper), "--model", str(piper_model),
        "--output_file", str(source), "--length_scale", str(length_scale),
    ]
    try:
        piper_proc = subprocess.run(
            piper_command, input=text, text=True, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, check=False,
        )
    except OSError as exc:
        raise RuntimeError(f"Piper could not start: {exc}") from exc
    if piper_proc.returncode != 0:
        raise RuntimeError(f"Piper failed with exit code {piper_proc.returncode}.\n{piper_proc.stdout or ''}")
    if not source.is_file() or source.stat().st_size == 0:
        raise RuntimeError("Piper produced no source WAV")


def _write_rvc_audio(path: Path, audio, sample_rate: int) -> None:
    import soundfile as sf
    path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(str(path), audio, sample_rate)


def run_worker(
    rvc_root: Path,
    model: Path,
    piper_model: Path,
    piper: Path,
    speaker: int,
    f0_method: str,
    index_rate: float,
    protect: float,
) -> int:
    os.chdir(rvc_root)
    sys.path.insert(0, str(rvc_root))
    os.environ["weight_root"] = str(model.parent)
    os.environ.setdefault("index_root", str(rvc_root / "logs"))
    os.environ.setdefault("outside_index_root", str(rvc_root / "assets" / "indices"))
    os.environ.setdefault("rmvpe_root", str(rvc_root / "assets" / "rmvpe"))
    from configs.config import Config
    from infer.vc.modules import VC

    original_argv = sys.argv[:]
    sys.argv = [sys.argv[0]]
    try:
        config = Config()
    finally:
        sys.argv = original_argv

    print(f"[VillagerTITAN worker] loading {model.name}", file=sys.stderr, flush=True)
    vc = VC(config)
    vc.get_vc(model.name)
    print("[VillagerTITAN worker] ready", file=sys.stderr, flush=True)
    print(json.dumps({"ready": True}), flush=True)

    for raw in sys.stdin:
        raw = raw.strip()
        if not raw:
            continue
        try:
            request = json.loads(raw)
            text = base64.b64decode(request["text"]).decode("utf-8").strip()
            output = Path(request["output"]).expanduser().resolve()
            rate = float(request.get("rate", 1.0))
            pitch = float(request.get("pitch", 1.0))
            if not text:
                raise ValueError("Villager TTS text is empty")

            pitch_shift = max(-12, min(12, int(round(
                pitch_to_semitones(pitch) + int(os.environ.get("TWR_RVC_PITCH", "0"))
            ))))
            length_scale = max(0.55, min(1.6, 1.0 / max(0.5, min(2.0, rate))))
            with tempfile.TemporaryDirectory(prefix="twr-villagertitan-worker-") as temp_dir:
                source = Path(temp_dir) / "source.wav"
                _piper_to_source(text, source, piper, piper_model, length_scale)
                status, result = vc.vc_single(
                    speaker,
                    str(source),
                    pitch_shift,
                    "",
                    f0_method,
                    "",
                    "",
                    index_rate,
                    3,
                    40000,
                    1.0,
                    protect,
                )
                if not result or result[0] is None or result[1] is None:
                    raise RuntimeError(str(status or "RVC produced no audio"))
                _write_rvc_audio(output, result[1], result[0])

            print(json.dumps({"ok": True, "output": str(output)}, ensure_ascii=False), flush=True)
        except BaseException as exc:
            print(json.dumps({"ok": False, "error": f"{type(exc).__name__}: {exc}"}, ensure_ascii=False), flush=True)
    return 0


def main() -> int:
    if "--worker" not in sys.argv[1:] and len(sys.argv) < 3:
        raise SystemExit("usage: tts_rvc_villager.py <text> <output.wav> [language] [model] [rate] [pitch] [expressiveness]")

    worker_mode = "--worker" in sys.argv[1:]
    text = sys.argv[1].strip() if not worker_mode else ""
    output = Path(sys.argv[2]).expanduser().resolve() if not worker_mode else Path(".").resolve()
    if not worker_mode and not text:
        raise SystemExit("Villager TTS text is empty")

    rate = float(sys.argv[5]) if len(sys.argv) > 5 else 1.0
    pitch = float(sys.argv[6]) if len(sys.argv) > 6 else 1.0

    rvc_root = env_path("TWR_RVC_ROOT", DEFAULT_RVC_ROOT)
    model = env_path("TWR_RVC_MODEL", DEFAULT_RVC_MODEL)
    piper_model = env_path("TWR_PIPER_MODEL", DEFAULT_PIPER_MODEL)
    python = env_path("TWR_RVC_PYTHON", DEFAULT_RVC_PYTHON)
    piper = env_path("TWR_PIPER_BIN", DEFAULT_PIPER_BIN, required=False)
    if piper is None:
        piper = Path("piper")

    assert rvc_root and model and piper_model and python
    cli = rvc_root / "infer" / "cli.py"

    if not cli.is_file():
        raise SystemExit(f"RVC CLI not found: {cli}")
    if not model.is_file():
        raise SystemExit(f"RVC model not found: {model}")
    if not piper_model.is_file():
        raise SystemExit(f"Piper model not found: {piper_model}")
    if not python.is_file():
        raise SystemExit(f"RVC Python not found: {python}")
    if not piper.is_file() and str(piper).lower() != "piper":
        raise SystemExit(f"Piper executable not found: {piper}")

    if "--worker" in sys.argv[1:]:
        # The launcher may invoke this script with system Python. RVC dependencies
        # live in the configured RVC virtual environment, so re-exec there before
        # importing any RVC modules. This prevents a silent worker-startup crash.
        configured_python = python.resolve()
        current_python = Path(sys.executable).resolve()
        if current_python != configured_python:
            os.execv(str(configured_python), [str(configured_python), str(Path(__file__).resolve()), "--worker"])
        speaker = int(os.environ.get("TWR_RVC_SPEAKER", "0").strip() or "0")
        f0_method = os.environ.get("TWR_RVC_F0_METHOD", "rmvpe").strip() or "rmvpe"
        index_rate = max(0.0, min(1.0, float(os.environ.get("TWR_RVC_INDEX_RATE", "0"))))
        protect = max(0.0, min(0.5, float(os.environ.get("TWR_RVC_PROTECT", "0.5"))))
        return run_worker(rvc_root, model, piper_model, piper, speaker, f0_method, index_rate, protect)

    output.parent.mkdir(parents=True, exist_ok=True)
    speaker = os.environ.get("TWR_RVC_SPEAKER", "0").strip() or "0"
    f0_method = os.environ.get("TWR_RVC_F0_METHOD", "rmvpe").strip() or "rmvpe"
    index_rate = max(0.0, min(1.0, float(os.environ.get("TWR_RVC_INDEX_RATE", "0"))))
    protect = max(0.0, min(0.5, float(os.environ.get("TWR_RVC_PROTECT", "0.5"))))
    extra_pitch = int(os.environ.get("TWR_RVC_PITCH", "0"))
    pitch_shift = max(-12, min(12, extra_pitch + pitch_to_semitones(pitch)))
    length_scale = max(0.55, min(1.6, 1.0 / max(0.5, min(2.0, rate))))

    with tempfile.TemporaryDirectory(prefix="twr-villagertitan-") as temp_dir:
        temp = Path(temp_dir)
        source = temp / "source.wav"
        converted = temp / "converted.wav"

        piper_command = [
            str(piper), "--model", str(piper_model),
            "--output_file", str(source), "--length_scale", str(length_scale),
        ]
        try:
            piper_proc = subprocess.run(
                piper_command, input=text, text=True, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, check=False,
            )
        except OSError as exc:
            raise SystemExit(f"Piper could not start: {exc}") from exc

        if piper_proc.returncode != 0:
            raise SystemExit(f"Piper failed with exit code {piper_proc.returncode}.\n{piper_proc.stdout or ''}")
        if not source.is_file() or source.stat().st_size == 0:
            raise SystemExit("Piper produced no source WAV")

        rvc_command = [
            str(python), "-m", "infer.cli",
            "--model", str(model), "--speaker-id", speaker,
            "--input", str(source), "--output", str(converted),
            "--pitch", str(pitch_shift), "--f0-method", f0_method,
            "--index-rate", str(index_rate), "--protect", str(protect),
            "--resample-sr", "40000", "--overwrite",
        ]
        run_checked(rvc_command, "RVC", cwd=rvc_root)

        if not converted.is_file() or converted.stat().st_size == 0:
            raise SystemExit("RVC produced no output WAV")
        output.write_bytes(converted.read_bytes())

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
