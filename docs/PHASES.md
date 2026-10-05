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
- [ ] Activity states
- [ ] Decision engine
- [ ] Busy vs available NPCs
- [ ] Leaving when annoyed
- [ ] Following when appropriate
- [ ] Follow-me trust/permission rules
- [ ] Travel distance awareness
- [ ] Stress and fatigue affecting decisions
- [ ] Persistent home assignment and access rules
- [ ] Home intrusion detection and consequences
- [ ] Home storage links
- [x] Persistent home data model
- [x] Persistent stress data model
- [x] Travel state model
- [ ] Interruptions and priority
- [ ] Contextual animations
- [x] NPC inventory and real item state
- [ ] Gathering, carrying and storage
- [ ] Chest/home storage links
- [ ] Economic decisions
- [ ] Threat responses and calls for help
- [ ] Role-aware behavior (farmer, merchant, warrior, guardian)
- [ ] NPC equipment and armor synchronization
- [x] NPC role/archetype model
- [x] Voice temperament model foundation

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

**Status: COMPLETE — v0.5.0-alpha**\n\n## Phase 6 — Villages & Society
- [ ] Village identity and persistent village state
- [ ] Population tracking
- [ ] Village history and important events
- [ ] Wealth and resource pressure
- [ ] Shared village resources and storage
- [ ] Village defense state
- [ ] Migration and settlement change
- [ ] Important buildings and landmarks
- [ ] Iron golems as social guardians
- [ ] Village-scale GameTests
- [ ] Population-scale performance validation

## Phase 7 — Rumors & Conversations
- [ ] Witness-based knowledge
- [ ] Rumor degradation
- [ ] Persistent conversations
- [ ] NPC-to-NPC conversations
- [ ] Grounded dialogue
- [ ] Direct, reported and rumored knowledge

## Phase 8 — Local Voice
- [ ] Local STT
- [ ] Local TTS
- [x] Voice profile model foundation
- [x] Personality-driven voice temperament model
- [ ] Spatial audio
- [ ] Voice scheduling
- [ ] State-driven delivery (timid, nervous, tired, angry, excited)

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

## Phase 3 acceptance test

A relationship changes when a meaningful event occurs, the change is clamped to safe bounds, persists with world state, and later behavior can query the resulting relationship without inventing facts.
