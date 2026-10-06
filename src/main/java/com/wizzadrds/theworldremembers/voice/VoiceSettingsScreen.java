package com.wizzadrds.theworldremembers.voice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class VoiceSettingsScreen extends Screen {
    private final Screen parent;
    private final VoiceClientConfig config;
    private EditBox apiKey;
    private EditBox model;
    private EditBox sttCommand;
    private EditBox ttsCommand;
    private EditBox inputVolume;
    private EditBox outputVolume;
    private EditBox distance;
    private Button microphoneButton;
    private Button providerButton;
    private Button outputDeviceButton;
    private List<String> microphones = List.of(MicrophoneCapture.DEFAULT_DEVICE);
    private List<String> providers = List.of("openai");
    private List<String> outputDevices = List.of("Default");
    private int microphoneIndex;
    private int providerIndex;
    private int outputDeviceIndex;

    public VoiceSettingsScreen(Screen parent, VoiceClientConfig config) {
        super(Component.literal("The World Remembers — Voice & AI"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int top = 42;

        microphones = MicrophoneCapture.devices();
        if (microphones.isEmpty()) microphones = List.of(MicrophoneCapture.DEFAULT_DEVICE);
        microphoneIndex = indexOfIgnoreCase(microphones, config.microphone);
        if (microphoneIndex < 0) microphoneIndex = 0;

        outputDevices = VoiceAudioPlayer.devices();
        if (outputDevices.isEmpty()) outputDevices = List.of("Default");
        outputDeviceIndex = indexOfIgnoreCase(outputDevices, config.outputDevice);
        if (outputDeviceIndex < 0) outputDeviceIndex = 0;

        providers = configuredProviders();
        providerIndex = indexOfIgnoreCase(providers, config.provider);
        if (providerIndex < 0) providerIndex = 0;

        microphoneButton = Button.builder(microphoneLabel(), button -> cycleMicrophone())
                .bounds(left, top, 150, 20).build();
        this.addRenderableWidget(microphoneButton);

        providerButton = Button.builder(providerLabel(), button -> cycleProvider())
                .bounds(right, top, 150, 20).build();
        this.addRenderableWidget(providerButton);

        outputDeviceButton = Button.builder(Component.literal(outputDeviceLabel()), button -> cycleOutputDevice())
                .bounds(left, top + 24, 310, 20).build();
        this.addRenderableWidget(outputDeviceButton);

        apiKey = field(left, top + 54, "API Key", config.apiKey);
        apiKey.setMaxLength(512);
        apiKey.setSuggestion("API key (stored locally)");
        model = field(right, top + 42, "AI Model", config.model);
        sttCommand = field(left, top + 96, "STT command", config.sttCommand);
        sttCommand.setSuggestion("Optional local faster-whisper adapter command");
        ttsCommand = field(right, top + 84, "TTS command", config.ttsCommand);
        ttsCommand.setSuggestion("Optional local Piper adapter command");
        inputVolume = field(left, top + 138, "Input volume", Float.toString(config.inputVolume));
        outputVolume = field(right, top + 126, "Output volume", Float.toString(config.outputVolume));
        distance = field(left, top + 180, "Voice distance", Float.toString(config.voiceDistance));

        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
                .bounds(right, top + 168, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Defaults"), button -> resetDefaults())
                .bounds(left, top + 210, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> close())
                .bounds(right, top + 198, 150, 20).build());
    }

    private List<String> configuredProviders() {
        List<String> result = new ArrayList<>();
        if (config.apiKey != null && !config.apiKey.isBlank()) result.add("openai");
        if (result.isEmpty()) result.add("openai");
        return List.copyOf(result);
    }

    private void cycleMicrophone() {
        microphoneIndex = (microphoneIndex + 1) % microphones.size();
        microphoneButton.setMessage(microphoneLabel());
    }

    private void cycleOutputDevice() {
        outputDeviceIndex = (outputDeviceIndex + 1) % outputDevices.size();
        outputDeviceButton.setMessage(Component.literal(outputDeviceLabel()));
    }

    private void cycleProvider() {
        providerIndex = (providerIndex + 1) % providers.size();
        providerButton.setMessage(providerLabel());
    }

    private String microphoneLabel() {
        return "Mic: " + microphones.get(microphoneIndex);
    }

    private String outputDeviceLabel() { return "Headphones / output: " + outputDevices.get(outputDeviceIndex); }

    private String providerLabel() {
        return "AI: " + providers.get(providerIndex);
    }

    private EditBox field(int x, int y, String label, String value) {
        EditBox box = new EditBox(this.font, x, y, 150, 20, Component.literal(label));
        box.setValue(value == null ? "" : value);
        this.addRenderableWidget(box);
        return box;
    }

    private void resetDefaults() {
        microphoneIndex = Math.max(0, indexOfIgnoreCase(microphones, MicrophoneCapture.detectDefaultDevice()));
        providerIndex = Math.max(0, indexOfIgnoreCase(providers, "openai"));
        outputDeviceIndex = Math.max(0, indexOfIgnoreCase(outputDevices, "Default"));
        outputDeviceButton.setMessage(Component.literal(outputDeviceLabel()));
        microphoneButton.setMessage(Component.literal(microphoneLabel()));
        providerButton.setMessage(Component.literal(providerLabel()));
        model.setValue("");
        sttCommand.setValue("");
        ttsCommand.setValue("");
        inputVolume.setValue("1.0");
        outputVolume.setValue("1.0");
        distance.setValue("32.0");
    }

    private void saveAndClose() {
        config.microphone = microphones.get(microphoneIndex);
        config.provider = providers.get(providerIndex);
        config.outputDevice = outputDevices.get(outputDeviceIndex);
        config.apiKey = apiKey.getValue();
        config.model = model.getValue().trim();
        config.sttCommand = sttCommand.getValue().trim();
        config.ttsCommand = ttsCommand.getValue().trim();
        config.inputVolume = boundedFloat(inputVolume.getValue(), config.inputVolume, 0.0f, 2.0f);
        config.outputVolume = boundedFloat(outputVolume.getValue(), config.outputVolume, 0.0f, 2.0f);
        config.voiceDistance = boundedFloat(distance.getValue(), config.voiceDistance, 1.0f, 128.0f);
        config.save(Minecraft.getInstance().gameDirectory.toPath());
        close();
    }

    private static int indexOfIgnoreCase(List<String> values, String value) {
        if (value == null) return -1;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    private static float boundedFloat(String value, float fallback, float min, float max) {
        try {
            float parsed = Float.parseFloat(value.trim());
            if (!Float.isFinite(parsed)) return fallback;
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void close() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, "THE WORLD REMEMBERS — VOICE & AI", this.width / 2, 14, 0xFFFFFFFF);
        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        graphics.text(this.font, "Microphone", left, 32, 0xFFE0E0E0, false);
        graphics.text(this.font, "AI Provider", right, 32, 0xFFE0E0E0, false);
        graphics.text(this.font, "Output device / headphones", left, 56, 0xFFE0E0E0, false);
        graphics.text(this.font, "API Key", left, 86, 0xFFE0E0E0, false);
        graphics.text(this.font, "AI Model", right, 86, 0xFFE0E0E0, false);
        graphics.text(this.font, "STT command", left, 128, 0xFFE0E0E0, false);
        graphics.text(this.font, "TTS command", right, 128, 0xFFE0E0E0, false);
        graphics.text(this.font, "Input volume", left, 170, 0xFFE0E0E0, false);
        graphics.text(this.font, "Output volume", right, 170, 0xFFE0E0E0, false);
        graphics.text(this.font, "Voice distance", left, 212, 0xFFE0E0E0, false);
        graphics.text(this.font, "V = push-to-talk · microphone uses the system audio devices", left, 230, 0xFFAAAAAA, false);
    }
}
