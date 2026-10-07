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

public final class GeminiResponsesAdapter implements AiChatAdapter {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;

    public GeminiResponsesAdapter(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("Gemini API key is required");
        this.apiKey = apiKey.trim();
        this.model = model == null || model.isBlank() ? "gemini-3.8-flash" : model.trim();
    }

    @Override
    public String respond(String userText, String systemPrompt) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("input", userText == null ? "" : userText);
        if (systemPrompt != null && !systemPrompt.isBlank()) body.addProperty("system_instruction", systemPrompt);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini request failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        return extractText(response.body());
    }

    static String extractText(String json) throws IOException {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement direct = root.get("output_text");
        if (direct != null && !direct.isJsonNull()) return direct.getAsString().trim();

        JsonArray steps = root.getAsJsonArray("steps");
        if (steps != null) {
            for (JsonElement stepElement : steps) {
                JsonObject step = stepElement.getAsJsonObject();
                JsonArray content = step.getAsJsonArray("content");
                if (content == null) continue;
                for (JsonElement partElement : content) {
                    JsonObject part = partElement.getAsJsonObject();
                    if ("text".equals(part.get("type").getAsString()) && part.has("text")) {
                        String text = part.get("text").getAsString().trim();
                        if (!text.isBlank()) return text;
                    }
                }
            }
        }
        throw new IOException("Gemini response did not contain output text");
    }
}
