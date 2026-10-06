package com.wizzadrds.theworldremembers.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class VoiceStreamPlayer implements AutoCloseable {
    private static final AudioFormat FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);
    private static final byte[] POISON = new byte[0];
    private static final int MAX_QUEUED_FRAMES = 12;
    private static final int MAX_REORDER_FRAMES = 4;

    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(MAX_QUEUED_FRAMES);
    private final Map<UUID, Integer> lastSequences = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Integer> eldest) {
            return size() > 128;
        }
    };
    private final Map<UUID, TreeMap<Integer, byte[]>> pendingSequences = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, TreeMap<Integer, byte[]>> eldest) {
            return size() > 128;
        }
    };
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

    public synchronized void enqueue(UUID speaker, int sequence, byte[] pcm) {
        if (speaker == null || pcm == null || pcm.length == 0) return;
        Integer previous = lastSequences.get(speaker);
        if (previous != null && sequence <= previous) return;

        TreeMap<Integer, byte[]> pending = pendingSequences.computeIfAbsent(speaker, ignored -> new TreeMap<>());
        if (previous == null || sequence == previous + 1) {
            enqueueReadyFrame(speaker, sequence, pcm.clone());
            flushContiguous(speaker, pending);
            return;
        }

        if (pending.putIfAbsent(sequence, pcm.clone()) != null) return;
        if (pending.size() >= MAX_REORDER_FRAMES) {
            Map.Entry<Integer, byte[]> next = pending.pollFirstEntry();
            if (next != null) {
                enqueueReadyFrame(speaker, next.getKey(), next.getValue());
            }
        }
        if (pending.isEmpty()) pendingSequences.remove(speaker);
    }

    private void flushContiguous(UUID speaker, TreeMap<Integer, byte[]> pending) {
        Integer last = lastSequences.get(speaker);
        while (last != null) {
            Map.Entry<Integer, byte[]> next = pending.firstEntry();
            if (next == null || next.getKey() != last + 1) break;
            pending.pollFirstEntry();
            enqueueReadyFrame(speaker, next.getKey(), next.getValue());
            last = next.getKey();
        }
        if (pending.isEmpty()) pendingSequences.remove(speaker);
    }

    private void enqueueReadyFrame(UUID speaker, int sequence, byte[] pcm) {
        lastSequences.put(speaker, sequence);
        start();
        if (!running) return;
        if (!queue.offer(pcm)) {
            queue.poll();
            queue.offer(pcm);
        }
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
        lastSequences.clear();
        pendingSequences.clear();
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
