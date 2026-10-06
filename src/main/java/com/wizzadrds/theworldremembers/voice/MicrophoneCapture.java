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

public final class MicrophoneCapture implements AutoCloseable {
    public static final float SAMPLE_RATE = 16000.0f;
    private static final int CHANNELS = 1;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, true, false);

    private volatile TargetDataLine line;
    private volatile Thread captureThread;
    private volatile ByteArrayOutputStream buffer;
    private volatile float level;

    public static List<String> devices() {
        List<String> result = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            Mixer mixer = AudioSystem.getMixer(info);
            DataLine.Info target = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (mixer.isLineSupported(target)) result.add(info.getName());
        }
        return result;
    }

    public synchronized boolean start(String deviceName) {
        if (line != null) return true;
        try {
            Mixer mixer = findMixer(deviceName);
            TargetDataLine target = mixer == null
                    ? AudioSystem.getTargetDataLine(FORMAT)
                    : (TargetDataLine) mixer.getLine(new DataLine.Info(TargetDataLine.class, FORMAT));
            target.open(FORMAT, 3200);
            buffer = new ByteArrayOutputStream(32000);
            line = target;
            target.start();
            captureThread = Thread.ofVirtual().name("twr-microphone").start(() -> capture(target));
            return true;
        } catch (LineUnavailableException | RuntimeException e) {
            close();
            return false;
        }
    }

    private void capture(TargetDataLine target) {
        byte[] chunk = new byte[1600];
        try {
            while (line == target) {
                int read = target.read(chunk, 0, chunk.length);
                if (read > 0) {
                    buffer.write(chunk, 0, read);
                    level = calculateLevel(chunk, read);
                }
            }
        } finally {
            level = 0.0f;
        }
    }

    public synchronized byte[] stop() {
        TargetDataLine target = line;
        line = null;
        if (target == null) return new byte[0];
        target.stop();
        target.flush();
        target.close();
        Thread thread = captureThread;
        captureThread = null;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] result = buffer == null ? new byte[0] : buffer.toByteArray();
        buffer = null;
        return result;
    }

    public float level() {
        return level;
    }

    public boolean isCapturing() {
        return line != null;
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

    private static Mixer findMixer(String requested) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase("Default")) return null;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equalsIgnoreCase(requested)) return AudioSystem.getMixer(info);
        }
        return null;
    }

    @Override
    public synchronized void close() {
        stop();
    }
}
