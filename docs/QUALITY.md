# Quality & Testing

The mod is not considered production-ready for a milestone until the following layers pass.

## Automated layers

- [x] Java unit tests with JUnit + Fabric Loader JUnit.
- [x] Minecraft server GameTest source set.
- [x] Real Minecraft GameTest smoke coverage.
- [x] Headless Fabric server smoke test in GitHub Actions.
- [x] CI artifact upload for reports/logs on failure.
- [x] Full CI validation confirmed on PR #1.
- [x] Performance cadence regression test confirmed on PR #2.

## Phase 4 acceptance

- [ ] Home state is persistent and keyed by villager UUID.
- [ ] Home intrusion creates a persistent memory.
- [ ] Home intrusion increases stress.
- [ ] Home intrusion changes the relationship.
- [x] Intrusion events have a cooldown to prevent per-tick spam.
- [ ] Stress has bounded persistent state and recovery.
- [ ] Memory storage has a safety cap and tolerates obsolete/corrupt entries.
- [ ] Villager personalities are stable per UUID.
- [ ] Main-hand-only equipment model remains enforced.
- [ ] Crossed-arm idle silhouette remains a design invariant.

## Still required before Phase 4 can be marked complete

- [ ] Real villager pathfinding for following and returning home.
- [ ] Actual house boundary/door detection rather than a proximity prototype.
- [ ] Actual Minecraft inventory/equipment synchronization.
- [ ] Role-specific behavior in the live entity AI.
- [ ] Gathering, carrying, storage and chest links.
- [ ] Client GameTest for the crossed-arm animation.
- [ ] Stress/fatigue integration with live activity state.
- [ ] Population-scale performance test.


## CI validation
A pull-request CI run must pass `gradle build` and the headless Fabric server smoke test before a milestone can be marked complete.

## Phase 5 acceptance

- [x] Persistent age progression and calendar state.
- [x] Live parent/child linking with idempotent relation creation.
- [x] Live sibling formation from shared parents.
- [x] Live spouse/courtship linking.
- [x] Generational memory inheritance without re-inheriting inherited memories.
- [x] Important possessions inherited by children or the direct spouse.
- [x] Family members converge on a shared persistent home state.
- [x] Persistent family protector assignment and release after protector death.
- [x] Family protection decisions drive live villager navigation.
- [x] Dedicated Minecraft GameTests cover sibling formation, spouse inheritance, family homes and protection navigation.


## Phase 6 acceptance

- [x] Persistent village identity and population state.
- [x] Village history tracks first/last observation and population peak.
- [x] Village-scale events persist for migration and resident/guardian deaths.
- [x] Live bread inventory drives derived resource pressure.
- [x] Real container inventories drive shared storage state.
- [x] Nearby iron golems drive village defense state.
- [x] Real village POIs are persisted as landmarks.
- [x] Settlement movement preserves village identity and records migration.
- [x] Dedicated GameTests cover history, resources, defense, landmarks, storage, migration and events.
- [ ] Population-scale performance validation is green in CI.

Phase 6 is not marked complete until the final pull-request CI run passes both gradle build and the headless Fabric server smoke test.
