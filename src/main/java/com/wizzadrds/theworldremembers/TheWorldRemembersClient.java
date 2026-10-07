package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.chronicle.*;
import com.wizzadrds.theworldremembers.voice.*;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.file.Files;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.npc.villager.Villager;

public final class TheWorldRemembersClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "chronicles"));
    private static final KeyMapping CHRONICLE_KEY =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.the_world_remembers.chronicles", InputConstants.Type.KEYSYM, InputConstants.KEY_J, CATEGORY));
    private static final KeyMapping VOICE_KEY =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.the_world_remembers.voice", InputConstants.Type.KEYSYM, InputConstants.KEY_V, CATEGORY));
    private static final KeyMapping SETTINGS_KEY =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.the_world_remembers.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORY));

    private static volatile VoicePacket lastVoice;
    private static VoiceClientConfig voiceConfig;
    private static VoiceConversationController voiceConversation;
    private static MicrophoneCapture microphone;
    private static VoiceStreamPlayer voiceStreamPlayer;
    private static VoiceAudioPlayer voicePlayer;
    private static boolean voiceKeyWasDown;
    private static boolean autoVoiceActive;
    private static int autoVoiceSilenceTicks;
    private static final float AUTO_VOICE_THRESHOLD = 0.025f;
    private static final int AUTO_VOICE_SILENCE_TICKS = 12;
    private static int voiceSequence;
    private static int audioDevicePollTicks;
    private static ExecutorService villagerSpeechExecutor;
    private static String appliedOutputDevice;
    private static final AtomicInteger pendingVillagerSpeech = new AtomicInteger();
    /** Changes whenever the connected world/session changes, invalidating old TTS jobs. */
    private static final AtomicLong voiceSessionGeneration = new AtomicLong();
    private static volatile String lastVillagerVoiceError = "";

    public static VoicePacket lastVoice() { return lastVoice; }
    public static VoiceClientConfig voiceConfig() { return voiceConfig; }
    public static float microphoneLevel() {
        return microphone == null ? 0.0f : microphone.level();
    }

    public static boolean microphoneCapturing() {
        return microphone != null && microphone.isCapturing();
    }

    public static String lastVillagerVoiceError() { return lastVillagerVoiceError; }

    public static void testVillagerVoice() {
        if (voiceConfig == null || !voiceConfig.villagerVoicesEnabled) {
            lastVillagerVoiceError = "Villager voices are disabled";
            return;
        }
        UUID speaker = UUID.nameUUIDFromBytes("twr-villager-test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        speakVillager(new VillagerVoicePacket(speaker, "farmer", "Hola, vecino. Soy un aldeano. ¿Me escuchas bien?", 0, 0, 0, 32.0f, 1.0f, 1.0f, 1.0f, 0.7f, 1));
    }

    public static boolean startMicrophoneTest() {
        if (microphone == null || voiceConfig == null) return false;
        if (microphone.isCapturing()) return true;
        return microphone.start(voiceConfig.microphone, voiceConfig.inputVolume, ignored -> {});
    }

    public static void stopMicrophoneTest() {
        if (microphone != null) microphone.stop();
    }

    public static VoiceConversationState voiceState() {
        return voiceConversation == null ? VoiceConversationState.IDLE : voiceConversation.state();
    }

    public void onInitializeClient() {
        voiceConfig = VoiceClientConfig.load(Minecraft.getInstance().gameDirectory.toPath());
        voiceConversation = new VoiceConversationController();
        microphone = new MicrophoneCapture();
        voiceStreamPlayer = new VoiceStreamPlayer();
        voicePlayer = new VoiceAudioPlayer();
        appliedOutputDevice = voiceConfig.outputDevice;
        voiceStreamPlayer.setOutputDevice(voiceConfig.outputDevice);
        voicePlayer.setOutputDevice(voiceConfig.outputDevice);
        villagerSpeechExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "twr-villager-voice");
            thread.setDaemon(true);
            return thread;
        });

        VoiceHud.register(VOICE_KEY, voiceConversation);
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> cleanupVoiceSession());

        ClientPlayNetworking.registerGlobalReceiver(VoiceAudioPacket.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (client.player == null || payload.speaker().equals(client.player.getUUID())) return;
                double distance = client.player.distanceToSqr(payload.x(), payload.y(), payload.z());
                double radius = Math.max(1.0, Math.min(64.0, payload.maxDistance()));
                if (distance >= radius * radius) return;
                float attenuation = (float) Math.max(0.0, 1.0 - Math.sqrt(distance) / radius);
                float gain = Math.max(0.0f, Math.min(2.0f, payload.volume() * voiceConfig.outputVolume * attenuation));
                voiceStreamPlayer.enqueue(payload.speaker(), payload.sequence(), scalePcm(payload.pcm(), gain));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(VillagerVoicePacket.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (voiceConfig != null && voiceConfig.villagerVoicesEnabled) speakVillager(payload);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(VoicePacket.TYPE, (payload, context) -> {
            lastVoice = payload;
            Minecraft client = context.client();
            client.execute(() -> {
                if (voiceConfig == null || !voiceConfig.villagerVoicesEnabled || client.player == null) return;
                double distance = client.player.distanceToSqr(payload.x(), payload.y(), payload.z());
                double radius = Math.max(1.0, Math.min(64.0, payload.maxDistance()));
                if (distance >= radius * radius) return;
                UUID speaker = UUID.nameUUIDFromBytes(
                        (payload.text() + "|" + payload.x() + "|" + payload.y() + "|" + payload.z())
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                speakVillager(new VillagerVoicePacket(
                        speaker, "villager", payload.text(),
                        payload.x(), payload.y(), payload.z(),
                        payload.maxDistance(), payload.volume(), payload.rate(),
                        payload.pitch(), payload.expressiveness(), payload.priority()));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ChronicleResponsePacket.TYPE, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new ChronicleScreen(payload.lines()))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++audioDevicePollTicks >= 20) {
                audioDevicePollTicks = 0;
                checkSelectedAudioDevices();
            }
            while (CHRONICLE_KEY.consumeClick()) {
                if (client.player != null) openChronicles();
            }
            while (SETTINGS_KEY.consumeClick()) {
                client.gui.setScreen(new VoiceSettingsScreen(client.gui.screen(), voiceConfig));
            }
            if (voiceConfig != null && !java.util.Objects.equals(appliedOutputDevice, voiceConfig.outputDevice)) {
                appliedOutputDevice = voiceConfig.outputDevice;
                voiceStreamPlayer.setOutputDevice(appliedOutputDevice);
                voicePlayer.setOutputDevice(appliedOutputDevice);
            }

            if (client.player != null && client.gui.screen() == null) {
                if (voiceConfig.pushToTalkMode) {
                    autoVoiceActive = false;
                    autoVoiceSilenceTicks = 0;
                    boolean down = InputConstants.isKeyDown(client.getWindow(), voiceConfig.pushToTalkKey);
                    if (down && !voiceKeyWasDown) {
                        boolean started = microphone.start(voiceConfig.microphone, voiceConfig.inputVolume, frame ->
                                sendVoiceFrame(client, frame));
                        if (started) voiceConversation.beginListening();
                        else voiceConversation.fail();
                    } else if (!down && voiceKeyWasDown) {
                        byte[] pcm = microphone.stop();
                        if (pcm.length > 0) processVoice(pcm);
                        else voiceConversation.fail();
                    }
                    voiceKeyWasDown = down;
                } else {
                    voiceKeyWasDown = false;
                    handleVoiceActivation(client);
                }
            }
        });
    }

    /**
     * Detect selected-device removal without ever falling back to another device.
     * The check is deliberately throttled because Java Sound mixer enumeration can be expensive.
     */
    private static void checkSelectedAudioDevices() {
        if (voiceConfig == null) return;
        String microphoneDevice = voiceConfig.microphone;
        if (microphoneDevice != null
                && !microphoneDevice.isBlank()
                && !microphoneDevice.equalsIgnoreCase(AudioDeviceManager.DEFAULT_DEVICE)
                && !AudioDeviceManager.inputAvailable(microphoneDevice)) {
            if (microphone.isCapturing()) microphone.stop();
            if (voiceConversation.state() != VoiceConversationState.IDLE) voiceConversation.fail();
        }

        String outputDevice = voiceConfig.outputDevice;
        if (outputDevice != null
                && !outputDevice.isBlank()
                && !outputDevice.equalsIgnoreCase(AudioDeviceManager.DEFAULT_DEVICE)
                && !AudioDeviceManager.outputAvailable(outputDevice)) {
            voiceStreamPlayer.stop();
            voicePlayer.stop();
        }
    }

    private static void handleVoiceActivation(Minecraft client) {
        if (voiceConversation.state() == VoiceConversationState.PROCESSING
                || voiceConversation.state() == VoiceConversationState.SPEAKING) {
            return;
        }

        if (!autoVoiceActive) {
            if (!microphone.isCapturing()) {
                boolean started = microphone.start(voiceConfig.microphone, voiceConfig.inputVolume, frame -> {
                    if (autoVoiceActive) sendVoiceFrame(client, frame);
                });
                if (!started) {
                    voiceConversation.fail(microphone.lastError());
                    return;
                }
            }
            if (microphone.level() >= AUTO_VOICE_THRESHOLD) {
                autoVoiceActive = true;
                autoVoiceSilenceTicks = 0;
                voiceConversation.beginListening();
            }
            return;
        }

        if (microphone.level() >= AUTO_VOICE_THRESHOLD) {
            autoVoiceSilenceTicks = 0;
        } else if (++autoVoiceSilenceTicks >= AUTO_VOICE_SILENCE_TICKS) {
            byte[] pcm = microphone.stop();
            autoVoiceActive = false;
            autoVoiceSilenceTicks = 0;
            if (pcm.length > 0) processVoice(pcm);
            else voiceConversation.fail(microphone.lastError());
        }
    }

    private static void sendVoiceFrame(Minecraft client, byte[] pcm) {
        if (client.player == null || !ClientPlayNetworking.canSend(VoiceAudioPacket.TYPE)) return;
        ClientPlayNetworking.send(new VoiceAudioPacket(
                client.player.getUUID(),
                client.player.getX(),
                client.player.getY() + client.player.getEyeHeight(),
                client.player.getZ(),
                1.0f,
                Math.max(1.0f, Math.min(64.0f, voiceConfig.voiceDistance)),
                voiceSequence++,
                pcm));
    }

    private static byte[] scalePcm(byte[] pcm, float gain) {
        byte[] result = pcm.clone();
        if (gain == 1.0f) return result;
        for (int i = 0; i + 1 < result.length; i += 2) {
            short sample = (short) (((result[i + 1] & 0xFF) << 8) | (result[i] & 0xFF));
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
            result[i] = (byte) scaled;
            result[i + 1] = (byte) (scaled >> 8);
        }
        return result;
    }

    private static void saveMicrophoneRecording(byte[] pcm) {
        if (pcm == null || pcm.length == 0) return;
        try {
            Path directory = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("the_world_remembers")
                    .resolve("voice");
            Files.createDirectories(directory);
            Path wav = directory.resolve("last_recording.wav");
            Files.write(wav, MicrophoneCapture.wavBytes(pcm, (int) MicrophoneCapture.SAMPLE_RATE));
            System.out.println("[The World Remembers] Voice recording saved: " + wav
                    + " (" + pcm.length + " PCM bytes, level "
                    + String.format(java.util.Locale.ROOT, "%.4f", microphone.lastRecordingLevel()) + ")");
        } catch (Exception e) {
            System.err.println("[The World Remembers] Could not save voice recording: " + e.getMessage());
        }
    }

    private static void processVoice(byte[] pcm) {
        VillagerSpeaker villager = findNearbyVillager();
        if (villager == null) {
            voiceConversation.fail("Acércate a un aldeano para hablar con él.");
            return;
        }
        if (pcm != null && pcm.length > 0) {
            byte[] recordingCopy = pcm.clone();
            Thread.ofVirtual().name("twr-voice-recording-save").start(() -> saveMicrophoneRecording(recordingCopy));
        }
        final long session = voiceSessionGeneration.get();
        if (pcm == null || pcm.length == 0 || session != voiceSessionGeneration.get()) {
            voiceConversation.fail();
            return;
        }
        VoiceService service;
        if ("gemini".equalsIgnoreCase(voiceConfig.provider)) {
            service = new VoiceService(
                    new GeminiSttAdapter(voiceConfig.apiKey, voiceConfig.language),
                    new GeminiTtsAdapter(voiceConfig.apiKey, voiceConfig.ttsModel, voiceConfig.ttsVoice, voiceConfig.ttsInstructions));
        } else {
            var sttCommand = VoiceCommandParser.parse(voiceConfig.sttCommand);
            var ttsCommand = VoiceCommandParser.parse(voiceConfig.ttsCommand);
            if (sttCommand.isEmpty() || ttsCommand.isEmpty()) {
                voiceConversation.fail();
                return;
            }
            service = new VoiceService(
                    new LocalProcessSttAdapter(sttCommand),
                    new LocalProcessTtsAdapter(ttsCommand));
        }

        voiceConversation.finishListening(pcm, service, transcript -> {
            try {
                if (session != voiceSessionGeneration.get()) return;
                AiChatAdapter ai = createAiAdapter();
                String villagerPrompt = buildVillagerPrompt(villager);
                if ("gemini".equalsIgnoreCase(voiceConfig.provider)) {
                    java.util.concurrent.ExecutorService speechQueue = Executors.newSingleThreadExecutor(r -> {
                        Thread thread = new Thread(r, "twr-voice-sentence-tts");
                        thread.setDaemon(true);
                        return thread;
                    });
                    java.util.List<java.util.concurrent.Future<?>> speechJobs =
                            new java.util.concurrent.CopyOnWriteArrayList<>();
                    StringBuilder sentence = new StringBuilder();
                    try {
                        String reply;
                        try {
                            reply = ai.respondStreaming(transcript, villagerPrompt, chunk -> {
                            sentence.append(chunk);
                            int boundary;
                            while ((boundary = sentenceBoundary(sentence)) >= 0) {
                                String part = sentence.substring(0, boundary + 1).trim();
                                sentence.delete(0, boundary + 1);
                                if (!part.isBlank()) {
                                    speechJobs.add(speechQueue.submit(() ->
                                            speakResponseSentence(part, service, session)));
                                }
                            }
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Voice AI request failed: request interrupted", e);
                        } catch (Exception e) {
                            throw new RuntimeException("Voice AI request failed: " + rootMessage(e), e);
                        }
                        String tail = sentence.toString().trim();
                        if (!tail.isBlank()) {
                            speechJobs.add(speechQueue.submit(() ->
                                    speakResponseSentence(tail, service, session)));
                        }
                        for (java.util.concurrent.Future<?> job : speechJobs) job.get();
                        if (reply == null || reply.isBlank()) throw new IllegalStateException("AI returned an empty reply");
                        if (session == voiceSessionGeneration.get()) voiceConversation.finishSpeaking();
                    } finally {
                        speechQueue.shutdownNow();
                    }
                } else {
                    String reply;
                    try {
                        reply = ai.respond(transcript, villagerPrompt);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Voice AI request failed: request interrupted", e);
                    } catch (Exception e) {
                        throw new RuntimeException("Voice AI request failed: " + rootMessage(e), e);
                    }
                    if (session != voiceSessionGeneration.get()) return;
                    if (reply == null || reply.isBlank()) {
                        throw new IllegalStateException("AI returned an empty reply");
                    }
                    speakResponseSentence(reply, service, session);
                    if (session == voiceSessionGeneration.get()) voiceConversation.finishSpeaking();
                }
            } catch (Exception e) {
                throw e instanceof RuntimeException runtime ? runtime : new RuntimeException("Voice processing failed: " + rootMessage(e), e);
            }
        });
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) return current.getMessage();
            current = current.getCause();
        }
        return error == null ? "unknown error" : error.getClass().getSimpleName();
    }

    private static int sentenceBoundary(StringBuilder text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '…' || c == '.' || c == '!' || c == '?') {
                if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length()) {
                    char next = text.charAt(i + 1);
                    if (!Character.isWhitespace(next)) continue;
                    if (c == '.' && i > 0 && i + 1 < text.length()
                            && Character.isDigit(text.charAt(i - 1)) && Character.isDigit(next)) continue;
                }
                return i;
            }
        }
        return -1;
    }

    private static void speakResponseSentence(String text, VoiceService service, long session) {
        if (session != voiceSessionGeneration.get() || text == null || text.isBlank()) return;
        String responseVoice = voiceConfig.ttsVoice == null || voiceConfig.ttsVoice.isBlank()
                ? voiceConfig.ttsModel : voiceConfig.ttsVoice.trim();
        VoiceProfile profile = new VoiceProfile(
                speechLanguage(), responseVoice, VoiceTemperament.CALM, 1.0f, 1.0f, 0.5f);
        Path output = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("the_world_remembers_voice_response_" + UUID.randomUUID() + ".wav");
        try {
            TtsAdapter tts = "gemini".equalsIgnoreCase(voiceConfig.provider)
                    ? new GeminiTtsAdapter(voiceConfig.apiKey, voiceConfig.ttsModel, voiceConfig.ttsVoice, voiceConfig.ttsInstructions)
                    : new LocalProcessTtsAdapter(VoiceCommandParser.parse(voiceConfig.ttsCommand), voiceConfig.ttsInstructions);
            InputStream stream = tts.synthesizeStream(text, profile);
            if (session != voiceSessionGeneration.get()) {
                if (stream != null) stream.close();
                return;
            }
            if (stream != null) {
                voicePlayer.playVillagerPcmStream(stream, voiceConfig.outputVolume);
            } else {
                Path audio = tts.synthesize(text, profile, output);
                if (audio == null || !Files.isRegularFile(audio)) throw new IllegalStateException("TTS did not produce audio");
                voicePlayer.playVillager(audio, voiceConfig.outputVolume);
            }
        } catch (Exception e) {
            if (session == voiceSessionGeneration.get()) throw new RuntimeException("Voice TTS failed", e);
        } finally {
            try { Files.deleteIfExists(output); } catch (Exception ignored) {}
        }
    }

    private static AiChatAdapter createAiAdapter() {
        String provider = voiceConfig.provider == null ? "" : voiceConfig.provider.trim().toLowerCase();
        return switch (provider) {
            case "gemini" -> new GeminiResponsesAdapter(voiceConfig.apiKey, voiceConfig.model);
            case "", "openai", "openai-responses" ->
                    new OpenAiResponsesAdapter(voiceConfig.apiKey, voiceConfig.model);
            default -> throw new IllegalArgumentException(
                    "Unsupported voice AI provider: " + voiceConfig.provider
                            + ". Supported providers: gemini, openai");
        };
    }

    private static void cleanupVoiceSession() {
        voiceSessionGeneration.incrementAndGet();
        voiceKeyWasDown = false;
        microphone.stop();
        autoVoiceActive = false;
        autoVoiceSilenceTicks = 0;
        voiceConversation.reset();
        voiceStreamPlayer.stop();
        voicePlayer.stop();
        // Keep the daemon TTS executor alive across server reconnects. The client
        // initializer runs only once, so shutting it down here would disable villager
        // voices for every subsequent world/session until Minecraft restarts.
        pendingVillagerSpeech.set(0);
    }

    private static void speakVillager(VillagerVoicePacket payload) {
        if (voiceConfig == null) return;
        boolean gemini = "gemini".equalsIgnoreCase(voiceConfig.provider);
        if (!gemini && (voiceConfig.ttsCommand == null || voiceConfig.ttsCommand.isBlank())) return;
        if (gemini && (voiceConfig.apiKey == null || voiceConfig.apiKey.isBlank())) {
            lastVillagerVoiceError = "Gemini API key is missing";
            return;
        }
        if (villagerSpeechExecutor == null) return;
        int reserved;
        do {
            reserved = pendingVillagerSpeech.get();
            if (reserved >= 4) return;
        } while (!pendingVillagerSpeech.compareAndSet(reserved, reserved + 1));
        final long session = voiceSessionGeneration.get();
        villagerSpeechExecutor.submit(() -> {
            Path output = null;
            try {
                if (session != voiceSessionGeneration.get()) return;
                VoiceTemperament temperament = resolveVillagerTemperament(payload.profession());
                long seed = payload.speaker().getMostSignificantBits() ^ payload.speaker().getLeastSignificantBits();
                float stablePitch = 0.94f + ((seed & 0xFFL) / 255.0f) * 0.12f;
                float stableRate = 0.94f + (((seed >>> 8) & 0xFFL) / 255.0f) * 0.12f;
                float rate = clampVoice(payload.rate() * stableRate, 0.60f, 1.30f);
                float pitch = clampVoice(payload.pitch() * stablePitch, 0.70f, 1.30f);
                float expressiveness = clampVoice(payload.expressiveness(), 0.0f, 1.0f);
                String modelOrVoice = selectVillagerVoice(payload.speaker());
                VoiceProfile profile = new VoiceProfile(speechLanguage(), modelOrVoice, temperament, rate, pitch, expressiveness);
                output = Minecraft.getInstance().gameDirectory.toPath().resolve("config")
                        .resolve("twr_villager_" + UUID.randomUUID() + ".wav");
                TtsAdapter tts = gemini
                        ? new GeminiTtsAdapter(voiceConfig.apiKey, voiceConfig.ttsModel, voiceConfig.ttsVoice, voiceConfig.ttsInstructions)
                        : new LocalProcessTtsAdapter(VoiceCommandParser.parse(voiceConfig.ttsCommand), voiceConfig.ttsInstructions);
                InputStream streamedAudio = tts.synthesizeStream(payload.text(), profile);
                if (session != voiceSessionGeneration.get()) {
                    if (streamedAudio != null) streamedAudio.close();
                    return;
                }
                float volume = Math.max(0.0f, Math.min(2.0f, voiceConfig.outputVolume));
                if (streamedAudio != null) {
                    lastVillagerVoiceError = "";
                    voicePlayer.playVillagerPcmStream(streamedAudio, volume);
                } else {
                    Path audio = tts.synthesize(payload.text(), profile, output);
                    if (session != voiceSessionGeneration.get()) return;
                    if (audio != null && Files.isRegularFile(audio)) {
                        lastVillagerVoiceError = "";
                        voicePlayer.playVillager(audio, volume);
                    } else {
                        lastVillagerVoiceError = "TTS did not produce a WAV file";
                    }
                }
            } catch (Exception e) {
                lastVillagerVoiceError = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "TTS failed" : e.getMessage());
                // Local TTS is optional: a missing/broken adapter must never stop gameplay.
            } finally {
                if (output != null) {
                    try { Files.deleteIfExists(output); } catch (Exception ignored) {}
                }
                pendingVillagerSpeech.updateAndGet(value -> Math.max(0, value - 1));
            }
        });
    }

    private static String selectVillagerVoice(UUID speaker) {
        String configured = voiceConfig == null ? null : voiceConfig.ttsVoice;
        if (configured == null || configured.isBlank()) return voiceConfig.ttsModel;
        String[] voices = java.util.Arrays.stream(configured.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toArray(String[]::new);
        if (voices.length == 0) return voiceConfig.ttsModel;
        int index = Math.floorMod(speaker.hashCode(), voices.length);
        return voices[index];
    }

    private static VoiceTemperament resolveVillagerTemperament(String profession) {
        String p = profession == null ? "" : profession.toLowerCase(java.util.Locale.ROOT);
        if (p.contains("cleric") || p.contains("librarian")) return VoiceTemperament.CALM;
        if (p.contains("butcher") || p.contains("weaponsmith") || p.contains("toolsmith")) return VoiceTemperament.ASSERTIVE;
        if (p.contains("farmer") || p.contains("fisherman")) return VoiceTemperament.CHEERFUL;
        if (p.contains("nitwit")) return VoiceTemperament.TIMID;
        try {
            return VoiceTemperament.valueOf(voiceConfig.villagerVoiceTemperament == null ? "WARM" : voiceConfig.villagerVoiceTemperament.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return VoiceTemperament.WARM;
        }
    }


    private static VillagerSpeaker findNearbyVillager() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return null;
        double radius = Math.min(8.0, Math.max(1.0, voiceConfig.voiceDistance));
        double radiusSquared = radius * radius;
        Villager nearest = null;
        double nearestDistance = radiusSquared;
        for (Villager villager : client.level.getEntitiesOfClass(
                Villager.class,
                client.player.getBoundingBox().inflate(radius),
                candidate -> candidate.isAlive() && !candidate.isRemoved())) {
            double distance = villager.distanceToSqr(client.player);
            if (distance < nearestDistance) {
                nearest = villager;
                nearestDistance = distance;
            }
        }
        if (nearest == null) return null;
        String name = nearest.hasCustomName() && nearest.getCustomName() != null
                ? nearest.getCustomName().getString() : "aldeano";
        return new VillagerSpeaker(nearest.getUUID(), name);
    }

    private static String buildVillagerPrompt(VillagerSpeaker villager) {
        String base = voiceConfig.systemPrompt == null ? "" : voiceConfig.systemPrompt.trim();
        return base
                + " The speaking character is the nearby villager named \"" + villager.name()
                + "\". You are that villager, not an AI narrator. Output only the dialogue that this villager would say to the player. "
                + "Do not prefix the answer with the villager name. Do not describe actions or scenes. "
                + "Finish every sentence naturally before stopping.";
    }

    private record VillagerSpeaker(UUID id, String name) {}

    private static String speechLanguage() {
        if (voiceConfig == null || voiceConfig.language == null || voiceConfig.language.isBlank()) return "es-ES";
        String language = voiceConfig.language.trim().replace('_', '-');
        return language.length() > 32 ? language.substring(0, 32) : language;
    }

    private static float clampVoice(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }

    public static void openChronicles() { ChronicleNetworking.request(); }
}
