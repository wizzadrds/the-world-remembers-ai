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
- [x] Final Phase 5 CI validation passed on PR #23 (Gradle build/tests + headless Fabric server smoke test).

## Phase 6 acceptance

- [x] Persistent village identity and population state.
- [x] Village history and typed resident/guardian events.
- [x] Live inventory resource pressure.
- [x] Real container shared storage.
- [x] Live iron-golem defense state.
- [x] Real village POI landmarks.
- [x] Migration identity preservation and movement history.
- [x] Dedicated village GameTests.
- [x] Population-scale performance GameTest.
- [x] Final CI #287: Gradle build/GameTests and headless Fabric server smoke test passed.


## Phase 7 acceptance

- [x] Witness-based knowledge with explicit provenance.
- [x] Direct, reported and rumored knowledge remain distinguishable.
- [x] Reported knowledge degrades and becomes rumor on further propagation.
- [x] Rumor confidence degrades with age.
- [x] Persistent NPC-to-NPC conversation records.
- [x] Grounded dialogue exposes knowledge provenance and confidence.
- [x] Live NPC-to-NPC rumor exchange GameTest.
- [x] Final CI #314: Gradle build/GameTests and headless Fabric server smoke test passed.


## Phase 8 acceptance

- [x] Local STT adapter contract with no online dependency.
- [x] Local TTS adapter contract with no online dependency.
- [x] Stable voice profiles remain independent of runtime engines.
- [x] Spatial delivery derives attenuation from world coordinates.
- [x] Voice scheduler is deterministic, bounded and priority-aware.
- [x] State-driven delivery modifies rate, pitch and expressiveness.
- [x] Local adapter failures degrade without blocking gameplay.
- [x] Live client/server voice payload integration GameTest.
- [x] Final CI #348: Gradle build/tests, headless Fabric server smoke test and client GameTests passed.


## Phase 9 acceptance

- [x] Bounded Chronicle aggregation derives timeline data from persistent memory and village events.
- [x] People, relationships, families and village history are exposed without creating a second source of truth.
- [x] Client/server Chronicle snapshot networking is registered and exercised by the live client environment.
- [x] Read-only Chronicle UI opens from the J key and renders bounded history.
- [x] Dedicated Chronicle client GameTest and screenshot coverage pass.
- [x] Final CI #379: Gradle build/tests, headless Fabric server smoke test and client GameTests passed.


## v1.0 release acceptance
- [x] Phases 0–10 are individually marked complete.
- [x] Unit-test, GameTest and headless-server quality layers exist and are required by CI.
- [x] Phase 10 final CI validation is recorded.
- [ ] Final v1.0 release CI passes Gradle build/tests, GameTests and headless Fabric server smoke test.
- [ ] Final v1.0 client validation passes where client-facing systems are present.
