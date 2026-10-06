# Reality Pass

This document replaces milestone checkboxes with gameplay acceptance. A feature is not complete because a model or manager exists; it must be reachable from normal Minecraft play and have a live test.

## Player ↔ NPC
- [x] Empty-hand interaction produces a grounded NPC reply.
- [x] Dialogue can recall a concrete prior event.
- [x] Player attack is remembered and changes relationship state.
- [x] Player attack produces personality-dependent flee/stand-ground behavior.
- [x] Brave/protective villagers can retarget an existing nearby iron golem to the attacker.
- [x] Sneak-right-click can give arbitrary items when the villager has real inventory capacity.
- [x] Item memories preserve the concrete item identifier.
- [ ] Full NPC inventory UI and slot inspection.
- [ ] Player can retrieve/give back arbitrary items through explicit interaction.

## Memory integrity
- [x] Memories persist in SavedData.
- [x] Inherited memories preserve item identity.
- [x] Direct interaction memories have explicit event types.
- [ ] Audit all event producers so every declared event type is actually emitted by gameplay.
- [ ] Audit all memory consumers so old memories influence behavior, not only dialogue text.
- [ ] Add bounded/cooldown semantics to every high-frequency event source.

## NPC autonomy
- [x] NPC-to-NPC conversation processing is invoked from the live tick loop.
- [ ] NPC conversation produces visible in-world behavior/audio, not only persisted records.
- [ ] Work, gathering, storage, eating and trading are driven by actual goals rather than navigation-only approximations.
- [ ] NPCs choose between flee, seek help, defend and negotiate using one shared decision layer.
- [ ] Defending NPCs can perform a real attack or delegated defense action when appropriate.

## Village society
- [ ] Phase 6 final integration.
- [ ] Migration identity GameTest passes on final branch.
- [ ] Storage GameTest passes on final branch.
- [ ] Village event GameTests pass on final branch.
- [ ] Population-scale performance passes on final branch.
- [ ] Final CI build + GameTests + client tests + server smoke test pass.

## Release rule
No phase receives a completion marker until every relevant checkbox above has a live implementation, a dedicated test where practical, and green CI evidence.
