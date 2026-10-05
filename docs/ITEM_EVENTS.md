# Item & Gameplay Events

Planned event types include:

- PLAYER_GAVE_ITEM
- PLAYER_RECEIVED_ITEM
- NPC_TRADED_ITEM
- NPC_COLLECTED_ITEM
- NPC_STORED_ITEM
- NPC_RETRIEVED_ITEM
- NPC_USED_ITEM
- PLAYER_STOLE_ITEM
- PLAYER_THREATENED_NPC
- PLAYER_ATTACKED_NPC
- PLAYER_SAVED_NPC
- GOLEM_CALLED_FOR_HELP
- GOLEM_DEFENDED_VILLAGE
- GOLEM_KILLED
- VILLAGE_PROPERTY_DAMAGED

Events must contain enough structured context for memories, relationships, witnesses and future chronicles.

## Event integrity

An event may only claim facts supported by the simulation. An item transaction must have matching inventory changes. A witness must satisfy perception rules. A save must correspond to a real threat. A theft must correspond to an item leaving its rightful storage.
