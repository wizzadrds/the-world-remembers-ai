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

- [x] Home state persists by villager UUID.
- [x] Home intrusion creates persistent memory.
- [x] Home intrusion increases stress.
- [x] Home intrusion changes relationship state.
- [x] Intrusion cooldown prevents per-tick spam.
- [x] Stress is bounded and recovers under safe conditions.
- [x] Memory storage has a safety cap and tolerant loading.
- [x] Personality remains stable per UUID.
- [x] Main-hand/live equipment state is synchronized from Minecraft.
- [x] Activity state exposes priority and interruptibility.
- [x] Real navigation is used for follow/leave/return-home behavior.
- [x] Real nearby beds, doors and containers ground home state.
- [x] Role-aware work and threat navigation affect live villagers.
- [x] Live inventory state is synchronized from actual villager inventory.
- [x] Home storage links reference real Minecraft containers.
- [x] Stress/fatigue are integrated with live activity.
- [x] Phase 4 GameTests and CI validation pass.


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
- [x] Population-scale performance validation is green in CI.

Phase 6 is not marked complete until the final pull-request CI run passes both gradle build and the headless Fabric server smoke test.


## Phase 6 acceptance
- [x] Persistent village identity and population
- [x] Village history and typed events
- [x] Resource pressure from live inventories
- [x] Shared storage derived from real containers
- [x] Defense state from live iron golems
- [x] Landmarks from real village POIs
- [x] Migration identity preservation
- [x] Village-scale GameTests
- [x] Population-scale performance GameTest
