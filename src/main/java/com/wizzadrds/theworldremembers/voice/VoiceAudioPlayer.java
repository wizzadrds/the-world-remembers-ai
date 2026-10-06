package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.FloatControl;
import java.nio.file.Path;

public final class VoiceAudioPlayer {
    private volatile SourceDataLine line;

    public void play(Path audioFile, float volume) throws Exception {
        stop();
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile.toFile())) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
            SourceDataLine output = (SourceDataLine) AudioSystem.getLine(info);
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
