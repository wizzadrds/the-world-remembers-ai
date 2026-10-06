# Phase 8 Acceptance

Phase 8 is complete only when local STT/TTS adapters, voice profiles, spatial delivery, scheduling and state-driven delivery are implemented and covered by deterministic tests plus Minecraft GameTests.

- Local STT adapter contract exists and never requires an online provider.
- Local TTS adapter contract exists and never requires an online provider.
- Stable NPC voice profiles persist independently of the runtime engine.
- Spatial audio requests derive from Minecraft world positions.
- Voice scheduling bounds concurrent synthesis and prioritizes direct interaction.
- Delivery state reflects timid, nervous, tired, angry and excited states.
- Failure degrades silently without blocking gameplay.
- CI passes build, tests, GameTests and headless Fabric smoke test.
