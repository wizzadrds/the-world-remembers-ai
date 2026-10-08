package com.wizzadrds.theworldremembers.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VoiceClientConfig {
    private static final String DEFAULT_TTS_INSTRUCTIONS =
            "Speak only the villager's exact dialogue. No narration, no stage directions, no scene description, no assistant wording. "
                    + "The game applies a dedicated Minecraft-villager voice modulator after TTS: nasal, compressed, rough, pitch-shifted, clipped NPC speech.";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are the specific Minecraft villager the player is standing near and speaking to. "
                    + "Reply only with that villager's spoken dialogue. Never narrate actions, emotions, scenes, or third-person events. "
                    + "Never answer as a generic AI. Keep the reply brief, conversational, and in character.";
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
    public String ttsVoice = "Algenib";
    public String ttsInstructions = DEFAULT_TTS_INSTRUCTIONS;
    public String systemPrompt = DEFAULT_SYSTEM_PROMPT;

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
        if (ttsVoice == null || ttsVoice.isBlank() || ttsVoice.equalsIgnoreCase("Kore")) ttsVoice = "Algenib";
        else ttsVoice = ttsVoice.trim();
        if (sttCommand == null) sttCommand = "";
        else sttCommand = sttCommand.trim();
        if (ttsCommand == null) ttsCommand = "";
        else ttsCommand = ttsCommand.trim();
        if (ttsInstructions == null || ttsInstructions.isBlank()
                || ttsInstructions.startsWith("Minecraft Villager voice.")
                || ttsInstructions.startsWith("Use a low, muffled, nasal")) {
            ttsInstructions = DEFAULT_TTS_INSTRUCTIONS;
        }
        if (systemPrompt == null || systemPrompt.isBlank()
                || systemPrompt.startsWith("You are one specific Minecraft villager")) {
            systemPrompt = DEFAULT_SYSTEM_PROMPT;
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