package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class OpenAiResponsesAdapter implements AiChatAdapter {
    private final HttpClient client;
    private final String apiKey;
    private final String model;

    public OpenAiResponsesAdapter(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("API key is required");
        this.client = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? "gpt-6-luna" : model;
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

    @Override
    public String respondStreaming(String userText, String systemPrompt, Consumer<String> chunkConsumer)
            throws IOException, InterruptedException {
        String body = "{"
            + "\"model\":\"" + json(model) + "\","
            + "\"instructions\":\"" + json(systemPrompt == null ? "" : systemPrompt) + "\","
            + "\"input\":\"" + json(userText) + "\","
            + "\"stream\":true"
            + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (Stream<String> lines = response.body()) {
                String detail = lines.limit(20).reduce("", (a, b) -> a + b);
                throw new IOException("OpenAI streaming request failed: HTTP " + response.statusCode()
                        + (detail.isBlank() ? "" : " — " + detail.substring(0, Math.min(300, detail.length()))));
            }
        }
        StringBuilder answer = new StringBuilder();
        try (Stream<String> lines = response.body()) {
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) continue;
                JsonObject event;
                try {
                    event = JsonParser.parseString(data).getAsJsonObject();
                } catch (RuntimeException malformed) {
                    continue;
                }
                String type = event.has("type") ? event.get("type").getAsString() : "";
                if ("response.output_text.delta".equals(type) && event.has("delta")) {
                    String delta = event.get("delta").getAsString();
                    answer.append(delta);
                    if (chunkConsumer != null && !delta.isEmpty()) chunkConsumer.accept(delta);
                } else if ("error".equals(type) || "response.failed".equals(type)) {
                    String detail = event.has("message") ? event.get("message").getAsString()
                            : event.has("error") ? event.get("error").toString() : "unknown streaming error";
                    throw new IOException("OpenAI streaming response failed: " + detail);
                }
            }
        }
        if (answer.toString().isBlank()) {
            throw new IOException("OpenAI streaming response did not contain output text");
        }
        return answer.toString().trim();
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
