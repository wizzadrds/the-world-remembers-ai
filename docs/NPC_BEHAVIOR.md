# NPC Behavior

NPC behavior is contextual rather than scripted around dialogue alone.

## Activity states

- IDLE
- WORKING
- EATING
- SLEEPING
- TRADING
- TALKING
- SOCIALIZING
- WALKING
- TRAVELLING
- FOLLOWING_PLAYER
- FLEEING
- FAMILY

Each activity exposes whether it can be interrupted and its current priority.

## Following the player

A trusted NPC can eventually accept a direct “follow me” request.

Acceptance depends on:

- trust and affection;
- resentment and suspicion;
- personality;
- current activity;
- stress and fatigue;
- danger;
- distance from home;
- family or work obligations.

Following is a real activity, not teleportation. The NPC keeps a reasonable distance, navigates terrain, reacts to danger and can ask to stop or return home.

## Travel awareness

Long journeys are measured using real distance/time.

A villager can react to the journey:

- “Are we there yet?”
- “We're still far from home.”
- “I think we should turn back.”
- “This place is strange...”

The exact line depends on personality, stress, distance and current events. Dialogue cannot invent those facts.

## Conversation availability

A villager may give a short answer while busy, remain with the player when available, refuse when hostile or occupied, leave when repeatedly annoyed, or initiate a conversation when an important event gives them a reason.

## Decision inputs

- current activity
- danger
- personality
- relationship with player
- recent memories
- stress and fatigue
- home and distance from home
- time of day
- social context
- conversation history
- role/archetype
- equipment

## Role-aware behavior

A warrior may prioritize defending a friend or village. A farmer may prioritize crops. A merchant may protect valuable stock. The role never overrides personality, relationships or immediate survival automatically.

## Animation principle

Animation is a consequence of a behavioral decision. An NPC that accepts a conversation can turn toward the player and stop working. An annoyed NPC can turn away and leave. A following NPC can look toward the player while travelling.


## Travel dialogue foundation

Travel dialogue is selected from measured simulation state. Distance, travel time, stress and personality determine whether a line such as “Are we there yet?” is appropriate.

A line is not allowed to invent distance, danger, family concerns or exhaustion. If the corresponding state is absent, the dialogue layer must remain silent or choose a grounded alternative.


## Stress-aware return-home behavior

Critical stress can force an NPC to leave the current interaction. High stress reduces willingness to follow the player unless trust is strong. RETURN_HOME is reserved for future pathfinding integration and represents an explicit intent to return to the persistent home rather than simply leaving the player.
