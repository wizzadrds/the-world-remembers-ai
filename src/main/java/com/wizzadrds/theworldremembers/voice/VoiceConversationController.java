package com.wizzadrds.theworldremembers.voice;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

public final class VoiceConversationController implements AutoCloseable {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "the-world-remembers-voice");
        thread.setDaemon(true);
        return thread;
    });

    private volatile VoiceConversationState state = VoiceConversationState.IDLE;
    private volatile Consumer<VoiceConversationState> stateListener = ignored -> {};
    private volatile long sessionGeneration;
    private volatile Future<?> activeTask;
    private volatile String lastError = "";

    public VoiceConversationState state() {
        return state;
    }

    public String lastError() {
        return lastError;
    }

    public void setStateListener(Consumer<VoiceConversationState> listener) {
        stateListener = Objects.requireNonNull(listener);
    }

    public synchronized void beginListening() {
        if (state == VoiceConversationState.IDLE || state == VoiceConversationState.ERROR) {
            sessionGeneration++;
            lastError = "";
            setState(VoiceConversationState.LISTENING);
        }
    }

    public void finishListening(byte[] pcm, VoiceService service, Consumer<String> transcriptConsumer) {
        if (pcm == null || pcm.length == 0 || service == null || transcriptConsumer == null) {
            fail("No audio was captured");
            return;
        }
        if (state != VoiceConversationState.LISTENING) return;
        setState(VoiceConversationState.PROCESSING);
        final long generation = sessionGeneration;
        try {
            activeTask = submitTracked(() -> {
                try {
                    if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
                    String transcript = service.transcribe(pcm);
                    if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
                    if (transcript == null || transcript.isBlank()) {
                        fail("Speech transcription returned no text");
                        return;
                    }
                    try {
                        if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
                        transcriptConsumer.accept(transcript);
                    } catch (RuntimeException e) {
                        if (generation == sessionGeneration && state == VoiceConversationState.PROCESSING) fail(messageOf(e));
                    }
                } catch (Exception e) {
                    if (generation == sessionGeneration && state == VoiceConversationState.PROCESSING) fail(messageOf(e));
                }
            });
        } catch (RejectedExecutionException e) {
            if (generation == sessionGeneration && state == VoiceConversationState.PROCESSING) fail(messageOf(e));
        }
    }

    public void synthesizeAndSpeak(
            String text,
            VoiceService service,
            VoiceProfile profile,
            Path output,
            VoiceAudioPlayer player,
            float outputVolume,
            Consumer<Path> completed) {
        setState(VoiceConversationState.PROCESSING);
        final long generation = sessionGeneration;
        try {
            activeTask = submitTracked(() -> {
                Path audio = null;
                try {
                    if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;

                    InputStream streamedAudio = service.synthesizeStream(text, profile);
                    if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) {
                        closeQuietly(streamedAudio);
                        return;
                    }
                    if (streamedAudio != null) {
                        setState(VoiceConversationState.SPEAKING);
                        player.playPcmStream(streamedAudio, outputVolume);
                        completed.accept(null);
                        finishSpeaking();
                        return;
                    }

                    audio = service.synthesize(text, profile, output);
                    if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
                    if (audio == null || !Files.exists(audio)) {
                        fail("TTS did not produce an audio file");
                        return;
                    }
                    setState(VoiceConversationState.SPEAKING);
                    if (generation != sessionGeneration) return;
                    player.play(audio, outputVolume);
                    completed.accept(audio);
                    finishSpeaking();
                } catch (Exception e) {
                    if (generation == sessionGeneration) fail(messageOf(e));
                } finally {
                    if (audio != null) {
                        try {
                            Files.deleteIfExists(audio);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            if (generation == sessionGeneration && state == VoiceConversationState.PROCESSING) fail();
        }
    }

    public void finishSpeaking() {
        setState(VoiceConversationState.IDLE);
    }

    public void fail() {
        fail("Check voice settings");
    }

    public void fail(String message) {
        lastError = message == null || message.isBlank() ? "Check voice settings" : message;
        setState(VoiceConversationState.ERROR);
    }

    private static String messageOf(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return error == null ? "Voice processing failed" : error.getClass().getSimpleName();
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) return;
        try {
            stream.close();
        } catch (Exception ignored) {
        }
    }

    public synchronized void reset() {
        sessionGeneration++;
        cancelActiveTask();
        setState(VoiceConversationState.IDLE);
    }

    private void setState(VoiceConversationState next) {
        state = next;
        try {
            stateListener.accept(next);
        } catch (RuntimeException ignored) {
            // UI/state observers must never break the voice worker or leave it wedged.
        }
    }

    private synchronized Future<?> submitTracked(Runnable task) {
        cancelActiveTask();
        FutureTask<Void> future = new FutureTask<>(task, null);
        activeTask = future;
        try {
            worker.execute(future);
            return future;
        } catch (RuntimeException e) {
            if (activeTask == future) activeTask = null;
            throw e;
        }
    }

    private void cancelActiveTask() {
        Future<?> task = activeTask;
        activeTask = null;
        if (task != null) task.cancel(true);
    }

    @Override
    public void close() {
        sessionGeneration++;
        cancelActiveTask();
        worker.shutdownNow();
    }
}
