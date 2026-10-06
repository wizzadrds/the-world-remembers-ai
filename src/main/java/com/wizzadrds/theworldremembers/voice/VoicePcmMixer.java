package com.wizzadrds.theworldremembers.voice;

import java.util.Collection;

public final class VoicePcmMixer {
    private VoicePcmMixer() {}

    public static byte[] mix(Collection<byte[]> frames) {
        if (frames == null || frames.isEmpty()) return null;

        int length = frames.stream()
                .filter(frame -> frame != null)
                .mapToInt(frame -> frame.length)
                .min()
                .orElse(0);
        if (length == 0) return null;

        length &= ~1;
        byte[] mixed = new byte[length];
        for (int offset = 0; offset < length; offset += 2) {
            int sample = 0;
            for (byte[] frame : frames) {
                if (frame == null || frame.length < offset + 2) continue;
                sample += (short) ((frame[offset] & 0xFF) | (frame[offset + 1] << 8));
            }
            sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
            mixed[offset] = (byte) sample;
            mixed[offset + 1] = (byte) (sample >> 8);
        }
        return mixed;
    }
}
