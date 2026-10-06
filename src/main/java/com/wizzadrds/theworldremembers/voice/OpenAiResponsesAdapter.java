package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class OpenAiResponsesAdapter implements AiChatAdapter {
    private final HttpClient client;
    private final String apiKey;
    private final String model;

    public OpenAiResponsesAdapter(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("API key is required");
        this.client = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? "gpt-5.6-luna" : model;
    }

    @Override
    public String respond(String userText, String systemPrompt) throws IOException, InterruptedException {
        String body = "{"
            + "\"model\":\"" + json(model) + "\","
            + "\"instructions\":\"" + json(systemPrompt == null ? "" : systemPrompt) + "\","
            + "\"input\":\"" + json(userText) + "\""
            + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI request failed: HTTP " + response.statusCode());
        }
        return extractOutputText(response.body());
    }

    private static String extractOutputText(String json) throws IOException {
        String marker = "\"type\":\"output_text\",\"text\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            marker = "\"text\":";
            start = json.indexOf(marker);
        }
        if (start < 0) throw new IOException("OpenAI response did not contain output text");
        start = json.indexOf('\"', start + marker.length());
        if (start < 0) throw new IOException("Invalid OpenAI response");
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                switch (ch) {
                    case '\"', '\\', '/' -> out.append(ch);
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    default -> out.append(ch);
                }
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '\"') {
                return out.toString().trim();
            } else {
                out.append(ch);
            }
        }
        throw new IOException("Invalid OpenAI response");
    }
    private static String json(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
