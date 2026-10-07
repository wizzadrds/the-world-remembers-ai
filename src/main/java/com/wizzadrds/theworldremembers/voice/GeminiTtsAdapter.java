package com.wizzadrds.theworldremembers.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class GeminiTtsAdapter implements TtsAdapter {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;
    private final String defaultVoice;
    private final String instructions;

    public GeminiTtsAdapter(String apiKey, String model, String defaultVoice, String instructions) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("Gemini API key is required");
        this.apiKey = apiKey.trim();
        this.model = model == null || model.isBlank() ? "gemini-3.8-flash-tts" : model.trim();
        this.defaultVoice = defaultVoice == null || defaultVoice.isBlank() ? "Kore" : defaultVoice.trim();
        this.instructions = instructions == null ? "" : instructions.trim();
    }

    @Override
    public Path synthesize(String text, VoiceProfile profile, Path output) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("TTS text is empty");
        if (output == null) throw new IllegalArgumentException("TTS output path is null");

        String voice = profile != null && profile.modelId() != null && !profile.modelId().isBlank()
                ? profile.modelId().trim() : defaultVoice;
        JsonObject body = new JsonObject();
        body.addProperty("model", model);

        JsonArray input = new JsonArray();
        JsonObject userInput = new JsonObject();
        userInput.addProperty("type", "user_input");
        JsonArray content = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", text);
        JsonArray annotations = new JsonArray();
        JsonObject speechMetadata = new JsonObject();
        speechMetadata.addProperty("type", "speech_metadata");
        String style = instructions;
        if (profile != null && profile.temperament() != null) {
            style = (style.isBlank() ? "" : style + " ") + "Temperament: " + profile.temperament().name().toLowerCase(java.util.Locale.ROOT) + ".";
        }
        if (!style.isBlank()) speechMetadata.addProperty("style", style);
        annotations.add(speechMetadata);
        textPart.add("annotations", annotations);
        content.add(textPart);
        userInput.add("content", content);
        input.add(userInput);
        body.add("input", input);

        body.add("response_format", JsonParser.parseString("{\"type\":\"audio\"}"));
        JsonObject generation = new JsonObject();
        JsonObject speechConfig = new JsonObject();
        speechConfig.addProperty("voice", voice);
        generation.add("speech_config", speechConfig);
        body.add("generation_config", generation);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini TTS failed: HTTP " + response.statusCode() + " - " + response.body());
        }

        String encoded = extractAudio(response.body());
        byte[] audio = Base64.getDecoder().decode(encoded);
        Path target = output.toAbsolutePath();
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);
        Files.write(target, audio);
        return target;
    }

    private static String extractAudio(String json) throws IOException {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray steps = root.getAsJsonArray("steps");
        if (steps != null) {
            for (JsonElement stepElement : steps) {
                JsonObject step = stepElement.getAsJsonObject();
                JsonArray content = step.getAsJsonArray("content");
                if (content == null) continue;
                for (JsonElement partElement : content) {
                    JsonObject part = partElement.getAsJsonObject();
                    if ("audio".equals(part.get("type").getAsString()) && part.has("data")) {
                        return part.get("data").getAsString();
                    }
                }
            }
        }
        JsonObject outputAudio = root.getAsJsonObject("output_audio");
        if (outputAudio != null && outputAudio.has("data")) return outputAudio.get("data").getAsString();
        throw new IOException("Gemini TTS response did not contain audio");
    }
}
