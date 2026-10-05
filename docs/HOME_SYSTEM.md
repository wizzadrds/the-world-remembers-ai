# Home System

Every important NPC should eventually have a persistent home identity.

## Home identity

A home is linked to an NPC UUID and can contain:

- home position;
- entrance;
- bed;
- personal storage;
- important possessions;
- future family members;
- memories associated with the place.

The home assignment persists across sessions. A home is not recreated just because the NPC loads again.

## Ownership and access

The home is a social space, not only a block structure.

Access can be:

- ALLOWED — the NPC welcomes the player.
- CONDITIONAL — the NPC may allow entry depending on context.
- DENIED — the NPC considers the entry unauthorized.

Permission is derived from relationship, personality, invitation/context and recent events.

A trusted friend may enter without anger. A suspicious or resentful NPC may react strongly to the same action.

## Intrusion

Unauthorized entry can become a meaningful event.

The system should:

1. detect the actual player entering the home;
2. identify the owner;
3. check permission;
4. determine whether the owner witnessed or later discovered it;
5. increase stress when appropriate;
6. create memory when the event is important;
7. update the relationship;
8. trigger behavior such as confrontation, leaving, calling for help or protecting possessions.

The system must not claim that an NPC witnessed something it did not witness.

## Storage

Home storage is linked to the NPC inventory/equipment system.

Items placed in a chest or personal storage must correspond to real state transitions. Theft removes real items and can create memories.

## Future family integration

Families will later share homes. Children, spouses, inheritance and important possessions will use the same persistent home identity.


## Permission policy foundation

Home access is evaluated from actual relationship state and personality.

- trusted/affectionate relationships can allow entry;
- suspicious or resentful relationships can deny entry;
- intermediate relationships can be conditional;
- an explicit invitation can allow entry regardless of ordinary thresholds.

The thresholds are implementation defaults and can evolve as the simulation becomes richer.
