package com.wizzadrds.theworldremembers.voice;

import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class VoiceHud {
    private VoiceHud() {}

    public static void register(KeyMapping voiceKey) {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("the_world_remembers", "voice_status"),
                (graphics, delta) -> render(graphics, voiceKey));
    }

    private static void render(GuiGraphicsExtractor graphics, KeyMapping voiceKey) {
        if (!voiceKey.isDown()) return;
        int x = 12, y = 12;
        graphics.fill(x, y, x + 142, y + 34, 0xCC10151B);
        graphics.fill(x, y, x + 4, y + 34, 0xFF62D6C8);
        graphics.fill(x + 14, y + 7, x + 22, y + 25, 0xFFFFFFFF);
        graphics.fill(x + 11, y + 13, x + 25, y + 22, 0xFFFFFFFF);
        graphics.fill(x + 15, y + 25, x + 21, y + 29, 0xFFFFFFFF);
        graphics.text(Component.literal("LISTENING"), x + 34, y + 8, 0xFFFFFFFF, true);
        graphics.text(Component.literal("Push to talk"), x + 34, y + 20, 0xFFB7C2C9, false);
    }
}
