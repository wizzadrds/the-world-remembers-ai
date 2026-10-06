# Voice System

Phase 8 is local-first and provider-agnostic.

## Pipeline

Microphone -> local STT adapter -> grounded utterance -> conversation/intent -> grounded response -> priority scheduler -> local TTS adapter -> spatial delivery.

## Adapter rule

The core mod never requires an online API. STT/TTS engines are external local executables behind narrow Java interfaces. A missing executable disables voice gracefully instead of breaking gameplay.

Initial adapters target faster-whisper for STT and Piper for TTS. Paths and commands are configuration, not hard-coded credentials or remote services.

## Scheduling

Priority order: DIRECT > DANGER > IMPORTANT > AMBIENT. Only one speech job per NPC may run at once; lower-priority ambient jobs may be dropped when the queue is saturated.

## Spatial delivery

Every utterance carries source position, maximum hearing distance and attenuation. The client computes audible gain from listener distance; the server remains authoritative over the text/event being spoken.

## Privacy

Microphone capture and transcription remain local by default. No cloud provider is required for core gameplay.
