# Phase 8 — Local Voice Architecture

## Principle
Voice is an optional local-first layer. The simulation remains authoritative; voice only renders already-grounded utterances.

## Pipeline

Microphone -> STT adapter -> normalized utterance -> conversation/intent -> grounded response -> voice scheduler -> TTS adapter -> spatial audio.

## Adapters

STT and TTS are interfaces. Local engines are implementations; tests use deterministic fakes so CI never requires a microphone, GPU, model download or network service.

## Scheduling

Priority order:
1. direct player/NPC conversation;
2. urgent event or danger response;
3. family/social interaction;
4. ambient conversation.

Duplicate utterances should be coalesced. Queue length and generated-audio cache must be bounded.

## Grounding

Voice cannot create facts. It receives a grounded utterance plus delivery state and renders it.

## Spatial audio

The renderer receives source position, listener position and base volume. Distance attenuation is deterministic and testable without actual audio hardware.

## State-driven delivery

Stress, personality and activity select a delivery state such as TIMID, NERVOUS, TIRED, ANGRY or EXCITED. Delivery modifies rate, pitch, intensity and pauses without changing semantic content.
