package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.FloatControl;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public final class VoiceAudioPlayer {
    private volatile SourceDataLine line;
    private volatile String outputDevice = AudioDeviceManager.DEFAULT_DEVICE;
    private final AtomicLong playbackGeneration = new AtomicLong();

    public void setOutputDevice(String device) {
        outputDevice = device == null || device.isBlank() ? AudioDeviceManager.DEFAULT_DEVICE : device;
        stop();
    }

    public void play(Path audioFile, float volume) throws Exception {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile.toFile())) {
            playStream(stream, stream.getFormat(), volume);
        }
    }

    public void playPcmStream(InputStream stream, float volume) throws Exception {
        if (stream == null) throw new IllegalArgumentException("Audio stream is null");
        AudioFormat format = new AudioFormat(24000.0f, 16, 1, true, false);
        try (InputStream input = stream) {
            playStream(input, format, volume);
        }
    }

    /**
     * Plays TTS through a deterministic DSP pass that approximates the classic Minecraft
     * villager voice: slightly raised pitch, nasal midrange, compressed dynamics and clipped
     * consonants. This is intentionally a post-process, so the AI only supplies the words.
     */
    public void playVillagerPcmStream(InputStream stream, float volume) throws Exception {
        if (stream == null) throw new IllegalArgumentException("Villager audio stream is null");
        byte[] pcm;
        try (InputStream input = stream) {
            pcm = input.readAllBytes();
        }
        if (pcm.length == 0) throw new IllegalStateException("Villager TTS returned no audio");
        pcm = modulateVillager(pcm);
        // Reinterpreting the same samples at a slightly higher playback rate gives the
        // characteristic short, raised villager delivery without changing the TTS text.
        AudioFormat format = new AudioFormat(27000.0f, 16, 1, true, false);
        playStream(new java.io.ByteArrayInputStream(pcm), format, volume);
    }

    private static byte[] modulateVillager(byte[] pcm) {
        int sampleCount = pcm.length / 2;
        byte[] result = new byte[sampleCount * 2];
        double low = 0.0;
        double high = 0.0;
        double previous = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            int lo = pcm[i * 2] & 0xFF;
            int hi = pcm[i * 2 + 1];
            short raw = (short) ((hi << 8) | lo);
            double x = raw / 32768.0;

            // Mild band shaping: remove rumble, keep the nasal speech band, soften the top.
            low += 0.18 * (x - low);
            high = x - low;
            double nasal = low + high * 1.18;
            nasal = Math.max(-1.0, Math.min(1.0, nasal * 1.35));

            // Gentle saturation/compression gives the compact, buzzy NPC character.
            double compressed = Math.tanh(nasal * 1.7) / Math.tanh(1.7);
            double attack = 0.985 * previous + 0.015 * compressed;
            previous = attack;

            // Very small periodic throat modulation; intentionally subtle so Spanish remains intelligible.
            double tremolo = 0.985 + 0.015 * Math.sin(i * 2.0 * Math.PI * 7.0 / 24000.0);
            int sample = (int) Math.round(Math.max(-1.0, Math.min(1.0, attack * tremolo)) * 32767.0);
            result[i * 2] = (byte) sample;
            result[i * 2 + 1] = (byte) (sample >> 8);
        }
        return result;
    }

    private void playStream(InputStream stream, AudioFormat format, float volume) throws Exception {
        SourceDataLine output = null;
        long generation = playbackGeneration.incrementAndGet();
        closeCurrentLine();
        try {
            if (!outputDevice.equalsIgnoreCase(AudioDeviceManager.DEFAULT_DEVICE) && !AudioDeviceManager.outputAvailable(outputDevice)) {
                throw new IllegalStateException("Selected output device is unavailable: " + outputDevice);
            }
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            var mixer = AudioDeviceManager.findOutputMixer(outputDevice, format);
            output = (SourceDataLine) (mixer == null ? AudioSystem.getLine(info) : mixer.getLine(info));
            output.open(format);
            if (output.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) output.getControl(FloatControl.Type.MASTER_GAIN);
                float linear = Math.max(0.001f, Math.min(1.0f, volume));
                float db = 20.0f * (float) Math.log10(linear);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
            output.start();
            if (generation != playbackGeneration.get()) {
                output.close();
                return;
            }
            line = output;
            byte[] buffer = new byte[8192];
            int read;
            while (generation == playbackGeneration.get() && line == output && (read = stream.read(buffer, 0, buffer.length)) >= 0) {
                if (read == 0) continue;
                // L16 is a 16-bit format: never hand a partial sample to the mixer.
                if ((read & 1) != 0) {
                    int next = stream.read(buffer, read, 1);
                    if (next <= 0) break;
                    read++;
                }
                output.write(buffer, 0, read);
            }
            if (generation == playbackGeneration.get() && line == output) output.drain();
        } finally {
            if (line == output) closeCurrentLine();
        }
    }

    public void stop() {
        playbackGeneration.incrementAndGet();
        closeCurrentLine();
    }

    private void closeCurrentLine() {
        SourceDataLine current = line;
        line = null;
        if (current != null) {
            current.stop();
            current.flush();
            current.close();
        }
    }

    public boolean isPlaying() {
        return line != null;
    }
}
