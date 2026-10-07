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

    private static final int COL_W = 128;
    private static final int GAP = 12;
    private static final int ROW_H = 28;
    private static final int CONTENT_W = COL_W * 2 + GAP;

    private int leftColumn() { return Math.max(6, (this.width - CONTENT_W) / 2); }
    private int rightColumn() { return leftColumn() + COL_W + GAP; }
    private int contentTop() { return 58; }

    private void rebuildPage() {
        savePageToConfig();
        this.clearWidgets();

        int left = leftColumn();
        int right = rightColumn();
        int top = contentTop();

        int tabsX = Math.max(6, (this.width - 3 * 86 - 2 * 4) / 2);
        addRenderableWidget(Button.builder(Component.literal("AUDIO"), b -> switchPage(0)).bounds(tabsX, 32, 86, 20).build());
        addRenderableWidget(Button.builder(Component.literal("AI & SPEECH"), b -> switchPage(1)).bounds(tabsX + 90, 32, 86, 20).build());
        addRenderableWidget(Button.builder(Component.literal("VILLAGERS"), b -> switchPage(2)).bounds(tabsX + 180, 32, 86, 20).build());

        if (page == 0) buildAudioPage(left, right, top);
        else if (page == 1) buildAiPage(left, right, top);
        else buildNpcPage(left, right, top);

        int bottom = Math.max(0, this.height - 27);
        addRenderableWidget(Button.builder(Component.literal("DEFAULTS"), b -> resetDefaults()).bounds(left, bottom, COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("CANCEL"), b -> close()).bounds(right, bottom, COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("SAVE"), b -> saveAndClose())
                .bounds(this.width / 2 - 45, bottom - 25, 90, 20).build());
    }

    private void buildAudioPage(int left, int right, int top) {
        int row = 0;
        microphoneButton = addButton(fit("Mic: " + microphones.get(microphoneIndex), COL_W - 8), left, top + row++ * ROW_H, this::cycleMicrophone);
        outputButton = addButton(fit("Out: " + outputs.get(outputIndex), COL_W - 8), right, top, this::cycleOutput);

        inputVolume = field(left, top + row * ROW_H, "Input volume", Float.toString(config.inputVolume));
        outputVolume = field(right, top + row++ * ROW_H, "Output volume", Float.toString(config.outputVolume));

        distance = field(left, top + row * ROW_H, "Range (blocks)", Float.toString(config.voiceDistance));
        pushToTalkButton = addButton("Hold to talk: " + (config.pushToTalkMode ? "ON" : "OFF"), right, top + row++ * ROW_H, () -> {
            config.pushToTalkMode = !config.pushToTalkMode;
            pushToTalkButton.setMessage(Component.literal("Hold to talk: " + (config.pushToTalkMode ? "ON" : "OFF")));
        });

        villagerVoicesButton = addButton("NPC voices: " + (config.villagerVoicesEnabled ? "ON" : "OFF"), left, top + row * ROW_H, () -> {
            config.villagerVoicesEnabled = !config.villagerVoicesEnabled;
            villagerVoicesButton.setMessage(Component.literal("NPC voices: " + (config.villagerVoicesEnabled ? "ON" : "OFF")));
        });
        pttKeyButton = addButton("PTT key: " + keyName(config.pushToTalkKey), right, top + row++ * ROW_H, this::cyclePushToTalkKey);

        microphoneTestButton = addButton("Test microphone", left, top + row * ROW_H, this::toggleMicrophoneTest);
        addRenderableWidget(Button.builder(Component.literal("Test speaker"), b -> {
            if (testToneRunning) return;
            testToneRunning = true;
            b.setMessage(Component.literal("Testing..."));
            String selected = actualDeviceName(outputs.get(outputIndex));
            float volume = config.outputVolume;
            java.util.concurrent.CompletableFuture.supplyAsync(() -> AudioDeviceManager.playTestTone(selected, volume))
                    .whenComplete((ok, error) -> Minecraft.getInstance().execute(() -> {
                        testToneRunning = false;
                        if (Minecraft.getInstance().gui.screen() == this) {
                            b.setMessage(Component.literal(error == null && Boolean.TRUE.equals(ok) ? "Speaker OK" : "Speaker failed"));
                        }
                    }));
        }).bounds(right, top + row++ * ROW_H, COL_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Rescan devices"), b -> {
            savePageToConfig();
            refreshDevices();
            refreshDeviceButtons();
            b.setMessage(Component.literal("Refreshed"));
        }).bounds(left, top + row * ROW_H, COL_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Setup guide"), b ->
                Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this)))
                .bounds(right, top + row++ * ROW_H, COL_W, 20).build());
    }

    private void buildAiPage(int left, int right, int top) {
        int row = 0;
        providerButton = addButton("Provider: " + displayProvider(config.provider), left, top + row++ * ROW_H, () -> {
            config.provider = nextProvider(config.provider);
            providerButton.setMessage(Component.literal("Provider: " + displayProvider(config.provider)));
        });

        language = field(left, top + row * ROW_H, "Language", config.language);
        model = field(right, top + row++ * ROW_H, "AI model", config.model);

        sttModel = field(left, top + row * ROW_H, "STT model", config.sttModel);
        ttsModel = field(right, top + row++ * ROW_H, "TTS model", config.ttsModel);

        ttsVoice = field(left, top + row * ROW_H, "TTS voice", config.ttsVoice);
        apiKey = field(right, top + row++ * ROW_H, "API key", config.apiKey);
        apiKey.setMaxLength(512);
        apiKey.setSuggestion("Stored locally in your Minecraft config");

        sttCommand = field(left, top + row * ROW_H, "Local STT (optional)", config.sttCommand);
        sttCommand.setSuggestion("Optional local speech-to-text command");
        ttsCommand = field(right, top + row++ * ROW_H, "Local TTS (optional)", config.ttsCommand);
        ttsCommand.setSuggestion("Optional local text-to-speech command");

        addRenderableWidget(Button.builder(Component.literal("Recommended defaults"), b -> {
            language.setValue("en-US");
            model.setValue("gpt-4o-mini");
            sttModel.setValue("gpt-4o-mini-transcribe");
            ttsModel.setValue("gpt-4o-mini-tts");
            ttsVoice.setValue("alloy");
            sttCommand.setValue("");
            ttsCommand.setValue("");
            b.setMessage(Component.literal("Defaults applied"));
        }).bounds(left, top + row * ROW_H, COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Setup guide"), b ->
                Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this)))
                .bounds(right, top + row * ROW_H, COL_W, 20).build());
    }

    private void buildNpcPage(int left, int right, int top) {
        int row = 0;
        temperamentButton = addButton("Temperament: " + prettyTemperament(temperaments.get(temperamentIndex)), left, top + row++ * ROW_H, this::cycleTemperament);
        villagerVoiceTestButton = addButton("Test villager voice", right, top, () -> {
            TheWorldRemembersClient.testVillagerVoice();
            villagerVoiceTestButton.setMessage(Component.literal("TTS test started"));
        });

        ttsInstructions = field(left, top + row * ROW_H, "Speaking style", config.ttsInstructions);
        systemPrompt = field(right, top + row++ * ROW_H, "AI rules", config.systemPrompt);

        addRenderableWidget(Button.builder(Component.literal("Villager defaults"), b -> {
            ttsInstructions.setValue("Speak like a Minecraft villager: warm, conversational, rustic, short phrases, natural pauses.");
            systemPrompt.setValue("You are a Minecraft villager. Speak briefly and in character. Only use facts supplied by the simulation.");
            b.setMessage(Component.literal("Defaults applied"));
        }).bounds(left, top + row * ROW_H, COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voice guide"), b ->
                Minecraft.getInstance().gui.setScreen(new VoiceSetupScreen(this)))
                .bounds(right, top + row++ * ROW_H, COL_W, 20).build());
    }

    private Button addButton(String text, int x, int y, Runnable action) {
        Button button = Button.builder(Component.literal(fit(text, COL_W - 8)), ignored -> action.run())
                .bounds(x, y, COL_W, 20).build();
        addRenderableWidget(button);
        return button;
    }

    private String fit(String text, int maxPixels) {
        if (text == null) return "";
        if (this.font.width(text) <= maxPixels) return text;
        String ellipsis = "...";
        String value = text;
        while (value.length() > 1 && this.font.width(value + ellipsis) > maxPixels) {
            value = value.substring(0, value.length() - 1);
        }
        return value + ellipsis;
    }

    private void refreshDeviceButtons() {
        if (microphones.isEmpty()) microphones = List.of(AudioDeviceManager.DEFAULT_DEVICE);
        if (outputs.isEmpty()) outputs = List.of(AudioDeviceManager.DEFAULT_DEVICE);
        microphoneIndex = Math.max(0, Math.min(microphoneIndex, microphones.size() - 1));
        outputIndex = Math.max(0, Math.min(outputIndex, outputs.size() - 1));
        if (microphoneButton != null) microphoneButton.setMessage(Component.literal(fit("Mic: " + microphones.get(microphoneIndex), COL_W - 8)));
        if (outputButton != null) outputButton.setMessage(Component.literal(fit("Out: " + outputs.get(outputIndex), COL_W - 8)));
        refreshDeviceStatus();
    }

    private void refreshDeviceStatus() {
        if (microphoneStatusButton != null && !microphones.isEmpty()) {
            microphoneStatusButton.setMessage(Component.literal("Mic: " + AudioDeviceManager.describeAvailability(actualDeviceName(microphones.get(microphoneIndex)), true)));
        }
        if (outputStatusButton != null && !outputs.isEmpty()) {
            outputStatusButton.setMessage(Component.literal("Out: " + AudioDeviceManager.describeAvailability(actualDeviceName(outputs.get(outputIndex)), false)));
        }
    }

    private EditBox field(int x, int y, String label, String value) {
        EditBox box = new EditBox(this.font, x, y, COL_W, 20, Component.literal(label));
        box.setValue(value == null ? "" : fit(value, COL_W - 10));
        box.setHint(Component.literal(label));
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
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == config.pushToTalkKey) {
                current = i;
                break;
            }
        }
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
        return switch (key) {
            case 66 -> "B";
            case 67 -> "C";
            case 71 -> "G";
            case 88 -> "X";
            default -> "V";
        };
    }

    private void cycleMicrophone() {
        microphoneIndex = (microphoneIndex + 1) % microphones.size();
        config.microphone = actualDeviceName(microphones.get(microphoneIndex));
        microphoneButton.setMessage(Component.literal(fit("Mic: " + microphones.get(microphoneIndex), COL_W - 8)));
        refreshDeviceStatus();
    }

    private void cycleOutput() {
        outputIndex = (outputIndex + 1) % outputs.size();
        config.outputDevice = actualDeviceName(outputs.get(outputIndex));
        outputButton.setMessage(Component.literal(fit("Out: " + outputs.get(outputIndex), COL_W - 8)));
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

        graphics.centeredText(this.font, "THE WORLD REMEMBERS", this.width / 2, 8, 0xFFFFFFFF);
        String subtitle = switch (page) {
            case 0 -> "Audio";
            case 1 -> "AI & Speech";
            default -> "Villagers";
        };
        graphics.centeredText(this.font, subtitle + "  •  compact voice configuration", this.width / 2, 20, 0xFFB8B8B8);

        int left = leftColumn();
        int right = rightColumn();
        int top = contentTop();

        if (page == 0) {
            graphics.text(this.font, "INPUT / OUTPUT", left, top - 9, 0xFFE6E6E6, false);
            graphics.text(this.font, "CONTROLS + TESTS", right, top - 9, 0xFFE6E6E6, false);
            graphics.text(this.font, "Detected: " + microphones.size() + " mic · " + outputs.size() + " output",
                    left, top + 122, 0xFF9E9E9E, false);
            graphics.text(this.font, fit("Mic: " + AudioDeviceManager.describeAvailability(actualDeviceName(microphones.get(microphoneIndex)), true), COL_W * 2 + GAP),
                    left, top + 136, 0xFFAAAAAA, false);
            graphics.text(this.font, fit("Out: " + AudioDeviceManager.describeAvailability(actualDeviceName(outputs.get(outputIndex)), false), COL_W * 2 + GAP),
                    left, top + 150, 0xFFAAAAAA, false);
        } else if (page == 1) {
            graphics.text(this.font, "ONLINE VOICE / AI", left, top - 9, 0xFFE6E6E6, false);
            graphics.text(this.font, "LOCAL OPTIONAL", left, top + 84, 0xFFE6E6E6, false);
            graphics.text(this.font, "Leave local fields empty for online voice.", left, top + 122, 0xFFAAAAAA, false);
        } else {
            graphics.text(this.font, "VILLAGER VOICE", left, top - 9, 0xFFE6E6E6, false);
            graphics.text(this.font, "PERSONALITY + SPEAKING RULES", left, top + 48, 0xFFE6E6E6, false);
            graphics.text(this.font, "Speech style only; simulation unchanged.", left, top + 76, 0xFFAAAAAA, false);
            graphics.text(this.font, "Temperament: " + prettyTemperament(temperaments.get(temperamentIndex)),
                    left, top + 92, 0xFF9E9E9E, false);
            String error = TheWorldRemembersClient.lastVillagerVoiceError();
            if (error != null && !error.isBlank()) {
                graphics.text(this.font, fit("TTS: " + error, CONTENT_W), left, top + 108, 0xFFFF7777, false);
            }
        }

        graphics.text(this.font, "Unsaved changes", left, this.height - 44, 0xFFE0C070, false);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String text, int x, int y) {
        graphics.text(this.font, text, x, y, 0xFFE6E6E6, false);
    }}
