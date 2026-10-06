# Chronicles System

## Goal
Phase 9 exposes the world's persistent history as a player-readable chronicle without inventing facts.

## Sources of truth
- NPC memories provide personal events.
- Relationships provide current social bonds.
- FamilyManager provides persistent family links.
- VillageHistoryManager and VillageEventManager provide village-scale history.

## Rules
- Chronicle entries are derived from persisted state.
- No generated entry may claim an event that is absent from simulation state.
- Timeline ordering is deterministic by game time, then stable type/UUID ordering.
- UI is a client presentation layer; persistence remains server-authoritative.
