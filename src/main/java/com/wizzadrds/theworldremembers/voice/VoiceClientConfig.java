package com.wizzadrds.theworldremembers.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VoiceClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String microphone = "Default";
    public int pushToTalkKey = 86;
    public boolean pushToTalkMode = true;
    public float inputVolume = 1.0f;
    public float outputVolume = 1.0f;
    public String outputDevice = "Default";
    public float voiceDistance = 32.0f;
    public String provider = "openai";
    public String apiKey = "";
    public String model = "";
    public String sttModel = "gpt-4o-mini-transcribe";
    public String sttCommand = "";
    public String ttsCommand = "";
    public String ttsModel = "gpt-4o-mini-tts";
    public String ttsVoice = "onyx";
    public String ttsInstructions = "Speak like a rustic, friendly Minecraft villager NPC: slightly nasal, expressive, short natural phrases, never like a narrator.";
    public String systemPrompt = "You are a Minecraft NPC. Answer briefly, naturally, and stay in character.";

    public static VoiceClientConfig load(Path gameDir) {
        Path file = file(gameDir);
        try {
            if (Files.exists(file)) {
                VoiceClientConfig config = GSON.fromJson(Files.readString(file), VoiceClientConfig.class);
                if (config != null) return config;
            }
        } catch (Exception ignored) {
        }
        return new VoiceClientConfig();
    }

    public void save(Path gameDir) {
        Path file = file(gameDir);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path file(Path gameDir) {
        return gameDir.resolve("config").resolve("the_world_remembers_voice.json");
    }
}
