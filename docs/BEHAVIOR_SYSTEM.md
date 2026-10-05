# Phase 4 Behavior System

## Persistent state
Each villager has a persistent behavior state keyed by UUID: activity, priority, interruptibility, follow target, travel origin, last decision tick and fatigue.

## Navigation
Following and returning home use Minecraft navigation. Teleportation is forbidden.

## Home grounding
Home assignment prefers real nearby beds and doors. If none are available, the existing persistent fallback position is retained.

## Permission
A follow request is accepted only when relationship trust/affection is sufficient, the NPC is not hostile, and stress is below the refusal threshold. Family protection can override ordinary following.

## Stress/fatigue
Stress is bounded to 0..100. Fatigue is bounded to 0..100 and recovers during rest/safe home activity.

## Priority
Survival and family protection outrank social following. Sleeping, fleeing and critical safety states are not interrupted by ordinary player interaction.
