package com.wizzadrds.theworldremembers.voice;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

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

            java.util.concurrent.ExecutorService readerExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "twr-rvc-response-reader");
                thread.setDaemon(true);
                return thread;
            });
            String response;
            try {
                Future<String> responseFuture = readerExecutor.submit(stdout::readLine);
                response = responseFuture.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException ex) {
                closeQuietly();
                throw new IOException("VillagerTITAN worker timed out after " + REQUEST_TIMEOUT_SECONDS + " seconds", ex);
            } catch (ExecutionException ex) {
                closeQuietly();
                Throwable cause = ex.getCause();
                if (cause instanceof IOException io) throw io;
                throw new IOException("VillagerTITAN worker response failed", cause);
            } finally {
                readerExecutor.shutdownNow();
            }
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
        if (process != null && process.isAlive() && stdout != null) return;

        closeQuietly();
        List<String> workerCommand = new ArrayList<>(command.subList(0, 2));
        workerCommand.add("--worker");

        ProcessBuilder builder = new ProcessBuilder(workerCommand);
        builder.redirectErrorStream(false);
        process = builder.start();
        Process startedProcess = process;
        stdin = new BufferedWriter(new OutputStreamWriter(startedProcess.getOutputStream(), StandardCharsets.UTF_8));
        stdout = new BufferedReader(new InputStreamReader(startedProcess.getInputStream(), StandardCharsets.UTF_8));

        Thread.ofVirtual().name("twr-rvc-worker-log").start(() -> {
            try (InputStream input = startedProcess.getErrorStream()) {
                input.transferTo(System.err);
            } catch (IOException ignored) {
            }
        });

        java.util.concurrent.ExecutorService startupReader = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "twr-rvc-startup-reader");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<String> readyFuture = startupReader.submit(stdout::readLine);
            String ready;
            try {
                ready = readyFuture.get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException ex) {
                closeQuietly();
                throw new IOException("VillagerTITAN worker did not become ready within "
                        + START_TIMEOUT_SECONDS + " seconds; check the Python/RVC log above", ex);
            } catch (ExecutionException ex) {
                closeQuietly();
                throw new IOException("Could not read VillagerTITAN worker startup response", ex.getCause());
            }
            if (ready == null) {
                int exitCode = startedProcess.isAlive() ? -1 : startedProcess.exitValue();
                closeQuietly();
                throw new IOException("VillagerTITAN worker exited during startup (exit code " + exitCode
                        + "); check the Python/RVC log above");
            }
            if (!ready.contains("\"ready\": true")) {
                closeQuietly();
                throw new IOException("Unexpected VillagerTITAN worker startup response: " + ready);
            }
        } finally {
            startupReader.shutdownNow();
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
