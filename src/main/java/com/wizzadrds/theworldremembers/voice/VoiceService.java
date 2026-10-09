package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public final class VoiceService {
    private final SttAdapter stt;
    private final TtsAdapter tts;

    public VoiceService(SttAdapter stt, TtsAdapter tts) {
        this.stt = stt;
        this.tts = tts;
    }

    /**
     * Keep transcription failures visible to VoiceConversationController.
     * Returning an empty string here hid HTTP/API errors and made them look
     * like a successful transcription that happened to contain no text.
     */
    public String transcribe(byte[] pcm) throws IOException, InterruptedException {
        return stt.transcribe(pcm);
    }

    public Path synthesize(String text, VoiceProfile profile, Path output) {
        try {
            return tts.synthesize(text, profile, output);
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public InputStream synthesizeStream(String text, VoiceProfile profile) {
        try {
            return tts.synthesizeStream(text, profile);
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
