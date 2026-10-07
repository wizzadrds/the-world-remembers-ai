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

    public String transcribe(byte[] pcm) {
        try {
            return stt.transcribe(pcm);
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
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
