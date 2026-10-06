# v1.0 Acceptance

v1.0 is the first release candidate for **The World Remembers**.

## Product contract
- [ ] Persistent memory survives reloads.
- [ ] Relationships, families and generations remain consistent.
- [ ] Villages retain identity, history, resources, storage, defense and migration state.
- [ ] Rumors and conversations remain grounded in simulation state.
- [ ] Local voice is optional and fails gracefully when adapters are unavailable.
- [ ] Chronicles are read-only and never mutate authoritative world state.
- [ ] Dreams are bounded, persistent and explicitly non-authoritative.

## Technical contract
- [ ] Clean Gradle build and unit tests.
- [ ] Full Minecraft GameTest suite passes.
- [ ] Headless Fabric server smoke test passes.
- [ ] Client GameTests pass where applicable.
- [ ] Population-scale performance test passes.
- [ ] Persistent SavedData loads after restart.
- [ ] No known release-blocking compile/runtime errors.

## Release hardening
- [ ] Version set to 1.0.0-rc1.
- [ ] README and changelog describe the release candidate.
- [ ] Phase documentation records the RC gate.
- [ ] Final CI run is green.
- [ ] Final release artifact is reproducible.

Only after every checkbox is satisfied may the RC become **v1.0.0**.
