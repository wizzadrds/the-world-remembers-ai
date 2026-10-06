# Phase 6 Final Validation

Final acceptance is intentionally stricter than compilation.

- Persistent village identity and population are exercised in live GameTests.
- Village history and typed resident/guardian events are persisted.
- Resource pressure is derived from live villager inventories.
- Shared storage is derived from real Minecraft containers.
- Defense state is derived from live iron golems.
- Landmarks are derived from real village POIs.
- Migration preserves the village UUID and records origin/destination.
- Population-scale observation is measured with 40 live villagers.
- Final CI must pass Gradle build, all automated tests, Minecraft GameTests and the headless Fabric server smoke test.

The milestone is not considered complete if any of these checks is skipped or merely inferred.
