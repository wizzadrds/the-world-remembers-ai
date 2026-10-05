# Development Phases

The project follows a staged implementation plan. Features are documented before implementation and integrated only when their phase is ready.

## Phase 0 — Foundation
- Fabric project
- Stable package structure
- Persistent world state
- Core event and memory models

## Phase 1 — The First Memory
- Important event detection
- NPC memories
- Memory importance
- Player/NPC relationships
- First remembered interaction

## Phase 2 — Personality
- Stable personality profiles
- Personality affects decisions
- Different reactions to identical events

## Phase 3 — Relationships
- Trust, gratitude, fear, respect, affection, resentment, suspicion
- Relationship persistence
- Relationship consequences from events

## Phase 4 — Behavior
- Activity states
- Decision engine
- Busy vs available NPCs
- Leaving when annoyed
- Following when appropriate
- Interruptions and priority
- Contextual animations

## Phase 5 — Families & Generations
- Persistent age
- Parents, siblings, spouses and children
- Births and deaths
- Generational memory

## Phase 6 — Villages & Society
- Village history
- Population
- Wealth and shortages
- Defenses
- Migration
- Important buildings and events

## Phase 7 — Rumors & Conversations
- Witness-based knowledge
- Rumor degradation
- Persistent conversations
- NPC-to-NPC conversations
- Grounded dialogue

## Phase 8 — Local Voice
- Local STT
- Local TTS
- Voice profiles
- Spatial audio
- Voice scheduling

## Phase 9 — Chronicles
- Timeline UI
- People
- Relationships
- Families
- Village history

## Phase 10 — Dreams
- Memory dreams
- Fear dreams
- Nostalgia
- Impossible dreams

## Phase 1 acceptance test

A villager remembers a meaningful player action after the player leaves and returns later. The stored memory is the source of truth for the response.
