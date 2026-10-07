package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AudioDeviceManager {
    public static final String DEFAULT_DEVICE = "Default";
    private static final AudioFormat VOICE_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);
    private static final long CACHE_MILLIS = 2000L;
    private static volatile List<String> cachedInputs = List.of(DEFAULT_DEVICE);
    private static volatile List<String> cachedOutputs = List.of(DEFAULT_DEVICE);
    private static volatile long cacheTimeMillis;
    private static volatile boolean scanRunning;
    private static final List<Runnable> DEVICE_SCAN_CALLBACKS = new CopyOnWriteArrayList<>();
    private static final java.util.concurrent.ExecutorService DEVICE_SCAN_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "twr-audio-device-scan");
                thread.setDaemon(true);
                return thread;
            });

    private AudioDeviceManager() {}

    public static List<String> inputDevices() {
        return cachedInputs;
    }

    public static List<String> outputDevices() {
        return cachedOutputs;
    }

    /** Synchronous scan retained for tests/background callers; never call this from the render thread. */
    public static synchronized void refreshDevices() {
        cachedInputs = devices(TargetDataLine.class);
        cachedOutputs = devices(SourceDataLine.class);
        cacheTimeMillis = System.currentTimeMillis();
    }

    /** Starts a Java Sound scan off the Minecraft/render thread. */
    public static void refreshDevicesAsync(Runnable onComplete) {
        if (onComplete != null) DEVICE_SCAN_CALLBACKS.add(onComplete);
        synchronized (AudioDeviceManager.class) {
            if (scanRunning) return;
            scanRunning = true;
        }
        DEVICE_SCAN_EXECUTOR.execute(() -> {
            try {
                refreshDevices();
            } catch (Throwable ignored) {
                // Audio drivers are optional and must never terminate Minecraft.
            } finally {
                scanRunning = false;
                List<Runnable> callbacks = new java.util.ArrayList<>(DEVICE_SCAN_CALLBACKS);
                DEVICE_SCAN_CALLBACKS.removeAll(callbacks);
                for (Runnable callback : callbacks) {
                    try { callback.run(); } catch (Throwable ignored) {}
                }
            }
        });
    }
    private static void refreshIfStale() {
        if (System.currentTimeMillis() - cacheTimeMillis <= CACHE_MILLIS) return;
        refreshDevicesAsync(null);
    }

    private static List<String> devices(Class<? extends DataLine> type) {
        Set<String> result = new LinkedHashSet<>();
        result.add(DEFAULT_DEVICE);
        Mixer.Info[] infos;
        try {
            infos = AudioSystem.getMixerInfo();
        } catch (Throwable ignored) {
            return List.copyOf(result);
        }
        for (Mixer.Info info : infos) {
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
        Mixer.Info[] infos;
        try {
            infos = AudioSystem.getMixerInfo();
        } catch (Throwable ignored) {
            return null;
        }
        for (Mixer.Info info : infos) {
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
        AudioFormat format = VOICE_FORMAT;
        SourceDataLine line = null;
        try {
            Mixer mixer = findMixer(requested, SourceDataLine.class, format);
            if (!requested.equalsIgnoreCase(DEFAULT_DEVICE) && mixer == null) return false;
            line = (SourceDataLine) (mixer == null
                    ? AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format))
                    : mixer.getLine(new DataLine.Info(SourceDataLine.class, format)));
            line.open(format, 4096);
            line.start();
            int samples = 16000 / 5;
            byte[] pcm = new byte[samples * 2];
            double gain = Math.max(0.0, Math.min(1.0, volume));
            for (int i = 0; i < samples; i++) {
                double envelope = Math.min(1.0, i / 400.0) * Math.min(1.0, (samples - i) / 400.0);
                short sample = (short) (Math.sin(2.0 * Math.PI * 440.0 * i / 16000.0) * 12000.0 * gain * envelope);
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
        if (requested == null || requested.isBlank()) return false;
        String normalized = requested.trim();
        if (info.getName().equalsIgnoreCase(normalized)) return true;
        String description = info.getDescription();
        return description != null && !description.isBlank()
                && (info.getName() + " — " + description).equalsIgnoreCase(normalized);
    }

    public static String describeAvailability(String requested, boolean input) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase(DEFAULT_DEVICE)) {
            return DEFAULT_DEVICE + " (system default)";
        }
        return (input ? inputAvailable(requested) : outputAvailable(requested))
                ? requested + " (available)"
                : requested + " (unavailable)";
    }

    public static boolean inputAvailable(String requested) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase(DEFAULT_DEVICE)) return true;
        refreshIfStale();
        return containsIgnoreCase(cachedInputs, requested);
    }

    public static boolean outputAvailable(String requested) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase(DEFAULT_DEVICE)) return true;
        refreshIfStale();
        return containsIgnoreCase(cachedOutputs, requested);
    }

    private static boolean containsIgnoreCase(List<String> values, String requested) {
        for (String value : values) if (requested.equalsIgnoreCase(value)) return true;
        return false;
    }
}
