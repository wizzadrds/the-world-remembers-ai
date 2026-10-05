# Voice System

Voice is planned for v0.8 and should be local-first.

## Speech-to-text

Initial candidate: faster-whisper. It provides local Whisper inference and can be selected by hardware profile.

## Text-to-speech

Initial candidate: Piper. TTS is an adapter so another local engine can replace it later.

## Pipeline

Microphone -> STT -> utterance -> conversation/intent -> grounded response -> TTS -> spatial Minecraft audio.

## Voice identity

Each NPC should have a stable voice profile, including language, voice model and supported rate/pitch adjustments.

## Performance

Voice generation should be scheduled and cached. Direct conversations take precedence over ambient chatter.

## Privacy

Default microphone processing should remain local. Online providers must never be required for core gameplay.

Voice is deliberately not part of v0.1.
