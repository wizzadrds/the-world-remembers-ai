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
        if (this.width <= 0 || this.height <= 0) return;
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int margin = 12;
        int maxWidth = Math.max(120, this.width - margin * 2);
        graphics.centeredText(this.font, "LOCAL VOICE SETUP", this.width / 2, 10, 0xFFFFFFFF);

        String[] lines = {
                "1. Install a local STT engine such as faster-whisper.",
                "2. Install a local TTS engine such as Piper.",
                "3. Create wrapper commands using the arguments below.",
                "4. Put them in Voice Settings > AI & Speech.",
                "5. Select your microphone and headset in Audio.",
                "6. Test with V (push-to-talk). Villagers use the same TTS.",
                "",
                "STT: <program> <pcm-file>",
                "TTS: <program> <text> <wav-output> <language> <model> <rate> <pitch> <expressiveness>",
                "",
                "Local TTS does not require an API key.",
                "The API key is only for the optional online AI reply provider."
        };

        int y = 30;
        for (String line : lines) {
            if (line.isEmpty()) {
                y += 7;
                continue;
            }
            for (String wrapped : wrap(line, maxWidth)) {
                graphics.text(this.font, wrapped, margin, y, 0xFFE0E0E0, false);
                y += 11;
            }
            y += 2;
        }
    }

    private List<String> wrap(String text, int maxPixels) {
        List<String> result = new java.util.ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (this.font.width(remaining) <= maxPixels) {
                result.add(remaining);
                break;
            }
            int cut = remaining.length();
            while (cut > 1 && this.font.width(remaining.substring(0, cut)) > maxPixels) cut--;
            int space = remaining.lastIndexOf(' ', cut - 1);
            if (space > 0) cut = space;
            result.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut).trim();
        }
        return result;
    }
}
