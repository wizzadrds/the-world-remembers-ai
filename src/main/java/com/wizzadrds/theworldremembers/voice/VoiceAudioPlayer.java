package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.FloatControl;
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
        long generation = playbackGeneration.incrementAndGet();
        closeCurrentLine();
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile.toFile())) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
            var mixer = AudioDeviceManager.findOutputMixer(outputDevice, stream.getFormat());
            SourceDataLine output = (SourceDataLine) (mixer == null
                    ? AudioSystem.getLine(info)
                    : mixer.getLine(info));
            output.open(stream.getFormat());
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
                if (read > 0) output.write(buffer, 0, read);
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
