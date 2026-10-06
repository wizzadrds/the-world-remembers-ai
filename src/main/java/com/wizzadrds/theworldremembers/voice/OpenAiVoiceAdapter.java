package com.wizzadrds.theworldremembers.voice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class OpenAiVoiceAdapter implements SttAdapter, TtsAdapter {
    private static final URI TRANSCRIBE = URI.create("https://api.openai.com/v1/audio/transcriptions");
    private static final URI SPEECH = URI.create("https://api.openai.com/v1/audio/speech");
    private static final Gson GSON = new Gson();
    private final String apiKey;
    private final String sttModel;
    private final String ttsModel;
    private final String voice;
    private final String instructions;
    private final HttpClient client = HttpClient.newHttpClient();

    public OpenAiVoiceAdapter(String apiKey, String sttModel, String ttsModel, String voice, String instructions) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("OpenAI API key is required");
        this.apiKey = apiKey.trim();
        this.sttModel = sttModel == null || sttModel.isBlank() ? "gpt-4o-mini-transcribe" : sttModel.trim();
        this.ttsModel = ttsModel == null || ttsModel.isBlank() ? "gpt-4o-mini-tts" : ttsModel.trim();
        this.voice = voice == null || voice.isBlank() ? "onyx" : voice.trim();
        this.instructions = instructions == null || instructions.isBlank()
                ? "Speak like a rustic, friendly Minecraft villager NPC: slightly nasal, expressive, short natural phrases, never like a narrator."
                : instructions;
    }

    @Override
    public String transcribe(byte[] pcm16kMono) throws IOException, InterruptedException {
        String boundary = "----TWR-" + UUID.randomUUID();
        byte[] wav = wav16kMono(pcm16kMono);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writePart(body, boundary, "model", sttModel);
        writeFilePart(body, boundary, "file", "voice.wav", "audio/wav", wav);
        body.write(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(TRANSCRIBE)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IOException("OpenAI STT failed: HTTP " + response.statusCode());
        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        return json.has("text") ? json.get("text").getAsString().trim() : "";
    }

    @Override
    public Path synthesize(String text, VoiceProfile profile, Path output) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", ttsModel);
        body.addProperty("voice", voice);
        body.addProperty("input", text);
        body.addProperty("instructions", instructions);
        body.addProperty("response_format", "pcm");
        HttpRequest request = HttpRequest.newBuilder(SPEECH)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) throw new IOException("OpenAI TTS failed: HTTP " + response.statusCode());
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, wav24kMono(response.body()));
        return output;
    }

    private static void writePart(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void writeFilePart(ByteArrayOutputStream out, String boundary, String name, String file, String type, byte[] data) throws IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file + "\"\r\nContent-Type: " + type + "\r\n\r\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static byte[] wav16kMono(byte[] pcm) {
        return wav(pcm, 16000);
    }

    private static byte[] wav24kMono(byte[] pcm) {
        return wav(pcm, 24000);
    }

    private static byte[] wav(byte[] pcm, int sampleRate) {
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
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + pcm.length);
        out.writeBytes(header.array());
        out.writeBytes(pcm);
        return out.toByteArray();
    }
}
