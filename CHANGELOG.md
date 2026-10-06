# Changelog

## v1.0.0 — The World Remembers
- Integrated the completed memory, personality, relationships, behavior, families, villages, rumors, local voice, chronicles and dreams systems.
- Added explicit v1.0 release acceptance criteria and final CI gates.
- Final CI run #409 passed Gradle build/tests, Minecraft client GameTests and headless Fabric server smoke test.

## v0.10.0-alpha — Phase 10 Dreams
- Added persistent bounded dream history per NPC.
- Added memory, fear, nostalgia and explicitly surreal impossible dream types.
- Added sleep-gated dream generation with cooldown protection.
- Added deterministic dream selection and persistence tests.
- Added Phase 10 Minecraft GameTests and CI validation.

## v0.9.0-alpha — Phase 9 Chronicles
- Added a bounded Chronicle aggregation layer over persistent memory, relationships, families and village history.
- Added server/client Chronicle networking with read-only world-history snapshots.
- Added a client Chronicle screen opened with the J key.
- Added timeline, people, relationships, families and village-history sections.
- Added a live client GameTest with screenshot coverage.
- Final CI run #379 passed Gradle build/tests, server GameTests, headless server smoke test and client GameTests.


## v0.8.0-alpha — Phase 8 Local Voice
- Added provider-agnostic local STT and TTS adapter contracts.
- Added local process adapters suitable for faster-whisper and Piper command wrappers.
- Added deterministic bounded voice scheduling with priority handling.
- Added spatial attenuation and state-driven delivery for timid, nervous, tired, angry and excited states.
- Added graceful failure handling so unavailable local voice engines do not block gameplay.
- Added real Fabric client/server voice payload networking and a live client GameTest.
- Final CI run #348 passed Gradle build/tests, headless server smoke test and client GameTests.

## v0.7.0-alpha — Phase 7 Rumors & Conversations
- Added persistent knowledge with direct/reported/rumored provenance.
- Added confidence degradation for reported and rumored knowledge.
- Added persistent NPC-to-NPC conversations.
- Added grounded dialogue that exposes provenance and confidence.
- Added live rumor exchange GameTest and final CI validation (PR #104).

<!-- Final Phase 6 CI verification trigger -->

## v0.6.0-alpha — Phase 6 Villages & Society
- Added persistent village identity, population and history.
- Added typed village events, migration history and resident/guardian death memory.
- Added live resource pressure and shared storage derived from real Minecraft state.
- Added village defense, landmarks and population-scale validation.
- Added dedicated village GameTests and final CI validation (run #287).

## v0.4.0-alpha — Phase 4 Behavior
- Completed live NPC behavior integration.
- Added persistent homes, stress, travel state, inventory/equipment synchronization and role-aware behavior.
- Added live navigation, home intrusion consequences, work/threat responses and Phase 4 GameTests.
- Final CI validation passed: Gradle build/tests and headless Fabric server smoke test on PR #80.

## Phase 4 Behavior — validated
- Completed live NPC activity and decision integration.
- Added real home, door and container discovery with persistent home storage links.
- Added live inventory and equipment synchronization.
- Added bounded stress/fatigue integration and role-aware work/threat navigation.
- Added deterministic Phase 4 GameTest coverage and CI validation.

## v0.5.0-alpha — Phase 5 Families & Generations
- Completed live parent, sibling, spouse and child relationships.
- Added idempotent family linking and sibling formation from shared parents.
- Added generational memory propagation and important-item inheritance.
- Added direct-spouse inheritance fallback.
- Added family-aware shared homes for spouses, parents and children.
- Added persistent family protection with live navigation behavior.
- Added GameTests covering sibling formation, spouse inheritance, family homes and protection.

## Unreleased — Phase 5 foundation
- Added persistent NPC age state keyed by villager UUID.
- Added persistent parent/child/sibling/spouse relationship primitives.
- Added integrity validation preventing self-family relations.
- Added birth, death, marriage, family-loss and inheritance memory event types.
- Added unit coverage for age and family invariants.
- Phase 4 remains intentionally unmarked until real Minecraft build/GameTest validation passes.

## v0.4.0-alpha

### Phase 3 — Relationships
- Completed the persistent relationship layer.
- Added relationship consequences for threats, attacks, rescues and guardian loss.
- Added reusable trusted, afraid, hostile and protective-bond relationship queries.
- Relationship changes remain bounded between -100 and 100.
- Relationship state can be queried without creating a new relationship entry.
- Expanded the event model so future behavior, guardian and rumor systems can react to meaningful social events.

## v0.3.0-alpha

### Phase 2 — Personality
- Personality profiles now expose reusable decision helpers.
- NPC behavior uses social, brave, cowardly, hot-tempered, suspicious and protective traits.
- Personality can produce different decisions from the same relationship state.
- Phase checklist updated.

## v0.2.0-alpha

### Phase 1 — The First Memory
- Completed the first persistent memory interaction.
- Bread gifts are remembered across reloads.
- The first interaction is distinguished from a later recollection.
- Bread gifts update the NPC/player relationship.
- Development phases now use checked acceptance items.
- Added the persistent NPC age data model and documented its Phase 5 integration.

## Unreleased

### Phase 4 — Behavior foundation
- Added persistent NPC inventory state with item counts.
- Added real inventory add/remove/count transaction primitives.
- Expanded event vocabulary for item lifecycle, theft and guardian interactions.
- Added surrender, resist and call-for-help behavior decisions for upcoming threat simulation.


### Project bootstrap
- Added project vision and design principles.
- Added roadmap from v0.1-alpha to v1.0.
- Documented memory, behavior, animation and voice architecture.
- Defined local-first voice direction using faster-whisper and Piper as initial adapters.
