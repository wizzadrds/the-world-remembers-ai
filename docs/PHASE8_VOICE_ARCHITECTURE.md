# Phase 8 Voice Architecture

Minecraft remains authoritative for identity, position, personality, relationships and world facts. Voice engines only transform grounded requests.

## Adapters
STT converts local audio into an utterance. TTS converts grounded text plus a voice profile into an audio result. Adapters never mutate world state.

## Voice profile
Persistent NPC metadata: voice id, language, rate, pitch and enabled state. UUID is the stable key.

## Scheduling
Direct player/NPC speech outranks ambient speech. Requests are bounded and cancellable. Failed synthesis never stalls simulation.

## Spatial delivery
A request carries source position, listener position, priority and delivery state. Attenuation is derived from Minecraft positions.

## Privacy
Local engines are the default. Online providers are optional adapters, never core dependencies.
