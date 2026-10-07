package com.wizzadrds.theworldremembers.voice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import com.wizzadrds.theworldremembers.TheWorldRemembersClient;

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
    private boolean deviceScanFailed;
    private boolean deviceScanPending;
    private boolean testToneRunning;

    private Button microphoneButton;
    private Button outputButton;
    private Button temperamentButton;
    private Button villagerVoicesButton;
    private Button pushToTalkButton;
    private Button providerButton;
    private Button microphoneTestButton;
    private Button pttKeyButton;
    private Button villagerVoiceTestButton;
    private Button microphoneStatusButton;
    private Button outputStatusButton;

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
        // The GUI renderer can enter a transient zero-height state during resize/reload.
        if (this.width <= 0 || this.height <= 0) return;
        refreshDevices();
        rebuildPage();
    }

    private void refreshDevices() {
        // Never enumerate Java Sound devices on the Minecraft render thread.
        try {
            microphones = preserveSelectedDevice(safeDevices(AudioDeviceManager.inputDevices()), config.microphone);
            outputs = preserveSelectedDevice(safeDevices(AudioDeviceManager.outputDevices()), config.outputDevice);
            deviceScanFailed = false;
        } catch (Throwable ignored) {
            deviceScanFailed = true;
            microphones = preserveSelectedDevice(List.of(AudioDeviceManager.DEFAULT_DEVICE), config.microphone);
            outputs = preserveSelectedDevice(List.of(AudioDeviceManager.DEFAULT_DEVICE), config.outputDevice);
        }
        temperaments = new ArrayList<>();
        for (VoiceTemperament temperament : VoiceTemperament.values()) temperaments.add(temperament.name());
        microphoneIndex = validIndex(microphones, config.microphone);
        outputIndex = validIndex(outputs, config.outputDevice);
        temperamentIndex = validIndex(temperaments, config.villagerVoiceTemperament);

        if (!deviceScanPending) {
            deviceScanPending = true;
            AudioDeviceManager.refreshDevicesAsync(() -> Minecraft.getInstance().execute(() -> {
                deviceScanPending = false;
                if (Minecraft.getInstance().gui.screen() == this) {
                    refreshDevicesFromCache();
                    refreshDeviceButtons();
                }
            }));
        }
    }

    private void refreshDevicesFromCache() {
        try {
            microphones = preserveSelectedDevice(safeDevices(AudioDeviceManager.inputDevices()), config.microphone);
            outputs = preserveSelectedDevice(safeDevices(AudioDeviceManager.outputDevices()), config.outputDevice);
            deviceScanFailed = false;
        } catch (Throwable ignored) {
            deviceScanFailed = true;
        }
        microphoneIndex = validIndex(microphones, config.microphone);
        outputIndex = validIndex(outputs, config.outputDevice);
    }

    private static List<String> safeDevices(List<String> values) {
        return values == null || values.isEmpty() ? List.of(AudioDeviceManager.DEFAULT_DEVICE) : values;
    }

    private static List<String> preserveSelectedDevice(List<String> detected, String selected) {
        List<String> result = new ArrayList<>(detected);
        if (selected != null && !selected.isBlank()
                && indexOfIgnoreCase(result, selected) < 0
                && !selected.equalsIgnoreCase(AudioDeviceManager.DEFAULT_DEVICE)) {
            result.add(selected + " (unavailable)");
        }
        return List.copyOf(result);
    }

    private static int indexOfIgnoreCase(List<String> values, String selected) {
        if (selected == null) return -1;
        for (int i = 0; i < values.size(); i++) if (selected.equalsIgnoreCase(values.get(i))) return i;
        return -1;
    }

    private static int validIndex(List<String> values, String selected) {
        int index = indexOfIgnoreCase(values, selected);
        if (index >= 0) return index;
        if (selected != null && !selected.isBlank()) {
            index = indexOfIgnoreCase(values, selected + " (unavailable)");
            if (index >= 0) return index;
        }
        return 0;
    }

    private static String actualDeviceName(String value) {
        if (value == null) return AudioDeviceManager.DEFAULT_DEVICE;
        String suffix = " (unavailable)";
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

    private void rebuildPage() {
        savePageToConfig();
        this.clearWidgets();
        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int top = 66;

        addRenderableWidget(Button.builder(Component.literal("1  Audio"), b -> switchPage(0)).bounds(left, 42, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("2  AI & Speech"), b -> switchPage(1)).bounds(left + 105, 42, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("3  Villagers"), b -> switchPage(2)).bounds(left + 210, 42, 100, 20).build());

        if (page == 0) buildAudioPage(left, right, top);
        else if (page == 1) buildAiPage(left, right, top);
        else buildNpcPage(left, right, top);

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose()).bounds(right, this.height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Defaults"), b -> resetDefaults()).bounds(left, this.height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> close()).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    private void buildAudioPage(int left, int right, int top) {
        microphoneButton = addButton("Microphone: " + microphones.get(microphoneIndex), left, top, this::cycleMicrophone);
        outputButton = addButton("Speaker: " + outputs.get(outputIndex), right, top, this::cycleOutput);

        inputVolume = field(left, top + 42, "Input volume (0–2)", Float.toString(config.inputVolume));
        outputVolume = field(right, top + 42, "Output volume (0–2)", Float.toString(config.outputVolume));

        distance = field(left, top + 84, "Voice range (blocks)", Float.toString(config.voiceDistance));
        pushToTalkButton = addButton("Push-to-talk: " + (config.pushToTalkMode ? "ON" : "OFF"), right, top + 84, () -> {
            config.pushToTalkMode = !config.pushToTalkMode;
            pushToTalkButton.setMessage(Component.literal("Push-to-talk: " + (config.pushToTalkMode ? "ON" : "OFF")));
        });

        villagerVoicesButton = addButton("Villager voices: " + (config.villagerVoicesEnabled ? "ON" : "OFF"), left, top + 126, () -> {
            config.villagerVoicesEnabled = !config.villagerVoicesEnabled;
            villagerVoicesButton.setMessage(Component.literal("Villager voices: " + (config.villagerVoicesEnabled ? "ON" : "OFF")));
        });

        pttKeyButton = addButton("PTT key: " + keyName(config.pushToTalkKey), right, top + 126, this::cyclePushToTalkKey);

        microphoneTestButton = addButton("Test microphone", left, top + 168, this::toggleMicrophoneTest);
        addRenderableWidget(Button.builder(Component.literal("Test speaker"), b -> {
            if (testToneRunning) return;
            testToneRunning = true;
            b.setMessage(Component.literal("Testing speaker..."));
            String selected = actualDeviceName(outputs.get(outputIndex));
            float volume = config.outputVolume;
            java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> AudioDeviceManager.playTestTone(selected, volume))
                    .whenComplete((ok, error) -> Minecraft.getInstance().execute(() -> {
                        testToneRunning = false;
                        if (Minecraft.getInstance().gui.screen() == this) {
                            b.setMessage(Component.literal(error == null && Boolean.TRUE.equals(ok)
                                    ? "Speaker test OK" : "Speaker test failed"));
                        }
                    }));
        }).bounds(right, top + 168, 150, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Rescan audio devices"),
                b -> {
                    savePageToConfig();
                    refreshDevices();
                    refreshDeviceButtons();
                    b.setMessage(Component.literal("Devices refreshed"));
                }).bounds(left, top + 210, 150, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("What do these settings do?"),
                b -> Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this)))
                .bounds(right, top + 210, 150, 20).build());
    }

    private void buildAiPage(int left, int right, int top) {
        providerButton = addButton("AI provider: " + displayProvider(config.provider), left, top, () -> {
            config.provider = nextProvider(config.provider);
            providerButton.setMessage(Component.literal("AI provider: " + displayProvider(config.provider)));
        });

        language = field(left, top + 42, "Speech language", config.language);
        model = field(right, top + 42, "AI model", config.model);

        sttModel = field(left, top + 84, "STT model", config.sttModel);
        ttsModel = field(right, top + 84, "TTS model", config.ttsModel);

        ttsVoice = field(left, top + 126, "TTS voice", config.ttsVoice);
        apiKey = field(right, top + 126, "API key (stored locally)", config.apiKey);
        apiKey.setMaxLength(512);
        apiKey.setSuggestion("Your API key is saved only in the local config");

        sttCommand = field(left, top + 168, "Local STT command", config.sttCommand);
        sttCommand.setSuggestion("Optional: program that receives the audio file path");

        ttsCommand = field(right, top + 168, "Local TTS command", config.ttsCommand);
        ttsCommand.setSuggestion("Optional: program that receives text and produces audio");

        addRenderableWidget(Button.builder(Component.literal("Use recommended AI defaults"), b -> {
            language.setValue("en-US");
            model.setValue("gpt-4o-mini");
            sttModel.setValue("gpt-4o-mini-transcribe");
            ttsModel.setValue("gpt-4o-mini-tts");
            ttsVoice.setValue("alloy");
            sttCommand.setValue("");
            ttsCommand.setValue("");
            b.setMessage(Component.literal("Recommended defaults applied"));
        }).bounds(left, top + 210, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Open setup guide"), b ->
                Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this)))
                .bounds(right, top + 210, 150, 20).build());
    }

    private void buildNpcPage(int left, int right, int top) {
        temperamentButton = addButton("Temperament: " + prettyTemperament(temperaments.get(temperamentIndex)), left, top, this::cycleTemperament);
        villagerVoiceTestButton = addButton("Test villager voice", right, top, () -> {
            TheWorldRemembersClient.testVillagerVoice();
            villagerVoiceTestButton.setMessage(Component.literal("Villager TTS test started"));
        });

        ttsInstructions = field(left, top + 42, "Villager speaking style", config.ttsInstructions);
        systemPrompt = field(right, top + 42, "AI system rules", config.systemPrompt);

        addRenderableWidget(Button.builder(Component.literal("Apply villager-safe defaults"), b -> {
            ttsInstructions.setValue("Speak like a Minecraft villager: warm, conversational, slightly rustic, short phrases, natural pauses. Avoid robotic or announcer delivery.");
            systemPrompt.setValue("You are a Minecraft villager. Speak briefly, naturally and in character. Only use facts supplied by the simulation. Never invent world state.");
            b.setMessage(Component.literal("Villager defaults applied"));
        }).bounds(left, top + 84, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Open voice guide"), b ->
                Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this)))
                .bounds(right, top + 84, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("How villager voice works"), b ->
                Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this)))
                .bounds(left, top + 126, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Return to Audio"), b -> switchPage(0))
                .bounds(right, top + 126, 150, 20).build());
    }

    private Button addButton(String text, int x, int y, Runnable action) {
        Button button = Button.builder(Component.literal(text), ignored -> action.run()).bounds(x, y, 150, 20).build();
        addRenderableWidget(button);
        return button;
    }

    private void refreshDeviceButtons() {
        if (microphones.isEmpty()) microphones = List.of(AudioDeviceManager.DEFAULT_DEVICE);
        if (outputs.isEmpty()) outputs = List.of(AudioDeviceManager.DEFAULT_DEVICE);
        microphoneIndex = Math.max(0, Math.min(microphoneIndex, microphones.size() - 1));
        outputIndex = Math.max(0, Math.min(outputIndex, outputs.size() - 1));
        if (microphoneButton != null) {
            microphoneButton.setMessage(Component.literal("Mic: " + microphones.get(microphoneIndex)));
        }
        if (outputButton != null) {
            outputButton.setMessage(Component.literal("Output: " + outputs.get(outputIndex)));
        }
        refreshDeviceStatus();
    }

    private void refreshDeviceStatus() {
        if (microphoneStatusButton != null && !microphones.isEmpty()) {
            microphoneStatusButton.setMessage(Component.literal(
                    "Mic: " + AudioDeviceManager.describeAvailability(actualDeviceName(microphones.get(microphoneIndex)), true)));
        }
        if (outputStatusButton != null && !outputs.isEmpty()) {
            outputStatusButton.setMessage(Component.literal(
                    "Out: " + AudioDeviceManager.describeAvailability(actualDeviceName(outputs.get(outputIndex)), false)));
        }
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
        config.microphone = actualDeviceName(microphones.get(microphoneIndex));
        microphoneButton.setMessage(Component.literal("Mic: " + microphones.get(microphoneIndex)));
        refreshDeviceStatus();
    }

    private void cycleOutput() {
        outputIndex = (outputIndex + 1) % outputs.size();
        config.outputDevice = actualDeviceName(outputs.get(outputIndex));
        outputButton.setMessage(Component.literal("Output: " + outputs.get(outputIndex)));
        refreshDeviceStatus();
    }

    private void cycleTemperament() {
        temperamentIndex = (temperamentIndex + 1) % temperaments.size();
        config.villagerVoiceTemperament = temperaments.get(temperamentIndex);
        temperamentButton.setMessage(Component.literal("Temperament: " + prettyTemperament(temperaments.get(temperamentIndex))));
    }

    private static String prettyTemperament(String value) {
        if (value == null || value.isBlank()) return "Neutral";
        String normalized = value.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private void savePageToConfig() {
        if (microphoneButton != null) config.microphone = actualDeviceName(microphones.get(microphoneIndex));
        if (outputButton != null) config.outputDevice = actualDeviceName(outputs.get(outputIndex));
        if (temperamentButton != null) config.villagerVoiceTemperament = temperaments.get(temperamentIndex);
        if (inputVolume != null) config.inputVolume = bounded(inputVolume.getValue(), config.inputVolume, 0, 2);
        if (outputVolume != null) config.outputVolume = bounded(outputVolume.getValue(), config.outputVolume, 0, 2);
        if (distance != null) config.voiceDistance = bounded(distance.getValue(), config.voiceDistance, 1, 64);
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
        if (this.width <= 0 || this.height <= 0) return;
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(this.font, "THE WORLD REMEMBERS — VOICE & AI", this.width / 2, 10, 0xFFFFFFFF);

        String subtitle = switch (page) {
            case 0 -> "Audio: choose your microphone, speaker and push-to-talk settings";
            case 1 -> "AI & speech: configure the service that turns villager text into voice";
            default -> "Villagers: shape their personality without changing their simulation memory";
        };
        graphics.centeredText(this.font, subtitle, this.width / 2, 27, 0xFFB8B8B8);

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int top = 66;

        if (page == 0) {
            drawLabel(graphics, "Microphone", left, top - 11);
            drawLabel(graphics, "Speaker / output", right, top - 11);
            drawLabel(graphics, "Input loudness", left, top + 31);
            drawLabel(graphics, "Output loudness", right, top + 31);
            drawLabel(graphics, "Maximum voice range", left, top + 73);
            drawLabel(graphics, "Hold-to-talk behavior", right, top + 73);
            drawLabel(graphics, "NPC speech", left, top + 115);
            drawLabel(graphics, "Push-to-talk key", right, top + 115);
            drawLabel(graphics, "Quick tests", left, top + 157);
            drawLabel(graphics, "Quick tests", right, top + 157);
            graphics.text(this.font, "Detected: " + microphones.size() + " microphone(s) · " + outputs.size() + " output(s)",
                    left, top + 200, 0xFF9E9E9E, false);
            graphics.text(this.font, "Use the guide if you are unsure what to choose.", right, top + 200, 0xFF9E9E9E, false);
            graphics.text(this.font, "Mic status: " + AudioDeviceManager.describeAvailability(actualDeviceName(microphones.get(microphoneIndex)), true),
                    left, top + 232, 0xFFAAAAAA, false);
            graphics.text(this.font, "Speaker status: " + AudioDeviceManager.describeAvailability(actualDeviceName(outputs.get(outputIndex)), false),
                    right, top + 232, 0xFFAAAAAA, false);
        } else if (page == 1) {
            drawLabel(graphics, "Provider", left, top - 11);
            drawLabel(graphics, "Language / model", left, top + 31);
            drawLabel(graphics, "Language / model", right, top + 31);
            drawLabel(graphics, "Speech recognition", left, top + 73);
            drawLabel(graphics, "Speech synthesis", right, top + 73);
            drawLabel(graphics, "Voice", left, top + 115);
            drawLabel(graphics, "Authentication", right, top + 115);
            drawLabel(graphics, "Optional local adapter", left, top + 157);
            drawLabel(graphics, "Optional local adapter", right, top + 157);
            graphics.text(this.font, "Leave local commands empty when using the built-in online provider.",
                    left, top + 232, 0xFFAAAAAA, false);
        } else {
            drawLabel(graphics, "Personality", left, top - 11);
            drawLabel(graphics, "Test", right, top - 11);
            drawLabel(graphics, "Style instructions", left, top + 31);
            drawLabel(graphics, "AI behavior rules", right, top + 31);
            graphics.text(this.font, "These instructions affect how a villager speaks; they do not replace the simulation.",
                    left, top + 116, 0xFFAAAAAA, false);
            graphics.text(this.font, "Temperament: " + prettyTemperament(temperaments.get(temperamentIndex)),
                    left, top + 148, 0xFF9E9E9E, false);
        }

        graphics.text(this.font, "Changes are not saved until you press Save.", left, this.height - 48, 0xFFE0C070, false);

        if (page == 2) {
            String error = TheWorldRemembersClient.lastVillagerVoiceError();
            if (error != null && !error.isBlank()) {
                graphics.text(this.font, "TTS error: " + error, left, this.height - 64, 0xFFFF7777, false);
            }
        }
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String text, int x, int y) {
        graphics.text(this.font, text, x, y, 0xFFE6E6E6, false);
    }}
