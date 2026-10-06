# Development Phases

The project follows a staged implementation plan. Features are documented before implementation and integrated only when their phase is ready.

## Phase 0 — Foundation
- [x] Fabric project
- [x] Stable package structure
- [x] Persistent world state
- [x] Core event and memory models

## Phase 1 — The First Memory
- [x] Important event detection
- [x] NPC memories
- [x] Memory importance
- [x] Player/NPC relationships
- [x] First remembered interaction

**Status: COMPLETE — v0.2.0-alpha**

## Phase 2 — Personality
- [x] Stable personality profiles
- [x] Personality affects decisions
- [x] Different reactions to identical events

**Status: COMPLETE — v0.3.0-alpha**

## Phase 3 — Relationships
- [x] Trust, gratitude, fear, respect, affection, resentment, suspicion
- [x] Threat reactions and protective bonds
- [x] Relationship persistence
- [x] Relationship consequences from events
- [x] Relationship state helpers
- [x] Threat, attack, rescue and guardian-loss relationship deltas

**Status: COMPLETE — v0.4.0-alpha**

## Phase 4 — Behavior
- [x] Activity states
- [x] Decision engine
- [x] Busy vs available NPCs
- [x] Leaving when annoyed
- [x] Following when appropriate
- [x] Follow-me trust/permission rules
- [x] Travel distance awareness
- [x] Stress and fatigue affecting decisions
- [x] Persistent home assignment and access rules
- [x] Home intrusion detection and consequences
- [x] Home storage links
- [x] Persistent home data model
- [x] Persistent stress data model
- [x] Travel state model
- [x] Interruptions and priority
- [x] Contextual animation state
- [x] NPC inventory and real item state
- [x] Gathering/carrying/storage navigation state
- [x] Chest/home storage links
- [x] Economic/work decisions
- [x] Threat responses and calls for help
- [x] Role-aware behavior
- [x] NPC equipment and armor synchronization
- [x] NPC role/archetype model
- [x] Voice temperament model foundation

**[Phase 4 complete]**

**Status: COMPLETE — v0.4.0-alpha — CI validated on PR #80**

## Phase 5 — Families & Generations
- [x] Persistent age data model
- [x] Persistent family relation model
- [x] Parent, child, sibling and spouse relation types
- [x] Family relation persistence
- [x] Family relation integrity checks
- [x] Birth/death/marriage/family-loss/inheritance event vocabulary
- [x] Live villager age assignment
- [x] Parents, siblings, spouses and children in live NPCs
- [x] Live parent/child relation linking
- [x] Live spouse/courtship linking
- [x] Live sibling formation
- [x] Birth and death family events (live)
- [x] Generational memory propagation
- [x] Inherited and important possessions
- [x] Family-aware home assignment
- [x] Family-aware behavior and protection

**[Phase 5 complete]**

**Status: COMPLETE — v0.5.0-alpha**

## Phase 6 — Villages & Society
- [x] Village identity and persistent village state
- [x] Population tracking
- [x] Village history and important events
- [x] Wealth and resource pressure
- [x] Shared village resources and storage
- [x] Village defense state
- [x] Migration and settlement change
- [x] Important buildings and landmarks
- [x] Iron golems as social guardians
- [x] Village-scale GameTests
- [x] Population-scale performance validation

**[Phase 6 complete]**

**Status: COMPLETE — v0.6.0-alpha — CI run #287 validated**

## Phase 7 — Rumors & Conversations
- [x] Witness-based knowledge
- [x] Rumor degradation
- [x] Persistent conversations
- [x] NPC-to-NPC conversations
- [x] Grounded dialogue
- [x] Direct, reported and rumored knowledge

**[Phase 7 complete]**

**Status: COMPLETE — v0.7.0-alpha — CI validated on PR #104**

## Phase 8 — Local Voice
- [x] Local STT
- [x] Local TTS
- [x] Voice profile model foundation
- [x] Personality-driven voice temperament model
- [x] Spatial audio
- [x] Voice scheduling
- [x] State-driven delivery (timid, nervous, tired, angry, excited)

**[Phase 8 complete]**

**Status: COMPLETE — v0.8.0-alpha — CI run #348 validated**

Phase 8 acceptance is satisfied by local adapter contracts, deterministic bounded scheduling, spatial delivery, state-driven delivery, graceful adapter failure, and live client/server voice payload integration.


## Phase 9 — Chronicles
- [ ] Timeline UI
- [ ] People
- [ ] Relationships
- [ ] Families
- [ ] Village history

## Phase 10 — Dreams
- [ ] Memory dreams
- [ ] Fear dreams
- [ ] Nostalgia
- [ ] Impossible dreams
