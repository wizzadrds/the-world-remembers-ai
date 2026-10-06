package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Path;

public final class VoiceAudioPlayer {
    private volatile SourceDataLine line;

    public void play(Path audioFile, float volume) throws Exception {
        stop();
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile.toFile())) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
            SourceDataLine output = (SourceDataLine) AudioSystem.getLine(info);
            output.open(stream.getFormat());
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
