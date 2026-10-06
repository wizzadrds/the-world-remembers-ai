package com.wizzadrds.theworldremembers.voice;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.nio.file.Path;

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

    public void synthesizeAndSpeak(String text, VoiceService service, VoiceProfile profile, Path output, VoiceAudioPlayer player, Consumer<Path> completed) {
        setState(VoiceConversationState.PROCESSING);
        worker.submit(() -> {
            try {
                Path audio = service.synthesize(text, profile, output);
                if (audio == null) { fail(); return; }
                setState(VoiceConversationState.SPEAKING);
                player.play(audio, 1.0f);
                completed.accept(audio);
                finishSpeaking();
            } catch (Exception e) {
                fail();
            }
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
