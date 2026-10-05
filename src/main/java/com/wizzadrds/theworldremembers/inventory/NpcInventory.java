package com.wizzadrds.theworldremembers.inventory;

import java.util.ArrayList;
import java.util.List;

public final class NpcInventory {
    private final List<NpcItemStack> items = new ArrayList<>();
    public NpcInventory() {}
    public NpcInventory(List<NpcItemStack> initial) { items.addAll(initial); }
    public List<NpcItemStack> items() { return List.copyOf(items); }
    public int count(String itemId) { return items.stream().filter(s -> s.itemId().equals(itemId)).mapToInt(NpcItemStack::count).sum(); }
    public void add(String itemId, int count) {
        if (count < 1) throw new IllegalArgumentException("count must be positive");
        for (int i = 0; i < items.size(); i++) {
            NpcItemStack stack = items.get(i);
            if (stack.itemId().equals(itemId)) { items.set(i, new NpcItemStack(itemId, stack.count() + count)); return; }
        }
        items.add(new NpcItemStack(itemId, count));
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
