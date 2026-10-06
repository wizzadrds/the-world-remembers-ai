package com.wizzadrds.theworldremembers.voice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public VoiceConversationState state() {
        return state;
    }

    public void setStateListener(Consumer<VoiceConversationState> listener) {
        stateListener = Objects.requireNonNull(listener);
    }

    public synchronized void beginListening() {
        if (state == VoiceConversationState.IDLE || state == VoiceConversationState.ERROR) {
            sessionGeneration++;
            setState(VoiceConversationState.LISTENING);
        }
    }

    public void finishListening(byte[] pcm, VoiceService service, Consumer<String> transcriptConsumer) {
        if (state != VoiceConversationState.LISTENING) return;
        setState(VoiceConversationState.PROCESSING);
        final long generation = sessionGeneration;
        worker.submit(() -> {
            String transcript = service.transcribe(pcm);
            if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
            if (transcript == null || transcript.isBlank()) {
                fail();
                return;
            }
            try {
                if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
                transcriptConsumer.accept(transcript);
            } catch (RuntimeException e) {
                if (generation == sessionGeneration && state == VoiceConversationState.PROCESSING) {
                    fail();
                }
            }
        });
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
        worker.submit(() -> {
            Path audio = null;
            try {
                if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
                audio = service.synthesize(text, profile, output);
                if (generation != sessionGeneration || state != VoiceConversationState.PROCESSING) return;
                if (audio == null || !Files.exists(audio)) {
                    fail();
                    return;
                }
                setState(VoiceConversationState.SPEAKING);
                if (generation != sessionGeneration) return;
                player.play(audio, outputVolume);
                completed.accept(audio);
                finishSpeaking();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (generation == sessionGeneration) {
                    fail();
                }
            } catch (Exception e) {
                if (generation == sessionGeneration) {
                    fail();
                }
            } finally {
                if (audio != null) {
                    try {
                        Files.deleteIfExists(audio);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    public void finishSpeaking() {
        setState(VoiceConversationState.IDLE);
    }

    public void fail() {
        setState(VoiceConversationState.ERROR);
    }

    public synchronized void reset() {
        sessionGeneration++;
        setState(VoiceConversationState.IDLE);
    }

    private void setState(VoiceConversationState next) {
        state = next;
        stateListener.accept(next);
    }

    @Override
    public void close() {
        worker.shutdownNow();
    }
}
