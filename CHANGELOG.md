# Changelog

## v0.6.0-alpha — Phase 6 Villages & Society
- Added persistent village identity and population.
- Added village history, typed events and migration memory.
- Added resource pressure, real shared storage observation and defense state.
- Added village landmarks and iron-golem guardian observation.
- Added village-scale and population-scale GameTests.


## v0.6.0-alpha — Phase 6 Village Society
- Added persistent village identity, population and historical state.
- Added village-scale migration, event, resource, storage, defense and landmark state.
- Added real container inventory observation and derived resource pressure.
- Added live migration identity preservation and guardian/resident death events.
- Added village-scale GameTests and population-scale validation.

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
