# Voice and Personality

Voice should communicate who an NPC is.

## Voice dimensions

Each NPC can have a stable voice profile containing:

- temperament;
- pitch;
- speaking rate;
- expressiveness;
- language/voice model.

Examples:

- **TIMID** — quieter, cautious, sometimes hesitant.
- **NERVOUS** — unstable or quicker delivery during danger.
- **CALM** — measured pace and low intensity.
- **WARM** — softer and friendly.
- **CHEERFUL** — brighter and expressive.
- **ASSERTIVE** — confident and direct.
- **IRRITABLE** — clipped and sharper.
- **TIRED** — slower and lower energy.
- **SERIOUS** — controlled and deliberate.
- **EXCITED** — faster and energetic.

The same NPC can change delivery with state. A normally cheerful villager can sound nervous after a zombie attack or exhausted after a long journey.

## Personality -> voice

Personality traits influence voice delivery, but do not force every NPC with the same trait to sound identical.

Two timid villagers can still sound different: one may speak softly and pause often, while another may speak quickly when nervous.

## Grounding rule

Voice never invents facts.

Personality + Relationship + Memory + Stress + Activity + Place + Event -> grounded response -> voice delivery.

## Local-first

The planned stack remains local-first:

Microphone -> STT -> grounded response -> TTS -> spatial audio.

Voice engines remain adapters so the project is not tied to one provider.
