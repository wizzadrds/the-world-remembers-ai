package com.wizzadrds.theworldremembers.inventory;

import java.util.ArrayList;
import java.util.List;

/** A bounded NPC inventory model: eight slots, 64 items per stack, matching the vanilla villager inventory scale. */
public final class NpcInventory {
    public static final int SLOT_COUNT = 8;
    public static final int MAX_STACK_SIZE = 64;
    private final List<NpcItemStack> items = new ArrayList<>();
    public NpcInventory() {}
    public NpcInventory(List<NpcItemStack> initial) { for (NpcItemStack stack : initial) add(stack.itemId(), stack.count()); }
    public List<NpcItemStack> items() { return List.copyOf(items); }
    public int usedSlots() { return items.size(); }
    public int freeSlots() { return SLOT_COUNT - items.size(); }
    public boolean hasFreeSlot() { return freeSlots() > 0; }
    public int count(String itemId) { return items.stream().filter(s -> s.itemId().equals(itemId)).mapToInt(NpcItemStack::count).sum(); }
    public boolean canAdd(String itemId, int count) {
        if (count < 1) return false;
        int existing = 0;
        for (NpcItemStack stack : items) if (stack.itemId().equals(itemId)) existing += stack.count();
        int capacity = (existing > 0 ? MAX_STACK_SIZE - Math.min(existing, MAX_STACK_SIZE) : 0) + freeSlots() * MAX_STACK_SIZE;
        return count <= capacity;
    }
    public boolean add(String itemId, int count) {
        if (!canAdd(itemId, count)) return false;
        int remaining = count;
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            NpcItemStack stack = items.get(i);
            if (!stack.itemId().equals(itemId) || stack.count() >= MAX_STACK_SIZE) continue;
            int moved = Math.min(MAX_STACK_SIZE - stack.count(), remaining);
            items.set(i, new NpcItemStack(itemId, stack.count() + moved));
            remaining -= moved;
        }
        while (remaining > 0) {
            int moved = Math.min(MAX_STACK_SIZE, remaining);
            items.add(new NpcItemStack(itemId, moved));
            remaining -= moved;
        }
        return true;
    }
    public boolean remove(String itemId, int count) {
        if (count < 1 || count(itemId) < count) return false;
        int remaining = count;
        for (int i = items.size() - 1; i >= 0 && remaining > 0; i--) {
            NpcItemStack stack = items.get(i);
            if (!stack.itemId().equals(itemId)) continue;
            int taken = Math.min(stack.count(), remaining);
            remaining -= taken;
            int left = stack.count() - taken;
            if (left == 0) items.remove(i); else items.set(i, new NpcItemStack(itemId, left));
        }
        return true;
    }
}
