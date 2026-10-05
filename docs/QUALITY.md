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
