# Architecture

## Simulation first

The world state is authoritative. Dialogue and future generative AI systems consume structured facts from the simulation; they do not directly mutate arbitrary world state.

## Planned modules

- MemorySystem — meaningful event storage and retrieval.
- EventSystem — converts gameplay actions into structured events.
- PersonalitySystem — traits and behavioral modifiers.
- RelationshipSystem — social relationships.
- BehaviorSystem — contextual NPC decisions.
- ConversationSystem — dialogue sessions.
- AnimationSystem — semantic animation triggers.
- FamilySystem — genealogy and generational memory.
- RumorSystem — knowledge propagation.
- VillageSystem — settlement simulation.
- ChronicleSystem — long-term history.
- VoiceSystem — microphone/STT/TTS integration.

## Event pipeline

Gameplay action -> Event -> Importance -> Memory -> Relationships -> Behavior -> Chronicle.

## Voice pipeline

Microphone -> local speech-to-text -> utterance -> ConversationSystem -> Memory/Relationships/World State -> grounded response -> local TTS -> spatial Minecraft audio.

Voice is an adapter around the simulation, not the simulation itself.

## Persistence

Persistent systems should use stable IDs and versioned serialized data so saves can migrate between mod versions.
