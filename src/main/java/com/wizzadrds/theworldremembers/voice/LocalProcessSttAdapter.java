package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LocalProcessSttAdapter implements SttAdapter {
    private final List<String> command;

    public LocalProcessSttAdapter(List<String> command) {
        if (command == null || command.isEmpty()) throw new IllegalArgumentException("STT command is empty");
        this.command = List.copyOf(command);
    }

    @Override
    public String transcribe(byte[] pcm) throws IOException, InterruptedException {
        Path file = Files.createTempFile("twr-stt-", ".pcm");
        try {
            Files.write(file, pcm);
            List<String> args = new ArrayList<>();
            boolean templated = command.stream().anyMatch(token -> token.contains("{pcm}"));
            if (templated) {
                for (String token : command) args.add(token.replace("{pcm}", file.toString()));
            } else {
                args.addAll(command);
                args.add(file.toString());
            }
            Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
            var outputBuffer = new java.io.ByteArrayOutputStream();
            Thread outputReader = Thread.ofVirtual().name("twr-stt-output").start(() -> {
                try {
                    process.getInputStream().transferTo(outputBuffer);
                } catch (IOException ignored) {
                }
            });
            try {
                if (!process.waitFor(45, TimeUnit.SECONDS)) {
                    process.destroy();
                    if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
                    throw new IOException("Local STT timed out after 45 seconds");
                }
                outputReader.join(1000);
                String output = outputBuffer.toString(java.nio.charset.StandardCharsets.UTF_8);
                if (process.exitValue() != 0) throw new IOException("Local STT failed with exit code " + process.exitValue() + ": " + output);
                return output.trim();
            } finally {
                if (process.isAlive()) process.destroyForcibly();
                try {
                    outputReader.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
