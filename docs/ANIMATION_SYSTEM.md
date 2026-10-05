# Animation System

Animations communicate decisions made by the behavior system.

Examples:
- turn toward player when engaged
- stop work when choosing to talk
- resume work after an interruption
- look away when uncomfortable
- move away when annoyed
- follow when agreeing to accompany the player
- flee when danger overrides conversation

Animation triggers should be semantic events, not hard-coded inside dialogue text.


## Villager silhouette rule

The classic villager crossed-arms pose is preserved as part of the character identity and humor of the mod.

- Idle villagers keep their arms crossed.
- No generic combat-ready pose should replace the recognizable villager silhouette.
- When a villager actively uses a main-hand item, the animation may temporarily represent that action.
- After the action, the villager returns to the crossed-arms idle pose.
- There is no off-hand animation system for villagers.
