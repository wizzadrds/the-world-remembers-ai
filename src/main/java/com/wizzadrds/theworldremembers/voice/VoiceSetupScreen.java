package com.wizzadrds.theworldremembers.voice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VoiceSetupScreen extends Screen {
    private final Screen parent;

    public VoiceSetupScreen(Screen parent) {
        super(Component.literal("Local Voice Setup"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Back"), b -> Minecraft.getInstance().gui.setScreen(parent))
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = this.width / 2 - 180;
        int y = 35;
        graphics.centeredText(this.font, "LOCAL VOICE SETUP", this.width / 2, 14, 0xFFFFFFFF);
        String[] lines = {
                "1. Install a local STT engine such as faster-whisper.",
                "2. Install a local TTS engine such as Piper.",
                "3. Create wrapper commands that accept the arguments described below.",
                "4. Put those commands in Voice Settings > AI / TTS.",
                "5. Select your microphone and headset in Audio.",
                "6. Test with V (push-to-talk). Villager voices use the same local TTS.",
                "",
                "STT command: <program> <pcm-file>",
                "TTS command: <program> <text> <wav-output> <language> <model> <rate> <pitch> <expressiveness>",
                "",
                "No audio or API key is required for the local TTS path.",
                "The API key is only used for the optional AI reply provider."
        };
        for (String line : lines) {
            graphics.text(this.font, line, x, y, 0xFFE0E0E0, false);
            y += 13;
        }
    }
}
