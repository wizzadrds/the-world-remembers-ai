package com.wizzadrds.theworldremembers.inventory;

public record NpcItemStack(String itemId, int count) {
    public NpcItemStack {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId cannot be blank");
        if (count < 1) throw new IllegalArgumentException("count must be positive");
    }
}
