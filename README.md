# THE WORLD REMEMBERS

> **Minecraft nunca olvida lo que haces.**

Minecraft Java mod focused on persistent memory, relationships, emergent NPC behavior, village history, conversations, local voice interaction, chronicles and dreams.

## Status

**Current milestone:** v1.0.0 — The World Remembers stable release candidate

## Core pillars

- Memory: meaningful events are stored and recalled. **[Phase 1 complete]**
- Personality: NPCs react differently to the same event. **[Phase 2 complete]**
- Relationships: trust, fear, gratitude and resentment evolve from remembered events. **[Phase 3 complete]**
- Behavior: NPCs decide whether to talk, continue, leave, work or ignore. **[Phase 4 complete]**
- Families: parents, siblings, spouses, shared homes and inherited history persist across generations. **[Phase 5 complete — v0.5.0-alpha]**
- Society: villages, resources, defense, landmarks, migration and shared storage persist. **[Phase 6 complete]**
- Rumors: NPCs distinguish direct, reported and rumored knowledge, with persistent conversations and grounded dialogue. **[Phase 7 complete]**
- Voice: local-first STT/TTS adapters, deterministic scheduling, spatial delivery and state-driven speech. **[Phase 8 complete]**
- Chronicles: a read-only world-history browser for timeline, people, relationships, families and villages. **[Phase 9 complete]**
- Dreams: persistent memory, fear, nostalgia and explicitly surreal impossible dreams derived from NPC memories. **[Phase 10 complete — v0.10.0-alpha]**

## Roadmap

| Version | Focus |
|---|---|
| 0.2 | First Memory |
| 0.3 | Personality |
| 0.4 | Relationships & Behavior |
| 0.5 | Families & Generations |
| 0.6 | Villages & Society |
| 0.7 | Rumors & Conversations |
| 0.8 | Local Voice |
| 0.9 | Chronicles |
| 0.10 | Dreams |
| 1.0 | The World Remembers |

## Development principles

1. Simulation is the source of truth.
2. Generative systems must not invent world-state facts.
3. Important events are remembered; trivial actions are not stored forever.
4. Behavior is contextual.
5. Voice is local-first and optional.
6. Dreams are derived inner-life state and never authoritative world facts.
7. Systems remain modular.

## Quality bar

Every milestone must compile, pass automated unit tests, pass Minecraft GameTests, and pass the headless Fabric server smoke test before its version is marked complete.