# Phase 4 Acceptance

Phase 4 is complete only when behavior is persistent, deterministic and demonstrated by Minecraft GameTests.

- Activity state is persistent per NPC and exposes interruptibility/priority.
- Follow behavior uses real navigation and trust/permission rules.
- Travel state tracks destination, distance and elapsed travel time from world state.
- Stress is bounded, persistent and changes with live safety/activity conditions.
- Home assignment persists and uses real nearby beds/doors when available.
- Home intrusion is based on actual player position crossing the home boundary, not proximity alone.
- Intrusion produces stress, memory and relationship consequences with cooldown.
- Inventory/equipment state synchronizes with real villager item stacks.
- Role-aware behavior changes live decisions without overriding personality/relationships.
- Gathering/carrying/storage decisions use real blocks/items and never invent resources.
- Interruptions respect activity priority and safety.
- Population-scale behavior processing has a regression test.
- CI passes Gradle build, unit tests, Minecraft GameTests and headless server smoke test.
