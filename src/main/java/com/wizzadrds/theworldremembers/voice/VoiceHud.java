package com.wizzadrds.theworldremembers.voice;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class VoiceHud {
    private VoiceHud() {}

    public static void register(KeyMapping voiceKey, VoiceConversationController conversation) {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("the_world_remembers", "voice_status"),
                (graphics, delta) -> render(graphics, voiceKey, conversation.state()));
    }

    private static void render(
            GuiGraphicsExtractor graphics,
            KeyMapping voiceKey,
            VoiceConversationState state) {
        if (!voiceKey.isDown() && state == VoiceConversationState.IDLE) return;

        Minecraft client = Minecraft.getInstance();
        int x = 12;
        int y = 12;
        int width = 178;
        graphics.fill(x, y, x + width, y + 38, 0xCC10151B);
        graphics.fill(x, y, x + 4, y + 38, stateAccent(state));

        String title = switch (state) {
            case LISTENING -> "LISTENING";
            case PROCESSING -> "PROCESSING";
            case SPEAKING -> "SPEAKING";
            case ERROR -> "VOICE ERROR";
            case IDLE -> "READY";
        };
        String detail = switch (state) {
            case LISTENING -> "Push to talk";
            case PROCESSING -> "Transcribing / thinking";
            case SPEAKING -> "NPC is speaking";
            case ERROR -> "Check voice settings";
            case IDLE -> "Hold V to talk";
        };

        graphics.text(client.font, Component.literal(title), x + 34, y + 8, 0xFFFFFFFF);
        graphics.text(client.font, Component.literal(detail), x + 34, y + 21, 0xFFB7C2C9);

        if (state == VoiceConversationState.LISTENING) {
            int bars = Math.max(1, Math.min(5, 1 + (int) (System.nanoTime() / 90_000_000L % 5)));
            for (int i = 0; i < bars; i++) {
                graphics.fill(x + width - 12 - i * 5, y + 25 - i * 3, x + width - 9 - i * 5, y + 29, 0xFFFFFFFF);
            }
        }
    }

    private static int stateAccent(VoiceConversationState state) {
        return switch (state) {
            case LISTENING -> 0xFF62D6C8;
            case PROCESSING -> 0xFFFFC857;
            case SPEAKING -> 0xFF8DB4FF;
            case ERROR -> 0xFFFF6B6B;
            case IDLE -> 0xFF69737D;
        };
    }
}
