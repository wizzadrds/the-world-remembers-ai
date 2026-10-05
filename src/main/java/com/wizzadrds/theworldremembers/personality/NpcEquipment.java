package com.wizzadrds.theworldremembers.personality;

public record NpcEquipment(
        String helmet,
        String chestplate,
        String leggings,
        String boots,
        String mainHand
) {
    public static NpcEquipment empty() {
        return new NpcEquipment(null, null, null, null, null);
    }

    public boolean hasArmor() {
        return helmet != null || chestplate != null || leggings != null || boots != null;
    }

    public boolean hasMainHandItem() {
        return mainHand != null;
    }
}
