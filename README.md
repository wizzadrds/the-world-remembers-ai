# THE WORLD REMEMBERS

> **Minecraft nunca olvida lo que haces.**

Minecraft Java mod focused on persistent memory, relationships, emergent NPC behavior, village history, conversations and local voice interaction.

## Status

**Current milestone:** v0.4.0-alpha — Phase 4 complete and CI-validated

## Core pillars

- Memory: meaningful events are stored and recalled. **[Phase 1 complete]**
- Personality: NPCs react differently to the same event. **[Phase 2 complete]**
- Relationships: trust, fear, gratitude and resentment evolve from remembered events. **[Phase 3 complete]**
- Behavior: NPCs decide whether to talk, continue, leave, work or ignore. **[Phase 4 complete]**
- Families: parents, siblings, spouses, shared homes and inherited history persist across generations. **Phase 5 complete — v0.5.0-alpha**
- Society: villages, resources, defense, landmarks and migration are under Phase 6 development and validation.
- Voice: planned as a local-first optional feature.

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
6. Systems remain modular.

## Quality bar

Every milestone must compile, pass automated unit tests, and pass the headless Fabric server smoke test before its version is marked complete. Gameplay-facing systems are designed as deterministic simulation rules first, with Minecraft integration layered on top.
