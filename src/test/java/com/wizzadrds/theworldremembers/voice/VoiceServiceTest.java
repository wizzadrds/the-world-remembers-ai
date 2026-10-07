package com.wizzadrds.theworldremembers.voice;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoiceServiceTest {
    @Test
    void adapterFailureDoesNotEscape() {
        var s = new VoiceService(p -> { throw new IOException(); }, (t, p, o) -> { throw new IOException(); });
        assertEquals("", s.transcribe(new byte[0]));
        assertNull(s.synthesize("x", new VoiceProfile("es", "m", VoiceTemperament.CALM, 1, 1, .5f), Path.of("x.wav")));
    }

    @Test
    void commandParserPreservesQuotedArguments() {
        assertEquals(List.of("python", "script.py", "hola mundo", "100%"),
                VoiceCommandParser.parse("python script.py \"hola mundo\" 100%"));
        assertEquals(List.of("say", "villager's hello"), VoiceCommandParser.parse("say 'villager\\'s hello'"));
    }

    @Test
    void commandParserRejectsUnclosedQuotes() {
        assertThrows(IllegalArgumentException.class, () -> VoiceCommandParser.parse("say \"hola"));
    }

    @Test
    void clientConfigNormalizesUnsafeAudioValuesAndDefaults() {
        var config = new VoiceClientConfig();
        config.inputVolume = Float.NaN;
        config.outputVolume = Float.POSITIVE_INFINITY;
        config.voiceDistance = -100;
        config.microphone = "";
        config.ttsModel = "  ";
        config.ttsVoice = "  es_ES-carlos  ";
        config.sttCommand = "  python stt.py  ";
        config.ttsCommand = "  python tts.py  ";
        config.normalized();
        assertEquals(1.0f, config.inputVolume);
        assertEquals(1.0f, config.outputVolume);
        assertEquals(1.0f, config.voiceDistance);
        assertEquals(AudioDeviceManager.DEFAULT_DEVICE, config.microphone);
        assertEquals("piper", config.ttsModel);
        assertEquals("es_ES-carlos", config.ttsVoice);
        assertEquals("python stt.py", config.sttCommand);
        assertEquals("python tts.py", config.ttsCommand);
    }

    @Test
    void corruptConfigIsBackedUpAndDefaultsAreReturned() throws Exception {
        Path gameDir = Files.createTempDirectory("twr-voice-config");
        Path file = gameDir.resolve("config/the_world_remembers_voice.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ definitely-not-json");
        VoiceClientConfig config = VoiceClientConfig.load(gameDir);
        assertEquals("es-ES", config.language);
        assertFalse(Files.exists(file));
        assertTrue(Files.exists(file.resolveSibling("the_world_remembers_voice.json.broken")));

        Files.writeString(file, "{ still-not-json");
        VoiceClientConfig.load(gameDir);
        assertTrue(Files.exists(file.resolveSibling("the_world_remembers_voice.json.broken.2")));
    }

    @Test
    void configRoundTripPersistsDeviceAndVoiceSettings() throws Exception {
        Path gameDir = Files.createTempDirectory("twr-voice-roundtrip");
        VoiceClientConfig config = new VoiceClientConfig();
        config.microphone = "USB Microphone";
        config.outputDevice = "Headphones";
        config.voiceDistance = 48;
        config.ttsVoice = "es_ES-carlos";
        config.save(gameDir);
        VoiceClientConfig loaded = VoiceClientConfig.load(gameDir);
        assertEquals("USB Microphone", loaded.microphone);
        assertEquals("Headphones", loaded.outputDevice);
        assertEquals(48.0f, loaded.voiceDistance);
        assertEquals("es_ES-carlos", loaded.ttsVoice);
    }
}
