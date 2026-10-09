package com.wizzadrds.theworldremembers.voice;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Cloud TTS fallback for users who only configure an OpenAI API key. */
public final class OpenAiTtsAdapter implements TtsAdapter {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;
    private final String defaultVoice;
    private final String instructions;

    public OpenAiTtsAdapter(String apiKey, String model, String defaultVoice, String instructions) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("OpenAI API key is required");
        this.apiKey = apiKey.trim();
        this.model = model == null || model.isBlank() || model.toLowerCase(Locale.ROOT).startsWith("gemini") ? "gpt-4o-mini-tts" : model.trim();
        this.defaultVoice = normalizeVoice(defaultVoice);
        this.instructions = instructions == null ? "" : instructions.trim();
    }

    @Override
    public Path synthesize(String text, VoiceProfile profile, Path output) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("TTS text is empty");
        if (output == null) throw new IllegalArgumentException("TTS output path is null");
        HttpRequest request = request(text, profile, "wav");
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI TTS failed: HTTP " + response.statusCode() + " - "
                    + new String(response.body(), StandardCharsets.UTF_8));
        }
        Path target = output.toAbsolutePath();
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Files.write(target, response.body());
        return target;
    }

    @Override
    public InputStream synthesizeStream(String text, VoiceProfile profile) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("TTS text is empty");
        HttpResponse<InputStream> response = client.send(request(text, profile, "pcm"), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (InputStream body = response.body()) {
                throw new IOException("OpenAI TTS failed: HTTP " + response.statusCode() + " - "
                        + new String(body.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return response.body();
    }

    private static String normalizeVoice(String value) {
        if (value == null || value.isBlank()) return "alloy";
        String voice = value.trim().toLowerCase(Locale.ROOT);
        if (voice.equals("algenib") || voice.equals("kore")) return "alloy";
        return switch (voice) {
            case "alloy", "ash", "ballad", "coral", "echo", "fable", "onyx", "nova", "sage", "shimmer", "verse", "marin", "cedar" -> voice;
            default -> "alloy";
        };
    }

    private HttpRequest request(String text, VoiceProfile profile, String format) {
        String voice = normalizeVoice(profile != null ? profile.modelId() : defaultVoice);
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("voice", voice.toLowerCase(Locale.ROOT));
        body.addProperty("input", text);
        body.addProperty("response_format", format);
        if (!instructions.isBlank()) body.addProperty("instructions", instructions);
        if (profile != null && Float.isFinite(profile.rate())) {
            body.addProperty("speed", Math.max(0.25f, Math.min(4.0f, profile.rate())));
        }
        return HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/speech"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
    }
}