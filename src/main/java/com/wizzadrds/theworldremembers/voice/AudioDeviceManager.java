package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AudioDeviceManager {
    public static final String DEFAULT_DEVICE = "Default";
    private static final AudioFormat VOICE_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);

    private AudioDeviceManager() {}

    public static List<String> inputDevices() {
        return devices(TargetDataLine.class);
    }

    public static List<String> outputDevices() {
        return devices(SourceDataLine.class);
    }

    private static List<String> devices(Class<? extends DataLine> type) {
        Set<String> result = new LinkedHashSet<>();
        result.add(DEFAULT_DEVICE);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(new DataLine.Info(type, VOICE_FORMAT))) {
                    String label = info.getName();
                    if (result.contains(label)) {
                        String description = info.getDescription();
                        if (description != null && !description.isBlank()) label = label + " — " + description;
                    }
                    result.add(label);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(result);
    }

    public static Mixer findInputMixer(String requested) {
        return findMixer(requested, TargetDataLine.class);
    }

    public static Mixer findOutputMixer(String requested) {
        return findMixer(requested, SourceDataLine.class, VOICE_FORMAT);
    }

    public static Mixer findOutputMixer(String requested, AudioFormat format) {
        return findMixer(requested, SourceDataLine.class, format);
    }

    private static Mixer findMixer(String requested, Class<? extends DataLine> type) {
        return findMixer(requested, type, VOICE_FORMAT);
    }

    private static Mixer findMixer(String requested, Class<? extends DataLine> type, AudioFormat format) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase(DEFAULT_DEVICE)) return null;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (!matchesRequested(info, requested)) continue;
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(new DataLine.Info(type, format))) return mixer;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    public static boolean playTestTone(String requested, float volume) {
        AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
        SourceDataLine line = null;
        try {
            Mixer mixer = findMixer(requested, SourceDataLine.class, format);
            line = (SourceDataLine) (mixer == null
                    ? AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format))
                    : mixer.getLine(new DataLine.Info(SourceDataLine.class, format)));
            line.open(format, 4096);
            line.start();
            int samples = 44100 / 5;
            byte[] pcm = new byte[samples * 2];
            double gain = Math.max(0.0, Math.min(1.0, volume));
            for (int i = 0; i < samples; i++) {
                double envelope = Math.min(1.0, i / 400.0) * Math.min(1.0, (samples - i) / 400.0);
                short sample = (short) (Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0) * 12000.0 * gain * envelope);
                pcm[i * 2] = (byte) sample;
                pcm[i * 2 + 1] = (byte) (sample >> 8);
            }
            line.write(pcm, 0, pcm.length);
            line.drain();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }

    private static boolean matchesRequested(Mixer.Info info, String requested) {
        if (info.getName().equalsIgnoreCase(requested)) return true;
        String description = info.getDescription();
        return description != null && !description.isBlank()
                && (info.getName() + " — " + description).equalsIgnoreCase(requested);
    }

    public static boolean inputAvailable(String requested) {
        return requested == null || requested.isBlank() || requested.equalsIgnoreCase(DEFAULT_DEVICE)
                || findInputMixer(requested) != null;
    }

    public static boolean outputAvailable(String requested) {
        return requested == null || requested.isBlank() || requested.equalsIgnoreCase(DEFAULT_DEVICE)
                || findOutputMixer(requested) != null;
    }
}
