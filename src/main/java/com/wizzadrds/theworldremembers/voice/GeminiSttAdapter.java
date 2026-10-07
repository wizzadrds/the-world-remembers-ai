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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GeminiSttAdapter implements SttAdapter {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String language;

    public GeminiSttAdapter(String apiKey, String language) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("Gemini API key is required");
        this.apiKey = apiKey.trim();
        this.language = language == null || language.isBlank() ? "es-ES" : language.trim();
    }

    @Override
    public String transcribe(byte[] pcm16kMono) throws IOException, InterruptedException {
        if (pcm16kMono == null || pcm16kMono.length == 0) return "";
        Path wav = Files.createTempFile("twr-gemini-stt-", ".wav");
        try {
            Files.write(wav, wavBytes(pcm16kMono, 16000));
            String fileUri = uploadFile(wav);
            JsonObject body = new JsonObject();
            body.addProperty("model", "gemini-3.5-transcribe");
            JsonArray input = new JsonArray();
            JsonObject audio = new JsonObject();
            audio.addProperty("type", "audio");
            audio.addProperty("uri", fileUri);
            audio.addProperty("mime_type", "audio/wav");
            input.add(audio);
            body.add("input", input);

            JsonObject generation = new JsonObject();
            JsonObject transcription = new JsonObject();
            JsonArray languages = new JsonArray();
            languages.add(language);
            transcription.add("language_codes", languages);
            JsonObject mode = new JsonObject();
            mode.addProperty("mode", "smart");
            transcription.addProperty("mode", "smart");
            generation.add("transcription_config", transcription);
            body.add("generation_config", generation);

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Gemini STT failed: HTTP " + response.statusCode() + " - " + response.body());
            }
            return GeminiResponsesAdapter.extractText(response.body());
        } finally {
            Files.deleteIfExists(wav);
        }
    }

    private String uploadFile(Path file) throws IOException, InterruptedException {
        byte[] bytes = Files.readAllBytes(file);
        HttpRequest start = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/upload/v1beta/files"))
                .header("x-goog-api-key", apiKey)
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", Long.toString(bytes.length))
                .header("X-Goog-Upload-Header-Content-Type", "audio/wav")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"file\":{\"display_name\":\"twr_voice_input.wav\"}}", StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> startResponse = client.send(start, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (startResponse.statusCode() < 200 || startResponse.statusCode() >= 300) {
            throw new IOException("Gemini file upload initialization failed: HTTP " + startResponse.statusCode() + " - " + startResponse.body());
        }
        String uploadUrl = startResponse.headers().firstValue("x-goog-upload-url").orElse("");
        if (uploadUrl.isBlank()) throw new IOException("Gemini file upload did not return an upload URL");

        HttpRequest upload = HttpRequest.newBuilder(URI.create(uploadUrl))
                .header("Content-Length", Long.toString(bytes.length))
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        HttpResponse<String> uploadResponse = client.send(upload, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (uploadResponse.statusCode() < 200 || uploadResponse.statusCode() >= 300) {
            throw new IOException("Gemini file upload failed: HTTP " + uploadResponse.statusCode() + " - " + uploadResponse.body());
        }
        JsonObject root = JsonParser.parseString(uploadResponse.body()).getAsJsonObject();
        JsonObject fileObject = root.getAsJsonObject("file");
        if (fileObject == null || !fileObject.has("uri")) throw new IOException("Gemini upload response did not contain a file URI");
        return fileObject.get("uri").getAsString();
    }

    private static byte[] wavBytes(byte[] pcm, int sampleRate) {
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'R','I','F','F'});
        header.putInt(36 + pcm.length);
        header.put(new byte[]{'W','A','V','E','f','m','t',' '});
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) 1);
        header.putInt(sampleRate);
        header.putInt(sampleRate * 2);
        header.putShort((short) 2);
        header.putShort((short) 16);
        header.put(new byte[]{'d','a','t','a'});
        header.putInt(pcm.length);
        byte[] result = new byte[44 + pcm.length];
        System.arraycopy(header.array(), 0, result, 0, 44);
        System.arraycopy(pcm, 0, result, 44, pcm.length);
        return result;
    }
}
