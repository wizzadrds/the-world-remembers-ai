package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MicrophoneCapture implements AutoCloseable {
    public static final float SAMPLE_RATE = 16000.0f;
    public static final String DEFAULT_DEVICE = AudioDeviceManager.DEFAULT_DEVICE;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final int MAX_CAPTURE_BYTES = (int) (SAMPLE_RATE * 2 * 120); // 120 seconds at 16-bit mono
    private volatile TargetDataLine line;
    private volatile Thread captureThread;
    private volatile ByteArrayOutputStream buffer;
    private volatile float level;
    private volatile float inputVolume = 1.0f;
    private volatile Consumer<byte[]> frameListener = ignored -> {};

    public static List<String> devices() {
        return AudioDeviceManager.inputDevices();
    }

    public static String detectDefaultDevice() {
        return DEFAULT_DEVICE;
    }

    public synchronized boolean start(String deviceName) {
        return start(deviceName, 1.0f, ignored -> {});
    }

    public synchronized boolean start(String deviceName, float volume, Consumer<byte[]> listener) {
        if (line != null) return true;
        inputVolume = Math.max(0.0f, Math.min(2.0f, volume));
        frameListener = listener == null ? ignored -> {} : listener;
        try {
            String requested = deviceName == null || deviceName.isBlank() ? DEFAULT_DEVICE : deviceName;
            if (!requested.equalsIgnoreCase(DEFAULT_DEVICE) && !AudioDeviceManager.inputAvailable(requested)) return false;
            Mixer mixer = AudioDeviceManager.findInputMixer(requested);
            TargetDataLine target = mixer == null ? AudioSystem.getTargetDataLine(FORMAT)
                    : (TargetDataLine) mixer.getLine(new DataLine.Info(TargetDataLine.class, FORMAT));
            target.open(FORMAT, 3200);
            ByteArrayOutputStream captureBuffer = new ByteArrayOutputStream(32000);
            buffer = captureBuffer;
            line = target;
            target.start();
            captureThread = Thread.ofVirtual().name("twr-microphone").start(() -> capture(target, captureBuffer, frameListener));
            return true;
        } catch (LineUnavailableException | RuntimeException e) {
            close();
            return false;
        }
    }

    private void capture(TargetDataLine target, ByteArrayOutputStream captureBuffer, Consumer<byte[]> listener) {
        byte[] chunk = new byte[640];
        try {
            while (line == target) {
                int read = target.read(chunk, 0, chunk.length);
                if (read > 0) {
                    applyGain(chunk, read, inputVolume);
                    if (captureBuffer.size() < MAX_CAPTURE_BYTES) {
                        int accepted = Math.min(read, MAX_CAPTURE_BYTES - captureBuffer.size());
                        captureBuffer.write(chunk, 0, accepted);
                    }
                    level = calculateLevel(chunk, read);
                    try {
                        listener.accept(java.util.Arrays.copyOf(chunk, read));
                    } catch (RuntimeException ignored) {
                        // A network/audio consumer must never kill microphone capture.
                    }
                }
            }
        } finally {
            level = 0.0f;
        }
    }

    public synchronized byte[] stop() {
        TargetDataLine target = line;
        line = null;
        frameListener = ignored -> {};
        if (target == null) return new byte[0];
        target.stop();
        target.flush();
        target.close();
        Thread thread = captureThread;
        captureThread = null;
        if (thread != null && thread != Thread.currentThread()) {
            try { thread.join(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        byte[] result = buffer == null ? new byte[0] : buffer.toByteArray();
        buffer = null;
        return result;
    }

    public float level() { return level; }
    public boolean isCapturing() { return line != null; }

    private static void applyGain(byte[] pcm, int length, float gain) {
        if (gain == 1.0f) return;
        for (int i = 0; i + 1 < length; i += 2) {
            short sample = (short) (((pcm[i + 1] & 0xFF) << 8) | (pcm[i] & 0xFF));
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
            pcm[i] = (byte) scaled;
            pcm[i + 1] = (byte) (scaled >> 8);
        }
    }

    private static float calculateLevel(byte[] pcm, int length) {
        long sum = 0;
        int samples = length / 2;
        for (int i = 0; i < samples; i++) {
            int lo = pcm[i * 2] & 0xFF;
            int hi = pcm[i * 2 + 1];
            short sample = (short) ((hi << 8) | lo);
            sum += (long) sample * sample;
        }
        return samples == 0 ? 0.0f : Math.min(1.0f, (float) Math.sqrt((double) sum / samples) / 32768.0f);
    }

    @Override public synchronized void close() { stop(); }
}
