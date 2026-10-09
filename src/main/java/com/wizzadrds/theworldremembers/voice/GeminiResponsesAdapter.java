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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public final class GeminiResponsesAdapter implements AiChatAdapter {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;
    private static final String CAPACITY_FALLBACK_MODEL = "gemini-3.7-flash";

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

        // Villager dialogue should answer quickly rather than spend time on deep reasoning.
        // Gemini documents lower thinking levels as the latency-oriented control.
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("thinking_level", "low");
        body.add("generation_config", generationConfig);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if ((response.statusCode() == 503 || response.statusCode() == 429) && model.equals("gemini-3.8-flash")) {
            return respondWithModel(userText, systemPrompt, CAPACITY_FALLBACK_MODEL);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw geminiHttpError(response.statusCode(), response.body());
        }
        return extractText(response.body());
    }

    @Override
    public String respondStreaming(String userText, String systemPrompt, Consumer<String> chunkConsumer) throws IOException, InterruptedException {
        if (chunkConsumer == null) return respond(userText, systemPrompt);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("input", userText == null ? "" : userText);
        if (systemPrompt != null && !systemPrompt.isBlank()) body.addProperty("system_instruction", systemPrompt);
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("thinking_level", "low");
        body.add("generation_config", generationConfig);
        body.addProperty("stream", true);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response =
                client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if ((response.statusCode() == 503 || response.statusCode() == 429) && model.equals("gemini-3.8-flash")) {
            try (java.io.InputStream stream = response.body()) {
                stream.readAllBytes();
            }
            return respondStreamingWithModel(userText, systemPrompt, chunkConsumer, CAPACITY_FALLBACK_MODEL);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (java.io.InputStream stream = response.body()) {
                throw geminiHttpError(response.statusCode(), new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        StringBuilder full = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) continue;
                JsonObject event;
                try {
                    event = JsonParser.parseString(data).getAsJsonObject();
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (!"step.delta".equals(event.get("event_type").getAsString())) continue;
                JsonObject delta = event.getAsJsonObject("delta");
                if (delta == null || !"text".equals(delta.get("type").getAsString()) || !delta.has("text")) continue;
                String chunk = delta.get("text").getAsString();
                if (chunk.isBlank()) continue;
                full.append(chunk);
                chunkConsumer.accept(chunk);
            }
        }
        if (full.toString().isBlank()) throw new IOException("Gemini streaming response did not contain output text");
        return full.toString().trim();
    }



    private String respondWithModel(String userText, String systemPrompt, String fallbackModel)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", fallbackModel);
        body.addProperty("input", userText == null ? "" : userText);
        if (systemPrompt != null && !systemPrompt.isBlank()) body.addProperty("system_instruction", systemPrompt);
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("thinking_level", "low");
        body.add("generation_config", generationConfig);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw geminiHttpError(response.statusCode(), response.body());
        }
        return extractText(response.body());
    }

    private String respondStreamingWithModel(String userText, String systemPrompt, Consumer<String> chunkConsumer, String fallbackModel)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", fallbackModel);
        body.addProperty("input", userText == null ? "" : userText);
        if (systemPrompt != null && !systemPrompt.isBlank()) body.addProperty("system_instruction", systemPrompt);
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("thinking_level", "low");
        body.add("generation_config", generationConfig);
        body.addProperty("stream", true);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (java.io.InputStream stream = response.body()) {
                throw geminiHttpError(response.statusCode(), new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        StringBuilder full = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) continue;
                try {
                    JsonObject event = JsonParser.parseString(data).getAsJsonObject();
                    if (!"step.delta".equals(event.get("event_type").getAsString())) continue;
                    JsonObject delta = event.getAsJsonObject("delta");
                    if (delta == null || !"text".equals(delta.get("type").getAsString()) || !delta.has("text")) continue;
                    String chunk = delta.get("text").getAsString();
                    if (!chunk.isBlank()) {
                        full.append(chunk);
                        chunkConsumer.accept(chunk);
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }
        if (full.toString().isBlank()) throw new IOException("Gemini streaming response did not contain output text");
        return full.toString().trim();
    }


    private static IOException geminiHttpError(int status, String body) {
        String message = body == null ? "" : body.trim();
        try {
            JsonObject root = JsonParser.parseString(message).getAsJsonObject();
            JsonObject error = root.getAsJsonObject("error");
            if (error != null && error.has("message")) {
                String detail = error.get("message").getAsString();
                String code = error.has("code") ? error.get("code").getAsString() : "";
                return new IOException("Gemini request failed: HTTP " + status + " - "
                        + (code.isBlank() ? detail : code + ": " + detail));
            }
        } catch (RuntimeException ignored) {
        }
        if (message.length() > 2000) message = message.substring(0, 2000);
        return new IOException("Gemini request failed: HTTP " + status + " - " + message);
    }

    static String extractText(String json) throws IOException {
        String text = extractTextOrEmpty(json);
        if (!text.isBlank()) return text;
        throw new IOException("Gemini response did not contain output text");
    }

    static String extractTextOrEmpty(String json) throws IOException {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement direct = root.get("output_text");
        if (direct != null && !direct.isJsonNull() && direct.isJsonPrimitive()) {
            String text = direct.getAsString().trim();
            if (!text.isBlank()) return text;
        }

        JsonArray steps = root.getAsJsonArray("steps");
        if (steps != null) {
            for (JsonElement stepElement : steps) {
                if (!stepElement.isJsonObject()) continue;
                JsonObject step = stepElement.getAsJsonObject();
                JsonArray content = step.getAsJsonArray("content");
                if (content == null) continue;
                for (JsonElement partElement : content) {
                    if (!partElement.isJsonObject()) continue;
                    JsonObject part = partElement.getAsJsonObject();
                    JsonElement value = part.get("text");
                    if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) {
                        String text = value.getAsString().trim();
                        if (!text.isBlank()) return text;
                    }
                }
            }
        }
        return "";
    }
}
