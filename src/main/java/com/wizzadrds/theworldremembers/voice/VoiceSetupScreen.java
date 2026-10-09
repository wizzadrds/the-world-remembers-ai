package com.wizzadrds.theworldremembers.voice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class VoiceSetupScreen extends Screen {
    private final Screen parent;

    public VoiceSetupScreen(Screen parent) {
        super(Component.literal("Voice Setup"));
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
        graphics.centeredText(this.font, "VOICE SETUP — NO INSTALLATION REQUIRED", this.width / 2, 10, 0xFFFFFFFF);

        String[] lines = {
                "QUICK START — CLOUD VOICE",
                "1. Open Voice & AI settings and choose Gemini or OpenAI.",
                "2. Paste an API key from the provider you selected.",
                "3. Press Recommended defaults, then save your settings.",
                "4. Choose your microphone and headset on the Audio page.",
                "5. Go near a villager and hold V to speak.",
                "",
                "No Python, terminal, local models or extra downloads are needed.",
                "Cloud speech uses your provider's API and may incur usage charges.",
                "",
                "ADVANCED: custom local STT/TTS commands are optional.",
                "VillagerTITAN/RVC is an optional local voice override and requires",
                "its own compatible Python environment and model files."
        };

        int y = 30;
        int bottomLimit = Math.max(y, this.height - 44);
        boolean clipped = false;
        for (String line : lines) {
            if (line.isEmpty()) {
                y += 6;
                continue;
            }
            for (String wrapped : wrap(line, maxWidth)) {
                if (y + 11 > bottomLimit) {
                    clipped = true;
                    break;
                }
                graphics.text(this.font, wrapped, margin, y, 0xFFE0E0E0, false);
                y += 11;
            }
            if (clipped) break;
            y += 1;
        }
        if (clipped && y + 11 <= bottomLimit) {
            graphics.text(this.font, "...", margin, y, 0xFFAAAAAA, false);
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
