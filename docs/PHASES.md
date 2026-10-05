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
- [ ] Interruptions and priority
- [ ] Contextual animations
- [ ] NPC inventory and real item state
- [ ] Gathering, carrying and storage
- [ ] Chest/home storage links
- [ ] Economic decisions
- [ ] Threat responses and calls for help

## Phase 5 — Families & Generations
- [ ] Persistent age
- [ ] Parents, siblings, spouses and children
- [ ] Births and deaths
- [ ] Generational memory
- [ ] Inherited and important possessions

## Phase 6 — Villages & Society
- [ ] Village history
- [ ] Population
- [ ] Wealth and shortages
- [ ] Defenses
- [ ] Migration
- [ ] Important buildings and events
- [ ] Shared resources and village storage
- [ ] Iron golems as social guardians

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
- [ ] Voice profiles
- [ ] Spatial audio
- [ ] Voice scheduling

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
