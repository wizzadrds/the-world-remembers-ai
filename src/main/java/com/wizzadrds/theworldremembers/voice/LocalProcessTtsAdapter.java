package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LocalProcessTtsAdapter implements TtsAdapter {
    private final List<String> command;
    private final String instructions;

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
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        boolean templated = command.stream().anyMatch(LocalProcessTtsAdapter::containsPlaceholder);
        List<String> args = new ArrayList<>();
        if (templated) {
            for (String token : command) {
                args.add(token
                        .replace("{text}", text)
                        .replace("{output}", output.toAbsolutePath().toString())
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
            args.add(output.toAbsolutePath().toString());
            args.add(profile.language());
            args.add(profile.modelId());
            args.add(Float.toString(profile.rate()));
            args.add(Float.toString(profile.pitch()));
            args.add(Float.toString(profile.expressiveness()));
        }

        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        String log = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IOException("Local TTS failed: " + log);
        if (!Files.isRegularFile(output)) throw new IOException("TTS produced no output: " + log);
        return output;
    }

    private static boolean containsPlaceholder(String value) {
        return value.contains("{text}") || value.contains("{output}") || value.contains("{language}")
                || value.contains("{model}") || value.contains("{voice}") || value.contains("{rate}")
                || value.contains("{pitch}") || value.contains("{expressiveness}") || value.contains("{temperament}")
                || value.contains("{instructions}");
    }
}