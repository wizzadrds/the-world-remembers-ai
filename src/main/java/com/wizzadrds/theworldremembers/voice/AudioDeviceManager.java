package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;

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
        List<String> result = new ArrayList<>();
        result.add(DEFAULT_DEVICE);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(new DataLine.Info(type, format))) {
                    result.add(info.getName());
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
            if (!info.getName().equalsIgnoreCase(requested)) continue;
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(new DataLine.Info(type, VOICE_FORMAT))) return mixer;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
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
