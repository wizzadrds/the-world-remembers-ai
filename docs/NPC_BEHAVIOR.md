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
- FLEEING
- FAMILY

Each activity exposes whether it can be interrupted and its current priority.

## Conversation availability

A villager may give a short answer while busy, remain with the player when available, refuse when hostile or occupied, leave when repeatedly annoyed, or initiate a conversation when an important event gives them a reason.

## Decision inputs

- current activity
- danger
- personality
- relationship with player
- recent memories
- time of day
- social context
- conversation history

## Animation principle

Animation is a consequence of a behavioral decision. An NPC that accepts a conversation can turn toward the player and stop working. An annoyed NPC can turn away and leave.
