package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (process.waitFor() != 0) throw new IOException("Local STT failed: " + output);
            return output.trim();
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
