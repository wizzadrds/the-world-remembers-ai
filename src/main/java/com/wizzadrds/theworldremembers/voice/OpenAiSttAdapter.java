package com.wizzadrds.theworldremembers.voice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** OpenAI cloud transcription adapter; accepts the same 16 kHz mono PCM as GeminiSttAdapter. */
public final class OpenAiSttAdapter implements SttAdapter {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;
    private final String language;

    public OpenAiSttAdapter(String apiKey, String model, String language) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("OpenAI API key is required");
        this.apiKey = apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-4o-mini-transcribe" : model.trim();
        this.language = language == null ? "" : language.trim();
    }

    @Override
    public String transcribe(byte[] pcm16kMono) throws IOException, InterruptedException {
        if (pcm16kMono == null || pcm16kMono.length == 0) return "";
        byte[] wav = MicrophoneCapture.wavBytes(pcm16kMono, 16000);
        String boundary = "----TWR" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n" + model + "\r\n");
        if (!language.isBlank()) {
            String lang = language.split("[-_]")[0].toLowerCase(java.util.Locale.ROOT);
            write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\n" + lang + "\r\n");
        }
        write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\njson\r\n");
        write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"voice.wav\"\r\nContent-Type: audio/wav\r\n\r\n");
        body.write(wav);
        write(body, "\r\n--" + boundary + "--\r\n");

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/transcriptions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI transcription failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String text = json.has("text") ? json.get("text").getAsString().trim() : "";
            if (text.isBlank()) throw new IOException("OpenAI transcription returned no text");
            return text;
        } catch (IllegalStateException | com.google.gson.JsonParseException e) {
            throw new IOException("OpenAI transcription returned an invalid response", e);
        }
    }

    private static void write(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }
}