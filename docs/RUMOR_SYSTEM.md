# Rumor & Conversation System

## Goal
NPC knowledge should spread through witnessed events and conversations without inventing facts.

## Knowledge provenance
Every social fact has a provenance:
- **DIRECT** — witnessed or directly experienced.
- **REPORTED** — communicated by another NPC with known provenance.
- **RUMORED** — passed through social links and subject to bounded degradation.

## Source of truth
World state and persistent memory remain authoritative. Dialogue may express knowledge, but cannot create world-state facts that never happened.

## Phase 7 foundation
- Witnesses come from real nearby entities at event time.
- Knowledge retains subject, event type, source and confidence.
- Rumor transmission is deterministic and bounded.
- Repeated transmission does not create duplicate facts.
- Degradation changes confidence/provenance, not the underlying event.

## Conversations
Conversation sessions are persistent social records referencing real NPCs and real knowledge records, not unbounded generated chat logs.
