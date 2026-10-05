# NPC Inventory & Item Simulation

## Principle

NPC inventory is authoritative world state. Dialogue may describe an item only when the simulation confirms the NPC owns it or has valid knowledge of it.

## Inventory layers

- Personal inventory: items currently carried by the NPC.
- Home storage: linked chests or containers associated with the NPC home.
- Village storage: future shared storage.
- Important possessions: optional tagged items that matter socially or historically.

## Item lifecycle

Acquire -> carry -> use/trade/store -> retrieve -> lose/destroy/inherit.

## Trade

Trading is a real state transition. The player gives an item, the NPC receives it, the NPC gives the trade result, and the relationship/event systems record the transaction.

## Threats

A future conversation system can interpret a threat such as asking for all emeralds. The simulation then checks the actual emerald count, personality, relationship, escape options and available help before deciding whether the NPC surrenders, flees, calls for help or resists.

The dialogue layer never creates emeralds or removes them without a corresponding inventory transaction.

## Storage

NPCs may return resources to linked home containers. Future village systems can make stored resources available to family and settlement simulation.
