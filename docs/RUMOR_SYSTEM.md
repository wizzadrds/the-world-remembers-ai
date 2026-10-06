# Rumors & Conversations

## Goal
NPC knowledge must travel socially without becoming omniscient. A villager only knows facts it directly witnessed, learned from another NPC, or inherited through the existing family-memory system.

## Knowledge layers
- **DIRECT** — the NPC personally observed the event.
- **REPORTED** — another NPC explicitly told them.
- **RUMORED** — the information has passed through multiple social links and may degrade.

## Rules
1. Minecraft world state is authoritative.
2. NPCs never gain facts merely because the server knows them.
3. Knowledge has a source, subject, event type and observed time.
4. Every transfer records who transmitted the information.
5. Rumor confidence decreases with social distance and time.
6. Contradictory reports do not silently overwrite direct knowledge.
7. Dialogue must only use knowledge available to the speaking NPC.

## Phase 7 foundation
The first implementation establishes persistent knowledge facts and controlled propagation. Natural-language generation is deliberately downstream of the knowledge model.
