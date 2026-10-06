package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MicrophoneCapture {
    public static final String DEFAULT_DEVICE = "System default";

    private MicrophoneCapture() {}

    public static List<String> devices() {
        List<String> result = new ArrayList<>();
        result.add(DEFAULT_DEVICE);
        try {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.getTargetLineInfo().length > 0) result.add(info.getName());
                mixer.close();
            }
        } catch (RuntimeException ignored) {
            // Audio enumeration is optional and may fail in a headless/CI environment.
        }
        return result.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public static String detectDefaultDevice() {
        try {
            Mixer.Info[] infos = AudioSystem.getMixerInfo();
            for (Mixer.Info info : infos) {
                Mixer mixer = AudioSystem.getMixer(info);
                boolean target = mixer.getTargetLineInfo().length > 0;
                mixer.close();
                if (target) return info.getName();
            }
        } catch (RuntimeException ignored) {
        }
        return DEFAULT_DEVICE;
    }
}
