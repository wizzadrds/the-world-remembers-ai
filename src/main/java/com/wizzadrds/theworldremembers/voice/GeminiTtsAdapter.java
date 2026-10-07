package com.wizzadrds.theworldremembers.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

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

        JsonObject body = requestBody(text, profile, false);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini TTS failed: HTTP " + response.statusCode() + " - " + response.body());
        }

        byte[] audio = Base64.getDecoder().decode(extractAudio(response.body()));
        Path target = output.toAbsolutePath();
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);
        Files.write(target, audio);
        return target;
    }

    @Override
    public InputStream synthesizeStream(String text, VoiceProfile profile) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("TTS text is empty");

        JsonObject body = requestBody(text, profile, true);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (InputStream bodyStream = response.body()) {
                String error = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("Gemini TTS failed: HTTP " + response.statusCode() + " - " + error);
            }
        }
        return new GeminiPcmStream(response.body());
    }

    private JsonObject requestBody(String text, VoiceProfile profile, boolean streaming) {
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
            style = (style.isBlank() ? "" : style + " ")
                    + "Temperament: " + profile.temperament().name().toLowerCase(Locale.ROOT) + ".";
        }
        if (!style.isBlank()) speechMetadata.addProperty("style", style);
        annotations.add(speechMetadata);
        textPart.add("annotations", annotations);

        content.add(textPart);
        userInput.add("content", content);
        input.add(userInput);
        body.add("input", input);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "audio");
        if (streaming) {
            responseFormat.addProperty("mime_type", "audio/l16");
            responseFormat.addProperty("sample_rate", 24000);
        }
        body.add("response_format", responseFormat);

        JsonObject generation = new JsonObject();
        JsonObject speechConfig = new JsonObject();
        speechConfig.addProperty("voice", voice);
        JsonArray speechConfigs = new JsonArray();
        speechConfigs.add(speechConfig);
        generation.add("speech_config", speechConfigs);
        body.add("generation_config", generation);
        if (streaming) body.addProperty("stream", true);
        return body;
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

    private static final class GeminiPcmStream extends InputStream {
        private final BufferedReader reader;
        private byte[] pending = new byte[0];
        private int offset;
        private boolean closed;

        private GeminiPcmStream(InputStream source) {
            reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            if (closed) return -1;
            if (buffer == null) throw new NullPointerException("buffer");
            if (off < 0 || len < 0 || off + len > buffer.length) throw new IndexOutOfBoundsException();
            if (len == 0) return 0;

            while (offset >= pending.length) {
                if (!readNextAudioChunk()) return -1;
            }

            int count = Math.min(len, pending.length - offset);
            System.arraycopy(pending, offset, buffer, off, count);
            offset += count;
            return count;
        }

        private boolean readNextAudioChunk() throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) continue;

                JsonObject event;
                try {
                    event = JsonParser.parseString(payload).getAsJsonObject();
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (!"step.delta".equals(getString(event, "event_type"))) continue;
                JsonObject delta = event.getAsJsonObject("delta");
                if (delta == null || !"audio".equals(getString(delta, "type")) || !delta.has("data")) continue;

                byte[] decoded;
                try {
                    decoded = Base64.getDecoder().decode(delta.get("data").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new IOException("Gemini TTS returned invalid audio data", e);
                }
                if (decoded.length == 0) continue;
                pending = decoded;
                offset = 0;
                return true;
            }
            return false;
        }

        private static String getString(JsonObject object, String key) {
            JsonElement value = object.get(key);
            return value == null || value.isJsonNull() ? "" : value.getAsString();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                reader.close();
            }
        }
    }
}
