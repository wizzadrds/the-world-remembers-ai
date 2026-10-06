package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class VoiceStreamPlayer implements AutoCloseable {
    private static final AudioFormat FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);
    private static final byte[] POISON = new byte[0];

    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(32);
    private volatile SourceDataLine line;
    private volatile boolean running;
    private volatile Thread worker;

    public synchronized void start() {
        if (running) return;
        try {
            SourceDataLine output = (SourceDataLine) AudioSystem.getLine(
                    new DataLine.Info(SourceDataLine.class, FORMAT));
            output.open(FORMAT, 6400);
            output.start();
            line = output;
            running = true;
            worker = Thread.ofVirtual().name("twr-voice-playback").start(() -> playLoop(output));
        } catch (Exception ignored) {
            running = false;
            line = null;
        }
    }

    public void enqueue(byte[] pcm) {
        if (pcm == null || pcm.length == 0) return;
        start();
        if (!running) return;
        byte[] copy = pcm.clone();
        queue.offer(copy);
    }

    private void playLoop(SourceDataLine output) {
        try {
            while (running && line == output) {
                byte[] pcm = queue.take();
                if (pcm == POISON) break;
                output.write(pcm, 0, pcm.length);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (line == output) {
                output.stop();
                output.flush();
                output.close();
                line = null;
            }
        }
    }

    public synchronized void stop() {
        running = false;
        queue.clear();
        queue.offer(POISON);
        Thread current = worker;
        worker = null;
        if (current != null && current != Thread.currentThread()) {
            current.interrupt();
        }
        SourceDataLine currentLine = line;
        line = null;
        if (currentLine != null) {
            currentLine.stop();
            currentLine.flush();
            currentLine.close();
        }
    }

    @Override
    public void close() {
        stop();
    }
}
