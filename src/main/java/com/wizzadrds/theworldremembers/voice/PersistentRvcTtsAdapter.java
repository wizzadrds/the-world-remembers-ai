package com.wizzadrds.theworldremembers.voice;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Keeps one VillagerTITAN RVC process alive so the checkpoint is loaded once per
 * Minecraft client instead of once per spoken reply.
 */
public final class PersistentRvcTtsAdapter implements TtsAdapter {
    private static final long START_TIMEOUT_SECONDS = 180;
    private static final long REQUEST_TIMEOUT_SECONDS = 180;

    private final List<String> command;
    private final Object lock = new Object();
    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;

    public PersistentRvcTtsAdapter(List<String> command) {
        if (command == null || command.size() < 2) {
            throw new IllegalArgumentException("RVC TTS command must contain python and script");
        }
        this.command = List.copyOf(command);
    }

    @Override
    public Path synthesize(String text, VoiceProfile profile, Path output) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("TTS text is empty");
        if (profile == null) throw new IllegalArgumentException("TTS voice profile is null");
        if (output == null) throw new IllegalArgumentException("TTS output path is null");

        synchronized (lock) {
            ensureStarted();
            Path target = output.toAbsolutePath();
            Files.createDirectories(target.getParent());
            Files.deleteIfExists(target);

            String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            String request = "{\"text\":\"" + encoded + "\",\"output\":\"" +
                    jsonEscape(target.toString()) + "\",\"rate\":" + profile.rate() +
                    ",\"pitch\":" + profile.pitch() + "}";
            stdin.write(request);
            stdin.newLine();
            stdin.flush();

            String response = stdout.readLine();
            if (response == null) {
                throw new IOException("VillagerTITAN worker exited unexpectedly");
            }
            if (!response.contains("\"ok\": true")) {
                throw new IOException("VillagerTITAN worker failed: " + response);
            }
            if (!Files.isRegularFile(target) || Files.size(target) == 0) {
                throw new IOException("VillagerTITAN worker produced no audio");
            }
            return target;
        }
    }

    private void ensureStarted() throws IOException, InterruptedException {
        if (process != null && process.isAlive()) return;

        closeQuietly();
        List<String> workerCommand = new ArrayList<>(command.subList(0, 2));
        workerCommand.add("--worker");

        ProcessBuilder builder = new ProcessBuilder(workerCommand);
        builder.redirectErrorStream(false);
        process = builder.start();
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        Thread err = Thread.ofVirtual().name("twr-rvc-worker-log").start(() -> {
            try (InputStream input = process.getErrorStream()) {
                input.transferTo(System.err);
            } catch (IOException ignored) {
            }
        });

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(START_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IOException("VillagerTITAN worker exited with code " + process.exitValue());
            }
            if (process.getErrorStream().available() >= 0 && stdout.ready()) {
                // stdout is reserved for JSON responses; readiness here is not a
                // reliable startup signal, so wait briefly for the worker marker.
            }
            Thread.sleep(100);
            if (process.isAlive() && Files.exists(Path.of(System.getProperty("java.io.tmpdir")))) {
                // The worker prints its ready marker on stderr. Its process being alive
                // after model initialization is sufficient; the first request is still
                // guarded by the request timeout in the Python worker.
                if (System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100) >= deadline) break;
                if (process.isAlive() && process.pid() > 0) {
                    // leave the actual readiness wait to the first request
                    break;
                }
            }
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void closeQuietly() {
        if (stdin != null) try { stdin.close(); } catch (IOException ignored) {}
        if (process != null && process.isAlive()) process.destroy();
        stdin = null;
        stdout = null;
        process = null;
    }

    public void stop() {
        synchronized (lock) {
            closeQuietly();
        }
    }
}
