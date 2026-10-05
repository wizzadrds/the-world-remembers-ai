# Phase 4 Acceptance

Phase 4 is complete only when live Minecraft behavior is grounded in real villager state and demonstrated by automated tests.

## Acceptance
- [ ] Activity states drive live decisions.
- [ ] Decision engine is exercised by live NPC behavior.
- [ ] Busy/available state changes interaction outcomes.
- [ ] Annoyed NPCs can leave through real navigation.
- [ ] Trusted NPCs can follow through real navigation.
- [ ] Follow permission respects relationship state.
- [ ] Travel distance/time affects behavior.
- [ ] Stress and recovery affect decisions and persist.
- [ ] Homes use real house/door/bed discovery rather than a spawn-position placeholder.
- [ ] Home intrusion creates memory, stress and relationship consequences with cooldown.
- [ ] Inventory state synchronizes with real villager inventory/equipment.
- [ ] Gathering/carrying/storage uses real Minecraft items and containers.
- [ ] Threat responses and calls for help use real navigation.
- [ ] Role/archetype affects live behavior.
- [ ] Equipment/armor synchronization is real and main-hand constraints are enforced.
- [ ] Contextual animation is validated by deterministic animation-state tests or client-capable tests.
- [ ] Population-scale performance remains within the project's CI budget.

## Completion rule
A green unit-test build alone is insufficient. The final PR must pass Gradle build, Minecraft GameTests, and the headless Fabric server smoke test in CI. Then PHASES, QUALITY, README and CHANGELOG are updated together.
