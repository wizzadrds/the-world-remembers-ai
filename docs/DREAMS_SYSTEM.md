# Dreams System

## Goal
Dreams turn persistent memory into a private, non-authoritative inner-life layer for NPCs.

## Rules
- Dreams never mutate world state.
- A dream may only use remembered facts or explicitly surreal recombinations.
- Dream records are persistent but bounded per NPC.
- Sleeping is the gameplay trigger; the system must not generate dreams while awake.
- Dream type is deterministic from available memory context.

## Types
- Memory: replays an important remembered event.
- Fear: reflects a remembered threat, loss or hostile event.
- Nostalgia: revisits an older positive/family memory.
- Impossible: combines known memories into a surreal scenario explicitly marked impossible; it is not a world-state fact.

MemoryManager is authoritative. Dreams are derived interpretations and must never be written back as factual memories.
