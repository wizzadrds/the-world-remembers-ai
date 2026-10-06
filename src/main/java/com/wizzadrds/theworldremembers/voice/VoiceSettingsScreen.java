package com.wizzadrds.theworldremembers.voice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VoiceSettingsScreen extends Screen {
    private final Screen parent;
    private final VoiceClientConfig config;
    private EditBox microphone;
    private EditBox apiKey;
    private EditBox provider;
    private EditBox model;

    public VoiceSettingsScreen(Screen parent, VoiceClientConfig config) {
        super(Component.literal("The World Remembers — Voice & AI"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 120;
        int top = 48;

        microphone = new EditBox(this.font, left, top, 240, 20, Component.literal("Microphone"));
        microphone.setValue(config.microphone);
        this.addRenderableWidget(microphone);

        provider = new EditBox(this.font, left, top + 42, 240, 20, Component.literal("AI Provider"));
        provider.setValue(config.provider);
        this.addRenderableWidget(provider);

        apiKey = new EditBox(this.font, left, top + 84, 240, 20, Component.literal("API Key"));
        apiKey.setValue(config.apiKey);
        apiKey.setSuggestion("optional for local providers");
        this.addRenderableWidget(apiKey);

        model = new EditBox(this.font, left, top + 126, 240, 20, Component.literal("AI Model"));
        model.setValue(config.model);
        this.addRenderableWidget(model);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
                .bounds(left, top + 180, 115, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> close())
                .bounds(left + 125, top + 180, 115, 20).build());
    }

    private void saveAndClose() {
        config.microphone = microphone.getValue().trim();
        config.provider = provider.getValue().trim();
        config.apiKey = apiKey.getValue();
        config.model = model.getValue().trim();
        config.save(Minecraft.getInstance().gameDirectory.toPath());
        close();
    }

    private void close() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, "THE WORLD REMEMBERS — VOICE & AI", this.width / 2, 18, 0xFFFFFFFF);
        int left = this.width / 2 - 120;
        graphics.text(this.font, "Microphone", left, 38, 0xFFE0E0E0, false);
        graphics.text(this.font, "AI Provider", left, 80, 0xFFE0E0E0, false);
        graphics.text(this.font, "API Key", left, 122, 0xFFE0E0E0, false);
        graphics.text(this.font, "Model", left, 164, 0xFFE0E0E0, false);
        graphics.text(this.font, "Push-to-talk: V (change it in Options > Controls)", left, 210, 0xFFAAAAAA, false);
    }
}
