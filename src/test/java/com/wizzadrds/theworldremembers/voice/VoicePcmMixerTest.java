package com.wizzadrds.theworldremembers.voice;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoicePcmMixerTest {
    @Test
    void mixesLittleEndianPcmSamples() {
        byte[] first = pcm(1000, -1000);
        byte[] second = pcm(2000, 500);

        assertArrayEquals(pcm(3000, -500), VoicePcmMixer.mix(List.of(first, second)));
    }

    @Test
    void saturatesPositiveAndNegativeOverflow() {
        byte[] positive = VoicePcmMixer.mix(List.of(pcm(30_000, 20_000), pcm(10_000, 20_000)));
        byte[] negative = VoicePcmMixer.mix(List.of(pcm(-30_000, -20_000), pcm(-10_000, -20_000)));

        assertArrayEquals(pcm(Short.MAX_VALUE, Short.MAX_VALUE), positive);
        assertArrayEquals(pcm(Short.MIN_VALUE, Short.MIN_VALUE), negative);
    }

    @Test
    void ignoresNullFramesAndUsesShortestEvenLength() {
        byte[] full = pcm(100, 200, 300);
        byte[] shortFrame = pcm(-50);

        assertArrayEquals(pcm(50), VoicePcmMixer.mix(List.of(null, full, shortFrame)));
    }

    @Test
    void returnsNullForMissingOrEmptyAudio() {
        assertNull(VoicePcmMixer.mix(null));
        assertNull(VoicePcmMixer.mix(List.of()));
        assertNull(VoicePcmMixer.mix(List.of(new byte[0])));
        assertNull(VoicePcmMixer.mix(Collections.singletonList(null)));
    }

    private static byte[] pcm(int... samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int sample = samples[i];
            bytes[i * 2] = (byte) sample;
            bytes[i * 2 + 1] = (byte) (sample >> 8);
        }
        return bytes;
    }
}
