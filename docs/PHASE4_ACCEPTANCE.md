# Phase 4 Behavior Acceptance

Phase 4 is complete only when live villagers use persistent activity state, real navigation, home boundaries, stress/fatigue, inventory/equipment synchronization and role-aware decisions.

## Required acceptance
- Persistent activity state keyed by villager UUID.
- Activity priorities and interruption rules.
- Follow/stop behavior requires relationship permission and respects stress.
- Real navigation is used for following and returning home.
- Home state uses nearby real Minecraft home signals (bed/door) when available.
- Intrusions create memory, stress and relationship consequences with cooldown.
- Stress remains bounded and recovers from safe/restful states.
- Persistent memory entries are bounded and malformed legacy entries do not crash loading.
- Live inventory/equipment state mirrors real villager equipment.
- Gathering/carrying/storage uses real item state.
- Role-aware decisions influence behavior without overriding survival.
- Dedicated Minecraft GameTests cover the live behavior invariants.
- Population-scale processing remains within the project performance budget.

## Source of truth
Minecraft entity, inventory, POI and block state are authoritative. Persistent simulation state records derived intent/history and never invents world facts.
