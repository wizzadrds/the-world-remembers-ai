package com.wizzadrds.theworldremembers.voice;

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
        setState(VoiceConversationState.LISTENING);
    }

    public void finishListening(byte[] pcm, VoiceService service, Consumer<String> transcriptConsumer) {
        if (state != VoiceConversationState.LISTENING) return;
        setState(VoiceConversationState.PROCESSING);
        worker.submit(() -> {
            String transcript = service.transcribe(pcm);
            if (transcript == null || transcript.isBlank()) {
                setState(VoiceConversationState.ERROR);
                return;
            }
            transcriptConsumer.accept(transcript);
        });
    }

    public void beginSpeaking() {
        setState(VoiceConversationState.SPEAKING);
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
