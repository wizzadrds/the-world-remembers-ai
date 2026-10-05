# NPC Equipment

NPC equipment is persistent simulation state.

## Rules

- Items cannot appear from dialogue.
- Equipping consumes an existing item.
- Unequipping returns the item to inventory.
- Theft removes the item from its rightful owner/storage.
- Breaking or losing an item creates a real event.
- Important possessions can be remembered.
- Family members can later inherit important equipment.

## Combat equipment

Warriors can eventually use swords, axes, shields, bows/crossbows and armor pieces.

Equipment changes behavior. A well-equipped brave warrior may defend a friend, while a poorly equipped or frightened warrior may retreat and call for help.

## Minecraft integration

The simulation layer stores authoritative equipment state. The Minecraft entity layer will later synchronize it with actual armor and hand slots.

This keeps behavior testable without making dialogue or personality code manipulate Minecraft item stacks directly.
