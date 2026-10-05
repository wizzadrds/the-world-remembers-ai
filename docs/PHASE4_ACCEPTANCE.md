# Phase 4 Acceptance

Phase 4 is complete only when live NPC behavior is demonstrated in Minecraft GameTests and CI: persistent homes, intrusion consequences, bounded stress/recovery, grounded pathfinding, inventory/equipment synchronization, role-aware behavior, gathering/carrying/storage, threat responses, interruptions/priorities and contextual animation invariants.

## Required proof
- Persistent home survives reload/save data.
- Intrusion creates memory, stress and relationship consequence with cooldown.
- Stress remains 0..100 and recovers from safe conditions.
- NPC follows trusted player only when behavior rules allow it.
- NPC returns toward its persistent home when appropriate.
- NPC inventory mirrors important live item state.
- Main-hand equipment synchronization is enforced.
- Role/archetype changes live decisions.
- Gathering/carrying/storage links use real Minecraft containers.
- Threats trigger flee/call-for-help behavior.
- Higher-priority interruptions supersede lower-priority activity.
- Contextual animation state remains consistent with behavior.
- Population-scale behavior processing passes CI.
