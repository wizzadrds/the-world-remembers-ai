# NPC Behavior

Behavior is a persistent simulation state that drives real Minecraft actions.

## Activity states
IDLE, WORKING, EATING, SLEEPING, TRADING, TALKING, SOCIALIZING, WALKING, TRAVELLING, FOLLOWING_PLAYER, FLEEING and FAMILY.

Each state has an interruptibility flag and priority. Safety-critical actions outrank ordinary social/work actions.

## Follow behavior
Following is a real navigation state. Permission requires a sufficiently positive relationship and is denied when resentment, suspicion or critical stress dominates. The NPC moves through Minecraft navigation rather than teleporting.

## Travel awareness
Travel records destination, start position, start tick and measured distance. Dialogue/decisions can only use these observed values.

## Decision inputs
Activity, danger, personality, relationship, memory, stress, home distance, time, role and equipment all contribute to decisions. Simulation state remains authoritative.

## Role-aware behavior
Roles provide priorities, not personalities. Farmers prioritize nearby crops, merchants protect valuable stock, warriors prioritize defense and guardians prioritize village safety.

## Interruptions
A higher-priority danger can interrupt work/social activity. Sleeping, fleeing and active emergency states are protected from ordinary interruptions.
