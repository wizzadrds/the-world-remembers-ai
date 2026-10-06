package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.FloatControl;\nimport net.minecraft.client.Minecraft;
import java.nio.file.Path;

public final class VoiceAudioPlayer {
    private volatile SourceDataLine line;

    public static List<String> devices() { List<String> r=new ArrayList<>(); r.add("Default"); for(Mixer.Info i:AudioSystem.getMixerInfo()){Mixer m=AudioSystem.getMixer(i); if(m.isLineSupported(new DataLine.Info(SourceDataLine.class,null))) r.add(i.getName());} return List.copyOf(r); }\n\n    public void play(Path audioFile, float volume) throws Exception {
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

    public void play(Path audioFile, float volume, String deviceName) throws Exception {\n        stop();\n        try (AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile.toFile())) {\n            DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());\n            Mixer mixer = findMixer(deviceName, info);\n            SourceDataLine output = mixer == null ? (SourceDataLine) AudioSystem.getLine(info) : (SourceDataLine) mixer.getLine(info);\n            output.open(stream.getFormat());\n            output.start(); line=output; byte[] buffer=new byte[8192]; int read;\n            while(line==output && (read=stream.read(buffer))>=0) if(read>0) output.write(buffer,0,read);\n            if(line==output) output.drain();\n        } finally { stop(); }\n    }\n\n    private static Mixer findMixer(String name, DataLine.Info info){ if(name==null||name.isBlank()||name.equalsIgnoreCase("Default")) return null; for(Mixer.Info i:AudioSystem.getMixerInfo()) if(i.getName().equalsIgnoreCase(name)){Mixer m=AudioSystem.getMixer(i); if(m.isLineSupported(info)) return m;} return null; }\n\n    public void stop() {
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
