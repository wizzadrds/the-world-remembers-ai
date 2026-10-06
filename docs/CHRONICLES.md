# Chronicles

## Goal
Chronicles expose the world's remembered history without becoming a second source of truth.

## Phase 9
- Timeline entries are derived from persistent memory, relationships, family state and village history.
- People view shows known NPC identities and salient remembered facts.
- Relationships view shows persistent social bonds and their current state.
- Families view shows persistent family links.
- Village history view shows persistent village events and migration history.

## Source of truth
Minecraft simulation and existing SavedData systems remain authoritative. The Chronicle layer aggregates and presents data; it does not invent events.

## UX constraints
- Read-only first.
- Deterministic ordering by game tick, then stable identifiers.
- Bounded history per screen to avoid client stalls.
- Missing/dead entities remain represented by UUID/name fallback where possible.
