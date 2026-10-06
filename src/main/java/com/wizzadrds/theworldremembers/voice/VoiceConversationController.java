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

    public VoiceConversationState state() {
        return state;
    }

    public void setStateListener(Consumer<VoiceConversationState> listener) {
        stateListener = Objects.requireNonNull(listener);
    }

    public void beginListening() {
        if (state == VoiceConversationState.IDLE || state == VoiceConversationState.ERROR) {
            setState(VoiceConversationState.LISTENING);
        }
    }

    public void finishListening(byte[] pcm, VoiceService service, Consumer<String> transcriptConsumer) {
        if (state != VoiceConversationState.LISTENING) return;
        setState(VoiceConversationState.PROCESSING);
        worker.submit(() -> {
            String transcript = service.transcribe(pcm);
            if (transcript == null || transcript.isBlank()) {
                fail();
                return;
            }
            try {
                transcriptConsumer.accept(transcript);
            } catch (RuntimeException e) {
                fail();
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
        worker.submit(() -> {
            Path audio = null;
            try {
                audio = service.synthesize(text, profile, output);
                if (audio == null || !Files.exists(audio)) {
                    fail();
                    return;
                }
                setState(VoiceConversationState.SPEAKING);
                player.play(audio, outputVolume);
                completed.accept(audio);
                finishSpeaking();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail();
            } catch (Exception e) {
                fail();
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

    public void reset() {
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
