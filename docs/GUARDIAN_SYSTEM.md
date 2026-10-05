# Guardian & Witness System

## Golems as social guardians

Iron golems are treated as more than hostile-mob counters. They can respond to structured distress events from villagers.

Examples include attacks on villagers, threats, attacks on protected family members, attacks on village property, and a player killing a guardian.

## Distress flow

Villager detects a valid threat -> creates distress event -> nearby guardian evaluates event -> guardian may intervene.

## Witnesses

A witness is not simply any nearby NPC. The system records witness identity, event, location, time, perception conditions, and whether the witness survived and retained the memory.

If a player kills a golem in front of three villagers, each valid witness receives a GOLEM_KILLED memory. A villager too far away is not a direct witness and may only learn through rumors later.

## Knowledge levels

- DIRECT: saw or heard the event personally.
- REPORTED: learned it from another NPC.
- RUMORED: degraded or indirect knowledge.
- LEGENDARY: inherited or culturally important knowledge.

Two villagers can therefore tell different versions of the same event without contradicting the underlying simulation.
