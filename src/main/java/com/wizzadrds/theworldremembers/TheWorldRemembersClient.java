package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.chronicle.*;
import com.wizzadrds.theworldremembers.voice.*;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Files;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

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
    private static int voiceSequence;
    private static ExecutorService villagerSpeechExecutor;
    private static String appliedOutputDevice;
    private static final AtomicInteger pendingVillagerSpeech = new AtomicInteger();
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
        villagerSpeechExecutor = Executors.newSingleThreadExecutor(r -> {
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
                if (voiceConfig != null && voiceConfig.villagerVoicesEnabled) speakVillager(payload);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ChronicleResponsePacket.TYPE, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new ChronicleScreen(payload.lines()))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
                boolean down = voiceConfig.pushToTalkMode
                        ? InputConstants.isKeyDown(client.getWindow().getWindow(), voiceConfig.pushToTalkKey)
                        : false;
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
            }
        });
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

    private static void processVoice(byte[] pcm) {
        var sttCommand = VoiceCommandParser.parse(voiceConfig.sttCommand);
        var ttsCommand = VoiceCommandParser.parse(voiceConfig.ttsCommand);
        if (sttCommand.isEmpty() || ttsCommand.isEmpty()) {
            voiceConversation.fail();
            return;
        }

        var service = new VoiceService(
                new LocalProcessSttAdapter(sttCommand),
                new LocalProcessTtsAdapter(ttsCommand));

        voiceConversation.finishListening(pcm, service, transcript -> {
            try {
                AiChatAdapter ai = createAiAdapter();
                String reply = ai.respond(transcript, voiceConfig.systemPrompt);
                if (reply == null || reply.isBlank()) {
                    throw new IllegalStateException("AI returned an empty reply");
                }

                VoiceProfile profile = new VoiceProfile(
                        speechLanguage(), voiceConfig.ttsModel, VoiceTemperament.CALM, 1.0f, 1.0f, 0.5f);
                Path output = Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("config")
                        .resolve("the_world_remembers_voice_response_" + UUID.randomUUID() + ".wav");
                voiceConversation.synthesizeAndSpeak(
                        reply, service, profile, output, voicePlayer, voiceConfig.outputVolume, ignored -> {});
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Voice AI request interrupted", e);
            } catch (Exception e) {
                throw new RuntimeException("Voice AI request failed", e);
            }
        });
    }

    private static AiChatAdapter createAiAdapter() {
        String provider = voiceConfig.provider == null ? "" : voiceConfig.provider.trim().toLowerCase();
        return switch (provider) {
            case "", "openai", "openai-responses" ->
                    new OpenAiResponsesAdapter(voiceConfig.apiKey, voiceConfig.model);
            default -> throw new IllegalArgumentException(
                    "Unsupported voice AI provider: " + voiceConfig.provider
                            + ". Supported providers: openai");
        };
    }

    private static void cleanupVoiceSession() {
        voiceKeyWasDown = false;
        microphone.stop();
        voiceConversation.reset();
        voiceStreamPlayer.stop();
        voicePlayer.stop();
        if (villagerSpeechExecutor != null) {
            villagerSpeechExecutor.shutdownNow();
            villagerSpeechExecutor = null;
        }
        pendingVillagerSpeech.set(0);
    }

    private static void speakVillager(VillagerVoicePacket payload) {
        if (voiceConfig == null || voiceConfig.ttsCommand == null || voiceConfig.ttsCommand.isBlank()) return;
        if (villagerSpeechExecutor == null) return;
        int reserved;
        do {
            reserved = pendingVillagerSpeech.get();
            if (reserved >= 4) return;
        } while (!pendingVillagerSpeech.compareAndSet(reserved, reserved + 1));
        villagerSpeechExecutor.submit(() -> {
            Path output = null;
            try {
                var command = VoiceCommandParser.parse(voiceConfig.ttsCommand);
                if (command.isEmpty()) {
                    lastVillagerVoiceError = "TTS command is empty";
                    return;
                }
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
                var tts = new LocalProcessTtsAdapter(command);
                Path audio = tts.synthesize(payload.text(), profile, output);
                if (audio != null && Files.isRegularFile(audio)) {
                    lastVillagerVoiceError = "";
                    voicePlayer.play(audio, Math.max(0.0f, Math.min(2.0f, voiceConfig.outputVolume)));
                } else {
                    lastVillagerVoiceError = "TTS did not produce a WAV file";
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
