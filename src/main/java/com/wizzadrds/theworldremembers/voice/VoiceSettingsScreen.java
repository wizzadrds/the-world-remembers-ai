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
    private int page;

    private List<String> microphones = List.of(AudioDeviceManager.DEFAULT_DEVICE);
    private List<String> outputs = List.of(AudioDeviceManager.DEFAULT_DEVICE);
    private List<String> temperaments = List.of();
    private int microphoneIndex;
    private int outputIndex;
    private int temperamentIndex;

    private Button microphoneButton;
    private Button outputButton;
    private Button temperamentButton;
    private Button villagerVoicesButton;
    private Button pushToTalkButton;
    private Button providerButton;
    private Button microphoneTestButton;
    private Button pttKeyButton;

    private EditBox inputVolume;
    private EditBox outputVolume;
    private EditBox distance;
    private EditBox apiKey;
    private EditBox language;
    private EditBox model;
    private EditBox sttModel;
    private EditBox ttsModel;
    private EditBox ttsVoice;
    private EditBox sttCommand;
    private EditBox ttsCommand;
    private EditBox ttsInstructions;
    private EditBox systemPrompt;

    public VoiceSettingsScreen(Screen parent, VoiceClientConfig config) {
        super(Component.literal("The World Remembers — Voice & AI"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        refreshDevices();
        rebuildPage();
    }

    private void refreshDevices() {
        microphones = safeDevices(AudioDeviceManager.inputDevices());
        outputs = safeDevices(AudioDeviceManager.outputDevices());
        temperaments = new ArrayList<>();
        for (VoiceTemperament temperament : VoiceTemperament.values()) temperaments.add(temperament.name());
        microphoneIndex = validIndex(microphones, config.microphone);
        outputIndex = validIndex(outputs, config.outputDevice);
        temperamentIndex = validIndex(temperaments, config.villagerVoiceTemperament);
    }

    private static List<String> safeDevices(List<String> values) {
        return values == null || values.isEmpty() ? List.of(AudioDeviceManager.DEFAULT_DEVICE) : values;
    }

    private static int validIndex(List<String> values, String selected) {
        int index = indexOfIgnoreCase(values, selected);
        return index < 0 ? 0 : index;
    }

    private void rebuildPage() {
        savePageToConfig();
        this.clearWidgets();
        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int top = 52;

        addRenderableWidget(Button.builder(Component.literal("Audio"), b -> switchPage(0)).bounds(left, 24, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("AI / TTS"), b -> switchPage(1)).bounds(left + 105, 24, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("NPC voices"), b -> switchPage(2)).bounds(left + 210, 24, 100, 20).build());

        if (page == 0) buildAudioPage(left, right, top);
        else if (page == 1) buildAiPage(left, right, top);
        else buildNpcPage(left, right, top);

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose()).bounds(right, this.height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Defaults"), b -> resetDefaults()).bounds(left, this.height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> close()).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    private void buildAudioPage(int left, int right, int top) {
        microphoneButton = addButton("Mic: " + microphones.get(microphoneIndex), left, top, this::cycleMicrophone);
        outputButton = addButton("Output: " + outputs.get(outputIndex), right, top, this::cycleOutput);
        inputVolume = field(left, top + 46, "Input volume", Float.toString(config.inputVolume));
        outputVolume = field(right, top + 46, "Output volume", Float.toString(config.outputVolume));
        distance = field(left, top + 92, "Voice distance", Float.toString(config.voiceDistance));
        pushToTalkButton = addButton("Push-to-talk: " + (config.pushToTalkMode ? "ON" : "OFF"), right, top + 92, () -> {
            config.pushToTalkMode = !config.pushToTalkMode;
            pushToTalkButton.setMessage(Component.literal("Push-to-talk: " + (config.pushToTalkMode ? "ON" : "OFF")));
        });
        pttKeyButton = addButton("PTT key: " + keyName(config.pushToTalkKey), left, top + 184, this::cyclePushToTalkKey);
        microphoneTestButton = addButton("Test microphone", left, top + 230, this::toggleMicrophoneTest);
        villagerVoicesButton = addButton("Villager voices: " + (config.villagerVoicesEnabled ? "ON" : "OFF"), left, top + 138, () -> {
            config.villagerVoicesEnabled = !config.villagerVoicesEnabled;
            villagerVoicesButton.setMessage(Component.literal("Villager voices: " + (config.villagerVoicesEnabled ? "ON" : "OFF")));
        });
        addRenderableWidget(Button.builder(Component.literal("Test output"), b -> {
            boolean ok = AudioDeviceManager.playTestTone(outputs.get(outputIndex), config.outputVolume);
            b.setMessage(Component.literal(ok ? "Test played" : "Test failed"));
        }).bounds(right, top + 138, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Rescan devices (" + microphones.size() + " mic / " + outputs.size() + " out)"), b -> {
            savePageToConfig();
            refreshDevices();
            rebuildPage();
        }).bounds(right, top + 164, 150, 20).build());
    }

    private void buildAiPage(int left, int right, int top) {
        providerButton = addButton("AI provider: " + displayProvider(config.provider), left, top, () -> {
            config.provider = nextProvider(config.provider);
            providerButton.setMessage(Component.literal("AI provider: " + displayProvider(config.provider)));
        });
        apiKey = field(left, top + 46, "API key", config.apiKey);
        apiKey.setMaxLength(512);
        apiKey.setSuggestion("stored locally in config");
        model = field(right, top + 46, "AI model", config.model);
        language = field(left, top + 230, "Speech language", config.language);
        sttModel = field(left, top + 92, "STT model", config.sttModel);
        ttsModel = field(right, top + 92, "TTS model", config.ttsModel);
        ttsVoice = field(left, top + 138, "TTS voice", config.ttsVoice);
        sttCommand = field(right, top + 138, "STT command", config.sttCommand);
        sttCommand.setSuggestion("local command; final argument is the PCM file");
        ttsCommand = field(left, top + 184, "TTS command", config.ttsCommand);
        ttsCommand.setSuggestion("text output language/model/rate are appended");
    }

    private void buildNpcPage(int left, int right, int top) {
        temperamentButton = addButton("Temperament: " + temperaments.get(temperamentIndex), left, top, this::cycleTemperament);
        addRenderableWidget(Button.builder(Component.literal("Reload local devices"), b -> {
            refreshDevices();
            b.setMessage(Component.literal("Devices rescanned"));
        }).bounds(right, top, 150, 20).build());
        ttsInstructions = field(left, top + 46, "Villager voice instructions", config.ttsInstructions);
        systemPrompt = field(right, top + 46, "AI system prompt", config.systemPrompt);
        addRenderableWidget(Button.builder(Component.literal("Use villager-safe defaults"), b -> {
            ttsInstructions.setValue("Speak like a Minecraft villager: warm, conversational, slightly rustic, short phrases, natural pauses. Avoid announcer, robotic or radio-presenter delivery.");
            systemPrompt.setValue("You are a Minecraft villager. Speak briefly, naturally and in character. Only use facts supplied by the simulation. Never invent world state.");
        }).bounds(left, top + 92, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("About local voice"), b ->
                Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this))).bounds(right, top + 92, 150, 20).build());
    }

    private Button addButton(String text, int x, int y, Runnable action) {
        Button button = Button.builder(Component.literal(text), ignored -> action.run()).bounds(x, y, 150, 20).build();
        addRenderableWidget(button);
        return button;
    }

    private EditBox field(int x, int y, String label, String value) {
        EditBox box = new EditBox(this.font, x, y, 150, 20, Component.literal(label));
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void switchPage(int next) {
        savePageToConfig();
        page = next;
        rebuildPage();
    }

    private void cyclePushToTalkKey() {
        int[] keys = {86, 66, 71, 67, 88};
        int current = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i] == config.pushToTalkKey) { current = i; break; }
        config.pushToTalkKey = keys[(current + 1) % keys.length];
        pttKeyButton.setMessage(Component.literal("PTT key: " + keyName(config.pushToTalkKey)));
    }

    private void toggleMicrophoneTest() {
        if (TheWorldRemembersClient.microphoneCapturing()) {
            TheWorldRemembersClient.stopMicrophoneTest();
            microphoneTestButton.setMessage(Component.literal("Test microphone"));
        } else {
            boolean started = TheWorldRemembersClient.startMicrophoneTest();
            microphoneTestButton.setMessage(Component.literal(started ? "Mic level: " + percentLevel() : "Mic failed"));
        }
    }

    private String percentLevel() {
        return Math.round(TheWorldRemembersClient.microphoneLevel() * 100.0f) + "%";
    }

    private static String keyName(int key) {
        return switch (key) { case 66 -> "B"; case 67 -> "C"; case 71 -> "G"; case 88 -> "X"; default -> "V"; };
    }

    private void cycleMicrophone() {
        microphoneIndex = (microphoneIndex + 1) % microphones.size();
        microphoneButton.setMessage(Component.literal("Mic: " + microphones.get(microphoneIndex)));
    }

    private void cycleOutput() {
        outputIndex = (outputIndex + 1) % outputs.size();
        outputButton.setMessage(Component.literal("Output: " + outputs.get(outputIndex)));
    }

    private void cycleTemperament() {
        temperamentIndex = (temperamentIndex + 1) % temperaments.size();
        temperamentButton.setMessage(Component.literal("Temperament: " + temperaments.get(temperamentIndex)));
    }

    private void savePageToConfig() {
        if (microphoneButton != null) config.microphone = microphones.get(microphoneIndex);
        if (outputButton != null) config.outputDevice = outputs.get(outputIndex);
        if (temperamentButton != null) config.villagerVoiceTemperament = temperaments.get(temperamentIndex);
        if (inputVolume != null) config.inputVolume = bounded(inputVolume.getValue(), config.inputVolume, 0, 2);
        if (outputVolume != null) config.outputVolume = bounded(outputVolume.getValue(), config.outputVolume, 0, 2);
        if (distance != null) config.voiceDistance = bounded(distance.getValue(), config.voiceDistance, 1, 128);
        if (apiKey != null) config.apiKey = apiKey.getValue().trim();
        if (language != null) config.language = language.getValue().trim();
        if (model != null) config.model = model.getValue().trim();
        if (sttModel != null) config.sttModel = sttModel.getValue().trim();
        if (ttsModel != null) config.ttsModel = ttsModel.getValue().trim();
        if (ttsVoice != null) config.ttsVoice = ttsVoice.getValue().trim();
        if (sttCommand != null) config.sttCommand = sttCommand.getValue().trim();
        if (ttsCommand != null) config.ttsCommand = ttsCommand.getValue().trim();
        if (ttsInstructions != null) config.ttsInstructions = ttsInstructions.getValue().trim();
        if (systemPrompt != null) config.systemPrompt = systemPrompt.getValue().trim();
    }

    private void saveAndClose() {
        savePageToConfig();
        config.save(Minecraft.getInstance().gameDirectory.toPath());
        close();
    }

    private void resetDefaults() {
        VoiceClientConfig defaults = new VoiceClientConfig();
        config.microphone = defaults.microphone;
        config.outputDevice = defaults.outputDevice;
        config.villagerVoicesEnabled = defaults.villagerVoicesEnabled;
        config.villagerVoiceTemperament = defaults.villagerVoiceTemperament;
        config.pushToTalkMode = defaults.pushToTalkMode;
        config.pushToTalkKey = defaults.pushToTalkKey;
        config.inputVolume = defaults.inputVolume;
        config.outputVolume = defaults.outputVolume;
        config.voiceDistance = defaults.voiceDistance;
        config.provider = defaults.provider;
        config.apiKey = defaults.apiKey;
        config.language = defaults.language;
        config.model = defaults.model;
        config.sttModel = defaults.sttModel;
        config.sttCommand = defaults.sttCommand;
        config.ttsCommand = defaults.ttsCommand;
        config.ttsModel = defaults.ttsModel;
        config.ttsVoice = defaults.ttsVoice;
        config.ttsInstructions = defaults.ttsInstructions;
        config.systemPrompt = defaults.systemPrompt;
        refreshDevices();
        rebuildPage();
    }

    private static float bounded(String value, float fallback, float min, float max) {
        try {
            float parsed = Float.parseFloat(value.trim());
            return Float.isFinite(parsed) ? Math.max(min, Math.min(max, parsed)) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String displayProvider(String provider) {
        if (provider == null || provider.isBlank()) return "OpenAI";
        return provider.equalsIgnoreCase("openai-responses") || provider.equalsIgnoreCase("openai") ? "OpenAI" : provider;
    }

    private static String nextProvider(String provider) {
        // Only providers implemented by the client are offered here; this avoids presenting
        // a selectable backend that would later fail at runtime.
        return "openai".equalsIgnoreCase(provider) || "openai-responses".equalsIgnoreCase(provider) ? "openai" : "openai";
    }

    @Override
    public void removed() {
        TheWorldRemembersClient.stopMicrophoneTest();
        super.removed();
    }

    private void close() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        if (microphoneTestButton != null && TheWorldRemembersClient.microphoneCapturing()) {
            microphoneTestButton.setMessage(Component.literal("Mic level: " + percentLevel()));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, "THE WORLD REMEMBERS — VOICE & AI", this.width / 2, 10, 0xFFFFFFFF);
        String subtitle = switch (page) {
            case 0 -> "Audio devices, volume and push-to-talk";
            case 1 -> "AI, speech-to-text and text-to-speech";
            default -> "Villager personality and grounded speech";
        };
        graphics.centeredText(this.font, subtitle, this.width / 2, 38, 0xFFAAAAAA);
    }
}
