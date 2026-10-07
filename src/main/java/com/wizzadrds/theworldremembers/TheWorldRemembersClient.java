package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.chronicle.*;
import com.wizzadrds.theworldremembers.voice.*;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static int pendingVillagerSpeech;

    public static VoicePacket lastVoice() { return lastVoice; }
    public static VoiceClientConfig voiceConfig() { return voiceConfig; }
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
                boolean down = VOICE_KEY.isDown();
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
            villagerSpeechExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "twr-villager-voice");
                thread.setDaemon(true);
                return thread;
            });
        }
    }

    private static void speakVillager(VoicePacket payload) {
        if (voiceConfig == null || voiceConfig.ttsCommand == null || voiceConfig.ttsCommand.isBlank()) return;
        if (villagerSpeechExecutor == null) return;
        if (pendingVillagerSpeech >= 4) return;
        pendingVillagerSpeech++;
        villagerSpeechExecutor.submit(() -> {
            Path output = null;
            try {
                var command = VoiceCommandParser.parse(voiceConfig.ttsCommand);
                if (command.isEmpty()) return;
                VoiceTemperament temperament;
                try {
                    temperament = VoiceTemperament.valueOf(
                            voiceConfig.villagerVoiceTemperament == null ? "WARM" : voiceConfig.villagerVoiceTemperament.toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    temperament = VoiceTemperament.WARM;
                }
                float rate = clampVoice(payload.rate(), 0.60f, 1.30f);
                float pitch = clampVoice(payload.pitch(), 0.70f, 1.30f);
                float expressiveness = clampVoice(payload.expressiveness(), 0.0f, 1.0f);
                String modelOrVoice = voiceConfig.ttsVoice == null || voiceConfig.ttsVoice.isBlank()
                        ? voiceConfig.ttsModel : voiceConfig.ttsVoice;
                VoiceProfile profile = new VoiceProfile(speechLanguage(), modelOrVoice, temperament, rate, pitch, expressiveness);
                output = Minecraft.getInstance().gameDirectory.toPath().resolve("config")
                        .resolve("twr_villager_" + UUID.randomUUID() + ".wav");
                var tts = new LocalProcessTtsAdapter(command);
                Path audio = tts.synthesize(payload.text(), profile, output);
                if (audio != null && Files.isRegularFile(audio)) {
                    voicePlayer.play(audio, Math.max(0.0f, Math.min(2.0f, voiceConfig.outputVolume)));
                }
            } catch (Exception ignored) {
                // Local TTS is optional: a missing/broken adapter must never stop gameplay.
            } finally {
                if (output != null) {
                    try { Files.deleteIfExists(output); } catch (Exception ignored) {}
                }
            }
        });
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
