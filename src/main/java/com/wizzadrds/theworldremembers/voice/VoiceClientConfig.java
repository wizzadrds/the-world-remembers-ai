package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class VoiceClientConfig {
    public String provider = "openai";
    public String apiKey = "";
    public String model = "";
    public String microphone = MicrophoneCapture.DEFAULT_DEVICE;
    public String sttCommand = "";
    public String ttsCommand = "";
    public float inputVolume = 1.0f;
    public float outputVolume = 1.0f;
    public float voiceDistance = 32.0f;

    private static final String FILE_NAME = "the-world-remembers-voice.properties";

    public static VoiceClientConfig load(Path gameDirectory) {
        VoiceClientConfig config = new VoiceClientConfig();
        Path file = gameDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) return config;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
            config.provider = p.getProperty("provider", config.provider);
            config.apiKey = p.getProperty("apiKey", config.apiKey);
            config.model = p.getProperty("model", config.model);
            config.microphone = p.getProperty("microphone", config.microphone);
            config.sttCommand = p.getProperty("sttCommand", config.sttCommand);
            config.ttsCommand = p.getProperty("ttsCommand", config.ttsCommand);
            config.inputVolume = bounded(p.getProperty("inputVolume"), config.inputVolume, 0.0f, 2.0f);
            config.outputVolume = bounded(p.getProperty("outputVolume"), config.outputVolume, 0.0f, 2.0f);
            config.voiceDistance = bounded(p.getProperty("voiceDistance"), config.voiceDistance, 1.0f, 128.0f);
        } catch (IOException ignored) {
            // Corrupt/unreadable optional client configuration must never prevent the mod from loading.
        }
        return config;
    }

    public void save(Path gameDirectory) {
        Properties p = new Properties();
        p.setProperty("provider", safe(provider));
        p.setProperty("apiKey", safe(apiKey));
        p.setProperty("model", safe(model));
        p.setProperty("microphone", safe(microphone));
        p.setProperty("sttCommand", safe(sttCommand));
        p.setProperty("ttsCommand", safe(ttsCommand));
        p.setProperty("inputVolume", Float.toString(inputVolume));
        p.setProperty("outputVolume", Float.toString(outputVolume));
        p.setProperty("voiceDistance", Float.toString(voiceDistance));
        Path file = gameDirectory.resolve(FILE_NAME);
        try {
            Files.createDirectories(gameDirectory);
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "The World Remembers voice settings");
            }
        } catch (IOException ignored) {
            // Settings are best-effort; gameplay must continue if the disk is read-only.
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static float bounded(String value, float fallback, float min, float max) {
        if (value == null) return fallback;
        try {
            float parsed = Float.parseFloat(value.trim());
            return Float.isFinite(parsed) ? Math.max(min, Math.min(max, parsed)) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
