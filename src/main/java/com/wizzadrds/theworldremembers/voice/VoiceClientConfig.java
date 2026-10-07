package com.wizzadrds.theworldremembers.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VoiceClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String microphone = AudioDeviceManager.DEFAULT_DEVICE;
    public String outputDevice = AudioDeviceManager.DEFAULT_DEVICE;
    public boolean villagerVoicesEnabled = true;
    public String villagerVoiceTemperament = "WARM";
    public int pushToTalkKey = 86;
    public boolean pushToTalkMode = true;
    public float inputVolume = 1.0f;
    public float outputVolume = 1.0f;
    public float voiceDistance = 32.0f;
    public String provider = "gemini";
    public String language = "es-ES";
    public String apiKey = "";
    public String model = "gemini-3.8-flash";
    public String sttModel = "gemini-3.5-transcribe";
    public String sttCommand = "";
    public String ttsCommand = "";
    public String ttsModel = "gemini-3.8-flash-tts";
    public String ttsVoice = "Kore";
    public String ttsInstructions = "Speak naturally as a Minecraft villager: short phrases, warm human-like delivery, no announcer voice.";
    public String systemPrompt = "You are a Minecraft NPC. Answer briefly, naturally, and stay in character.";

    public static VoiceClientConfig load(Path gameDir) {
        Path file = file(gameDir);
        try {
            if (Files.exists(file)) {
                VoiceClientConfig config = GSON.fromJson(Files.readString(file), VoiceClientConfig.class);
                if (config != null) return config.normalized();
            }
        } catch (Exception ignored) {
            // Preserve the broken file for diagnosis instead of silently overwriting it.
            backupCorruptConfig(file);
        }
        return new VoiceClientConfig();
    }

    public VoiceClientConfig normalized() {
        if (microphone == null || microphone.isBlank()) microphone = AudioDeviceManager.DEFAULT_DEVICE;
        if (outputDevice == null || outputDevice.isBlank()) outputDevice = AudioDeviceManager.DEFAULT_DEVICE;
        if (villagerVoiceTemperament == null || villagerVoiceTemperament.isBlank()) villagerVoiceTemperament = "WARM";
        if (pushToTalkKey <= 0) pushToTalkKey = 86;
        inputVolume = finiteClamp(inputVolume, 0.0f, 2.0f, 1.0f);
        outputVolume = finiteClamp(outputVolume, 0.0f, 2.0f, 1.0f);
        voiceDistance = finiteClamp(voiceDistance, 1.0f, 64.0f, 32.0f);
        if (provider == null || provider.isBlank()) provider = "gemini";
        if (apiKey == null) apiKey = "";
        else apiKey = apiKey.trim();
        if (language == null || language.isBlank()) language = "es-ES";
        if (sttModel == null || sttModel.isBlank()) sttModel = "gemini-3.5-transcribe";
        else sttModel = sttModel.trim();
        if (ttsModel == null || ttsModel.isBlank()) ttsModel = "gemini-3.8-flash-tts";
        else ttsModel = ttsModel.trim();
        if (ttsVoice == null || ttsVoice.isBlank()) ttsVoice = "Kore";
        else ttsVoice = ttsVoice.trim();
        if (sttCommand == null) sttCommand = "";
        else sttCommand = sttCommand.trim();
        if (ttsCommand == null) ttsCommand = "";
        else ttsCommand = ttsCommand.trim();
        if (ttsInstructions == null || ttsInstructions.isBlank()) {
            ttsInstructions = "Speak naturally as a Minecraft villager: short phrases, warm human-like delivery, no announcer voice.";
        }
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "You are a Minecraft NPC. Answer briefly, naturally, and stay in character.";
        }
        return this;
    }

    private static float finiteClamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    public boolean save(Path gameDir) {
        Path file = file(gameDir);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(normalized()), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            return false;
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
        }
        try {
            VoiceClientConfig persisted = load(gameDir);
            return java.util.Objects.equals(persisted.apiKey, normalized().apiKey);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void backupCorruptConfig(Path file) {
        try {
            if (!Files.isRegularFile(file)) return;
            Path backup = file.resolveSibling(file.getFileName() + ".broken");
            if (Files.exists(backup)) {
                int suffix = 2;
                Path candidate;
                do {
                    candidate = file.resolveSibling(file.getFileName() + ".broken." + suffix++);
                } while (Files.exists(candidate) && suffix < 1000);
                backup = candidate;
            }
            Files.move(file, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private static Path file(Path gameDir) {
        return gameDir.resolve("config").resolve("the_world_remembers_voice.json");
    }
}