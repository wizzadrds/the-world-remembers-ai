package com.wizzadrds.theworldremembers.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VoiceClientConfig {
    private static final String LEGACY_DEFAULT_TTS_INSTRUCTIONS =
            "Speak only the villager's exact dialogue. No narration, no stage directions, no scene description, no assistant wording. "
                    + "Keep Spanish natural, conversational and concise. Do not add \"hmm\", \"hrrm\", grunts, or artificial villager vocalizations. The configured villager voice pipeline supplies the NPC timbre.";
    private static final String LEGACY_DEFAULT_TTS_INSTRUCTIONS_ES =
            "Speak the exact words written in natural, correct Spanish. Use standard grammar, natural word order, correct verb conjugations, and ordinary vocabulary. "
                    + "Do not insert filler words, random words, repetitions, false starts, or words that do not belong in the sentence. "
                    + "No narration, stage directions, sound effects, grunts, or artificial villager noises. The voice model provides the villager sound.";
    private static final String DEFAULT_TTS_INSTRUCTIONS =
            "Speak the exact words written in natural, correct Spanish, as one connected utterance with natural rhythm. "
                    + "Keep normal continuity between words; do not add dramatic pauses, false starts, ellipses, repetitions, or extra words. "
                    + "Use standard grammar, natural word order, correct verb conjugations, and ordinary vocabulary. "
                    + "No narration, stage directions, sound effects, grunts, or artificial villager noises. The voice model provides the villager sound.";
    private static final String LEGACY_DEFAULT_SYSTEM_PROMPT =
            "You are the specific Minecraft villager the player is standing near and speaking to. "
                    + "Reply only with that villager's spoken dialogue. Never narrate actions, emotions, scenes, or third-person events. "
                    + "Never answer as a generic AI. Do not add \"hmm\", \"hrrm\", grunts, or other artificial vocalizations. Keep the reply brief, conversational, and in character.";
    private static final String LEGACY_DEFAULT_SYSTEM_PROMPT_ES =
            "Eres un aldeano concreto de Minecraft hablando cara a cara con el jugador. Responde siempre en español natural y correcto. "
                    + "La prioridad absoluta es que cada frase esté bien construida: orden normal de las palabras, concordancia, verbos bien conjugados y sentido claro. "
                    + "No improvises palabras sueltas ni metas palabras donde no encajan. No uses frases telegráficas, traducciones literales ni expresiones raras. "
                    + "Contesta directamente a lo que te preguntan con una sola frase breve de 5 a 9 palabras. Si hace falta, usa hasta 12 palabras para que la frase sea correcta y completa. "
                    + "Usa vocabulario sencillo pero adulto, natural y variado; no hables como un niño ni como alguien que no sabe expresarse. "
                    + "Adapta el contenido a tu profesión solo si viene al caso. Si no sabes algo, dilo con naturalidad; no inventes datos. "
                    + "Devuelve únicamente el diálogo hablado, sin narración, acciones, nombres, sonidos, gruñidos, muletillas, repeticiones ni explicaciones. Termina la frase completa y no añadas nada más.";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "Eres un aldeano concreto de Minecraft hablando cara a cara con el jugador. Responde siempre en español natural y correcto. "
                    + "Construye frases completas, con orden natural, concordancia correcta y verbos bien conjugados. "
                    + "No digas palabras sueltas ni fragmentes la frase. No uses frases telegráficas, traducciones literales, elipsis (...) ni puntos suspensivos para separar palabras. "
                    + "Contesta directamente con una o dos frases conectadas, normalmente de 10 a 16 palabras en total, sin alargar por alargar. "
                    + "Usa vocabulario sencillo, adulto y natural. Adapta el contenido a tu profesión solo si viene al caso; no inventes datos. "
                    + "Devuelve únicamente el diálogo hablado, sin narración, acciones, nombres, sonidos, gruñidos, muletillas, repeticiones ni explicaciones. Debe sonar como una intervención continua y natural, no como palabras aisladas. Termina la frase completa y no añadas nada más.";
    private static final String LEGACY_DEFAULT_VILLAGER_TTS_COMMAND =
            "python tools/voice/tts_rvc_villager.py {text} {output} {language} {model} {rate} {pitch} {expressiveness}";
    private static final String DEFAULT_VILLAGER_TTS_COMMAND = "";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String microphone = AudioDeviceManager.DEFAULT_DEVICE;
    public String outputDevice = AudioDeviceManager.DEFAULT_DEVICE;
    public boolean villagerVoicesEnabled = true;
    public String villagerVoiceTemperament = "WARM";
    public int pushToTalkKey = 86;
    public boolean pushToTalkMode = true;
    public float inputVolume = 1.0f;
    public float outputVolume = 1.0f;
    public float voiceDistance = 32.0f;
    public String provider = "gemini";
    public String language = "es-ES";
    public String apiKey = "";
    public String model = "gemini-3.5-flash-lite";
    public String sttModel = "gemini-3.5-transcribe";
    public String sttCommand = "";
    public String ttsCommand = "";
    public String ttsModel = "gemini-3.8-flash-tts";
    public String ttsVoice = "Algenib";
    public String ttsInstructions = DEFAULT_TTS_INSTRUCTIONS;
    /** Local villager-only TTS pipeline: Piper Spanish source -> VillagerTITAN RVC. */
    public String villagerTtsCommand = DEFAULT_VILLAGER_TTS_COMMAND;
    public String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    public static VoiceClientConfig load(Path gameDir) {
        Path file = file(gameDir);
        try {
            if (Files.exists(file)) {
                VoiceClientConfig config = GSON.fromJson(Files.readString(file), VoiceClientConfig.class);
                if (config != null) return config.normalized();
            }
        } catch (Exception ignored) {
            backupCorruptConfig(file);
        }
        return new VoiceClientConfig();
    }

    public VoiceClientConfig normalized() {
        if (microphone == null || microphone.isBlank()) microphone = AudioDeviceManager.DEFAULT_DEVICE;
        if (outputDevice == null || outputDevice.isBlank()) outputDevice = AudioDeviceManager.DEFAULT_DEVICE;
        if (villagerVoiceTemperament == null || villagerVoiceTemperament.isBlank()) villagerVoiceTemperament = "WARM";
        if (pushToTalkKey <= 0) pushToTalkKey = 86;
        inputVolume = finiteClamp(inputVolume, 0.0f, 2.0f, 1.0f);
        outputVolume = finiteClamp(outputVolume, 0.0f, 2.0f, 1.0f);
        voiceDistance = finiteClamp(voiceDistance, 1.0f, 64.0f, 32.0f);
        if (provider == null || provider.isBlank()) provider = "gemini";
        // Migrate the old default model that produced very long first-token delays.
        if (model == null || model.isBlank() || model.equals("gemini-3.8-flash")) model = "gemini-3.5-flash-lite";
        else model = model.trim();
        if (apiKey == null) apiKey = "";
        else apiKey = apiKey.trim();
        if (language == null || language.isBlank()) language = "es-ES";
        if (sttModel == null || sttModel.isBlank()) sttModel = "gemini-3.5-transcribe";
        else sttModel = sttModel.trim();
        if (ttsModel == null || ttsModel.isBlank()) ttsModel = "gemini-3.8-flash-tts";
        else ttsModel = ttsModel.trim();
        if (ttsVoice == null || ttsVoice.isBlank() || ttsVoice.equalsIgnoreCase("Kore")) ttsVoice = "Algenib";
        else ttsVoice = ttsVoice.trim();
        if (sttCommand == null) sttCommand = "";
        else sttCommand = sttCommand.trim();
        if (ttsCommand == null) ttsCommand = "";
        else ttsCommand = ttsCommand.trim();
        if (villagerTtsCommand == null) villagerTtsCommand = "";
        else {
            villagerTtsCommand = villagerTtsCommand.trim();
            // The previous out-of-box command required users to install Python, Piper and RVC.
            // Migrate only that old default to cloud TTS; custom local commands remain untouched.
            if (villagerTtsCommand.equals(LEGACY_DEFAULT_VILLAGER_TTS_COMMAND)) villagerTtsCommand = "";
        }
        if (ttsInstructions == null || ttsInstructions.isBlank() || ttsInstructions.equals(LEGACY_DEFAULT_TTS_INSTRUCTIONS)
                || ttsInstructions.equals(LEGACY_DEFAULT_TTS_INSTRUCTIONS_ES)) {
            ttsInstructions = DEFAULT_TTS_INSTRUCTIONS;
        }
        if (systemPrompt == null || systemPrompt.isBlank() || systemPrompt.equals(LEGACY_DEFAULT_SYSTEM_PROMPT)
                || systemPrompt.equals(LEGACY_DEFAULT_SYSTEM_PROMPT_ES)
                || systemPrompt.startsWith("You are one specific Minecraft villager")) {
            systemPrompt = DEFAULT_SYSTEM_PROMPT;
        }
        return this;
    }

    private static float finiteClamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    public boolean save(Path gameDir) {
        Path file = file(gameDir);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(normalized()), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            return false;
        } finally {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
        }
        try {
            VoiceClientConfig persisted = load(gameDir);
            return java.util.Objects.equals(persisted.apiKey, normalized().apiKey);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void backupCorruptConfig(Path file) {
        try {
            if (!Files.isRegularFile(file)) return;
            Path backup = file.resolveSibling(file.getFileName() + ".broken");
            if (Files.exists(backup)) {
                int suffix = 2;
                Path candidate;
                do {
                    candidate = file.resolveSibling(file.getFileName() + ".broken." + suffix++);
                } while (Files.exists(candidate) && suffix < 1000);
                backup = candidate;
            }
            Files.move(file, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }

    private static Path file(Path gameDir) {
        return gameDir.resolve("config").resolve("the_world_remembers_voice.json");
    }
}