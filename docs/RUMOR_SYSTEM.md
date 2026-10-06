# Rumors & Conversations

## Goal
NPC knowledge travels socially without becoming omniscient. A villager only knows facts it directly witnessed or learned through an explicit social transfer.

## Knowledge layers
- **DIRECT** — personally witnessed.
- **REPORTED** — explicitly learned from another NPC.
- **RUMORED** — propagated beyond the reliable source and subject to degradation.

## Invariants
1. Minecraft/world state is authoritative.
2. No NPC gains facts merely because the server knows them.
3. Every knowledge fact has an event identity, subject, source, acquisition tick and confidence.
4. A transfer records the transmitting NPC.
5. Confidence decreases with propagation distance and time.
6. Direct knowledge is never silently downgraded by a rumor.
7. Contradictory reports remain distinguishable.
8. Re-processing the same event is idempotent.

## Dialogue boundary
Dialogue may verbalize only facts present in the speaker's knowledge. Dialogue cannot create a new world-state event.
