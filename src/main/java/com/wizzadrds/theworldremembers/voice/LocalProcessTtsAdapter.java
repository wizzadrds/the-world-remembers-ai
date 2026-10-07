package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LocalProcessTtsAdapter implements TtsAdapter {
    private final List<String> command;
    private final String instructions;
    private static final long PROCESS_TIMEOUT_SECONDS = 45;

    public LocalProcessTtsAdapter(List<String> command) {
        this(command, "");
    }

    public LocalProcessTtsAdapter(List<String> command, String instructions) {
        if (command == null || command.isEmpty()) throw new IllegalArgumentException("TTS command is empty");
        this.command = List.copyOf(command);
        this.instructions = instructions == null ? "" : instructions;
    }

    @Override
    public Path synthesize(String text, VoiceProfile profile, Path output) throws IOException, InterruptedException {
        if (output == null) throw new IllegalArgumentException("TTS output path is null");
        Path target = output.toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        // Never accept audio left by a previous failed/aborted synthesis.
        Files.deleteIfExists(target);

        boolean templated = command.stream().anyMatch(LocalProcessTtsAdapter::containsPlaceholder);
        List<String> args = new ArrayList<>();
        if (templated) {
            for (String token : command) {
                args.add(token
                        .replace("{text}", text)
                        .replace("{output}", target.toString())
                        .replace("{language}", profile.language())
                        .replace("{model}", profile.modelId())
                        .replace("{voice}", profile.modelId())
                        .replace("{rate}", Float.toString(profile.rate()))
                        .replace("{pitch}", Float.toString(profile.pitch()))
                        .replace("{expressiveness}", Float.toString(profile.expressiveness()))
                        .replace("{temperament}", profile.temperament().name())
                        .replace("{instructions}", instructions));
            }
        } else {
            args.addAll(command);
            args.add(text);
            args.add(target.toString());
            args.add(profile.language());
            args.add(profile.modelId());
            args.add(Float.toString(profile.rate()));
            args.add(Float.toString(profile.pitch()));
            args.add(Float.toString(profile.expressiveness()));
        }

        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        Thread logReader = Thread.ofVirtual().name("twr-tts-log").start(() -> {
            try {
                process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            } catch (IOException ignored) {
            }
        });
        try {
            if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
                throw new IOException("Local TTS timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
            }
            if (process.exitValue() != 0) {
                throw new IOException("Local TTS failed with exit code " + process.exitValue());
            }
            if (!Files.isRegularFile(output) || Files.size(output) == 0) {
                throw new IOException("TTS produced no audio output");
            }
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            try {
                logReader.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean containsPlaceholder(String value) {
        return value.contains("{text}") || value.contains("{output}") || value.contains("{language}")
                || value.contains("{model}") || value.contains("{voice}") || value.contains("{rate}")
                || value.contains("{pitch}") || value.contains("{expressiveness}") || value.contains("{temperament}")
                || value.contains("{instructions}");
    }
}