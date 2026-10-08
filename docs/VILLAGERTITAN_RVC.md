# VillagerTITAN — RVC villager voice

The project can use the supplied **VillagerTITAN** RVC v2 checkpoint as the actual villager voice.

The integration is:

~~~text
AI dialogue
   ↓
human Spanish TTS (Piper)
   ↓
VillagerTITAN RVC v2
   ↓
40 kHz WAV
   ↓
Minecraft audio output
~~~

This is deliberately different from the old Java-only DSP. When the dedicated RVC pipeline is active, the client plays the RVC result directly so the old post-processing does not blur the converted voice.

## Model

The supplied model is configured as:

- RVC v2 / PyTorch
- F0 enabled
- 40 kHz target sample rate
- HuBERT base, output layer 12
- speaker ID 0 (target)
- no added index

Because there is no index file, the wrapper uses index-rate 0. The current RVC CLI documents this as the way to disable retrieval when no index is available.

## 1. Install RVC

Use a local checkout of the official RVC project and install the dependencies for the hardware you intend to use.

The mod does not bundle the RVC runtime or the checkpoint. This keeps the Minecraft JAR small and avoids redistributing a voice model whose license/credit information is not part of the supplied metadata.

The wrapper expects:

~~~text
<TWR_RVC_ROOT>/
  infer/
    cli.py
~~~

## 2. Put the checkpoint somewhere local

For example:

~~~text
C:\Voces\VillagerTITAN.pth
~~~

Do not commit the checkpoint to the Minecraft repository.

## 3. Install a human Spanish source TTS

The RVC model converts an existing voice; it is not the text-to-speech engine itself.

Piper is supported by the wrapper. Set:

### Windows PowerShell

~~~powershell
$env:TWR_RVC_ROOT="C:\RVC"
$env:TWR_RVC_MODEL="C:\Voces\VillagerTITAN.pth"
$env:TWR_PIPER_MODEL="C:\Voces\es-model.onnx"
$env:TWR_PIPER_BIN="piper"
~~~

### Linux/macOS

~~~bash
export TWR_RVC_ROOT="$HOME/RVC"
export TWR_RVC_MODEL="$HOME/voces/VillagerTITAN.pth"
export TWR_PIPER_MODEL="$HOME/voces/es-model.onnx"
export TWR_PIPER_BIN="piper"
~~~

Optional tuning:

~~~text
TWR_RVC_SPEAKER=0
TWR_RVC_F0_METHOD=rmvpe
TWR_RVC_INDEX_RATE=0
TWR_RVC_PROTECT=0.5
TWR_RVC_PITCH=0
~~~

The wrapper also maps the villager profile pitch to a small RVC semitone adjustment, capped to a safe range.

## 4. Configure Minecraft

Open **K → Voice & AI → VILLAGERS**.

Set **Villager TTS command** to:

~~~text
python tools/voice/tts_rvc_villager.py {text} {output} {language} {model} {rate} {pitch} {expressiveness}
~~~

Keep the normal AI provider as Gemini/OpenAI if desired. The dedicated villager TTS command is independent from the AI provider, so Gemini can still generate the dialogue while RVC handles the villager voice.

The command supports the same placeholders as the local TTS adapter:

- {text}
- {output}
- {language}
- {model}
- {rate}
- {pitch}
- {expressiveness}

## 5. Test

Use **Test villager voice** in the VILLAGERS page.

The built-in test phrase is:

~~~text
Hola, vecino. Soy un aldeano. ¿Me escuchas bien?
~~~

If the test fails, Minecraft will keep running and the last villager voice error is retained for diagnosis.

## Performance

RVC inference is performed by the local process, outside the Minecraft tick thread. The existing villager speech queue still limits pending jobs.

For GPU inference, point TWR_RVC_PYTHON at the Python environment containing the RVC dependencies. CPU inference is possible but substantially slower.

## Troubleshooting

### RVC CLI not found

Check:

~~~text
TWR_RVC_ROOT/infer/cli.py
~~~

### Model not found

Check:

~~~text
TWR_RVC_MODEL
~~~

The wrapper fails clearly instead of silently falling back to the old DSP.

### Piper not found

Set:

~~~text
TWR_PIPER_BIN
TWR_PIPER_MODEL
~~~

### The voice sounds over-processed

Make sure the command contains tts_rvc_villager.py. The Minecraft client detects this pipeline and does **not** run the legacy villager DSP after RVC.

### No index file

This model does not provide one. Keep:

~~~text
TWR_RVC_INDEX_RATE=0
~~~

That is intentional.
