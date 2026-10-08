package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public final class LocalProcessSttAdapter implements SttAdapter {
    private static final int MAX_PCM_BYTES = 4 * 1024 * 1024;
    private static final long PROCESS_TIMEOUT_SECONDS = 45;
    private final List<String> command;

    public LocalProcessSttAdapter(List<String> command) {
        if (command == null || command.isEmpty()) throw new IllegalArgumentException("STT command is empty");
        this.command = List.copyOf(command);
    }

    @Override
    public String transcribe(byte[] pcm) throws IOException, InterruptedException {
        if (pcm == null || pcm.length == 0) throw new IllegalArgumentException("STT audio is empty");
        if (pcm.length > MAX_PCM_BYTES) throw new IOException("STT audio exceeds 4 MiB limit");
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
            var outputBuffer = new CappedOutputStream(64 * 1024);
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
                String output = outputBuffer.toString();
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

    private static final class CappedOutputStream extends OutputStream {
        private final int maxBytes;
        private final java.io.ByteArrayOutputStream delegate = new java.io.ByteArrayOutputStream();
        private boolean truncated;

        private CappedOutputStream(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public synchronized void write(int value) {
            if (delegate.size() < maxBytes) delegate.write(value);
            else truncated = true;
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            if (bytes == null) throw new NullPointerException("bytes");
            if (offset < 0 || length < 0 || offset > bytes.length - length) throw new IndexOutOfBoundsException();
            int remaining = maxBytes - delegate.size();
            if (remaining > 0) delegate.write(bytes, offset, Math.min(length, remaining));
            if (length > remaining) truncated = true;
        }

        @Override
        public synchronized String toString() {
            String text = delegate.toString(java.nio.charset.StandardCharsets.UTF_8);
            return truncated ? text + "\n[output truncated at 64 KiB]" : text;
        }
    }
}
