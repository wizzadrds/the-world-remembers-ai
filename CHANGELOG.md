# Changelog

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

### Project bootstrap
- Added project vision and design principles.
- Added roadmap from v0.1-alpha to v1.0.
- Documented memory, behavior, animation and voice architecture.
- Defined local-first voice direction using faster-whisper and Piper as initial adapters.
