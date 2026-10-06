package com.wizzadrds.theworldremembers.voice;

import net.minecraft.client.Minecraft;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class VoiceAudioPlayer {
    private volatile SourceDataLine line;

    public static List<String> devices() {
        List<String> result = new ArrayList<>();
        result.add("Default");
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(new DataLine.Info(SourceDataLine.class, null))) result.add(info.getName());
        }
        return List.copyOf(result);
    }

    public void play(Path audioFile, float volume) throws Exception {
        String device = VoiceClientConfig.load(Minecraft.getInstance().gameDirectory.toPath()).outputDevice;
        play(audioFile, volume, device);
    }

    public void play(Path audioFile, float volume, String deviceName) throws Exception {
        stop();
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile.toFile())) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
            Mixer mixer = findMixer(deviceName, info);
            SourceDataLine output = mixer == null
                    ? (SourceDataLine) AudioSystem.getLine(info)
                    : (SourceDataLine) mixer.getLine(info);
            output.open(stream.getFormat());
            if (output.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) output.getControl(FloatControl.Type.MASTER_GAIN);
                float linear = Math.max(0.001f, Math.min(1.0f, volume));
                float db = 20.0f * (float) Math.log10(linear);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
            output.start();
            line = output;
            byte[] buffer = new byte[8192];
            int read;
            while (line == output && (read = stream.read(buffer, 0, buffer.length)) >= 0) {
                if (read > 0) output.write(buffer, 0, read);
            }
            if (line == output) output.drain();
        } finally {
            stop();
        }
    }

    private static Mixer findMixer(String name, DataLine.Info info) {
        if (name == null || name.isBlank() || name.equalsIgnoreCase("Default")) return null;
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (mixerInfo.getName().equalsIgnoreCase(name)) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) return mixer;
            }
        }
        return null;
    }

    public void stop() {
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
